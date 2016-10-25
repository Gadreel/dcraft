package dcraft.web.http;

import dcraft.io.ByteBufWriter;
import dcraft.lang.op.FuncCallback;
import dcraft.log.Logger;
import dcraft.web.core.IContentDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;

public class BasicContentDecoder implements IContentDecoder {
	protected FuncCallback<ByteBuf> callback = null;
	protected ByteBufWriter buffer = ByteBufWriter.createLargeHeap();
	
	public ByteBuf getBuffer() {
		return this.buffer.getByteBuf();
	}
	
	public BasicContentDecoder withCallback(FuncCallback<ByteBuf> v) {
		this.callback = v;
		return this;
	}	
	
	@Override
	public void offer(HttpContent chunk) {
		boolean finalchunk = (chunk instanceof LastHttpContent); 
		
		ByteBuf buffer = chunk.content();
	
        int size = buffer.readableBytes();
        
        if (Logger.isDebug())
        	Logger.debug("Offered chunk size: " + size + " final: " + finalchunk);
        
       	this.buffer.write(buffer);
       	
		if (finalchunk) {
			this.callback.setResult(this.buffer.getByteBuf());
			this.callback.complete();
		}
	}

	@Override
	public void release() {
	}
}
