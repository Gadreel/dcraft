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

import dcraft.schema.DataType;
import dcraft.schema.RootType;
import dcraft.script.StackEntry;
import dcraft.xml.XElement;


abstract public class ScalarStruct extends Struct {
	abstract public Object getGenericValue();
	abstract public void adaptValue(Object v);
	
	public ScalarStruct() {
	}
	
	public ScalarStruct(DataType type) {
		super(type);
	}

	abstract public int compare(Object y);
	
	// just a reminder of the things to override in types

	@Override
	abstract public boolean equals(Object obj);

	@Override
	abstract public int hashCode();
	
	// interior method - don't call unless you understand - TODO maybe move to Struct level...
	public boolean validateData(DataType type) {
		return true;
	}
	
	abstract public Object toInternalValue(RootType t);
	
	abstract public boolean checkLogic(StackEntry stack, XElement source);
}
