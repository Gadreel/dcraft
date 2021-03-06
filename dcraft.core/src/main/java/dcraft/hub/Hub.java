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

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.io.File;
import java.io.IOException;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.joda.time.DateTime;

import dcraft.api.ApiSession;
import dcraft.api.IApiSessionFactory;
import dcraft.bus.Bus;
import dcraft.count.CountManager;
import dcraft.ctp.net.CtpServices;
import dcraft.db.Constants;
import dcraft.db.DataRequest;
import dcraft.db.IDatabaseManager;
import dcraft.db.ObjectResult;
import dcraft.io.IFileWatcher;
import dcraft.io.LocalFileStore;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.OperationResult;
import dcraft.locale.LocaleDefinition;
import dcraft.log.HubLog;
import dcraft.log.Logger;
import dcraft.mod.IModule;
import dcraft.mod.ModuleLoader;
import dcraft.scheduler.Scheduler;
import dcraft.schema.SchemaManager;
import dcraft.script.ActivityManager;
import dcraft.service.simple.AuthService;
import dcraft.service.simple.TenantsService;
import dcraft.session.Sessions;
import dcraft.sql.SqlManager;
import dcraft.sql.SqlManager.SqlDatabase;
import dcraft.struct.CompositeStruct;
import dcraft.struct.FieldStruct;
import dcraft.struct.RecordStruct;
import dcraft.util.FileUtil;
import dcraft.util.IOUtil;
import dcraft.util.MimeUtil;
import dcraft.util.StringUtil;
import dcraft.work.WorkPool;
import dcraft.work.WorkQueue;
import dcraft.xml.XAttribute;
import dcraft.xml.XElement;
 
/**
 * Hub is the center of activity for DivConq applications.  Most of the built-in resources/features are available via the Hub.
 * The Hub must be initialized by creating a HubResource and passing it to "start".  When the application quits it is best
 * to call "stop".
 * 
 * There is only one Hub object per process (per JVM), the way to access the Hub object is through Hub.instance.
 *  
 * @author Andy White
 *
 */
public class Hub {

	// ============ STATIC ============
	
	static public Hub instance = new Hub();
	
	static {
		// java 6 does not do TTL correctly, change it
		java.security.Security.setProperty("networkaddress.cache.ttl" , "0");		
		
		//PooledByteBufAllocator.DEFAULT.
	}
	
	// ============ INSTANCE ============	
	
	protected long starttime = System.currentTimeMillis();
	protected Clock clock = new Clock();
	protected String[] libpaths = null;
	protected Map<String,ModuleLoader> modules = new HashMap<>();
	protected List<ModuleLoader> orderedModules = new ArrayList<>();

	protected Bus bus = new Bus();
	protected CtpServices ctp = new CtpServices();
	protected WorkPool workpool = null;
	protected WorkQueue workqueue = new WorkQueue(); 
	protected Scheduler scheduler = new Scheduler();
	protected SqlManager sqldbman = new SqlManager();
	protected LocalFileStore tenantsfilestore = null;
	protected WatchKey tenantsfilequeuekey = null;
	protected Sessions sessions = new Sessions();
	protected HubResources resources = null;
	protected SecurityPolicy policy = new SecurityPolicy();
	protected IDatabaseManager db = null;
	
	protected TenantManager tenantman = new TenantManager();
	
	// api session managers
	protected ConcurrentHashMap<String, IApiSessionFactory> apimans = new ConcurrentHashMap<>();
	
	// hub events
	protected ConcurrentHashMap<Integer, Set<IEventSubscriber>> subscribers = new ConcurrentHashMap<>();
	
	protected ActivityManager actman = null;
	protected ByteBufAllocator bufferAllocator = PooledByteBufAllocator.DEFAULT; // UnpooledByteBufAllocator.DEFAULT;  // 
	protected EventLoopGroup eventLoopGroup = null;
	
	protected CountManager countman = new CountManager(); 
	
	protected HubState state = HubState.Booting;
	protected boolean idleflag = false;		// set if Run should really be Idle or reverse
	
	protected int dependencyCntBoot = 0;
	protected int dependencyCntConn = 0;
	protected int dependencyCntRun = 0;
	
	protected ReentrantLock depedencyLock = new ReentrantLock();
	protected HashMap<String, HubDependency> dependencies = new HashMap<>();
	
	protected WatchService watcher = null;
	protected Map<WatchKey, IFileWatcher> filewatchdata = new HashMap<>();

	public HubState getState() {
		return this.state;
	}
	
