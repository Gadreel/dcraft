package dcraft.web.ui.tags.form;

import dcraft.web.ui.UIElement;

public class SelectField extends CoreField {
	@Override
	public void addControl() {
		InputControl input = InputControl.fromField(this, new InputControl());
		
		// copy the options over into the control
		input.replaceChildren(this.fieldinfo);

		this.with(new UIElement("div")
			.withClass("dc-pui-control", "dc-pui-input-group")
			.with(input)
		);
		
		/*
			<div class="dc-pui-control dc-pui-input-group">
				<select id="state">
					<option>AL</option>
					<option>CA</option>
					<option>IL</option>
				</select>		
			</div>
		*/		
	}
}
