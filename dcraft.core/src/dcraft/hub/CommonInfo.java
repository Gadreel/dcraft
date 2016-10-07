package dcraft.hub;

import java.nio.file.Path;

import dcraft.io.CacheFile;
import dcraft.util.StringUtil;

abstract public class CommonInfo {
	abstract public String getAlias();

	abstract public Path getPath();

	abstract public CacheFile resolveCachePath(String path);
	
	public Path resolvePath(String path) {
		Path base = this.getPath();
		
		if (StringUtil.isEmpty(path))
			return base;
		
		if (path.charAt(0) == '/')
			return base.resolve(path.substring(1));
		
		return base.resolve(path);
	}
	
	public String relativize(Path path) {
		if (path == null)
			return null;
		
		path = path.normalize().toAbsolutePath();
		
		if (path == null)
			return null;
		
		String rpath = path.toString().replace('\\', '/');
		
		String dpath = this.getPath().toString().replace('\\', '/');
		
		if (!rpath.startsWith(dpath))
			return null;
		
		return rpath.substring(dpath.length());
	}
}
