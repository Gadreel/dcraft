package dcraft.cms.thread.proc;

import org.joda.time.DateTime;

import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseTask;
import dcraft.db.IStoredProc;
import dcraft.db.update.DbRecordRequest;
import dcraft.db.update.UpdateRecordRequest;
import dcraft.lang.op.OperationResult;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.util.HashUtil;
import dcraft.util.TimeUtil;

/*
 * {
 * 	  Id: nnn,
 *    Content: [
		 {
		 	Content: nnn
			ContentType: nnn
			ContentOriginator: nnn
			Source: [uid]
			Attributes: nnn
			Stamp: nnn
     	 }
 *    ]
 * }
 */
public class AddContentAction implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		RecordStruct params = task.getParamsAsRecord();
		
		//System.out.println("NT 1 Got: " + params.toPrettyString());
		
		// TODO replicating
		// if (task.isReplicating()) 
		
		DateTime now = new DateTime();
		
		DbRecordRequest req = new UpdateRecordRequest()
			.withTable("dcmThread")
			.withId(params.getFieldAsString("Id"))
			.withUpdateField("dcmModified", now);

		
		ListStruct clist = params.getFieldAsList("Content");

		for (Struct cs : clist.getItems()) {
			RecordStruct cnt = Struct.objectToRecord(cs);
			
			String content = cnt.getFieldAsString("Content");
			
			String stamp = TimeUtil.stampFmt.print(cnt.hasField("Stamp") 
					? cnt.getFieldAsDateTime("Stamp") : new DateTime());
			
			req
				.withUpdateField("dcmContent", stamp, content)
				.withUpdateField("dcmContentHash", stamp, HashUtil.getSha256(content))
				.withUpdateField("dcmContentType", stamp, cnt.getFieldAsString("ContentType"));
		
			if (!cnt.isFieldEmpty("Source"))
				req.withUpdateField("dcmSource", stamp, cnt.getFieldAsString("Source"));
			
			if (!cnt.isFieldEmpty("Attributes"))
				req.withUpdateField("dcmAttributes", stamp, cnt.getFieldAsRecord("Attributes"));
		}
		
		task.getDbm().submit(req, task.getResult());
	}

}
