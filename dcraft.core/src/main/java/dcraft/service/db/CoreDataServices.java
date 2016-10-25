package dcraft.service.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.joda.time.DateTime;

import static dcraft.db.Constants.DB_GLOBAL_TENANT_DB;

import dcraft.bus.IService;
import dcraft.bus.Message;
import dcraft.db.DataRequest;
import dcraft.db.IDatabaseManager;
import dcraft.db.ObjectFinalResult;
import dcraft.db.ObjectResult;
import dcraft.db.ReplicatedDataRequest;
import dcraft.db.common.AddGroupRequest;
import dcraft.db.common.AddUserRequest;
import dcraft.db.common.RequestFactory;
import dcraft.db.common.UpdateGroupRequest;
import dcraft.db.common.UpdateUserRequest;
import dcraft.db.common.UsernameLookupRequest;
import dcraft.db.query.LoadRecordRequest;
import dcraft.db.query.SelectDirectRequest;
import dcraft.db.query.SelectFields;
import dcraft.db.update.InsertRecordRequest;
import dcraft.db.update.RetireRecordRequest;
import dcraft.db.update.ReviveRecordRequest;
import dcraft.db.update.UpdateRecordRequest;
import dcraft.hub.Hub;
import dcraft.hub.HubEvents;
import dcraft.hub.SiteInfo;
import dcraft.lang.op.FuncResult;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.OperationContextBuilder;
import dcraft.lang.op.UserContext;
import dcraft.mod.ExtensionBase;
import dcraft.schema.DbProc;
import dcraft.struct.CompositeStruct;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.util.IOUtil;
import dcraft.util.StringUtil;
import dcraft.work.TaskRun;
import dcraft.xml.XAttribute;
import dcraft.xml.XElement;
import dcraft.xml.XmlReader;

public class CoreDataServices extends ExtensionBase implements IService {
	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");
		RecordStruct rec = msg.getFieldAsRecord("Body");
		
		OperationContext tc = OperationContext.get();
		UserContext uc = tc.getUserContext();
		
		IDatabaseManager db = Hub.instance.getDatabase();
		
		if (db == null) {
			request.errorTr(443);
			request.complete();
			return;
		}
		
		// =========================================================
		//  users
		// =========================================================
		
