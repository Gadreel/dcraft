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
package dcraft.session;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import dcraft.bus.MessageUtil;
import dcraft.bus.net.StreamMessage;
import dcraft.filestore.CommonPath;
import dcraft.hub.Hub;
import dcraft.lang.op.FuncCallback;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.OperationResult;
import dcraft.log.Logger;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;

/**
 *  TODO track if we are a source or dest stream - if a send of "Block" is tried on a dest then error
 *  if a receive of "Block" on source happens then error
 */
public class DataStreamChannel extends OperationResult {
	protected String id = Session.nextUUId();
	protected String title = null;
	protected CommonPath path = null;
	protected String mime = null;
	protected Struct params = null;
	
	protected IStreamDriver driver = null;
	
	protected String sessid = null;
	
	protected long timeout = 60 * 1000;   // timeout in 1 full minute of no activity on the channel
	protected long deadline = 0;
	protected boolean closed = false;
	
	protected long started = System.currentTimeMillis();
	
	protected final Lock completionlock = new ReentrantLock();
	protected boolean completed = false;
	protected FuncCallback<RecordStruct> oncomplete = null;
	
	// contains the FromHub, Session, Channel info to reply to
	protected RecordStruct binding = null;
	protected RecordStruct result = null;
	
	public String getId() {
		return this.id;
	}
	
	public String getTitle() {
		return this.title;
	}
	
	public void setTitle(String v) {
		this.title = v;
	}
	
	public String getMime() {
		return this.mime;
	}
	
	public void setMime(String v) {
		this.mime = v;
	}
	
	public CommonPath getPath() {
		return this.path;
	}
	
	public void setPath(CommonPath v) {
		this.path = v;
	}
	
	public void setPath(String v) {
		this.path = new CommonPath(v);
	}
	
	public IStreamDriver getDriver() {
		return this.driver;
	}
	
	public void setDriver(IStreamDriver v) {
		this.driver = v;
	}
	
	public Struct getParams() {
		return this.params;
	}
	
	public void setParams(Struct v) {
		this.params = v;
	}
	
	// in seconds
	public int getTimeout() {
		return (int) (this.timeout / 1000);
	}
	
	// set in seconds
	public void setTimeout(int v) {
		this.timeout = v * 1000;
	}
	
	// in seconds
	public int getDeadline() {
		return (int) (this.deadline / 1000);
	}
	
	// set in seconds
	public void setDeadline(int v) {
		this.deadline = v * 1000;
	}

	public RecordStruct getBinding() {
		return this.binding;
	}
	
	public void setBinding(RecordStruct v) {
		this.binding = v;
		
		if (!v.isFieldEmpty("FilePath"))
			this.setPath(v.getFieldAsString("FilePath"));
		
		if (!v.isFieldEmpty("Mime"))
			this.setMime(v.getFieldAsString("Mime"));
	}
	
	public void setResult(RecordStruct v) {
		this.result = v;
	}

	public String getSessionId() {
		return this.sessid;
	}
	
	public DataStreamChannel(String sessid, String title) {
		super();
		
		this.title = title;
		this.sessid = sessid;
	}
	
	public DataStreamChannel(String sessid, String title, RecordStruct binding) {
		super();
		
		this.title = title;
		this.sessid = sessid;
		this.binding = binding;
	}
	
	public void resume() {
		OperationContext.set(this.opcontext);
	}
	
	public void setCompleteCallback(FuncCallback<RecordStruct> v) {
		this.oncomplete = v;
	}

	/**
	 * abort is only for our end of the channel.  if the other end sends an error message
	 * we should just close, not abort.  don't send messages to a channel that has errored
	 * 
	 */
	public void abort() {
		if (Logger.isDebug())
			Logger.debug("Data stream aborted: " + this.id);
		
		this.send(MessageUtil.streamError(1, "Aborting data stream: " + this));
		
		this.close();
	}
	
