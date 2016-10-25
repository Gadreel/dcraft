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

import java.lang.ref.WeakReference;

import dcraft.hub.Hub;
import dcraft.util.StringUtil;
import dcraft.xml.XElement;

public class ExtensionLoader {
	public static ExtensionLoader from(IModule module) {
		ExtensionLoader ext = new ExtensionLoader();
		ext.module = new WeakReference<IModule>(module);
		return ext;
	}
	
	protected IExtension extension = null;
	protected String name = null;
	protected XElement config = null;		// extension tag
	protected XElement setting = null;		// extension.settings tag
	protected WeakReference<IModule> module = null;

	public String getName() {
		return this.name;
	}
	
	public IExtension getExtension() {
		return this.extension;
	}
	
	public XElement getConfig() {
		return this.config;
	}
	
	public XElement getSettings() {
		return this.setting;
	}

	public IModule getModule() {
		if (this.module != null)
			return this.module.get();
		
		return null;
	}

	protected ExtensionLoader() {
	}
	
	public void init(XElement config) {
		try {
			this.config = config;
			this.name = config.getAttribute("Name");
			
			if (config != null) {
				this.setting = config.find("Settings");

				// after all bundles are loaded, instantiate the RunClass
				String runclass = config.getAttribute("RunClass");
	
				if (StringUtil.isNotEmpty(runclass)) {
					this.extension = (IExtension) Hub.instance.getInstance(runclass);
					
					// TODO if (this.extension == null) 
					
					this.extension.setLoader(this);
					this.extension.init(this.setting);
				}
			}
		} 
		catch (Exception x) {
			// TODO log
			System.out.println("trouble loading the extension: " + x);
		}
	}

	public void start() {
		if (this.extension != null)
			this.extension.start();
	}
	
	public void stop() {
		if (this.extension != null)
			this.extension.stop();
	}
}
