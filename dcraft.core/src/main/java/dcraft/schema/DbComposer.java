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

import dcraft.db.IComposer;
import dcraft.hub.Hub;
import dcraft.lang.op.OperationContext;
import dcraft.util.StringUtil;

public class DbComposer {
	public String name = null;
	public String execute = null;
	public String[] securityTags = null;
	public IComposer sp = null;
	
	public IComposer getComposer() {
		if (this.sp != null)
			return this.sp;
		
		// composer should be stateless, save effort by putting 1 instance inside DbComposer and reusing it
		if (StringUtil.isNotEmpty(this.execute)) 
			this.sp = (IComposer) Hub.instance.getInstance(this.execute);
		
		if (this.sp == null)
			OperationContext.get().error("Composer " + this.name + " failed to create.");
		
		return this.sp;
	}
}