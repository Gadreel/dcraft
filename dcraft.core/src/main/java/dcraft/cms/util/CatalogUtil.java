package dcraft.cms.util;

import dcraft.hub.Hub;
import dcraft.hub.SiteInfo;
import dcraft.lang.op.OperationContext;
import dcraft.util.StringUtil;
import dcraft.xml.XElement;

public class CatalogUtil {
	static public XElement getSettings(String name) {
		XElement cat = CatalogUtil.getCatalog(name, null);
		
		if (cat != null)
			return cat.find("Settings");
		
		return null;
	}
	
	static public XElement getSettings(String name, String account) {
		XElement cat = CatalogUtil.getCatalog(name, account);
		
		if (cat != null)
			return cat.find("Settings");
		
		return null;
	}
	
	static public XElement getCatalog(String name, String account) {
		if (StringUtil.isEmpty(name))
			return null;
		
		if (StringUtil.isNotEmpty(account))
			name = name + "-" + account;
		
		SiteInfo site = OperationContext.get().getSite();
		
		XElement cat = CatalogUtil.matchCatalog(name, site.getSettings());
		
		if (cat != null)
			return cat;
		
		cat = CatalogUtil.matchCatalog(name, site.getTenant().getSettings());
		
		if (cat != null)
			return cat;
		
		cat = CatalogUtil.matchCatalog(name, Hub.instance.getConfig());
		
		if (cat != null)
			return cat;
		
		return null;
	}
	
	static public XElement matchCatalog(String name, XElement settings) {
		if (StringUtil.isEmpty(name) || (settings == null))
			return null;
		
		for (XElement cat : settings.selectAll("Catalog")) {
			if (!name.equals(cat.getAttribute("Name")))
				continue;
			
			String use = cat.getAttribute("Use", "Both");
			
			if (Hub.instance.getResources().isForTesting() && "Production".equals(use))
				continue;
			
			if (!Hub.instance.getResources().isForTesting() && "Test".equals(use))
				continue;
			
			// TODO add checks for HubId="id" Team="team" Squad
			
			return cat;
		}
		
		return null;
	}
}
