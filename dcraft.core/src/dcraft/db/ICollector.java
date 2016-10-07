package dcraft.db;

import java.util.function.Function;

import dcraft.lang.op.OperationResult;
import dcraft.struct.RecordStruct;

public interface ICollector {
	void collect(DatabaseInterface conn, DatabaseTask task, OperationResult log, 
			RecordStruct collector, Function<Object,Boolean> uniqueConsumer);
}
