package dcraft.cms.feed.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import dcraft.hub.SiteInfo;
import dcraft.lang.op.FuncCallback;
import dcraft.lang.op.FuncResult;
import dcraft.lang.op.OperationCallback;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.OperationResult;
import dcraft.struct.CompositeStruct;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.util.IOUtil;
import dcraft.util.StringUtil;
import dcraft.util.TimeUtil;
import dcraft.xml.XElement;
import dcraft.xml.XmlReader;

public class FeedInfo {
	public static FeedInfo recordToInfo(RecordStruct rec) {
		String channel = rec.getFieldAsString("Channel");
		String path = rec.getFieldAsString("Path");
		
		return FeedInfo.buildInfo(channel, path);
	}
	
	// path without channel
	public static FeedInfo buildInfo(String channel, String path) {
		if (StringUtil.isEmpty(channel) || StringUtil.isEmpty(path))
			return null;
		
		XElement channelDef = FeedIndexer.findChannel(channel); 
		
		// if channelDef is null then it does not exist
		if (channelDef == null)
			return null;
		
		FeedInfo fi = new FeedInfo();
		
		fi.channel = channel;
		fi.path = path;
		fi.channelDef = channelDef;
		
		fi.init();
		
		return fi;
	}
	
	protected String channel = null;
	protected String path = null;
	
	protected XElement channelDef = null;
	protected XElement draftDcfContent = null;
	protected XElement pubDcfContent = null;
	protected Path pubpath = null;
	protected Path prepath = null;

	// for this work correctly you need to set channel and path first
	public void init() {
		if (this.channelDef == null) 
			return;
		
		SiteInfo site = OperationContext.get().getSite();
		
		//this.innerpath = "/" + site.getAlias() + this.feedpath;		
		
		this.prepath = site.resolvePath("feed-preview/" + this.channel + this.path + ".dcf.xml").toAbsolutePath().normalize();
		this.pubpath = site.resolvePath("feed/" + this.channel + this.path + ".dcf.xml").toAbsolutePath().normalize();
	}

	public List<String> collectExternalFileNames(boolean draft) {
		ArrayList<String> list = new ArrayList<String>();
		
		// load the feed file
		XElement dcf = draft ? this.getDraftDcfContent() : this.getPubDcfContent();
		
		if (dcf == null)
			return list;
		
		// the Site default locale
		String deflocale = dcf.getAttribute("Locale", OperationContext.get().getSite().getDefaultLocale());
		
		// check for external parts and move them
		for (XElement fel : dcf.selectAll("PagePart")) {
			if (! fel.getAttributeAsBooleanOrFalse("External"))
				continue;
			
			// use the override locale if present
			String locale = fel.getAttribute("Locale", deflocale);	
		
			String sname = (draft ? FeedInfo.this.prepath : FeedInfo.this.pubpath).getFileName().toString();
			
			int pos = sname.indexOf('.');
			sname = sname.substring(0, pos) + "." + fel.getAttribute("For") + "." + locale + "." + fel.getAttribute("Format");

			list.add(sname);
		}
		
		return list;
	}
	
	public String getChannel() {
		return this.channel;
	}
	
	public String getUrlPath() {
		if ("Pages".equals(this.channel)) 
			return this.path;
		
		// TODO if this channel has an alternative Url path, lookup here
		
		return "/" + this.channel + this.path;
	}
	
	public XElement getChannelDef() {
		return this.channelDef;
	}
	
	public XElement getDraftDcfContent() {
		if (this.draftDcfContent == null) {
			if (Files.exists(this.prepath)) {
				FuncResult<XElement> res = XmlReader.loadFile(this.prepath, false);
				this.draftDcfContent = res.getResult();
			}
		}
		
		return this.draftDcfContent;
	}
	
	public XElement getPubDcfContent() {
		if (this.pubDcfContent == null) {
			if (Files.exists(this.pubpath)) {
				FuncResult<XElement> res = XmlReader.loadFile(this.pubpath, false);
				this.pubDcfContent = res.getResult();
			}
		}
		
		return this.pubDcfContent;
	}
	
