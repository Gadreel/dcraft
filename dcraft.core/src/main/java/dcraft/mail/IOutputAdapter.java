package dcraft.mail;

import dcraft.filestore.CommonPath;
import dcraft.io.CacheFile;
import dcraft.lang.op.OperationCallback;

public interface IOutputAdapter {
	void init(EmailContext ctx, CacheFile file, CommonPath web);
	void execute(EmailContext ctx, OperationCallback callback) throws Exception;
}
