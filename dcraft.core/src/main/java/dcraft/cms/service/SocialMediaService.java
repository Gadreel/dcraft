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
package dcraft.cms.service;

import java.net.URL;

import dcraft.bus.IService;
import dcraft.bus.Message;
import dcraft.bus.MessageUtil;
import dcraft.cms.util.CatalogUtil;
import dcraft.hub.Hub;
import dcraft.hub.SiteInfo;
import dcraft.lang.op.FuncResult;
import dcraft.mod.ExtensionBase;
import dcraft.struct.CompositeParser;
import dcraft.struct.CompositeStruct;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;
import dcraft.struct.Struct;
import dcraft.util.StringUtil;
import dcraft.util.TimeUtil;
import dcraft.work.TaskRun;
import dcraft.xml.XElement;

public class SocialMediaService extends ExtensionBase implements IService {	
	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");
				
		// =========================================================
		//  Facebook
		// =========================================================
		
		if ("Facebook".equals(feature)) {
			if ("Feed".equals(op)) {
				this.handleFacebookFeed(request);
				return;
			}
		}
				
		// =========================================================
		//  Instagram
		// =========================================================
		
		if ("Instagram".equals(feature)) {
			if ("Feed".equals(op)) {
				this.handleInstagramFeed(request);
				return;
			}
		}
		
		// =========================================================
		//  Twitter
		// =========================================================
		
		if ("Twitter".equals(feature)) {
			if ("Feed".equals(op)) {
				this.handleTwitterFeed(request);
				return;
			}
		}
		
