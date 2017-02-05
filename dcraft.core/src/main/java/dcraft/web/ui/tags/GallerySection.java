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

public class GallerySection extends Section {
	public GallerySection() {
		super("dc.GallerySection");
	}
	
	@Override
	public UIElement newNode() {
		return new GallerySection();
	}
	
	@Override
	public void build(WeakReference<UIWork> work) {
		Long maximgs = this.hasNotEmptyAttribute("Max") ? StringUtil.parseInt(this.getAttribute("Max")) : null;
		
	    String template = "";
		UIElement xtemp = (UIElement) this.find("Template");
		UIElement txtemp = (UIElement) this.find("TextTemplate");
		
		if (xtemp != null) 
			template = xtemp.toInnerString();
		else if (txtemp != null) 
			template = txtemp.getText();
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
					  String val = GalleryThumbs.expandMacro(GallerySection.this.getAttribute("Path"), meta, show, img, macro);
					  
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
	    		UIElement lbox = (UIElement) xres.getResult();
	    		
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
			
	    /* let normal section handling do this
		UIElement p = this.getParent();
		
		if (p != null) {
			if (! (p instanceof PagePart))
				p = p.getParent();
			
			if ((p instanceof PagePart) && ((PagePart)p).isCmsEditable()) {
				this.with(new Button("dcmi.GalleryButton")
					.withClass("dcuiGalleryButton", "dcuiCmsi")
					.withAttribute("Icon", "fa-pencil")
				);
			}
		}
		*/

		super.build(work);
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		this.withClass("dc-section-gallery")
			.withAttribute("data-dc-enhance", "true")
			.withAttribute("data-dc-tag", this.getName())
			.withAttribute("data-dccms-plugin", "Gallery")
			.withAttribute("data-variant", this.getAttribute("Variant"))
			.withAttribute("data-ext", this.getAttribute("Extension"))
			.withAttribute("data-path", this.getAttribute("Path"))
			.withAttribute("data-show", this.getAttribute("Show"));
		
		if (this.hasNotEmptyAttribute("Title"))
			this.add(0, new UIElement("h2").withText(this.getAttribute("Title")));
		
		super.translate(work, pnodes);
	}
}
