package dcraft.cms.bucket;

import java.util.List;

import dcraft.filestore.IFileStoreFile;
import dcraft.filestore.bucket.Bucket;
import dcraft.lang.op.FuncCallback;
import dcraft.lang.op.OperationCallback;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;

public class GalleryBucket extends Bucket {
	@Override
	public void handleListFiles(RecordStruct request, FuncCallback<ListStruct> fcb) {
		// check bucket security
		if (!this.checkReadAccess()) {
			fcb.errorTr(434);
			fcb.complete();
			return;
		}
		
		this.mapRequest(request, new FuncCallback<IFileStoreFile>() {
			@Override
			public void callback() {
				if (this.hasErrors()) {
					fcb.complete();
					return;
				}
				
				if (this.isEmptyResult()) {
					fcb.error("Your request appears valid but does not map to a file.  Unable to complete.");
					fcb.complete();
					return;
				}
				
				IFileStoreFile fi = this.getResult();
				
				if (!fi.exists()) {
					fcb.complete();
					return;
				}

				GalleryBucket.this.fsd.getFolderListing(fi.path(), new FuncCallback<List<IFileStoreFile>>() {
					@Override
					public void callback() {
						if (this.hasErrors()) {
							fcb.complete();
							return;					
						}
						
						boolean showHidden = fcb.getContext().getUserContext().isTagged("Admin");
						
						ListStruct files = new ListStruct();
						
						for (IFileStoreFile file : this.getResult()) {
							if (file.getName().equals(".DS_Store"))
								continue;
							
							if (!showHidden && file.getName().startsWith("."))		 
								continue;
							
							if (!file.isFolder())
								continue;
							
							boolean isImage = file.getName().endsWith(".v");
							
							RecordStruct fdata = new RecordStruct();
							
							fdata.setField("FileName", isImage ? file.getName().substring(0, file.getName().length() - 2) : file.getName());
							fdata.setField("IsFolder", !isImage);
							
							if (isImage) {
								// TODO set modified and size based on the `original` variation 
							}
							
							fdata.setField("LastModified", file.getModificationTime());
							fdata.setField("Size", file.getSize());
							fdata.setField("Extra", file.getExtra());
							
							files.addItem(fdata);
						}
						
						fcb.setResult(files);
						fcb.complete();
					}
				});
			}
		});
	}	

	@Override
	public void handleCustom(RecordStruct request, FuncCallback<RecordStruct> fcb) {
		// check bucket security
		if (!this.checkWriteAccess()) {
			fcb.errorTr(434);
			fcb.complete();
			return;
		}

		String cmd = request.getFieldAsString("Command");
		
		if ("LoadMeta".equals(cmd)) {
			this.mapRequest(request, new FuncCallback<IFileStoreFile>() {
				@Override
				public void callback() {
					if (this.hasErrors()) {
						fcb.complete();
						return;
					}
					
					if (this.isEmptyResult()) {
						fcb.error("Your request appears valid but does not map to a folder.  Unable to complete.");
						fcb.complete();
						return;
					}
					
					IFileStoreFile fi = this.getResult();
					
					if (!fi.exists()) {
						fcb.error("Your request appears valid but does not map to a folder.  Unable to complete.");
						fcb.complete();
						return;
					}
					
					GalleryBucket.this.fsd.getFileDetail(fi.path().resolve("meta.json"), new FuncCallback<IFileStoreFile>() {
						@Override
						public void callback() {
							if (this.hasErrors() || this.isEmptyResult()) {
								fcb.complete();
								return;
							}
							
							IFileStoreFile mfi = this.getResult();
							
							if (!mfi.exists()) {
								fcb.complete();
								return;
							}
							
							mfi.readAllText(new FuncCallback<String>() {							
								@Override
								public void callback() {
									if (this.isNotEmptyResult())
										fcb.setResult(new RecordStruct()
											.withField("Extra", Struct.objectToComposite(this.getResult()))
										);
							
									fcb.complete();
								}
							});
						}
					});
				}
			});
			
			return;
		}
		
		if ("SaveMeta".equals(cmd)) {
			this.mapRequest(request, new FuncCallback<IFileStoreFile>() {
				@Override
				public void callback() {
					if (this.hasErrors()) {
						fcb.complete();
						return;
					}
					
					if (this.isEmptyResult()) {
						fcb.error("Your request appears valid but does not map to a folder.  Unable to complete.");
						fcb.complete();
						return;
					}
					
					IFileStoreFile fi = this.getResult();
					
					if (!fi.exists()) {
						fcb.error("Your request appears valid but does not map to a folder.  Unable to complete.");
						fcb.complete();
						return;
					}
					
					GalleryBucket.this.fsd.getFileDetail(fi.path().resolve("meta.json"), new FuncCallback<IFileStoreFile>() {
						@Override
						public void callback() {
							if (this.hasErrors() || this.isEmptyResult()) {
								fcb.complete();
								return;
							}
							
							IFileStoreFile mfi = this.getResult();
							
							mfi.writeAllText(request.getFieldAsRecord("Params").toPrettyString(), new OperationCallback() {
								@Override
								public void callback() {
									fcb.complete();
								}
							});
						}
					});
				}
			});
			
			return;
		}
		
		if ("ImageDetail".equals(cmd)) {
			this.mapRequest(request, new FuncCallback<IFileStoreFile>() {
				@Override
				public void callback() {
					if (this.hasErrors()) {
						fcb.complete();
						return;
					}
					
					if (this.isEmptyResult()) {
						fcb.error("Your request appears valid but does not map to a folder.  Unable to complete.");
						fcb.complete();
						return;
					}
					
					IFileStoreFile fi = this.getResult();
					
					if (!fi.exists()) {
						fcb.error("Your request appears valid but does not map to a folder.  Unable to complete.");
						fcb.complete();
						return;
					}
					
					GalleryBucket.this.fsd.getFolderListing(fi.path(), new FuncCallback<List<IFileStoreFile>>() {
						@Override
						public void callback() {
							if (this.hasErrors() || this.isEmptyResult()) {
								fcb.complete();
								return;
							}
							
							ListStruct files = new ListStruct();
							
							for (IFileStoreFile file : this.getResult()) {
								if (file.getName().startsWith(".")) 
									continue;
								
								String fname = file.getName();
								
								if (! fname.endsWith(".jpg") && ! fname.endsWith(".jpeg") && ! fname.endsWith(".png")
										&& ! fname.endsWith(".gif")) 
									continue;
								
								RecordStruct fdata = new RecordStruct();
								
								String ext = file.getExtension();
								
								fdata.setField("Alias", fname.substring(0, fname.length() - ext.length() - 1));
								fdata.setField("Extension", ext);					
								fdata.setField("LastModified", file.getModificationTime());
								fdata.setField("Size", file.getSize());
								
								files.addItem(fdata);
							}

							fcb.setResult(new RecordStruct().withField("Extra", files));
							fcb.complete();
						}
					});
				}
			});
			
			return;
		}
		
		super.handleCustom(request, fcb);
	}
}
