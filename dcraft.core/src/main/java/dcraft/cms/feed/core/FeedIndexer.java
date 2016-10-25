package dcraft.cms.feed.core;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import dcraft.hub.SiteInfo;
import dcraft.hub.TenantInfo;
import dcraft.lang.CountDownCallback;
import dcraft.lang.op.OperationCallback;
import dcraft.lang.op.OperationContext;
import dcraft.log.Logger;
import dcraft.util.StringUtil;
import dcraft.xml.XElement;

/*
 * TODO whole class needs review
 */
public class FeedIndexer {
	public static XElement findChannel(String site, String channel) {
		if (StringUtil.isEmpty(channel))
			return null;
		
		if (StringUtil.isEmpty(site))
			site = "root";
			
		TenantInfo tenant = OperationContext.get().getTenant();
		
		SiteInfo siteinfo = tenant.resolveSiteInfo(site);
		
		XElement feed = siteinfo.getSettings().find("Feed");
		
		if (feed == null) 
			return null;
		
		for (XElement chan : feed.selectAll("Channel")) { 
			String calias = chan.getAttribute("Alias");
			
			if (calias == null)
				calias = chan.getAttribute("Name");
			
			if (!calias.equals(channel)) 
				continue;
			
			// TODO check if site name is allowed in this channel - right now we act as if all sites use the same list
			
			return chan;
		}
		
		return null;
	}
	
	public static List<XElement> findChannels(String site) {
		List<XElement> list = new ArrayList<>();
		
		TenantInfo tenant = OperationContext.get().getTenant();
		
		SiteInfo siteinfo = tenant.resolveSiteInfo(site);
		
		XElement feed = siteinfo.getSettings().find("Feed");
		
		if (feed == null) 
			return list;
		
		for (XElement chan : feed.selectAll("Channel")) { 
			String calias = chan.getAttribute("Alias");
			
			if (calias == null)
				calias = chan.getAttribute("Name");
			
			// TODO check if site name is allowed in this channel - right now we act as if all sites use the same list
			
			list.add(chan);
		}
		
		return list;
	}
	
	protected Map<String, FeedInfo> feedpaths = new HashMap<>();

	/*
	 * run collectTenant first
	 */
	public void importTenant(OperationCallback op) {
		CountDownCallback cd = new CountDownCallback(this.feedpaths.size() + 1, new OperationCallback() {
			@Override
			public void callback() {
				// =============== DONE ==============
				if (op.hasErrors()) 
					op.info("Website import completed with errors!");
				else
					op.info("Website import completed successfully");
				
				op.complete();
			}
		});
		
		for (FeedInfo fi : this.feedpaths.values())
			fi.updateDb(new OperationCallback() {				
				@Override
				public void callback() {
					cd.countDown();
				}
			});
		
		cd.countDown();
	}
	
	public void collectTenant(CollectContext cctx) {
		XElement del = OperationContext.get().getTenant().getSettings();
		
		if (del == null) {
			Logger.error("Unable to import domain, settings not found");
			return;
		}
		
		Logger.info("Importing web content for domain: " + del.getAttribute("Title", "[unknown]"));
		
		// --------- import sub sites ----------
		
		for (XElement site : del.selectAll("Site")) {
			String sname = site.getAttribute("Name");
			
			for (XElement chan : FeedIndexer.findChannels(sname)) 
				this.collectChannel(cctx, sname, chan);
		}
		
		// --------- import root site ----------
		
		for (XElement chan : FeedIndexer.findChannels("root")) 
			this.collectChannel(cctx, "root", chan);
		
		Logger.info("File count collected for import: " + this.feedpaths.size());
	}
	
	public void collectSite(CollectContext cctx, String site) {
		XElement del = OperationContext.get().getTenant().resolveSiteInfo(site).getSettings();
		
		Logger.info("Importing web content for domain: " + del.getAttribute("Title", "[unknown]") + " site: " + site);
		
		for (XElement chan : FeedIndexer.findChannels(site)) 
			this.collectChannel(cctx, site, chan);
		
		Logger.info("File count collected for import: " + this.feedpaths.size());
	}
	
	public void collectChannel(CollectContext cctx, String site, XElement chan) {
		String alias = chan.getAttribute("Alias");
		
		if (alias == null)
			alias = chan.getAttribute("Name");
		
		// pages and blocks do not index the same way for public
		if (cctx.isForSitemap() && ("Pages".equals(alias) || "Blocks".equals(alias) || !chan.getAttribute("AuthTags", "Guest").contains("Guest")))
			return;
		
		if (cctx.isForIndex() && "true".equals(chan.getAttribute("DisableIndex", "False").toLowerCase()))
			return;
		
		Logger.info("Importing site content for: " + site + " > " + alias);
		
		this.collectArea(site, "feed", alias, false);
		
		if (!cctx.isForSitemap())
			this.collectArea(site, "feed", alias, true);
	}
	
