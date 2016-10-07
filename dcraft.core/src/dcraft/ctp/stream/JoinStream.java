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

import dcraft.ctp.f.FileDescriptor;
import dcraft.script.StackEntry;
import dcraft.util.StringUtil;
import dcraft.xml.XElement;

public class JoinStream extends TransformStream {
	protected FileDescriptor jfile = null;
	protected String namehint = null;
	
	public JoinStream withNameHint(String v) {
		this.namehint = v;
		return this;
	}
	
    public JoinStream() {
    }

	@Override
	public void init(StackEntry stack, XElement el) {
	}
    
	// make sure we don't return without first releasing the file reference content
	@Override
	public ReturnOption handle(FileSlice slice) {
    	if (slice == FileSlice.FINAL) 
    		return this.downstream.handle(slice);
    	
    	if (this.jfile == null) {
    		// create the output file desc
    		this.jfile = new FileDescriptor();
    		
    		this.jfile.setModTime(System.currentTimeMillis());		
            
            // keep the path, just vary the name to the template
    		this.jfile.setPath(slice.file.path().resolvePeer(StringUtil.isNotEmpty(this.namehint) ? this.namehint : "/file.bin"));		// TODO support other names, currently assumes we are writing to a file dest instead of folder dest so name ignored
    		this.jfile.setSize(0);						// don't know size ahead of time
    	}
    	
    	FileSlice sliceout = FileSlice.allocate(this.jfile, slice.data, 0, false);

		return this.downstream.handle(sliceout);
    }
}
