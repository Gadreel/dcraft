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
package dcraft.web.ui.adapter;

import java.io.BufferedReader;
import java.io.StringReader;

import dcraft.hub.Hub;
import dcraft.util.StringUtil;
import dcraft.web.core.IOutputContext;
import dcraft.web.core.WebContext;
import dcraft.web.ui.UIElement;
import dcraft.web.ui.tags.Body;
import dcraft.web.ui.tags.Html;
import dcraft.web.ui.tags.Markdown;
import dcraft.web.ui.tags.PagePart;
import dcraft.xml.XText;

public class MarkdownOutputAdapter extends DynamicOutputAdapter  {
	protected UIElement source = null;
	protected String[] auth = null;		

	@Override
	public UIElement getSource(WebContext wctx) {
		if (this.source != null)
			return this.source;
		
		String md = this.file.asString();
		
		if (md.length() == 0) 
			return null;
		
		md = this.processIncludes(wctx, md);
		
		try {
			Html html = new Html();
			
			BufferedReader bufReader = new BufferedReader(new StringReader(md));
	
			String line = bufReader.readLine();
			
			while (StringUtil.isNotEmpty(line)) {
				int pos = line.indexOf(':');
				
				if (pos == -1)
					break;
				
				html.withAttribute(line.substring(0, pos), line.substring(pos + 1).trim());
	
				line = bufReader.readLine();
			}
			
			// see if there is more - the body
			if (line != null) {
				UIElement body = html.hasNotEmptyAttribute("Skeleton") 
						? (UIElement) new PagePart().withAttribute("For", "article")
						: new Body();
				
				html.with(body);
				
				Markdown mtag = new Markdown();
				
				body.with(mtag);  
				
				XText mdata = new XText();
				mdata.setCData(true);
				
				mtag.with(mdata);
				
				line = bufReader.readLine();
				
				while (line != null) {
					mdata.appendBuffer(line);
					mdata.appendBuffer("\n");
		
					line = bufReader.readLine();
				}
				
				mdata.closeBuffer();
			}
			
			this.source = html;
		}
		catch (Exception x) {
			System.out.println("md parse issue");
		}
		
		return this.source;
	}
	
	@Override
	public void execute(IOutputContext ctx) throws Exception {
		Hub.instance.getCountManager().countObjects("dcWebOutMarkdownCount-" + ctx.getTenant().getAlias(), this);
		
		super.execute(ctx);
	}
}
