package dcraft.mail;

import dcraft.hub.Hub;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.OperationObserver;
import dcraft.struct.RecordStruct;
import dcraft.work.StateWork;
import dcraft.work.Task;
import dcraft.work.TaskRun;
import dcraft.work.WorkStep;

public class BuildSendWork extends StateWork {
	final public WorkStep BUILD = WorkStep.allocate("Build", this::build);
	final public WorkStep SEND = WorkStep.allocate("Send", this::send);
	
	protected RecordStruct builtmail = null;
	
	@Override
	public void prepSteps(TaskRun trun) {
		this.withStep(this.INITIALIZE)
		.withStep(this.BUILD)
		.withStep(this.SEND)
		.withStep(this.FINIALIZE);
	}
	
	public WorkStep build(TaskRun trun) {
		Task sendtask = MailUtil.createBuildTask(trun.getTask().getParams());
		
		sendtask.withTopic("Default");	// "Default" - we should not wait on the send task as it count toward current task
		
		sendtask.withObserver(new OperationObserver() {
			@Override
			public void completed(OperationContext ctx) {
				BuildSendWork.this.builtmail = (RecordStruct) ctx.getTaskRun().getResult();
				BuildSendWork.this.transition(trun, WorkStep.NEXT);
			}
		});
		
		Hub.instance.getWorkPool().submit(sendtask);
		
		return WorkStep.WAIT;
	}
	
	public WorkStep send(TaskRun trun) {
		Task sendtask = MailUtil.createSendTask(this.builtmail);
		
		sendtask.withTopic("Default");	// "Default" - we should not wait on the send task as it count toward current task
		
		Hub.instance.getWorkPool().submit(sendtask);
		
		return this.FINIALIZE;
	}
}
