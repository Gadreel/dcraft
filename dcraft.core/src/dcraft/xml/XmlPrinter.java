package dcraft.xml;

import java.io.PrintStream;
import java.util.Map;

public class XmlPrinter {
	protected boolean formatted = true;
	protected PrintStream out = null;		// only use the "append" functions - can be piped
	
	public void setOut(PrintStream v) {
		this.out = v;
	}
	
	public void setFormatted(boolean v) {
		this.formatted = v;
	}
	
	public void print(XNode root) {
		this.print(root, 0, null);
	}
	
	public void print(XNode node, int level, XElement parent) {
		// Add leading newline and spaces, if necessary
		if (formatted && level > 0) {
			this.out.append("\n");
			for (int i = level; i > 0; i--)
				this.out.append("\t");
		}

		if (node instanceof XText) {
			XText txt = (XText) node;
					
			if (txt.cdata) {
				this.out.append("<![CDATA[");
				this.out.append(txt.content);
				this.out.append("]]>");

				/*  TODO fix to support ]]> in content
				 * 
				 * You do not escape the ]]> but you escape the > after ]] by inserting ]]><![CDATA[ before the >, think 
				 * of this just like a \ in C/Java/PHP/Perl string but only needed before a > and after a ]].
				 */
			}
			else
				this.out.append(txt.content);
		}
		else if (node instanceof XElement) {
			XElement el = (XElement) node;
			
			// Put the opening tag out
			this.out.append("<" + el.tagName);
	
			// Write the attributes out
			if (el.attributes != null) 
				for (Map.Entry<String, String> entry : el.attributes.entrySet()) {
					this.out.append(" " + entry.getKey() + "=");
					this.out.append("\"" + entry.getValue() + "\"");
				}
	
			// write out the closing tag or other elements
			boolean formatThis = formatted;
			boolean fndelement = false;
			
			if (!el.hasChildren()) {
				this.out.append(" /> ");
			} 
			else {
				this.out.append(">");
				
				for (XNode cnode : el.children) {
					if (cnode instanceof XText)
						formatThis = false;
					else
						fndelement = true;		
	
					this.print(cnode, level + 1, el);
				}
				
				// Add leading newline and spaces, if necessary
				if (formatThis || fndelement) {
					this.out.append("\n");
					
					for (int i = level; i > 0; i--)
						this.out.append("\t");
				}
				
				// Now put the closing tag out
				this.out.append("</" + el.tagName + "> ");
			}
		}
	}
}
