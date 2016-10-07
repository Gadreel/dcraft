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
package dcraft.hub;

import groovy.lang.GroovyObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.net.ssl.TrustManager;

import org.joda.time.DateTime;

import dcraft.bus.IService;
import dcraft.bus.ServiceRouter;
import dcraft.io.CacheFile;
import dcraft.io.FileStoreEvent;
import dcraft.io.LocalFileStore;
import dcraft.lang.op.FuncResult;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.OperationContextBuilder;
import dcraft.lang.op.UserContext;
import dcraft.scheduler.ISchedule;
import dcraft.scheduler.SimpleSchedule;
import dcraft.scheduler.common.CommonSchedule;
import dcraft.schema.SchemaManager;
import dcraft.service.DomainServiceAdapter;
import dcraft.service.DomainWatcherAdapter;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.util.IOUtil;
import dcraft.util.ISettingsObfuscator;
import dcraft.util.StringUtil;
import dcraft.web.http.SslContextFactory;
import dcraft.web.http.WebTrustManager;
import dcraft.work.Task;
import dcraft.xml.XAttribute;
import dcraft.xml.XElement;
import dcraft.xml.XmlReader;

public class DomainInfo extends CommonInfo {
	protected RecordStruct info = null;
	protected ISettingsObfuscator obfuscator = null;
	protected XElement settings = null;
	protected SchemaManager schema = null;
	protected Map<String, IService> registered = new HashMap<String, IService>();
	protected Map<String, ServiceRouter> routers = new HashMap<String, ServiceRouter>();
	protected DomainWatcherAdapter watcher = null;
	protected List<ISchedule> schedulenodes = new ArrayList<>();
	
	protected TrustManager[] trustManagers = new TrustManager[1];
	protected DomainNameMapping<SslContextFactory> certs = null;

	protected Map<String, SiteInfo> sites = new HashMap<>();
	protected Map<String, SiteInfo> domainsites = new HashMap<>();
	
	public String getId() {
		return this.info.getFieldAsString("Id");
	}
	
	@Override
	public String getAlias() {
		return this.info.getFieldAsString("Alias");
	}
	
	public String getTitle() {
		return this.info.getFieldAsString("Title");
	}
	
	public RecordStruct getInfo() {
		return this.info;
	}
	
	public IService getService(String name) {
		return this.registered.get(name);
	}
	
	public ServiceRouter getServiceRouter(String name) {
		return this.routers.get(name);
	}
	
	public GroovyObject getScript(String service, String feature) {
		IService s = this.registered.get(service);
		
		if (s instanceof DomainServiceAdapter) 
			return ((DomainServiceAdapter)s).getScript(this, feature);
		
		return null;
	}
	
	/* TODO review
	public GroovyObject getWatcherScript() {
		if (this.watcher != null) 
			return this.watcher.getScript();
		
		return null;
	}
	*/
	
	public ISettingsObfuscator getObfuscator() {
		return this.obfuscator;
	}

	public ListStruct getNames() {
		return this.info.getFieldAsList("Names");
	}
	
	public XElement getSettings() {
		if (this.settings != null)
			return this.settings;
		
		return this.info.getFieldAsXml("Settings");
	}
	
	public SchemaManager getSchema() {
		if (this.schema != null)
			return this.schema;
		
		return Hub.instance.getSchema();
	}

	@Override
	public Path getPath() {
		LocalFileStore fs = Hub.instance.getPublicFileStore();
		
		if (fs == null)
			return null;
		
		return fs.getFilePath().resolve("dcw/" + this.getAlias());
	}

	@Override
	public CacheFile resolveCachePath(String path) {
		LocalFileStore fs = Hub.instance.getPublicFileStore();
		
		if (fs == null)
			return null;
		
		if (StringUtil.isEmpty(path))
			return fs.cacheResolvePath("dcw/" + this.getAlias());
		
		if (path.charAt(0) == '/')
			return fs.cacheResolvePath("dcw/" + this.getAlias() + path);
		
		return fs.cacheResolvePath("dcw/" + this.getAlias() + "/" + path);
	}
	
	public SiteInfo getRootSite() {
		return this.sites.get("root");
	}
	
