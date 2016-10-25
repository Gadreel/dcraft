package dcraft.hub;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import dcraft.bus.Message;
import dcraft.db.DataRequest;
import dcraft.db.ObjectResult;
import dcraft.db.rocks.DatabaseManager;
import dcraft.filestore.CommonPath;
import dcraft.io.FileStoreEvent;
import dcraft.lang.op.FuncCallback;
import dcraft.lang.op.OperationCallback;
import dcraft.log.Logger;
import dcraft.struct.CompositeStruct;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.util.StringUtil;

public class TenantManager {
	// domain tracking
	protected DomainNameMapping<TenantInfo> domainmap = new DomainNameMapping<>();
	protected ConcurrentHashMap<String, TenantInfo> idmap = new ConcurrentHashMap<>();
	protected ConcurrentHashMap<String, TenantInfo> aliasmap = new ConcurrentHashMap<>();
	
	public Collection<TenantInfo> getTenants() {
		return this.idmap.values();
	}

	public void dumpTenantNames() {
		// TODO this.dnamemap.dumpTenantNames();
	}
	
	public String resolveTenantId(String domain) {
		if (StringUtil.isEmpty(domain)) 
			return null;
		
		// if this is a domain id then return it
		if (this.idmap.containsKey(domain))
			return domain;

		// if not an id then try lookup of domain name
		TenantInfo di = this.domainmap.get(domain);
		
		if (di != null)
			return di.getId();

		// if not an id then try lookup of alias
		di = this.aliasmap.get(domain);
		
		if (di != null)
			return di.getId();
		
		return null;
	}
	
	public TenantInfo resolveTenantInfo(String domain) {
		if (StringUtil.isEmpty(domain)) 
			return null;
		
		// if this is a domain id then return it
		TenantInfo di = this.idmap.get(domain);
		
		if (di != null)
			return di;

		// if not an id then try lookup of domain name
		di = this.domainmap.get(domain);
		
		if (di != null)
			return di;

		// if not an id then try lookup of domain alias
		di = this.aliasmap.get(domain);
		
		if (di != null)
			return di;
		
		return null;
	}
	
	public void updateTenantRecord(String did, RecordStruct drec) {
		TenantInfo di = TenantManager.this.idmap.get(did);
		
		// update old
		if (di != null) {
			ListStruct names = di.getNames();

			if (names != null)
				for (Struct dn : names.getItems()) {
					String n = Struct.objectToCharsStrict(dn).toString();
					this.domainmap.remove(n);
				}
		}
		// insert new
		else {
			di = TenantInfo.from(drec);
			this.idmap.put(did, di);
		}
		
		di.load(drec);

		this.aliasmap.put(di.getAlias(), di);
		
		ListStruct names = di.getNames();

		if (names != null)
			for (Struct dn : names.getItems()) {
				String n = Struct.objectToCharsStrict(dn).toString();
				this.domainmap.add(n, di);
			}
	}

	public void registerDomain(String domain, TenantInfo tenantInfo) {
		this.domainmap.add(domain, tenantInfo);
	}
	
