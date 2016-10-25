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
package dcraft.scheduler;

import dcraft.lang.op.IOperationObserver;
import dcraft.struct.RecordStruct;
import dcraft.work.Task;
import dcraft.xml.XElement;

public interface ISchedule extends IOperationObserver {
	void init(XElement config);
	Task task();
	void setTask(Task v);
	boolean reschedule();
	long when();
	RecordStruct getHints();
	void cancel();
	boolean isCanceled();
}
