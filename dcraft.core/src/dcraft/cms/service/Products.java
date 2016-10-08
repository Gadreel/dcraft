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

import java.io.IOException;
import java.nio.file.Files;

import dcraft.bus.Message;
import dcraft.db.IDatabaseManager;
import dcraft.db.ObjectFinalResult;
import dcraft.db.ObjectResult;
import dcraft.db.query.LoadRecordRequest;
import dcraft.db.query.SelectDirectRequest;
import dcraft.db.query.SelectFields;
import dcraft.db.query.WhereEqual;
import dcraft.db.query.WhereField;
import dcraft.db.update.InsertRecordRequest;
import dcraft.db.update.RetireRecordRequest;
import dcraft.db.update.ReviveRecordRequest;
import dcraft.db.update.UpdateRecordRequest;
import dcraft.hub.TenantInfo;
import dcraft.hub.Hub;
import dcraft.lang.op.OperationContext;
import dcraft.struct.CompositeStruct;
import dcraft.struct.RecordStruct;
import dcraft.work.TaskRun;

public class Products {
	
	/******************************************************************
	 * Categories
	 ******************************************************************/	
	static public void handleCategories(TaskRun request, String op, Message msg) {
		RecordStruct rec = msg.getFieldAsRecord("Body");
		
		IDatabaseManager db = Hub.instance.getDatabase();
		
		if ("Load".equals(op)) {
			LoadRecordRequest req = new LoadRecordRequest()
				.withTable("dcmCategory")
				.withId(rec.getFieldAsString("Id"))
				.withNow()
				.withSelect(new SelectFields()
					.withField("Id")
					.withField("dcmTitle", "Title")
					.withField("dcmAlias", "Alias")
					.withField("dcmMode", "Mode")
					.withField("dcmParent", "Parent")
					.withField("dcmDescription", "Description")
					.withField("dcmCustomDisplayField", "CustomDisplayField", null, true)
					.withField("dcmShipAmount", "ShipAmount")
				);  
			
			db.submit(req, new ObjectResult() {
				@Override
				public void process(CompositeStruct result) {
					if (result == null) {
						request.error("Unable to load category record");
						request.complete();
						return;
					}
					
					db.submit(
							new SelectDirectRequest()
								.withTable("dcmCategory")
								.withSelect(new SelectFields()
									.withField("Id")
									.withField("dcmTitle", "Title")
									.withField("dcmAlias", "Alias")
									.withField("dcmMode", "Mode")
									.withField("dcmParent", "Parent")
									.withField("dcmDescription", "Description"))
								.withWhere(new WhereEqual(new WhereField("dcmParent"), rec.getFieldAsString("Id"))),
							new ObjectResult() {
								@Override
								public void process(CompositeStruct result2) {
									if (result2 == null) {
										request.error("Unable to load category record");
										request.complete();
										return;
									}
									
									((RecordStruct)result).setField("Children", result2);
									
									request.returnValue(result);
								}
							});					
				}
			});
			
			return;
		}
		
		if ("Update".equals(op)) {
			UpdateRecordRequest req = new UpdateRecordRequest();
			
			req
				.withTable("dcmCategory")		
				.withId(rec.getFieldAsString("Id"))
				.withConditionallySetFields(rec, "Title", "dcmTitle", "Alias", "dcmAlias", "Mode", "dcmMode", "Parent", "dcmParent", "Description", "dcmDescription", "ShipAmount", "dcmShipAmount");
			
			db.submit(req, new ObjectFinalResult(request));
			
			return ;
		}
		
		if ("Add".equals(op)) {
			InsertRecordRequest req = new InsertRecordRequest();
			
			req
				.withTable("dcmCategory")		
				.withConditionallySetFields(rec, "Title", "dcmTitle", "Alias", "dcmAlias", "Mode", "dcmMode", "Parent", "dcmParent", "Description", "dcmDescription", "ShipAmount", "dcmShipAmount");
			
			db.submit(req, new ObjectResult() {
				@Override
				public void process(CompositeStruct result) {
					if (!this.hasErrors()) {
						TenantInfo domain = OperationContext.get().getUserContext().getTenant();
						
						String path = "/dcw/" + domain.getAlias() + "/galleries/store/category/" + rec.getFieldAsString("Alias");
						
						try {
							Files.createDirectories(Hub.instance.getTenantsFileStore().resolvePath(path));
						} 
						catch (IOException x) {
							// TODO Auto-generated catch block
							x.printStackTrace();
						}
						
						/* TODO review what to do here...probably use the Bucket instead
						fs..addFolder(new CommonPath(path), new FuncCallback<IFileStoreFile>() {							
							@Override
							public void callback() {
								request.returnValue(result);
								
								// TODO remove this - it is specific to a website - make general purpose setting instead
								/*
								if (!this.hasErrors()) {
									CommonPath metapath = this.getResult().resolvePath(new CommonPath("/meta.json"));
									
									fs.getFileDetail(metapath, new FuncCallback<IFileStoreFile>() {
										@Override
										public void callback() {
											if (!this.hasErrors()) {
												RecordStruct meta = new RecordStruct(
														new FieldStruct("Variations", new ListStruct(
																new RecordStruct(
																		new FieldStruct("ExactWidth", 175),
																		new FieldStruct("ExactHeight", 150),
																		new FieldStruct("Alias", "full"),
																		new FieldStruct("Name", "Full Size")
																)
														))
												);
												
												this.getResult().writeAllText(meta.toPrettyString(), new OperationCallback() {										
													@Override
													public void callback() {
														request.returnValue(result);
													}
												});
											}
											else {
												request.returnValue(result);
											}
										}
									});
								}
								else {
									request.returnValue(result);
								}
								* /
							}
						});
						*/
					}
					else {
						request.returnValue(result);
					}
				}
			});
			
			return;
		}
		
		if ("Retire".equals(op)) {
			db.submit(new RetireRecordRequest("dcmCategory", rec.getFieldAsString("Id")), new ObjectFinalResult(request));
			return ;
		}
		
		if ("Revive".equals(op)) {
			db.submit(new ReviveRecordRequest("dcmCategory", rec.getFieldAsString("Id")), new ObjectFinalResult(request));
			return ;
		}
		
		if ("Lookup".equals(op)) {
			db.submit(
					new SelectDirectRequest()
						.withTable("dcmCategory") 
						.withSelect(new SelectFields()
								.withField("Id")
								.withField("dcmTitle", "Title")
								.withField("dcmAlias", "Alias")
								.withField("dcmMode", "Mode")
								.withField("dcmParent", "Parent")
								.withField("dcmDescription", "Description"))
						.withWhere(new WhereEqual(new WhereField("dcmAlias"), rec.getFieldAsString("Alias"))), 
					new ObjectFinalResult(request));
			return ;
		}

		if ("List".equals(op)) {
			db.submit(
				new SelectDirectRequest()
					.withTable("dcmCategory")
					.withSelect(new SelectFields()
						.withField("Id")
						.withField("dcmTitle", "Title")
						.withField("dcmAlias", "Alias")
						.withField("dcmMode", "Mode")
						.withField("dcmParent", "Parent")
						.withField("dcmDescription", "Description")),
				new ObjectFinalResult(request));
			
			return ;
		}		
	}
	
	
	/******************************************************************
	 * Coupons
	 ******************************************************************/	
	
