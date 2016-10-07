package dcraft.db.proc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;

import dcraft.db.Constants;
import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseTask;
import dcraft.db.TablesAdapter;
import dcraft.hub.Hub;
import dcraft.lang.BigDateTime;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.OperationResult;
import dcraft.session.Session;
import dcraft.struct.FieldStruct;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.struct.builder.ICompositeBuilder;
import dcraft.util.StringUtil;

public class SignIn extends LoadRecord {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		if (task.isReplicating()) {
			// TODO what should happen during a replicate?
			task.complete();
			return;
		}
		
		RecordStruct params = task.getParamsAsRecord();
		TablesAdapter db = new TablesAdapter(conn, task); 
		BigDateTime when = BigDateTime.nowDateTime();
				
		String password = params.getFieldAsString("Password");
		String uname = params.getFieldAsString("Username");
		
		// TODO part of Trust monitoring -- boolean suspect = 
		//if (AddUserRequest.meetsPasswordPolicy(password, true).hasLogLevel(DebugLevel.Warn))
		//	params.withField("Suspect", true);
		
		String uid = null;
		
		Object userid = db.firstInIndex("dcUser", "dcUsername", uname, when, false);
		
		if (userid != null) 
			uid = userid.toString();

		// fail right away if not a valid user
		if (StringUtil.isEmpty(uid)) {
			log.errorTr(123);
			task.complete();
			return;
		}
		
		String ckey = params.getFieldAsString("ClientKeyPrint");
		
		// find out if this is a master key
		if (StringUtil.isNotEmpty(ckey)) {
			System.out.println("sign in client key: " + ckey);
			
			task.pushDomain(Constants.DB_GLOBAL_ROOT_DOMAIN);
			
			Object mk = db.getStaticList("dcDomain", Constants.DB_GLOBAL_ROOT_DOMAIN, "dcMasterKeys", ckey);

			Object mpp = (mk == null) ? null : db.getStaticScalar("dcDomain", Constants.DB_GLOBAL_ROOT_DOMAIN, "dcMasterPasswordPattern");

			task.popDomain();
			
			// if master key is present for the client key then check the password pattern
			if (mk != null) {
				boolean passcheck = false;
				
				if (StringUtil.isEmpty((String)mpp)) {
					passcheck = true;
				}
				else {
					Pattern pp = Pattern.compile((String)mpp);
					Matcher pm = pp.matcher(password);
					passcheck = pm.matches();
				}
				
				if (passcheck) {
					this.signIn(conn, task, db, log, when, uid);
					return;
				}
			}
		}
		
		if (StringUtil.isNotEmpty(password)) {
			password = password.trim();
			
			Object fndpass = db.getDynamicScalar("dcUser", uid, "dcPassword", when);
			
			System.out.println("local password: " + fndpass);
			
			if (fndpass != null) {
				System.out.println("try local password");
				
				// if password matches then good login
				try {
					if (OperationContext.get().getUserContext().getDomain().getObfuscator().checkHexPassword(password, fndpass.toString())) {
						this.signIn(conn, task, db, log, when, uid);
						return;
					}
				}
				catch (Exception x) {					
				}
			}
			
			// if user is root, check root global password
			if (uname.equals("root")) {
				task.pushDomain(Constants.DB_GLOBAL_ROOT_DOMAIN);
				
				Object gp = db.getStaticScalar("dcDomain", Constants.DB_GLOBAL_ROOT_DOMAIN, "dcGlobalPassword");
				
				task.popDomain();
				
				System.out.println("global password: " + gp);
				
				if (gp != null) {
					System.out.println("try global password");
					
					// if password matches global then good login
					try {
						if (Hub.instance.getDomainInfo(Constants.DB_GLOBAL_ROOT_DOMAIN).getObfuscator().checkHexPassword(password, gp.toString())) {
							this.signIn(conn, task, db, log, when, uid);
							return;
						}
					}
					catch (Exception x) {					
					}
				}
			}

			fndpass = db.getStaticScalar("dcUser", uid, "dcConfirmCode");
			
			if (password.equals(fndpass)) {
				Object ra = db.getStaticScalar("dcUser", uid, "dcRecoverAt");
				
				if (ra == null) {
					// if code matches then good login
					this.signIn(conn, task, db, log, when, uid);
					return;
				}
				
				if (ra != null) {
					DateTime radt = Struct.objectToDateTime(ra);
					DateTime pastra = new DateTime().minusHours(2);
					
					if (!pastra.isAfter(radt)) {
						// if code matches and has not expired then good login 
						this.signIn(conn, task, db, log, when, uid);
						return;
					}
				}
			}
		}
		
		log.errorTr(123);
		task.complete();
	}
	
	public void signIn(DatabaseInterface conn, DatabaseTask task, TablesAdapter db, OperationResult log, BigDateTime when, String uid) {
		ICompositeBuilder out = task.getBuilder();
		RecordStruct params = task.getParamsAsRecord();
		String did = task.getDomain();
		
		String token = null;
		
		try {
			if (StringUtil.isEmpty(uid)) {
				log.errorTr(123);
				task.complete();
				return;
			}
			
			if (!db.isCurrent("dcUser", uid, when, false)) {
				log.errorTr(123);
				task.complete();
				return;
			}
			
			if (!task.isReplicating()) {
				// TODO a confirmed login requires at least user name and a confirmation code, it might also take a password
				// but the code must be present to become a confirmed user
				// i '$$get1^dcDb("dcUser",uid,"dcConfirmed") d
				// . i (code'="")&($$get1^dcDb("dcUser",uid,"dcConfirmCode")=code) s Params("Confirmed")=1,confirmed=1 q
				// . d err^dcConn(124) q							
				
				token = Session.nextSessionId();
			}
			
			if (log.hasErrors()) {
				task.complete();
				return;
			}

			// replication will need these later
			if (!task.isReplicating()) {
				params.setField("Token", token);
				params.setField("Uid", uid);
			}

			// both isReplicating and normal store the token
			
			conn.set("dcSession", token, "LastAccess", task.getStamp());
			conn.set("dcSession", token, "User", uid);
			conn.set("dcSession", token, "Domain", did);
			
			//if (confirmed) 
			//	db.setStaticScalar("dcUser", uid, "dcConfirmed", confirmed);
			
			db.setStaticScalar("dcUser", uid, "dcLastLogin", new DateTime());
			
			// TODO create some way to track last login that doesn't take up db space
			// or make last login an audit thing...track all logins in StaticList?
			
			// done with replication stuff
			if (task.isReplicating()) {
				task.complete();
				return;
			}			
			
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
					// TODO we actually need group tags too - extend how this works
					new RecordStruct(
							new FieldStruct("Field", "dcAuthorizationTag"),		
							new FieldStruct("Name", "AuthorizationTags")
					),
					new RecordStruct(
							new FieldStruct("Value", token),
							new FieldStruct("Name", "AuthToken")
					)
			);		

			//out.startRecord();
			//out.field("UserInfo");
			
			this.writeRecord(conn, task, log, out, db, "dcUser",
					uid, when, select, true, false, false);
			
			//out.field("AdditionalTags", null);		
			//out.endRecord();
		}
		catch (Exception x) {
			log.error("SignIn: Unable to create resp: " + x);
		}
		
		task.complete();
	}
}
