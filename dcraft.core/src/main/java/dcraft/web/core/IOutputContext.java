package dcraft.web.core;

import dcraft.hub.TenantInfo;
import dcraft.hub.SiteInfo;
import dcraft.web.md.ProcessContext;

public interface IOutputContext {
    TenantInfo getTenant();
    SiteInfo getSite();
    boolean isPreview();
    
    IOutputMacro getMacro(String name);    
    String expandMacros(String value);
    String expandMacro(String macro);
    
    ProcessContext getMarkdownContext();
    ProcessContext getSafeMarkdownContext();
    
    void putInternalParam(String name, String value);
	boolean hasInternalParam(String name);
	String getInternalParam(String name);
    
	boolean hasExternalParam(String name);
	String getExternalParam(String name);
}
