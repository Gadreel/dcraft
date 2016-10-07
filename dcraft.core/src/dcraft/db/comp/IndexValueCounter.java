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
import dcraft.struct.Struct;
import dcraft.struct.builder.BuilderStateException;
import dcraft.struct.builder.ICompositeBuilder;
import dcraft.util.StringUtil;

// TODO re-think, this is not yet used
public class IndexValueCounter implements IComposer {
	@Override
	public void writeField(DatabaseInterface conn, DatabaseTask task, OperationResult log, ICompositeBuilder out, TablesAdapter db,
			String table, String id, BigDateTime when, RecordStruct field, boolean historical, boolean compact)
	{	
		try {
			String fname = field.getFieldAsString("Field");

			if (StringUtil.isEmpty(fname)) {
				out.value(new Long(0));
				return;
			}
			
			DbField fdef = task.getSchema().getDbField(table, fname);

			if (fdef == null) {
				out.value(new Long(0));
				return;
			}
			
			RecordStruct params = field.getFieldAsRecord("Params");

			if ((params == null) || params.isFieldEmpty("Value")) {
				out.value(new Long(0));
				return;
			}
			
			// get as a type we understand
			Object val = Struct.objectToCore(field.getField("Value"));
			
			AtomicLong cnt = new AtomicLong();

			db.traverseIndex(table, fname, val, when, historical, new Function<Object,Boolean>() {				
				@Override
				public Boolean apply(Object subid) {
					cnt.incrementAndGet();
					return true;
				}
			});
			
			task.getBuilder().value(new Long(cnt.get()));
		} 
		catch (BuilderStateException x) {
			// TODO Auto-generated catch block
			x.printStackTrace();
		}
	}
}