	public void init() {
		HubDependency domdep = new HubDependency("Tenants");
		domdep.setPassRun(false);
		Hub.instance.addDependency(domdep);
		
		Hub.instance.subscribeToEvent(HubEvents.TenantAdded, new IEventSubscriber() {			
			@Override
			public void eventFired(Object e) {
				String did = (String) e;
				
				Hub.instance.getBus().sendMessage(
						(Message) new Message("dcTenants", "Manager", "Load")
							.withField("Body", new RecordStruct().withField("Id", did)), 
						result -> {
							// if this fails the hub cannot start
							if (result.hasErrors()) {
								Logger.error("Unable to load new domain into hub");
								return;
							}
							
							TenantManager.this.updateTenantRecord(did, result.getBodyAsRec());
						}
					);
			}
		});
		
		Hub.instance.subscribeToEvent(HubEvents.TenantUpdated, new IEventSubscriber() {			
			@Override
			public void eventFired(Object e) {
				String did = (String) e;
				
				Hub.instance.getBus().sendMessage(
						(Message) new Message("dcTenants", "Manager", "Load")
							.withField("Body", new RecordStruct().withField("Id", did)), 
						result -> {
							// if this fails the hub cannot start
							if (result.hasErrors()) {
								Logger.error("Unable to update domain in hub");
								return;
							}
							
							TenantManager.this.updateTenantRecord(did, result.getBodyAsRec());
						}
					);
			}
		});
		
		// register for file store events before we start any services that might listen to these events
		// we need to catch domain config change events 

		/*	Examples:
			./tenants/[domain alias]/config     holds web setting for tenant
				- settings.xml are the general settings (dcmHomePage - dcmDefaultTemplate[path]) - editable in CMS only
				- dictionary.xml is the domain level dictionary - direct edit by web dev
				- vars.json is the domain level variable store - direct edit by web dev
		*/
		
		FuncCallback<FileStoreEvent> localfilestorecallback = new FuncCallback<FileStoreEvent>() {
			@Override
			public void callback() {
				this.resetCalledFlag();
				
				CommonPath p = this.getResult().getPath();
				
				//System.out.println(p);
				
				// only notify on section updates - no notice to root of a tenant
				if (p.getNameCount() < 3) 
					return;
				
				// must be inside a domain or we don't care
				String tenant = p.getName(0);
				String section = p.getName(1);
				
				TenantInfo ten = TenantManager.this.resolveTenantInfo(tenant);
				
				if (ten != null) {
					if (("config".equals(section) || "services".equals(section) || "glib".equals(section) || "buckets".equals(section))) {
						ten.reloadSettings();
						Hub.instance.fireEvent(HubEvents.TenantConfigChanged, ten);
					}
					
					if ("feed".equals(section) || "feed-preview".equals(section)) {
						/*  TODO automatically import feeds - also do so in nightly backup task if the feed date is recent
						 * 
						Task task = new Task()
							.withWork(new IWork() {
								@Override
								public void run(TaskRun trun) {
									ImportWebsiteTool iutil = new ImportWebsiteTool();
									
									
									// TODO use domain path resolution
									iutil.importFeedFile(Paths.get("./public" + p), new OperationCallback() {
										@Override
										public void callback() {
											trun.complete();
										}
									});
								}
							})
							.withTitle("Importing feed " + p)
							.withTopic("Batch")		// only one at a time
							.withContext(new OperationContextBuilder()
								.withRootTaskTemplate()
								.withTenantId(wdomain.getId())
								.toOperationContext()
							);
						
						Hub.instance.getWorkPool().submit(task);
						 */
					}
					
					ten.fileChanged(this.getResult());	
				}
			}
		};
		
		Hub.instance.getTenantsFileStore().register(localfilestorecallback);
		
		Hub.instance.getBus().sendMessage(
			new Message("dcTenants", "Manager", "LoadAll"), 
			result -> {
				// if this fails the hub cannot start
				if (result.hasErrors()) {
					// stop if we think we are connected, but if not then wait maybe we'll connect again and trigger this load again
					if (Hub.instance.state == HubState.Connected)
						Hub.instance.stop();
					
					return;
				}
				
				ListStruct domains = result.getBodyAsList();
				
				for (Struct d : domains.getItems()) {
					RecordStruct drec = (RecordStruct) d;
					
					String did = drec.getFieldAsString("Id");
					
					TenantManager.this.updateTenantRecord(did, drec);
				}
				
				Hub.instance.removeDependency(domdep.source);
			}
		);
	}
	
	public void initFromDB(DatabaseManager db, OperationCallback callback) {
		DataRequest req = new DataRequest("dcLoadTenants").withRootTenant();	// use root for this request
	
		db.submit(req, new ObjectResult() {
			@Override
			public void process(CompositeStruct result) {
				// if this fails the hub cannot start
				if (this.hasErrors()) {
					callback.complete();
					return;
				}
				
				ListStruct domains = (ListStruct) result;
				
				for (Struct d : domains.getItems()) {
					RecordStruct drec = (RecordStruct) d;
					
					String did = drec.getFieldAsString("Id");
					
					TenantManager.this.updateTenantRecord(did, drec);
				}
				
				callback.complete();
			}
		});
	}
}
