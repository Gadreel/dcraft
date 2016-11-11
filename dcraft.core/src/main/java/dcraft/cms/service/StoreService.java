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
package dcraft.cms.service;

import dcraft.bus.IService;
import dcraft.bus.Message;
import dcraft.mod.ExtensionBase;
import dcraft.work.TaskRun;

// TODO refactor into better service structure
public class StoreService extends ExtensionBase implements IService {	
	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");
		
		// =========================================================
		//  store categories
		// =========================================================
		
		if ("Category".equals(feature)) {
			Products.handleCategories(request, op, msg);
			return;
		}
		
		// =========================================================
		//  store products
		// =========================================================
		
		if ("Product".equals(feature)) {
			Products.handleProducts(request, op, msg);
			return;
		}
		
		// =========================================================
		//  store coupons
		// =========================================================
		
		if ("Coupons".equals(feature)) {
			Products.handleCoupons(request, op, msg);
			return;
		}
		
		request.errorTr(441, this.serviceName(), feature, op);
		request.complete();
	}
}
