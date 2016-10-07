package dcraft.service;

import groovy.lang.GroovyObject;

import java.nio.file.Files;
import java.nio.file.Path;

import dcraft.groovy.GCompClassLoader;
import dcraft.hub.DomainInfo;
import dcraft.lang.op.OperationContext;

public class DomainWatcherAdapter {
	protected Path domainpath = null;
	protected GroovyObject script = null;
	
	public DomainWatcherAdapter(Path dpath) {
		this.domainpath = dpath;
	}
	
	public GroovyObject getScript() {
		return this.script;
	}

	public void init(DomainInfo domaininfo) {
		if (this.script != null) {
			GCompClassLoader.tryExecuteMethod(this.script, "Kill", domaininfo);
			this.script = null;
		}
		
		Path cpath = this.domainpath.resolve("config");

		if (Files.notExists(cpath))
			return;
		
		Path spath = cpath.resolve("Watcher.groovy");
		
		if (Files.notExists(spath))
			return;
		
		try {
			Class<?> groovyClass = domaininfo.getRootSite().getScriptLoader().toClass(spath);
			
			this.script = (GroovyObject) groovyClass.newInstance();
			
			GCompClassLoader.tryExecuteMethod(this.script, "Init", domaininfo);
		}
		catch (Exception x) {
			OperationContext.get().error("Unable to prepare domain watcher script: " + spath);
			OperationContext.get().error("Error: " + x);
		}
	}
	
	public boolean tryExecuteMethod(String name, Object... params) {
		return GCompClassLoader.tryExecuteMethod(this.script, name, params);
	}
}
