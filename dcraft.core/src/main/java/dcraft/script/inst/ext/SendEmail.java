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
package dcraft.script.inst.ext;

import dcraft.filestore.CommonPath;
import dcraft.filestore.IFileStoreFile;
import dcraft.log.Logger;
import dcraft.mail.MailUtil;
import dcraft.script.ExecuteState;
import dcraft.script.Instruction;
import dcraft.script.StackEntry;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.util.StringUtil;
import dcraft.work.Task;

public class SendEmail extends Instruction {
	@Override
	public void run(final StackEntry stack) {
		String from = stack.stringFromSource("From");		// optional
		String reply = stack.stringFromSource("ReplyTo");		// optional
		String to = stack.stringFromSource("To");
		boolean managed = stack.boolFromSource("Managed");
		
		// direct send
		String subject = stack.stringFromSource("Subject");
		String body = this.source.hasText() ? stack.resolveValue(this.source.getText()).toString() : stack.stringFromSource("Body", "[under construction]");
		
		// build from template, then send
		Struct dstruct = stack.refFromSource("Data");
		
		RecordStruct data = (dstruct instanceof RecordStruct) ? (RecordStruct) dstruct : new RecordStruct();

		// use DataPath if it is a filestore file
		Struct datapath = stack.refFromSource("DataPath");
		
		if (! (datapath instanceof IFileStoreFile))
			datapath = null;
		
		String template = stack.stringFromSource("Template");
		
		Task task = null;
		
		if (StringUtil.isNotEmpty(template)) {
			if (datapath != null)
				task = MailUtil.createBuildSendEmailTask(from, to, reply, new CommonPath(template), (IFileStoreFile) datapath, managed);
			else
				task = MailUtil.createBuildSendEmailTask(from, to, reply, new CommonPath(template), data);
		}
		else if (StringUtil.isNotEmpty(subject) || StringUtil.isNotEmpty(body)) {
			task = MailUtil.createSendTask(from, to, reply, subject, body);
		}
		
		if (task != null) {
			MailUtil.submit(task);
		}
		else {
			Logger.error("Missing parameters to SendMail instruction.");
		}
		
		stack.setState(ExecuteState.Done);
		stack.resume();
		
		/* TODO support for attachments also */
	}
	
	@Override
	public void cancel(StackEntry stack) {
		// do nothing, this isn't cancellable
	}
}
