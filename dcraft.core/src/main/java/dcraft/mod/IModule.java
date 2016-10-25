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
package dcraft.mod;

import dcraft.xml.XElement;

public interface IModule {
	void setLoader(ModuleLoader v);	
	ModuleLoader getLoader();
	void init(XElement config);
	long startTime();
	void start();
	void stop(); 
}
