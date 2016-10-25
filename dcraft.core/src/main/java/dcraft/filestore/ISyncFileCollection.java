package dcraft.filestore;

import dcraft.lang.op.FuncResult;

public interface ISyncFileCollection extends IFileCollection {
	FuncResult<IFileStoreFile> next();
}
