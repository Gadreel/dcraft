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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.cert.X509Certificate;

import dcraft.bus.Message;
import dcraft.cms.feed.tool.FeedAdapter;
import dcraft.hub.TenantInfo;
import dcraft.hub.Hub;
import dcraft.hub.SiteInfo;
import dcraft.io.CacheFile;
import dcraft.lang.op.OperationContext;
import dcraft.locale.Tr;
import dcraft.log.Logger;
import dcraft.net.ssl.SslHandler;
import dcraft.session.Session;
import dcraft.struct.CompositeStruct;
import dcraft.struct.RecordStruct;
import dcraft.util.KeyUtil;
import dcraft.util.MimeUtil;
import dcraft.util.StringUtil;
import dcraft.web.WebModule;
import dcraft.web.mdx.Configuration;
import dcraft.web.mdx.ProcessContext;
import dcraft.web.mdx.plugin.GallerySection;
import dcraft.web.mdx.plugin.HtmlSection;
import dcraft.web.mdx.plugin.PairedMediaSection;
import dcraft.web.mdx.plugin.StandardSection;
import dcraft.xml.XElement;

@SuppressWarnings("deprecation")
public class WebContext implements IInnerContext {
	public static WebContext from(Channel channel) {
		WebContext ctx = new WebContext();
		ctx.chan = new WeakReference<Channel>(channel);
		return ctx;
	}
	
	protected WeakReference<Channel> chan = null;
	protected SiteInfo site = null;
    
    // used with HTTP only, not WS
	protected IContentDecoder decoder = null;    	
    protected Request request = null;
    protected Response response = null;
    protected RecordStruct altparams = null;
    protected String sessionid = null;
	protected boolean preview = false;

	protected Map<String, String> innerparams = new HashMap<String, String>();
    
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
    
	@Override
	public void setAltParams(RecordStruct v) {
		this.altparams = v;
	}
	
	@Override
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
	
