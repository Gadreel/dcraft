package dcraft.db.proc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseTask;
import dcraft.db.IStoredProc;
import dcraft.db.TablesAdapter;
import dcraft.lang.BigDateTime;
import dcraft.lang.op.OperationResult;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;

public class UpdateSet implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		RecordStruct params = task.getParamsAsRecord();
		
		String table = params.getFieldAsString("Table");
		String field = params.getFieldAsString("Field");
		String op = params.getFieldAsString("Operation");
		
		ListStruct records = params.getFieldAsList("Records");
		ListStruct subids = params.getFieldAsList("Values");
		
		TablesAdapter db = new TablesAdapter(conn, task);
		
		BigDateTime when = BigDateTime.nowDateTime();		// TODO store in params for replication - use same when 

		for (Struct ssid : records.getItems()) {
			String id = ssid.toString();
		
			// make a copy
			List<String> lsubids = subids.toStringList();
			List<String> othersubids = new ArrayList<>();
			
			db.traverseSubIds(table, id, field, when, false, new Function<Object,Boolean>() {			
				@Override
				public Boolean apply(Object msub) {
					String suid = msub.toString();
					
					boolean fnd = lsubids.remove(suid);
					
					if (!fnd)
						othersubids.add(suid);
					
					if ("RemoveFromSet".equals(op) && fnd) {
						// if present in our list then retire it
						db.setFields(table, id, new RecordStruct()
							.withField(field, new RecordStruct()
								.withField(suid, new RecordStruct()
									.withField("Retired", true)
								)
							)
						);
						
						return true;
					}
					
					return false;
				}
			});

			// Make negates non matches, so retire those
			if ("MakeSet".equals(op)) {
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
			}
			
			// Make and Add will add any remaining - unmatched - suids
			if ("MakeSet".equals(op) || "AddToSet".equals(op)) {
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
			
			// TODO make a record of everything for replication? or just let it figure it out?
		}
		
		task.complete();
	}
}
