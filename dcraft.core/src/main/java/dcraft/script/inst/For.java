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
import dcraft.lang.op.OperationContext;
import dcraft.script.BlockInstruction;
import dcraft.script.ExecuteState;
import dcraft.script.StackBlockEntry;
import dcraft.script.StackEntry;
import dcraft.struct.RecordStruct;
import dcraft.struct.scalar.IntegerStruct;

public class For extends BlockInstruction {
    @Override
    public void alignInstruction(final StackEntry stack, OperationCallback callback) {
    	StackBlockEntry bstack = (StackBlockEntry)stack;
    	
    	// signal end if conditional logic fails after loop
    	if (bstack.getPosition() >= this.instructions.size()) { 
    		RecordStruct store = stack.getStore();
    	    
    		IntegerStruct cntvar = (IntegerStruct) store.getField("Counter");
    	    long to = store.getFieldAsInteger("To");
    	    long step = store.getFieldAsInteger("Step");
    	    
        	cntvar.setValue(cntvar.getValue() + step);
        	
        	boolean flagdone = false;

        	if (step > 0) 
        		flagdone = (cntvar.getValue() > to);
        	else 
        		flagdone = (cntvar.getValue() < to);
        	
        	if (flagdone) 
	        	stack.setState(ExecuteState.Done);
        	else
        		bstack.setPosition(0);
    	}
    	
       	super.alignInstruction(stack, callback);
    }
    
    @Override
    public void run(final StackEntry stack) {
    	StackBlockEntry bstack = (StackBlockEntry)stack;
    	
		if (stack.getState() == ExecuteState.Ready) {
			long from = stack.intFromSource("From", 0);  
			long to = stack.intFromSource("To", 0);
			long step = stack.intFromSource("Step", 1);

			IntegerStruct cntvar = new IntegerStruct();

			cntvar.setType(OperationContext.get().getSchema().getType("Integer"));		// TODO souldn't need this
			//cntvar.setName(stack.stringFromSource("Name", "_forindex"));
			cntvar.setValue(from);
			
    		RecordStruct store = stack.getStore();
    	    
    		store.setField("Counter", cntvar);
    	    store.setField("To", to);
    	    store.setField("Step", step);

			bstack.addVariable(stack.stringFromSource("Name", "_forindex"), cntvar);
		}
		
		super.run(stack);
	}
}
