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
package dcraft.ctp.stream;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

import dcraft.ctp.f.FileDescriptor;
import dcraft.lang.op.OperationContext;
import dcraft.struct.RecordStruct;

abstract public class BaseStream extends RecordStruct implements IStream {
	protected IStream upstream = null;
	protected IStream downstream = null;
	
	@Override
	public void setUpstream(IStream upstream) {
		this.upstream = upstream;
		
		upstream.setDownstream(this);
	}
	
	@Override
	public void setDownstream(IStream downstream) {
		this.downstream = downstream;
	}
	
	/*
	 * A message was sent from upstream to me.
	 * 
	 * @param msg data to process
	 * @return AWAIT if you are processing this async
	 */
	@Override
	abstract public ReturnOption handle(FileSlice slice);
	
	/**
	 * downstream is requesting that you send more messages
	 */
	@Override
	abstract public void read();
	
	@Override
	public IStreamSource getOrigin() {
		if (this instanceof IStreamSource)
			return (IStreamSource) this;
		
		if (this.upstream != null)
			return this.upstream.getOrigin();
		
		return null;
	}
	
	@Override
	public void cleanup() {
		IStream up = this.upstream;
		
		if (up != null)
			up.cleanup();
		
		this.close();
	}
	
	@Override
	public void close() {
		this.currfile = null;
		
    	// not truly thread safe, consider
    	for (FileSlice bb : this.outslices)
    		bb.release();
    	
    	this.outslices.clear();
    
		this.upstream = null;
		this.downstream = null;
	}
	protected List<FileSlice> outslices = new ArrayList<>();
	protected FileDescriptor currfile = null;
	
	public void addSlice(ByteBuf buf, long offset, boolean eof) {
		FileSlice s = FileSlice.allocate(this.currfile, buf, offset, eof);

		this.outslices.add(s);
	}

	public ReturnOption handlerFlush() {
		if (OperationContext.get().getTaskRun().isComplete())
			return ReturnOption.DONE;
		
		// write all messages in the queue
		while (this.outslices.size() > 0) {
			FileSlice slice = this.outslices.remove(0);
			
			ReturnOption ret = this.downstream.handle(slice);
			
			if (ret != ReturnOption.CONTINUE)
				return ret;
		}
    	
       	return ReturnOption.CONTINUE;
	}
}
