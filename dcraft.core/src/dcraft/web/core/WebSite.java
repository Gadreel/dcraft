package dcraft.web.core;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import dcraft.filestore.CommonPath;
import dcraft.hub.HubPackage;
import dcraft.hub.SiteInfo;
import dcraft.hub.Hub;
import dcraft.io.CacheFile;
import dcraft.lang.op.FuncResult;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.OperationContextBuilder;
import dcraft.lang.op.UserContext;
import dcraft.locale.LocaleDefinition;
import dcraft.log.Logger;
import dcraft.mod.IExtension;
import dcraft.net.ssl.SslHandler;
import dcraft.session.Session;
import dcraft.struct.Struct;
import dcraft.util.StringUtil;
import dcraft.web.WebModule;
import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIUtil;
import dcraft.web.ui.adapter.SsiOutputAdapter;
import dcraft.web.ui.adapter.GasOutputAdapter;
import dcraft.web.ui.adapter.StaticOutputAdapter;
import dcraft.web.ui.adapter.DynamicOutputAdapter;
import dcraft.xml.XElement;
import dcraft.xml.XNode;
import dcraft.xml.XmlReader;

public class WebSite {
	static public WebSite from(XElement settings, SiteInfo site) {
		WebSite web = new WebSite();
		web.site = new WeakReference<SiteInfo>(site);
		web.init(settings);
		return web;
	}
	
	protected WeakReference<SiteInfo> site = null;
	
	protected HtmlMode htmlmode = HtmlMode.Dynamic;
	protected CommonPath homepath = null;
	protected XElement webconfig = null;

	protected Map<String, Class<? extends XElement>> tagmap = new HashMap<String, Class<? extends XElement>>();
	
	protected String[] specialExtensions = new String[] { ".html", ".gas" };	
	
	protected List<HubPackage> packagelist = null;
	
	protected boolean sharedsessenabled = false;		// TODO configure for this
	protected Session sharedsess = null;
	
	protected boolean oldweb = false;
	
	public HtmlMode getHtmlMode() {
		return this.htmlmode;
	}
	
	public CommonPath getHomePath() {
		return this.homepath;
	}
	
	public XElement getWebConfig() {
		return this.webconfig;
	}
	
	public List<HubPackage> getPackagelist() {
		return this.packagelist;
	}

	// TODO - not working as intended, may be best to fold into caller
	public void translatePath(WebContext ctx) {
		CommonPath path = ctx.getRequest().getPath();
		
		/* TODO also try
	
			//new CommonPath("/index.html");
			// 				this.homepath = new CommonPath("/Home");		
		 * 
		 */
		
		if (path.isRoot()) 
			ctx.getRequest().setPath(this.getHomePath());
	}
	
	public CommonPath getNotFound() {
		if (this.homepath != null)
			return this.homepath;

		return new CommonPath("/dcw/notfound.html");
	}
	
