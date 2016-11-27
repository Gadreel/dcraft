package dcraft.web.ui.tags;

import java.lang.ref.WeakReference;
import java.util.List;

import dcraft.web.ui.UIWork;
import dcraft.xml.XNode;

public class GallerySection extends Section {
	public GallerySection() {
		super("dc.GallerySection");
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		this.withClass("dc-section-gallery")
			.withAttribute("data-dc-enhance", "true")
			.withAttribute("data-dc-tag", this.getName())
			.withAttribute("data-dccms-plugin", "GallerySection")
			.withAttribute("data-path", this.getAttribute("Path"))
			.withAttribute("data-show", this.getAttribute("Show"));
		
		super.translate(work, pnodes);
	}
}
