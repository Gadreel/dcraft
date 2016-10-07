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
package dcraft.work;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;

import dcraft.hub.Hub;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.util.StringUtil;

public class ShellWork implements IWork {
	static public final Pattern logpattern = Pattern.compile("^(\\d\\d\\d)\\s.+$");
	
	static public RecordStruct buildTaskParams(String cmd, String workingFolder, Long errorOffset, String... args) {
		ListStruct arglist = new ListStruct((Object[]) args);
		
		return new RecordStruct()
				.withField("Command", cmd)
				.withField("WorkingFolder", workingFolder)
				.withField("Args", arglist)
				.withField("ErrorOffset", errorOffset);
	}
	
	static public String quoteArg(String v) {
		if (StringUtil.isEmpty(v))
			v = "null";
		
		return "\"" + v.replace("\"", "\"\"") + "\"";			// TODO could be ^ instead??
	}
	
	// members
	
	protected Process proc = null;
	protected ScheduledFuture<?> sfuture = null;
	protected boolean beforestart = true;
	
	@Override
	public void run(TaskRun trun) {
		RecordStruct params = trun.getTask().getParams();
		List<String> oparams  = new ArrayList<String>();

		String cmd = params.getFieldAsString("Command");
		
		if (cmd.endsWith(".bat") || cmd.endsWith(".bat\"")) {
			oparams.add("cmd.exe");
			oparams.add("/c");
		}
		else if (cmd.endsWith(".sh") || cmd.endsWith(".sh\"")) {
			oparams.add("sh");
			oparams.add("-c");
		}
		
		oparams.add(cmd);
		
		ListStruct args = params.getFieldAsList("Args");
		
		for (int i = 0; i < args.getSize(); i++) {
			String v = args.getItemAsString(i);
			
			if (StringUtil.isEmpty(v)) {
				trun.error("Missing value for parameter: " + (i + 1));
				// TODO error code and set last
				trun.complete();
				return;
			}
			
			oparams.add(v);
		}
		
		ProcessBuilder pb = new ProcessBuilder(oparams);
		pb.redirectErrorStream(true);
		pb.directory(new File(params.getFieldAsString("WorkingFolder")));
		
		this.sfuture = Hub.instance.getClock().schedulePeriodicInternal(new Runnable() {
			@Override
			public void run() {
				if (ShellWork.this.beforestart)
					return;
				
				Process p = ShellWork.this.proc;
				
				System.out.println(new DateTime() + ": Keep alive check shell proc: " + trun.getTask().getTitle());

				// if started and alive just keep the context alive
				if ((p != null) && p.isAlive()) {
					System.out.println(new DateTime() + ": Keep alive signaled shell proc: " + trun.getTask().getTitle());
					trun.touch();
					return;
				}
				
				// otherwise if started and not alive/present the clear out this schedule
				System.out.println(new DateTime() + ": Keep alive failed shell proc: " + trun.getTask().getTitle());
				
				ScheduledFuture<?> future = ShellWork.this.sfuture;
				
				if (future != null)
					future.cancel(true);
				
				ShellWork.this.sfuture = null;
			}
		}, 55);
		
		// must keep in same thread so we obey the rules of the pool we are running in
		try {
			this.proc = pb.start();
			
			this.beforestart = false;
			
			boolean ignoreoutput = params.getFieldAsBooleanOrFalse("IgnoreOutput");
			
			BufferedReader input = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			
			String line = null;
			
			Long rooterr = params.getFieldAsInteger("ErrorOffset");
			
			while ((line = input.readLine()) != null) {
				trun.touch();
				
				if (ignoreoutput)
					continue;
				
				line = line.trim();
				
				// TODO configure if empty lines should be removed
				if (StringUtil.isEmpty(line))
					continue;
				
				Matcher m = ShellWork.logpattern.matcher(line);
				int code = 0;
						
				if (m.matches()) {
					code = Integer.parseInt(m.group(1));
					line = line.substring(4);
				}
				
				if (code < 300)
					trun.info(0, line);
				else if (code < 500)
					trun.warn(0, line);
				else if (rooterr != null)
					trun.error(rooterr + code, line);	
			}
			
			input.close();
			  
			long ecode = (long) proc.exitValue();
			
			// special handling
			if (rooterr != null) {
				if (ecode >= 500) 
					trun.error(ecode + rooterr, "Shell exited with error", "Exit");
				else if (ecode > 0)
					trun.info(ecode + rooterr, "Shell exited with code", "Exit");
				else
					trun.info("Shell exited with no error", "Exit");
			}
			else {
				if (ecode > 0)
					trun.info(ecode, "Shell exited with code", "Exit");
				else
					trun.info("Shell exited with no error", "Exit");
			}
		} 
		catch (IOException x) {
			trun.error("Shell IO Error " + x);
		}
		finally {
			this.proc = null;
			
			ScheduledFuture<?> future = ShellWork.this.sfuture;
			
			if (future != null)
				future.cancel(true);
			
			ShellWork.this.sfuture = null;
		}
		
		trun.complete();
	}
}
