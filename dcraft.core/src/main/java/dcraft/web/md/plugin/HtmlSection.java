package dcraft.web.md.plugin;

import java.util.List;
import java.util.Map;

import dcraft.lang.op.FuncResult;
import dcraft.log.Logger;
import dcraft.web.md.Plugin;
import dcraft.web.md.ProcessContext;
import dcraft.web.ui.UIElement;
import dcraft.xml.XElement;
import dcraft.xml.XNode;
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
        
		UIElement pel = new dcraft.web.ui.tags.HtmlSection();
		
		if (params.containsKey("Id"))
			pel.withAttribute("id", params.get("Id"));
		
		if (params.containsKey("Lang"))
			pel.withAttribute("lang", params.get("Lang"));
		
		if (params.containsKey("Class"))
			pel.withClass(params.get("Class").split(" "));
	
        try {
        	FuncResult<XElement> xres = XmlReader.parse(in, false);
        	
        	if (xres.isNotEmptyResult()) {
        		XElement cbox = xres.getResult();
     			
    			// copy all children
    			for (XNode n : cbox.getChildren())
    				pel.add(n);
        	}
        }
        catch (Exception x) {
			pel.with(new UIElement("InvalidContent"));
        	Logger.warn("Error adding html box " + x);
        }
		
		parent.with(pel);
	}
}
