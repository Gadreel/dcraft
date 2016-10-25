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
import dcraft.ctp.f.FileDescriptor;
import dcraft.hub.Hub;
import dcraft.lang.Memory;
import dcraft.lang.op.OperationContext;
import dcraft.script.StackEntry;
import dcraft.util.StringUtil;
import dcraft.xml.XElement;

public class MemorySourceStream extends BaseStream implements IStreamSource {
	protected Memory source = null;	
	protected long inprog = 0;
	protected boolean eof = false;
	protected String fname = null;
	
	public MemorySourceStream withBinary(Memory v) {
		this.source = new Memory(v);
		return this;
	}
	
	public MemorySourceStream withBinary(byte[] v) {
		this.source = new Memory(v);
		this.source.setPosition(0);
		return this;
	}
	
	public MemorySourceStream withChars(CharSequence v) {
		this.source = new Memory(v);
		this.source.setPosition(0);
		return this;
	}

	public MemorySourceStream withFilename(String v) {
		this.fname = v;
		return this;
	}

	// for use with dcScript
	@Override
	public void init(StackEntry stack, XElement el) {
		// anything we need to gleam from the xml?
	}

	@Override
	public ReturnOption handle(FileSlice slice) {
		// we are at top of stream, nothing to do here
		return ReturnOption.CONTINUE;
	}
	
	@Override
	public void close() {
		this.source = null;
		
		super.close();
	}

	/**
	 * Someone downstream wants more data
	 */
	@Override
	public void read() {
		if (this.source == null) {
			this.downstream.handle(FileSlice.FINAL);
			return;
		}
		
		if (this.inprog == 0) {
			// As a source we are responsible for progress tracking
			OperationContext.get().setAmountCompleted(0);
			this.currfile = new FileDescriptor();
	        this.currfile.setPath(StringUtil.isNotEmpty(this.fname) ? this.fname : "/memory.bin");
	        this.currfile.setSize(this.source.getLength());
		}
		else if (this.eof) {
			this.downstream.handle(FileSlice.FINAL);
			return;
		}
		
		while (true) {
			// TODO sizing?
	        ByteBuf data = Hub.instance.getBufferAllocator().heapBuffer(32768);
	        
	        int amt = this.source.read(data.array(), data.arrayOffset(), data.capacity());
	
	        this.eof = this.source.getPosition() == this.source.getLength();
	        
	        data.writerIndex(amt);
	        
	        FileSlice sliceout = FileSlice.allocate(this.currfile, data, 0, this.eof);
	        
        	this.inprog += amt;
	        OperationContext.get().setAmountCompleted((int)(this.inprog * 100 / this.source.getLength()));
	        
	    	if (this.downstream.handle(sliceout) != ReturnOption.CONTINUE)
	    		break;

	    	if (this.eof) {
				this.downstream.handle(FileSlice.FINAL);
	    		break;
	    	}
		}
	}
}
