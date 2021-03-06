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
package dcraft.script.inst;

import dcraft.lang.op.OperationContext;
import dcraft.script.Ops;
import dcraft.script.StackEntry;
import dcraft.struct.ScalarStruct;
import dcraft.struct.Struct;
import dcraft.xml.XElement;

public class With extends Ops {
	@Override
	public void prepTarget(StackEntry stack) {
        Struct var = stack.refFromSource("Target");

        this.setTarget(stack, var);
        
		if (stack.codeHasAttribute("SetTo")) {
	        Struct var3 = stack.refFromSource("SetTo");
							
			if (var instanceof ScalarStruct) 
				((ScalarStruct) var).adaptValue(var3);
			else 
				OperationContext.get().errorTr(540);
		}
		
		this.nextOpResume(stack);
	}
	
	@Override
	public void runOp(StackEntry stack, XElement op, Struct target) {
		stack.operate(target, op);
	}
	
	@Override
	public void cancel(StackEntry stack) {
		// do nothing, this isn't cancellable
	}
}
