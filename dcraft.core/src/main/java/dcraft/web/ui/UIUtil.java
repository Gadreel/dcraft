package dcraft.web.ui;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import dcraft.cms.feed.core.FeedAdapter;
import dcraft.cms.feed.core.FeedPartMatchResult;
import dcraft.hub.Hub;
import dcraft.lang.op.FuncCallback;
import dcraft.lang.op.FuncResult;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.OperationObserver;
import dcraft.lang.op.OperationResult;
import dcraft.locale.ITranslationAdapter;
import dcraft.log.DebugLevel;
import dcraft.log.Logger;
import dcraft.util.StringUtil;
import dcraft.web.core.IOutputContext;
import dcraft.web.md.Processor;
import dcraft.web.ui.tags.Body;
import dcraft.web.ui.tags.Button;
import dcraft.web.ui.tags.Callout;
import dcraft.web.ui.tags.Html;
import dcraft.web.ui.tags.IncludeFrag;
import dcraft.web.ui.tags.IncludeParam;
import dcraft.web.ui.tags.Link;
import dcraft.web.ui.tags.Markdown;
import dcraft.web.ui.tags.MixIn;
import dcraft.web.ui.tags.PagePart;
import dcraft.web.ui.tags.Param;
import dcraft.web.ui.tags.ServerScript;
import dcraft.web.ui.tags.cms.BasicCarousel;
import dcraft.web.ui.tags.cms.Facebook;
import dcraft.web.ui.tags.cms.Instagram;
import dcraft.web.ui.tags.cms.QotDBlock;
import dcraft.web.ui.tags.cms.ServerInfo;
import dcraft.web.ui.tags.cms.SocialMediaIcon;
import dcraft.web.ui.tags.cms.TwitterTimelineLoader;
import dcraft.web.ui.tags.form.AlignedField;
import dcraft.web.ui.tags.form.AlignedInstructions;
import dcraft.web.ui.tags.form.ButtonGroup;
import dcraft.web.ui.tags.form.CheckControl;
import dcraft.web.ui.tags.form.CheckGroup;
import dcraft.web.ui.tags.form.Form;
import dcraft.web.ui.tags.form.InputControl;
import dcraft.web.ui.tags.form.InputField;
import dcraft.web.ui.tags.form.Instructions;
import dcraft.web.ui.tags.form.ManagedForm;
import dcraft.web.ui.tags.form.MultiInputField;
import dcraft.web.ui.tags.form.RadioControl;
import dcraft.web.ui.tags.form.RadioGroup;
import dcraft.web.ui.tags.form.SelectField;
import dcraft.web.ui.tags.form.SubmitButton;
import dcraft.web.ui.tags.form.TextArea;
import dcraft.web.ui.tags.form.Uploader;
import dcraft.web.ui.tags.form.YesNo;
import dcraft.web.ui.tags.Fragment;
import dcraft.web.ui.tags.Panel;
import dcraft.work.Task;
import dcraft.xml.XElement;
import dcraft.xml.XNode;
import dcraft.xml.XmlReader;

public class UIUtil {
	// @[a-zA-Z0-9_\\-,:/]+@
	public static Pattern macropatten = Pattern.compile("@\\S+?@", Pattern.MULTILINE);

