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

/**
 * Support for testing the dcFileSever demo.  This shows the DivConq remote API
 * system support. 
 */
package dcraft.tool;

import static dcraft.db.Constants.DB_GLOBAL_INDEX_SUB;
import static dcraft.db.Constants.DB_GLOBAL_RECORD;
import static dcraft.db.Constants.DB_GLOBAL_RECORD_META;
import static dcraft.db.Constants.DB_GLOBAL_ROOT_TENANT;
import static dcraft.db.Constants.DB_GLOBAL_ROOT_USER;
import static dcraft.db.Constants.DB_OMEGA_MARKER_ARRAY;
import static dcraft.db.Constants.DB_GLOBAL_TENANT_DB;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.rocksdb.BackupInfo;
import org.rocksdb.BackupableDBOptions;
import org.rocksdb.RestoreBackupableDB;
import org.rocksdb.RestoreOptions;
import org.rocksdb.RocksIterator;

import dcraft.api.ApiSession;
import dcraft.db.Constants;
import dcraft.db.UtilitiesAdapter;
import dcraft.db.rocks.DatabaseManager;
import dcraft.db.rocks.RocksInterface;
import dcraft.db.rocks.keyquery.KeyQuery;
import dcraft.db.util.ByteUtil;
import dcraft.hub.TenantInfo;
import dcraft.hub.TenantManager;
import dcraft.hub.Foreground;
import dcraft.hub.Hub;
import dcraft.hub.ILocalCommandLine;
import dcraft.lang.op.FuncResult;
import dcraft.lang.op.OperationCallback;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.OperationResult;
import dcraft.script.Activity;
import dcraft.util.HexUtil;
import dcraft.util.IOUtil;
import dcraft.util.ISettingsObfuscator;
import dcraft.util.StringUtil;
import dcraft.work.Task;
import dcraft.xml.XAttribute;
import dcraft.xml.XElement;

public class HubUtil implements ILocalCommandLine {
	/*
	 * Consider developing CLI further with libraries like these:
	 * 
	 * https://github.com/jline/jline3
	 * https://github.com/fusesource/jansi
	 * 
	 */
	@Override
	public void run(final Scanner scan, final ApiSession api) {
		boolean running = true;

		while(running) {
			try {
				System.out.println();
				System.out.println("-----------------------------------------------");
				System.out.println("   Hub General Utils");
				System.out.println("-----------------------------------------------");
				System.out.println("0)   Exit");
				System.out.println("1)   dcDatabase Utils");
				System.out.println("2)   Local Utilities");
				System.out.println("3)   Crypto Utilities");
				System.out.println("100) dcScript GUI Debugger");
				System.out.println("101) dcScript Run Script");

				String opt = scan.nextLine();
				
				Long mopt = StringUtil.parseInt(opt);
				
				if (mopt == null)
					continue;
				
				switch (mopt.intValue()) {
				case 0: {
					running = false;
					break;
				}
				
				case 1: {
					this.utilityMenu(scan);
					break;
				}
				
				case 2: {
					Foreground.utilityMenu(scan);					
					break;
				}
				
				case 3: {
					this.cryptoMenu(scan);
					break;
				}
				
				/*
				case 100: {
					ScriptUtility.goSwing(null);					
					break;
				}
				*/
				
				case 101: {
					System.out.println("*** Run A dcScript ***");
					System.out.println("If you are looking for something to try, consider one of these:");
					System.out.println("  ./packages/dcTest/dcs/examples/99-bottles.dcs.xml");
					System.out.println("  ./packages/dcTest/dcs/examples/99-bottles-debug.dcs.xml");
					
					System.out.println();
					System.out.println("Path to script to run: ");
					String spath = scan.nextLine();
			    	
					System.out.println();
					
					FuncResult<CharSequence> rres = IOUtil.readEntireFile(Paths.get(spath));
					
					if (rres.hasErrors()) {
						System.out.println("Error reading script: " + rres.getMessage());
						break;
					}
					
					Activity act = new Activity();
					
					OperationResult compilelog = act.compile(rres.getResult().toString());
					
					if (compilelog.hasErrors()) {
						System.out.println("Error compiling script: " + compilelog.getMessage());
						break;
					}
					
					Task task = Task.taskWithRootContext()
						.withTitle(act.getScript().getXml().getAttribute("Title", "Debugging dcScript"))	
						.withTimeout(0)							// no timeout in editor mode
						.withWork(act);
					
					Hub.instance.getWorkPool().submit(task);
					
					break;
				}
				
				}
			}
			catch (Exception x) {
				System.out.println("Command Line Error: " + x);
			}
		}
	}
		
