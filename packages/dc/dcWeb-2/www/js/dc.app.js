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


dc.pui = {
};

dc.pui.layer = {
};


dc.pui.layer.Base = function(contentshell, content, options) {
	this.init(contentshell, content, options);
};

dc.pui.layer.Base.prototype = {
	init: function(contentshell, content, options) {
		$.extend(this, { 
			Name: '[unknown]',
			Params: { },
			Current: null,
			Context: null,
			Store: null,
			History: [ ],
			Observers: { },
			ContentShell: contentshell,
			Content: content,
		}, options);

		// TODO add layer Timers	- try to work with Velocity instead of making our own timers
	},
	
	// TODO define use of observer - more than just "onload"
	setObserver: function(name, observer) {
		this.Observers[name] = observer;
	},
	
	deleteObserver: function(name, observer) {
		delete this.Observers[name];
	},
	
	setContentSelector: function(v) {
		this.Content = v;
	},
	
	// call with separate args or with a single "options" record
	loadPage: function(page, params, replaceState, callback) {
		var options = {};
		
		if (dc.util.Struct.isRecord(page)) {
			options = page;
		}
		else {
			var hpage = page.split('#');
			
			options.Name = hpage[0];
			options.Params = params;
			options.ReplaceState = replaceState;
			options.Callback = callback;
			
			if (hpage.length)
				options.TargetElement = hpage[1];
		}
		
		this.loadPageAdv(options);
	},
	loadPageAdv: function(options) {
		if (!options || !options.Name)
			return;
		
		var oldentry = this.Current;
		
		if (oldentry) {
			oldentry.freeze();
			this.History.push(oldentry);
		}
		
		options.Layer = this;
		
		var entry = new dc.pui.PageEntry(options);
		var loader = dc.pui.Loader;
		
		loader.LoadPageId = entry.Id;
		
		// if page is already loaded then show it
		if (loader.Pages[options.Name] && ! loader.StalePages[options.Name]) {
			loader.resumePageLoad();
			return;
		}
		
		delete loader.StalePages[options.Name];		// no longer stale
		
		var script = document.createElement('script');
		script.src = options.Name + '?_dcui=dyn&nocache=' + dc.util.Crypto.makeSimpleKey();
		script.id = 'page' + options.Name.replace(/\//g,'.');
		script.async = false;  	
		
		document.head.appendChild(script);
	},
	
	manifestPage: function(entry) {
		var layer = this;
		var loader = dc.pui.Loader;
		
		if (!entry)
			return;
			
		if (layer.Current) 
			layer.Current.onDestroy();
		
		layer.Current = entry;
		
		this.open();
		
		var page = loader.Pages[entry.Name];
		
		$(layer.Content).empty().append(page.Layout).promise().then(function() {
			if (layer.Content == layer.ContentShell)
				$(layer.Content).attr('class', page.PageClass + ' dcuiLayer');
			else
				$(layer.Content).attr('class', page.PageClass);
				
			if (entry.Loaded && entry.FreezeTop)
				$(layer.Content).scrollTop(entry.FreezeTop);				
			else if (entry.TargetElement)
				$(layer.Content).scrollTop($('#' + entry.TargetElement).get(0).getBoundingClientRect().top + $(layer.Content).scrollTop());
			else
				$(layer.Content).scrollTop(0);				
			
			layer.enhancePage();
			
			Object.getOwnPropertyNames(layer.Observers).forEach(function(name) {
				layer.Observers[name]();
			});
		});			
	},
	
	enhancePage: function() {
		var layer = this;
		
		$(layer.Content + ' *[data-dc-enhance="true"]').each(function() { 
			var tag = $(this).attr('data-dc-tag');
			
			if (!tag || !dc.pui.Tags[tag])
				return;
			
			dc.pui.Tags[tag](layer.Current, this);
		});
		
		layer.Current.onLoad(function() {
			if (layer.Current.Callback)
				layer.Current.Callback.call(layer.Current);
		});
	},
	
	query: function(selector) {
		if (selector)
			return $(this.Content).find(selector);

		return $(this.Content);
	},
	
	closePage: function(opts) {
		if (opts)
			this.loadPage(opts.Path, opts.Params);
		else 
			this.back();
	},
	
	getHistory: function() {
		return this.History;
	},
	
	clearHistory: function() {
		var layer = this;
		
		if (layer.Current) 
			layer.Current.onDestroy();
		
		layer.Current = null;
		
		$(layer.Content).empty();
		
		var entry = this.History.pop();
		
		while (entry) {
			entry.onDestroy();
			entry = this.History.pop();
		}
	},
	
	back: function() {
		var layer = this;
		
		if (layer.Current) 
			layer.Current.onDestroy();
		
		layer.Current = null;
		
		$(layer.Content).empty();
		
		var entry = this.History.pop();
		
		if (entry) {
			this.manifestPage(entry);
		}
		else {
			this.Current = null;
			this.close();
		}
	},
	
	open: function() {
		$(this.ContentShell).show();
	},
	
	close: function() {
		this.clearHistory();

		/* TODO some sort of event
		if (!dmode)
			dc.pui.Loader.currentPageEntry().callPageFunc('dcwDialogEvent', { Name: 'Close', Target: 'DialogPane' });
		*/
		
		$(this.ContentShell).hide();
	},
	
	current: function() {
		return this.Current;
	},
	
	scrollPage: function(selector) {
		var entry = this.Current;
		
		if (entry)
			entry.scrollPage(selector);
	},
	
	onResize: function(e) {
		var entry = this.Current;
		
		if (entry)
			entry.onResize(e);
	},
	
	onDestroy: function(e) {
		var entry = this.Current;		// TODO shouldn't this operate on all history?
		
		if (entry)
			entry.onDestroy(e);
		
		$(this.ContentShell).remove();
	},
	
	onFrame: function(e) {
		var entry = this.Current;
		
		if (entry)
			entry.onFrame(e);
	}
};


dc.pui.layer.Main = function() {
	this.init('#dcuiMain', '#dcuiMain');
};

dc.pui.layer.Main.prototype = new dc.pui.layer.Base();

dc.pui.layer.Main.prototype.FirstLoad = true;

dc.pui.layer.Main.prototype.back = function() {
	window.history.back();
};

dc.pui.layer.Main.prototype.loadPageAdv = function(options) {
	if (! options || ! options.Name)
		return;
	
	// really old browsers simply navigate again, rather than do the advanced page view
	if (! history.pushState) {
		if (window.location.pathname != options.Name)
			window.location = options.Name;
		
		return;
	}
	
	dc.pui.layer.Base.prototype.loadPageAdv.call(this, options);
};

dc.pui.layer.Main.prototype.manifestPage = function(entry, frompop) {
	var layer = this;
	var loader = dc.pui.Loader;
	
	if (!entry)
		return;
	
	var page = loader.Pages[entry.Name];
	
	if (!frompop) {		 //  && !this.FirstLoad) {
		if (entry.ReplaceState)
			history.replaceState(
				{ Id: loader.LoadPageId, Params: entry.Params }, 
				page.Title, 
				entry.Name);
		else
			history.pushState(
				{ Id: loader.LoadPageId, Params: entry.Params }, 
				page.Title, 
				entry.Name);
	}
	
	// remove all layers
	for (var i = 0; i < loader.Layers.length; i++) 
		loader.Layers[i].onDestroy();
	
	loader.Layers = [ ];
	
	if (this.FirstLoad) {
		this.FirstLoad = false;
		layer.Current = entry;
		layer.enhancePage();
		return;
	}
	
	dc.pui.layer.Base.prototype.manifestPage.call(this, entry);
};

dc.pui.layer.Main.prototype.enhancePage = function() {
	dc.pui.layer.Base.prototype.enhancePage.call(this);
	
	var layer = this;
	var loader = dc.pui.Loader;
	var entry = layer.Current;
	var page = loader.Pages[entry.Name];
	
	if (typeof ga == 'function') {
		ga('set', {
			  page: entry.Name,
			  title: page.Title
		});

		ga('send', 'pageview');
	}
	
	if (page.Title)
		document.title = dc.util.Web.unescapeHtml(page.Title);
};

dc.pui.layer.Main.prototype.refreshPage = function() {
	var layer = this;
	var loader = dc.pui.Loader;
	
	var entry = layer.Current;
	var top = $(layer.Content).scrollTop();
	
	loader.clearPageCache(window.location.pathname);
	
	layer.loadPage(window.location.pathname, entry.Params, true, function() {
		$(layer.Content).scrollTop(top);
	});
};


dc.pui.layer.Dialog = function() {
	this.init('#dcuiDialog', '#dcuiDialogPane');
};

dc.pui.layer.Dialog.prototype = new dc.pui.layer.Base();

dc.pui.layer.Dialog.prototype.open = function() {
	var dialog = this;
	var del = $('#dcuiDialog');
	
	if (! del.length) {
		var dbox = $('<div id="dcuiDialog" class="dcuiLayer"></div>');
		
		var dbclose = $('<a href="#" class="dcuiCloseButton"><i class="fa fa-times fa-lg"></i></a>');
		
		$(dbclose).click(function (e) {
			dialog.close();
			
			e.preventDefault();
			return false;
		});
		
		var dbpane = $('<div id="dcuiDialogPane"></div>');
		
		dbox.append(dbclose).append(dbpane);
		
		$('body').append(dbox);
	}

	dc.pui.layer.Base.prototype.open.call(this);
};

// Dialog feature (singleton)
dc.pui.Dialog = new dc.pui.layer.Dialog();


dc.pui.layer.Alert = function() {
	this.init('#dcuiAlert', '#dcuiAlertPane');
};

dc.pui.layer.Alert.prototype = new dc.pui.layer.Base();

dc.pui.layer.Alert.prototype.open = function() {
	var dialog = this;
	var del = $('#dcuiAlert');
	
	if (! del.length) {
		var dbox = $('<div id="dcuiAlert" class="dcuiLayer"></div>');
		
		var dbclose = $('<a href="#" class="dcuiCloseButton"><i class="fa fa-times fa-lg"></i></a>');
		
		$(dbox, dbclose).click(function (e) {
			dialog.close();
			
			e.preventDefault();
			return false;
		});
		
		var dbpane = $('<div id="dcuiAlertPane"></div>');
		
		dbox.append(dbclose).append(dbpane);
		
		$('body').append(dbox);
	}

	dc.pui.layer.Base.prototype.open.call(this);
};

// Alert feature (singleton)
dc.pui.Alert = new dc.pui.layer.Alert();


dc.pui.layer.App = function() {
	this.init('#dcuiApp', '#dcuiAppPane');
};

dc.pui.layer.App.prototype = new dc.pui.layer.Base();

dc.pui.layer.App.prototype.open = function() {
	var dialog = this;
	var del = $('#dcuiApp');
	
	if (! del.length) {
		var dbox = $('<div id="dcuiApp" class="dcuiLayer"></div>');
		
		var dbmenu = $('<div id="dcuiAppMenu"></div>');
		var dbpane = $('<div id="dcuiAppPane"></div>');
		
		dbox
			.append(dbmenu)
			.append(dbpane);
		
		$('body').append(dbox);
		
		/* TODO ??
		pane.setObserver('CMSPane', function() {
			dc.cms.ui.Loader.loadMenu();
		});
		*/
	}

	dc.pui.layer.Base.prototype.open.call(this);
};

dc.pui.layer.App.prototype.start = function(context) {
	var app = this;
	app.Context = context;
	
	this.clearHistory();
	
	app.loadPage(context.Page, context.Params);
};

dc.pui.layer.App.prototype.manifestPage = function(entry, frompop) {
	var app = this;
	
	dc.pui.layer.Base.prototype.manifestPage.call(this, entry);

	app.loadMenu();
};

dc.pui.layer.App.prototype.loadMenu = function() {
	var app = this;
	
	var area = null;
	
	// get area from current pane, if any
	if (app.Current && app.Current.Params._Menu)
		area = app.Current.Params._Menu;
	
	if (!area && app.Context && app.Context.Menu)
		area = app.Context.Menu;
	
	if (!area)
		area = 'dcSignIn';
	
	$('#dcuiAppMenu').empty();
	
	// if any page is loaded, use back
	var hist = app.getHistory();
	
	var mcntrl = $('<div id="dcuiAppMenuCntrl"></div>');
	
	// mobile popup menu
	var node1 = $('<a href="#"></a>');
	node1.append('<i class="fa fa-bars"></i>');
	node1.addClass('mobile');
	
	mcntrl.append(node1);
	
	// help menu
	var node0 = $('<a href="#"></a>');
	node0.append('<i class="fa fa-question"></i>');
	
	node0.click(function(e) {
		var hpage = app.Current.Name + "-help";
		
		app.loadPage(hpage);
	});
	
	mcntrl.append(node0);
	
	// back menu
	var node2 = $('<a id="dcuiIntegratedMenuBack" href="#"></a>');
	node2.append('<i class="fa fa-chevron-left"></i>');
	
	node2.click(function(e) {
		if (! $(this).hasClass('disabled')) {
			app.back();
		}
	});
	
	if (hist.length == 0) 
		node2.addClass('disabled');
	
	mcntrl.append(node2);
	
	// close menu
	var node3 = $('<a href="#"></a>');
	node3.append('<i class="fa fa-times"></i>');
	
	node3.click(function(e) {
		app.close();
	});
	
	mcntrl.append(node3);
	
	$('#dcuiAppMenu').append(mcntrl);
	
	var addbMenu = function(mnu) {
		if (mnu.Auth && !dc.user.isAuthorized(mnu.Auth))
			return;
		
		var node = $('<a href="#" class="pure-button"></a>');
		
		node.text(mnu.Title);
		node.addClass(mnu.Kind);
		
		node.click(mnu, function(e) {
			if (!dc.pui.Apps.busyCheck()) 
				e.data.Op.call(app, e);
			
			e.preventDefault();
			return false;
		});
	
		$('#dcuiAppMenu').append(node);
	};
	
	var amenu = dc.pui.Apps.Menus[area];
	
	if (amenu && amenu.Options) {
		for (var i = 0; i < amenu.Options.length; i++) 
			addbMenu(amenu.Options[i]);
	}
	
	node1.click(amenu, function(e) {
		dc.pui.Popup.menu(e.data);
		
		e.preventDefault();
		return false;
	});
};

// App feature (singleton)
dc.pui.App = new dc.pui.layer.App();


dc.pui.layer.FullScreen = function() {
	this.init('#dcuiFullScreen', '#dcuiFullScreen');
};

dc.pui.layer.FullScreen.prototype = new dc.pui.layer.Base();

dc.pui.layer.FullScreen.prototype.open = function() {
	var dialog = this;
	var del = $('#dcuiFullScreen');
	
	if (! del.length) {
		var dbox = $('<div id="dcuiFullScreen" class="dcuiLayer"></div>');
		
		$('body').append(dbox);
	}

	dc.pui.layer.Base.prototype.open.call(this);
};

// Dialog feature (singleton)
dc.pui.FullScreen = new dc.pui.layer.FullScreen();


// ------------------- end Layer -------------------------------------------------------


dc.pui.Loader = {
	LoadPageId: null,
	Ids: { },
	Pages: { },
	StalePages: { },
	Libs: { },		// TODO fill this with all "Global" scripts on first load
	Styles: { },		// TODO fill this with all "Global" styles on first load
	ExtraLibsCallback: null,
	OriginPage: null,
	OriginHash: null,
	OriginSearch: null,
	FrameRequest: false,
	MainLayer: null,
	Layers: [],
	
	init: function() {
		var loader = this;
		
		loader.MainLayer = new dc.pui.layer.Main(); 
		
		loader.OriginPage = location.pathname;
		loader.OriginHash = location.hash;
		loader.OriginSearch = location.search;
		
		$(window).on('popstate', function(e) {
			//console.log("pop - location: " + document.location + ", state: " + JSON.stringify(e.originalEvent.state));
			
			var state = e.originalEvent.state;
			
			var id = state ? state.Id : null; 
			
			if (id) {
				loader.LoadPageId = id;
				
				if (loader.Ids[id])
					loader.MainLayer.manifestPage(loader.Ids[id], true);
				else
					loader.MainLayer.loadPage(document.location.pathname, state.Params);
			}
	    });
		
		// watch for orientation change or resize events
		$(window).on('orientationchange resize', function (e) {
			loader.MainLayer.onResize(e);
				
			for (var i = 0; i < loader.Layers.length; i++) 
				loader.Layers[i].onResize(e);
				
			loader.requestFrame();
		});
		
		if (document.fonts) {
			document.fonts.onloadingdone = function(e) {
				// force all canvas updates that may be using loaded forms
				loader.requestFrame();
			}
		}
	},
	signout: function() {
		dc.user.signout();
		dc.user.saveRememberedUser();

		window.location = '/';
	},
	
	loadPage: function(page, params, replaceState, callback) {
		this.MainLayer.loadPage(page, params, replaceState, callback);
	},
	addPageDefinition: function(name, def) {
		var loader = this;
		
		loader.Pages[name] = def;
	},
	resumePageLoad: function() {
		var loader = this;
		
		var entry = loader.Ids[loader.LoadPageId];
		
		if (!entry)
			return;
		
		var page = loader.Pages[entry.Name];
		
		dc.schema.Manager.load(page.RequireType);
		
		dc.lang.Dict.load(page.RequireTr);

		var needWait = false;
		
		if (page.RequireStyles) {
			for (var i = 0; i < page.RequireStyles.length; i++) {
				var path = page.RequireStyles[i];
				
				if (loader.Styles[path])
					continue;
				
				$('head').append('<link rel="stylesheet" type="text/css" href="' + path + '?nocache=' + dc.util.Crypto.makeSimpleKey() + '" />'); 
				
				loader.Styles[path] = true;		// not really yet, but as good as we can reasonably get
				needWait = true;
			}
		}
		
		if (page.RequireLibs) {
			for (var i = 0; i < page.RequireLibs.length; i++) {
				var path = page.RequireLibs[i];
				
				if (loader.Libs[path])
					continue;
				
				var script = document.createElement('script');
				script.src = path + '?nocache=' + dc.util.Crypto.makeSimpleKey();
				script.id = 'req' + path.replace(/\//g,'.');					
				script.async = false;  	// needed when loading additional libraries, we can inject a final fake script that echos 
										// a param (e.g. ?opid=3345) to us saying that it is loaded and hence all preceding scripts are also loaded
				
				document.head.appendChild(script);
				
				loader.Libs[path] = true;		// not really yet, but as good as we can reasonably get
				needWait = true;
			}
		}
		
		if (needWait) {
			var key = dc.util.Crypto.makeSimpleKey();
			
			var script = document.createElement('script');
			script.src = '/js/dc.require.js?nocache=' + key;
			script.id = 'lib' + key;					
			script.async = false;  	// needed when loading additional libraries, we can inject a final fake script that echos 
									// a param (e.g. ?opid=3345) to us saying that it is loaded and hence all preceding scripts are also loaded
			
			document.head.appendChild(script);
			
			return;
		}
		
		entry.Layer.manifestPage(entry);
	},
	finializePageLoad: function() {
		var loader = this;
		
		var entry = loader.Ids[loader.LoadPageId];
		
		if (entry)
			entry.Layer.manifestPage(entry);
	},
	clearPageCache: function(page) {
		var loader = this;
		
		if (page)
			loader.StalePages[page] = true; 
	},
	failedPageLoad: function(reason) {
		var loader = this;
		
		dc.pui.Popup.alert('Failed to load page, you may not have permissions for it.');
	},
	callbackExtraLibs: function() {
		var loader = this;
		
		if (loader.ExtraLibsCallback)
			loader.ExtraLibsCallback();
	},
	addExtraLibs: function(scripts, cb) {
		var loader = this;
		
		var needWait = false;
		
		loader.ExtraLibsCallback = cb;
		
		for (var i = 0; i < scripts.length; i++) {
			var path = scripts[i];
			
			if (loader.Libs[path])
				continue;
			
			var script = document.createElement('script');
			script.src = path + '?nocache=' + dc.util.Crypto.makeSimpleKey();
			script.id = 'req' + path.replace(/\//g,'.');					
			script.async = false;  	// needed when loading additional libraries, we can inject a final fake script that echos 
									// a param (e.g. ?opid=3345) to us saying that it is loaded and hence all preceding scripts are also loaded
			
			document.head.appendChild(script);
			
			loader.Libs[path] = true;		// not really yet, but as good as we can reasonably get
			needWait = true;
		}
		
		if (needWait) {
			var key = dc.util.Crypto.makeSimpleKey();
			
			var script = document.createElement('script');
			script.src = '/js/dc.extra-lib-callback.js?nocache=' + key;
			script.id = 'lib' + key;					
			script.async = false;  	// needed when loading additional libraries, we can inject a final fake script that echos 
									// a param (e.g. ?opid=3345) to us saying that it is loaded and hence all preceding scripts are also loaded
			
			document.head.appendChild(script);
		}
		else {
			if (loader.ExtraLibsCallback)
				loader.ExtraLibsCallback();
		}
	},
	addExtraStyles: function(styles, cb) {
		var loader = this;
		
		var needWait = false;
		
		loader.ExtraLibsCallback = cb;
		
		for (var i = 0; i < styles.length; i++) {
			var path = styles[i];
			
			if (loader.Styles[path])
				continue;
			
			$('head').append('<link rel="stylesheet" type="text/css" href="' + path + '?nocache=' + dc.util.Crypto.makeSimpleKey() + '" />'); 
			
			loader.Styles[path] = true;		// not really yet, but as good as we can reasonably get
			needWait = true;
		}
		
		if (needWait) {
			var key = dc.util.Crypto.makeSimpleKey();
			
			var script = document.createElement('script');
			script.src = '/js/dc.extra-lib-callback.js?nocache=' + key;		// TODO consolidate with dc.require.js ...
			script.id = 'lib' + key;					
			script.async = false;  	// needed when loading additional libraries, we can inject a final fake script that echos 
									// a param (e.g. ?opid=3345) to us saying that it is loaded and hence all preceding scripts are also loaded
			
			document.head.appendChild(script);
		}
		else {
			if (loader.ExtraLibsCallback)
				loader.ExtraLibsCallback();
		}
	},
	requestFrame: function() {
		var loader = this;
		
		if (!loader.FrameRequest) {
			window.requestAnimationFrame(loader.buildFrame);
			loader.FrameRequest = true;
		}
	},
	buildFrame: function(e) {
		var loader = this;
		
		loader.FrameRequest = false;
		
		var entry = loader.Current;
		
		if (entry)
			entry.onFrame(e);
		
		if (!loader.Layers)
			return;
		
		for (var i = 0; i < loader.Layers.length; i++) 
			loader.Layers[i].onFrame(e);
	},
	getLayer: function(name) {
		var loader = this;
		
		for (var i = 0; i < loader.Layers.length; i++) {
			var l = loader.Layers[i];
			
			if (l.Name == name)
				return l;
		}
		
		return null;
	},
	addLayer: function(layer) {
		var loader = this;
		
		loader.Layers.push(layer);
	}
};

dc.pui.Apps = {
	Busy: false,				// dcui is busy and not accepting new clicks right now - especially for submits 
		
	sessionChanged: function() {
		dc.user.updateUser(false, function() {
			// TODO maybe change page if not auth tags? or if became guest
		}, true);
		
		if (dc.handler && dc.handler.sessionChanged)
			dc.handler.sessionChanged();
	},
	busyCheck: function() {
		if (this.Busy) {		// protect against user submit such as Enter in a TextField
			console.log('click denied, dcui is busy');			// TODO if we have been busy for more than 2 seconds show a message screen...obviously someone is trying to click while nothing appears to be happening, hide the screen after load is done - unless someone else updated it
			return true;
		}
		
		return false;
	},
	activateCms: function(options) {
		dc.pui.Apps.loadCms(function() {
			dc.cms.Loader.init(options);		
		});
	},
	loadCms: function(cb) {
		dc.pui.Loader.addExtraStyles([ '/css/dcm.cms.css' ], function() {
			dc.pui.Loader.addExtraLibs([ '/js/dcm.cms.js' ], function() {
				if (cb)
					cb();
			});
		});
	},
	Menus: {
		dcSignIn: {
			Options: [
				{
					Title: 'Welcome',
					Kind: 'Option',
					Op: function(e) {
						dc.pui.App.loadPage('/dcw/Welcome');
					}
				},
				{
					Title: 'Sign In',
					Kind: 'Option',
					Op: function(e) {
						dc.pui.App.loadPage('/dcw/SignIn');
					}
				},
				{
					Title: 'Password Reset',
					Kind: 'Option',
					Op: function(e) {
						dc.pui.App.loadPage('/dcw/Reset');
					}
				},
				{
					Title: 'My Account',
					Auth: [ 'User' ],
					Op: function(e) {
						dc.pui.Dialog.loadPage('/dcw/EditSelf');
					}
				},
				{
					Title: 'Sign Out',
					Auth: [ 'User' ],
					Kind: 'Option',
					Op: function(e) {
						dc.pui.Loader.signout();
					}
				}
			]
		}
	}
};

dc.pui.Popup = {
	alert: function(msg, callback, title) {
		dc.pui.Alert.loadPage('/dcw/Alert', { Message: msg, Callback: callback, Title: title })
	},

	confirm: function(msg, callback, title) {
		dc.pui.Alert.loadPage('/dcw/Confirm', { Message: msg, Callback: callback, Title: title })
	},

	menu: function(menu) {
		if (dc.util.String.isString(menu))
			menu = dc.pui.Apps.Menus[menu];
		
		dc.pui.Alert.loadPage('/dcw/Menu', { Menu: menu })
	}
};

// ------------------- end Loader/Popup -------------------------------------------------------

dc.pui.PageEntry = function(options) {
	$.extend(this, { 
			Name: '/na',
			Params: { },
			TargetElement: null,
			Callback: null,
			Layer: null,		// layer entry belongs to, null for default
			Store: { },
			Forms: { },
			Timers: [],
			_onResize: [],
			_onFrame: [],
			_onDestroy: []
		}, options);
		
	var id = this.Name + '@' + dc.util.Uuid.create().replace(/-/g,'');
	
	this.Id = id;
	
	dc.pui.Loader.Ids[id] = this;
}

dc.pui.PageEntry.prototype = {
	getDefinition: function() {
		return dc.pui.Loader.Pages[this.Name];
	},
	onLoad: function(callback) {
		var page = dc.pui.Loader.Pages[this.Name];
		var entry = this;
		
		var steps = [ ];
		
		for (var i = 0; i < page.LoadFunctions.length; i++) {
			steps.push({
				Alias: 'LoadPageFunc' + i,
				Title: 'Load page function: ' + i,
				Params: {
					Idx: i
				},
				Func: function(step) {
					var event = { Wait: false, Task: this };
					
					var func = page.LoadFunctions[step.Params.Idx];
					
					if (dc.util.String.isString(func)) 
						entry.callPageFunc(func, event);
					else
						func.call(entry, event);
					
					if (! event.Wait)
						this.resume();
				}
			});
		}

		steps.push({
			Alias: 'MainLoadPage',
			Title: 'Main load page function',
			Func: function(step) {
				var event = { Wait: false, Task: this };

				entry.callPageFunc('Load', event); 
				
				if (! event.Wait)
					this.resume();
			}
		});

		// now load forms, if any
		Object.getOwnPropertyNames(entry.Forms).forEach(function(name) {
			steps.push({
				Alias: 'LoadFormFunc' + name,
				Title: 'Load form function: ' + name,
				Params: {
					Name: name
				},
				Func: function(step) {
					var task = this;
					
					entry.Forms[step.Params.Name][entry.Loaded ? 'thaw' : 'load'](function() {
						task.resume();
					});
				}
			});
		});

		var loadtask = new dc.lang.Task(steps, function(res) {
			//console.log('observer: ' + res.Code);
			
			entry.Loaded = true;
			
			if (callback)
				callback.call(entry);
		});
		
		loadtask.run();		
	},
	
	callPageFunc: function(method) {
		var page = dc.pui.Loader.Pages[this.Name];
		
		if (page && page.Functions[method]) 
			return page.Functions[method].apply(this, Array.prototype.slice.call(arguments, 1));
			
		return null;
	},
	
	query: function(selector) {
		return this.Layer.query(selector);
	},
	
	form: function(name) {
		if (name)
			return this.Forms[name];
	
		// if no name then return the first we find
		var fnames = Object.getOwnPropertyNames(this.Forms);
		
		if (fnames)
			return this.Forms[fnames[0]];
		
		return null;
	},
	
	formQuery: function(name) {
		if (name && this.Forms[name])
			return $('#' + this.Forms[name].Id);
		
		return $('#__unreal');
	},
	
	freeze: function() {
		var entry = this;
		var page = dc.pui.Loader.Pages[entry.Name];
		
		entry.FreezeTop = $(entry.Layer.Content).scrollTop();
		
		entry.callPageFunc('Freeze'); 
		
		Object.getOwnPropertyNames(entry.Forms).forEach(function(name) {
			entry.Forms[name].freeze();
		});
	},
	
	onResize: function(e) {
		var page = dc.pui.Loader.Pages[this.Name];
		
		this.callPageFunc('onResize', e); 
		
		for (var i = 0; i < this._onResize.length; i++) 
			this._onResize[i].call(this, e);
	},
	
	registerResize: function(callback) {
		this._onResize.push(callback);
	},
	
	onFrame: function(e) {
		var page = dc.pui.Loader.Pages[this.Name];
	    var now = Date.now();
		
		this.callPageFunc('onFrame', e); 
		
		for (var i = 0; i < this._onFrame.length; i++) {
			var render = this._onFrame[i];
			
		    var delta = now - render.Then;
		     
		    if (delta > render.Interval) {
		        // adjust so lag time is removed, produce even rendering 
		    	render.Then = now - (delta % render.Interval);
		         
				render.Run.call(this, e);
		    }
		    else {
				dc.pui.Loader.requestFrame();	// keep calling until we don't skip
		    }
		}
	},
	
	registerFrame: function(render) {
		render.Then = Date.now();
		render.Interval = 1000 / render.Fps;
		
		this._onFrame.push(render);
	},
			
	onDestroy: function() {
		var page = dc.pui.Loader.Pages[this.Name];

		// clear the old timers
		for (var x = 0; x < this.Timers.length; x++) {
			var tim = this.Timers[x];
			
			if (!tim)
				continue;
			
			if (tim.Tid)
				window.clearTimeout(tim.Tid);
			else if (tim.Iid)
				window.clearInterval(tim.Iid);
		}
		
		for (var i = 0; i < this._onDestroy.length; i++) 
			this._onDestroy[i].call(this);
		
		this._onResize = [];
		this._onDestroy = [];
		this._onFrame = [];
		this.Timers = [];
		
		// run the destroy on old page
		this.callPageFunc('onDestroy'); 
	},
				
	registerDestroy: function(callback) {
		this._onDestroy.push(callback);
	},
	
	allocateTimeout: function(options) {
		var entry = this;
		
		var pos = this.Timers.length;
		
		options.Tid = window.setTimeout(function () {
				window.clearTimeout(options.Tid);		// no longer need to clear this later, it is called
				entry.Timers[pos] = null;
				
				options.Op.call(this, options.Data);
			}, 
			options.Period);
		
		this.Timers.push(options);		
	},
	
	allocateInterval: function(options) {
		var entry = this;
		
		options.Iid = window.setInterval(function () {
				options.Op.call(this, options.Data);
			}, 
			options.Period);
		
		entry.Timers.push(options);		
	},
		
	formForInput: function(el) {
		var entry = this;
		var res = null;
		var fel = $(el).closest('form');
		var id = $(fel).attr('id');
		
		Object.getOwnPropertyNames(entry.Forms).forEach(function(name) {
			var f = entry.Forms[name];
			
			if (f.Id == id)
				res = f;
		});
			
		return res;
	},
		
	// list of results from validateForm
	validate: function() {
		var entry = this;
		var res = [ ];
		
		Object.getOwnPropertyNames(entry.Forms).forEach(function(name) {
			res.push(entry.Forms[name].validate());
		});
				
		return res;
	}
};

// ------------------- end PageEntry -------------------------------------------------------

dc.pui.Form = function(pageEntry, node) {
	$.extend(this, { 
			Name: '[unknown]',
			PageEntry: pageEntry,		// page entry that form belongs to, null for default
			Inputs: { },
			InputOrder: [ ],
			Prefix: null,
			RecordOrder: [ "Default" ],
			RecordMap: { }, 		 // map records to data types 
			AsNew: { },
			AlwaysNew: false,
			Managed: false,
			Focus: null,
			TitleFields: [ ],
			FreezeInfo: null		//  { [recordname]: { Originals: { [fld]: [value] }, Values: { [fld]: [value] } }, [records]... }
		});
	
	this.Id = $(node).attr('id');
	
	this.Name = $(node).attr('data-dcf-name');
	
	var recorder = $(node).attr('data-dcf-record-order');
	var rectype = $(node).attr('data-dcf-record-type');
	
	var focus = $(node).attr('data-dcf-focus');
	var prefix = $(node).attr('data-dcf-prefix');
	var alwaysnew = $(node).attr('data-dcf-always-new');
	var managed = $(node).attr('data-dcf-managed');
	var titles = $(node).attr('data-dcf-title-fields');
	
	recorder = dc.util.String.isString(recorder) ? recorder.split(',') : [ 'Default' ];
	rectype = dc.util.String.isString(rectype) ? rectype.split(',') : [ '' ];
	
	this.RecordOrder = recorder;
	
	for (var im = 0; (im < recorder.length) && (im < rectype.length); im++) 
		this.RecordMap[recorder[im]] = dc.schema.Manager.resolveType(rectype[im]);
	
	if (dc.util.String.isString(alwaysnew))
		this.AlwaysNew = (alwaysnew == 'true');
	
	if (dc.util.String.isString(managed))
		this.Managed = (managed == 'true');
	
	if (dc.util.String.isString(focus))
		this.Focus = focus;
	
	if (dc.util.String.isString(prefix))
		this.Prefix = prefix;
	
	if (dc.util.String.isString(titles))
		this.TitleFields = titles.split(',');
}

dc.pui.Form.prototype = {
	input: function(name) {
		return this.Inputs[name];
	},
	
	query: function(selector) {
		if (selector)
			return $('#' + this.Id).find(selector);
		
		return $('#' + this.Id);
	},
	
	inputQuery: function(name) {
		var inp = this.Inputs[name];		// TODO use Input's "query" - return container control if more than one control
		
		return $('#' + (inp ? inp.Id : '__unreal'));
	},
	
	submit: function() {
		return $('#' + this.Id).submit();
	},
	
	getValue: function(field) { 
		var form = this;
		
		if (form.Inputs[field])
			return form.Inputs[field].getValue();
			
		return null;
	},
	
	setValue: function(field, value) { 
		var form = this;
		
		if (form.Inputs[field])
			form.Inputs[field].setValue(value);
	},
	
	getValues: function() {
		var form = this;
		
		var values = { };
		
		Object.getOwnPropertyNames(form.Inputs).forEach(function(name) {
			var iinfo = form.Inputs[name];
			values[iinfo.Field] = iinfo.getValue();
		});
		
		return values;
	},
					
	load: function(callback) {
		var form = this;
		
		if(!form.RecordOrder) {
			callback();
			return;
		}

		var steps = [ ];
		
		steps.push( {
			Alias: 'BeforeLoad',
			Title: 'Before Load',
			Func: function(step) {
				var task = this;
				
				var event = { 
					Task: task,
					Wait: false,
					Stop: false
				};
			
				// do before load record event
				task.Store.Form.raiseEvent('BeforeLoad', event);
			
				if (event.Stop) 			// quit the task
					task.kill();
				else if (! event.Wait) 			//  continue the task, unless told to wait
					task.resume();
			}
		} );

		// TODO add before load controls
		
		for (var i = 0 ; i < form.RecordOrder.length; i++) {
			var rec = form.RecordOrder[i];
			
			steps.push( {
				Alias: 'LoadRecord',
				Title: 'Load Record',
				Params: {
					Record: rec
				},
				Func: function(step) {
					var task = this;

					task.Store.Current = {
						Result: 0,
						Message: null,
						Record: step.Params.Record,
						Data: { },
						AsNew: false
					};
					
					task.Store.Form.initChanges(step.Params.Record);
				
					var event = { 
						Task: task,
						Wait: false,
						Stop: false,
						Record: step.Params.Record,
						Data: { },
						AsNew: false
					};
				
					// do before save record event
					task.Store.Form.raiseEvent('LoadRecord', event);
				
					if (event.Stop) {			// quit the task
						task.kill();
						return;
					}
				
					if (event.Wait)
						return;
					
					// if you use Wait then set task.Store.Current before resume
					// otherwise set the result into event Data
					task.Store.Current.Data = event.Data;
					task.Store.Current.AsNew = event.AsNew;
					
					if (event.Alert) {
						dc.pui.Popup.alert(event.Alert, function() {
							task.kill();
						});
						
						return;
					}
					
					if (event.Message) {					
						dc.comm.sendMessage(event.Message, function (e) {
							if (e.Result != 0) { 
								var ignore = false;
								
								if (event.IgnoreResults) {
									for (var i = 0; i < event.IgnoreResults.length; i++) 
										if (event.IgnoreResults[i] == e.Result) {
											ignore = true;
											break;
										}
								}
								
								if (!ignore) {
									dc.pui.Popup.alert(e.Message);
									task.kill();
									return;
								}
							}
							
							task.Store.Current.Data = e.Body;
							task.Store.Current.Result = e.Result;
							task.Store.Current.Message = e.Message;
							
							task.resume();
						});
					}
					else {
						task.resume();
					}	
				}
			} );		
			
			steps.push( {
				Alias: 'AfterLoadRecord',
				Title: 'After Load Record',
				Params: {
					Record: rec
				},
				Func: function(step) {
					var task = this;
					
					var event = { 
						Task: task,
						Wait: false,
						Stop: false,
						Record: step.Params.Record,
						Data: task.Store.Current.Data,
						AsNew: task.Store.Current.AsNew
					};
				
					// do after load record event
					task.Store.Form.raiseEvent('AfterLoadRecord', event);
				
					if (event.Stop) {			// quit the task
						task.kill();
						return;
					}
				
					if (event.Wait)
						return;
					
					// if you use Wait then set task.Store.Current before resume
					// otherwise set the result into event Data
					task.Store.Current.Data = event.Data;
					task.Store.Current.AsNew = event.AsNew;
					
					if (event.Alert) {
						dc.pui.Popup.alert(event.Alert, function() {
							task.kill();
						});
						
						return;
					}
					
					task.resume();
				}
			} );		
			
			steps.push( {
				Alias: 'FillRecordInputs',
				Title: 'Fill Record Inputs',
				Params: {
					Record: rec
				},
				Func: function(step) {
					var task = this;
					
					task.Store.Form.loadRecord(task.Store.Current);
					
					task.resume();
				}
			} );		
		}
		
		// TODO AfterLoadCtrls
		
		steps.push( {
			Alias: 'AfterLoad',
			Title: 'After Load',
			Func: function(step) {
				var task = this;
				
				task.Store.Current = {
					Result: 0,
					Message: null,
					Record: null,
					Data: { },
					AsNew: false
				};
				
				var event = { 
					Task: task,
					Wait: false,
					Stop: false
				};
			
				// do before save record event
				task.Store.Form.raiseEvent('AfterLoad', event);
			
				if (event.Stop) {			// quit the task
					task.kill();
					return;
				}
			
				if (event.Wait)
					return;
				
				if (event.Alert) {
					dc.pui.Popup.alert(event.Alert, function() {
						task.kill();
					});
					
					return;
				}
				
				task.resume();				
			}
		} );		

		var loadtask = new dc.lang.Task(steps, function(res) {
			//console.log('observer: ' + res.Code);
			callback();
		});
		
		loadtask.Store = {
			Form: form,
			Current: {
				Result: 0,
				Message: null,
				Record: null,
				Data: { },
				AsNew: false
			}
		};
		
		loadtask.run();				
	},
	
	loadDefaultRecord: function(data, asNew) {
		return this.loadRecord( { 
			Record: 'Default', 
			Data: data, 
			AsNew: asNew
		});
	},
	
	loadRecord: function(info) {
		var form = this;
		
		if (info.AsNew)
			form.AsNew[info.Record] = true;

		if (!info.Data) 
			return;
		
		Object.getOwnPropertyNames(form.Inputs).forEach(function(name) {
			var iinfo = form.Inputs[name];
			
			if ((iinfo.Record == info.Record) && info.Data.hasOwnProperty(iinfo.Field)) {
				iinfo.setValue(info.Data[iinfo.Field]);
				iinfo.OriginalValue = info.Data[iinfo.Field];  
			}
		});
	},
					
	thaw: function(callback) {
		var form = this;
		
		if(!form.RecordOrder) {
			callback();
			return;
		}

		var steps = [ ];
		
		steps.push( {
			Alias: 'BeforeThaw',
			Title: 'Before Thaw',
			Func: function(step) {
				var task = this;
				
				var event = { 
					Task: task,
					Wait: false,
					Stop: false
				};
			
				// do before load record event
				task.Store.Form.raiseEvent('BeforeThaw', event);
			
				if (event.Stop) 			// quit the task
					task.kill();
				else if (! event.Wait) 			//  continue the task, unless told to wait
					task.resume();
			}
		} );

		// TODO add before thaw controls
		
		for (var i = 0 ; i < form.RecordOrder.length; i++) {
			var rec = form.RecordOrder[i];
			
			steps.push( {
				Alias: 'ThawRecord',
				Title: 'Thaw Record',
				Params: {
					Record: rec
				},
				Func: function(step) {
					var task = this;

					task.Store.Current = {
						Result: 0,
						Message: null,
						Data: { },
						AsNew: false,
						Record: step.Params.Record,
						Data: task.Store.Form.FreezeInfo[step.Params.Record].Values,
						Originals: task.Store.Form.FreezeInfo[step.Params.Record].Originals
					};
				
					var event = { 
						Task: task,
						Wait: false,
						Stop: false,
					};
				
					// do before thaw record event, set task.Store.Current before resume
					task.Store.Form.raiseEvent('ThawRecord', event);
				
					if (event.Stop) {			// quit the task
						task.kill();
						return;
					}
				
					if (event.Wait)
						return;
					
					if (event.Alert) {
						dc.pui.Popup.alert(event.Alert, function() {
							task.kill();
						});
						
						return;
					}
					
					task.resume();
				}
			} );		
			
			steps.push( {
				Alias: 'FillRecordInputs',
				Title: 'Fill Record Inputs',
				Params: {
					Record: rec
				},
				Func: function(step) {
					var task = this;
					
					task.Store.Form.thawRecord(task.Store.Current);
					
					task.resume();
				}
			} );		
		}
		
		// TODO AfterThawCtrls
		
		steps.push( {
			Alias: 'AfterThaw',
			Title: 'After Thaw',
			Func: function(step) {
				var task = this;
				
				task.Store.Current = {
					Result: 0,
					Message: null,
					Record: null,
					Data: { },
					AsNew: false
				};
				
				var event = { 
					Task: task,
					Wait: false,
					Stop: false
				};
			
				// do before save record event
				task.Store.Form.raiseEvent('AfterThaw', event);
			
				if (event.Stop) {			// quit the task
					task.kill();
					return;
				}
			
				if (event.Wait)
					return;
				
				if (event.Alert) {
					dc.pui.Popup.alert(event.Alert, function() {
						task.kill();
					});
					
					return;
				}
				
				task.resume();				
			}
		} );		

		var thawtask = new dc.lang.Task(steps, function(res) {
			//console.log('observer: ' + res.Code);
			form.FreezeInfo = null;
				
			callback();
		});
		
		thawtask.Store = {
			Form: form,
			Current: {
				Result: 0,
				Message: null,
				Record: null,
				Data: { },
				AsNew: false
			}
		};
		
		thawtask.run();				
	},
	
	thawRecord: function(info) {
		var form = this;

		if (!info.Data) 
			return;
		
		Object.getOwnPropertyNames(form.Inputs).forEach(function(name) {
			var iinfo = form.Inputs[name];
			
			if ((iinfo.Record == info.Record) && info.Data.hasOwnProperty(iinfo.Field)) {
				iinfo.setValue(info.Data[iinfo.Field]);
				iinfo.OriginalValue = info.Originals[iinfo.Field];
			}
		});
	},

	freeze: function() {
		var form = this;
		
		form.FreezeInfo = { };
		
		if(!form.RecordOrder) 
			return;
		
		for (var i = 0; i < form.RecordOrder.length; i++)
			form.freezeRecord(form.RecordOrder[i]);
	},
	
	freezeRecord: function(recname) { 
		var form = this;
		
		form.FreezeInfo[recname] = { 
				Originals: { },
				Values: { }
		};
		
		Object.getOwnPropertyNames(form.Inputs).forEach(function(name) {
			var iinfo = form.Inputs[name];
			
			if (iinfo.Record == recname) {
				form.FreezeInfo[recname].Originals[iinfo.Field] = iinfo.OriginalValue;
				form.FreezeInfo[recname].Values[iinfo.Field] = iinfo.getValue();
			}
		});
	},
	
	save: function(callback) {
		var form = this;
		
		if (!form.RecordOrder) {
			callback();
			return;
		}

		var steps = [ ];
		
		steps.push( {
			Alias: 'BeforeSave',
			Title: 'Before Save',
			Func: function(step) {
				var task = this;
				
				var event = { 
					Task: task,
					Wait: false,
					Stop: false,
					Changed: false
				};
			
				// do before save record event
				task.Store.Form.raiseEvent('BeforeSave', event);
			
				if (event.Changed)
					task.Store.AnyChanged = true;
			
				if (event.Stop) 			// quit the task
					task.kill();
				else if (! event.Wait) 			//  continue the task, unless told to wait
					task.resume();
			}
		} );

		// TODO add before save controls
		
		for (var i = 0 ; i < form.RecordOrder.length; i++) {
			var rec = form.RecordOrder[i];
			
			steps.push( {
				Alias: 'SaveRecord',
				Title: 'Save Record',
				Params: {
					Record: rec
				},
				Func: function(step) {
					var task = this;
					
					task.Store.Current = {
						Result: 0,
						Message: null,
						Record: step.Params.Record,
						Data: task.Store.Form.getChanges(step.Params.Record),
						AnyChanged: false,		// for current record
						AfterData: null,
						AfterFlag: false
					};
					
					if (! task.Store.Form.isChanged(step.Params.Record)) {
						task.resume();
						return;
					}
				
					task.Store.AnyChanged = true;
					task.Store.Current.AnyChanged = true;
				
					var event = { 
						Task: task,
						Wait: false,
						Stop: false,
						Record: task.Store.Current.Record,
						Data: task.Store.Current.Data
					};
					
					if (task.Store.Form.Managed) {
						var title = '';
						
						for (var n = 0; n < task.Store.Form.TitleFields.length; n++) {
							var fname = task.Store.Form.TitleFields[n];
							
							if (event.Data[fname]) {
								if (title.length != 0)
									title += ' - ';
								
								title += event.Data[fname];
							}
						}
						
						// form the save message
						event.Message = {
							"Service":"dcmCore",
							"Feature":"ManagedForm",
							"Op":"Submit", 
							Body: { 
								Form: task.Store.Form.Name, 
								Title: title, 
								Data: event.Data
							}
						};
					}
					else {
						// do before save record event
						task.Store.Form.raiseEvent('SaveRecord', event);
					}
					
					if (event.Stop) {			// quit the task
						task.kill();
						return;
					}
				
					if (event.Wait)
						return;
					
					// if you use Wait then set task.Store.Current before resume
					// otherwise set the result into event Data
					task.Store.Current.Data = event.Data;
					
					if (event.Alert) {
						dc.pui.Popup.alert(event.Alert, function() {
							task.kill();
						});
						
						return;
					}
					
					if (event.Message) {					
						dc.comm.sendMessage(event.Message, function (e) {
							if (e.Result != 0) { 
								dc.pui.Popup.alert(e.Message);
								task.kill();
								return;
							}
					
							task.Store.Current.AfterFlag = true;
							task.Store.Current.AfterData = e.Body;
							task.Store.Current.Result = e.Result;
							task.Store.Current.Message = e.Message;
				
							task.Store.Form.clearChanges(step.Params.Record);
							
							task.resume();
						});
					}
					else {
						task.Store.Form.clearChanges(step.Params.Record);
				
						task.resume();
					}	
				}
			} );		
			
			steps.push( {
				Alias: 'AfterSaveRecord',
				Title: 'After Save Record',
				Params: {
					Record: rec
				},
				Func: function(step) {
					var task = this;
					
					if (! task.Store.Current.AnyChanged) {
						task.resume();
						return;
					}
					
					var event = { 
						Task: task,
						Wait: false,
						Stop: false,
						Record: step.Params.Record,
						Data: task.Store.Current.AfterData
					};
				
					// do before save record event
					task.Store.Form.raiseEvent('AfterSaveRecord', event);
				
					if (event.Stop) {			// quit the task
						task.kill();
						return;
					}
				
					if (event.Wait)
						return;
					
					// if you use Wait then set task.Store.Current before resume
					// otherwise set the result into event Data
					task.Store.Current.AfterData = event.Data;
					
					if (event.Alert) {
						dc.pui.Popup.alert(event.Alert, function() {
							task.kill();
						});
						
						return;
					}
					
					task.resume();
				}
			} );		
			
			// AfterSaveCtrls
			
			Object.getOwnPropertyNames(form.Inputs).forEach(function(name) {
				var iinfo = form.Inputs[name];
				
				if (iinfo.Record != rec)
					return;
				
				if (iinfo.onAfterSave) { 
					//changes[name] = iinfo.getValue();
					
					steps.push( {
						Alias: 'AfterSaveControl',
						Title: 'After Save Control',
						Params: {
							Record: rec,
							Input: iinfo
						},
						Func: function(step) {
							var task = this;
							
							if (! task.Store.Current.AnyChanged) {
								task.resume();
								return;
							}
							
							var event = { 
								Task: task,
								Record: step.Params.Record,
								Data: task.Store.Current.AfterData
							};
							
							// control has to resume/kill/etc
							step.Params.Input.onAfterSave(event);
						}
					} );		
				}
			});
			
			if (form.Managed) {
				steps.push( {
					Alias: 'AfterSaveManaged',
					Title: 'After Save Managed Event',
					Params: {
						Record: rec
					},
					Func: function(step) {
						var task = this;
						
						if (! task.Store.Current.AnyChanged) {
							task.resume();
							return;
						}
						
						dc.comm.sendMessage({
							Service: "dcmCore",
							Feature: "ManagedForm",
							Op: "Complete", 
							Body: { 
								Form: task.Store.Form.Name,
								Token: task.Store.Current.AfterData.Token
							}
						}, 
						function (e) {
							if (e.Result != 0) { 
								dc.pui.Popup.alert(e.Message);
								task.kill();
								return;
							}
							
							task.resume();
						});
					}
				} );		
			}
		}
		
		steps.push( {
			Alias: 'AfterSave',
			Title: 'After Save',
			Func: function(step) {
				var task = this;
				
				var event = { 
					Task: task,
					Wait: false,
					Stop: false,
					Changed: task.Store.AnyChanged,
					DefaultSaved: false
				};
			
				if (task.Store.Form.Managed) {
					task.Store.Form.query('a[data-dc-tag="dcf.SubmitButton"]').addClass('pure-button-disabled');
					event.Alert = 'Form successfully submitted.';
				}
				
				// do before save record event
				task.Store.Form.raiseEvent('AfterSave', event);
			
				if (event.Stop) {			// quit the task
					task.kill();
					return;
				}
			
				if (event.Wait)
					return;
				
				task.Store.Current = {
					Result: 0,
					Message: null,
					Record: null,
					Data: { },
					AnyChanged: false,		// for current record
					AfterData: null,
					AfterFlag: false,
				};
				
				if (event.Alert) {
					dc.pui.Popup.alert(event.Alert, function() {
						task.kill();
					});
					
					return;
				}
				
				if (event.DefaultSaved) {
					dc.pui.Popup.alert(task.Store.AnyChanged 
						? 'Saved' : 'No changes, nothing to save.', function() 
					{
						task.resume();
					});
			
					return;
				}
				
				task.resume();				
			}
		} );		

		var savetask = new dc.lang.Task(steps, function(res) {
			//console.log('observer: ' + res.Code);
			callback();
		});
		
		savetask.Store = {
			Form: form,
			AnyChanged: false,
			Current: {
				Result: 0,
				Message: null,
				Record: null,
				Data: { },
				AnyChanged: false,		// for current record
				AfterData: null,
				AfterFlag: false,
			}
		};
		
		savetask.run();		
	},
	
	initChanges: function(recname) { 
		var form = this;
		
		Object.getOwnPropertyNames(form.Inputs).forEach(function(name) {
			var iinfo = form.Inputs[name];
			
			if (iinfo.Record == recname) {
				iinfo.setValue(null);
				iinfo.OriginalValue = iinfo.DefaultValue;
			}
		});
	},
	
	isDefaultChanged: function() { 
		return this.isChanged('Default');
	},
	
	isChanged: function(recname) { 
		var form = this;

		if (form.AsNew[recname] || form.AlwaysNew)
			return true;

		var changed = false;
		
		Object.getOwnPropertyNames(form.Inputs).forEach(function(name) {
			var iinfo = form.Inputs[name];
			
			if ((iinfo.Record == recname) && iinfo.isChanged())
				changed = true;
		});
		
		if (changed)
			return true;
		
		var event = {
			Changed: false
		};
	
		form.raiseEvent('IsRecordChanged', event);
			
		return event.Changed;
	},
	
	getDefaultChanges: function() { 
		return this.getChanges('Default');
	},
	
	getChanges: function(recname) { 
		var form = this;
		var changes = { };
		
		var asNew = (form.AsNew[recname] || form.AlwaysNew);
		
		Object.getOwnPropertyNames(form.Inputs).forEach(function(name) {
			var iinfo = form.Inputs[name];
			
			if (iinfo.Record != recname)
				return;
			
			if (asNew || iinfo.isChanged()) 
				changes[name] = iinfo.getValue();
		});
		
		return changes;
	},
	
	clearDefaultChanges: function() { 
		return this.clearChanges('Default');
	},
	
	clearChanges: function(recname) { 
		var form = this;
		
		form.AsNew[recname] = false;							
		form.FreezeInfo = null;
		
		Object.getOwnPropertyNames(form.Inputs).forEach(function(name) {
			var iinfo = form.Inputs[name];
			
			if (iinfo.Record == recname) 
				iinfo.OriginalValue = iinfo.getValue();
		});
	},
	
	raiseEvent: function(name, event) {
		if (event && event.Record && (event.Record != 'Default'))
			this.PageEntry.callPageFunc(this.Prefix ? this.Prefix + '-' + name + '-' + event.Record : name + '-' + event.Record, event);
		else
			this.PageEntry.callPageFunc(this.Prefix ? this.Prefix + '-' + name : name, event);
	},
	
	/*
	// return results on all controls
	
	{
		Form: [form obj],
		Pass: t/f,
		Inputs: [ {
			Input: [input record],
			Code: 0 or err num,
			Message: [message]
		} ]
	}		
	*/
	
	validate: function() {
		var form = this;

		var res = { 
			Form: form,
			Pass: true,
			Inputs: [ ]
		};
		
		for (var n = 0; n < form.InputOrder.length; n++) {
			var mr = form.input(form.InputOrder[n]).validateInput();
			
			res.Inputs.push(mr);
			
			if (mr.Code != 0) 
				res.Pass = false;
		}
		
		return res;
	},

	validateControl: function(iinfo) {
		var form = this;
		
		// validate entire field, even if multiple inputs are in it 
		var fld = $('#' + iinfo.Id).closest('div.dc-pui-field');
		
		return form.validateField(fld);
	},

	validateField: function(fld) {
		var form = this;
		
		if (dc.util.String.isString(fld))
			fld = $('#' + form.Id).find('div.dc-pui-field[data-dcf-name=' + fld + ']');
		
		if (!fld)
			return;

		var res = { 
			Form: form,
			Pass: true,
			Inputs: [ ]
		};
		
		// find all inputs in field and validate them
		$(fld).find('*[data-dcf-name]').each(function() { 
			var mr = form.input($(this).attr('data-dcf-name')).validateInput();
			
			res.Inputs.push(mr);
			
			if (mr.Code != 0) 
				res.Pass = false;
		});
		
		// update the messages
		form.updateMessages(res);
		
		return res;
	},
	
	updateMessages: function(formresults) {
		var form = this;
	
		for (var i = 0; i < formresults.Inputs.length; i++) {
			var mr = formresults.Inputs[i];
			var fld = $('#' + mr.Input.Id).closest('div.dc-pui-field');
			
			if (fld)
				$(fld).removeClass('dc-pui-invalid');
		}
	
		// start at end so that multi-text gets the first message if there are multiple
		for (var i = formresults.Inputs.length - 1; i >= 0; i--) {
			var mr = formresults.Inputs[i];
			var fld = $('#' + mr.Input.Id).closest('div.dc-pui-field');
			
			if (fld && mr.Code) {
				$(fld).addClass('dc-pui-invalid');
				
				// if not using a fixed message then show specific message
				$(fld).find('div.dc-pui-message-danger:not(.dc-pui-fixed-message)').text(mr.Message);
			}
		}
	}
};

// ------------------- end Form -------------------------------------------------------

dc.pui.TagCache = { };   // place to cache data that spans instances of a tag

dc.pui.Tags = {
	'dc.Button': function(entry, node) {
		dc.pui.Tags['dc.Link'](entry, node);
	},
	'dc.Link': function(entry, node) {
		var click = $(node).attr('data-dc-click');
		var page = $(node).attr('data-dc-page');
		var link = $(node).attr('href');
		
		if (link && (dc.util.String.startsWith(link, 'http:') || dc.util.String.startsWith(link, 'https:') || dc.util.String.startsWith(link, 'mailto:') || dc.util.String.endsWith(link, '.pdf')))
			$(node).attr('target', '_blank');
		
		if (click || page) {
			$(node).click(function(e) {
				if (! $(node).hasClass('pure-button-disabled') && !dc.pui.Apps.busyCheck()) {
					if (click)
						entry.callPageFunc(click, e, this);
					else if (page)
						entry.Layer.loadPage(page);
				}
				
				e.preventDefault();
				return false;
			});
		}
		else if (link && (link.length > 1) && (link.charAt(0) == '#')) {
			$(node).click(link, function(e) {
				entry.scrollPage(e.data);
				e.preventDefault();
				return false;
			});
		}
		else if (link && (link.charAt(0) == '/')) {
			if ((link.length == 1) || (link.charAt(1) != '/')) {
				var dpos = link.lastIndexOf('.');
				var ext = (dpos == -1) ? '' : link.substr(dpos);
				var hasext = false;
				
				if (ext && ext.indexOf('/') == -1)
					hasext = true;

				if (! hasext || (ext == '.html')) {
					$(node).click(link, function(e) {
						entry.Layer.loadPage(e.data);
						
						e.preventDefault();
						return false;
					});
				}
				else {
					$(node).attr('target', '_blank');
				}
			}
		}
	},
	'dcf.MD': function(entry, node) {
		/* TODO review	*/
	},
	'dcf.ManagedForm': function(entry, node) {
		dc.pui.Tags['dcf.Form'](entry, node);
	},
	'dcf.Form': function(entry, node) {
		var fname = $(node).attr('data-dcf-name');
		
		$(node).attr('novalidate', 'novalidate'); 	// for HTML5
		
		// don't add form info on a thaw, reuse but reset the inputs
		if (entry.Forms[fname]) 
			entry.Forms[fname].Inputs = { };
		else 
			entry.Forms[fname] = new dc.pui.Form(entry, node);
		
		$(node).submit(function(e) {
			if (dc.pui.Apps.busyCheck()) 		// proect against user submit such as Enter in a TextField
				return false;
			
			dc.pui.Apps.Busy = true;
			
			var form = entry.Forms[fname];
			
			//	validator.settings.submitHandler.call( validator, validator.currentForm, e );
			var vres = form.validate();
			
			form.updateMessages(vres);
			
			var vpass = true;
			
			for (var i = 0; i < vres.Inputs.length; i++) {
				var inp = vres.Inputs[i];
				
				if (inp.Code != 0) {
					dc.pui.Popup.alert(inp.Message, function() {
						if (form.OnFocus)
							form.PageEntry.callPageFunc(form.OnFocus, inp.Input);
						else
							form.inputQuery(inp.Input.Field).focus();
					});
					
					vpass = false;
					break;
				}
			}
			
			if (vpass) {
				form.save(function() {
					dc.pui.Apps.Busy = false;
				});
			}
			else {
				dc.pui.Apps.Busy = false;
			}

			e.preventDefault();
			return false;
		});		
	},
	'dcf.Text': function(entry, node) {
		// input self registers so don't do anything with it
		var input = new dc.pui.controls.TextInput(entry, node);
	},
	'dcf.TextArea': function(entry, node) {
		// input self registers so don't do anything with it
		var input = new dc.pui.controls.TextInput(entry, node);
	},
	'dcf.Hidden': function(entry, node) {
		// input self registers so don't do anything with it
		var input = new dc.pui.controls.TextInput(entry, node);
	},
	'dcf.Label': function(entry, node) {
		// input self registers so don't do anything with it
		var input = new dc.pui.controls.LabelInput(entry, node);
	},
	'dcf.Select': function(entry, node) {
		// input self registers so don't do anything with it
		var input = new dc.pui.controls.Select(entry, node);
	},
	'dcf.Checkbox': function(entry, node) {
		// input self registers so don't do anything with it
		var input = new dc.pui.controls.Checkbox(entry, node);
	},
	'dcf.CheckGroup': function(entry, node) {
		// input self registers so don't do anything with it
		var input = new dc.pui.controls.CheckGroup(entry, node);
	},
	'dcf.HorizCheckGroup': function(entry, node) {
		// input self registers so don't do anything with it
		var input = new dc.pui.controls.CheckGroup(entry, node);
	},
	'dcf.RadioGroup': function(entry, node) {
		// input self registers so don't do anything with it
		var input = new dc.pui.controls.RadioGroup(entry, node);
	},
	'dcf.HorizRadioGroup': function(entry, node) {
		// input self registers so don't do anything with it
		var input = new dc.pui.controls.RadioGroup(entry, node);
	},
	'dcf.YesNo': function(entry, node) {
		// input self registers so don't do anything with it
		var input = new dc.pui.controls.YesNo(entry, node);
	},
	'dcf.Uploader': function(entry, node) {
		// input self registers so don't do anything with it
		var input = new dc.pui.controls.Uploader(entry, node);
	},
	'dcf.ValidateButton': function(entry, node) {
		$(node).on("click", function(e) { 
			var fnode = $(node).closest('form');
			
			if (!fnode)
				return;
			
			var fname = $(fnode).attr('data-dcf-name');
			
			if (!fname)
				return;
			
			var form = entry.form(fname);
			
			if (!form) 
				return null;
			
			var fld = $(node).closest('div.dc-pui-field');
			
			if (!fld) 
				return null;
			
			var vres = form.validateField(fld);
			
			if (vres.Pass)
				dc.pui.Popup.alert($(fld).find('div.dc-pui-message-info').text());		// TODO if no message then say "all good"
			else
				dc.pui.Popup.alert($(fld).find('div.dc-pui-message-danger').text());
			
			e.preventDefault();
			return false;
		});
	},
	'dcf.SubmitButton': function(entry, node) {
		$(node).on("click", function(e) { 
			if (! $(node).hasClass('pure-button-disabled') && !dc.pui.Apps.busyCheck()) {
				var fnode = $(node).closest('form');
				
				if (fnode)
					$(fnode).submit();
			}
			
			e.preventDefault();
			return false;
		});
	},
	'dc.GallerySection': function(entry, node) {
		$(node).find('.dc-section-gallery-list a').on("click", function(e) { 
			//console.log('img: ' + $(this).attr('data-image') + " show: "
			//	+ $(node).attr('data-show') + " path: " + $(node).attr('data-path'));
			
			var show = {
				Path: $(node).attr('data-path'),
				Show: $(node).attr('data-show'),
				StartPos: $(this).index(),
				Images: []
			};
			
			$(node).find('.dc-section-gallery-list a').each(function() { 
				show.Images.push($(this).attr('data-image'));
			});
			
			console.log('- ' + JSON.stringify(show, null, '\t'));
			
			/*
			if (! $(node).hasClass('pure-button-disabled') && !dc.pui.Apps.busyCheck()) {
				var fnode = $(node).closest('form');
				
				if (fnode)
					$(fnode).submit();
			}
			*/
			
			e.preventDefault();
			return false;
		});
	}
};

// ------------------- end Tags -------------------------------------------------------

dc.pui.controls = { };

dc.pui.controls.Input = function(entry, node) {
	this.init(entry, node);
}

dc.pui.controls.Input.prototype = {
	init: function(entry, node) {
		$.extend(this, {
			Id: null,
			Field: null,
			Record: 'Default',
			DataType: 'String',
			Form: null,
			Label: null,
			DefaultValue: null,
			OriginalValue: null,
			Required: false,
			Pattern: null,
			InvalidMessage: null
		});
		
		var fnode = $(node).closest('form');
		
		if (!fnode)
			return;
		
		var frmname = $(fnode).attr('data-dcf-name');
		
		if (!frmname)
			return;
		
		var form = entry.form(frmname);
		
		if (!form) 
			return null;
		
		this.Form = form;
		
		var id = $(node).attr('id');
		
		if (!id) 
			return;
		
		this.Id = id;

		var fname = $(node).attr('data-dcf-name');
		
		if (!fname)
			return;
		
		this.Field = fname;
		
		form.Inputs[fname] = this;
		form.InputOrder.push(fname);
		
		var rec = $(node).attr('data-dcf-record');
		
		if (! rec) 
			rec = 'Default';
			
		this.Record = rec;

		var dtype = $(node).attr('data-dcf-data-type');
		
		if (dtype)
			this.DataType = dtype;
		
		var lbl = $(node).attr('data-dcf-label');
		
		if (!lbl) {
			var fld = $(node).closest('div.dc-pui-field');
			
			if (fld)
				lbl = $(fld).attr('data-dcf-label');
		}

		if (!lbl) 
			lbl = fname;

		this.Label = lbl;
		this.Required = ($(node).attr('data-dcf-required') == 'true');
		this.Pattern = $(node).attr('data-dcf-pattern');

		if (this.Pattern)
			this.PatternExp = new RegExp('^(?:' + this.Pattern + ')$');
		
		this.Schema = dc.schema.Manager.resolveType(this.DataType);

		if (!dtype && form.RecordMap[rec] && form.RecordMap[rec].Fields[fname]) {
			this.Schema = form.RecordMap[rec].Fields[fname];
			this.Data = fname;		// TODO - can we get the name from Schema?  this makes sure subclasses know a type has been assigned, not just default
		}
		
		if (!this.Required && this.Schema && (this.Schema.Required == 1))
			this.Required = true;
		
		if (this.Required)
			$(node).closest('div.dc-pui-field').addClass('dc-pui-required');
		
		$(node).on('focusin', this, function(e) { 
			entry.Store.__LastFocus = e.data; 
		});
	},
	validate: function() {
		this.Form.validateControl(this); 
	},
	flag: function(msg) {
		this.InvalidMessage = msg;
		this.validate(); 
	},
	unflag: function() {
		this.InvalidMessage = null;
		this.validate(); 
	},
	setValue: function(value) {
		value = dc.util.String.toString(value);

		if (!value)
			value = '';
		
		$('#' + this.Id).val(value);
	},	
	getValue: function() {
		var val = $('#' + this.Id).val();
		
		if (dc.util.Struct.isEmpty(val))
			val = null;
			
		return val;
	},
	validateInput: function() {
		var mr = new dc.lang.OperationResult();
		
		mr.Input = this;
		
		var value = this.getValue();
		var vempty = dc.util.Struct.isEmpty(value);

		if (this.InvalidMessage) {
			mr.error(1, this.InvalidMessage);
		}
		else if (this.Required && vempty) {
			mr.errorTr(424, [ value, this.Label ]);
		}
		else if (!vempty) {
			if (this.PatternExp && !this.PatternExp.test(value)) 
				mr.errorTr(447, [ value, this.Label ]);
			else if (this.Schema && (this.Schema instanceof dc.schema.FieldOption)) 
				this.Schema.validate(true, value, mr);		// present
			else if (this.Schema) 
				this.Schema.validate(value, mr);
			else  
				mr.errorTr(436);		
		}
		
		return mr;
	},
	isChanged: function() {
		return (this.OriginalValue != this.getValue());
	}
};


dc.pui.controls.TextInput = function(entry, node) {
	this.init(entry, node);
};  

dc.pui.controls.TextInput.prototype = new dc.pui.controls.Input();

dc.pui.controls.TextInput.prototype.init = function(entry, node) {
	dc.pui.controls.Input.prototype.init.call(this, entry, node);
	
	var input = this;
	
	$(node).on("keyup", function(e) { 
		if ((e.keyCode || e.which) != '9') 		// don't validate on tab key 
			input.validate(); 
	});
};


dc.pui.controls.LabelInput = function(entry, node) {
	this.init(entry, node);
};  

dc.pui.controls.LabelInput.prototype = new dc.pui.controls.Input();

dc.pui.controls.LabelInput.prototype.setValue = function(value) {
	value = dc.util.String.toString(value);

	if (!value)
		value = '';
	
	$('#' + this.Id).text(value);
};

dc.pui.controls.LabelInput.prototype.getValue = function() {
	var val = $('#' + this.Id).text();
	
	if (dc.util.Struct.isEmpty(val))
		val = null;
		
	return val;
};


dc.pui.controls.Select = function(entry, node) {
	this.init(entry, node);
};  

dc.pui.controls.Select.prototype = new dc.pui.controls.Input();

dc.pui.controls.Select.prototype.init = function(entry, node) {
	dc.pui.controls.Input.prototype.init.call(this, entry, node);
	
	$(node).on("click focusout keyup", this, function(e) { e.data.validate(); });
};

dc.pui.controls.Select.prototype.setValue = function(value) {
	value = dc.util.String.toString(value);

	if (!value)
		value = 'NULL';
	
	$('#' + this.Id).val(value);
};

dc.pui.controls.Select.prototype.getValue = function() {
	var val = $('#' + this.Id).val();
	
	if ((val == 'NULL') || dc.util.Struct.isEmpty(val))
		val = null;
		
	return val;
};

dc.pui.controls.Select.prototype.add = function(values) {
	for (var i = 0; i < values.length; i++) {
		var opt = values[i];			
		var val = dc.util.Struct.isEmpty(opt.Value) ? 'NULL' : opt.Value;
		var oval = (val == 'NULL') ? null : val;
		
		var $on = $('<option value="' + val + '">' + opt.Label + '</option>');
		
		if (this.OriginalValue == oval)
			$on.attr('selected', 'selected');
			
		$('#' + this.Id).append($on);
	}
};


dc.pui.controls.Checkbox = function(entry, node) {
	this.init(entry, node);
};  

dc.pui.controls.Checkbox.prototype = new dc.pui.controls.Input();

dc.pui.controls.Checkbox.prototype.init = function(entry, node) {
	dc.pui.controls.Input.prototype.init.call(this, entry, node);
	
	this.DefaultValue = false;

	// override the default type
	if (this.DataType == 'String') {
		this.DataType = 'Boolean';
		this.Schema = dc.schema.Manager.resolveType(this.DataType);
	}
	
	$(node).find('input').on("click focusout keyup", this, function(e) { e.data.validate(); });
};

dc.pui.controls.Checkbox.prototype.setValue = function(value) {
	value = dc.util.Boolean.toBoolean(value);
	
	$('#' + this.Id).find('input').prop('checked',value)
};

dc.pui.controls.Checkbox.prototype.getValue = function() {
	return $('#' + this.Id).find('input').prop('checked');
};


dc.pui.controls.YesNo = function(entry, node) {
	this.init(entry, node);
};  

dc.pui.controls.YesNo.prototype = new dc.pui.controls.Checkbox();

dc.pui.controls.YesNo.prototype.setValue = function(value) {
	value = dc.util.Boolean.toBoolean(value) ? 'true' : 'false';
	
	$('#' + this.Id + '-' + dc.util.Hex.toHex(value)).prop('checked',true);
};

dc.pui.controls.YesNo.prototype.getValue = function() {
	return ($('#' + this.Id).find('input:checked').val() === 'true');
};


dc.pui.controls.RadioGroup = function(entry, node) {
	this.init(entry, node);
};  

dc.pui.controls.RadioGroup.prototype = new dc.pui.controls.Input();

dc.pui.controls.RadioGroup.prototype.init = function(entry, node) {
	dc.pui.controls.Input.prototype.init.call(this, entry, node);
	
	$(node).find('input').on("click focusout keyup", this, function(e) { e.data.validate(); });
};

dc.pui.controls.RadioGroup.prototype.setValue = function(value) {
	value = dc.util.String.toString(value);
	
	if (!value) 
		value = 'NULL';
	
	$('#' + this.Id + '-' + dc.util.Hex.toHex(value)).prop('checked',true);
};

dc.pui.controls.RadioGroup.prototype.getValue = function() {
	var val = $('#' + this.Id).find('input:checked').val();
	
	if ((val == 'NULL') || dc.util.Struct.isEmpty(val))
		val = null;
		
	return val;
};

dc.pui.controls.RadioGroup.prototype.removeAll = function() {
	$('#' + this.Id).empty();
};

dc.pui.controls.RadioGroup.prototype.add = function(values) {
	for (var i = 0; i < values.length; i++) {
		var opt = values[i];			
		var val = dc.util.Struct.isEmpty(opt.Value) ? 'NULL' : opt.Value;
		var tval = (val == 'NULL') ? null : val;
		var id = this.Id + '-' + dc.util.Hex.toHex(val);
		
		var $ctrl = $('<div class="dc-pui-radio"></div>');
		
		var $cbox = $('<input type="radio" id="' + id + '" value="' + value + '" name="' + this.Field + '" />');
		
		if (this.OriginalValue == tval) 
			$cbox.prop('checked', true);
		
		$cbox.on("click focusout keyup", this, function(e) { e.data.validate(); });
		
		$ctrl.append($cbox);
		
		if (opt.Label) {
			var $clbl = $('<label for="' + id + 
					'" class="dc-pui-input-button"><i aria-hidden="true" class="fa fa-circle"></i> <i aria-hidden="true" class="fa fa-check"></i> ' + 
					opt.Label + '</label>');
	
			$ctrl.append($clbl);
		}
		
		$('#' + this.Id).append($ctrl);
	}
}


dc.pui.controls.ListInput = function(entry, node) {
	this.init(entry, node);
};  

dc.pui.controls.ListInput.prototype = new dc.pui.controls.Input();

dc.pui.controls.ListInput.prototype.init = function(entry, node) {
	dc.pui.controls.Input.prototype.init.call(this, entry, node);
	
	this.DefaultValue = [ ];

	// override the default type
	if (this.DataType == 'String') {
		this.DataType = 'AnyList';
		this.Schema = dc.schema.Manager.resolveType(this.DataType);
	}
};

dc.pui.controls.ListInput.prototype.isChanged = function() {
	var values = this.getValue();

	if (values.length != this.OriginalValue.length)
		return true;
	
	for (var i = 0; i < values.length; i++) {
		var val = values[i];
		var contains = false;
		
		for (var i2 = 0; i2 < this.OriginalValue.length; i2++) {
			var val2 = this.OriginalValue[i2];
			
			if (val2 == val) {
				contains = true;
				break;
			}
		}
		
		if (!contains)
			return true;
	}
	
	return false;
}


dc.pui.controls.CheckGroup = function(entry, node) {
	this.init(entry, node);
};  

dc.pui.controls.CheckGroup.prototype = new dc.pui.controls.ListInput();

dc.pui.controls.CheckGroup.prototype.init = function(entry, node) {
	dc.pui.controls.ListInput.prototype.init.call(this, entry, node);
	
	$(node).find('input').on("click focusout keyup", this, function(e) { e.data.validate(); });
};

dc.pui.controls.CheckGroup.prototype.setValue = function(values) {
	if (!dc.util.Struct.isList(values))
		values = [];

	for (var i = 0; i < values.length; i++)
		$('#' + this.Id + '-' + dc.util.Hex.toHex(values[i])).prop('checked',true);
};

dc.pui.controls.CheckGroup.prototype.getValue = function() {
	return $('#' + this.Id).find('input[name=' + this.Field + ']:checked').map(function() { return this.value; }).get();
};

dc.pui.controls.CheckGroup.prototype.removeAll = function() {
	$('#' + this.Id).empty();
};

dc.pui.controls.CheckGroup.prototype.add = function(values) {
	for (var i = 0; i < values.length; i++) {
		var opt = values[i];			
		var id = this.Id + '-' + dc.util.Hex.toHex(opt.Value);
		
		var $ctrl = $('<div class="dc-pui-checkbox"></div>');
		
		var $cbox = $('<input type="checkbox" id="' + id + '" value="' + opt.Value + '" name="' + this.Field + '" />');
		
		if (this.OriginalValue) {
			for (var o = 0; o < this.OriginalValue.length; o++) {
				if (this.OriginalValue[o] == opt.Value) {
					$cbox.prop('checked', true);
					break;
				}
			}
		}
		
		$cbox.on("click focusout keyup", this, function(e) { e.data.validate(); });
		
		$ctrl.append($cbox);
		
		if (opt.Label) {
			var $clbl = $('<label for="' + id + 
					'" class="dc-pui-input-button"><i aria-hidden="true" class="fa fa-square"></i> <i aria-hidden="true" class="fa fa-check"></i> ' + 
					opt.Label + '</label>');
	
			$ctrl.append($clbl);
		}
		
		$('#' + this.Id).append($ctrl);
	}
}


dc.pui.controls.Uploader = function(entry, node) {
	this.init(entry, node);
	
	this.Values = [ ];
	this.Files = [ ];
};  

dc.pui.controls.Uploader.prototype = new dc.pui.controls.ListInput();

dc.pui.controls.Uploader.prototype.init = function(entry, node) {
	var ctrl = this;
	
	dc.pui.controls.ListInput.prototype.init.call(this, entry, node);
	
	$(node).find('input').on("click focusout keyup", this, function(e) { e.data.validate(); });
	
	$(node).find('input').on("change", this, function(e) {
		ctrl.addFiles(this.files); 
		
		e.data.validate();
	});
	
	// TODO drag and drop
};

dc.pui.controls.Uploader.prototype.setValue = function(values) {
	// do nothing - only file chooser can do this
};

dc.pui.controls.Uploader.prototype.getValue = function() {
	return this.Values;
};

dc.pui.controls.Uploader.prototype.removeAll = function() {
	this.Values = [ ];
	this.Files = [ ];
	
	$('#' + this.Id + ' .dc-pui-uploader-listing').empty();
};

dc.pui.controls.Uploader.prototype.onAfterSave = function(e) {
	var task = e.Task;
	
	if (! e.Task.Store.Form.Managed) {
		task.resume();
		return;
	}
	
	console.log('onAfterSave: ' + this.Field)
	
	// TODO progress bars

	var files = [ ];
	
	for (var i = 0; i < this.Files.length; i++) {
		files.push({
			File: this.Files[i],
			Name: this.Values[i]
		});
	}
	
	var uploadtask = dc.transfer.Util.uploadFiles(files, 'ManagedForm', e.Data.Token, function() {
		task.resume();
	});
	
	uploadtask.Parent = task;

	uploadtask.run();		
};

dc.pui.controls.Uploader.prototype.addFiles = function(values) {
	for (var i = 0; i < values.length; i++) {
		var file = values[i];			
		
		var fname = dc.util.File.toCleanFilename(file.name);
		
		this.Files.push(file);
		this.Values.push(fname);
		
		var $ctrl = $('<div class="dc-pui-uploader-entry">' + fname + '</div>');
		
		$('#' + this.Id + ' .dc-pui-uploader-listing').append($ctrl);
	}
}


// ------------------- end Controls -------------------------------------------------------

/*
$.fn.dcVal = function() {
    var v = this.attr('Value');
    
    if (!v)
    	v = this.attr('value');
    
    if (!v)
    	v = this.text();
    
    return v;
};

$.fn.dcMDUnsafe = function(txt) {
	marked.setOptions({
	  renderer: new marked.Renderer(),
	  gfm: true,
	  tables: true,
	  breaks: true,
	  pedantic: false,
	  sanitize: false,
	  smartLists: true,
	  smartypants: false
	});

	this.html(marked(txt));
}

$.fn.dcMDSafe = function(txt) {
	marked.setOptions({
	  renderer: new marked.Renderer(),
	  gfm: true,
	  tables: true,
	  breaks: true,
	  pedantic: false,
	  sanitize: true,
	  smartLists: true,
	  smartypants: false
	});

	this.html(marked(txt));
}
*/