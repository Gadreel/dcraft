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
package dcraft.sql;

public enum SqlNull {
	DateTime,
	VarChar,
	Long,
	Int,
	Double,
	BigDecimal,
	Text;
	
	public Object orValue(Object value) {
		if (value != null)
			return value;
		
		return this;
	}
}
