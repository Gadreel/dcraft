package dcraft.web.md;

import java.util.List;
import java.util.Map;

import dcraft.web.md.ProcessContext;
import dcraft.web.ui.UIElement;

public abstract class Plugin {
	protected String idPlugin;
	
	public Plugin(String idPlugin) {
		this.idPlugin = idPlugin;
	}

	public abstract void emit(ProcessContext ctx, UIElement parent, List<String> lines, Map<String, String> params);

	public String getIdPlugin() {
		return idPlugin;
	}
}
