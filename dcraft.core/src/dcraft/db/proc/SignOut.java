package dcraft.db.proc;

import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseTask;
import dcraft.db.IStoredProc;
import dcraft.lang.op.OperationResult;
import dcraft.struct.RecordStruct;
import dcraft.util.StringUtil;

public class SignOut implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		RecordStruct params = task.getParamsAsRecord();
				
		String token = params.getFieldAsString("AuthToken");

		try {
			if (StringUtil.isEmpty(token)) 
				log.errorTr(117);
			else 
				conn.kill("dcSession", token);
		}
		catch (Exception x) {
			log.error("SignOut: Unable to create resp: " + x);
		}
		
		task.complete();
	}
}
