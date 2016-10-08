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
	protected DomainNameMapping<TenantInfo> dnamemap = new DomainNameMapping<>();
	protected ConcurrentHashMap<String, TenantInfo> dsitemap = new ConcurrentHashMap<>();
	
	public Collection<TenantInfo> getTenants() {
		return this.dsitemap.values();
	}

	public void dumpTenantNames() {
		// TODO this.dnamemap.dumpTenantNames();
	}
	
	public String resolveTenantId(String domain) {
		if (StringUtil.isEmpty(domain)) 
			return null;
		
		// if this is a domain id then return it
		if (this.dsitemap.containsKey(domain))
			return domain;

		// if not an id then try lookup of domain name
		TenantInfo di = this.dnamemap.get(domain);
		
		if (di != null)
			return di.getId();
		
		return null;
	}
	
	public TenantInfo resolveTenantInfo(String domain) {
		if (StringUtil.isEmpty(domain)) 
			return null;
		
		// if this is a domain id then return it
		TenantInfo di = this.dsitemap.get(domain);
		
		if (di != null)
			return di;

		// if not an id then try lookup of domain name
		di = this.dnamemap.get(domain);
		
		if (di != null)
			return di;
		
		return null;
	}
		
	public TenantInfo getTenantInfo(String id) {
		if (StringUtil.isEmpty(id))
			return null;
		
		return this.dsitemap.get(id);
	}
	
	public void updateTenantRecord(String did, RecordStruct drec) {
		TenantInfo di = TenantManager.this.dsitemap.get(did);
		
		// update old
		if (di != null) {
			ListStruct names = di.getNames();

			if (names != null)
				for (Struct dn : names.getItems()) {
					String n = Struct.objectToCharsStrict(dn).toString();
					TenantManager.this.dnamemap.remove(n);
				}
		}
		// insert new
		else {
			di = TenantInfo.from(drec);
			TenantManager.this.dsitemap.put(did, di);
		}
		
		di.load(drec);
		
		ListStruct names = di.getNames();

		if (names != null)
			for (Struct dn : names.getItems()) {
				String n = Struct.objectToCharsStrict(dn).toString();
				TenantManager.this.dnamemap.add(n, di);
			}
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
		if (Hub.instance.getTenantsFileStore() != null) { 
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
					
					// only notify on config updates
					if (p.getNameCount() < 4) 
						return;
					
					// must be inside a domain or we don't care
					String mod = p.getName(0);
					String domain = p.getName(1);
					String section = p.getName(2);
					
					if ("dcw".equals(mod)) {
						for (TenantInfo wdomain : TenantManager.this.dsitemap.values()) {
							if (domain.equals(wdomain.getAlias())) {
								if (("config".equals(section) || "services".equals(section) || "glib".equals(section) || "buckets".equals(section))) {
									wdomain.reloadSettings();
									Hub.instance.fireEvent(HubEvents.TenantConfigChanged, wdomain);
								}
								
								wdomain.fileChanged(this.getResult());								
								break;
							}
						}
					}
				}
			};
			
			Hub.instance.getTenantsFileStore().register(localfilestorecallback);
		}		
		
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
