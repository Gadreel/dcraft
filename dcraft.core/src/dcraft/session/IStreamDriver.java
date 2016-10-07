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

import dcraft.bus.net.StreamMessage;

public interface IStreamDriver {
	void message(StreamMessage msg);
	void cancel();
	void nextChunk();		// for source, tell it to send more
}
