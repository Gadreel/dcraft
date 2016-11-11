package dcraft.web.md.plugin;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import dcraft.lang.op.FuncResult;
import dcraft.log.Logger;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.util.StringUtil;
import dcraft.web.WebModule;
import dcraft.web.core.GalleryImageConsumer;
import dcraft.web.md.Plugin;
import dcraft.web.md.ProcessContext;
import dcraft.web.ui.UIElement;
import dcraft.xml.XElement;

public class GallerySection extends Plugin {
	public GallerySection() {
		super("GallerySection");
	}

	@Override
	public void emit(ProcessContext ctx, UIElement parent, List<String> lines, Map<String, String> params) {
		UIElement cbox = new UIElement("div");
		
		cbox
			.withAttribute("class", "dc-section dc-section-gallery " + (params.containsKey("Class") ? params.get("Class") : ""))
			.withAttribute("data-dccms-section", "plugin");
			
		if (params.containsKey("Id"))
			cbox.withAttribute("id", params.get("Id"));
		
		if (params.containsKey("Lang"))
			cbox.withAttribute("lang", params.get("Lang"));
		
		if (params.containsKey("Path"))
			cbox.withAttribute("data-path", params.get("Path"));
		
		if (params.containsKey("Show"))
			cbox.withAttribute("data-show", params.get("Show"));
		
		parent.add(cbox);
		
		if (params.containsKey("Title"))
			cbox.add(new UIElement("h2").withText("Title"));
		
		// convert the template to one string 
        StringBuilder in = new StringBuilder();

        for (String n : lines)
        	in.append(n).append("\n");
        
        String template = in.toString();
		
        if (StringUtil.isEmpty(template))
        	template = "<div><img src=\"@path@\" /><MD>@img|Description@</MD></div>";
        
        String ftemplate = template;
        
        // now build up the xml for the content
        StringBuilder out = new StringBuilder();

        out.append("<div>");

        ctx.getOutput().getSite().forEachGalleryShowImage(params.get("Path"), params.get("Show"), 
        		ctx.getOutput().isPreview(), new GalleryImageConsumer() 
        {
			@Override
			public void accept(RecordStruct meta, RecordStruct show, Struct img) {
			  boolean checkmatches = true;
			  
			  String value = ftemplate;
			  
			  while (checkmatches) {
				  checkmatches = false;
				  Matcher m = WebModule.macropatten.matcher(value);
				  
				  while (m.find()) {
					  String grp = m.group();
					  String macro = grp.substring(1, grp.length() - 1);
					  String val = GallerySection.this.expandMacro(ctx, params.get("Path"), meta, show, img, macro);
					  
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
        	FuncResult<XElement> xres = ctx.getOutput().getSite().getWebsite().parseUI(out);
        	
        	if (xres.isNotEmptyResult()) {
        		XElement lbox = xres.getResult();
        		
        		lbox
					.withAttribute("class", "dc-section-gallery-list");
					
				if (params.containsKey("ListId"))
					lbox.withAttribute("id", params.get("ListId"));
				
				cbox.add(lbox);
        	}
        	else {
				cbox.add(new UIElement("div")
					.withText("Error parsing section."));
        	}
        }
        catch (Exception x) {
        	Logger.error("Error adding gallery section: " + x);
        }
	}

  public String expandMacro(ProcessContext ctx, String path, RecordStruct meta, RecordStruct show, Struct img, String macro) {
	  String[] parts = macro.split("\\|");
	  
	  String val = null;
	  
	  // TODO get the format from the variation itself, rather than hard code the .jpg
	  if ("path".equals(parts[0])) {
		  val = "/galleries" + path + "/" + ((RecordStruct) img).getFieldAsString("Alias") + ".v/" + show.getFieldAsString("Variation") + ".jpg";
	  }
	  else if ("img".equals(parts[0]) && (parts.length > 1) && (img instanceof RecordStruct)) {
		  val = ((RecordStruct) img).getFieldAsString(parts[1]);
	  }
	  else if ("show".equals(parts[0]) && (parts.length > 1)) {
		  val = show.getFieldAsString(parts[1]);
	  }
	  else if ("meta".equals(parts[0]) && (parts.length > 1)) {
		  val = meta.getFieldAsString(parts[1]);
	  }
	  
	  if (val == null)
		  return "";
	  
	  return val;
  }
}
