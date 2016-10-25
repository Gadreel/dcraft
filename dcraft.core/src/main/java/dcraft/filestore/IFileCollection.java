package dcraft.filestore;

import dcraft.lang.op.FuncCallback;
import dcraft.script.StackEntry;
import dcraft.xml.XElement;

public interface IFileCollection {
	CommonPath path();		
	void next(FuncCallback<IFileStoreFile> callback);
	void forEach(FuncCallback<IFileStoreFile> callback);
	
	// scripts
	public void operation(final StackEntry stack, XElement code);
}
