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
package dcraft.bus;

import dcraft.hub.Hub;
import dcraft.lang.TimeoutPlan;
import dcraft.lang.op.FuncCallback;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.UserContext;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;

abstract public class ServiceResult extends FuncCallback<Message> {
	protected String replytag = null;
	
	public void setReplyTag(String v) {
		this.replytag = v;
	}
	
	// timeout on regular schedule  
	public ServiceResult() {
		super(TimeoutPlan.Regular);
	}
	
	public ServiceResult(TimeoutPlan plan) {
		super(plan);
	}
	
	@Override
	public boolean abandon() {
		if (super.abandon()) {
			Hub.instance.getBus().getLocalHub().getReplyService().clearReply(this.replytag);
			return true;
		}
		
		return false;
	}
	
	public void setReply(Message v) {
		this.setResult(v);
		
		this.opcontext.logResult(v);
		
		UserContext usr = OperationContext.get().getUserContext(); 
		
		// switch the user without switching the operation context, and not elevating
		OperationContext.switchUser(this.opcontext, usr);
	}
	
	/**
	 * @return the service result as String
	 */
	public String getBodyAsString() {
		return this.getResult().getFieldAsString("Body");
	}
	
	/**
	 * @return the service result as Integer
	 */
	public Long getBodyAsInteger() {
		return this.getResult().getFieldAsInteger("Body");
	}

	/**
	 * @return the service result as RecordStruct
	 */
	public RecordStruct getBodyAsRec() {
		return this.getResult().getFieldAsRecord("Body");
	}

	/**
	 * @return the service result as ListStruct
	 */
	public ListStruct getBodyAsList() {
		return this.getResult().getFieldAsList("Body");
	}
}
