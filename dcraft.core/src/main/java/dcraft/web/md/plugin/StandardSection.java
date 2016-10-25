package dcraft.web.md.plugin;

import java.util.List;
import java.util.Map;

import dcraft.log.Logger;
import dcraft.web.md.Plugin;
import dcraft.web.md.ProcessContext;
import dcraft.web.md.Processor;
import dcraft.web.ui.UIElement;
import dcraft.xml.XElement;

public class StandardSection extends Plugin {
	public StandardSection() {
		super("StandardSection");
	}

	@Override
	public void emit(ProcessContext ctx, UIElement parent, List<String> lines, Map<String, String> params) {
        StringBuilder in = new StringBuilder();

        for (String n : lines)
        	in.append(n).append("\n");

        try {
			XElement cbox = Processor.parse(ctx, in.toString())
				.withAttribute("class", "dc-section dc-section-standard " + (params.containsKey("Class") ? params.get("Class") : ""));

			cbox.setAttribute("data-dccms-section", "plugin");
				
			if (params.containsKey("Id"))
				cbox.withAttribute("id", params.get("Id"));
			
			if (params.containsKey("Lang"))
				cbox.withAttribute("lang", params.get("Lang"));
			
			parent.with(cbox);
        }
        catch (Exception x) {
        	Logger.error("Error adding copy box: " + x);
        }
	}
}
