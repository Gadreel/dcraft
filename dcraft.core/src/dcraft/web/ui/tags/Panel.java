package dcraft.web.ui.tags;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import dcraft.util.StringUtil;
import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.xml.XElement;
import dcraft.xml.XNode;

public class Panel extends UIElement {
	public Panel() {
		super("dc.Panel");
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		// the children will move into the body, so clear out our child list
		List<XNode> hiddenchildren = this.children;
		
		this.children = new ArrayList<>();

		String title = this.getAttribute("Title");
		
		String id = this.getAttribute("id");
		
		this.setName("div");
		
		// Default, Primary, Success, Info, Warning, Danger
		String scope = this.getAttribute("Scope", "Primary").toLowerCase();
		
		this.setAttribute("class", this.getAttribute("class", "") + " dc-pui-panel dc-pui-panel-" + scope);
		
		this.with(new UIElement("div")
				.withAttribute("class", "dc-pui-panel-heading")
				.withText(title)
			);
		
		XElement bodyui = new UIElement("div")
				.withAttribute("class", "dc-pui-panel-body");	
		
		if (StringUtil.isNotEmpty(id))
			bodyui.withAttribute("id", id + "Body");

		if (hiddenchildren != null) {
			for (XNode n : hiddenchildren)
				bodyui.add(n);
		}
		
		this.with(bodyui);
		
		super.translate(work, pnodes);
    }
}