	static public Map<String, Class<? extends XElement>> allocateCoreMap() {
		Map<String, Class<? extends XElement>> tagmap = new HashMap<String, Class<? extends XElement>>();

		// fully server side, not sent to client
		tagmap.put("dc.Html", Html.class);
		tagmap.put("dc.MixIn", MixIn.class);		
		tagmap.put("dc.Body", Body.class);
		tagmap.put("dc.Fragment", Fragment.class);
		tagmap.put("dc.PagePart", PagePart.class);
		tagmap.put("dc.ServerScript", ServerScript.class);
		tagmap.put("dc.Param", Param.class);
		tagmap.put("dc.IncludeFrag", IncludeFrag.class);
		tagmap.put("dc.IncludeParam", IncludeParam.class);
		tagmap.put("dc.Markdown", Markdown.class);

		// enhanced basic tags
		tagmap.put("dc.Link", Link.class);
		tagmap.put("dc.Button", Button.class);
		tagmap.put("dc.Panel", Panel.class);
		tagmap.put("dc.Callout", Callout.class);
		
		// advanced forms
		tagmap.put("dcf.Form", Form.class);
		tagmap.put("dcf.ManagedForm", ManagedForm.class);
		tagmap.put("dcf.Text", InputField.class);
		tagmap.put("dcf.Password", InputField.class);
		tagmap.put("dcf.Hidden", InputField.class);
		tagmap.put("dcf.Label", InputField.class);
		tagmap.put("dcf.InputControl", InputControl.class);
		tagmap.put("dcf.Aligned", AlignedField.class);
		tagmap.put("dcf.FormButtons", AlignedField.class);
		tagmap.put("dcf.SubmitButton", SubmitButton.class);
		tagmap.put("dcf.AlignedInstructions", AlignedInstructions.class);
		tagmap.put("dcf.Instructions", Instructions.class);
		tagmap.put("dcf.ButtonGroup", ButtonGroup.class);
		tagmap.put("dcf.Select", SelectField.class);
		tagmap.put("dcf.TextArea", TextArea.class);
		tagmap.put("dcf.MultiText", MultiInputField.class);
		tagmap.put("dcf.YesNo", YesNo.class);
		tagmap.put("dcf.RadioGroup", RadioGroup.class);
		tagmap.put("dcf.HorizRadioGroup", RadioGroup.class);
		tagmap.put("dcf.RadioButton", RadioControl.class);
		tagmap.put("dcf.Checkbox", CheckGroup.class);
		tagmap.put("dcf.CheckGroup", CheckGroup.class);
		tagmap.put("dcf.HorizCheckGroup", CheckGroup.class);
		tagmap.put("dcf.CheckInput", CheckControl.class);
		tagmap.put("dcf.Uploader", Uploader.class);
		
		// cms tags
		tagmap.put("dcm.Facebook", Facebook.class);
		tagmap.put("dcm.Instagram", Instagram.class);
		tagmap.put("dcm.TwitterTimelineLoader", TwitterTimelineLoader.class);
		tagmap.put("dcm.SocialMediaIcon", SocialMediaIcon.class);
		tagmap.put("dcm.BasicCarousel", BasicCarousel.class);
		tagmap.put("dcm.QotD", QotDBlock.class);
		tagmap.put("dcm.ServerInfo", ServerInfo.class);
		
		return tagmap;
	}

	static public FuncResult<? extends XElement> parse(CharSequence xml, Map<String, Class<? extends XElement>> map) {
		return XmlReader.parse(xml, false, map, UIElement.class);
	}

	// useful for tests, try to use webdomain instead
	static public FuncResult<? extends XElement> parseCore(CharSequence xml) {
		return XmlReader.parse(xml, false, UIUtil.allocateCoreMap(), UIElement.class);
	}
	
	// runs expand and build so be sure these children won't expand or build again
	static public void convert(WeakReference<UIWork> parentwork, UIElement parent, XElement frag, FuncCallback<UIElement> ccallback) {
		if (! (frag instanceof UIElement)) 
			frag = parentwork.get().getContext().getSite().getWebsite().convertUI(parent, frag);
		
		ccallback.setResult((UIElement) frag);
		
		// set so that only expanding is done
		UIWork work = new UIInitialWork();
		work.setContext(parentwork.get().getContext());
		work.setRoot((UIElement) frag);
		
		Task task = Task
			.taskWithSubContext()
			.withLogging(DebugLevel.Warn)
			.withTitle("Working on web fragment: " + frag.getName())
			//.withTopic("Web")  no topic needed if already in Web
			.withObserver(new OperationObserver() {
				@Override
				public void completed(OperationContext ctx) {
					//System.out.println("XElement convert complete");

					ccallback.complete();
				}
			})
			.withWork(work);
		
		Hub.instance.getWorkPool().submit(task);
	}

	// take a feed and build page content from it

