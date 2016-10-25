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
package dcraft.tasks.sql;

import dcraft.bus.IService;
import dcraft.bus.Message;
import dcraft.hub.Hub;
import dcraft.lang.op.FuncResult;
import dcraft.mod.ExtensionBase;
import dcraft.sql.SqlSelect;
import dcraft.sql.SqlSelectString;
import dcraft.sql.SqlSelectStringList;
import dcraft.sql.SqlManager.SqlDatabase;
import dcraft.struct.ListStruct;
import dcraft.work.TaskRun;

public class TenantsService extends ExtensionBase implements IService {

	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");
		
		if ("Manager".equals(feature)) {
			if ("LoadAll".equals(op)) {
				SqlDatabase db = Hub.instance.getSQLDatabase();
				
				if (db == null) {
					request.errorTr(443);
					request.complete();
					return;
		        }

				// TODO GROUP_CONCAT only works with MariaDB/MySQL -- should work with H2 -- fix for others
        		String nsql = "(SELECT GROUP_CONCAT(DISTINCT dcName) FROM dcTenantNames dn WHERE dn.dcTenantId = d.Id)";
				
				FuncResult<ListStruct> rsres = db.executeQuery(
						new SqlSelect[] { 
								new SqlSelectString("Id"), 
								new SqlSelectString("dcTitle", "Title", null), 
								new SqlSelectString("dcObscureClass", "ObscureClass", null), 
								new SqlSelectString("dcObscureSeed", "ObscureSeed", null), 
								new SqlSelectStringList(nsql, "Names", null) 
						},
						"dcTenant d",  
						"Active = 1", 
						null, 
						"Id"
				);
				
				ListStruct rs = rsres.getResult();
				
				request.setResult(rs);
				request.complete();
				return;
			}			
		}
		
		request.errorTr(441, this.serviceName(), feature, op);
		request.complete();
	}
}
