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
package dcraft.io;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import dcraft.filestore.CommonPath;
import dcraft.lang.op.OperationResult;
import dcraft.log.Logger;
import dcraft.xml.XElement;

public class LocalFileStore {
	// absolute, normalized paths in Path and String forms
	protected Path path = null;
	protected String spath = null;
	protected XElement config = null;
	
	protected Map<String, CacheFile> cache = new HashMap<>();

	public String getPath() {
		return this.spath;
	}

	public Path getFilePath() {
		return this.path;
	}
	
	public XElement getConfig() {
		return this.config;
	}

	public void clearCache() {
		this.cache = new HashMap<>();
	}
	
	public Path resolvePath(String path) {
		return this.path.resolve(path.startsWith("/") ? path.substring(1) : path).normalize().toAbsolutePath();
	}

	public Path resolvePath(Path path) {
		if (path.isAbsolute()) {
			if (path.startsWith(this.path))
				return path;
			
			return null;
		}
		
		return this.path.resolve(path).normalize().toAbsolutePath();
	}

	public Path resolvePath(CommonPath path) {
		return this.path.resolve(path.toString().substring(1)).normalize().toAbsolutePath();
	}
	
	public String relativize(Path path) {
		path = path.normalize().toAbsolutePath();
		
		if (path == null)
			return null;
		
		String rpath = path.toString().replace('\\', '/');
		
		if (!rpath.startsWith(this.spath))
			return null;
		
		return rpath.substring(this.spath.length());
	}
	
	// use CacheFile once and then let go, call this every time you need it or you may be holding on to stale content
	public CacheFile cacheResolvePath(String file) {
		Path lf = this.resolvePath(file);
		
		if (lf != null) {
			String ln = this.relativize(lf);
			
			CacheFile ra = this.cache.get(ln);
			
			if (ra != null) 
				return ra;
			
			ra = CacheFile.fromFile(ln, lf);
			
			if (ra != null) {
				this.cache.put(ln, ra);
				return ra;
			}
		}
		
		return null;
	}
	
	// use CacheFile once and then let go, call this every time you need it or you may be holding on to stale content
	public CacheFile cacheResolvePath(Path file) {
		if (Logger.isDebug())
			Logger.debug("cache resolve path: " + file);
		
		Path lf = this.resolvePath(file);
		
		if (Logger.isDebug())
			Logger.debug("cache resolve path, resolve: " + lf);
		
		if (lf != null) {
			String ln = this.relativize(lf);
			
			if (Logger.isDebug())
				Logger.debug("cache resolve path, relativize: " + ln);
			
			CacheFile ra = this.cache.get(ln);
			
			if (Logger.isDebug())
				Logger.debug("cache resolve path, cache: " + ra);
			
			if (ra != null) 
				return ra;
			
			ra = CacheFile.fromFile(ln, lf);
			
			if (Logger.isDebug())
				Logger.debug("cache resolve path, file: " + ra);
			
			//System.out.println("rcache " + this.cache);
			
			if (ra != null) {
				this.cache.put(ln, ra);
				return ra;
			}
		}
		
		return null;
	}
	
	// use CacheFile once and then let go, call this every time you need it or you may be holding on to stale content
	public CacheFile cacheResolvePath(CommonPath file) {
		Path lf = this.resolvePath(file);
		
		if (lf != null) {
			String ln = this.relativize(lf);
			
			CacheFile ra = this.cache.get(ln);
			
			if (ra != null) 
				return ra;
			
			ra = CacheFile.fromFile(ln, lf);
			
			if (ra != null) {
				this.cache.put(ln, ra);
				return ra;
			}
		}
		
		return null;
	}
	
	public boolean cacheHas(String file) {
		Path lf = this.resolvePath(file);
		
		if (lf != null) {
			String ln = this.relativize(lf);
			
			CacheFile ra = this.cache.get(ln);
			
			if (ra != null) 
				return true;
			
			ra = CacheFile.fromFile(ln, lf);
			
			if (ra != null) {
				this.cache.put(ln, ra);
				return true;
			}
		}
		
		return false;
	}	
	
	public void init(OperationResult or, XElement fstore) {
		this.config = fstore;
		
		String fpath = fstore.hasAttribute("FolderPath") 
				? fstore.getAttribute("FolderPath")
					: fstore.getName().equals("PackageFileStore")
						? "./packages"
						: "./tenants";
		
		this.path = Paths.get(fpath).normalize().toAbsolutePath();
			
		if (Files.exists(this.path) && !Files.isDirectory(this.path)) {
			or.error("File Store cannot be mounted: " + fpath);
			return;
		}
		
		try {
			Files.createDirectories(this.path);
			
			this.spath = this.path.toString().replace('\\', '/');
		} 
		catch (Exception x) {
			or.errorTr(132, x);
		}
	}
}
