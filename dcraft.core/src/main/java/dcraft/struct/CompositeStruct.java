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
package dcraft.struct;

import io.netty.buffer.ByteBuf;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import dcraft.hub.Hub;
import dcraft.lang.Memory;
import dcraft.lang.chars.Special;
import dcraft.lang.op.FuncResult;
import dcraft.schema.DataType;
import dcraft.script.StackEntry;
import dcraft.struct.builder.BuilderStateException;
import dcraft.struct.builder.ICompositeOutput;
import dcraft.struct.builder.JsonMemoryBuilder;
import dcraft.struct.builder.JsonStreamBuilder;
import dcraft.struct.serial.CompositeToBufferBuilder;
import dcraft.xml.XElement;

/**
 * DivConq uses a specialized type system that provides type consistency across services 
 * (including web services), database fields and stored procedures, as well as scripting.
 * 
 * All scalars (including primitives) and composites (collections) are wrapped by some
 * subclass of Struct.  All composites are wrapped by a subclass of this class.  See
 * ListStruct and RecordStruct.
 * 
 *  TODO link to blog entries.
 * 
 * @author Andy
 *
 */
abstract public class CompositeStruct extends Struct implements ICompositeOutput {
	public CompositeStruct() {
	}
	
	public CompositeStruct(DataType type) {
		super(type);
	}
	
	/**
	 * A way to select a child or sub child structure similar to XPath but lightweight.
	 * Can select composites and scalars.  Use a . or / delimiter.
	 * 
	 * For example: "Toys.3.Name" called on "Person" Record means return the (Struct) name of the 
	 * 4th toy in this person's Toys list.
	 * 
	 * Cannot go up levels, or back to root.  Do not start with a dot or slash as in ".People".
	 * 
	 * @param path string holding the path to select
	 * @return selected structure if any, otherwise null
	 */
	public FuncResult<Struct> select(String path) {
		FuncResult<Struct> log = new FuncResult<Struct>();		
		log.setResult(this.select(PathPart.parse(log, path)));		
		return log;
	} 
	
	/**
	 * A way to select a child or sub child structure similar to XPath but lightweight.
	 * Can select composites and scalars.  Use a . or / delimiter.
	 * 
	 * For example: "Toys.3.Name" called on "Person" Record means return the (Struct) name of the 
	 * 4th toy in this person's Toys list.
	 * 
	 * Cannot go up levels, or back to root.  Do not start with a dot or slash as in ".People".
	 * 
	 * @param path parts of the path holding a list index or a field name
	 * @return selected structure if any, otherwise null
	 */
	abstract public Struct select(PathPart... path);	
	
	/**
	 * Does this collection have any items or fields
	 * 
	 * @return true if no items or fields
	 */
	abstract public boolean isEmpty();
	
	/**
	 * Remove all child fields or items.
	 */
	abstract public void clear();
	
	@Override
	public String toString() {
		try {
			JsonMemoryBuilder rb = new JsonMemoryBuilder();		
			this.toBuilder(rb);		
			return rb.getMemory().toString();
		}
		catch (Exception x) {
			//
		}
		
		return null;
	}
	
	// TODO there may be more efficient ways to do this, just a quick way for now
	@Override
	public boolean equals(Object obj) {
		if ((obj instanceof CompositeStruct) && this.toString().equals(((CompositeStruct)obj).toString()))
			return true;
			
		return super.equals(obj);
	}
	
	public String toPrettyString() {
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(os);
			JsonStreamBuilder rb = new JsonStreamBuilder(ps, true);		
			this.toBuilder(rb);		
			return os.toString("UTF-8");
		}
		catch (Exception x) {
			//
		}
		
		return null;
	}
	
	@Override
	public boolean isNull() {
		return false;
	}
	
	/**
	 * Convert the structure to Json and return in Memory (think StringBuilder in this usage).
	 *  
	 * @return Memory holding JSON representation of this structure and all children
	 * @throws BuilderStateException if the structure is invalid then this exception arises
	 */
	public Memory toMemory() throws BuilderStateException {		// TODO return funcresult
		JsonMemoryBuilder rb = new JsonMemoryBuilder();		
		this.toBuilder(rb);		
		return rb.getMemory();
	}
	
	public void toSerial(ByteBuf buf) throws BuilderStateException {		
		CompositeToBufferBuilder rb = new CompositeToBufferBuilder(buf);		
		this.toBuilder(rb);		
		rb.write(Special.End);
	}
	
	public void toSerialMemory(Memory res) throws BuilderStateException {
		ByteBuf buf = Hub.instance.getBufferAllocator().heapBuffer(16 * 1024, 16 * 1024 * 1024);
		
		try {
			CompositeToBufferBuilder rb = new CompositeToBufferBuilder(buf);		
			this.toBuilder(rb);		
			rb.write(Special.End);
			
			res.write(buf.array(), buf.arrayOffset(), buf.readableBytes());
		}
		finally {
			buf.release();
		}
	}
	
	public Memory toSerialMemory() throws BuilderStateException {
		ByteBuf buf = Hub.instance.getBufferAllocator().heapBuffer(16 * 1024, 16 * 1024 * 1024);
		
		try {
			CompositeToBufferBuilder rb = new CompositeToBufferBuilder(buf);		
			this.toBuilder(rb);		
			rb.write(Special.End);
			
			Memory res = new Memory(buf.readableBytes());
			res.write(buf.array(), buf.arrayOffset(), buf.readableBytes());
			
			return res;
		}
		finally {
			buf.release();
		}
	}
	
	@Override
	public void operation(StackEntry stack, XElement code) {
		if ("Clear".equals(code.getName())) {
			this.clear();
			stack.resume();
			return;
		}
		
		super.operation(stack, code);
	}
}
