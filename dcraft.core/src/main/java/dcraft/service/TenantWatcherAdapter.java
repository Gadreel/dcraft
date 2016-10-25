package dcraft.service;

import groovy.lang.GroovyObject;

import java.nio.file.Files;
import java.nio.file.Path;

import dcraft.groovy.GCompClassLoader;
import dcraft.hub.TenantInfo;
import dcraft.log.Logger;

public class TenantWatcherAdapter {
	protected Path tenantpath = null;
	protected GroovyObject script = null;
	
	public TenantWatcherAdapter(Path dpath) {
		this.tenantpath = dpath;
	}
	
	public GroovyObject getScript() {
		return this.script;
	}

	public void init(TenantInfo domaininfo) {
		if (this.script != null) {
			GCompClassLoader.tryExecuteMethod(this.script, "Kill", domaininfo);
			this.script = null;
		}
		
		Path cpath = this.tenantpath.resolve("config");

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
			Logger.error("Unable to prepare domain watcher script: " + spath);
			Logger.error("Error: " + x);
		}
	}
	
	public boolean tryExecuteMethod(String name, Object... params) {
		return GCompClassLoader.tryExecuteMethod(this.script, name, params);
	}
}
