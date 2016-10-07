/* ************************************************************************
#
#  designCraft.io
#
#  http://designcraft.io/
#
#  Copyright:
#    Copyright 2014 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */
package dcraft.service.simple;

import dcraft.bus.IService;
import dcraft.bus.Message;
import dcraft.hub.Hub;
import dcraft.mod.ExtensionBase;
import dcraft.mod.ExtensionLoader;
import dcraft.struct.FieldStruct;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.work.TaskRun;
import dcraft.xml.XElement;

public class DomainsService extends ExtensionBase implements IService {

	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");
		
		if ("Manager".equals(feature)) {
			if ("LoadAll".equals(op)) {
				XElement mdomains = Hub.instance.getConfig().selectFirst("Domains");
				
				if (mdomains != null) {
					ListStruct res = new ListStruct();
					
					for (XElement mdomain : mdomains.selectAll("Domain")) {
						ListStruct names = new ListStruct();
						
						for (XElement del : mdomain.selectAll("Name"))
							names.addItem(del.getText());
						
						res.addItem(new RecordStruct(
								new FieldStruct("Id", mdomain.getAttribute("Id")),
								new FieldStruct("Alias", mdomain.getAttribute("Alias")),
								new FieldStruct("Title", mdomain.getAttribute("Title")),
								new FieldStruct("Names", names),
								new FieldStruct("Settings", Struct.objectToStruct(mdomain.find("Settings")))
							)
						);
					}
					
					request.setResult(res);
				}
				else {
					ListStruct names = new ListStruct("root", "localhost");
					
					ExtensionLoader el = this.getLoader(); 
					
					XElement config = null;
					
					if (el != null) {
						config = el.getSettings();
						
						if (config != null)
							for (XElement del : config.selectAll("Domain"))
								names.addItem(del.getAttribute("Name"));
					}
					
					request.setResult(new ListStruct(
							new RecordStruct(
									new FieldStruct("Id", "00000_000000000000001"),
									new FieldStruct("Title", "root"),
									new FieldStruct("Alias", "root"),
									new FieldStruct("Names", names),
									new FieldStruct("Settings", Struct.objectToStruct(config))
							)
					));
				}
				
				request.complete();
				return;
			}			
		}
		
		request.errorTr(441, this.serviceName(), feature, op);
		request.complete();
	}
	
	@Override
	public String serviceName() {
		// TODO if through Loader then get name there (super)
		return "dcDomains";
	}
}
