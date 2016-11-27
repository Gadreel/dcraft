package dcraft.web.ui.tags;

import java.lang.ref.WeakReference;
import java.util.List;

import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.xml.XNode;

public class Section extends UIElement {
	public Section() {
		super("dc.Section");
	}
	
	public Section(String name) {
		super(name);
	}
	
	@Override
	public void build(WeakReference<UIWork> work) {
		UIElement p = this.getParent();
		
		if ((p instanceof PagePart) && ((PagePart)p).isCmsEditable()) {
			this.with(new Button("dcmi.SectionButton")
				.withClass("dcuiSectionButton", "dcuiCmsi")
				.withAttribute("Icon", "fa-pencil")
			);
		}

		super.build(work);
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		// don't change my identity until after the scripts run
		this.setName("div");

		this
			.withClass("dc-section");
		
		super.translate(work, pnodes);
	}
}
