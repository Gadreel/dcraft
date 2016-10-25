package dcraft.web.md.plugin;

import java.util.List;
import java.util.Map;

import dcraft.lang.op.FuncResult;
import dcraft.log.Logger;
import dcraft.web.md.Plugin;
import dcraft.web.md.ProcessContext;
import dcraft.web.ui.UIElement;
import dcraft.xml.XElement;
import dcraft.xml.XmlReader;

public class HtmlSection extends Plugin {
	public HtmlSection() {
		super("HtmlSection");
	}

	@Override
	public void emit(ProcessContext ctx, UIElement parent, List<String> lines, Map<String, String> params) {
        StringBuilder in = new StringBuilder();
        
        in.append("<div>");

        for (String n : lines)
        	in.append(n).append("\n");
        
        in.append("</div>");

        try {
        	FuncResult<XElement> xres = XmlReader.parse(in, false);
        	
        	if (xres.isNotEmptyResult()) {
        		XElement cbox = xres.getResult();
        		
        		cbox
					.withAttribute("class", "dc-section dc-section-html " + (params.containsKey("Class") ? params.get("Class") : ""))
					.withAttribute("data-dccms-section", "plugin");
					
				if (params.containsKey("Id"))
					cbox.withAttribute("id", params.get("Id"));
				
				if (params.containsKey("Lang"))
					cbox.withAttribute("lang", params.get("Lang"));
				
        		parent.add(cbox);
        	}
        	else {
        		XElement cbox = new XElement("div")
					.withAttribute("class", "dc-section dc-section-html " + (params.containsKey("Class") ? params.get("Class") : ""))
					.withAttribute("data-dccms-section", "plugin")
					.withText("Error parsing section.");
					
				if (params.containsKey("Id"))
					cbox.withAttribute("id", params.get("Id"));
				
        		parent.add(cbox);
        	}
        }
        catch (Exception x) {
        	Logger.error("Error adding html: " + x);
        }
	}
}
