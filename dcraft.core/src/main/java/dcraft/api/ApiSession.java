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
package dcraft.api;

import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import dcraft.bus.Message;
import dcraft.bus.MessageUtil;
import dcraft.hub.Hub;
import dcraft.lang.TimeoutPlan;
import dcraft.lang.op.FuncCallback;
import dcraft.lang.op.OperationCallback;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.UserContext;
import dcraft.script.StackEntry;
import dcraft.struct.FieldStruct;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.xml.XElement;

// TODO make some of the properties accessible via RecordStruct fields for dcScript
abstract public class ApiSession extends RecordStruct implements AutoCloseable {
	static public ApiSession createLocalSession(String domain) {
		return Hub.instance.createLocalApiSession(domain);
	}
	
	static public ApiSession createSessionFromConfig(String name) {
		return Hub.instance.createApiSession(name);
	}
	
	protected String sessionid = null;
	//protected String sessionKey = null;
	protected String hubid = null;
	protected UserContext user = null;
	protected ReplyService replies = new ReplyService();
	public Message lastResult = null;
	
	public ReplyService getReplyService() {
		return this.replies;
	}
	
	public Message getLastResult() {
		return this.lastResult;
	}
	
	abstract public void init(XElement config);
	
	public void receiveMessage(Message msg) {
		// we need to restore/set the local operation context if anything is done here.
		// don't use the messages op context, it only applies within its own matrix
		// at least for starters let's set to guest
		
		OperationContext.useNewGuest();
		
		System.out.println("Got a message for ApiSession, not sure what to do with it!\n\n" + msg);
		
		// TODO else look at other services published through this session (raise message in event to API)
		// push message back out to CoreApi
	}

	public UserContext getUser() {
		return this.user;
	}

	public String getSessionId() {
		return this.sessionid;
	}

	public String getHubId() {
		return this.hubid;
	}
	
	/*
	public String getSessionKey() {
		return this.sessionKey;
	}
	*/
	
	abstract public void sendForgetMessage(Message msg);
	
	public Message sendMessage(Message msg) {
		return this.sendMessage(msg, TimeoutPlan.Regular);
	}
	
	public Message sendMessage(Message msg, TimeoutPlan timeoutPlan) {
		this.lastResult = null;
		
		final CountDownLatch latch = new CountDownLatch(1);
		
		this.sendMessage(msg, new ServiceResult(timeoutPlan) {
			@Override
			public void callback() {
				ApiSession.this.lastResult = this.getResult();
				
				latch.countDown();
			}
		});			
		
		try {
			latch.await();
		} 
		catch (InterruptedException x) {
			this.lastResult = MessageUtil.errorTr(445, x);
		}
		
		return this.lastResult;
	}
	
	abstract public void sendMessage(Message msg, ServiceResult callback);
	
	public void allocDataChannel(String title, final FuncCallback<String> callback) {
		Message msg = new Message("Session", "DataChannel", "Allocate", new RecordStruct(new FieldStruct("Title", title)));
		
		this.sendMessage(msg, new ServiceResult() {
			@Override
			public void callback() {
				if (!this.hasErrors())
					callback.setResult(this.getBodyAsRec().getFieldAsString("ChannelId"));
				
				callback.complete();				
			}
		});			
	}

	public void freeDataChannel(String channelid, final OperationCallback callback) {
		Message msg = new Message("Session", "DataChannel", "Free", new RecordStruct(new FieldStruct("ChannelId", channelid)));
		
		this.sendMessage(msg, new ServiceResult() {
			@Override
			public void callback() {
				callback.complete();				
			}
		});			
	}
	
	public void establishDataStream(String title, String mode, Message streamRequest, final FuncCallback<RecordStruct> callback) {
		Message msg = new Message("Session", "DataChannel", "Establish", new RecordStruct(
				new FieldStruct("Title", title),
				new FieldStruct("Mode", mode),
				new FieldStruct("StreamRequest", streamRequest)
		));
		
		this.sendMessage(msg, new ServiceResult() {
			@Override
			public void callback() {
				if (!this.hasErrors())
					callback.setResult(this.getBodyAsRec());
				
				callback.complete();				
			}
		});			
	}
	
