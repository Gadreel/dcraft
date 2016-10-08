package dcraft.service.db;

import dcraft.bus.IService;
import dcraft.bus.Message;
import dcraft.db.DataRequest;
import dcraft.db.IDatabaseManager;
import dcraft.db.ObjectFinalResult;
import dcraft.hub.Hub;
import dcraft.log.Logger;
import dcraft.mod.ExtensionBase;
import dcraft.work.TaskRun;

public class TenantService extends ExtensionBase implements IService {

	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");

		IDatabaseManager db = Hub.instance.getDatabase();
		
		if (db == null) {
			request.errorTr(443);
			request.complete();
			return;
		}
		
		if (Logger.isDebug())
			Logger.debug("Tenants called with: " + feature + " op: " + op);
		
		if ("Manager".equals(feature)) {
			if ("LoadAll".equals(op)) {
				DataRequest req = new DataRequest("dcLoadTenants")
					.withRootTenant();	// use root for this request
				
				db.submit(req, new ObjectFinalResult(request));
				return;
			}			
			
			if ("Load".equals(op)) {
				DataRequest req = new DataRequest("dcLoadTenant")
					.withParams(msg.getFieldAsRecord("Body"))
					.withRootTenant();	// use root for this request
				
				db.submit(req, new ObjectFinalResult(request));
				return;
			}			
		}
		
		request.errorTr(441, this.serviceName(), feature, op);
		request.complete();
	}
}
