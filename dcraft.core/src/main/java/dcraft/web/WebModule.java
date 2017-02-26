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
package dcraft.web;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import dcraft.hub.TenantInfo;
import dcraft.hub.Hub;
import dcraft.hub.HubEvents;
import dcraft.hub.SiteInfo;
import dcraft.log.Logger;
import dcraft.mod.ExtensionLoader;
import dcraft.mod.ModuleBase;
import dcraft.net.IpAddress;
import dcraft.struct.Struct;
import dcraft.util.MimeUtil;
import dcraft.util.StringUtil;
import dcraft.web.core.IWebExtension;
import dcraft.web.core.IOutputMacro;
import dcraft.web.core.Request;
import dcraft.web.core.ValuesMacro;
import dcraft.web.http.HttpContentCompressor;
import dcraft.web.http.ServerHandler;
import dcraft.web.http.SniHandler;
import dcraft.web.http.SslContextFactory;
import dcraft.xml.XElement;

//TODO integrate streaming - http://code.google.com/p/red5/
public class WebModule extends ModuleBase {
	// @[a-zA-Z0-9_\\-,:/]+@
	public static Pattern macropatten =  Pattern.compile("@\\S+?@", Pattern.MULTILINE);
	
	static public String resolveHost(Request req) {
		return WebModule.resolveHost(req.getHeader("Host"));
	}
	
	static public String resolveHost(String dname) {
		if (StringUtil.isNotEmpty(dname)) {
			int cpos = dname.indexOf(':');
			
			if (cpos > -1)
				dname = dname.substring(0, cpos);
		}
		
		if (StringUtil.isEmpty(dname) || IpAddress.isIpLiteral(dname))
			dname = "localhost";
		
		return dname;
	}

	/*			
			for (XElement httpconfig : this.config.selectAll("HttpListener")) {
		        boolean secure = "True".equals(httpconfig.getAttribute("Secure"));
		        int httpport = (int) StringUtil.parseInt(httpconfig.getAttribute("Port"), secure ? 443 : 80);
				
				// -------------------------------------------------
				// message port
				// -------------------------------------------------
		        ServerBootstrap b = new ServerBootstrap();
		        
		        // TODO consider using shared EventLoopGroup
		        // http://normanmaurer.me/presentations/2014-facebook-eng-netty/slides.html#25.0
		        
		        b.group(Hub.instance.getEventLoopGroup())
		         .channel(NioServerSocketChannel.class)
		         .option(ChannelOption.ALLOCATOR, Hub.instance.getBufferAllocator())
		         //.option(ChannelOption.SO_BACKLOG, 125)			// this is probably not needed but serves as note to research
		         .option(ChannelOption.TCP_NODELAY, true)			
		         .childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
		    	        ChannelPipeline pipeline = ch.pipeline();
		    	        
		    	        pipeline.addLast("timeout", new ReadTimeoutHandler(600));   // solves notebook sleep issue.  after 10 minutes close socket
		    	        
		    	        if (secure) {
		    	        	SniHandler ssl = new SniHandler(WebModule.this.siteman);
		    	        	
		    	            //SslHandler ssl = new SslHandler(WebModule.this.siteman.findSslContextFactory("root").getServerEngine()); 
		    	        	
		    	        	pipeline.addLast("ssl", ssl);
		    	        }
		    	        
		    	        //pipeline.addLast("codec-http", new HttpServerCodec());
		    	        //pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
		    	        
		    	        pipeline.addLast("decoder", new HttpRequestDecoder(4096,8192,262144));
		    	        pipeline.addLast("encoder", new HttpResponseEncoder());
		    	        
		    	        if (deflate)
		    	        	pipeline.addLast("deflater", new HttpContentCompressor());
		    	        
		    	        pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
		    	        
		    	        pipeline.addLast("handler", new ServerHandler(httpconfig, WebModule.this.siteman));
		    	        
		    	        if (Logger.isDebug())
		    	        	Logger.debug("New web connection from " + ch.remoteAddress().getAddress().toString());
					}        	 
		        });
    	        
    	        if (Logger.isDebug())
    	        	b.handler(new LoggingHandler("www", Logger.isTrace() ? LogLevel.TRACE : LogLevel.DEBUG));
	
		        try {
		        	// must wait here, both to keep the activelisteners listeners up to date
		        	// but also to make sure we don't release connectLock too soon
			        ChannelFuture bfuture = b.bind(httpport).sync();
			        
			        if (bfuture.isSuccess()) {
			        	Logger.info("Web Server listening - now listening for HTTP on TCP port " + httpport);
				        this.activelisteners.put(httpport, bfuture.channel());
			        }
			        else
			        	Logger.error("Web Server unable to bind: " + bfuture.cause());
		        }
		        catch (InterruptedException x) {
		        	Logger.error("Web Server interrupted while binding: " + x);
		        }
		        catch (Exception x) {
		        	Logger.error("Web Server errored while binding: " + x);
		        }
			}
		}
		finally {
			this.listenlock.unlock();
	*/
	
