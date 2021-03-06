package dcraft.service.db;

import java.net.URL;
import java.net.URLEncoder;
import java.util.function.Consumer;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;

import dcraft.bus.IService;
import dcraft.bus.Message;
import dcraft.db.DataRequest;
import dcraft.db.IDatabaseManager;
import dcraft.db.ObjectFinalResult;
import dcraft.db.ObjectResult;
import dcraft.db.common.RequestFactory;
import dcraft.db.query.LoadRecordRequest;
import dcraft.db.query.SelectDirectRequest;
import dcraft.db.query.SelectFields;
import dcraft.db.query.WhereEqual;
import dcraft.db.query.WhereField;
import dcraft.db.update.InsertRecordRequest;
import dcraft.db.update.UpdateRecordRequest;
import dcraft.hub.Hub;
import dcraft.lang.op.FuncResult;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.UserContext;
import dcraft.mod.ExtensionBase;
import dcraft.session.Session;
import dcraft.struct.CompositeParser;
import dcraft.struct.CompositeStruct;
import dcraft.struct.FieldStruct;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.util.HexUtil;
import dcraft.util.StringUtil;
import dcraft.work.TaskRun;

public class AuthService extends ExtensionBase implements IService {
	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");

		OperationContext tc = OperationContext.get();
		UserContext uc = tc.getUserContext();

		// uc is different from sess.getUser as uc may have credentials with it...sess should not
		Session sess = tc.getSession();
		
		// TODO we are a specialized service - should be allowed direct access to DB, maybe?
		
		IDatabaseManager db = Hub.instance.getDatabase();
		
		if (db == null) {
			request.errorTr(303);
			request.complete();
			return;
		}
		
		//System.out.println("Auth: " + feature + " - " + op);
		
