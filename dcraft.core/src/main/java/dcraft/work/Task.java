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
package dcraft.work;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.joda.time.DateTime;

import dcraft.bus.Message;
import dcraft.hub.Hub;
import dcraft.lang.op.IOperationObserver;
import dcraft.lang.op.OperationCallback;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.OperationLogger;
import dcraft.lang.op.OperationObserver;
import dcraft.lang.op.OperationResult;
import dcraft.log.DebugLevel;
import dcraft.log.Logger;
import dcraft.struct.FieldStruct;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.util.StringUtil;

// conforms to dcTaskInfo data type
public class Task {
	static public String nextTaskId() {
		  return Task.nextTaskId("DEFAULT");
	}	
	
	static public String nextTaskId(String part) {
		  return OperationContext.getHubId() + "_" + part + "_" + UUID.randomUUID().toString().replace("-", "");
	}	
	
	// create a subtask of a running task
	public static Task subtask(TaskRun run, String suffix, OperationCallback cb) {
		Task t = new Task();
		
		Task parent = run.getTask();
		
		t.context = parent.context.subContext();
		
		// sub tasks can be found through "child" context
		//t.withId(parent.getId() + "_" + Session.nextUUId());
		t.withId(Task.nextTaskId());
		t.withTitle(parent.getTitle() + " - Subtask: " + suffix);

		t.withTimeout(parent.getTimeout());
		t.withThrottle(parent.getThrottle());
		
		// subtasks should almost always use default 
		
		if (cb != null) {
			t.withObserver(new OperationObserver() {
				@Override
				public void completed(OperationContext ctx) {
					cb.complete();
				}
			});
		}
		
		return t;
	}
	
	static public Task taskWithContext(OperationContext ctx) {
		return new Task().withContext(ctx);
	}
	
	static public Task taskWithSubContext(OperationContext ctx) {
		return new Task().withContext(ctx.subContext());
	}
	
	static public Task taskWithSubContext() {
		return new Task().withContext(OperationContext.get().subContext());
	}
	
	static public Task taskWithRootContext() {
		return new Task().withContext(OperationContext.allocateRoot());
	}
	
	static public Task taskWithSiteRootContext() {
		return new Task().withContext(OperationContext.allocateSiteRoot());
	}
	
	static public Task taskWithGuestContext() {
		return new Task().withContext(OperationContext.allocateGuest());
	}
	
	static public Task taskFromRecord(RecordStruct info) {
		Task task = new Task();
		
		task.info = info;
		
		if (!info.isFieldEmpty("Context"))
			task.context = OperationContext.allocate(info.getFieldAsRecord("Context"));
		else
			task.context = OperationContext.get().subContext();
		
		return task;
	}
	
	// MEMBERS
	
	// used during run
	protected IWork work = null;
	protected List<IOperationObserver> observers = null;
	
	// used with run or queueable
	protected OperationContext context = null;	
	protected RecordStruct info = new RecordStruct();

	private Task() {
	}

	public Task copy() {
		RecordStruct clone = (RecordStruct) this.info.deepCopyExclude("Context");
		
		Task t = new Task();
		
		t.info = clone;
		t.context = this.context.subContext();		// context has observers we don't want copied
		
		return t;
	}
	
	public RecordStruct freezeToRecord() {
		RecordStruct clone = (RecordStruct) this.info.deepCopy();
		
		clone.setField("Context", this.context.freezeToRecord());
		
		return clone;
	}
	
	public Task withWork(IWork work) {
		this.work = work;
		
		if (this.work != null)
			this.info.setField("WorkClassname", this.work.getClass().getCanonicalName());
		
		return this;
	}
	
	public Task withWork(Runnable work) {
		return this.withWork(new WorkAdapter(work));
	}
	
	public Task withWork(Class<? extends IWork> classref) {
		this.info.setField("WorkClassname", classref.getCanonicalName());
		return this;
	}
	
	// class name
	public Task withWork(String classname) {
		this.info.setField("WorkClassname", classname);
		return this;
	}
	
	public IWork getWork() {
		if (this.work == null) 
			this.work = (IWork) Hub.instance.getInstance(this.getWorkClassname());
		
		return this.work;
	}
	
	// won't create if not present
	public IWork getWorkInstance() {
		return this.work;
	}
	
	public String getWorkClassname() {
		return this.info.getFieldAsString("WorkClassname");
	}
	
	public String getDebugWorkname() {
		if (this.work != null)
			return this.work.getClass().getName();
		
		return this.info.getFieldAsString("WorkClassname");
	}
	
	public Task withTopic(String v) {
		this.info.setField("Topic", v);
		return this;
	}

	public String getTopic() {
		String name = this.info.getFieldAsString("Topic");
		
		if (StringUtil.isEmpty(name))
			name = "Default";
		
		return name;
	}
	
	public Task withContext(OperationContext v) {
		this.context = v;
		return this;
	}

	public OperationContext getContext() {
		return this.context;
	}
	
