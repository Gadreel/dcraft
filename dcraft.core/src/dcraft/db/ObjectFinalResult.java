/* ************************************************************************
#
#  designCraft.io
#
#  http://designcraft.io/
#
#  Copyright:
#    Copyright 2012 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */
package dcraft.db;

import dcraft.struct.CompositeStruct;
import dcraft.work.TaskRun;

/**
 * 
 * @author Andy
 *
 */
public class ObjectFinalResult extends ObjectResult {
	protected TaskRun request = null;
	
	public ObjectFinalResult(TaskRun request) {
		this.request = request;
	}
	
	@Override
	public void process(CompositeStruct result) {
		this.request.returnValue(result);
	}
}
