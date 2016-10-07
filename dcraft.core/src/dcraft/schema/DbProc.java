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
package dcraft.schema;

public class DbProc {
	public String name = null;
	public String execute = null;
	public String[] securityTags = null;
	
	public DataType request = null;
	public DataType response = null;
}