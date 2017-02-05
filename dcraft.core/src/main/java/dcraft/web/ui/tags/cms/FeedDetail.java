package dcraft.web.ui.tags.cms;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import dcraft.cms.feed.core.FeedAdapter;
import dcraft.filestore.CommonPath;
import dcraft.lang.op.FuncResult;
import dcraft.lang.op.OperationContext;
import dcraft.log.Logger;
import dcraft.struct.Struct;
import dcraft.util.StringUtil;
import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIUtil;
import dcraft.web.ui.UIWork;
import dcraft.web.ui.tags.Button;
import dcraft.xml.XElement;
import dcraft.xml.XNode;

public class FeedDetail extends UIElement {

	@Override
	public void build(WeakReference<UIWork> work) {
		String channel = this.getAttribute("Channel");
		boolean pagemain = this.getAttributeAsBooleanOrFalse("PageMain");
		int pathpart = Struct.objectToInteger(this.getAttribute("PathPart", "1")).intValue();

		/*
		<dcm.FeedDetail Channel="Announcements" PathPart="1" PageMain="true">
			<Template>
				<div class="event-title"><h3>@val|Field|Title@</h3></div>
				<dc.Markdown class="event-content"><![CDATA[@val|Field|Summary@]]></dc.Markdown>
			</Template>
		</dcm.FeedDetail>
		 * 
		 */
		
		this.withAttribute("data-dcm-channel", channel);
		
		UIElement tel = (UIElement) this.find("Template");
		
		UIElement mtel = (UIElement) this.find("MissingTemplate");

		// start with clean children
		this.children = new ArrayList<>();
		
		if ((tel == null) || (mtel == null))
			return;
        
        // now build up the xml for the content
        StringBuilder out = new StringBuilder();

        out.append("<div>");

		CommonPath fpath = work.get().getContext().getPath().subpath(pathpart);
		int pdepth = fpath.getNameCount();
		
		// check file system
		while (pdepth > 0) {
			CommonPath ppath = fpath.subpath(0, pdepth);
			
			// possible to override the file path and grab a random Page from `feed`
			String cmspath = ppath.toString();
			
			FeedAdapter feed = FeedAdapter.from(channel, cmspath, work.get().getContext().isPreview());
			
			if (feed != null) {
				FeedParams ftemp = new FeedParams();
				ftemp.setFeedData(feed);
				
				this.withAttribute("data-dcm-path", cmspath);
				
				String template = tel.getText();
				  
				String value = ftemp.expandMacro(work.get().getContext(), template);
				 
				value = value.replace("*![CDATA[", "<![CDATA[").replace("]]*", "]]>");
				
				out.append(value);
				
				if (pagemain)
					UIUtil.decorateHtmlPageUI(feed, OperationContext.get(), this.getRoot());
				
				break;
			}
			
			pdepth--;
		}

		if (pdepth == 0) {
			String template = mtel.getText();
			
			if (StringUtil.isNotEmpty(template)) {
				String value = this.expandMacro(work.get().getContext(), template);
				 
				if (StringUtil.isNotEmpty(template)) {
					value = value.replace("*![CDATA[", "<![CDATA[").replace("]]*", "]]>");
					
					out.append(value);
				}
			}
		}
		
        out.append("</div>");

        try {
        	FuncResult<XElement> xres = work.get().getContext().getSite().getWebsite().parseUI(out);
        	
        	if (xres.isNotEmptyResult()) {
        		XElement lbox = xres.getResult();
        		
        		this.replaceChildren(lbox);
        	}
        	else {
        		// TODO
				//pel.add(new UIElement("div")
				//	.withText("Error parsing section."));
        	}
        }
        catch (Exception x) {
        	Logger.error("Error adding feed detail: " + x);
        }
		
		this.with(new Button("dcmi.EditFeedButton")
				.withClass("dcuiSectionButton", "dcuiCmsi")
				.withAttribute("Icon", "fa-cog")
			);
        
		FeedDetail.super.build(work);
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		this
			.withClass("dcm-cms-editable", "dcm-feed-detail")
			.withAttribute("data-dccms-edit", this.getAttribute("AuthTags", "Editor,Admin,Developer"))	// TODO get from channel def, for Feed too
			.withAttribute("data-dc-enhance", "true")
			.withAttribute("data-dc-tag", this.getName());
		
		this.setName("div");
		
		super.translate(work, pnodes);
	}
}
