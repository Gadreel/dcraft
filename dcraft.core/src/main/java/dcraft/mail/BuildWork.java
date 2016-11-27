package dcraft.mail;

import java.util.List;

import dcraft.filestore.CommonPath;
import dcraft.filestore.IFileStoreFile;
import dcraft.groovy.GCompClassLoader;
import dcraft.io.CacheFile;
import dcraft.lang.op.FuncCallback;
import dcraft.lang.op.OperationCallback;
import dcraft.lang.op.OperationContext;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.web.md.Processor;
import dcraft.web.ui.UIElement;
import dcraft.work.StateWork;
import dcraft.work.TaskRun;
import dcraft.work.WorkStep;
import groovy.lang.GroovyObject;

public class BuildWork extends StateWork {
	final public WorkStep BUILD_MD = WorkStep.allocate("Build Markdown", this::buildMd);
	final public WorkStep BULD_HTML = WorkStep.allocate("Build Html", this::buildHtml);
	final public WorkStep BULD_ATTACH = WorkStep.allocate("Build Attachments", this::buildAttach);
	
	protected EmailContext ctx = null;
	protected GroovyObject script = null;
	
	@Override
	public void prepSteps(TaskRun trun) {
		this.withStep(this.INITIALIZE)
		.withStep(this.BUILD_MD)
		.withStep(this.BULD_HTML)
		.withStep(this.BULD_ATTACH)
		.withStep(this.FINIALIZE);
	}
	
	@Override
	public WorkStep initialize(TaskRun trun) {
		super.initialize(trun);
		
		RecordStruct params = trun.getTask().getParams();
		
		RecordStruct dparams = params.getFieldAsRecord("Data");
		IFileStoreFile dpath = (IFileStoreFile) params.getField("DataPath");
		
		if (dpath != null) {
			dpath.driver().getFileDetail(dpath.resolvePath(new CommonPath("/data.json")),
				new FuncCallback<IFileStoreFile>() {
					@Override
					public void callback() {
						if (this.hasErrors() || ! this.getResult().exists()) {
							BuildWork.this.transition(trun, BuildWork.this.FINIALIZE);
							return;
						}
						
						this.getResult().readAllText(new FuncCallback<String>() {
							@Override
							public void callback() {
								if (this.hasErrors()) {
									BuildWork.this.transition(trun, BuildWork.this.FINIALIZE);
									return;
								}
								
								CharSequence chars = this.getResult();

								RecordStruct rparams = (RecordStruct) Struct.objectToComposite(chars);
								
								BuildWork.this.transition(trun, BuildWork.this.finalInit(params, rparams, dpath, trun));
							}
						});
					}
				});
			
			return WorkStep.WAIT;
		}
		else {
			return this.finalInit(params, dparams, null, trun);
		}
	}
	
	public WorkStep finalInit(RecordStruct params, RecordStruct dparams, IFileStoreFile ddpath, TaskRun trun) {
		String path = params.getFieldAsString("Template");
		
		this.ctx = EmailContext.forRequestParams(new CommonPath(path), params, dparams, ddpath);
		
		CacheFile cfile = this.ctx.getSite().getWebsite().findSectionFile("email", path + ".groovy", this.ctx.isPreview());
		
		if (cfile == null) 
			return WorkStep.NEXT;
		
		try {
			Class<?> groovyClass = this.ctx.getSite().getScriptLoader().toClass(cfile.getFilePath());
			
			this.script = (GroovyObject) groovyClass.newInstance();
			
			// run init if present
			if (GCompClassLoader.tryExecuteMethodCtx(this.script, "init", this.ctx, this, trun))
				return WorkStep.WAIT;
			
			return WorkStep.NEXT;
		}
		catch (Exception x) {
			trun.error("Unable to prepare email script: " + path + ".groovy");
			trun.error("Error: " + x);
		}		
		
		return this.FINIALIZE;
	}
	
	public WorkStep buildMd(TaskRun trun) {
		RecordStruct params = trun.getTask().getParams();
		
		String path = params.getFieldAsString("Template") + ".md";
		
		CacheFile cfile = this.ctx.getSite().getWebsite().findSectionFile("email", path, this.ctx.isPreview());
		
		if (cfile == null) 
			return WorkStep.NEXT;

		try {
			TextOutputAdapter output = new TextOutputAdapter();
			
			output.init(this.ctx, cfile, new CommonPath(path));
			
			// TODO finish this
			if (this.script != null) {
				try {
					// run beforeMarkdown if present
					GCompClassLoader.tryExecuteMethodCtx(this.script, "beforeMarkdown", this.ctx, trun, output);
				}
				catch (Exception x) {
					trun.error("Unable to run beforeMarkdown email script: " + path);
					trun.error("Error: " + x);
					return this.FINIALIZE;
				}		
			}
			
			if (OperationContext.get().hasErrors()) {
				trun.errorTr(150001);
				return this.FINIALIZE;
			}
			
			output.execute(this.ctx, new OperationCallback() {
				@Override
				public void callback() {
					BuildWork.this.transition(trun, WorkStep.NEXT);
				}
			});
			
			return WorkStep.WAIT;
		} 
		catch (Exception x) {
			trun.error("Unable to build email: " + x);
		}		
		
		return this.FINIALIZE;
	}
	
