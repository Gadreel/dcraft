package dcraft.web.ui.tags;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import dcraft.util.StringUtil;
import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.xml.XElement;
import dcraft.xml.XNode;

public class PagePanel extends UIElement {
	public PagePanel() {
		super("dc.PagePanel");
	}
	
	@Override
	public UIElement newNode() {
		return new PagePanel();
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		this
			.withAttribute("data-dc-enhance", "true")
			.withAttribute("data-dc-tag", this.getName());
	
		// the children will move into the body, so clear out our child list
		List<XNode> hiddenchildren = this.children;
		
		this.children = new ArrayList<>();

		String title = "@val|PageTitle@";
		
		String id = this.getAttribute("id");
		
		this.setName("div");
		
		// Default, Primary, Success, Info, Warning, Danger
		String scope = this.getAttribute("Scope", "Primary").toLowerCase();
		
		this.withClass("dc-pui-panel", "dc-pui-panel-" + scope, "dc-pui-panel-page");
		
		this.with(new UIElement("div")
				.withAttribute("class", "dc-pui-panel-heading")
				.with(new UIElement("h5").withText(title))
				.with(new Link().withClass("dc-pui-panel-header-btn", "dcui-pagepanel-close").withAttribute("Icon", "fa-times"))
				.with(new Link().withClass("dc-pui-panel-header-btn", "dcui-pagepanel-help").withAttribute("Icon", "fa-question"))
				.with(new Link().withClass("dc-pui-panel-header-btn", "dcui-pagepanel-menu").withAttribute("Icon", "fa-bars"))
				.with(new Link().withClass("dc-pui-panel-header-btn", "dcui-pagepanel-back").withAttribute("Icon", "fa-chevron-left"))
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