	public void init(XElement settings) {
		this.webconfig = settings;
		this.tagmap = UIUtil.allocateCoreMap();
		
		// collect a list of the packages names enabled for this domain
		HashSet<String> packagenames = new HashSet<>();
		
		if (Logger.isDebug())
			Logger.debug("Checking web domain settings from domain: " + this.site.get().getTenant().getAlias() + 
				" : " + this.site.get().getAlias() + " - " + settings);
		
		if (settings != null) {
			// UI = app or customer uses builder
			// UI = basic is just 'index.html' approach
			if (settings.hasAttribute("UI")) {
				this.oldweb = "custom".equals(settings.getAttribute("UI").toLowerCase());
				
				if (this.oldweb) {
					// TODO a setting or such to get the ServerHandler something to do if old domain
					// vs new domain - two modes of web server
				}
			}
			
			if (settings.hasAttribute("HomePath")) 
				this.homepath = new CommonPath(settings.getAttribute("HomePath"));
			
			if (settings.hasAttribute("HtmlMode")) {
				try {
					this.htmlmode = HtmlMode.valueOf(settings.getAttribute("HtmlMode"));
				}
				catch(Exception x) {
					OperationContext.get().error("Unknown HTML Mode: " + settings.getAttribute("HtmlMode"));
				}
			}

			/* TODO make sure these ideas still get through
			else if (this.appFramework == Framework.basic)
				this.homepath = new CommonPath("/index.html");		
			else if ((this.appFramework == Framework.custom) || (this.appFramework == Framework.dc))
				this.homepath = new CommonPath("/Home");	
				*/	
			
			// collect packages - if not in the domain, then go look in the packages 
			for (XElement pel :  settings.selectAll("Package")) 
				packagenames.add(pel.getAttribute("Name"));
		}
		
		// build PACKAGES
		
		// TODO may wish to support more than one web server someday...this only works with the default server
		WebModule mod = (WebModule) Hub.instance.getModule("Web");
		
		if (mod != null) {
			IExtension ext = mod.getWebExtension();
			
			if (ext != null) {
				XElement webextconfig = ext.getLoader().getConfig();
				
				// collect a list of the packages names enabled for this domain
				
				// add to the package name list all the packages turned on for entire web service
				if (webextconfig != null) {
					for (XElement pel :  webextconfig.selectAll("Package"))
						packagenames.add(pel.hasAttribute("Id") ? pel.getAttribute("Id") :pel.getAttribute("Name"));
				}
			}
		}
		
		this.packagelist = Hub.instance.getResources().getPackages().buildLookupList(packagenames);
		
		if (Logger.isDebug())
			Logger.debug("Package list: " + this.site.get().getTenant().getAlias() + " : " + this.site.get().getAlias() + " - " + this.packagelist.size());
	}

	public Cookie resolveLocale(WebContext context, UserContext usr, OperationContextBuilder ctxb) {
		Map<String, LocaleDefinition> locales = this.site.get().getLocales();

		LocaleDefinition locale = null;

		// see if the path indicates a language
		CommonPath path = context.getRequest().getPath();
		
		if (path.getNameCount() > 0)  {
			String lvalue = path.getName(0);
			
			locale = locales.get(lvalue);
			
			// extract the language from the path
			if (locale != null)
				context.getRequest().setPath(path.subpath(1));
		}

		// but respect the cookie if it matches something though
		Cookie langcookie = context.getRequest().getCookie("dcLang");
		
		if (locale == null) {
			if (langcookie != null) {
				String lvalue = langcookie.value();
				
				// if everything checks out set the op locale and done
				if (locales.containsKey(lvalue)) {
					ctxb.withOperatingLocale(lvalue);
					return null;
				}
				
				locale = this.site.get().getLocaleDefinition(lvalue);
				
				// use language if variant - still ok and done
				if (locale.hasVariant()) {
					if (locales.containsKey(locale.getLanguage())) {
						ctxb.withOperatingLocale(lvalue);		// keep the variant part, it may be used in places on site - supporting a lang implicitly allows all variants
						return null;
					}
				}
				
				// otherwise ignore the cookie, will replace it
			}
		}
		
		// see if the domain is set for a specific language
		if (locale == null) {
			String domain = context.getRequest().getHeader("Host");
			
			if (domain.indexOf(':') > -1)
				domain = domain.substring(0, domain.indexOf(':'));
			
			locale = this.site.get().getSiteLocales().get(domain);
		}
		
		// see if the user has a preference
		if (locale == null) {
			String lvalue = usr.getLocale();
			
			if (StringUtil.isNotEmpty(lvalue)) 
				locale = locales.get(lvalue);
		}
		
		// if we find any locale at all then to see if it is the default
		// if not use it, else use the default
		if ((locale != null) && !locale.equals(this.site.get().getDefaultLocaleDefinition())) {
			ctxb.withOperatingLocale(locale.getName());
			return new DefaultCookie("dcLang", locale.getName());
		}
		
		// clear the cookie if we are to use default locale
		if (langcookie != null) 
			return new DefaultCookie("dcLang", "");
		
		// we are using default locale, nothing more to do
		return null;
	}

