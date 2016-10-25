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
import dcraft.api.LocalSession;
import dcraft.api.HyperSession;
import dcraft.hub.TenantInfo;
import dcraft.hub.Hub;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.OperationObserver;
import dcraft.script.ExecuteState;
import dcraft.script.StackEntry;
import dcraft.script.inst.With;
import dcraft.session.Session;
import dcraft.util.StringUtil;

public class CtpSession extends With {
	@Override
	public void prepTarget(StackEntry stack) {
        String name = stack.stringFromSource("Name");            
        String host = stack.stringFromSource("Host");
        String user = stack.stringFromSource("User");
        String pwd = stack.stringFromSource("Password");
        
        if (StringUtil.isEmpty(name)) {
			stack.setState(ExecuteState.Done);
			OperationContext.get().errorTr(527);
			stack.resume();
			return;
        }
        
        if (StringUtil.isEmpty(host)) {
			stack.setState(ExecuteState.Done);
			OperationContext.get().errorTr(528);
			stack.resume();
			return;
        }
        
        TenantInfo di = Hub.instance.getTenants().resolveTenantInfo(host);
        
        ApiSession sess = null;
        
        // if we handle the domain then use local session
        if (di != null) {
        	Session session = Hub.instance.getSessions().create("hub:", host, "root", null);
        	sess = new LocalSession();
    		((LocalSession)sess).init(session, stack.getInstruction().getXml());
    		
    		// then use root user
        	if (StringUtil.isEmpty(user)) {
        		((LocalSession)sess).startSessionAsRoot();
        	}
        	else if (!sess.startSession(user, pwd)) {
        		sess.close();
        		
				stack.setState(ExecuteState.Done);
				OperationContext.get().errorTr(530);
				stack.resume();
				return;            		
        	}
        }
        else {
        	if (StringUtil.isEmpty(user)) {
				stack.setState(ExecuteState.Done);
				OperationContext.get().errorTr(529);
				stack.resume();
				return;
        	}
        	
        	// TODO enhance this some
    		sess = new HyperSession();
    		((HyperSession)sess).init(stack.getInstruction().getXml());
    		
            if (!sess.startSession(user, pwd)) {
            	sess.close();
            	
				stack.setState(ExecuteState.Done);
				OperationContext.get().errorTr(530);
				stack.resume();
				return;
            }
        }
        
        ApiSession fsess = sess;
        
        // this only works if we are in a task context, however this should be the case
        // so that is ok
		OperationContext.get().addObserver(new OperationObserver() {
			@Override
			public void completed(OperationContext ctx) {
				try {
					fsess.close();
				} 
				catch (Exception x) {
					// TODO
				}
			}
		});
		
        stack.addVariable(name, sess);
        this.setTarget(stack, sess);
		
		this.nextOpResume(stack);
	}

	/*
	@Override
	public void run(final StackEntry stack) {
		if (stack.getState() == ExecuteState.Ready) {
            String name = stack.stringFromSource("Name");            
            String host = stack.stringFromSource("Host");
            String user = stack.stringFromSource("User");
            String pwd = stack.stringFromSource("Password");
            
            if (StringUtil.isEmpty(name)) {
				stack.setState(ExecuteState.Exit);
				OperationContext.get().errorTr(527);
				stack.resume();
				return;
            }
            
            if (StringUtil.isEmpty(host)) {
				stack.setState(ExecuteState.Exit);
				OperationContext.get().errorTr(528);
				stack.resume();
				return;
            }
            
            DomainInfo di = Hub.instance.resolveDomainInfo(host);
            
            ApiSession sess = null;
            
            // if we handle the domain then use local session
            if (di != null) {
            	Session session = Hub.instance.getSessions().create("hub:", host);
            	sess = new LocalSession();
        		((LocalSession)sess).init(session, stack.getInstruction().getXml());
        		
        		// then use root user
            	if (StringUtil.isEmpty(user)) {
            		((LocalSession)sess).startSessionAsRoot();
            	}
            	else if (!sess.startSession(user, pwd)) {
    				stack.setState(ExecuteState.Exit);
    				OperationContext.get().errorTr(530);
    				stack.resume();
    				return;            		
            	}
            }
            else {
            	if (StringUtil.isEmpty(user)) {
    				stack.setState(ExecuteState.Exit);
    				OperationContext.get().errorTr(529);
    				stack.resume();
    				return;
            	}
            	
            	// TODO enhance this some
        		sess = new WebSession();
        		((WebSession)sess).init(stack.getInstruction().getXml());
        		
                if (!sess.startSession(user, pwd)) {
    				stack.setState(ExecuteState.Exit);
    				OperationContext.get().errorTr(530);
    				stack.resume();
    				return;
                }
            }
    		
            stack.addVariable(name, sess);

			stack.getStore().setField("CurrNode", 0);
			stack.getStore().setField("Target", sess);
			stack.setState(ExecuteState.Resume);
			
			stack.resume();
		}		
		else
			super.run(stack);
	}
	*/
}
