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
package dcraft.ctp.stream;

import dcraft.lang.op.OperationContext;
import dcraft.lang.op.OperationObserver;
import dcraft.work.IWork;
import dcraft.work.TaskRun;

public class StreamWork implements IWork {
	protected IStreamDest dest = null;
	protected boolean init = false;
	
	public StreamWork(IStreamDest dest) {
		this.dest = dest;
	}

	@Override
	public void run(TaskRun trun) {
		if (!this.init) {
			trun.getContext().addObserver(new OperationObserver() {
				@Override
				public void completed(OperationContext ctx) {
					IStreamDest d = StreamWork.this.dest;
					
					if (d != null)
						d.cleanup();
					
					StreamWork.this.dest = null;
				}
			});
			
			this.init = true;
		}
		
		IStreamDest d = this.dest;
		
		if (d != null)
			d.execute();
		else 
			trun.kill("Attempted to run StreamWork but missing dest.");
	}
}
