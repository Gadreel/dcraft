package dcraft.web.core;

import dcraft.filestore.CommonPath;
import dcraft.hub.SiteInfo;
import dcraft.io.CacheFile;

public interface IOutputAdapter {
	CacheFile getFile();
	boolean isAuthorized();
	
	// TODO there isn't much point in separating these two - combine
	void init(SiteInfo site, CacheFile file, CommonPath loc, boolean isPreview);
	void execute(IOutputContext ctx) throws Exception;
}
