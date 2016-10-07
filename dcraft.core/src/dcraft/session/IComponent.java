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
package dcraft.session;

import dcraft.lang.op.OperationResult;
import dcraft.struct.RecordStruct;

public interface IComponent {
	void start(String name, Session session, RecordStruct msg, OperationResult errs);
	void call(RecordStruct msg, OperationResult errs);		
	void end(RecordStruct msg, OperationResult errs);
}
