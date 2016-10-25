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

import dcraft.lang.op.FuncResult;
import dcraft.lang.op.OperationResult;
import dcraft.struct.ListStruct;
import dcraft.xml.XElement;

public interface ISchedulerDriver {
	void init(OperationResult or, XElement config);	
	void start(OperationResult or);	
	void stop(OperationResult or);	
	
	FuncResult<ListStruct> loadSchedule();
	FuncResult<ScheduleEntry> loadEntry(String id);
}