	public boolean isSharedSession() {
		return this.sharedsessenabled;
	}
	
	public Session getSharedSession() {
		if (this.sharedsess == null) {
			this.sharedsess = Hub.instance.getSessions().create("http:", this.site.get().getTenant().getId(), this.site.get().getAlias(), null);
			this.sharedsess.setKeep(true);		// don't remove, stays until server shuts down
			
			Logger.info("Started new shared session: " + this.sharedsess.getId());
		}
		
		return this.sharedsess;
	}
	
	public IWebMacro getMacro(String name) {
		// TODO site or domain level support for macros
		
		WebModule mod = (WebModule) Hub.instance.getModule("Web");
		
		if (mod != null)
			return mod.getMacro(name);
		
		return null;
	}
	
	public String route(Request req, SslHandler ssl) {
		String host = req.getHeader("Host");
		String port = "";
		
		if (host.contains(":")) {
			int pos = host.indexOf(':');
			port = host.substring(pos);
			host = host.substring(0, pos);
		}
		
		XElement web = this.webconfig.selectFirst("Web");
		
		if (web == null)
			return null;
		
		String defPort = ((WebModule) Hub.instance.getModule("Web")).getDefaultTlsPort();
		
		String orguri = req.getOriginalPathUri();
		
		for (XElement route : web.selectAll("Route")) {
			if (host.equals(route.getAttribute("Name"))) {
				if (route.hasAttribute("RedirectPath"))
					return route.getAttribute("RedirectPath");
				
				if (!route.hasAttribute("ForceTls") && !route.hasAttribute("RedirectName"))
					continue;
				
				boolean tlsForce = Struct.objectToBoolean(route.getAttribute("ForceTls", "False"));				
				String rname = route.getAttribute("RedirectName");
				
				boolean changeTls = ((ssl == null) && tlsForce);
				
				if (StringUtil.isNotEmpty(rname) || changeTls) {
					String path = ((ssl != null) || tlsForce) ? "https://" : "http://";
					
					path += StringUtil.isNotEmpty(rname) ? rname : host;
					
					// if forcing a switch, use another port
					path += changeTls ? ":" + route.getAttribute("TlsPort", defPort) : port;
					
					return path + req.getOriginalPath(); 
				}
			}
			
			if (orguri.equals(route.getAttribute("Path"))) {
				if (route.hasAttribute("RedirectPath"))
					return route.getAttribute("RedirectPath");
			}
		}
		
		if ((ssl == null) && Struct.objectToBoolean(web.getAttribute("ForceTls", "False"))) 
			return "https://" + host + ":" + web.getAttribute("TlsPort", defPort) + req.getOriginalPath(); 
		
		return null;
	}
	
	// something changed in the www folder
	// force compiled content to reload from file system 
	public void dynNotify() {
	}
	
	public void execute(WebContext ctx) {
		if (Logger.isDebug())
			Logger.debug("Site: " + (site != null ? this.site.get().getAlias() : "[missing]"));
		
		if (Logger.isDebug())
			Logger.debug("Translating path: " + ctx.getRequest().getPath());
		
		this.translatePath(ctx);
		
		CommonPath path = ctx.getRequest().getPath();
	
		if (Logger.isDebug())
			Logger.debug("Process path: " + path);
		
		// translate above should take us home for root 
		if (path.isRoot()) { 
			OperationContext.get().errorTr(150001);
			return;
		}
		
		IOutputAdapter output = this.findFile(ctx);

		if (OperationContext.get().hasErrors() || (output == null)) {
			OperationContext.get().errorTr(150001);			
			return;
		}
		
		if (Logger.isDebug())
			Logger.debug("Executing adapter: " + output.getClass().getName());
		
		try {
			output.execute(ctx);
		} 
		catch (Exception x) {
			Logger.error("Unable to process web file: " + x);
			
			x.printStackTrace();
			
			ctx.sendNotFound();
		}
	}

	/*
	public IOutputAdapter findFile(CommonPath path, boolean isPreview) {
		return this.domain.findFile(this, path, isPreview);
	}
	*/

