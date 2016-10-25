package dcraft.db.query;

import dcraft.struct.Struct;

/**
 * A field may be used as a rule for to select results.
 * 
 * @author Andy
 *
 */
public interface IWhereField {
	/**
	 * @return query parameters for this field.
	 */
	Struct getParams();
}
