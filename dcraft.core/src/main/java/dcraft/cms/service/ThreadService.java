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
import dcraft.db.DataRequest;
import dcraft.db.ObjectFinalResult;
import dcraft.db.ReplicatedDataRequest;
import dcraft.hub.Hub;
import dcraft.mod.ExtensionBase;
import dcraft.work.TaskRun;

// TODO refactor into better service structure
public class ThreadService extends ExtensionBase implements IService {	
	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");
				
		// =========================================================
		//  cms Threads
		// =========================================================
		
		if ("Threads".equals(feature)) {
			DataRequest req = null;
			
			if ("NewThread".equals(op) || "UpdateThreadCore".equals(op) || "ChangePartiesAction".equals(op) || "ChangeFolderAction".equals(op) || "AddContentAction".equals(op) || "ChangeStatusAction".equals(op) || "ChangeLabelsAction".equals(op))
				req = new ReplicatedDataRequest("dcmThread" + op)
					.withParams(msg.getFieldAsComposite("Body"));
			else if ("ThreadDetail".equals(op) || "FolderListing".equals(op) || "FolderCounting".equals(op))
				req = new DataRequest("dcmThread" + op)
					.withParams(msg.getFieldAsComposite("Body"));
			
			if (req != null) {
				Hub.instance.getDatabase().submit(req, new ObjectFinalResult(request));
				return;
			}
		}
				
		request.errorTr(441, this.serviceName(), feature, op);
		request.complete();
	}
}
