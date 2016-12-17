package dcraft.web.ui.tags.cms;

import java.lang.ref.WeakReference;
import java.util.List;

import dcraft.cms.feed.core.FeedAdapter;
import dcraft.lang.op.OperationContext;
import dcraft.struct.RecordStruct;
import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.xml.XNode;

public class FeedParams extends UIElement {
	//protected RecordStruct feeddata = null;
	protected FeedAdapter adapter = null;
	
	public void setFeedData(RecordStruct v) {
		this.adapter = FeedAdapter.from(v);
	}
	
	public void setFeedData(FeedAdapter v) {
		this.adapter = v;
	}
	
	public FeedParams() {
		super("dcm.FeedParams");
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		super.translateSkip(work, pnodes);
	}
	
	@Override
	public String getParam(String name) {
		if (name.startsWith("Field|")) {
			String v = this.adapter.getField(OperationContext.get(), name.substring(6));
			return (v == null) ? "" : v;
		}
		
		if (name.startsWith("Part|")) {
			String v= this.adapter.getPart(OperationContext.get(), name.substring(5), false);		// TODO someday support preview
			return (v == null) ? "" : v;
		}
		
		if (name.equals("Path")) {
			String v= this.adapter.getPath();
			return (v == null) ? "" : v;
		}
		
		if (name.equals("Tags")) {
			String v= this.adapter.getTags();
			return (v == null) ? "" : v;
		}
		
		return super.getParam(name);
	}
}
