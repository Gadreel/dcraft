package dcraft.hub;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import dcraft.cms.feed.core.FeedAdapter;
import dcraft.filestore.bucket.Bucket;
import dcraft.filestore.bucket.BucketUtil;
import dcraft.groovy.GCompClassLoader;
import dcraft.io.CacheFile;
import dcraft.io.LocalFileStore;
import dcraft.lang.op.FuncResult;
import dcraft.lang.op.OperationContext;
import dcraft.locale.Dictionary;
import dcraft.locale.ILocaleResource;
import dcraft.locale.LocaleDefinition;
import dcraft.log.Logger;
import dcraft.struct.CompositeStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.util.IOUtil;
import dcraft.util.MimeUtil;
import dcraft.util.StringUtil;
import dcraft.web.core.GalleryImageConsumer;
import dcraft.web.core.SiteIntegration;
import dcraft.web.core.WebSite;
import dcraft.web.http.SslContextFactory;
import dcraft.xml.XElement;
import dcraft.xml.XmlReader;

/*
 * The "root" site never comes from the /alias/sites/root/* folder.  It always uses the configuration
 * for the domain.  It's files are in /alias/www, /alias/feeds, etc
 * 
 * 
 */

public class SiteInfo extends CommonInfo implements ILocaleResource {
	static public SiteInfo forRoot(TenantInfo tenant) {
		SiteInfo site = new SiteInfo();
		site.tenant = new WeakReference<TenantInfo>(tenant);
		site.alias = "root";
		
		site.init(tenant.getSettings());
		
		return site;
	}
	
	static public SiteInfo from(XElement settings, TenantInfo tenant) {
		SiteInfo site = new SiteInfo();
		site.tenant = new WeakReference<TenantInfo>(tenant);
		site.alias = settings.getAttribute("Name");

		site.init(settings);
		
		return site;
	}
	
	protected String alias = null;
	protected WeakReference<TenantInfo> tenant = null;
	protected WebSite website = null;
	protected GCompClassLoader scriptloader = null;
	protected XElement settings = null;
	protected DomainNameMapping<SslContextFactory> certs = null;
	protected Map<String, Bucket> buckets = new HashMap<String, Bucket>();
	protected SiteIntegration integration = SiteIntegration.Files;
	
	protected Map<String, CacheEntry> cache = new HashMap<String, CacheEntry>();
	
	protected String locale = null;
	protected LocaleDefinition localedef = null;
	protected Dictionary dictionary = null;
	protected Map<String, LocaleDefinition> locales = new HashMap<>();
	protected Map<String, LocaleDefinition> sitelocales = new HashMap<>();
	
	@Override
	public String getAlias() {
		return this.alias;
	}
	
	public TenantInfo getTenant() {
		if (this.tenant != null)
			return this.tenant.get();
		
		return null;
	}
	
	public WebSite getWebsite() {
		return this.website;
	}
	
	public Map<String, LocaleDefinition> getLocales() {
		if (this.locales.size() == 0) {
			// make sure we have at least 1 locale listed for the site
			String lvalue = this.getDefaultLocale();
			
			// add the list of locales supported for this site
			this.locales.put(lvalue, this.getLocaleDefinition(lvalue));
		}
		
		return this.locales;
	}
	
	public Map<String, LocaleDefinition> getSiteLocales() {
		return this.sitelocales;
	}
	
	public SiteIntegration getIntegration() {
		if (this.isRoot())
			return SiteIntegration.None;
		
		return this.integration;
	}
	
	public boolean isSharedSection(String section) {
		if (this.isRoot())
			return false;
		
		if (this.integration == SiteIntegration.None)
			return false;
		
		if (this.integration == SiteIntegration.Full)
			return true;

		return ("files".equals(section) || "galleries".equals(section));
	}
	
	public boolean isRoot() {
		return "root".equals(this.alias);
	}
	
	public XElement getSettings() {
		return this.settings;
	}
	
	public Bucket getBucket(String name) {
		// support sub sites that try to access buckets via prepending "Root" to files store name.
		if (this.isRoot()) {
			if ("RootWebGallery".equals(name)) 
				name = "WebGallery";
			else if ("RootWebFileStore".equals(name)) 
				name = "WebFileStore";
		}
		
		Bucket b = this.buckets.get(name);
		
		if (b == null) {
			b = BucketUtil.buildBucket(name, this);
			
			if (b != null)
				this.buckets.put(name, b);
		}
		
		return b;
	}
	
