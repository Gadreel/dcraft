package dcraft.web.ui;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import dcraft.lang.op.OperationContext;
import dcraft.log.Logger;
import dcraft.schema.DataType;
import dcraft.util.StringUtil;
import dcraft.web.core.WebContext;
import dcraft.web.ui.tags.Html;
import dcraft.xml.XElement;
import dcraft.xml.XNode;
import dcraft.xml.XmlPrinter;
import dcraft.xml.XmlToJsonPrinter;

public class JsonPrinter extends XmlToJsonPrinter {
	protected WebContext ctx = null;

	public void setContext(WebContext v) {
		this.ctx = v;
	}
	
	@Override
	public void setOut(PrintStream v) {
		super.setOut(v);
		
		this.jsb.setStreamIndent(1);
	}
	
	public JsonPrinter(WebContext wctx) {
		super();

		this.setContext(wctx);
	}
	
	@Override
	public void print(XNode root) {
		Html doc = (Html) root;
		
		this.jsb.write("dc.pui.Loader.addPageDefinition('" + ctx.getRequest().getOriginalPath() + "', ");
		
		try {
			List<XNode> hidden = doc.getHiddenChildren();
			OperationContext oc = OperationContext.get();
			
			this.jsb.startRecord();
			
			// ==============================================
			//  Styles
			// ==============================================
			
			this.jsb.field("RequireStyles");
			
			this.jsb.startList();
			
			for (XElement func : this.selectAll("dc.RequireStyle", hidden)) {
				if (func.hasNotEmptyAttribute("Path"))
					this.jsb.value(func.getAttribute("Path"));
			}
			
			this.jsb.endList();
			
			// ==============================================
			//  Require Types
			// ==============================================
			
			this.jsb.field("RequireType");
			
			this.jsb.startList();
			
			for (XElement func : this.selectAll("dc.Require", hidden)) {
				if (func.hasNotEmptyAttribute("Types")) {
					String[] types = func.getAttribute("Types").split(",");
		
					for (String name : types) {
						DataType dt = oc.getSchema().getType(name);
						
						if (dt != null) 
							dt.toJsonDef().toBuilder(this.jsb);		// TODO enhance types to go straight to builder
					}
				}				
			}
			
			this.jsb.endList();
			
			// ==============================================
			//  Require Tr
			// ==============================================
			
			this.jsb.field("RequireTr");
			
			this.jsb.startList();
			
			for (XElement func : this.selectAll("dc.Require", hidden)) {
				if (func.hasNotEmptyAttribute("Trs")) {
					String[] trs = func.getAttribute("Trs").split(",");
		
					for (String name : trs) {
						// TODO support ranges - NNN:MMM
						
						if (StringUtil.isDataInteger(name))
							name = "_code_" + name;
						
						this.jsb
							.startRecord()
							.field("Token", name)
							.field("Value", oc.findToken(name))
							.endRecord();
					}
				}
			}
			
			this.jsb.endList();
			
			// ==============================================
			//  Libs
			// ==============================================
			
			this.jsb.field("RequireLibs");
			
			this.jsb.startList();
			
			for (XElement func : this.selectAll("dc.RequireLib", hidden)) {
				if (func.hasNotEmptyAttribute("Path"))
					this.jsb.value(func.getAttribute("Path"));
			}
			
			this.jsb.endList();
			
			// ==============================================
			//  Functions
			// ==============================================
			
			this.jsb.field("Functions");
			
			this.jsb.startRecord();
			
			for (XElement func : this.selectAll("dc.Function", hidden)) {
				if (!func.hasNotEmptyAttribute("Name"))
					continue;
				
				this.jsb.field(func.getAttribute("Name"));
				
				StringBuilder sb = new StringBuilder();
				
				sb.append(" function(" + func.getAttribute("Params", "") + ") { ");
				
				String code = valueMacro(func.getText(), func);
				
				code = StringUtil.stripTrailingWhitespace(code);
				
				if (code.charAt(0) != '\n')
					sb.append("\n");
				
				sb.append(code);
				
				sb.append("\n\t\t\t}");
				
				this.jsb.rawValue(sb);
			}
			
			this.jsb.endRecord();
			
			// ==============================================
			//  Load Functions
			// ==============================================
			
			this.jsb.field("LoadFunctions");
			
			this.jsb.startList();
			
			for (XElement func : this.selectAll("dc.Function", hidden)) {
				if (!"Load".equals(func.getAttribute("Mode")))
					continue;
				
				if (func.hasAttribute("Name")) {
					this.jsb.value(func.getAttribute("Name"));
				}
				else {
					StringBuilder sb = new StringBuilder();
					
					sb.append(" function(" + func.getAttribute("Params", "") + ") { ");
					
					String code = valueMacro(func.getText(), func);
					
					code = StringUtil.stripTrailingWhitespace(code);
					
					if (code.charAt(0) != '\n')
						sb.append("\n");
					
					sb.append(code);
					
					sb.append("\n\t\t\t}");
					
					this.jsb.rawValue(sb);
				}
			}
			
			this.jsb.endList();

			this.jsb.field("Title", valueMacro(doc.getAttribute("Title"), doc));
			
			XElement body = doc.selectFirst("body");
			
			if (body != null) 
				this.jsb.field("PageClass", valueMacro(body.getAttribute("class"), doc));
			
			this.jsb.field("Layout");
	
			XmlPrinter prt = new HtmlPrinter(ctx);  
			
	    	prt.setFormatted(false);
	    	prt.setOut(this.jsb.startStreamValue());
	    	
			for (XNode cnode : body.getChildren()) 
				prt.print(cnode, 0, body);

			this.jsb.endStreamValue();
			
			this.jsb.endRecord();
			
			this.jsb.write(");\n\ndc.pui.Loader.resumePageLoad();");
		}
		catch(Exception x) {
			Logger.error("Unable to write page JSON: " + x);
			
			// TODO failed js code
		}
	}
	
	@Override
	protected String valueMacro(String value, XElement scope) {
		return ((UIElement)scope).expandMacro(this.ctx, value);
	}
	
	public List<XElement> selectAll(String path, List<XNode> src) {
		List<XElement> matches = new ArrayList<XElement>();
		this.selectAll(path, src, matches);
		return matches;
	}
	
	/**
	 * Internal, recursive search used by selectAll
	 * 
	 * @param path a backslash delimited string
	 * @param matches list of all matching elements, or empty list if no match
	 */
	protected void selectAll(String path, List<XNode> src, List<XElement> matches) {
		if (src.isEmpty())
			return;
		
		int pos = path.indexOf('/');

		// go back to root not supported
		if (pos == 0)
			return;
		
		String name = null;
		
		if (pos == -1) {
			name = path;
			path = null;
		}
		else { 
			name = path.substring(0, pos);
			path = path.substring(pos + 1);
		}
		
		for (XNode n : src) {
			if (n instanceof XElement) {
				if ("*".equals(name) || ((XElement)n).getName().equals(name)) {
					if (pos == -1) 
						matches.add((XElement)n);
					else  
						((XElement)n).selectAll(path, matches);
				}
			}
		}
	}	
}
