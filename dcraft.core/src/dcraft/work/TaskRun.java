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

import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.joda.time.DateTime;

import dcraft.hub.Hub;
import dcraft.lang.op.FuncResult;
import dcraft.lang.op.IOperationLogger;
import dcraft.lang.op.IOperationObserver;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.OperationEvents;
import dcraft.lang.op.OperationResult;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.util.FileUtil;
import dcraft.util.StringUtil;

/**
 * Do not run same task object in parallel 
 * 
 */
public class TaskRun extends FuncResult<Struct> implements Runnable {
	protected Task task = null;
	
	protected long started = -1;  
	protected long lastclaimed = -1; 
	protected int slot = 0;
	
	protected boolean completed = false;
	
	protected final Lock completionlock = new ReentrantLock();		
	
	public boolean hasStarted() {
		return (this.started > -1);
	}
	
	// don't alter this after submitting to work pool, this is for view only as submit
	public Task getTask() {
		return this.task;
	}
	
	public TaskRun() {
		super();
		
		this.task = Task.taskWithSubContext(); 
		
		this.msgStart = 0;
	}
	
	public TaskRun(Task info) {
		super();
		
		this.task = info;
		this.msgStart = 0;
	}

	// prep is running in external context, log to that context but 
	public void prep() {
		// if we are resuming, leave the rest alone
		if (this.started != -1)
			return;
		
		this.task.prep();
		
		OperationContext ctx = this.task.getContext();
		
		this.opcontext = ctx;
		
		this.markStart();

		// add any new observers
		for (IOperationObserver ob : this.task.getObservers())
			ctx.addObserver(ob);
		
		ctx.fireEvent(OperationEvents.PREP_TASK, null);
	}
	
	public boolean isComplete() {
		return this.completed;
	}

	// must report if timed out, even if completed - otherwise Worker thread might lock forever if WorkTopic kills us first
	public boolean isHung() {
		return this.isInactive() || this.isOverdue();
	}
	
	public boolean isInactive() {	
		long timeout = this.task.getTimeoutMS();
        
        //System.out.println("Get last activity in active test: " + this.getLastActivity());
		
		// has activity been quiet for longer than timeout?  
		if ((timeout > 0) && (this.getLastActivity() < (System.currentTimeMillis() - timeout))) 
				return true;
		
		return false;
	}
	
	// only become overdue after it has started
	public boolean isOverdue() {	
		long deadline = this.task.getDeadlineMS();
		
		// has activity been working too long?
		if ((this.started != -1) && (deadline > 0) && (this.started < (System.currentTimeMillis() - deadline)))
				return true;
		
		return false;
	}
	
	// if task has been doing work but not fast enough we may need to renew/review claim
	// will not work if you use less than 2 minutes for timeout
	public void reviewClaim() {
		// if not started, if completed or if hung then nothing to review
		if ((this.started == -1) || this.completed || this.isHung())
			return;

		// once every 5 seconds we can renew a claim  (might cause problems if run log is huge and we are tracking work back to the database)
		if (this.lastclaimed >= (System.currentTimeMillis() - 5000))
			return;
		
		// otherwise there has been activity recently enough to warrant and update
		this.updateClaim();
	}
	
	// return true if claimed or completed - false if canceled or timed out
	public boolean updateClaim() {
		if (this.task.isFromWorkQueue()) { 
			// an incomplete load from work queue - edge error condition
			if (!this.task.hasAuditId()) {
				this.errorTr(191, this.task.getId());
				return false;
			}
			
			// get the logs up to date as much as possible
			OperationResult res1 = Hub.instance.getWorkQueue().trackWork(this, false);		// TODO add param for update claim?  review this
			
			if (res1.hasErrors()) {
				this.errorTr(191, this.task.getId());
				return false;
			}
			
			// try to extend our claim
			OperationResult res2 = Hub.instance.getWorkQueue().updateClaim(this.task);
			
			if (res2.hasErrors()) {
				this.errorTr(191, this.task.getId());
				return false;
			}
		}
		
		this.lastclaimed = System.currentTimeMillis();		
		
		return true;
	}
	
