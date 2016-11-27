package dcraft.cms.feed.proc;

import java.util.concurrent.atomic.AtomicReference;

import org.joda.time.DateTime;

import dcraft.db.DatabaseInterface;
import dcraft.db.DatabaseResult;
import dcraft.db.DatabaseTask;
import dcraft.db.IStoredProc;
import dcraft.db.ObjectResult;
import dcraft.db.TablesAdapter;
import dcraft.db.query.LoadRecordRequest;
import dcraft.db.query.SelectFields;
import dcraft.db.update.DbRecordRequest;
import dcraft.db.update.InsertRecordRequest;
import dcraft.db.update.UpdateRecordRequest;
import dcraft.filestore.CommonPath;
import dcraft.lang.BigDateTime;
import dcraft.lang.op.OperationResult;
import dcraft.struct.CompositeStruct;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.builder.ICompositeBuilder;
import dcraft.util.StringUtil;
import dcraft.util.TimeUtil;

// database does not separate preview from published - fields and tags in general 
// are always published not preview
// however, when no published content is available, we fall back on the preview
public class UpdateFeed implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		RecordStruct params = task.getParamsAsRecord();
		
		String path = "/" + params.getFieldAsString("Site") + "/" +
				params.getFieldAsString("Channel") + params.getFieldAsString("Path");
		
		ListStruct atags = params.getFieldAsList("AuthorizationTags");
		ListStruct tempctags = params.getFieldAsList("ContentTags");
		
		if (tempctags == null)
			tempctags = params.getFieldAsList("PreviewContentTags");
		
		ListStruct tempfields = params.getFieldAsList("Fields");
		
		if (tempfields == null)
			tempfields = params.getFieldAsList("PreviewFields");

		ListStruct ctags = tempctags;
		ListStruct fields = tempfields;
		
		CommonPath cp = CommonPath.from(path);
		
		CommonPath nchan = cp.subpath(0, 2);		// site and channel
		
		// TODO replicating
		// if (task.isReplicating())
		
		TablesAdapter db = new TablesAdapter(conn, task); 
		
		BigDateTime when = BigDateTime.nowDateTime();
		Object oid = db.firstInIndex("dcmFeed", "dcmPath", path, when, false);
		
		AtomicReference<RecordStruct> oldvalues = new AtomicReference<>();
		AtomicReference<DateTime> updatepub = new AtomicReference<>();
		
		DatabaseResult fromUpdate = new ObjectResult() {
			@Override
			public void process(CompositeStruct result) {
				/*
				 * Update index
				 * 
				 * ^dcmFeedIndex(did, site/channel, publish datetime, id)=[content tags]
				 * 
				 */
				
				log.touch();
				
				String recid = null;
				
				if (oid != null) 
					recid = oid.toString();
				else if (result != null)
					recid = ((RecordStruct) result).getFieldAsString("Id");
				
				if (StringUtil.isEmpty(recid)) {
					log.error("Unable to update feed index - no id available");
					task.complete();
					return;
				}

				String did = task.getTenant();
				
				CommonPath ochan = null;
				DateTime opubtime = null;
				String otags = "|";
				
				if (oldvalues.get() != null) {
					CommonPath opath  = CommonPath.from(oldvalues.get().getFieldAsString("Path"));
					ochan = opath.subpath(0, 2);		// site and channel
					
					opubtime = oldvalues.get().getFieldAsDateTime("Published");
					
					ListStruct otlist = oldvalues.get().getFieldAsList("ContentTags");
					
					if (ctags != null) 
						otags = "|" + StringUtil.join(otlist.toStringList(), "|") + "|";
				}
				
				DateTime npubtime = updatepub.get();
				String ntags = "|";
				
				if (npubtime == null)
					npubtime = opubtime;
				
				if (ctags != null) {
					ntags = "|" + StringUtil.join(ctags.toStringList(), "|") + "|";
				}
				else {
					ntags = otags; 
				}
				
				try {
					// only kill if needed
					if ((opubtime != null) && (! opubtime.equals(npubtime) || ! ochan.equals(nchan)))
						conn.kill("dcmFeedIndex", did, ochan.toString(), conn.inverseTime(opubtime), recid);
					
					if (npubtime != null) 
						conn.set("dcmFeedIndex", did, nchan.toString(), conn.inverseTime(npubtime), recid, ntags);
				}
				catch (Exception x) {
					log.error("Error updating feed index: " + x);
				}

				try {
					ICompositeBuilder out = task.getBuilder();
					
					out.startRecord();
					out.field("Id", recid);
					out.endRecord();
				}
				catch (Exception x) {
					log.error("Error writing record id: " + x);
				}
				
				task.complete();
			}
		};
		
		DatabaseResult fromLoad = new ObjectResult() {
			@Override
			public void process(CompositeStruct result) {
				if ((oid != null) && (result == null)) {
					log.error("Unable to update feed - id found but no record loaded");
					task.complete();
					return;
				}
				
				log.touch();
				
				oldvalues.set((RecordStruct) result);
				
				if ((oid == null) && StringUtil.isEmpty(path)) {
					log.error("Unable to insert feed - missing Path");
					task.complete();
					return;
				}
				
				DbRecordRequest req = (oid == null) ? new InsertRecordRequest() : new UpdateRecordRequest().withId(oid.toString());
				
				req.withTable("dcmFeed");
				
				if (path != null)
					req.withUpdateField("dcmPath", path);
				
				if (atags != null)
					req.withSetList("dcmAuthorizationTags", atags);
				
				if (ctags != null)
					req.withSetList("dcmContentTags", ctags);
				
				if (fields != null) {
					for (int i = 0; i < fields.getSize(); i++) {
						RecordStruct entry = fields.getItemAsRecord(i);
						String key = entry.getFieldAsString("Name") + "." + entry.getFieldAsString("Locale");
						req.withUpdateField("dcmFields", key, entry.getFieldAsString("Value"));
						
						if ("Published".equals(entry.getFieldAsString("Name"))) {
							DateTime pd = TimeUtil.parseDateTime(entry.getFieldAsString("Value")).withMillisOfSecond(0).withSecondOfMinute(0);
							updatepub.set(pd);							
							req.withUpdateField("dcmPublished", pd);
						}
						
						if ("AuthorUsername".equals(entry.getFieldAsString("Name"))) {
							Object userid = db.firstInIndex("dcUser", "dcUsername", entry.getFieldAsString("Value"), when, false);
							
							if (userid != null) {
								String uid = userid.toString();
								req.withUpdateField("dcmAuthor", uid, uid);
							}
						}
					}
				}
					
				task.getDbm().submit(req, fromUpdate);
			}
		};
		
		if (oid != null) {
			LoadRecordRequest lr1 = new LoadRecordRequest()
				.withTable("dcmFeed")
				.withId(oid.toString())
				.withSelect(new SelectFields()
					.withField("Id")
					.withField("dcmPath", "Path")
					.withField("dcmPublished", "Published")
					.withField("dcmContentTags", "ContentTags")
				);
			
			task.getDbm().submit(lr1, fromLoad);
		}
		else {
			fromLoad.complete();
		}
	}
}
