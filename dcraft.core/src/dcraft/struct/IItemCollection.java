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
package dcraft.struct;

import dcraft.util.IAsyncIterable;

public interface IItemCollection {
	Iterable<Struct> getItems();
	IAsyncIterable<Struct> getItemsAsync();
}