	public Task withObserver(IOperationObserver watcher) {
		if (this.observers == null)
			this.observers = new ArrayList<>();
			
		this.observers.add(watcher);
		
		String cname = watcher.getClass().getCanonicalName();
		
		// if anonymous class or such then don't bother storing the class name
		if (StringUtil.isEmpty(cname))
			return this;
		
		if (watcher instanceof RecordStruct) {
			RecordStruct w = (RecordStruct)watcher;
			w.setField("_Classname", cname);
			
			return this.withObserverRec(w);
		}
		
		return this.withObserver(cname);
	}
	
	public Task withObserver(Class<? extends IOperationObserver> classref) {
		return this.withObserver(classref.getCanonicalName());
	}
	
	public Task withObserver(String classname) {
		if (StringUtil.isEmpty(classname))
			return this;
		
		return this.withObserverRec(new RecordStruct(
				new FieldStruct("_Classname", classname)
		));
	}
	
	public Task withObserverRec(RecordStruct observer) {
		ListStruct buildobservers = this.info.getFieldAsList("Observers"); 
		
		if (buildobservers == null) {
			buildobservers = new ListStruct();
			this.info.setField("Observers", buildobservers);
		}
		
		buildobservers.addItem(observer);
		
		return this;
	}
	
	public Task withDefaultLogger() {
		return this.withObserver(OperationLogger.class);
	}
	
	public List<IOperationObserver> getObservers() {
		return this.observers;
	}
	
	public Task withUsesTempFolder(boolean v) {
		this.info.setField("UsesTempFolder", v);
		return this;
	}
	
	public boolean isUsesTempFolder() {
		return this.info.getFieldAsBooleanOrFalse("UsesTempFolder");
	}
	
	public Task withId(String v) {
		this.info.setField("Id", v);
		return this;
	}
	
	public String getId() {
		return this.info.getFieldAsString("Id");
	}
	
	public Task withTitle(String v) {
		this.info.setField("Title", v);
		return this;
	}
	
	public String getTitle() {
		return this.info.getFieldAsString("Title");
	}
	
	public Task withStatus(String v) {
		this.info.setField("Status", v);
		return this;
	}
	
	public String getStatus() {
		return this.info.getFieldAsString("Status");
	}
	
	public Task withSquad(String v) {
		this.info.setField("Squad", v);
		return this;
	}
	
	public String getSquad() {
		return this.info.getFieldAsString("Squad");
	}
	
	public Task withHub(String v) {
		this.info.setField("HubId", v);
		return this;
	}
	
	public String getHub() {
		return this.info.getFieldAsString("HubId");
	}
	
	public Task withWorkId(String v) {
		this.info.setField("WorkId", v);
		return this;
	}
	
	public String getWorkId() {
		return this.info.getFieldAsString("WorkId");
	}
	
	public Task withAuditId(String v) {
		this.info.setField("AuditId", v);
		return this;
	}
	
	public String getAuditId() {
		return this.info.getFieldAsString("AuditId");
	}
	
	public boolean hasAuditId() {
		return !this.info.isFieldEmpty("AuditId");
	}
	
	public Task withCurrentTry(int v) {
		this.info.setField("CurrentTry", v);
		return this;
	}
	
	public int getCurrentTry() {
		return (int)this.info.getFieldAsInteger("CurrentTry", 0);
	}
	
	public void incCurrentTry() {
		int v = (int)this.info.getFieldAsInteger("CurrentTry", 0) + 1;
		this.info.setField("CurrentTry", v);
	}	
	
	public Task withMaxTries(int v) {
		this.info.setField("MaxTries", v);
		return this;
	}
	
	public int getMaxTries() {
		return (int)this.info.getFieldAsInteger("MaxTries", 1);
	}
	
	public Task withThrottle(int v) {
		this.info.setField("Throttle", v);
		return this;
	}
	
	public Task withThrottleIfEmpty(int v) {
		if (this.info.isFieldEmpty("Throttle"))
			this.info.setField("Throttle", v);
		
		return this;
	}
	
	// default to 2 resumes
	public int getThrottle() {
		return (int)this.info.getFieldAsInteger("Throttle", 2);
	}
	
	public Task withClaimedStamp(String v) {
		this.info.setField("ClaimedStamp", v);
		return this;
	}
	
	public String getClaimedStamp() {
		return this.info.getFieldAsString("ClaimedStamp");
	}
	
	public Task withAddStamp(DateTime v) {
		this.info.setField("AddStamp", v);
		return this;
	}
	
	public DateTime getAddStamp() {
		return this.info.getFieldAsDateTime("AddStamp");
	}
	
	// do not retry this task on the queue
	public boolean getFinalTry() {
		return (this.info.getFieldAsBooleanOrFalse("FinalTry") || (this.getCurrentTry() >= this.getMaxTries()));
	}
	
	// finish the current run, but go no further, don't try again even if Tries left  
	public Task withFinalTry(boolean v) {
		this.info.setField("FinalTry", v);
		return this;
	}
	
	
	public Task withSetTags(String... v) {
		this.info.setField("Tags", new ListStruct((Object[])v));
		return this;
	}
	
	public Task withSetTags(ListStruct v) {
		this.info.setField("Tags", v);
		return this;
	}
	