	public WorkStep buildHtml(TaskRun trun) {
		try {
			RecordStruct params = trun.getTask().getParams();
			
			HtmlOutputAdapter output = new HtmlOutputAdapter();
			
			String path = params.getFieldAsString("Template") + ".html";
			
			// TODO finish this
			if (this.script != null) {
				try {
					// run beforeMarkdown if present
					GCompClassLoader.tryExecuteMethodCtx(this.script, "afterMarkdown", this.ctx, trun);
				}
				catch (Exception x) {
					trun.error("Unable to run afterMarkdown email script: " + path);
					trun.error("Error: " + x);
					return this.FINIALIZE;
				}		
			}
			
			CacheFile cfile = this.ctx.getSite().getWebsite().findSectionFile("email", path, this.ctx.isPreview());
			
			if (cfile == null) {
				UIElement html = Processor.parse(ctx.getMarkdownContext(), ctx.getText());
				
				output.init(this.ctx, html, new CommonPath(path));
			}
			else {
				output.init(this.ctx, cfile, new CommonPath(path));
			}
			
			if (this.script != null) {
				try {
					// run beforeMarkdown if present
					GCompClassLoader.tryExecuteMethodCtx(this.script, "beforeHtml", this.ctx, trun, output);
				}
				catch (Exception x) {
					trun.error("Unable to run beforeHtml email script: " + path);
					trun.error("Error: " + x);
					return this.FINIALIZE;
				}		
			}
			
			if (OperationContext.get().hasErrors()) {
				trun.errorTr(150001);
				return this.FINIALIZE;
			}
			
			output.execute(this.ctx, new OperationCallback() {
				@Override
				public void callback() {
					BuildWork.this.transition(trun, WorkStep.NEXT);
				}
			});
			
			return WorkStep.WAIT;
		} 
		catch (Exception x) {
			trun.error("Unable to build email: " + x);
		}		
		
		return this.FINIALIZE;
	}
	
	public WorkStep buildAttach(TaskRun trun) {
		RecordStruct params = trun.getTask().getParams();
		
		if (! params.getFieldAsBooleanOrFalse("Managed"))
			return WorkStep.NEXT;
		
		IFileStoreFile dpath = (IFileStoreFile) params.getField("DataPath");
		
		if (dpath == null) 
			return WorkStep.NEXT;
		
		dpath.driver().getFolderListing(dpath.path(),
			new FuncCallback<List<IFileStoreFile>>() {
				@Override
				public void callback() {
					if (this.hasErrors()) {
						BuildWork.this.transition(trun, BuildWork.this.FINIALIZE);
						return;
					}
					
					ListStruct files = BuildWork.this.ctx.getData().getFieldAsRecord("Data").getFieldAsList("Files");
					
					for (IFileStoreFile fsf : this.getResult()) {
						if ("data.json".equals(fsf.getName()))
							continue;
						
						for (Struct fn : files.getItems()) {
							if (Struct.objectToString(fn).equals(fsf.getName())) {
								BuildWork.this.ctx.addAttachment(EmailAttachment.forFile(fsf));
								break;
							}
						}
					}
					
					BuildWork.this.transition(trun, WorkStep.NEXT);
				}
			});
		
		return WorkStep.WAIT;
	}
	
	@Override
	public WorkStep finialize(TaskRun trun) {
		RecordStruct params = trun.getTask().getParams();
		String path = params.getFieldAsString("Template") + ".html";
		
		// TODO finish this
		if (this.script != null) {
			try {
				// run beforeMarkdown if present
				GCompClassLoader.tryExecuteMethodCtx(this.script, "afterHtml", this.ctx, trun);
			}
			catch (Exception x) {
				trun.error("Unable to run afterHtml email script: " + path);
				trun.error("Error: " + x);
				
				return super.finialize(trun);
			}		
		}
		
		trun.setResult(this.ctx.toParams());

		return super.finialize(trun);
	}
}