	public void init(XElement settings) {
		this.settings = settings;

		Path cpath = this.resolvePath("/config");
		
		if ((cpath != null) && Files.exists(cpath)) {
			Path cspath = cpath.resolve("settings.xml");
			
			if (Files.exists(cspath)) {
				FuncResult<CharSequence> res = IOUtil.readEntireFile(cspath);
				
				if (res.isEmptyResult())
					return;
				
				FuncResult<XElement> xres = XmlReader.parse(res.getResult(), true);
				
				if (xres.isEmptyResult())
					return;
				
				this.settings = settings = xres.getResult();
			}
			
			// dictionary
			
			Path dicpath = cpath.resolve("dictionary.xml");
	
			if (Files.exists(dicpath)) {
				this.dictionary = new Dictionary();
				this.dictionary.setParent(Hub.instance.getResources().getDictionary());
				this.dictionary.load(dicpath);
			}		
		}

		if (settings != null) {
			for (XElement del : settings.selectAll("Domain")) {
				String dname = del.getAttribute("Name");					// TODO check Use attribute		
				this.tenant.get().registerSiteDomain(dname, this);
			}
	
			if (settings.hasAttribute("Locale")) {
				this.locale = settings.getAttribute("Locale");
				
				this.localedef = this.getLocaleDefinition(this.locale);
				
				// add the list of locales supported for this site
				this.locales.put(this.locale, this.localedef);
			}
			
			// these settings are valid for root and sub sites
			
			for (XElement pel : settings.selectAll("Locale")) {
				String lname = pel.getAttribute("Name");
				
				if (StringUtil.isEmpty(lname))
					continue;
	
				LocaleDefinition def = this.getLocaleDefinition(lname);
				
				this.locales.put(lname, def);
				
				for (XElement del : pel.selectAll("Domain")) {
					String dname = del.getAttribute("Name");					// TODO check Use attribute
					
					if (StringUtil.isEmpty(lname))
						continue;
					
					this.tenant.get().registerSiteDomain(dname, this);
					
					this.sitelocales.put(dname, def);
				}
			}		
			
			// this setting is only valid for sub sites
			if (settings.hasAttribute("Integration") && ! this.isRoot()) {
				try {
					this.integration = SiteIntegration.valueOf(settings.getAttribute("Integration", "Files"));
				}
				catch (Exception x) {
					this.integration = SiteIntegration.Files;
				}
			}
			
			Path certpath = this.resolvePath("config/certs");
	
			if (Files.exists(certpath)) {
				this.certs = new DomainNameMapping<>();
				
				for (XElement cel : settings.selectAll("Certificate")) {
					SslContextFactory ssl = new SslContextFactory();
					ssl.init(cel, certpath.toString() + "/", this.tenant.get().getTrustManagers());
					this.certs.add(cel.getAttribute("Name"), ssl);
				}
			}
		}
		
		this.website = WebSite.from((settings != null) ? settings.selectFirst("Web") : null, this);
	}

	public String getMimeType(String ext) {
		// TODO check settings Site before system - no move to domain, domain is where settings / data go
		
		return MimeUtil.getMimeType(ext);
	}
	
	public boolean getMimeCompress(String mime) {
		// TODO check settings Site before system - no move to domain, domain is where settings / data go
		
		return MimeUtil.getMimeCompress(mime);
	}

	// matchname might be a wildcard match
	public SslContextFactory getSecureContextFactory(String matchname) {
		if (this.certs != null)
			return this.certs.get(matchname);
		
		return null;
	}
	
	@Override
	public String getDefaultLocale() {
		if (this.locale != null)
			return this.locale;
		
		return this.getParentLocaleResource().getDefaultLocale();
	}

	@Override
	public LocaleDefinition getDefaultLocaleDefinition() {
		return this.getLocaleDefinition(this.getDefaultLocale());
	}

	@Override
	public LocaleDefinition getLocaleDefinition(String name) {
		// TODO lookup definitions
		
		return new LocaleDefinition(name);
	}
	
	// 0 is best, higher the number the worse, -1 for not supported
	@Override
	public int rateLocale(String locale) {
		if ((this.localedef != null) && this.localedef.match(locale))
			return 0;
		
		int r = this.getParentLocaleResource().rateLocale(locale);
		
		if (r < 0)
			return -1;
		
		return r + 1;
	}
	
	@Override
	public ILocaleResource getParentLocaleResource() {
		return Hub.instance.getResources();
	}
	
	@Override
	public Dictionary getDictionary() {
		if (this.dictionary != null)
			return this.dictionary;
		
		return this.getParentLocaleResource().getDictionary();
	}

	@Override
	public Path getPath() {
		if (this.isRoot())
			return this.tenant.get().getPath();
		
		LocalFileStore fs = Hub.instance.getTenantsFileStore();
		
		if (fs == null)
			return null;
		
		return fs.resolvePath(this.tenant.get().getAlias() + "/sites/" + this.getAlias());
	}

	@Override
	public CacheFile resolveCachePath(String path) {
		if (this.isRoot())
			return this.tenant.get().resolveCachePath(path);
		
		LocalFileStore fs = Hub.instance.getTenantsFileStore();
		
		if (fs == null)
			return null;
		
		if (StringUtil.isEmpty(path))
			return fs.cacheResolvePath(this.tenant.get().getAlias() + "/sites/" + this.getAlias());
		
		if (path.charAt(0) == '/')
			return fs.cacheResolvePath(this.tenant.get().getAlias() + "/sites/" + this.getAlias() + path);
		
		return fs.cacheResolvePath(this.tenant.get().getAlias() + "/sites/" + this.getAlias() + "/" + path);
	}
	
