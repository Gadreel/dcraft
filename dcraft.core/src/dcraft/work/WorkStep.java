package dcraft.work;

import java.util.function.Function;

public class WorkStep {
	// special case of WorkStep - do not add it to your step list, it just means go to the next step in my list
	// and can be used as a return value
	static public final WorkStep NEXT = new WorkStep();
	// special case of WorkStep - do not add it to your step list
	// means wait, I'll tell you when to transition
	static public final WorkStep WAIT = new WorkStep();
	
	static public WorkStep allocate(String title, Function<TaskRun, WorkStep> method) {
		WorkStep step = new WorkStep();
		step.title = title;
		step.method = method;
		return step;
	}
	
	protected String title = null;
	protected Function<TaskRun, WorkStep> method = null;
	
	public String getTitle() {
		return this.title;
	}
	
	public WorkStep runStep(TaskRun run) {
		return this.method.apply(run);
	}
}