		if ("Users".equals(feature)) {
			if ("LoadSelf".equals(op) || "LoadUser".equals(op)) {
				LoadRecordRequest req = new LoadRecordRequest()
					.withTable("dcUser")
					.withId("LoadUser".equals(op) ? rec.getFieldAsString("Id") : uc.getUserId())
					.withNow()
					.withSelect(new SelectFields()
						.withField("Id")
						.withField("dcUsername", "Username")
						.withField("dcFirstName", "FirstName")
						.withField("dcLastName", "LastName")
						.withForeignField("dcGroup", "Groups", "dcName")
						.withField("dcEmail", "Email")
						.withField("dcBackupEmail", "BackupEmail")
						.withField("dcLocale", "Locale")
						.withField("dcChronology", "Chronology")
						.withField("dcDescription", "Description")
						.withField("dcConfirmed", "Confirmed")
						.withField("dcAuthorizationTag", "AuthorizationTags")
					);  
				
				db.submit(req, new ObjectFinalResult(request));
				
				return;
			}
						
			if ("UpdateSelf".equals(op) || "UpdateUser".equals(op)) {
				final UpdateUserRequest req = new UpdateUserRequest("UpdateUser".equals(op) ? rec.getFieldAsString("Id") : uc.getUserId());

				if (rec.hasField("Username"))
					req.setUsername(rec.getFieldAsString("Username"));

				if (rec.hasField("FirstName"))
					req.setFirstName(rec.getFieldAsString("FirstName"));

				if (rec.hasField("LastName"))
					req.setLastName(rec.getFieldAsString("LastName"));

				if (rec.hasField("Email"))
					req.setEmail(rec.getFieldAsString("Email"));

				if (rec.hasField("BackupEmail"))
					req.setBackupEmail(rec.getFieldAsString("BackupEmail"));

				if (rec.hasField("Locale"))
					req.setLocale(rec.getFieldAsString("Locale"));

				if (rec.hasField("Chronology"))
					req.setChronology(rec.getFieldAsString("Chronology"));
				
				if (rec.hasField("Password")) 
					req.setPassword(rec.getFieldAsString("Password")); 

				// not allowed for Self (see schema)
				if (rec.hasField("Confirmed")) 
					req.setConfirmed(rec.getFieldAsBoolean("Confirmed"));
				
				// not allowed for Self (see schema)
				if (rec.hasField("Description")) 
					req.setDescription(rec.getFieldAsString("Description"));
				
				// not allowed for Self (see schema)
				if (rec.hasField("AuthorizationTags"))
					req.setAuthorizationTags(rec.getFieldAsList("AuthorizationTags"));
				
				db.submit(req, new ObjectFinalResult(request));
				
				return ;
			}
			
			if ("AddUser".equals(op)) {
				AddUserRequest req = new AddUserRequest(rec.getFieldAsString("Username"));
			
				if (rec.hasField("FirstName"))
					req.setFirstName(rec.getFieldAsString("FirstName"));
			
				if (rec.hasField("LastName"))
					req.setLastName(rec.getFieldAsString("LastName"));
			
				if (rec.hasField("Email"))
					req.setEmail(rec.getFieldAsString("Email"));

				if (rec.hasField("BackupEmail"))
					req.setBackupEmail(rec.getFieldAsString("BackupEmail"));
			
				if (rec.hasField("Locale"))
					req.setLocale(rec.getFieldAsString("Locale"));
			
				if (rec.hasField("Chronology"))
					req.setChronology(rec.getFieldAsString("Chronology"));
				
				if (rec.hasField("Password")) 
					req.setPassword(rec.getFieldAsString("Password"));
				
				if (rec.hasField("Confirmed")) 
					req.setConfirmed(rec.getFieldAsBoolean("Confirmed"));
				else
					req.setConfirmed(true);
				
				if (rec.hasField("ConfirmCode")) 
					req.setConfirmCode(rec.getFieldAsString("ConfirmCode"));
				
				if (rec.hasField("Description")) 
					req.setDescription(rec.getFieldAsString("Description"));
				
				if (rec.hasField("AuthorizationTags"))
					req.setAuthorizationTags(rec.getFieldAsList("AuthorizationTags"));
				
				db.submit(req, new ObjectFinalResult(request));
				
				return;
			}
			
			if ("RetireSelf".equals(op) || "RetireUser".equals(op)) {
				db.submit(new RetireRecordRequest("dcUser", "RetireUser".equals(op) ? rec.getFieldAsString("Id") : uc.getUserId()),
						new ObjectResult() {
							@Override
							public void process(CompositeStruct result) {
								if ("RetireSelf".equals(op)) {
									// be sure we keep the Tenant id
									UserContext uc = request.getContext().getUserContext();
									
									OperationContext.switchUser(request.getContext(), new OperationContextBuilder()
										.withGuestUserTemplate()
										.withTenantId(uc.getTenantId())
										.toUserContext());
								}
								
								request.complete();
							}
						});
				
				return ;
			}
			
			if ("ReviveUser".equals(op)) {
				db.submit(new ReviveRecordRequest("dcUser", rec.getFieldAsString("Id")), new ObjectFinalResult(request));
				
				return ;
			}
			
			if ("SetUserAuthTags".equals(op)) {
				ListStruct users = rec.getFieldAsList("Users");
				ListStruct tags = rec.getFieldAsList("AuthorizationTags");
				
				db.submit(RequestFactory.makeSet("dcUser", "dcAuthorizationTag", users, tags), new ObjectFinalResult(request));
				
				return ;
			}
			
			if ("AddUserAuthTags".equals(op)) {
				ListStruct users = rec.getFieldAsList("Users");
				ListStruct tags = rec.getFieldAsList("AuthorizationTags");
				
				db.submit(RequestFactory.addToSet("dcUser", "dcAuthorizationTag", users, tags), new ObjectFinalResult(request));
				
				return ;
			}
			
			if ("RemoveUserAuthTags".equals(op)) {
				ListStruct users = rec.getFieldAsList("Users");
				ListStruct tags = rec.getFieldAsList("AuthorizationTags");
				
				db.submit(RequestFactory.removeFromSet("dcUser", "dcAuthorizationTag", users, tags), new ObjectFinalResult(request));
				
				return ;
			}
			
			if ("UsernameLookup".equals(op)) {
				db.submit(new UsernameLookupRequest(rec.getFieldAsString("Username")), new ObjectFinalResult(request));
				
				return ;
			}

			// use with discretion
			if ("ListUsers".equals(op)) {
				db.submit(
					new SelectDirectRequest()
						.withTable("dcUser")
						.withSelect(new SelectFields()
							.withField("Id")
							.withField("dcUsername", "Username")
							.withField("dcFirstName", "FirstName")
							.withField("dcLastName", "LastName")
							.withField("dcEmail", "Email")), 
					new ObjectFinalResult(request));
				
				return ;
			}
		}
		
