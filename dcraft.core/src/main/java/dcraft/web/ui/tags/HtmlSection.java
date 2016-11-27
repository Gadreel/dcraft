package dcraft.web.ui.tags;

import java.lang.ref.WeakReference;
import java.util.List;

import dcraft.web.ui.UIWork;
import dcraft.xml.XNode;

public class HtmlSection extends Section {
	public HtmlSection() {
		super("dc.HtmlSection");
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		super.translate(work, pnodes);

		this.withClass("dc-section-html")
			.withAttribute("data-dccms-plugin", "HtmlSection");
	}
}
