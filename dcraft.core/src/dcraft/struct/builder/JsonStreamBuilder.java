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

import java.io.PrintStream;

public class JsonStreamBuilder extends JsonBuilder {
	protected PrintStream pw = null;
	
	public JsonStreamBuilder(PrintStream pw) {
		super(false);
		this.pw = pw;
	}
	
	public JsonStreamBuilder(PrintStream pw, boolean pretty) {
		super(pretty);
		this.pw = pw;
	}

	public void write(String v) {
		this.pw.append(v);
	}

	public void writeChar(char v) {
		this.pw.append(v);
	}

	public PrintStream startStreamValue() {
		this.write("\"");
		
		return new PrintStream(this.pw) {
			@Override
			public PrintStream append(CharSequence csq) {
				JsonStreamBuilder.this.writeEscape(csq);
				return this;
			}
			
			@Override
			public PrintStream append(char c) {
				JsonStreamBuilder.this.writeEscape(c);
				return this;
			}
		};
	}

	public void endStreamValue() throws BuilderStateException {
		this.write("\"");
		
		this.completeValue();
	}	
}