	public void dependencyChanged() {
		OperationContext.useHubContext();
		
		if (Logger.isDebug()) 
			Logger.debug("Dependency channged");
		
		this.depedencyLock.lock();
		
		try {
			this.dependencyCntBoot = 0;
			this.dependencyCntConn = 0;
			this.dependencyCntRun = 0;
			
			for (HubDependency d : this.dependencies.values()) {
				if (Logger.isDebug()) 
					Logger.debug("Dependency " + d.source + " - boot: " + d.passBoot
							+ " connect: " + d.passConnected + " run: " + d.passRun);
				
				if (!d.passBoot)
					this.dependencyCntBoot++;
				
				if (!d.passConnected)
					this.dependencyCntConn++;
				
				if (!d.passRun)
					this.dependencyCntRun++;
			}
			
			// not ready to start
			if (this.dependencyCntBoot > 0) {
				// not ready is ok if we are still starting
				if (this.state == HubState.Booting) {
					Logger.info("Waiting on boot dependencies: " + this.dependencyCntBoot);
					return;
				}
				
				Logger.error("Illegal hub state, higher than booting but missing boot dependencies: " + this.dependencyCntBoot);
				
				// if in any other state and we are not ready then not good - stop
				this.stop();
				return;
			}
			
			if (this.state == HubState.Booting) {
				if (Logger.isDebug()) 
					Logger.debug("Switch hub to Booted");
				
				this.booted();		// set status 
				return;
			}
			
			if (this.dependencyCntConn > 0) {
				// not ready is ok if we are just starting
				if (this.state == HubState.Booted) {
					Logger.info("Waiting on connect dependencies: " + this.dependencyCntConn);
					return;
				}
				
				Logger.warn("Hub state higher than connected but missing connected dependencies: " + this.dependencyCntConn);
				
				this.booted();	// update status
				return;
			}
			
			if (this.state == HubState.Booted) {
				if (Logger.isDebug()) 
					Logger.debug("Switch hub to Connected");
				
				this.connected();		// go into Connected status
				return;
			}
			
			if (this.dependencyCntRun > 0) {
				// not ready is ok if we are just starting
				if (this.state == HubState.Connected) {
					Logger.info("Waiting on run dependencies: " + this.dependencyCntRun);
					return;
				}
				
				Logger.warn("Hub state higher than booted but missing running dependencies: " + this.dependencyCntRun);
				
				this.connected();	// update status
				return;
			}
			
			if (this.state == HubState.Connected) {
				if (Logger.isDebug()) 
					Logger.debug("Switch hub to Running");
				
				this.running();		// go into Running or Idle status
				return;
			}
		}
		finally {
			this.depedencyLock.unlock();
		}
	}
	
	protected void running() {
		if (this.idleflag) {
			this.state = HubState.Idle;
			
			Logger.info("Hub entered Idled state");
			
			this.fireEvent(HubEvents.Idling, null);
		}
		else {
			this.state = HubState.Running;
			
			Logger.info("Hub entered Running state");
			
			this.fireEvent(HubEvents.Running, null);
		}
	}
	
	protected void booted() {
		this.state = HubState.Booted;
		
		Logger.info("Hub entered Booted state");
		
		this.fireEvent(HubEvents.Booted, null);
		
		this.dependencyChanged();
	}
	
	public void connected() {
		// if unconnected, gateway will get a message later
		//if (this.resources.isGateway() && !this.bus.isConnected())
		//	return;
		
		this.state = HubState.Connected;
		
		Logger.info("Hub entered Connected state");
		
		this.fireEvent(HubEvents.Connected, null);
		
		// TODO add prep functions to load from database or do automatic self updates
		
		// TODO check for repo updates if connected via db and if in Production mode (not dev mode)
		// if true then restart with resume code
		
		// gateways always use Auth and Tenant service from internal server
		// these two services are required, if service not available and we are not on Gateway then load them
			
		// load default dcTenants
		if (!this.bus.isServiceAvailable("dcTenants")) {
			TenantsService s = new TenantsService();
			s.init(null);
			this.bus.getLocalHub().registerService(s);
		}
		
		// load default dcAuth  
		if (!this.bus.isServiceAvailable("dcAuth")) {
			AuthService s = new AuthService();
			s.init(null);
			this.bus.getLocalHub().registerService(s);
		}

		this.tenantman.init();
		
		this.dependencyChanged();
	}
	
	public void setIdled(boolean v) {
		if (this.idleflag == v)
			return;
		
		this.idleflag = v;
		
		if ((this.state == HubState.Running) || (this.state == HubState.Idle))
			this.running();
	}
	
	public boolean isStopping() {
		return (this.state == HubState.Stopping) || (this.state == HubState.Stopped); 
	}
	
	public boolean isIdled() {
		return (this.state == HubState.Idle); 
	}
	
	public boolean isRunning() {
		return (this.state == HubState.Running); 
	}
	
	public boolean isBooted() {
		return (this.state.getCode() > HubState.Booting.getCode()) && (this.state.getCode() < HubState.Stopped.getCode());
	}
	
	public void addDependency(HubDependency v) {
		if (Logger.isDebug()) 
			Logger.debug("Adding Dependency " + v.source + " - boot: " + v.passBoot
					+ " connect: " + v.passConnected + " run: " + v.passRun);
		
		this.depedencyLock.lock();
		
		try {
			this.dependencies.put(v.source, v);
			v.added = true;
		}
		finally {
			this.depedencyLock.unlock();
		}
		
		this.dependencyChanged();
	}
	
	public void removeDependency(String v) {
		if (Logger.isDebug()) 
			Logger.debug("Removing Dependency " + v);
		
		this.depedencyLock.lock();
		
		try {
			this.dependencies.remove(v);
		}
		finally {
			this.depedencyLock.unlock();
		}
		
		this.dependencyChanged();
	}
	
	public HubDependency getDependency(String v) {
		this.depedencyLock.lock();
		
		try {
			return this.dependencies.get(v);
		}
		finally {
			this.depedencyLock.unlock();
		}
	}
	
	public Bus getBus() {
		return this.bus;
	}
	
	public SecurityPolicy getSecurityPolicy() {
		return this.policy;
	}
	
