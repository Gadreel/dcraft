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
package dcraft.web.ui;

import dcraft.work.TaskRun;

// remove the Build and Sequence steps, do only the Expand
public class UIInitialWork extends UIWork {
	@Override
	public void prepSteps(TaskRun trun) {
		this.withStep(this.INITIALIZE)
			.withStep(this.EXPAND)
			.withStep(this.BUILD)
			.withStep(this.FINIALIZE);
	}
	
	/*
	@Override
	public WorkStep initialize(TaskRun trun) {
		super.initialize(trun);

		// setup the callbacks here so the correct OperationContext applies
		
		this.buildcount = new CountDownCallback(1, new OperationCallback() {
			@Override
			public void callback() {
				TaskRun trun = this.getContext().getTaskRun();
				UIInitialWork.this.transition(trun, FINIALIZE);
			}
		});
		
		return this.EXPAND;
	}
	*/
}
