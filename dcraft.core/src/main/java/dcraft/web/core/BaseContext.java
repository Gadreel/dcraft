package dcraft.web.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import org.joda.time.LocalDate;

import dcraft.hub.SiteInfo;
import dcraft.hub.TenantInfo;
import dcraft.lang.op.OperationContext;
import dcraft.locale.Tr;
import dcraft.util.StringUtil;
import dcraft.web.WebModule;
import dcraft.web.md.Configuration;
import dcraft.web.md.ProcessContext;
import dcraft.web.md.plugin.GallerySection;
import dcraft.web.md.plugin.HtmlSection;
import dcraft.web.md.plugin.PairedMediaSection;
import dcraft.web.md.plugin.StandardSection;
import dcraft.xml.XElement;

abstract public class BaseContext implements IOutputContext {
	protected Map<String, String> innerparams = new HashMap<String, String>();
	protected boolean preview = false;

	@Override
	public void putInternalParam(String name, String value) {
		this.innerparams.put(name, value);
	}

	@Override
	public boolean hasInternalParam(String name) {
		return this.innerparams.containsKey(name);
	}

	@Override
	public String getInternalParam(String name) {
		return this.innerparams.get(name);
	}

	@Override
	public IOutputMacro getMacro(String name) {
		return this.getSite().getWebsite().getMacro(name);
	}

	@Override
	public String expandMacros(String value) {
		if (StringUtil.isEmpty(value))
			return null;

		boolean checkmatches = true;

		while (checkmatches) {
			checkmatches = false;
			Matcher m = WebModule.macropatten.matcher(value);

			while (m.find()) {
				String grp = m.group();

				String macro = grp.substring(1, grp.length() - 1);

				String val = this.expandMacro(macro);

				// if any of these, then replace and check (expand) again
				if (val != null) {
					value = value.replace(grp, val);
					checkmatches = true;
				}
			}
		}

		return value;
	}

	// if the macro name is recognized then hide if no match, but otherwise
	// don't
	@Override
	public String expandMacro(String macro) {
		String[] parts = macro.split("\\|");

		// params on this tree
		if ("param".equals(parts[0]) && (parts.length > 1)) {
			String val = this.getExternalParam(parts[1]);

			return (val == null) ? "" : val;
		} 
		else if ("val".equals(parts[0]) && (parts.length > 1)) {
			String vname = parts[1];
			
			if ("PageTitle".equals(vname))
				return "Unknown";
			
			return null;
		} 
		else if ("ctx".equals(parts[0]) && (parts.length > 1)) {
			String vname = parts[1];

			String val = this.getInternalParam(vname);

			if (val == null) {
				XElement web = this.getSite().getWebsite().getWebConfig();

				if (vname.equals("SiteAuthor")) {
					if ((web != null) && (web.hasAttribute(vname))) {
						val = web.getRawAttribute(vname);
					} 
					else if ((web != null) && (web.hasAttribute("SiteTitle"))) {
						val = web.getRawAttribute("SiteTitle");
					}
					else {
						val = this.getSite().getSettings().getRawAttribute("Title");
					}
				}
				else if (vname.equals("SiteTitle")) {
					if ((web != null) && (web.hasAttribute(vname))) {
						val = web.getRawAttribute(vname);
					} 
					else {
						val = this.getSite().getSettings().getRawAttribute("Title");
					}
				}
				else if (vname.equals("SiteUrl")) {
					if ((web != null) && (web.hasAttribute("IndexUrl"))) {
						val = web.getRawAttribute("IndexUrl");
					} 
				}
				else if (vname.equals("SiteCopyright")) {
					if ((web != null) && (web.hasAttribute(vname)))
						val = web.getRawAttribute(vname);
					else
						val = "" + new LocalDate().getYear();
				}

				// if not a web setting, perhaps a user field?
				else if (vname.equals("dcUserFullname")) {
					val = OperationContext.get().getUserContext().getFullName();
				}
			}

			return (val == null) ? "" : val;
		}
		// definitions in the dictionary
		else if ("tr".equals(parts[0])) {
			String val = null;

			if ((parts.length > 1) && (StringUtil.isDataInteger(parts[1])))
				parts[1] = "_code_" + parts[1];

			if (parts.length > 2) {
				String[] params = Arrays
						.copyOfRange(parts, 2, parts.length - 2);
				val = Tr.tr(parts[1], (Object) params); // TODO test this
			} 
			else if (parts.length > 1) {
				val = Tr.tr(parts[1]);
			}

			return (val == null) ? "" : val;
		} 
		else {
			IOutputMacro macroproc = this.getMacro(parts[0]);

			if (macroproc != null) {
				String val = macroproc.process(this, parts);

				return (val == null) ? "" : val;
			}
		}

		return null;
	}

	// TODO enhance how plugins are loaded
	public ProcessContext getMarkdownContext() {
		Configuration cfg = new Configuration().setSafeMode(false)
				.registerPlugins(new PairedMediaSection(),
						new StandardSection(), new GallerySection(),
						new HtmlSection());

		return new ProcessContext(cfg, this);
	}

	public ProcessContext getSafeMarkdownContext() {
		Configuration cfg = new Configuration();

		return new ProcessContext(cfg, this);
	}

	@Override
	public TenantInfo getTenant() {
		return OperationContext.get().getTenant();
	}

	@Override
	public SiteInfo getSite() {
		return OperationContext.get().getSite();
	}

	@Override
	public boolean isPreview() {
		return this.preview;
	}

	// external and internal the same unless overridden
	@Override
	public boolean hasExternalParam(String name) {
		return this.hasInternalParam(name);
	}

	@Override
	public String getExternalParam(String name) {
		return this.getInternalParam(name);
	}
}
