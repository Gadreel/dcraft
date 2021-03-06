package dcraft.web.ui.tags;

import java.lang.ref.WeakReference;
import java.util.List;

import dcraft.util.StringUtil;
import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.xml.XNode;

public class Link extends UIElement {
	public Link() {
		super("dc.Link");
	}
	
	@Override
	public UIElement newNode() {
		return new Link();
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		String to = this.getAttribute("To", "#");
		String label = this.getAttribute("Label");
		String icon = this.getAttribute("Icon");
		String click = this.getAttribute("Click");
		String page = this.getAttribute("Page");
		
		if (StringUtil.isNotEmpty(label))
			this.withText(label);
		else if (StringUtil.isNotEmpty(icon))
			this.with(new UIElement("i").withAttribute("class", "fa " + icon));
		
		this
			.withAttribute("href", StringUtil.isNotEmpty(page) ? page : to)
			.withAttribute("data-dc-enhance", "true")
			.withAttribute("data-dc-tag", this.getName());
		
		this.setName("a");
		
		if (StringUtil.isNotEmpty(page))
			this.withAttribute("data-dc-page", page);
		
		if (StringUtil.isNotEmpty(click))
			this.withAttribute("data-dc-click", click);
		
		super.translate(work, pnodes);
    }
}
