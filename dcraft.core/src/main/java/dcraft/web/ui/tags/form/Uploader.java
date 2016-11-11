package dcraft.web.ui.tags.form;

import dcraft.web.ui.UIElement;

public class Uploader extends CoreField {
	@Override
	public void addControl() {
		UIElement grp = new UIElement("div")
			.withClass("dc-pui-control dc-pui-uploader");
		
		grp
			.withAttribute("id", this.fieldid)
			.withAttribute("data-dc-enhance", "true")
			.withAttribute("data-dc-tag", this.getName())
			.with(new UIElement("div")
					.withClass("dc-pui-uploader-file")
					.with(new UIElement("input")
							.withAttribute("type", "file")
							.withAttribute("capture", "capture")
							.withAttribute("multiple", "multiple")
					)
			)
			.with(new UIElement("div")
					.withClass("dc-pui-uploader-list-area dc-pui-message dc-pui-message-info")
					.with(
						new UIElement("div")
							.withClass("dc-pui-uploader-list-header")	
							.withText("Selected Files: ")
					)
					.with(
						new UIElement("div")
							.withClass("dc-pui-uploader-listing")	
					)
			);
		
		RadioControl.enhanceField(this, grp);

		this.with(grp);
		
		/*
				<dcf.Uploader Label="Files" Name="Attachments" />
				
				<div class="dc-pui-control dc-pui-uploader">
					<div class="dc-pui-uploader-file">
						<input type="file" />
					</div>
					<div class="dc-pui-uploader-list">
					</div>
				</div>
		*/		
	}
}
