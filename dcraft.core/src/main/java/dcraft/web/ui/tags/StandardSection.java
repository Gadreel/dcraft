package dcraft.web.ui.tags;

import java.lang.ref.WeakReference;
import java.util.List;

import dcraft.web.ui.UIWork;
import dcraft.xml.XNode;

public class StandardSection extends Section {
	public StandardSection() {
		super("dc.StandardSection");
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		super.translate(work, pnodes);

		this.withClass("dc-section-standard")
			.withAttribute("data-dccms-plugin", "StandardSection");
	}
}
