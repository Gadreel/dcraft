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
package dcraft.hub;

import java.io.Console;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;

import org.joda.time.DateTime;

import dcraft.api.ApiSession;
import dcraft.bus.Bus;
import dcraft.hub.Hub;
import dcraft.hub.HubResources;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.OperationResult;
import dcraft.log.DebugLevel;
import dcraft.log.Logger;
import dcraft.script.Activity;
import dcraft.script.IDebuggerHandler;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.builder.JsonStreamBuilder;
import dcraft.util.StringUtil;
import dcraft.work.TaskRun;
import dcraft.work.WorkTopic;
import dcraft.work.WorkPool;
import dcraft.xml.XElement;

/*
 */
public class Foreground {
	static public TaskRun lastdebugrequest = null; 
	
	public static void main(String[] args) {
		String deployment = (args.length > 0) ? args[0] : null;
		String squadid = (args.length > 1) ? args[1] : null;
		String hubid = (args.length > 2) ? args[2] : null;
		
		OperationContext.useHubContext();
		
		HubResources resources = new HubResources(deployment, squadid, null, hubid);

		resources.setDebugLevel(DebugLevel.Info);
		OperationResult or = resources.init();
		
		if (or.hasErrors()) {
			Logger.error("Unable to continue, hub resources not properly configured");
			return;
		}
		
		or = Hub.instance.start(resources);
		
		if (or.hasErrors()) {
			Logger.error("Unable to continue, hub resources not properly initialized");			
			Hub.instance.stop();
			return;
		}
		
		Hub.instance.getActivityManager().registerDebugger(new IDebuggerHandler() {				
			@Override
			public void startDebugger(TaskRun run) {
				/*
				if (!GraphicsEnvironment.isHeadless() && !"true".equals(resources.getConfig().getAttribute("Headless", "true").toLowerCase())) {
					ScriptUtility.goSwing(run);
				}
				else {
				*/
					System.out.println("---------------------------------------------------------------------------");
					System.out.println("  Script Requesting Debugger: " + run.getTask().getTitle());
					System.out.println("---------------------------------------------------------------------------");
					
					Foreground.lastdebugrequest = run;
				//}
			}
		});
		
		OperationContext.useNewRoot();
		
		final Scanner scan = new Scanner(System.in, "UTF-8");
		
		XElement croot = resources.getConfig();
		
		XElement cliel = croot.find("CommandLine");
		
		if (cliel == null) {
			cliel = new XElement("CommandLine");
			croot.with(cliel);
		}
		
		if (!cliel.hasAttribute("ClientClass")) 
			cliel.withAttribute("ClientClass", "dcraft.tool.HubUtil");
		
		ILocalCommandLine cli = (ILocalCommandLine) Hub.instance.getInstance(cliel.getAttribute("ClientClass"));
		
		ApiSession capi = null;
		boolean auth = true;
		
		String mode = cliel.getAttribute("Mode", "domain");
		String sess = cliel.getAttribute("Session");
		
		if ("root".equals(mode)) {
			if (StringUtil.isEmpty(sess))
				capi = ApiSession.createLocalSession(mode);
			else
				capi = ApiSession.createSessionFromConfig(sess);  //LocalSession("root");
			
			//System.out.print("Password: ");
			//String pass = scan.nextLine();
			
			Console cons = null;
			String pass = null; 
			char[] passwd = null;
			 
			if ((cons = System.console()) != null &&
			    (passwd = cons.readPassword("Password:")) != null) {
				pass = new String(passwd);
			}
			else {
				System.out.print("Password: ");
				pass = scan.nextLine();
			}
			
			//System.out.println("Entered: " + pass);
			
			if (StringUtil.isEmpty(pass) || "0".equals(pass))
				System.out.println("Stopping.");
			else if (capi.startSession("root", pass)) 
				try {
					cli.run(scan, capi);
				}
				catch(Exception x) {
					System.out.println("Unable to start commandline interface");
				}
			else
				System.out.println("Error logging in session");
		}
		else {
			while (true) {			
				System.out.print("Tenant (e.g. root): ");
				String domain = scan.nextLine();
				
				if ("-".equals(domain)) {
					System.out.println("--------------------------------------------");
					continue;
				}
				
				if ("0".equals(domain)) {
					auth = false;
					break;
				}
				
				if ("*".equals(domain)) {
					if (StringUtil.isEmpty(sess))
						capi = ApiSession.createLocalSession("root");
					else
						capi = ApiSession.createSessionFromConfig(sess);  // LocalSession("root");
			
					if (capi.startSession("root", "A1s2d3f4"))
						break;
				}
				else {
					if (StringUtil.isEmpty(sess))
						capi = ApiSession.createLocalSession(domain);
					else
						capi = ApiSession.createSessionFromConfig(sess);  //  LocalSession(domain);
					
					if (capi == null) {
						System.out.println("Bad tenant");
						continue;
					}
					
					//capi = CoreApi.createSessionFromConfig(domain);
					
					System.out.print("Username: ");
					String user = scan.nextLine();
					
					Console cons = null;
					String pass = null; 
					char[] passwd = null;
					 
					if ((cons = System.console()) != null &&
					    (passwd = cons.readPassword("Password:")) != null) {
						pass = new String(passwd);
					}
					else {
						System.out.print("Password: ");
						pass = scan.nextLine();
					}
			
					if (capi.startSession(user, pass))
						break;
				}
				
				System.out.println("Failed");
			}

			if (auth) {
				try {
					cli.run(scan, capi);
				}
				catch(Exception x) {
					System.out.println("Unable to start commandline interface");
				}
			}
		}

		if (capi != null)
			capi.stop();
		
		Hub.instance.stop();
	}
		
