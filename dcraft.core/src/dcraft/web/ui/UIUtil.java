package dcraft.web.ui;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import dcraft.hub.Hub;
import dcraft.lang.op.FuncCallback;
import dcraft.lang.op.FuncResult;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.OperationObserver;
import dcraft.log.DebugLevel;
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
import dcraft.web.ui.tags.QotDBlock;
import dcraft.web.ui.tags.ServerInfo;
import dcraft.web.ui.tags.ServerScript;
import dcraft.web.ui.tags.form.AlignedField;
import dcraft.web.ui.tags.form.AlignedInstructions;
import dcraft.web.ui.tags.form.ButtonGroup;
import dcraft.web.ui.tags.form.CheckControl;
import dcraft.web.ui.tags.form.CheckGroup;
import dcraft.web.ui.tags.form.Form;
import dcraft.web.ui.tags.form.InputControl;
import dcraft.web.ui.tags.form.InputField;
import dcraft.web.ui.tags.form.Instructions;
import dcraft.web.ui.tags.form.MultiInputField;
import dcraft.web.ui.tags.form.RadioControl;
import dcraft.web.ui.tags.form.RadioGroup;
import dcraft.web.ui.tags.form.SelectField;
import dcraft.web.ui.tags.form.SubmitButton;
import dcraft.web.ui.tags.form.TextArea;
import dcraft.web.ui.tags.form.YesNo;
import dcraft.web.ui.tags.Fragment;
import dcraft.web.ui.tags.Panel;
import dcraft.work.Task;
import dcraft.xml.XElement;
import dcraft.xml.XmlReader;

public class UIUtil {
	// @[a-zA-Z0-9_\\-,:/]+@
	public static Pattern macropatten = Pattern.compile("@\\S+?@", Pattern.MULTILINE);

	static public Map<String, Class<? extends XElement>> allocateCoreMap() {
		Map<String, Class<? extends XElement>> tagmap = new HashMap<String, Class<? extends XElement>>();

		// fully server side, not sent to client
		tagmap.put("html", Html.class);
		tagmap.put("dc.MixIn", MixIn.class);		// TODO figure out MixIn role - it is nice in NAK Simple-2
		tagmap.put("body", Fragment.class);
		tagmap.put("dc.Fragment", Fragment.class);
		tagmap.put("dc.PagePart", PagePart.class);
		tagmap.put("dc.ServerScript", ServerScript.class);
		tagmap.put("dc.Param", Param.class);
		tagmap.put("dc.IncludeFrag", IncludeFrag.class);
		tagmap.put("dc.IncludeParam", IncludeParam.class);
		tagmap.put("dc.QotD", QotDBlock.class);
		tagmap.put("dc.ServerInfo", ServerInfo.class);
		tagmap.put("dc.Markdown", Markdown.class);

		// enhanced basic tags
		tagmap.put("dc.Link", Link.class);
		tagmap.put("dc.Button", Button.class);
		tagmap.put("dc.Panel", Panel.class);
		tagmap.put("dc.Callout", Callout.class);
		
		// advanced forms
		tagmap.put("dcf.Form", Form.class);
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
			.withTopic("Web")
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
}
