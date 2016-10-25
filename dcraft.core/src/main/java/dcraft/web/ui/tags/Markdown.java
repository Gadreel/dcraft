package dcraft.web.ui.tags;

import java.io.IOException;
import java.lang.ref.WeakReference;

import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.xml.XElement;

public class Markdown extends UIElement {
	public Markdown() {
		super("dc.Markdown");
	}
	
	@Override
	public void build(WeakReference<UIWork> work) {
    	String mode = this.getAttribute("Mode", "Unsafe");
    	String content = this.getText();
		
		// TODO allocate from webdomain
    	dcraft.web.md.Markdown mdp = new dcraft.web.md.Markdown();

		try {
			XElement root = "Safe".equals(mode) 
				? mdp.processSafe2(work.get().getContext(), content)
				: mdp.process2(work.get().getContext(), content);
				
			// root is just a container and has no value
			this.replaceChildren(root);
		} 
		catch (IOException x) {
			System.out.println("inline md error: " + x);
		}
    	
		this.setName("div");
		
		super.build(work);
	}
}