	static public TenantInfo resolveTenantInfo(Request req) {
		return Hub.instance.getTenants().resolveTenantInfo(WebModule.resolveHost(req));
	}
	
	static public TenantInfo resolveTenantInfo(String dname) {
		return Hub.instance.getTenants().resolveTenantInfo(WebModule.resolveHost(dname));
	}

	static public SiteInfo resolveSiteInfo(Request req) {
		String host = WebModule.resolveHost(req);
		
		TenantInfo domain = WebModule.resolveTenantInfo(host);
		
		if (domain != null)
			return domain.resolveSiteInfo(host);
		
		return null;
	}

	static public SiteInfo resolveSiteInfo(String host) {
		TenantInfo domain = WebModule.resolveTenantInfo(host);
		
		if (domain != null)
			return domain.resolveSiteInfo(host);
		
		return null;
	}
	
	protected ConcurrentHashMap<Integer, Channel> activelisteners = new ConcurrentHashMap<>();
    protected ReentrantLock listenlock = new ReentrantLock();
	
	protected String defaultTlsPort = "443";
	
	protected List<SslContextFactory> tls = new ArrayList<>(); 

	protected Map<String,IOutputMacro> macros = new HashMap<String,IOutputMacro>();
	protected ValuesMacro vmacros = new ValuesMacro();
	
	protected boolean sharedSession = false;
	protected boolean srcptstlcache = false;
	
	protected IWebExtension webExtension = null;
	
	public boolean isSharedSession() {
		return this.sharedSession;
	}
	
	public String getDefaultTlsPort() {
		return this.defaultTlsPort;
	}

	public boolean isScriptStyleCached() {
		return this.srcptstlcache;
	}
    
	@Override
	public void start() {
		// prepare the web site manager from settings in module config
		if (config != null) {
	    	for (XElement scel : config.selectAll("SslContext")) {
	    		SslContextFactory tls = new SslContextFactory();
	    		tls.init(config, scel);
	    		this.tls.add(tls);
	    	}
			
			this.defaultTlsPort = config.getAttribute("DefaultTlsPort", this.defaultTlsPort);
			
	        this.sharedSession = Struct.objectToBooleanOrFalse(config.getAttribute("SharedSession"));		// Want, Need, None
			
	        this.srcptstlcache = Struct.objectToBooleanOrFalse(config.getAttribute("ScriptStyleCache"));		
			
			XElement settings = config.find("ViewSettings");
			
			if (settings != null) {
				// ideally we would only load in Hub level settings, try to use sparingly
				MimeUtil.load(settings);
				
				for (XElement macros : settings.selectAll("Macro")) {
					String name = macros.getAttribute("Name");
					
					if (StringUtil.isEmpty(name))
						continue;
					
					String bname = macros.getAttribute("Class");
					
					if (StringUtil.isNotEmpty(bname)) {
						Class<?> cls = Hub.instance.getClass(bname);   
						
						if (cls != null) {
							Class<? extends IOutputMacro> tcls = cls.asSubclass(IOutputMacro.class);
							
							if (tcls != null)
								try {
									this.macros.put(name, tcls.newInstance());
								} 
								catch (Exception x) {
									x.printStackTrace();
								}
						} 
	
						// TODO log
						//System.out.println("unable to load class: " + cname);
					}
					
					String value = macros.getAttribute("Value");
					
					if (StringUtil.isNotEmpty(value)) 
						this.vmacros.add(name, value);
				}
			}
		}
		
		for (ExtensionLoader el : this.getLoader().getExtensions()) {
			if (el.getExtension() instanceof IWebExtension) {
				this.webExtension = (IWebExtension) el.getExtension();
				break;
			}
		}
		
		if (! Hub.instance.getResources().isForTesting())
			this.srcptstlcache = true;
		
		Hub.instance.subscribeToEvent(HubEvents.Connected, e -> this.goOnline());
		Hub.instance.subscribeToEvent(HubEvents.Booted, e -> this.goOffline());
	}
	
