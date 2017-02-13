package dcraft.web.ui.tags;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.regex.Matcher;

import dcraft.lang.op.FuncResult;
import dcraft.log.Logger;
import dcraft.util.StringUtil;
import dcraft.web.WebModule;
import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.xml.XElement;
import dcraft.xml.XNode;

public class MediaSection extends Section {
	public MediaSection() {
		super("dc.MediaSection");
	}
	
	@Override
	public UIElement newNode() {
		return new MediaSection();
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
	    	template = "<a href=\"#\"><img src=\"@path@\" data-dc-media=\"@mediadata@\" /></a>";
	    
	    List<XElement> medias = this.selectAll("Media");
	    
	    this.clearChildren();
	    
	    // now build up the xml for the content
	    StringBuilder out = new StringBuilder();
	
	    out.append("<div>");
	
	    int currimg = 0;
	    
	    for (XElement media : medias) {
	    	long cidx = currimg++;

	    	if ((maximgs != null) && (cidx > maximgs))
	    		return;

	    	boolean checkmatches = true;

	    	String value = template;

	    	while (checkmatches) {
	    		checkmatches = false;
	    		Matcher m = WebModule.macropatten.matcher(value);

	    		while (m.find()) {
	    			String grp = m.group();
	    			String macro = grp.substring(1, grp.length() - 1);
	    			String val = GalleryThumbs.expandMacro(media, macro);

	    			// if any of these, then replace and check (expand) again 
	    			if (val != null) {
	    				value = value.replace(grp, UIElement.quote(val));
	    				checkmatches = true;
	    			}
	    		}
	    	}

	    	out.append(value);
	    }
	            
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

		super.build(work);
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		this.withClass("dc-section-media")
			.withAttribute("data-dc-enhance", "true")
			.withAttribute("data-dc-tag", this.getName())
			.withAttribute("data-dccms-plugin", "Media");
		
		if (this.hasNotEmptyAttribute("Title"))
			this.add(0, new UIElement("h2").withText(this.getAttribute("Title")));
		
		super.translate(work, pnodes);
	}
}
