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
import dcraft.lang.CountDownCallback;
import dcraft.lang.op.OperationCallback;
import dcraft.lang.op.OperationContext;
import dcraft.log.Logger;
import dcraft.util.StringUtil;
import dcraft.xml.XElement;

public class FeedIndexer {
	public static XElement findChannel(String channel) {
		if (StringUtil.isEmpty(channel))
			return null;
		
		SiteInfo siteinfo = OperationContext.get().getSite();
		
		XElement feed = siteinfo.getSettings().find("Feed");
		
		if (feed == null) 
			return null;
		
		for (XElement chan : feed.selectAll("Channel")) { 
			String calias = chan.getAttribute("Alias");
			
			if (calias == null)
				calias = chan.getAttribute("Name");
			
			if (!calias.equals(channel)) 
				continue;
			
			return chan;
		}
		
		return null;
	}
	
	public static List<XElement> findChannels() {
		List<XElement> list = new ArrayList<>();
		
		SiteInfo siteinfo = OperationContext.get().getSite();
		
		XElement feed = siteinfo.getSettings().find("Feed");
		
		if (feed == null) 
			return list;
		
		for (XElement chan : feed.selectAll("Channel")) { 
			String calias = chan.getAttribute("Alias");
			
			if (calias == null)
				calias = chan.getAttribute("Name");
			
			list.add(chan);
		}
		
		return list;
	}
	
	protected Map<String, FeedInfo> feedpaths = new HashMap<>();
	
	/*
	 * run collectTenant first
	 */
	public void importSite(OperationCallback op) {
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
	
	public void collectSite(CollectContext cctx) {
		XElement del = OperationContext.get().getSite().getSettings();
		
		Logger.info("Importing web content for domain: " + del.getAttribute("Title", "[unknown]") + " site: " + OperationContext.get().getSite().getAlias());
		
		// TODO improve collection process
		
		for (XElement chan : FeedIndexer.findChannels()) 
			this.collectChannel(cctx, chan);
		
		Logger.info("File count collected for import: " + this.feedpaths.size());
	}
	
	public void collectChannel(CollectContext cctx, XElement chan) {
		String alias = chan.getAttribute("Alias");
		
		if (alias == null)
			alias = chan.getAttribute("Name");
		
		// pages and blocks do not index the same way for public
		if (cctx.isForSitemap() && ("Pages".equals(alias) || !chan.getAttribute("AuthTags", "Guest").contains("Guest")))
			return;
		
		if (cctx.isForIndex() && "true".equals(chan.getAttribute("DisableIndex", "False").toLowerCase()))
			return;
		
		Logger.info("Importing site content for: " + OperationContext.get().getSite().getAlias() + " > " + alias);
		
		this.collectArea("feed", alias, false);
		
		if (!cctx.isForSitemap())
			this.collectArea("feed", alias, true);
	}
	
	public void collectArea(String area, String channel, boolean preview) {
		SiteInfo siteinfo = OperationContext.get().getSite();

		String wwwpathf1 = preview ? area +  "-preview/" + channel : area +  "/" + channel;
		
		Path wwwsrc1 = siteinfo.resolvePath(wwwpathf1).toAbsolutePath().normalize();
		
		if (!Files.exists(wwwsrc1)) 
			return;
		
		try {
			Files.walkFileTree(wwwsrc1, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path sfile, BasicFileAttributes attrs) {
					Path relpath = wwwsrc1.relativize(sfile);
					
					String fpath = "/" + relpath.toString().replace('\\', '/');

					// only collect dcf files
					if (!fpath.endsWith(".dcf.xml")) 
						return FileVisitResult.CONTINUE;
					
					// TODO if this is a Page channel then confirm that there is a corresponding .dcui.xml file - if not skip it
					
					fpath = fpath.substring(0, fpath.length() - 8);
					
					fpath = "/" + channel + fpath;
					
					Logger.debug("Considering file " + channel + " > " + fpath);

					// skip if already in the collect list
					if (FeedIndexer.this.feedpaths.containsKey(fpath)) 
						return FileVisitResult.CONTINUE;
					
					// add to the list
					if (preview) 
						Logger.info("Adding preview only " + channel + " > " + fpath);
					else 
						Logger.info("Adding published " + channel + " > " + fpath);
						
					FeedInfo fi = FeedInfo.buildInfo(channel, fpath);
					
					FeedIndexer.this.feedpaths.put(fpath, fi);
					
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
				
				// TODO except for Pages?  review
				
				sel.add(new XElement("loc", indexurl + fi.getFeedPath().substring(1)));
				sel.add(new XElement("lastmod", lmFmt.print(Files.getLastModifiedTime(fi.getPubpath()).toMillis())));

				for (String lname : altlocales)
					sel.add(new XElement("xhtml:link")
						.withAttribute("rel", "alternate")
						.withAttribute("hreflang", lname)
						.withAttribute("href", indexurl + lname + fi.getFeedPath())
					);
				
				smel.add(sel);
			}
			catch (Exception x) {
				Logger.error("Unable to add " + fi.getFeedPath() + ": " + x);
			}
		}
	}
}
