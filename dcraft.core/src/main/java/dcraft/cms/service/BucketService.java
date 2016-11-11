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
import dcraft.filestore.bucket.Bucket;
import dcraft.hub.SiteInfo;
import dcraft.lang.op.FuncCallback;
import dcraft.lang.op.OperationContext;
import dcraft.mod.ExtensionBase;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.work.TaskRun;

public class BucketService extends ExtensionBase implements IService {
	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");
		
		// =========================================================
		//  custom file store
		// =========================================================
		
		if ("Buckets".equals(feature)) {
			BucketService.handle(request, op, msg);
			return;
		}
		
		request.errorTr(441, this.serviceName(), feature, op);
		request.complete();
	}
	
	// TODO remove legacy
	// TODO refactor into cleaner service structure
	
	static public void handle(TaskRun request, String op, Message msg) {
		// in order to conserve efforts, check that we have a known operation first
		
		/*
		if (!"FileDetail".equals(op) && !"DeleteFile".equals(op) && !"DeleteFolder".equals(op) && !"AddFolder".equals(op)
				&& !"ListFiles".equals(op) && !"StartUpload".equals(op) && !"FinishUpload".equals(op) 
				&& !"StartDownload".equals(op) && !"FinishDownload".equals(op)) 
		{
			request.errorTr(441, "dcmCms", "Buckets", op);
			request.complete();	
			return;
		}
		*/
		
		RecordStruct rec = msg.getFieldAsRecord("Body");
		
		SiteInfo site = OperationContext.get().getUserContext().getSite();
		
		Bucket bucket = site.getBucket(rec.getFieldAsString("Bucket"));
		
		if (bucket == null) {
			request.error("Missing bucket.");
			return;
		}
		
		if ("AllocateUploadToken".equals(op)) {
			bucket.handleAllocateUploadToken(rec, new FuncCallback<RecordStruct>() {			
				@Override
				public void callback() {
					request.setResult(this.getResult());
					request.complete();
				}
			});
			
			return;
		}
		
		if ("FileDetail".equals(op)) {
			bucket.handleFileDetail(rec, new FuncCallback<RecordStruct>() {			
				@Override
				public void callback() {
					request.setResult(this.getResult());
					request.complete();
				}
			});
			
			return;
		}
		
		if ("DeleteFile".equals(op) || "DeleteFolder".equals(op)) {
			bucket.handleDeleteFile(rec, new FuncCallback<RecordStruct>() {			
				@Override
				public void callback() {
					request.setResult(this.getResult());
					request.complete();
				}
			});
			
			return;
		}
		
		if ("AddFolder".equals(op)) {
			bucket.handleAddFolder(rec, new FuncCallback<RecordStruct>() {			
				@Override
				public void callback() {
					request.setResult(this.getResult());
					request.complete();
				}
			});
			
			return;
		}
		
		if ("ListFiles".equals(op)) {
			bucket.handleListFiles(rec, new FuncCallback<ListStruct>() {			
				@Override
				public void callback() {
					request.setResult(this.getResult());
					request.complete();
				}
			});
			
			return;
		}
		
		if ("Custom".equals(op)) {
			bucket.handleCustom(rec, new FuncCallback<RecordStruct>() {			
				@Override
				public void callback() {
					request.setResult(this.getResult());
					request.complete();
				}
			});
			
			return;
		}
		
		if ("StartUpload".equals(op)) {
			bucket.handleStartUpload(rec, new FuncCallback<RecordStruct>() {			
				@Override
				public void callback() {
					request.setResult(this.getResult());
					request.complete();
				}
			});
			
			return;
		}
		
		if ("FinishUpload".equals(op)) {
			bucket.handleFinishUpload(rec, new FuncCallback<RecordStruct>() {			
				@Override
				public void callback() {
					request.setResult(this.getResult());
					request.complete();
				}
			});
			
			return;
		}
		
		if ("StartDownload".equals(op)) {
			bucket.handleStartDownload(rec, new FuncCallback<RecordStruct>() {			
				@Override
				public void callback() {
					request.setResult(this.getResult());
					request.complete();
				}
			});
			
			return;
		}
		
		if ("FinishDownload".equals(op)) {
			bucket.handleFinishDownload(rec, new FuncCallback<RecordStruct>() {			
				@Override
				public void callback() {
					request.setResult(this.getResult());
					request.complete();
				}
			});
			
			return;
		}
		
		request.errorTr(441, "dcmCms", "Buckets", op);
		request.complete();	
	}
}