		// =========================================================
		//  Tenants
		// =========================================================
		
		if ("Tenants".equals(feature)) {
			if ("LoadTenant".equals(op) || "MyLoadTenant".equals(op)) {
				LoadRecordRequest req = new LoadRecordRequest()
					.withTable(DB_GLOBAL_TENANT_DB)
					.withId("MyLoadTenant".equals(op) ? uc.getTenantId() : rec.getFieldAsString("Id"))
					.withNow()
					.withSelect(new SelectFields()
						.withField("Id")
						.withField("dcTitle", "Title")
						.withField("dcAlias", "Alias")
						.withField("dcDescription", "Description")
						.withField("dcObscureClass", "ObscureClass")
						.withField("dcName", "Names")
					);
				
				req.withTenant("MyLoadTenant".equals(op) ? uc.getTenantId() : rec.getFieldAsString("Id"));
				
				db.submit(req, new ObjectFinalResult(request));
				
				return;
			}
						
			if ("UpdateTenant".equals(op) || "MyUpdateTenant".equals(op)) {
				ReplicatedDataRequest req = new UpdateRecordRequest()
					.withTable(DB_GLOBAL_TENANT_DB)
					.withId("MyUpdateTenant".equals(op) ? uc.getTenantId() : rec.getFieldAsString("Id"))
					.withConditionallySetFields(rec, "Title", "dcTitle", "Alias", "dcAlias", "Description", "dcDescription", "ObscureClass", "dcObscureClass")
					.withConditionallySetList(rec, "Names", "dcName");
				
				req.withTenant("MyUpdateTenant".equals(op) ? uc.getTenantId() : rec.getFieldAsString("Id"));
				
				db.submit(req, new ObjectResult() {
					@Override
					public void process(CompositeStruct result) {
						Hub.instance.fireEvent(HubEvents.TenantUpdated, rec.getFieldAsString("Id"));					
						request.returnValue(result);
					}
				});
				
				return ;
			}
			
			if ("AddTenant".equals(op)) {
				ReplicatedDataRequest req = new InsertRecordRequest()
					.withTable(DB_GLOBAL_TENANT_DB)
					.withConditionallySetFields(rec, "Title", "dcTitle", "Alias", "dcAlias", "Description", "dcDescription", "ObscureClass", "dcObscureClass")
					.withSetList("dcName", rec.getFieldAsList("Names"));
				
				db.submit(req, new ObjectResult() {
					@Override
					public void process(CompositeStruct result) {
						SiteInfo site = OperationContext.get().getSite();
						
						try {
							Files.createDirectories(site.resolvePath("files"));
							Files.createDirectories(site.resolvePath("galleries"));
							Files.createDirectories(site.resolvePath("www"));
						} 
						catch (IOException x) {
							request.error("Unable to create directories for new Tenant: " + x);
							request.returnEmpty();
							return;
						}
						
						Path cpath = site.resolvePath("config/settings.xml");

						XElement tenantsettings = new XElement("Settings",
								new XElement("Web", 
										new XAttribute("UI", "Custom"),
										new XAttribute("SiteTitle", rec.getFieldAsString("Title")),
										new XAttribute("SiteAuthor", rec.getFieldAsString("Title")),
										new XAttribute("SiteCopyright", new DateTime().getYear() + ""),
										new XElement("Package", 
												new XAttribute("Name", "dcWeb")
										),
										new XElement("Package", 
												new XAttribute("Name", "dc/dcCms")
										)
								)
						);

						IOUtil.saveEntireFile(cpath, tenantsettings.toString(true));
						
						Hub.instance.fireEvent(HubEvents.TenantAdded, ((RecordStruct)result).getFieldAsString("Id"));
						
						request.returnValue(result);
					}
				});
				
				return;
			}
						
			if ("ImportTenant".equals(op)) {
				SiteInfo site = OperationContext.get().getSite();
				String alias = rec.getFieldAsString("Alias");
				
				Path cpath = site.resolvePath("config/settings.xml");
				
				if (Files.notExists(cpath)) {
					request.error("Settings file not present.");
					request.complete();
					return;
				}
				
				FuncResult<XElement> sres = XmlReader.loadFile(cpath, false);
				
				if (sres.hasErrors()) {
					request.complete();
					return;
				}
				
				XElement domainsettings = sres.getResult();
				
				String title = domainsettings.getAttribute("Title");
				String desc = "";
				XElement del = domainsettings.find("Description");
				
				if (del != null)
					desc = del.getValue();
				
				String fdesc = desc;
				
				String obs = "";
				XElement oel = domainsettings.find("ObscureClass");
				
				if (oel != null)
					obs = oel.getValue();
				
				if (StringUtil.isEmpty(obs))
					obs = "dcraft.util.StandardSettingsObfuscator";
				
				String fobs = obs;
				
				ListStruct dnames = new ListStruct();
				
				for (XElement del2 : domainsettings.selectAll("Tenant"))
					dnames.addItem(del2.getAttribute("Name"));
				
				DataRequest req = new DataRequest("dcLoadTenants");		// must be in root .withRootTenant();	// use root for this request
				
				db.submit(req, new ObjectResult() {
					@Override
					public void process(CompositeStruct result) {
						// if this fails the hub cannot start
						if (this.hasErrors()) {
							request.complete();
							return;
						}
						
						ListStruct domains = (ListStruct) result;
						
						for (Struct d : domains.getItems()) {
							RecordStruct drec = (RecordStruct) d;
							
							String did = drec.getFieldAsString("Id");
							String dalais = drec.getFieldAsString("Alias");
							
							if (!dalais.equals(alias))
								continue;
							
							ReplicatedDataRequest req = new UpdateRecordRequest()
								.withTable(DB_GLOBAL_TENANT_DB)
								.withId(did)
								.withUpdateField("dcTitle", title)
								.withUpdateField("dcAlias", alias)
								.withUpdateField("dcDescription", fdesc)
								.withUpdateField("dcObscureClass", fobs)
								.withSetList("dcName", dnames);
							
							// updates execute on the domain directly
							req.withTenant(did);
							
							db.submit(req, new ObjectResult() {
								@Override
								public void process(CompositeStruct result) {
									Hub.instance.fireEvent(HubEvents.TenantUpdated, did);
									
									request.returnValue(new RecordStruct().withField("Id", did));
								}
							});
							
							return;
						}
						
						ReplicatedDataRequest req = new InsertRecordRequest()
							.withTable(DB_GLOBAL_TENANT_DB)
							.withUpdateField("dcTitle", title)
							.withUpdateField("dcAlias", alias)
							.withUpdateField("dcDescription", fdesc)
							.withUpdateField("dcObscureClass", fobs)
							.withSetList("dcName", dnames);
						
						db.submit(req, new ObjectResult() {
							@Override
							public void process(CompositeStruct result) {
								Hub.instance.fireEvent(HubEvents.TenantAdded, ((RecordStruct)result).getFieldAsString("Id"));
								
								request.returnValue(result);
							}
						});
					}
				});
				
				return;
			}
			
			if ("RetireTenant".equals(op)) {
				db.submit(new RetireRecordRequest(DB_GLOBAL_TENANT_DB, rec.getFieldAsString("Id")).withTenant(rec.getFieldAsString("Id")), new ObjectFinalResult(request));
				
				return ;
			}
			
			if ("ReviveTenant".equals(op)) {
				db.submit(new ReviveRecordRequest(DB_GLOBAL_TENANT_DB, rec.getFieldAsString("Id")).withTenant(rec.getFieldAsString("Id")), new ObjectFinalResult(request));
				
				return ;
			}			
		}
		