	static public void handleCoupons(TaskRun request, String op, Message msg) {
		RecordStruct rec = msg.getFieldAsRecord("Body");
		
		IDatabaseManager db = Hub.instance.getDatabase();
		
		if ("Load".equals(op)) {
			LoadRecordRequest req = new LoadRecordRequest()
				.withTable("dcmDiscount")
				.withId(rec.getFieldAsString("Id"))			// TODO load by coupon Code too
				.withNow()
				.withSelect(new SelectFields()
					.withField("Id")
					.withField("dcmTitle", "Title")
					.withField("dcmCode", "Code")
					.withField("dcmType", "Type")
					.withField("dcmMode", "Mode")
					.withField("dcmAmount", "Amount")
					.withField("dcmMinimumOrder", "MinimumOrder")
					.withField("dcmStart", "Start")
					.withField("dcmExpire", "Expire")
					.withField("dcmAutomatic", "Automatic")
					.withField("dcmOneTimeUse", "OneTimeUse")
					.withField("dcmWasUsed", "WasUsed")
				);  

			db.submit(req, new ObjectFinalResult(request));
			
			return;
		}
		
		if ("Update".equals(op)) {
			UpdateRecordRequest req = new UpdateRecordRequest();
			
			req
				.withTable("dcmDiscount")		
				.withId(rec.getFieldAsString("Id"))
				.withConditionallySetFields(rec, "Title", "dcmTitle", "Type", "dcmType", "Mode", "dcmMode", 
						"Code", "dcmCode", "MinimumOrder", "dcmMinimumOrder", "Amount", "dcmAmount",
						"Start", "dcmStart", "Expire", "dcmExpire", "Automatic", "dcmAutomatic",
						"OneTimeUse", "dcmOneTimeUse", "WasUsed", "dcmWasUsed"
				);

			db.submit(req, new ObjectFinalResult(request));
			
			return ;
		}
		
		if ("Add".equals(op)) {
			InsertRecordRequest req = new InsertRecordRequest();
			
			req
				.withTable("dcmDiscount")		
				.withConditionallySetFields(rec, "Title", "dcmTitle", "Type", "dcmType", "Mode", "dcmMode", 
						"Code", "dcmCode", "MinimumOrder", "dcmMinimumOrder", "Amount", "dcmAmount",
						"Start", "dcmStart", "Expire", "dcmExpire", "Automatic", "dcmAutomatic",
						"OneTimeUse", "dcmOneTimeUse", "WasUsed", "dcmWasUsed"
				);
			
			db.submit(req, new ObjectResult() {
				@Override
				public void process(CompositeStruct result) {
					if (!this.hasErrors()) {
						TenantInfo domain = OperationContext.get().getUserContext().getTenant();
						
						String path = "/dcw/" + domain.getAlias() + "/galleries/store/category/" + rec.getFieldAsString("Alias");
						
						try {
							Files.createDirectories(Hub.instance.getTenantsFileStore().resolvePath(path));
						} 
						catch (IOException x) {
							// TODO Auto-generated catch block
							x.printStackTrace();
						}

						/* TODO review, use Bucket instead
						fs.addFolder(new CommonPath(path), new FuncCallback<IFileStoreFile>() {							
							@Override
							public void callback() {
								request.returnValue(result);
							}
						});
						*/
					}
					else {
						request.returnValue(result);
					}
				}
			});
			
			return;
		}
		
		if ("Retire".equals(op)) {
			db.submit(new RetireRecordRequest("dcmDiscount", rec.getFieldAsString("Id")), new ObjectFinalResult(request));
			return ;
		}
		
		if ("Revive".equals(op)) {
			db.submit(new ReviveRecordRequest("dcmDiscount", rec.getFieldAsString("Id")), new ObjectFinalResult(request));
			return ;
		}
		
		if ("List".equals(op)) {
			db.submit(
				new SelectDirectRequest()
					.withTable("dcmDiscount")
					.withSelect(new SelectFields()
						.withField("Id")
						.withField("dcmTitle", "Title")
						.withField("dcmCode", "Code")
						.withField("dcmType", "Type")
						.withField("dcmMode", "Mode")
						.withField("dcmAmount", "Amount")
						.withField("dcmMinimumOrder", "MinimumOrder")
						.withField("dcmStart", "Start")
						.withField("dcmExpire", "Expire")
						.withField("dcmAutomatic", "Automatic")
						.withField("dcmOneTimeUse", "OneTimeUse")
						.withField("dcmWasUsed", "WasUsed")
					),
				new ObjectFinalResult(request));
			
			return;
		}		
	}
	
