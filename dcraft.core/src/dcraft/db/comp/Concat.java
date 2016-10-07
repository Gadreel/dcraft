package dcraft.db.comp;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseTask;
import dcraft.db.IComposer;
import dcraft.db.TablesAdapter;
import dcraft.lang.BigDateTime;
import dcraft.lang.op.OperationResult;
import dcraft.schema.DbField;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.struct.builder.ICompositeBuilder;
import dcraft.util.StringUtil;

public class Concat implements IComposer {
	@Override
	public void writeField(DatabaseInterface conn, DatabaseTask task, OperationResult log, ICompositeBuilder out, TablesAdapter db,
			String table, String id, BigDateTime when, RecordStruct field, boolean historical, boolean compact)
	{	
		try {
			RecordStruct params = field.getFieldAsRecord("Params");
			
			ListStruct items = params.getFieldAsList("Parts");
			String ret = "";
			
			if (items != null) {
				//LoadRecord lr  = new LoadRecord();
				
				for (int i = 0; i < items.getSize(); i++) {
					RecordStruct fld = (RecordStruct) items.getAt(i);
					
					ret += this.getField(conn, task, log, db, table, id, when, fld, historical, compact);
				}
			}
		
			out.value(ret);
		} 
		catch (Exception x) {
			// TODO Auto-generated catch block
			x.printStackTrace();
		}
	}
	
	public String getField(DatabaseInterface conn, DatabaseTask task, OperationResult log, 
			TablesAdapter db, String table, String id, BigDateTime when, RecordStruct field, 
			boolean historical, boolean compact) throws Exception 
	{		
		// composer not valid inside of concat
		if (!field.isFieldEmpty("Composer")) 
			return "";
		
		if (field.hasField("Value")) 
			return field.getFieldAsString("Value");
		
		String fname = field.getFieldAsString("Field");
		String format = field.getFieldAsString("Format");
		
		DbField fdef = task.getSchema().getDbField(table, fname);

		if (fdef == null) 
			return "";
		
		// subquery/foreign field not allowed in concat - TODO add support for single foreign field
		
		if ("Id".equals(fname)) 
			return id;
		
		String subid = field.getFieldAsString("SubId");

		if (StringUtil.isNotEmpty(subid) && fdef.isList()) 
			return Struct.objectToString(db.getDynamicList(table, id, fname, subid, when, format));

		// DynamicList, StaticList (or DynamicScalar is when == null)
		if (fdef.isList() || (fdef.isDynamic() && when == null)) {
			AtomicReference<String> res = new AtomicReference<>("");
			
			// keep in mind that `id` is the "value" in the index
			db.traverseSubIds(table, id, fname, when, historical, new Function<Object,Boolean>() {				
				@Override
				public Boolean apply(Object subid) {
					try {
						// don't output null values in this list - Extended might return null data but otherwise no nulls
							Object value = db.getDynamicList(table, id, fname, subid.toString(), when);
							
							if (value != null) {
								String v = res.get();
								
								if (v.length() > 0)
									v += ",";
								
								v += Struct.objectToString(value);
								
								res.set(v);
								
								return true;
							}
					}
					catch (Exception x) {
						log.error("Unable to write subid: " + x);
					}
					
					return false;
				}
			});
		
			return res.get();
		}		
		
		// DynamicScalar
		if (fdef.isDynamic()) 
			return Struct.objectToString(db.getDynamicScalar(table, id, fname, when, format, historical));
		
		// StaticScalar
		return Struct.objectToString(db.getStaticScalar(table, id, fname, format));
	}
}
