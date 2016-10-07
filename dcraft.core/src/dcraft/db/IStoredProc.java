package dcraft.db;

import dcraft.lang.op.OperationResult;

public interface IStoredProc {
	void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log);
}