	public Path getPubpath() {
		return this.pubpath;
	}
	
	public Path getPrepath() {
		return this.prepath;
	}
	
	public boolean exists() {
		return Files.exists(this.pubpath) || Files.exists(this.prepath);
	}

	public RecordStruct getDetails() {
		// work through the adapters
		FeedAdapter pubfeed = this.getPubAdapter();
		FeedAdapter prefeed = this.getPreAdapter();
		
		if ((pubfeed == null) && (prefeed == null)) 
			return null;
		
		XElement pubxml = (pubfeed != null) ? pubfeed.getXml() : null;
		XElement prexml = (prefeed != null) ? prefeed.getXml() : null;

		// if no file is present then delete record for feed
		if ((pubxml == null) && (prexml == null)) 
			return null;
		
		// if at least one xml file then update/add a record for the feed
		
		RecordStruct feed = new RecordStruct()
			.withField("Site", OperationContext.get().getSite().getAlias())
			.withField("Channel", this.channel)
			.withField("Path", this.path);
		
		// the "edit" authorization, not the "view" auth
		String authtags = (pubfeed != null) ? pubfeed.getAttribute("AuthTags") : prefeed.getAttribute("AuthTags");
		
		if (StringUtil.isEmpty(authtags))
			feed.withField("AuthorizationTags", new ListStruct());
		else
			feed.withField("AuthorizationTags", new ListStruct((Object[]) authtags.split(",")));

		if (pubxml != null) {
			ListStruct ctags = new ListStruct();
			
			for (XElement tag : pubxml.selectAll("Tag")) {
				String alias = tag.getAttribute("Alias");
				
				if (StringUtil.isNotEmpty(alias))
					ctags.addItem(alias);
			}
			
			feed.withField("ContentTags", ctags);
		}
		else if (prexml != null) {
			ListStruct ctags = new ListStruct();
			
			for (XElement tag : prexml.selectAll("Tag")) {
				String alias = tag.getAttribute("Alias");
				
				if (StringUtil.isNotEmpty(alias))
					ctags.addItem(alias);
			}
			
			feed.withField("ContentTags", ctags);
		}
		
		if (pubxml != null) {
			// public fields
			
			String primelocale = pubxml.getAttribute("Locale"); 
			
			if (StringUtil.isEmpty(primelocale))
				primelocale = OperationContext.get().getWorkingLocaleDefinition().getName();
			
			feed.withField("Locale", primelocale);
	
			ListStruct pubfields = new ListStruct();
			feed.withField("Fields", pubfields);
			
			for (XElement fld : pubxml.selectAll("Field")) 
				pubfields.addItem(new RecordStruct()
					.withField("Name", fld.getAttribute("Name"))
					.withField("Locale", fld.getAttribute("Locale", primelocale))		// prime locale can be override for field, though it means little besides adding to search info
					.withField("Value", fld.getValue())
				);
			
			ListStruct pubparts = new ListStruct();
			feed.withField("PartContent", pubparts);
			
			for (XElement fld : pubxml.selectAll("PagePart"))
				pubparts.addItem(new RecordStruct()
					.withField("Name", fld.getAttribute("For"))
					.withField("Format", fld.getAttribute("Format", "md"))
					.withField("Locale", fld.getAttribute("Locale", primelocale))		// prime locale can be override for specific part
				);
		}
		
		if (prexml != null) {
			// preview fields
			
			String primelocale = prexml.getAttribute("Locale"); 
			
			if (StringUtil.isEmpty(primelocale))
				primelocale = OperationContext.get().getWorkingLocaleDefinition().getName();

			if (! feed.hasField("Locale"))
				feed.withField("Locale", primelocale);

			ListStruct prefields = new ListStruct();
			feed.withField("PreviewFields", prefields);
			
			for (XElement fld : prexml.selectAll("Field")) 
				prefields.addItem(new RecordStruct()
					.withField("Name", fld.getAttribute("Name"))
					.withField("Locale", fld.getAttribute("Locale", primelocale))		// prime locale can be override for field, though it means little besides adding to search info
					.withField("Value", fld.getValue())
				);
			
			ListStruct preparts = new ListStruct();
			feed.withField("PreviewPartContent", preparts);
			
			for (XElement fld : prexml.selectAll("PagePart")) 
				preparts.addItem(new RecordStruct()
					.withField("Name", fld.getAttribute("For"))
					.withField("Format", fld.getAttribute("Format", "md"))
					.withField("Locale", fld.getAttribute("Locale", primelocale))		// prime locale can be override for specific part
				);
		}
		
		return feed;
	}

