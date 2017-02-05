package dcraft.db.trigger;

import static dcraft.db.Constants.DB_GLOBAL_INDEX_SUB;
import static dcraft.db.Constants.DB_GLOBAL_RECORD;
import static dcraft.db.Constants.DB_GLOBAL_RECORD_META;
import static dcraft.db.Constants.DB_GLOBAL_ROOT_TENANT;
import static dcraft.db.Constants.DB_GLOBAL_ROOT_USER;

import java.math.BigDecimal;
import java.util.Locale;

import org.joda.time.DateTime;

import dcraft.db.Constants;
import dcraft.db.DatabaseException;
import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseTask;
import dcraft.db.IStoredProc;
import dcraft.db.TablesAdapter;
import dcraft.lang.BigDateTime;
import dcraft.lang.op.OperationResult;

public class AfterTenantInsert implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		String id = task.getParamsAsRecord().getFieldAsString("Id");
		
		// ===========================================
		//  insert root Tenant index
		// ===========================================
		try {
			conn.set(DB_GLOBAL_RECORD, DB_GLOBAL_ROOT_TENANT, Constants.DB_GLOBAL_TENANT_DB, DB_GLOBAL_ROOT_TENANT, Constants.DB_GLOBAL_TENANT_IDX_DB, 
					id, task.getStamp(), "Data", id);
		} 
		catch (DatabaseException x) {
			log.error("Unable to set " + Constants.DB_GLOBAL_TENANT_IDX_DB + ": " + x);
			return;
		}
		
		// ===========================================
		//  insert a template for the root user of this new Tenant
		// ===========================================

		try {
			BigDecimal stamp = task.getStamp();
			String did = task.getTenant();
			
			String unamesub = conn.allocateSubkey();
			
			// insert root user name
			conn.set(DB_GLOBAL_RECORD, did, "dcUser", DB_GLOBAL_ROOT_USER, "dcUsername", unamesub, stamp, "Data", "root");
			// increment index count
			conn.inc(DB_GLOBAL_INDEX_SUB, did, "dcUser", "dcUsername", "root");					
			// set the new index new
			conn.set(DB_GLOBAL_INDEX_SUB, did, "dcUser", "dcUsername", "root", DB_GLOBAL_ROOT_USER, unamesub, null);

			// TODO enhance to take email from root Tenant's root user
			String email = "awhite@filetransferconsulting.com";

			task.pushTenant(DB_GLOBAL_ROOT_TENANT);
			
			TablesAdapter db = new TablesAdapter(conn, task); 
			
			Object rdemail = db.getDynamicScalar("dcUser", DB_GLOBAL_ROOT_USER, "dcEmail", new BigDateTime());

			if (rdemail != null)
				email = (String)rdemail;

			task.popTenant();
			
			String emailsub = conn.allocateSubkey();
			
			// insert root user email
			conn.set(DB_GLOBAL_RECORD, did, "dcUser", DB_GLOBAL_ROOT_USER, "dcEmail", emailsub, stamp, "Data", email);
			// increment index count
			conn.inc(DB_GLOBAL_INDEX_SUB, did, "dcUser", "dcEmail", email.toLowerCase(Locale.ROOT));					
			// set the new index new
			conn.set(DB_GLOBAL_INDEX_SUB, did, "dcUser", "dcEmail", email.toLowerCase(Locale.ROOT), DB_GLOBAL_ROOT_USER, emailsub, null);
			
			// TODO enhance how confirm code is generated/returned
			
			// insert root user confirmation code - they have N minutes to login with recovery code
			conn.set(DB_GLOBAL_RECORD, did, "dcUser", DB_GLOBAL_ROOT_USER, "dcConfirmCode", stamp, "Data", "A1s2d3f4");
			conn.set(DB_GLOBAL_RECORD, did, "dcUser", DB_GLOBAL_ROOT_USER, "dcRecoverAt", stamp, "Data", new DateTime());
			
			// insert root user auth tags
			conn.set(DB_GLOBAL_RECORD, did, "dcUser", DB_GLOBAL_ROOT_USER, "dcAuthorizationTag", "Admin", stamp, "Data", "Admin");
			conn.set(DB_GLOBAL_RECORD, did, "dcUser", DB_GLOBAL_ROOT_USER, "dcAuthorizationTag", "Developer", stamp, "Data", "Developer");
			
			// increment index count
			conn.inc(DB_GLOBAL_INDEX_SUB, did, "dcUser", "dcAuthorizationTag", "Admin".toLowerCase(Locale.ROOT));
			conn.inc(DB_GLOBAL_INDEX_SUB, did, "dcUser", "dcAuthorizationTag", "Developer".toLowerCase(Locale.ROOT));
			// set the new index new
			conn.set(DB_GLOBAL_INDEX_SUB, did, "dcUser", "dcAuthorizationTag", "Admin".toLowerCase(Locale.ROOT), DB_GLOBAL_ROOT_USER, "Admin", null);
			conn.set(DB_GLOBAL_INDEX_SUB, did, "dcUser", "dcAuthorizationTag", "Developer".toLowerCase(Locale.ROOT), DB_GLOBAL_ROOT_USER, "Developer", null);
			
			// insert root Tenant record count
			conn.set(DB_GLOBAL_RECORD_META, did, "dcUser", "Count", 1);
		} 
		catch (DatabaseException x) {
			log.error("Unable to set dcTenantIndex: " + x);
			return;
		}
	}
}
