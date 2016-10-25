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
import dcraft.script.StackEntry;
import dcraft.struct.Struct;

public class ReturnIfErrored extends Instruction {
	@Override
	public void run(final StackEntry stack) {
		if (!stack.getActivity().hasErrored()) {
			stack.setState(ExecuteState.Done);
			stack.resume();
			return;
		}
		
        if (this.source.hasAttribute("Target")) {
        	Struct target = stack.refFromElement(this.source, "Target");        	
        	stack.getExecutingStack().setLastResult(target);
        }
        
        if (stack.boolFromSource("ResetFlag", true))
        	stack.getActivity().clearErrored();
        
    	stack.setState(ExecuteState.Break);
		stack.resume();
	}
	
	@Override
	public void cancel(StackEntry stack) {
		// do nothing, this isn't cancellable
	}
}
