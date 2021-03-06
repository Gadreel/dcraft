package dcraft.db.proc;

import org.joda.time.DateTime;

import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseTask;
import dcraft.db.IStoredProc;
import dcraft.db.TablesAdapter;
import dcraft.lang.BigDateTime;
import dcraft.lang.op.OperationResult;
import dcraft.struct.RecordStruct;
import dcraft.struct.builder.ICompositeBuilder;
import dcraft.util.StringUtil;

public class InitiateRecovery implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		TablesAdapter db = new TablesAdapter(conn, task); 
		BigDateTime when = BigDateTime.nowDateTime();
		
		RecordStruct params = task.getParamsAsRecord();
		String user = params.getFieldAsString("User");

		try {			
			if (task.isReplicating()) {
				// TODO
			}
			else {
				boolean uisemail = false;
				Object userid = db.firstInIndex("dcUser", "dcUsername", user, when, false);
				
				if (userid == null) {
					userid = db.firstInIndex("dcUser", "dcEmail", user, when, false);
					uisemail = true;		// true for email or backup email
				}
				
				if (userid == null)	
					userid = db.firstInIndex("dcUser", "dcBackupEmail", user, when, false);
				
				if (userid == null) {
					log.error("Unable to complete recovery");
					task.complete();
					return;
				}

				String uid = userid.toString();
				String code = StringUtil.buildSecurityCode();
				
				db.setStaticScalar("dcUser", uid, "dcConfirmCode", code);
				db.setStaticScalar("dcUser", uid, "dcRecoverAt", new DateTime());
				
				String email = uisemail ? uid : (String) db.getDynamicScalar("dcUser", uid, "dcEmail", when);
				
				ICompositeBuilder out = task.getBuilder();

				out.startRecord();
				out.field("Code", code);
				out.field("Email", email);
				out.endRecord();
			}
		}
		catch (Exception x) {
			log.error("Account Recovery: Unable to create resp: " + x);
		}
				
		task.complete();
	}
}
