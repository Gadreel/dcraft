package dcraft.web.ui.tags.cms;

import java.lang.ref.WeakReference;
import java.util.List;

import dcraft.cms.util.CatalogUtil;
import dcraft.util.StringUtil;
import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.xml.XElement;
import dcraft.xml.XNode;

public class TwitterTimelineLoader extends UIElement {
	public TwitterTimelineLoader() {
		super("dcm.TwitterTimelineLoader");
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		this
			.withAttribute("data-dc-enhance", "true")
			.withAttribute("data-dc-tag", this.getName());
		
		this.setName("div");
		
		this.withClass("dcm-tw-listing");
		
		String alternate = this.getAttribute("Alternate");
		
		String name = "Twitter";
		
		if (StringUtil.isNotEmpty(alternate))
			name = name + "-" + alternate;
		
		XElement twsettings = CatalogUtil.getSettings(name);
		
		if (twsettings != null) {
			String scrname = twsettings.getAttribute("ScreenName");
			
			if (StringUtil.isNotEmpty(scrname)) {
				this
					.with(new UIElement("a")
						.withClass("twitter-timeline")
						.withAttribute("href", "https://twitter.com/" + scrname)
						.withAttribute("target", "_blank")
						.withText("Tweets by @" + scrname)
					);
			}
		}
		
		super.translate(work, pnodes);
    }
}
