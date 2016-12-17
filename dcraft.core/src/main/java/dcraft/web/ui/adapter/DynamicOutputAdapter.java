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

import dcraft.hub.Hub;
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
	
	public UIElement getSource(WebContext wctx) {
		if (this.source != null)
			return this.source;
		
		CharSequence xml = this.file.asChars();
		
		if (xml.length() == 0) 
			return null;
		
		xml = this.processIncludes(wctx, xml.toString());
		
		FuncResult<XElement> xres = OperationContext.get().getSite().getWebsite().parseUI(xml);
		
		// don't stop on error
		if (xres.hasErrors()) 
			return null;
		
		this.source = (UIElement) xres.getResult();
		
		return this.source;
	}
	
	@Override
	public boolean isAuthorized() {
		return ((this.auth == null) || OperationContext.get().getUserContext().isTagged(this.auth));
	}

	@Override
	public void execute(IOutputContext ctx) throws Exception {
		if (ctx instanceof WebContext) {
			WebContext wctx = (WebContext) ctx;
		
			String mode = wctx.getExternalParam("_dcui");
			
			boolean prebuilt = false;
			
			// one time assist - cache the first page load
			UIElement xr = OperationContext.get().getSession().getPageCache(webpath);
			
			if ((xr == null) || ! "dyn".equals(mode)) {
				xr = this.getSource(wctx);
			}
			else {
				this.source = xr;
				prebuilt = true;
			}
			
			if (xr == null) {
				OperationContext.get().clearExitCode();		// TODO review how to handle this
				
				// no source - then run as a SSI
				if (ctx.getSite().getWebsite().getHtmlMode() == HtmlMode.Strict) {
					this.source = xr = new Html();
					
					xr.with(new UIElement("h1")
						.withText("Unable to parse page error!!")
					);
				}
				else {
					super.execute(ctx);
				}
				
				return;
			}
			
			if (xr.hasAttribute("AuthTags"))
				this.auth = xr.getAttribute("AuthTags").split(",");
			
			if (!this.isAuthorized()) {
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
			
			UIElement fxr = xr;
			
			OperationObserver oo = new OperationObserver() {
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
			    	prt.print(fxr);

					wctx.send();
					
					if (! wctx.isDynamic()) 
						octx.getSession().setPageCache(DynamicOutputAdapter.this.webpath, fxr);
				}
			};
			
			if (prebuilt) {
				System.out.println("using page cache");
				oo.completed(OperationContext.get());
			}
			else {
				UIWork work = new UIWork();
				work.setContext(ctx);
				work.setRoot(fxr);
				
				Task task = Task
					.taskWithSubContext()
					.withTitle("Working on web content: " + this.getFile().getFilePath())
					.withTopic("Web")
					.withObserver(oo)
					.withWork(work);
				
				Hub.instance.getWorkPool().submit(task);
			}
		}
		else {
			// TODO some sort of error
		}
	}
}
