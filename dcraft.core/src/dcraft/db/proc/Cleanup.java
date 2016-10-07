package dcraft.db.proc;

import java.math.BigDecimal;

import org.joda.time.DateTime;

import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseTask;
import dcraft.db.IStoredProc;
import dcraft.db.util.ByteUtil;
import dcraft.lang.op.OperationResult;
import dcraft.struct.RecordStruct;

public class Cleanup implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		RecordStruct params = task.getParamsAsRecord();
		//DateTime expire = params.getFieldAsDateTime("ExpireThreshold");
		DateTime lexpire = params.getFieldAsDateTime("LongExpireThreshold");

		try {
			byte[] sessonid = conn.nextPeerKey("dcSession", null);

			while (sessonid != null) { 
				String token = ByteUtil.extractValue(sessonid).toString();
				
				BigDecimal la = conn.getAsDecimal("dcSession", token, "LastAccess");
				
				if ((la == null) || (lexpire.getMillis() > la.abs().longValue())) 
					conn.kill("dcSession", token);
				
				sessonid = conn.nextPeerKey("dcSession", token);
			}
		}
		catch (Exception x) {
			log.error("SignOut: Unable to create resp: " + x);
		}
		
		task.complete();
	}
}
