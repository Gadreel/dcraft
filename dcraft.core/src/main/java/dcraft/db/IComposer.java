package dcraft.db;

import dcraft.lang.BigDateTime;
import dcraft.lang.op.OperationResult;
import dcraft.struct.RecordStruct;
import dcraft.struct.builder.ICompositeBuilder;

public interface IComposer {
	void writeField(DatabaseInterface conn, DatabaseTask task, OperationResult log, ICompositeBuilder out,
			TablesAdapter db, String table, String id, BigDateTime when, RecordStruct field, 
			boolean historical, boolean compact);
}
