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

import dcraft.bus.Message;
import dcraft.bus.MessageUtil;
import dcraft.bus.ServiceResult;
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
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;

public class RpcHandler implements IBodyCallback {
	protected WebContext context = null;

	public RpcHandler(WebContext ctx) {
		this.context = ctx;
	}
	
	@Override
	public void fail() {
   		Logger.error("RPC Message failed");
    	
		// sort of cleanup references
   		WebContext ctx = this.context;
		this.context = null;
		
		if (ctx == null)
			return;
		
		ctx.sendRequestBad();
	}
	
	@Override
	public void ready(Memory mem) {
    	if (Logger.isTrace())
    		Logger.trace("RPC Message collected");
    	
		// sort of cleanup references
    	WebContext ctx = this.context;
		this.context = null;
		
		if (ctx == null)
			return;
		
		FuncResult<CompositeStruct> pres = CompositeParser.parseJson(mem);
		
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
			
			if ((sessionid != null) && !currsessid.equals(sessionid))
				rmsg.setField("SessionChanged", true);
			
			String authupdate = sess.checkTokenUpdate();
			
			if (authupdate != null) {
				Cookie sk = new DefaultCookie("dcAuthToken", authupdate);
				sk.setPath("/");
				sk.setHttpOnly(true);
				
				ctx.getResponse().setCookie(sk);
			}
    		
			// TODO pickup from mailbox
			
    		// reply to client, don't wait for response
			
			ctx.send(rmsg);
			
			return;
    	}
    	
    	ctx.getSession().sendMessageWait(msg, new RPCServiceResult(ctx, sessionid));
    	
    	// make sure we don't use this in inner classes (by making it non-final)
    	ctx = null;
	}
	
	static public class RPCServiceResult extends ServiceResult {
		protected WebContext context = null;
		protected String sessid = null;
		
		public RPCServiceResult(WebContext context, String sessid) {
			this.context = context;
			this.sessid = sessid;
		}
		
		@Override
		public void callback() {
			WebContext ctx = this.context;
			this.context = null;
			
			if (ctx == null)
				return;
			
			try {
				// if we did not get an official reply to the request then
				// it may have been a timeout.  regardless, collect messages
				// and prepare to return any payload
				Message rmsg = this.toLogMessage();
				
				// add the body (payload) if any
				Message reply = this.getResult();
				
				if (reply != null) {
					if (reply.hasField("Service"))
						rmsg.setField("Service", reply.getField("Service"));
					
					if (reply.hasField("Feature"))
						rmsg.setField("Feature", reply.getField("Feature"));
					
					if (reply.hasField("Op"))
						rmsg.setField("Op", reply.getField("Op"));
					
					if (reply.hasField("Tag"))
						rmsg.setField("Tag", reply.getField("Tag"));
					
					if (reply.hasField("FromHub"))
						rmsg.setField("FromHub", reply.getField("FromHub"));
					
					//if (reply.hasField("Body"))  - always even if null
					rmsg.setField("Body", reply.getField("Body"));
				}
				
				Session sess = ctx.getSession();

				// session may be null on Session - Control - Stop
				if (sess != null) {
					String currsessid = sess.getId();
					
					rmsg.setField("Session", currsessid);
					
					if ((this.sessid != null) && !currsessid.equals(this.sessid))
						rmsg.setField("SessionChanged", true);
					
					// web server does not send SessionSecret or AuthToken in response
					
					//System.out.println("outgoing rpc: " + rmsg);
					
					String authupdate = sess.checkTokenUpdate();
					
					if (authupdate != null) {
						Cookie sk = new DefaultCookie("dcAuthToken", authupdate);
						sk.setPath("/");
						sk.setHttpOnly(true);
						
						ctx.getResponse().setCookie(sk);
					}
				}
				
				// TODO switch so we can trace by tag - these values aren't helpful
		    	if (Logger.isDebug())
		    		Logger.debug("RPC Reply Message: " + rmsg.getFieldAsString("Service") + " - " + rmsg.getFieldAsString("Feature")
		    				+ " - " + rmsg.getFieldAsString("Op"));
				
				ctx.send(rmsg);
			}
			catch (Exception x) {
				Logger.info("Error replying to RPC request: " + x);
				ctx.sendInternalError();
			}
		}		
	}
}
