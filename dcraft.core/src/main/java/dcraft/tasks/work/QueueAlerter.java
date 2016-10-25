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
package dcraft.tasks.work;

import dcraft.lang.op.OperationResult;
import dcraft.work.IQueueAlerter;
import dcraft.xml.XElement;

// TODO this is an out dated system, move to use Hub Events instead 
public class QueueAlerter implements IQueueAlerter {
	@Override
	public void init(OperationResult or, XElement config) {
	}

	@Override
	public void sendAlert(long code, Object... params) {
		// if could not start task
		if (code == 179) {
			// maybe filter some
		}
		
		//Email.sendOperatorAlert(code, params);
	}
}
