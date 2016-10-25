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
import dcraft.util.FileUtil;
import dcraft.util.StringUtil;
import dcraft.xml.XElement;
import io.netty.buffer.ByteBuf;

public class SplitStream extends TransformStream {
	protected int seqnum = 1;
	protected int size = 10 * 1024 * 1024;
	protected String template = "file-%seq%.bin";
	
	protected int currchunk = 0;
	
	public SplitStream withSize(int v) {
		this.size = v;
		return this;
	}
	
	// include a %seq% to be replaced, like this file-%seq%.bin
	public SplitStream withNameTemplate(String v) {
		this.template = v;
		return this;
	}
	
    public SplitStream() {
    }

	@Override
	public void init(StackEntry stack, XElement el) {
		this.seqnum = (int) stack.intFromElement(el, "StartAt", this.seqnum);
		
		String size = stack.stringFromElement(el, "Size", "10MB");
		
		this.size = (int) FileUtil.parseFileSize(size);
		
		String temp = stack.stringFromElement(el, "Template");
		
		if (StringUtil.isNotEmpty(temp))
			this.template = temp;
	}
    
	// make sure we don't return without first releasing the file reference content
	@Override
	public ReturnOption handle(FileSlice slice) {
    	if (slice == FileSlice.FINAL) 
    		return this.downstream.handle(slice);

    	if (this.currfile == null) 
    		this.currfile = this.buildCurrent(slice.file, false);
    	
    	ByteBuf in = slice.data;

    	if (in != null) {
    		while (in.isReadable()) {
    			int amt = Math.min(in.readableBytes(), this.size - this.currchunk);
    			
    			ByteBuf out = in.copy(in.readerIndex(), amt);
    			
    			in.skipBytes(amt);
    			this.currchunk += amt;
    		
    			boolean eof = (this.currchunk == this.size) || (!in.isReadable() && slice.isEof());
    			
    			this.addSlice(out, 0, eof); 
    			
    			if (eof) {
    				this.seqnum++;
    				this.currchunk = 0;
    	    		this.currfile = this.buildCurrent(slice.file, eof);
    			}
			}
    		
    		in.release();
    	}
    	else if (slice.isEof()) {
			this.addSlice(null, 0, false); 
    	}
    	
    	return this.handlerFlush();
    }
    
    public FileDescriptor buildCurrent(FileDescriptor curr, boolean eof) {
		// create the output message
    	FileDescriptor blk = new FileDescriptor();
		
        blk.setModTime(System.currentTimeMillis());		
        
        // keep the path, just vary the name to the template
        blk.setPath(curr.path().resolvePeer("/" + this.template.replace("%seq%", this.seqnum + "")));
        
        if (eof)
        	blk.setSize(this.currchunk);
        else
        	blk.setSize(0);						// don't know yet
        
        return blk;
    }
}
