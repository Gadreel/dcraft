package dcraft.web.core;

import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import dcraft.filestore.CommonPath;
import dcraft.hub.HubPackage;
import dcraft.hub.SiteInfo;
import dcraft.hub.Hub;
import dcraft.io.ByteBufWriter;
import dcraft.io.CacheFile;
import dcraft.io.LocalFileStore;
import dcraft.lang.op.FuncResult;
import dcraft.lang.op.OperationContext;
import dcraft.log.Logger;
import dcraft.mod.IExtension;
import dcraft.net.ssl.SslHandler;
import dcraft.struct.Struct;
import dcraft.util.IOUtil;
import dcraft.util.StringUtil;
import dcraft.web.WebModule;
import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIUtil;
import dcraft.web.ui.adapter.MarkdownOutputAdapter;
import dcraft.web.ui.adapter.SsiOutputAdapter;
import dcraft.web.ui.adapter.GasOutputAdapter;
import dcraft.web.ui.adapter.StaticOutputAdapter;
import dcraft.work.IWork;
import dcraft.work.Task;
import dcraft.work.TaskRun;
import dcraft.web.ui.adapter.DynamicOutputAdapter;
import dcraft.xml.XElement;
import dcraft.xml.XNode;
import dcraft.xml.XmlReader;

public class WebSite {
	static final public CommonPath PATH_INDEX = new CommonPath("/index");
	static final public CommonPath PATH_HOME = new CommonPath("/Home");
	
	static final public String[] EXTENSIONS_STD = new String[] { ".html", ".md", ".gas" };	
	static final public String[] EXTENSIONS_LEGACY = new String[] { ".dcui.xml", ".html", ".gas" };	

	static public WebSite from(XElement settings, SiteInfo site) {
		WebSite web = new WebSite();
		web.site = new WeakReference<SiteInfo>(site);
		web.init(settings);
		return web;
	}
	
	protected WeakReference<SiteInfo> site = null;
	
	protected HtmlMode htmlmode = HtmlMode.Dynamic;
	protected CommonPath homepath = WebSite.PATH_HOME;
	protected XElement webconfig = null;

	protected Map<String, Class<? extends XElement>> tagmap = new HashMap<String, Class<? extends XElement>>();
	
	protected String[] specialExtensions = WebSite.EXTENSIONS_STD;	
	
	protected List<HubPackage> packagelist = null;
	
	protected boolean legacyweb = false;
	protected boolean srcptstlcache = false;
	
	public HtmlMode getHtmlMode() {
		return this.htmlmode;
	}
	
	public CommonPath getHomePath() {
		return this.homepath;
	}
	
	public XElement getWebConfig() {
		return this.webconfig;
	}
	
	public SiteInfo getSite() {
		if (this.site != null)
			return this.site.get();
		
		return null;
	}
	
	public boolean isLegacySite() {
		return this.legacyweb;
	}
	
	public boolean isScriptStyleCached() {
		return this.srcptstlcache;
	}
	
	public List<HubPackage> getPackagelist() {
		return this.packagelist;
	}
	
	public CommonPath getNotFound() {
		if (this.homepath != null)
			return this.homepath;

		return new CommonPath("/Not-Found.html");
	}
	
