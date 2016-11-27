package dcraft.web.ui;

import java.lang.ref.WeakReference;

import dcraft.web.ui.tags.MixIn;

public interface IExpandHelper {
	void expand(MixIn mixIn, WeakReference<UIWork> work);
}
