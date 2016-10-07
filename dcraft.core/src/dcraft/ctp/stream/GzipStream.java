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

import java.util.zip.CRC32;
import java.util.zip.Deflater;

import org.apache.commons.compress.compressors.gzip.GzipUtils;

import dcraft.ctp.f.FileDescriptor;
import dcraft.hub.Hub;
import dcraft.script.StackEntry;
import dcraft.util.FileUtil;
import dcraft.util.StringUtil;
import dcraft.xml.XElement;

public class GzipStream extends TransformStream {
    protected static final byte[] gzipHeader = {0x1f, (byte) 0x8b, Deflater.DEFLATED, 0, 0, 0, 0, 0, 0, 0};
    
    protected int compressionLevel = 6;
    
    protected Deflater deflater = null;
    protected CRC32 crc = new CRC32();
    protected boolean writeHeader = true;
    
    protected String nameHint = null;
    protected String lastpath = null;
    
    public GzipStream() {
    }
    
    public GzipStream(int compressionLevel) {
    	this.compressionLevel = compressionLevel;
    }

	@Override
	public void init(StackEntry stack, XElement el) {
		this.nameHint = stack.stringFromElement(el, "NameHint");
	}

	@Override
    public void close() {
		//System.out.println("GZip killed");	// TODO
		
    	this.deflater = null;
    
    	super.close();
    }
    
	// make sure we don't return without first releasing the file reference content
	@Override
	public ReturnOption handle(FileSlice slice) {
    	if (slice == FileSlice.FINAL) 
    		return this.downstream.handle(slice);
    	
    	// we don't know what to do with a folder at this stage - gzip is for file content only
    	// folder scanning is upstream in the FileSourceStream and partners
    	if (slice.file.isFolder())
    		return ReturnOption.CONTINUE;
    	
    	// init if not set for this round of processing 
    	if (this.deflater == null) {
            this.deflater = new Deflater(this.compressionLevel, true);
            this.crc.reset();
        	this.writeHeader = true;
    	}
        
    	ByteBuf in = slice.data;
        ByteBuf out = null; 
    	
		if (in != null) {
	        byte[] inAry = in.array();
	
	        // always allow for a header (10) plus footer (8) plus extra (12)
	        // in addition to content
	        int sizeEstimate = (int) Math.ceil(in.readableBytes() * 1.001) + 30;
	        out = Hub.instance.getBufferAllocator().heapBuffer(sizeEstimate);
	        
	        if (this.writeHeader) {
	        	this.writeHeader = false;
	            out.writeBytes(gzipHeader);
	        } 
	
	        this.crc.update(inAry, in.arrayOffset(), in.writerIndex());
	
	        this.deflater.setInput(inAry, in.arrayOffset(), in.writerIndex());
	        
	        while (!this.deflater.needsInput()) 
	            deflate(out);
		}
		else
			out = Hub.instance.getBufferAllocator().heapBuffer(30);
		
		FileDescriptor blk = new FileDescriptor();
		FileSlice sliceout = FileSlice.allocate(blk, out, 0, false);
		
		if (StringUtil.isEmpty(this.lastpath)) {
			if (StringUtil.isNotEmpty(this.nameHint)) 
				this.lastpath = "/" +  this.nameHint;
			else if (slice.file.getPath() != null) 
				this.lastpath = "/" +  GzipUtils.getCompressedFilename(slice.file.path().getFileName());
			else
				this.lastpath = "/" + FileUtil.randomFilename("gz");
		}
		
		blk.setPath(this.lastpath);
		
		slice.file.setModTime(System.currentTimeMillis());

        if (slice.isEof()) {
	        this.deflater.finish();
	        
	        while (!this.deflater.finished()) 
	            deflate(out);
	    
	        int crcValue = (int) this.crc.getValue();
	        
	        out.writeByte(crcValue);
	        out.writeByte(crcValue >>> 8);
	        out.writeByte(crcValue >>> 16);
	        out.writeByte(crcValue >>> 24);
	        
	        int uncBytes = this.deflater.getTotalIn();
	        
	        out.writeByte(uncBytes);
	        out.writeByte(uncBytes >>> 8);
	        out.writeByte(uncBytes >>> 16);
	        out.writeByte(uncBytes >>> 24);
	
	        this.deflater.end();
	        this.deflater = null;		// cause a reset for next time we use stream
	        
        	sliceout.setEof(true);
        }
        
        if (in != null)
        	in.release();
        
       	return this.downstream.handle(sliceout);
    }

    protected void deflate(ByteBuf out) {
        int numBytes = 0;
        
        do {
        	byte[] o = out.array();
        	
            numBytes = this.deflater.deflate(o, out.arrayOffset() + out.writerIndex(), out.writableBytes(), Deflater.SYNC_FLUSH);
            
            out.writerIndex(out.writerIndex() + numBytes);
        } while (numBytes > 0);
    }
    
    @Override
    public void read() {
    	this.upstream.read();
    }
}
