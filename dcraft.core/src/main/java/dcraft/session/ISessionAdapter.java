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

import dcraft.bus.Message;
import dcraft.lang.op.UserContext;
import dcraft.struct.ListStruct;

public interface ISessionAdapter {
	void deliver(Message msg);
	ListStruct popMessages();
	void stop();
	String getClientKey();
	void UserChanged(UserContext user);
}
