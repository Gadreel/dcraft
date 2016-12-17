package dcraft.web.ui.adapter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dcraft.filestore.CommonPath;
import dcraft.hub.SiteInfo;
import dcraft.io.ByteBufWriter;
import dcraft.io.CacheFile;
import dcraft.web.core.IOutputAdapter;
import dcraft.web.core.IOutputContext;
import dcraft.web.core.Response;
import dcraft.web.core.WebContext;

public class SsiOutputAdapter implements IOutputAdapter {
	public static final Pattern SSI_VIRTUAL_PATTERN = Pattern.compile("<!--#include virtual=\"(.*)\" -->");
	
	public String processIncludes(IOutputContext ctx, String content) {
		boolean checkmatches = true;

		while (checkmatches) {
			checkmatches = false;
			Matcher m = SsiOutputAdapter.SSI_VIRTUAL_PATTERN.matcher(content);

			while (m.find()) {
				String grp = m.group();

				String vfilename = grp.substring(1, grp.length() - 1);
				
				vfilename = vfilename.substring(vfilename.indexOf('"') + 1);
				vfilename = vfilename.substring(0, vfilename.indexOf('"'));

				//System.out.println("include v file: " + vfilename);
				
				CacheFile sf = ctx.getSite().getWebsite().findSectionFile("www", vfilename, ctx.isPreview());
				
				if (sf == null) 
					continue;
				
				CharSequence val = sf.asChars();
				
				if (val == null)
					val = "";
				
				content = content.replace(grp, val);
				checkmatches = true;
			}
		}	
		
		return content;
	}
	
	public CommonPath webpath = null;
	public CacheFile file = null;
	protected String mime = null; 

	public CacheFile getFile() {
		return this.file;
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
			
			resp.setHeader("Content-Type", this.mime);
			resp.setDateHeader("Date", System.currentTimeMillis());
			resp.setDateHeader("Last-Modified", this.file.getWhen());
			resp.setHeader("X-UA-Compatible", "IE=Edge,chrome=1");
			
			// because of Macro support we need to rebuild this page every time it is requested
			String content = this.processIncludes(ctx, this.file.asString());
			
			content = ctx.expandMacros(content);

			// TODO add compression
			//if (asset.getCompressed())
			//	resp.setHeader("Content-Encoding", "gzip");
			
			ByteBufWriter buffer = ByteBufWriter.createLargeHeap();
			buffer.write(content);
			
			wctx.sendStart(buffer.readableBytes());

			wctx.send(buffer.getByteBuf());

			wctx.sendEnd();
		}
		else {
			// TODO some sort of error
		}
	}
}
