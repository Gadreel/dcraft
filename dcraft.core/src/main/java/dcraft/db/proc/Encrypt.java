package dcraft.db.proc;

import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseTask;
import dcraft.db.IStoredProc;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.OperationResult;
import dcraft.struct.RecordStruct;
import dcraft.struct.builder.ICompositeBuilder;

public class Encrypt implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		ICompositeBuilder resp = task.getBuilder();
		RecordStruct params = (RecordStruct) task.getParams();
		
		try {
			resp.startRecord();
			resp.field("Value", OperationContext.get().getUserContext().getTenant().getObfuscator().encryptStringToHex(params.getFieldAsString("Value")));
			resp.endRecord();
		}
		catch (Exception x) {
			log.error("Encrypt: Unable to create response: " + x);
		}
		
		task.complete();
	}
}
