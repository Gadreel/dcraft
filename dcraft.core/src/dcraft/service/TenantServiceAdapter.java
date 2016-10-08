package dcraft.service;

import groovy.lang.GroovyObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import dcraft.bus.IService;
import dcraft.bus.Message;
import dcraft.groovy.GCompClassLoader;
import dcraft.hub.TenantInfo;
import dcraft.lang.op.OperationContext;
import dcraft.work.TaskRun;

public class TenantServiceAdapter implements IService {
	protected String name = null;
	protected Path sourcepath = null;
	protected Path domainpath = null;
	protected Map<String, ServiceFeature> features = new HashMap<String, ServiceFeature>();
	
	public TenantServiceAdapter(String name, Path spath, Path dpath) {
		this.name = name;
		this.sourcepath = spath;
		this.domainpath = dpath;
	}
	
	public GroovyObject getScript(TenantInfo domain, String name) {
		ServiceFeature f = this.getFeature(domain, name);
		
		if (f != null) 
			return f.script;
		
		return null;
	}
	
	public ServiceFeature getFeature(TenantInfo domain, String name) {
		ServiceFeature f = this.features.get(name);
		
		if (f == null) {
			f = new ServiceFeature(domain, name);
			this.features.put(name, f);
		}
		
		return f;
	}
	
	@Override
	public String serviceName() {
		return this.name;
	}

	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		
		ServiceFeature f = this.getFeature(request.getContext().getTenant(), feature);
		
		if (f != null)
			f.handle(request);
	}
	
	public class ServiceFeature {
		protected GroovyObject script = null;
		
		public ServiceFeature(TenantInfo domain, String feature) {
			Path spath = TenantServiceAdapter.this.sourcepath.resolve(feature + ".groovy");
			
			if (Files.notExists(spath))
				return;
			
			try {
				Class<?> groovyClass = domain.getRootSite().getScriptLoader().toClass(spath);
				
				this.script = (GroovyObject) groovyClass.newInstance();
			}
			catch (Exception x) {
				OperationContext.get().error("Unable to prepare service script: " + spath);
				OperationContext.get().error("Error: " + x);
			}		
		}
		
		public void handle(TaskRun request) {
			Message msg = (Message) request.getTask().getParams();
			
			String feature = msg.getFieldAsString("Feature");
			String op = msg.getFieldAsString("Op");
			
			if (! GCompClassLoader.tryExecuteMethod(this.script, op, request, msg.getProperty("Body"))) {
				request.errorTr(441, TenantServiceAdapter.this.serviceName(), feature, op);
				request.complete();
			}
		}
	}
}
