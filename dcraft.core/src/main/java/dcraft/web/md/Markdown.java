package dcraft.web.md;

import java.io.IOException;

import dcraft.web.core.IOutputContext;
import dcraft.web.md.Configuration;
import dcraft.web.md.Markdown;
import dcraft.web.md.Plugin;
import dcraft.web.md.Processor;
import dcraft.web.md.plugin.GallerySection;
import dcraft.web.md.plugin.HtmlSection;
import dcraft.web.md.plugin.PairedMediaSection;
import dcraft.web.md.plugin.StandardSection;
import dcraft.web.ui.UIElement;

// TODO config should be per domain / website
public class Markdown {
	protected Configuration unsafeconfig = null;
	protected Configuration safeconfig = null;
	
	public Configuration getUnsafeConfig() {
		return this.unsafeconfig;
	}
	
	public Configuration getSafeConfig() {
		return this.safeconfig;
	}
	
	public Markdown() {
		this.unsafeconfig = new Configuration()
			.setSafeMode(false)
			.registerPlugins(new PairedMediaSection(), new StandardSection(), new GallerySection(), new HtmlSection());
						
			// TODO
			//.registerPlugins(new YumlPlugin(), new WebSequencePlugin(), new IncludePlugin());
		
		this.safeconfig = new Configuration();
	}
	
	public Markdown registerPlugins(Plugin ... plugins) {
		this.unsafeconfig.registerPlugins(plugins);
		return this;
	}
	
	public UIElement process2(IOutputContext ctx, String input) throws IOException {
		return Processor.process(ctx, input, this.unsafeconfig);
	}
	
	public UIElement processSafe2(IOutputContext ctx, String input) throws IOException {
		return Processor.process(ctx, input, this.safeconfig);
	}
}
