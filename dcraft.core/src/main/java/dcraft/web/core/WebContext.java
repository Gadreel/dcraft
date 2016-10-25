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
package dcraft.web.core;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.stream.ChunkedInput;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.cert.X509Certificate;

import dcraft.bus.Message;
import dcraft.cms.feed.core.FeedAdapter;
import dcraft.hub.TenantInfo;
import dcraft.hub.Hub;
import dcraft.hub.SiteInfo;
import dcraft.log.Logger;
import dcraft.net.ssl.SslHandler;
import dcraft.session.Session;
import dcraft.struct.CompositeStruct;
import dcraft.struct.RecordStruct;
import dcraft.util.KeyUtil;
import dcraft.util.MimeUtil;
import dcraft.web.WebModule;
import dcraft.xml.XElement;

@SuppressWarnings("deprecation")
public class WebContext extends BaseContext {
	public static WebContext forChannel(Channel channel) {
		WebContext ctx = new WebContext();
		ctx.chan = new WeakReference<Channel>(channel);
		return ctx;
	}
	
	protected WeakReference<Channel> chan = null;
	protected SiteInfo site = null;						// TODO deprecate this, use op ctx
    
    // used with HTTP only, not WS
	protected IContentDecoder decoder = null;    	
    protected Request request = null;
    protected Response response = null;
    protected RecordStruct altparams = null;		// TODO remove once legacy email is removed, see DGA or such
    protected String sessionid = null;					// TODO deprecate this, use op ctx
    
    public Request getRequest() {
		return this.request;
	}
    
    public Response getResponse() {
		return this.response;
	}
    
    public void setSessionId(String v) {
		this.sessionid = v;
	}
    
    public String getSessionId() {
		return this.sessionid;
	}
    
	public void setAltParams(RecordStruct v) {
		this.altparams = v;
	}
	
	public RecordStruct getAltParams() {
		return this.altparams;
	}
	
	@Override
	public TenantInfo getTenant() {
		if (this.site != null)
			return this.site.getTenant();
		
		return null;
	}
	
	@Override
	public SiteInfo getSite() {
		return this.site;
	}
	
	public void setSite(SiteInfo v) {
		this.site = v;
	}
	
	public XElement getConfig() {
		return this.site.getWebsite().getWebConfig();
	}
	
	public Channel getChannel() {
		if (this.chan != null)
			return this.chan.get();
		
		return null;
	}
	
	public void setChannel(Channel chan) {
		this.chan = new WeakReference<Channel>(chan);
	}
    
    public void setDecoder(IContentDecoder v) {
		this.decoder = v;
	}
    
	public void init(ChannelHandlerContext ctx, HttpRequest req) {
		IContentDecoder d = this.decoder;
		
		if (d != null) {
			d.release();  
			this.decoder = null;
		}
		
        this.request = new Request();
        this.request.load(ctx, req);
        
        this.response = new Response();
        this.response.load(ctx, req);
    	
		Cookie ck = this.request.getCookie("dcPreview");

		if (ck != null)
			this.preview = "true".equals(ck.value().toLowerCase());
	}
	
	public Session getSession() {
		return Hub.instance.getSessions().lookup(this.sessionid);
	}
	
	public void offerContent(HttpContent v) {
		// don't hold on to sessions directly, so we DO need to look this up
		Session sess = this.getSession();

		if (sess != null)
			sess.touch();		// TODO make sure we are in proper context when this is called - as well as any calls to getSession above - then remove this extra reference to current session
		
		IContentDecoder d = this.decoder;
		
		if (d != null) 
			d.offer(v);
		
		if (v instanceof LastHttpContent) 
			this.decoder = null;
		
		// TODO in netty 5 alpha 1 this is getting called after each normal http get and is forcing us to close everytime
		// we may want to review later
		
		//else
		//	this.sendBadRequest();
	}
	
	public void close() {
		try {
			Channel tchan = this.getChannel();
			
			if (tchan != null)
				tchan.close().await(2000);
		} 
		catch (InterruptedException x) {
			// ignore 
		}
	}
	
