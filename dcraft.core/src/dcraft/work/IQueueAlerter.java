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
package dcraft.work;

import dcraft.lang.op.OperationResult;
import dcraft.xml.XElement;

public interface IQueueAlerter {
	void init(OperationResult or, XElement config);	
	void sendAlert(long code, Object... params);
}
