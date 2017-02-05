package dcraft.web.ui.tags;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.xml.XNode;

public class PairedMediaSection extends Section {
	public PairedMediaSection() {
		super("dc.PairedMediaSection");
	}
	
	@Override
	public UIElement newNode() {
		return new PairedMediaSection();
	}
	
	@Override
	public void build(WeakReference<UIWork> work) {
    	String mode = this.getAttribute("Mode", "Unsafe");
    	String content = this.getText();
		
    	this.clearChildren();
    	
		// TODO allocate from webdomain
    	dcraft.web.md.Markdown mdp = new dcraft.web.md.Markdown();

		try {
			UIElement cbox = "Safe".equals(mode) 
				? mdp.processSafe2(work.get().getContext(), content)
				: mdp.process2(work.get().getContext(), content);

			cbox.withAttribute("class", "dc-copy-box");
				
			// root is just a container and has no value
			this.add(cbox);
		} 
		catch (IOException x) {
			this.with(new UIElement("InvalidContent"));
			System.out.println("inline md error: " + x);
		}
		
		UIElement mbox = new UIElement("div");
		
		mbox.withClass("dc-media-box");
		
		if (this.hasNotEmptyAttribute("Image")) {
			mbox.withClass("dc-media-image");
			
			String[] images = this.getAttribute("Image").split(",");
			
			for (String img : images)
				mbox.with(new UIElement("img").withAttribute("src", img));
		}
		else if (this.hasNotEmptyAttribute("YouTubeId")) {
			mbox.with(new UIElement("div")
					.withClass("dc-media-video", "dc-youtube-container-16x9")
					.with(new UIElement("img").withAttribute("src", "/imgs/16by9.png"))
					.with(new UIElement("iframe")
						.withAttribute("src", "https://www.youtube.com/embed/" + this.getAttribute("YouTubeId") + "?html5=1&rel=0&showinfo=0")
						.withAttribute("frameborder", "0")
						.withAttribute("allowfullscreen", "allowfullscreen")
					)
				);
		}
		else if (this.hasNotEmptyAttribute("YouTubeUrl")) {
			mbox.with(new UIElement("div")
					.withClass("dc-media-video", "dc-youtube-container-16x9")
					.with(new UIElement("img").withAttribute("src", "/imgs/16by9.png"))
					.with(new UIElement("iframe")
						.withAttribute("src", this.getAttribute("YouTubeUrl"))
						.withAttribute("frameborder", "0")
						.withAttribute("allowfullscreen", "allowfullscreen")
					)
				);
		}
		
		if (this.hasNotEmptyAttribute("MediaId"))
			mbox.withAttribute("id", this.getAttribute("MediaId"));
		
		this.with(mbox);
		
		super.build(work);
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		super.translate(work, pnodes);

		this.withClass("dc-section-paired-media")
			.withAttribute("data-dccms-plugin", "PairedMedia");
	}
}