	@Override
	public IWebMacro getMacro(String name) {
		return this.site.getWebsite().getMacro(name);
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
	
	public void putInternalParam(String name, String value) {
		this.innerparams.put(name, value);
	}
	
	public boolean hasInternalParam(String name) {
		return this.innerparams.containsKey(name);
	}
	
	public String getInternalParam(String name) {		
		return this.innerparams.get(name);
	}

	/* TODO search this
	public IInnerContext getInnerContext() {
		return this.innerctx;
	}
	  */
	
	  public String expandMacros(String value) {
		  if (StringUtil.isEmpty(value))
			  return null;
		  
		  boolean checkmatches = true;
		  
		  while (checkmatches) {
			  checkmatches = false;
			  Matcher m = WebModule.macropatten.matcher(value);
			  
			  while (m.find()) {
				  String grp = m.group();
				  
				  String macro = grp.substring(1, grp.length() - 1);
				  
				  String val = this.expandMacro(macro);
				  
				  // if any of these, then replace and check (expand) again 
				  if (val != null) {
					  value = value.replace(grp, val);
					  checkmatches = true;
				  }
			  }
		  }
		  
		  return value;
	  }
	  
	  // if the macro name is recognized then hide if no match, but otherwise don't
	  public String expandMacro(String macro) {
		  String[] parts = macro.split("\\|");
		  
		  // params on this tree
		  if ("param".equals(parts[0]) && (parts.length > 1)) {
			  String val = this.getExternalParam(parts[1]);
			  
			  return (val == null) ? "" : val;
		  }
		  else if ("ctx".equals(parts[0]) && (parts.length > 1)) {
			  String vname = parts[1];
			  
			  String val = this.getInternalParam(vname);
			  
			  // TODO review and remove paths, excessive
			  if ((val == null) && (vname.equals("SignInPath") || vname.equals("HomePath") || vname.equals("PortalPath")
					  || vname.equals("SiteTitle") || vname.equals("SiteAuthor") || vname.equals("SiteCopyright")))
			  {
				TenantInfo domain = this.getTenant();
				
				XElement domconfig = domain.getSettings();
				
				if (domconfig != null) {
					XElement web = domconfig.selectFirst("Web");
					
					if ((web != null) && (web.hasAttribute(vname))) 
						val = web.getRawAttribute(vname);
				}
			  }
			  
			  // if not a web setting, perhaps a user field?
			  if ((val == null) && (vname.equals("dcUserFullname"))) {
					val = OperationContext.get().getUserContext().getFullName();
			  }
			  
			  return (val == null) ? "" : val;
		  }
		  // definitions in the dictionary
		  else if ("tr".equals(parts[0])) {
			String val = null;
			  
			if ((parts.length > 1) && (StringUtil.isDataInteger(parts[1]))) 
				parts[1] = "_code_" + parts[1];
			  
			if (parts.length > 2) {
				String[] params = Arrays.copyOfRange(parts, 2, parts.length - 2);
				val = Tr.tr(parts[1], (Object) params);		// TODO test this
			}
			else if (parts.length > 1) {
				val = Tr.tr(parts[1]);		
			}
			  
			return (val == null) ? "" : val;
		  }
		  else {
			IWebMacro macroproc = this.getMacro(parts[0]);
			  
			if (macroproc != null) {
				String val = macroproc.process(this, parts);
				  
				return (val == null) ? "" : val;
			}
		  }
		  
		  return null;
	  }
    
    public Map<String, List<String>> getParameters() {
    	return this.getRequest().getParameters();
    }
	
	public String getHost() {
		return WebModule.resolveHost(this.getRequest());
	}

	public boolean isPreview() {
		return this.preview;
	}

	public boolean isDynamic() {
		return "dyn".equals(this.getExternalParam("_dcui"));
	}
	
	/*
	public Path findPath(String path) {
		return this.innerctx.getTenant().findFilePath(this.isPreview(), new CommonPath(path), null);
	}
	
	public Path findSectionPath(String section, String path) {
		return this.innerctx.getTenant().findSectionFile(this.isPreview(), section, path);
	}
	*/
	
	// string path is relative to tenants/[alias]/[path]
	public XElement getXmlResource(String section, String path) {
		CacheFile fpath = this.getSite().getWebsite().findSectionFile(section, path, this.isPreview());
		
		if (fpath == null)
			return null;
		
		return fpath.asXml();
	}
	
	// string path is relative to tenants/[alias]/[path]
	public CompositeStruct getJsonResource(String section, String path) {
		CacheFile fpath = this.getSite().getWebsite().findSectionFile(section, path, this.isPreview());
		
		if (fpath == null)
			return null;
		
		return fpath.asJson();
	}
	
	// string path is relative to tenants/[alias]/[path]
	public String getTextResource(String section, String path) {
		CacheFile fpath = this.getSite().getWebsite().findSectionFile(section, path, this.isPreview());
		
		if (fpath == null)
			return null;
		
		return fpath.asString();
	}
	
	public FeedAdapter getFeedAdapter(String alias, String path) {
		XElement feed = OperationContext.get().getTenant().getSettings().find("Feed");
		
		if (feed == null) 
			return null;
		
		// there are two special channels - Pages and Blocks
		for (XElement chan : feed.selectAll("Channel")) {
			String calias = chan.getAttribute("Alias");
			
			if (calias == null)
				calias = chan.getAttribute("Name");
			
			if (calias == null)
				continue;
			
			if (calias.equals(alias)) {
				// TODO support sites
				
				String innerpath = chan.getAttribute("Path", chan.getAttribute("InnerPath", "")) + path + ".dcf.xml";
				//String innerpath = chan.getAttribute("InnerPath", "") + path + ".dcf.xml";		// InnerPath or empty string
				
				CacheFile fpath = this.getSite().getWebsite().findSectionFile("feed", innerpath, this.isPreview());
				
				if (fpath != null) {
					FeedAdapter adapt = new FeedAdapter();
					adapt.init(calias, path, fpath);		
					return adapt; 
				}
				
				return null;
			}
		}
		
		return null;
	}
	
	public CompositeStruct getGalleryMeta(String path) {
		return this.getJsonResource("galleries", path + "/meta.json");
	}

	// TODO enhance how plugins are loaded
	public ProcessContext getMarkdownContext() {
		Configuration cfg = new Configuration()
			.setSafeMode(false)
			.registerPlugins(new PairedMediaSection(), new StandardSection(), new GallerySection(), new HtmlSection());
		
		return new ProcessContext(cfg, this);
	}
	
	public ProcessContext getSafeMarkdownContext() {
		Configuration cfg = new Configuration();
		
		return new ProcessContext(cfg, this);
	}

}
