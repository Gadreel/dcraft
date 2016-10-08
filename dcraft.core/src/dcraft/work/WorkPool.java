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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import dcraft.hub.Hub;
import dcraft.hub.ISystemWork;
import dcraft.hub.SysReporter;
import dcraft.lang.op.IOperationObserver;
import dcraft.lang.op.OperationResult;
import dcraft.log.Logger;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.xml.XAttribute;
import dcraft.xml.XElement;

public class WorkPool implements ExecutorService {
	protected LinkedBlockingQueue<TaskRun> queue = new LinkedBlockingQueue<>();
	protected Worker[] slots = null;
	protected ConcurrentHashMap<String, WorkTopic> topics = new ConcurrentHashMap<>();
	
	// when set, the pool will work only on N number of tasks until one of those tasks completes
	// where upon a new task from the general queue "queue" can be accepted
	// in other words, when fullsize == inprogress.size() we are full and do no additional processing
	// when this is null, we pull tasks off the general queue ANY time a thread has spare cycles
	
	protected AtomicLong totalThreadsCreated = new AtomicLong();
	protected AtomicLong totalThreadsHung = new AtomicLong();		// based on timeout
	
	protected boolean shutdown = false;		// TODO replace with state - starting, running, stopping, stopped
	protected long scheduleFreq = 150;
	protected boolean poolTrace = false;
	
	public void init(OperationResult or, XElement config) {
		int size = 16;
		
		if (config != null) {
			size = Integer.parseInt(config.getAttribute("Threads", "16"));
			this.scheduleFreq = Integer.parseInt(config.getAttribute("TimeoutChecker", "150"));
			this.poolTrace = "True".equals(config.getAttribute("Trace", "False"));
		}
		
		this.slots = new Worker[size];
		
		// place the default topic in - it might be overridden in config
		WorkTopic deftopic = new WorkTopic();
		deftopic.init(or, new XElement("Topic", new XAttribute("Name", "Default")), size);
		
		if (or.hasErrors())
			return;
		
		this.topics.put(deftopic.getName(), deftopic);
		
		if (config != null) {
 			for (XElement topicel : config.selectAll("Topic")) {
 				WorkTopic topic = new WorkTopic();
 				topic.init(or, topicel, size);
 				
 				if (or.hasErrors())
 					return;
 				
 				this.topics.put(topic.getName(), topic);
 			}
		}
		
		Hub.instance.getCountManager().allocateSetNumberCounter("dcWorkPool_Topics", this.topics.size());
		
		Hub.instance.getCountManager().allocateSetNumberCounter("dcWorkPool_Threads", size);
	}

	public void addTopic(WorkTopic topic) {
		this.topics.put(topic.getName(), topic);
	}

	public void removeTopic(String name) {
		this.topics.remove(name);
	}
	
	public int threadCount() {
		return this.slots.length; 
	}
	
	public long threadsCreated() {
		return this.totalThreadsCreated.get();
	}
	
	public void incThreadsCreated() {
		long n = this.totalThreadsCreated.incrementAndGet();
		
		Hub.instance.getCountManager().allocateSetNumberCounter("dcWorkPool_ThreadsCreated", n);
	}
	
	public long threadsHung() {
		return this.totalThreadsHung.get();
	}
	
	public void incThreadsHung() {
		long n = this.totalThreadsHung.incrementAndGet();

		Hub.instance.getCountManager().allocateSetNumberCounter("dcWorkPool_ThreadsHung", n);
	}
	
	public Collection<WorkTopic> getTopics() {
		return this.topics.values();
	}
	
	// to work as an Executor
	@Override
	public void execute(Runnable command) {
		if (command instanceof TaskRun)
			this.submit((TaskRun)command);		// useful for resume
		else {
			Task builder = Task.taskWithSubContext()
				.withWork(command);
			
			this.submit(builder);
		}
	}
	
