package dcraft.web.ui;

import java.util.HashSet;
import java.util.Map;

import dcraft.web.core.IOutputContext;
import dcraft.xml.XElement;
import dcraft.xml.XNode;
import dcraft.xml.XText;
import dcraft.xml.XmlPrinter;

public class HtmlPrinter extends XmlPrinter {
	protected HashSet<String> selfclosers = new HashSet<>();
	//protected HashSet<String> compacttags = new HashSet<>();
	protected IOutputContext ctx = null;
	
	public void setContext(IOutputContext v) {
		this.ctx = v;
	}
	
	public HtmlPrinter(IOutputContext wctx) {
		this();

		this.setContext(wctx);
	}
	
	public HtmlPrinter() {
		super();
		
		this.selfclosers.add("base");
		this.selfclosers.add("basefont");
		this.selfclosers.add("frame");
		this.selfclosers.add("link");
		this.selfclosers.add("meta");
		this.selfclosers.add("area");
		this.selfclosers.add("br");
		this.selfclosers.add("col");
		this.selfclosers.add("hr");
		this.selfclosers.add("img");
		this.selfclosers.add("input");
		this.selfclosers.add("param");
		
		/* these are allowed to close self
		 * 
		<base />
		<basefont />
		<frame />
		<link />
		<meta />

		<area />
		<br />
		<col />
		<hr />
		<img />
		<input />
		<param />			    	 
		*/ 
		
		/*
		this.compacttags.add("a");
		this.compacttags.add("abbr");
		this.compacttags.add("b");
		this.compacttags.add("button");
		this.compacttags.add("caption");
		this.compacttags.add("cite");
		this.compacttags.add("em");
		this.compacttags.add("i");
		this.compacttags.add("label");
		this.compacttags.add("legend");
		this.compacttags.add("mark");
		this.compacttags.add("q");
		this.compacttags.add("s");
		this.compacttags.add("small");
		this.compacttags.add("span");
		this.compacttags.add("strong");
		this.compacttags.add("title");
		this.compacttags.add("u");
		*/
	}
	
	protected String valueMacro(String value, XElement scope) {
		if (scope instanceof UIElement) 
			value = ((UIElement)scope).expandMacro(this.ctx, value);
		
		return value;
	}
	
	@Override
	public void print(XNode root) {
    	this.out.append("<!DOCTYPE html>\n");
		
		super.print(root);
	}
	
	public void printFormatLead(int level) {
		// Add leading newline and spaces, if necessary
		if (formatted && level > 0) {
			this.out.append("\n");
			for (int i = level; i > 0; i--)
				this.out.append("\t");
		}
	}
	
	@Override
	public void print(XNode node, int level, XElement parent) {
		if (node instanceof XText) {
			String val = this.valueMacro(((XText) node).getRawValue(), parent);
			
			if (val != null) {
				this.printFormatLead(level);
				this.out.append(val); 
			}
		}
		else if (node instanceof XElement) {
			this.printFormatLead(level);
			
			XElement el = (XElement) node;
			
			String name = el.getName();
			
			if (!Character.isLowerCase(name.charAt(0)) || name.contains(".")) {
				this.out.append("<!-- found unsupported tag " + name + " -->");
				
				if (formatted)
					this.out.append("\n");
				
				return;
			}				
			
			// Put the opening tag out
			this.out.append("<" + name);
	
			// Write the attributes out
			if (el.hasAttributes()) 
				for (Map.Entry<String, String> entry : el.getAttributes().entrySet()) {
					String aname = entry.getKey();
					
					// remove all uppercase led attrs
					if (Character.isLowerCase(aname.charAt(0))) {
						this.out.append(" " + aname + "=");
						
						// note that we do not officially support and special entities in our code
						// except for the XML standards of &amp; &lt; &gt; &quot; &apos;
						// while entities - including &nbsp; - may work in text nodes we aren't supporting
						// them in attributes and suggest the dec/hex codes - &spades; should be &#9824; or &#x2660;
						String normvalue = XNode.unquote(entry.getValue());
						String expandvalue = this.valueMacro(normvalue, el);
						
						this.out.append("\"" + XNode.quote(expandvalue) + "\"");		
					}
				}
	
			// write out the closing tag or other elements
			boolean formatThis = formatted;
			boolean fndelement = false;
			
			if (!el.hasChildren() && this.selfclosers.contains(name) ) {
				this.out.append(" /> ");
			} 
			else if (!el.hasChildren()) {
				this.out.append(">");
				this.out.append("</" + name + "> ");
			} 
			else {
				this.out.append(">");
				
				for (XNode cnode : el.getChildren()) {
					if (cnode instanceof XText)
						formatThis = false;
					else
						fndelement = true;		
				}
				
				// Add leading newline and spaces, if necessary
				if ((formatThis || fndelement) && formatted) {
					for (XNode cnode : el.getChildren()) 
						this.print(cnode, level + 1, el);
					
					this.out.append("\n");
					
					for (int i = level; i > 0; i--)
						this.out.append("\t");
				}
				else {
					for (XNode cnode : el.getChildren()) 
						this.print(cnode, 0, el);
				}
				
				// Now put the closing tag out
				this.out.append("</" + name + "> ");
			}
		}
	}
}