	public void utilityMenu(Scanner scan) { 	
		boolean running = true;
		
		while(running) {
			try {
				System.out.println();
				System.out.println("-----------------------------------------------");
				System.out.println("   Hub " + Hub.instance.getResources().getHubId() + " DB Utility Menu");
				System.out.println("-----------------------------------------------");
				System.out.println("0)  Exit");
				System.out.println("1)  Database Dump");
				System.out.println("2)  Create Database");
				System.out.println("3)  Initialize Root Tenant (create db if not present)");
				System.out.println("4)  Backup Database");
				System.out.println("5)  Database Backup Info");
				System.out.println("6)  Restore Database");
				System.out.println("7)  Compact Database - TODO");
				System.out.println("8)  Mess Database");
				System.out.println("9)  Re-index dcTables");
	
				String opt = scan.nextLine();
				
				Long mopt = StringUtil.parseInt(opt);
				
				if (mopt == null)
					continue;
				
				switch (mopt.intValue()) {
				case 0:
					running = false;
					break;
				
				case 1: {
					Path dbpath = this.getDbPath(scan);
					
					if (dbpath == null) 
						break;
				
					System.out.print("Dump keys too (y/n): ");
					boolean keystoo = scan.nextLine().toLowerCase().equals("y");
				
					DatabaseManager db = this.getDatabase(dbpath, false);
					
					if (db == null) {
						System.out.println("Database missing or bad!");
						break;
					}					
					
					try {
						RocksInterface adapt = db.allocateAdapter();
						
						RocksIterator it = adapt.iterator();

						try {
							it.seekToFirst();
							
							while (it.isValid()) {
								byte[] key = it.key();						
								
								if (key[0] == Constants.DB_TYPE_MARKER_OMEGA) {
									System.out.println("END");
									break;
								}
								
								byte[] val = it.value();
								
								if (keystoo)
									System.out.println("Hex Key: " + HexUtil.bufferToHex(key));
								
								List<Object> keyParts = ByteUtil.extractKeyParts(key);
								
								for (Object p : keyParts)
									System.out.print((p == null) ? " / " : p.toString() + " / ");
								
								System.out.println(" = " + ByteUtil.extractValue(val));
								
								it.next();
							}
						}
						finally {
							it.close();
						}
					}
					finally {
						db.stop();
					}
					
					break;
				}
				
				case 2: {
					System.out.println("Create Database");
					Path dbpath = this.getDbPath(scan);
					
					if (dbpath == null) 
						break;
				
					DatabaseManager db = this.getDatabase(dbpath, true);
					
					if (db == null) {
						System.out.println("Database missing or bad!");
						break;
					}					
					
					try {
						RocksInterface dbconn = db.allocateAdapter();
						
						byte[] x = dbconn.get(DB_OMEGA_MARKER_ARRAY);
						
						if (x == null) 
							System.out.println("Error creating database!");
						else
							System.out.println("Database created");
					}
					finally {
						db.stop();
					}
					
					break;
				}
				
				case 3: {
					System.out.println("Initialize Root Tenant");
					Path dbpath = this.getDbPath(scan);
					
					if (dbpath == null) 
						break;

					System.out.print("Obfuscator Class (empty for default): ");
					String obfclass = scan.nextLine();
					
					if (StringUtil.isEmpty(obfclass))
						obfclass = "dcraft.util.StandardSettingsObfuscator";

					System.out.print("Obfuscator Seed (empty for random): ");
					String obfseed = scan.nextLine();
					
					if (StringUtil.isEmpty(obfseed)) {
						XElement obfconfig = new XElement("Clock");
						
						Hub.instance.getClock().getObfuscator().configure(obfconfig);
						
						obfseed = obfconfig.getAttribute("Feed");
					}

					System.out.print("Root Tenant Name (empty for localhost only): ");
					String dname = scan.nextLine();

					System.out.print("Global Root Password (required): ");
					String password = scan.nextLine();
					
					if (StringUtil.isEmpty(password)) {
						System.out.println("required!");
						break;
					}

					System.out.print("Root User Email (required): ");
					String email = scan.nextLine();
					
					if (StringUtil.isEmpty(email)) {
						System.out.println("required!");
						break;
					}
					
					DatabaseManager db = this.getDatabase(dbpath, true);
					
					if (db == null) {
						System.out.println("Database missing or bad!");
						break;
					}					
					
					try {
						ISettingsObfuscator obfuscator = TenantInfo.prepTenantObfuscator(obfclass, obfseed);
						
						if (obfuscator == null) {
							OperationContext.get().error("dcDatabase prep error, obfuscator bad");
							return;
						}
					
						RocksInterface dbconn = db.allocateAdapter();
						
						BigDecimal stamp = db.allocateStamp(0);
						
						// insert root domain title
						dbconn.set(DB_GLOBAL_RECORD, DB_GLOBAL_ROOT_TENANT, DB_GLOBAL_TENANT_DB, DB_GLOBAL_ROOT_TENANT, "dcTitle", stamp, "Data", "Root Domain");
						
						dbconn.set(DB_GLOBAL_RECORD, DB_GLOBAL_ROOT_TENANT, DB_GLOBAL_TENANT_DB, DB_GLOBAL_ROOT_TENANT, "dcAlias", stamp, "Data", "root");
						
						// insert root domain name
						dbconn.set(DB_GLOBAL_RECORD, DB_GLOBAL_ROOT_TENANT, DB_GLOBAL_TENANT_DB, DB_GLOBAL_ROOT_TENANT, "dcName", "root", stamp, "Data", "root");
						dbconn.set(DB_GLOBAL_RECORD, DB_GLOBAL_ROOT_TENANT, DB_GLOBAL_TENANT_DB, DB_GLOBAL_ROOT_TENANT, "dcName", "localhost", stamp, "Data", "localhost");
						
						if (StringUtil.isNotEmpty(dname)) 
							dbconn.set(DB_GLOBAL_RECORD, DB_GLOBAL_ROOT_TENANT, DB_GLOBAL_TENANT_DB, DB_GLOBAL_ROOT_TENANT, "dcName", dname, stamp, "Data", dname);
						
						dbconn.set(DB_GLOBAL_RECORD, DB_GLOBAL_ROOT_TENANT, DB_GLOBAL_TENANT_DB, DB_GLOBAL_ROOT_TENANT, "dcObscureClass", stamp, "Data", obfclass);
						dbconn.set(DB_GLOBAL_RECORD, DB_GLOBAL_ROOT_TENANT, DB_GLOBAL_TENANT_DB, DB_GLOBAL_ROOT_TENANT, "dcObscureSeed", stamp, "Data", obfseed);
						
						// insert global root user password 
						dbconn.set(DB_GLOBAL_RECORD, DB_GLOBAL_ROOT_TENANT, DB_GLOBAL_TENANT_DB, DB_GLOBAL_ROOT_TENANT, "dcGlobalPassword", stamp, "Data", obfuscator.hashPassword(password));

						/*
						XElement domainsettings = new XElement("Settings",
								new XElement("Web", 
										new XAttribute("UI", "Custom"),
										new XAttribute("SiteTitle", "Root Tenant Manager"),
										new XAttribute("SiteAuthor", "DivConq"),
										new XAttribute("SiteCopyright", new DateTime().getYear() + ""),
										new XAttribute("HomePath", "/tenants/root/Home"),								
										new XElement("Package", 
												new XAttribute("Name", "dcWeb")
										),
										new XElement("Global", 
												new XAttribute("Script", "/dcw/js/root.js")		
										)
								)
						);
						*/
						
						//dbconn.set(DB_GLOBAL_RECORD, DB_GLOBAL_ROOT_DOMAIN, DB_GLOBAL_TENANT_DB, DB_GLOBAL_ROOT_DOMAIN, "dcCompiledSettings", stamp, "Data", domainsettings);
						
						// insert root domain index
						dbconn.set(DB_GLOBAL_RECORD, DB_GLOBAL_ROOT_TENANT, DB_GLOBAL_TENANT_DB, DB_GLOBAL_ROOT_TENANT, Constants.DB_GLOBAL_TENANT_IDX_DB, DB_GLOBAL_ROOT_TENANT, stamp, "Data", DB_GLOBAL_ROOT_TENANT);
						
						// insert hub domain record id sequence
						dbconn.set(DB_GLOBAL_RECORD_META, DB_GLOBAL_TENANT_DB, "Id", "00000", 1);
						
						// insert root domain record count
						dbconn.set(DB_GLOBAL_RECORD_META, DB_GLOBAL_ROOT_TENANT, DB_GLOBAL_TENANT_DB, "Count", 1);
								
						String unamesub = db.allocateSubkey();
						
						// insert root user name
						dbconn.set(DB_GLOBAL_RECORD, DB_GLOBAL_ROOT_TENANT, "dcUser", DB_GLOBAL_ROOT_USER, "dcUsername", unamesub, stamp, "Data", "root");
						// increment index count
						dbconn.inc(DB_GLOBAL_INDEX_SUB, DB_GLOBAL_ROOT_TENANT, "dcUser", "dcUsername", "root");					
						// set the new index new
						dbconn.set(DB_GLOBAL_INDEX_SUB, DB_GLOBAL_ROOT_TENANT, "dcUser", "dcUsername", "root", DB_GLOBAL_ROOT_USER, unamesub, null);
						
						String emailsub = db.allocateSubkey();
	
						// insert root user email
						dbconn.set(DB_GLOBAL_RECORD, DB_GLOBAL_ROOT_TENANT, "dcUser", DB_GLOBAL_ROOT_USER, "dcEmail", emailsub, stamp, "Data", email);
						// increment index count
						dbconn.inc(DB_GLOBAL_INDEX_SUB, DB_GLOBAL_ROOT_TENANT, "dcUser", "dcEmail", email.toLowerCase(Locale.ROOT));
						// set the new index new
						dbconn.set(DB_GLOBAL_INDEX_SUB, DB_GLOBAL_ROOT_TENANT, "dcUser", "dcEmail", email.toLowerCase(Locale.ROOT), DB_GLOBAL_ROOT_USER, emailsub, null);
						
						// insert root user auth tags
						dbconn.set(DB_GLOBAL_RECORD, DB_GLOBAL_ROOT_TENANT, "dcUser", DB_GLOBAL_ROOT_USER, "dcAuthorizationTag", "SysAdmin", stamp, "Data", "SysAdmin");
						dbconn.set(DB_GLOBAL_RECORD, DB_GLOBAL_ROOT_TENANT, "dcUser", DB_GLOBAL_ROOT_USER, "dcAuthorizationTag", "Admin", stamp, "Data", "Admin");
						//dbconn.set(DB_GLOBAL_RECORD, DB_GLOBAL_ROOT_DOMAIN, "dcUser", DB_GLOBAL_ROOT_USER, "dcAuthorizationTag", "PowerUser", stamp, "Data", "PowerUser");
						
						// increment index count
						dbconn.inc(DB_GLOBAL_INDEX_SUB, DB_GLOBAL_ROOT_TENANT, "dcUser", "dcAuthorizationTag", "Admin".toLowerCase(Locale.ROOT));
						// set the new index new
						dbconn.set(DB_GLOBAL_INDEX_SUB, DB_GLOBAL_ROOT_TENANT, "dcUser", "dcAuthorizationTag", "Admin".toLowerCase(Locale.ROOT), DB_GLOBAL_ROOT_USER, "Admin", null);
						
						// increment index count
						dbconn.inc(DB_GLOBAL_INDEX_SUB, DB_GLOBAL_ROOT_TENANT, "dcUser", "dcAuthorizationTag", "SysAdmin".toLowerCase(Locale.ROOT));
						// set the new index new
						dbconn.set(DB_GLOBAL_INDEX_SUB, DB_GLOBAL_ROOT_TENANT, "dcUser", "dcAuthorizationTag", "SysAdmin".toLowerCase(Locale.ROOT), DB_GLOBAL_ROOT_USER, "SysAdmin", null);
						
						// insert hub domain record id sequence - set to 2 because root and guest are both users - guest just isn't entered
						dbconn.set(DB_GLOBAL_RECORD_META, "dcUser", "Id", "00000", 2);
						
						// insert root domain record count
						dbconn.set(DB_GLOBAL_RECORD_META, DB_GLOBAL_ROOT_TENANT, "dcUser", "Count", 1);	
					}
					finally {
						db.stop();
					}
					
					break;
				}
				
				case 4: {
					System.out.println("Backup Database");
					Path dbpath = this.getDbPath(scan);
					
					if (dbpath == null) 
						break;
				
					DatabaseManager db = this.getDatabase(dbpath, true);
					
					if (db == null) {
						System.out.println("Database missing or bad!");
						break;
					}					
					
					try {
						db.backup();  
						System.out.println("Database backed up!");
					}
					catch (Exception x) {
						System.out.println("Error backing up database: " + x);
					}
					finally {
						db.stop();
					}
					
					break;
				}
				
				case 5: {
					System.out.println("Backup Database Stats");
					Path dbpath = this.getDbPath(scan);
					
					if (dbpath == null) 
						break;
				
					DatabaseManager db = this.getDatabase(dbpath, true);
					
					if (db == null) {
						System.out.println("Database missing or bad!");
						break;
					}					
					
					try {
						List<BackupInfo> list = db.dbBackup().getBackupInfo();
						
						for (BackupInfo info : list) {
							System.out.println("Backup: " + info.backupId() + " size: " + info.size() + " stamp: " + info.timestamp());
						}
					}
					catch (Exception x) {
						System.out.println("Error on database: " + x);
					}
					finally {
						db.stop();
					}
					
					break;
				}
				
				case 6: {
					System.out.println("Restore Database");
					Path dbpath = this.getDbPath(scan);
					
					if (dbpath == null) 
						break;
					
					String dbbakpath = "./datastore-bak/" + dbpath.getFileName().toString();	// TODO configure location
					
					BackupableDBOptions bdb = new BackupableDBOptions(dbbakpath);
				
					RestoreBackupableDB restore = new RestoreBackupableDB(bdb);
					
					RestoreOptions ropts = new RestoreOptions(false);
					
					try {
						restore.restoreDBFromLatestBackup(dbpath.toString(), dbpath.toString(), ropts);
						restore.close();
						System.out.println("Database restored!");
					}
					catch (Exception x) {
						System.out.println("Error restoring database: " + x);
					}
					
					break;
				}
				
				case 7: {
					System.out.println("Compact Database");
					Path dbpath = this.getDbPath(scan);
					
					if (dbpath == null) 
						break;
				
					DatabaseManager db = this.getDatabase(dbpath, true);
					
					if (db == null) {
						System.out.println("Database missing or bad!");
						break;
					}					
					
					try {
						db.dbDirect().compactRange();
						System.out.println("Database compacted up!");
					}
					catch (Exception x) {
						System.out.println("Error compacting database: " + x);
					}
					finally {
						db.stop();
					}
					
					break;
				}
				
				case 8: {
					System.out.println("Messy Database");
					Path dbpath = this.getDbPath(scan);
					
					if (dbpath == null) 
						break;
				
					DatabaseManager db = this.getDatabase(dbpath, true);
					
					if (db == null) {
						System.out.println("Database missing or bad!");
						break;
					}					
					
					try {
						RocksInterface dbconn = db.allocateAdapter();
						
						BigDecimal stamp = db.allocateStamp(0);
						
						dbconn.set(DB_GLOBAL_RECORD, DB_GLOBAL_ROOT_TENANT, DB_GLOBAL_TENANT_DB, DB_GLOBAL_ROOT_TENANT, "dcTitle", stamp, "Data", "BLAH!!");
						
						dbconn.set(DB_GLOBAL_RECORD, DB_GLOBAL_ROOT_TENANT, DB_GLOBAL_TENANT_DB, DB_GLOBAL_ROOT_TENANT, "dcAlias", stamp, "Data", "foobar");
						
						System.out.println("Database messed up!");
					}
					catch (Exception x) {
						System.out.println("Error messing up database: " + x);
					}
					finally {
						db.stop();
					}
					
					break;
				}
				
				case 9: {
					System.out.println("Re-index db");
					Path dbpath = this.getDbPath(scan);
					
					if (dbpath == null) 
						break;
				
					DatabaseManager db = this.getDatabase(dbpath, true);
					
					if (db == null) {
						System.out.println("Database missing or bad!");
						break;
					}					
					
					try {
						TenantManager dm = new TenantManager();
						
						dm.initFromDB(db, new OperationCallback() {							
							@Override
							public void callback() {
								UtilitiesAdapter adp = new UtilitiesAdapter(db, dm);
								adp.rebuildIndexes();
								
								System.out.println("Database indexed!");
							}
						});
					}
					catch (Exception x) {
						System.out.println("Error indexing database: " + x);
					}
					finally {
						db.stop();
					}
					
					break;
				}
				
				/*
				case 212: {
					RocksInterface adapt = ((DatabaseManager)Hub.instance.getDatabase()).allocateAdapter();
					
					KeyQuery kq = new KeyQuery(adapt, new MatchKeyLevel("Record"), 
							new MatchKeyLevel("Person"), new MatchKeyLevel(2045), new MatchKeyLevel("Name"));
					
					this.dumpQuery(kq);
					
					break;
				}
				
				case 213: {
					RocksInterface adapt = ((DatabaseManager)Hub.instance.getDatabase()).allocateAdapter();
					
					KeyQuery kq = new KeyQuery(adapt, new MatchKeyLevel("Record"), 
							new MatchKeyLevel("Person"), new MatchKeyLevel(2045), new WildcardKeyLevel());
					
					this.dumpQuery(kq);
					
					break;
				}
				
				case 214: {
					RocksInterface adapt = ((DatabaseManager)Hub.instance.getDatabase()).allocateAdapter();
					
					KeyQuery kq = new KeyQuery(adapt, new MatchKeyLevel("Record"), 
							new MatchKeyLevel("Person"), new WildcardKeyLevel(), new MatchKeyLevel("Name"));
					
					kq.setBrowseMode(true);
					
					this.dumpQuery(kq);
					
					break;
				}
				
				case 215: {
					RocksInterface adapt = ((DatabaseManager)Hub.instance.getDatabase()).allocateAdapter();
					
					KeyQuery kq = new KeyQuery(adapt, new MatchKeyLevel("Record"), 
							new WildcardKeyLevel(), new WildcardKeyLevel(), new MatchKeyLevel("Name"));
					
					this.dumpQuery(kq);
					
					break;
				}
				
				case 216: {
					RocksInterface adapt = ((DatabaseManager)Hub.instance.getDatabase()).allocateAdapter();
					
					KeyQuery kq = new KeyQuery(adapt, new WildcardKeyLevel(), 
							new MatchKeyLevel("Person"), new WildcardKeyLevel(), new MatchKeyLevel("Name"));
					
					this.dumpQuery(kq);
					
					break;
				}
				
				case 217: {
					RocksInterface adapt = ((DatabaseManager)Hub.instance.getDatabase()).allocateAdapter();
					
					KeyQuery kq = new KeyQuery(adapt, new WildcardKeyLevel(), 
							new MatchKeyLevel("Person"), new ExpandoKeyLevel());
					
					this.dumpQuery(kq);
					
					break;
				}
				
				case 218: {
					RocksInterface adapt = ((DatabaseManager)Hub.instance.getDatabase()).allocateAdapter();
					
					KeyQuery kq = new KeyQuery(adapt, new WildcardKeyLevel(), 
							new WildcardKeyLevel(), new WildcardKeyLevel());
					
					//KeyQuery kq = new KeyQuery(adapt, new WildcardKeyLevel(), 
					//		new MatchKeyLevel("Person"), new WildcardKeyLevel());
					
					//KeyQuery kq = new KeyQuery(adapt, new MatchKeyLevel("Record"), 
					//		new MatchKeyLevel("Person"), new WildcardKeyLevel());
					
					kq.setBrowseMode(true);
					
					this.dumpQuery(kq);
					
					break;
				}
				
				case 219: {
					RocksInterface adapt = ((DatabaseManager)Hub.instance.getDatabase()).allocateAdapter();
					
					System.out.println("First: ");
					
					KeyQuery kq = new KeyQuery(adapt, new MatchKeyLevel("Record"), 
							new MatchKeyLevel("Person"), new MatchKeyLevel(3045));
					
					kq.setBrowseMode(true);
					
					this.dumpQuery(kq);
					
					System.out.println("Second: ");
					
					kq = new KeyQuery(adapt, new MatchKeyLevel("Record"), 
							new MatchKeyLevel("Person"), new MatchKeyLevel(3046));
					
					kq.setBrowseMode(true);
					
					this.dumpQuery(kq);
					
					break;
				}
				
				case 220: {
					RocksInterface adapt = ((DatabaseManager)Hub.instance.getDatabase()).allocateAdapter();
					
					KeyQuery kq = new KeyQuery(adapt, new WildcardKeyLevel(), 
							new MatchKeyLevel("Person"));
					
					kq.setBrowseMode(true);
					
					this.dumpQuery(kq);
					
					break;
				}
				
				case 221: {
					RocksInterface adapt = ((DatabaseManager)Hub.instance.getDatabase()).allocateAdapter();
					
					KeyQuery kq = new KeyQuery(adapt, new WildcardKeyLevel());
					
					kq.setBrowseMode(true);
					
					this.dumpQuery(kq);
					
					break;
				}
				
				case 222: {
					RocksInterface adapt = ((DatabaseManager)Hub.instance.getDatabase()).allocateAdapter();
					
					KeyQuery kq = new KeyQuery(adapt, new WildcardKeyLevel(),
							new WildcardKeyLevel());
					
					kq.setBrowseMode(true);
					
					this.dumpQuery(kq);
					
					break;
				}
				
				case 223: {
					Hub.instance.getDatabase().submit(new KeyQueryRequest(), new ObjectResult() {
						@Override
						public void process(CompositeStruct result) {
							System.out.println("KeyQuery returned: " + result);
						}
					});
					
					break;
				}
				
				case 224: {
					DbRecordRequest req = new InsertRecordRequest()
							.withTable(DB_GLOBAL_TENANT_DB)
							.withSetField("dcTitle", "Betty Site")
							.withSetField("dcName", "betty.com", "betty.com")
							.withSetField("dcName", "www.betty.com", "www.betty.com")
							.withSetField("dcDescription", "Website for Betty Example");
							
					Hub.instance.getDatabase().submit(req, new ObjectResult() {
						@Override
						public void process(CompositeStruct result) {
							System.out.println("InsertRecordRequest returned: " + result);
						}
					});
					
					break;
				}
				
				case 225: {
					DbRecordRequest req = new UpdateRecordRequest()
							.withTable(DB_GLOBAL_TENANT_DB)
							.withId("00100_000000000000001")
							.withSetField("dcName", "mail.betty.com", "mail.betty.com")			// add mail
							.withSetField("dcName", "www.betty.com", "web.betty.com")			// change www to web
							.withRetireField("dcName", "betty.com")								// retire a name
							.withSetField("dcDescription", "Website for Betty Example 2");		// update a field		
							
					Hub.instance.getDatabase().submit(req, new ObjectResult() {
						@Override
						public void process(CompositeStruct result) {
							System.out.println("UpdateRecordRequest returned: " + result);
						}
					});
					
					break;
				}
				
				case 226: {
					// alternative syntax
					DbRecordRequest req = new InsertRecordRequest()
						.withTable(DB_GLOBAL_TENANT_DB)
						.withSetField("dcTitle", "Mandy Site")
						.withSetField("dcDescription", "Website for Mandy Example")
						.withSetField("dcName", "mandy.com", "mandy.com")
						.withSetField("dcName", "www.mandy.com", "www.mandy.com");
							
					Hub.instance.getDatabase().submit(req, new ObjectResult() {
						@Override
						public void process(CompositeStruct result) {
							System.out.println("InsertRecordRequest returned: " + result);
						}
					});
					
					break;
				}
				
				case 227: {
					DbRecordRequest req = new InsertRecordRequest()
							.withTable("dcUser")
							// all DynamicScalar, but suibid is auto assigned
							.withSetField("dcUsername", "mblack")
							.withSetField("dcEmail", "mblack@mandy.com")
							.withSetField("dcFirstName", "Mandy")
							.withSetField("dcLastName", "Black");
					
					Hub.instance.getDatabase().submit(req, new ObjectResult() {
						@Override
						public void process(CompositeStruct result) {
							System.out.println("InsertRecordRequest returned: " + result);
						}
					});
					
					break;
				}
				
				case 228: {
					System.out.println("Last name sid: ");
					String subid = scan.nextLine();
					
					DbRecordRequest req = new UpdateRecordRequest()
						.withTable("dcUser")
						.withId("00100_000000000000001")
						.withSetField("dcLastName", subid, "Blackie");
					
					Hub.instance.getDatabase().submit(req, new ObjectResult() {
						@Override
						public void process(CompositeStruct result) {
							System.out.println("UpdateRecordRequest returned: " + result);
						}
					});
					
					DbRecordRequest ireq = new InsertRecordRequest()
						.withTable("dcUser")
						// all DynamicScalar, but suibid is auto assigned
						.withSetField("dcUsername", "xblackie")
						.withSetField("dcEmail", "xblackie@mandy.com")
						.withSetField("dcFirstName", "Charles")
						.withSetField("dcLastName", "Blackie");
					
					Hub.instance.getDatabase().submit(ireq, new ObjectResult() {
						@Override
						public void process(CompositeStruct result) {
							System.out.println("InsertRecordRequest returned: " + result);
						}
					});
					
					break;
				}
				
				case 229: {
					
					DataRequest req = new DataRequest("dcPing");
					
					Hub.instance.getDatabase().submit(req, new ObjectResult() {
						@Override
						public void process(CompositeStruct result) {
							System.out.println("PingRequest 1 returned: " + result);
						}
					});
					
					Hub.instance.getDatabase().submit(RequestFactory.ping(), new ObjectResult() {
						@Override
						public void process(CompositeStruct result) {
							System.out.println("PingRequest 2 returned: " + result);
						}
					});
					
					
					break;
				}
				
				case 230: {
					System.out.println("Echo phrase: ");
					String in = scan.nextLine();
					
					Hub.instance.getDatabase().submit(RequestFactory.echo(in), new ObjectResult() {
						@Override
						public void process(CompositeStruct result) {
							System.out.println("EchoRequest returned: " + result);
						}
					});
					
					break;
				}
				
				case 231: {
					LoadRecordRequest req = new LoadRecordRequest()
						.withTable(DB_GLOBAL_TENANT_DB)
						.withId(OperationContext.get().getUserContext().getTenantId()) 
						.withSelect(new SelectFields()
							.withField("dcTitle", "SiteName")
							.withField("dcDescription", "Description")
							.withField("dcName", "Names")
						);
					
					Hub.instance.getDatabase().submit(req, new ObjectResult() {
						@Override
						public void process(CompositeStruct result) {
							System.out.println("LoadRecordRequest returned: " + result);
						}
					});
					
					break;
				}
				
				case 232: {
					RecordStruct params = new RecordStruct(
							new FieldStruct("ExpireThreshold", new DateTime().minusMinutes(3)),
							new FieldStruct("LongExpireThreshold", new DateTime().minusMinutes(5))
					);
					
					
					Hub.instance.getDatabase().submit(new DataRequest("dcCleanup").withParams(params), new ObjectResult() {							
						@Override
						public void process(CompositeStruct result) {
							if (this.hasErrors())
								Logger.errorTr(114);
						}
					});
					
				}
				
				
				case 239: {
					ListDirectRequest req = new ListDirectRequest("dcUser", new SelectField()
						.withField("dcUsername"));
					
					Hub.instance.getDatabase().submit(req, new ObjectResult() {
						@Override
						public void process(CompositeStruct result) {
							System.out.println("ListDirectRequest returned: " + result);
						}
					});
					
					break;
				}
				*/
				}
			}
			catch(Exception x) {
				System.out.println("CLI error: " + x);
			}
		}		
	}
	
