package dcraft.web.ui.tags.form;

import dcraft.web.ui.UIElement;
import dcraft.xml.XElement;

public class ButtonGroup extends CoreField {
	@Override
	public void addControl() {
		/*
					
			<dcf.ButtonGroup ...			from CoreField
				Compact="true"
			>
				<Button Icon="square" Label="nnn" Click="aaa" />
				<Button Icon="cricle" />
				<Button Icon="star" />
				<Button Icon="bell" />
			</dcf.ButtonGroup>
		 * 
		 * 
			<div class="dc-pui-control dc-pui-input-group">
				<span class="dc-pui-input-group-addon dc-pui-addon-button"><i class="fa fa-square"></i></span>
				<span class="dc-pui-input-group-addon dc-pui-addon-button"><i class="fa fa-circle"></i></span>
				<span class="dc-pui-input-group-addon dc-pui-addon-button"><i class="fa fa-star"></i></span>
				<span class="dc-pui-input-group-addon dc-pui-addon-button"><i class="fa fa-bell"></i></span>
			</div>

			<div class="dc-pui-control dc-pui-button-group">
				<span class="dc-pui-input-group-addon dc-pui-addon-button"><i class="fa fa-square"></i></span>
				<span class="dc-pui-input-group-addon dc-pui-addon-button"><i class="fa fa-circle"></i></span>
				<span class="dc-pui-input-group-addon dc-pui-addon-button"><i class="fa fa-star"></i></span>
				<span class="dc-pui-input-group-addon dc-pui-addon-button"><i class="fa fa-bell"></i></span>
			</div>
		 * 
		 * 
		 * 
		 */
		
		UIElement grp = new UIElement("div")
			.withClass("dc-pui-control", 
				this.getAttributeAsBooleanOrFalse("Compact") ?  "dc-pui-button-group" : "dc-pui-input-group");
		
		for (XElement el : this.fieldinfo.selectAll("*")) {
			String ename = el.getName();
			
			if ("Glyph".equals(ename) || "Info".equals(ename) || "Button".equals(ename)) {
				UIElement ic = InputControl.fromGylph((UIElement) el);
				
				// not helpful in this context
				ic.withoutClass("dc-pui-addon-glyph-button");
				
				grp.with(ic);
			}
		}

		this.with(grp);
	}
}