		// =========================================================
		//  groups
		// =========================================================
		
		if ("Groups".equals(feature)) {
			if ("LoadGroup".equals(op)) {
				LoadRecordRequest req = new LoadRecordRequest()
					.withTable("dcGroup")
					.withId(rec.getFieldAsString("Id"))
					.withNow()
					.withSelect(new SelectFields()
						.withField("Id")
						.withField("dcName", "Name")
						.withField("dcDescription", "Description")
						.withReverseForeignField("Users", "dcUser", "dcGroup", "dcUsername")
						.withField("dcAuthorizationTag", "AuthorizationTags")
					);
				
				db.submit(req, new ObjectFinalResult(request));
				
				return ;
			}
						
			if ("UpdateGroup".equals(op)) {
				final UpdateGroupRequest req = new UpdateGroupRequest(rec.getFieldAsString("Id"));

				if (rec.hasField("Name"))
					req.setName(rec.getFieldAsString("Name"));
				
				if (rec.hasField("Description")) 
					req.setDescription(rec.getFieldAsString("Description"));
				
				if (rec.hasField("AuthorizationTags"))
					req.setAuthorizationTags(rec.getFieldAsList("AuthorizationTags"));
				
				db.submit(req, new ObjectFinalResult(request));
				
				return ;
			}
			
			if ("AddGroup".equals(op)) {
				final AddGroupRequest req = new AddGroupRequest(rec.getFieldAsString("Name"));
				
				if (rec.hasField("Description")) 
					req.setDescription(rec.getFieldAsString("Description"));
				
				if (rec.hasField("AuthorizationTags"))
					req.setAuthorizationTags(rec.getFieldAsList("AuthorizationTags"));
				
				db.submit(req, new ObjectFinalResult(request));
				
				return ;
			}
			
			if ("RetireGroup".equals(op)) {
				db.submit(new RetireRecordRequest("dcGroup", rec.getFieldAsString("Id")), new ObjectFinalResult(request));				
				return ;
			}
			
			if ("ReviveGroup".equals(op)) {
				db.submit(new ReviveRecordRequest("dcGroup", rec.getFieldAsString("Id")), new ObjectFinalResult(request));				
				return ;
			}
			
			if ("SetGroupAuthTags".equals(op)) {
				final ListStruct groups = rec.getFieldAsList("Groups");
				final ListStruct tags = rec.getFieldAsList("AuthorizationTags");
				
				db.submit(RequestFactory.makeSet("dcGroup", "dcAuthorizationTag", groups, tags), new ObjectFinalResult(request));
				
				return ;
			}
			
			if ("AddGroupAuthTags".equals(op)) {
				final ListStruct groups = rec.getFieldAsList("Groups");
				final ListStruct tags = rec.getFieldAsList("AuthorizationTags");
				
				db.submit(RequestFactory.addToSet("dcGroup", "dcAuthorizationTag", groups, tags), new ObjectFinalResult(request));
				
				return ;
			}
			
			if ("RemoveGroupAuthTags".equals(op)) {
				final ListStruct groups = rec.getFieldAsList("Groups");
				final ListStruct tags = rec.getFieldAsList("AuthorizationTags");
				
				db.submit(RequestFactory.removeFromSet("dcGroup", "dcAuthorizationTag", groups, tags), new ObjectFinalResult(request));
				
				return ;
			}

			// use with discretion
			if ("ListGroups".equals(op)) {
				db.submit(
					new SelectDirectRequest()
						.withTable("dcGroup")
						.withSelect(new SelectFields()
							.withField("Id")
							.withField("dcName", "Name")), 
					new ObjectFinalResult(request));
				
				return ;
			}
			
			if ("SetUsersToGroups".equals(op)) {
				final ListStruct groups = rec.getFieldAsList("Groups");
				final ListStruct users = rec.getFieldAsList("Users");
				
				db.submit(RequestFactory.makeSet("dcUser", "dcGroup", users, groups), new ObjectFinalResult(request));
				
				return ;
			}
			
			if ("AddUsersToGroups".equals(op)) {
				final ListStruct groups = rec.getFieldAsList("Groups");
				final ListStruct users = rec.getFieldAsList("Users");
				
				db.submit(RequestFactory.addToSet("dcUser", "dcGroup", users, groups), new ObjectFinalResult(request));
				
				return ;
			}
			
			if ("RemoveUsersFromGroups".equals(op)) {
				final ListStruct groups = rec.getFieldAsList("Groups");
				final ListStruct users = rec.getFieldAsList("Users");
				
				db.submit(RequestFactory.removeFromSet("dcUser", "dcGroup", users, groups), new ObjectFinalResult(request));
				
				return ;
			}
		}
		
