package dcraft.web.ui.tags;

import java.lang.ref.WeakReference;
import java.util.List;

import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.xml.XNode;

public class Param extends UIElement {
	public Param() {
		super("dc.Param");
	}
	
	@Override
	public void expand(WeakReference<UIWork> work) {
		// do not expand children, just set the params on parent
		
		UIElement p = this.getParent();
		
		if (this.hasAttribute("Name") && this.hasAttribute("Value"))
			p.withParam(this.getAttribute("Name"), this.getAttribute("Value"));
		
		if (this.hasAttribute("Name") && this.hasChildren())
			p.withUIParam(this.getAttribute("Name"), this.getChildren());
	}
	
	@Override
	public void build(WeakReference<UIWork> work) {
		// nothing should happen, this is not to be used
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		// nothing should happen, this is not to be used
	}
	
	@Override
	public void sequence() {
		// nothing should happen, this is not to be used
	}
}
