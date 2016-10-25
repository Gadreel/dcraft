package dcraft.web.md;

import dcraft.web.core.IOutputContext;
import dcraft.web.md.Configuration;

public class ProcessContext {
	protected Configuration config = null;
	protected IOutputContext outcontext = null;
	
	public Configuration getConfig() {
		return this.config;
	}
	
	public void setConfig(Configuration v) {
		this.config = v;
	}
	
	public IOutputContext getOutput() {
		return this.outcontext;
	}
	
	public void setOutput(IOutputContext v) {
		this.outcontext = v;
	}
	
	public ProcessContext(Configuration config, IOutputContext web) {
		this.config = config;
		this.outcontext = web;
	}
}
