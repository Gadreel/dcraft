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
package dcraft.cms.service;

import dcraft.bus.IService;
import dcraft.bus.Message;
import dcraft.bus.MessageUtil;
import dcraft.cms.util.EventUtil;
import dcraft.mod.ExtensionBase;
import dcraft.struct.RecordStruct;
import dcraft.work.TaskRun;

public class CoreService extends ExtensionBase implements IService {	
	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");
				
		// =========================================================
		//  Events
		// =========================================================
		
		if ("Events".equals(feature)) {
			if ("Trigger".equals(op)) {
				this.handleEventTrigger(request);
				return;
			}
		}
		
		request.errorTr(441, this.serviceName(), feature, op);
		request.complete();
	}
	
	public void handleEventTrigger(TaskRun request) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		
		EventUtil.triggerEvent(rec.getFieldAsString("Event"), rec.getFieldAsString("Alternate"), 
				rec.getFieldAsStruct("Data"));
		
		request.returnEmpty();
		return;
	}
}
