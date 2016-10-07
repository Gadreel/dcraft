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
package dcraft.ctp.stream;

import dcraft.script.StackEntry;
import dcraft.xml.XElement;

public interface IStreamDest extends IStream {
	void init(StackEntry stack, XElement el, boolean autorelative);
	void execute();
}
