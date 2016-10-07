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
package dcraft.script.inst.file;

import java.nio.file.Path;
import java.nio.file.Paths;

import dcraft.filestore.CommonPath;
import dcraft.filestore.local.FileSystemDriver;
import dcraft.filestore.local.FileSystemFile;
import dcraft.lang.op.OperationContext;
import dcraft.script.StackEntry;
import dcraft.script.inst.With;
import dcraft.util.StringUtil;

public class LocalFolder extends With {
	@Override
	public void prepTarget(StackEntry stack) {
        String name = stack.stringFromSource("Name");
        
        if (StringUtil.isEmpty(name))
        	name = "LocalFolder_" + stack.getActivity().tempVarName();
        
        String vname = name;
        
        String path = stack.stringFromSource("Path");
        
        if (StringUtil.isEmpty(path)) {
        	OperationContext.get().errorTr(525);
			this.nextOpResume(stack);
			return;
        }
        
        Path lpath = null;
        
        try {
        	lpath = Paths.get(path);
        }
        catch (Exception x) {
        	OperationContext.get().errorTr(526, x);
			this.nextOpResume(stack);
			return;
        }

        FileSystemDriver drv = new FileSystemDriver(lpath);
        FileSystemFile fh = new FileSystemFile(drv, CommonPath.ROOT, true);
        
        stack.addVariable("LocalFS_" + stack.getActivity().tempVarName(), drv);
        stack.addVariable(vname, fh);
        
        this.setTarget(stack, fh);
		
		this.nextOpResume(stack);
	}
}
