package dcraft.web.ui.adapter;

import io.netty.handler.codec.http.HttpResponseStatus;

import java.lang.reflect.Method;

import dcraft.filestore.CommonPath;
import dcraft.hub.SiteInfo;
import dcraft.io.ByteBufWriter;
import dcraft.io.CacheFile;
import dcraft.lang.op.OperationContext;
import dcraft.net.NetUtil;
import dcraft.util.StringUtil;
import dcraft.web.core.IOutputAdapter;
import dcraft.web.core.IOutputContext;
import dcraft.web.core.Response;
import dcraft.web.core.WebContext;
import groovy.lang.GroovyObject;

public class GasOutputAdapter implements IOutputAdapter {
	protected CommonPath webpath = null;
	protected CacheFile file = null;
	protected String mime = null; 
	protected String attachmentName = null; 
	protected boolean compressed = false;
	protected ByteBufWriter buffer = null;

	public CacheFile getFile() {
		return this.file;
	}
	
	public void setMime(String v) {
		this.mime = v;
	}
	
	public String getMime() {
		return this.mime;
	}
	
	public void setCompressed(boolean v) {
		this.compressed = v;
	}
	
	public boolean getCompressed() {
		return this.compressed;
	}
	
	public void setAttachmentName(String v) {
		this.attachmentName = v;
	}

	public String getAttachmentName() {
		return this.attachmentName;
	}
	
	@Override
	public boolean isAuthorized() {
		return true;
	}
	
	@Override
	public void init(SiteInfo site, CacheFile file, CommonPath web, boolean isPreview) {
		this.webpath = web;
		this.file = file;		
		this.mime = site.getMimeType(this.file.getExt());		
	}
	
	@Override
	public void execute(IOutputContext ctx) throws Exception {
		if (ctx instanceof WebContext) {
			WebContext wctx = (WebContext) ctx;
			
			Response resp = wctx.getResponse(); 
			
			resp.setDateHeader("Date", System.currentTimeMillis());
			resp.setDateHeader("Last-Modified", this.file.getWhen());
			resp.setHeader("X-UA-Compatible", "IE=Edge,chrome=1");
			resp.setHeader("Cache-Control", "no-cache");
			
			// because of Macro support we need to rebuild this page every time it is requested
			String content = file.asString();
	
			try {
				Method runmeth = null;
				
				Class<?> groovyClass = ctx.getSite().getScriptLoader().toClass(content);
				
				for (Method m : groovyClass.getMethods()) {
					//System.out.println("method: " + m.getName());
					if (!m.getName().startsWith("runAsync"))
						continue;
					
					runmeth = m;
					break;
				}
				
				if (runmeth == null) {
					for (Method m : groovyClass.getMethods()) {
						//System.out.println("method: " + m.getName());
						if (!m.getName().startsWith("run"))
							continue;
						
						runmeth = m;
						break;
					}
				}
				
				if (runmeth != null) {
					this.buffer = ByteBufWriter.createLargeHeap();
		
					this.mime = "text/html";
					
					GroovyObject groovyObject = (GroovyObject) groovyClass.newInstance();
					Object[] args2 = { ctx, this.buffer, this };
					
					groovyObject.invokeMethod(runmeth.getName(), args2);
									
					// assume runAsync will handle the send
					if (!"runAsync".equals(runmeth.getName()))
						this.send(wctx);
				}
			}
			catch (Exception x) {
				OperationContext.get().error("Unable to execute script! Error: " + x);
				wctx.getResponse().setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
				wctx.getResponse().setKeepAlive(false);
				wctx.send();			
			}
		}
		else {
			// TODO some sort of error
		}
	}
	
	public void send(WebContext ctx) {
		Response resp = ctx.getResponse(); 
		
		resp.setHeader("Content-Type", this.mime);

		if (StringUtil.isNotEmpty(this.attachmentName))
			resp.setHeader("Content-Disposition", "attachment; filename=\"" + NetUtil.urlEncodeUTF8(this.attachmentName) + "\"");
		
		if (this.compressed)
			resp.setHeader("Content-Encoding", "gzip");
		
		//ByteBufWriter buffer = ByteBufWriter.createLargeHeap();
		//buffer.write(content);
		
		ctx.sendStart(this.buffer.readableBytes());

		ctx.send(this.buffer.getByteBuf());

		ctx.sendEnd();			
	}
}