	public void close() {
		if (Logger.isDebug())
			Logger.debug("Data stream closed: " + this.id);
		
		this.completionlock.lock();
		
		try {			
			if (this.completed)
				return;
			
			OperationContext.set(this.opcontext);
			
			// collect inactive before error logging, logging updates the activity
			boolean inactive = this.isInactive();
			
			this.errorTr(196, this);
			
			if (this.isOverdue())
				this.errorTr(222, this);
			else if (inactive)	
				this.errorTr(223, this);
			
			this.closed = true;
				
			if (this.driver != null)
				this.driver.cancel();

			//	this.send(MessageUtil.streamError(1, "Connection killed: " + this));
			
			this.complete();
		}
		finally {
			this.completionlock.unlock();
		}
	}
	
	public boolean isClosed() {
		return this.closed;
	}

	// must report if timed out, even if completed - otherwise Worker thread might lock forever if WorkTopic kills us first
	public boolean isHung() {
		return this.isInactive() || this.isOverdue();
	}
	
	public boolean isInactive() {	
		// has activity been quiet for longer than timeout?  
		if ((this.timeout > 0) && (this.getLastActivity() < (System.currentTimeMillis() - this.timeout))) {
			if (Logger.isDebug())
				Logger.debug("Data stream reports being inactive: " + this.id);
			
			return true;
		}
		
		return false;
	}
	
	public boolean isOverdue() {	
		// has activity been working too long?
		if ((this.deadline > 0) && (this.started < (System.currentTimeMillis() - this.deadline))) {
			if (Logger.isDebug())
				Logger.debug("Data stream reports being overdue: " + this.id);
			
			return true;
		}
		
		return false;
	}
	
	public void complete() {
		if (Logger.isDebug())
			Logger.debug("Data stream has completed: " + this.id);
		
		// make sure we complete in the correct context (only worker should call this method)
		OperationContext.set(this.opcontext);
		
		this.completionlock.lock();
		
		try {
			// don't complete twice
			if (this.completed)
				return;
			
			this.completed = true;
			
			this.traceTr(224, this.getCode());
			
			Logger.info("Channel completed: " + this);
			
			// TODO make sure channel gets cleared
			Hub.instance.getSessions().lookup(this.sessid).removeChannel(this.id);
			
			if (this.oncomplete != null) {
				this.oncomplete.setResult(this.result);
				this.oncomplete.complete();
				this.oncomplete = null;
			}
		}
		finally {
			this.completionlock.unlock();
		}
	}

	@Override
	public String toString() {
		return this.getTitle() + " (" + this.getId() + ")";
	}
	
	public boolean isComplete() {
		return this.completed;
	}
	
	public RecordStruct toStatusReport() {
		RecordStruct rec = new RecordStruct();
		
		rec.setField("Id", this.id);
		rec.setField("Title", this.title);
		
		rec.setField("Canceled", this.closed);
		rec.setField("Completed", this.completed);
		
		// TODO anything else?
		
		return rec;
	}
	
	public void deliverMessage(StreamMessage msg) {
		this.touch();
		
		OperationContext.set(this.opcontext);
		
		if (Logger.isDebug())
			Logger.debug("Data Stream Message arrived: " + msg.toPrettyString());
		
		if (msg.hasErrors()) {
			this.close();
        	msg.release();
			return;
		}
		
    	OperationResult vres = msg.validate("StreamMessage");
    	
    	if (vres.hasErrors()) {
			this.abort();
        	msg.release();
        	return;
    	}	    		
    	
    	if (this.isClosed()) {
    		this.send(MessageUtil.streamError(1, "Channel is closed!"));
        	msg.release();
    		return;
    	}
    	
		if (this.driver != null)
			this.driver.message(msg);
	}
	
	public OperationResult send(StreamMessage msg) {		
		OperationContext.set(this.opcontext);
		
		if (Logger.isDebug())
			Logger.debug("Data Stream Message sending: " + msg.toPrettyString());
		
		msg.setField("FromHub", OperationContext.getHubId());
		msg.setField("FromSession", this.sessid);
		msg.setField("FromChannel", this.id);		
		
		if (this.binding == null)
			return new OperationResult();
		
		return Hub.instance.getBus().sendReply(msg, this.binding);
	}
}