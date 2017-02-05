package dcraft.web.ui.tags;

import java.lang.ref.WeakReference;

import dcraft.cms.feed.core.FeedAdapter;
import dcraft.filestore.CommonPath;
import dcraft.hub.Hub;
import dcraft.lang.CountDownCallback;
import dcraft.lang.op.OperationCallback;
import dcraft.lang.op.OperationContext;
import dcraft.util.StringUtil;
import dcraft.web.core.IOutputContext;
import dcraft.web.core.WebContext;
import dcraft.web.ui.IExpandHelper;
import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIUtil;
import dcraft.web.ui.UIWork;
import dcraft.xml.XElement;

public class MixIn extends UIElement {
	public MixIn() {
		this("dc.MixIn");
	}
	
	public MixIn(String name) {
		super(name);
	}
	
	@Override
	public UIElement newNode() {
		return new MixIn();
	}
	
	@Override
	public void expand(WeakReference<UIWork> work) {
		if (this.hasNotEmptyAttribute("ExpandClass")) {
			IExpandHelper exh = (IExpandHelper) Hub.instance.getInstance(this.getAttribute("ExpandClass"));
			
			exh.expand(this, work);
		}
		
		// load the CMS if any
		if ("auto".equals(this.getAttribute("Cms", "false").toLowerCase())) {
			IOutputContext octx = work.get().getContext();
			
			if (octx instanceof WebContext) {
				WebContext wctx = (WebContext) octx;

				// possible to override the file path and grab a random Page from `feed`
				if (this.hasNotEmptyAttribute("CmsPath")) {
					String cmspath = this.getAttribute("CmsPath");
					
					FeedAdapter feed = FeedAdapter.from("Pages", cmspath, wctx.isPreview());
					
					if (feed != null) {
						UIUtil.buildHtmlPageUI(feed, work.get().getContext(), this);
						this.withAttribute("CmsPath", cmspath);
					}
				}
				else {
					CommonPath path = wctx.getRequest().getPath();
					int pdepth = path.getNameCount();
					
					// check file system
					while (pdepth > 0) {
						CommonPath ppath = path.subpath(0, pdepth);
						
						// possible to override the file path and grab a random Page from `feed`
						String cmspath = ppath.toString();
						
						FeedAdapter feed = FeedAdapter.from("Pages", cmspath, wctx.isPreview());
						
						if (feed != null) {
							UIUtil.buildHtmlPageUI(feed, work.get().getContext(), this);
							this.withAttribute("CmsPath", cmspath);
							
							break;
						}
						
						pdepth--;
					}
				}
			}
		}
		
		super.expand(work);
	}
	
	@Override
	public void build(WeakReference<UIWork> work) {
		OperationContext or = OperationContext.get();
		
		XElement skel = this.selectFirst("dc.Body");

		if (skel == null)
			skel = this.selectFirst("dc.Fragment");
		
		//skel.withAttribute("data-dccms-path", this.getAttribute("data-dccms-path"));
		
		// arrange the parts
		for (XElement pdef : this.selectAll("dc.PagePartDef")) {
			String forpart = pdef.getAttribute("For");
			
			if (StringUtil.isEmpty(forpart)) {
				or.warn("Unable to build page element, no For: " + pdef);
				continue;
			}
			
			String bid = pdef.getAttribute("BuildId", forpart);
			
			XElement bbparent = skel.findParentOfId(bid);
			
			if (bbparent == null) {
				or.error("Missing parent of parent to part: " + pdef);
				continue;
			}
			
			XElement bparent = bbparent.findId(bid); 
			
			if (bparent == null) {
				or.error("Missing parent to part: " + pdef);
				continue;
			}
			
			XElement content = null;
			
			for (XElement part : this.selectAll("dc.PagePart")) {
				if (forpart.equals(part.getAttribute("For"))) {
					content = part;
					break;
				}
			}
			
			if (content == null) {
				or.warn("Missing content to build page element: " + pdef);
				
				content = new UIElement("p").withText("Missing content part.");
			}
			
			this.remove(content);
			
			String bop = pdef.getAttribute("BuildOp", "Append");
			
			if ("Append".equals(bop)) {
				bparent.add(-1, content);
			}
			else if ("Prepend".equals(bop)) {
				bparent.add(0, content);
			}
			else if ("Before".equals(bop)) {
				int ccnt = bbparent.getChildCount();
				int cpos = 0;
				
				for (int i = 0; i < ccnt; i++) 
					if (bbparent.getChild(i) == bparent) {
						cpos = i;
						break;
					}
						
				bbparent.add(cpos, content);
			}
			else if ("After".equals(bop)) {
				int ccnt = bbparent.getChildCount();
				int cpos = 0;
				
				for (int i = 0; i < ccnt; i++) 
					if (bbparent.getChild(i) == bparent) {
						cpos = i;
						break;
					}
						
				bbparent.add(cpos + 1, content);
			}
			
			// make id available to the server script
			content.setAttribute("id", forpart);
		}
    	
		work.get().incBuild();
		
    	CountDownCallback scriptscallback = new CountDownCallback(1, new OperationCallback() {
			@Override
			public void callback() {
				MixIn.super.build(work);
				
				work.get().decBuild();
			}
		});
		
		// run the script
		for (XElement sscript : this.selectAll("dc.ServerScript")) {
			String src = sscript.getText();
			
			if (StringUtil.isNotEmpty(src)) {
				scriptscallback.increment();
				
				OperationCallback cb = new OperationCallback() {
					@Override
					public void callback() {
						scriptscallback.countDown();
					}
				};
				
				try {
					work.get().getContext().getSite().execute(src, "run", work.get().getContext(), MixIn.this, cb);
				}
				catch (Exception x) {
					OperationContext.get().error("Unable to prepare web page server script: " + x);
				}
			}
		}
		
		scriptscallback.countDown();
	}
}
