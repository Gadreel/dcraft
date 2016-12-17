package dcraft.web.ui.tags.form;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map.Entry;

import dcraft.session.Session;
import dcraft.util.StringUtil;
import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.xml.XNode;

public class InputControl extends UIElement {
	static public UIElement fromGylph(UIElement input) {
		/*
				<span class="dc-pui-input-group-addon dc-pui-addon-glyph"><i class="fa fa-star"></i></span>
				<span class="dc-pui-input-group-addon dc-pui-addon-info">@designcraft.io</span>
				<span class="dc-pui-input-group-addon dc-pui-addon-button dc-pui-addon-glyph-button"><i class="fa fa-bell dc-pui-valid-flag"></i></span>
		 * 
		 * 
				<span class="dc-pui-input-group-addon dc-pui-addon-info"><i class="fa fa-square"></i></span>
				<span class="dc-pui-input-group-addon dc-pui-addon-glyph dc-pui-valid-flag"><i class="fa fa-circle"></i></span>
				<input id="email" type="text" placeholder="Email"  />
				<span class="dc-pui-input-group-addon dc-pui-addon-glyph"><i class="fa fa-star"></i></span>
				<span class="dc-pui-input-group-addon dc-pui-addon-info">@designcraft.io</i></span>
				<span class="dc-pui-input-group-addon dc-pui-addon-button dc-pui-addon-glyph-button"><i class="fa fa-bell dc-pui-valid-flag"></i></span>

		*
				<span class="dc-pui-input-group-addon dc-pui-addon-button"><i class="fa fa-square"></i></span>
				<span class="dc-pui-input-group-addon dc-pui-addon-button"><i class="fa fa-circle"></i></span>
				<span class="dc-pui-input-group-addon dc-pui-addon-button"><i class="fa fa-star"></i></span>
				<span class="dc-pui-input-group-addon dc-pui-addon-button"><i class="fa fa-bell"></i></span>
		*
				<span class="dc-pui-input-group-addon dc-pui-addon-glyph">?</span>
		*
				<span class="dc-pui-input-group-addon dc-pui-addon-button dc-pui-addon-glyph-button"><i class="fa fa-info-circle dc-pui-invalid-hidden"></i><i class="fa fa-warning dc-pui-valid-hidden dc-pui-valid-flag"></i></span>
		*
	<Glyph Label="$" Icon="fa-info-circle" InvalidIcon="warning" Flag="true" />
	<Input />
	<Info Label="$" Icon="fa-info-circle" InvalidIcon="warning" Flag="true" />
	<Button Label="$" Icon="bell" InvalidIcon="warning" Flag="true" Click="aaa" />
		*
		 */

		String gtype = input.getName();
		
		input.withClass("dc-pui-input-group-addon");

		String label = input.getAttribute("Label");
		String icon = input.getAttribute("Icon");
		String invicon = input.getAttribute("InvalidIcon");
		
		if ("Glyph".equals(gtype)) {
			input.withClass("dc-pui-addon-glyph");
		}
		else if ("Button".equals(gtype)) {
			input.withClass("dc-pui-addon-button");
			
			if (StringUtil.isNotEmpty(icon))
				input.withClass("dc-pui-addon-glyph-button");
		}
		else if ("Info".equals(gtype)) {
			input.withClass("dc-pui-addon-info");
		}
		
		if (input.getAttributeAsBooleanOrFalse("Flag"))
			input.withClass("dc-pui-valid-flag");
		
		if (StringUtil.isNotEmpty(icon) && StringUtil.isNotEmpty(invicon)) 
			input
				.with(new UIElement("i").withAttribute("class", "dc-pui-invalid-hidden fa " + icon))
				.with(new UIElement("i").withAttribute("class", "dc-pui-valid-hidden dc-pui-valid-flag fa " + invicon));
		else if (StringUtil.isNotEmpty(icon)) 
			input.with(new UIElement("i").withAttribute("class", "fa " + icon));
		else if (StringUtil.isNotEmpty(label)) 
			input.withText(label);

		String to = input.getAttribute("To", "#");
		String click = input.getAttribute("Click");
		String page = input.getAttribute("Page");
		
		input
			.withAttribute("href", StringUtil.isNotEmpty(page) ? page : to);
		
		if (! input.hasNotEmptyAttribute("data-dc-enhance"))
			input
				.withAttribute("data-dc-enhance", "true")
				.withAttribute("data-dc-tag", "dc.Button");
		
		if (StringUtil.isNotEmpty(page))
			input.withAttribute("data-dc-page", page);
		
		if (StringUtil.isNotEmpty(click))
			input.withAttribute("data-dc-click", click);

		input.setName("span");
		
		return input;
	}
	
