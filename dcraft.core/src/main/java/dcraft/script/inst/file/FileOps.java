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
package dcraft.script.inst.file;

import dcraft.ctp.stream.FileSourceStream;
import dcraft.ctp.stream.FunnelStream;
import dcraft.ctp.stream.GzipStream;
import dcraft.ctp.stream.IStreamDest;
import dcraft.ctp.stream.IStreamUp;
import dcraft.ctp.stream.JoinStream;
import dcraft.ctp.stream.NullDest;
import dcraft.ctp.stream.PgpEncryptStream;
import dcraft.ctp.stream.SplitStream;
import dcraft.ctp.stream.StreamWork;
import dcraft.ctp.stream.TarStream;
import dcraft.ctp.stream.UngzipStream;
import dcraft.ctp.stream.UntarStream;
import dcraft.filestore.IFileCollection;
import dcraft.filestore.IFileStoreDriver;
import dcraft.filestore.IFileStoreFile;
import dcraft.hub.Hub;
import dcraft.lang.op.OperationCallback;
import dcraft.lang.op.OperationContext;
import dcraft.script.Ops;
import dcraft.script.StackEntry;
import dcraft.struct.Struct;
import dcraft.struct.scalar.NullStruct;
import dcraft.struct.scalar.StringStruct;
import dcraft.util.StringUtil;
import dcraft.work.IWork;
import dcraft.work.Task;
import dcraft.work.TaskRun;
import dcraft.xml.XElement;

public class FileOps extends Ops {
	@Override
	public void prepTarget(StackEntry stack) {
		this.nextOpResume(stack);
	}

	@Override
	public void runOp(StackEntry stack, XElement op, Struct target) {
		if ("Copy".equals(op.getName())) 
			this.copy(stack, op);					
		else if ("XCopy".equals(op.getName())) 
			this.xcopy(stack, op);
		else if ("Tar".equals(op.getName())) 
			this.injectStream(stack, op, new TarStream());					
		else if ("Untar".equals(op.getName())) 
			this.injectStream(stack, op, new UntarStream());					
		else if ("Gzip".equals(op.getName())) 
			this.injectStream(stack, op, new GzipStream());
		else if ("Ungzip".equals(op.getName())) 
			this.injectStream(stack, op, new UngzipStream());
		else if ("Funnel".equals(op.getName())) 
			this.injectStream(stack, op, new FunnelStream());
		else if ("Split".equals(op.getName())) 
			this.injectStream(stack, op, new SplitStream());
		else if ("Join".equals(op.getName())) 
			this.injectStream(stack, op, new JoinStream());
		else if ("PGPEncrypt".equals(op.getName())) 
			this.injectStream(stack, op, new PgpEncryptStream());
		else {
			OperationContext.get().error("Unknown FileOp: " + op.getName());
			this.nextOpResume(stack);
		}
	}

	protected void copy(StackEntry stack, XElement el) {
		IStreamUp streamin = this.getSourceStream(stack, el);
		
		if (streamin != null)
			this.executeDest(stack, el, streamin, false, true);
	}

	protected void xcopy(StackEntry stack, XElement el) {
		IStreamUp streamin = this.getSourceStream(stack, el);
		
		if (streamin != null)
			this.executeDest(stack, el, streamin, true, true);
	}

	protected void injectStream(StackEntry stack, XElement el, IStreamUp add) {
		IStreamUp streamin = this.getSourceStream(stack, el);
		
		if (streamin == null) 
			return;
		
		add.init(stack, el);
		
		add.setUpstream(streamin);
		
		this.registerUpStream(stack, el, add);

        this.executeDest(stack, el, add, true, false);
	}

	protected IStreamUp getSourceStream(StackEntry stack, XElement el) {
        Struct src = stack.refFromElement(el, "Source");
        
        if ((src == null) || (src instanceof NullStruct)) {
        	src = stack.queryVariable("_LastStream");
        	
            if ((src == null) || (src instanceof NullStruct)) {
            	OperationContext.get().error("Missing source");
				this.nextOpResume(stack);
	        	return null;
            }
        }
        
        if (src instanceof IStreamUp)
        	return (IStreamUp) src;
        
        if (!(src instanceof IFileStoreFile) && !(src instanceof IFileStoreDriver) && !(src instanceof IFileCollection)) {
        	OperationContext.get().error("Invalid source type");
			this.nextOpResume(stack);
        	return null;
        }
        
        IStreamUp filesrc = null;
		
        if (src instanceof IFileStoreFile) 
        	filesrc = ((IFileStoreFile)src).allocSrc();
        else if (src instanceof IFileStoreDriver) 
       		filesrc = ((IFileStoreDriver)src).rootFolder().allocSrc();
        else 
        	filesrc = new FileSourceStream((IFileCollection) src); 
        
        if (filesrc == null) {
        	OperationContext.get().error("Invalid source type");
			this.nextOpResume(stack);
        	return null;
        }
        
        filesrc.init(stack, el);
        
		return filesrc;
	}

	protected IStreamDest getDestStream(StackEntry stack, XElement el, boolean autorelative) {
        Struct dest = stack.refFromElement(el, "Dest");
        
        if ((dest == null) || (dest instanceof NullStruct)) 
        	return null;
        
        if ((dest instanceof StringStruct) && "NULL".equals(((StringStruct)dest).getValue()))
        	return new NullDest();
        
        if (dest instanceof IStreamDest)
        	return (IStreamDest) dest;
        
        if (!(dest instanceof IFileStoreFile) && !(dest instanceof IFileStoreDriver)) {
        	OperationContext.get().error("Invalid dest type");
			this.nextOpResume(stack);
        	return null;
        }
        
        IStreamDest deststrm = null;
        
        if (dest instanceof IFileStoreDriver) 
        	deststrm = ((IFileStoreDriver)dest).rootFolder().allocDest();
        else 
        	deststrm = ((IFileStoreFile)dest).allocDest();
        
        if (deststrm == null) {
        	OperationContext.get().error("Unable to create destination stream");
			this.nextOpResume(stack);
        	return null;
        }
        
        deststrm.init(stack, el, autorelative);
        return deststrm;
	}
	
	protected void executeDest(StackEntry stack, XElement el, IStreamUp src, boolean autorelative, boolean destRequired) {
		stack.addVariable("_LastStream", (Struct)src);
		
		IStreamDest streamout = this.getDestStream(stack, el, autorelative);
        
        if (streamout != null) {
    		streamout.setUpstream(src);
    		
    		IWork sw = new StreamWork(streamout);
    		
    		Task t = Task.subtask(OperationContext.get().getTaskRun(), "Streaming", new OperationCallback() {
    			@Override
    			public void callback() {
    				FileOps.this.nextOpResume(stack);
    	        	return;
    			}
    		});
    		
    		t.withWork(sw);
    		
    		TaskRun run = new TaskRun(t);

    		Hub.instance.getWorkPool().submit(run);
        }
        else {
        	if (destRequired)
        		OperationContext.get().error("Missing dest for " + el.getName());
        	
			this.nextOpResume(stack);
        	return;
        }
	}

	protected void registerUpStream(StackEntry stack, XElement el, IStreamUp src) {
        String name = stack.stringFromElement(el, "Name");
        
        if (StringUtil.isEmpty(name))
        	name = "Stream_" + stack.getActivity().tempVarName();
        
        // to be sure we cleanup the stream, all variables added will later be disposed of
        stack.addVariable(name, (Struct)src);
	}
	
	@Override
	public void cancel(StackEntry stack) {
		// TODO review after we make operations
	}
}