	public void run() {
		try {
			OperationContext.set(this.opcontext);
			
			this.opcontext.setTaskRun(this);
			
			if (this.started == -1) {
				
				if (this.task.isFromWorkQueue())
					this.infoTr(153, this.task.getId());
				else
					this.traceTr(153, this.task.getId());
					
				this.traceTr(144, Hub.instance.getWorkPool().getTopicOrDefault(this));
				
				// if this is a queue task then mark it started
				if (this.task.isFromWorkQueue()) {
					FuncResult<String> k = Hub.instance.getWorkQueue().startWork(this.task.getWorkId());
					
					if (k.hasErrors()) {
						// TODO replace with hub events
						Hub.instance.getWorkQueue().sendAlert(179, this.task.getId(), k.getMessage());
						
						this.errorTr(179, this.task.getId(), k.getMessage());
						this.complete();
						return;
					}
					
					this.task.incCurrentTry();
					this.task.withAuditId(k.getResult());
				}
				
				RecordStruct params = this.task.getParams();
				
				if (params == null) {
					params = new RecordStruct();
					this.task.withParams(params);
				}
				
				// use temp folder unless skip flag 
				if (this.task.isUsesTempFolder()) {
					try {
						File tempFolder = FileUtil.allocateTempFolder();
						
						// needs to be canonical for log filtering
						params.setField("_TempFolder", tempFolder.getCanonicalPath());
					}
					catch (Exception x) {
						this.errorTr(215, this, x);
						this.complete();
						return;
					}
				}
				
				// the official "logger" is available via the _Logger special var
				IOperationLogger logger = this.opcontext.getLogger();
				
				if (logger != null)
					params.setField("_Logger", logger);
				
				this.started = this.lastclaimed = System.currentTimeMillis();
				
				// task start before work
				this.opcontext.fireEvent(OperationEvents.START_TASK, null);
			}
			
			//  TODO review info feature DCTASKLOG in NCC
			// task might need some way to refer to info structures
			//params.setField("_Info", this.info.info);
			
			IWork work = this.task.getWork();
			
			if (work == null) {
				this.errorTr(217, this);
				this.complete();
				return;
			}
			
			work.run(this);
			
			if (work instanceof ISynchronousWork)
				this.complete();
		}
		catch (Exception x) {
			this.errorTr(155, this.task.getId(), x);
			
			IWork work = this.task.getWorkInstance();
			
			if (work != null)			
				System.out.println("Work pool caught exception: " + work.getClass());
			
			System.out.println("Stack Trace: ");
			x.printStackTrace();
			
			this.complete();
		}
		finally {
			//OperationContext.clear();
			OperationContext.useHubContext();
		}
	}		
	
	public void resume() {
		if (this.opcontext != null)
			this.opcontext.touch();
		
		Hub.instance.getWorkPool().submit(this);		
	}
	
	public void kill(String msg) {
		this.error(msg);
		this.kill();
	}
	
	/**
	 * @param code code for message
	 * @param msg message
	 */
	public void kill(long code, String msg) {
		this.error(code, msg);
		this.kill();
	}
	
	public void killTr(long code, Object... params) {
		this.errorTr(code, params);
		this.kill();
	}
	
	public void kill() {
		OperationContext.set(this.opcontext);
		
		this.completionlock.lock();
		
		try {			
			if (this.completed)
				return;
			
			// collect inactive before error logging, logging updates the activity
			boolean inactive = this.isInactive();
			
			this.errorTr(196, this.task);
			
			if (this.isOverdue())
				this.errorTr(222, this.task);
			else if (inactive)	
				this.errorTr(223, this.task);
			
			this.complete();
		}
		finally {
			this.completionlock.unlock();
			
			// always mark task completed so it isn't stuck in work pool, thread safe
			Hub.instance.getWorkPool().complete(this);
		}
	}
	
