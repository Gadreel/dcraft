package dcraft.cms.thread.proc;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

import org.joda.time.DateTime;

import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseTask;
import dcraft.db.IStoredProc;
import dcraft.db.TablesAdapter;
import dcraft.db.util.ByteUtil;
import dcraft.lang.BigDateTime;
import dcraft.lang.op.OperationResult;
import dcraft.struct.Struct;

public class FullIndex implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		// TODO replicating
		// if (task.isReplicating()) 

		TablesAdapter db = new TablesAdapter(conn, task); 
		
		String did = task.getTenant();
		BigDateTime when = BigDateTime.nowDateTime();
		boolean historical = false;
		
		try {
			conn.kill("dcmThreadA", did);
			
			Function<Object,Boolean> partyConsumer = new Function<Object,Boolean>() {				
				@Override
				public Boolean apply(Object t) {
					try {
						String id = t.toString();						
						
						DateTime mod = Struct.objectToDateTime(db.getStaticScalar("dcmThread", id, "dcmModified"));
						
						BigDecimal revmod = ByteUtil.dateTimeToReverse(mod);
						
						List<String> parties = db.getStaticListKeys("dcmThread", id, "dcmFolder");
						
						for (String party : parties) {
							String folder = (String) db.getStaticList("dcmThread", id, "dcmFolder", party);
							
							Boolean isread = (Boolean) db.getStaticList("dcmThread", id, "dcmRead", party);
							
							conn.set("dcmThreadA", did, party, folder, revmod, id, isread);
						}
						
						/*
						 * 
						dcmFolder//Usr/00000_000000000000001/-1485186885395.0427/Data
						/Archive
						
						dcmRead//Usr/00000_000000000000001/-1485186871456.041/Data
						true
						
						 * 
  						 * 
						 */
						
						return true;
					}
					catch (Exception x) {
						log.error("Issue with folder listing: " + x);
					}
					
					return false;
				}
			};				
			
			// collect data for this party
			db.traverseRecords("dcmThread", when, historical, partyConsumer);
		}
		catch (Exception x) {
			log.error("Issue with record listing: " + x);
		}
		
		task.complete();
	}
}