	public SiteInfo resolveSiteInfo(String domain) {
		if (StringUtil.isEmpty(domain)) 
			return null;
		
		// if this is a site alias then return it
		SiteInfo di = this.sites.get(domain);
		
		if (di != null)
			return di;

		// if not an alias then try lookup of domain name
		di = this.domainsites.get(domain);
		
		if (di != null)
			return di;
		
		// root is default
		return this.sites.get("root");
	}
	
	public void load(RecordStruct info) {
		this.info = info;
				
		this.obfuscator = DomainInfo.prepDomainObfuscator(
				info.getFieldAsString("ObscureClass"), 
				info.getFieldAsString("ObscureSeed"));
		
		this.reloadSettings();
	}

	/* TODO reload more settings too - consider:
	 * 
			./dcw/[domain alias]/config     holds web setting for domain
				- settings.xml are the general settings (dcmHomePage - dcmDefaultTemplate[path]) - direct edit by web dev
				  (includes code tags that map to classes instead of to groovy)
				- dictionary.xml is the domain level dictionary - direct edit by web dev
				- vars.json is the domain level variable store - direct edit by web dev
				
				- cms-settings.xml is extra settings - editable in CMS
				- cms-dictionary.xml is the domain level dictionary - editable in CMS
				- cms-vars.json is the domain level variable store - editable in CMS
	 * 
	 */
	
	public void reloadSettings() {
		this.settings = null;
		
		Path cpath = this.resolvePath("/config");

		if ((cpath == null) || Files.notExists(cpath))
			return;
		
		Path cspath = cpath.resolve("settings.xml");

		if (Files.exists(cspath)) {
			FuncResult<CharSequence> res = IOUtil.readEntireFile(cspath);
			
			if (res.isEmptyResult())
				return;
			
			FuncResult<XElement> xres = XmlReader.parse(res.getResult(), true);
			
			if (xres.isEmptyResult())
				return;
			
			this.settings = xres.getResult();
		}
		
		// get the settings for the domain, be they override or not
		XElement settings = this.getSettings();
		
		// TODO check for and load dictionaries, variables, etc
		
		try {
			this.schema = null;
			
			Path shpath = cpath.resolve("schema.xml");
	
			if (Files.exists(shpath)) {
				this.schema = new SchemaManager();
				this.schema.setChain(Hub.instance.getSchema());
				this.schema.loadSchema(shpath);
				this.schema.compile();
			}		
		}
		catch (Exception x) {
			OperationContext.get().error("Error loading schema: " + x);
		}

		this.registered.clear();
		this.routers.clear();

		Path spath = this.resolvePath("/services");

		if (Files.exists(spath)) {
			try (Stream<Path> str = Files.list(spath)) {
				str.forEach(path -> {
					// only directories are services - files in dir are features
					if (!Files.isDirectory(path))
						return;
					
					String name = path.getFileName().toString();
					
					this.registerService(new DomainServiceAdapter(name, path, DomainInfo.this.getPath()));
				});
			} 
			catch (IOException x) {
				// TODO Auto-generated catch block
				x.printStackTrace();
			}
		}
		
		// watcher comes after services so it can register a service if it likes... if this came before it would be cleared from the registered list
		if (this.watcher == null)
			this.watcher = new DomainWatcherAdapter(this.getPath());
		
		this.watcher.init(this);
		
		for (SiteInfo site : this.sites.values())
			site.kill();

		this.sites.clear();
		
		// discover CERTS
		
		WebTrustManager trustman = new WebTrustManager();
		trustman.init(settings);
		
		this.trustManagers[0] = trustman;
		
		Path certpath = this.resolvePath("config/certs");

		if (Files.notExists(certpath))
			return;
		
		this.certs = new DomainNameMapping<>();
		
		for (XElement cel : settings.selectAll("Certificate")) {
			SslContextFactory ssl = new SslContextFactory();
			ssl.init(cel, certpath.toString() + "/", trustManagers);
			this.certs.add(cel.getAttribute("Name"), ssl);
		}
		
		// load SITES
		this.sites.put("root", SiteInfo.forRoot(this));
		
		for (XElement pel :  settings.selectAll("Site")) {
			String sname = pel.getAttribute("Name");
			
			// root does not get the config from in a Site element
			if (StringUtil.isNotEmpty(sname) && ! "root".equals(sname)) 
				this.sites.put(sname, SiteInfo.from(pel, this));
		}
		
		this.prepDomainSchedule();
	}
	
