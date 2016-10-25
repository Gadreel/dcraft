package dcraft.web.ui.tags;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import dcraft.filestore.CommonPath;
import dcraft.hub.Hub;
import dcraft.hub.SiteInfo;
import dcraft.lang.op.OperationContext;
import dcraft.util.StringUtil;
import dcraft.web.core.IOutputAdapter;
import dcraft.web.core.IOutputContext;
import dcraft.web.core.WebContext;
import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.web.ui.adapter.DynamicOutputAdapter;
import dcraft.xml.XElement;
import dcraft.xml.XNode;

public class Html extends MixIn {
	protected Map<String, String> hiddenattributes = null;
	protected List<XNode> hiddenchildren = null;
	
	public Map<String, String> getHiddenAttributes() {
		return this.hiddenattributes;
	}
	
	public String getHiddenAttribute(String name) {
		return (this.hiddenattributes == null ? null : XNode.unquote(this.hiddenattributes.get(name)));
	}
	
	public List<XNode> getHiddenChildren() {
		return this.hiddenchildren;
	}
	
	@Override
	public UIElement getRoot() {
		return this;
	}
	
	public Html() {
		super("html");
	}
	
	@Override
	public void expand(WeakReference<UIWork> work) {
		if (this.hasAttribute("Skeleton")) {
			String tpath = this.getAttribute("Skeleton");
			
			CommonPath pp = new CommonPath(tpath + ".html");		
			
			IOutputAdapter sf = work.get().getContext().getSite().getWebsite().findFile(pp, work.get().getContext().isPreview());
			
			if (sf instanceof DynamicOutputAdapter) {
				UIElement layout = ((DynamicOutputAdapter)sf).getSource();
				
				layout.mergeWithRoot(work, this, true);		// merge content but don't actually add the root of skeleton itself 
			}
		}

		super.expand(work);
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		// don't change my identity until after the scripts run
		this.setName("html");

		XElement body = this.find("body");
		
		if (body == null) {
			body = new Fragment()
				.with(new UIElement("h1")
						.withText("Missing Body Error!!")
				);
			
			this.add(body);
		}
		
		List<XElement> reqstyles =  this.selectAll("dc.RequireStyle");
		
		// we only want head and body in translated document
		// set apart the rest for possible use later in dynamic out
		this.hiddenattributes = this.attributes;
		this.hiddenchildren = this.children;
		
		this.attributes = new HashMap<>();
		this.children = new ArrayList<>();
    	
    	// setup a parameter so that PageTitle is available to macros when executing above
    	this.withParam("PageTitle", XNode.unquote(this.hiddenattributes.get("Title")));

    	IOutputContext octx = work.get().getContext();
    	
    	if ((octx instanceof WebContext) && ((WebContext) octx).isDynamic()) {
    		this
    			.withAttribute("Title", "@val|PageTitle@ - @ctx|SiteTitle@")
    			.with(body);
    	}
    	else {
    		UIElement head = new UIElement("head");
    		
	    	head
				.with(new UIElement("meta")
		    		.withAttribute("chartset", "utf-8")
				)
	    		.with(new UIElement("meta")
		    		.withAttribute("name", "format-detection")
		    		.withAttribute("content", "telephone=no")
		    	)
	    		.with(new UIElement("meta")
		    		.withAttribute("name", "viewport")
		    		.withAttribute("content", "width=device-width, initial-scale=1, maximum-scale=1.0, user-scalable=no")
		    	)
	    		.with(new UIElement("meta")
		    		.withAttribute("name", "robots")
		    		.withAttribute("content", ("false".equals(this.getAttribute("Public", "true").toLowerCase())) 
		    				? "noindex,nofollow" : "index,follow")
		    	)
				.with(new UIElement("title").withText("@val|PageTitle@ - @ctx|SiteTitle@"))   
	   		;
	    	
	    	SiteInfo site = work.get().getContext().getSite();
			
			XElement domainwebconfig = site.getWebsite().getWebConfig();
			
			String icon = this.getAttribute("Icon");
			
			if (StringUtil.isEmpty(icon))
				icon = this.getAttribute("Icon16");
			
			if (StringUtil.isEmpty(icon) && (domainwebconfig != null))
				icon = domainwebconfig.getAttribute("Icon");
			
			if (StringUtil.isEmpty(icon))
				icon = "/imgs/logo";
			
			if (StringUtil.isNotEmpty(icon)) { 
				// if full name then use as the 16x16 version
				if (icon.endsWith(".png")) {
			    	head
						.with(new UIElement("link")
					    		.withAttribute("type", "image/png")
					    		.withAttribute("rel", "shortcut icon")
					    		.withAttribute("href", icon)
						)
						.with(new UIElement("link")
					    		.withAttribute("sizes", "16x16")
					    		.withAttribute("rel", "icon")
					    		.withAttribute("href", icon)
						);
				}
				else {
			    	head
						.with(new UIElement("link")
					    		.withAttribute("type", "image/png")
					    		.withAttribute("rel", "shortcut icon")
					    		.withAttribute("href", icon + "16.png")
						)
						.with(new UIElement("link")
					    		.withAttribute("sizes", "16x16")
					    		.withAttribute("rel", "icon")
					    		.withAttribute("href", icon + "16.png")
						)
						.with(new UIElement("link")
					    		.withAttribute("sizes", "32x32")
					    		.withAttribute("rel", "icon")
					    		.withAttribute("href", icon + "32.png")
						)
						.with(new UIElement("link")
					    		.withAttribute("sizes", "152x152")
					    		.withAttribute("rel", "icon")
					    		.withAttribute("href", icon + "152.png")
						);
				}
			}
			
			icon = this.getAttribute("Icon32");
			
			if (StringUtil.isNotEmpty(icon)) { 
				head.with(new UIElement("link")
			    		.withAttribute("sizes", "32x32")
			    		.withAttribute("rel", "icon")
			    		.withAttribute("href", icon)
				);
			}
			
			icon = this.getAttribute("Icon152");
			
			if (StringUtil.isNotEmpty(icon)) { 
				head.with(new UIElement("link")
			    		.withAttribute("sizes", "152x152")
			    		.withAttribute("rel", "icon")
			    		.withAttribute("href", icon)
				);
			}
			
			XElement del = this.find("dc.Description");
			String desc = (del != null) ? del.getText() : "@ctx|SiteDescription@";
			
			if (StringUtil.isNotEmpty(desc))
				head.with(new UIElement("meta")
						.withAttribute("name", "description")
						.withAttribute("content", desc)
				);
			
			XElement kel = this.find("dc.Keywords");
			String keywords = (kel != null) ? kel.getText() : "@ctx|SiteKeywords@";
			
			if (StringUtil.isNotEmpty(keywords))
				head.with(new UIElement("meta")
						.withAttribute("name", "keywords")
						.withAttribute("content", keywords)
				);
			
			// TODO test meta, like MFD
			if (domainwebconfig != null) {
				for (XElement gel : domainwebconfig.selectAll("Meta")) {
					UIElement m = new UIElement("meta");
					
					for (Entry<String, String> mset : gel.getAttributes().entrySet()) 
						m.withAttribute(mset.getKey(), mset.getValue());
					
					head.with(m);
				}
			}
			
			if (Hub.instance.getResources().isForTesting()) {
				head
					.with(new UIElement("link")
							.withAttribute("type", "text/css")
							.withAttribute("rel", "stylesheet")
							.withAttribute("href", "/css/font-awesome.css"))
					.with(new UIElement("link")
							.withAttribute("type", "text/css")
							.withAttribute("rel", "stylesheet")
							.withAttribute("href", "/css/dc.pui.css"));		// has Normalize and Pure, plus dc
			}
			else {
				head.with(new UIElement("link")
					.withAttribute("type", "text/css")
					.withAttribute("rel", "stylesheet")
					.withAttribute("href", "/css/cache/dc.min.css"));
			}
			
			if (domainwebconfig != null) {
				for (XElement gel : domainwebconfig.selectAll("Global")) {
					if (gel.hasAttribute("Style"))
						head.with(new UIElement("link")
								.withAttribute("type", "text/css")
								.withAttribute("rel", "stylesheet")
								.withAttribute("href", gel.getAttribute("Style")));
				}
			}

			// add in styles specific for this page so we don't have to wait to see them load 
			// TODO enhance so style doesn't double load
			for (XElement func : reqstyles) {
				if (func.hasAttribute("Path"))
					head.with(new UIElement("link")
							.withAttribute("type", "text/css")
							.withAttribute("rel", "stylesheet")
							.withAttribute("href", func.getAttribute("Path")));
			}
			
			// trim down the required code
			if (Hub.instance.getResources().isForTesting()) {
				head
					.with(new UIElement("script")
							.withAttribute("defer", "defer")
							.withAttribute("src", "/js/vendor/jquery-3.0.0.slim.min.js"))
					.with(new UIElement("script")
							.withAttribute("defer", "defer")
							.withAttribute("src", "/js/vendor/moment.min.js"))
					.with(new UIElement("script")
							.withAttribute("defer", "defer")
							.withAttribute("src", "/js/vendor/numeral/numeral.min.js"))
					.with(new UIElement("script")
							.withAttribute("defer", "defer")
							.withAttribute("src", "/js/vendor/numeral/languages.min.js"))
					.with(new UIElement("script")
							.withAttribute("defer", "defer")
							.withAttribute("src", "/js/vendor/velocity.min.js"))
					.with(new UIElement("script")
							.withAttribute("defer", "defer")
							.withAttribute("src", "/js/dc/dc.lang.js"))
					.with(new UIElement("script")
							.withAttribute("defer", "defer")
							.withAttribute("src", "/js/dc/dc.schema.js"))
					.with(new UIElement("script")
							.withAttribute("defer", "defer")
							.withAttribute("src", "/js/dc/dc.schema.def.js"))   
					.with(new UIElement("script")
							.withAttribute("defer", "defer")
							.withAttribute("src", "/js/dc/dc.user.js"))
					.with(new UIElement("script")
							.withAttribute("defer", "defer")
							.withAttribute("src", "/js/dc/dc.comm.js"))
					.with(new UIElement("script")
							.withAttribute("defer", "defer")
							.withAttribute("src", "/js/dc/dc.pui.js"))
					.with(new UIElement("script")
							.withAttribute("defer", "defer")
							.withAttribute("src", "/js/dc/dc.app.js"))
				;
			}
			else {
				head.with(new UIElement("script")
					.withAttribute("defer", "defer")
					.withAttribute("src", "/js/cache/dc.min.js"));
			}
			
			head.with(new UIElement("script")
					.withAttribute("defer", "defer")
					.withAttribute("src", "/js/dc.main.js"));		// after ui so we can override 
				
			if (domainwebconfig != null) {
				for (XElement gel : domainwebconfig.selectAll("Global")) {
					if (gel.hasAttribute("Script"))
						head.with(new UIElement("script")
								.withAttribute("defer", "defer")
								.withAttribute("src", gel.getAttribute("Script")));
				}
			}

			head.with(new UIElement("script")
					.withAttribute("defer", "defer")
					.withAttribute("src", "/js/dc/dc.go.js"));		// start the UI scripts
	    	
			this
				.withAttribute("lang", OperationContext.get().getWorkingLocaleDefinition().getLanguage())
				.withAttribute("dir", OperationContext.get().getWorkingLocaleDefinition().isRightToLeft() ? "rtl" : "ltr")
				.with(head)
				.with(body);
    	}
    	
		super.translate(work, pnodes);		
	}
}
