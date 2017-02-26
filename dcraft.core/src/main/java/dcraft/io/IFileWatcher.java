package dcraft.io;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

public interface IFileWatcher {
	void fireFolderEvent(Path file, WatchEvent.Kind<Path> kind);
}