	public TaskRun submit(IWork work) {
		Task task = Task.taskWithSubContext()
			.withWork(work);
	
		return this.submit(task);
	}
	
	public TaskRun submit(Task task) {
		TaskRun run = new TaskRun(task);
		
		this.submit(run);
		
		return run;
	}
	
	public TaskRun submit(IWork work, IOperationObserver observer) {
		Task task = Task.taskWithSubContext()
			.withWork(work);
	
		return this.submit(task, observer);
	}
	
	public TaskRun submit(Task task, IOperationObserver observer) {
		TaskRun run = new TaskRun(task);
		
		if (observer != null)
			task.withObserver(observer);
		
		this.submit(run);
		
		return run;
	}
	
	// this might accept "resubmits" or "new" - either way we should run "complete" if it fails
	public void submit(TaskRun run) {
		if (run == null)
			return;
		
		// don't run if shut down
		if (this.shutdown) {
			run.errorTr(197, run);
			run.complete();
			return;
		}
		
		// make sure context and logging, etc are ready
		run.prep();
		
		// this will also catch if run was resubmitted but killed
		if (run.hasErrors() && !run.hasStarted()) {
			run.errorTr(216, run);		// TODO different error messages if resume
			run.complete();
			return;
		}
		
		// after prep, prep will setup context 
		if (run.getContext() == null) {
			run.errorTr(198, run);
			run.complete();
			return;
		}
		
		if (run.isComplete())
			return;
		
		// if resume then see if we are a currently running thread, if so just reuse the thread (throttling allowing)
		if (run.hasStarted()) { 
			Worker w = this.slots[run.slot];
			
			if ((w != null) && w.resume(run))
				return;
		}
		
		// find the work topic
		WorkTopic topic = this.getTopicOrDefault(run);
		
		// see if the topic advises a submit, if not the topic will hold onto the run
		// in a wait queue.  if true then we put right on the active work queue
		if (topic.canSubmit(run)) 
			this.queue.add(run);
	}
	
	public TaskRun take() throws InterruptedException {
		TaskRun run = this.queue.take();
		
		// find the work topic
		WorkTopic topic = this.getTopicOrDefault(run);
		
		// let the topic know this run is in progress
		topic.took(run);
		
		return run;
	}

	public void complete(TaskRun run) {
		// find the work topic
		WorkTopic topic = this.getTopicOrDefault(run);
		
		// tell the topic to complete run
		TaskRun newrun = topic.complete(run);
		
		// see if the topic advises a submit
		if (newrun != null) {
			Logger.traceTr(199, newrun);
			this.queue.add(newrun);
		}
	}
	
	public WorkTopic getTopicOrDefault(String name) {
		WorkTopic topic = this.topics.get(name);
		
		if (topic != null)
			return topic;
		
		return this.topics.get("Default");
	}
	
	public WorkTopic getTopicOrDefault(TaskRun run) {
		WorkTopic topic = this.topics.get(run.getTask().getTopic());
		
		if (topic != null)
			return topic;
		
		return this.topics.get("Default");
	}
	
	public void start(OperationResult or) {
		for (int i = 0; i < this.slots.length; i++) 
			this.initSlot(i);
		
		// the task defines a timeout, not the pool.  tasks with no timeout set
		// will simply not timeout and the pool will be burdened - so set timeouts
		// on tasks if there is any possibility that they might
		Hub.instance.getClock().addSlowSystemWorker(new ISystemWork() {
			@Override
			public void run(SysReporter reporter) {
				reporter.setStatus("Reviewing hung topics");
				
				// even when stopping we still want to clear hung tasks
				for (int i = 0; i < WorkPool.this.slots.length; i++) {
					Worker w = WorkPool.this.slots[i];
					
					if (w != null) 
						w.checkIfHung();
				}
				
				for (WorkTopic b : WorkPool.this.topics.values()) 
					b.checkIfHung();
				
				reporter.setStatus("After reviewing hung topics");
			}
			
			@Override
			public int period() {
				return 5;  // TODO remove/advise -- this.scheduleFreq);   
			}
		});    
	}
	
