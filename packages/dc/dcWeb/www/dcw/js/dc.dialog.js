
// dc.pui.Loader.addExtraLibs( ['/dcw/js/dc.dialog.js'] );
// dc.pui.Loader.addExtraStyles( ['/dcw/css/dc.dialog.css'] );

dc.pui.dialog = {
	__context: null,
	
	Loader: {
		getContext: function() {
			return dc.pui.dialog.__context;
		},
		setContext: function(v) {
			dc.pui.dialog.__context = v;
		},
		getPane: function() {
			var pane = dc.pui.Loader.getLayer("DialogPane");
			
			if (!pane) {
				$('#dcuiDialog').remove();
	
				$('body').append('<div id="dcuiDialog" class="ui-content">'
					+ '<div id="dcuiDialogPane"></div></div>');
				
				pane = new dc.pui.dialog.Layer('#dcuiDialogPane', { Name: 'DialogPane' });
				
				/*
				pane.setObserver('DialogPane', function() {
					dc.pui.dialog.Loader.show();
				});
				*/
				
				dc.pui.Loader.addLayer(pane);
			}
			
			return pane;
		},
		// area from MenuEnum above (or from custom defined)
		/*
		show: function() {
			var pane = dc.pui.dialog.Loader.getPane();
			
			$('#dcuiDialog').show();
		},
		*/
		// start with a fresh new pane
		openPane: function(page, params) {
			var layer = dc.pui.dialog.Loader.getPane();
			
			layer.clearHistory();
			
			dc.pui.dialog.Loader.loadPane(page, params);
		},
		loadPane: function(page, params, replaceState) {
			var layer = dc.pui.dialog.Loader.getPane();
			
			$('#dcuiDialog').show();
			
			if (!params)
				params = { };
			
			layer.loadPage(page, params, replaceState);
				
			$('body').addClass('dcDialogMode');
		},
		closePane: function() {
			var pane = dc.pui.dialog.Loader.getPane();
			
			if (pane) 
				pane.closePage();
		},
		closeAll: function(dmode) {
			var pane = dc.pui.Loader.getLayer("DialogPane");
			
			if (pane) 
				pane.clearHistory();
			
			$('#dcuiDialog').hide();
				
			$('body').removeClass('dcDialogMode');

			if (!dmode)
				dc.pui.Loader.currentPageEntry().callPageFunc('dcwDialogEvent', { Name: 'Close', Target: 'DialogPane' });
		},
		destroy: function() {
			dc.pui.dialog.Loader.closeAll(true);
			
			$('#dcuiDialog').remove();
		}
	}
};

dc.pui.dialog.Layer = function(contentsel, options) {
	this.init(contentsel, options);
};  

dc.pui.dialog.Layer.prototype = new dc.pui.Layer();

dc.pui.dialog.Layer.prototype.manifestPage = function(entry, killpane) {
	var layer = this;
	
	if (!entry)
		return;
	
	// only destroy if current was lost due to a back button 
	// otherwise we should expect we'll want the entry again later
	if (layer.__current && (killpane || entry.ReplaceState)) {
		layer.__current.onDestroy();
		
		try {
			$('#' + layer.__current.__dialogid).remove();
		}
		catch (x) {
			console.log('Unable to remove: ' + x);
		}
		
		// get rid of current spot in history
		if (this.__history.length && (this.__history[this.__history.length - 1] == layer.__current)) 
			this.__history.pop();
	}
	else if (layer.__current) {
		$('#' + layer.__current.__dialogid).hide();
	}
	
	layer.__current = entry;
	
	if (entry.__dialogid) {
		$('#' + entry.__dialogid).show();
		
		if (entry.Loaded && entry.FreezeTop)
			$(layer.__content).animate({ scrollTop: entry.FreezeTop }, "fast");
		else if (entry.TargetElement)
			$(layer.__content).animate({ scrollTop: $('#' + entry.TargetElement).get(0).getBoundingClientRect().top + window.pageYOffset }, "fast");
		else
			$(layer.__content).animate({ scrollTop: 0 }, "fast");
		
		entry.onLoad();
		
		return;
	}
	
	entry.__dialogid = dc.util.Uuid.create();
	
	this.open();
	
	var pane = $('<div></div>');
	pane.attr('id', entry.__dialogid);
	
	$(layer.__content).append(pane).promise().then(function() {
		var page = dc.pui.Loader.__pages[entry.Name];
	
		// layout using 'pageContent' as the top of the chain of parents
		entry.layout(page.Layout, new dc.pui.LayoutEntry({
			Element: $('#' + layer.__current.__dialogid),
			PageEntry: entry
		}));
					
		$('#' + layer.__current.__dialogid).enhanceWithin().promise().then(function() {	
			// TODO review, probably doesn't work - see scrollPage below
			if (entry.Loaded && entry.FreezeTop)
				$(layer.__content).animate({ scrollTop: entry.FreezeTop }, "fast");
			else if (entry.TargetElement)
				$(layer.__content).animate({ scrollTop: $('#' + entry.TargetElement).get(0).getBoundingClientRect().top + window.pageYOffset }, "fast");
			else
				$(layer.__content).animate({ scrollTop: 0 }, "fast");
			
			layer.enhancePage();
			
			Object.getOwnPropertyNames(layer.__observers).forEach(function(name) {
				layer.__observers[name]();
			});
			
			entry.onLoad();
		});
	});
};

