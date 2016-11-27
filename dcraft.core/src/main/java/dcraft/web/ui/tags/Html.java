package dcraft.web.ui.tags;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dcraft.filestore.CommonPath;
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
	
	public List<XNode> getHiddenChildren() {
		return this.hiddenchildren;
	}
	
	@Override
	public UIElement getRoot() {
		return this;
	}
	
	public Html() {
		super("dc.Html");
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

		UIElement body = (UIElement) this.find("dc.Body");
		
		if (body == null) {
			body = new Fragment();
			
			body
				.with(new UIElement("h1")
						.withText("Missing Body Error!!")
				);
		}

		String pc = this.getAttribute("PageClass");
		
		if (StringUtil.isNotEmpty(pc)) 
			body.withClass(pc);
		
		for (XNode rel : this.getChildren()) {
			if (! (rel instanceof XElement))
				continue;

			XElement xel = (XElement) rel;
			
			if (xel.getName().equals("dc.Require") && xel.hasNotEmptyAttribute("Class")) 
				body.withClass(xel.getAttribute("Class"));
		}
		
		List<XElement> reqstyles =  this.selectAll("dc.RequireStyle");
		
		// we only want head and body in translated document
		// set apart the rest for possible use later in dynamic out
		this.hiddenattributes = this.attributes;
		this.hiddenchildren = this.children;
		
		this.attributes = new HashMap<>();
		this.children = new ArrayList<>();
    	
    	// setup a parameter so that PageTitle is available to macros when executing above
		if (this.hiddenattributes != null) 
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
			
			/*
			 * 	Essential Meta Tags
			 * 
				https://css-tricks.com/essential-meta-tags-social-media/  
				
				- images:  Reconciling the guidelines for the image is simple: follow Facebook's 
				recommendation of a minimum dimension of 1200x630 pixels (can go as low as 600 x 315) 
				and an aspect ratio of 1.91:1, but adhere to Twitter's file size requirement of less than 1MB.  
				
				- Title max 70 chars
				- Desc max 200 chars			
			 */
			
			head
				.with(new UIElement("meta")
					.withAttribute("property", "og:title")
					.withAttribute("content", "@val|PageTitle@")
				);
			
			String keywords = XNode.unquote(this.hiddenattributes.get("Keywords"));

			if (StringUtil.isNotEmpty(keywords))
				head
					.with(new UIElement("meta")
						.withAttribute("name", "keywords")
						.withAttribute("content", keywords)
					);
			
			String desc = XNode.unquote(this.hiddenattributes.get("Description"));
			
			if (StringUtil.isNotEmpty(desc))
				head
					.with(new UIElement("meta")
						.withAttribute("name", "description")
						.withAttribute("content", desc)
					)
					.with(new UIElement("meta")
						.withAttribute("property", "og:description")
						.withAttribute("content", desc)
					);

			
			String indexurl = null;

			if ((domainwebconfig != null) && domainwebconfig.hasNotEmptyAttribute("IndexUrl")) 
				indexurl = domainwebconfig.getAttribute("IndexUrl");
			
			String image = XNode.unquote(this.hiddenattributes.get("Image"));

			if (StringUtil.isEmpty(image) && (domainwebconfig != null) && domainwebconfig.hasNotEmptyAttribute("SiteImage")) 
				image = domainwebconfig.getAttribute("SiteImage");
			
			if (StringUtil.isNotEmpty(indexurl) && StringUtil.isNotEmpty(image))
				head
					.with(new UIElement("meta")
						.withAttribute("property", "og:image")
						.withAttribute("content", this.getAttribute("Image", indexurl + image.substring(1)))
					);
			
			if (StringUtil.isNotEmpty(indexurl))
				head
					.with(new UIElement("meta")
						.withAttribute("property", "og:url")
						.withAttribute("content", indexurl + work.get().getContext().getPath().toString().substring(1))
					);
			
			/* TODO review
				.with(new UIElement("meta")
					.withAttribute("name", "twitter:card")
					.withAttribute("content", "summary")
				);
			*/
			
			/* TODO review, generalize so we can override
			if (domainwebconfig != null) {
				for (XElement gel : domainwebconfig.selectAll("Meta")) {
					UIElement m = new UIElement("meta");
					
					for (Entry<String, String> mset : gel.getAttributes().entrySet()) 
						m.withAttribute(mset.getKey(), mset.getValue());
					
					head.with(m);
				}
			}
			*/
			
			// TODO research canonical url too
			
			// TODO someday support compiled scripts and css
			//if (Hub.instance.getResources().isForTesting()) {
				head
					.with(new UIElement("link")
							.withAttribute("type", "text/css")
							.withAttribute("rel", "stylesheet")
							.withAttribute("href", "/css/font-awesome.css"))
					.with(new UIElement("link")
							.withAttribute("type", "text/css")
							.withAttribute("rel", "stylesheet")
							.withAttribute("href", "/css/dc.app.css"))		// has Normalize and Pure, plus dc
					.with(new UIElement("link")
							.withAttribute("type", "text/css")
							.withAttribute("rel", "stylesheet")
							.withAttribute("href", "/css/main.css"));		// default website styling, until overridden
			/*
			}
			else {
				head.with(new UIElement("link")
					.withAttribute("type", "text/css")
					.withAttribute("rel", "stylesheet")
					.withAttribute("href", "/css/cache/dc.min.css"));
			}
			*/
			
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
			//if (Hub.instance.getResources().isForTesting()) {
				head
					.with(new UIElement("script")
							.withAttribute("defer", "defer")
							.withAttribute("src", "/js/vendor/jquery-3.1.1.slim.min.js"))
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
							.withAttribute("src", "/js/dc.lang.js"))
					.with(new UIElement("script")
							.withAttribute("defer", "defer")
							.withAttribute("src", "/js/dc.schema.js"))
					.with(new UIElement("script")
							.withAttribute("defer", "defer")
							.withAttribute("src", "/js/dc.schema.def.js"))   
					.with(new UIElement("script")
							.withAttribute("defer", "defer")
							.withAttribute("src", "/js/dc.user.js"))
					.with(new UIElement("script")
							.withAttribute("defer", "defer")
							.withAttribute("src", "/js/dc.comm.js"))
					.with(new UIElement("script")
							.withAttribute("defer", "defer")
							.withAttribute("src", "/js/dc.app.js"))
				;
				/*
			}
			else {
				head.with(new UIElement("script")
					.withAttribute("defer", "defer")
					.withAttribute("src", "/js/cache/dc.min.js"));
			}
			*/
			
			head.with(new UIElement("script")
					.withAttribute("defer", "defer")
					.withAttribute("src", "/js/main.js"));		// after ui so we can override 
				
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
					.withAttribute("src", "/js/dc.go.js"));		// start the UI scripts
	    	
			this
				.withAttribute("lang", OperationContext.get().getWorkingLocaleDefinition().getLanguage())
				.withAttribute("dir", OperationContext.get().getWorkingLocaleDefinition().isRightToLeft() ? "rtl" : "ltr")
				.with(head)
				.with(body);
    	}
    	
		super.translate(work, pnodes);		
	}
}
