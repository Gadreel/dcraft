package dcraft.cms.feed.core;

import java.nio.file.Files;
import java.nio.file.Path;

import dcraft.filestore.CommonPath;
import dcraft.io.CacheFile;
import dcraft.lang.op.FuncResult;
import dcraft.lang.op.OperationContext;
import dcraft.locale.ITranslationAdapter;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.util.IOUtil;
import dcraft.util.StringUtil;
import dcraft.xml.XElement;
import dcraft.xml.XmlReader;

public class FeedAdapter {
	static public FeedAdapter from(String channel, String path, boolean preview) {
		XElement chan = FeedIndexer.findChannel(channel);
		
		if (chan == null) 
			return null;
		
		CacheFile cfile = OperationContext.get().getSite().findSectionFile("feed", "/" + channel + path + ".dcf.xml", true);
		
		if (cfile == null)
			return null;
		
		FeedAdapter adapt = new FeedAdapter();
		
		adapt.init(channel, path, cfile.getFilePath());
		
		return adapt; 
	}
	
	static public FeedAdapter from(ITranslationAdapter ctx, RecordStruct data) {
		CommonPath cpath = new CommonPath(data.getFieldAsString("Path"));
		
		FeedAdapter adapt = new FeedAdapter();

		adapt.channel = cpath.getName(1);		// site alias is 0
		adapt.path = cpath.subpath(2).toString();
		adapt.xml = new XElement("dcf");
		
		String deflocale = ctx.getWorkingLocale();
		
		adapt.xml.withAttribute("Locale", deflocale);
		
		for (Struct fs : data.getFieldAsList("Fields").getItems()) {
			RecordStruct frec = (RecordStruct) fs;
			
			String sub = frec.getFieldAsString("SubId");
			String fdata = frec.getFieldAsString("Data");
			
			if (StringUtil.isEmpty(sub) || StringUtil.isEmpty(fdata))
				continue;
			
			int dpos = sub.lastIndexOf('.');
			
			adapt.setField(ctx, sub.substring(dpos + 1), sub.substring(0, dpos), fdata);
		}

		return adapt; 
	}

	/*
	System.out.println("data: " + v.toPrettyString());
	
data:  { 
"Path": "\/root\/Announcements\/test-announcement-one", 
"Fields":  [ 
	 { 
		"SubId": "AuthorName.en", 
		"Retired": null, 
		"Data": "[unknown]", 
		"From": null, 
		"To": null, 
		"Tags": null
	 } , 
	 { 
		"SubId": "AuthorUsername.en", 
		"Retired": null, 
		"Data": "root", 
		"From": null, 
		"To": null, 
		"Tags": null
	 } , 
	 { 
		"SubId": "Created.en", 
		"Retired": null, 
		"Data": "20161129T144808620Z", 
		"From": null, 
		"To": null, 
		"Tags": null
	 } , 
	 { 
		"SubId": "EndAt.en", 
		"Retired": null, 
		"Data": "2017-05-01", 
		"From": null, 
		"To": null, 
		"Tags": null
	 } , 
	 { 
		"SubId": "Published.en", 
		"Retired": null, 
		"Data": "20170501T050000000Z", 
		"From": null, 
		"To": null, 
		"Tags": null
	 } , 
	 { 
		"SubId": "Summary.en", 
		"Retired": null, 
		"Data": "Just a little bit about this announcement to let you know what it is all about and how to attend if it is an event and similar sorts of things that we may need to know but have a hard time recalling.", 
		"From": null, 
		"To": null, 
		"Tags": null
	 } , 
	 { 
		"SubId": "Title.en", 
		"Retired": null, 
		"Data": "Test Announcement One", 
		"From": null, 
		"To": null, 
		"Tags": null
	 } 
 ] , 
"Id": "00200_000000000000013"
} 		
	*/	
	protected String channel = null;
	protected String path = null;
	protected Path filepath = null;
	protected XElement xml = null;
	
	public String getChannel() {
		return this.channel;
	}
	
	public String getPath() {
		return this.path;
	}
	
	public Path getFilePath() {
		return this.filepath;
	}
	
	public XElement getXml() {
		return this.xml;
	}
	
	// best not to use CacheFile always, not if from feed save/feed delete
	public void init(String channel, String path, Path filepath) {
		if (filepath == null)
			return;
		
		this.channel = channel;
		this.path = path;
		this.filepath = filepath;
		
		if (Files.notExists(filepath))
			return;
		
		FuncResult<XElement> res = XmlReader.loadFile(filepath, false);
		
		if (res.hasErrors())
			OperationContext.get().error("Bad feed file - " + this.channel + " | " + path);
		
		this.xml = res.getResult();
	}
	
