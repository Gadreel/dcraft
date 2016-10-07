package dcraft.ctp.cmd;

import io.netty.buffer.ByteBuf;
import dcraft.ctp.CtpCommand;
import dcraft.hub.Hub;
import dcraft.lang.chars.Special;
import dcraft.lang.chars.Utf8Encoder;
import dcraft.struct.RecordStruct;
import dcraft.struct.builder.ObjectBuilder;
import dcraft.struct.serial.BufferToCompositeParser;

abstract public class BodyCommand extends CtpCommand {
	protected RecordStruct body = null;
	
	public void setBody(RecordStruct v) {
		this.body = v;
	}
	
	public RecordStruct getBody() {
		return this.body;
	}
	
	public boolean isOp(String name) {
		if (this.body == null)
			return false;
		
		return name.equals(this.body.getFieldAsString("Op"));
	}
	
	@Override
	public ByteBuf encode() throws Exception {
		int size = 1 + 4096;  // code + JSON   --- TODO, current max is 4KB
		
		ByteBuf bb = Hub.instance.getBufferAllocator().buffer(size);
		
		bb.writeByte(this.cmdCode);
		
		if (this.body != null)
			this.body.toSerial(bb);
		else
			bb.writeBytes(Utf8Encoder.encode(Special.End.getCode()));
		
		return bb;
	}

	@Override
	public void release() {
		// na
	}
	
    protected BufferToCompositeParser headerparser = null;		
    protected ObjectBuilder builder = null;

	@Override
	public boolean decode(ByteBuf in) throws Exception {
		if (this.headerparser == null) {
			this.builder = new ObjectBuilder();
			this.headerparser = new BufferToCompositeParser(this.builder);
		}
		
		this.headerparser.parseStruct(in);
		
		// if not done wait for more bytes
		if (!this.headerparser.isDone())
			return false;
		
		this.body = (RecordStruct)this.builder.getRoot();
		this.builder = null;
		this.headerparser = null;

		return true;
	}
}
