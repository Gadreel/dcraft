package dcraft.mail;

import java.util.regex.Matcher;

import dcraft.filestore.CommonPath;
import dcraft.io.CacheFile;
import dcraft.lang.op.OperationCallback;
import dcraft.util.StringUtil;
import dcraft.web.ui.adapter.SsiOutputAdapter;

public class TextOutputAdapter implements IOutputAdapter {
	protected CommonPath webpath = null;
	protected CacheFile file = null;
	protected String mime = null; 
	protected String content = null; 

	public CacheFile getFile() {
		return this.file;
	}
	
	public void init(EmailContext ctx, String content, CommonPath web) {
		this.webpath = web;
		this.content = content;
	}
	
	@Override
	public void init(EmailContext ctx, CacheFile file, CommonPath web) {
		this.webpath = web;
		this.file = file;		
		this.mime = ctx.getSite().getMimeType(this.file.getExt());		// TODO review
		this.content = this.file.asString();
	}
	
	@Override
	public void execute(EmailContext ctx, OperationCallback callback) {
		String content = this.processContent(ctx, this.content);
		
		// first line is the subject
		int pos = content.indexOf('\n');
		
		ctx.setSubject(content.substring(0, pos));
		ctx.setText(StringUtil.stripLeadingWhitespace(content.substring(pos + 1)));
		
		callback.complete();
	}
	
	public String processContent(EmailContext ctx, String content) {
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
				
				CacheFile sf = ctx.getSite().findSectionFile("email", vfilename, ctx.isPreview());
				
				if (sf == null) 
					continue;
				
				CharSequence val = sf.asChars();
				
				if (val == null)
					val = "";
				
				content = content.replace(grp, val);
				checkmatches = true;
			}
		}
		
		content = ctx.expandMacros(content);
		
		return content;
	}
}
