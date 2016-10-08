/* ************************************************************************
#
#  designCraft.io
#
#  https://designcraft.io/
#
#  Copyright:
#    Copyright 2015 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */

if (!dc.cms)
	dc.cms = {};

dc.cms.vary = {
	formatDims: function(img) {
		var desc = 'Width ';
		
		if (img.ExactWidth)
			desc += img.ExactWidth;
		else if (img.MaxWidth && img.MinWidth)
			desc += 'between ' + img.MinWidth + ' and ' + img.MaxWidth;
		else if (img.MaxWidth)
			desc += 'no more than ' + img.MaxWidth;
		else if (img.MinWidth)
			desc += 'at least ' + img.MinWidth;
		else 
			desc += 'unrestricted';
		
		desc += ' x Height ';
		
		if (img.ExactHeight)
			desc += img.ExactHeight;
		else if (img.MaxHeight && img.MinHeight)
			desc += 'between ' + img.MinHeight + ' and ' + img.MaxHeight;
		else if (img.MaxHeight)
			desc += 'no more than ' + img.MaxHeight;
		else if (img.MinHeight)
			desc += 'at least ' + img.MinHeight;
		else 
			desc += 'unrestricted';
		
		return desc;
	},
	find: function(settings, alias) {
		if (settings.Variations) {
			for (var i = 0; i < settings.Variations.length; i++) {
				var v = settings.Variations[i];
				
				if (v.Alias == alias)
					return v;
			}
		}
		
		return null;
	}
}

dc.cms.feed = {
	parseFormLayout: function(xel) {
		var elements = [];
		
		$.each(xel.children, function(i, child){
			var obj = { 
				Element: $(child).prop("tagName"),
				Attributes: { }
			};
			
			$.each(child.attributes, function(i, attrib){
				var keyCode = attrib.name.charCodeAt(0);
				
				if (keyCode > 96 && keyCode < 123) 
					obj.Attributes[attrib.name] = attrib.value;
				else
					obj[attrib.name] = attrib.value;
			});
			
			obj.Children = dc.cms.feed.parseFormLayout(child);
			
			elements.push(obj);
		});
		
		return elements;
	}
},

Image.prototype.load = function(url, callback, progcallback) {
	var thisImg = this;
	var xmlHTTP = new XMLHttpRequest();
	
	var completedPercentage = 0;
	var prevValue = 0;
	
	xmlHTTP.open('GET', url , true);
	xmlHTTP.responseType = 'arraybuffer';

    xmlHTTP.onload = function( e ) {
		var h = xmlHTTP.getAllResponseHeaders();
		var m = h.match(/^Content-Type\:\s*(.*?)$/mi);
		var mimeType = m[1] || 'image/png';
		
		var blob = new Blob([ this.response ], { type: mimeType });
		
		thisImg.src = window.URL.createObjectURL(blob);
		
		if (callback) 
			callback(thisImg);
    };

    xmlHTTP.onprogress = function(e) {
        if (e.lengthComputable) {
            completedPercentage = parseInt(( e.loaded / e.total ) * 100);

	        if (progcallback && (prevValue != completedPercentage)) 
	        	progcallback(thisImg, completedPercentage);
        }
    };

    xmlHTTP.onloadstart = function() {
        // Display your progress bar here, starting at 0
        completedPercentage = 0;
        
        if (progcallback) 
        	progcallback(thisImg, completedPercentage);
    };

    xmlHTTP.onloadend = function() {
        // You can also remove your progress bar here, if you like.
        completedPercentage = 100;
        
        if (progcallback) 
        	progcallback(thisImg, completedPercentage);
    }

    xmlHTTP.send();
};

