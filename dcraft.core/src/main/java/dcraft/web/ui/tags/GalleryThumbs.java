package dcraft.web.ui.tags;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;

import dcraft.lang.op.FuncResult;
import dcraft.log.Logger;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.util.StringUtil;
import dcraft.web.WebModule;
import dcraft.web.core.GalleryImageConsumer;
import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.xml.XElement;
import dcraft.xml.XNode;

public class GalleryThumbs extends UIElement {
	public GalleryThumbs() {
		super("dc.GalleryThumbs");
	}
	
	@Override
	public UIElement newNode() {
		return new GalleryThumbs();
	}
	
	@Override
	public void build(WeakReference<UIWork> work) {
		Long maximgs = this.hasNotEmptyAttribute("Max") ? StringUtil.parseInt(this.getAttribute("Max")) : null;
		
	    String template = "";
		XElement xtemp = this.find("Template");
		
		if (xtemp != null) 
			template = xtemp.toInnerString();
		else 
			template = this.getText();
		
	    if (StringUtil.isEmpty(template))
	    	template = "<a href=\"#\" data-image=\"@img|Alias@\"><img src=\"@path@\" data-dc-img=\"@imgdata@\" /></a>";
	    
	    String ftemplate = template;
	    
	    this.clearChildren();
	    
	    // now build up the xml for the content
	    StringBuilder out = new StringBuilder();
	
	    out.append("<div>");
	
	    AtomicLong currimg = new AtomicLong();
	    
	    work.get().getContext().getSite().forEachGalleryShowImage(this.getAttribute("Path"), this.getAttribute("Show"), 
	    		work.get().getContext().isPreview(), new GalleryImageConsumer() 
	    {
			@Override
			public void accept(RecordStruct meta, RecordStruct show, Struct img) {
				long cidx = currimg.incrementAndGet();
				
				if ((maximgs != null) && (cidx > maximgs))
					return;
				
			  boolean checkmatches = true;
			  
			  String value = ftemplate;
			  
			  while (checkmatches) {
				  checkmatches = false;
				  Matcher m = WebModule.macropatten.matcher(value);
				  
				  while (m.find()) {
					  String grp = m.group();
					  String macro = grp.substring(1, grp.length() - 1);
					  String val = GalleryThumbs.expandMacro(GalleryThumbs.this.getAttribute("Path"), meta, show, img, macro);
					  
					  // if any of these, then replace and check (expand) again 
					  if (val != null) {
						  value = value.replace(grp, UIElement.quote(val));
						  checkmatches = true;
					  }
				  }
			  }
			  
			  out.append(value);
			}
		});
	            
	    out.append("</div>");
	
	    try {
	    	FuncResult<XElement> xres = work.get().getContext().getSite().getWebsite().parseUI(out);
	    	
	    	if (xres.isNotEmptyResult()) {
	    		XElement lbox = xres.getResult();
	    		
	    		lbox
					.withAttribute("class", "dc-section-gallery-list");
					
				if (this.hasNotEmptyAttribute("ListId"))
					lbox.withAttribute("id", this.getAttribute("ListId"));
				
				this.add(lbox);
	    	}
	    }
	    catch (Exception x) {
	    	Logger.error("Error adding gallery section: " + x);
	    }
			
		UIElement p = this.getParent();
		
		if (p != null) {
			if (! (p instanceof PagePart))
				p = p.getParent();
			
			if (p != null) {
				if (! (p instanceof PagePart))
					p = p.getParent();
				
				if (p != null) {
					if ((p instanceof PagePart) && ((PagePart)p).isCmsEditable()) {
						this.with(new Button("dcmi.GalleryButton")
							.withClass("dcuiGalleryButton", "dcuiCmsi")
							.withAttribute("Icon", "fa-pencil")
						);
					}
				}
			}
		}

		super.build(work);
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		this.withClass("dc-gallery-thumbs")
			.withAttribute("data-dc-enhance", "true")
			.withAttribute("data-dc-tag", this.getName())
			//.withAttribute("data-dccms-plugin", "Gallery")
			.withAttribute("data-variant", this.getAttribute("Variant"))
			.withAttribute("data-ext", this.getAttribute("Extension"))
			.withAttribute("data-path", this.getAttribute("Path"))
			.withAttribute("data-show", this.getAttribute("Show"));
		
		this.setName("div");
		
		super.translate(work, pnodes);
	}

