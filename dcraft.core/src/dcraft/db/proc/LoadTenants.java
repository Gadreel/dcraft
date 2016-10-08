package dcraft.db.proc;

import static dcraft.db.Constants.*;

import java.util.function.Function;

import dcraft.db.Constants;
import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseTask;
import dcraft.db.TablesAdapter;
import dcraft.lang.BigDateTime;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.OperationResult;
import dcraft.log.Logger;
import dcraft.struct.FieldStruct;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.builder.ICompositeBuilder;

public class LoadTenants extends LoadRecord {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		TablesAdapter db = new TablesAdapter(conn, task); 
		
		ICompositeBuilder out = task.getBuilder();
		
		ListStruct select = new ListStruct(
				new RecordStruct(
						new FieldStruct("Field", "Id")
				),
				new RecordStruct(
						new FieldStruct("Field", "dcTitle"),
						new FieldStruct("Name", "Title")
				),
				new RecordStruct(
						new FieldStruct("Field", "dcAlias"),
						new FieldStruct("Name", "Alias")
				),
				new RecordStruct(
						new FieldStruct("Field", "dcName"),
						new FieldStruct("Name", "Names")
				)
		);		
		
		// some options don't load for gateways - gateways are not allowed to obscure on a per Tenant basis so they don't 
		// get to know that info (they will get a default but it doesn't apply to real data for the Tenant)
		
		if (!OperationContext.get().isGateway()) {
			select.addItem(
				new RecordStruct(
						new FieldStruct("Field", "dcObscureClass"),
						new FieldStruct("Name", "ObscureClass")
				),
				new RecordStruct(
						new FieldStruct("Field", "dcObscureSeed"),
						new FieldStruct("Name", "ObscureSeed")
				)
			);
		}
		
		if (Logger.isDebug())
			Logger.debug("Load Tenants proc called");
		
		BigDateTime when = BigDateTime.nowDateTime();
		
		try {
			out.startList();
			
			db.traverseSubIds(DB_GLOBAL_TENANT_DB, DB_GLOBAL_ROOT_TENANT, Constants.DB_GLOBAL_TENANT_IDX_DB, when, false, new Function<Object,Boolean>() {				
				@Override
				public Boolean apply(Object t) {
					String did = t.toString();
					
					if (Logger.isDebug())
						Logger.debug("Load Tenants found: " + did);
					
					task.pushTenant(did);
					
					try {
						LoadTenants.this.writeRecord(conn, task, log, out, db, DB_GLOBAL_TENANT_DB,
								did, when, select, true, false, false);
						
						return true;
					}
					catch (Exception x) {
						log.error("LoadTenantsProc: Unable to create resp 2: " + x);
					}
					finally {
						task.popTenant();
					}
					
					return false;
				}
			});
			
			out.endList();
		}
		catch (Exception x) {
			log.error("LoadTenantsProc: Unable to create resp: " + x);
		}
		
		task.complete();
	}
}
