package dcraft.db.proc;

import java.util.concurrent.atomic.AtomicLong;
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
import dcraft.struct.builder.ICompositeBuilder;

public class CountIndexes implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		RecordStruct params = task.getParamsAsRecord();
		
		String table = params.getFieldAsString("Table");
		String fname = params.getFieldAsString("Field");
		BigDateTime when = params.getFieldAsBigDateTime("When");
		boolean historical = params.getFieldAsBooleanOrFalse("Historical");	
		ListStruct values = params.getFieldAsList("Values");
		
		if (when == null)
			when = BigDateTime.nowDateTime();
		
		TablesAdapter db = new TablesAdapter(conn, task); 
		ICompositeBuilder out = task.getBuilder();
		
		try {
			out.startList();

			if ((values == null) || (values.getSize() == 0)) {
				BigDateTime fwhen = when;
				
				db.traverseIndexValRange(table, fname, null, null, fwhen, historical, new Function<Object,Boolean>() {
					@Override
					public Boolean apply(Object val) {
						AtomicLong cnt = new AtomicLong();
						
						db.traverseIndex(table, fname, val, fwhen, historical, new Function<Object,Boolean>() {				
							@Override
							public Boolean apply(Object subid) {
								cnt.incrementAndGet();
								return true;
							}
						});
						
						try {
							out.startRecord();
							out.field("Name", val);
							out.field("Count", new Long(cnt.get()));
							out.endRecord();
							
							return true;
						}
						catch (Exception x) {
							log.error("Issue with counting index record: " + x);
						}
						
						return false;
					}
				});
			}
			else {
				for (Struct vs : values.getItems()) {
					Object val = Struct.objectToCore(vs);
					
					AtomicLong cnt = new AtomicLong();
			
					db.traverseIndex(table, fname, val, when, historical, new Function<Object,Boolean>() {				
						@Override
						public Boolean apply(Object subid) {
							cnt.incrementAndGet();
							return true;
						}
					});
					
					out.startRecord();
					out.field("Name", val);
					out.field("Count", new Long(cnt.get()));
					out.endRecord();
				}
			}
			
			out.endList();
		}
		catch (Exception x) {
			log.error("Issue with counting index record: " + x);
		}
		
		task.complete();
	}
}