	public CtpServices getCtp() {
		return this.ctp;
	}
	
	public LocalFileStore getTenantsFileStore() {
		return this.tenantsfilestore;
	}
	
	public ActivityManager getActivityManager() {
		return this.actman;
	}
	
	public CountManager getCountManager() {
		return this.countman;
	}
	
	public ByteBufAllocator getBufferAllocator() {
		return this.bufferAllocator;
	}
	
	public EventLoopGroup getEventLoopGroup() {
		if (this.eventLoopGroup == null)
			this.eventLoopGroup = new NioEventLoopGroup();
		
		return this.eventLoopGroup;
	}
	
	/**
	 * DivConq uses a specialized type system that provides type consistency across services 
	 * (including web services), database fields and stored procedures, as well as scripting.
	 * 
	 * All scalars (including primitives) and composites (collections) are wrapped by some
	 * subclass of Struct.  List/array collections are expressed by this class.  
	 * This class is analogous to an Array in JSON but may contain type information as well, 
	 * similar to Yaml.
	 * 
	 * There are schema files (written in Xml and stored in the Packages repository) that define
	 * all the known data types, including complex data types.
	 * 
	 * @return the master collection of all known simple and complex data types
	 */
	public SchemaManager getSchema() {
		if (this.resources != null)
			return this.resources.getSchema();
		
		return null;
	}
	
	/**
	 * The WorkPool is a general purpose thread pool that should be used to execute tasks.
	 * DivConq does not recommend using your own threads as you'll lose the TaskContext and
	 * many features will cease to work correctly.  However, do not use WorkPool to run tasks that
	 * are known to block for long periods (seconds).  WorkPool is for tasks that execute non-stop
	 * and that use async calls when encountering a potentially blocking situation.  (note the
	 * async result should come back on a WorkPool thread also - not necessarily the same
	 * thread).
	 * 
	 * @see OperationContext for addition notes.
	 *   
	 * @return the WorkPool manager
	 */
	public WorkPool getWorkPool() {
		return this.workpool;
	}
	
	public WorkQueue getWorkQueue() {
		return this.workqueue;
	}
	
	/**
	 * Along the same lines as WorkPool (see "getWorkPool") the scheduler provides a way to 
	 * run tasks.  Scheduler uses the WorkPool for running tasks.  However, Scheduler has 
	 * features to schedule the task at some future time and to optionally make the task recurring.
	 * 
	 * See the "schedule" methods in Clock for an alternative.  Scheduler is best for tasks that
	 * need to retain TaskContext (which is often) and that occur once or every 10 seconds or more.
	 * Tasks that need to occur very frequently or need to be on the system clock may be candidates 
	 * for Clock scheduling.  
	 * 
	 * @return the scheduling manager
	 */	
	public Scheduler getScheduler() {
		return this.scheduler;
	}
	
	public Sessions getSessions() {
		return this.sessions;
	}
	
	/**
	 * The clock tracks the application's time (time zone and date time).  The application's time
	 * can be altered or even sped up.  There are also some scheduling methods in Clock but consider
	 * using Scheduler over Clock, see "getScheduler".
	 * 
	 * @return the clock manager
	 */
	public Clock getClock() {
		return this.clock;
	}
	
	/**
	 * The whole of Hub is started based off the settings from a single config file.  
	 * 
	 * @return the root element of the Hub config file
	 */
	public XElement getConfig() {
		if (this.resources != null)
			return this.resources.getConfig();

		return null;
	}

	/**
	 * @return time the Hub started in ms since 1970 
	 */
	public long getStartTime() {
		return this.starttime;
	}

	public IDatabaseManager getDatabase() {
		return this.db;
	}
	
	public SqlDatabase getSQLDatabase() {
		return this.getSQLDatabase("default");
	}
	
	public SqlDatabase getSQLDatabase(String name) {
		return this.sqldbman.getDatabase(name);
	}

	public SqlManager getSQLManager() {
		return this.sqldbman;
	}
	
	/**
	 * @return the HubResources object that was used to start the Hub 
	 */
	public HubResources getResources() {
		return this.resources;
	}
	
	public TenantManager getTenants() {
		return this.tenantman;
	}
	
	public TenantInfo getTenantInfo(String id) {
		return this.tenantman.resolveTenantInfo(id);
	}
	
