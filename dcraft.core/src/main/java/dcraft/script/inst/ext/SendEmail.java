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
import dcraft.log.Logger;
import dcraft.mail.MailUtil;
import dcraft.script.ExecuteState;
import dcraft.script.Instruction;
import dcraft.script.StackEntry;
import dcraft.struct.RecordStruct;
import dcraft.util.StringUtil;
import dcraft.work.Task;

public class SendEmail extends Instruction {
	@Override
	public void run(final StackEntry stack) {
		String from = stack.stringFromSource("From");		// optional
		String reply = stack.stringFromSource("ReplyTo");		// optional
		String to = stack.stringFromSource("To");
		
		// direct send
		String subject = stack.stringFromSource("Subject");
		String body = this.source.hasText() ? stack.resolveValue(this.source.getText()).toString() : stack.stringFromSource("Body", "[under construction]");
		
		// build from template, then send
		RecordStruct data = (RecordStruct) stack.refFromSource("Data");
		String datapath = stack.stringFromSource("DataPath");
		String template = stack.stringFromSource("Template");
		
		Task task = null;
		
		if (StringUtil.isNotEmpty(template)) {
			if (StringUtil.isNotEmpty(datapath))
				task = MailUtil.createBuildSendEmailTask(from, to, reply, new CommonPath(template), new CommonPath(datapath));
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