	public boolean isFound() {
		return (this.xml != null);
	}
	
	public void validate() {
		if (this.xml == null)
			return;
		
		String locale = this.getAttribute("Locale");
		
		if (StringUtil.isEmpty(locale))
			OperationContext.get().error("Missing Locale - " + this.channel + " | " + this.path);
		
		String title = this.getDefaultFieldValue("Title");
		
		if (StringUtil.isEmpty(title))
			OperationContext.get().error("Missing Title - " + this.channel + " | " + this.path);
		
		/*
		String desc = this.getDefaultField("Description");
		
		if (StringUtil.isEmpty(desc))
			OperationContext.get().warn("Missing Description - " + this.channel + " | " + filepath);
		
		String key = this.getDefaultField("Keywords");
		
		if (StringUtil.isEmpty(key))
			OperationContext.get().warn("Missing Keywords - " + this.channel + " | " + filepath);
		
		//String img = this.getField("Image");
		
		//if (StringUtil.isEmpty(img))
		//	OperationContext.get().warn("Missing Image - " + this.key + " | " + path);
		 * 
		 */
	}

	public String getAttribute(String name) {
		if ((this.xml == null) || StringUtil.isEmpty(name))
			return null;
		
		return this.xml.getAttribute(name);
	}
	
	public String getDefaultFieldValue(String name) {
		XElement fel = this.getDefaultField(name);

		return (fel != null) ? fel.getValue() : null;
	}
	
	public XElement getDefaultField(String name) {
		if ((this.xml == null) || StringUtil.isEmpty(name))
			return null;
		
		// provide the value for the `default` locale of the feed 
		String deflocale = this.xml.getAttribute("Locale");
		
		for (XElement fel : this.xml.selectAll("Field")) {
			if (name.equals(fel.getAttribute("Name"))) {
				if (!fel.hasAttribute("Locale"))
					return fel;
					
				if ((deflocale != null) && deflocale.equals(fel.getAttribute("Locale")))
					return fel;
			}
		}
		
		return null;
	}
		
	public XElement getDefaultPart(String name) {
		if ((this.xml == null) || StringUtil.isEmpty(name))
			return null;
		
		// provide the value for the `default` locale of the feed 
		String deflocale = this.xml.getAttribute("Locale");
		
		for (XElement fel : this.xml.selectAll("PagePart")) {
			if (name.equals(fel.getAttribute("For"))) {
				if (!fel.hasAttribute("Locale"))
					return fel;
					
				if ((deflocale != null) && deflocale.equals(fel.getAttribute("Locale")))
					return fel;
			}
		}
		
		return null;
	}
	
	public FeedPartMatchResult bestMatch(String tag, String attr,  String name) {
		return this.bestMatch(OperationContext.get(), tag, attr, name);
	}
	
	public FeedPartMatchResult bestMatch(ITranslationAdapter ctx, String tag, String attr, String match) {
		if ((this.xml == null) || StringUtil.isEmpty(tag) || StringUtil.isEmpty(attr) || StringUtil.isEmpty(match))
			return null;

		int highest = Integer.MAX_VALUE;
		FeedPartMatchResult best = new FeedPartMatchResult();
		
		String deflocale = this.xml.getAttribute("Locale", ctx.getWorkingLocale());
		
		for (XElement fel : this.xml.selectAll(tag)) {
			if (! match.equals(fel.getAttribute(attr)))
					continue;
			
			String flocale = fel.getAttribute("Locale", deflocale);
		
			int arate = ctx.rateLocale(flocale);
			
			if ((arate == -1) && ! deflocale.equals(flocale))
				break;	
			
			// if all else fails use default
			if ((arate == -1))
				arate = 100;	
			
			if (arate >= highest) 
				continue;
				
			best.el = fel;
			best.localename = flocale;
			highest = arate;
		}
		
		if (highest == Integer.MAX_VALUE)
			return null;
		
		best.locale = ctx.getLocaleDefinition(best.localename);
		
		return best;
	}
	
	public String getFirstField(String... names) {
		return this.getFirstField(OperationContext.get(), names);
	}
	
	public String getFirstField(ITranslationAdapter ctx, String... names) {
		for (String n : names) {
			String v = this.getField(n);
			
			if (v != null)
				return v;
		}
		
		return null;
	}
	
	public String getField(String name) {
		return this.getField(OperationContext.get(), name);
	}
	
