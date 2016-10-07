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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import dcraft.filestore.IFileStoreDriver;
import dcraft.filestore.IFileStoreFile;
import dcraft.filestore.local.FileSystemFile;
import dcraft.lang.op.OperationContext;
import dcraft.script.StackEntry;
import dcraft.struct.Struct;
import dcraft.struct.scalar.NullStruct;
import dcraft.xml.XElement;

public class FileDestStream extends BaseStream implements IStreamDest {
	protected FileSystemFile file = null;
	protected FileChannel out = null;
	protected boolean userelpath = false;
	protected String relpath = "";
	
	public FileDestStream(Path src) {
		this(StreamUtil.localFile(src));
	}

	public FileDestStream(FileSystemFile file) {
		this.file = file;
	}

	public FileDestStream withRelative(boolean v) {
		this.userelpath = v;
		return this;
	}
	
	// for use with dcScript
	@Override
	public void init(StackEntry stack, XElement el, boolean autorelative) {
		if (autorelative || stack.boolFromElement(el, "Relative", false) || el.getName().startsWith("X")) {
        	this.userelpath = true;
        }

        Struct src = stack.refFromElement(el, "RelativeTo");
        
        if ((src != null) && !(src instanceof NullStruct)) {
            if (src instanceof IFileStoreDriver) 
            	this.relpath = "";
            else if (src instanceof IFileStoreFile)
            	this.relpath = ((IFileStoreFile)src).getPath();
            else 
            	this.relpath = src.toString();
            
        	this.userelpath = true;
        }
	}
	
	@Override
	public void close() {
		//System.out.println("File DEST killed");	// TODO
		
		if (this.out != null)
			try {
				this.out.close();
			} 
			catch (IOException x) {
			}
		
		this.out = null;
		this.file = null;
		
		super.close();
	}
	
	@Override
	public ReturnOption handle(FileSlice slice) {
		if (slice == FileSlice.FINAL) {
			OperationContext.get().getTaskRun().complete();
			return ReturnOption.DONE;
		}
		
		if (this.file.isFolder())
			return this.handleLocalFolder(slice);
		
		return this.handleLocalFile(slice);
	}
	
	public ReturnOption handleLocalFile(FileSlice slice) {
		if (slice.file.isFolder()) {
			if (slice.data != null)
				slice.data.release();
			
			OperationContext.get().getTaskRun().kill("Folder cannot be stored into a file");
			return ReturnOption.DONE;
		}
		
		if (slice.data != null) {
			if (this.out == null) {
				try {
					Path dpath = this.file.localPath();
					
					Files.createDirectories(dpath.getParent());
					
					this.out = FileChannel.open(dpath, 
							StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC);
				} 
				catch (IOException x) {
					if (slice.data != null)
						slice.data.release();
					
					OperationContext.get().getTaskRun().kill("Problem opening destination file: " + x);
					return ReturnOption.DONE;
				}
			}
			
			for (ByteBuffer buff : slice.data.nioBuffers()) {
				try {
					this.out.write(buff);
				} 
				catch (IOException x) {
					slice.data.release();
					OperationContext.get().getTaskRun().kill("Problem writing destination file: " + x);
					return ReturnOption.DONE;
				}
			}
		
			slice.data.release();
		}
		
		if (slice.isEof()) {
			try {
				if (this.out != null) {
					this.out.close();
					this.out = null;
				}
				
				this.file.refreshProps();
			} 
			catch (IOException x) {
				OperationContext.get().getTaskRun().kill("Problem closing destination file: " + x);
				return ReturnOption.DONE;
			}
		}
		
		return ReturnOption.CONTINUE;
	}
	
	public ReturnOption handleLocalFolder(FileSlice slice) {
		Path folder = this.file.localPath();
		
		if (Files.notExists(folder))
			try {
				Files.createDirectories(folder); 
			} 
			catch (IOException x) {
				if (slice.data != null)
					slice.data.release();
				
				OperationContext.get().getTaskRun().kill("Problem making destination top folder: " + x);
				return ReturnOption.DONE;
			}
		
		String fpath = (this.userelpath) ? this.relpath + slice.file.getPath() : "/" + slice.file.path().getFileName();
		
		if (slice.file.isFolder()) {
			try {
				Files.createDirectories(folder.resolve(fpath.substring(1))); 
			} 
			catch (IOException x) {
				if (slice.data != null)
					slice.data.release();
				
				OperationContext.get().getTaskRun().kill("Problem making destination folder: " + x);
				return ReturnOption.DONE;
			}
			
			return ReturnOption.CONTINUE;
		}

		if (this.out == null)
			try {
				Path dpath = folder.resolve(fpath.substring(1));
				
				Files.createDirectories(dpath.getParent());
				
				this.out = FileChannel.open(dpath, 
						StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC);
			} 
			catch (IOException x) {
				if (slice.data != null)
					slice.data.release();
				
				OperationContext.get().getTaskRun().kill("Problem opening destination file: " + x);
				return ReturnOption.DONE;
			}
		
		if (slice.data != null) {
			for (ByteBuffer buff : slice.data.nioBuffers()) {
				try {
					this.out.write(buff);
				} 
				catch (IOException x) {
					slice.data.release();
					OperationContext.get().getTaskRun().kill("Problem writing destination file: " + x);
					return ReturnOption.DONE;
				}
			}
			
			slice.data.release();
		}
		
		if (slice.isEof()) {
			try {
				this.out.close();
				this.out = null;
				
				this.file.refreshProps();
			} 
			catch (IOException x) {
				OperationContext.get().getTaskRun().kill("Problem closing destination file: " + x);
				return ReturnOption.DONE;
			}
		}
		
		return ReturnOption.CONTINUE;
	}

	@Override
	public void read() {
		// we are terminal, no downstream should call us
		OperationContext.get().getTaskRun().kill("File destination cannot be a source");
	}

	@Override
	public void execute() {
		// TODO optimize if upstream is local file also
		
		this.upstream.read();
	}
}
