package dcraft.web.ui.tags.form;

import java.util.ArrayList;
import java.util.List;

import dcraft.struct.Struct;
import dcraft.web.ui.UIElement;
import dcraft.xml.XElement;

public class InputField extends CoreField {
	@Override
	public void addControl() {
		if (Struct.objectToBooleanOrFalse(this.fieldinfo.getAttribute("ValidateButton")))
			this.fieldinfo.with(new UIElement("Button")
				.withAttribute("Icon", "fa-info-circle")
				.withAttribute("InvalidIcon", "fa-warning")
				.withAttribute("Flag", "true")
				.withAttribute("data-dc-enhance", "true")
				.withAttribute("data-dc-tag", "dcf.ValidateButton")
			);
		
		List<UIElement> before = new ArrayList<>();
		UIElement input = null;
		List<UIElement> after = new ArrayList<>();
		
		List<UIElement> curr = before;
		
		for (XElement el : this.fieldinfo.selectAll("*")) {
			String ename = el.getName();
			
			if ("Glyph".equals(ename) || "Info".equals(ename) || "Button".equals(ename)) {
				curr.add(InputControl.fromGylph((UIElement) el));
			}
			else if ("Input".equals(ename)) {
				input = InputControl.fromField(this, (UIElement) el);
				curr = after;
			}
		}
		
		if (input == null) {
			input = InputControl.fromField(this, new InputControl());
			// if no input found then all glyphs become after
			after = before;
			before = null;
		}
		
		// TODO support ValidIcon - into after

		this.with(new UIElement("div")
			.withClass("dc-pui-control", "dc-pui-input-group")
			.withAll(before)
			.with(input)
			.withAll(after)
		);
		
		/*
		<div class="dc-pui-control dc-pui-input-group">
			<input id="name" type="text" placeholder="Username"  />
		</div>
		*/		
	}
}
