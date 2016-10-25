/* ************************************************************************
#
#  designCraft.io
#
#  http://designcraft.io/
#
#  Copyright:
#    Copyright 2014 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */
package dcraft.web.http;

import java.net.InetSocketAddress;

import dcraft.bus.Message;
import dcraft.bus.MessageUtil;
import dcraft.bus.net.StreamMessage;
import dcraft.hub.Hub;
import dcraft.hub.HubState;
import dcraft.hub.SiteInfo;
import dcraft.lang.op.FuncCallback;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.OperationContextBuilder;
import dcraft.lang.op.OperationResult;
import dcraft.log.Logger;
import dcraft.net.NetUtil;
import dcraft.net.ssl.SslHandler;
import dcraft.session.DataStreamChannel;
import dcraft.session.IStreamDriver;
import dcraft.session.Session;
import dcraft.util.StringUtil;
import dcraft.web.WebModule;
import dcraft.web.core.HttpBodyRequestDecoder;
import dcraft.web.core.IContentDecoder;
import dcraft.web.core.Request;
import dcraft.web.core.Response;
import dcraft.web.core.RpcHandler;
import dcraft.web.core.WebContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;

/**
 * Handles handshakes and messages
 */
@SuppressWarnings("deprecation")
public class ServerHandler extends SimpleChannelInboundHandler<Object> {
	static protected final String RPC_PATH = "/rpc";
	static protected final String STATUS_PATH = "status";
	static protected final String DOWNLOAD_PATH = "download";
	static protected final String UPLOAD_PATH = "upload";

	// this is the context used until we figure out if we have a session or not
	static protected OperationContext defaultOpContext = OperationContext.useNewGuest();  
	
    protected WebContext context = null; 
    
	// TODO improve to ignore large POSTs on most Paths
	// TODO ip lockout
	// TODO acl
	// TODO debug level based in ip address
	// TODO any where along the way, especially RPC, ping Remote Trust Center with down votes if doesn't work out

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
    	//System.out.println("server got object: " + msg.getClass().getName());
    	OperationContext.set(ServerHandler.defaultOpContext);
    	
    	WebContext wctx = this.context;
    	
    	if (wctx == null) 
    		this.context = wctx = WebContext.forChannel(ctx.channel());

    	if (this.context.getChannel() == null)
    		this.context.setChannel(ctx.channel());
    	    	
    	if (msg instanceof HttpContent) {
    		this.context.offerContent((HttpContent)msg);
    		return;
    	}    	
    	
    	if (Logger.isDebug())
    		Logger.debug("Web server request " + msg.getClass().getName() + "  " + ctx.channel().localAddress() 
    				+ " from " + ctx.channel().remoteAddress()); 
    	
    	if (!(msg instanceof HttpRequest)) {
        	this.context.sendRequestBad();
            return;
        }
		
    	HttpRequest httpreq = (HttpRequest) msg;

    	// at the least don't allow web requests until running
    	// TODO later we may need to have a "Going Down" flag and filter new requests but allow existing 
    	if (!Hub.instance.isRunning()) {
    		this.context.sendInternalError();
    		return;
    	}
        
        // Handle a bad request.
        if (!httpreq.getDecoderResult().isSuccess()) {
        	this.context.sendRequestBad();
            return;
        }
    	
    	this.context.init(ctx, httpreq);
        
        Request req = this.context.getRequest();
        Response resp = this.context.getResponse();
        
        /*
		// to avoid lots of unused sessions
		if (req.pathEquals("/favicon.ico")) {
			this.context.sendNotFound();
			return;
		}
		*/
		
		// make sure we don't have a leftover task context
		OperationContext.clear();
		
		String origin = "http:" + NetUtil.formatIpAddress((InetSocketAddress)ctx.channel().remoteAddress());
		
		// TODO use X-Forwarded-For  if available, maybe a plug in approach to getting client's IP?
		
		SiteInfo site = WebModule.resolveSiteInfo(req);
		
		if (site == null) {
	    	if (Logger.isDebug())
	    		Logger.debug("Tenant not found for: " + req.getHeader("Host"));
	    	
        	this.context.sendForbidden();
            return;
		}
		
		this.context.setSite(site);
		
		// check into url re-routing
		String reroute = site.getWebsite().route(req, (SslHandler)ctx.channel().pipeline().get("ssl"));
		
