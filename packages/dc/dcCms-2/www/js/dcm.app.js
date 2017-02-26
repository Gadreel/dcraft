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

dc.pui.Tags['dcm.Twitter'] = function(entry, node) {
	var alternate = $(node).attr('data-dcm-twitter-alternate');
	var count = $(node).attr('data-dcm-twitter-count');
	
	dc.comm.sendMessage({
		Service: "dcmSocialMedia",
		Feature: "Twitter",
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
			
			var entry = $('<div class="dcm-tw-entry"></div>');
			
			var icon = $('<div class="dcm-tw-icon"></div>');
			
			icon.append('<img src="https://twitter.com/' + item.ScreenName + '/profile_image?size=normal" />');

			entry.append(icon);
			
			var body = $('<div class="dcm-tw-body"></div>');
			
			var info = $('<div class="dcm-tw-info"></div>');

			info.append('<span class="dcm-tw-info-name">' + item.By + '</span>');
			info.append(' ');
			info.append('<span class="dcm-tw-info-user">@' + item.ScreenName + '</span>');
			info.append(' ');
			info.append('<span class="dcm-tw-info-at">' + dc.util.Date.zToMoment(item.Posted).format('MMM D \\a\\t h:mm a') + '</span>');

			body.append(info);

			if (item.Message)
				body.append('<div class="dcm-tw-text">' + item.Html + '</div>');
			
			entry.append(body);
				
			$(node).append(entry);
		}
		
		if (entry)
			entry.addClass('dcm-tw-entry-last');
		
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
	
	if (! period)
		period = 3500;
	
	var gallery = $(node).attr('data-dcm-gallery');
	var show = $(node).attr('data-dcm-show');
	
	// TODO add this to a standard utility
	var imgLoadedFunc = function(img) { return img && img.complete && (img.naturalHeight !== 0); }
	var ssinit = false;
	var sscurr = -1;
	var switchblock = false;
	
	var imgPlacement = function(selector, idx) {
		$(node).find(selector).css({
		     marginLeft: '0'
		 });
	
		var fimg = dc.pui.TagCache['dcm.BasicCarousel'][gallery][show][idx];
		
		var idata = $(fimg).attr('data-dcm-img');
		
		if (!idata)
			return;
			
		var ii = JSON.parse(idata);
		
		//$(node).find('.dcm-basic-carousel-caption').text(ii.Description ? ii.Description : '');
		
		var centerEnable = $(node).attr('data-dcm-centering');

		if (! centerEnable || (centerEnable.toLowerCase() != 'true'))
			return;
		
		if (!ii.CenterHint)
			return;
	
		var ch = ii.CenterHint;
		var srcWidth = fimg.naturalWidth;
		var srcHeight = fimg.naturalHeight;
		var currWidth = $(node).width();
		var currHeight = $(node).height();
	
		// stretch whole image, no offset 
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
		
		$(node).find(selector).css({
		     marginLeft: '-' + xoff + 'px'
		 });
	};
	
	entry.registerResize(function(e) {
		imgPlacement('.dcm-basic-carousel-img', sscurr);
	});

	var nextImage = function() {
		var idx = sscurr + 1;
		
		if (idx >= dc.pui.TagCache['dcm.BasicCarousel'][gallery][show].length)
			idx = 0;
		
		if (sscurr == idx)
			return -1;
		
		var fimg = dc.pui.TagCache['dcm.BasicCarousel'][gallery][show][idx];
		
		if (! imgLoadedFunc(fimg)) 
			return -1;
		
		return idx;
	};
	
	var switchImage = function(idx) {
		if (idx == -1)
			return;
		
		var fimg = dc.pui.TagCache['dcm.BasicCarousel'][gallery][show][idx];
		
		$(node).addClass('dcm-loaded');

		$(node).find('.dcm-basic-carousel-img').attr('src', $(fimg).attr('src'));
		
		sscurr = idx;

		imgPlacement('.dcm-basic-carousel-img', sscurr);
		
		if (dc.handler && dc.handler.tags && dc.handler.tags.BasicCarousel && dc.handler.tags.BasicCarousel.switched)
			dc.handler.tags.BasicCarousel.switched(entry, node, show, idx, fimg);
	};
	
	if (! dc.pui.TagCache['dcm.BasicCarousel'])
		dc.pui.TagCache['dcm.BasicCarousel'] = { };
	
	if (! dc.pui.TagCache['dcm.BasicCarousel'][gallery])
		dc.pui.TagCache['dcm.BasicCarousel'][gallery] = { };
	
	var icache = dc.pui.TagCache['dcm.BasicCarousel'][gallery][show];
	
	// if not cached, build it
	if (!icache) {
		icache = [];
		dc.pui.TagCache['dcm.BasicCarousel'][gallery][show] = icache;
		
		$(node).find('.dcm-basic-carousel-list img').each(function() { 
			icache.push(this);
		});
	}
	
	if (dc.handler && dc.handler.tags && dc.handler.tags.BasicCarousel && dc.handler.tags.BasicCarousel.init)
		dc.handler.tags.BasicCarousel.init(entry, node, show, icache, {
			switchImage: function(idx) {
				animatefade(idx);
			}
		});

	if (icache.length == 0)
		return;
	
	switchImage(0);

	// make sure the "placement" code gets run
	entry.allocateTimeout({
		Title: 'Slide Show Controller',
		Period: 1000,
		Op: function() {
			switchImage(0);
		}
	});	

	if (icache.length == 1)
		return;
	
	var animatefade = function(idx) {
		if (! dc.util.Number.isNumber(idx))
			idx = nextImage();
		
		if (idx == -1) {
			tryAnimate(1000);
			return;
		}
	
		var fimg = dc.pui.TagCache['dcm.BasicCarousel'][gallery][show][idx];

		$(node).find('.dcm-basic-carousel-fader')
			.css({ opacity: 0 })
			.attr('src', $(fimg).attr('src'));

		imgPlacement('.dcm-basic-carousel-fader', idx);
		
		var cd = new dc.lang.CountDownCallback(2, function() {
			switchImage(idx);

			$(node).find('.dcm-basic-carousel-fader')
				.css({ opacity: 0 });

			$(node).find('.dcm-basic-carousel-img')
				.css({ opacity: 1 })
				.attr('src', $(fimg).attr('src'));
			
      		tryAnimate(period);
		});
		
		$(node).find('.dcm-basic-carousel-img').velocity('stop').velocity({
			opacity: 0.01
		}, 
		{
			duration: 650, 
			complete: function() {
	      		cd.dec();
			}
		});
		
		$(node).find('.dcm-basic-carousel-fader').velocity('stop').velocity({
	      	opacity: 1
	      }, 
	      {
	      	duration: 650, 
	      	complete: function() {
	      		cd.dec();
	      	}
	      });
		
		if (dc.handler && dc.handler.tags && dc.handler.tags.BasicCarousel && dc.handler.tags.BasicCarousel.switching)
			dc.handler.tags.BasicCarousel.switching(entry, node, show, idx, fimg);
	};
	
	var toid = null;
	
	var tryAnimate = function(ms) {
		//console.log('toid: ' + toid);
		
		if (toid)
			window.clearTimeout(toid);
		
		toid = entry.allocateTimeout({
			Title: 'Slide Show Controller',
			Period: ms,
			Op: function() {
				if (switchblock)
					tryAnimate(1000);
				else
					animatefade();
			}
		});	
	};
	
	tryAnimate(period);
	
	$(node).find('.dcm-basic-carousel-img').click(function(e) {
		if (dc.handler && dc.handler.tags && dc.handler.tags.BasicCarousel && dc.handler.tags.BasicCarousel.click) {
			var fimg = null;
			
			if (sscurr != -1)
				fimg = dc.pui.TagCache['dcm.BasicCarousel'][gallery][show][sscurr];
			
			dc.handler.tags.BasicCarousel.click(entry, node, show, sscurr, fimg);
		}
		
		e.preventDefault();
		return false;
	});
	
	$(node).on('mouseover', function(e) {
		switchblock = true;
	});
	
	$(node).on('mouseout', function(e) {
		switchblock = false;
	});
};

dc.pui.Tags['dcmi.PartButton'] = function(entry, node) {
	$(node).click(function(e) {
		var pel = $(this).closest('div.dc-part').get(0);
		
		if (! pel)
			return;
		
		dc.pui.App.Context = {
			Menu: 'dcmPagePart',
			Params: {
				Part: { 
					Channel: $(pel).attr('data-dccms-channel'), 
					Path: $(pel).attr('data-dccms-path'),
					Part: $(pel).attr('id')
				}
			}
		};
		
		if ($(pel).hasClass('dc-part-basic'))
			dc.pui.App.loadTab('Content');
			//dc.pui.Popup.menu('dcmPartBasicPopup');
		else
			dc.pui.Popup.menu('dcmPartPopup');
		
		e.preventDefault();
		return false;
	});
};

dc.pui.Tags['dcmi.SectionButton'] = function(entry, node) {
	$(node).click(function(e) {
		var pel = $(this).closest('div.dc-part').get(0);
		var sel = $(this).closest('div.dc-section').get(0);
		
		if (! pel || ! sel)
			return;
		
		dc.pui.App.Context = {
			Menu: 'dcmPagePart' + $(sel).attr('data-dccms-plugin') + 'Section',
			Params: {
				Part: { 
					Channel: $(pel).attr('data-dccms-channel'), 
					Path: $(pel).attr('data-dccms-path'), 
					Part: $(pel).attr('id')
				},
				Section: {
					Id: $(sel).attr('id'),
					Plugin:  $(sel).attr('data-dccms-plugin')
				} 
			}
		};
		
		dc.pui.Popup.menu('dcmSectionPopup');
		
		e.preventDefault();
		return false;
	});
};

dc.pui.Tags['dcmi.GalleryButton'] = function(entry, node) {
	$(node).click(function(e) {
		var cel = $(this).closest('div.dcm-basic-carousel').get(0);
		
		if (! cel) {
			cel = $(this).closest('div.dc-gallery-thumbs').get(0);
			
			if (! cel)
				return;
	
			dc.pui.Dialog.loadPage('/dcm/cms/show/Edit', { 
				Path: $(cel).attr('data-path'), 
				Alias: $(cel).attr('data-show'), 
				Callback: function(g) {
					// TODO refresh ... entry.callPageFunc('LoadMeta');
				} 
			});	
		}
		else {
			dc.pui.Dialog.loadPage('/dcm/cms/show/Edit', { 
				Path: $(cel).attr('data-dcm-gallery'), 
				Alias: $(cel).attr('data-dcm-show'), 
				Callback: function(g) {
					// TODO refresh ... entry.callPageFunc('LoadMeta');
				} 
			});	
		}
		
		e.preventDefault();
		return false;
	});
};

dc.pui.Tags['dcmi.AddFeedButton'] = function(entry, node) {
	$(node).click(function(e) {
		var cel = $(this).closest('div[data-dcm-channel]').get(0);
		
		if (! cel)
			return;

		var chan = $(cel).attr('data-dcm-channel');
		
		dc.pui.Dialog.loadPage('/dcm/cms/feed/Add-Feed-Prop/' + chan, { 
			Callback: function(g) {
				if (g.Path)
					dc.pui.Loader.MainLayer.loadPage(g.Path);
			} 
		});	
		
		e.preventDefault();
		return false;
	});
};

dc.pui.Tags['dcmi.EditFeedButton'] = function(entry, node) {
	$(node).click(function(e) {
		var cel = $(this).closest('div[data-dcm-channel]').get(0);
		
		if (! cel)
			return;

		var chan = $(cel).attr('data-dcm-channel');
		var path = $(cel).attr('data-dcm-path');
		
		dc.pui.Dialog.loadPage('/dcm/cms/feed/Edit-Feed-Prop/' + chan, { 
			Channel: chan,
			Path: path,
			Callback: function(g) {
				// TODO refresh ... entry.callPageFunc('LoadMeta');
			} 
		});	
		
		e.preventDefault();
		return false;
	});
};

// ------------------- end Tags -------------------------------------------------------
