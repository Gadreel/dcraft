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

import dcraft.script.ExecuteState;
import dcraft.script.Instruction;
import dcraft.script.LogicBlockInstruction;
import dcraft.script.StackEntry;
import dcraft.struct.ScalarStruct;
import dcraft.struct.Struct;

public class BreakIf extends Instruction {
	@Override
	public void run(final StackEntry stack) {
        Struct target = this.source.hasAttribute("Target")
        		? stack.refFromElement(this.source, "Target")
        	    : stack.queryVariable("_LastResult");

        if (LogicBlockInstruction.checkLogic(stack, (ScalarStruct)target, this.source))		
        	stack.setState(ExecuteState.Break);
        else
        	stack.setState(ExecuteState.Done);
        
		stack.resume();
	}
	
	@Override
	public void cancel(StackEntry stack) {
		// do nothing, this isn't cancellable
	}
}
