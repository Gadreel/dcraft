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
			
			link.append('<a href="http://www.facebook.com/permalink.php?id='
				+ item.ById + '&story_fbid=' + item.PostId
				+ '" target="_blank">View on Facebook - Share</a>');
			
			entry.append(link);
				
			$(node).append(entry);
		}
		
		if (entry)
			entry.addClass('dcm-fb-entry-last');
		
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

dc.pui.Tags['dcm.BasicCarousel'] = function(entry, node) {
	var period = dc.util.Number.toNumberStrict($(node).attr('data-dcm-period'));
	var gallery = $(node).attr('data-dcm-gallery');
	var show = $(node).attr('data-dcm-show');
	
	// TODO add this to a standard utility
	var imgLoadedFunc = function(img) { return img && img.complete && (img.naturalHeight !== 0); }
	var ssinit = false;
	var sscurr = -1;
	
	var imgPlacement = function(e) {
		var centerEnable = $(node).attr('data-dcm-centering');

		if (! centerEnable || (centerEnable.toLowerCase() != 'true'))
			return;
	
		$(node).find('.dcm-basic-carousel-img').css({
		     marginLeft: '0'
		 });
	
		var fimg = dc.pui.TagCache['dcm.BasicCarousel'][gallery][show][sscurr];
		
		var idata = $(fimg).attr('data-dcm-img');
		
		if (!idata)
			return;
			
		var ii = JSON.parse(idata);
		
		if (!ii.CenterHint)
			return;
	
		var ch = ii.CenterHint;
		var srcWidth = fimg.naturalWidth;
		var srcHeight = fimg.naturalHeight;
		var currWidth = $(node).width();
		var currHeight = $(node).height();
	
		// strech whole image, no offset 
		if (currWidth > srcWidth)
			return;
	
		var zoom = currHeight / srcHeight;
		var availWidth = srcWidth * zoom;
		
		var xoff = (availWidth - currWidth) / 2;
		
		if (dc.util.Number.isNumber(ch)) 
			xoff -= ((srcWidth / 2) - ch) * zoom;
		
		if (xoff < 0)
			xoff = 0;
		if (xoff + currWidth > availWidth)
			xoff = availWidth - currWidth;
		
		$(node).find('.dcm-basic-carousel-img').css({
		     marginLeft: '-' + xoff + 'px'
		 });
	};
	
	entry.registerResize(imgPlacement);
	
	var switchImage = function(idx) {
		if (sscurr == idx)
			return;
			
		var fimg = dc.pui.TagCache['dcm.BasicCarousel'][gallery][show][idx];
		
		if (! imgLoadedFunc(fimg)) 
			return;
			
		$(node).addClass('dcm-loaded');

		$(node).find('.dcm-basic-carousel-img').attr('src', $(fimg).attr('src'));

		imgPlacement();
		
		sscurr = idx;
	};
	
	if (! dc.pui.TagCache['dcm.BasicCarousel'])
		dc.pui.TagCache['dcm.BasicCarousel'] = { };
	
	if (! dc.pui.TagCache['dcm.BasicCarousel'][gallery])
		dc.pui.TagCache['dcm.BasicCarousel'][gallery] = { };
	
	var icache = dc.pui.TagCache['dcm.BasicCarousel'][gallery][show];
	
	if (!icache) {
		icache = [];
		dc.pui.TagCache['dcm.BasicCarousel'][gallery][show] = icache;
	}
	
	$(node).find('.dcm-basic-carousel-list img').each(function() { 
		icache.push(this);
	});

	switchImage(0);
		
	entry.allocateInterval({
		Title: 'Slide Show Controller',
		Period: 1000,
		Op: function() {
			switchImage(0);
			
			// TODO switch slides periodically
		}
	});	
};

// ------------------- end Tags -------------------------------------------------------
