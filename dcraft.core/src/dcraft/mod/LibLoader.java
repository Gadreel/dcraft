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
package dcraft.mod;

import java.util.HashMap;
import java.util.Map;
import java.util.zip.Adler32;

public class LibLoader {
	protected String name = null;
	protected Map<String,byte[]> entries = new HashMap<String,byte[]>();

	public LibLoader(String name) {
		this.name = name;
	}
	
	public byte[] getEntry(String name) {
		return this.entries.get(name);
	}
	
	public boolean hasEntry(String name) {
		return this.entries.containsKey(name);
	}

	public void adler(Adler32 ad) {
		for (byte[] entry : this.entries.values())
			ad.update(entry);
	}
}