	/******************************************************************
	 * Products
	 ******************************************************************/	
	static public void handleProducts(TaskRun request, String op, Message msg) {
		RecordStruct rec = msg.getFieldAsRecord("Body");
		
		IDatabaseManager db = Hub.instance.getDatabase();
		
		if ("Load".equals(op)) {
			LoadRecordRequest req = new LoadRecordRequest()
				.withTable("dcmProduct")
				.withId(rec.getFieldAsString("Id"))
				.withNow()
				.withSelect(new SelectFields()
					.withField("Id")
					.withField("dcmTitle", "Title")
					.withField("dcmAlias", "Alias")
					.withField("dcmSku", "Sku")
					.withField("dcmDescription", "Description")
					.withField("dcmCustomDisplayField", "CustomDisplayField", null, true)
					.withField("dcmCategory", "Category")
					.withField("dcmCategoryPosition", "CategoryPosition")
					.withField("dcmPrice", "Price")
					.withField("dcmVariablePrice", "VariablePrice")
					.withField("dcmMininumPrice", "MininumPrice")
					.withField("dcmShipAmount", "ShipAmount")
					.withField("dcmShipWeight", "ShipWeight")
					.withField("dcmShipFree", "ShipFree")
					.withField("dcmTaxFree", "TaxFree")
					.withField("dcmShowInStore", "ShowInStore")
					.withField("dcmTag", "Tags")
				);  
			
			db.submit(req, new ObjectFinalResult(request));
			
			return;
		}
		
		if ("Update".equals(op)) {
			UpdateRecordRequest req = new UpdateRecordRequest();
			
			req
				.withTable("dcmProduct")		
				.withId(rec.getFieldAsString("Id"))
				.withConditionallySetFields(rec, "Title", "dcmTitle", "Alias", "dcmAlias", "Sku", "dcmSku", 
						"Description", "dcmDescription", "Category", "dcmCategory", "CategoryPosition", "dcmCategoryPosition",
						"Price", "dcmPrice", "ShipAmount", "dcmShipAmount", "ShipWeight", "dcmShipWeight", 
						"VariablePrice", "dcmVariablePrice", "MininumPrice", "dcmMininumPrice",
						"ShipFree", "dcmShipFree", "TaxFree", "dcmTaxFree", "ShowInStore", "dcmShowInStore")
				.withConditionallySetList(rec, "Tags", "dcmTag");
			
			db.submit(req, new ObjectFinalResult(request));
			
			return ;
		}
		
		if ("Add".equals(op)) {
			InsertRecordRequest req = new InsertRecordRequest();
			
			req
				.withTable("dcmProduct")		
				.withConditionallySetFields(rec, "Title", "dcmTitle", "Alias", "dcmAlias", "Sku", "dcmSku", 
						"Description", "dcmDescription", "Category", "dcmCategory", "CategoryPosition", "dcmCategoryPosition",
						"Price", "dcmPrice", "ShipAmount", "dcmShipAmount", "ShipWeight", "dcmShipWeight", 
						"VariablePrice", "dcmVariablePrice", "MininumPrice", "dcmMininumPrice",
						"ShipFree", "dcmShipFree", "TaxFree", "dcmTaxFree", "ShowInStore", "dcmShowInStore")
				.withConditionallySetList(rec, "Tags", "dcmTag");
			
			db.submit(req, new ObjectResult() {
				@Override
				public void process(CompositeStruct result) {
					if (!this.hasErrors()) {
						TenantInfo domain = OperationContext.get().getUserContext().getTenant();
						
						String path = "/dcw/" + domain.getAlias() + "/galleries/store/product/" + rec.getFieldAsString("Alias");
						
						try {
							Files.createDirectories(Hub.instance.getTenantsFileStore().resolvePath(path));
						} 
						catch (IOException x) {
							// TODO Auto-generated catch block
							x.printStackTrace();
						}
						
						/* TODO review - use Bucket instead
						fs.addFolder(new CommonPath(path), new FuncCallback<IFileStoreFile>() {							
							@Override
							public void callback() {
								request.returnValue(result);
							}
						});
						*/
					}
					else {
						request.returnValue(result);
					}
				}
			});
			
			return;
		}
		
		if ("Retire".equals(op)) {
			db.submit(new RetireRecordRequest("dcmProduct", rec.getFieldAsString("Id")), new ObjectFinalResult(request));
			return ;
		}
		
		if ("Revive".equals(op)) {
			db.submit(new ReviveRecordRequest("dcmProduct", rec.getFieldAsString("Id")), new ObjectFinalResult(request));
			return ;
		}
		
		if ("Lookup".equals(op)) {
			db.submit(
					new SelectDirectRequest()
						.withTable("dcmProduct")
						.withSelect(new SelectFields()
								.withField("Id")
								.withField("dcmTitle", "Title")
								.withField("dcmAlias", "Alias")
								.withField("dcmSku", "Sku")
								.withField("dcmShowInStore", "ShowInStore")
								.withField("dcmPrice", "Price")
								.withField("dcmDescription", "Description"))
						.withWhere(new WhereEqual(new WhereField("dcmAlias"), rec.getFieldAsString("Alias"))), 
					new ObjectFinalResult(request));
			
			return ;
		}
		
		if ("CatList".equals(op)) {
			// TODO support category alias lookup too
			
			LoadRecordRequest req = new LoadRecordRequest()
				.withTable("dcmCategory")
				.withId(rec.getFieldAsString("Id"))
				.withNow()
				.withSelect(new SelectFields()
					.withField("Id", "CategoryId")
					.withField("dcmTitle", "Category")
					.withField("dcmAlias", "CategoryAlias")
				);  
			
			db.submit(req, new ObjectResult() {
				@Override
				public void process(CompositeStruct result) {
					if (result == null) {
						request.error("Unable to load category record");
						request.complete();
						return;
					}
					
					db.submit(
							new SelectDirectRequest()
								.withTable("dcmProduct")
								.withSelect(new SelectFields()
										.withField("Id")
										.withField("dcmTitle", "Title")
										.withField("dcmAlias", "Alias")
										.withField("dcmSku", "Sku")
										.withField("dcmShowInStore", "ShowInStore")
										.withField("dcmPrice", "Price")
										.withField("dcmDescription", "Description"))
								.withWhere(new WhereEqual(new WhereField("dcmCategory"), rec.getFieldAsString("Id"))), 
							new ObjectResult() {
								@Override
								public void process(CompositeStruct result2) {
									if (result2 == null) {
										request.error("Unable to load product list");
										request.complete();
										return;
									}
									((RecordStruct)result).setField("Products", result2);
									
									request.returnValue(result);
								}
							});
				}
			});

			return ;
		}

		if ("List".equals(op)) {
			db.submit(
				new SelectDirectRequest()
					.withTable("dcmProduct")
					.withSelect(new SelectFields()
					.withField("Id")
					.withField("dcmTitle", "Title")
					.withField("dcmAlias", "Alias")
					.withField("dcmSku", "Sku")
					.withField("dcmShowInStore", "ShowInStore")
					.withField("dcmPrice", "Price")
					.withField("dcmDescription", "Description")), 
				new ObjectFinalResult(request));
			
			return ;
		}		
	}
}
