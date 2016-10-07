package dcraft.ctp.stream;

import dcraft.script.StackEntry;
import dcraft.xml.XElement;

public interface IStreamUp extends IStream {
	void init(StackEntry stack, XElement el);
}
