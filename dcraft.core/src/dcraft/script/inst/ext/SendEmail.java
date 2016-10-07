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

import dcraft.bus.Message;
import dcraft.bus.ServiceResult;
import dcraft.hub.Hub;
import dcraft.script.ExecuteState;
import dcraft.script.Instruction;
import dcraft.script.StackEntry;
import dcraft.struct.FieldStruct;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;

public class SendEmail extends Instruction {
	@Override
	public void run(final StackEntry stack) {
		String to = stack.stringFromSource("To");
		String from = stack.stringFromSource("From");
		String subject = stack.stringFromSource("Subject");
		String body = stack.stringFromSource("Body");
		ListStruct attachments = new ListStruct();
		
		/* TODO
		XElement cmd = stack.getInstruction().getXml();
		
		for (XElement attach : cmd.selectAll("Attachment")) {
			Struct src = stack.refFromElement(attach, "Source");

			if (!(src instanceof IFileStoreFile) && ! (src instanceof RecordStruct)) {
				// TODO log wrong type
				stack.resume();
				return;
			}
			
			IFileStoreFile source = (IFileStoreFile)src;

			Memory battach = new Memory();
			
			attachments.addItem(new RecordStruct(
					new FieldStruct("Name", stack.stringFromElement(attach, "Name")),
					new FieldStruct("Mime", stack.stringFromElement(attach, "Mime")),
					new FieldStruct("Content", battach)
			));
			
			final CountDownLatch latch = new CountDownLatch(1);		// TODO review need for this, vs moving send message into here
			final OutputStream out = new OutputWrapper(battach);
			
			source.copyTo(out, new OperationCallback() {				
				@Override
				public void callback() {
					// TODO improve, check abort, etc
					
					try {
						out.close();
					} 
					catch (IOException x) {
					}
					
					latch.countDown();
				}
			});
			
			try {
				latch.await();
			} 
			catch (InterruptedException x) {
				// TODO log error
				stack.resume();
				return;
			}
		}
			*/
		
		Hub.instance.getBus().sendMessage(
				new Message("dciEmail", "Message", "Send", new RecordStruct(
						new FieldStruct("To", to),
						new FieldStruct("From", from),
						new FieldStruct("Subject", subject),
						new FieldStruct("Body", body),
						new FieldStruct("Attachments", attachments)
				)), 
				new ServiceResult() {
					@Override
					public void callback() {
						stack.setState(ExecuteState.Done);
						stack.resume();
					}
				}
		);	
	}
	
	@Override
	public void cancel(StackEntry stack) {
		// do nothing, this isn't cancellable
	}
}
