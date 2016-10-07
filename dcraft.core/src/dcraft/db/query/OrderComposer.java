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
import dcraft.util.StringUtil;

/**
 * A Composer script to generate values to order by.
 * 
 * @author Andy
 *
 */
public class OrderComposer implements IOrderField {
	protected RecordStruct column = new RecordStruct();
	
	/**
	 * @param composer name of content generating script
	 * @param format formatting for return value
	 */
	public OrderComposer(String composer, String format) {
		this.column.setField("Composer", composer);
		
		if (StringUtil.isNotEmpty(format))
			this.column.setField("Format", format);
	}
	
	/* (non-Javadoc)
	 * @see dcraft.db.query.ISelectItem#getSelection()
	 */
	@Override
	public Struct getParams() {
		return this.column;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.column.toString();
	}
}
