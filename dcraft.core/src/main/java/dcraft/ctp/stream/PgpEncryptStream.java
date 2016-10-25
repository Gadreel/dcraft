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

import java.io.IOException;
import java.nio.file.Paths;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRing;

import dcraft.ctp.f.FileDescriptor;
import dcraft.lang.op.OperationContext;
import dcraft.log.Logger;
import dcraft.pgp.EncryptedFileStream;
import dcraft.script.StackEntry;
import dcraft.util.FileUtil;
import dcraft.xml.XElement;

public class PgpEncryptStream extends TransformStream {
	protected EncryptedFileStream pgp = new EncryptedFileStream();
	protected boolean needInit = true;
	protected FileDescriptor efile = null;
	
    public PgpEncryptStream() {
    }
    
    public PgpEncryptStream withPgpKeyring(PGPPublicKeyRing ring) {
    	this.pgp.addPublicKey(ring);
    
    	return this;
    }
    
    public PgpEncryptStream withAlgorithm(int v) {
		this.pgp.setAlgorithm(v); 	    
    	return this;
	}
    
	@Override
	public void init(StackEntry stack, XElement el) {
		String keyPath = stack.stringFromElement(el, "Keyring");
		
		try {
			this.pgp.loadPublicKey(Paths.get(keyPath));
		} 
		catch (IOException x) {
			OperationContext.get().error("Unabled to read keyfile: " + x);
		} 
		catch (PGPException x) {
			OperationContext.get().error("Unabled to load keyfile: " + x);
		}
	}
    
	@Override
    public void close() {
		try {
			this.pgp.close();
		} 
		catch (PGPException x) {
			// it should already be closed, unless we got here by a task kill/cancel
			Logger.warn("Error closing PGP stream: " + x);
		}
    
    	super.close();
    }

	// make sure we don't return without first releasing the file reference content
	@Override
	public ReturnOption handle(FileSlice slice) {
    	if (slice == FileSlice.FINAL) 
    		return this.downstream.handle(slice);
    	
    	if (this.needInit) {
    		this.pgp.setFileName(slice.file.path().getFileName());
    		
    		try {
    			this.pgp.init();
    		}
    		catch (Exception x) {
    			OperationContext.get().getTaskRun().kill("PGP init failed: " + x);
    			return ReturnOption.DONE;
    		}
    		
    		this.initializeFileValues(slice.file);
    		
    		this.needInit = false;
    	}
    	
    	// inflate the payload into 1 or more outgoing buffers set in a queue
    	ByteBuf in = slice.data;
    	
		if (in != null) {
			this.pgp.writeData(in);
			
        	in.release();
			
			if (OperationContext.get().getTaskRun().isComplete())
				return ReturnOption.DONE;
		}
		
		// write all buffers in the queue
        ByteBuf buf = this.pgp.nextReadyBuffer();
        
        while (buf != null) {
        	ReturnOption ret = this.nextMessage(buf);
			
			if (ret != ReturnOption.CONTINUE)
				return ret;
        	
        	buf = this.pgp.nextReadyBuffer();
        }
		
        if (slice.isEof()) {
        	try {
				this.pgp.close();
			} 
        	catch (PGPException x) {
        		OperationContext.get().getTaskRun().kill("PGP close failed: " + x);
				return ReturnOption.DONE;
			}
        	
    		// write all buffers in the queue
            buf = this.pgp.nextReadyBuffer();
            
            while (buf != null) {
            	ReturnOption ret = this.nextMessage(buf);
    			
    			if (ret != ReturnOption.CONTINUE)
    				return ret;
            	
            	buf = this.pgp.nextReadyBuffer();
            }
            
            ReturnOption ret = this.lastMessage();
			
			if (ret != ReturnOption.CONTINUE)
				return ret;
        }
		
		// otherwise we need more data
		return ReturnOption.CONTINUE;
    }
    
    public ReturnOption nextMessage(ByteBuf out) {
        return this.downstream.handle(FileSlice.allocate(this.efile, out, 0, false));
    }
    
    public ReturnOption lastMessage() {
        return this.downstream.handle(FileSlice.allocate(this.efile, null, 0, true));
    }
    
    public void initializeFileValues(FileDescriptor src) {
    	this.efile = new FileDescriptor();
    	
		if (src.hasPath()) 
			this.efile.setPath(src.getPath().toString() + ".gpg");
		else
			this.efile.setPath("/" + FileUtil.randomFilename("bin") + ".gpg");    	
		
		this.efile.setModTime(src.getModTime());
		this.efile.setPermissions(src.getPermissions());
    }
    
    @Override
    public void read() {
		// write all buffers in the queue
        ByteBuf buf = this.pgp.nextReadyBuffer();
        
        while (buf != null) {
        	ReturnOption ret = this.nextMessage(buf);
			
			if (ret != ReturnOption.CONTINUE)
				return;
        	
        	buf = this.pgp.nextReadyBuffer();
        }
		
		// if we reached done and we wrote all the buffers, then send the EOF marker if not already
		if (this.pgp.isClosed()) {
			ReturnOption ret = this.lastMessage();
			
			if (ret != ReturnOption.CONTINUE)
				return;
		}
		
    	this.upstream.read();
    }	
}
