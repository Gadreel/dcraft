package dcraft.web.ui.tags;

import java.lang.ref.WeakReference;
import java.util.List;

import dcraft.util.StringUtil;
import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.xml.XElement;
import dcraft.xml.XNode;

public class Fragment extends UIElement {
	public Fragment() {
		super("dc.Fragment");
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		Html p = (Html)this.getRoot();
		
		if (p != null) {
			String pc = p.getHiddenAttribute("PageClass");
			
			if (StringUtil.isNotEmpty(pc)) 
				this.withClass(pc);
			
			for (XNode rel : p.getHiddenChildren()) {
				if ((rel instanceof XElement) && ((XElement)rel).getName().equals("dc.Require") && ((XElement)rel).hasAttribute("Class")) 
					this.withClass(((XElement)rel).getAttribute("Class", ""));
			}
		}

		// don't change my identity until after the scripts run
		this.setName("body");
		
		super.translate(work, pnodes);
	}
}