	static public void utilityMenu(Scanner scan) { 	
		boolean running = true;
		
		while(running) {
			try {
				System.out.println();
				System.out.println("-----------------------------------------------");
				System.out.println("   Hub " + Hub.instance.getResources().getHubId() + " Utility Menu");
				System.out.println("-----------------------------------------------");
				System.out.println("0)  Exit");
				System.out.println("1)  Encrypt Setting");
				System.out.println("2)  Hash Setting");
				System.out.println("3)  Hash Password Setting");
				System.out.println("4)  System Status");
				System.out.println("5)  Backup Server");
				System.out.println("100)  Enter Script Debugger");

				String opt = scan.nextLine();
				
				Long mopt = StringUtil.parseInt(opt);
				
				if (mopt == null)
					continue;
				
				switch (mopt.intValue()) {
				case 0:
					running = false;
					break;
				case 1: {
					System.out.println("Enter setting to encrypt:");
					String val = scan.nextLine();
					
					System.out.println("Result: "+ Hub.instance.getClock().getObfuscator().encryptStringToHex(val));
					break;
				}
				case 2: {
					System.out.println("Enter setting to hash:");
					String val = scan.nextLine();
					
					System.out.println("Result: "+ Hub.instance.getClock().getObfuscator().hashStringToHex(val));
					break;
				}
				case 3: {
					System.out.println("Enter password to hash:");
					String val = scan.nextLine();
					
					System.out.println("Result: "+ Hub.instance.getClock().getObfuscator().hashPassword(val));
					break;
				}
				case 793: {
					System.out.println("Enter setting to decrypt:");
					String val = scan.nextLine();
					
					System.out.println("Result: "+ Hub.instance.getClock().getObfuscator().decryptHexToString(val));
					break;
				}
				case 4: {
					Foreground.dumpStatus();
					break;
				}
				case 100: {
					Foreground.debugScript(scan);
					break;
				}
				}
			}
			catch(Exception x) {
				System.out.println("CLI error: " + x);
			}
		}		
	}
	