		// =========================================================
		//  globals
		// =========================================================
		
		if ("Globals".equals(feature)) {
			if ("DollarO".equals(op)) {
				DataRequest req = new DataRequest("dcKeyQuery")
					.withParams(rec);
				
				db.submit(req, new ObjectFinalResult(request));
				
				return ;
			}
			
			if ("Kill".equals(op)) {
				DataRequest req = new DataRequest("dcKeyKill")
					.withParams(rec);
				
				db.submit(req, new ObjectFinalResult(request));
				
				return ;
			}
		}	
		
		// =========================================================
		//  database directly
		// =========================================================
		if ("Database".equals(feature)) {
			if ("ExecuteProc".equals(op)) {
				String proc = rec.getFieldAsString("Proc");
				
				DbProc pdef = request.getContext().getSchema().getDbProc(proc);
				
				if (!request.getContext().getUserContext().isTagged(pdef.securityTags)) {
					request.errorTr(434);
					request.complete();
					return;
				}
				
				DataRequest req = new DataRequest(proc)
					.withParams(rec.getFieldAsComposite("Params"));
				
				db.submit(req, new ObjectFinalResult(request));
				
				return ;
			}
		}	
		
		request.errorTr(441, this.serviceName(), feature, op);
		request.complete();
	}
}
