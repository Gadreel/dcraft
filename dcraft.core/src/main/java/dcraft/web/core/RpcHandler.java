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
package dcraft.web.core;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import dcraft.bus.Message;
import dcraft.bus.MessageUtil;
import dcraft.lang.Memory;
import dcraft.lang.op.FuncResult;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.OperationResult;
import dcraft.log.DebugLevel;
import dcraft.log.HubLog;
import dcraft.log.Logger;
import dcraft.session.Session;
import dcraft.struct.CompositeParser;
import dcraft.struct.CompositeStruct;
import dcraft.struct.RecordStruct;
import dcraft.util.StringUtil;

public class RpcHandler implements IBodyCallback {
	protected AtomicReference<WebContext> context = new AtomicReference<>();
	protected CountDownLatch latch = new CountDownLatch(2);
	protected Memory mem = null;

	public RpcHandler(WebContext ctx) {
		this.context.set(ctx);
	}
	
	@Override
	public void fail() {
   		Logger.error("RPC Message failed");
    	
		// sort of cleanup references
   		WebContext ctx = this.context.getAndSet(null);
		
		if (ctx == null)
			return;
		
		ctx.sendRequestBad();
	}
	
	@Override
	public void ready(Memory mem) {
		this.mem = mem;
		
		this.latch.countDown();
		
		if (this.latch.getCount() == 0)
			this.process();
	}
	
	public boolean tryProcess() {
		this.latch.countDown();
		
		if (this.latch.getCount() == 0) {
			this.process();
			return true;
		}
		
		return false;
	}
	
	public void process() {
		try {
			this.latch.await(15, TimeUnit.SECONDS);
		} 
		catch (InterruptedException x) {
			return;
		}
		
    	if (Logger.isTrace())
    		Logger.trace("RPC Message collected");
    	
		// sort of cleanup references
   		WebContext ctx = this.context.getAndSet(null);
		
		if (ctx == null)
			return;
		
		FuncResult<CompositeStruct> pres = CompositeParser.parseJson(this.mem);
		
		if (pres.hasErrors()) {
			ctx.sendRequestBad();
			return;
		}
		
		CompositeStruct croot = pres.getResult();
		
		if ((croot == null) || !(croot instanceof RecordStruct)) {
			ctx.sendRequestBad();
			return;
		}
		
		// update the context, based off original
		ctx.getSession().useContext(ctx.getSession().allocateContextBuilder());
		
		RecordStruct mrec = (RecordStruct) croot;
		
		// check that the request conforms to the schema for RpcMessage
		OperationResult rootres = mrec.validate("RpcMessage");
		
		if (rootres.hasErrors()) {
			ctx.sendRequestBad();
			return;
		}
		
		// if so convert the Record into a Message for transport over our bus
		Message msg = MessageUtil.fromRecord(mrec);  
		
		//System.out.println("got rpc message: " + msg);
		
    	String sessionid = msg.getFieldAsString("Session");

    	msg.removeField("Session");
    	
    	String dlevel = msg.getFieldAsString("DebugLevel");
    	
    	// allow people to change debug level if debug is enabled
    	if (StringUtil.isNotEmpty(dlevel) && HubLog.getDebugEnabled()) {
        	msg.removeField("DebugLevel");
			OperationContext.get().setLevel(DebugLevel.parse(dlevel));
    	}
    	
    	if (Logger.isDebug())
    		Logger.debug("RPC Message: " + msg.getFieldAsString("Service") + " - " + msg.getFieldAsString("Feature")
    				+ " - " + msg.getFieldAsString("Op"));
    	
		// for SendForget don't wait for a callback, just return success
    	if ("SendForget".equals(msg.getFieldAsString("RespondTag"))) {
    		// send to bus
    		ctx.getSession().sendMessage(msg);
    		
    		Message rmsg = MessageUtil.success();
			
			Session sess = ctx.getSession();
			
			String currsessid = sess.getId();
			
			rmsg.setField("Session", currsessid);
			
			// TODO review - this will really be about tokens not sessions
			if ((sessionid != null) && !currsessid.equals(sessionid))
				rmsg.setField("SessionChanged", true);
			
			/*
			String authupdate = sess.checkTokenUpdate();
			
			if (authupdate != null) {
				Cookie sk = new DefaultCookie("dcAuthToken", authupdate);
				sk.setPath("/");
				sk.setHttpOnly(true);
				
				ctx.getResponse().setCookie(sk);
			}
			*/
    		
			// TODO pickup from mailbox
			
    		// reply to client, don't wait for response
			
			ctx.send(rmsg);
			
			return;
    	}
    	
    	ctx.getSession().sendMessageWait(msg, new RpcServiceResult(ctx, sessionid));
    	
    	// make sure we don't use this in inner classes (by making it non-final)
    	ctx = null;
	}
}
