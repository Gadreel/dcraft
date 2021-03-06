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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.compress.archivers.zip.ZipEncoding;
import org.apache.commons.compress.archivers.zip.ZipEncodingHelper;
import org.apache.commons.compress.utils.ArchiveUtils;

import dcraft.ctp.f.FileDescriptor;
import dcraft.lang.op.OperationContext;
import dcraft.script.StackEntry;
import dcraft.xml.XElement;

public class UntarStream extends TransformStream {
	protected enum UntarState {
        RECORD,
        XTRAS,
        PREP,
        CONTENT,
        SKIP
    }
	
    protected byte[] header_buffer = new byte[TarConstants.DEFAULT_RCDSIZE];
    protected int partialLength = 0;
    
    protected TarArchiveEntry currEntry = null;
    protected ZipEncoding encoding = null;
    protected long remainContent = 0;
    protected long remainSkip = 0;

    protected UntarState tstate = UntarState.RECORD;
    
    public UntarStream() {
        this.encoding  = ZipEncodingHelper.getZipEncoding("UTF-8");
    }

	@Override
	public void init(StackEntry stack, XElement el) {
	}
    
	// make sure we don't return without first releasing the file reference content
	@Override
	public ReturnOption handle(FileSlice slice) {
    	if (slice == FileSlice.FINAL) 
    		return this.downstream.handle(slice);

    	ByteBuf in = slice.getData();

    	if (in != null) {
    		while (in.isReadable()) {
    			switch (this.tstate) {
    			case RECORD:
		    		// starting a new record
		    		if (in.readableBytes() < TarConstants.DEFAULT_RCDSIZE - this.partialLength) {
		    			int offset = this.partialLength;
		    			
		    			this.partialLength += in.readableBytes();
		    			
		    			in.readBytes(this.header_buffer, offset, in.readableBytes());
		    			
		    			continue;
		    		}
		    		
	    			in.readBytes(this.header_buffer, this.partialLength, TarConstants.DEFAULT_RCDSIZE - this.partialLength);
		    		
		    		this.partialLength = 0;
		            
		    		//in.readBytes(this.header_buffer, 0, this.header_buffer.length);
		
		            boolean hasHitEOF = this.isEOFRecord(this.header_buffer);
	
		            // if we hit this twice in a row we are at the end - however, source will send FINAL anyway so we don't really care
		            if (hasHitEOF) {
		                this.setEntry(null); 
		                continue;
		            }
		    		
		            try {
		            	this.setEntry(new TarArchiveEntry(this.header_buffer, this.encoding));
		            } 
		            catch (Exception x) {
		            	OperationContext.get().getTaskRun().kill("Error detected parsing the header: " + x);
		                in.release();
		                return ReturnOption.DONE;
		            }
		            
		            this.tstate = UntarState.XTRAS;
    			case XTRAS:
		            if (!in.isReadable())
		            	continue;
		    		
		            // TODO support long names and such - see org.apache.commons.compress.archivers.tar.TarArchiveInputStream
			        if (this.currEntry.isGNULongLinkEntry()) {
			    		/* 
			            byte[] longLinkData = getLongNameData();
			            if (longLinkData == null) {
			                // Bugzilla: 40334
			                // Malformed tar file - long link entry name not followed by
			                // entry
			                return null;
			            }
			            currEntry.setLinkName(encoding.decode(longLinkData));
			            */
			        	
			        	OperationContext.get().getTaskRun().kill("long link currently not supported");
		                in.release();
			        	return ReturnOption.DONE;
			        }
			
			        if (this.currEntry.isGNULongNameEntry()) {
			    		/* 
			            byte[] longNameData = getLongNameData();
			            if (longNameData == null) {
			                // Bugzilla: 40334
			                // Malformed tar file - long entry name not followed by
			                // entry
			                return null;
			            }
			            currEntry.setName(encoding.decode(longNameData));
			            */
			        	
			        	OperationContext.get().getTaskRun().kill("long name currently not supported");
		                in.release();
			        	return ReturnOption.DONE;
			        }
			
			        if (this.currEntry.isPaxHeader()) { 
			        	// Process Pax headers
			    		/* 
			            paxHeaders();
			            */
			        	
			        	OperationContext.get().getTaskRun().kill("pax currently not supported");
		                in.release();
			        	return ReturnOption.DONE;
			        }
			
			        if (this.currEntry.isGNUSparse()) { 
			        	// Process sparse files
			    		/* 
			            readGNUSparse();
			            */
			        	
			        	OperationContext.get().getTaskRun().kill("sparse currently not supported");
		                in.release();
			        	return ReturnOption.DONE;
			        }
    				
		            this.tstate = UntarState.PREP;
    			case PREP:
		            if (!in.isReadable())
		            	continue;
		            
			        // TODO remove
		            System.out.println("name: " + this.currEntry.getName());
		            System.out.println("size: " + this.currEntry.getSize());
		            System.out.println("modified: " + this.currEntry.getModTime());
		            
		            // If the size of the next element in the archive has changed
		            // due to a new size being reported in the posix header
		            // information, we update entrySize here so that it contains
		            // the correct value.
		            long entrySize = this.currEntry.getSize();
		            this.remainContent = entrySize;
		            
		            long numRecords = (entrySize / this.header_buffer.length) + 1;
		            this.remainSkip = (numRecords * this.header_buffer.length) - entrySize;

		            // grab as much as we can from the current buffer
		            int readSize = (int) Math.min(this.remainContent, in.readableBytes());            
		            this.remainContent -= readSize;

		            // handle empty files too
		            if ((readSize > 0) || (this.remainContent == 0)) {
			            System.out.println("reading content: " + readSize);
		
			            ByteBuf out = in.copy(in.readerIndex(), readSize);
			            
			            int skipSize = (int) Math.min(this.remainSkip, in.readableBytes() - readSize);            
			            this.remainSkip -= skipSize;
			            
			            in.skipBytes(readSize + skipSize);

			            this.addSlice(out, 0, this.remainContent == 0);
		            }
		            
		            this.tstate = UntarState.CONTENT;
    			case CONTENT:
		            if (!in.isReadable())
		            	continue;
		            
	    			// check if there is still content left in the entry we were last reading from
	    			if (this.remainContent > 0) {
	    	            readSize = (int) Math.min(this.remainContent, in.readableBytes());            
	    	            this.remainContent -= readSize;
	    	            
	    	            //System.out.println("reading content: " + readSize);
	    	            
	    	            //ByteBuf out = Hub.instance.getBufferAllocator().heapBuffer((int) readSize);
	    	            
	    	            ByteBuf out = in.copy(in.readerIndex(), readSize);
	    	            
	    	            int skipSize = (int) Math.min(this.remainSkip, in.readableBytes() - readSize);            
	    	            this.remainSkip -= skipSize;
		                
		                //System.out.println("skipping content: " + skipSize);
	    	            
	    	            in.skipBytes(readSize + skipSize);

			            this.addSlice(out, 0, this.remainContent == 0);
	    			}
	    			
	    			if (this.remainContent > 0) 
	    				continue;
	    			
		            this.setEntry(null);
		            
		            this.tstate = UntarState.SKIP;
    			case SKIP:
    	            if (!in.isReadable()) 
    	            	continue;
    	            
	    			// check if there is still padding left in the entry we were last reading from
		    		if (this.remainSkip > 0) {
		                int skipSize = (int) Math.min(this.remainSkip, in.readableBytes());                
		                this.remainSkip -= skipSize;                
		                
		                //System.out.println("skipping content: " + skipSize);
		                
		                in.skipBytes((int) skipSize);
		    		}
	    			
	    			if (this.remainSkip > 0) 
	    				continue;
		    		
		            this.tstate = UntarState.RECORD;
    			}
    		}
    		
    		in.release();
    	}
    	
		// write all messages in the queue
    	return this.handlerFlush();
    }

	public void setEntry(TarArchiveEntry v) {
		if (v == null) {
			this.currEntry = null;
			this.currfile = null;
		}
		else {
			// create the output message
			FileDescriptor blk = new FileDescriptor();
			
	        blk.setPath("/" + v.getName());
	        blk.setSize(v.getRealSize());
	        blk.setModTime(v.getModTime().getTime());
	        
	        this.currfile = blk;
			this.currEntry = v;
		}
	}
    
    protected boolean isEOFRecord(byte[] record) {
        return record == null || ArchiveUtils.isArrayZero(record, TarConstants.DEFAULT_RCDSIZE);
    }
}
