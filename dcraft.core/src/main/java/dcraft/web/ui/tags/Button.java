package dcraft.web.ui.tags;

import java.lang.ref.WeakReference;
import java.util.List;

import dcraft.util.StringUtil;
import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.xml.XNode;

public class Button extends UIElement {
	public Button() {
		super("dc.Button");
	}
	
	public Button(String btnname) {
		super(btnname);
	}
	
	@Override
	public UIElement newNode() {
		return new Button();
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
			.withClass("pure-button")
			.withAttribute("href", StringUtil.isNotEmpty(page) ? page : to)
			.withAttribute("data-dc-enhance", "true")
			.withAttribute("data-dc-tag", this.getName());
		
		// Default, Primary, Selected (TODO Success, Info, Warning, Danger)
		String scope = this.getAttribute("Scope", "Default").toLowerCase();
		
		this.withClass("pure-button-" + scope);
		
		if (this.hasNotEmptyAttribute("To"))
			this.withAttribute("data-dc-to", to);
		
		if (StringUtil.isNotEmpty(page))
			this.withAttribute("data-dc-page", page);
		
		if (StringUtil.isNotEmpty(click))
			this.withAttribute("data-dc-click", click);
		
    	if (this.getName().startsWith("dc.Wide")) {
    		this
    			.withClass("pure-button-wide");		// TODO wide
    	}
    	
		this.setName("a");
		
		super.translate(work, pnodes);
    }
}