		if ("Facebook".equals(feature)) {
			if ("LinkAccount".equals(op)) {
				// try to authenticate
				RecordStruct creds = msg.getFieldAsRecord("Body");
				
				String fbtoken = creds.getFieldAsString("AccessToken");
				
				RecordStruct fbinfo = AuthService.fbSignIn(fbtoken, null);		// TODO use FB secret key someday? for app proof...
				
				if (request.hasErrors() || (fbinfo == null)) {
					request.error("Missing Facebook fields");
					request.complete();
					return;
				}
				
				// TODO allow only `verified` fb users?
				if (fbinfo.isFieldEmpty("id") || fbinfo.isFieldEmpty("email")
						 || fbinfo.isFieldEmpty("first_name") || fbinfo.isFieldEmpty("last_name")) {		
					request.error("Missing Facebook fields");
					request.complete();
					return;
				}
				
				String uid = fbinfo.getFieldAsString("id");
									
				UpdateRecordRequest req = new UpdateRecordRequest();
				
				req
					.withTable("dcUser")
					.withId(OperationContext.get().getUserContext().getUserId())
					.withUpdateField("dcmFacebookId", uid);
				
				db.submit(req, new ObjectFinalResult(request) );
				
				return;
			}
		}
		else if ("Authentication".equals(feature)) {
			if ("SignIn".equals(op)) {
				
				if (sess == null) {
					OperationContext.switchUser(request.getContext(), UserContext.allocateGuest());
					
					request.errorTr(442);
					request.error("Session not found");
					request.complete();
					return;
				}
				
				LoadRecordRequest req = new LoadRecordRequest()
					.withTable("dcUser")
					.withId(uc.getUserId())
					.withNow()
					.withSelect(new SelectFields()
						.withField("dcUsername", "Username")
						.withField("dcFirstName", "FirstName")
						.withField("dcLastName", "LastName")
						.withField("dcEmail", "Email")
					);				
				
				db.submit(req, new ObjectResult() {
					@Override
					public void process(CompositeStruct result) {
						if (request.hasErrors() || (result == null)) {
							tc.getTenant().authEvent(op, "Fail", uc);
							AuthService.this.clearUserContext(sess, request.getContext());
							request.errorTr(442);
						}
						else {
							tc.getTenant().authEvent(op, "Success", uc);
						}
						
						request.returnValue(result);
					}
				});						
				
				return;
			}			

			
			if ("SignInFacebook".equals(op)) {
				
				if (sess == null) {
					OperationContext.switchUser(request.getContext(), UserContext.allocateGuest());
					
					request.errorTr(442);
					request.error("Session not found");
					request.complete();
					return;
				}
				
				// TODO check domain settings that FB sign in is allowed
				
				// try to authenticate
				RecordStruct creds = msg.getFieldAsRecord("Body");
				
				//String uid = creds.getFieldAsString("UserId");
				String fbtoken = creds.getFieldAsString("AccessToken");
				
				RecordStruct fbinfo = AuthService.fbSignIn(fbtoken, null);		// TODO use FB secret key someday? for app proof...
				
				if (request.hasErrors() || (fbinfo == null)) {
					tc.getTenant().authEvent("SignIn", "Fail", uc);
					AuthService.this.clearUserContext(sess, OperationContext.get());
					request.errorTr(442);
					request.complete();
					return;
				}
				
				// TODO allow only `verified` fb users?
				if (fbinfo.isFieldEmpty("id") || fbinfo.isFieldEmpty("email")
						 || fbinfo.isFieldEmpty("first_name") || fbinfo.isFieldEmpty("last_name")) {		
					tc.getTenant().authEvent("SignIn", "Fail", uc);
					AuthService.this.clearUserContext(sess, OperationContext.get());
					request.errorTr(442);
					request.complete();
					return;
				}
				
				String uid = fbinfo.getFieldAsString("id");
				
				// sigin callback
				Consumer<String> signincb = new Consumer<String>() {					
					@Override
					public void accept(String userid) {
						DataRequest tp1 = RequestFactory.startSessionRequest(userid);
						
						// TODO for all services, be sure we return all messages from the submit with the message
						db.submit(tp1, new ObjectResult() {
							@Override
							public void process(CompositeStruct result) {
								RecordStruct sirec = (RecordStruct) result;
								OperationContext ctx = request.getContext();
								
								//System.out.println("auth 2: " + request.getContext().isElevated());
								
								if (request.hasErrors() || (sirec == null)) {
									tc.getTenant().authEvent("SignIn", "Fail", uc);
									AuthService.this.clearUserContext(sess, ctx);
									request.errorTr(442);
									request.complete();
									return;
								}

								ListStruct atags = sirec.getFieldAsList("AuthorizationTags");
								atags.addItem("User");
								
								// TODO make locale smart
								String fullname = "";
								
								if (!sirec.isFieldEmpty("FirstName"))
									fullname = sirec.getFieldAsString("FirstName");
								
								if (!sirec.isFieldEmpty("LastName") && StringUtil.isNotEmpty(fullname))
									fullname += " " + sirec.getFieldAsString("LastName");
								else if (!sirec.isFieldEmpty("LastName"))
									fullname = sirec.getFieldAsString("LastName");
								
								if (StringUtil.isEmpty(fullname))
									fullname = "[unknown]";
								
								UserContext usr = sess.getUser().toBuilder() 
										.withVerified(true)
										.withAuthToken(sirec.getFieldAsString("AuthToken"))
										.withUserId(sirec.getFieldAsString("UserId"))
										.withUsername(sirec.getFieldAsString("Username"))
										.withFullName(fullname)		
										.withEmail(sirec.getFieldAsString("Email"))
										.withAuthTags(atags)
										.toUserContext();

								sess.withUser(usr);
								
								OperationContext.switchUser(ctx, usr);
								
								tc.getTenant().authEvent("SignIn", "Success", usr);
								
								request.returnValue(new RecordStruct(
										new FieldStruct("Username", sirec.getFieldAsString("Username")),
										new FieldStruct("FirstName", sirec.getFieldAsString("FirstName")),
										new FieldStruct("LastName", sirec.getFieldAsString("LastName")),
										new FieldStruct("Email", sirec.getFieldAsString("Email"))
								));
							}
						});
					}
				};
				
				// -----------------------------------------
				// find user - update or insert user record
				// -----------------------------------------
				
				db.submit(
						new SelectDirectRequest()
							.withTable("dcUser")
							.withSelect(new SelectFields()
									.withField("Id")
									.withField("dcUsername", "Username")
									.withField("dcFirstName", "FirstName")
									.withField("dcLastName", "LastName")
									.withField("dcEmail", "Email")
							)
							.withWhere(
									new WhereEqual(new WhereField("dcmFacebookId"), uid)		// TODO or where `username` = `fb email` - maybe?
							),
						new ObjectResult() {
							@Override
							public void process(CompositeStruct uLookupResult) {
								if (this.hasErrors() || (uLookupResult == null)) {
									tc.getTenant().authEvent("SignIn", "Fail", uc);
									request.error("Error finding user record");
									request.complete();
									return;
								}
								
								ListStruct ulLookupResult = (ListStruct) uLookupResult;
								
								if (ulLookupResult.getSize() == 0) {
									// insert new user record
									InsertRecordRequest req = new InsertRecordRequest();
									
									req
										.withTable("dcUser")		
										.withSetField("dcUsername", fbinfo.getFieldAsString("email"))
										.withSetField("dcEmail", fbinfo.getFieldAsString("email"))
										.withSetField("dcFirstName", fbinfo.getFieldAsString("first_name"))
										.withSetField("dcLastName", fbinfo.getFieldAsString("last_name"))
										.withSetField("dcmFacebookId", uid)
										.withSetField("dcConfirmed", true);									
									
									// TODO look at fb `locale` and `timezone` too
									
									db.submit(req, new ObjectResult() {										
										@Override
										public void process(CompositeStruct result) {
											if (this.hasErrors()) {
												tc.getTenant().authEvent("SignIn", "Fail", uc);
												request.complete();
											}
											else
												signincb.accept(((RecordStruct)result).getFieldAsString("Id"));
										}
									});
								}
								else {
									String dcuid = ulLookupResult.getItemAsRecord(0).getFieldAsString("Id");
									
									UpdateRecordRequest req = new UpdateRecordRequest();
									
									req
										.withTable("dcUser")
										.withId(dcuid)
										// TODO add these once UpdateField works with Dynamic Scalar
										//.withUpdateField("dcUsername", fbinfo.getFieldAsString("email"))
										//.withUpdateField("dcEmail", fbinfo.getFieldAsString("email"))
										//.withUpdateField("dcFirstName", fbinfo.getFieldAsString("first_name"))
										//.withUpdateField("dcLastName", fbinfo.getFieldAsString("last_name"))
										.withUpdateField("dcmFacebookId", uid)
										.withUpdateField("dcConfirmed", true);									
									
									// TODO look at fb `locale` and `timezone` too
									
									db.submit(req, new ObjectResult() {										
										@Override
										public void process(CompositeStruct result) {
											if (this.hasErrors()) {
												tc.getTenant().authEvent("SignIn", "Fail", uc);
												request.complete();
											}
											else
												signincb.accept(dcuid);
										}
									});
								}
							}
						}
				);
				
				return;
			}			
			
			// TODO now that we trust the token in Session this won't get called often - think about how to keep
			// auth token fresh in database - especially since the token will expire in 30 minutes
			if ("Verify".equals(op)) {
				
				if (sess == null) {
					OperationContext.switchUser(request.getContext(), UserContext.allocateGuest());
					
					request.errorTr(442);
					request.error("Session not found");
					request.complete();
					return;
				}
				
				// if token is present that is all we use, get rid of token if you want a creds check
				
				String authToken = uc.getAuthToken();
				
				if (StringUtil.isNotEmpty(authToken)) {
					DataRequest tp1 = RequestFactory.verifySessionRequest(uc.getAuthToken());
					
					db.submit(tp1, new ObjectResult() {
						public void process(CompositeStruct result) {
							RecordStruct urec = (RecordStruct) result;
							OperationContext ctx = request.getContext();
							
							if (request.hasErrors() || (urec == null)) {
								tc.getTenant().authEvent(op, "Fail", uc);
								AuthService.this.clearUserContext(sess, ctx);
								request.errorTr(442);
							}
							else {
								//System.out.println("verify existing");
								ListStruct atags = urec.getFieldAsList("AuthorizationTags");
								atags.addItem("User");								
								
								// TODO make locale smart
								String fullname = "";
								
								if (!urec.isFieldEmpty("FirstName"))
									fullname = urec.getFieldAsString("FirstName");
								
								if (!urec.isFieldEmpty("LastName") && StringUtil.isNotEmpty(fullname))
									fullname += " " + urec.getFieldAsString("LastName");
								else if (!urec.isFieldEmpty("LastName"))
									fullname = urec.getFieldAsString("LastName");
								
								if (StringUtil.isEmpty(fullname))
									fullname = "[unknown]";

								UserContext usr = sess.getUser().toBuilder() 
										.withVerified(true)
										.withUserId(urec.getFieldAsString("UserId"))
										.withUsername(urec.getFieldAsString("Username"))
										.withFullName(fullname)	
										.withEmail(urec.getFieldAsString("Email"))
										.withAuthTags(atags)
										.toUserContext();
								
								sess.withUser(usr);
								
								OperationContext.switchUser(ctx, usr);
								
								tc.getTenant().authEvent(op, "Success", usr);
							}
							
							request.complete();
						}
					});
					
					return;
				}				
				
				// else try to authenticate
				RecordStruct creds = uc.getCredentials();  // msg.getFieldAsRecord("Credentials");
				
				if (creds == null) {
					tc.getTenant().authEvent(op, "Fail", uc);
					request.errorTr(442);
					request.complete();
					return;
				}
				
				//System.out.println("auth 1: " + request.getContext().isElevated());
				
				DataRequest tp1 = RequestFactory.signInRequest(creds.getFieldAsString("Username"), 
						creds.getFieldAsString("Password"), creds.getFieldAsString("ClientKeyPrint"));
				
				// TODO for all services, be sure we return all messages from the submit with the message
				db.submit(tp1, new ObjectResult() {
					@Override
					public void process(CompositeStruct result) {
						RecordStruct sirec = (RecordStruct) result;
						OperationContext ctx = request.getContext();
						
						//System.out.println("auth 2: " + request.getContext().isElevated());
						
						if (request.hasErrors() || (sirec == null)) {
							tc.getTenant().authEvent(op, "Fail", uc);
							AuthService.this.clearUserContext(sess, ctx);
							request.errorTr(442);
						}
						else {
							//System.out.println("verify new");
							ListStruct atags = sirec.getFieldAsList("AuthorizationTags");
							atags.addItem("User");
							
							// TODO make locale smart
							String fullname = "";
							
							if (!sirec.isFieldEmpty("FirstName"))
								fullname = sirec.getFieldAsString("FirstName");
							
							if (!sirec.isFieldEmpty("LastName") && StringUtil.isNotEmpty(fullname))
								fullname += " " + sirec.getFieldAsString("LastName");
							else if (!sirec.isFieldEmpty("LastName"))
								fullname = sirec.getFieldAsString("LastName");
							
							if (StringUtil.isEmpty(fullname))
								fullname = "[unknown]";

							UserContext usr = sess.getUser().toBuilder() 
								.withVerified(true)
								.withAuthToken(sirec.getFieldAsString("AuthToken"))
								.withUserId(sirec.getFieldAsString("UserId"))
								.withUsername(sirec.getFieldAsString("Username"))
								.withFullName(fullname)		
								.withEmail(sirec.getFieldAsString("Email"))
								.withAuthTags(atags)
								.toUserContext();
							
							sess.withUser(usr);
							
							OperationContext.switchUser(ctx, usr);
							
							tc.getTenant().authEvent(op, "Success", usr);
						}
						
						request.complete();
					}
				});
				
				return;
			}			
			
			if ("SignOut".equals(op)) {
				db.submit(RequestFactory.signOutRequest(uc.getAuthToken()), new ObjectResult() {
					@Override
					public void process(CompositeStruct result) {
						if (sess != null)
							AuthService.this.clearUserContext(sess, request.getContext());
						
						request.complete();
					}
				});
				
				return;
			}		
		}
		else if ("Recovery".equals(feature)) {
			if ("InitiateSelf".equals(op) || "InitiateAdmin".equals(op)) {
				String user = msg.bodyRecord().getFieldAsString("User");  
				
				DataRequest req = RequestFactory.initiateRecoveryRequest(user);
				
				db.submit(req, new ObjectResult() {
					@Override
					public void process(CompositeStruct result) {
						if (this.hasErrors()) { 
							request.errorTr(442);
						}
						else {
							String code = ((RecordStruct)result).getFieldAsString("Code");
							String email = ((RecordStruct)result).getFieldAsString("Email");
							String email2 = ((RecordStruct)result).getFieldAsString("BackupEmail");
							
							// TODO send email
							
							System.out.println("Sending recovery code: " + code + " to " + email + " and " + email2);
						}
						
						if ("InitiateAdmin".equals(op))
							// return the code/emails to the admin
							request.returnValue(request);
						else
							// don't return to guest
							request.complete();
					}
				});
				
				return;
			}			
		}
		