	static public InputControl fromField(CoreField fld, UIElement input) {
		InputControl ic = (input instanceof InputControl) ? (InputControl) input : new InputControl();
		
		//Form frm = fld.getForm();
		
		if ("dcf.Password".equals(fld.getName())) {
			ic.withAttribute("type", "password");
		}
		else if ("dcf.Label".equals(fld.getName())) {
			ic.setName("dcf.Label");					// TODO enhance so this works with glyphs
		}
		else if ("dcf.Select".equals(fld.getName())) {
			ic.setName("dcf.Select");					// TODO enhance so this works with glyphs
		}
		else if ("dcf.TextArea".equals(fld.getName())) {
			ic.setName("dcf.TextArea");					
		}
		else if ("dcf.Hidden".equals(fld.getName())) {
			ic.withAttribute("type", "hidden");		// is otherwise just like a text field
		}
		else if (! input.hasNotEmptyAttribute("type")) {
			ic.withAttribute("type", "text");
		}
		
		if ("dcf.MultiText".equals(fld.getName())) {
			if (! input.hasNotEmptyAttribute("id"))
				ic.withAttribute("id", "gen" + Session.nextUUId());
		}
		else if (! input.hasNotEmptyAttribute("id")) {
			ic.withAttribute("id", fld.fieldid);
		}
		
		if (! input.hasNotEmptyAttribute("readonly") && fld.hasNotEmptyAttribute("readonly"))
			ic.withAttribute("readonly", fld.getAttribute("readonly"));
		
		if (! input.hasNotEmptyAttribute("disabled") && fld.hasNotEmptyAttribute("disabled"))
			ic.withAttribute("disabled", fld.getAttribute("disabled"));
		
		if ("dcf.Label".equals(ic.getName())) {
			if (! input.hasNotEmptyAttribute("value") && fld.hasNotEmptyAttribute("value"))
				ic.withAttribute("data-value", fld.getAttribute("value"));
			
			if (input.hasNotEmptyAttribute("value")) {
				ic.withAttribute("data-value", input.getAttribute("value"));
				ic.removeAttribute("value");
			}
			
			if (ic.hasNotEmptyAttribute("data-value")) 
				ic.withText(ic.getAttribute("data-value"));
		}
		else {
			if (! input.hasNotEmptyAttribute("value") && fld.hasNotEmptyAttribute("value"))
				ic.withAttribute("value", fld.getAttribute("value"));
		}
		
		if (! input.hasNotEmptyAttribute("name") && fld.hasNotEmptyAttribute("name"))
			ic.withAttribute("name", fld.getAttribute("name"));
		
		if (! input.hasNotEmptyAttribute("placeholder") && fld.hasNotEmptyAttribute("placeholder"))
			ic.withAttribute("placeholder", fld.getAttribute("placeholder"));
		
		if (input.hasNotEmptyAttribute("Name"))
			ic.withAttribute("data-dcf-name", input.getAttribute("Name"));
		else if (fld.hasNotEmptyAttribute("Name"))
			ic.withAttribute("data-dcf-name", fld.getAttribute("Name"));
		
		if (input.hasNotEmptyAttribute("Record"))
			ic.withAttribute("data-dcf-record", input.getAttribute("Record"));
		else if (fld.hasNotEmptyAttribute("Record"))
			ic.withAttribute("data-dcf-record", fld.getAttribute("Record"));
		
		if (input.hasNotEmptyAttribute("Required"))
			ic.withAttribute("data-dcf-required", input.getAttribute("Required"));
		else if (fld.hasNotEmptyAttribute("Required"))
			ic.withAttribute("data-dcf-required", fld.getAttribute("Required"));
		
		if ("true".equals(ic.getAttribute("data-dcf-required")))
			fld.withClass("dc-pui-required");
		
		if (input.hasNotEmptyAttribute("DataType"))
			ic.withAttribute("data-dcf-data-type", input.getAttribute("DataType"));
		else if (fld.hasNotEmptyAttribute("DataType"))
			ic.withAttribute("data-dcf-data-type", fld.getAttribute("DataType"));
		
		if (input.hasNotEmptyAttribute("Pattern"))
			ic.withAttribute("data-dcf-pattern", input.getAttribute("Pattern"));
		else if (fld.hasNotEmptyAttribute("Pattern"))
			ic.withAttribute("data-dcf-pattern", fld.getAttribute("Pattern"));
		
		if (input.hasNotEmptyAttribute("Size"))
			ic.withClass("dc-pui-input-" + input.getAttribute("Size"));
		
		// copy attributes over, only if not the same object
		if (ic != input) {
			for (Entry<String, String> entry : input.getAttributes().entrySet()) {
				if (Character.isLowerCase(entry.getKey().charAt(0))) {
					ic.withAttribute(entry.getKey(), entry.getValue());
				}
			}
		}
		
		return ic;
	}
	
	public InputControl() {
		super("dcf.Text");
	}

	@Override
	public void build(WeakReference<UIWork> work) {
		if (this.hasNotEmptyAttribute("data-dcf-data-type")) {
			// automatically require the form's data types
			this.getRoot().with(new UIElement("dc.Require")
					.withAttribute("Types", this.getAttribute("data-dcf-data-type")));
		}
		
		super.build(work);
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		this
			.withAttribute("data-dc-enhance", "true")
			.withAttribute("data-dc-tag", this.getName());
		
		String opname = this.getName();
		
		this.setName("input");
		
		if ("dcf.Label".equals(opname))
			this.setName("label");
		
		if ("dcf.Select".equals(opname))
			this.setName("select");
		
		if ("dcf.TextArea".equals(opname)) {
			this.setName("textarea");
			this.withClass("dc-pui-textarea");
		}
		
		super.translate(work, pnodes);
	}
}