	public void complete() {
		// make sure we complete in the correct context (only worker should call this method)
		OperationContext.set(this.opcontext);
		
		this.completionlock.lock();
		
		try {			
			// don't complete twice (but in try so we unlock)
			if (this.completed)
				return;
			
			this.completed = true;
			
			// task observers could log still - so before close log
			this.opcontext.fireEvent(OperationEvents.COMPLETED, null);
			
			// task observers stop can/should no longer log
			this.opcontext.fireEvent(OperationEvents.STOP_TASK, null);
			
			// if this is a queue task then end it - only if we got an audit it though
			// TODO refine this - if we have a task id but not an audit id we should cleanup the queue...
			// TODO what should we do if not started - (this.started == -1)
			if (this.task.isFromWorkQueue() && this.task.hasAuditId()) {				
				// don't go forward if this no longer holds a claim
				if (!this.updateClaim()) 
					// record only that we ended but not a status or a queue change
					Hub.instance.getWorkQueue().trackWork(this, true);
				else if (this.hasErrors()) 
					// record failure if errors
					Hub.instance.getWorkQueue().failWork(this);
				else 
					// otherwise record completed
					Hub.instance.getWorkQueue().completeWork(this);
			}
			
			// don't remove temp folder till after record to queue in case the logger needs the folder to read log content from
			RecordStruct params = this.task.getParams();
			
			if (params != null) {
				String tempFolder = params.getFieldAsString("_TempFolder");
				
				if (StringUtil.isNotEmpty(tempFolder))
					FileUtil.deleteDirectory(Paths.get(tempFolder));
			}			
			
			// TODO what should we do if not started - (this.started == -1)
			if (this.task.isFromWorkQueue())
				this.infoTr(154, this.getCode());
			else
				this.traceTr(154, this.getCode());
		}
		finally {
			this.completionlock.unlock();
			
			// always mark task completed so it isn't stuck in work pool, thread safe
			Hub.instance.getWorkPool().complete(this);
		}
	}

	@Override
	public String toString() {
		return this.task.getTitle() + " (" + this.task.getId() + ")";
	}
	
	public RecordStruct toStatusReport() {
		RecordStruct rec = new RecordStruct();
		
		rec.setField("Id", this.task.getId());
		rec.setField("Title", this.task.getTitle());
		
		rec.setField("Tags", this.task.getTags());
		
		rec.setField("Completed", this.completed);
		
		// TODO started, last touched/action, code, message, finished...
		
		return rec;
	}
	
	@Override
	public int hashCode() {
		return this.task.getTitle().hashCode();
	}
	
	/*
	 * For scripting calls - set the return value (convert to struct if not already) then call complete all at once
	 * @param v
	 */
	public void returnValue(Object v) {
		this.value = Struct.objectToStruct(v);
		this.complete();
	}
	
	public void returnEmpty() {
		this.complete();
	}	
	
	/* TODO supports groovy, enhance
	@Override
	public Object invokeMethod(String name, Object arg1) {
		// is really an object array
		Object[] args = (Object[])arg1;
		
		if ("return".equals(name)) {
			if (args.length > 0)
				this.returnValue(args[0]);
			else
				this.returnEmpty();
			
			return null;
		}
		
		return super.invokeMethod(name, arg1);
	}
	*/

	public RecordStruct status() {
		RecordStruct status = this.task.status();
		
		// TODO some of this may need review
		status.setField("Status", this.completed ? "Completed" : "Running");			
		status.setField("Start", new DateTime(this.started)); 
		status.setField("End", null); 
		status.setField("Hub", OperationContext.getHubId());
		
		status.setField("Code", this.getCode());
		status.setField("Message", this.getMessage()); 
		status.setField("Log", this.getContext().getLog());
		status.setField("Progress", this.opcontext.getProgressMessage()); 
		status.setField("StepName", this.opcontext.getCurrentStepName()); 
		status.setField("Completed", this.opcontext.getAmountCompleted()); 
		status.setField("Step", this.opcontext.getCurrentStep()); 
		status.setField("Steps", this.opcontext.getSteps());
		
		return status;
	}
}
