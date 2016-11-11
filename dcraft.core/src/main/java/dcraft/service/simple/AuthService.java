/* ************************************************************************
#
#  designCraft.io
#
#  http://designcraft.io/
#
#  Copyright:
#    Copyright 2014 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */
package dcraft.service.simple;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dcraft.bus.IService;
import dcraft.bus.Message;
import dcraft.hub.Hub;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.UserContext;
import dcraft.log.Logger;
import dcraft.mod.ExtensionBase;
import dcraft.session.Session;
import dcraft.struct.FieldStruct;
import dcraft.struct.RecordStruct;
import dcraft.util.HexUtil;
import dcraft.util.StringUtil;
import dcraft.work.TaskRun;
import dcraft.xml.XElement;

/*
 * AuthService can not be remote, though it may call a remote service
 * 
 * VERIFY
 * 
 * auth token is key here - if present in request and we find it, then pass (no matter session id)
 * 	if present in request and we don't find it then reset to Guest and return error
 *  if not present then check creds
 *  
 * NOTICE
 * 
 * Simple service does not (yet - TODO) timeout on the auth tokens, will collect forever
 * 
 */
public class AuthService extends ExtensionBase implements IService {
	protected SecureRandom random = new SecureRandom();

	protected Map<String, TenantUsers> tenantusers = new HashMap<>();
	
	@Override
	public void init(XElement config) {
		super.init(config);
		
		XElement tenants = Hub.instance.getConfig().selectFirst("Tenants");
		
		// create a root tenant if none listed
		if (tenants == null) {
			String pw = "A1s2d3f4";
			String epw = "";
			
			if (config != null) { 
				pw = config.getAttribute("Password", pw);
				epw = config.getAttribute("EncryptedPassword", epw);
			}
			
			tenants = new XElement("Tenants")
					.with(new XElement("Tenant")
							.withAttribute("Id", "0")
							.withAttribute("Alias", "root")
							.withAttribute("Title", "Root and Local")
							.with(new XElement("Name").withText("localhost"))
							.with(new XElement("Name").withText("root"))
							.with(new XElement("User")
									.withAttribute("Id", "0")
									.withAttribute("Username", "root")
									.withAttribute("First", "Root")
									.withAttribute("Last", "User")
									.withAttribute("Email", "root@locahost")
									.withAttribute("Password", pw)
									.withAttribute("EncryptedPassword", epw)
									.with(new XElement("AuthTag").withText("Admin"))
									.with(new XElement("AuthTag").withText("SysAdmin"))
							)
					);
			
			Hub.instance.getConfig().with(tenants);
		}
		
		// load the tenant user directory
		for (XElement mtenant : tenants.selectAll("Tenant")) {
			String id = mtenant.getAttribute("Id");
			
			TenantUsers du = new TenantUsers();
			du.load(id, mtenant);
			
			this.tenantusers.put(id, du);
		}
	}
	
	@Override
	public String serviceName() {
		return "dcAuth";
	}
	
	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");

		// uc is different from sess.getUser as uc may have credentials with it...sess should not
		UserContext uc = request.getContext().getUserContext();
		Session sess = request.getContext().getSession();
		
		TenantUsers du = this.tenantusers.get(uc.getTenantId());
		
		if (du == null) {
			sess.clearToGuest();
			OperationContext.switchUser(request.getContext(), sess.getUser());
			
			request.errorTr(442);
			request.error("Tenant not found");
			request.complete();
			return;
		}
		
		//System.out.println("Auth: " + feature + " - " + op);
		
		if ("Authentication".equals(feature)) {
			
			System.out.println("=== NOVHOATS Auth: " + op);
			
			if ("SignIn".equals(op)) {
				
				if (sess == null) {
					OperationContext.switchUser(request.getContext(), UserContext.allocateGuest());
					
					request.errorTr(442);
					request.error("Session not found");
					request.complete();
					return;
				}
				
				String uname = uc.getUsername();
				
				RecordStruct urec = du.info(uname);
				
				if (urec == null) {
					du.clear(sess, uc.getAuthToken());
					request.error("User not found");
				}
				else {
					request.setResult(urec);
				}
				
				request.complete();
				return;
			}			
			
			if ("Verify".equals(op)) {
				
				if (sess == null) {
					OperationContext.switchUser(request.getContext(), UserContext.allocateGuest());
					
					request.errorTr(442);
					request.error("Session not found");
					request.complete();
					return;
				}
				
				String authToken = uc.getAuthToken();
				
				if (StringUtil.isNotEmpty(authToken)) {
					du.verifyToken(sess, authToken);
					request.complete();
					return;
				}				
				
				//System.out.println("---------- Token empty or bad");
				
				// else try to authenticate
				RecordStruct creds = uc.getCredentials();  // msg.getFieldAsRecord("Credentials");
				
				if (creds == null) {
					du.clear(sess, uc.getAuthToken());
					
					request.errorTr(442);
					request.complete();
					return;
				}
				
				//System.out.println("---------- Using Creds");
				
				String uname = creds.getFieldAsString("Username"); 
				String passwd = creds.getFieldAsString("Password");
				
				du.verifyCreds(sess, uname, passwd);
				
				request.complete();
				return;
			}			
			
			if ("SignOut".equals(op)) {
				if (sess != null) 
					du.clear(sess, uc.getAuthToken());
				
				request.complete();
				return;
			}		
		}
		else if ("Recovery".equals(feature)) {
			if ("Initiate".equals(op)) {
				request.complete();
				return;
			}			
		}
		
