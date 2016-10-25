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

public interface IStream {
	void setUpstream(IStream upstream);
	void setDownstream(IStream downstream);
	
	ReturnOption handle(FileSlice slice);
	void read();
	
	IStreamSource getOrigin();
	void cleanup();
	void close();
}