		if (StringUtil.isNotEmpty(reroute)) {
	    	if (Logger.isDebug())
	    		Logger.debug("Routing the request to: " + reroute);
	    	
			this.context.getResponse().setStatus(HttpResponseStatus.FOUND);
			this.context.getResponse().setHeader("Location", reroute);
			this.context.send();
            return;
		}
		
		if (Logger.isTrace())
			Logger.trace("Site: " + site.getAlias());
		
		Session sess = null;
		boolean needVerify = false;
		
		if (site.getWebsite().isSharedSession()) {
			sess = site.getWebsite().getSharedSession();
			
	    	if (Logger.isDebug())
				Logger.info("Using shared session: " + sess.getId() + " on " + req.getPath() + " for " + origin + " agent: " + req.getHeader("User-Agent"));
		}
		else {
			Cookie sesscookie = req.getCookie("dcSessionId");
			
			if (sesscookie != null) {
				String v = sesscookie.value();
				int upos = v.lastIndexOf('_');
				
				if (upos != -1) {
					String sessionid = v.substring(0, upos);
					String accesscode = v.substring(upos + 1);
					
					// TODO if to fails then negative Trust to client
					sess = Hub.instance.getSessions().lookupAuth(sessionid, accesscode);
				}
			}
			
			if (sess == null) {
				Cookie catoken = req.getCookie("dcAuthToken");
	
				sess = Hub.instance.getSessions().create(origin, site.getTenant().getId(), site.getAlias(), (catoken != null) ? catoken.value() : null);
				
				Logger.info("Started new session: " + sess.getId() + " on " + req.getPath() + " for " + origin + " agent: " + req.getHeader("User-Agent"));
				
				// this is non-blocking, the remainder of this HTTP operation will run on guest even though 
				// token may be restored any moment and allow more access
				if (catoken != null) 
					needVerify = true;
				
				// TODO if ssl set client key on user context
				//req.getSecuritySession().getPeerCertificates();
				
				/* TODO adapter review
				sess.setAdatper(new HttpAdapter(this.context));
				*/
				
				Cookie sk = new DefaultCookie("dcSessionId", sess.getId() + "_" + sess.getKey());
				sk.setPath("/");
				sk.setHttpOnly(true);
				
				// TODO configure, but make Secure by default if using https 
				if (ctx.channel().pipeline().get("ssl") != null)
					sk.setSecure(true);
				
				resp.setCookie(sk);		 
			}
			else {
				String authupdate = sess.checkTokenUpdate();
				
				if (authupdate != null) {
					Cookie sk = new DefaultCookie("dcAuthToken", authupdate);
					sk.setPath("/");
					sk.setHttpOnly(true);
					
					resp.setCookie(sk);
				}
			}
		}
		
		this.context.setSessionId(sess.getId());
		
		sess.touch();
		
		OperationContextBuilder ctxb = sess.allocateContextBuilder()
				.withOrigin(origin);
		
		Cookie localek = site.getWebsite().resolveLocale(this.context, sess.getUser(), ctxb);
		
		if (localek != null) {
			localek.setPath("/");
			localek.setHttpOnly(true);
			
			// TODO configure, but make Secure by default if using https 
			if (ctx.channel().pipeline().get("ssl") != null)
				localek.setSecure(true);
			
			resp.setCookie(localek);		 
		}
		
		OperationContext tc = sess.useContext(ctxb);		
		//tc.setLocaleResource(site);
		
		// check errors now because we will clear errors after calling verify
		// the call to verify does not count as real errors in our own operations of loading pages
		if (tc.hasErrors()) {
			// TODO add code and message
			//resp.setHeader("X-dcResultCode", res.getCode() + "");
			//resp.setHeader("X-dcResultMesage", res.getMessage());
			this.context.sendNotFound();
		}

