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

import java.io.PrintStream;

import dcraft.filestore.CommonPath;
import dcraft.hub.Hub;
import dcraft.hub.SiteInfo;
import dcraft.io.CacheFile;
import dcraft.lang.op.FuncResult;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.OperationObserver;
import dcraft.web.core.HtmlMode;
import dcraft.web.core.IOutputContext;
import dcraft.web.core.WebContext;
import dcraft.web.ui.HtmlPrinter;
import dcraft.web.ui.JsonPrinter;
import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.web.ui.tags.Html;
import dcraft.work.Task;
import dcraft.xml.XElement;
import dcraft.xml.XmlPrinter;

public class DynamicOutputAdapter extends SsiOutputAdapter  {
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
		
		CharSequence xml = this.file.asChars();
		
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
			
			UIElement source = this.getSource();
			
			// no source - then run as a SSI
			if (source == null) {
				if (ctx.getSite().getWebsite().getHtmlMode() == HtmlMode.Strict) {
					source = new Html();
					
					source.with(new UIElement("h1")
						.withText("Unable to parse page error!!")
					);
				}
				else {
					super.execute(ctx);
					return;
				}
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
