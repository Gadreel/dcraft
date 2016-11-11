package dcraft.web.ui.tags.cms;

import java.lang.ref.WeakReference;
import java.util.List;

import dcraft.cms.util.CatalogUtil;
import dcraft.util.StringUtil;
import dcraft.web.ui.UIWork;
import dcraft.web.ui.tags.Button;
import dcraft.xml.XElement;
import dcraft.xml.XNode;

public class SocialMediaIcon extends Button {
	public SocialMediaIcon() {
		super("dcm.SocialMediaIcon");
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		String formedia = this.getAttribute("For");
		
		if (StringUtil.isNotEmpty(formedia)) {
			XElement setting = CatalogUtil.getSettings(formedia, this.getAttribute("Alternate"));
			
			if ((setting != null) && ! this.hasAttribute("To")) 
				this.withAttribute("To", setting.getAttribute("Url"));
				
			if (! this.hasAttribute("Icon"))
				this.withAttribute("Icon", "fa-" + formedia.toLowerCase());
		}
		
		this.withClass("dcm-social-media-icon");
		
		this.setName("dc.Button");
		
		super.translate(work, pnodes);
	}
}
