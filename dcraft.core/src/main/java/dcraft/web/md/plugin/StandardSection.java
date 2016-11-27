package dcraft.web.md.plugin;

import java.util.List;
import java.util.Map;

import dcraft.log.Logger;
import dcraft.web.md.Plugin;
import dcraft.web.md.ProcessContext;
import dcraft.web.md.Processor;
import dcraft.web.ui.UIElement;
import dcraft.xml.XNode;

public class StandardSection extends Plugin {
	public StandardSection() {
		super("StandardSection");
	}

	@Override
	public void emit(ProcessContext ctx, UIElement parent, List<String> lines, Map<String, String> params) {
        StringBuilder in = new StringBuilder();

        for (String n : lines)
        	in.append(n).append("\n");
        
		UIElement pel = new dcraft.web.ui.tags.StandardSection();
		
		if (params.containsKey("Id"))
			pel.withAttribute("id", params.get("Id"));
		
		if (params.containsKey("Lang"))
			pel.withAttribute("lang", params.get("Lang"));
		
		if (params.containsKey("Class"))
			pel.withClass(params.get("Class").split(" "));
	
        try {
			UIElement cbox = Processor.parse(ctx, in.toString());
 			
			// copy all children
			for (XNode n : cbox.getChildren())
				pel.add(n);
        }
        catch (Exception x) {
			pel.with(new UIElement("InvalidContent"));
        	Logger.warn("Error adding copy box " + x);
        }
		
		parent.with(pel);
	}
}
