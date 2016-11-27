package dcraft.mail;

import java.util.Collection;

import dcraft.cms.util.CatalogUtil;
import dcraft.filestore.CommonPath;
import dcraft.filestore.IFileStoreFile;
import dcraft.hub.Hub;
import dcraft.lang.op.FuncResult;
import dcraft.log.Logger;
import dcraft.struct.FieldStruct;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.work.Task;
import dcraft.xml.XElement;

public class MailUtil {
	static public Task createSendTask(String from, String to, String reply, String subject, String body) {
		return MailUtil.createSendTask(from, to, reply, subject, body, null);
	}
	
	static public Task createSendTask(String from, String to, String reply, String subject, String body, Collection<? extends EmailAttachment> attachments) {
		RecordStruct params = new RecordStruct(
			new FieldStruct("From", from),
			new FieldStruct("ReplyTo", reply),
			new FieldStruct("To", to),
			new FieldStruct("Subject", subject),
			new FieldStruct("Body", body)
		);
		
		if ((attachments != null) && (attachments.size() > 0)) {
			ListStruct alist = new ListStruct();
			
			for (EmailAttachment attch : attachments)
				alist.addItem(attch.toParams());
			
			params.setField("Attachments", alist);		
		}
		
		return MailUtil.createSendTask(params);
	}
	
	static public Task createSendTask(RecordStruct params) {
		String tid = Task.nextTaskId("EMAIL");
		
		XElement settings = CatalogUtil.getSettings("Email");
		
		if (settings == null) {
			Logger.error("Missing Email settings");
			return null;
		}
		
		String useTopic = settings.getAttribute("Topic", "Batch");
		
		Task task = Task.taskWithSubContext()
			.withId(tid)
			.withTitle("Send Email To " + params.getFieldAsString("To"))
			.withParams(params)
			.withTopic(useTopic)
			.withDefaultLogger()
			.withMaxTries(6)
			.withTimeout(30);
		
		String method = settings.getAttribute("Method", "Smtp");

		if ("Smtp".equals(method)) {
			task.withWork(SmtpWork.class);
		}
		else {
			Logger.error("Unknown Email send Method: " + method);
			return null;		
		}
		
		return task;
	}
	
	static public Task createBuildSendEmailTask(String from, String to, String reply, CommonPath template, RecordStruct data) {
		RecordStruct sparams = new RecordStruct(
				new FieldStruct("From", from),
				new FieldStruct("To", to),
				new FieldStruct("ReplyTo", reply),
				new FieldStruct("Template", template),
				new FieldStruct("Data", data)
			);
			
		return MailUtil.createSendBuildTask(sparams);
	}
	
	static public Task createBuildSendEmailTask(String from, String to, String reply, CommonPath template, IFileStoreFile datapath, boolean managed) {
		RecordStruct sparams = new RecordStruct(
				new FieldStruct("From", from),
				new FieldStruct("To", to),
				new FieldStruct("ReplyTo", reply),
				new FieldStruct("Template", template),
				new FieldStruct("Managed", managed),
				new FieldStruct("DataPath", datapath)
			);
			
		return MailUtil.createSendBuildTask(sparams);
	}
	
	// submit to the mail queue just like send task
	static public Task createSendBuildTask(RecordStruct params) {
		String tid = Task.nextTaskId("EMAIL");
		
		XElement settings = CatalogUtil.getSettings("Email");
		
		if (settings == null) {
			Logger.error("Missing Email settings");
			return null;
		}
		
		String useTopic = settings.getAttribute("Topic", "Batch");
		
		Task task = Task.taskWithSubContext()
			.withId(tid)
			.withTitle("Build and Send Email To " + params.getFieldAsString("To"))
			.withParams(params)
			.withTopic(useTopic)
			.withDefaultLogger()
			.withMaxTries(6)
			.withTimeout(30)
			.withWork(BuildSendWork.class);
		
		return task;
	}	
	
	// not necessary to submit to the mail queue, generally not useful
	static public Task createBuildTask(RecordStruct params) {
		String tid = Task.nextTaskId("EMAIL");
		
		XElement settings = CatalogUtil.getSettings("Email");
		
		if (settings == null) {
			Logger.error("Missing Email settings");
			return null;
		}
		
		String useTopic = settings.getAttribute("Topic", "Batch");
		
		Task task = Task.taskWithSubContext()
			.withId(tid)
			.withTitle("Build Email To " + params.getFieldAsString("To"))
			.withParams(params)
			.withTopic(useTopic)
			.withDefaultLogger()
			.withMaxTries(6)
			.withTimeout(30)
			.withWork(BuildWork.class);
		
		return task;
	}
	
	static public FuncResult<RecordStruct> submit(Task mail) {
		FuncResult<RecordStruct> or = new FuncResult<>();
		
		XElement settings = CatalogUtil.getSettings("Email");
		
		if (settings == null) {
			or.error("Missing Email settings");
			return or;
		}
		
		String submode = settings.getAttribute("SubmitMode", "Local");
		
		// run the message off the queue
		if ("Queue".equals(submode)) {
			FuncResult<String> ares = Hub.instance.getWorkQueue().submit(mail);
			
			if (!or.hasErrors())
				or.setResult(new RecordStruct(new FieldStruct("WorkId", ares.getResult()), new FieldStruct("TaskId", mail.getId())));
		}
		// run the message in local pool, don't wait
		else { 
			Hub.instance.getWorkPool().submit(mail);
			
			if (!or.hasErrors())
				or.setResult(new RecordStruct(new FieldStruct("TaskId", mail.getId())));
		}
		
		return or;
	}
}
