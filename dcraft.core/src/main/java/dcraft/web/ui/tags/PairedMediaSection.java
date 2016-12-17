package dcraft.web.ui.tags;

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
	public void expand(WeakReference<UIWork> work) {
		// TODO Auto-generated method stub
		super.expand(work);
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		super.translate(work, pnodes);

		this.withClass("dc-section-paired-media")
			.withAttribute("data-dccms-plugin", "PairedMediaSection");
	}
}