	/**
	 * Before many of the features of DivConq can be used the Hub must be started.  This requires that a HubResources
	 * object be created and hold valid paths/config/etc.
	 * 
	 * @param resources the resources to use in startup
	 * @return a log of the startup process, see "hasErrors" and such in OperationResult
	 */
	public OperationResult start(HubResources resources) {
		this.resources = resources;
		
		OperationContext.useHubContext();
		
		// in case resources have not be initialized, do so
		// OperationResult will be set to the Hub Task Context is all is well
		OperationResult or = resources.init();
		
		if (or.hasErrors()) {
			or.exit(113, "Unable to continue, hub rescources not properly initialized");
			return or;
		}
		
		XElement config = this.resources.getConfig();
		
		List<LocaleDefinition> fallbacklocales = new ArrayList<LocaleDefinition>();
		
		String hlocale = this.resources.getDefaultLocale();
		
		fallbacklocales.add(this.resources.getLocaleDefinition(hlocale));

		// put Logger into default locale and level
		//Logger.setLocale(this.resources.getDefaultLocale());
		
		// change to overrides if find config
		XElement logger = config.find("Logger");
		
		// prepare the logger - use files, use custom log writer
		HubLog.init(logger);
		
		// initialize hub context locale and level (depends on logger above)
		OperationContext.startHubContext(config);
		
		or.boundary("Origin", "hub:", "Op", "Start");		
		
		// TODO use translation codes for all start up messages after dictionaries are loaded
		or.info(0, "Using hub id: " + OperationContext.getHubId());
		
		or.info(0, "Java version: " + System.getProperty("java.version"));
		or.info(0, "Java vendor: " + System.getProperty("java.vendor"));
		or.info(0, "Java vm: " + System.getProperty("java.vm.name"));		
		
		if (Logger.isDebug()) {
			or.debug(0, "Java clas path: " + System.getProperty("java.class.path"));
			or.debug(0, "Java home: " + System.getProperty("java.home"));
			or.debug(0, "OS: " + System.getProperty("os.name"));
			or.debug(0, "OS Ver: " + System.getProperty("os.version"));
			or.debug(0, "OS Arch: " + System.getProperty("os.arch"));
			or.debug(0, "User: " + System.getProperty("user.name"));
			or.debug(0, "User working dir: " + System.getProperty("user.dir"));
		}
		
		Security.addProvider(new BouncyCastleProvider());		
		
		or.debug(0, "Starting Clock");
		
		HubDependency bootdep = new HubDependency("Hub Boot");
		
		bootdep.setPassBoot(false);
		
		this.addDependency(bootdep);
		
		this.clock.init(or, config.find("Clock"));
		this.clock.start(or);
		
		if (or.hasErrors()) {
			or.exitCodeTr(136);
			return or;
		}
		
		//
		this.actman = new ActivityManager();
		
		// work pool prep
		
		or.debug(0, "Starting Work Pool");

		this.workpool = new WorkPool();
		
		XElement wpxel = config.find("WorkPool");
		
		this.workpool.init(or, wpxel);
		this.workpool.start(or);
		
		if (or.hasErrors()) {
			or.exitCodeTr(137);
			return or;
		}
		
		this.workqueue.init(or, config.find("WorkQueue"));
		
		if (or.hasErrors()) {
			or.exitCodeTr(175);
			return or;
		}
		
		this.workqueue.start(or);
		
		if (or.hasErrors()) {
			or.exitCodeTr(176);
			return or;
		}
		
		or.debug(0, "Initializing scheduler");

		this.scheduler.init(or, config.find("Scheduler"));
		
		if (or.hasErrors()) {
			or.exitCodeTr(138);
			return or;
		}
		
		or.debug(0, "Initializing Bus");
		
		this.ctp.init(config.find("Ctp"));
		
		// setup our bus
		this.bus.init(or, config.find("Bus"));
		
		if (or.hasErrors()) {
			or.exitCodeTr(139);
			return or;
		}
				
		try {
			this.watcher = FileSystems.getDefault().newWatchService();
		} 
		catch (IOException x1) {
			or.error("Cannot create file watcher service: " + x1);
		}
		
		// load the dc databases, if any		
		or.debug(0, "Initializing dcDatabase");
		
		// LEGACY support old tenant names
		XElement dcdbmode = config.find("dcDatabaseMode");
		
		if (dcdbmode != null) {
			if ("true".equals(dcdbmode.getAttribute("Legacy"))) {
				// tenants used to be called domains - ignore this if past version 0.8
				Constants.DB_GLOBAL_TENANT_DB = "dcDomain";
				Constants.DB_GLOBAL_TENANT_IDX_DB = "dcDomainIndex";
			}
		}
		
		// start the database
		XElement dcdb = config.find("dcDatabase");
		
		if (dcdb != null) {
			String cname = dcdb.getAttribute("Class", "dcraft.db.rocks.DatabaseManager");
			
			try {
				Class<?> dbclass = Class.forName(cname);				
				this.db = (IDatabaseManager) dbclass.newInstance();
				this.db.init(dcdb);
				this.db.start();
			} 
			catch (Exception x) {
				or.error("Unable to load/start database class: " + x);
			}
			
			if (or.hasErrors()) {
				or.exitCodeTr(146);		// TODO fix code to dcdb code
				return or;
			}
		}
		
		// load the sql databases, if any
		
		or.debug(0, "Initializing SQL Database Manager");
		
		this.sqldbman.init(or, config.find("SQLDatabases"));
		
		if (or.hasErrors()) {
			or.exitCodeTr(146);
			return or;
		}
		
		or.debug(0, "Initializing package file store");		
		
		this.resources.getPackages().init(or, config.find("PackageFileStore"));
		
		XElement fstore = config.find("TenantsFileStore");
		
		if (fstore == null) {
			fstore = new XElement("TenantsFileStore");
			config.with(fstore);
		}
		
		this.tenantsfilestore = new LocalFileStore();
		or.debug(0, "Initializing tenants file store");		
		this.tenantsfilestore.init(or, fstore);		
		
		if (or.hasErrors()) {
			or.exitCodeTr(141);
			return or;
		}
		
		Path fqueue = this.tenantsfilestore.resolvePath("/_fqueue");

		try {
			Files.createDirectories(fqueue);
			
			this.tenantsfilequeuekey = Hub.instance.registerFileWatcher(new IFileWatcher() {
				@Override
				public void fireFolderEvent(Path fname, Kind<Path> kind) {
					if (fname.toString().equals("CMD") && (kind != ENTRY_DELETE)) {
						Path fpath = Hub.this.tenantsfilestore.resolvePath("/_fqueue/" + fname);
						
						try {
							String msg = StringUtil.stripWhitespace(IOUtil.readEntireFile2(fpath).toString());
							
							Files.delete(fpath);
							
							String cmd = msg.substring(0, msg.indexOf('='));
							String exp = msg.substring(msg.indexOf('=') + 1);
							
							if (cmd.equals("REFRESH")) {
								// clear all cache
								if (exp.equals("_core")) {
									Logger.info("Clearing the resource and tenant filestores cache");
									
									Hub.this.resources.getPackages().getPackageFileStore().clearCache();
									Hub.this.tenantsfilestore.clearCache();
								}
								// reload just one tenant
								else {
									TenantInfo t = Hub.instance.tenantman.resolveTenantInfo(exp);
									
									if (t != null)
										Hub.instance.getWorkPool().submit(t.reloadSettingsTask());
								}
							}
						}
						catch (Exception x) {
							System.out.println("failed fqueue file: " + fname);
						}						
					}
				}
			}, fqueue);
		}
		catch (Exception x) {
			or.exitCodeTr(141);
			return or;
		}
		
		// sessions 
		or.debug(0, "Initializing local session manager");		
		
		this.sessions.init(or, config.find("Sessions"));
		this.bus.getLocalHub().registerService(this.sessions);		
		
		if (or.hasErrors()) {
			or.exitCodeTr(142);
			return or;
		}
		
		// CountManager - initialize late, may depend on other features
		or.debug(0, "Initializing count/stats manager");		
		
		this.countman.init(or, config.find("CountManager"));
		
		if (or.hasErrors()) {
			or.exitCodeTr(193);
			return or;
		}
		
		MimeUtil.load(config.find("MimeDefs"));
		
		or.debug(0, "Loading modules");
		
		for (XElement el : config.selectAll("Module")) {
			or.info(0, "Loading module: " + el.getAttribute("Name"));
			
			ModuleLoader loader = ModuleLoader.from(el);
			this.modules.put(loader.getName(), loader);
			this.orderedModules.add(loader);
			loader.start();
		}
		
		if (or.hasErrors()) {
			or.exitCodeTr(143);
			return or;
		}
		
		or.debug(0, "Starting scheduler");
		
		this.scheduler.start(or);
		
		if (or.hasErrors()) {
			or.exitCodeTr(147);
			return or;
		}

		/* was a slick solution, but just not in the place
		if (this.resources.isGateway()) {
			HubDependency conndep = new HubDependency("Gateway");
			conndep.setPassConnected(false);
			
			this.subscribeToEvent(HubEvents.BusConnected, e -> {
				conndep.setPassConnected(this.bus.isConnected());
			});
			
			this.subscribeToEvent(HubEvents.BusDisconnected, e -> {
				conndep.setPassConnected(this.bus.isConnected());
			});
			
			this.addDependency(conndep);
		}
		*/
		
		ISystemWork filefolderwatcher = new ISystemWork() {
			@SuppressWarnings("unchecked")
			@Override
			public void run(SysReporter reporter) {
			    // wait for key to be signaled
			    WatchKey key = Hub.this.watcher.poll();
			    
			    if (key == null)
			    	return;
			    
			    //System.out.println("Watch key count: " + Hub.instance.filewatchdata.size());
			    
		        IFileWatcher fw = Hub.this.filewatchdata.get(key);
			    
			    if (fw == null)
			    	return;
		        
			    for (WatchEvent<?> event: key.pollEvents()) {
			        WatchEvent.Kind<?> kind = event.kind();

					WatchEvent<Path> ev = (WatchEvent<Path>)event;
			        Path filename = ev.context();

			        System.out.println("detected " + filename + " : " + kind);
			        
			        fw.fireFolderEvent(filename, (Kind<Path>) kind); 
			    }

			    // Reset the key -- this step is critical if you want to
			    // receive further watch events.  If the key is no longer valid,
			    // the directory is inaccessible so exit the loop.
			    if (! key.reset()) 
			    	Hub.instance.filewatchdata.remove(key);
			}

			@Override
			public int period() {
				return 1;
			}
		};
		
		this.clock.addSlowSystemWorker(filefolderwatcher);		
			
		// TODO review if this even works...
		// every five minutes run cleanup to remove expired temp files
		// also cleanup hub/default operating contexts
		ISystemWork cleanexpiredtemp = new ISystemWork() {
				@Override
				public void run(SysReporter reporter) {
					reporter.setStatus("Cleaning contexts and temp files");
					
					if (!Hub.instance.isStopping())  {
						FileUtil.cleanupTemp();
					
						IDatabaseManager db = Hub.instance.getDatabase();
						
						if (db != null) {
							RecordStruct params = new RecordStruct(
									new FieldStruct("ExpireThreshold", new DateTime().minusMinutes(5)),
									new FieldStruct("LongExpireThreshold", new DateTime().minusHours(72))
							);
							
							db.submit(new DataRequest("dcCleanup").withParams(params), new ObjectResult() {
								@Override
								public void process(CompositeStruct result) {
									if (this.hasErrors())
										Logger.errorTr(114);
								}
							});
						}
					}
					
					reporter.setStatus("After cleaning contexts and temp files");
				}

				@Override
				public int period() {
					return 300;
				}
		};
		
		this.clock.addSlowSystemWorker(cleanexpiredtemp);		
		
		// monitor the Hub/Java/Core counters
		ISystemWork monitorcounters = new ISystemWork() {			
			@Override
			public void run(SysReporter reporter) {
				reporter.setStatus("Updating hub counters");
				
				Hub h = Hub.instance;
				CountManager cm = h.getCountManager();
				
				h.getSessions().recordCounters();
				
				if (! cm.hasCounter("dcRunId"))
					cm.allocateSetStringCounter("dcRunId", OperationContext.getRunId());
				
				//long st = System.currentTimeMillis();
				
				ClassLoadingMXBean clbean = ManagementFactory.getClassLoadingMXBean();
				
				cm.allocateSetNumberCounter("javaClassCount", clbean.getLoadedClassCount());
				cm.allocateSetNumberCounter("javaClassLoads", clbean.getTotalLoadedClassCount());
				cm.allocateSetNumberCounter("javaClassUnloads", clbean.getUnloadedClassCount());
				
				CompilationMXBean cpbean = ManagementFactory.getCompilationMXBean();

				if (cpbean != null)
					cm.allocateSetNumberCounter("javaCompileTime", cpbean.getTotalCompilationTime());
				
				MemoryMXBean mebean = ManagementFactory.getMemoryMXBean();
				
				cm.allocateSetNumberCounter("javaMemoryHeapCommitted", mebean.getHeapMemoryUsage().getCommitted());
				cm.allocateSetNumberCounter("javaMemoryHeapUsed", mebean.getHeapMemoryUsage().getUsed());
				cm.allocateSetNumberCounter("javaMemoryHeapInit", mebean.getHeapMemoryUsage().getInit());
				cm.allocateSetNumberCounter("javaMemoryHeapMax", mebean.getHeapMemoryUsage().getMax());
				cm.allocateSetNumberCounter("javaMemoryNonHeapCommitted", mebean.getNonHeapMemoryUsage().getCommitted());
				cm.allocateSetNumberCounter("javaMemoryNonHeapUsed", mebean.getNonHeapMemoryUsage().getUsed());
				cm.allocateSetNumberCounter("javaMemoryNonHeapInit", mebean.getNonHeapMemoryUsage().getInit());
				cm.allocateSetNumberCounter("javaMemoryNonHeapMax", mebean.getNonHeapMemoryUsage().getMax());
				cm.allocateSetNumberCounter("javaMemoryFinals", mebean.getObjectPendingFinalizationCount());
				
				List<GarbageCollectorMXBean> gcbeans = ManagementFactory.getGarbageCollectorMXBeans();
				long collects = 0;
				long collecttime = 0;
				
				for (GarbageCollectorMXBean gcbean : gcbeans) {
					collects += gcbean.getCollectionCount();
					collecttime += gcbean.getCollectionTime();
				}
				
				cm.allocateSetNumberCounter("javaGarbageCollects", collects);
				cm.allocateSetNumberCounter("javaGarbageTime", collecttime);
				
				OperatingSystemMXBean osbean = ManagementFactory.getOperatingSystemMXBean();
				
				cm.allocateSetNumberCounter("javaSystemLoadAverage", osbean.getSystemLoadAverage());
				
				RuntimeMXBean rtbean = ManagementFactory.getRuntimeMXBean();
				
				cm.allocateSetNumberCounter("javaJvmUptime", rtbean.getUptime());
				
				ThreadMXBean thbean = ManagementFactory.getThreadMXBean();
				
				cm.allocateSetNumberCounter("javaJvmRunningDaemonThreads", thbean.getDaemonThreadCount());
				cm.allocateSetNumberCounter("javaJvmRunningPeakThreads", thbean.getPeakThreadCount());
				cm.allocateSetNumberCounter("javaJvmRunningThreads", thbean.getThreadCount());
				cm.allocateSetNumberCounter("javaJvmStartedThreads", thbean.getTotalStartedThreadCount());
				
				//System.out.println("collect: " + (System.currentTimeMillis() - st));
				
				//System.out.println("reply count: " + Hub.instance.getCountManager().getCounter("dcBusReplyHandlers"));
				
				reporter.setStatus("After reviewing hub counters");
			}

			@Override
			public int period() {
				return 300;
			}
		};
		
		Hub.instance.getClock().addSlowSystemWorker(monitorcounters);
		
		/*
		Hub.instance.getClock().addSlowSystemWorker(new ISystemWork() {
			@Override
			public void run(SysReporter reporter) {
				PooledByteBufAllocator ba = (PooledByteBufAllocator) Hub.this.bufferAllocator;
				
				System.out.println("----");
				
				for (PoolArenaMetric ha : ba.heapArenas()) {
					System.out.println("ha " + ha.numAllocations() + " : " + ha.numDeallocations() + " - " + ha.numActiveAllocations() + " - " + ha.numActiveBytes());
					
					for (PoolChunkListMetric cl : ha.chunkLists()) {
						System.out.println("  - cl " + cl.minUsage() + " : " + cl.maxUsage());
						
						Iterator<PoolChunkMetric> pci = cl.iterator();
						
						while (pci.hasNext()) {
							PoolChunkMetric pcm = pci.next();
							
							System.out.println("    - pcm " + pcm.chunkSize() + " : " + pcm.freeBytes());
						}
					}
					
					for (PoolSubpageMetric psm : ha.smallSubpages()) {
						System.out.println("  - psms " + psm.pageSize() + " : " + psm.elementSize() + " : " +psm.numAvailable());
					}
					
					for (PoolSubpageMetric psm : ha.tinySubpages()) {
						System.out.println("  - psmt " + psm.pageSize() + " : " + psm.elementSize() + " : " +psm.numAvailable());
					}
				}
				
				for (PoolArenaMetric ha : ba.directArenas()) {
					System.out.println("da " + ha.numAllocations() + " : " + ha.numDeallocations() + " - " + ha.numActiveAllocations());
					
					for (PoolChunkListMetric cl : ha.chunkLists()) {
						System.out.println("  - cl " + cl.minUsage() + " : " + cl.maxUsage());
						
						Iterator<PoolChunkMetric> pci = cl.iterator();
						
						while (pci.hasNext()) {
							PoolChunkMetric pcm = pci.next();
							
							System.out.println("    - pcm " + pcm.chunkSize() + " : " + pcm.freeBytes());
						}
					}
					
					for (PoolSubpageMetric psm : ha.smallSubpages()) {
						System.out.println("  - psms " + psm.pageSize() + " : " + psm.elementSize() + " : " +psm.numAvailable());
					}
					
					for (PoolSubpageMetric psm : ha.tinySubpages()) {
						System.out.println("  - psmt " + psm.pageSize() + " : " + psm.elementSize() + " : " +psm.numAvailable());
					}
				}
			}
			
			@Override
			public int period() {
				return 1;
			}
		});
		*/
		
		//System.out.println("allocator: " + Hub.this.bufferAllocator.getClass().getCanonicalName());
		
		this.removeDependency(bootdep.source);
		
		or.boundary("Origin", "hub:", "Op", "Run");
		
		return or;
	}

