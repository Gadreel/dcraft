package dcraft.cms.thread.trigger;

import java.math.BigDecimal;
import java.util.List;

import org.joda.time.DateTime;

import dcraft.db.DatabaseException;
import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseTask;
import dcraft.db.IStoredProc;
import dcraft.db.TablesAdapter;
import dcraft.db.util.ByteUtil;
import dcraft.lang.op.OperationResult;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;

public class AfterThreadUpdate implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		RecordStruct params = task.getParamsAsRecord();

		if (params.isFieldEmpty("__OldValues"))
			return;
		
		try {
			String id = params.getFieldAsString("Id");
			TablesAdapter db = new TablesAdapter(conn, task); 
			String did = task.getTenant();
			
			//System.out.println("Updating dcmThreadA for " + id);
			
			RecordStruct oldvalues = params.getFieldAsRecord("__OldValues");
			
			DateTime oldmod = oldvalues.getFieldAsDateTime("dcmModified");
			
			BigDecimal revoldmod = ByteUtil.dateTimeToReverse(oldmod);
			
			for (Struct olditem : oldvalues.getFieldAsList("Parties").getItems()) {
				RecordStruct oldparty = (RecordStruct) olditem;
				
				conn.kill("dcmThreadA", did, oldparty.getFieldAsString("dcmParty"), 
						oldparty.getFieldAsString("dcmFolder"), revoldmod, id);
			}
			
			DateTime mod = Struct.objectToDateTime(db.getStaticScalar("dcmThread", id, "dcmModified"));
			
			BigDecimal revmod = ByteUtil.dateTimeToReverse(mod);
			
			List<String> parties = db.getStaticListKeys("dcmThread", id, "dcmFolder");
			
			for (String party : parties) {
				String folder = (String) db.getStaticList("dcmThread", id, "dcmFolder", party);
				
				Boolean isread = (Boolean) db.getStaticList("dcmThread", id, "dcmRead", party);
				
				conn.set("dcmThreadA", did, party, folder, revmod, id, isread);
			}
		}
		catch (DatabaseException x) {
			log.error("Unable to update dcmThreadA: " + x);
		}
	}
}