		request.errorTr(441, this.serviceName(), feature, op);
		request.complete();
	}
	
	public class TenantUsers {
		protected String tenantid = null;
		protected Map<String, XElement> cachedIndex = new HashMap<>();
		
		// token to username map
		protected Map<String, String> authtokens = new HashMap<>();
		
		public void verifyToken(Session sess, String authtoken) {
			String uname = this.authtokens.get(authtoken);
			
			if (StringUtil.isEmpty(uname)) {
				Logger.errorTr(442);
				Logger.error("AuthToken not found, could not verify");
				this.clear(sess, authtoken);
				return;
			}
			
			this.setContext(sess, uname, authtoken);
		}
		
		public void verifyCreds(Session sess, String username, String password) {
			XElement usr = this.cachedIndex.get(username);
			
			if (usr == null) {
				Logger.errorTr(442);
				Logger.error("User not found, could not verify");
				this.clear(sess, null);
				return;
			}
			
			String upass = usr.getAttribute("EncryptedPassword");
			
			// any setting in the config file is set with Hub crypto not tenant crypto
			if (! Hub.instance.getClock().getObfuscator().checkHexPassword(password, upass)) {
				Logger.errorTr(442);
				Logger.error("Invalid credentials, could not verify");
				this.clear(sess, null);
				return;
			}
			
			byte[] feedbuff = new byte[32];
			AuthService.this.random.nextBytes(feedbuff);
			String token = HexUtil.bufferToHex(feedbuff);
			
			this.setContext(sess, username, token);
		}
		
		public void clear(Session sess, String token) {
			sess.clearToGuest();
			OperationContext.switchUser(OperationContext.get(), sess.getUser());
			
			if (StringUtil.isNotEmpty(token))
				this.authtokens.remove(token);
		}
		
		public void setContext(Session sess, String username, String token) {
			XElement usr = this.cachedIndex.get(username);
			
			if ((usr == null)) {
				Logger.errorTr(442);
				Logger.error("User not found, could not verify");
				this.clear(sess, null);
				return;
			}

			String uid = usr.getAttribute("Id");
			
			List<XElement> tags = usr.selectAll("AuthTag");
			
			String[] atags = new String[tags.size() + 1];
			
			atags[0] = "User";
			
			for (int i = 1; i < atags.length; i++) 
				atags[i] = tags.get(i - 1).getText();
			
			sess.withUser(sess.getUser().toBuilder()
				.withTenantId(tenantid)
				.withUserId(uid)
				.withUsername(usr.getAttribute("Username"))
				.withFullName(usr.getAttribute("FullName"))
				.withEmail(usr.getAttribute("Email"))
				.withVerified(true)
				.withAuthTags(atags)
				.withAuthToken(token)
				.toUserContext());
			
			OperationContext.switchUser(OperationContext.get(), sess.getUser());
			
			this.authtokens.put(token, username);
		}
		
		public RecordStruct info(String username) {
			XElement usr = this.cachedIndex.get(username);
			
			if (usr == null) 
				return null;
			
			return new RecordStruct(
				new FieldStruct("Username", usr.getAttribute("Username")),
				new FieldStruct("FirstName", usr.getAttribute("First")),
				new FieldStruct("LastName", usr.getAttribute("Last")),
				new FieldStruct("Email", usr.getAttribute("Email"))
			);
		}
		
		public void load(String did, XElement tenant) {
			this.tenantid = did;
			
			for (XElement usr : tenant.selectAll("User")) {
				this.cachedIndex.put(usr.getAttribute("Username"), usr);

				// make sure we have an encrypted password for use with verify
				if (! usr.hasNotEmptyAttribute("EncryptedPassword") && usr.hasNotEmptyAttribute("Password")) 
					usr.withAttribute("EncryptedPassword", Hub.instance.getClock().getObfuscator().hashPassword(usr.getAttribute("Password")));
				
				if (! usr.hasNotEmptyAttribute("FullName")) {
					String fullname = "";
					
					if (usr.hasAttribute("First"))
						fullname = usr.getAttribute("First");
					
					if (usr.hasAttribute("Last") && StringUtil.isNotEmpty(fullname))
						fullname += " " + usr.getAttribute("Last");
					else if (usr.hasAttribute("Last"))
						fullname = usr.getAttribute("Last");
					
					if (StringUtil.isEmpty(fullname))
						fullname = "[unknown]";
					
					usr.withAttribute("FullName", fullname);
				}
			}
		}
	}
}
