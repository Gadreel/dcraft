package dcraft.web.core;

import dcraft.hub.TenantInfo;
import dcraft.hub.SiteInfo;
import dcraft.struct.RecordStruct;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.stream.ChunkedInput;

public interface IInnerContext {
    Request getRequest();
    Response getResponse();
    TenantInfo getTenant();
    SiteInfo getSite();
    IWebMacro getMacro(String name);
    void setAltParams(RecordStruct v);
    RecordStruct getAltParams();

	void send();
	void sendStart(int contentLength);
	void send(ByteBuf content);
	void send(ChunkedInput<HttpContent> content);
	void sendEnd();
	void close();
}
