package dcraft.db.query;

import dcraft.struct.Struct;

/**
 * A field may be used as a return result or as a key for a
 * subquery.  Either way a selectable field should implement 
 * this.
 * 
 * @author Andy
 *
 */
public interface ISelectField {
	/**
	 * @return query parameters for this selected field.
	 */
	Struct getParams();
}