	/**
	 * File paths come in as /dcf/index.html but really they are in -  
	 * 
	 * Tenant Path Map:
	 * 		"/dcf/index.html"
	 * 
	 * Tenant Phantom Files:                           			(draft/preview mode files)
	 * 		./public/tenants/[domain id]/www-preview/dcf/index.html
	 * 
	 * Tenant Override Files:
	 * 		./public/tenants/[domain id]/www/dcf/index.html
	 * 
	 * Package Files:
	 * 		./packages/[package id]/www/dcf/index.html
	 * 
	 * Example:
	 * - ./private/tenants/filetransferconsulting/www-preview/dcf/index.html
	 * - ./private/tenants/filetransferconsulting/www/dcf/index.html
	 * - ./public/tenants/filetransferconsulting/www-preview/dcf/index.html
	 * - ./public/tenants/filetransferconsulting/www/dcf/index.html
	 * - ./packages/zCustomPublic/www/dcf/index.html
	 * - ./packages/dc/dcFilePublic/www/dcf/index.html
	 * - ./packages/dcWeb/www/dcf/index.html
	 * 
	 * 
	 * @param ctx
	 * @return an adapter that can execute to generate web response
	 */	
	
	// TODO if the Path selected is different than the path that goes in, we should update the context
	public IOutputAdapter findFile(WebContext ctx) {		
		return this.findFile(ctx.getRequest().getPath(), ctx.isPreview());
	}
	
	public IOutputAdapter findFile(CommonPath path, boolean isPreview) {
		// =====================================================
		//  if request has an extension do specific file lookup
		// =====================================================
		
		if (Logger.isDebug())
			Logger.debug("find file before ext check: " + path + " - " + isPreview);
		
		// if we have an extension then we don't have to do the search below
		// never go up a level past a file (or folder) with an extension
		if (path.hasFileExtension()) {
			CacheFile wpath = this.findFilePath(path, isPreview);
			
			if (wpath != null) 
				return this.pathToAdapter(isPreview, path, wpath);
			
			// TODO not found file!!
			OperationContext.get().errorTr(150007);		
			return null;
		}
		
		// =====================================================
		//  if request does not have an extension look for files
		//  that might match this path or one of its parents
		//  using the special extensions
		// =====================================================
		
		if (Logger.isDebug())
			Logger.debug("find dyn file: " + path + " - " + isPreview);
		
		CacheFile wpath = this.findFilePath(path, isPreview);
		
		if (wpath == null) {
			OperationContext.get().errorTr(150007);		
			return null;
		}			
		
		if (Logger.isDebug())
			Logger.debug("find file path: " + wpath + " - " + path + " - " + isPreview);
		
		return this.pathToAdapter(isPreview, path, wpath);
	}

	public IOutputAdapter pathToAdapter(boolean isPreview, CommonPath path, CacheFile cfile) {
		IOutputAdapter ioa = null;
		
		String filename = cfile.getPath();
		
		HtmlMode hmode = this.getHtmlMode();
		
		// /galleries and /files always processed by static output because these areas can be altered by Editors and Admins
		// only developers and sysadmins should make changes that can run server scripts
		if ((path.getNameCount() > 1) && ("galleries".equals(path.getName(0)) || "files".equals(path.getName(0)))) {
			ioa = new StaticOutputAdapter();
		}
		else if (filename.endsWith(".html")) {
			if (hmode == HtmlMode.Ssi) 
				ioa = new SsiOutputAdapter();
			else if ((hmode == HtmlMode.Dynamic) || (hmode == HtmlMode.Strict))
				ioa = new DynamicOutputAdapter();
			else if (hmode == HtmlMode.Static)
				ioa = new StaticOutputAdapter();
		}		
		else if (filename.endsWith(".gas")) {
			ioa = new GasOutputAdapter();		
		}
		else {
			ioa = new StaticOutputAdapter();
		}
		
		ioa.init(this.site.get(), cfile, path, isPreview);
		
		return ioa;
	}
	
