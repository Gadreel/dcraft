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

import dcraft.script.BlockInstruction;
import dcraft.script.ExecuteState;
import dcraft.script.StackEntry;

public class IfErrored extends BlockInstruction {
    @Override
    public void run(final StackEntry stack) {
        // if we do not pass logical condition then mark as done so we will skip this block
        // note that for the sake of nice debugging we do not set Done state here, would cause skip in debugger
		if (stack.getState() == ExecuteState.Ready) {
            boolean pass = stack.getActivity().hasErrored();
            
			stack.getStore().setField("Pass", pass);
			
			if (!pass)
				stack.setState(ExecuteState.Done); 
		}
		
    	super.run(stack);
    	
		if (stack.getState() == ExecuteState.Done) 
    		stack.getParent().addVariable("_LastIf", stack.getStore().getField("Pass"));
    }

}