package dcraft.web.ui.tags;

import java.lang.ref.WeakReference;
import java.util.List;

import dcraft.util.StringUtil;
import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.xml.XNode;

public class Callout extends UIElement {
	public Callout() {
		super("dc.Callout");
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		this.setName("div");
		
		// Info, Warning, Danger
		String scope = this.getAttribute("Scope", "Info").toLowerCase();
		
		this.setAttribute("class", this.getAttribute("class", "") + " dc-pui-callout dc-pui-callout-" + scope);
		
		String title = this.getAttribute("Title");
		
		if (StringUtil.isNotEmpty(title))
			this.add(0, new UIElement("h4")
				.withText(title)
			);
		
		super.translate(work, pnodes);
    }
}