		request.errorTr(441, this.serviceName(), feature, op);
		request.complete();
	}
	
	public void handleFacebookFeed(TaskRun request) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		
		String alternate = (rec != null) ? rec.getFieldAsString("Alternate") : null;
		
		String name = "Facebook";
		
		if (StringUtil.isNotEmpty(alternate))
			name = name + "-" + alternate;
		
		long ncount = StringUtil.parseInt((rec != null) ? rec.getFieldAsString("Count") : null, 3);

		String cname = name + "-" + ncount;
		
		XElement fbsettings = CatalogUtil.getSettings(name);
		
		if (fbsettings == null) {
			request.error("Missing Facebook settings");
			request.returnEmpty();
			return;
		}
		
		SiteInfo site = request.getContext().getSite();
		
		ListStruct list = (ListStruct) site.getCache(cname);
		
		if (list != null) {
			System.out.println("use cache: " + cname);
			request.returnValue(list);
			return;
		}
	
		list = new ListStruct();

		try {
			URL url = new URL("https://graph.facebook.com/" + fbsettings.getAttribute("NodeId")  + 
				"/posts" + "?access_token=" + fbsettings.getAttribute("AccessToken") +
				"&fields=id,full_picture,created_time,from,type,message" +
				"&limit=" + ncount);

			FuncResult<CompositeStruct> res = CompositeParser.parseJson(url);

			if (res.isEmptyResult()) {
				request.error("Empty Facebook response");
				request.returnEmpty();
				return;
			}
		
			ListStruct fblist = ((RecordStruct) res.getResult()).getFieldAsList("data");
	
			for (Struct s : fblist.getItems()) {
				RecordStruct entry = (RecordStruct) s;
				
				String etype = entry.getFieldAsString("type");
				
				if (! "photo".equals(etype) && ! "link".equals(etype))
					continue;
					
				RecordStruct ret = new RecordStruct();
		
				String id = entry.getFieldAsString("id");
				String fid = id.substring(id.indexOf("_") + 1);
		
				RecordStruct from = entry.getFieldAsRecord("from");
				
				ret
					.withField("PostId", fid)
					.withField("By", from.getFieldAsString("name"))
					.withField("ById", from.getFieldAsString("id"))
					.withField("Posted", TimeUtil.parseDateTime(entry.getFieldAsString("created_time")))
					.withField("Message", entry.getFieldAsString("message"))
					.withField("Picture", "photo".equals(etype) ? entry.getFieldAsString("full_picture") : null);
		
				list.addItem(ret);
			}
			
			site.setCache(cname, list, StringUtil.parseInt(fbsettings.getAttribute("CachePeriod"), 900));
			request.returnValue(list);
			return;
		}
		catch (Exception x) {
			request.error("Unable to load Facebook feed.");
			request.error("Detail: " + x);
		}
		
		request.returnEmpty();
		return;
	}
		
	/*
	IG returns a record with "data" which contains a list of entries, highlights of the entry:
	
		"tags": [], 
		"type": "image", 
		"created_time": "1472072222", 
		"link": "https://www.instagram.com/p/BJgXLgGDWh_/", 
		"images": {
			"low_resolution": {
				"url": "https://scontent.cdninstagram.com/t51.2885-15/s320x320/e35/14033531_1159591777448720_392003303_n.jpg?ig_cache_key=MTMyNDE2MDIzNTg5MjIwNTY5NQ%3D%3D.2", 
				"width": 320, 
				"height": 320
			}, 
			"thumbnail": {
				"url": "https://scontent.cdninstagram.com/t51.2885-15/s150x150/e35/14033531_1159591777448720_392003303_n.jpg?ig_cache_key=MTMyNDE2MDIzNTg5MjIwNTY5NQ%3D%3D.2", "width": 150, "height": 150}, "standard_resolution": {"url": "https://scontent.cdninstagram.com/t51.2885-15/s640x640/sh0.08/e35/14033531_1159591777448720_392003303_n.jpg?ig_cache_key=MTMyNDE2MDIzNTg5MjIwNTY5NQ%3D%3D.2", "width": 640, "height": 640
			}
		}, 
		"id": "1324160235892205695_3724299625", 
	*/
	public void handleInstagramFeed(TaskRun request) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		
		String alternate = (rec != null) ? rec.getFieldAsString("Alternate") : null;
		
		String name = "Instagram";
		
		if (StringUtil.isNotEmpty(alternate))
			name = name + "-" + alternate;
		
		long ncount = StringUtil.parseInt((rec != null) ? rec.getFieldAsString("Count") : null, 12);

		String cname = name + "-" + ncount;
		
		XElement igsettings = CatalogUtil.getSettings(name);
		
		if (igsettings == null) {
			request.error("Missing Instagram settings");
			request.returnEmpty();
			return;
		}
		
		SiteInfo site = request.getContext().getSite();
		
		ListStruct list = (ListStruct) site.getCache(cname);
		
		if (list != null) {
			System.out.println("use cache: " + cname);
			request.returnValue(list);
			return;
		}
	
		list = new ListStruct();

		try {
			ListStruct iglist = dcraft.interchange.instagram.User.mediaFeed(igsettings.getAttribute("AccessToken"));

			if (iglist == null) {
				request.error("Empty Instagram response");
				request.returnEmpty();
				return;
			}
	
			int cnt = 0;
			
			for (Struct s : iglist.getItems()) {
				RecordStruct entry = (RecordStruct) s;
				
				String etype = entry.getFieldAsString("type");
				
				if (! "image".equals(etype) || ! entry.hasField("images"))
					continue;
		
				RecordStruct imgs = entry.getFieldAsRecord("images");
				
				list.addItem(new RecordStruct()
					.withField("PostId", entry.getFieldAsString("id"))
					.withField("Link", entry.getFieldAsString("link"))
					.withField("Picture", imgs.getFieldAsRecord("thumbnail").getFieldAsString("url"))
				);
				
				cnt++;
				
				if (cnt >= ncount)
					break;
			}
			
			site.setCache(cname, list, StringUtil.parseInt(igsettings.getAttribute("CachePeriod"), 900));
			request.returnValue(list);
			return;
		}
		catch (Exception x) {
			request.error("Unable to load Instagram feed.");
			request.error("Detail: " + x);
		}
		
		request.returnEmpty();
		return;
	}
	
	public void handleTwitterFeed(TaskRun request) {
		// TODO fix - this is experimental for now
		
		IService ts = (IService) Hub.instance.getInstance("dcraft.cms.service.TwitterService");
		
		if (ts != null)
			ts.handle(request);
	}
}
