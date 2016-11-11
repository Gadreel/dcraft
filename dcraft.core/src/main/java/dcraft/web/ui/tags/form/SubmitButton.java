package dcraft.web.ui.tags.form;

import java.lang.ref.WeakReference;
import java.util.List;

import dcraft.web.ui.UIWork;
import dcraft.web.ui.tags.Button;
import dcraft.xml.XNode;

public class SubmitButton extends Button {
	public SubmitButton() {
		super("dcf.SubmitButton");
	}
	
	@Override
	public void build(WeakReference<UIWork> work) {
		if (! this.hasNotEmptyAttribute("Label"))
			this.withAttribute("Label", "Submit");
		
		super.build(work);
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		this.withClass("pure-button-primary");
		
		super.translate(work, pnodes);
	}
}
