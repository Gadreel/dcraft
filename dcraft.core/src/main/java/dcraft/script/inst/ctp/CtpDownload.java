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
package dcraft.script.inst.ctp;

import java.nio.file.Path;
import java.nio.file.Paths;

import dcraft.api.ApiSession;
import dcraft.api.tasks.DownloadFile;
import dcraft.filestore.CommonPath;
import dcraft.hub.Hub;
import dcraft.lang.op.OperationCallback;
import dcraft.lang.op.OperationContext;
import dcraft.script.ExecuteState;
import dcraft.script.Instruction;
import dcraft.script.StackEntry;
import dcraft.struct.FieldStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.util.StringUtil;
import dcraft.work.Task;

public class CtpDownload extends Instruction {
	@Override
	public void run(final StackEntry stack) {
        String service = stack.stringFromSource("Service");
        
        if (StringUtil.isEmpty(service))
        	service = "dcFileServer";
        
        String fname = stack.stringFromSource("Dest");
        
        if (StringUtil.isEmpty(fname)) {
			stack.setState(ExecuteState.Done);
			OperationContext.get().error("Missing Dest");
        	stack.resume();
        	return;
        }
        
    	Path dest = null;
    	
    	try {
    		dest = Paths.get(fname);
    	}
    	catch (Exception x) {
			stack.setState(ExecuteState.Done);
			OperationContext.get().error("Dest error: " + x);
        	stack.resume();
        	return;
    	}
        
        String dname = stack.stringFromSource("Source");
        
        if (StringUtil.isEmpty(dname)) {
			stack.setState(ExecuteState.Done);
			OperationContext.get().error("Missing Source");
        	stack.resume();
        	return;
        }
    	
    	CommonPath src = null;
    	
    	try {
    		src = new CommonPath(dname);
    	}
    	catch (Exception x) {
			stack.setState(ExecuteState.Done);
			OperationContext.get().error("Source error: " + x);
        	stack.resume();
        	return;
    	}
        
        Struct ss = stack.refFromSource("Session");
        
        if ((ss == null) || !(ss instanceof ApiSession)) {
			stack.setState(ExecuteState.Done);
			OperationContext.get().errorTr(531);
        	stack.resume();
        	return;
        }
        
		ApiSession sess = (ApiSession) ss;
        
		Task t = Task.subtask(OperationContext.get().getTaskRun(), "Downloading", new OperationCallback() {
			@Override
			public void callback() {
				stack.setState(ExecuteState.Done);
				stack.resume();
	        	return;
			}
		});
		
		t.withParams(new RecordStruct(
				new FieldStruct("LocalPath", dest),
				new FieldStruct("RemotePath", src),
				new FieldStruct("ServiceName", service)
				//new FieldStruct("TransferParams", storeParams),
				//new FieldStruct("ForceOverwrite", !allowResume)
		));
		
		DownloadFile work = new DownloadFile();
		work.setSession(sess);
		
		t.withWork(work);
		
		Hub.instance.getWorkPool().submit(t);
	}
	
	@Override
	public void cancel(StackEntry stack) {
		// do nothing, this isn't cancellable
	}
}
