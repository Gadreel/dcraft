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
package dcraft.lang.op;

import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;

// TODO probably not needed
public class ParentLogger extends OperationObserver {
	protected OperationContext opcontext = null;
	
	public ParentLogger(OperationContext ctx) {
		this.opcontext = ctx;
	}
	
	@Override
	public Struct deepCopy() {
		ParentLogger cp = new ParentLogger(this.opcontext);
		this.doCopy(cp);
		return cp;
	}
	
	@Override
	public void log(OperationContext ctx, RecordStruct entry) {
		this.opcontext.log(entry);
	}
}