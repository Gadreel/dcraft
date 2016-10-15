package dcraft.web.core;

import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;

public interface GalleryImageConsumer {
	void accept(RecordStruct meta, RecordStruct show, Struct img);
}
