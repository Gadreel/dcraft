package dcraft.web.ui.tags;

import java.lang.ref.WeakReference;
import java.util.List;

import dcraft.hub.Hub;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.OperationObserver;
import dcraft.web.ui.UIElement;
import dcraft.web.ui.UIWork;
import dcraft.work.IWork;
import dcraft.work.Task;
import dcraft.work.TaskRun;
import dcraft.xml.XNode;

public class ServerInfo extends UIElement {
	public ServerInfo() {
		super("dc.ServerInfo");
	}
	
	@Override
	public void expand(WeakReference<UIWork> work) {
		// TODO add parameters - only works for Admins 
		
		work.get().incExpand();
		
		Task itask = Task
			.taskWithSubContext()
			.withTitle("Collecting Server Info")
			.withObserver(new OperationObserver() {
				@Override
				public void completed(OperationContext ctx) {
					work.get().decExpand();
				}
			})
			.withWork(new IWork() {
				@Override
				public void run(TaskRun trun) {
					trun.info("Starting Task: " + trun.getTask());
					
					ServerInfo.this.with(new UIElement("div")
							.withText("Value from database.")
						);
					
					ServerInfo.this.getRoot()
						.with(new UIElement("dc.Function").withAttribute("Mode", "Load").withCData("console.log('t1');"))
						.with(new UIElement("dc.Function").withAttribute("Mode", "Load").withCData("console.log('t2');"));
					
					trun.complete();
				}
			});
		
		Hub.instance.getScheduler().runIn(itask, 2);
	}
	
	@Override
	public void translate(WeakReference<UIWork> work, List<XNode> pnodes) {
		// don't change my identity until after the scripts run
		this.setName("div");
		this.withAttribute("class", "dcw-server-info");
		
		super.translate(work, pnodes);
	}
}
