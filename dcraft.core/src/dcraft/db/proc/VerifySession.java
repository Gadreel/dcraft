package dcraft.db.proc;

import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseTask;
import dcraft.db.TablesAdapter;
import dcraft.lang.BigDateTime;
import dcraft.lang.op.OperationResult;
import dcraft.struct.FieldStruct;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.builder.ICompositeBuilder;
import dcraft.util.StringUtil;

public class VerifySession extends LoadRecord {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		ICompositeBuilder out = task.getBuilder();
		TablesAdapter db = new TablesAdapter(conn, task); 
		String did = task.getDomain();
		BigDateTime when = BigDateTime.nowDateTime();
		
		RecordStruct params = task.getParamsAsRecord();
		String token = params.getFieldAsString("AuthToken");

		try {
			if (StringUtil.isEmpty(token)) 
				log.errorTr(117);
			else {
				String dd = (String) conn.get("dcSession", token, "Domain");
				String uu = (String) conn.get("dcSession", token, "User");
				
				if (!did.equals(dd)) {
					log.errorTr(121);
				}
				else {					
					conn.set("dcSession", token, "LastAccess", task.getStamp());

					// load info about the user
					ListStruct select = new ListStruct(
							new RecordStruct(
									new FieldStruct("Field", "Id"),
									new FieldStruct("Name", "UserId")
							),
							new RecordStruct(
									new FieldStruct("Field", "dcUsername"),
									new FieldStruct("Name", "Username")
							),
							new RecordStruct(
									new FieldStruct("Field", "dcFirstName"),
									new FieldStruct("Name", "FirstName")
							),
							new RecordStruct(
									new FieldStruct("Field", "dcLastName"),
									new FieldStruct("Name", "LastName")
							),
							new RecordStruct(
									new FieldStruct("Field", "dcEmail"),
									new FieldStruct("Name", "Email")
							),
							new RecordStruct(
									new FieldStruct("Field", "dcLocale"),
									new FieldStruct("Name", "Locale")
							),
							new RecordStruct(
									new FieldStruct("Field", "dcChronology"),
									new FieldStruct("Name", "Chronology")
							),
							new RecordStruct(
									new FieldStruct("Field", "dcAuthorizationTag"),		
									new FieldStruct("Name", "AuthorizationTags")
							)
					);		
					
					this.writeRecord(conn, task, log, out, db, "dcUser",
							uu, when, select, true, false, false);
					
					// TODO someday get group tags too
				}
			}
		}
		catch (Exception x) {
			log.error("SignOut: Unable to create resp: " + x);
		}
		
		task.complete();
	}
}