	  static public String expandMacro(String path, RecordStruct meta, RecordStruct show, Struct img, String macro) {
		  String[] parts = macro.split("\\|");
		  
		  String val = null;
		  
		  // TODO get the format from the variation itself, rather than hard code the .jpg
		  if ("path".equals(parts[0])) {
			  val = "/galleries" + path + "/" + ((RecordStruct) img).getFieldAsString("Alias") + ".v/" + show.getFieldAsString("Variation") + ".jpg";
		  }
		  else if ("img".equals(parts[0]) && (parts.length > 1) && (img instanceof RecordStruct)) {
			  val = ((RecordStruct) img).getFieldAsString(parts[1]);
		  }
		  else if ("imgdata".equals(parts[0])) {
			  val = ((RecordStruct) img).toString();
		  }
		  else if ("show".equals(parts[0]) && (parts.length > 1)) {
			  val = show.getFieldAsString(parts[1]);
		  }
		  else if ("meta".equals(parts[0]) && (parts.length > 1)) {
			  val = meta.getFieldAsString(parts[1]);
		  }
		  
		  return val;
	  }

	  /*
		<img name="b1" src="http://img.youtube.com/vi/6_XYrX5FvOY/0.jpg"> 
		<img name="b2" src="http://img.youtube.com/vi/6_XYrX5FvOY/1.jpg">   (thumb for in listing - 120x90  4:3)
		<img name="b3" src="http://img.youtube.com/vi/6_XYrX5FvOY/2.jpg">
		<img name="b4" src="http://img.youtube.com/vi/6_XYrX5FvOY/3.jpg">
		<img name="b5" src="http://img.youtube.com/vi/6_XYrX5FvOY/hqdefault.jpg">   (bigger, black bars - 480x360  4:3)
		<img name="b6" src="http://img.youtube.com/vi/6_XYrX5FvOY/mqdefault.jpg">   (smaller, no bars - 320x180  16:9)
		<img name="b7" src="http://img.youtube.com/vi/6_XYrX5FvOY/maxresdefault.jpg">
	   * 
	   */
	  static public String expandMacro(XElement media, String macro) {
		  String[] parts = macro.split("\\|");
		  
		  String val = null;
		  
		  if ("path".equals(parts[0])) {
			  val = media.getAttribute("Thumb");
			  
			  if (StringUtil.isEmpty(val)) {
				  String ytid = media.getAttribute("YouTubeId");
				  String thint = media.getAttribute("ThumbHint", "4:3-ratio");
				  
				  if (StringUtil.isNotEmpty(ytid)) {
					  val = "https://img.youtube.com/vi/" + ytid;
					  
					  val += ("16:9-ratio".equals(thint)) ? "/mqdefault.jpg" : "/hqdefault.jpg";
				  }
			  }
		  }
		  else if ("attr".equals(parts[0]) && (parts.length > 1)) {
			  val = media.getAttribute(parts[1]);
		  }
		  else if ("mediakind".equals(parts[0])) {
			  val = media.hasNotEmptyAttribute("Thumb") ? "image" : "video";
		  }
		  else if ("mediadata".equals(parts[0])) {
			  RecordStruct mdata = new RecordStruct();
			  
			 for (String att : media.getAttributes().keySet()) {
				 mdata.withField(att, media.getAttribute(att));
			 }
			  
			  RecordStruct fields = new RecordStruct();
			  
			  mdata.withField("Fields", fields);
			  
			  for (XElement fld : media.selectAll("Field")) {
				  if (fld.hasNotEmptyAttribute("Name")) 
					  fields.withField(fld.getAttribute("Name"), fld.getAttribute("Value"));
			  }
			  
			  val = mdata.toString();
		  }
		  else if ("fld".equals(parts[0]) && (parts.length > 1)) {
			  for (XElement fld : media.selectAll("Field")) {
				  if (parts[1].equals(fld.getAttribute("Name"))) {
					  val = fld.getAttribute("Value");
				  }
			  }
		  }
		  
		  return val;
	  }
}
