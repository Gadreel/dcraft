package dcraft.hub;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import dcraft.lang.op.FuncResult;
import dcraft.util.IOUtil;
import dcraft.xml.XElement;
import dcraft.xml.XmlReader;

public class HubPackage {
	protected XElement xml = null;
	protected XElement settings = null;
	protected String name = null;

	public String getName() {
		return this.name;
	}
	
	public XElement getSettings() {
		return this.settings;
	}
	
	public XElement getXml() {
		return this.xml;
	}
	
	public void load(XElement pack) {
		this.xml = pack;
		this.name =  pack.getAttribute("Name");
		
		Path ppath = Paths.get("./packages/" + this.name);
		
		if (! Files.exists(ppath))
			return;
		
		Path pxml = ppath.resolve("package.xml");
		
		if (Files.exists(pxml)) {
			FuncResult<CharSequence> res = IOUtil.readEntireFile(pxml);
			
			if (res.isEmptyResult())
				return;
			
			FuncResult<XElement> xres = XmlReader.parse(res.getResult(), true);
			
			if (xres.isEmptyResult())
				return;
			
			this.settings = xres.getResult().find("Settings");
		}
		

		/*
		Path www = Paths.get("./packages/" + this.name + "/www/");
		
		if (! Files.exists(www))
			return;
		
		// TODO load www cache, all lower case for indexing
		 * 
		 */
	}
}
