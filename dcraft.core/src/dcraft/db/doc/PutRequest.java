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

import dcraft.db.ReplicatedDataRequest;
import dcraft.struct.Struct;

public class PutRequest extends ReplicatedDataRequest {
	public PutRequest(String id, Struct doc) {
		this(id, null, doc);
	}
	
	public PutRequest(String id, String path, Struct doc) {
		super("dcPutDocument");
		
		/* TODO
		this.parameters.setField("Id", id);		
		this.parameters.setField("Document", doc);
		
		if (StringUtil.isNotEmpty(path))
			this.parameters.setField("Path", path);
			*/
	}
}
