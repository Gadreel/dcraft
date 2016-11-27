package dcraft.web.ui.tags;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map.Entry;

import dcraft.filestore.CommonPath;
import dcraft.hub.Hub;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.OperationObserver;
import dcraft.log.DebugLevel;
import dcraft.web.core.IOutputAdapter;
import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIInitialWork;
import dcraft.web.ui.UIWork;
import dcraft.web.ui.adapter.DynamicOutputAdapter;
import dcraft.work.Task;
import dcraft.xml.XElement;
import dcraft.xml.XNode;

public class IncludeFrag extends UIElement {
	public IncludeFrag() {
		super("dc.IncludeFrag");
	}
	
	@Override
	public void expand(WeakReference<UIWork> work) {
		// expand out the Params (those don't expand their templates though)
		super.expand(work);
		
		// remove the children
		this.clearChildren();
		
    	// find and include the fragment file
		if (this.hasAttribute("Path")) {
			String tpath = this.getAttribute("Path");
			
			CommonPath pp = new CommonPath(tpath + ".html");		
			
			IOutputAdapter sf = work.get().getContext().getSite().getWebsite().findFile(pp, work.get().getContext().isPreview());
			
			if (sf instanceof DynamicOutputAdapter) {
				UIElement layout = ((DynamicOutputAdapter)sf).getSource();
				
				layout.setParent(this);
				
				work.get().incExpand();
				
				// set so that only expanding is done
				UIWork dwork = new UIInitialWork();
				dwork.setContext(work.get().getContext());
				dwork.setRoot(layout);
				
				Task task = Task
					.taskWithSubContext()
					.withLogging(DebugLevel.Warn)
					.withTitle("Working on web fragment: " + pp)
					.withTopic("Web")
					.withObserver(new OperationObserver() {
						@Override
						public void completed(OperationContext ctx) {
							// don't use the dcuif element in our child list, instead pull out the parts that matter
							
							// pull out the merge parts
							layout.mergeWithRoot(work, IncludeFrag.this.getRoot(), false);
							
							// pull out the UI and copy into us, leave dcuif and Skeleton out
							XElement frag = layout.find("dc.Fragment");
							
							if (frag != null) {
								for (Entry<String, String> attr : frag.getAttributes().entrySet())
									if (!IncludeFrag.this.hasAttribute(attr.getKey()))
										IncludeFrag.this.setAttribute(attr.getKey(), attr.getValue());
								
								for (XNode n : frag.getChildren()) {
									//if (n instanceof ServerScript)
									//	continue;
									
									IncludeFrag.this.add(n);
								}
							}
							
							// finally done with this frag
							work.get().decExpand();
						}
					})
					.withWork(dwork);
				
				Hub.instance.getWorkPool().submit(task);
			}
		}
		
		// the Task will expand, ignore this rest of this step
	}
	
	@Override
	public void build(WeakReference<UIWork> work) {
		// KEEP
		// the Task in expand phase already built the children, ignore this step
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		if (! "block".equals(this.getAttribute("Mode", "skip").toLowerCase())) {
			this.translateSkip(work, pnodes);
			return;
		}
		
		// don't change my identity until after the scripts run
		this.setName("div");
		
		this.removeAttribute("Path");
		
		super.translate(work, pnodes);
	}
}
