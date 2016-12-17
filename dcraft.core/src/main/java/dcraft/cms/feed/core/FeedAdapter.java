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
	
	static public FeedAdapter from(RecordStruct data) {
		CommonPath cpath = new CommonPath(data.getFieldAsString("Path"));
		
		FeedAdapter adapt = new FeedAdapter();

		adapt.channel = cpath.getName(1);		// site alias is 0
		adapt.path = cpath.subpath(2).toString();
		adapt.xml = new XElement("dcf");
		
		String deflocale = OperationContext.get().getSite().getDefaultLocale();
		
		adapt.xml.withAttribute("Locale", deflocale);
		
		for (Struct fs : data.getFieldAsList("Fields").getItems()) {
			RecordStruct frec = (RecordStruct) fs;
			
			String sub = frec.getFieldAsString("SubId");
			String fdata = frec.getFieldAsString("Data");
			
			if (StringUtil.isEmpty(sub) || StringUtil.isEmpty(fdata))
				continue;
			
			int dpos = sub.lastIndexOf('.');
			
			adapt.setField(sub.substring(dpos + 1), sub.substring(0, dpos), fdata);
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
		
		String title = this.getDefaultField("Title");
		
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
	
	public String getDefaultField(String name) {
		if ((this.xml == null) || StringUtil.isEmpty(name))
			return null;
		
		// provide the value for the `default` locale of the feed 
		String deflocale = this.xml.getAttribute("Locale");
		
		for (XElement fel : this.xml.selectAll("Field")) {
			if (name.equals(fel.getAttribute("Name"))) {
				if (!fel.hasAttribute("Locale"))
					return fel.getValue();
					
				if ((deflocale != null) && deflocale.equals(fel.getAttribute("Locale")))
					return fel.getValue();
			}
		}
		
		return null;
	}
	
	public XElement getDefaultFieldX(String name) {
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
	
	public XElement getDefaultPartX(String name) {
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
		
		for (XElement afel : this.xml.selectAll("Alternate")) {
			String alocale = afel.getAttribute("Locale");
			
			int arate = ctx.rateLocale(alocale);
			
			if ((arate >= highest) || (arate == -1))
				continue;
			
			for (XElement fel : afel.selectAll(tag)) {
				if (match.equals(fel.getAttribute(attr))) {
					best.el = fel;
					best.localename = alocale;
					highest = arate;
					break;
				}
			}
		}
		
		for (XElement fel : this.xml.selectAll(tag)) {
			if (fel.hasAttribute("Locale") && match.equals(fel.getAttribute(attr))) {
				String flocale = fel.getAttribute("Locale");
				
				int arate = ctx.rateLocale(flocale);
				
				if ((arate >= highest) || (arate == -1))
					continue;
				
				best.el = fel;
				best.localename = flocale;
				highest = arate;
			}
		}

		String deflocale = this.xml.getAttribute("Locale");
		
		if (StringUtil.isNotEmpty(deflocale)) {
			int arate = ctx.rateLocale(deflocale);
			
			if ((arate != -1) && (arate < highest)) {
				for (XElement fel : this.xml.selectAll(tag)) {
					if (!fel.hasAttribute("Locale") && match.equals(fel.getAttribute(attr))) {
						best.el = fel;
						best.localename = deflocale;
						highest = arate;
						break;
					}
				}
			}
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
	
	// TODO enhance this currently only works with default fields
	public void setField(String locale, String name, String value) {
		if ((this.xml == null) || StringUtil.isEmpty(name))
			return;
		
		XElement mr = this.getDefaultFieldX(name);
		
		if (mr == null) {
			mr = new XElement("Field").withAttribute("Name", name);
			this.xml.with(mr);
		}
		
		mr.setValue(value);
	}
	
	// TODO enhance this currently only works with default fields
	public void removeField(String locale, String name) {
		if ((this.xml == null) || StringUtil.isEmpty(name))
			return;
		
		XElement mr = this.getDefaultFieldX(name);
		
		if (mr != null) 
			this.xml.remove(mr);
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
	
	// TODO enhance this currently only works with default fields
	public void setPart(Path folder, String locale, String name, String format, String value) {
		if ((this.xml == null) || StringUtil.isEmpty(name))
			return;

		if (value == null)
			value = "";
		
		XElement mr = this.getDefaultPartX(name);
		
		if (mr == null) {
			mr = new XElement("PagePart")
					.withAttribute("For", name);
			
			this.xml.with(mr);
		}
		
		mr.clearValue();
		
		mr
			.withAttribute("Format", format)
			.withAttribute("External", "true");
		
		// remove the file if external
		String fname = this.filepath.getFileName().toString();
		int pos = fname.indexOf('.');
		String spath = (pos != -1) ? fname.substring(0, pos) : fname;
		
		try {
			IOUtil.saveEntireFile2(folder.resolve(spath + "." + name + "." + locale + "." + format), value);
		}
		catch (Exception x) {
		}
	}
	
	// TODO enhance this currently only works with default fields
	public void removePart(Path folder, String locale, String name) {
		if ((this.xml == null) || StringUtil.isEmpty(name))
			return;
		
		XElement mr = this.getDefaultPartX(name);
		
		if (mr != null) {
			this.xml.remove(mr);
		
			String fmt = mr.getAttribute("Format");
			
			if (StringUtil.isNotEmpty(fmt))
				return;
			
			// remove the file if external
			String fname = this.filepath.getFileName().toString();
			int pos = fname.indexOf('.');
			String spath = (pos != -1) ? fname.substring(0, pos) : fname;
						
			try {
				Files.deleteIfExists(folder.resolve(spath + "." + name + "." + locale + "." + fmt));
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
		
		String ex = part.getAttribute("External", "False");
		
		if (part.hasAttribute("Locale"))
			locale = part.getAttribute("Locale");	// use the override locale if present
		
		if (StringUtil.isNotEmpty(ex) && "true".equals(ex.toLowerCase())) {
			String spath = "/" + this.channel + this.path + "." + part.getAttribute("For") + "." + locale + "." + part.getAttribute("Format");

			// TODO connect to file caching system - use resolveCachePath instead ?
			
			Path fpath = null;
			
			if (isPreview) {
				fpath = OperationContext.get().getSite().resolvePath("/feed-preview" + spath);
				
				if (Files.notExists(fpath))
					fpath = OperationContext.get().getSite().resolvePath("/feed" + spath);
			}
			else {
				fpath = OperationContext.get().getSite().resolvePath("/feed" + spath);
			}

			if (Files.exists(fpath)) {
				FuncResult<CharSequence> mres = IOUtil.readEntireFile(fpath);
				
				if (mres.isNotEmptyResult()) 
					return mres.getResult().toString();
			}
		}
		
		return part.getValue();
	}
}
