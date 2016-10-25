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
package dcraft.script.inst.ctp;

import dcraft.api.ApiSession;
import dcraft.api.ServiceResult;
import dcraft.bus.Message;
import dcraft.bus.MessageUtil;
import dcraft.lang.op.OperationContext;
import dcraft.script.ExecuteState;
import dcraft.script.Instruction;
import dcraft.script.StackEntry;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.struct.scalar.NullStruct;
import dcraft.util.StringUtil;

public class CtpCall extends Instruction {
	@SuppressWarnings("resource")
	@Override
	public void run(final StackEntry stack) {
        String name = stack.stringFromSource("Name");
        
        if (StringUtil.isEmpty(name))
        	name = "CtpCall_" + stack.getActivity().tempVarName();
        
        String vname = name;
        
        Struct ss = stack.refFromSource("Session");
        
        if ((ss == null) || !(ss instanceof ApiSession)) {
			stack.setState(ExecuteState.Done);
			OperationContext.get().errorTr(531);
        	stack.resume();
        	return;
        }
        
		ApiSession sess = (ApiSession) ss;
        
        Message msg = null;
        
        Struct ms = stack.refFromSource("Message");
        
        if ((ms != null) && !(ms instanceof NullStruct)) {
        	if (ms instanceof Message) {
        		msg = (Message) ms;
        	}
        	else if (ms instanceof RecordStruct) {
        		msg = MessageUtil.fromRecord((RecordStruct) ms);
        	} 
        	else {
    			stack.setState(ExecuteState.Done);
    			OperationContext.get().errorTr(532);
            	stack.resume();
            	return;
        	}
        }
        else {
            Struct bdy = stack.refFromSource("Data");
            String ser = stack.stringFromSource("Service");
            String feat = stack.stringFromSource("Feature");
            String op = stack.stringFromSource("Op");
        	
            msg = new Message(ser, feat, op, bdy);
        }
        
        if ((msg == null)) {
			stack.setState(ExecuteState.Done);
			OperationContext.get().errorTr(533);
        	stack.resume();
        	return;        	
        }
        
        if (stack.getInstruction().getXml().getName().equals("CtpCallForget")) {
        	sess.sendForgetMessage(msg);
			stack.setState(ExecuteState.Done);
			stack.resume();
        }
        else {
        	sess.sendMessage(msg, new ServiceResult() {				
				@Override
				public void callback() {
					Message resp = this.getResult();
					Struct rdata = NullStruct.instance;
					
					if ((resp != null) && resp.hasField("Body"))
						rdata = resp.getField("Body");
		    		
		            stack.addVariable(vname, rdata);
					
					stack.setState(ExecuteState.Done);
					stack.resume();
				}
			});
        }
	}
	
	@Override
	public void cancel(StackEntry stack) {
		// do nothing, this isn't cancellable
	}
}