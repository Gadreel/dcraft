package dcraft.web.md.plugin;

import java.util.List;
import java.util.Map;

import dcraft.struct.FieldStruct;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.util.StringUtil;
import dcraft.web.md.Plugin;
import dcraft.web.md.ProcessContext;
import dcraft.web.ui.UIElement;

public class MediaSection extends Plugin {
	public MediaSection() {
		super("MediaSection");
	}

	@Override
	public void emit(ProcessContext ctx, UIElement parent, List<String> lines, Map<String, String> params) {
		UIElement pel = new dcraft.web.ui.tags.MediaSection();
			
		if (params.containsKey("Id"))
			pel.withAttribute("id", params.get("Id"));
		
		if (params.containsKey("Lang"))
			pel.withAttribute("lang", params.get("Lang"));
		
		if (params.containsKey("Class"))
			pel.withClass(params.get("Class").split(" "));
		
		if (params.containsKey("Hidden"))
			pel.withAttribute("Hidden", params.get("Hidden"));
		
		if (params.containsKey("Variant"))
			pel.withAttribute("Variant", params.get("Variant"));

		if (params.containsKey("ListId"))
			pel.withAttribute("ListId", params.get("ListId"));
		
		if (params.containsKey("Title"))
			pel.withAttribute("Title", params.get("Title"));

		if (params.containsKey("Max"))
			pel.withAttribute("Max", params.get("Max"));
		
        StringBuilder in = new StringBuilder(); 
        
        for (String n : lines) 
          in.append(n).append("\n"); 
         
        String template = in.toString(); 		
        String data = null;
        
        int splitpos = template.indexOf("\n---\n");

        if ((splitpos == -1) && template.startsWith("---\n")) {
        	data = template.substring(5);
        	template = "";
        }
        if (splitpos != -1) {
        	data = template.substring(splitpos + 6);
        	template = template.substring(0, splitpos);
        }
        
        pel.with(new UIElement("TextTemplate").withText(template));
        
		parent.with(pel);
		
		if (StringUtil.isNotEmpty(data)) {
			ListStruct dlist = Struct.objectToList(data);
			
			for (Struct dstr : dlist.getItems()) {
				UIElement mel = new UIElement("Media");
				
				RecordStruct drec = (RecordStruct) dstr;
				
				for (FieldStruct fld : drec.getFields()) {
					if ("Fields".equals(fld.getName())) {
						ListStruct flist = Struct.objectToList(fld.getValue());
						
						if (flist != null) {
							for (Struct fstr : flist.getItems()) {
								UIElement fel = new UIElement("Field");
								
								fel
									.withAttribute("Name", ((RecordStruct) fstr).getFieldAsString("Name"))
									.withAttribute("Value", ((RecordStruct) fstr).getFieldAsString("Value"));
								
								mel.with(fel);
							}
						}
					}
					else {
						mel.setAttribute(fld.getName(), Struct.objectToString(fld.getValue()));
					}
				}
				
				pel.with(mel);
			}
		}
	}
}
