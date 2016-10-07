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
package dcraft.api;

import dcraft.xml.XElement;

public interface IApiSessionFactory {
	// start a connector
	void init(XElement config);

	ApiSession create();
	ApiSession create(XElement config);
}
