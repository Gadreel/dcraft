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
package dcraft.db.doc;

import dcraft.struct.RecordStruct;
import dcraft.util.StringUtil;

public class QueryRequest extends dcraft.db.DataRequest {
	public QueryRequest(String id) {
		this(id, null);
	}
	
	public QueryRequest(String id, String path) {
		super("dcQueryDocument");

		RecordStruct params = new RecordStruct();
		
		params.setField("Id", id);		
		
		if (StringUtil.isNotEmpty(path))
			params.setField("Path", path);
		
		this.withParams(params);
	}
}