	public void registerService(IService service) {
		this.registered.put(service.serviceName(), service);
		
		ServiceRouter r = new ServiceRouter(service.serviceName());
		r.indexLocal();
		
		this.routers.put(service.serviceName(), r);
	}

	// matchname might be a wildcard match
	public SslContextFactory getSecureContextFactory(String matchname) {
		if (this.certs != null)
			return this.certs.get(matchname);
		
		return null;
	}
	
	@Override
	public String toString() {
		return this.getTitle();
	}
	
	static public ISettingsObfuscator prepDomainObfuscator(String obclass, String seed) {
		ISettingsObfuscator obfuscator = null;
		
		if (StringUtil.isEmpty(obclass)) 
			obclass = "dcraft.util.StandardSettingsObfuscator";
		else if ("divconq.util.BasicSettingsObfuscator".equals(obclass))
			obclass = "dcraft.util.BasicSettingsObfuscator";
		else if ("divconq.util.StandardSettingsObfuscator".equals(obclass))
			obclass = "dcraft.util.StandardSettingsObfuscator";
			
		try {
			obfuscator = (ISettingsObfuscator) Hub.instance.getInstance(obclass);  
		}
		catch (Exception x) {
			OperationContext.get().error("Bad Settings Obfuscator");
			return null;
		}
		
		XElement clock1 = Hub.instance.getConfig().find("Clock");
		
		String obid = (clock1 != null) ? clock1.getAttribute("Id") : null;
		
		obfuscator.init(new XElement("Clock",
				new XAttribute("Id", obid),
				new XAttribute("Feed", seed)
		));
		
		return obfuscator;
	}
	
	public void prepDomainSchedule() {
		XElement settings = this.getSettings();
		
		// cancel and remove any previous schedules 
		if (this.schedulenodes.size() > 0) {
			OperationContext.get().info("Cancelling schedules for " + this.getAlias());
			
			for (ISchedule sch : this.schedulenodes) {
				OperationContext.get().info("- schedule: " + sch.task().getTitle());
				sch.cancel();
			}
		}
		
		this.schedulenodes.clear();
		
		// now load new schedules
		boolean testing = Hub.instance.getResources().isForTesting(); 
		
		OperationContext.get().info("Prepping schedules for " + this.getAlias());
		
		for (XElement schedule : settings.selectAll("Schedules/*")) {
			String sfor = schedule.getAttribute("For", "Production,Test");
			
			if (testing && !sfor.contains("Test"))
				continue;
			
			if (!testing && !sfor.contains("Production"))
				continue;
			
			OperationContext.get().info("- find schedule: " + schedule.getAttribute("Title"));
			
			ISchedule sched = "CommonSchedule".equals(schedule.getName()) ? new CommonSchedule() : new SimpleSchedule();
			
			sched.init(schedule);
			
			sched.setTask(Task
				.taskWithContext(new OperationContextBuilder().withRootTaskTemplate().withDomainId(this.getId()).toOperationContext())
				.withId(Task.nextTaskId("DomainSchedule"))
				.withTitle("Domain Scheduled Task: " + schedule.getAttribute("Title"))
				.withWork(trun -> {
					OperationContext.get().info("Executing schedule: " + trun.getTask().getTitle() + " for domain " + trun.getTask().getContext().getDomain().getAlias());
					
					if (schedule.hasAttribute("MethodName") && (this.watcher != null))
						this.watcher.tryExecuteMethod(schedule.getAttribute("MethodName"), new Object[] { trun });
				})
			);
		
			OperationContext.get().info("- prepped schedule: " + schedule.getAttribute("Title") + " next run " + new DateTime(sched.when()));
			
			this.schedulenodes.add(sched);
			
			Hub.instance.getScheduler().addNode(sched);
		}
	}

	public void authEvent(String op, String result, UserContext uctx) {
		this.watcher.tryExecuteMethod("AuthEvent", op, result, uctx);
	}

	public void fileChanged(FileStoreEvent result) {
		this.watcher.tryExecuteMethod("FileChanged", result);
	}

	public void fireAfterReindex() {
		this.watcher.tryExecuteMethod("AfterReindex");
	}

	public void registerSiteDomain(String dname, SiteInfo site) {
		this.domainsites.put(dname, site);
	}
}
