package dcraft.cms.util;

import java.lang.ref.WeakReference;

import dcraft.web.ui.IExpandHelper;
import dcraft.web.ui.UIWork;
import dcraft.web.ui.tags.MixIn;

public class ShowImageHelper implements IExpandHelper  {
	@Override
	public void expand(MixIn mixIn, WeakReference<UIWork> work) {
		System.out.println("expand helper called");
	}
}