	public FeedAdapter getPreAdapter() {
		OperationResult op = new OperationResult();
		
		FeedAdapter adapt = new FeedAdapter();
		adapt.init(this.channel, this.path, this.prepath);		// load direct, not cache - cache may not have updated yet
		adapt.validate();
		
		// if an error occurred during the init or validate, don't use the feed
		if (op.hasErrors())
			return null;
		
		return adapt;
	}

	public FeedAdapter getPubAdapter() {
		OperationResult op = new OperationResult();
		
		FeedAdapter adapt = new FeedAdapter();
		adapt.init(this.channel, this.path, this.pubpath);		// load direct, not cache - cache may not have updated yet
		adapt.validate();
		
		// if an error occurred during the init or validate, don't use the feed
		if (op.hasErrors())
			return null;
		
		return adapt;
	}
	
	public FeedAdapter getAdapter(boolean draft) {
		if (draft) {
			FeedAdapter adpt = this.getPreAdapter();
			
			if ((adpt != null) && (adpt.getXml() != null))
				return adpt;
		}
		
		FeedAdapter adpt = this.getPubAdapter();
		
		if ((adpt != null) && (adpt.getXml() != null))
			return adpt;
		
		return null;
	}
	
	// this is not required, you may go right to saveDraftFile
	// dcui = only if a Page
	/*
	public void initDraftFile(String locale, String title, String dcui, FuncCallback<CompositeStruct> op) {
		if (StringUtil.isEmpty(locale))
			locale = OperationContext.get().getSite().getDefaultLocale();		
		
		if ("Pages".equals(this.getChannel()) && StringUtil.isNotEmpty(dcui)) {
			SiteInfo site = OperationContext.get().getSite();
			
			// don't go to www-preview at first, www-preview would only be used by a developer showing an altered page
			// for first time save, it makes sense to have the dcui file in www
			Path uisrcpath = site.resolvePath("www" + this.path + ".html");		
			
			try {
				Files.createDirectories(uisrcpath.getParent());
				IOUtil.saveEntireFile(uisrcpath, dcui);
			}
			catch (Exception x) {
				op.error("Unable to add dcui file: " + x);
				op.complete();
				return;
			}
		}

		try {
			Files.createDirectories(this.getPrepath().getParent());
		}
		catch (Exception x) {
			op.error("Unable to create draft folder: " + x);
			op.complete();
			return;
		}
		
		XElement root = this.getFeedTemplate(title, locale);
		
		// TODO clear the CacheFile index for this path so that we get up to date entries when importing
		IOUtil.saveEntireFile(this.getPrepath(), root.toString(true));
		
		this.updateDb(op);
	}	
	*/
	
	protected XElement getFeedTemplate(String title, String locale) {
		return new XElement("dcf")
				.withAttribute("Locale", locale)
				.with(new XElement("Field")
					.withAttribute("Value", title)
					.withAttribute("Name", "Title")
				)
				.with(new XElement("Field")
					.withAttribute("Value", OperationContext.get().getUserContext().getFullName())
					.withAttribute("Name", "AuthorName")
				)
				.with(new XElement("Field")
					.withAttribute("Value", OperationContext.get().getUserContext().getUsername())
					.withAttribute("Name", "AuthorUsername")
				)
				.with(new XElement("Field")
					.withAttribute("Value", TimeUtil.stampFmt.print(new DateTime()))
					.withAttribute("Name", "Created")
				);
	}
	
