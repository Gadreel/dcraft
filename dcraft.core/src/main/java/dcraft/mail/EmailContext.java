package dcraft.mail;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import dcraft.filestore.CommonPath;
import dcraft.struct.FieldStruct;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.util.StringUtil;
import dcraft.web.core.BaseContext;

public class EmailContext extends BaseContext {
	static public EmailContext forRequestParams(CommonPath template, RecordStruct params, RecordStruct dparams, Path datapath) {
		EmailContext ctx = new EmailContext();
		
		ctx.data = dparams;
		ctx.datapath = datapath;
		ctx.path = template;
		
		if (params != null) {
			if (! params.isFieldEmpty("To")) {
				ctx.to = params.getFieldAsString("To");
				ctx.putInternalParam("To", ctx.to);
			}
			
			if (! params.isFieldEmpty("From")) {
				ctx.from = params.getFieldAsString("From");
				ctx.putInternalParam("From", ctx.from);
			}
			
			if (! params.isFieldEmpty("ReplyTo")) {
				ctx.replyto = params.getFieldAsString("ReplyTo");
				ctx.putInternalParam("ReplyTo", ctx.replyto);
			}
		}
		
		if (dparams != null) {
			for (FieldStruct fld : dparams.getFields()) 
				ctx.putInternalParam(fld.getName(), Struct.objectToString(fld.getValue()));
		}
		
		return ctx;
	}

	protected String from = null;
	protected String to = null;
	protected String replyto = null;
	protected String subject = null;
	protected String html = null;
	protected String text = null;
	protected Path datapath = null;
	protected CommonPath path = null;
	protected RecordStruct data = null;
	protected List<EmailAttachment> attachments = new ArrayList<EmailAttachment>();

	public String getFrom() {
		return this.from;
	}
	
	public void setFrom(String v) {
		this.from = v;
		this.putInternalParam("From", v);
	}
	
	public String getTo() {
		return this.to;
	}
	
	public void setTo(String v) {
		this.to = v;
		this.putInternalParam("To", v);
	}
	
	public String getReplyTo() {
		return this.replyto;
	}
	
	public void setReplyTo(String v) {
		this.replyto = v;
		this.putInternalParam("ReplyTo", v);
	}
	
	public String getSubject() {
		return this.subject;
	}
	
	public void setSubject(String v) {
		this.subject = v;
		this.putInternalParam("Subject", v);
	}
	
	public String getHtml() {
		return this.html;
	}
	
	public void setHtml(String v) {
		this.html = v;
	}
	
	public String getText() {
		return this.text;
	}
	
	public void setText(String v) {
		this.text = v;
	}
	
	@Override
	public CommonPath getPath() {
		return this.path;
	}
	
	public void addAttachment(EmailAttachment v) {
		this.attachments.add(v);
	}
	
	public RecordStruct getData() {
		return this.data;
	}
	
	public RecordStruct toParams() {
		RecordStruct resp = new RecordStruct();
		
		if (StringUtil.isNotEmpty(this.from))
			resp.withField("From", this.from);
		
		if (StringUtil.isNotEmpty(this.replyto))
			resp.withField("ReplyTo", this.replyto);
		
		if (StringUtil.isNotEmpty(this.to))
			resp.withField("To", this.to);
		
		if (StringUtil.isNotEmpty(this.subject))
			resp.withField("Subject", this.subject);
		
		if (StringUtil.isNotEmpty(this.to))
			resp.withField("To", this.to);
		
		if (StringUtil.isNotEmpty(this.html))
			resp.withField("Body", this.html);
		
		if (StringUtil.isNotEmpty(this.text))
			resp.withField("TextBody", this.text);
				
		if (this.attachments.size() > 0) {
			ListStruct atlist = new ListStruct();
			
			for (EmailAttachment at : this.attachments)
				atlist.addItem(at.toParams());
			
			resp.withField("Attachments", atlist);
		}
		
		return resp;
	}
	
}
