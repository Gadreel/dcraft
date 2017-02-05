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
	public UIElement newNode() {
		return new Section();
	}
	
	public PagePart getPagePart() {
		UIElement p = this.getParent();
		
		if (p != null) {
			if (! (p instanceof PagePart))
				p = p.getParent();
			
			if (p instanceof PagePart) 
				return (PagePart) p;
		}
		
		return null;
	}
	
	@Override
	public void build(WeakReference<UIWork> work) {
		PagePart p = this.getPagePart();
		
		if ((p != null) && p.isCmsEditable()) {
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
		
		if (! this.getAttributeAsBooleanOrFalse("Hidden")) {
			super.translate(work, pnodes);
		}
		else {
			PagePart p = this.getPagePart();
			
			if ((p != null) && p.isCmsEditable()) {
				super.translate(work, pnodes);

				this
					.withClass("dc-section-hidden");
			}
		}
	}
}
