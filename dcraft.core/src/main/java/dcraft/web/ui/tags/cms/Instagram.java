package dcraft.web.ui.tags.cms;

import java.lang.ref.WeakReference;
import java.util.List;

import dcraft.util.StringUtil;
import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.xml.XNode;

public class Instagram extends UIElement {
	public Instagram() {
		super("dcm.Instagram");
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		this
			.withAttribute("data-dc-enhance", "true")
			.withAttribute("data-dc-tag", this.getName());
		
		this.setName("div");
		
		this.withClass("dcm-ig-listing");
		
		String alternate = this.getAttribute("Alternate");
		
		if (StringUtil.isNotEmpty(alternate))
			this.withAttribute("data-dcm-instagram-alternate", alternate);
		
		String count = this.getAttribute("Count");
		
		if (StringUtil.isNotEmpty(count))
			this.withAttribute("data-dcm-instagram-count", count);
		
		super.translate(work, pnodes);
    }
}
