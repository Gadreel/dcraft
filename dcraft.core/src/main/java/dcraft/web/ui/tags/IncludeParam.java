package dcraft.web.ui.tags;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.List;

import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.xml.XNode;

// fragment file
public class IncludeParam extends UIElement {
	public IncludeParam() {
		super("dc.IncludeParam");
	}
	
	@Override
	public UIElement newNode() {
		return new IncludeParam();
	}

	@Override
	public void expand(WeakReference<UIWork> work) {
		Collection<XNode> nodes = this.getUIParam(this.getAttribute("Name"));
		
		if (nodes != null) {
			for (XNode n : nodes)
				this.add(n);
		}
		
		super.expand(work);
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		super.translateSkip(work, pnodes);
	}
}