	// dcf = content for the dcf file
	// updates = list of records (Name, Content) to write out
	// deletes = list of filenames to remove
	public void saveFile(boolean publish, ListStruct fields, ListStruct parts, ListStruct tags, FuncCallback<CompositeStruct> op) {
		OperationContext ctx = OperationContext.get();
		String locale = ctx.getWorkingLocale();
		
		if (fields == null)
			fields = new ListStruct();
		
		if (parts == null)
			parts = new ListStruct();
		
		Path savepath = this.getPrepath();
		
		try {
			Files.createDirectories(savepath.getParent());
		}
		catch (Exception x) {
			op.error("Unable to create draft folder: " + x);
			op.complete();
			return;
		}
		
		FeedAdapter ad = this.getPreAdapter();
		
		if (!ad.isFound())
			ad = this.getPubAdapter();
		
		if (!ad.isFound()) {
			//op.error("Unable to locate feed file, cannot alter.");
			//op.complete();
			//return;
			ad.xml = this.getFeedTemplate("[Unknown]", locale);			
		}
		
		// default published, can be overridden in the SetFields collection
		if (publish) {
			XElement mr = ad.getDefaultField("Published");
			
			if (mr == null) 
				ad.setField(ctx, locale, "Published", new LocalDate().toString());
		}
		
		for (Struct fs : fields.getItems()) {
			RecordStruct frec = (RecordStruct) fs;
			
			if (frec.hasField("Value"))
				ad.setField(ctx, locale, frec.getFieldAsString("Name"), frec.getFieldAsString("Value"));
			else
				ad.removeField(ctx, locale, frec.getFieldAsString("Name"));
		}
		
		for (Struct ps : parts.getItems()) {
			RecordStruct frec = (RecordStruct) ps;
			
			if (frec.hasField("Value"))
				ad.setPart(ctx, locale, frec.getFieldAsString("For"), frec.getFieldAsString("Format"), frec.getFieldAsString("Value"), ! publish);
			else
				ad.removePart(ctx, locale, frec.getFieldAsString("For"), ! publish);
		}
		
		if (tags != null) {
			ad.clearTags();
			
			for (Struct ts : tags.getItems()) 
				ad.addTag(ts.toString());
		}
		
		IOUtil.saveEntireFile(savepath, ad.getXml().toString(true));

		if (publish) 
			this.publicizeFile(op);
		else 
			this.updateDb(op);
	}
	
	// dcf = content for the dcf file
	// updates = list of records (Name, Content) to write out
	// deletes = list of filenames to remove
	public void saveFile(boolean draft, XElement dcf, ListStruct updates, ListStruct deletes, FuncCallback<CompositeStruct> op) {
		Path savepath = this.getPrepath();
		
		try {
			Files.createDirectories(savepath.getParent());
		}
		catch (Exception x) {
			op.error("Unable to create draft folder: " + x);
			op.complete();
			return;
		}
		
		String locale = dcf.getAttribute("Locale");
		
		if (StringUtil.isEmpty(locale))
			dcf.setAttribute("Locale", OperationContext.get().getSite().getDefaultLocale());		
		
		try {
			if (deletes != null)
				for (Struct df : deletes.getItems()) 
					// TODO clear the CacheFile index for this path so that we get up to date entries when importing
					Files.deleteIfExists(savepath.resolveSibling(df.toString()));
				
			if (updates != null)
				for (Struct uf : updates.getItems()) {
					RecordStruct urec = (RecordStruct) uf;
	
					// TODO clear the CacheFile index for this path so that we get up to date entries when importing
					IOUtil.saveEntireFile(savepath.resolveSibling(urec.getFieldAsString("Name")), urec.getFieldAsString("Content"));
				}
			
			if (dcf != null)
				// TODO clear the CacheFile index for this path so that we get up to date entries when importing
				IOUtil.saveEntireFile(savepath, dcf.toString(true));
			
			// cleanup any draft files, we skipped over them
			if (draft) 
				this.updateDb(op);
			else
				this.publicizeFile(op);
			
		}
		catch (Exception x) {
			op.error("Unable to update feed: " + x);
			op.complete();
		}
	}
	
