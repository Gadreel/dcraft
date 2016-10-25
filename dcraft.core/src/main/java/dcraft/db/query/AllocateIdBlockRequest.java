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

import dcraft.db.DataRequest;
import dcraft.struct.RecordStruct;

// TODO, get a block of ids for the given table (returns hid also so we can build
public class AllocateIdBlockRequest extends DataRequest {
	public AllocateIdBlockRequest(String table, int count) {
		super("dcAllocateIdBlock");
		
		RecordStruct params = new RecordStruct();
		
		this.parameters = params;
	
		params.setField("Table", table);
		params.setField("Count", count);
	}
	
	// process return values, store locally
	
	// isNeedMore
	
	// getMore support
	
	// next
}
