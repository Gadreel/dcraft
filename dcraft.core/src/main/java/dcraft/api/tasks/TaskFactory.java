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
package dcraft.api.tasks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import dcraft.api.ApiSession;
import dcraft.filestore.CommonPath;
import dcraft.log.Logger;
import dcraft.struct.FieldStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.work.Task;

public class TaskFactory {
	static public Task createUploadTask(ApiSession sess, String servicename, Path local, CommonPath remote, Struct storeParams, boolean allowResume) {
		UploadFile work = new UploadFile();
		work.session = sess;
		
		return Task.taskWithSubContext()
			.withTitle("Upload file " + local)
			.withWork(work)
			.withParams(new RecordStruct(
					new FieldStruct("LocalPath", local),
					new FieldStruct("RemotePath", remote),
					new FieldStruct("ServiceName", servicename),
					new FieldStruct("TransferParams", storeParams),
					new FieldStruct("ForceOverwrite", !allowResume)
			))
			.withTimeout(1)
			.withDeadline(0);
	}
	
	static public Task createDownloadTask(ApiSession sess, String servicename, Path local, CommonPath remote, Struct storeParams, boolean allowResume) {
		DownloadFile work = new DownloadFile();
		work.session = sess;
		
		RecordStruct params = new RecordStruct(
				new FieldStruct("LocalPath", local),
				new FieldStruct("RemotePath", remote),
				new FieldStruct("ServiceName", servicename),
				new FieldStruct("TransferParams", storeParams)
		);
		
		if (allowResume && Files.exists(local)) {
			try {
				params.setField("Offset", Files.size(local));
			} 
			catch (IOException x) {
				Logger.error("Unable to get file size for: " + local);
				return null;
			}
		}		
		
		return Task.taskWithSubContext()
			.withTitle("Download file " + local)
			.withWork(work)
			.withParams(params)
			.withTimeout(1)
			.withDeadline(0);
	}
}