	/**
	 * Gracefully stop the scheduler, database connections, workpool, logger and such
	 * 
	 * @return a log of the shutdown process
	 */
	public OperationResult stop() {
		// tell surface modules and user tasks not to accept new requests - enter a slow and careful come down
		this.state = HubState.Stopping;
		
		Logger.info("Hub entered Stopping state");
		
		this.fireEvent(HubEvents.Stopping, null);
		
		OperationContext.useHubContext();

		OperationResult or = new OperationResult();
		
		or.boundary("Origin", "hub:", "Op", "Stop");
		
		or.info(0, "Stopping hub");
		
		if (this.tenantsfilequeuekey != null) {
			this.tenantsfilequeuekey.cancel();
			this.tenantsfilequeuekey = null;
		}
		
		or.info(0, "Waiting on Primary Tasks");
		
		// wait up to 5 minutes -- TODO configure
		for (int i = 0; i < 300; i++) {
			if (this.sessions.countIncompleteTasks() == 0)
				break;
			
			try {
				Thread.sleep(1000);
			}
			catch (Exception x) {				
			}
			
			if (i % 30 == 29) 
				or.info(0, "Still Waiting on Primary Tasks");
		}
		
		or.debug(0, "Stopping bus matrix");
		
		this.ctp.stopMatrix();
		
		// will wait up to 2 seconds for each session to close (should be faster)
		this.bus.stopMatrix(or);
		
		or.debug(0, "Stopping scheduler");
		
		this.scheduler.stop(or);
		
		or.debug(0, "Stopping work queue");
		
		this.workqueue.stop(or);
		
		or.debug(0, "Stopping modules");
		
		for (int i = this.orderedModules.size() - 1; i >= 0; i--) {
			ModuleLoader mod = this.orderedModules.get(i);			
			or.info(0, "Stopping module: " + mod.getName());			
			mod.stop();
		}
		
		or.debug(0, "Stopping count manager");
		this.countman.stop(or);
		
		or.debug(0, "Stopping work pool");
		
		// let everyone know it is time to stop
		this.workpool.stop(or);
		
		// give just a little time for everything to cleanup
		try {
			Thread.sleep(500);
		} 
		catch (InterruptedException x) {
		}
		
		or.debug(0, "Stopping SQL Database Manager");		
		
		this.sqldbman.stop();
		
		if (this.db != null) {
			or.debug(0, "Stopping dcDatabase");
			
			this.db.stop();
		}
		
		or.debug(0, "Stopping bus");
		
		this.bus.stopFinal(or);
		
		try {
			if (this.eventLoopGroup != null)
				this.eventLoopGroup.shutdownGracefully().await();
		} 
		catch (InterruptedException x) {
		}
		
		or.debug(0, "Stopping clock");
		
		this.clock.stop(or);

		// find and list any threads from our pools that linger beyond the shut down
		Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
		Thread[] threadArray = threadSet.toArray(new Thread[threadSet.size()]);		
		
		for (Thread t : threadArray) {
			if ((t.getThreadGroup() == null) || !"main".equals(t.getThreadGroup().getName()))
				continue;
			
			boolean fnd = false;
			
			if (t.getName().startsWith("WorkPool")) 
				fnd = true;
			
			if (fnd) {
				Logger.info("Lingering Thread: " + t.getName());
				
				StackTraceElement[] g = t.getStackTrace();
				
				if (g.length > 3) {
					Logger.info(" - " + g[0]);
					Logger.info(" - " + g[1]);
					Logger.info(" - " + g[2]);
					Logger.info(" - " + g[3]);
				}
			}
		}
		
		or.debug(0, "Stopping logger");		
		HubLog.stop(or);
		
		or.info(0, "Hub stopped");
		
		return or;
	}
	
