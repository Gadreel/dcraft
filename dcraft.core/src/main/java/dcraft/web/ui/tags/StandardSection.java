package dcraft.web.ui.tags;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.xml.XNode;

public class StandardSection extends Section {
	public StandardSection() {
		super("dc.StandardSection");
	}
	
	@Override
	public UIElement newNode() {
		return new StandardSection();
	}
	
	@Override
	public void build(WeakReference<UIWork> work) {
    	String mode = this.getAttribute("Mode", "Unsafe");
    	String content = this.getText();
		
    	this.clearChildren();
    	
		// TODO allocate from webdomain
    	dcraft.web.md.Markdown mdp = new dcraft.web.md.Markdown();

		try {
			UIElement root = "Safe".equals(mode) 
				? mdp.processSafe2(work.get().getContext(), content)
				: mdp.process2(work.get().getContext(), content);
				
			// root is just a container and has no value
			this.add(root);
		} 
		catch (IOException x) {
			this.with(new UIElement("InvalidContent"));
			System.out.println("inline md error: " + x);
		}
        
		super.build(work);
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		super.translate(work, pnodes);

		this.withClass("dc-section-standard")
			.withAttribute("data-dccms-plugin", "Standard");
	}
}
