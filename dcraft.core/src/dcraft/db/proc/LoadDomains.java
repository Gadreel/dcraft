package dcraft.db.proc;

import static dcraft.db.Constants.*;

import java.util.function.Function;

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

public class LoadDomains extends LoadRecord {
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
		
		// some options don't load for gateways - gateways are not allowed to obscure on a per domain basis so they don't 
		// get to know that info (they will get a default but it doesn't apply to real data for the domain)
		
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
			Logger.debug("Load Domains proc called");
		
		BigDateTime when = BigDateTime.nowDateTime();
		
		try {
			out.startList();
			
			db.traverseSubIds("dcDomain", DB_GLOBAL_ROOT_DOMAIN, "dcDomainIndex", when, false, new Function<Object,Boolean>() {				
				@Override
				public Boolean apply(Object t) {
					String did = t.toString();
					
					if (Logger.isDebug())
						Logger.debug("Load Domains found: " + did);
					
					task.pushDomain(did);
					
					try {
						LoadDomains.this.writeRecord(conn, task, log, out, db, "dcDomain",
								did, when, select, true, false, false);
						
						return true;
					}
					catch (Exception x) {
						log.error("LoadDomainsProc: Unable to create resp 2: " + x);
					}
					finally {
						task.popDomain();
					}
					
					return false;
				}
			});
			
			out.endList();
		}
		catch (Exception x) {
			log.error("LoadDomainsProc: Unable to create resp: " + x);
		}
		
		task.complete();
	}
}
