package dcraft.web.ui.tags;

import java.lang.ref.WeakReference;
import java.util.List;

import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.xml.XElement;
import dcraft.xml.XNode;

public class QotDBlock extends UIElement {
	public QotDBlock() {
		super("dc.QotDBlock");
	}
	
	@Override
	public void expand(WeakReference<UIWork> work) {
		// TODO add parameters and backend data files to this
		
		// test the Build step ability to convert XElement to UIElement 
		
		this
			.with(new XElement("h3").withText("Quote of the Day!"))
			.with(new XElement("blockquote")
				.with(new XElement("p")
					.withText("My soul can find no staircase to Heaven unless it be through Earth's loveliness.")
				)
				.with(new XElement("p")
					.withAttribute("class", "quoteby")
					.withText("- Michelangelo")
				)
			);
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		// don't change my identity until after the scripts run
		this.setName("div");
		this.withAttribute("class", "dcw-qotd");
		
		super.translate(work, pnodes);
	}
}
