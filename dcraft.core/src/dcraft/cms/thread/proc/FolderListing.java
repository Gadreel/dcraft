package dcraft.cms.thread.proc;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseTask;
import dcraft.db.IStoredProc;
import dcraft.db.TablesAdapter;
import dcraft.lang.BigDateTime;
import dcraft.lang.op.OperationResult;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.struct.builder.ICompositeBuilder;

public class FolderListing implements IStoredProc {
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
		AtomicReference<String> currparty = new AtomicReference<>();
		String folder = params.getFieldAsString("Folder");
		
		try {
			Function<Object,Boolean> partyConsumer = new Function<Object,Boolean>() {				
				@Override
				public Boolean apply(Object t) {
					try {
						String id = t.toString();						
						String party = currparty.get();

						// TODO filter labels too
						
						String foldr = (String) db.getStaticList("dcmThread", id, "dcmFolder", party);
						
						if (!folder.equals(foldr))
							return false;
						
						out.startRecord();
						out.field("Id", id);
						out.field("Uuid", db.getStaticScalar("dcmThread", id, "dcmUuid"));
						out.field("Title", db.getStaticScalar("dcmThread", id, "dcmTitle"));
						out.field("TargetDate", db.getStaticScalar("dcmThread", id, "dcmTargetDate"));
						out.field("EndDate", db.getStaticScalar("dcmThread", id, "dcmEndDate"));
						out.field("Created", db.getStaticScalar("dcmThread", id, "dcmCreated"));
						out.field("Modified", db.getStaticScalar("dcmThread", id, "dcmModified"));
						out.field("Originator", db.getStaticScalar("dcmThread", id, "dcmOriginator"));
						out.field("Read", db.getStaticList("dcmThread", id, "dcmRead", party));
						
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
			
			ListStruct values = params.getFieldAsList("FilterParties");
			
			out.startList();
			
			for (Struct s : values.getItems()) {
				currparty.set(s.toString());
				
				//output data for this party
				out.startRecord();
				out.field("Party", currparty.get());
				out.field("Folder");
				out.startList();
				
				// collect data for this party
				db.traverseIndex("dcmThread", "dcmParty", currparty.get(), when, historical, partyConsumer);

				out.endList();
				
				out.endRecord();						
			}
			
			out.endList();
		}
		catch (Exception x) {
			log.error("Issue with folder listing: " + x);
		}
		
		task.complete();
	}
}
