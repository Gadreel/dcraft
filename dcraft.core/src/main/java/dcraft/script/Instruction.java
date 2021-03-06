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
package dcraft.script;

import dcraft.lang.op.OperationResult;
import dcraft.struct.RecordStruct;
import dcraft.xml.XElement;

abstract public class Instruction {
	protected XElement source = null;

    public XElement getXml() {
        return this.source; 
    }
    
    public void setXml(XElement v) { 
    	this.source = v; 
    }

    public void compile(ActivityManager manager, OperationResult log) {
    	
    }
    
    abstract public void run(final StackEntry stack);
	
    // override this if your instruction can cancel...
    abstract public void cancel(StackEntry stack);
    
	public RecordStruct collectDebugRecord(final StackEntry stack, RecordStruct rec) {		
		rec.setField("Line", this.source.getLine());
		rec.setField("Column", this.source.getCol());		
	   	rec.setField("Command", this.source.toLocalString());
	   	
	   	return null;
	}

	public StackEntry createStack(Activity act, StackEntry parent) {
		return new StackEntry(act, parent, this);
	}	
}
