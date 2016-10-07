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

import dcraft.filestore.CommonPath;
import dcraft.filestore.IFileStoreDriver;
import dcraft.filestore.IFileStoreFile;
import dcraft.lang.op.FuncCallback;
import dcraft.lang.op.OperationContext;
import dcraft.script.StackEntry;
import dcraft.script.inst.With;
import dcraft.struct.Struct;
import dcraft.util.StringUtil;

public class File extends With {
	@Override
	public void prepTarget(StackEntry stack) {
        String name = stack.stringFromSource("Name");
        
        if (StringUtil.isEmpty(name))
        	name = "Folder_" + stack.getActivity().tempVarName();
        
        String vname = name;
        
        Struct ss = stack.refFromSource("In");
        
        if ((ss == null) || (!(ss instanceof IFileStoreDriver) && !(ss instanceof IFileStoreFile))) {
        	OperationContext.get().errorTr(536);
    		this.nextOpResume(stack);
        	return;
        }
        
        CommonPath path = null;
        
        try {
            path = new CommonPath(stack.stringFromSource("Path", "/"));
        }
        catch (Exception x) {
        	OperationContext.get().errorTr(537);
			this.nextOpResume(stack);
			return;
        }

        IFileStoreDriver drv = null;
        
        if (ss instanceof IFileStoreDriver) {
            drv = (IFileStoreDriver)ss;
        }
        else {
        	drv = ((IFileStoreFile)ss).driver();
        	path = ((IFileStoreFile)ss).resolvePath(path);
        }
        
        drv.getFileDetail(path, new FuncCallback<IFileStoreFile>() {
			@Override
			public void callback() {
				if (this.hasErrors()) {
					OperationContext.get().errorTr(538);
					File.this.nextOpResume(stack);
					return;
				}
				
	            IFileStoreFile fh = this.getResult();			            
	            
	            if (!fh.exists() && stack.getInstruction().getXml().getName().equals("Folder"))
	            	fh.isFolder(true); 
				
	            stack.addVariable(vname, (Struct)fh);
	            
	            File.this.setTarget(stack, (Struct)fh);
	            
	    		File.this.nextOpResume(stack);
			}
		});
	}
}
