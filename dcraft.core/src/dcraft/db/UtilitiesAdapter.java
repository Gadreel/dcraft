package dcraft.db;

import static dcraft.db.Constants.*;

import java.util.function.Function;

import dcraft.db.rocks.DatabaseManager;
import dcraft.hub.TenantManager;
import dcraft.lang.BigDateTime;
import dcraft.lang.op.OperationContext;
import dcraft.struct.RecordStruct;

public class UtilitiesAdapter {
	protected DatabaseManager db = null;
	protected DatabaseInterface conn = null;
	protected DatabaseTask task = null;
	protected TenantManager dm = null;
	protected TablesAdapter tables = null;
	
	// don't call for general code...
	public UtilitiesAdapter(DatabaseManager db, TenantManager dm) {
		this.db = db;
		this.dm = dm;
		this.conn = db.allocateAdapter();
		
		RecordStruct req = new RecordStruct();
		
		req.setField("Replicate", false);		// means this should replicate, where as Replicating means we are doing replication currently
		req.setField("Name", "dcRebuildIndexes");
		req.setField("Stamp", this.db.allocateStamp(0));
		req.setField("Tenant", DB_GLOBAL_ROOT_TENANT);
		
		this.task = new DatabaseTask();
		this.task.setRequest(req);
		
		this.tables = new TablesAdapter(conn, task);
	}
	
	public void rebuildIndexes() {
		TablesAdapter ta = new TablesAdapter(conn, task); 
		BigDateTime when = BigDateTime.nowDateTime();
		
		ta.traverseSubIds(DB_GLOBAL_TENANT_DB, DB_GLOBAL_ROOT_TENANT, Constants.DB_GLOBAL_TENANT_IDX_DB, when, false, new Function<Object,Boolean>() {				
			@Override
			public Boolean apply(Object t) {
				String did = t.toString();
				
				System.out.println("Indexing domain: " + did);
				
				task.pushTenant(did);
				
				try {
					// see if there is even such a table in the schema
					tables.rebuildIndexes(dm.getTenantInfo(did), when);
					
					return true;
				}
				catch (Exception x) {
					System.out.println("dcRebuildIndexes: Unable to index: " + did);
					OperationContext.get().error("rebuildTenantIndexes error: " + x);
				}
				finally {
					task.popTenant();
				}
				
				return false;
			}
		});
	}
}