	static void debugScript(Scanner scn) {
		TaskRun r = Foreground.lastdebugrequest;
		
		if (r == null) {
			System.out.println("No debugger requests are availabled.");
			return;
		}
		
		Activity act = (Activity) Foreground.lastdebugrequest.getTask().getWork();
		act.setInDebugger(true);
		
		AtomicLong lastinstrun = new AtomicLong(act.getRunCount());
		AtomicLong lastinstmrk = new AtomicLong(lastinstrun.get());
		
		Hub.instance.getClock().schedulePeriodicInternal(new Runnable() {
			@Override
			public void run() {
				long cnt = act.getRunCount();
				
				if (lastinstrun.get() == cnt)
					return;
				
				lastinstrun.set(cnt);
				
				if (r.isComplete())
					System.out.println("DEBUGGER: Press enter to exit or ? for help.");
				else {
					RecordStruct debuginfo = act.getDebugInfo();
					
					ListStruct stack = debuginfo.getFieldAsList("Stack");					
					RecordStruct currinst = stack.getItemAsRecord(stack.getSize() - 1);
					long line = currinst.getFieldAsInteger("Line");
					long col = currinst.getFieldAsInteger("Column");
					
					System.out.println("DEBUGGER: (" + line + "," + col +") " + currinst.getFieldAsString("Command"));
					System.out.println("DEBUGGER: Press enter to continue or ? for help.");
				}
			}
		}, 1);
		
		System.out.println("DEBUGGER: Press enter to continue or ? for help.");
		
		while (!r.isComplete()) {
			String cmd = scn.nextLine();
			
			// dump
			if (cmd.startsWith("d")) {
				System.out.println("------------------------------------------------------------------------");

				try {
					act.getDebugInfo().toBuilder(new JsonStreamBuilder(System.out, true));	
				}
				catch (Exception x) {
					System.out.println("DEBUGGER: unable to dump" + x);
				}
				
				System.out.println("------------------------------------------------------------------------");
			}
			// help
			else if (cmd.startsWith("?")) {
				System.out.println("(n)ext");
				System.out.println("(r)un");
				System.out.println("(s)top");
				System.out.println("(d)ump stack");
			}
			// stop
			else if (cmd.startsWith("s")) {
				r.kill();
			}
			// run
			else if (cmd.startsWith("r")) {
				if (lastinstmrk.get() > lastinstrun.get()) {
					System.out.println("DEBUGGER: Wait, script is executing...");
				}
				else {
					lastinstmrk.set(lastinstrun.get());
					lastinstmrk.incrementAndGet();
					
					System.out.println("DEBUGGER: Running...");
					act.setDebugMode(false);
					Hub.instance.getWorkPool().submit(r);
				}
			}
			// next
			else if (!r.isComplete()) {
				if (lastinstmrk.get() > lastinstrun.get()) {
					System.out.println("DEBUGGER: Wait, script is executing...");
				}
				else {
					lastinstmrk.set(lastinstrun.get());
					lastinstmrk.incrementAndGet();
					
					System.out.println("DEBUGGER: Executing...");
					act.setDebugMode(true);
					Hub.instance.getWorkPool().submit(r);
				}
			}
		}
		
		System.out.println("DEBUGGER: Script done.");
	}
	
	static public void dumpStatus() {
		WorkPool pool = Hub.instance.getWorkPool();
		
		System.out.println(" ------------------------------------------- ");
		//System.out.println("        Pool: " + pool.getName());
		//System.out.println("    Back Log: " + pool.backlog());
		
		//System.out.println("  Busy Level: " + pool.howBusy());
		
		System.out.println("   # Threads: " + pool.threadCount());
		System.out.println("   # Created: " + pool.threadsCreated());
		System.out.println("      # Hung: " + pool.threadsHung());
		System.out.println(" ------------------------------------------- ");
		
		for (WorkTopic topic : pool.getTopics()) {
			System.out.println(" Topic:        " + topic.getName());
			System.out.println(" - In Progress: " + topic.inprogress());
			
			for (TaskRun task : topic.tasksInProgress()) {
				System.out.println(" -- " + task.getTask().getId());
			}
		}
		
		SysReporter rep = Hub.instance.getClock().getSlowSysReporter();
		
		System.out.println(" Slow Sys Work Status: " + rep.getStatus() + " @ " + new DateTime(rep.getLast()));
		
		rep = Hub.instance.getClock().getFastSysReporter();
		
		System.out.println(" Fast Sys Work Status: " + rep.getStatus() + " @ " + new DateTime(rep.getLast()));
		
		Bus b = Hub.instance.getBus();
		
		b.dumpInfo();
		
		Hub.instance.getScheduler().dump();
	}
}
