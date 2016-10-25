package dcraft.filestore;

import dcraft.ctp.CtpAdapter;
import dcraft.filestore.select.FileSelection;

public interface IFileSelector extends IFileCollection {
	FileSelection selection();
	void read(CtpAdapter adapter);
}
