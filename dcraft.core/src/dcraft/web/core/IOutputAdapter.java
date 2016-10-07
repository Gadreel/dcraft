package dcraft.web.core;

import dcraft.filestore.CommonPath;
import dcraft.hub.SiteInfo;
import dcraft.io.CacheFile;

public interface IOutputAdapter {
	CacheFile getFile();
	boolean isAuthorized();
	void init(SiteInfo site, CacheFile file, CommonPath loc, boolean isPreview);
	void execute(WebContext ctx) throws Exception;
}
