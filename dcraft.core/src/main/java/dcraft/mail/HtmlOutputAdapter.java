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
package dcraft.mail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import dcraft.filestore.CommonPath;
import dcraft.hub.Hub;
import dcraft.io.CacheFile;
import dcraft.lang.chars.Utf8Decoder;
import dcraft.lang.op.FuncResult;
import dcraft.lang.op.OperationCallback;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.OperationObserver;
import dcraft.web.ui.HtmlPrinter;
import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.web.ui.tags.Html;
import dcraft.work.Task;
import dcraft.xml.XElement;
import dcraft.xml.XmlPrinter;

public class HtmlOutputAdapter implements IOutputAdapter {
	
	protected CommonPath webpath = null;
	protected CacheFile file = null;
	protected String mime = null; 
	protected UIElement source = null;

	public CacheFile getFile() {
		return this.file;
	}
	
	public UIElement getSource() {
		return this.source;
	}
	
	public void init(EmailContext ctx, UIElement source, CommonPath webpath) {
		this.webpath = webpath;
		this.source = source;
	}
	
	@Override
	public void init(EmailContext ctx, CacheFile file, CommonPath web) {
		this.webpath = web;
		this.file = file;		
		this.mime = ctx.getSite().getMimeType(this.file.getExt());		// TODO review mime resolve
		
		CharSequence xml = this.file.asChars();
		
		FuncResult<XElement> xres = ctx.getSite().getWebsite().parseUI(xml);
		
		if (xres.hasErrors()) 
			return;
		
		this.source = (UIElement) xres.getResult();
	}
	
	@Override
	public void execute(EmailContext ctx, OperationCallback callback) {
		UIElement source = this.getSource();
		
		// no source - provide error
		if (source == null) {
			source = new Html();
			
			source.with(new UIElement("h1")
				.withText("Unable to parse email template error!!")
			);
		}

		UIElement fsource = source;
		
		UIWork work = new UIWork();
		work.setContext(ctx);
		work.setRoot(source);
		
		Task task = Task
			.taskWithSubContext()
			.withTitle("Working on email html content: " + ((this.file != null) ? this.file.getFilePath() : ""))
			.withTopic("Batch")		// TODO configure
			.withObserver(new OperationObserver() {
				@Override
				public void completed(OperationContext octx) {
					// TODO if errors then create an error document
					
					try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
						PrintStream ps = new PrintStream(baos, true, "utf-8");					
	
						XmlPrinter prt = new HtmlPrinter(ctx);
						
				    	prt.setFormatted(true);
				    	prt.setOut(ps);
				    	prt.print(fsource);

				    	ctx.setHtml(Utf8Decoder.decode(baos.toByteArray()).toString());
					} 
					catch (IOException x) {
						// TODO if errors then create an error document
					}
					
					callback.complete();
				}
			})
			.withWork(work);
		
		Hub.instance.getWorkPool().submit(task);
	}
}