	/**
	 * Please don't use this, it goes against design principles of dcraft.
	 * 
	 * @param name of the module desired
	 * 
	 * @return the module if loaded
	 */
	public IModule getModule(String name) {
		ModuleLoader ml = this.modules.get(name);
		
		if (ml != null)
			return ml.getModule();
		
		return null;
	}
	
	public void subscribeToEvent(Integer event, IEventSubscriber sub) {
		this.subscribers.putIfAbsent(event, new HashSet<IEventSubscriber>());
		
		Set<IEventSubscriber> list = this.subscribers.get(event);
		
		list.add(sub);
	}
	
	public void unsubscribeFromEvent(Integer event, IEventSubscriber sub) {
		Set<IEventSubscriber> list = this.subscribers.get(event);
		
		if (list != null)
			list.remove(sub);
	}
	
	public void fireEvent(Integer event, Object e) {
		if (Logger.isDebug())
			Logger.debug("Hub Event fired: " + event + " with " + e);		
		
		Set<IEventSubscriber> list = this.subscribers.get(event);
		
		if (list == null)
			return;
		
		// to array to be thread safe
		for (IEventSubscriber sub : list.toArray(new IEventSubscriber[list.size()])) {
			try {
				sub.eventFired(e);
			}
			catch (Exception x) {
				Logger.warn("Event subscriber threw an error: " + x);
			}
		}
	}
	
