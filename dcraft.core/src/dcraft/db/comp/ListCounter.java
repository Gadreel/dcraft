package dcraft.db.comp;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseTask;
import dcraft.db.IComposer;
import dcraft.db.TablesAdapter;
import dcraft.lang.BigDateTime;
import dcraft.lang.op.OperationResult;
import dcraft.schema.DbField;
import dcraft.struct.RecordStruct;
import dcraft.struct.builder.BuilderStateException;
import dcraft.struct.builder.ICompositeBuilder;

public class ListCounter implements IComposer {
	@Override
	public void writeField(DatabaseInterface conn, DatabaseTask task, OperationResult log, ICompositeBuilder out, TablesAdapter db,
			String table, String id, BigDateTime when, RecordStruct field, boolean historical, boolean compact)
	{	
		try {
			String fname = field.getFieldAsString("Field");
			
			DbField fdef = task.getSchema().getDbField(table, fname);

			if (fdef == null) {
				out.value(new Long(0));
				return;
			}
			
			AtomicLong cnt = new AtomicLong();
			
			if ("Id".equals(fname)) {
				cnt.set(1);
			}
			// DynamicList, StaticList (or DynamicScalar is when == null)
			else if (fdef.isList() || (fdef.isDynamic() && when == null)) {
				// keep in mind that `id` is the "value" in the index
				db.traverseSubIds(table, id, fname, when, historical, new Function<Object,Boolean>() {				
					@Override
					public Boolean apply(Object subid) {
						cnt.incrementAndGet();
						return true;
					}
				});
			}		
			// DynamicScalar
			else if (fdef.isDynamic()) {
				if (db.getDynamicScalarRaw(table, id, fname, when, historical) != null)
					cnt.set(1);
			}
			// StaticScalar
			else {
				if (db.getStaticScalarRaw(table, id, fname) != null)
					cnt.set(1);
			}
			
			task.getBuilder().value(new Long(cnt.get()));
		} 
		catch (BuilderStateException x) {
			// TODO Auto-generated catch block
			x.printStackTrace();
		}
	}
}
