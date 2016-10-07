package dcraft.web.ui.tags.form;

import dcraft.web.ui.UIElement;

public class TextArea extends CoreField {
	@Override
	public void addControl() {
		InputControl input = InputControl.fromField(this, new InputControl());

		this.with(new UIElement("div")
			.withClass("dc-pui-control", "dc-pui-input-group")
			.with(input)
		);
		
		/*
			<div class="dc-pui-control dc-pui-input-group">
				<textarea id="comment" name="comment" class="dc-pui-textarea"></textarea>
			</div>
		*/		
	}
}
