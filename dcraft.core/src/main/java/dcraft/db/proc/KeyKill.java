package dcraft.db.proc;

import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseTask;
import dcraft.db.IStoredProc;
import dcraft.db.util.ByteUtil;
import dcraft.lang.op.OperationResult;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.util.HexUtil;

public class KeyKill implements IStoredProc {
	@Override
	public void execute(DatabaseInterface adapter, DatabaseTask task, OperationResult or) {
		RecordStruct params = task.getParamsAsRecord();

		ListStruct keys = params.getFieldAsList("Keys");
		
		byte[] basekey = null;
		
		for (Struct ss : keys.getItems()) 
			basekey =  ByteUtil.combineKeys(basekey, HexUtil.decodeHex(ss.toString())); 

		adapter.kill(basekey);
		
		task.complete();
	}
}
