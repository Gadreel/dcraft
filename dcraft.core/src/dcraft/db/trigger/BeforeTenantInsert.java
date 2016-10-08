package dcraft.db.trigger;

import dcraft.db.Constants;
import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseTask;
import dcraft.db.IStoredProc;
import dcraft.db.TablesAdapter;
import dcraft.hub.Hub;
import dcraft.lang.op.FuncResult;
import dcraft.lang.op.OperationResult;
import dcraft.struct.RecordStruct;
import dcraft.xml.XElement;

public class BeforeTenantInsert implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		if (task.isReplicating())
			return;
		
		RecordStruct fields = task.getParamsAsRecord().getFieldAsRecord("Fields");
		
		if (!fields.hasField("dcObscureSeed")) {
			XElement obfconfig = new XElement("Clock");
			
			Hub.instance.getClock().getObfuscator().configure(obfconfig);
			
			// set the obscure seed before insert
			fields.withField("dcObscureSeed", new RecordStruct()
				.withField("Data", obfconfig.getAttribute("Feed"))
			);
		}
		
		TablesAdapter db = new TablesAdapter(conn, task); 
		
		FuncResult<String> ires = db.createRecord(Constants.DB_GLOBAL_TENANT_DB);
		
		if (ires.hasErrors()) 
			return;
		
		String id = ires.getResult();
		
		task.getParamsAsRecord().setField("Id", id);		// now the calling code thinks we are an update rather than insert
		
		// for the rest of this request we are running in another domain
		task.pushTenant(id);
	}
}
