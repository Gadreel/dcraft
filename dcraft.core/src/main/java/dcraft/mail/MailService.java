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
package dcraft.mail;

import dcraft.bus.IService;
import dcraft.bus.Message;
import dcraft.lang.op.FuncResult;
import dcraft.mod.ExtensionBase;
import dcraft.struct.RecordStruct;
import dcraft.work.Task;
import dcraft.work.TaskRun;

public class MailService extends ExtensionBase implements IService {
	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");
		
		if ("Message".equals(feature)) {
			if ("Send".equals(op)) {
				Task task = MailUtil.createSendTask(msg.getFieldAsRecord("Body"));
				
				FuncResult<RecordStruct> ares = MailUtil.submit(task);
				
				if (!ares.hasErrors()) 
					request.setResult(ares.getResult());
				
				request.complete();
				return;
			}
			
			if ("BuildSend".equals(op)) {
				Task task = MailUtil.createSendBuildTask(msg.getFieldAsRecord("Body"));
				
				FuncResult<RecordStruct> ares = MailUtil.submit(task);
				
				if (!ares.hasErrors()) 
					request.setResult(ares.getResult());
				
				request.complete();
				return;
			}
		}
		
		request.errorTr(441, this.serviceName(), feature, op);
		request.complete();
	}
}