	protected void initSlot(int slot) {
		if (!this.shutdown) {
			Worker work = new Worker();
			this.slots[slot] = work;
			work.start(slot);
		}
		else
			this.slots[slot] = null;
		
		//Logger.trace("Thread Pool slot " + slot + " changed, now have " + this.slots.size() + " threads");
	}
	
	public void stop(OperationResult or) {
		or.trace(0, "Work Pool Stopping");
		
		this.shutdown = true;
		
		// quickly let everyone know it is time to stop
		or.trace(0, "Work Pool Stopping Nice");
		
		for (int i = 0; i < WorkPool.this.slots.length; i++) {
			Worker w = WorkPool.this.slots[i];
			
			if (w != null) 
				w.stopNice();
		}
		
		or.trace(0, "Work Pool Waiting");
				
		int remaincnt = 0;
		
		// wait a minute for things to finish up.   -- TODO config
		for (int i2 = 0; i2 < 60; i2++) {
			remaincnt = 0;
			
			for (int i = 0; i < WorkPool.this.slots.length; i++) {
				Worker w = WorkPool.this.slots[i];
				
				if (w != null) 
					remaincnt++;
			}
			
			if (remaincnt == 0)
				break;
			
			try {
				Thread.sleep(1000);
			}
			catch (Exception x) {				
			}
		}
		
		or.trace(0, "Work Pool Size: " + remaincnt);
		
		or.trace(0, "Work Pool Interrupt Remaining Workers");
		
		for (int i = 0; i < WorkPool.this.slots.length; i++) {
			Worker w = WorkPool.this.slots[i];
			
			if (w != null) 
				w.stop();
		}
		
		or.trace(0, "Work Pool Cleaning Topics");
		
		for (WorkTopic topic : this.topics.values()) 
			topic.stop();
		
		or.trace(0, "Work Pool Stopped");
	}

	public int queued() {
		return this.queue.size();		
	}
	
	public RecordStruct toStatusReport() {
		RecordStruct rec = new RecordStruct();
		
		rec.setField("Queued", this.queued());
		rec.setField("Threads", this.threadCount());
		rec.setField("ThreadsCreated", this.threadsCreated());
		rec.setField("ThreadsHung", this.threadsHung());
		
		ListStruct topics = new ListStruct();
		
		for (WorkTopic topic : this.topics.values()) 
			topics.addItem(topic.toStatusReport());

		rec.setField("Topics", topics);
		
		return rec;
	}
	
	// for a task by identity alone
	public RecordStruct status(String taskid) {
		/* TODO */
		
		return null;
	}
	
	// for a task by identity plus workid (slightly more secure)
	public RecordStruct status(String taskid, String workid) {
		for (WorkTopic topic : this.topics.values()) {
			TaskRun run = topic.findTask(taskid);
			
			if (run != null) 
				return run.status();
		}

		return null;
	}

	// TODO start - someday make this a full working executor service
	@Override
	public boolean awaitTermination(long arg0, TimeUnit arg1)
			throws InterruptedException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> arg0)
			throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> List<Future<T>> invokeAll(
			Collection<? extends Callable<T>> arg0, long arg1, TimeUnit arg2)
			throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> arg0)
			throws InterruptedException, ExecutionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> arg0, long arg1,
			TimeUnit arg2) throws InterruptedException, ExecutionException,
			TimeoutException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isShutdown() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isTerminated() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<Runnable> shutdownNow() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> Future<T> submit(Callable<T> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<?> submit(Runnable arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> Future<T> submit(Runnable arg0, T arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	public int inprogress() {
		int cnt = 0;
		
		for (WorkTopic topic : this.topics.values()) 
			cnt += topic.inprogress();
		
		return cnt;
	}
	
	// TODO end - someday make this a full working executor service
}
