package dcraft.web.ui.tags.form;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map.Entry;

import dcraft.util.HexUtil;
import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.xml.XElement;
import dcraft.xml.XNode;

public class RadioControl extends UIElement {
	static public void enhanceField(CoreField fld, UIElement grp) {
		if (fld.hasNotEmptyAttribute("Name"))
			grp.withAttribute("data-dcf-name", fld.getAttribute("Name"));
		
		if (fld.hasNotEmptyAttribute("Record"))
			grp.withAttribute("data-dcf-record", fld.getAttribute("Record"));
		
		if (fld.hasNotEmptyAttribute("Required"))
			grp.withAttribute("data-dcf-required", fld.getAttribute("Required"));
		
		if ("true".equals(grp.getAttribute("data-dcf-required")))
			fld.withClass("dc-pui-required");
		
		if (fld.hasNotEmptyAttribute("DataType"))
			grp.withAttribute("data-dcf-data-type", fld.getAttribute("DataType"));
		
		if (fld.hasNotEmptyAttribute("Pattern"))
			grp.withAttribute("data-dcf-pattern", fld.getAttribute("Pattern"));
	}
	
	static public XElement fromRadioField(CoreField fld, UIElement input) {
		RadioControl ic = (input instanceof RadioControl) ? (RadioControl) input : new RadioControl();

		ic.withAttribute("type", "radio");
		
		if (! input.hasNotEmptyAttribute("value") && input.hasNotEmptyAttribute("Value"))
			ic.withAttribute("value", input.getAttribute("Value"));
		
		if (! input.hasNotEmptyAttribute("id")) 
			ic.withAttribute("id", fld.fieldid + "-" + HexUtil.encodeHex(ic.getAttribute("value")));
		
		if (! input.hasNotEmptyAttribute("name") && fld.hasNotEmptyAttribute("Name"))
			ic.withAttribute("name", fld.getAttribute("Name"));
		
		if (! input.hasNotEmptyAttribute("readonly") && fld.hasNotEmptyAttribute("readonly"))
			ic.withAttribute("readonly", fld.getAttribute("readonly"));
		
		if (! input.hasNotEmptyAttribute("disabled") && fld.hasNotEmptyAttribute("disabled"))
			ic.withAttribute("disabled", fld.getAttribute("disabled"));
		
		// copy attributes over, only if not the same object
		if (ic != input) {
			for (Entry<String, String> entry : input.getAttributes().entrySet()) {
				if (Character.isLowerCase(entry.getKey().charAt(0))) {
					ic.withAttribute(entry.getKey(), entry.getValue());
				}
			}
		}
		
		return new UIElement("div")
				.withClass("dc-pui-radio")
				.with(ic)
				.with(new UIElement("label")
					.withClass("dc-pui-input-button")
					.withAttribute("for", ic.getAttribute("id"))
					.with(new UIElement("i").withClass("fa fa-circle").withAttribute("aria-hidden", "true"))
					.with(new UIElement("i").withClass("fa fa-check").withAttribute("aria-hidden", "true"))
					.withText(input.getAttribute("Label"))
				);
		
		/*
					<div class="dc-pui-radio">
						<input type="radio" id="comm1" name="CertInterest" value="No" />
						<label for="comm1" class="dc-pui-input-button"><i class="fa fa-circle" aria-hidden="true"></i> Not Interested</label>
					</div>
		 * 
		 */
	}
		
	public RadioControl() {
		super("dcf.RadioButton");
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		//String opname = this.getName();
		
		this.setName("input");
		
		super.translate(work, pnodes);
	}
}