dc.cms.createSlideShow = function(options) {
	var canid = dc.util.Uuid.create();

	var cel = $('<canvas></canvas>');
	
	cel.attr('id', canid);

	var container = $('#' + options.Element);
	
	if (!options.NoStyle) {
		container.css({
			'justify-content': 'center',
			display: 'flex'
		});
	}
	
	var needcel = true;

	var slideShow = {
		__show: null,
		__images: [],
		__loadIdx: 0,
		__currIdx: 1,
		__fadeIdx: 1,
		__fade: 0,
		__lock: false,
		__lastX: 0,
		__lastY: 0,
		__nav: null,
		__touch: dc.util.Params.is_touch_device(),
		__onTap: options.OnTap,
		
		// call only once
		setShow: function(v) {
			if (!v)
				return;
			
			this.__show = v;
			this.__loadIdx = 0;
			this.resize();
			this.loadNextImage();
			
			if (this.__show.Images.length) {
				this.__currIdx = options.Index ? options.Index : 1;
	
				if (options.ImageChanged)
					options.ImageChanged(this.__currIdx, this);
			}
			
			var ss = this;
			
			dc.pui.Loader.allocateInterval({
				Title: 'Slide Show Controller: ' + options.Element,
				Period: options.Period ? options.Period : 4000,
				Op: function() {
					if (!ss.__show.Images || (ss.__show.Images.length < 2) || ss.__lock)
						return;
					
					if (ss.__nav && ss.__nav.__lock)
						return;
					
					// TODO support other than sequential
		
					ss.showNextImage();
				}
			});
		},
		loadNextImage: function() {
			var ss = this;
			
			if (this.__show.Images && (this.__show.Images.length > this.__loadIdx)) {
				var img = new Image(); //new image in memory
				
				img.onload = function(e) {
					ss.__images.push(this);
					
					if (needcel) {
						if (options.FadeIn) {
							options.FadeIn(ss, cel);
						}
						else {
							container.append(cel);
							ss.resize();
						}
						
						needcel = false;
					}
					
					dc.pui.Loader.requestFrame();

					ss.__loadIdx++;
					ss.loadNextImage();
				};			
				
				var iname = this.__show.Images[this.__loadIdx];
				
				img.src = '/galleries/' + options.Gallery + '/' + iname.Alias + '.v/' + this.__show.Variation + '.jpg';
			}
		},
		showNextImage: function() {
			var n = this.__currIdx + 1;
			
			if (n > this.__show.Images.length)
				n = 1;
			
			this.setImageIndex(n);
		},
		showPrevImage: function() {
			var n = this.__currIdx - 1;
			
			if (n < 1)
				n = this.__show.Images.length;
			
			this.setImageIndex(n);
		},
		setImageIndex: function(v) {
			this.__fadeIdx = this.__currIdx;
			this.__fade = 100;
			
			this.__currIdx = v;
			
			if (options.ImageChanged)
				options.ImageChanged(v, this);
			
			dc.pui.Loader.requestFrame();
		},
		getImageIndex: function() {
			return this.__currIdx;
		},
		getImageInfo: function(idx) {
			if (! dc.util.Number.isNumber(idx))
				idx = this.__currIdx;
				
			return this.__show.Images[idx - 1];
		},
		fireTap: function(e) {
			if (this.__onTap)
				this.__onTap(options, this.__show.Images[this.__currIdx - 1], this, e);
		},
		resize: function() {
			if (!this.__show)
				return;
			
			var w = this.__show.VariationSettings.ExactWidth;
			var h = this.__show.VariationSettings.ExactHeight;
			
			var tcon = $('#' + options.Element).get(0);
			
			var maxw = $(tcon).width();
			
			//console.log('maxw: ' + maxw);
			
			var tcan = $('#' + canid).get(0);
			
			if (!tcan)
				return;
			
			if (options.ResizeHandler) {
				var res = options.ResizeHandler(tcan, this.__show);
				
				w = res.width;
				h = res.height;
			}
			else {
				var adj = 1;
				var strch = true;
				
				// if no exact width then no stretch mode support
				if (!w) {
					w = maxw;
					strch = false;
				}
				else if (w > maxw) { 
					adj = maxw / w;			
					w = w * adj;
					h = h * adj;
				}
			}
		
			//console.log('resize set width: ' + w);
			
			tcan.width = w;
			tcan.height = h;

			/*
			// chrome 40.0.2214.111 has a oddity where setting width and height are not resizing the canvas element
			// although there is some logic to that, it isn't how it used to work nor how the rest of the world
			// expects it (FF, etc).
			if (/chrom(e|ium)/.test(navigator.userAgent.toLowerCase())) {
				$(tcan).css({
				    // minWidth: w + 'px', 
				    // maxWidth: h + 'px',
				     width: '100%'
				 });
			}
			else {
			}
			*/
			
			/* TODO this is probably right (see BW) but not so happy on TDS so see below for TDS)
			$(tcan).css({
			     width: '100%'
			 });
			*/
			
			$(tcan).css({
			     width: w +'px',
			     height: h +'px'
			 });
			
			//console.log('resize set width 2: ' + tcan.width);
			
			dc.pui.Loader.requestFrame();
		},
		draw: function() {
			if (!this.__show)
				return;
			
			if (options.DrawHandler) {
				options.DrawHandler(this);
				return;
			}
			
			var w = this.__show.VariationSettings.ExactWidth;
			var h = this.__show.VariationSettings.ExactHeight;
			var left = 0;
			
			var tcan = $('#' + canid).get(0);
			
			if (!tcan)
				return;
			
			var ctx = tcan.getContext('2d');
			
			var maxw = tcan.width;
			
			var adj = 1;
			var strch = true;
			
			// if no exact width then no stretch mode support
			if (!w) {
				strch = false;
			}
			else if (w > maxw) { 
				adj = maxw / w;			
				h = h * adj;
				w = w * adj;
			}
			
			ctx.clearRect(0, 0, tcan.width, tcan.height);

			// this works with stretch and without
			if (this.__fade > 0) {
				ctx.globalAlpha = this.__fade / 100;

				if (this.__images.length > this.__fadeIdx - 1) {
					left = strch ? 0 : (tcan.width - this.__images[this.__fadeIdx - 1].width) / 2;
					w = strch ? w : this.__images[this.__fadeIdx - 1].width;
					
					if (options.DrawImage)
						options.DrawImage(ctx, this.__images[this.__fadeIdx - 1], left, 0, w, h, this.__fadeIdx, options, this);
					else
						ctx.drawImage(this.__images[this.__fadeIdx - 1], left, 0, w, h);
				}
				
				ctx.globalAlpha = (100 - this.__fade) / 100;
			}
			else
				ctx.globalAlpha = 1;
	        
			if (this.__images.length >= this.__currIdx) {
				left = strch ? 0 : (tcan.width - this.__images[this.__currIdx - 1].width) / 2;
				w = strch ? w : this.__images[this.__currIdx - 1].width;
				
				if (options.DrawImage)
					options.DrawImage(ctx, this.__images[this.__currIdx - 1], left, 0, w, h, this.__currIdx, options, this);
				else
					ctx.drawImage(this.__images[this.__currIdx - 1], left, 0, w, h);
			}
			
			ctx.globalAlpha = 1;

			if (options.Overlay)
				options.Overlay(ctx, left, 0, w, h, this.__currIdx, options, this);

			//ctx.fillText('fade: ' + this.__fade, 10, 10);
			//ctx.fillText('x: ' + this.__lastX, 10, 10);
			
			if (!options.NoNav && (this.__show.Images.length > 1) && (this.__lock || this.__touch)) {
				var mpos = 0;
				var vpos = (h / 2);
				
				ctx.lineWidth = 2;
				
				ctx.fillStyle = (this.__lastX < 50) ? 'black' : 'rgba(0,0,0,0.6)';
					
				ctx.beginPath();
				ctx.arc(21, vpos - 5, 14, 0, 2 * Math.PI);
				ctx.fill();						
				
				ctx.fillStyle = (this.__lastX > (tcan.width - 50)) ? 'black' : 'rgba(0,0,0,0.6)';
				
				ctx.beginPath();
				ctx.arc(tcan.width - 21, vpos - 5, 14, 0, 2 * Math.PI);
				ctx.fill();						
				
				ctx.fillStyle = 'rgba(256,256,256,0.4)';
				
				ctx.beginPath();
				ctx.moveTo(12, vpos - 5);
				ctx.lineTo(26, vpos - 12);
				ctx.lineTo(26, vpos + 2);
				ctx.closePath();
				ctx.fill();						
				
				ctx.beginPath();
				ctx.moveTo(tcan.width - 12, vpos - 5);
				ctx.lineTo(tcan.width - 26, vpos - 12);
				ctx.lineTo(tcan.width - 26, vpos + 2);
				ctx.closePath();
				ctx.fill();		
			}
			
			if (this.__fade > 0) {
				this.__fade -= 3;
				
				if (this.__fade < 0)
					this.__fade = 0;
				
				dc.pui.Loader.requestFrame();
			}
		},
		getCanvas: function() {
			return $('#' + canid).get(0);
		},
		destroy: function() {
			this.__images = [];		// release image memory
			cel = null;
		}
	};
	
	cel.mouseover(function(e) {
		slideShow.__lock = true;
		dc.pui.Loader.requestFrame();
		
		e.preventDefault();
		return false;
	});
	
	cel.mouseout(function(e) {
		slideShow.__lock = false;
		dc.pui.Loader.requestFrame();
		
		e.preventDefault();
		return false;
	});
	
	cel.mousemove(function(e) {
		var rect = $(cel).get(0).getBoundingClientRect();
		
		slideShow.__lastX = e.clientX - rect.left;
		slideShow.__lastY = e.clientY - rect.top;
		
		dc.pui.Loader.requestFrame();
		
		//e.preventDefault();
		return true;
	});

	cel.mouseup(function(e) {
		var tcan = $(cel).get(0);
	
		if (!options.NoNav && (slideShow.__lastX < 50))
			slideShow.showPrevImage();
		else if (!options.NoNav && (slideShow.__lastX > (tcan.width - 50)))
			slideShow.showNextImage();
		else if (options.OnClick)
			options.OnClick(e, slideShow);
		else
			slideShow.fireTap(e);
		
		//e.preventDefault();
		return true;
	});
	
	cel.on("swipeleft",function(e) {
		slideShow.showNextImage();
		
		e.preventDefault();
		return false;
	});
	
	cel.on("swiperight",function(e) {
		slideShow.showPrevImage();
		
		e.preventDefault();
		return false;
	});
	
	dc.pui.Loader.registerResize(function() { slideShow.resize(); });
	
	dc.pui.Loader.registerFrame({
		Fps: 24,
		Run: function() { slideShow.draw(); } 
	});
	
	dc.pui.Loader.registerDestroy(function() { slideShow.destroy(); });
	
	if (options.Data) {
		slideShow.setShow(options.Data);
		
		if (options.Callback)
			options.Callback(slideShow);
		
		return;
	}
	
	if (options.Handler && dc.handler.banners) {
		dc.handler.banners.load(slideShow, options);
		return;
	}
	
	dc.comm.sendMessage({ 
		Service: 'dcmCms',
		Feature: 'WebGallery',
		Op: 'LoadSlideShow',
		Body: { 
			GalleryPath: options.Gallery, 
			Alias: options.Alias
		}
	}, function(rmsg) {
		if (rmsg.Result > 0) {
			if (options.ErrorCallback)
				options.ErrorCallback(rmsg);
			else if (options.Callback)
				options.Callback(null);
			
			//dc.pui.Popup.alert('Unable to load slide show: ' + rmsg.Message);
			return;
		}
		
		//console.log(JSON.stringify(rmsg)); 

		if (rmsg.Body)
			slideShow.setShow(rmsg.Body);
		
		if (options.NavElement) {
			dc.cms.createSlideShowNavigator({
				Gallery: options.Gallery, 
				Alias: options.Alias, 
				Element: options.NavElement, 
				Data: {
					Images: rmsg.Body.Images, 
					Height: options.NavHeight,			// TODO steal this from rmsg.Body 
					Width: options.NavWidth,			// TODO steal this from rmsg.Body 
					Variation: options.NavThumb ? options.NavThumb : 'thumb'			// TODO steal this from rmsg.Body
				},
				Callback: function(nss) {
					slideShow.__nav = nss;
					
					if (options.Callback)
						options.Callback(slideShow);
				},
				Viewer: slideShow
			});
		}
		else if (options.Callback)
			options.Callback(slideShow);
	});
};

