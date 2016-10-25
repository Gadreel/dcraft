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

import dcraft.lang.op.OperationContext;
import dcraft.script.StackEntry;
import dcraft.xml.XElement;

public class FolderDumpStream extends BaseStream implements IStreamDest {

    @Override
    public void init(StackEntry stack, XElement el, boolean autorelative) {
    }
    
	// make sure we don't return without first releasing the file reference content
    @Override
    public ReturnOption handle(FileSlice slice) {
    	if (slice == FileSlice.FINAL) {
    		OperationContext.get().getTaskRun().complete();
           	return ReturnOption.DONE;
    	}
    	
		System.out.println(" " + slice.file.getPath() + "     " + slice.file.getSize()
				+ "     " + (slice.file.isFolder() ? "FOLDER" : "FILE"));

       	return ReturnOption.CONTINUE;
    }
    
    @Override
    public void read() {
    	this.upstream.read();
    }

	@Override
	public void execute() {
		this.upstream.read();
	}
}
