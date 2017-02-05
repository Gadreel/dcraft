package dcraft.web.md.plugin;

import java.util.List;
import java.util.Map;

import dcraft.web.md.Plugin;
import dcraft.web.md.ProcessContext;
import dcraft.web.ui.UIElement;

public class PairedMediaSection extends Plugin {
	public PairedMediaSection() {
		super("PairedMediaSection");
	}

	@Override
	public void emit(ProcessContext ctx, UIElement parent, List<String> lines, Map<String, String> params) {
		UIElement pel = new dcraft.web.ui.tags.PairedMediaSection();
		
		if (params.containsKey("Id"))
			pel.withAttribute("id", params.get("Id"));
		
		if (params.containsKey("Lang"))
			pel.withAttribute("lang", params.get("Lang"));
		
		if (params.containsKey("Class"))
			pel.withClass(params.get("Class").split(" "));
		
		if (params.containsKey("Hidden"))
			pel.withAttribute("Hidden", params.get("Hidden"));
		
		if (params.containsKey("Image"))
			pel.withAttribute("Image", params.get("Image"));
		
		if (params.containsKey("YouTubeId"))
			pel.withAttribute("YouTubeId", params.get("YouTubeId"));
		
		if (params.containsKey("YouTubeUrl"))
			pel.withAttribute("YouTubeUrl", params.get("YouTubeUrl"));
		
		if (params.containsKey("MediaId"))
			pel.withAttribute("MediaId", params.get("MediaId"));
		
        StringBuilder in = new StringBuilder();

        for (String n : lines)
        	in.append(n).append("\n");

        pel.withText(in.toString());
		
		parent.with(pel);
	}
}
