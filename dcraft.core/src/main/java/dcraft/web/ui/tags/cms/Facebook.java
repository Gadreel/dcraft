package dcraft.web.ui.tags.cms;

import java.lang.ref.WeakReference;
import java.util.List;

import dcraft.util.StringUtil;
import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.xml.XNode;

public class Facebook extends UIElement {
	public Facebook() {
		super("dcm.Facebook");
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		this
			.withAttribute("data-dc-enhance", "true")
			.withAttribute("data-dc-tag", this.getName());
		
		this.setName("div");
		
		this.withClass("dcm-fb-listing");
		
		String alternate = this.getAttribute("Alternate");
		
		if (StringUtil.isNotEmpty(alternate))
			this.withAttribute("data-dcm-facebook-alternate", alternate);
		
		String count = this.getAttribute("Count");
		
		if (StringUtil.isNotEmpty(count))
			this.withAttribute("data-dcm-facebook-count", count);
		
		super.translate(work, pnodes);
    }
}