	public void sendNotFound() {
    	if (Logger.isDebug())
    		Logger.debug("Web server respond with Not Found");
    	
		if (this.response != null) {
			this.response.setStatus(HttpResponseStatus.NOT_FOUND);
			this.send();
		}
	}
	
	public void sendForbidden() {
    	if (Logger.isDebug())
    		Logger.debug("Web server respond with Forbidden");
    	
		if (this.response != null) {
			this.response.setStatus(HttpResponseStatus.FORBIDDEN);
			this.response.setKeepAlive(false);
			this.send();
		}
	}
	
	public void sendInternalError() {
    	if (Logger.isDebug())
    		Logger.debug("Web server respond with Internal Server Error");
    	
		if (this.response != null) {
			this.response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
			this.response.setKeepAlive(false);
			this.send();
		}
	}
	
	public void sendRequestBad() {
    	if (Logger.isDebug())
    		Logger.debug("Web server respond with Request Bad");
    	
		if (this.response != null) {
			this.response.setStatus(HttpResponseStatus.BAD_REQUEST);
			this.response.setKeepAlive(false);
			this.send();
		}
	}
	
	public void sendRequestOkClose() {
		if (this.response != null) {
			this.response.setStatus(HttpResponseStatus.OK);
			this.response.setKeepAlive(false);
			this.send();
		}
	}
	
	public void sendRequestOk() {
		if (this.response != null) {
			this.response.setStatus(HttpResponseStatus.OK);
			//this.response.setKeepAlive(true);
			this.send();
		}
	}
	
	public void send() {
		//if ((this.chan != null) && this.chan.isWritable() && (this.response != null)) 
		Channel tchan = this.getChannel();
		
		if ((tchan != null) && (this.response != null)) 
			this.response.write(tchan);
	}
	
	public void sendStart(int contentLength) {
		Channel tchan = this.getChannel();
		
		if ((tchan != null) && (this.response != null)) 
			this.response.writeStart(tchan, contentLength);
	}

	public void send(ByteBuf content) {
		Channel tchan = this.getChannel();
		
		if (tchan != null)
			tchan.write(new DefaultHttpContent(content));
	}

	public void send(ChunkedInput<HttpContent> content) {
		Channel tchan = this.getChannel();
		
		if (tchan != null)
			tchan.write(content);
		
		/* TODO we don't need this?
		.addListener(new GenericFutureListener<Future<? super Void>>() {
				@Override
				public void operationComplete(Future<? super Void> future)
						throws Exception {
					//System.out.println("Sending an end");
					//HttpContext.this.response.writeEnd(HttpContext.this.chan);
				}
			});
			*/
	}
	
	public void sendEnd() {
		Channel tchan = this.getChannel();
		
		if ((tchan != null) && (this.response != null)) 
			this.response.writeEnd(tchan);
	}
	
	public void sendChunked() {
		//if ((this.chan != null) && this.chan.isWritable() && (this.response != null)) 
		Channel tchan = this.getChannel();
		
		if ((tchan != null) && (this.response != null)) 
			this.response.writeChunked(tchan);
	}
	
	public void sendDownloadHeaders(String name, String mime) {
		//if ((this.chan != null) && this.chan.isWritable() && (this.response != null)) 
		Channel tchan = this.getChannel();
		
		if ((tchan != null) && (this.response != null)) 
			this.response.writeDownloadHeaders(tchan, name, mime);
	}
	
