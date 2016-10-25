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
package dcraft.web.core;

import java.util.HashMap;

public class ValuesMacro implements IOutputMacro {
	protected HashMap<String, String> values = new HashMap<>();
	
	public void add(String macro, String value) {
		this.values.put(macro, value);
	}
	
	@Override
	public String process(IOutputContext ctx, String... macro) {
		if (macro.length > 0)
			return this.values.get(macro[0]);
		
		return null;
	}

	public boolean hasKey(String macro) {
		return this.values.containsKey(macro);
	}
}