	public String getLibraryPath(String libraryName, String alias) {
		if (this.libpaths == null) {
			this.libpaths = System.getProperty("java.class.path").split(";");
			
			// if this is UNIX/Linux then split on ':' instead
			if (this.libpaths.length == 1) 
				this.libpaths = System.getProperty("java.class.path").split(":");
		}
		
		String retpath = null;
		
		for (String path : this.libpaths) {
			if (path.contains(File.separatorChar + libraryName + ".jar")) {
				retpath = path;
				break;
			}
			
			if (path.contains(File.separatorChar + libraryName + File.separatorChar)) {
				retpath = path + "/";
				break;
			}
		}
		
		if (retpath == null) {
			try {
				// try some predictable places
				File proj = new File("./" + libraryName + "/bin");
				
				if (proj.exists()) 
					retpath = proj.getCanonicalPath() + "/";
				else {
					File jar = new File("./lib/" + libraryName + ".jar");
					
					if (jar.exists()) 
						retpath = jar.getCanonicalPath();
				}
			}
			catch (Exception x) {
				
			}
		}
		
		if (retpath != null)
			retpath = retpath.replace("\\", "/");
		
		return retpath;
	}

	public ApiSession createLocalApiSession(String domain) {
		IApiSessionFactory man = this.apimans.get("_local");
		
		if (man == null) {
			man = (IApiSessionFactory) this.getInstance("dcraft.api.LocalSessionFactory");
			this.apimans.put("_local", man);						
		}
		
		return man.create(new XElement("ApiSession", new XAttribute("Domain", domain)));
	}
	
