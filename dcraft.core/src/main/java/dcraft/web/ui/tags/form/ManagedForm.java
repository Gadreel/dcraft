package dcraft.web.ui.tags.form;

import java.lang.ref.WeakReference;

import dcraft.web.ui.UIWork;

public class ManagedForm extends Form {
	public ManagedForm() {
		super("dcf.ManagedForm");
	}
	
	@Override
	public void build(WeakReference<UIWork> work) {
		this.removeAttribute("RecordOrder");		// only Default record is allowed

		this.withAttribute("data-dcf-managed", "true");

		// defaults to true so that form prefills will still be saved
		if (! this.hasNotEmptyAttribute("AlwaysNew"))
			this.withAttribute("AlwaysNew", "true");
		
		super.build(work);
	}
}
