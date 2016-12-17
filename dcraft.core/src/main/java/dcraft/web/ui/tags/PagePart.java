package dcraft.web.ui.tags;

import java.lang.ref.WeakReference;
import java.util.List;

import dcraft.cms.feed.core.FeedIndexer;
import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.xml.XElement;
import dcraft.xml.XNode;

public class PagePart extends UIElement {
	protected boolean editable = false;
	
	public boolean isCmsEditable() {
		return this.editable;
	}
	
	public PagePart() {
		super("dc.PagePart");
	}
	
	@Override
	public UIElement newNode() {
		return new PagePart();
	}
	
	@Override
	public void build(WeakReference<UIWork> work) {
		if (this.hasNotEmptyAttribute("Channel")) {
			XElement channelDef = FeedIndexer.findChannel(this.getAttribute("Channel")); 
			
			if (channelDef != null) {
				this.withAttribute("AuthTags",
						channelDef.getAttribute("AuthTags", "Editor,Admin,Developer"));
				
				this.editable = true;
			}
		}
		
		if (this.editable)
			this.with(new Button("dcmi.PartButton")
				.withClass("dcuiPartButton", "dcuiCmsi")
				.withAttribute("Icon", this.hasClass("dc-part-basic") ? "fa-i-cursor" : "fa-cog")
			);
		
		
		super.build(work);
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		// don't change my identity until after the scripts run
		this.setName("div");

		this
			.withClass("dc-part")
			.withAttribute("id", this.getAttribute("For"))
			.withAttribute("lang", this.getAttribute("Lang"))
			.withAttribute("data-dccms-edit", this.getAttribute("AuthTags"))
			.withAttribute("data-dccms-channel", this.getAttribute("Channel"))
			.withAttribute("data-dccms-path", this.getAttribute("Path"));
		
		if (this.hasNotEmptyAttribute("AuthTags"))
			this.withClass("dcm-cms-editable");
		
		super.translate(work, pnodes);
	}
}