	public ApiSession createApiSession(String name) {
		IApiSessionFactory man = this.apimans.get(name);
		
		if (man == null) {
			for (XElement mel : this.getConfig().selectAll("ApiSessions/ApiSession")) {
				if (mel.getAttribute("Name").equals(name)) {
					String cls = mel.getAttribute("Class");
					
					if (StringUtil.isEmpty(cls))
						break;
					
					man = (IApiSessionFactory) this.getInstance(cls);
				
					if (man != null) {
						man.init(mel);
						this.apimans.put(name, man);						
					}
					
					break;
				}
			}
		}
		
		if (man != null)
			return man.create();
		
		return null;
	}
	
	public WatchKey registerFileWatcher(IFileWatcher fwatcher, Path path) {
		try {
			WatchKey watchID = path.register(this.watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
			
			this.filewatchdata.put(watchID, fwatcher);
		    
		    //System.out.println("register - Watch key count: " + Hub.instance.filewatchdata.size());
			
			return watchID;
		}
		catch (Exception x) {
			Logger.error("Hub file watcher error: " + x);
		}
		
		return null;
	}
	
	public void unregisterFileWatcher(WatchKey key) {
		if (key == null)
			return;
		
		key.cancel();
		
		this.filewatchdata.remove(key);
	    
	    //System.out.println("unregister - Watch key count: " + Hub.instance.filewatchdata.size());
	}
	
	// TODO add hub info/detail collector service
	// System.out.println("Boss Threads: " + this.getBossGroup().isShutdown() + " - " + this.getBossGroup().isShuttingDown() + " - " + this.getBossGroup().isTerminated());
	
	public Object getInstance(String cname) {
		try {
			return this.getClass(cname).newInstance();
		} 
		catch (Exception x) {
		}
		
		return null;
	}
	
	public Class<?> getClass(String cname) {
		try {
			return Class.forName(cname);
		} 
		catch (Exception x) {
		}
		
		return null;
	}
}
