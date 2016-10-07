package dcraft.db;

import static dcraft.db.Constants.*;

import java.util.function.Function;

import dcraft.db.rocks.DatabaseManager;
import dcraft.hub.DomainsManager;
import dcraft.lang.BigDateTime;
import dcraft.lang.op.OperationContext;
import dcraft.struct.RecordStruct;

public class UtilitiesAdapter {
	protected DatabaseManager db = null;
	protected DatabaseInterface conn = null;
	protected DatabaseTask task = null;
	protected DomainsManager dm = null;
	protected TablesAdapter tables = null;
	
	// don't call for general code...
	public UtilitiesAdapter(DatabaseManager db, DomainsManager dm) {
		this.db = db;
		this.dm = dm;
		this.conn = db.allocateAdapter();
		
		RecordStruct req = new RecordStruct();
		
		req.setField("Replicate", false);		// means this should replicate, where as Replicating means we are doing replication currently
		req.setField("Name", "dcRebuildIndexes");
		req.setField("Stamp", this.db.allocateStamp(0));
		req.setField("Domain", DB_GLOBAL_ROOT_DOMAIN);
		
		this.task = new DatabaseTask();
		this.task.setRequest(req);
		
		this.tables = new TablesAdapter(conn, task);
	}
	
	public void rebuildIndexes() {
		TablesAdapter ta = new TablesAdapter(conn, task); 
		BigDateTime when = BigDateTime.nowDateTime();
		
		ta.traverseSubIds("dcDomain", DB_GLOBAL_ROOT_DOMAIN, "dcDomainIndex", when, false, new Function<Object,Boolean>() {				
			@Override
			public Boolean apply(Object t) {
				String did = t.toString();
				
				System.out.println("Indexing domain: " + did);
				
				task.pushDomain(did);
				
				try {
					// see if there is even such a table in the schema
					tables.rebuildIndexes(dm.getDomainInfo(did), when);
					
					return true;
				}
				catch (Exception x) {
					System.out.println("dcRebuildIndexes: Unable to index: " + did);
					OperationContext.get().error("rebuildDomainIndexes error: " + x);
				}
				finally {
					task.popDomain();
				}
				
				return false;
			}
		});
	}
}
