package dcraft.web.ui.tags.form;

import java.lang.ref.WeakReference;
import java.util.List;

import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.xml.XElement;
import dcraft.xml.XNode;

public class MultiInputField extends CoreField {
	@Override
	public void addControl() {
		UIElement grp = new UIElement("div")
			.withClass("dc-pui-control", "dc-pui-input-multi");
		
		List<XElement> inputs = this.fieldinfo.selectAll("Input");
		
		for (XElement el : inputs) 
			grp.with(InputControl.fromField(this, (UIElement) el));

		this.with(grp);
		
		/*
			<div class="dc-pui-control dc-pui-input-multi">
				<input id="first" placeholder="First" class="dc-pui-input-40"  />
				<input id="mid" placeholder="M" class="dc-pui-input-10"  />
				<input id="last" placeholder="Last"  class="dc-pui-input-40" />
			</div>
		*/		
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		this.withClass("dc-pui-field-multi");
		
		super.translate(work, pnodes);
	}
}
