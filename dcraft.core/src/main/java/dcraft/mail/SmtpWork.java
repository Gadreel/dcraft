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
package dcraft.mail;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import dcraft.cms.util.CatalogUtil;
import dcraft.filestore.IFileStoreFile;
import dcraft.hub.Hub;
import dcraft.io.InputWrapper;
import dcraft.io.OutputWrapper;
import dcraft.lang.Memory;
import dcraft.lang.op.FuncCallback;
import dcraft.lang.op.OperationContext;
import dcraft.lang.op.OperationResult;
import dcraft.log.DebugLevel;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.struct.scalar.StringStruct;
import dcraft.util.HexUtil;
import dcraft.util.IOUtil;
import dcraft.util.StringUtil;
import dcraft.work.StateWork;
import dcraft.work.TaskRun;
import dcraft.work.WorkStep;
import dcraft.xml.XElement;

public class SmtpWork extends StateWork {
	final public WorkStep PREP_EMAIL = WorkStep.allocate("Prep Email Session", this::prepEmail);
	final public WorkStep ADD_ATTACH = WorkStep.allocate("Add Attachments", this::addAttach);
	final public WorkStep SEND_EMAIL = WorkStep.allocate("Send Email", this::sendEmail);
	
	protected javax.mail.Message email = null;
	protected String to = null;
	protected InternetAddress[] toaddrs = new InternetAddress[0];
	protected InternetAddress[] dbgaddrs = new InternetAddress[0];
	protected MimeMultipart content = null;
	protected int attachcnt = 0;
	protected int currattach = 0;
	
	@Override
	public void prepSteps(TaskRun trun) {
		this.withStep(this.INITIALIZE)
		.withStep(this.PREP_EMAIL)
		.withStep(this.ADD_ATTACH)
		.withStep(this.SEND_EMAIL)
		.withStep(this.FINIALIZE);
	}	
	
	public WorkStep prepEmail(TaskRun task) {
		XElement settings = CatalogUtil.getSettings("Email");
		
		if (settings == null) {
			task.error("Missing email settings");
			return this.FINIALIZE;
		}
		
		boolean smtpAuth = Struct.objectToBoolean(settings.getAttribute("SmtpAuth", "false"));
		boolean smtpDebug = Struct.objectToBoolean(settings.getAttribute("SmtpDebug", "false"));
		
		String debugBCC = settings.getAttribute("BccDebug");
		String skipto = settings.getAttribute("SkipToAddress");
		
		RecordStruct req = (RecordStruct) task.getTask().getParams();
		
		try {
			String from = req.getFieldAsString("From");		
			String reply = req.getFieldAsString("ReplyTo");		
			
			if (StringUtil.isEmpty(from))
				from = settings.getAttribute("DefaultFrom");
			
			if (StringUtil.isEmpty(reply))
				reply = settings.getAttribute("DefaultReplyTo");
			
			this.to = req.getFieldAsString("To");
			String subject = req.getFieldAsString("Subject");
			String body = req.getFieldAsString("Body");
			String textbody = req.getFieldAsString("TextBody");
			
			task.info(0, "Sending email from: " + from);
			task.info(0, "Sending email to: " + to);
			
			Properties props = new Properties();
			
			if (smtpAuth) {
				props.put("mail.smtp.auth", "true");
				
				// TODO put this back in for Java8 - until then we have issues with Could not generate DH keypair
				// see http://stackoverflow.com/questions/12743846/unable-to-send-an-email-using-smtp-getting-javax-mail-messagingexception-could
				props.put("mail.smtp.starttls.enable", "true");
			}
			
	        Session sess = Session.getInstance(props);
	
	        // do debug on task with trace level
	        if (smtpDebug || (OperationContext.get().getLevel() == DebugLevel.Trace)) {
	        	sess.setDebugOut(new DebugPrintStream(task));
	        	sess.setDebug(true);			        
	        }
	        
	        // Create a new Message
	    	this.email = new MimeMessage(sess);
	    	
			InternetAddress fromaddr = StringUtil.isEmpty(from) ? null : InternetAddress.parse(from.replace(';', ','))[0];
			InternetAddress[] rplyaddrs = StringUtil.isEmpty(reply) ? null : InternetAddress.parse(reply.replace(';', ','));
			
			if (StringUtil.isNotEmpty(to))
				this.toaddrs = InternetAddress.parse(to.replace(';', ','));
			
			if (StringUtil.isNotEmpty(debugBCC))
				this.dbgaddrs = InternetAddress.parse(debugBCC.replace(';', ','));
			
			if (StringUtil.isNotEmpty(skipto)) {
				List<InternetAddress> passed = new ArrayList<InternetAddress>();
				
				for (int i = 0; i < toaddrs.length; i++) {
					InternetAddress toa = toaddrs[i];
					
					if (!toa.getAddress().contains(skipto))
						passed.add(toa);
				}
				
				toaddrs = passed.stream().toArray(InternetAddress[]::new);
			}
			
	        try {				
				this.email.setFrom(fromaddr);
	        	
	        	if (rplyaddrs != null)
	        		this.email.setReplyTo(rplyaddrs);
	        	
	        	if (toaddrs != null)
	        		this.email.addRecipients(javax.mail.Message.RecipientType.TO, toaddrs);
	        	
	        	if (dbgaddrs != null)
	        		this.email.addRecipients(javax.mail.Message.RecipientType.BCC, dbgaddrs);
	        	
	        	this.email.setSubject(subject);
	     
	            // ALTERNATIVE TEXT/HTML CONTENT
	            MimeMultipart cover = new MimeMultipart((textbody != null) ? "alternative" : "mixed");
	            
	            if (textbody != null) {
		            MimeBodyPart txt = new MimeBodyPart();
		            txt.setText(textbody);
		            cover.addBodyPart(txt);
	            }
	        	
	            // add the message part 
	            MimeBodyPart html = new MimeBodyPart();
	            html.setContent(body, "text/html");
	            cover.addBodyPart(html);
	            
	            ListStruct attachments = req.getFieldAsList("Attachments");
	            
	            this.attachcnt = (attachments != null) ? attachments.getSize() : 0;
	            
	            if (attachcnt > 0) {
	            	// hints - https://mlyly.wordpress.com/2011/05/13/hello-world/
		            // COVER WRAP
		            MimeBodyPart wrap = new MimeBodyPart();
		            wrap.setContent(cover);
		            
		            this.content = new MimeMultipart("related");
		            this.content.addBodyPart(wrap);	        	
	            	
	            	this.email.setContent(this.content);
	            }
	            else {
	            	this.email.setContent(cover);
	            }
	            
	        } 
	        catch (Exception x) {
	        	task.error(1, "dcSendMail unable to send message due to invalid fields.");
	        }
		}
        catch (AddressException x) {
        	task.error(1, "dcSendMail unable to send message due to addressing problems.  Error: " + x);
        }
		
		return WorkStep.NEXT;
	}	