dc.cms.createSlideShowNavigator = function(options) {
	var canid = dc.util.Uuid.create();

	var cel = $('<canvas></canvas>');
	
	cel.attr('id', canid);
	cel.css( { width: '100%' });

	var container = $('#' + options.Element);
	
	if (!options.NoStyle) {
		container.css({
			'justify-content': 'center',
			display: 'flex'
		});
	}
	
	container.append(cel);		

	var slideShow = {
		__show: null,
		__images: [],
		__loadIdx: 0,
		__currIdx: 1,
		__lock: false,
		__lastX: 0,
		__lastY: 0,
		
		// call only once
		setShow: function(v) {
			if (!v)
				return;
			
			this.__show = v;
			this.__loadIdx = 0;
			this.resize();
			
			this.loadNextImage();
		},
		showNextImage: function() {
			this.__currIdx++;
			
			if (this.__currIdx >= this.__show.Images.length)
				this.__currIdx = this.__show.Images.length - 1;
			
			dc.pui.Loader.requestFrame();
		},
		showPrevImage: function() {
			this.__currIdx--;
			
			if (this.__currIdx < 1)
				this.__currIdx = 1;
			
			dc.pui.Loader.requestFrame();
		},
		loadNextImage: function() {
			var ss = this;
			
			if (this.__show.Images && (this.__show.Images.length > this.__loadIdx)) {
				var img = new Image(); //new image in memory
				
				img.onload = function(e) {
					ss.__images.push(this);
					
					dc.pui.Loader.requestFrame();

					ss.__loadIdx++;
					ss.loadNextImage();
				};			
				
				var iname = this.__show.Images[this.__loadIdx];
				
				img.src = '/galleries/' + options.Gallery + '/' + iname.Alias + '.v/' + this.__show.Variation + '.jpg';
			}
		},
		resize: function() {
			if (!this.__show)
				return;
			
			var h = this.__show.Height + 2;		// plus top bottom lines
			var w = this.__show.Width ? this.__show.Width : -1;
			
			var tcon = $('#' + options.Element).get(0);
			
			var maxw = $(tcon).width();
			
			if ((w == -1) || (w > maxw))
				w = maxw;
			
			var tcan = $('#' + canid).get(0);
			
			tcan.width = w;
			tcan.height = h;
			
			cel.css( { height: h + 'px', width: w + 'px' });
			
			dc.pui.Loader.requestFrame();
		},
		draw: function() {
			if (!this.__show)
				return;
			
			//debugger;
			
			var left = 0;
			
			var tcan = $('#' + canid).get(0);
			var ctx = tcan.getContext('2d');
			
			var vw = tcan.width;
			var vh = tcan.height;
			
			ctx.clearRect(0, 0, vw, vh);

			var left = 40;
			var idx = this.__currIdx;
			
			while (this.__images.length >= idx) {
				var iw = this.__images[idx - 1].width;
				var ih = this.__images[idx - 1].height;
				
				// TODO adjust height/width if doesn't fit in vh - 2
				var adj = (vh - 2) / ih;			
				ih *= adj;
				iw *= adj;
				
				if (left + iw > vw - 40)
					break;
				
				ctx.drawImage(this.__images[idx - 1], left, 1, iw, ih);
				
				left += iw + Math.round(vh / 3);
				idx++;
			}
			
			//if (this.__lock) {
				var mpos = 0;
				var vpos = Math.round(vh / 2) + 3;
				
				ctx.lineWidth = 2;
				
				ctx.fillStyle = (this.__lastX < 50) && (this.__lock) ? 'black' : 'rgba(0,0,0,0.6)';
				
				ctx.beginPath();
				ctx.moveTo(12, vpos - 5);
				ctx.lineTo(26, vpos - 12);
				ctx.lineTo(26, vpos + 2);
				ctx.closePath();
				ctx.fill();						

				/*
				ctx.beginPath();
				ctx.arc(20, vpos - 5, 10, 0, 2 * Math.PI);
				ctx.fill();						
				*/
				
				ctx.fillStyle = (this.__lastX > (vw - 50)) && (this.__lock) ? 'black' : 'rgba(0,0,0,0.6)';
				
				/*
				ctx.beginPath();
				ctx.arc(vw - 20, vpos - 5, 10, 0, 2 * Math.PI);
				ctx.fill();						
				
				ctx.fillStyle = 'white';
				*/
				
				ctx.beginPath();
				ctx.moveTo(vw - 12, vpos - 5);
				ctx.lineTo(vw - 26, vpos - 12);
				ctx.lineTo(vw - 26, vpos + 2);
				ctx.closePath();
				ctx.fill();		
			//}
			
				/*
			ctx.beginPath();
			ctx.moveTo(0, 0);
			ctx.lineTo(vw, 0);
			ctx.stroke();		
			
			ctx.beginPath();
			ctx.moveTo(0, vh);
			ctx.lineTo(vw, vh);
			ctx.stroke();
			*/		
		},
		destroy: function() {
			this.__images = [];		// release image memory
			cel = null;
		}
	};
	
	cel.mouseover(function(e) {
		slideShow.__lock = true;
		dc.pui.Loader.requestFrame();
		
		e.preventDefault();
		return false;
	});
	
	cel.mouseout(function(e) {
		slideShow.__lock = false;
		dc.pui.Loader.requestFrame();
		
		e.preventDefault();
		return false;
	});
	
	cel.mousemove(function(e) {
		var rect = $(cel).get(0).getBoundingClientRect();
		
		slideShow.__lastX = e.clientX - rect.left;
		slideShow.__lastY = e.clientY - rect.top;
		
		dc.pui.Loader.requestFrame();
		
		//e.preventDefault();
		return true;
	});

	cel.mouseup(function(e) {
		var tcan = $(cel).get(0);
	
		if (slideShow.__lastX < 50)
			slideShow.showPrevImage();
		else if (slideShow.__lastX > (tcan.width - 50))
			slideShow.showNextImage();
		else {
			var left = 40;
			var idx = slideShow.__currIdx;
			var vh = tcan.height;
			
			while (slideShow.__images.length >= idx) {
				var iw = slideShow.__images[idx - 1].width;
				var ih = slideShow.__images[idx - 1].height;
				
				var adj = (vh - 2) / ih;			
				iw *= adj;

				if ((slideShow.__lastX >= left) && (slideShow.__lastX <= left + iw)) {
					//console.log('selected: ' + idx);
					
					if (options.Viewer)
						options.Viewer.setImageIndex(idx);
					
					break;
				}
				
				// TODO adjust height/width if doesn't fit in vh - 2
				
				left += iw + Math.round(vh / 3);
				idx++;
			}			
		}
		
		//e.preventDefault();
		return true;
	});
	
	cel.on("swipeleft",function(e) {
		slideShow.__currIdx += 3;
		
		if (slideShow.__currIdx >= slideShow.__show.Images.length)
			slideShow.__currIdx = slideShow.__show.Images.length - 1;
		
		dc.pui.Loader.requestFrame();
		
		e.preventDefault();
		return false;
	});
	
	cel.on("swiperight",function(e) {
		slideShow.__currIdx -= 3;
		
		if (slideShow.__currIdx < 1)
			slideShow.__currIdx = 1;
		
		dc.pui.Loader.requestFrame();
		
		e.preventDefault();
		return false;
	});
	
	dc.pui.Loader.registerFrame({
		Fps: 24,
		Run: function() { 
			slideShow.draw();			 
		}
	});
	
	dc.pui.Loader.registerResize(function() { slideShow.resize(); });
	dc.pui.Loader.registerDestroy(function() { slideShow.destroy(); });
	
	if (options.Data) {
		slideShow.setShow(options.Data);
		
		if (options.Callback)
			options.Callback(slideShow);
		
		return;
	}
	
	/*	TODO
	dc.comm.sendMessage({ 
		Service: 'dcmCms',
		Feature: 'WebGallery',
		Op: 'LoadSlideShow',
		Body: { 
			GalleryPath: options.Gallery, 
			Alias: options.Alias
		}
	}, function(rmsg) {
		if (rmsg.Result > 0) {
			dc.pui.Popup.alert('Unable to load slide show: ' + rmsg.Message);
			return;
		}
		
		//console.log(JSON.stringify(rmsg)); 

		if (rmsg.Body)
			slideShow.setShow(rmsg.Body);
		
		if (options.Callback)
			options.Callback(slideShow);
	});
	*/
};

