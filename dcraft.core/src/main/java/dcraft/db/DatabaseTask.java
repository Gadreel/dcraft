/* ************************************************************************
#
#  designCraft.io
#
#  http://designcraft.io/
#
#  Copyright:
#    Copyright 2012 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */
package dcraft.db;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import dcraft.hub.TenantInfo;
import dcraft.hub.Hub;
import dcraft.lang.op.OperationContext;
import dcraft.schema.SchemaManager;
import dcraft.struct.CompositeStruct;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.builder.ICompositeBuilder;

/**
 * Internal class used to track a request and response across thread boundaries
 * (after being submitted to queue) and also assists with the post processing
 * of results and notifying the submitter via the callback.
 * 
 * @author Andy
 *
 */
public class DatabaseTask {
	protected DatabaseResult result = null;		// this is the official OC
	protected RecordStruct request = null;
	protected List<String> tenants = null;
	protected IDatabaseManager dbm = null;
	
	public IDatabaseManager getDbm() {
		return this.dbm;
	}
	
	public void setDbm(IDatabaseManager v) {
		this.dbm = v;
	}
	
	public DatabaseResult getResult() {
		return this.result;
	}
	
	public void setResult(DatabaseResult v) {
		this.result = v;
	}
	
	public void setRequest(RecordStruct v) {
		this.request = v;
	}
	
	public ICompositeBuilder getBuilder() {
		return this.result.getResult();
	}
	
	public RecordStruct getRequest() {
		return this.request;
	}
	
	public String getTenant() {
		if ((this.tenants == null) || (this.tenants.size() == 0))		
			return this.request.getFieldAsString("Tenant");
		
		return this.tenants.get(this.tenants.size() - 1);
	}
	
	public SchemaManager getSchema() {
		String did = this.getTenant();
		
		TenantInfo di = Hub.instance.getTenantInfo(did);
		
		if (di != null)
			return di.getSchema();
		
		return OperationContext.get().getSchema();
	}
	
	public BigDecimal getStamp() {
		return this.request.getFieldAsDecimal("Stamp");
	}
	
	public String getName() {
		return this.request.getFieldAsString("Name");
	}
	
	public boolean isReplicating() {
		return this.request.getFieldAsBooleanOrFalse("Replicating");
	}
	
	public CompositeStruct getParams() {
		return this.request.getFieldAsComposite("Params");
	}

	public RecordStruct getParamsAsRecord() {
		return this.request.getFieldAsRecord("Params");
	}

	public ListStruct getParamsAsList() {
		return this.request.getFieldAsList("Params");
	}
	
	/**
	 * Called after "result" is filled.  Sets about with post processing and call backs.
	 */
	public void complete() {
		this.result.useContext();	// this is OK because the firing thread does not have a significant ctx 
		
		// TODO change this to a Validate call on DatabaseResult
		if (this.result.getResult() instanceof ObjectResult) {
			// TODO not currently working, review
			CompositeStruct res = ((ObjectResult)this.result.getResult()).getResultAsComposite();
			
			this.getSchema().validateProcResponse(this.request.getFieldAsString("Name"), res);
		}
		
		this.result.complete();
	}

	public void pushTenant(String did) {
		if (this.tenants == null)
			this.tenants = new ArrayList<>();
		
		this.tenants.add(did);
	}

	public void popTenant() {
		if (this.tenants != null)
			this.tenants.remove(this.tenants.size() - 1);
	}
}
