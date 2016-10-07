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
package dcraft.db.query;

import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;

/**
 * A Composer script to generate content in a query.
 * 
 * @author Andy
 *
 */
public class SelectComposer implements ISelectField {
	protected RecordStruct column = new RecordStruct();
	
	public SelectComposer withComposer(String v) {
		this.column.setField("Composer", v);		
		return this;
	}
	
	public SelectComposer withName(String v) {
		this.column.setField("Name", v);		
		return this;
	}
	
	public SelectComposer withFormat(String v) {
		this.column.setField("Format", v);		
		return this;
	}
	
	public SelectComposer withField(String v) {
		this.column.setField("Field", v);		
		return this;
	}
	
	public SelectComposer withParams(RecordStruct v) {
		this.column.setField("Params", v);		
		return this;
	}

	@Override
	public Struct getParams() {
		return this.column;
	}
	
	@Override
	public String toString() {
		return this.column.toString();
	}
}
