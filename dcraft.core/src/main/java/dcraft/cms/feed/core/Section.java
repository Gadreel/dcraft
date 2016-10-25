package dcraft.cms.feed.core;

import java.util.Map;

import dcraft.lang.op.OperationContext;
import dcraft.struct.FieldStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.util.IOUtil;
import dcraft.util.StringUtil;
import dcraft.web.md.process.Emitter;

public class Section {
	static public Section parseSection(String[] lines, int offset) {
		if ((lines == null) || (lines.length <= offset))
			return null;
		
		// check for plain white space only
		boolean fndcontent = false;
		
		for (int i = offset; i < lines.length; i++) {
			if (StringUtil.isNotEmpty(lines[i])) {
				offset = i;
				fndcontent = true;
				break;
			}
		}
		
		if (!fndcontent)
			return null;
		
		// OK, some content exists lets pull it together
		Section sec = new Section();
		sec.start = offset;

		// first line
		String line = lines[offset];
		
		// if not a section then default
		if (!line.startsWith("%%%")) {
			sec.id = "sectAuto" + StringUtil.buildSecurityCode();
			sec.plugin = "StandardSection";
			
			// section has content in it, collect it up
			sec.content = new StringBuilder(line);
			
			for (int i = offset + 1; i < lines.length; i++) {
				line = lines[i];
				
				if (line.startsWith("%%%")) {
					sec.end = i - 1;
					break;
				}
				
				sec.content.append(line + "\n");
			}
		}
		else {
			int pos = line.indexOf(' ');
			
			if (pos == -1) {
				OperationContext.get().error("Section header syntax error.");
				return null;
			}
			
			String meta = line.substring(pos).trim();
			
			if (StringUtil.isEmpty(meta)) {
				OperationContext.get().error("Section header syntax error.");
				return null;
			}
			
			int iow = meta.indexOf(' '); 
			
			if (iow == -1) {
				sec.plugin = meta;
			}
			else {
				sec.plugin = meta.substring(0, iow);
				
				Map<String, String> params = Emitter.parsePluginParams(meta.substring(iow + 1));
				
				sec.attrs = new RecordStruct();
				
				for (String key : params.keySet()) {
					if ("Id".equals(key))
						sec.id = params.get(key);
					else
						sec.attrs.setField(key, params.get(key));
				}
			}
			
			if (!IOUtil.isLegalFilename(sec.plugin) || sec.plugin.contains("%")) {
				OperationContext.get().error("Section plugin type error.");
				return null;
			}
			
			if (meta.endsWith("%%%")) {
				sec.end = offset;
				return sec;
			}
			
			// section has content in it, collect it up
			sec.content = new StringBuilder();
			
			for (int i = offset + 1; i < lines.length; i++) {
				line = lines[i];
				
				if (line.startsWith("%%%")) {
					sec.end = i;
					break;
				}
				
				sec.content.append(line + "\n");
			}
		}
		
		// collected all lines
		if (sec.end == -1)
			sec.end = lines.length - 1;
		
		// remove excess line break at start
		if ((sec.content.length() > 0) && (sec.content.charAt(0) == '\n'))
			sec.content.deleteCharAt(0);
		
		// remove excess line break at end
		if ((sec.content.length() > 1) && (sec.content.charAt(sec.content.length() - 1) == '\n') && (sec.content.charAt(sec.content.length() - 2) == '\n'))
			sec.content.deleteCharAt(sec.content.length() - 1);
		
		return sec;
	}
	
	static public Section parseRecord(RecordStruct rec) {
		Section sec = new Section();

		sec.id = rec.getFieldAsString("Id");
		sec.plugin = rec.getFieldAsString("Plugin");
		sec.attrs = rec.getFieldAsRecord("Params");
		
		if (!rec.isFieldEmpty("Content"))
			sec.content = new StringBuilder(rec.getFieldAsString("Content"));
		
		return sec;
	}
	
	protected String plugin = null;
	protected String id = null;
	protected RecordStruct attrs = null;
	protected StringBuilder content = null;
	protected int start = -1;
	protected int end = -1;
	
	public String getPlugin() {
		return this.plugin;
	}
	
	public String getId() {
		return this.id;
	}
	
	public CharSequence getContent() {
		return this.content;
	}
	
	public RecordStruct getAttrs() {
		return this.attrs;
	}
	
	protected Section() {
		
	}
	
	public void update(RecordStruct rec) {
		if (!rec.isFieldEmpty("Id"))
			this.id = rec.getFieldAsString("Id");
		
		if (!rec.isFieldEmpty("Plugin"))
			this.plugin = rec.getFieldAsString("Plugin");

		if (!rec.isFieldEmpty("Content"))
			this.content = new StringBuilder(rec.getFieldAsString("Content"));
		
		if (!rec.isFieldEmpty("Params"))
			this.attrs = rec.getFieldAsRecord("Params");
		
		// Id is not allowed this way
		if (this.attrs != null)
			this.attrs.removeField("Id");
	}
	
	@Override
	public String toString() {
		StringBuilder content = new StringBuilder();
		
		this.write(content);
		
		return content.toString();
	}
	
	public void write(StringBuilder content) {
		content.append("%%% ");
		content.append(this.plugin);
		
		if (StringUtil.isEmpty(this.id))
			this.id = "sectAuto" + StringUtil.buildSecurityCode();
		
		content.append(" Id=\"");
		content.append(this.id);  // TODO escape value
		content.append("\"");
		
		if (this.attrs != null) {
			for (FieldStruct fld : this.attrs.getFields()) {
				String val = Struct.objectToString(fld.getValue());
				
				if (StringUtil.isEmpty(val))
					continue;
				
				content.append(' ');
				content.append(fld.getName());
				content.append("=\"");
				content.append(val);  // TODO escape value
				content.append("\"");
			}
		}
		
		if (StringUtil.isEmpty(this.content)) {
			content.append(" %%%\n\n");
			return;
		}
		
		content.append('\n');
		content.append('\n');
		
		content.append(this.content);
		content.append('\n');

		// add extra only if no LF at end at all
		if (this.content.charAt(this.content.length() - 1) != '\n')
			content.append('\n');
		
		content.append("%%%%%%%%%%%%%% end section %%%%%%%%%%%%%%\n");
	}
}