dc.pui.dialog.Layer.prototype.clearHistory = function() {
	dc.pui.Layer.prototype.clearHistory.call(this);
	
	$(this.__content).empty();
};

dc.pui.dialog.Layer.prototype.onDestroy = function() {
	dc.pui.Layer.prototype.onDestroy.call(this);
	
	dc.pui.dialog.Loader.destroy();
};

dc.pui.dialog.Layer.prototype.back = function() {
	var entry = this.__history.pop();
	
	if (entry) {
		this.manifestPage(entry, true);
	}
	else {
		this.__current = null;
		this.close();
	}
};

// need to override so that we only enhance current section not the entire layer
dc.pui.dialog.Layer.prototype.enhancePage = function() {
	var layer = this;
	
	$('#' + layer.__current.__dialogid + ' *[data-dcui-mode="enhance"] a').each(function() { 
		var en = $(this).attr('data-enhance');
		
		if (en && (en.toLowerCase() == 'false'))
			return;
		
		var l = $(this).attr('href');
		
		if (!l)
			return;
		
		if (l.startsWith('http:') || l.startsWith('https:') || l.endsWith('.pdf')) {
			$(this).attr('target', '_blank')
			return;
		}
		
		// TODO enhance this, maybe part of it should be in CMS layer
		if (l.charAt(0) == '?') {
			$(this).click(l, function(e) {
				if (! $(this).hasClass('ui-btn')) {
					dc.pui.dialog.Loader.loadPane(e.data.substr(1));
					e.preventDefault();
					return false;
				}
			});
		}
		
		if (l.charAt(0) == '#') {
			$(this).click(l, function(e) {
				layer.scrollPage(e.data);
				e.preventDefault();
				return false;
			});
			return;
		}
		
		// look for a single starting slash
		if ((l.charAt(0) != '/') || (l.charAt(1) == '/'))
			return;
		
		var name = l.substr(l.lastIndexOf('/') + 1);
		
		if (name.indexOf('.') != -1)
			return;
		
		console.log('click x: ' + l);
			
		$(this).click(l, function(e) {
			if (! $(this).hasClass('ui-btn')) {
				console.log('click a: ' + e.data);
			
				layer.loadPage(e.data);
				
				console.log('click b: ' + e.data);
			
				e.preventDefault();
				return false;
			}
		});
	});
};

dc.pui.dialog.Layer.prototype.close = function() {
	$(this.__content).hide();
	dc.pui.dialog.Loader.closeAll();
};


dc.pui.dialog.Layer.prototype.scrollPage = function(selector) {
	$("#" + this.__current.__dialogid).animate({ scrollTop: $(selector).get(0).getBoundingClientRect().top + window.pageYOffset }, "fast");
};


/*
dc.pui.dialog.Layer.prototype.init = function(contentsel, options) {
	dc.pui.Layer.prototype.init.call(this, contentsel, options);
};
*/

