package dcraft.cms.util;

import dcraft.hub.Hub;
import dcraft.lang.op.OperationResult;
import dcraft.script.Activity;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.util.StringUtil;
import dcraft.work.Task;
import dcraft.xml.XElement;

public class EventUtil {
	static public void triggerEvent(String name, String alternate, Struct data) {
		if (StringUtil.isEmpty(name))
			return;
		
		name = "Event-" + name;
		
		XElement catalog = CatalogUtil.getCatalog(name, alternate);
		
		if (catalog == null)
			return;

		XElement settings = catalog.find("Settings");

		String mode = "dcScript";
		
		if (settings != null)
			mode = settings.getAttribute("Mode", mode);
		
		if ("dcScript".equals(mode)) {
			Activity act = new Activity();
			
			XElement screl = catalog.find("dcScript");
			
			OperationResult compilelog = act.compile(screl);
			
			if (compilelog.hasErrors()) {
				// TODO cleanup
				System.out.println("Error compiling script: " + compilelog.getMessage());
				return;
			}
			
			// TODO use settings to configure task
			
			// use site root, not current user - event actions are tied to System
			Task task = Task.taskWithSiteRootContext()
				.withTitle(screl.getAttribute("Title", "Event Script " + name))	
				.withTopic("Batch")
				.withTimeout(1)	
				.withParams(new RecordStruct().withField("Data", data))
				.withWork(act);
			
			Hub.instance.getWorkPool().submit(task);
		}
		
		// TODO support Class and Groovy
	}
}
