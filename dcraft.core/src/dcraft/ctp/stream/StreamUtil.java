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
package dcraft.ctp.stream;

import java.nio.file.Path;

import dcraft.filestore.CommonPath;
import dcraft.filestore.local.FileSystemDriver;
import dcraft.filestore.local.FileSystemFile;
import dcraft.util.FileUtil;
import dcraft.util.StringUtil;

public class StreamUtil {
	static public StreamWork composeStream(IStream... steps) {
		if (steps.length < 2)
			throw new IllegalArgumentException("Stream steps must contain a source and destination step");
		
		if (!(steps[0] instanceof IStreamSource))
			throw new IllegalArgumentException("Stream steps must contain a source as the first step");
		
		if (!(steps[steps.length - 1] instanceof IStreamDest))
			throw new IllegalArgumentException("Stream steps must contain a destination as the last step");
		
		IStream last = null;

		// skip any null stream in the middle
		for (int i = 0; i < steps.length; i++) {
			IStream curr = steps[i];
			
			if (curr == null)
				continue;
			
			if (last != null)
				curr.setUpstream(last);
			
			last = curr;
		}
		
		return new StreamWork((IStreamDest) last);
	}
	
	static public FileSystemFile localFile(Path lpath) {
	    FileSystemDriver drv = new FileSystemDriver(lpath.getParent());
	    return new FileSystemFile(drv, new CommonPath("/" + lpath.getFileName().toString()), false);
	}
	
	static public FileSystemFile localFolder(Path lpath) {
	    FileSystemDriver drv = new FileSystemDriver(lpath.getParent());
	    return new FileSystemFile(drv, new CommonPath("/" + lpath.getFileName().toString()), true);
	}
	
	static public FileSystemDriver localDriver(Path lpath) {
	    return new FileSystemDriver(lpath.getParent());
	}
	
	static public FileSystemFile tempFile(String ext) {
        CommonPath path = new CommonPath("/" + (StringUtil.isNotEmpty(ext) ? FileUtil.randomFilename(ext) : FileUtil.randomFilename()));
        
        Path tfpath = FileUtil.allocateTempFolder2();

        FileSystemDriver drv = new FileSystemDriver(tfpath);
        drv.isTemp(true);
        
        return new FileSystemFile(drv, path, false);
	}
	
	static public FileSystemFile tempFolder() {
        Path path = FileUtil.allocateTempFolder2();

        FileSystemDriver drv = new FileSystemDriver(path);
        drv.isTemp(true);
        
        return new FileSystemFile(drv, CommonPath.ROOT, true);
	}

}
