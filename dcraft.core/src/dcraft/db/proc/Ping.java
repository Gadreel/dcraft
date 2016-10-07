package dcraft.db.proc;

import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseTask;
import dcraft.db.IStoredProc;
import dcraft.lang.op.OperationResult;
import dcraft.struct.builder.ICompositeBuilder;

public class Ping implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		ICompositeBuilder resp = task.getBuilder();
		//CompositeStruct params = task.getParams();
		
		try {
			resp.startRecord();
			resp.field("Text", "Pong");
			resp.endRecord();

			/* alternative solution
			rec.toBuilder(new RecordStruct(new FieldStruct("Text", "Pong")));
			*/
		}
		catch (Exception x) {
			log.error("Ping: Unable to create response: " + x);
		}
		
		task.complete();
	}
}