	// should already have TaskContext if needed (SERVICES and HELLO do not need task context)
	public void send(Message m) {
		try {
			//if ((this.chan != null) && this.chan.isWritable()) {
			Channel tchan = this.getChannel();
			
			if (tchan != null) {
				// include the version hash for the current deployed files
				//m.setField("DeployVersion", this.siteman.getVersion());
				
				// we are always using UTF 8, charset is required with any "text/*" mime type that is not ANSI text 
				this.response.setHeader(Names.CONTENT_TYPE, MimeUtil.getMimeType("json") + "; charset=utf-8");
				
				// TODO enable CORS - http://www.html5rocks.com/en/tutorials/file/xhr2/
				// TODO possibly config to be more secure for some users - see CORS handler in Netty 
				this.response.setHeader(Names.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
				
				this.response.setBody(m);
				this.response.write(tchan);
			}
		}
		catch (Exception x) {
		}
	}
	
	public void send(HttpContent chunk) {
		try {
			Channel tchan = this.getChannel();
			
			if (tchan != null) 
				tchan.writeAndFlush(chunk);   // we do not need to sync - HTTP is one request, one response.  we would not pile messages on this channel
		}
		catch (Exception x) {
		}
	}
	
	public void sendDownload(HttpContent chunk) {
		try {
			Channel tchan = this.getChannel();
			
			if (tchan != null)
				tchan.writeAndFlush(chunk).sync();    // for downloads we do need sync so we don't overwhelm client
			
			// TODO see if we can use something other than sync - http://normanmaurer.me/presentations/2014-facebook-eng-netty/slides.html#10.0
		}
		catch (Exception x) {
		}
	}

	public void closed() {
		IContentDecoder d = this.decoder;
		
		if (d != null) {
			d.release();
			this.decoder = null;
		}
	}
	
	// get the thumbprint of client cert, if available
	public String getClientCert() {
		Channel tchan = this.getChannel();
		
		if (tchan != null) {
			SslHandler sslhandler = (SslHandler) tchan.pipeline().get("ssl");
			
			if (sslhandler != null) {
				try {
					X509Certificate[] list = sslhandler.engine().getSession().getPeerCertificateChain();
					
					if (list.length > 0) {
						String thumbprint = KeyUtil.getCertThumbprint(list[0]); 
						
						//System.out.println("got thumbprint: " + thumbprint);
						
						return thumbprint;
					}
				}
				catch (SSLPeerUnverifiedException x) {
					// ignore, at this point we don't enforce peer certs
				}
			}
		}
		
		return null;
	}	
	
	// ------------------------
	
	@Override
	public boolean hasExternalParam(String name) {
		return (this.getRequest().getParameter(name) != null);
	}
	
	@Override
	public String getExternalParam(String name) {
		return this.getExternalParam(name, null);
	}

	public String getExternalParam(String name, String defaultvalue) {
		try {
			String ret = this.getRequest().getParameter(name);

			if (ret == null)
				ret = defaultvalue;

			return ret;
		} 
		catch (Exception x) {
		}

		return null;
	}
	
	/* TODO search this
	public IInnerContext getInnerContext() {
		return this.innerctx;
	}
	  */
    
    public Map<String, List<String>> getParameters() {
    	return this.getRequest().getParameters();
    }
	
	public String getHost() {
		return WebModule.resolveHost(this.getRequest());
	}

	public boolean isDynamic() {
		return "dyn".equals(this.getExternalParam("_dcui"));
	}
	
	// string path is relative to tenants/[alias]/[path]
	public XElement getXmlResource(String section, String path) {
		return this.site.getXmlResource(section, path, this.isPreview());
	}
	
	// string path is relative to tenants/[alias]/[section]/[path]
	public CompositeStruct getJsonResource(String section, String path) {
		return this.site.getJsonResource(section, path, this.isPreview());
	}
	
	// string path is relative to tenants/[alias]/[section]/[path]
	public String getTextResource(String section, String path) {
		return this.site.getTextResource(section, path, this.isPreview());
	}
	
	public FeedAdapter getFeedAdapter(String alias, String path) {
		return this.site.getFeedAdapter(alias, path, this.isPreview());
	}
	
	public CompositeStruct getGalleryMeta(String path) {
		return this.site.getGalleryMeta(path, this.isPreview());
	}
	
	public void forEachGalleryShowImage(String path, String show, GalleryImageConsumer consumer) {
		this.site.forEachGalleryShowImage(path, show, this.isPreview(), consumer);
	}
}
