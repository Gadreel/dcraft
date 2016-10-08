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
import dcraft.struct.FieldStruct;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.work.TaskRun;
import dcraft.xml.XElement;

public class TenantsService extends ExtensionBase implements IService {

	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");
		
		if ("Manager".equals(feature)) {
			if ("LoadAll".equals(op)) {
				// AuthService should provide Tenants config, so no default is needed here
				XElement mdomains = Hub.instance.getConfig().selectFirst("Tenants");
				
				if (mdomains == null) {
					request.error("No Tenants found");
					request.complete();
				}
				
				ListStruct res = new ListStruct();
				
				for (XElement mdomain : mdomains.selectAll("Tenant")) {
					// this are just some of the possible names - others are in local - gives a taste of the names used
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
				
				request.returnValue(res);
				return;
			}			
		}
		
		request.errorTr(441, this.serviceName(), feature, op);
		request.complete();
	}
	
	@Override
	public String serviceName() {
		return "dcTenants";
	}
}
