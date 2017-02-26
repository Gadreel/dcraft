package dcraft.web.core;

import dcraft.bus.Message;
import dcraft.bus.ServiceResult;
import dcraft.log.Logger;
import dcraft.session.Session;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;

public class RpcServiceResult extends ServiceResult {
	protected WebContext context = null;
	protected String sessid = null;
	
	public RpcServiceResult(WebContext context, String sessid) {
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