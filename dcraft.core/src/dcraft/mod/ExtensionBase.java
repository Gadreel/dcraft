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
package dcraft.mod;

import dcraft.bus.IService;
import dcraft.hub.Hub;
import dcraft.xml.XElement;

public class ExtensionBase implements IExtension {
	protected long starttime = System.currentTimeMillis();
	protected ExtensionLoader loader = null;
	
	@Override
	public void setLoader(ExtensionLoader v) {
		this.loader = v;
	}

	@Override
	public ExtensionLoader getLoader() {
		return this.loader;
	}

	public long startTime() {
		return this.starttime;
	}

	@Override
	public void init(XElement config) {
	}

	// if an extension happens to be a service - well this is handled for ya
	public String serviceName() {
		return this.getLoader().getName();
	}

	@Override
	public void start() {
		// if some subclass of this extension happens to be a Service, we'll register it automatically
		if (this instanceof IService) 
			Hub.instance.getBus().getLocalHub().registerService((IService)this);		
	}

	@Override
	public void stop() {
		// TODO
		// if some subclass of this extension happens to be a Service, we'll register it automatically
		//if (this instanceof IService) 
		//	Hub.instance.getBus().getLocalHub().unregisterService((IService)this);		
	}
}
