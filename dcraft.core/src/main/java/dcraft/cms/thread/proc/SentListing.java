package dcraft.cms.thread.proc;

import java.util.function.Function;

import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseTask;
import dcraft.db.IStoredProc;
import dcraft.db.TablesAdapter;
import dcraft.lang.BigDateTime;
import dcraft.lang.op.OperationResult;
import dcraft.struct.RecordStruct;
import dcraft.struct.builder.ICompositeBuilder;

// TODO look in /Sent folder instead
public class SentListing implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		RecordStruct params = task.getParamsAsRecord();
		
		// TODO replicating
		// if (task.isReplicating()) 

		TablesAdapter db = new TablesAdapter(conn, task); 
		
		/* TODO use dcmThreadA or dcmThreadB 	 */
		
		BigDateTime when = BigDateTime.nowDateTime();
		boolean historical = false;
		ICompositeBuilder out = task.getBuilder();
		String origin = params.getFieldAsString("Originator");
		
		try {
			Function<Object,Boolean> partyConsumer = new Function<Object,Boolean>() {				
				@Override
				public Boolean apply(Object t) {
					try {
						String id = t.toString();						
						
						out.startRecord();
						out.field("Id", id);
						out.field("Uuid", db.getStaticScalar("dcmThread", id, "dcmUuid"));
						out.field("Title", db.getStaticScalar("dcmThread", id, "dcmTitle"));
						out.field("TargetDate", db.getStaticScalar("dcmThread", id, "dcmTargetDate"));
						out.field("EndDate", db.getStaticScalar("dcmThread", id, "dcmEndDate"));
						out.field("Created", db.getStaticScalar("dcmThread", id, "dcmCreated"));
						out.field("Modified", db.getStaticScalar("dcmThread", id, "dcmModified"));
						out.field("Originator", db.getStaticScalar("dcmThread", id, "dcmOriginator"));
						
						// TODO split and output labels
						out.field("Labels");
						out.startList();
						out.endList();
						
						out.endRecord();
						
						return true;
					}
					catch (Exception x) {
						log.error("Issue with folder listing: " + x);
					}
					
					return false;
				}
			};				
			
			out.startList();
			
			// collect data for this party
			db.traverseIndex("dcmThread", "dcmOriginator", origin, when, historical, partyConsumer);
			
			out.endList();
		}
		catch (Exception x) {
			log.error("Issue with folder listing: " + x);
		}
		
		task.complete();
	}
}
