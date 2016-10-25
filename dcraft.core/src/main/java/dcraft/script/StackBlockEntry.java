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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import dcraft.lang.op.FuncResult;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.OperationObserver;
import dcraft.struct.CompositeStruct;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.util.StringUtil;
import dcraft.work.TaskRun;

public class StackBlockEntry extends StackEntry {
	protected Map<String, Struct> variables = new HashMap<String, Struct>();
	protected StackEntry currEntry = null;
	protected int currPosition = 0;
	protected boolean topFlag = true;

    public int getPosition() {
        return this.currPosition;
    }

    public void setPosition(int v) {
    	this.currPosition = v;
    }

    public boolean getTopFlag() {
        return this.topFlag;
    }

    public void setTopFlag(boolean v) {
    	this.topFlag = v;
    }
    
    public StackEntry getChild() {
        return this.currEntry; 
    }
    
    public void setChild(StackEntry v) { 
    	this.currEntry = v; 
    }
    
	public StackBlockEntry(Activity act, StackEntry parent, Instruction inst) {
		super(act, parent, inst);
	}

	public Collection<Struct> variables() {
        return this.variables.values(); 
    }

	@Override
    public void addVariable(String name, Struct var) {
    	this.variables.put(name, var);
    	
    	if (var instanceof AutoCloseable) {
    		TaskRun run = OperationContext.get().getTaskRun();
    		
    		if (run != null) {
	    		run.getContext().addObserver(new OperationObserver() {
					@Override
					public void completed(OperationContext ctx) {
						try {
							((AutoCloseable) var).close();
						} 
						catch (Exception x) {
							// TODO
						}
					}
				});
    		}
    	}
    }

    public void clearVariables() {		
		this.variables.clear();
    }

	@Override
    public Struct queryVariable(String name) {
		if (StringUtil.isEmpty(name))
			return null;
		
		// if variable not here, look in parent block
		
        int dotpos = name.indexOf(".");

        if (dotpos > -1) {
            String oname = name.substring(0, dotpos);

            Struct ov = this.variables.containsKey(oname) ? this.variables.get(oname) : null;

            // if not here, look up blocks until Function level and that catch globals too
            if (ov == null) 
            	ov = super.queryVariable(oname);

            if (ov == null) {
            	OperationContext.get().errorTr(510, oname);
            	return null;
            }
            
            if (!(ov instanceof CompositeStruct)){
            	OperationContext.get().errorTr(511, oname);
            	return null;
            }
            
            FuncResult<Struct> sres = ((CompositeStruct)ov).select(name.substring(dotpos + 1)); 
            
            return sres.getResult();
        }
        else if (this.variables.containsKey(name)) {
            return this.variables.get(name);
        }

        return super.queryVariable(name);
    }
    
	@Override
    public void debugStack(ListStruct dumpList) {
    	RecordStruct dumpRec = new RecordStruct();
    	dumpList.addItem(dumpRec);
    	
    	this.collectDebugRecord(dumpRec);
    	RecordStruct subRec = this.inst.collectDebugRecord(this, dumpRec);

    	if (subRec != null)
    		dumpList.addItem(subRec);
    	
        if (this.currEntry != null)
        	this.currEntry.debugStack(dumpList);
    }

	@Override
    public void collectDebugRecord(RecordStruct rec) {
    	RecordStruct dumpVariables = new RecordStruct();
    	rec.setField("Variables", dumpVariables);
        
        for (Entry<String, Struct> var : this.variables.entrySet()) 
            dumpVariables.setField(var.getKey(), var.getValue());
    }	
	
	@Override
	public StackBlockEntry queryBlockStack() {
		return this;
	}
	
	@Override
	public StackEntry getExecutingStack() {
		return (this.currEntry != null) ? this.currEntry : this;
	}
}