	public WorkStep addAttach(TaskRun task) {
		RecordStruct req = (RecordStruct) task.getTask().getParams();

		if (this.currattach >= this.attachcnt)
			return WorkStep.NEXT;

		ListStruct attachments = req.getFieldAsList("Attachments");

		RecordStruct attachment = attachments.getItemAsRecord(this.currattach);

		// add the attachment parts, if any
		String name = attachment.getFieldAsString("Name");
		String mime = attachment.getFieldAsString("Mime");
		
		Consumer<Memory> addAttach = new Consumer<Memory>() {
			@Override
			public void accept(Memory mem) {
				mem.setPosition(0);

				MimeBodyPart apart = new MimeBodyPart();

				DataSource source = new DataSource() {
					@Override
					public OutputStream getOutputStream() throws IOException {
						return new OutputWrapper(mem);
					}

					@Override
					public String getName() {
						return name;
					}

					@Override
					public InputStream getInputStream() throws IOException {
						return new InputWrapper(mem);
					}

					@Override
					public String getContentType() {
						return mime;
					}
				};

				try {
					apart.setDataHandler(new DataHandler(source));
					apart.setFileName(name);

					SmtpWork.this.content.addBodyPart(apart);

					SmtpWork.this.currattach++;
				} 
				catch (Exception x) {
					task.error(1, "dcSendMail unable to send message due to invalid fields.");
				}
				
				task.resume();
			}
		};

		Memory smem = attachment.getFieldAsBinary("Content");
		Struct fobj = attachment.getField("File");

		if (smem != null) {
			addAttach.accept(smem);
		}
		else if (fobj instanceof StringStruct) {
			addAttach.accept(IOUtil.readEntireFileToMemory(Paths.get(((StringStruct) fobj).getValue())));
		}
		else if (fobj instanceof IFileStoreFile) {
			((IFileStoreFile) fobj).readAllBinary(new FuncCallback<Memory>() {
				@Override
				public void callback() {
					if (this.hasErrors())
						task.resume();
					else
						addAttach.accept(this.getResult());
				}
			});
		}

		return WorkStep.WAIT;
	}

	public WorkStep sendEmail(TaskRun task) {
		XElement settings = CatalogUtil.getSettings("Email");

		if (settings == null) {
			task.error("Missing email settings");
			return this.FINIALIZE;
		}

		String smtpHost = settings.getAttribute("SmtpHost");
		int smtpPort = (int) StringUtil.parseInt(
				settings.getAttribute("SmtpPort"), 587);
		String smtpUsername = settings.getAttribute("SmtpUsername");
		String smtpPassword = settings.hasAttribute("SmtpPassword") 
				? Hub.instance.getClock().getObfuscator().decryptHexToString(settings.getAttribute("SmtpPassword"))
				: null;

		InternetAddress[] recip = Stream.concat(Arrays.stream(this.toaddrs),
				Arrays.stream(this.dbgaddrs)).toArray(InternetAddress[]::new);

		if (!task.hasErrors() && (recip.length > 0)) {
			Transport t = null;

			try {
				this.email.saveChanges();

				t = this.email.getSession().getTransport("smtp");

				t.connect(smtpHost, smtpPort, smtpUsername, smtpPassword);

				t.sendMessage(email, recip);

				t.close();

				// TODO wish we could get INFO: Received successful response:
				// 200, AWS Request ID: b599ca95-bc82-11e0-846a-ab5fa57d84d4
			} 
			catch (Exception x) {
				task.error(1, "dcSendMail unable to send message due to service problems.  Error: " + x);
			}

			if (t != null) {
				if (t.isConnected()) {
					try {
						t.close();
					} 
					catch (MessagingException e) {
					}
				}
			}
		}

		if (task.hasErrors())
			task.info(0, "Unable to send email to: " + this.to);
		else
			task.info(0, "Email sent to: " + this.to);

		return WorkStep.NEXT;
	}

	@Override
	protected void finalize() throws Throwable {
		this.content = null;
		this.email = null;
		
		super.finalize();
	}

	public class DebugPrintStream extends PrintStream {
		protected OperationResult or = null;

		public DebugPrintStream(OperationResult or) {
			super(new OutputStream() {
				@Override
				public void write(int b) throws IOException {
					if (b == 13)
						System.out.println();
					else
						System.out.print(HexUtil.charToHex(b));
				}
			});

			this.or = or;
		}

		@Override
		public void println(String msg) {
			or.trace(0, msg);
		}
	}
}
