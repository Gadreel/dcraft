package dcraft.web.ui.tags.form;

import java.lang.ref.WeakReference;
import java.util.List;

import dcraft.session.Session;
import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.xml.XNode;

public class AlignedInstructions extends UIElement {
	protected String fieldid = null;
	
	public AlignedInstructions() {
		super("dcf.AlignedInstructions");
	}
	
	@Override
	public void expand(WeakReference<UIWork> work) {
		if (this.hasNotEmptyAttribute("id")) 
			this.fieldid = this.getAttribute("id");
		else 
			this.fieldid = "gen" + Session.nextUUId();
		
		UIElement inst = (UIElement) new UIElement("div")
				.withAttribute("class", "dc-pui-message dc-pui-message-info");
		
		inst.replaceChildren(this);	// copy instructions into here
		
		this.clearChildren();		// remove them from here
		
		this.with(new UIElement("div").withClass("dc-pui-spacer"));
		
		this.with(new UIElement("div")
			.withClass("dc-pui-control")
			.with(inst)							// fill up like a control
		);

		super.expand(work);
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		this.setName("div");
		
		this
			.withClass("dc-pui-field")
			.withAttribute("id", "fld" + this.fieldid);
		
		Form frm = this.getForm();
		
		if (this.getAttributeAsBooleanOrFalse("Invalid") || ((frm != null) && frm.getAttributeAsBooleanOrFalse("Invalid")))
			this.withClass("dc-pui-invalid");
		
		if (this.getAttributeAsBooleanOrFalse("Stacked") || ((frm != null) && frm.getAttributeAsBooleanOrFalse("Stacked")))
			this.withClass("dc-pui-field-stacked");
		
		super.translate(work, pnodes);
	}
}
