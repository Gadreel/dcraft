package dcraft.mail;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.regex.Matcher;

import dcraft.filestore.CommonPath;
import dcraft.io.CacheFile;
import dcraft.lang.op.OperationCallback;
import dcraft.log.Logger;
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
		String md = this.processContent(ctx, this.content);
		
		try (BufferedReader bufReader = new BufferedReader(new StringReader(md))) {
			String line = bufReader.readLine();
			
			while (StringUtil.isNotEmpty(line)) {
				int pos = line.indexOf(':');
				
				if (pos == -1)
					break;
				
				String name = line.substring(0, pos).replace("-", "");	// dash is optional (as in Reply-To:)
				String value = line.substring(pos + 1).trim();
				
				if ("Subject".equals(name))
					ctx.setSubject(value);
				else if ("Title".equals(name))
					ctx.setSubject(value);
				else if ("ReplyTo".equals(name))
					ctx.setReplyTo(value);
				else if ("From".equals(name))
					ctx.setFrom(value);
				else if ("To".equals(name))
					ctx.setTo(value);
	
				line = bufReader.readLine();
			}
			
			// see if there is more - the body
			if (line != null) {
				StringBuilder sb = new StringBuilder();
				
				line = bufReader.readLine();
				
				while (line != null) {
					sb.append(line);
					sb.append("\n");
		
					line = bufReader.readLine();
				}
				
				ctx.setText(StringUtil.stripLeadingWhitespace(sb.toString()));
			}		
		}
		catch (Exception x) {
			Logger.error("Problem loading Email template: " + x);
		}
		
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
