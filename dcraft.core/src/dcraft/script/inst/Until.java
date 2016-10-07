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

import dcraft.lang.op.OperationCallback;
import dcraft.script.ExecuteState;
import dcraft.script.LogicBlockInstruction;
import dcraft.script.StackBlockEntry;
import dcraft.script.StackEntry;

public class Until extends LogicBlockInstruction {
    @Override
    public void alignInstruction(final StackEntry stack, OperationCallback callback) {
    	StackBlockEntry bstack = (StackBlockEntry)stack;
    	
    	// signal end if conditional logic fails after loop
    	if (bstack.getPosition() >= this.instructions.size()) 
    		if (!this.checkLogic(stack))
    			bstack.setPosition(0);
    		else
	        	stack.setState(ExecuteState.Done);
    	
       	super.alignInstruction(stack, callback);
    }
}