	public String getField(ITranslationAdapter ctx, String name) {
		if ((this.xml == null) || StringUtil.isEmpty(name))
			return null;
		
		FeedPartMatchResult mr = this.bestMatch(ctx, "Field", "Name", name);
		
		if (mr != null)
			return mr.el.getValue();
		
		return null;
	}
	
	public String getField(ITranslationAdapter ctx, String locale, String name) {
		if ((this.xml == null) || StringUtil.isEmpty(name))
			return null;

		if (StringUtil.isEmpty(locale))
			locale = ctx.getWorkingLocale();
		
		// provide the value for the `default` locale of the feed 
		String deflocale = this.xml.getAttribute("Locale", ctx.getWorkingLocale());

		// if matches default locale then Field goes in top level elements
		for (XElement fel : this.xml.selectAll("Field")) {
			if (! name.equals(fel.getAttribute("Name"))) 
				continue;
			
			if (locale.equals(deflocale) && ! fel.hasAttribute("Locale")) 
				return fel.getValue();
			
			if (locale.equals(fel.getAttribute("Locale"))) 
				return fel.getValue();
		}
				
		return null;
	}
	
	public void setField(ITranslationAdapter ctx, String locale, String name, String value) {
		if ((this.xml == null) || StringUtil.isEmpty(name))
			return;
		
		// provide the value for the `default` locale of the feed 
		String deflocale = this.xml.getAttribute("Locale", ctx.getWorkingLocale());
		
		// TODO support other fields based on channel
		// special handling for some fields - always at top level
		if ("Published".equals(name) || "AuthorUsername".equals(name) || "AuthorName".equals(name) || "Created".equals(name))
			locale = deflocale;

		// if matches default locale then Field goes in top level elements
		for (XElement fel : this.xml.selectAll("Field")) {
			if (! name.equals(fel.getAttribute("Name"))) 
				continue;
			
			if (locale.equals(deflocale) && ! fel.hasAttribute("Locale")) {
				fel.setValue(value);
				return;
			}
			
			if (locale.equals(fel.getAttribute("Locale"))) {
				fel.setValue(value);
				return;
			}
		}
		
		XElement fel = new XElement("Field")
				.withAttribute("Name", name);
		
		if (! locale.equals(deflocale))
			fel.withAttribute("Locale", locale);
		
		fel.setValue(value);
				
		this.xml.with(fel);
	}
	
	public void removeField(ITranslationAdapter ctx, String locale, String name) {
		if ((this.xml == null) || StringUtil.isEmpty(name))
			return;

		if (StringUtil.isEmpty(locale))
			locale = ctx.getWorkingLocale();
		
		// provide the value for the `default` locale of the feed 
		String deflocale = this.xml.getAttribute("Locale", ctx.getWorkingLocale());

		// if matches default locale then Field goes in top level elements
		for (XElement fel : this.xml.selectAll("Field")) {
			if (! name.equals(fel.getAttribute("Name"))) 
				continue;
			
			if (locale.equals(deflocale) && ! fel.hasAttribute("Locale")) {
				this.xml.remove(fel);
				return;
			}
			
			if (locale.equals(fel.getAttribute("Locale"))) {
				this.xml.remove(fel);
				return;
			}
		}
	}
	
	public void clearTags() {
		if (this.xml == null)
			return;
		
		for (XElement t : this.xml.selectAll("Tag"))
			this.xml.remove(t);
	}
	
	public void addTag(String tag) {
		if (this.xml == null)
			return;
		
		this.xml.with(new XElement("Tag").withAttribute("Alias", tag));
	}
	
	public String getTags() {
		if (this.xml == null)
			return "";
		
		StringBuilder sb = new StringBuilder();
		
		for (XElement t : this.xml.selectAll("Tag")) {
			if (sb.length() > 0)
				sb.append(", ");
			
			sb.append(t.getAttribute("Alias"));
		}
		
		return sb.toString();
	}
	
	public String getPart(String name, boolean preview) {
		return this.getPart(OperationContext.get(), name, preview);
	}
	
	public String getPart(ITranslationAdapter ctx, String name, boolean preview) {
		if ((this.xml == null) || StringUtil.isEmpty(name))
			return null;
		
		FeedPartMatchResult mr = this.bestMatch(ctx, "PagePart", "For", name);
		
		if (mr == null)
			return null;

		return this.getPartValue(ctx, mr, preview);
	}
	
