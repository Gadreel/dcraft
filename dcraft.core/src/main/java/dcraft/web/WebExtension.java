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
package dcraft.web;

import dcraft.bus.IService;
import dcraft.bus.Message;
import dcraft.bus.MessageUtil;
import dcraft.mod.ExtensionBase;
import dcraft.web.core.IWebExtension;
import dcraft.work.TaskRun;

public class WebExtension extends ExtensionBase implements IService, IWebExtension {	
	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");
		
		// TODO log?
		//System.out.println("web ex: " + feature + "-" + op);
		
		request.setResult(MessageUtil.errorTr(441, this.serviceName(), feature, op));
		request.complete();
	}
}
