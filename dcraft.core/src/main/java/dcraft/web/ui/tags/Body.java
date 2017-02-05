package dcraft.web.ui.tags;

import java.lang.ref.WeakReference;
import java.util.List;

import dcraft.web.core.IOutputContext;
import dcraft.web.core.WebContext;
import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.xml.XNode;

public class Body extends UIElement {
	public Body() {
		super("dc.Body");
	}
	
	@Override
	public UIElement newNode() {
		return new Body();
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		UIElement bodel = this;
		
		bodel = new UIElement();
		
		bodel.replace(this);
		
		IOutputContext ctx = work.get().getContext();
		
		if ((ctx instanceof WebContext) && ! ((WebContext) ctx).isDynamic()) {
			bodel
				.withClass("dcuiLayer")
				.withAttribute("id", "dcuiMain");
		}		
		else {
			bodel
				.withClass("dcuiPage")
			.withAttribute("id", "dcuiMain");
		}
		
		bodel.setName("div");		// set after replace so name sticks
		
		this.clear();
		
		this.with(bodel);
		
		// don't change my identity until after the scripts run
		this.setName("body");

		super.translate(work, pnodes);
	}
}
