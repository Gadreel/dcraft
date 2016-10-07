package dcraft.hub;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import dcraft.filestore.bucket.Bucket;
import dcraft.filestore.bucket.BucketUtil;
import dcraft.groovy.GCompClassLoader;
import dcraft.io.CacheFile;
import dcraft.io.LocalFileStore;
import dcraft.locale.Dictionary;
import dcraft.locale.ILocaleResource;
import dcraft.locale.LocaleDefinition;
import dcraft.util.MimeUtil;
import dcraft.util.StringUtil;
import dcraft.web.core.SiteIntegration;
import dcraft.web.core.WebSite;
import dcraft.xml.XElement;

/*
 * The "root" site never comes from the /alias/sites/root/* folder.  It always uses the configuration
 * for the domain.  It's files are in /alias/www, /alias/feeds, etc
 * 
 * 
 */

public class SiteInfo extends CommonInfo implements ILocaleResource {
	static public SiteInfo forRoot(DomainInfo domain) {
		SiteInfo site = new SiteInfo();
		site.domain = new WeakReference<DomainInfo>(domain);
		site.alias = "root";
		
		site.init(domain.getSettings());
		
		return site;
	}
	
	static public SiteInfo from(XElement settings, DomainInfo domain) {
		SiteInfo site = new SiteInfo();
		site.domain = new WeakReference<DomainInfo>(domain);

		site.init(settings);
		
		return site;
	}
	
	protected String alias = null;
	protected WeakReference<DomainInfo> domain = null;
	protected WebSite website = null;
	protected GCompClassLoader scriptloader = null;
	protected XElement settings = null;
	protected Map<String, Bucket> buckets = new HashMap<String, Bucket>();
	protected SiteIntegration integration = SiteIntegration.Files;
	
	protected String locale = null;
	protected LocaleDefinition localedef = null;
	protected Dictionary dictionary = null;
	protected Map<String, LocaleDefinition> locales = new HashMap<>();
	protected Map<String, LocaleDefinition> sitelocales = new HashMap<>();
	
	@Override
	public String getAlias() {
		return this.alias;
	}
	
	public DomainInfo getDomain() {
		if (this.domain != null)
			return this.domain.get();
		
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
		
		for (XElement del : settings.selectAll("Domain")) {
			String dname = del.getAttribute("Name");					
			this.domain.get().registerSiteDomain(dname, this);
		}

		Path cpath = this.resolvePath("/config");
		
		if ((cpath != null) && Files.exists(cpath)) {
			// dictionary
			
			Path dicpath = cpath.resolve("dictionary.xml");
	
			if (Files.exists(dicpath)) {
				this.dictionary = new Dictionary();
				this.dictionary.setParent(Hub.instance.getResources().getDictionary());
				this.dictionary.load(dicpath);
			}		
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
				String dname = del.getAttribute("Name");					
				
				if (StringUtil.isEmpty(lname))
					continue;
				
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
		
		this.website = WebSite.from(settings, this);
	}

	public String getMimeType(String ext) {
		// TODO check settings Site before system - no move to domain, domain is where settings / data go
		
		return MimeUtil.getMimeType(ext);
	}
	
	public boolean getMimeCompress(String mime) {
		// TODO check settings Site before system - no move to domain, domain is where settings / data go
		
		return MimeUtil.getMimeCompress(mime);
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
			return this.domain.get().getPath();
		
		LocalFileStore fs = Hub.instance.getPublicFileStore();
		
		if (fs == null)
			return null;
		
		return fs.getFilePath().resolve("dcw/" + this.domain.get().getAlias() + "/sites/" + this.getAlias());
	}

	@Override
	public CacheFile resolveCachePath(String path) {
		if (this.isRoot())
			return this.domain.get().resolveCachePath(path);
		
		LocalFileStore fs = Hub.instance.getPublicFileStore();
		
		if (fs == null)
			return null;
		
		if (StringUtil.isEmpty(path))
			return fs.cacheResolvePath("dcw/" + this.domain.get().getAlias() + "/sites/" + this.getAlias());
		
		if (path.charAt(0) == '/')
			return fs.cacheResolvePath("dcw/" + this.domain.get().getAlias() + "/sites/" + this.getAlias() + path);
		
		return fs.cacheResolvePath("dcw/" + this.domain.get().getAlias() + "/sites/" + this.getAlias() + "/" + path);
	}
	
	public GCompClassLoader getScriptLoader() {
		if (this.scriptloader == null) {
			this.scriptloader = new GCompClassLoader();
			this.scriptloader.init(this.resolvePath("gcache"), this.resolvePath("glib"));
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
}
