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

import dcraft.ctp.CtpAdapter;
import dcraft.ctp.f.BlockCommand;
import dcraft.ctp.f.CtpFCommand;
import dcraft.script.StackEntry;
import dcraft.xml.XElement;

public class CtpStreamSource extends BaseStream implements IStreamSource {
	protected CtpAdapter adapter = null;
	protected boolean initialized = false;
	
	public CtpStreamSource(CtpAdapter adapter) {
		this.adapter = adapter;
	}

	@Override
	public void init(StackEntry stack, XElement el) {
	}
	
	public void setFinal() {
		/*
		this.entryLock.lock();
		
		try {
			this.currFile = null;
			this.entries.add(new FileEntry(FileDescriptor.FINAL, null));
		}
		finally {
			this.entryLock.unlock();
		}
		*/
	}
	
	public void addNext(BlockCommand cmd) {
		/* TODO
		this.entryLock.lock();
		
		try {
			if (this.currFile == null)
				this.currFile = new FileDescriptor();
			
			this.currFile.copyAttributes(cmd);
			
			this.entries.add(new FileEntry(this.currFile, cmd.getData()));
			
			if (this.currFile.isEof())
				this.currFile = null;
		}
		finally {
			this.entryLock.unlock();
		}
		*/
	}
	
	@Override
	public ReturnOption handle(FileSlice slice) {
		return ReturnOption.CONTINUE;
	}

	@Override
	public void read() {
		// if not initialized get the stream flowing by sending a READ
		if (!this.initialized) {
			this.initialized = true;
			
			try {
				this.adapter.sendCommand(CtpFCommand.STREAM_READ);
			} 
			catch (Exception x) {
				System.out.println("Error sending READ: " + x);
			}
		}
		else {
			// since only we remove, it is ok to check > 0 from here 
			// adding thread cannot add and call this at the same time
			// when they do call us, they'll catch any missed entries.
			if (this.handlerFlush() == ReturnOption.CONTINUE)
				// if no entries (left or to start with) then ask for more
				this.adapter.read();
		}
	}
}
