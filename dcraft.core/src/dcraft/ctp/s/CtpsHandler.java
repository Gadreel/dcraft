package dcraft.ctp.s;

import dcraft.ctp.CtpAdapter;
import dcraft.ctp.CtpCommand;
import dcraft.ctp.ICommandHandler;
import dcraft.ctp.cmd.EngageCommand;
import dcraft.ctp.cmd.ResponseCommand;
import dcraft.hub.Hub;
import dcraft.lang.op.OperationContext;
import dcraft.work.IWork;
import dcraft.work.Task;
import dcraft.work.TaskRun;

public class CtpsHandler implements ICommandHandler {

	@Override
	public void handle(CtpCommand cmd, CtpAdapter adapter) throws Exception {
		OperationContext ctx = OperationContext.get();
		
		ctx.touch();
		
		System.out.println("Ctp-S Server got command: " + cmd.getCmdCode());
		
		if (cmd instanceof EngageCommand) {
			if (ctx != null) {
				// put the call back into the work pool, don't tie up the IO thread 
				Task t = Task.taskWithContext(ctx.subContext())
					.withWork(new IWork() {
						@Override
						public void run(TaskRun trun) {
      						try {
      							adapter.sendCommand(new ResponseCommand());
							}
							catch (Exception x) {
								System.out.println("Ctp-S Server error: " + x);
							}
							
							adapter.read();
							
							trun.complete();
						}
					});
				
				Hub.instance.getWorkPool().submit(t);
			}
			
			return;
		}
	}

	@Override
	public void close() {
		System.out.println("Ctp-S Server Connection closed");
	}
}
