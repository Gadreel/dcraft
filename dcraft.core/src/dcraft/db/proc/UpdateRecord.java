package dcraft.db.proc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseTask;
import dcraft.db.IStoredProc;
import dcraft.db.TablesAdapter;
import dcraft.lang.BigDateTime;
import dcraft.lang.op.FuncResult;
import dcraft.lang.op.OperationResult;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.struct.builder.ICompositeBuilder;
import dcraft.util.StringUtil;

public class UpdateRecord implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		boolean isUpdate = task.getName().equals("dcUpdateRecord");

		RecordStruct params = task.getParamsAsRecord();
		String table = params.getFieldAsString("Table");
		
		TablesAdapter db = new TablesAdapter(conn, task); 
		
		// ===========================================
		//  verify the fields
		// ===========================================
		
		RecordStruct fields = params.getFieldAsRecord("Fields");
		BigDateTime when = params.getFieldAsBigDateTime("When");
		
		if (when == null)
			when = BigDateTime.nowDateTime();
		
		if (!task.isReplicating()) {
			// only check first time, otherwise allow replication
			OperationResult cor = db.checkFields(table, fields, params.getFieldAsString("Id"));
			
			if (cor.hasErrors()) {
				task.complete();
				return;
			}
		}
		
		// ===========================================
		//  run before trigger
		// ===========================================
		OperationResult cor = db.executeTrigger(table, isUpdate ? "BeforeUpdate" : "BeforeInsert", conn, task, log);
		
		if (cor.hasErrors()) {
			task.complete();
			return;
		}
		
		// it is possible for Id to be set by trigger (e.g. with domains)
		String id = params.getFieldAsString("Id");
		
		// TODO add db filter option
		//d runFilter("Insert" or "Update") quit:Errors  ; if any violations in filter then do not proceed
		
		// ===========================================
		//  create new id
		// ===========================================
		
		// don't create a new id during replication - not even for dcInsertRecord
		if (StringUtil.isEmpty(id)) {
			FuncResult<String> ires = db.createRecord(table);
			
			if (ires.hasErrors()) {
				task.complete();
				return;
			}
			
			id = ires.getResult();
			
			params.setField("Id", id);
		}

		// ===========================================
		//  do the data update
		// ===========================================
		db.setFields(table, id, fields);
		
		// ===========================================
		//  and set fields
		// ===========================================

		// TODO move to tables interface
		if (params.hasField("Sets")) {
			ListStruct sets = params.getFieldAsList("Sets");
			
			for (Struct set : sets.getItems()) {
				RecordStruct rset = (RecordStruct) set;
				
				String field = rset.getFieldAsString("Field");
				
				// make a copy
				List<String> lsubids = rset.getFieldAsList("Values").toStringList();
				List<String> othersubids = new ArrayList<>();
				
				db.traverseSubIds(table, id, field, when, false, new Function<Object,Boolean>() {			
					@Override
					public Boolean apply(Object msub) {
						String suid = msub.toString();
					
						// if a value is already set, don't set it again
						if (!lsubids.remove(suid))
							othersubids.add(suid);		
						
						return true;
					}
				});
		
				// Retire non matches
				for (String suid : othersubids) {
					// if present in our list then retire it
					db.setFields(table, id, new RecordStruct()
						.withField(field, new RecordStruct()
							.withField(suid, new RecordStruct()
								.withField("Retired", true)
							)
						)
					);
				}
				
				// add any remaining - unmatched - suids
				for (String suid : lsubids) {
					// if present in our list then retire it
					db.setFields(table, id, new RecordStruct()
						.withField(field, new RecordStruct()
							.withField(suid, new RecordStruct()
								.withField("Data", suid)
							)
						)
					);
				}
			}
		}
		
		// TODO make a record of everything for replication? or just let it figure it out?
		
		// ===========================================
		//  run after trigger
		// ===========================================
		cor = db.executeTrigger(table, isUpdate ? "AfterUpdate" : "AfterInsert", conn, task, log);
		
		if (cor.hasErrors()) {
			task.complete();
			return;
		}
		
		// ===========================================
		//  return results
		// ===========================================
		
		// don't bother returning data during replication 
		if (!isUpdate && !task.isReplicating()) {
			ICompositeBuilder resp = task.getBuilder();
			
			try {
				resp.startRecord();
				resp.field("Id", id);
				resp.endRecord();

				/* alternative solution
				RecordStruct rec = new RecordStruct(new FieldStruct("Id", id));
				rec.toBuilder(resp);
				*/
			}
			catch (Exception x) {
				log.error("UpdateRecord: Unable to create response: " + x);
			}
		}
		
		task.complete();
	}
}
