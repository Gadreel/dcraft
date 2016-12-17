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
package dcraft.cms.service;

import org.joda.time.DateTime;

import dcraft.bus.IService;
import dcraft.bus.Message;
import dcraft.bus.MessageUtil;
import dcraft.cms.feed.core.CollectContext;
import dcraft.cms.feed.core.DeleteMode;
import dcraft.cms.feed.core.FeedAdapter;
import dcraft.cms.feed.core.FeedIndexer;
import dcraft.cms.feed.core.FeedInfo;
import dcraft.cms.feed.core.FeedPartMatchResult;
import dcraft.cms.feed.core.Section;
import dcraft.cms.feed.core.Sections;
import dcraft.cms.util.EventUtil;
import dcraft.filestore.CommonPath;
import dcraft.filestore.IFileStoreFile;
import dcraft.filestore.bucket.Bucket;
import dcraft.lang.op.FuncCallback;
import dcraft.lang.op.OperationCallback;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.OperationResult;
import dcraft.mod.ExtensionBase;
import dcraft.struct.CompositeStruct;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.work.TaskRun;
import dcraft.xml.XElement;

public class CoreService extends ExtensionBase implements IService {	
	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");
				
		// =========================================================
		//  Events
		// =========================================================
		
		if ("Events".equals(feature)) {
			if ("Trigger".equals(op)) {
				this.handleEventTrigger(request);
				return;
			}
		}
		
		// =========================================================
		//  cms Site
		// =========================================================
		
		if ("Site".equals(feature)) {
			/* TODO
		
			if ("BuildMap".equals(op)) {
				this.handleSiteBuildMap(request);
				return;
			}
			*/
			
			if ("ImportSite".equals(op)) {
				FeedIndexer iutil = new FeedIndexer();
				
				iutil.collectSite(new CollectContext().forIndex());
				
				iutil.importSite(new OperationCallback() {
					@Override
					public void callback() {
						request.complete();
					}
				});
				
				return;
			}
		}
				
		// =========================================================
		//  cms Feeds
		// =========================================================
		
		if ("Feeds".equals(feature)) {
			/* TODO
			if ("ListPages".equals(op)) {
				this.handleListPages(request);
				return;
			}
			
			if ("AddPageFolder".equals(op)) {
				this.handleAddPageFolder(request);
				return;
			}
			
			if ("LoadFeedsDefinition".equals(op)) {
				this.handleLoadFeedsDefinitions(request);
				return;
			}
			
			if ("LoadList".equals(op)) {
				this.handleFeedLoadList(request);
				return;
			}
			
			if ("AddFeedFiles".equals(op)) {
				this.handleAddFeedFiles(request);
				return;
			}
			
			if ("AddPageFiles".equals(op)) {
				this.handleAddPageFiles(request);
				return;
			}
			
			if ("UpdateFeedFiles".equals(op)) {
				this.handleUpdateFeedFiles(request);
				return;
			}
			
			if ("UpdatePublishFeedFiles".equals(op)) {
				this.handleUpdatePublishFeedFiles(request);
				return;
			}
			
			if ("PublishFeedFiles".equals(op)) {
				this.handlePublishFeedFiles(request);
				return;
			}
			
			if ("ImportFeedFiles".equals(op)) {
				this.handleImportFeedFiles(request);
				return;
			}
			*/
			
			if ("LoadFeedInfo".equals(op)) {
				this.handleLoadFeedInfo(request);
				return;
			}
			
			if ("AlterFeedInfo".equals(op)) {
				this.handleAlterFeedInfo(request);
				return;
			}
			
			if ("DeleteFeedInfo".equals(op)) {
				this.handleDeleteFeedInfo(request);
				return;
			}
			
			if ("LoadFeedPart".equals(op)) {
				this.handleLoadFeedPart(request);
				return;
			}
			
			if ("AlterFeedPart".equals(op)) {
				this.handleAlterFeedPart(request);
				return;
			}
			
			if ("LoadFeedSection".equals(op)) {
				this.handleLoadFeedSection(request);
				return;
			}
			
			if ("AlterFeedSection".equals(op)) {
				this.handleAlterFeedSection(request);
				return;
			}
		}
		
		// =========================================================
		//  cms ManagedForm
		// =========================================================
		
