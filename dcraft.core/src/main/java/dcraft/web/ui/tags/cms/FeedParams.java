package dcraft.web.ui.tags.cms;

import java.lang.ref.WeakReference;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import dcraft.cms.feed.core.FeedAdapter;
import dcraft.lang.op.OperationContext;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.xml.XNode;

public class FeedParams extends UIElement {
	//protected RecordStruct feeddata = null;
	protected FeedAdapter adapter = null;
	
	public void setFeedData(RecordStruct v) {
		this.adapter = FeedAdapter.from(OperationContext.get(), v);
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
			String[] pieces = name.split("\\|");
			
			String v = this.adapter.getField(OperationContext.get(), pieces[1]);
			
			v = (v == null) ? "" : v;
			
			if (pieces.length > 2) {
				String fmt = pieces[2];
				
				if ("dtfmt".equals(fmt)) {
					if (pieces.length > 3) {
						DateTime dtv = Struct.objectToDateTime(v);
						
						//System.out.println("zone: " + OperationContext.get().getWorkingChronologyDefinition());
						
						String pattern = pieces[3].replaceAll("\\\\s", " ");
						DateTimeFormatter dtfmt = DateTimeFormat.forPattern(pattern).withZone(OperationContext.get().getWorkingChronologyDefinition());
						
						v = dtfmt.print(dtv);
					}
				}
			}
			
			return v;
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