	public void collectArea(String site, String area, String channel, boolean preview) {
		TenantInfo tenant = OperationContext.get().getTenant();
		
		SiteInfo siteinfo = tenant.resolveSiteInfo(site);

		String wwwpathf1 = preview ? area +  "-preview/" + channel : area +  "/" + channel;
		
		Path wwwsrc1 = siteinfo.resolvePath(wwwpathf1).toAbsolutePath().normalize();
		
		if (!Files.exists(wwwsrc1)) 
			return;
		
		try {
			Files.walkFileTree(wwwsrc1, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path sfile, BasicFileAttributes attrs) {
					Path relpath = wwwsrc1.relativize(sfile);
					
					String outerpath = "/" + relpath.toString().replace('\\', '/');

					// only collect dcf files
					if (!outerpath.endsWith(".dcf.xml")) 
						return FileVisitResult.CONTINUE;
					
					// TODO if this is a Page channel then confirm that there is a corresponding .dcui.xml file - if not skip it
					
					outerpath = outerpath.substring(0, outerpath.length() - 8);
					
					XElement chel = FeedIndexer.findChannel(site, channel);
					
					if (!chel.hasAttribute("InnerPath"))
						outerpath = "/" + channel + outerpath;
					
					Logger.debug("Considering file " + channel + " > " + outerpath);

					// skip if already in the collect list
					if (FeedIndexer.this.feedpaths.containsKey(site + outerpath)) 
						return FileVisitResult.CONTINUE;
					
					// add to the list
					if (preview) 
						Logger.info("Adding preview only " + channel + " > " + outerpath);
					else 
						Logger.info("Adding published " + channel + " > " + outerpath);
						
					FeedInfo fi = FeedInfo.buildInfo(site, channel, outerpath);
					
					FeedIndexer.this.feedpaths.put(site + outerpath, fi);
					
					return FileVisitResult.CONTINUE;
				}
			});
		}
		catch (IOException x) {
			Logger.error("Error indexing channel " + area + ": " + channel + " : " + x);
		}
	}

	public void addToSitemap(String indexurl, XElement smel, List<String> altlocales) {
		DateTimeFormatter lmFmt = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		
		for (FeedInfo fi : this.feedpaths.values()) {
			try {
				XElement sel = new XElement("url");
				
				sel.add(new XElement("loc", indexurl + fi.getOuterPath().substring(1)));
				sel.add(new XElement("lastmod", lmFmt.print(Files.getLastModifiedTime(fi.getPubpath()).toMillis())));

				for (String lname : altlocales)
					sel.add(new XElement("xhtml:link")
						.withAttribute("rel", "alternate")
						.withAttribute("hreflang", lname)
						.withAttribute("href", indexurl + lname + fi.getOuterPath())
					);
				
				smel.add(sel);
			}
			catch (Exception x) {
				Logger.error("Unable to add " + fi.getInnerPath() + ": " + x);
			}
		}
	}
	
	

	/*
	 * run collectDomain first
	 */
	public void importDomain(OperationCallback op) {
		CountDownCallback cd = new CountDownCallback(this.feedpaths.size() + 1, new OperationCallback() {
			@Override
			public void callback() {
				// =============== DONE ==============
				if (op.hasErrors()) 
					op.info("Website import completed with errors!");
				else
					op.info("Website import completed successfully");
				
				op.complete();
			}
		});
		
		for (FeedInfo fi : this.feedpaths.values())
			fi.updateDb(new OperationCallback() {				
				@Override
				public void callback() {
					cd.countDown();
				}
			});
		
		cd.countDown();
	}
	
	public void collectDomain(CollectContext cctx) {
		XElement web = OperationContext.get().getSite().getWebsite().getWebConfig();
		
		if (web == null) {
			Logger.error("Unable to import domain, web settings not found");
			return;
		}
		
		Logger.info("Importing web content for domain: " + OperationContext.get().getTenant().getAlias());
		
		// TODO improve collection process
		
		// --------- import sub sites ----------
		
		for (XElement site : web.selectAll("Site")) {
			String sname = site.getAttribute("Name");
			
			// skip "root", it comes below
			if ("root".equals(sname))
				continue;
			
			for (XElement chan : FeedIndexer.findChannels(sname)) 
				this.collectChannel(cctx, sname, chan);
		}
		
		// --------- import root site ----------
		
		for (XElement chan : FeedIndexer.findChannels("root")) 
			this.collectChannel(cctx, "root", chan);
		
		Logger.info("File count collected for import: " + this.feedpaths.size());
	}
	
}