		if ("ManagedForm".equals(feature)) {
			if ("Submit".equals(op)) {
				this.handleManagedFormSubmit(request);
				return;
			}
			
			if ("Complete".equals(op)) {
				this.handleManagedFormComplete(request);
				return;
			}
		}		
		
		request.errorTr(441, this.serviceName(), feature, op);
		request.complete();
	}
	
	public void handleEventTrigger(TaskRun request) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		
		EventUtil.triggerEvent(rec.getFieldAsString("Event"), rec.getFieldAsString("Alternate"), 
				rec.getFieldAsStruct("Data"));
		
		request.returnEmpty();
		return;
	}
	
	public void handleManagedFormSubmit(TaskRun request) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		
		String form = rec.getFieldAsString("Form");
		boolean formfnd = false;
		
		XElement mforms = OperationContext.get().getSite().getSettings().find("ManagedForms");
		
		if (mforms == null) {
			request.error("ManagedForms not enabled.");
			request.returnEmpty();
			return;
		}
		
		for (XElement mf : mforms.selectAll("ManagedForm")) {
			if (form.equals(mf.getAttribute("Name"))) {
				formfnd = true;
				
				if (mf.hasNotEmptyAttribute("Type")) {
					OperationResult vor = request.getContext().getSchema().validateType(rec.getField("Data"), mf.getAttribute("Type"));
					
					if (vor.hasErrors()) {
						request.returnEmpty();
						return;
					}
				}
			}
		}
		
		if (! formfnd) {
			request.error("Requested Managed Form not enabled.");
			request.returnEmpty();
			return;
		}
		
		Bucket mfbucket = OperationContext.get().getSite().getBucket("ManagedForm");
		
		mfbucket.handleAllocateUploadToken(rec, new FuncCallback<RecordStruct>() {			
			@Override
			public void callback() {
				if (this.hasErrors()) {
					request.returnEmpty();
					return;
				}
				
				RecordStruct token = this.getResult();
				
				mfbucket.getFileStore().getFileDetail(new CommonPath("/" + token.getFieldAsString("Token") + "/data.json"), new FuncCallback<IFileStoreFile>() {
					@Override
					public void callback() {
						if (this.hasErrors()) {
							request.returnEmpty();
							return;
						}
						
						RecordStruct eventdata = (RecordStruct) rec.deepCopy();
						
						eventdata.withField("Token", token.getFieldAsString("Token"));
						eventdata.withField("SubmitAt", new DateTime());
						
						this.getResult().writeAllText(eventdata.toPrettyString(), new OperationCallback() {
							@Override
							public void callback() {
								// TODO add database layer - store title, submit at and token
								
								/*
								mfbucket.getFileStore().getFolderListing(new CommonPath("/" + token.getFieldAsString("Token")), new FuncCallback<List<IFileStoreFile>>() {
									@Override
									public void callback() {
										if (this.hasErrors()) {
											request.returnEmpty();
											return;
										}
										
										List<IFileStoreFile> filelist = this.getResult();
										ListStruct ffilelist = new ListStruct();
										
										for (IFileStoreFile f : filelist)
											ffilelist.addItem(f);
									
										eventdata.withField("Files", ffilelist);
										
										EventUtil.triggerEvent("ManagedForm-" + rec.getFieldAsString("Form") + "-Submit", null, eventdata);
										
										request.returnValue(token);
									}
								});
								*/
								
								request.returnValue(token);
							}
						});
					}
				});
			}
		});
		
		return;
	}
	
	public void handleManagedFormComplete(TaskRun request) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		
		String form = rec.getFieldAsString("Form");
		boolean formfnd = false;
		
		XElement mforms = OperationContext.get().getSite().getSettings().find("ManagedForms");
		
		if (mforms == null) {
			request.error("ManagedForms not enabled.");
			request.returnEmpty();
			return;
		}
		
		for (XElement mf : mforms.selectAll("ManagedForm")) {
			if (form.equals(mf.getAttribute("Name"))) 
				formfnd = true;
		}
		
		if (! formfnd) {
			request.error("Requested Managed Form not enabled.");
			request.returnEmpty();
			return;
		}
		
		Bucket mfbucket = OperationContext.get().getSite().getBucket("ManagedForm");
		
		mfbucket.getFileStore().getFileDetail(new CommonPath("/" + rec.getFieldAsString("Token")), 
			new FuncCallback<IFileStoreFile>() {
				@Override
				public void callback() {
					if (! this.hasErrors()) {
						EventUtil.triggerEvent("ManagedForm-" + rec.getFieldAsString("Form") + "-Submit", null, new RecordStruct()
								.withField("DataPath", this.getResult())
								.withField("Token", rec.getFieldAsString("Token"))
						);
					}
					
					request.returnEmpty();
				}
			});
		
		return;
	}
	
	/*
	public void handleListPages(TaskRun request) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		
		String channel = "Pages";
		String path = rec.getFieldAsString("Path");
		String site = rec.getFieldAsString("Site");	
		
		// link to fake so we can use FeedInfo to locate the folders
		FeedInfo fi = FeedInfo.buildInfo(site, channel, path + "/_fake.dcf.xml");
		
		Path pubpath = fi.getPubpath().getParent();
		Path prepath = fi.getPrepath().getParent();
		
		// hash so we never dup entries - but folder and dcf file can both have entry with same name
		Map<String, RecordStruct> collected = new HashMap<>();

		// code to list files
		BiConsumer<Path, Boolean> listing = new BiConsumer<Path, Boolean>() {			
			@Override
			public void accept(Path apath, Boolean preview) {
				Path wwwsrc1 = apath.toAbsolutePath().normalize();
				
				if (!Files.exists(wwwsrc1)) 
					return;
				
				try {
					Files.list(wwwsrc1).forEach(new Consumer<Path>() {
						@Override
						public void accept(Path sfile) {
							Path relpath = wwwsrc1.relativize(sfile);
							
							String fname = relpath.getFileName().toString();

							// only collect dcf files and folders
							boolean isdcf = fname.endsWith(".dcf.xml");
							boolean isfolder = Files.isDirectory(sfile);
							
							if (!isdcf && !isfolder) 
								return;
							
							if (isdcf)
								fname = fname.substring(0, fname.length() - 8);
								
							try {
								RecordStruct fdata = new RecordStruct();
								
								fdata.setField("FileName", fname);
								fdata.setField("IsFolder", isfolder);
								fdata.setField("IsPreview", preview);
								fdata.setField("LastModified", new DateTime(Files.getLastModifiedTime(sfile).toMillis(), DateTimeZone.UTC));
								fdata.setField("Size", Files.size(sfile));
								
								String ename = (isfolder ? "d_" : "f_") + fname;
								
								if (!collected.containsKey(ename))
									collected.put(ename, fdata);
							}
							catch (IOException x) {
								Logger.error("Error collecting file: " + fname + " : " + x);
							}
						}					
					});
				}
				catch (IOException x) {
					Logger.error("Error collecting files: " + path + " : " + x);
				}
			}
		};
				
		listing.accept(prepath, true);
		listing.accept(pubpath, false);
		
		ListStruct files = new ListStruct();
		
		for (RecordStruct s : collected.values())
			files.addItem(s);
		
		request.setResult(files);
		
		request.complete();
	}
	
	public void handleAddPageFolder(TaskRun request) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		
		String channel = "Pages";
		String path = rec.getFieldAsString("Path");
		String site = rec.getFieldAsString("Site");	
		
		// link to fake so we can use FeedInfo to locate the folders
		FeedInfo fi = FeedInfo.buildInfo(site, channel, path + "/_fake.dcf.xml");
		
		Path pubpath = fi.getPubpath().getParent();
		Path prepath = fi.getPrepath().getParent();

		try {
			Files.createDirectories(prepath);
			Files.createDirectories(pubpath);
		}
		catch (IOException x) {
			Logger.error("Error collecting files: " + path + " : " + x);
		}
		
		request.complete();
	}
	
	public void handleFeedLoadList(TaskRun request) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String channel = rec.getFieldAsString("Channel");
		
		// TODO add site support
		
		Hub.instance.getDatabase().submit(
				new SelectDirectRequest() 
					.withTable("dcmFeed")
					.withSelect(new SelectFields()
						.withField("Id")
						.withField("dcmPath", "Path")
						.withSubField("dcmFields", "Published.en", "Published")
						.withSubField("dcmFields", "Title.en", "Title")
						.withSubField("dcmFields", "Image.en", "Image")
						.withSubField("dcmFields", "Description.en", "Description")
				)
				.withCollector(
					new CollectorField("dcmChannel")
						.withValues(channel)
				),
				new ObjectFinalResult(request));
	}
	
	public void handleAddFeedFiles(TaskRun request) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		
		FeedInfo fi = FeedInfo.recordToInfo(rec);

		fi.initDraftFile(rec.getFieldAsString("Locale"), rec.getFieldAsString("Title"), null, new FuncCallback<CompositeStruct>() {
			@Override
			public void callback() {
				//request.setResult(this.getResult());
				request.complete();
			}
		});
	}
	
	public void handleAddPageFiles(TaskRun request) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		
		String channel = "Pages";
		String path = rec.getFieldAsString("Path");
		String site = rec.getFieldAsString("Site");	
		String tname = rec.getFieldAsString("Template");	
		
		FeedInfo fi = FeedInfo.buildInfo(site, channel, path);
		
		String dcui = null;
		
		XElement chel = FeedIndexer.findChannel(site, channel);
		
		for (XElement tel : chel.selectAll("Template")) {
			if (tname.equals(tel.getAttribute("Name"))) {
				XElement ctel = (XElement) tel.deepCopy();				
				ctel.setName("dcui");
				dcui = ctel.toString(true);
				break;
			}
		}
		
		SiteInfo si = OperationContext.get().getSite();
		
		Path tpath = si.resolvePath("config/templates/" + tname + ".dcui.xml");
		
		if (Files.exists(tpath)) 
			dcui = IOUtil.readEntireFile(tpath).getResult().toString();
		
		fi.initDraftFile(rec.getFieldAsString("Locale"), rec.getFieldAsString("Title"), dcui, new FuncCallback<CompositeStruct>() {
			@Override
			public void callback() {
				//request.setResult(this.getResult());
				request.complete();
			}
		});
	}
	
	public void handleUpdateFeedFiles(TaskRun request) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		
		FeedInfo fi = FeedInfo.recordToInfo(rec);
		
		fi.saveFile(true, rec.getFieldAsXml("ContentXml"), rec.getFieldAsList("UpdateFiles"), rec.getFieldAsList("DeleteFiles"), new FuncCallback<CompositeStruct>() {
			@Override
			public void callback() {
				request.complete();
			}
		});
	}
	
	public void handleUpdatePublishFeedFiles(TaskRun request) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		
		FeedInfo fi = FeedInfo.recordToInfo(rec);
		
		fi.saveFile(false, rec.getFieldAsXml("ContentXml"), rec.getFieldAsList("UpdateFiles"), rec.getFieldAsList("DeleteFiles"), new FuncCallback<CompositeStruct>() {
			@Override
			public void callback() {
				request.complete();
			}
		});
	}
	
	public void handleLoadFeedsDefinitions(TaskRun request) {
		SiteInfo site = OperationContext.get().getSite();
		
		XElement feed = site.getSettings().find("Feed");	
		
		if (feed == null) {
			request.error("Feed definition does not exist.");
			request.complete();
			return;
		}
		
		RecordStruct resp = new RecordStruct()
			.withField("FeedsXml", feed);
		 
		request.returnValue(resp);
	}
	*/
		
	public void handleLoadFeedInfo(TaskRun request) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		
		FeedInfo fi = FeedInfo.recordToInfo(rec);
		
		request.returnValue(fi.getDetails());
	}

	public void handleAlterFeedInfo(TaskRun request) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		
		FeedInfo fi = FeedInfo.recordToInfo(rec);
		
		fi.saveFile(rec.getFieldAsBooleanOrFalse("Publish"), rec.getFieldAsList("SetFields"), rec.getFieldAsList("SetParts"), rec.getFieldAsList("SetTags"), new FuncCallback<CompositeStruct>() {
			@Override
			public void callback() {
				request.complete();
			}
		});
	}
	
	public void handleDeleteFeedInfo(TaskRun request) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		
		FeedInfo fi = FeedInfo.recordToInfo(rec);
	
		// delete pub and draft
		fi.deleteFile(DeleteMode.Both, new OperationCallback() {			
			@Override
			public void callback() {
				request.complete();
			}
		});
	}	
	
	public void handleLoadFeedPart(TaskRun request) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		
		FeedInfo fi = FeedInfo.recordToInfo(rec);
		
		String part = rec.getFieldAsString("Part");

		FeedAdapter fa = fi.getAdapter(true);
		
		FeedPartMatchResult partm = fa.bestMatch("PagePart", "For", part);
		
		if (partm == null) {
			request.error("Feed part " + part + " does not exist.");
			request.returnEmpty();
			return;
		}
		
		// TODO locale support
		String locale = partm.locale.getName();
		
		String src = fa.getPartValue(locale, partm.el, true);
		
		if (src == null) 
			src = "";
		
		request.returnValue(new RecordStruct()
				.withField("Name", part)
				.withField("Locale", locale)
				.withField("Format", partm.el.getAttribute("Format"))
				.withField("Value", src)
		);
	}
	
	public void handleAlterFeedPart(TaskRun request) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		RecordStruct partinfo = rec.getFieldAsRecord("Part");
		RecordStruct data = rec.getFieldAsRecord("Data");
		data.renameField("Name", "For");
		
		FeedInfo fi = FeedInfo.recordToInfo(partinfo);
		
		// TODO does not yet support change of Format or Locale? (Name cannot change either)
		
		fi.saveFile(rec.getFieldAsBooleanOrFalse("Publish"), 
				new ListStruct(), 
				new ListStruct().withItems(data), 
				null, 
				new FuncCallback<CompositeStruct>() {
					@Override
					public void callback() {
						request.returnEmpty();
					}
				});
	}
		
	public void handleLoadFeedSection(TaskRun request) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		
		FeedInfo fi = FeedInfo.recordToInfo(rec);
		
		String part = rec.getFieldAsString("Part");
		String section = rec.getFieldAsString("Section");
		
		Sections secs = Sections.parse(fi, part);
		
		if (secs != null) {
			Section sec = secs.findSection(section);
			
			if (sec != null) {
				request.returnValue(new RecordStruct()
						.withField("Id", section)
						.withField("Plugin", sec.getPlugin())
						.withField("Params", sec.getAttrs())
						.withField("Content", sec.getContent())
				);
				return;
			}
		}

		request.error("Section not found or is incomplete.");
		request.returnEmpty();
	}
	
	/*
	 * Operations:
	 * 		Edit, InsertAbove, InsertBelow, InsertBottom, Delete, MoveUp, MoveDown
	 * 
	 * Part def:
	 * 		{  Site: "nnn", Channel: "nnn", Path: "nnn", Part: "nnn" }
	 * 
	 * Section def:
	 * 		{  Id: 'nnn', Content: 'nnn', Plugin: "nnn", Params: (any record) }
	 * 
	 * Edit Request
	 * 		{
	 * 			Part: (part def),
	 * 			Section: (section def),
	 * 			Action: {
	 * 				Op: 'Edit',
	 * 				Publish: t/f
	 * 			}
	 * 		}
	 * 
	 * InsertAbove Request
	 * 		{
	 * 			Part: (part def),
	 * 			Section: (section def),
	 * 			Action: {
	 * 				Op: 'InsertAbove',
	 * 				TargetSection: (id),
	 * 				Publish: t/f
	 * 			}
	 * 		}
	 * 
	 * InsertBelow Request
	 * 		{
	 * 			Part: (part def),
	 * 			Section: (section def),
	 * 			Action: {
	 * 				Op: 'InsertBelow',
	 * 				TargetSection: (id),
	 * 				Publish: t/f
	 * 			}
	 * 		}
	 * 
	 * InsertBottom Request
	 * 		{
	 * 			Part: (part def),
	 * 			Section: (section def),
	 * 			Action: {
	 * 				Op: 'InsertBottom',
	 * 				Publish: t/f
	 * 			}
	 * 		}
	 * 
	 * Delete Request
	 * 		{
	 * 			Part: (part def),
	 * 			Action: {
	 * 				Op: 'Delete',
	 * 				TargetSection: (id),
	 * 				Publish: t/f
	 * 			}
	 * 		}
	 * 
	 * MoveUp Request
	 * 		{
	 * 			Part: (part def),
	 * 			Action: {
	 * 				Op: 'MoveUp',
	 * 				TargetSection: (id),
	 * 				Levels: (defaults to 1),
	 * 				Publish: t/f
	 * 			}
	 * 		}
	 * 
	 * MoveDown Request
	 * 		{
	 * 			Part: (part def),
	 * 			Action: {
	 * 				Op: 'MoveDown',
	 * 				TargetSection: (id),
	 * 				Levels: (defaults to 1),
	 * 				Publish: t/f
	 * 			}
	 * 		}
	 * 
	 * 
	 */
	public void handleAlterFeedSection(TaskRun request) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		RecordStruct partinfo = rec.getFieldAsRecord("Part");
		
		FeedInfo fi = FeedInfo.recordToInfo(partinfo);
		
		String part = partinfo.getFieldAsString("Part");
		
		Sections secs = Sections.parse(fi, part);
		
		RecordStruct actinfo = rec.getFieldAsRecord("Action");
		
		String op = actinfo.getFieldAsString("Op");
		
		FuncCallback<CompositeStruct> cb = new FuncCallback<CompositeStruct>() {
			@Override
			public void callback() {
				request.returnEmpty();
			}
		};
		
		boolean publish = actinfo.getFieldAsBooleanOrFalse("Publish");
		
		if ("Edit".equals(op)) {
			RecordStruct sectinfo = rec.getFieldAsRecord("Section");
			
			String section = sectinfo.getFieldAsString("Id");
			
			Section sec = secs.findSection(section);
			
			if (sec == null) {
				request.error("Feed part " + part + " does not contain " + section);
				request.complete();
				return;
			}
			
			sec.update(sectinfo);
			
			secs.save(publish, cb);
		}		
		else if ("Delete".equals(op)) {
			if (secs.deleteSection(actinfo.getFieldAsString("TargetSection"))) 
				secs.save(publish, cb);
		}
		else if ("InsertAbove".equals(op)) {
			Section sec = Section.parseRecord(rec.getFieldAsRecord("Section"));
			
			if (secs.insertAbove(actinfo.getFieldAsString("TargetSection"), sec)) 
				secs.save(publish, cb);
			else
				request.complete();
		}		
		else if ("InsertBelow".equals(op)) {
			Section sec = Section.parseRecord(rec.getFieldAsRecord("Section"));
			
			if (secs.insertBelow(actinfo.getFieldAsString("TargetSection"), sec)) 
				secs.save(publish, cb);
			else
				request.complete();
		}		
		else if ("InsertBottom".equals(op)) {
			Section sec = Section.parseRecord(rec.getFieldAsRecord("Section"));
			
			if (secs.insertBottom(sec)) 
				secs.save(publish, cb);
			else
				request.complete();
		}		
		else if ("MoveUp".equals(op)) {
			if (secs.moveUp(actinfo.getFieldAsString("TargetSection"), actinfo.getFieldAsInteger("Levels")))
				secs.save(publish, cb);
			else
				request.complete();
		}		
		else if ("MoveDown".equals(op)) {
			if (secs.moveDown(actinfo.getFieldAsString("TargetSection"), actinfo.getFieldAsInteger("Levels")))
				secs.save(publish, cb);
			else
				request.complete();
		}		
		else {
			request.error("Unknown alter section operation: " + op);
			request.complete();
		}
	}
	
	/*
	public void handlePublishFeedFiles(TaskRun request) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		
		FeedInfo fi = FeedInfo.recordToInfo(rec);
		
		fi.publicizeFile(new FuncCallback<CompositeStruct>() {
			@Override
			public void callback() {
				request.complete();
			}
		});
	}
	
	public void handleImportFeedFiles(TaskRun request) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		
		FeedInfo fi = FeedInfo.recordToInfo(rec);
		
		fi.updateDb(new OperationCallback() {			
			@Override
			public void callback() {
				request.complete();
			}
		});
	}
	*/
		
	/* TODO general cleanup
	public void handleSiteBuildMap(TaskRun request) {
		SiteInfo site = OperationContext.get().getSite();
		
		XElement dsel = site.getWebsite().getWebConfig();
		
		XElement wsel = dsel.find("Web");
		
		if (wsel == null) {
			request.warn("Missing Web config");
			request.complete();
			return;
		}

		Consumer<XElement> consumer = new Consumer<XElement>() {			
			@Override
			public void accept(XElement wsel) {
				String indexurl = wsel.getAttribute("IndexUrl");
				
				if (StringUtil.isEmpty(indexurl)) {
					request.warn("Missing IndexUrl");
					request.complete();
					return;
				}
				
				List<String> altlocales = new ArrayList<String>();
				
				for (XElement locel : wsel.selectAll("Locale"))
					altlocales.add(locel.getAttribute("Name"));

				Path webdir = site.resolvePath("www");
				
				XElement smel = new XElement("urlset")
					.withAttribute("xmlns", "http://www.sitemaps.org/schemas/sitemap/0.9")
					.withAttribute("xmlns:xhtml", "http://www.w3.org/1999/xhtml");
				
				DateTimeFormatter lmFmt = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
				
		        try {
		        	if (Files.exists(webdir)) { 
						Files.walkFileTree(webdir, EnumSet.of(FileVisitOption.FOLLOW_LINKS), 5,
						        new SimpleFileVisitor<Path>() {
						            @Override
						            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
						                throws IOException
						            {
										Path relpath = webdir.relativize(file);
										
										String outerpath = "/" + relpath.toString().replace('\\', '/');
						            	
						            	if (!outerpath.endsWith(".dcui.xml")) 
							                return FileVisitResult.CONTINUE;
										
										outerpath = outerpath.substring(0, outerpath.length() - 9);
						            	
										FuncResult<XElement> xres = XmlReader.loadFile(file, true);
										
										if (xres.hasErrors()) 
							                return FileVisitResult.CONTINUE;
										
										XElement root = xres.getResult();
										
										if (!root.getName().equals("dcui"))
							                return FileVisitResult.CONTINUE;
										
										if (!root.getAttribute("AuthTags", "Guest").contains("Guest"))
							                return FileVisitResult.CONTINUE;
										
										if ("True".equals(root.getAttribute("NoIndex")))
							                return FileVisitResult.CONTINUE;
					            		
					            		// TODO look for an indexing script in the page
					            		// TODO look for a gallery list in the page
				            		
					            		XElement sel = new XElement("url");
					            		
					            		sel.add(new XElement("loc", indexurl + outerpath.substring(1)));
					            		sel.add(new XElement("lastmod", lmFmt.print(Files.getLastModifiedTime(file).toMillis())));

					    				for (String lname : altlocales)
					    					sel.add(new XElement("xhtml:link")
					    						.withAttribute("rel", "alternate")
					    						.withAttribute("hreflang", lname)
					    						.withAttribute("href", indexurl + lname + outerpath)
					    					);
					            		
					            		smel.add(sel);
					            					            		
						                return FileVisitResult.CONTINUE;
						            }
						        });
		        	}
		        	
					FeedIndexer iutil = new FeedIndexer();
					
					iutil.collectSite(new CollectContext().forIndex(), wsel.getAttribute("Name"));
					
					iutil.addToSitemap(indexurl, smel, altlocales);
		        	
		        	Path smfile = webdir.resolve("sitemap.xml");
		        	
		        	IOUtil.saveEntireFile2(smfile, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
		        			+ smel.toString(true));
		            
		            //System.out.println("map: " + smel.toString(true));
				} 
		        catch (IOException x) {
		        	request.error("Error building sitemap file: " + x);
				}
			}
		};

		boolean rootfnd = false;
		
		for (XElement site : wsel.selectAll("Site")) {
			if ("root".equals(site.getAttribute("Name"))) {
				rootfnd = true;
				site.withAttribute("IndexUrl", wsel.getAttribute("IndexUrl"));
			}
			
			consumer.accept(site);
		}
		
		if (!rootfnd)
			consumer.accept(new XElement("Site").withAttribute("Name", "root").withAttribute("IndexUrl", wsel.getAttribute("IndexUrl")));
		
		request.complete();
	}
		*/
		
	/******************************************************************
	 * Tenant Files
	 ******************************************************************/

	// TODO
	/*
	public void handleImportUIFile(TaskRun request, FileSystemDriver fs, CommonPath sectionpath) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String fpath = rec.getFieldAsString("FilePath");
		
		CommonPath path = sectionpath.resolve(fpath);
		
		fs.getFileDetail(path, new FuncCallback<IFileStoreFile>() {			
			@Override
			public void callback() {
				if (request.hasErrors()) {
					request.complete();
					return;
				}
				
				IFileStoreFile fi = this.getResult();
				
				if (!fi.exists()) {
					request.error("File does not exist");
					request.complete();
					return;
				}
				
				fi.readAllText(new FuncCallback<String>() {					
					@Override
					public void callback() {
						if (this.hasErrors()) {
							request.error("Unable to read file");
							request.complete();
							return;
						}
						
						String text = this.getResult();

						FuncResult<XElement> xres = XmlReader.parse(text, true);
						
						if (xres.hasErrors()) {
							System.out.println("Error parsing file: " + xres.getMessages());
							request.complete();
							return;
						}
						
						XElement root = xres.getResult();
						
						String spath = path.subpath(3).toString();						// remove the www 
						
						if (fpath.endsWith(".dcuis.xml")) {
							String fspath = spath.substring(0, spath.length() - 10);		// remove the extension
							
							System.out.println("Importing skeleton: " + root.getAttribute("Title") + " " + fspath);
							
							InsertRecordRequest req = new InsertRecordRequest();
							
							req
								.withTable("dcmSkeleton")		
								.withSetField("dcmTitle", root.getAttribute("Title"))
								.withSetField("dcmPath", fspath);
							
							Hub.instance.getDatabase().submit(req, new ObjectFinalResult(request));
						}
						else if (root.getName().equals("dcuip")) {
							// TODO deal with block
							
							request.returnValue(5);		// TODO page id
						}
						else {
							// TODO check for ReqLib, ReqStyle, Function - these cannot be imported so return an error
							String fspath = spath.substring(0, spath.length() - 9);		// remove the extension
							
							System.out.println("Importing page: " + root.getAttribute("Title") + " " + root.getAttribute("Skeleton"));

							// only support external skeletons
							if (!root.hasAttribute("Skeleton")) {
								request.error("Missing skeleton path, skeleton must be external");
								request.complete();
								return;
							}

							String tpath = root.getAttribute("Skeleton");
							
							Hub.instance.getDatabase().submit(
								new SelectDirectRequest()
									.withTable("dcmSkeleton") 
									.withSelect(new SelectFields().withField("Id"))
									.withWhere(new WhereEqual(new WhereField("dcmPath"), tpath)), 
								new ObjectResult() {
									@Override
									public void process(CompositeStruct result) {
										if (this.hasErrors()) {
											request.complete();
											return;
										}
										
										if (result == null) {
											request.error("Search for skeleton failed");
											request.complete();
											return;
										}
										
										FuncCallback<String> processPage = new FuncCallback<String>() {
											@Override
											public void callback() {
												if (this.isEmptyResult()) {
													request.error("Skeleton id cannot be estalished");
													request.complete();
													return;
												}
												
												String sid = this.getResult();
												
												InsertRecordRequest req = new InsertRecordRequest();
												
												req
													.withTable("dcmPage")		
													.withSetField("dcmTitle", root.getAttribute("Title"))
													.withSetField("dcmPath", fspath)
													.withSetField("dcmSkeleton", sid)
													.withSetField("dcmAuthor", OperationContext.get().getUserContext().getUserId())
													.withSetField("dcmCreated", new DateTime())
													.withSetField("dcmModified", new DateTime());
												
												XElement keywords = root.find("Keywords");
												
												if ((keywords != null) && keywords.hasText())
													req.withSetField("dcmKeywords", keywords.getText());
												
												XElement desc = root.find("Description");
												
												if ((desc != null) && desc.hasText())
													req.withSetField("dcmDescription", desc.getText());
												
												for (XElement part : root.selectAll("PagePart")) {
													String content = part.getText();
													
													String locale = part.getAttribute("Locale", "default");
													String forid = part.getAttribute("For", "main-content");
													
													String subkey = forid + "." + locale;
													
													req.withSetField("dcmPartContent", subkey, content);
													
													RecordStruct pattrs = new RecordStruct();
													
													for (Entry<String, String> attr : part.getAttributes().entrySet()) 
														pattrs.setField(attr.getKey(), attr.getValue());
													
													req.withSetField("dcmPartAttributes", subkey, pattrs);
												}
												
												Hub.instance.getDatabase().submit(req, new ObjectFinalResult(request));
											}
										};
										
										ListStruct sklist = (ListStruct) result;
										
										if (sklist.getSize() ==  0) {
											InsertRecordRequest req = new InsertRecordRequest();
											
											req
												.withTable("dcmSkeleton")		
												.withSetField("dcmTitle", root.getAttribute("Title"))
												.withSetField("dcmPath", root.getAttribute("Skeleton"));
											
											Hub.instance.getDatabase().submit(req, new ObjectResult() {
												@Override
												public void process(CompositeStruct result) {
													if (this.isNotEmptyResult())
														processPage.setResult(this.getResultAsRec().getFieldAsString("Id"));
													
													processPage.complete();
												}
											});
										}
										else {
											processPage.setResult(sklist.getItemAsRecord(0).getFieldAsString("Id"));
											processPage.complete();
										}
									}
								});
						}
					}
				});
			}
		});
	}
	*/
}
