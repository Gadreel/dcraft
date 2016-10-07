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
import dcraft.util.HashUtil;
import dcraft.util.StringUtil;
import dcraft.util.TimeUtil;

public class ReplyThread implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		RecordStruct params = task.getParamsAsRecord();

		DateTime now = new DateTime();
		
		DbRecordRequest req = new UpdateRecordRequest()
			.withTable("dcmThread")
			.withId(params.getFieldAsString("Id"))
			.withUpdateField("dcmModified", now);
			
		ListStruct lbs = params.getFieldAsList("Labels");
		
		if ((lbs != null) && !lbs.isEmpty())
			req.withUpdateField("dcmLabels", "|" + StringUtil.join(lbs.toStringList(), "|") + "|");
			
		ListStruct parties = params.getFieldAsList("Parties");
		
		if (!parties.isEmpty()) {
			for (int i = 0; i < parties.getSize(); i++) {
				RecordStruct party = parties.getItemAsRecord(i);
				
				String pident = party.getFieldAsString("Party");
				
				req
					.withUpdateField("dcmParty", pident, pident)
					.withUpdateField("dcmRead", pident, false)
					.withUpdateField("dcmFolder", pident, party.getFieldAsString("Folder"));
				
				lbs = party.getFieldAsList("PartyLabels");
				
				if ((lbs != null) && !lbs.isEmpty())
					req.withUpdateField("dcmPartyLabels", pident, "|" + StringUtil.join(lbs.toStringList(), "|") + "|");
			}
		}
		
		// it is possible to start a thread without content
		if (params.hasField("Content")) {
			RecordStruct cnt = params.getFieldAsRecord("Content");
			String content = cnt.getFieldAsString("Content");
			String stamp = TimeUtil.stampFmt.print(new DateTime());
			
			req
				.withUpdateField("dcmContent", stamp, content)
				.withUpdateField("dcmContentHash", stamp, HashUtil.getSha256(content))
				.withUpdateField("dcmContentType", stamp, cnt.getFieldAsString("ContentType"))
				.withUpdateField("dcmContentOriginator", stamp, cnt.hasField("ContentOriginator") 
						? cnt.getFieldAsString("ContentOriginator") 
						: log.getContext().getUserContext().getUserId());
		
			if (!cnt.isFieldEmpty("Source"))
				req.withUpdateField("dcmSource", stamp, cnt.getFieldAsString("Source"));
			
			if (!cnt.isFieldEmpty("Attributes"))
				req.withUpdateField("dcmAttributes", stamp, cnt.getFieldAsRecord("Attributes"));
		}
		
		task.getDbm().submit(req, task.getResult());
	}
}