	public Path getDbPath(Scanner scan) {
		System.out.print("Enter name (or path) of database: ");
		String name = scan.nextLine();
		
		if (StringUtil.isDataInteger(name) || "0".equals(name))
			return null;
		
		// if the path is only the folder name and nothing more, put it in ./datastore
		if (!name.contains("/"))
			name = "./datastore/" + name;
		
		return Paths.get(name);
	}
	
	public DatabaseManager getDatabase(Path dbpath, boolean createIfNotPresent) {
		if (!Files.exists(dbpath)) {
			if (createIfNotPresent)
				try {
					Files.createDirectories(dbpath);
				} 
				catch (IOException x) {
					System.out.println("Bad directory: " + x);
					return null;
				}
			else
				return null;
		}
		
		XElement dcdb = new XElement("dcDatabase", new XAttribute("Path", dbpath.toString()));
		
		DatabaseManager dm = new DatabaseManager();		// TODO generalize, support more than rocksdb
		dm.init(dcdb);
		
		// but do not start because that gets a backup going
		
		return dm;
	}
	
	public void dumpQuery(KeyQuery kq) {
		while (kq.nextKey() != null) {
			byte[] key = kq.key();						
			
			if (key[0] == Constants.DB_TYPE_MARKER_OMEGA) {
				System.out.println("END");
				break;
			}
			
			byte[] val = kq.value();
			
			List<Object> keyParts = ByteUtil.extractKeyParts(key);
			
			for (Object p : keyParts)
				System.out.print(p.toString() + " / ");
			
			System.out.println(" = " + ByteUtil.extractValue(val));
		}
	}
	
	
	public void cryptoMenu(Scanner scan) { 	
		boolean running = true;
		
		while(running) {
			try {
				System.out.println();
				System.out.println("-----------------------------------------------");
				System.out.println("   Hub " + Hub.instance.getResources().getHubId() + " Crypto Utility Menu");
				System.out.println("-----------------------------------------------");
				System.out.println("0)  Exit");
				System.out.println("1)  Cipher Dump");
	
				String opt = scan.nextLine();
				
				Long mopt = StringUtil.parseInt(opt);
				
				if (mopt == null)
					continue;
				
				switch (mopt.intValue()) {
				case 0:
					running = false;
					break;
				
				case 1: {
			        String protocol = "TLSv1.2";
		            SSLContext serverContext = SSLContext.getInstance(protocol);
		            serverContext.init(null, null, null);
		            
		            SSLEngine engine = serverContext.createSSLEngine();
			        
			        System.out.println("Enabled");
			        
			        for (String p : engine.getEnabledProtocols())
			        	System.out.println("Proto: " + p);
			        
			        for (String p : engine.getEnabledCipherSuites())
			        	System.out.println("Suite: " + p);
			        
			        System.out.println();        
			        System.out.println("Supported");
			        System.out.println();        
			        
			        for (String p : engine.getSupportedProtocols())
			        	System.out.println("Proto: " + p);
			        
			        for (String p : engine.getSupportedCipherSuites())
			        	System.out.println("Suite: " + p);
					
					break;
				}
				}
			}
			catch(Exception x) {
				System.out.println("CLI error: " + x);
			}
		}
	}
}
