/* ************************************************************************
#
#  designCraft.io
#
#  https://designcraft.io/
#
#  Copyright:
#    Copyright 2016 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */

dc.pui.Tags['dcm.Facebook'] = function(entry, node) {
	var alternate = $(node).attr('data-dcm-facebook-alternate');
	var count = $(node).attr('data-dcm-facebook-count');
	
	dc.comm.sendMessage({
		Service: "dcmSocialMedia",
		Feature: "Facebook",
		Op: "Feed",
		Body: {
			Alternate: alternate,
			Count: count
		}
	}, function (rmsg) {
		if (rmsg.Result != 0) { 
			// TODO do something, probably not popup
			// dc.pui.Popup.alert(e.Message);
			return;
		}

		var fbfeed = rmsg.Body;
		
		// posts

		for (var i = 0; i < fbfeed.length; i++) {
			var item = fbfeed[i];
			
			var entry = $('<div class="dcm-fb-entry"></div>');
			var hdr = $('<div class="dcm-fb-header"></div>');

			entry.append(hdr);
			
			var icon = $('<div class="dcm-fb-icon"></div>');
			
			icon.append('<img src="https://graph.facebook.com/' + item.ById + '/picture?type=small" style="width: 50px;" />');

			hdr.append(icon);
			
			var stamp = $('<div class="dcm-fb-stamp"></div>');

			stamp.append('<h3>' + item.By + '</h3>');
			stamp.append('<i>' + dc.util.Date.zToMoment(item.Posted).format('MMM D \\a\\t h:mm a') + '</i>');

			hdr.append(stamp);
			
			var body = $('<div class="dcm-fb-body"></div>');

			if (item.Message)
				body.append('<p>' + item.Message + '</p>');
			
			if (item.Picture) 
				body.append('<img src="' + item.Picture + '" />');
			
			entry.append(body);
			
			var link = $('<div class="dcm-fb-link"></div>');
			
			link.append('<p><a href="http://www.facebook.com/permalink.php?id='
				+ item.ById + '&story_fbid=' + item.PostId
				+ '" target="_blank">View on Facebook - Share</a></p>');
			
			entry.append(link);
				
			$(node).append(entry);
		}				
		
		$(node).append('<div style="clear: both;"></div>');
	});	
};

dc.pui.Tags['dcm.Instagram'] = function(entry, node) {
	var alternate = $(node).attr('data-dcm-instagram-alternate');
	var count = $(node).attr('data-dcm-instagram-count');
	
	dc.comm.sendMessage({
		Service: "dcmSocialMedia",
		Feature: "Instagram",
		Op: "Feed",
		Body: {
			Alternate: alternate,
			Count: count
		}
	}, function (rmsg) {
		if (rmsg.Result != 0) { 
			// TODO do something, probably not popup
			// dc.pui.Popup.alert(e.Message);
			return;
		}

		var igfeed = rmsg.Body;
		
		// posts

		for (var i = 0; i < igfeed.length; i++) {
			var item = igfeed[i];
			
			var entry = $('<div class="dcm-ig-entry"></div>');
			
			var link = $('<a class="dcm-fb-link" href="' + item.Link + '" target="_blank"></a>');

			link.append('<img src="' + item.Picture + '" />');
			
			entry.append(link);
				
			$(node).append(entry);
		}				
		
		$(node).append('<div style="clear: both;"></div>');
	});	
};

dc.pui.Tags['dcm.TwitterTimelineLoader'] = function(entry, node) {
	window.twttr = (function(d, s, id) {
	  var js, fjs = d.getElementsByTagName(s)[0],
	    t = window.twttr || {};
	  if (d.getElementById(id)) return t;
	  js = d.createElement(s);
	  js.id = id;
	  js.src = "https://platform.twitter.com/widgets.js";
	  fjs.parentNode.insertBefore(js, fjs);
	 
	  t._e = [];
	  t.ready = function(f) {
	    t._e.push(f);
	  };
	 
	  return t;
	}(document, "script", "twitter-wjs"));
};

// ------------------- end Tags -------------------------------------------------------
