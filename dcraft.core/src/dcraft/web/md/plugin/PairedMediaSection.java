package dcraft.web.md.plugin;

import java.util.List;
import java.util.Map;

import dcraft.log.Logger;
import dcraft.web.md.Plugin;
import dcraft.web.md.ProcessContext;
import dcraft.web.md.Processor;
import dcraft.web.ui.UIElement;
import dcraft.xml.XElement;

public class PairedMediaSection extends Plugin {
	public PairedMediaSection() {
		super("PairedMediaSection");
	}

	@Override
	public void emit(ProcessContext ctx, UIElement parent, List<String> lines, Map<String, String> params) {
		XElement div = new XElement("div")
			.withAttribute("class", "dc-section dc-section-paired-media " + (params.containsKey("Class") ? params.get("Class") : ""));

		div.setAttribute("data-dccms-section", "plugin");
		
		if (params.containsKey("Id"))
			div.withAttribute("id", params.get("Id"));
		
		if (params.containsKey("Lang"))
			div.withAttribute("lang", params.get("Lang"));
		
        StringBuilder in = new StringBuilder();

        for (String n : lines)
        	in.append(n).append("\n");

        try {
			XElement cbox = Processor.parse(ctx, in.toString())
					.withAttribute("class", "dc-copy-box");
			
			div.with(cbox);
        }
        catch (Exception x) {
        	Logger.error("Error adding copy box" + x);
        }
        
		XElement mbox = new XElement("div");
		
		if (params.containsKey("Image")) {
			mbox.withAttribute("class", "dc-media-box dc-media-image");
			
			String[] images = params.get("Image").split(",");
			
			for (String img : images)
				mbox.with(new XElement("img").withAttribute("src", img));
		}
		else if (params.containsKey("YouTubeId")) {
			/*
			<!-- need nested div for FF -->
			<div class="dc-media-video dc-youtube-container">
				<iframe src="https://www.youtube.com/embed/tnnJdYaztuA?html5=1&rel=0&showinfo=0" frameborder="0" allowfullscreen></iframe>
			</div>
			*/
			
			mbox.withAttribute("class", "dc-media-box")
				.with(new XElement("div")
					.withAttribute("class", "dc-media-video dc-youtube-container")
					.with(new XElement("iframe")
						.withAttribute("src", "https://www.youtube.com/embed/" + params.get("YouTubeId") + "?html5=1&rel=0&showinfo=0")
						.withAttribute("frameborder", "0")
						.withAttribute("allowfullscreen", "allowfullscreen")
					)
				);
		}
		else if (params.containsKey("YouTubeUrl")) {
			/*
			<!-- need nested div for FF -->
			<div class="dc-media-video dc-youtube-container">
				<iframe src="https://www.youtube.com/embed/tnnJdYaztuA?html5=1&rel=0&showinfo=0" frameborder="0" allowfullscreen></iframe>
			</div>
			*/
			
			mbox.withAttribute("class", "dc-media-box")
				.with(new XElement("div")
					.withAttribute("class", "dc-media-video dc-youtube-container")
					.with(new XElement("iframe")
						.withAttribute("src", params.get("YouTubeUrl"))
						.withAttribute("frameborder", "0")
						.withAttribute("allowfullscreen", "allowfullscreen")
					)
				);
		}
		
		if (params.containsKey("MediaId"))
			mbox.withAttribute("id", params.get("MediaId"));
		
		div.with(mbox);
		
		parent.add(div);
	}
}
