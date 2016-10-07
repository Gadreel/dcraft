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
package dcraft.util;

import dcraft.lang.op.FuncCallback;

public interface IAsyncIterator<T> {
	void hasNext(FuncCallback<Boolean> callback);
	void next(FuncCallback<T> callback);
}
