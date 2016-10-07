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
package dcraft.struct.builder;

import dcraft.lang.Memory;
import dcraft.struct.CompositeParser;
import dcraft.struct.CompositeStruct;

public class JsonMemoryBuilder extends JsonBuilder {
	protected Memory mem = new Memory();

	public Memory getMemory() {
		return this.mem;
	}
	
	public JsonMemoryBuilder() {
		super(false);
	}
	
	public JsonMemoryBuilder(boolean pretty) {
		super(pretty);
	}
	
	@Override
	public Memory toMemory() {
		return this.mem;
	}
	
	@Override
	public CompositeStruct toLocal() {
		this.mem.setPosition(0);
		return CompositeParser.parseJson(this.mem).getResult();
	}

	@Override
	public void write(String v) {
		this.mem.write(v);		
	}

	@Override
	public void writeChar(char v) {
		this.mem.writeChar(v);		
	}
}
