package dcraft.web.mdx;

import java.io.IOException;

import dcraft.web.core.WebContext;
import dcraft.web.mdx.Configuration;
import dcraft.web.mdx.Markdown;
import dcraft.web.mdx.Plugin;
import dcraft.web.mdx.Processor;
import dcraft.web.mdx.plugin.GallerySection;
import dcraft.web.mdx.plugin.HtmlSection;
import dcraft.web.mdx.plugin.PairedMediaSection;
import dcraft.web.mdx.plugin.StandardSection;
import dcraft.xml.XElement;

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
	
	public XElement process2(WebContext ctx, String input) throws IOException {
		return Processor.process2(ctx, input, this.unsafeconfig);
	}
	
	public XElement processSafe2(WebContext ctx, String input) throws IOException {
		return Processor.process2(ctx, input, this.safeconfig);
	}
}