        // complete the verify before loading the page
		if (needVerify) {
			sess.verifySession(new FuncCallback<Message>() {					
				@Override
				public void callback() {
					if (this.hasErrors())
						tc.info("NOT Verified session: " + tc.getSessionId() + " on " + req.getPath() + " for " + origin);
					else
						tc.info("Verified session: " + tc.getSessionId() + " on " + req.getPath() + " for " + origin);
					
					tc.clearExitCode();
					
					ServerHandler.this.continueHttpRequest(tc);
				}
			});
		}
		else {
			ServerHandler.this.continueHttpRequest(tc);
		}
    }
    
    // after we get here we know we have a valid session and a valid user, even if that means that
    // the user session and user session requested has been replaced with Guest
	public void continueHttpRequest(OperationContext tc) {
		Request req = this.context.getRequest();
		String origin = tc.getOrigin();
		String sessid = tc.getSessionId();
		
        // --------------------------------------------
        // rpc request
        // --------------------------------------------
        
		// "rpc" is it's own built-in extension.  all requests to rpc are routed through
		// DivConq bus, if the request is valid
        if (req.pathEquals(ServerHandler.RPC_PATH)) {
    		if (req.getMethod() != HttpMethod.POST) {
                this.context.sendRequestBad();
                return;
    		}
    		
	    	if (Logger.isDebug())
	    		tc.debug("Web request for host: " + req.getHeader("Host") +  " url: " + req.getPath() + " by: " + origin + " session: " + sessid);

    		// RPC will automatically do the user verify before the request is sent
            ServerHandler.this.context.setDecoder(new HttpBodyRequestDecoder(4096 * 1024, new RpcHandler(ServerHandler.this.context)));
    		
        	return;
        }        
		
		tc.info("Web request for host: " + req.getHeader("Host") +  " url: " + req.getPath() + " by: " + origin + " session: " + sessid);
		
		// TODO if (Logger.isDebug()) {
		//	System.out.println("Operating locale " + tc.getWorkingLocale());
		//}
		
		/*
		System.out.println("sess proto: " + ((SslHandler)ctx.channel().pipeline().get("ssl")).engine().getSession().getProtocol());
		System.out.println("sess suite: " + ((SslHandler)ctx.channel().pipeline().get("ssl")).engine().getSession().getCipherSuite());
		*/
		
		try {			
	        // --------------------------------------------
	        // upload file request
	        // --------------------------------------------
	        
			// "upload" is it's own built-in extension.  
	        if ((req.getPath().getNameCount() == 3) && req.getPath().getName(0).equals(ServerHandler.UPLOAD_PATH)) {
				if (!Hub.instance.isRunning()) {		// only allow uploads when running
					this.context.sendRequestBad();
					return;
				}
				
				// currently only supporting POST/PUT of pure binary - though support for form uploads can be restored, see below
				// we cannot rely on content type being meaningful
				//if (!"application/octet-stream".equals(req.getContentType().getPrimary())) {
	            //    this.context.sendRequestBad();
	            //    return;
				//}

				// TODO add CORS support if needed
				
				if ((req.getMethod() != HttpMethod.PUT) && (req.getMethod() != HttpMethod.POST)) {
	                this.context.sendRequestBad();
	                return;
				}
				
				final String cid = req.getPath().getName(1);
				final String op = req.getPath().getName(2);
		        
		    	if (Logger.isDebug())
		    		Logger.debug("Initiating an upload block on " + cid + " for " + sessid);
				
		    	Session sess = Hub.instance.getSessions().lookup(sessid);
				
	    		if (sess == null) {
	                this.context.sendRequestBad();
	                return;
	    		}
		    	
				DataStreamChannel dsc = sess.getChannel(cid);
				
	    		if (dsc == null) {
	                this.context.sendRequestBad();
	                return;
	    		}
	    		
	    		dsc.setDriver(new IStreamDriver() {
	    			@Override
	    			public void cancel() {
	    				Logger.error("Transfer canceled on channel: " + cid);
						dsc.complete();
						ServerHandler.this.context.sendRequestBad();	// TODO headers?
	    			}
	    			
	    			@Override
	    			public void nextChunk() {
	    				Logger.debug("Continue on channel: " + cid);
						ServerHandler.this.context.sendRequestOk();
	    			}
	    			
	    			@Override
	    			public void message(StreamMessage msg) {
	    				if (msg.isFinal()) {
	    					Logger.debug("Final on channel: " + cid);
	    					dsc.complete();
							ServerHandler.this.context.sendRequestOk();
	    				}
	    			}
	    		});	
	    		
	            this.context.setDecoder(new IContentDecoder() {
	            	protected boolean completed = false;
	            	protected int seq = 0;
	            	
					@Override
					public void release() {
						// trust that http connection is closing or what ever needs to happen, we just need to deal with datastream
						
						Logger.debug("Releasing data stream decoder: " + cid + " completed: " + completed);
						
						// TODO this is not true, client may switch connections to continue upload, only a session timeout or datastream timeout count as true problems
						// if not done with request then something went wrong, kill data channel
						//if (!this.completed)
						//	dsc.abort();
					}
					
					@Override
					public void offer(HttpContent chunk) {
						boolean finalchunk = (chunk instanceof LastHttpContent); 
						
						//System.out.println("Chunk: " + finalchunk);
						
						ByteBuf buffer = chunk.content();
						
						if (!dsc.isClosed()) {
				            int size = buffer.readableBytes();
				
				            //System.out.println("Chunk size: " + size);
				            
				            if (Logger.isDebug())
				            	Logger.debug("Offered chunk on: " + cid + " size: " + size + " final: " + finalchunk);
				            
				            dsc.touch();	// TODO try to set progress on dsc
				            
				            // TODO set hint in netty as to where this buffer was handled and sent
				            
				            if (size > 0) {
				            	buffer.retain();		// we will be using a reference up during send
				            	
					    		StreamMessage b = new StreamMessage("Block", buffer);  
					    		b.setField("Sequence", this.seq);
					    		
					    		//System.out.println("Buffer ref cnt a: " + buffer.refCnt());

					    		OperationResult or = dsc.send(b);
					    		
					    		//System.out.println("Buffer ref cnt b: " + buffer.refCnt());
					    		
					    		// indicate we have read the buffer?
					    		buffer.readerIndex(buffer.writerIndex());
					    		
					    		if (or.hasErrors()) {
					    			dsc.close();
					    			return;
					    		}
					    		
					    		this.seq++;
				            }
				
				            // if last buffer of last block then mark the upload as completed
				    		if (finalchunk) {
				    			if ("Final".equals(op))  
				    				dsc.send(MessageUtil.streamFinal());
				    			else
				    				dsc.getDriver().nextChunk();   
							}
						}
						else {
				            if (Logger.isDebug())
				            	Logger.debug("Offered chunk on closed channel: " + cid);
						}
						
						// means this block is completed, not necessarily entire file uploaded
			    		if (finalchunk) 
				            this.completed = true;
					}
				});

				return;
			}
			
	        // --------------------------------------------
	        // download file request
	        // --------------------------------------------
			
			// "download" is it's own built-in extension.  
	        if ((req.getPath().getNameCount() == 2) && req.getPath().getName(0).equals(ServerHandler.DOWNLOAD_PATH)) {
				if (!Hub.instance.isRunning()) {		// only allow downloads when running
					this.context.sendRequestBad();
					return;
				}
				
	    		if (req.getMethod() != HttpMethod.GET) {
	                this.context.sendRequestBad();
	                return;
	    		}
				
				String cid = req.getPath().getName(1);
				
	            if (Logger.isDebug())
		    		Logger.debug("Initiating an download on " + cid + " for " + sessid);
				
		    	Session sess = Hub.instance.getSessions().lookup(sessid);
				
	    		if (sess == null) {
	                this.context.sendRequestBad();
	                return;
	    		}
				
				DataStreamChannel dsc = sess.getChannel(cid);
				
	    		if (dsc == null) {
	                this.context.sendRequestBad();
	                return;
	    		}
	    		
	    		dsc.setDriver(new IStreamDriver() {
	    			//protected long amt = 0;
	    			protected long seq = 0;
	    			
	    			@Override
	    			public void cancel() {
	    				Logger.debug("Transfer canceled on channel: " + cid);
						dsc.complete();
	    				ServerHandler.this.context.close();
	    			}
	    			
	    			@Override
	    			public void nextChunk() {
	    				// meaningless in download
	    			}
	    			
	    			@Override
	    			public void message(StreamMessage msg) {
	    				int seqnum = (int) msg.getFieldAsInteger("Sequence", 0);
	    				
	    				if (seqnum != this.seq) {
	    					this.error(1, "Bad sequence number: " + seqnum);
	    					return;
	    				}
	    				
    					if (msg.hasData()) {
    						if (Logger.isDebug())
    							Logger.debug("Transfer data: " + msg.getData().readableBytes());
    	    				
	    					//this.amt += msg.getData().readableBytes();
	    					HttpContent b = new DefaultHttpContent(Unpooled.copiedBuffer(msg.getData()));		// TODO not copied
	    					ServerHandler.this.context.sendDownload(b);
    					}
    					
    					this.seq++;

    					// TODO update progress
    					
    					if (msg.isFinal()) {
    						if (Logger.isDebug())
    							Logger.debug("Transfer completed: " + msg.getData().readableBytes());
    	    				
	    					ServerHandler.this.context.sendDownload(new DefaultLastHttpContent());
		    				ServerHandler.this.context.close();
	    					dsc.complete();
    					}
	    			}
	    			
	    			public void error(int code, String msg) {
						Logger.error("Transfer error - " + code + ": " + msg);
	    				
	    				dsc.send(MessageUtil.streamError(code, msg));
	    				ServerHandler.this.context.close();
	    			}
	    		});		

	    		// for some reason HyperSession is sending content. 
	    		this.context.setDecoder(new IContentDecoder() {					
					@Override
					public void release() {
					}
					
					@Override
					public void offer(HttpContent chunk) {
						if (!(chunk instanceof LastHttpContent))
							Logger.error("Unexplained and unwanted content during download: " + chunk);
					}
				});

	    		// tell the client that chunked content is coming
	    		this.context.sendDownloadHeaders(dsc.getPath() != null ? dsc.getPath().getFileName() : null, dsc.getMime());
	    		
	    		// TODO for now disable compression on downloads - later determine if we should enable for some cases
				this.context.getResponse().setHeader(HttpHeaders.Names.CONTENT_ENCODING, HttpHeaders.Values.IDENTITY);
	    		
				if (Logger.isDebug())
					Logger.debug("Singal Transfer Start - " + cid);
	    		
	    		// get the data flowing
	    		dsc.send(new StreamMessage("Start"));
	    		
				return;
			}
			
	        // --------------------------------------------
	        // status request
	        // --------------------------------------------
	        
			if ((req.getPath().getNameCount() == 1) && req.getPath().getName(0).equals(ServerHandler.STATUS_PATH)) {
				if (Hub.instance.getState() == HubState.Running)
	                this.context.sendRequestOk();
				else
					this.context.sendRequestBad();
                
                return;
	        }
			
	        // --------------------------------------------
	        // regular path/file request
	        // --------------------------------------------
	        
			if (Logger.isDebug())
				Logger.debug("Request posted to web domain: " + sessid);
			
			OperationResult res = new OperationResult();
			
			this.context.getSite().getWebsite().execute(this.context);
			
			// no errors starting page processing, return 
			if (res.hasErrors()) {
				this.context.getResponse().setHeader("X-dcResultCode", res.getCode() + "");
				this.context.getResponse().setHeader("X-dcResultMesage", res.getMessage());
				this.context.sendNotFound();
			}
		}
		catch (Exception x) {
			Logger.error("Request triggered exception: " + sessid + " - " + x);
			
			this.context.sendInternalError();
		}
    }

    // TODO this may not be a real threat but review it anyway
    // http://www.christian-schneider.net/CrossSiteWebSocketHijacking.html
    
    // https://www.owasp.org/index.php/HTML5_Security_Cheat_Sheet
    // https://www.owasp.org/index.php/Cross_Site_Scripting_Flaw
    // https://www.owasp.org/index.php/XSS_(Cross_Site_Scripting)_Prevention_Cheat_Sheet
    // https://code.google.com/p/owasp-java-encoder/source/browse/trunk/core/src/main/java/org/owasp/encoder/HTMLEncoder.java
    // http://kefirsf.org/kefirbb/
    // http://codex.wordpress.org/Validating_Sanitizing_and_Escaping_User_Data
    // http://excess-xss.com/
    // http://en.wikipedia.org/wiki/HTTP_cookie
    
    //  If you wish to support both HTTP requests and websockets in the one server, refer to the io.netty.example.http.websocketx.server.WebSocketServer example. To know once a handshake was done you can intercept the ChannelInboundHandler.userEventTriggered(ChannelHandlerContext, Object) and check if the event was of type WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE.
    
    // TODO CORS
    // also review
    // https://github.com/netty/netty/pull/2427/files
    // http://www.html5rocks.com/en/tutorials/file/xhr2/
    // http://www.html5rocks.com/en/tutorials/cors/
    // http://enable-cors.org/server.html
    
    // BREACH etc
    // https://community.qualys.com/blogs/securitylabs/2013/08/07/defending-against-the-breach-attack
    // https://en.wikipedia.org/wiki/BREACH_(security_exploit)
    
    /*
GET http://229097002.log.optimizely.com/event?a=229097002&d=229097002&y=false&x761570292=750582396&s231842852=gc&s231947722=search&s232031415=false&n=http%3A%2F%2Fwww.telerik.com%2Fdownload%2Ffiddler%2Ffirst-run&u=oeu1393506471224r0.17277055932208896&wxhr=true&t=1398696975163&f=702401691,760731745,761570292,766240693,834650096 HTTP/1.1
Host: 229097002.log.optimizely.com
Connection: keep-alive
User-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.131 Safari/537.36
Origin: http://www.telerik.com
Accept: * /*
Referer: http://www.telerik.com/download/fiddler/first-run
Accept-Encoding: gzip,deflate,sdch
Accept-Language: en-US,en;q=0.8
Cookie: fixed_external_20728634_bucket_map=; fixed_external_9718688_bucket_map=; fixed_external_138031368_bucket_map=; end_user_id=oeu1393506471224r0.17277055932208896; bucket_map=761570292%3A750582396



HTTP/1.1 200 OK
Access-Control-Allow-Credentials: true
Access-Control-Allow-Methods: POST, GET
Access-Control-Allow-Origin: http://www.telerik.com
Content-Type: application/json
Date: Mon, 28 Apr 2014 14:56:18 GMT
P3P: CP="IDC DSP COR CURa ADMa OUR IND PHY ONL COM STA"
Server: nginx/1.2.7
Content-Length: 2
Connection: keep-alive

{}



Chrome Web Socket Request:

GET /rpc HTTP/1.1
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Key: v8MIKFOPlaVtRK2C1iOJ4Q==
Host: localhost:9443
Sec-WebSocket-Origin: http://localhost:9443
Sec-WebSocket-Version: 13
x-DivConq-Mode: Private


Java API with Session Id

POST /rpc HTTP/1.1
Host: localhost
User-Agent: DivConq HyperAPI Client 1.0
Connection: keep-alive
Content-Encoding: UTF-8
Content-Type: application/json; charset=utf-8
Cookie: SessionId=00700_fa2h199tkc2e8i2cs4e8s9ujhh_EetvVV9EocXc; $Path="/"





	 * 
     */
	
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    	if (cause instanceof ReadTimeoutException) {
    		Logger.info("Web server closed channel, read timeout " + ctx.channel().localAddress() 
    				+ " from " + ctx.channel().remoteAddress()); // + " session " + this.context.getSession().getId());
    	}
    	else {
	        Logger.warn("Web server connection exception was " + cause);
	    	
	    	if (Logger.isDebug())
	    		Logger.debug("Web server connection exception was " + ctx.channel().localAddress() 
	    				+ " from " + ctx.channel().remoteAddress()); // + " session " + this.context.getSession().getId());
    	}
    	
    	ctx.close();
    	
    	WebContext wctx = this.context;
    	
    	if (wctx != null) 
    		wctx.closed();
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    	if (Logger.isDebug())
    		Logger.debug("Connection inactive was " + ctx.channel().localAddress() 
    				+ " from " + ctx.channel().remoteAddress());
    	
    	WebContext wctx = this.context;
    	
    	if (wctx != null) {
    		Logger.info("Web Server connection inactive: " + wctx.getSessionId());
    		wctx.closed();
    	}
    }
	
	/* TODO adapter review
	static public class HttpAdapter implements ISessionAdapter {
		protected volatile ListStruct msgs = new ListStruct();
	    protected HttpContext context = null; 
		
		public HttpAdapter(HttpContext ctx) {
			this.context = ctx;
		}
		
		@Override
		public void stop() {
	    	if (Logger.isDebug())
	    		Logger.debug("Web server session adapter got a STOP request.");
	    	
			this.context.close();
		}
		
		@Override
		public String getClientKey() {
			return this.context.getClientCert();
		}
		
		@Override
		public ListStruct popMessages() {
			ListStruct ret = this.msgs;
			this.msgs = new ListStruct();
			return ret;
		}
		
		@Override
		public void deliver(Message msg) {
			// keep no more than 100 messages - this is not a "reliable" approach, just basic comm help					
			while (this.msgs.getSize() > 99)
				this.msgs.removeItem(0);
			
			this.msgs.addItem(msg);
		}

		@Override
		public void UserChanged(UserContext user) {
			// we use the checkTokenUpdate approach instead 
		}
	}
	*/
}