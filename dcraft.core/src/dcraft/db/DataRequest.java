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

import dcraft.db.IDatabaseRequest;
import dcraft.struct.CompositeStruct;
import dcraft.struct.RecordStruct;
import dcraft.util.StringUtil;

/**
 * Assemble a generic Query request for the database.  A query request should not
 * call a stored procedure that will cause the database to update/alter data
 * (other than temp tables and caches).  Other than that restriction
 * this class can call nearly any stored procedure if the parameters are assembled 
 * correctly.
 * 
 * @author Andy
 *
 */
public class DataRequest implements IDatabaseRequest {
	protected CompositeStruct parameters = null;	
	protected String proc = null;
	protected String domain = null;
	
	@Override
	public boolean hasTenant() {		
		return StringUtil.isNotEmpty(this.domain);
	}
	
	@Override
	public String getTenant() {
		return this.domain;
	}
	
	public DataRequest withRootTenant() {
		this.domain = Constants.DB_GLOBAL_ROOT_TENANT;
		return this;
	}
	
	public DataRequest withTenant(String v) {
		this.domain = v;
		return this;
	}
	
	public DataRequest withParams(CompositeStruct v) {
		this.parameters = v;
		return this;
	}
	
	public DataRequest withEmptyParams() {
		this.parameters = new RecordStruct();
		return this;
	}
	
	/**
	 * Build an unguided query request.
	 * 
	 * @param proc procedure name to call
	 */
	public DataRequest(String proc) {
		this.proc = proc;
	}
	
	@Override
	public CompositeStruct buildParams() {
		return this.parameters;
	}
	
	@Override
	public boolean isReplicate() {
		return false;
	}
	
	@Override
	public String getProcedure() {
		return this.proc;
	}
}
