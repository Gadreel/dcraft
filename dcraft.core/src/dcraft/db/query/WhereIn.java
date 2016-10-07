/* ************************************************************************
#
#  designCraft.io
#
#  http://designcraft.io/
#
#  Copyright:
#    Copyright 2012 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */
package dcraft.db.query;

import dcraft.util.StringUtil;

public class WhereIn extends WhereExpression {
	public WhereIn(Object a, String... b) {
		super("In");
		
		this.addValue("A", a);
		this.addValue("B", "|" + StringUtil.join(b,"|") + "|");
	}
	
	public WhereIn(IWhereField a, String... b) {
		super("In");
		
		this.addField("A", a);
		this.addValue("B", "|" + StringUtil.join(b,"|") + "|");
	}
}
