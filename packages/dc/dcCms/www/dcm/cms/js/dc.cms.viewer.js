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

if (!dc.cms.viewer)
	dc.cms.viewer = {};

dc.cms.viewer.open = function(options) {
	var canid = dc.util.Uuid.create();
	
	var cel = $('<canvas style="z-index: 100; position: absolute; top: 0; left: 0; background-color: rgba(33,33,33,0.5); height: 100%; width: 100%;" width="200" height="200"></canvas>');
	
	$('body').append(cel);
}

dc.cms.viewer.createSlideShow = function(options) {
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
	
	container.append(cel);		

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
				this.__currIdx = 1;
	
				if (options.ImageChanged)
					options.ImageChanged(1);
			}
			
			var ss = this;
			
			dc.pui.Loader.allocateInterval({
				Title: 'Slide Show Controller: ' + options.Element,
				Period: 4000,
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
				options.ImageChanged(v);
			
			dc.pui.Loader.requestFrame();
		},
		fireTap: function() {
			if (this.__onTap)
				this.__onTap(options, this.__show.Images[this.__currIdx - 1]);
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
					ctx.drawImage(this.__images[this.__fadeIdx - 1], left, 0, w, h);
				}
				
				ctx.globalAlpha = (100 - this.__fade) / 100;
			}
			else
				ctx.globalAlpha = 1;
	        
			if (this.__images.length >= this.__currIdx) {
				left = strch ? 0 : (tcan.width - this.__images[this.__currIdx - 1].width) / 2;
				w = strch ? w : this.__images[this.__currIdx - 1].width;
				ctx.drawImage(this.__images[this.__currIdx - 1], left, 0, w, h);
			}
			
			ctx.globalAlpha = 1;
			
			//ctx.fillText('fade: ' + this.__fade, 10, 10);
			//ctx.fillText('x: ' + this.__lastX, 10, 10);
			
			if (this.__lock || this.__touch) {
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
		
		slideShow.__lastX = e.pageX - rect.left;
		slideShow.__lastY = e.pageY - rect.top;
		
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
		else
			slideShow.fireTap();
		
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

dc.cms.viewer.createSlideShowNavigator = function(options) {
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
		
		slideShow.__lastX = e.pageX - rect.left;
		slideShow.__lastY = e.pageY - rect.top;
		
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