	public void goOnline() {
		this.listenlock.lock();
		
		try {
			// don't try if already in online mode
			if (this.activelisteners.size() > 0)
				return;
			
			// typically we should have an extension, unless we are supporting RPC only
			// TODO if (WebSiteManager.instance.getDefaultExtension() == null) 
			//	log.warn(0, "No default extension for web server");
	        boolean deflate = "True".equals(this.config.getAttribute("Deflate"));
			
			for (XElement httpconfig : this.config.selectAll("HttpListener")) {
		        boolean secure = "True".equals(httpconfig.getAttribute("Secure"));
		        int httpport = (int) StringUtil.parseInt(httpconfig.getAttribute("Port"), secure ? 443 : 80);
				
				// -------------------------------------------------
				// message port
				// -------------------------------------------------
		        ServerBootstrap b = new ServerBootstrap();
		        
		        // TODO consider using shared EventLoopGroup
		        // http://normanmaurer.me/presentations/2014-facebook-eng-netty/slides.html#25.0
		        
		        b.group(Hub.instance.getEventLoopGroup())
		         .channel(NioServerSocketChannel.class)
		         .option(ChannelOption.ALLOCATOR, Hub.instance.getBufferAllocator())
		         //.option(ChannelOption.SO_BACKLOG, 125)			// this is probably not needed but serves as note to research
		         .option(ChannelOption.TCP_NODELAY, true)			
		         .childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
		    	        ChannelPipeline pipeline = ch.pipeline();
		    	        
		    	        pipeline.addLast("timeout", new ReadTimeoutHandler(600));   // solves notebook sleep issue.  after 10 minutes close socket

		    	        if (secure) 
		    	        	pipeline.addLast("ssl", new SniHandler());
		    	        
		    	        pipeline.addLast("decoder", new HttpRequestDecoder(4096,8192,262144));
		    	        pipeline.addLast("encoder", new HttpResponseEncoder());
		    	        
		    	        if (deflate)
		    	        	pipeline.addLast("deflater", new HttpContentCompressor());
		    	        
		    	        pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
		    	        
		    	        pipeline.addLast("handler", new ServerHandler());
		    	        
		    	        if (Logger.isDebug())
		    	        	Logger.debug("New web connection from " + ch.remoteAddress().getAddress().toString());
					}        	 
		        });
    	        
    	        if (Logger.isDebug())
    	        	b.handler(new LoggingHandler("www", Logger.isTrace() ? LogLevel.TRACE : LogLevel.DEBUG));
	
		        try {
		        	// must wait here, both to keep the activelisteners listeners up to date
		        	// but also to make sure we don't release connectLock too soon
			        ChannelFuture bfuture = b.bind(httpport).sync();
			        
			        if (bfuture.isSuccess()) {
			        	Logger.info("Web Server listening - now listening for HTTP on TCP port " + httpport);
				        this.activelisteners.put(httpport, bfuture.channel());
			        }
			        else
			        	Logger.error("Web Server unable to bind: " + bfuture.cause());
		        }
		        catch (InterruptedException x) {
		        	Logger.error("Web Server interrupted while binding: " + x);
		        }
		        catch (Exception x) {
		        	Logger.error("Web Server errored while binding: " + x);
		        }
			}
		}
		finally {
			this.listenlock.unlock();
		}
	}
	
	public void goOffline() {
		this.listenlock.lock();
		
		try {
			// we don't want to listen anymore
			for (final Integer port : this.activelisteners.keySet()) {
				// tear down message port
				Channel ch = this.activelisteners.remove(port);
	
		        try {
		        	// must wait here, both to keep the activelisteners listeners up to date
		        	// but also to make sure we don't release connectLock too soon
		        	ChannelFuture bfuture = ch.close().sync();
			        
			        if (bfuture.isSuccess()) 
			        	Logger.info("Web Server unbound");
			        else
			        	Logger.error("Web Server unable to unbind: " + bfuture.cause());
		        }
		        catch (InterruptedException x) {
		        	Logger.error("Web Server unable to unbind: " + x);
		        }
			}
		}
		finally {
			this.listenlock.unlock();
		}
	}

	@Override
	public void stop() {
		this.goOffline();
	}

	// only call this with normalized hostnames 
	public SslContextFactory findSslContextFactory(String hostname) {
		SiteInfo di = WebModule.resolveSiteInfo(hostname);
		
		if (di != null) {
			SslContextFactory scf = di.getSecureContextFactory(hostname);
			
			if (scf != null)
				return scf;
		}
		
		for (SslContextFactory cf : this.tls)
			if (cf.keynameMatch(hostname))
				return cf;
		
		if (this.tls.size() > 0)
			return this.tls.get(0);
		
		return null;
	}

	public IOutputMacro getMacro(String name) {
		if (this.vmacros.hasKey(name))
			return this.vmacros;
		
		return this.macros.get(name);
	}
	
	public IWebExtension getWebExtension() {
		return this.webExtension;
	}
}
