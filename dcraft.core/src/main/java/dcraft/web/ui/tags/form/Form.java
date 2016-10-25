package dcraft.web.ui.tags.form;

import java.lang.ref.WeakReference;
import java.util.List;

import dcraft.session.Session;
import dcraft.struct.Struct;
import dcraft.util.StringUtil;
import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.xml.XNode;

public class Form extends UIElement {
	public Form() {
		super("dcf.Form");
	}
	
	@Override
	public void build(WeakReference<UIWork> work) {
		String name = this.getAttribute("Name");
		
		// no name?  assign a temp name
		if (StringUtil.isEmpty(name))
			name = StringUtil.buildSimpleCode(12);
		
		this
			.withAttribute("data-dcf-name", name);
		
		// to avoid too many duplicates, lets not connect form to an auto assigned id
		//	.withAttribute("id", "frm" + name);
		
		if (! this.hasNotEmptyAttribute("id")) 
			this.withAttribute("id", "gen" + Session.nextUUId());
		
		if (this.hasNotEmptyAttribute("RecordOrder"))
			this.withAttribute("data-dcf-record-order", this.getAttribute("RecordOrder"));
		
		if (this.hasNotEmptyAttribute("RecordType")) {
			this.withAttribute("data-dcf-record-type", this.getAttribute("RecordType"));

			// automatically require the form's data types
			this.getRoot().with(new UIElement("dc.Require")
					.withAttribute("Types", this.getAttribute("RecordType")));
		}

		if (this.hasNotEmptyAttribute("Focus"))
			this.withAttribute("data-dcf-focus", this.getAttribute("Focus"));

		if (this.hasNotEmptyAttribute("Prefix"))
			this.withAttribute("data-dcf-prefix", this.getAttribute("Prefix"));

		if (this.hasNotEmptyAttribute("AlwaysNew"))
			this.withAttribute("data-dcf-always-new", this.getAttribute("AlwaysNew").toLowerCase());
		
		super.build(work);
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		this
			.withAttribute("data-dc-enhance", "true")
			.withAttribute("data-dc-tag", this.getName());
		
		this.setName("form");
		
		this.withClass("dc-pui-form");
		
		if (Struct.objectToBooleanOrFalse(this.getAttribute("Stacked")))
			this.withClass("dc-pui-stacked-form");
		
		super.translate(work, pnodes);
	}
}