	public Task withAddTags(String... v) {
		if (this.info.isFieldEmpty("Tags"))
			this.info.setField("Tags", new ListStruct((Object[])v));
		else
			this.info.getFieldAsList("Tags").addItem((Object[])v);
		
		return this;
	}
	
	public Task withAddTags(ListStruct v) {
		if (this.info.isFieldEmpty("Tags"))
			this.info.setField("Tags", v);
		else
			this.info.getFieldAsList("Tags").addItem(v);
		
		return this;
	}
	
	public ListStruct getTags() {
		return this.info.getFieldAsList("Tags");
	}
	
	/**
	 * @param tags to search for with this task
	 * @return true if this task has one of the requested tags  
	 */
	public boolean isTagged(String... tags) {
		if (this.info.isFieldEmpty("Tags"))
			return false;
		
		for (Struct shas : this.info.getFieldAsList("Tags").getItems()) {
			String has = shas.toString();
			
			for (String wants : tags) {
				if (has.equals(wants))
					return true;
			}
		}
		
		return false;
	}
	
	public Task withParams(RecordStruct v) {
		this.info.setField("Params", v);
		return this;
	}
	
	public RecordStruct getParams() {
		return this.info.getFieldAsRecord("Params");
	}
	
	public Message getParamsAsMessage() {
		return (Message) this.info.getFieldAsRecord("Params");
	}
	
	public Task withExtra(RecordStruct v) {
		this.info.setField("Extra", v);
		return this;
	}
	
	public RecordStruct getExtra() {
		return this.info.getFieldAsRecord("Extra");
	}
	
	/*
	 * Timeout is when nothing happens for v minutes...see Overdue also
	 * 
	 * @param v
	 * @return
	 */
	public Task withTimeout(int v) {
		this.info.setField("Timeout", v);
		
		//if (v > this.getDeadline())
		//	this.withDeadline(v + 1);
		
		return this;
	}
	
	// in minutes
	public int getTimeout() {
		return (int) this.info.getFieldAsInteger("Timeout", 1);  
	}
	
	public int getTimeoutMS() {
		return (int) this.info.getFieldAsInteger("Timeout", 1)  * 60 * 1000; // convert to ms	
	}
	
	/*
	 * Deadline is v minutes until the task must complete, see Timeout also
	 * 
	 * @param v
	 * @return
	 */
	public Task withDeadline(int v) {
		this.info.setField("Deadline", v);
		return this;
	}
	
	// stalled even if still active, not getting anything done
	// in minutes
	public int getDeadline() {
		return (int) this.info.getFieldAsInteger("Deadline", 0); 
	}
	
	public int getDeadlineMS() {
		return (int) this.info.getFieldAsInteger("Deadline", 0)  * 60 * 1000; // convert to ms	
	}
	
	public OperationResult validate() {
		return this.info.validate("dcTaskInfo");
	}

	// happens after submit to pool or to queue
	public void prep() {
		if (this.info.isFieldEmpty("Title"))
			this.info.setField("Title", "[unnamed]");
		
		if (this.info.isFieldEmpty("Id"))
			this.info.setField("Id", Task.nextTaskId());
		
		if (this.observers == null)
			this.observers = new ArrayList<>();
		
		ListStruct buildobservers = this.info.getFieldAsList("Observers"); 
		
		if (buildobservers != null) {
			for (Struct s : buildobservers.getItems()) {
				RecordStruct orec = (RecordStruct) s;
				
				if (orec.isFieldEmpty("_Classname")) {
					Logger.warn("Missing observer classname (" + this.getId() + "): " + orec);
					continue;
				}
				
				IOperationObserver observer = (IOperationObserver) Hub.instance.getInstance(orec.getFieldAsString("_Classname").toString());
				
				if (observer instanceof RecordStruct)
					((RecordStruct)observer).copyFields(orec);
				
				this.observers.add(observer);
			}
		}		
	}

	/* doesn't work with lambda's
	// this builder is going to be used with another task (repeat task) so cleanup
	public void reset() {
		// if we have the class name then start with a fresh instance each run
		if (!this.info.isFieldEmpty("WorkClassname"))
			this.work = null;
		
		// if we have the class name then start with a fresh instance each run
		if (!this.info.isFieldEmpty("ObserverClassname"))
			this.observers = null;
		
		this.info.removeField("FinalTry");
	}
	*/

	public boolean isFromWorkQueue() {
		return (StringUtil.isNotEmpty(this.getWorkId()));
	}

	@Override
	public String toString() {
		return this.getTitle() + " (" + this.getId() + ")";
	}

	public RecordStruct status() {
		return new RecordStruct( 
				this.info.getFieldStruct("WorkId"),
				new FieldStruct("TaskId", this.info.getField("Id")),
				this.info.getFieldStruct("Title"),
				new FieldStruct("MaxTry", this.getMaxTries()),
				new FieldStruct("Added", this.getAddStamp()),
				new FieldStruct("Try", this.getCurrentTry())
		);
	}

	public Task withLogging(DebugLevel v) {
		this.context.setLevel(v);
		return this;
	}
}
