package dcraft.work;

import java.util.ArrayList;
import java.util.List;

abstract public class StateWork implements IWork {
	final public WorkStep INITIALIZE = WorkStep.allocate("Initialize", this::initialize);
	final public WorkStep FINIALIZE = WorkStep.allocate("Finialize", this::finialize);
	
	protected List<WorkStep> steps = new ArrayList<>();
	protected WorkStep last = null;
	protected WorkStep current = this.INITIALIZE;
	protected boolean failOnErrors = true;
	
	public StateWork withStep(WorkStep v) {
		this.steps.add(v);
		return this;
	}
	
	public StateWork withSteps(WorkStep... v) {
		for (WorkStep s : v)
			this.steps.add(s);
		
		return this;
	}
	
	public StateWork withoutFailOnErrors() {
		this.failOnErrors = false;
		return this;
	}
	
	public StateWork withFailOnErrors() {
		this.failOnErrors = true;
		return this;
	}
	
	abstract public void prepSteps(TaskRun trun);
	
	@Override
	public void run(TaskRun trun) {
		// null means we are still processing something
		if (this.current == null)
			return;
		
		// indicate the number of steps
		if (this.current == this.INITIALIZE) {
			this.prepSteps(trun);
			trun.getContext().setSteps(steps.size());
		}
		
		trun.getContext().setCurrentStep(this.steps.indexOf(this.current) + 1, this.current.getTitle());
		
		this.last = this.current;
		
		try {
			this.transition(trun, this.current, this.current.runStep(trun));
		}
		catch (Exception x) {
			WorkStep s = this.current;
			
			System.out.println("error: " + trun.getTask().getTitle());
			
			x.printStackTrace(System.out);
			
			if (s == null)
				trun.error("Unexpected StateWork exception - no current step");
			else
				trun.error("Unexpected StateWork exception for: " + s.getTitle());
			
			trun.kill("Message: " + x);
		}
	}
	
	public void transition(TaskRun trun, WorkStep to) {
		this.transition(trun, this.last, to);
	}
	
	public void transition(TaskRun trun, WorkStep from, WorkStep to) {
		if (this.failOnErrors && trun.hasErrors()) {
			trun.kill();
			return;
		}
		
		if (from == this.FINIALIZE) {
			trun.complete();
			return;
		}
		
		// null means step will manually call transition - typical of async calls
		if ((to == null) || (to == WorkStep.WAIT))
			return;
		
		if (to == WorkStep.NEXT) {
			int topos = this.steps.indexOf(from) + 1;
			
			if (topos < this.steps.size()) {
				to = this.steps.get(topos);
			}
			else {
				trun.complete();
				return;
			}
		}
		
		this.last = from;
		this.current = to;

		this.transitionEvent(trun, from, to);
		
		trun.resume();
	}
	
	public void transitionEvent(TaskRun trun, WorkStep from, WorkStep to) {
		// real work can override
	}

	public WorkStep initialize(TaskRun trun) {
		trun.info("Starting Task: " + trun.getTask());
		
		if (this.steps.size() < 2)
			return this.FINIALIZE;
		
		return this.steps.get(1);
	}

	public WorkStep finialize(TaskRun trun) {
		trun.info("Ending Task: " + trun.getTask());
		return null;
	}
}
