package dcraft.web.ui.tags.form;

import java.lang.ref.WeakReference;
import java.util.List;

import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.xml.XNode;

public class Instructions extends UIElement {
	public Instructions() {
		super("dcf.Instructions");
	}
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		this.setName("div");
		
		this
			.withClass("dc-pui-message", "dc-pui-message-info");
		
		Form frm = this.getForm();
		
		if (this.getAttributeAsBooleanOrFalse("Invalid") || ((frm != null) && frm.getAttributeAsBooleanOrFalse("Invalid")))
			this.withClass("dc-pui-invalid");
		
		if (this.getAttributeAsBooleanOrFalse("Stacked") || ((frm != null) && frm.getAttributeAsBooleanOrFalse("Stacked")))
			this.withClass("dc-pui-field-stacked");
		
		super.translate(work, pnodes);
	}
}
