package dcraft.web.mdx.plugin;

import java.util.List;
import java.util.Map;

import dcraft.log.Logger;
import dcraft.web.mdx.Plugin;
import dcraft.web.mdx.ProcessContext;
import dcraft.web.mdx.Processor;
import dcraft.xml.XElement;

public class StandardSection extends Plugin {
	public StandardSection() {
		super("StandardSection");
	}

	@Override
	public void emit(ProcessContext ctx, XElement parent, List<String> lines, Map<String, String> params) {
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