	public CacheFile findSectionFile(String section, String path, boolean isPreview) {
		if (Logger.isDebug())
			Logger.debug("find section file: " + path + " in " + section);
		
		// for a sub-site, check first in the site folder
		
		if (Logger.isDebug())
			Logger.debug("find section file, check site: " + path + " in " + section);
		
		CacheFile cfile = null;
		
		if (isPreview) {
			cfile = this.resolveCachePath("/" + section + "-preview" + path);

			if (cfile != null) 
				return cfile;
		}
			
		cfile = this.resolveCachePath("/" + section + path);
		
		if (cfile != null) 
			return cfile;
		
		// if not root and if shared then try the root level files
		if (! this.isRoot() && this.isSharedSection(section)) {
			// now check the root site folders

			if (Logger.isDebug())
				Logger.debug("find section file, check root: " + path + " in " + section);
			
			if (isPreview) {
				cfile = this.getTenant().resolveCachePath("/" + section + "-preview" + path);

				if (cfile != null) 
					return cfile;
			}
			
			cfile = this.getTenant().resolveCachePath("/" + section + path);
			
			if (cfile != null) 
				return cfile;
			
			if (Logger.isDebug())
				Logger.debug("find section check packages: " + path + " in " + section);
		}
		
		return null;
	}

	// string path is relative to tenants/[alias]/[path]
	public XElement getXmlResource(String section, String path, boolean preview) {
		CacheFile fpath = this.findSectionFile(section, path, preview);
		
		if (fpath == null)
			return null;
		
		return fpath.asXml();
	}
	
	// string path is relative to tenants/[alias]/[section]/[path]
	public CompositeStruct getJsonResource(String section, String path, boolean preview) {
		CacheFile fpath = this.findSectionFile(section, path, preview);
		
		if (fpath == null)
			return null;
		
		return fpath.asJson();
	}
	
	// string path is relative to tenants/[alias]/[section]/[path]
	public String getTextResource(String section, String path, boolean preview) {
		CacheFile fpath = this.findSectionFile(section, path, preview);
		
		if (fpath == null)
			return null;
		
		return fpath.asString();
	}
	
	public FeedAdapter getFeedAdapter(String alias, String path, boolean preview) {
		XElement settings = OperationContext.get().getSite().getSettings();
		
		if (settings == null) 
			return null;
		
		XElement feed = settings.find("Feed");
		
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
				
				CacheFile fpath = this.findSectionFile("feed", innerpath, preview);
				
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
	
	public CompositeStruct getGalleryMeta(String path, boolean preview) {
		return this.getJsonResource("galleries", path + "/meta.json", preview);
	}
	
	public void forEachGalleryShowImage(String path, String show, boolean preview, GalleryImageConsumer consumer) {
		RecordStruct gallery = (RecordStruct) this.getGalleryMeta(path, preview);
		
		if ((gallery != null) && (gallery.hasField("Shows"))) {
			for (Struct s : gallery.getFieldAsList("Shows").getItems()) {
				RecordStruct showrec = (RecordStruct) s;
				
				if (!show.equals(showrec.getFieldAsString("Alias")))
					continue;
				
				for (Struct i : showrec.getFieldAsList("Images").getItems()) {
					consumer.accept(gallery, showrec, i);
				}
			}
		}
	}	
	
	public GCompClassLoader getScriptLoader() {
		if (this.scriptloader == null) {
			this.scriptloader = new GCompClassLoader();
			this.scriptloader.init(this.resolvePath("cache"), this.resolvePath("glib"));
		}
		
		return this.scriptloader;
	}
	
	public void execute(Path src, String method, Object... args) throws FileNotFoundException, ClassNotFoundException, 
		InstantiationException, IllegalAccessException, IOException 
	{
		GCompClassLoader sl = this.getScriptLoader();
		
		sl.execute(src, method, args);
	}
	
	public void execute(String code, String method, Object... args) throws ClassNotFoundException, 
		InstantiationException, IllegalAccessException, IOException 
	{
		GCompClassLoader sl = this.getScriptLoader();
				
		sl.execute(code, method, args);
	}

	public void kill() {
		for (Bucket b : this.buckets.values())
			b.tryExecuteMethod("Kill", this);
		
		this.buckets.clear();
	}

	public void setCache(String cname, Struct data, long secs) {
		CacheEntry c = new CacheEntry();
		c.data = data;
		c.expires = System.currentTimeMillis() + (secs * 1000);
		c.name = cname;
		
		this.cache.put(cname, c);
	}

	public Struct getCache(String cname) {
		CacheEntry c = this.cache.get(cname);
		
		if ((c == null) || c.isExpired()) {
			this.cache.remove(cname);
			return null;
		}
		
		return c.data;
	}
	
	protected class CacheEntry {
		protected Struct data = null;
		protected long expires = 0;
		protected String name = null;
		
		public boolean isExpired() {
			return System.currentTimeMillis() > this.expires;
		}
	}
}
