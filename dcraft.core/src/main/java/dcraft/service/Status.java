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
package dcraft.service;

import dcraft.bus.IService;
import dcraft.bus.Message;
import dcraft.hub.Hub;
import dcraft.lang.op.FuncCallback;
import dcraft.lang.op.FuncResult;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.UserContext;
import dcraft.log.Logger;
import dcraft.mod.ExtensionBase;
import dcraft.session.Session;
import dcraft.struct.FieldStruct;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.tool.Updater;
import dcraft.work.TaskRun;

public class Status extends ExtensionBase implements IService {
	protected String version = null;
	protected String app = null;
	
	@Override
	public void start() {
		super.start();
		
		OperationContext.useNewRoot();
		
		FuncResult<RecordStruct> ldres = Updater.loadDeployed();
		
		if (ldres.hasErrors()) {
			OperationContext.get().error("Error reading deployed.json file: " + ldres.getMessage());
			return;
		}
		
		RecordStruct deployed = ldres.getResult();
		
		this.version = deployed.getFieldAsString("Version");
		this.app = deployed.getFieldAsString("PackagePrefix");
	}

	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");

		if ("Echo".equals(feature)) {
			if ("Test".equals(op)) {
				
				/*
				try {
					Thread.sleep(5000);
				} 
				catch (InterruptedException x) {
				}
				*/
				
				request.debug("Echo got: " + msg.getField("Body"));
				
				request.setResult(msg.getField("Body"));
				request.complete();
				return;
			}
		}
		else if ("Tickle".equals(feature)) {
			if ("Test".equals(op)) {
				System.out.println("got message: " + msg);
				request.complete();
				return;
			}			
		}
		else if ("Translate".equals(feature)) {
			/* TODO
			RecordStruct rec = msg.getFieldAsRecord("Body");
			
			String original = rec.getFieldAsString("Content");
			String from = rec.getFieldAsString("From");
			String to = rec.getFieldAsString("To");
			
			if ("Text".equals(op)) {
				// TODO support From and To - for now it is en to x-pig-latin
				return MessageUtil.successAlt(TranslateLang.translateText(original, from, to));
			}
			else if ("Xml".equals(op)) {
				// TODO support From and To - for now it is en to x-pig-latin
				return MessageUtil.successAlt(TranslateLang.translateXml(original, from, to));
			}
			*/
		}
		else if ("Info".equals(feature)) {
			if ("Test".equals(op)) {
				OperationContext tc = OperationContext.get();
				
				tc.verify(new FuncCallback<UserContext>() {					
					@Override
					public void callback() {
						UserContext uc = this.getResult();
						
						if (uc != null) {
							OperationContext.use(uc, OperationContext.get().toBuilder());
							
							if (!uc.isVerified())
								request.error(1, "Invalid auth token.");
							else
								request.setResult(new RecordStruct(new FieldStruct("UserId", uc.getUserId())));
						}
						else
							request.error(1, "User context.");
						
						request.complete();
					}
				});
				
				return;
			}			
			else if ("HubStatus".equals(op)) {
				RecordStruct rec = new RecordStruct();

				Logger.info("Status check");
				
				rec.setField("ServerIdled", Hub.instance.isIdled());
				
				rec.setField("WorkPool", Hub.instance.getWorkPool().toStatusReport());
				
				ListStruct sessions = new ListStruct();
				
				for (Session sess : Hub.instance.getSessions().list()) 
					sessions.addItem(sess.toStatusReport());
				
				rec.setField("Sessions", sessions);
				
				rec.setField("WorkQueue", Hub.instance.getWorkQueue().list());
				
				request.setResult(rec);
				request.complete();
				return;
			}			
			else if ("TaskStatus".equals(op)) {
				ListStruct requests = msg.getFieldAsList("Body");
				ListStruct results = new ListStruct();
				
				for (Struct struct : requests.getItems()) {
					RecordStruct req = (RecordStruct) struct;
					
					// try to get the info locally
					RecordStruct trec = Hub.instance.getWorkPool().status(
							req.getFieldAsString("TaskId"), 
							req.getFieldAsString("WorkId")
					);
					
					if ((trec != null) && trec.isEmpty())
						System.out.println("empty from pool");
					
					// else look for it in database
					if (trec == null)
						trec = Hub.instance.getWorkQueue().status(
								req.getFieldAsString("TaskId"), 
								req.getFieldAsString("WorkId")
						);
					
					if ((trec != null) && trec.isEmpty())
						System.out.println("empty from queue");
					
					if (trec != null)
						results.addItem(trec);
				}
				
				request.setResult(results);
				request.complete();
				return;
			}			
			else if ("Version".equals(op)) { 
				request.setResult(new RecordStruct(
						new FieldStruct("Version", this.version), 
						new FieldStruct("App", this.app)
				));
				request.complete();
				return;
			}
		}
		else if ("Management".equals(feature)) {
			if ("Idle".equals(op)) {
				RecordStruct rec = msg.getFieldAsRecord("Body");
				boolean idle = rec.getFieldAsBoolean("Idle");
				Hub.instance.setIdled(idle);
				
				request.complete();
				return;
			}			
		}
		
		request.error("Feature or operation not supported");
		request.complete();
	}
}