	public String getPart(ITranslationAdapter ctx, String locale, String name, boolean preview) {
		if ((this.xml == null) || StringUtil.isEmpty(name))
			return null;

		if (StringUtil.isEmpty(locale))
			locale = ctx.getWorkingLocale();
		
		// provide the value for the `default` locale of the feed 
		String deflocale = this.xml.getAttribute("Locale", ctx.getWorkingLocale());

		// if matches default locale then Field goes in top level elements
		for (XElement fel : this.xml.selectAll("PagePart")) {
			if (! name.equals(fel.getAttribute("For"))) 
				continue;
			
			if (locale.equals(deflocale) && ! fel.hasAttribute("Locale")) 
				return this.getPartValue(locale, fel, preview);
			
			if (locale.equals(fel.getAttribute("Locale"))) 
				return this.getPartValue(locale, fel, preview);
		}
				
		return null;
	}
	
	public void setPart(ITranslationAdapter ctx, String locale, String name, String format, String value, boolean isPreview) {
		if ((this.xml == null) || StringUtil.isEmpty(name))
			return;

		if (value == null)
			value = "";
		
		XElement mr = null;
		
		// provide the value for the `default` locale of the feed 
		String deflocale = this.xml.getAttribute("Locale", ctx.getWorkingLocale());

		// if matches default locale then Field goes in top level elements
		for (XElement fel : this.xml.selectAll("PagePart")) {
			if (! name.equals(fel.getAttribute("For"))) 
				continue;
			
			if (locale.equals(deflocale) && ! fel.hasAttribute("Locale")) {
				mr = fel;
				break;
			}
			
			if (locale.equals(fel.getAttribute("Locale"))) {
				mr = fel;
				break;
			}
		}
		
		if (mr == null) {
			mr = new XElement("PagePart")
					.withAttribute("For", name);
			
			if (! locale.equals(deflocale))
				mr.withAttribute("Locale", locale);
					
			this.xml.with(mr);
		}
		
		mr.clearValue();
		
		mr
			.withAttribute("Format", format)
			.withAttribute("External", "true");
		
		Path fpath = this.getPartFile(locale, mr, isPreview);
		
		try {
			IOUtil.saveEntireFile2(fpath, value);
		}
		catch (Exception x) {
		}
	}
	
	public void removePart(ITranslationAdapter ctx, String locale, String name, boolean isPreview) {
		if ((this.xml == null) || StringUtil.isEmpty(name))
			return;

		if (StringUtil.isEmpty(locale))
			locale = ctx.getWorkingLocale();
		
		// provide the value for the `default` locale of the feed 
		String deflocale = this.xml.getAttribute("Locale", ctx.getWorkingLocale());

		// if matches default locale then Field goes in top level elements
		for (XElement fel : this.xml.selectAll("PagePart")) {
			if (! name.equals(fel.getAttribute("For"))) 
				continue;
			
			if (locale.equals(deflocale) && ! fel.hasAttribute("Locale")) {
				this.removePartValue(locale, fel, isPreview);	
				return;
			}
			
			if (locale.equals(fel.getAttribute("Locale"))) {
				this.removePartValue(locale, fel, isPreview);	
				return;
			}
		}
	}
	
	public void removePartValue(String locale, XElement part, boolean isPreview) {
		if (part == null)
			return;
		
		this.xml.remove(part);
		
		if (part.getAttributeAsBooleanOrFalse("External")) {
			Path fpath = this.getPartFile(locale, part, isPreview);
			
			try {
				Files.deleteIfExists(fpath);
			}
			catch (Exception x) {
			}
		}
	}
	
	// get parts relative to a context
	
	public String getPartValue(ITranslationAdapter ctx, FeedPartMatchResult mr, boolean isPreview) {
		if ((ctx == null) || (mr == null))
			return null;
		
		return this.getPartValue(mr.locale.getName(), mr.el, isPreview);
	}
		
	public String getPartValue(String locale, XElement part, boolean isPreview) {
		if (part == null)
			return null;
		
		if (part.getAttributeAsBooleanOrFalse("External")) {
			Path fpath = this.getPartFile(locale, part, isPreview);
			
			if (isPreview && Files.notExists(fpath)) 
				fpath = this.getPartFile(locale, part, false);

			if (Files.exists(fpath)) {
				FuncResult<CharSequence> mres = IOUtil.readEntireFile(fpath);
				
				if (mres.isNotEmptyResult()) 
					return mres.getResult().toString();
			}
		}
		
		return part.getValue();
	}
	
	public Path getPartFile(String locale, XElement part, boolean isPreview) {
		// use the override locale if present
		
		String spath = this.channel + this.path + "." + part.getAttribute("For") + "." 
			+ part.getAttribute("Locale", locale) + "." + part.getAttribute("Format");
		
		if (isPreview)
			return OperationContext.get().getSite().resolvePath("/feed-preview/" + spath); 
		
		return OperationContext.get().getSite().resolvePath("/feed/" + spath); 
	}
}
