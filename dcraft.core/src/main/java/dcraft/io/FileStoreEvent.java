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
package dcraft.io;

import dcraft.filestore.CommonPath;

public class FileStoreEvent {
	protected boolean delete = false;		// else it was modified
	protected CommonPath path = null;		// relative to the package
	
	public boolean isDeleted() {
		return this.delete;
	}
	
	public CommonPath getPath() {
		return this.path;
	}
}

