package dcraft.web.ui.tags.form;

import java.lang.ref.WeakReference;
import java.util.List;

import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.xml.XNode;

public class YesNo extends RadioGroup {
	@Override
	public void addControl() {
		this.withAttribute("DataType", "Boolean");		//	always
		
		this.fieldinfo.with(
			new UIElement("RadioButton")
				.withAttribute("Label", "Yes")
				.withAttribute("Value", "true")
		).with(
			new UIElement("RadioButton")
				.withAttribute("Label", "No")
				.withAttribute("Value", "false")
		);
		
		/*
				<dcf.RadioGroup Label="Radio:" Name="Internship">
				   <RadioButton Value="true" Label="Yes" />
				   <RadioButton Value="false" Label="No" />
				</dcf.RadioGroup>
		*/		
		
		super.addControl();
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		super.translate(work, pnodes);
		
		this
			.withAttribute("data-dc-enhance", "true")
			.withAttribute("data-dc-tag", "dcf.YesNo");
	}
}
