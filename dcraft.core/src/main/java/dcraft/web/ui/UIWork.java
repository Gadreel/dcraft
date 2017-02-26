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

import java.lang.ref.WeakReference;

import dcraft.lang.CountDownCallback;
import dcraft.lang.op.OperationCallback;
import dcraft.web.core.IOutputContext;
import dcraft.work.StateWork;
import dcraft.work.TaskRun;
import dcraft.work.WorkStep;

public class UIWork extends StateWork {
	final public WorkStep EXPAND = WorkStep.allocate("Expand", this::expand);
	final public WorkStep BUILD = WorkStep.allocate("Build", this::build);
	final public WorkStep TRANSLATE = WorkStep.allocate("Translate", this::translate);
	final public WorkStep SEQUENCE = WorkStep.allocate("Sequence", this::sequence);
	
	protected UIElement root = null;
	protected IOutputContext ctx = null;

	// do not set the callbacks at instantiation - they may have the wrong OperationContext
	protected CountDownCallback expandcount = null;
	protected CountDownCallback buildcount = null;
	protected CountDownCallback translatedcount = null;
	
	public void setRoot(UIElement v) {
		this.root = v;
	}
	
	public void setContext(IOutputContext v) {
		this.ctx = v;
	}
	
	public IOutputContext getContext() {
		return this.ctx;
	}
	
	public void incExpand() {
		this.expandcount.increment();
	}
	
	public void decExpand() {
		this.expandcount.countDown();
	}
	
	public void incBuild() {
		this.buildcount.increment();
	}
	
	public void decBuild() {
		this.buildcount.countDown();
	}
	
	public void incTranslate() {
		this.translatedcount.increment();
	}
	
	public void decTranslate() {
		this.translatedcount.countDown();
	}
	
	@Override
	public void prepSteps(TaskRun trun) {
		this.withStep(this.INITIALIZE)
			.withStep(this.EXPAND)
			.withStep(this.BUILD)
			.withStep(this.TRANSLATE)
			.withStep(this.SEQUENCE)
			.withStep(this.FINIALIZE);
	}
	
	@Override
	public WorkStep initialize(TaskRun trun) {
		// setup the callbacks here so the correct OperationContext applies
		
		this.expandcount = new CountDownCallback(1, new OperationCallback() {
			@Override
			public void callback() {
				TaskRun trun = this.getContext().getTaskRun();
				UIWork.this.transition(trun, WorkStep.NEXT);
			}
		});
		
		this.buildcount = new CountDownCallback(1, new OperationCallback() {
			@Override
			public void callback() {
				TaskRun trun = this.getContext().getTaskRun();
				UIWork.this.transition(trun, WorkStep.NEXT);
			}
		});
		
		// refine does not have a count system, it is simple - goes right to translate

		this.translatedcount = new CountDownCallback(1, new OperationCallback() {
			@Override
			public void callback() {
				TaskRun trun = this.getContext().getTaskRun();
				UIWork.this.transition(trun, WorkStep.NEXT);
			}
		});
		
		return WorkStep.NEXT;
	}
	
	public WorkStep expand(TaskRun trun) {
		this.root.expand(new WeakReference<UIWork>(this));
		
		this.expandcount.countDown();
		
		return null;	
	}
	
	public WorkStep build(TaskRun trun) {
		this.root.build(new WeakReference<UIWork>(this));
		
		this.buildcount.countDown();
		
		return null;	
	}

	public WorkStep translate(TaskRun trun) {
		this.root.translate(new WeakReference<UIWork>(this), null);

		this.translatedcount.countDown();
		
		return null;
	}

	public WorkStep sequence(TaskRun trun) {
		this.root.sequence();
		
		return WorkStep.NEXT;
	}

	public WorkStep finialize(TaskRun trun) {
		return WorkStep.NEXT;
	}
}