	public void publicizeFile(FuncCallback<CompositeStruct> op) {
		// if no preview available then nothing we can do here
		if (Files.notExists(this.getPrepath())) {
			op.complete();
			return;
		}

		try {
			Files.createDirectories(this.getPubpath().getParent());
		}
		catch (Exception x) {
			op.error("Unable to create publish folder: " + x);
			op.complete();
			return;
		}
		
		List<String> filelist = this.collectExternalFileNames(true);
		
		// move all the external files
		for (String sname : filelist) {
			Path ypath = this.getPrepath().resolveSibling(sname);
			
			// don't bother if there is no preview file
			if (Files.notExists(ypath))
				continue;

			try {
				Files.move(ypath, this.getPubpath().resolveSibling(sname), StandardCopyOption.REPLACE_EXISTING);
				// TODO clear the CacheFile index for this path so that we get up to date entries when importing
			}
			catch (Exception x) {
				op.error("Unable to move preview file: " + ypath +  " : " + x);
			}
		}
		
		// finally move the feed file itself
		try {
			Files.move(this.getPrepath(), this.getPubpath(), StandardCopyOption.REPLACE_EXISTING);
			// TODO clear the CacheFile index for this path so that we get up to date entries when importing
			
			this.updateDb(op);
		}
		catch (Exception x) {
			op.error("Unable to move preview file: " + this.getPrepath() +  " : " + x);
			op.complete();
		}
	}

	public void deleteFile(DeleteMode mode, OperationCallback op) {
		for (int i = 0; i < 2; i++) {
			boolean draft = (i == 0);
			
			if (draft && (mode == DeleteMode.Published))
				continue;
			
			if (!draft && (mode == DeleteMode.Draft))
				continue;
			
			Path fpath = draft ? this.getPrepath() : this.getPubpath();
			
			// if no dcf file available then nothing we can do 
			if (Files.notExists(fpath)) 
				continue;
			
			List<String> filelist = this.collectExternalFileNames(draft);
			
			// move all the external files
			for (String sname : filelist) {
				Path ypath = fpath.resolveSibling(sname);
	
				try {
					Files.deleteIfExists(ypath);
				}
				catch (Exception x) {
					op.error("Unable to delete feed external file: " + ypath +  " : " + x);
				}
			}
			
			// finally move the feed file itself
			try {
				Files.deleteIfExists(fpath);
			}
			catch (Exception x) {
				op.error("Unable to delete feed file: " + fpath +  " : " + x);
			}
			
			// delete Page definitions...
			if ("Pages".equals(channel)) {
				SiteInfo siteinfo = OperationContext.get().getSite();
				
				Path srcpath = draft 
						? siteinfo.resolvePath("www-preview/" + this.path + ".dcui.xml")
						: siteinfo.resolvePath("www/" + this.path + ".dcui.xml");
				
				try {
					Files.deleteIfExists(srcpath);
				}
				catch  (Exception x) {
				}
			}			
		}
		
		this.deleteDb(op);
	}

	public void deleteDb(OperationCallback cb) {
		DataStore.deleteFeed("/" + OperationContext.get().getSite().getAlias() + "/" + this.channel + this.path, cb);
	}

	public void updateDb(OperationCallback cb) {
		RecordStruct feed = this.getDetails();

		if (feed == null) {
			this.deleteDb(cb);
			return;
		}
		
		DataStore.updateFeed(feed, cb);
	}
}