	static public void buildHtmlPageUI(FeedAdapter adapt, IOutputContext wctx, UIElement frag) {
		UIUtil.buildHtmlPageUI(adapt, wctx, OperationContext.get(), frag);
	}
	
	static public void buildHtmlPageUI(FeedAdapter adapt, IOutputContext wctx, ITranslationAdapter ctx, UIElement frag) {
		OperationResult or = new OperationResult();
		
		String title = adapt.getField(ctx, "Title");
				
		if (title != null) 
			frag.setAttribute("Title", title);

		String desc = adapt.getField(ctx, "Description");

		if (desc != null) 
			frag.setAttribute("Description", desc);

		String keywords = adapt.getField(ctx, "Keywords");
		
		if (keywords != null) 
			frag.setAttribute("Keywords", keywords);

		String image = adapt.getField(ctx, "Image");
		
		if (image != null) 
			frag.setAttribute("Image", image);
		
		//frag.withParam("CMS-Channel", adapt.getChannel()); 
		//frag.withParam("CMS-Path", adapt.getFeedPath()); 
		
		for (XElement pdef : frag.selectAll("dc.PagePartDef")) {
			String forpart = pdef.getAttribute("For");
			
			if (StringUtil.isEmpty(forpart)) {
				or.error("Unable to build page element: " + pdef);
				continue;
			}
			
			FeedPartMatchResult mr = adapt.bestMatch(ctx, "PagePart", "For", forpart);
			
			if (mr != null) {
				UIElement content = UIUtil.buildHtmlPartUI(adapt, wctx, ctx, mr, forpart, pdef.getAttribute("BuildClass"));
				
				if (content != null) 
					frag.add(content);
			}
		}
	}
	
	static public UIElement buildHtmlPartUI(FeedAdapter adapt, IOutputContext wctx, ITranslationAdapter ctx, FeedPartMatchResult mr, String id, String clss) {
		String lang = mr.locale.getLanguage();
		
		String fmt = mr.el.getAttribute("Format", "md");
		
		UIElement pel = new PagePart();
		
		String chan = adapt.getChannel();
		String fpath = adapt.getFeedPath().substring(chan.length() + 1);
		
		pel
			.withClass(clss, "dcm-cms-part")
			.withAttribute("id", id)
			.withAttribute("data-dccms-mode", "edit")
			.withAttribute("data-dccms-channel", chan)
			.withAttribute("data-dccms-path", fpath)
			.withAttribute("lang", lang);
		
		// copy all attributes 
		for (Entry<String, String> attr : mr.el.getAttributes().entrySet()) 
			pel.setAttribute(attr.getKey(), attr.getValue());
		
		String val = adapt.getPartValue(ctx, mr, wctx.isPreview());
		
		if ("image".equals(fmt)) {
			pel.setName("img");
			pel.setAttribute("src", "/galleries" + val);
			pel.setAttribute("data-dccms-editor", "image");
		}
		else if ("html".equals(fmt)) {
			pel.setAttribute("data-dccms-editor", "html");
			
			FuncResult<XElement> xres = wctx.getSite().getWebsite().parseUI(val);
			
			// don't stop on error
			if (xres.hasErrors()) {
				OperationContext.get().clearExitCode();
				
				pel.with(new UIElement("InvalidContent"));
			}
			else {
				XElement xr = xres.getResult();
	
				// if we cannot parse then fallback on SSI handler
				if (xr instanceof UIElement) {
					UIElement html = (UIElement) xr;
					
					// copy all children
					for (XNode n : html.getChildren())
						pel.add(n);
				}
			}
		}
		else if ("md".equals(fmt)) {
			pel.setAttribute("data-dccms-editor", "md");

	        try {
				// TODO support safe mode?
				UIElement html = Processor.parse(wctx.getMarkdownContext(), val);
				
				// copy all children
				for (XNode n : html.getChildren())
					pel.add(n);
	        }
	        catch (Exception x) {
				pel.with(new UIElement("InvalidContent"));
	        	Logger.warn("Error adding copy box " + x);
	        }
		}
		else {
			pel.with(new UIElement("InvalidFormat"));
		}
		
		return pel;
	}	
}