	@SuppressWarnings("unchecked")
	public void init(XElement settings) {
		this.webconfig = settings;
		this.tagmap = UIUtil.allocateCoreMap();
		
		// collect a list of the packages names enabled for this domain
		HashSet<String> packagenames = new HashSet<>();
		
		if (Logger.isDebug())
			Logger.debug("Checking web domain settings from domain: " + this.site.get().getTenant().getAlias() + 
				" : " + this.site.get().getAlias() + " - " + settings);
		
		if (settings != null) {
			if (settings.hasAttribute("HtmlMode")) {
				try {
					this.htmlmode = HtmlMode.valueOf(settings.getAttribute("HtmlMode"));
				}
				catch(Exception x) {
					OperationContext.get().error("Unknown HTML Mode: " + settings.getAttribute("HtmlMode"));
				}
			}
			
			if (settings.hasAttribute("HomePath")) 
				this.homepath = new CommonPath(settings.getAttribute("HomePath"));
			else if ((this.htmlmode == HtmlMode.Static) || (this.htmlmode == HtmlMode.Ssi))
				this.homepath = WebSite.PATH_INDEX;		
			
			// legacy support
			if (settings.hasAttribute("UI")) 
				this.legacyweb = "custom".equals(settings.getAttribute("UI").toLowerCase());
			
			if (this.legacyweb)
				this.specialExtensions = WebSite.EXTENSIONS_LEGACY;
			
			// TODO load tag addons (tagmap)
			
			for (XElement tagel : settings.selectAll("Tag")) {
				String name = tagel.getAttribute("Name");
				String cname = tagel.getAttribute("Class");
				
				try {
					if (StringUtil.isNotEmpty(name) && StringUtil.isNotEmpty(cname))
						this.tagmap.put(name, (Class<? extends XElement>) Class.forName(cname));
				}
				catch (ClassNotFoundException x) {
					Logger.error("Unable to load tag class: " + cname);
				}
			}
			
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
						packagenames.add(pel.getAttribute("Name"));
				}
			}
		}
		
		// adjust the order the packages
		this.packagelist = Hub.instance.getResources().getPackages().buildLookupList(packagenames);
		
		if (Logger.isDebug())
			Logger.debug("Package list: " + this.site.get().getTenant().getAlias() + " : " + this.site.get().getAlias() + " - " + this.packagelist.size());
		
		if (! this.legacyweb && ((this.htmlmode == HtmlMode.Dynamic) || (this.htmlmode == HtmlMode.Strict)) 
				&& (mod != null) && mod.isScriptStyleCached()) 
			this.buildScriptStyleCache();
	}

	// see Html class also - if scripts ever change
	public void buildScriptStyleCache() {
		//System.out.println("Script Style Cache for: " + this.getSite().getTenant().getAlias() + 
		//		"/" + this.getSite().getAlias());
		
		Task buildcache = Task.taskWithSubContext()
				.withTitle("Script Style Cache for: " + this.getSite().getTenant().getAlias() + 
						"/" + this.getSite().getAlias())
				.withTopic("Batch")
				.withWork(new IWork() {
					@Override
					public void run(TaskRun trun) {
						Path sitepath = WebSite.this.site.get().getPath();
						
						LocalFileStore fs = Hub.instance.getTenantsFileStore();
						
						if (fs == null)
							return;
						
						// styles
						
						List<String> styles = WebSite.this.globalStyles(false, false);
						
						ByteBufWriter bbw = ByteBufWriter.createLargeHeap();
						
						for (String style : styles) {
							CacheFile wpath = WebSite.this.findFilePath(new CommonPath(style), false);
							
							if (wpath != null)
								bbw.write(wpath.asBuffer());		// TODO min
							else
								Logger.error("Style not found: " + style);
						}
						
						Path stylepth = sitepath.resolve("www/css/dc-cache.min.css");

						IOUtil.saveBuffer(stylepth, bbw.getByteBuf());	// releases the buffer
						
						// scripts
						
						List<String> scripts = WebSite.this.globalScripts(false, false);
						
						bbw = ByteBufWriter.createLargeHeap();
						
						for (String script : scripts) {
							CacheFile wpath = WebSite.this.findFilePath(new CommonPath(script), false);
							
							if (wpath != null)
								bbw.write(wpath.asBuffer());		// TODO min
							else
								Logger.error("Script not found: " + script);
						}
						
						IOUtil.saveBuffer(sitepath.resolve("www/js/dc-cache.min.js"), bbw.getByteBuf());	// releases the buffer
						
						WebSite.this.srcptstlcache = true;
						
						trun.complete();
					}
				});
		
		Hub.instance.getWorkPool().submit(buildcache);
	}
	
	public List<String> globalStyles(boolean includeWebRemote, boolean cachmode) {
		List<String> ret = new ArrayList<>();
		
		if (includeWebRemote && (this.webconfig != null)) {
			for (XElement gel : this.webconfig.selectAll("Global")) {
				if (gel.hasNotEmptyAttribute("Style")) {
					String surl = gel.getAttribute("Style");
					
					if (! UIUtil.urlLooksLocal(surl))
						ret.add(surl);
				}
			}
		}
		
		if (cachmode) {
			ret.add("/css/dc-cache.min.css");
			return ret;
		}
		
		for (int i = this.packagelist.size() - 1; i >= 0; i--) {
			HubPackage pack = this.packagelist.get(i);
			
			XElement set = pack.getSettings();
			
			if (set == null)
				continue;
			
			XElement web = set.find("Web");
			
			if (web == null)
				continue;
			
			for (XElement gel : web.selectAll("Global")) {
				if (gel.hasNotEmptyAttribute("Style")) 
					ret.add(gel.getAttribute("Style"));
			}
		}
		
		if (this.webconfig != null) {
			for (XElement gel : this.webconfig.selectAll("Global")) {
				if (gel.hasNotEmptyAttribute("Style")) {
					String surl = gel.getAttribute("Style");
					
					if (UIUtil.urlLooksLocal(surl))
						ret.add(surl);
				}
			}
		}

		ret.add("/css/main.css");		// site specifics and overrides
		
		return ret;
	}
	
	public List<String> globalScripts(boolean includeWebRemote, boolean cachmode) {
		List<String> ret = new ArrayList<>();
		
		if (includeWebRemote && (this.webconfig != null)) {
			for (XElement gel : this.webconfig.selectAll("Global")) {
				if (gel.hasNotEmptyAttribute("Script")) {
					String surl = gel.getAttribute("Script");
					
					if (! UIUtil.urlLooksLocal(surl))
						ret.add(surl);
				}
			}
		}
		
		if (cachmode) {
			ret.add("/js/dc-cache.min.js");
			return ret;
		}
		
		for (int i = this.packagelist.size() - 1; i >= 0; i--) {
			HubPackage pack = this.packagelist.get(i);
			
			XElement set = pack.getSettings();
			
			if (set == null)
				continue;
			
			XElement web = set.find("Web");
			
			if (web == null)
				continue;
			
			for (XElement gel : web.selectAll("Global")) {
				if (gel.hasNotEmptyAttribute("Script")) 
					ret.add(gel.getAttribute("Script"));
			}
		}
		
		if (this.webconfig != null) {
			for (XElement gel : this.webconfig.selectAll("Global")) {
				if (gel.hasNotEmptyAttribute("Script")) {
					String surl = gel.getAttribute("Script");
					
					if (UIUtil.urlLooksLocal(surl))
						ret.add(surl);
				}
			}
		}

		ret.add("/js/main.js");		// site specifics and overrides

		ret.add("/js/dc.go.js");		// start the UI scripts
		
		return ret;
	}
	
	public IOutputMacro getMacro(String name) {
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
		
		XElement web = this.webconfig;
		
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
			Logger.debug("Site: " + (this.site != null ? this.site.get().getAlias() : "[missing]"));
		
		if (Logger.isDebug())
			Logger.debug("Translating path: " + ctx.getRequest().getPath());

		CommonPath path = ctx.getRequest().getPath();
				
		if (path.isRoot()) {
			path = this.getHomePath();
			ctx.getRequest().setPath(path);
		}
		
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
		else if (this.legacyweb && (filename.endsWith(".dcui.xml") || filename.endsWith(".dcuis.xml"))) {
			ioa = (IOutputAdapter) Hub.instance.getInstance("dcraft.web.ui.adapter.DcuiOutputAdapter");
		}
		else if (filename.endsWith(".html")) {
			if (hmode == HtmlMode.Ssi) 
				ioa = new SsiOutputAdapter();
			else if ((hmode == HtmlMode.Dynamic) || (hmode == HtmlMode.Strict))
				ioa = new DynamicOutputAdapter();
			else if (hmode == HtmlMode.Static)
				ioa = new StaticOutputAdapter();
		}		
		else if (filename.endsWith(".md")) {
			ioa = new MarkdownOutputAdapter();		
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
		CacheFile cfile = this.site.get().findSectionFile(section, path, isPreview);
		
		if (cfile != null)
			return cfile;
		
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
