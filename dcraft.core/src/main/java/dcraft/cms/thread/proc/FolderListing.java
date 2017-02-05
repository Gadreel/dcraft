package dcraft.cms.thread.proc;

import java.math.BigDecimal;

import org.joda.time.DateTime;

import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseTask;
import dcraft.db.IStoredProc;
import dcraft.db.TablesAdapter;
import dcraft.db.util.ByteUtil;
import dcraft.lang.op.OperationResult;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.struct.builder.ICompositeBuilder;

public class FolderListing implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		//long st = System.currentTimeMillis();
		
		//System.out.println("starting folder listing x at " + st);
		
		RecordStruct params = task.getParamsAsRecord();
		
		// TODO replicating
		// if (task.isReplicating()) 

		TablesAdapter db = new TablesAdapter(conn, task); 
		
		String did = task.getTenant();
		ICompositeBuilder out = task.getBuilder();
		String folder = params.getFieldAsString("Folder");
		BigDecimal fordate = ByteUtil.dateTimeToReverse(new DateTime());
		
		try {
			ListStruct values = params.getFieldAsList("FilterParties");
			
			out.startList();
			
			for (Struct s : values.getItems()) {
				String party = s.toString();
				
				//output data for this party
				out.startRecord();
				out.field("Party", party);
				out.field("Folder");
				out.startList();
				
				// collect data for this party
				//db.traverseIndex("dcmThread", "dcmParty", currparty.get(), when, historical, partyConsumer);
				
				// 		conn.set("dcmThreadA", did, party, folder, revmod, id, isread);

				byte[] fbdate = conn.nextPeerKey("dcmThreadA", did, party, folder, fordate);
				
				while (fbdate != null) {
					Object fdate = ByteUtil.extractValue(fbdate);

					byte[] recid = conn.nextPeerKey("dcmThreadA", did, party, folder, fdate, null);
					
					while (recid != null) {
						String id = (String) ByteUtil.extractValue(recid);
						
						out.startRecord();
						out.field("Id", id);
						out.field("Uuid", db.getStaticScalar("dcmThread", id, "dcmUuid"));
						out.field("Title", db.getStaticScalar("dcmThread", id, "dcmTitle"));
						out.field("TargetDate", db.getStaticScalar("dcmThread", id, "dcmTargetDate"));
						out.field("EndDate", db.getStaticScalar("dcmThread", id, "dcmEndDate"));
						out.field("Created", db.getStaticScalar("dcmThread", id, "dcmCreated"));
						out.field("Modified", db.getStaticScalar("dcmThread", id, "dcmModified"));
						out.field("Originator", db.getStaticScalar("dcmThread", id, "dcmOriginator"));
						out.field("Read", conn.get("dcmThreadA", did, party, folder, fdate, id)); // db.getStaticList("dcmThread", id, "dcmRead", party));
						
						// TODO split and output labels
						out.field("Labels");
						out.startList();
						out.endList();
						
						out.endRecord();
						
						recid = conn.nextPeerKey("dcmThreadA", did, party, folder, fdate, id);
					}				
					
					fbdate = conn.nextPeerKey("dcmThreadA", did, party, folder, fdate);
				}				

				out.endList();
				
				out.endRecord();						
			}
			
			out.endList();
		}
		catch (Exception x) {
			log.error("Issue with folder listing: " + x);
		}
		
		//long et = System.currentTimeMillis();
		
		//System.out.println("ending folder listing x at " + et + " took " + (et - st));
		
		task.complete();
	}
}