	public CacheFile findFilePath(CommonPath path, boolean isPreview) {
		// figure out which section we are looking in
		String sect = "www";
		
		if ("files".equals(path.getName(0)) || "galleries".equals(path.getName(0))) {
			sect = path.getName(0);
			path = path.subpath(1);
		}
		
		if (Logger.isDebug())
			Logger.debug("find file path: " + path + " in " + sect);
		
		// =====================================================
		//  if request has an extension do specific file lookup
		// =====================================================
		
		// if we have an extension then we don't have to do the search below
		// never go up a level past a file (or folder) with an extension
		if (path.hasFileExtension()) 
			return this.findSectionFile(sect, path.toString(), isPreview);
		
		// =====================================================
		//  if request does not have an extension look for files
		//  that might match this path or one of its parents
		//  using the special extensions
		// =====================================================
		
		if (Logger.isDebug())
			Logger.debug("find file path dyn: " + path + " in " + sect);
		
		// we get here if we have no extension - thus we need to look for path match with specials
		int pdepth = path.getNameCount();
		
		// check file system
		while (pdepth > 0) {
			CommonPath ppath = path.subpath(0, pdepth);
			
			for (String ext : this.specialExtensions) {
				CacheFile cfile = this.findSectionFile(sect, ppath.toString() + ext, isPreview);
				
				if (cfile != null)
					return cfile;
			}
			
			pdepth--;
		}
		
		OperationContext.get().errorTr(150007);		
		return null;
	}
	
	public CacheFile findSectionFile(String section, String path, boolean isPreview) {
		if (Logger.isDebug())
			Logger.debug("find section file: " + path + " in " + section);
		
		// for a sub-site, check first in the site folder
		
		if (Logger.isDebug())
			Logger.debug("find section file, check site: " + path + " in " + section);
		
		CacheFile cfile = null;
		
		if (isPreview) {
			cfile = this.site.get().resolveCachePath("/" + section + "-preview" + path);

			if (cfile != null) 
				return cfile;
		}
			
		cfile = this.site.get().resolveCachePath("/" + section + path);
		
		if (cfile != null) 
			return cfile;
		
		// if not root and if shared then try the root level files
		if (! this.site.get().isRoot() && this.site.get().isSharedSection(section)) {
			// now check the root site folders

			if (Logger.isDebug())
				Logger.debug("find section file, check root: " + path + " in " + section);
			
			if (isPreview) {
				cfile = this.site.get().getTenant().resolveCachePath("/" + section + "-preview" + path);

				if (cfile != null) 
					return cfile;
			}
			
			cfile = this.site.get().getTenant().resolveCachePath("/" + section + path);
			
			if (cfile != null) 
				return cfile;
			
			if (Logger.isDebug())
				Logger.debug("find section check packages: " + path + " in " + section);
		}
		
		// check in the modules
		return Hub.instance.getResources().getPackages().cacheLookupPath(this.packagelist, "/" + section + path);
	}

	public FuncResult<XElement> parseUI(CharSequence xml) {
		return XmlReader.parse(xml, true, this.tagmap, UIElement.class);
	}

	public UIElement convertUI(UIElement parent, XElement source) {
		UIElement dest = null;
		
		if (source instanceof UIElement) {
			dest = (UIElement) source;
		}
		else {
			Class<? extends XElement> el = this.tagmap.get(source.getName());
			
			if (el == null)
				el = UIElement.class;
			
			try {
				dest = (UIElement) el.newInstance();
				dest.setName(source.getName());
				dest.replaceAttributes(source);
			} 
			catch (Exception x) {
				Logger.error("Unable to create UI element " + source.getName() + " because: " + x);
				return null;
			}
		}
		
		dest.setParent(parent);
		
		for (int i = 0; i < source.children(); i++) {
			XNode child = source.getChild(i);
			
			if (! (child instanceof UIElement) && child instanceof XElement)  
				dest.replace(i, this.convertUI(dest, (XElement)child));
			else
				dest.replace(i, child);
		}
		
		return dest;
	}
}
