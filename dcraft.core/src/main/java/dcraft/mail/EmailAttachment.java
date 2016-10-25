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

import java.nio.file.Path;

import dcraft.lang.Memory;
import dcraft.lang.op.OperationContext;
import dcraft.struct.FieldStruct;
import dcraft.struct.RecordStruct;
import dcraft.util.IOUtil;
import dcraft.util.MimeUtil;
import dcraft.util.StringUtil;

public class EmailAttachment {
	static public EmailAttachment forFile(Path file) {
		return EmailAttachment.forFile(file, null, null);
	}
	
	static public EmailAttachment forFile(Path file, String name) {
		return EmailAttachment.forFile(file, name, null);
	}
	
	static public EmailAttachment forFile(Path file, String name, String mime) {
		if (file == null)
			return null;
		
		if (StringUtil.isEmpty(name))
			name = file.getFileName().toString();
		
		if (StringUtil.isNotEmpty(mime))
			mime = OperationContext.get().getSite().getMimeType(MimeUtil.getMimeTypeForFile(name));
		
		return new EmailAttachment()
			.withFile(file)
			.withName(name)
			.withMime(mime);
	}
	
	static public EmailAttachment forEmbedFile(Path file) {
		if (file == null)
			return null;
		
		return EmailAttachment.forMemory(IOUtil.readEntireFileToMemory(file), file.getFileName().toString(), null);
	}
	
	static public EmailAttachment forEmbedFile(Path file, String name) {
		return EmailAttachment.forMemory(IOUtil.readEntireFileToMemory(file), name, null);
	}
	
	static public EmailAttachment forEmbedFile(Path file, String name, String mime) {
		return EmailAttachment.forMemory(IOUtil.readEntireFileToMemory(file), name, mime);
	}
	
	static public EmailAttachment forMemory(Memory content, String name) {
		return EmailAttachment.forMemory(content, name, null);
	}
	
	static public EmailAttachment forMemory(Memory content, String name, String mime) {
		if (content == null)
			return null;
		
		if (StringUtil.isEmpty(name))
			return null;
		
		if (StringUtil.isNotEmpty(mime))
			mime = OperationContext.get().getSite().getMimeType(MimeUtil.getMimeTypeForFile(name));
		
		return new EmailAttachment()
			.withContent(content)
			.withName(name)
			.withMime(mime);
		
	}
	
	protected String name = null;
	protected String mime = null;
	protected Path file = null;
	protected Memory mem = null;
	
	public String getName() {
		return this.name;
	}
	
	public String getMime() {
		return this.mime;
	}
	
	public Path getFile() {
		return this.file;
	}
	
	public Memory getContent() {
		if (this.mem != null)
			return this.mem;
		
		if (this.file != null)
			return IOUtil.readEntireFileToMemory(this.file);
		
		return null;
	}
	
	public EmailAttachment() {
	}
	
	public EmailAttachment withName(String v) {
		this.name = v;
		return this;
	}
	
	public EmailAttachment withMime(String v) {
		this.mime = v;
		return this;
	}
	
	public EmailAttachment withFile(Path v) {
		this.file = v;
		return this;
	}
	
	public EmailAttachment withContent(Memory v) {
		this.mem = v;
		return this;
	}
	
	public RecordStruct toParams() {
		if (this.mem != null)
			return new RecordStruct(
					new FieldStruct("Name", this.name),
					new FieldStruct("Mime", this.mime),
					new FieldStruct("Content", this.mem)
			);
		
		if (this.file != null)
			return new RecordStruct(
					new FieldStruct("Name", this.name),
					new FieldStruct("Mime", this.mime),
					new FieldStruct("File", this.file.toAbsolutePath().toString())
			);
		
		return null;
	}
}