		request.errorTr(441, this.serviceName(), feature, op);
		request.complete();
	}
	
	public void clearUserContext(Session sess, OperationContext ctx) {
		sess.clearToGuest();
		OperationContext.switchUser(ctx, sess.getUser());
	}
	
	// TODO move to another class
	static public RecordStruct fbSignIn(String token, String secret) {
        try {
        	URL url = null;
        	
        	if (StringUtil.isEmpty(secret)) {
				url = new URL("https://graph.facebook.com/v2.2/me?access_token=" + URLEncoder.encode(token, "UTF-8"));
        	}
        	else {
	            Mac mac = Mac.getInstance("HmacSHA256");
	            
	            mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
	            
	            String verify = HexUtil.bufferToHex(mac.doFinal(token.getBytes())); 
	            
				//System.out.println("verify: " + verify);
				
				url = new URL("https://graph.facebook.com/v2.2/me?access_token=" + URLEncoder.encode(token, "UTF-8")
						+ "&appsecret_proof=" + URLEncoder.encode(verify, "UTF-8"));					
				
				//System.out.println("url: " + url);
        	}
        	
			HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
			 
			con.setRequestProperty("User-Agent", "DivConq/1.0 (Language=Java/8)");
	 
			int responseCode = con.getResponseCode();
	 
			if (responseCode == 200) {
				FuncResult<CompositeStruct> res = CompositeParser.parseJson(con.getInputStream());
				
				//System.out.println("res: " + res.getResult());
				
				return (RecordStruct) res.getResult();
			}
			
			OperationContext.get().error("FB Response Code : " + responseCode);
        } 
        catch (Exception x) {
            OperationContext.get().error("FB error: " + x);
        }
        
        return null;
	}
}
