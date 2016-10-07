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
import dcraft.script.Activity;
import dcraft.script.BlockInstruction;
import dcraft.script.ExecuteState;
import dcraft.script.IInstructionCallback;
import dcraft.script.StackEntry;
import dcraft.script.StackFunctionEntry;

public class Main extends BlockInstruction {
	@Override
	public void run(final StackEntry stack) {
		StackFunctionEntry fstack = (StackFunctionEntry)stack;
		
		// first time through init steps
		if (stack.getState() == ExecuteState.Ready) {
			OperationContext.get().setSteps((int)stack.intFromSource("Steps", 0));			
			fstack.setParameterName(stack.stringFromSource("Parameter"));
		}
		
		stack.updateCallback(new IInstructionCallback() {
			@Override
			public void resume() {
				if (stack.getState() == ExecuteState.Done) 
					stack.getActivity().setExitFlag(true);
			}
		});

		super.run(stack);
	}
	
	@Override
	public StackEntry createStack(Activity act, StackEntry parent) {
		return new StackFunctionEntry(act, parent, this);
	}
}
