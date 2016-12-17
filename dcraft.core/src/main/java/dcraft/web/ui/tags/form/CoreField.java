package dcraft.web.ui.tags.form;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import dcraft.session.Session;
import dcraft.struct.Struct;
import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.xml.XElement;
import dcraft.xml.XNode;

abstract public class CoreField extends UIElement {
	protected String fieldid = null;
	protected UIElement fieldinfo = null;
	
	abstract public void addControl();
	
	/**
	 * call this from subclasses before making child updates in
	 * expand
	 * 
	 * no field children are directly used in the final document, 
	 * move them out and make room for the real field children
	 */
	public void initFieldInfo() {
		if (this.fieldinfo == null) {
			this.fieldinfo = new UIElement(this.tagName);
			this.fieldinfo.replace(this);
			this.children = new CopyOnWriteArrayList<XNode>();
		}
	}
	
	@Override
	public void expand(WeakReference<UIWork> work) {
		this.initFieldInfo();	// should already be called by subclass
		
		if (this.hasNotEmptyAttribute("id")) 
			this.fieldid = this.getAttribute("id");
		else 
			this.fieldid = "gen" + Session.nextUUId();
		
		boolean usespacer = ! this.getAttributeAsBooleanOrFalse("NoSpacer");
		
		if (this.hasNotEmptyAttribute("Label"))
			this.with(new UIElement("div")
				.withClass("dc-pui-label")
				.with(new UIElement("label")
					.withAttribute("for", this.fieldid)
					.withText(this.getAttribute("Label") + ":")
				)
			)
			.withAttribute("data-dcf-label", this.getAttribute("Label"));
		else if (usespacer)
			this.with(new UIElement("div").withClass("dc-pui-spacer"));
		
		String cmpt = Struct.objectToBooleanOrFalse(this.fieldinfo.getAttribute("ValidateButton")) ? "dc-pui-compact" : null;
		
		if ((this.fieldinfo.find("Instructions") != null) || this.hasNotEmptyAttribute("Instructions")) {
			UIElement inst = (UIElement) new UIElement("div")
					.withAttribute("class", "dc-pui-message dc-pui-message-info");
			
			XElement instsrc = this.fieldinfo.find("Instructions");
			
			if (instsrc != null)
				inst.replaceChildren(instsrc);
			else
				inst.withText(this.getAttribute("Instructions"));
			
			this.with(new UIElement("div")
				.withClass("dc-pui-control")
				.withClass(cmpt)
				.with(inst)
			);
			
			// add spacer before input
			if (usespacer)
				this.with(new UIElement("div").withClass("dc-pui-spacer").withClass(cmpt));
		}
		
		this.addControl();
		
		// add spacer before error message 
		if (usespacer)
			this.with(new UIElement("div").withClass("dc-pui-spacer", "dc-pui-valid-hidden").withClass(cmpt));
		
		UIElement inst = (UIElement) new UIElement("div")
				.withClass("dc-pui-message", "dc-pui-message-danger");
		
		if ((this.fieldinfo.find("Message") != null) || this.hasNotEmptyAttribute("Message")) {
			XElement instsrc = this.fieldinfo.find("Message");
			
			if (instsrc != null)
				inst.replaceChildren(instsrc);
			else
				inst.withText(this.getAttribute("Message"));
			
			inst.withClass("dc-pui-fixed-message");
		}
		
		this.with(new UIElement("div")
			.withClass("dc-pui-control", "dc-pui-valid-hidden")
			.withClass(cmpt)
			.with(inst)
		);

		super.expand(work);
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		this.setName("div");
		
		this
			.withClass("dc-pui-field")
			.withAttribute("id", "fld" + this.fieldid);
		
		Form frm = this.getForm();
		
		if (this.getAttributeAsBooleanOrFalse("Invalid") || ((frm != null) && frm.getAttributeAsBooleanOrFalse("Invalid")))
			this.withClass("dc-pui-invalid");
		
		if (this.getAttributeAsBooleanOrFalse("Stacked") || ((frm != null) && frm.getAttributeAsBooleanOrFalse("Stacked")))
			this.withClass("dc-pui-field-stacked");
		
		super.translate(work, pnodes);
	}
}
