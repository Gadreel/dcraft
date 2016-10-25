package dcraft.ctp.stream;

import io.netty.buffer.ByteBuf;
import dcraft.ctp.f.FileDescriptor;

public class FileSlice {
	final static public FileSlice FINAL = new FileSlice();
	
	public static FileSlice allocate(FileDescriptor file, ByteBuf buf, long offset, boolean eof) {
		FileSlice s = new FileSlice();
		
		s.file = file;
		s.data = buf;
		s.offset = offset;
		s.eof = eof;
		
		return s;
	}
	
	// members
	
	protected FileDescriptor file = null;
	protected ByteBuf data = null;
	protected long offset = 0;
	protected boolean eof = false;
	
	public FileDescriptor getFile() {
		return this.file;
	}
	
	public void setFile(FileDescriptor v) {
		this.file = v;
	}
	
	public ByteBuf getData() {
		return this.data;
	}
	
	public void setData(ByteBuf v) {
		this.data = v;
	}
	
	public long getOffset() {
		return this.offset;
	}
	
	public void setOffset(long v) {
		this.offset = v;
	}
	
	public boolean isEof() {
		return this.eof;
	}
	
	public void setEof(boolean v) {
		this.eof = v;
	}
	
	public void release() {
		if (this.data != null) 
			this.data.release();
		
		this.data = null;
		this.file = null;
	}

}
