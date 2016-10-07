package dcraft.cms.feed.tool;

import java.util.ArrayList;
import java.util.List;

import dcraft.cms.feed.tool.FeedAdapter.MatchResult;
import dcraft.lang.op.FuncCallback;
import dcraft.lang.op.OperationContext;
import dcraft.struct.CompositeStruct;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.util.StringUtil;
import dcraft.xml.XElement;

public class Sections {
	static public Sections parse(FeedInfo fi, String part) {
		FeedAdapter fa = fi.getAdapter(true);
		
		MatchResult partm = fa.bestMatch("PagePart", "For", part);
		
		if (partm == null) {
			OperationContext.get().error("Feed part " + part + " does not exist.");
			return null;
		}
		
		// TODO locale support
		String src = fa.getPartValue("en", partm.el, true);
		
		if (src == null) {
			OperationContext.get().error("Feed part " + part + " has no content.");
			return null;
		}
		
		Sections secs = Sections.parse(src);
		
		secs.partel = partm.el;
		secs.fi = fi;
		
		return secs;
	}
	
	static public Sections parse(String value) {
		Sections secs = new Sections();

		if (StringUtil.isEmpty(value))
			return secs;
		
		String[] lines = value.replace("\r", "").split("\n");
		
		Section sec = Section.parseSection(lines, 0);
		
		while (sec != null) {
			secs.sections.add(sec);
			
			if (sec.end == -1) 
				break;
			
			sec = Section.parseSection(lines, sec.end + 1);
		}
		
		return secs;
	}
	
	protected List<Section> sections = new ArrayList<>();
	protected FeedInfo fi = null;
	protected XElement partel = null;
	
	protected Sections() {
	}
	
	public void save(boolean publish, FuncCallback<CompositeStruct> cb) {
		String content = this.toString();
		
		ListStruct fields = new ListStruct(); 		// don't change part's fields
		
		ListStruct parts = new ListStruct()
			.withItems(new RecordStruct()
				.withField("For", this.partel.getAttribute("For"))
				.withField("Format", this.partel.getAttribute("Format"))
				.withField("Value", content.trim())
			);
		
		this.fi.saveFile(publish, fields, parts, null, cb);
	}

	public Section findSection(String section) {
		for (Section s : this.sections)
			if (s.id.equals(section))
				return s;
		
		return null;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		for (Section s : this.sections) {
			s.write(sb);
			sb.append('\n');
		}
		
		return sb.toString();
	}

	public boolean deleteSection(String target) {
		for (int i = 0; i < this.sections.size(); i++) {
			if (target.equals(this.sections.get(i).id)) {
				this.sections.remove(i);
				return true;
			}
		}
		
		OperationContext.get().error("Could not find section: " + target);
		return false;
	}

	public boolean insertAbove(String target, Section sec) {
		for (int i = 0; i < this.sections.size(); i++) {
			if (target.equals(this.sections.get(i).id)) {
				this.sections.add(i, sec);
				return true;
			}
		}
		
		OperationContext.get().error("Could not find section: " + target);
		return false;
	}

	public boolean insertBelow(String target, Section sec) {
		for (int i = 0; i < this.sections.size(); i++) {
			if (target.equals(this.sections.get(i).id)) {
				this.sections.add(i + 1, sec);
				return true;
			}
		}
		
		OperationContext.get().error("Could not find section: " + target);
		return false;
	}

	public boolean insertBottom(Section sec) {
		this.sections.add(sec);
		
		return true;
	}

	public boolean moveUp(String target, Long levels) {
		if ((levels == null) || levels < 0)
			levels = 1L;
		
		for (int i = 0; i < this.sections.size(); i++) {
			if (target.equals(this.sections.get(i).id)) {
				long al = i - levels;
				
				if (al < 0) 
					al = 0;
				
				Section sec = this.sections.remove(i);
				
				this.sections.add((int)al, sec);

				return true;
			}
		}
		
		OperationContext.get().error("Could not find section: " + target + " or it is already at the top.");
		return false;
	}

	public boolean moveDown(String target, Long levels) {
		if ((levels == null) || levels < 0)
			levels = 1L;
		
		for (int i = 0; i < this.sections.size(); i++) {
			if (target.equals(this.sections.get(i).id)) {
				long al = i + levels;
				
				if (al >= this.sections.size()) 
					al = this.sections.size() - 1;
				
				Section sec = this.sections.remove(i);
				
				this.sections.add((int)al, sec);

				return true;
			}
		}
		
		OperationContext.get().error("Could not find section: " + target + " or it is already at the bottom.");
		return false;
	}
}