dc.cms.createEditor = function(def, loc, changecb) {
	// select a template
	var template = null;
	
	/* TODO
	if (def.Templates && def.Templates.length) {
		template = def.Templates[0];		// TODO need a way to choose a template
	}
	*/
	
	var edname = $(def).attr('Editor');
	var edfor = $(def).attr('For');
	
	// TODO support other editors
	if (edname == 'html') {
	    editor = ace.edit(loc);
	    
	    editor.setTheme("ace/theme/chrome");
	    editor.getSession().setMode("ace/mode/markdown");
		editor.setShowPrintMargin(false);
		editor.getSession().setTabSize(5);
		editor.getSession().setUseSoftTabs(false);	    
		editor.getSession().setUseWrapMode(true);

		editor.setContent = function(v) {
			this._setmode = true;
			this.setValue(v, -1);
			this._setmode = false;
		};
		
		editor.updateContent = function(data) {
			data.SetParts.push({
				For: edfor,
				Format: this.getFormat(),
				Value: this.getValue()
			});
		}
		
		editor.getContent = function() {
			return this.getValue();
		}
		
		editor.getFormat = function() {
			return 'md';
		}
		
		editor.getOther = function() {
			return {					
			};
		}
		
		editor.getTemplate = function() {
			return template;
		}
		
		editor.getHelp = function() {
			// get Help from first template
			var t1 = $(def).find('Help').text();

			//if (!template)
			//	return null;
			
			if (!t1)
				t1 = '';
			
			//return t1 + '\n\n\\*\\*for Bold\\*\\*\n\\_for Italics\\_\n\\#\\# or \\#\\#\\# for header';		// TODO add standard help based on Format

			return t1 + " \n"
+ "## Text Formatting \n"
+ "\n"
+ "### Bold \n"
+ "\n"
+ "Put \*\* on either side of bold text: \n"
+ "\n"
+ "	Example text for your page with \*\*Bold\*\*.\n"
+ "\n"
+ "Produces:\n"
+ "\n"
+ "Example text for your page with **Bold**.\n"
+ "\n"
+ "### Italic\n"
+ "\n"
+ "Put \_ on either side of italic text:\n"
+ "\n"
+ "	Example text for your page with \_Italic\_.\n"
+ "\n"
+ "Produces:\n"
+ "\n"
+ "Example text for your page with _Italic_.\n"
+ "\n"
+ "### Horizontal Rule\n"
+ "\n"
+ "Put --- (three dashes) at start of line to indicate a rule.  Continue content on following lines leave this with just the three dashes.\n"
+ "\n"
+ "Produces:\n"
+ "\n"
+ "---\n"
+ "### Headers\n" 
+ "\n"
+ "Put \#, \#\# or \#\#\# at start of line:\n"
+ "\n"
+ "		\#\# Example header on your page.\n"
+ "\n"
+ "Produces:\n"
+ "\n"
+ "## Example header on your page.\n"
+ "\n"
+ "### List \n"
+ "\n"
+ "Put - (single dash) at start of line for each list item:\n"
+ "\n"
+ "	\- Item one on your page.\n"
+ "	\- Item two on your page.\n"
+ "	\- Item three on your page.\n"
+ "\n"
+ "Produces:\n"
+ "- Item one on your page.\n"
+ "- Item two on your page.\n"
+ "- Item three on your page.\n"
+ "\n"
+ "### Links\n"
+ "\n"
+ "Put text in square brackets, followed by link in parenthesis:\n"
+ "\n"
+ "	\[Go to Google website\]\(http://www.google.com\).\n"
+ "\n"
+ "Produces:\n"
+ "\n"
+ "[Go to Google website](http://www.google.com).\n"
+ "\n"
+ "### Images\n"
+ "\n"
+ "For images you have uploaded to the Gallery, just click the Gallery button above and select the image.  If you need to link an image from another site do it like a Link but with a ! in front.\n"
+ "\n"
+ "	!\[The W3C Logo\]\(http://www.w3.org/Icons/w3c_home.png\)\n"
+ "\n"
+ "Produces:\n"
+ "\n"
+ "![The W3C Logo](http://www.w3.org/Icons/w3c_home.png)\n"
+ "";

		}
		
		editor.getGalleryVariation = function() {
			var vari = $(def).attr('GalleryVariation');
			
			if (!vari)
				vari = 'full';
			
			return vari;
		}
		
		editor.openGallery = function(gpath) {
			var scope = $(def).attr('GalleryScope');
			
			if (!scope)
				scope = 'Variation';
			
			if (!gpath)
				gpath = $(def).attr('GalleryPath');
			
			dc.pui.Loader.__current.callPageFunc('DoShowGallery', scope, gpath, function(img) {
				if (scope == 'Image')
					img = img + '/' + $(def).attr('GalleryVariation') + '.jpg'; 
				
				editor.insert('![](/galleries' + img + ' "")');
			});
		}
		
		editor.selectGallery = function(path,data) {
			var vari = $(def).attr('GalleryVariation');
			
			if (!vari)
				vari = 'full';
			
			path = path + '.v/' + vari + '.jpg';   
			console.log('selected image for md: ' + path);
			
			this.insert('![](/galleries' + path + ' "")');
		}
		
		editor.on("change", function() {
			// `this` isn't available here - must use `editor`
			if (!editor._setmode && changecb)
				changecb();
		});								
		
		return editor;
	}
	
	if (edname == 'gallery') {
		var editor = {
			focus: function() {
				this._cntrl.focus();
			},
			destroy: function() {
				delete this._cntrl;
			},
			updateContent: function(data) {
				data.SetParts.push({
					For: edfor,
					Format: this.getFormat(),
					Value: this.getValue()
				});
			},
			setContent: function(v) {
				this._cntrl.val(v);
				
				if (v)
					$('#' + loc).append('<img src="/galleries' + v + '" />')
			},
			getContent: function() {
				var v = this._cntrl.val();  
				return v;
			},
			getFormat: function() {
				return 'image';
			},
			getValue: function() {
				var v = this._cntrl.val();  
				return v;
			},
			getOther: function() {
				return {
					Variation: 'thumb'
				};
			},
			
			openGallery: function() {
				var scope = $(def).attr('GalleryScope');
				
				if (!scope)
					scope = 'Variation';
				
				dc.pui.Loader.__current.callPageFunc('DoShowGallery', scope, $(def).attr('GalleryPath'), function(img) {
					if (scope == 'Image')
						img = img + '/' + $(def).attr('GalleryVariation') + '.jpg'; 
					
					editor._cntrl.val(img);
					
					$('#' + loc + ' > img').remove();
					$('#' + loc).append('<img src="/galleries' + img + '" />')
					changecb();				
				});
			},
			
			getGalleryVariation: function() {
				var vari = $(def).attr('Variation');
				
				if (!vari)
					vari = 'full';
				
				return vari;
			},
			selectGallery: function(path,data) {
				var vari = $(def).attr('Variation');
				
				if (!vari)
					vari = 'full';
				
				path = path + '.v/' + vari + '.jpg';   

				console.log('selected image: ' + path);
				this._cntrl.val(path);
				
				$('#' + loc + ' > img').remove();
				$('#' + loc).append('<img src="/galleries' + path + '" />')
				changecb();				
			}
		};
		
		editor._id = loc;
		
		editor._cntrl = $('<input id="txt' + loc + '" />');
		
		editor._cntrl.change(function() {
			changecb();				
		});
		
		$('#' + loc).empty()
			.append('Image Path: ')
			.append(editor._cntrl)
			.enhanceWithin();
		
		return editor;
	}
	
	return null;
};
