package dcraft.web.ui.tags;

import dcraft.web.ui.UIElement;

public class ServerScript extends UIElement {
	public ServerScript() {
		super("dc.ServerScript");
	}
	
	@Override
	public UIElement newNode() {
		return new ServerScript();
	}
}
