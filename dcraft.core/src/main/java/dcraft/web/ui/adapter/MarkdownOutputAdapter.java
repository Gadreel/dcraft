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

import io.netty.handler.codec.http.HttpResponseStatus;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.io.StringReader;

import dcraft.filestore.CommonPath;
import dcraft.hub.Hub;
import dcraft.hub.SiteInfo;
import dcraft.io.CacheFile;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.OperationObserver;
import dcraft.util.StringUtil;
import dcraft.web.core.IOutputContext;
import dcraft.web.core.WebContext;
import dcraft.web.ui.HtmlPrinter;
import dcraft.web.ui.JsonPrinter;
import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.web.ui.tags.Body;
import dcraft.web.ui.tags.Html;
import dcraft.web.ui.tags.Markdown;
import dcraft.web.ui.tags.PagePart;
import dcraft.work.Task;
import dcraft.xml.XText;
import dcraft.xml.XmlPrinter;

public class MarkdownOutputAdapter extends SsiOutputAdapter  {
	protected UIElement source = null;
	protected String[] auth = null;		
	
	public UIElement getSource() {
		return this.source;
	}
	
	@Override
	public boolean isAuthorized() {
		return ((this.auth == null) || OperationContext.get().getUserContext().isTagged(this.auth));
	}

	@Override
	public void init(SiteInfo site, CacheFile filepath, CommonPath webpath, boolean isPreview) {
		super.init(site, filepath, webpath, isPreview);
	}

	@Override
	public void execute(IOutputContext ctx) throws Exception {
		if (ctx instanceof WebContext) {
			WebContext wctx = (WebContext) ctx;
				
			if (!this.isAuthorized()) {
				String mode = wctx.getExternalParam("_dcui");
	
				if ("dyn".equals(mode)) {
					wctx.getResponse().setHeader("Content-Type", "application/javascript");
					PrintStream ps = wctx.getResponse().getPrintStream();
					ps.println("dc.pui.Loader.failedPageLoad(1);");			
					wctx.send();
				}
				else {
					wctx.getResponse().setStatus(HttpResponseStatus.FOUND);
					wctx.getResponse().setHeader("Location", "/");
					wctx.send();
				}
				
				return;
			}
			
			String md = this.file.asString();
			
			if (md.length() == 0) {
				wctx.sendInternalError();
				return;
			}
			
			md = this.processContent(wctx, md);
			
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
			
			/* TODO
			FuncResult<XElement> xres = site.getWebsite().parseUI(xml);
			
			// don't stop on error
			if (xres.hasErrors()) {
				OperationContext.get().clearExitCode();
				return;
			}
			
			XElement xr = xres.getResult();

			// if we cannot parse then fallback on SSI handler
			if (xr instanceof UIElement) {
				this.source = (UIElement) xr;

				// cache auth tags - only after source has been fully loaded
				if (this.source.hasAttribute("AuthTags"))
					this.auth = this.source.getAttribute("AuthTags").split(",");
			}
			*/
			
			
			// TODO checks
			
			UIElement source = this.getSource();
			
			// no source - then run as a SSI
			if (source == null) {
				wctx.sendInternalError();
				return;
			}
	
			UIElement fsource = source;
			
			UIWork work = new UIWork();
			work.setContext(ctx);
			work.setRoot(source);
			
			Task task = Task
				.taskWithSubContext()
				.withTitle("Working on web content: " + this.getFile().getFilePath())
				.withTopic("Web")
				.withObserver(new OperationObserver() {
					@Override
					public void completed(OperationContext octx) {
						// TODO if errors then create an error document
						
						if (wctx.isDynamic()) {
							wctx.getResponse().setHeader("Content-Type", "application/javascript");
						}
						else {
							wctx.getResponse().setHeader("Content-Type", "text/html; charset=utf-8");
							wctx.getResponse().setHeader("X-UA-Compatible", "IE=Edge,chrome=1");
						}
						
						PrintStream ps = wctx.getResponse().getPrintStream();
	
						XmlPrinter prt = wctx.isDynamic() ? new JsonPrinter(wctx) : new HtmlPrinter(wctx);
						
				    	prt.setFormatted(true);
				    	prt.setOut(ps);
				    	prt.print(fsource);
	
						wctx.send();
					}
				})
				.withWork(work);
			
			Hub.instance.getWorkPool().submit(task);
		}
		else {
			// TODO some sort of error
		}
	}
}