	public void bindSourceChannel(String channelid, RecordStruct addressing, final OperationCallback callback) {
		// TODO validate addressing 
		
		RecordStruct body = new RecordStruct(
				new FieldStruct("ChannelId", channelid),
				new FieldStruct("Mode", "Source"),
				new FieldStruct("Hub", addressing.getFieldAsString("Hub")),
				new FieldStruct("Session", addressing.getFieldAsString("Session")),
				new FieldStruct("Channel", addressing.getFieldAsString("Channel"))
		);
		
		Message msg = new Message("Session", "DataChannel", "Bind", body);
		
		this.sendMessage(msg, new ServiceResult() {
			@Override
			public void callback() {
				callback.complete();				
			}
		});			
	}
	
	public void bindDestChannel(String channelid, RecordStruct addressing, final OperationCallback callback) {
		// TODO validate addressing 
		
		RecordStruct body = new RecordStruct(
				new FieldStruct("ChannelId", channelid),
				new FieldStruct("Mode", "Destination"),
				new FieldStruct("Hub", addressing.getFieldAsString("Hub")),
				new FieldStruct("Session", addressing.getFieldAsString("Session")),
				new FieldStruct("Channel", addressing.getFieldAsString("Channel"))
		);
		
		Message msg = new Message("Session", "DataChannel", "Bind", body);
		
		this.sendMessage(msg, new ServiceResult() {
			@Override
			public void callback() {
				callback.complete();				
			}
		});			
	}
	
	abstract public void sendStream(ScatteringByteChannel in, long size, long offset, String channelid, OperationCallback or);
	abstract public void receiveStream(WritableByteChannel out, long size, long offset, String channelid, OperationCallback callback);
	abstract public void abortStream(String channelid);

	public void thawContext(Message result) {
		if (result == null)
			return;	
		
		RecordStruct info = result.getFieldAsRecord("Body");
		
		if (info == null)
			return;
		
		this.user = UserContext.allocate(info);		
		
		//this.sessionKey = info.getFieldAsString("SessionKey");
		this.sessionid =  info.getFieldAsString("SessionId");
		
		this.hubid = this.sessionid.substring(0, this.sessionid.indexOf('_'));
	}
	
	public void clearToGuest() {
		this.user = UserContext.allocateGuest();
	}
	
	public boolean startSession() {
		return this.startSession(null);
	}
	
	public boolean startSession(String user, String pass) {
		return this.startSession(new RecordStruct(
				new FieldStruct("Username", user),
				new FieldStruct("Password", pass)
			));
	}
	
	/*
	public boolean startSession(String user, String pass, String code) {
		return this.startSession(new RecordStruct(
				new FieldStruct("Username", user),
				new FieldStruct("Password", pass)
			));
	}
	*/
	
	public boolean startSessionAsGuest() {
		return this.startSession(null);
	}
	
	public boolean startSession(RecordStruct creds) {
		// new creds means new user, start as guest
		if (creds != null)
			this.clearToGuest();
		
		Message msg = new Message();
		msg.setField("Service", "Session");
		msg.setField("Feature", "Control");
		msg.setField("Op", "Start");
		
		if (creds != null)
			msg.setField("Credentials", creds);
		
		Message rmsg = this.sendMessage(msg);
		
		if (rmsg == null)
			return false;
		
		this.thawContext(rmsg);
		
		// a real user or a guest will both be verified
		return !rmsg.hasErrors() && this.getUser().isVerified();
	}
	
	public void stop() {
		Message msg = new Message();
		msg.setField("Service", "Session");
		msg.setField("Feature", "Control");
		msg.setField("Op", "Stop");
		
		this.sendForgetMessage(msg);
		
		this.clearToGuest();
		this.stopped();
	}
	
	abstract public void stopped();
	
	public Collection<Message> checkInBox() {
		Message msg = new Message();
		msg.setField("Service", "Session");
		msg.setField("Feature", "Control");
		msg.setField("Op", "CheckInBox");
		
		Message rmsg = this.sendMessage(msg);
		
		if (rmsg.hasErrors())
			return null;
		
		ListStruct mlist = rmsg.getFieldAsList("Body"); 
		
		if (mlist == null)
			return null;
		
		ArrayList<Message> msgs = new ArrayList<Message>();
		
		for (Struct m : mlist.getItems()) {
			if (m instanceof RecordStruct) 
				msgs.add(MessageUtil.fromRecord((RecordStruct) m));
		}
		
		return msgs;
	}
	
	@Override
	public void operation(StackEntry stack, XElement code) {
		if ("Stop".equals(code.getName())) {
			this.stop();
			
			stack.resume();
			return;
		}
		
		super.operation(stack, code);
	}
	
	@Override
	public void close() {
		this.stop();
	}
}
