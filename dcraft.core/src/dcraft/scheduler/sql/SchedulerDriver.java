/* ************************************************************************
#
#  designCraft.io
#
#  http://designcraft.io/
#
#  Copyright:
#    Copyright 2014 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */
package dcraft.scheduler.sql;

import dcraft.hub.Hub;
import dcraft.lang.op.FuncResult;
import dcraft.lang.op.OperationResult;
import dcraft.scheduler.ISchedulerDriver;
import dcraft.scheduler.ScheduleEntry;
import dcraft.scheduler.ScheduleEntry.ScheduleArea;
import dcraft.sql.SqlSelect;
import dcraft.sql.SqlSelectString;
import dcraft.sql.SqlManager.SqlDatabase;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.util.StringUtil;
import dcraft.work.Task;
import dcraft.xml.XElement;

public class SchedulerDriver implements ISchedulerDriver {
	@Override
	public void init(OperationResult or, XElement config) {
	}

	@Override
	public void start(OperationResult or) {
		or.infoTr(225);
	}

	@Override
	public void stop(OperationResult or) {
		or.infoTr(226);
	}

	@Override
	public FuncResult<ListStruct> loadSchedule() {
		SqlDatabase db = Hub.instance.getSQLDatabase();
		
		if (db == null) {
			FuncResult<ListStruct> res = new FuncResult<ListStruct>();
			res.errorTr(156);
			return res;
		}
		
		return db.executeQuery(
				new SqlSelect[] { 
						new SqlSelectString("Id"), 
						new SqlSelectString("dcTitle", "Title", null), 
						new SqlSelectString("dcSchedule", "Schedule", null)
				}, 
				"dcSchedule",					// from 
				"Active = 1", 					// where
				null, 							// group by
				null			 				// order by
		);	
	}
	
	@Override
	public FuncResult<ScheduleEntry> loadEntry(String id) {
		FuncResult<ScheduleEntry> res = new FuncResult<ScheduleEntry>();
		
		SqlDatabase db = Hub.instance.getSQLDatabase();
		
		if (db == null) {
			res.errorTr(156);
			return res;
		}
		
		FuncResult<RecordStruct> rsres = db.executeQueryRecord(
				new SqlSelect[] { 
						new SqlSelectString("Id"),
						new SqlSelectString("dcTitle"),
						new SqlSelectString("dcKind"),
						new SqlSelectString("dcProvider"),
						new SqlSelectString("dcTask"),
						new SqlSelectString("dcParams")
				},
				"dcSchedule",				// from 
				"Id = ? AND Active = 1",	// where
				StringUtil.parseInt(id, 0)	// param 1 - dcSchedule record id
		);	
		
		if (rsres.hasErrors())
			return res;
			
		if (rsres.isEmptyResult()) {
			res.errorTr(166, id);
			return res;
		}
		
		RecordStruct rec = rsres.getResult();
		
		ScheduleEntry entry = new ScheduleEntry();
		
		entry.setScheduleId(rec.getFieldAsString("Id"));
		entry.setArea(ScheduleArea.valueOf(rec.getFieldAsString("dcKind")));
		entry.setTitle(rec.getFieldAsString("dcTitle"));
		entry.setProvider(rec.getFieldAsString("dcProvider"));
		
		if (!rec.isFieldEmpty("dcTask")) 
			entry.setTask(Task.taskFromRecord(rec.getFieldAsRecord("dcTask")));
		
		entry.setParams(rec.getFieldAsRecord("dcParams"));		
		
		res.setResult(entry);
		
		return res;
	}
}
