package dcraft.web.ui.tags.form;

import dcraft.web.ui.UIElement;
import dcraft.xml.XElement;

public class CheckGroup extends CoreField {
	@Override
	public void addControl() {
		UIElement grp = new UIElement("div")
			.withClass("dc-pui-control");
		
		grp
			.withAttribute("id", this.fieldid)
			.withAttribute("data-dc-enhance", "true")
			.withAttribute("data-dc-tag", this.getName());
		
		if ("dcf.HorizCheckGroup".equals(this.getName()))
			grp.withClass("dc-pui-controlgroup-horizontal");
		else
			grp.withClass("dc-pui-controlgroup-vertical");
		
		CheckControl.enhanceField(this, grp);
		
		if ("dcf.Checkbox".equals(this.getName())) {
			this.removeAttribute("Label");
			
			if (this.hasNotEmptyAttribute("LongLabel"))
				this.withAttribute("Label", this.getAttribute("LongLabel"));
			
			this.withAttribute("Value", "true");	// always true if checked
			this.withAttribute("DataType", "Boolean");
			
			grp.with(CheckControl.fromCheckField(this, this));
		}
		else {
			for (XElement el : this.fieldinfo.selectAll("Checkbox")) 
				grp.with(CheckControl.fromCheckField(this, (UIElement) el));
		}
		
		this.with(grp);
	}
}
