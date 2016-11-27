package dcraft.web.ui.tags.form;

import java.lang.ref.WeakReference;
import java.util.List;

import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.xml.XNode;

public class AlignedField extends CoreField {
	@Override
	public void addControl() {
		this.with(new UIElement("div")
			.withClass("dc-pui-control")
			.withAll(this.fieldinfo.getChildren())
		);
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		if ("dcf.FormButtons".equals(this.getName()))
			this.withClass("dc-pui-form-buttons", "dc-pui-field-stacked");
		
		// TODO if FormButtons add a <noscript> explaining JS needs to be enabled
		
		super.translate(work, pnodes);
	}
}
