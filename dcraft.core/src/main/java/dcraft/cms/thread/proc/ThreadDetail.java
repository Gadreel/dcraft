package dcraft.cms.thread.proc;

import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseTask;
import dcraft.db.IStoredProc;
import dcraft.db.TablesAdapter;
import dcraft.lang.op.OperationResult;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.struct.builder.ICompositeBuilder;

public class ThreadDetail implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		RecordStruct params = task.getParamsAsRecord();
		ICompositeBuilder out = task.getBuilder();
		
		// TODO replicating
		// if (task.isReplicating()) 
		
		TablesAdapter db = new TablesAdapter(conn, task); 
		
		String id = UpdateThreadCore.getThreadId(db, params);
		
		String party = params.getFieldAsString("Party");
		
		try {
			out.startRecord();
			out.field("Id", id);
			out.field("Uuid", db.getStaticScalar("dcmThread", id, "dcmUuid"));
			out.field("Title", db.getStaticScalar("dcmThread", id, "dcmTitle"));
			out.field("TargetDate", db.getStaticScalar("dcmThread", id, "dcmTargetDate"));
			out.field("EndDate", db.getStaticScalar("dcmThread", id, "dcmEndDate"));
			out.field("Created", db.getStaticScalar("dcmThread", id, "dcmCreated"));
			out.field("Modified", db.getStaticScalar("dcmThread", id, "dcmModified"));
			
			String oid = Struct.objectToString(db.getStaticScalar("dcmThread", id, "dcmOriginator"));
			
			out.field("Originator", oid);
			
			if (params.isFieldEmpty("DisplayNameField"))
				out.field("OriginatorName", db.getStaticScalar("dcUser", oid, "dcFirstName") + " " + db.getStaticScalar("dcUser", oid, "dcLastName"));
			else
				out.field("OriginatorName", db.getStaticScalar("dcUser", oid, params.getFieldAsString("DisplayNameField")));
			
			out.field("Read", db.getStaticList("dcmThread", id, "dcmRead", party));
			out.field("Folder", db.getStaticList("dcmThread", id, "dcmFolder", party));
			
			out.field("Parties");
			out.startList();
			
			for (String pvalue : db.getStaticListKeys("dcmThread", id, "dcmParty")) {
				out.startRecord();
				out.field("Party", pvalue);
				
				if (pvalue.startsWith("/Usr/")) {
					String pid = pvalue.substring(5);
					
					if (params.isFieldEmpty("DisplayNameField"))
						out.field("Name", db.getStaticScalar("dcUser", pid, "dcFirstName") + " " + db.getStaticScalar("dcUser", pid, "dcLastName"));
					else
						out.field("Name", db.getStaticScalar("dcUser", pid, params.getFieldAsString("DisplayNameField")));
				}
				else {
					out.field("Name", pvalue);
				}
				
				//out.field("Name", ThreadDetail.partyValueToPartyName(pvalue, params.getFieldAsString("DisplayNameField")));
				out.endRecord();
			}
			
			out.endList();
			
			// TODO split and output labels
			out.field("Labels");
			out.startList();
			out.endList();
			
			out.field("Content");
			out.startList();
			
			for (String stamp : db.getStaticListKeys("dcmThread", id, "dcmContent")) {
				out.startRecord();
				out.field("Content", db.getStaticList("dcmThread", id, "dcmContent", stamp));
				out.field("ContentType", db.getStaticList("dcmThread", id, "dcmContentType", stamp));
				out.field("ContentOriginator", db.getStaticList("dcmThread", id, "dcmContentOriginator", stamp));
				out.field("Attributes", db.getStaticList("dcmThread", id, "dcmAttributes", stamp));
				out.endRecord();
			}
			
			out.endList();
			
			out.endRecord();
		}
		catch (Exception x) {
			log.error("Issue with thread detail: " + x);
		}
		
		task.complete();
	}
	
	/*
	static public String partyValueToPartyName(String name, String displayname) {
		// TODO 
		// /Usr/00200_000000000000001
		if (name.startsWith("/Usr")) {
			if (StringUtil.isEmpty(displayname))
				name = db.getStaticScalar("dcUser", oid, "dcFirstName") + " " + db.getStaticScalar("dcUser", oid, "dcLastName"));
			else
				out.field("OriginatorName", db.getStaticScalar("dcUser", oid, params.getFieldAsString("DisplayNameField")));
		}
		
		return name;
	}
	*/
}
