package dcraft.cms.feed.proc;

import org.joda.time.DateTime;

import dcraft.db.DatabaseException;
import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseTask;
import dcraft.db.IStoredProc;
import dcraft.db.ObjectResult;
import dcraft.db.TablesAdapter;
import dcraft.db.query.LoadRecordRequest;
import dcraft.db.query.SelectFields;
import dcraft.db.update.RetireRecordRequest;
import dcraft.filestore.CommonPath;
import dcraft.lang.BigDateTime;
import dcraft.lang.op.OperationResult;
import dcraft.struct.CompositeStruct;
import dcraft.struct.RecordStruct;

public class DeleteFeed implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		RecordStruct params = task.getParamsAsRecord();
		
		String path = params.getFieldAsString("Path");
		
		// TODO replicating
		// if (task.isReplicating())
		
		CommonPath opath  = CommonPath.from(path);
		CommonPath ochan = opath.subpath(0, 2);		// site and channel
		
		TablesAdapter db = new TablesAdapter(conn, task); 
		
		BigDateTime when = BigDateTime.nowDateTime();
		Object oid = db.firstInIndex("dcmFeed", "dcmPath", path, when, false);
		
		if (oid != null) {
			LoadRecordRequest lr1 = new LoadRecordRequest()
					.withTable("dcmFeed")
					.withId(oid.toString())
					.withSelect(new SelectFields()
						.withField("Id")
						.withField("dcmPath", "Path")
						.withField("dcmPublished", "Published")
					);
				
				task.getDbm().submit(lr1, new ObjectResult() {
					@Override
					public void process(CompositeStruct result) {
						if ((oid != null) && (result == null)) {
							log.error("Unable to update feed - id found but no record loaded");
							task.complete();
							return;
						}
						
						log.touch();
						
						// delete from dcmFeedIndex too
						DateTime opubtime = ((RecordStruct) result).getFieldAsDateTime("Published");
					
						try {
							conn.kill("dcmFeedIndex", task.getTenant(), ochan, conn.inverseTime(opubtime), oid);
						} 
						catch (DatabaseException x) {
							// TODO 
						}
						
						RetireRecordRequest lr1 = new RetireRecordRequest("dcmFeed", oid.toString());
						
						task.getDbm().submit(lr1, task.getResult());
					}
				});
		}
		else {
			task.complete();
		}
	}
}
