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
package dcraft.filestore;

import dcraft.lang.op.OperationCallback;
import dcraft.session.DataStreamChannel;
import dcraft.session.IStreamDriver;

// TODO rename
public interface IFileStoreStreamDriver extends IStreamDriver {
	void init(DataStreamChannel channel, OperationCallback cb);
}
