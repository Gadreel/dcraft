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
package dcraft.web.core;

import java.nio.ByteBuffer;

import dcraft.lang.Memory;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;

public class HttpBodyRequestDecoder implements IContentDecoder {
	protected Memory m = new Memory();
	protected int max = 0;
	protected IBodyCallback callback = null;
	
	public HttpBodyRequestDecoder(int max, IBodyCallback cb) {
		this.callback = cb;
		this.max = max;
	}
	
	public void offer(HttpContent chunk) {
		int newsize = chunk.content().readableBytes() + m.getLength();
		
		if (newsize > this.max) {
			this.callback.fail();
			this.callback = null;
			return;
		}
		
		for (ByteBuffer b : chunk.content().nioBuffers())
			m.write(b);
		
		if (chunk instanceof LastHttpContent)  {
			this.callback.ready(this.m);
			this.callback = null;
		}
	}
	
	@Override
	public void release() {
		this.callback = null;
	}
	
	@Override
	public String toString() {
		return this.m.toString();
	}
}
