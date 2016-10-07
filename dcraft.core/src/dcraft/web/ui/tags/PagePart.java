package dcraft.web.ui.tags;

import java.lang.ref.WeakReference;
import java.util.List;

import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.xml.XNode;

public class PagePart extends UIElement {
	public PagePart() {
		super("dc.PagePart");
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		// don't change my identity until after the scripts run
		this.setName("div");
		
		if ("image".equals(this.getAttribute("data-dccms-editor")))
			this.setName("img");
		
		this.setAttribute("id", this.getAttribute("For"));
		
		this.removeAttribute("For");
		this.removeAttribute("Format");
		this.removeAttribute("Locale");
		
		super.translate(work, pnodes);
	}
}
