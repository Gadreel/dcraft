/* ************************************************************************
#
#  designCraft.io
#
#  https://designcraft.io/
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

dc.pui = {
	Loader: {
		__content: null,		// selector
		__loadPageId: null,
		__current: null,		
		__devmode: false,
		__firstload: true,
		__ids: { },
		__pages: { },
		__stalePages: { },
		__libs: { },
		__styles: { },
		__cache: { },   // only valid during page show, wiped each page transition
		__homePage: '/Home',
		__portalPage: '/Portal',
		__signInPage: '/SignIn',
		__destPage: null,
		__originPage: null,
		__originHash: null,
		__originSearch: null,
		__frameRequest: false,
		__busy: false,				// dcui is busy and not accepting new clicks right now - especially for submits 
		__layers: [],
		
		init: function() {
			dc.pui.Loader.__content = 'body'; 
			
			dc.pui.Loader.__destPage = location.pathname;
			dc.pui.Loader.__originPage = location.pathname;
			dc.pui.Loader.__originHash = location.hash;
			dc.pui.Loader.__originSearch = location.search;
			
			$(window).on('popstate', function(e) {
				//console.log("pop - location: " + document.location + ", state: " + JSON.stringify(e.originalEvent.state));
				
				var state = e.originalEvent.state;
				
				var id = state ? state.Id : null; 
				
				if (id) {
					dc.pui.Loader.__loadPageId = id;
					
					if (dc.pui.Loader.__ids[id])
						dc.pui.Loader.manifestPage(dc.pui.Loader.__ids[id]);
					else
						dc.pui.Loader.loadPage(document.location.pathname, state.Params);
				}
		    });
			
			// watch for orientation change or resize events
			$(window).on('orientationchange resize', function (e) {
				var entry = dc.pui.Loader.__current;
				
				if (entry)
					entry.onResize(e);
					
				for (var i = 0; i < dc.pui.Loader.__layers.length; i++) 
					dc.pui.Loader.__layers[i].onResize(e);
					
				dc.pui.Loader.requestFrame();
			});
			
			if (document.fonts) {
				document.fonts.onloadingdone = function(e) {
					// force all canvas updates that may be using loaded forms
					dc.pui.Loader.requestFrame();
				}
			}
		},
		setHomePage: function(v) {
			dc.pui.Loader.__homePage = v;
		},
		loadHomePage: function() {
			var hpath = dc.pui.Loader.__homePage;
			
			if (!hpath)
				hpath = $('html').attr('data-dcw-Home');
			
			if (hpath)
				dc.pui.Loader.loadPage(hpath);
		},
		setPortalPage: function(v) {
			dc.pui.Loader.__portalPage = v;
		},
		loadPortalPage: function() {
			var hpath = dc.pui.Loader.__portalPage;
			
			if (!hpath)
				hpath = $('html').attr('data-dcw-Portal');
			
			if (hpath)
				dc.pui.Loader.loadPage(hpath);
		},
		setSigninPage: function(v) {
			dc.pui.Loader.__signInPage = v;
		},
		loadSigninPage: function(p) {
			var hpath = dc.pui.Loader.__signInPage;
			
			if (!hpath)
				hpath = $('html').attr('data-dcw-SignIn');
			
			if (hpath)
				dc.pui.Loader.loadPage(hpath, p);
		},
		setDestPage: function(v) {
			dc.pui.Loader.__destPage = v;
		},
		loadDestPage: function() {
			if (dc.pui.Loader.__destPage)
				dc.pui.Loader.loadPage(dc.pui.Loader.__destPage, null, true);
		},
		signout: function() {
			dc.user.signout();
			dc.user.saveRememberedUser();
	
			window.location = '/';
		},
		sessionChanged: function() {
			dc.user.updateUser(false, function() {
				// TODO maybe change page if not auth tags? or if became guest
			}, true);
			
			if (dc.handler.sessionChanged)
				dc.handler.sessionChanged();
		},
		busyCheck: function() {
			if (dc.pui.Loader.__busy) {		// protect again user submit such as Enter in a TextField
				console.log('click denied, dcui is busy');			// TODO if we have been busy for more than 2 seconds show a message screen...obviously someone is trying to click while nothing appears to be happening, hide the screen after load is done - unless someone else updated it
				return true;
			}
			
			return false;
		},
		loadPage: function(page, params, replaceState, callback) {
			if (!page)
				return;
			
			// really old browsers simply navigate again, rather than do the advanced page view
			if (!history.pushState) {
				if (window.location.pathname != page)
					window.location = page;
				
				return;
			}
			
			var oldentry = dc.pui.Loader.__current;
			
			if (oldentry && oldentry.Loaded)
				oldentry.freeze();

			var rp = dc.handler.reroute ? dc.handler.reroute(page, params) : null;
			
			if (rp != null)
				page = rp;
			
			var hpage = page.split('#');
			page = hpage[0];
			var targetel = hpage[1];
			
			var opts = {
				Name: page
			};
			
			if (params)
				opts.Params = params;
			
			if (replaceState)
				opts.ReplaceState = replaceState;
			
			if (targetel)
				opts.TargetElement = targetel;
			
			if (callback)
				opts.Callback = callback;
			
			var entry = new dc.pui.PageEntry(opts);
			
			dc.pui.Loader.__loadPageId = entry.Id;
			
			// if page is already loaded then show it
			if (!dc.pui.Loader.__devmode && dc.pui.Loader.__pages[page] && !dc.pui.Loader.__stalePages[page]) {
				dc.pui.Loader.resumePageLoad();
				return;
			}
			
			delete dc.pui.Loader.__stalePages[page];		// no longer stale
			
			//console.log('checking staleness b: ' + dc.pui.Loader.__stalePages[page])
			
			var script = document.createElement('script');
			script.src = page + '?_dcui=dyn&nocache=' + dc.util.Crypto.makeSimpleKey();
			script.id = 'page' + page.replace(/\//g,'.');
			script.async = false;  	
			
			document.head.appendChild(script);
		},
		resumePageLoad: function() {
			var entry = dc.pui.Loader.__ids[dc.pui.Loader.__loadPageId];
			
			if (!entry)
				return;
			
			var page = dc.pui.Loader.__pages[entry.Name];
			var needWait = false;
			
			if (page.RequireStyles && (page.RequireStyles.length > 0)) {
				for (var i = 0; i < page.RequireStyles.length; i++) {
					var path = page.RequireStyles[i];
					
					if (dc.pui.Loader.__styles[path])
						continue;
					
					$('head').append('<link rel="stylesheet" type="text/css" href="' + path + '?nocache=' + dc.util.Crypto.makeSimpleKey() + '" />'); 
					
					dc.pui.Loader.__styles[path] = true;		// not really yet, but as good as we can reasonably get
					needWait = true;
				}
			}
			
			var rtype = [];
			
			if (page.RequireType && (page.RequireType.length > 0)) {
				for (var i = 0; i < page.RequireType.length; i++) {
					var tname = page.RequireType[i];
					var tparts = tname.split(',');
					
					for (var i2 = 0; i2 < tparts.length; i2++) {
						var tn = tparts[i2];
						
						if (dc.schema.Manager.resolveType(tn) == null)
							rtype.push(tn);
					}
				}
			}
			
			var rtr = [];
			
			if (page.RequireTr && (page.RequireTr.length > 0)) {
				for (var i = 0; i < page.RequireTr.length; i++) {
					var tname = page.RequireTr[i];
					var tparts = tname.split(',');
					
					for (var i2 = 0; i2 < tparts.length; i2++) {
						var tn = tparts[i2];
						
						// codes get expanded automatically
						if (dc.util.Number.isNumber(tn))
							tn = "_code_" + tn;
						
						if (!dc.lang.Dict.get(tn))
							rtr.push(tn);
					}
				}
			}
			
			if ((rtype.length > 0) || (rtr.length > 0)) {
				var key = dc.util.Crypto.makeSimpleKey();
				
				var script = document.createElement('script');
				script.src = '/dcw/LoadDefinitions?nocache=' + key + '&DataTypes=' + rtype.join(',')  + '&Tokens=' + rtr.join(',');
				script.id = 'def' + key;					
				script.async = false;  	// needed when loading additional libraries, we can inject a final fake script that echos 
										// a param (e.g. ?opid=3345) to us saying that it is loaded and hence all preceding scripts are also loaded
				
				document.head.appendChild(script);
				
				needWait = true;
			}
			
			// TODO combine the load lib calls
			
			if (page.RequireLibs && (page.RequireLibs.length > 0)) {
				for (var i = 0; i < page.RequireLibs.length; i++) {
					var path = page.RequireLibs[i];
					
					if (dc.pui.Loader.__libs[path])
						continue;
					
					var script = document.createElement('script');
					script.src = path + '?nocache=' + dc.util.Crypto.makeSimpleKey();
					script.id = 'req' + path.replace(/\//g,'.');					
					script.async = false;  	// needed when loading additional libraries, we can inject a final fake script that echos 
											// a param (e.g. ?opid=3345) to us saying that it is loaded and hence all preceding scripts are also loaded
					
					document.head.appendChild(script);
					
					dc.pui.Loader.__libs[path] = true;		// not really yet, but as good as we can reasonably get
					needWait = true;
				}
			}
			
			if (needWait) {
				var key = dc.util.Crypto.makeSimpleKey();
				
				var script = document.createElement('script');
				script.src = '/dcw/RequireLib?_dcui=dyn&nocache=' + key;
				script.id = 'lib' + key;					
				script.async = false;  	// needed when loading additional libraries, we can inject a final fake script that echos 
										// a param (e.g. ?opid=3345) to us saying that it is loaded and hence all preceding scripts are also loaded
				
				document.head.appendChild(script);
				
				return;
			}
			
			if (entry.Layer) {
				entry.Layer.manifestPage(entry);
				return;
			}
			
			if (entry.ReplaceState)
				history.replaceState(
					{ Id: dc.pui.Loader.__loadPageId, Params: entry.Params }, 
					page.Title, 
					entry.Name);
			else
				history.pushState(
					{ Id: dc.pui.Loader.__loadPageId, Params: entry.Params }, 
					page.Title, 
					entry.Name);
			
			dc.pui.Loader.manifestPage(entry);
		},
		manifestPage: function(entry) {
			if (!entry)
				return;
			
			// remove all layers
			for (var i = 0; i < dc.pui.Loader.__layers.length; i++) 
				dc.pui.Loader.__layers[i].onDestroy();
			
			dc.pui.Loader.__layers = [ ];
			
			// get rid of current page
			if (dc.pui.Loader.__current) 
				dc.pui.Loader.__current.onDestroy();
			
			// start loading new page
			dc.pui.Loader.__cache = {};
			dc.pui.Loader.__current = entry;
			
			var page = dc.pui.Loader.__pages[entry.Name];
			
			var enhancefunc = function() {
				dc.pui.Loader.enhancePage('body', null);
				
				entry.onLoad(function() {
					if (typeof ga == 'function') {
						ga('set', {
							  page: entry.Name,
							  title: page.Title
						});
	
						ga('send', 'pageview');
					}
					
					if (page.Title)
						document.title = page.Title;
					
					if (entry.Callback)
						entry.Callback.call(entry);
				});
			};
			
			if (dc.pui.Loader.__firstload) {
				dc.pui.Loader.__firstload = false;
				enhancefunc();
				return;
			}
			
			$(dc.pui.Loader.__content).empty().append(page.Layout).promise().then(function() {
				$('body').attr('class', page.PageClass);
				
				if (entry.Loaded && entry.FreezeTop)
					$("html, body").velocity({ scrollTop: entry.FreezeTop }, "fast");
				else if (entry.TargetElement)
					$("html, body").velocity({ scrollTop: $('#' + entry.TargetElement).get(0).getBoundingClientRect().top + window.pageYOffset }, "fast");
				else
					$("html, body").velocity({ scrollTop: 0 }, "fast");
				
				enhancefunc();
			});
		},
		enhancePage: function(root, layer) {
			$(root + ' *[data-dc-enhance="true"]').each(function() { 
				var tag = $(this).attr('data-dc-tag');
				
				if (!tag || !dc.pui.Tags[tag])
					return;
				
				dc.pui.Tags[tag].enhanceNode(layer, this);
			});
		},		
		clearPageCache: function(page) {
			if (page)
				dc.pui.Loader.__stalePages[page] = true; 
		},
		failedPageLoad: function(reason) {
			dc.pui.Loader.__destPage = dc.pui.Loader.__ids[dc.pui.Loader.__loadPageId].Name;
			
			if (reason == 1)
				dc.pui.Loader.loadSigninPage({FromFail: true});
		},
		addPageDefinition: function(name, def) {
			dc.pui.Loader.__pages[name] = def;
		},
		scrollPage: function(id) {
			$("html, body").velocity({ scrollTop: $('#' + id).get(0).getBoundingClientRect().top + window.pageYOffset }, "fast");
		},
		scrollPage2: function(selector) {
			$("html, body").velocity({ scrollTop: $(selector).get(0).getBoundingClientRect().top + window.pageYOffset }, "fast");
		},
		closePage: function(opts) {
			if (opts)
				dc.pui.Loader.loadPage(opts.Path, opts.Params);
			else 
				window.history.back();
		},
		currentPageEntry: function() {
			return dc.pui.Loader.__current;
		},
		callbackExtraLibs: function() {
			if (dc.pui.Loader.__extraLibsCallback)
				dc.pui.Loader.__extraLibsCallback();
		},
		addExtraLibs: function(scripts, cb) {
			var needWait = false;
			
			dc.pui.Loader.__extraLibsCallback = cb;
			
			for (var i = 0; i < scripts.length; i++) {
				var path = scripts[i];
				
				if (dc.pui.Loader.__libs[path])
					continue;
				
				var script = document.createElement('script');
				script.src = path + '?nocache=' + dc.util.Crypto.makeSimpleKey();
				script.id = 'req' + path.replace(/\//g,'.');					
				script.async = false;  	// needed when loading additional libraries, we can inject a final fake script that echos 
										// a param (e.g. ?opid=3345) to us saying that it is loaded and hence all preceding scripts are also loaded
				
				document.head.appendChild(script);
				
				dc.pui.Loader.__libs[path] = true;		// not really yet, but as good as we can reasonably get
				needWait = true;
			}
			
			if (needWait) {
				var key = dc.util.Crypto.makeSimpleKey();
				
				var script = document.createElement('script');
				script.src = '/dcw/js/dc.extra-lib-callback.js?nocache=' + key;
				script.id = 'lib' + key;					
				script.async = false;  	// needed when loading additional libraries, we can inject a final fake script that echos 
										// a param (e.g. ?opid=3345) to us saying that it is loaded and hence all preceding scripts are also loaded
				
				document.head.appendChild(script);
			}
			else {
				if (dc.pui.Loader.__extraLibsCallback)
					dc.pui.Loader.__extraLibsCallback();
			}
		},
		addExtraStyles: function(styles, cb) {
			var needWait = false;
			
			dc.pui.Loader.__extraLibsCallback = cb;
			
			for (var i = 0; i < styles.length; i++) {
				var path = styles[i];
				
				if (dc.pui.Loader.__styles[path])
					continue;
				
				$('head').append('<link rel="stylesheet" type="text/css" href="' + path + '?nocache=' + dc.util.Crypto.makeSimpleKey() + '" />'); 
				
				dc.pui.Loader.__styles[path] = true;		// not really yet, but as good as we can reasonably get
				needWait = true;
			}
			
			if (needWait) {
				var key = dc.util.Crypto.makeSimpleKey();
				
				var script = document.createElement('script');
				script.src = '/dcw/js/dc.extra-lib-callback.js?nocache=' + key;
				script.id = 'lib' + key;					
				script.async = false;  	// needed when loading additional libraries, we can inject a final fake script that echos 
										// a param (e.g. ?opid=3345) to us saying that it is loaded and hence all preceding scripts are also loaded
				
				document.head.appendChild(script);
			}
			else {
				if (dc.pui.Loader.__extraLibsCallback)
					dc.pui.Loader.__extraLibsCallback();
			}
		},
		requestFrame: function() {
			if (!dc.pui.Loader.__frameRequest) {
				window.requestAnimationFrame(dc.pui.Loader.buildFrame);
				dc.pui.Loader.__frameRequest = true;
			}
		},
		buildFrame: function(e) {
			dc.pui.Loader.__frameRequest = false;
			
			var entry = dc.pui.Loader.__current;
			
			if (entry)
				entry.onFrame(e);
					
			for (var i = 0; i < dc.pui.Loader.__layers.length; i++) 
				dc.pui.Loader.__layers[i].onFrame(e);
		},
		registerResize : function(callback) {
			var entry = dc.pui.Loader.__current;
			
			if (entry)
				entry.registerResize(callback);
		},
		registerFrame : function(render) {
			var entry = dc.pui.Loader.__current;
			
			if (entry)
				entry.registerFrame(render);
		},
		registerDestroy : function(callback) {
			var entry = dc.pui.Loader.__current;
			
			if (entry)
				entry.registerDestroy(callback);
		},
		getLayer: function(name) {
			for (var i = 0; i < dc.pui.Loader.__layers.length; i++) {
				var l = dc.pui.Loader.__layers[i];
				
				if (l.Name == name)
					return l;
			}
			
			return null;
		},
		addLayer: function(layer) {
			dc.pui.Loader.__layers.push(layer);
		},
		allocateTimeout : function(options) {
			var entry = dc.pui.Loader.__current;
			
			if (entry)
				entry.allocateTimeout(options);
		},
		allocateInterval : function(options) {
			var entry = dc.pui.Loader.__current;
			
			if (entry)
				entry.allocateInterval(options);
		},
		activateCms: function(tabcb) {
			if (!tabcb && !dc.user.isAuthorized(['Editor','Contributor','Developer']))
				return;
			
			if (!tabcb) {
				tabcb = function() {
					var def = dc.pui.Loader.currentPageEntry().getDefinition();
					
					dc.cms.edit.Loader.setContext({
						Menu: dc.cms.edit.MenuEnum.PageProps,
						Params: {
							Page: { 
								Channel: def.CMSChannel, 
								Path: def.CMSPath
							}
						}
					});
				
					dc.cms.edit.Loader.openPane('/dcm/edit/page/Edit-Feed-Prop');
				};
			}
			
			dc.pui.Loader.addExtraStyles([ '/dcm/edit/css/main.css' ], function() {
				dc.pui.Loader.addExtraLibs([ '/dcm/edit/js/main.js' ], function() {
					dc.cms.edit.Loader.init(tabcb);
				});
			});
		},
		loadCms: function(cb) {
			dc.pui.Loader.addExtraStyles([ '/dcm/edit/css/main.css' ], function() {
				dc.pui.Loader.addExtraLibs([ '/dcm/edit/js/main.js' ], function() {
					cb();
				});
			});
		},
		refreshPage: function() {
			var entry = dc.pui.Loader.__current;
			var top = $(window).scrollTop();
			
			dc.pui.Loader.clearPageCache(window.location.pathname);
			dc.pui.Loader.loadPage(window.location.pathname, entry.Params, true, function() {
				$("html, body").velocity({ scrollTop: top }, "fast");
			});
		}
	},

	Popup: {
		alert: function(msg, callback) {
			$('#dcuiAlertPane').remove();

			$('body').append('<div id="dcuiAlertPane" class="ui-content"> \
					<a id="dcuiAlertPaneClose" href="#" class="ui-corner-all ui-icon-delete ui-btn-icon-notext ui-btn-right">Close</a> \
					<div id="dcuiAlertPaneHtml"></div> \
			</div>');
			
			$("#dcuiAlertPaneClose,#dcuiAlertPane").click(function (e) {
				if (dc.pui.Popup.__cb)
					dc.pui.Popup.__cb();

				dc.pui.Popup.__cb = null;
				
				$('#dcuiAlertPane').remove();
				
				e.preventDefault();
				return false;
			});
			
			dc.pui.Popup.__cb = callback;

			$('#dcuiAlertPaneHtml').html(msg);
		},
		help: function(msg, callback) {
			$('#dcuiHelpPane').remove();

			$('body').append('<div id="dcuiHelpPane" class="ui-content"> \
					<a id="dcuiHelpPaneClose" href="#" class="ui-corner-all ui-icon-delete ui-btn-icon-notext ui-btn-right">Close</a> \
					<div id="dcuiHelpPaneHtml"></div> \
			</div>');
			
			$("#dcuiHelpPane,#dcuiHelpPaneClose").click(function (e) {
				if (dc.pui.Popup.__cb)
					dc.pui.Popup.__cb();

				dc.pui.Popup.__cb = null;
				
				$('#dcuiHelpPane').remove();
				
				e.preventDefault();
				return false;
			});
			
			$("#dcuiHelpPaneHtml").click(function (e) {
				if (e.target.tagName == 'A' && e.target.href)
					window.open(e.target.href);
				
				e.preventDefault();
				return false;
			});
			
			dc.pui.Popup.__cb = callback;

			$('#dcuiHelpPaneHtml').html(msg);
		},
		image: function(src, style) {
			var pi = $('#puImage');
			
			// build alert if none present
			if (!pi.length) {
				$('body').append('<div data-role="popup" id="puImage" data-theme="a" class="ui-content" data-overlay-theme="b"> \
						<a href="#" data-rel="back" class="ui-btn ui-corner-all ui-shadow ui-btn-a ui-icon-delete ui-btn-icon-notext ui-btn-right">Close</a> \
						<img id="puImageImg" /> \
				</div>');
				
				// TODO
				$('#puImage').enhanceWithin().popup();
			}

			$('#puImageImg').attr('src', src);
			$('#puImageImg').attr('style', style);
			$('#puImage').popup('open', { positionTo: 'window', transition: 'pop' });
		},
		confirm: function(msg,callback) {
			var pi = $('#puConfirm');
			
			// build alert if none present
			if (!pi.length) {
				$('body').append('<div data-role="popup" id="puConfirm" data-theme="a" class="ui-corner-all"> \
						<a href="#" data-rel="back" class="ui-btn ui-corner-all ui-shadow ui-btn-a ui-icon-delete ui-btn-icon-notext ui-btn-right">Close</a> \
						<form> \
						<div> \
							<div id="puConfirmHtml"></div> \
							<button id="btnConfirmPopup" type="button" class="ui-btn ui-corner-all ui-shadow ui-btn-a">Yes</button> \
							<button id="btnRejectPopup" type="button" class="ui-btn ui-corner-all ui-shadow ui-btn-a">No</button> \
						</div> \
					</form> \
				</div>');
				
				// TODO
				$('#puConfirm').enhanceWithin().popup();
				
				$("#puConfirm").on("popupafterclose", function () {
					if (dc.pui.Popup.__cbApprove && dc.pui.Popup.__cb)
						dc.pui.Popup.__cb();

					dc.pui.Popup.__cb = null;
					
					//console.log('aaaa');
				});
				
				$('#btnConfirmPopup').click(function(e) {
					dc.pui.Popup.__cbApprove = true;
					
					$('#puConfirm').popup('close');
						
					e.preventDefault();
					return false;
				});
				
				$('#btnRejectPopup').click(function(e) {
					$('#puConfirm').popup('close');
						
					e.preventDefault();
					return false;
				});
			}
			
			dc.pui.Popup.__cb = callback;
			dc.pui.Popup.__cbApprove = false;
			$('#puConfirmHtml').html(msg);
			$('#puConfirm').popup('open', { positionTo: 'window', transition: 'pop' });
		},
		loading: function() {
		}
	}
};

// ------------------- end Loader/Popup -------------------------------------------------------

dc.pui.Layer = function(contentsel, options) {
	this.init(contentsel, options);
};

dc.pui.Layer.prototype = {
	init: function(contentsel, options) {
		$.extend(this, { 
			Name: '[unknown]',
			Params: { },
			Store: { }
		}, options);

		this.__content = contentsel;		// selector for content element
		this.__current = null;
		this.__cache = { };
		this.__history = [ ];
		this.__observers = { };
		// TODO add layer Timers
	},
	
	setObserver: function(name, observer) {
		this.__observers[name] = observer;
	},
	
	deleteObserver: function(name, observer) {
		delete this.__observers[name];
	},
	
	setContentSelector: function(v) {
		this.__content = v;
	},
		
	loadPage: function(page, params, replaceState) {
		var hpage = page.split('#');
		
		page = hpage[0];
		
		var opts = {
			Name: page
		};
		
		if (params)
			opts.Params = params;
		
		if (replaceState)
			opts.ReplaceState = replaceState;
		
		if (hpage.length)
			opts.TargetElement = hpage[1];
			
		this.loadPageX(opts);
	},
	
	loadPageX: function(options) {
		if (!options)
			return;
		
		var oldentry = this.__current;
		
		if (oldentry) {
			oldentry.freeze();
			this.__history.push(oldentry);
		}
		
		options.Layer = this;
		
		var entry = new dc.pui.PageEntry(options);
		
		dc.pui.Loader.__loadPageId = entry.Id;
		
		// if page is already loaded then show it
		if (!dc.pui.Loader.__devmode && dc.pui.Loader.__pages[options.Name]) {
			dc.pui.Loader.resumePageLoad();
			return;
		}
		
		var script = document.createElement('script');
		script.src = options.Name + '?_dcui=dyn&nocache=' + dc.util.Crypto.makeSimpleKey();
		script.id = 'page' + options.Name.replace(/\//g,'.');
		script.async = false;  	
		
		document.head.appendChild(script);
	},
	
	manifestPage: function(entry) {
		var layer = this;
		
		if (!entry)
			return;
			
		if (layer.__current) 
			layer.__current.onDestroy();
		
		layer.__current = entry;
		
		this.open();
		
		// TODO rewrite
		
		$(layer.__content).empty().promise().then(function() {
			var page = dc.pui.Loader.__pages[entry.Name];
		
			// layout using 'pageContent' as the top of the chain of parents
			entry.layout(page.Layout, new dc.pui.LayoutEntry({
				Element: $(layer.__content),
				PageEntry: entry
			}));
						
			$(layer.__content).enhanceWithin().promise().then(function() {			
				if (entry.Loaded && entry.FreezeTop)
					$(layer.__content).velocity({ scrollTop: entry.FreezeTop }, "fast");
				else if (entry.TargetElement)
					$(layer.__content).velocity({ scrollTop: $('#' + entry.TargetElement).get(0).getBoundingClientRect().top + window.pageYOffset }, "fast");
				else
					$(layer.__content).velocity({ scrollTop: 0 }, "fast");
				
				layer.enhancePage();
				
				Object.getOwnPropertyNames(layer.__observers).forEach(function(name) {
					layer.__observers[name]();
				});
				
				entry.onLoad();
			});
		});
	},
	
	enhancePage: function() {
		var layer = this;

		dc.pui.Loader.enhancePage(layer.__content, layer);
	},
	
	closePage: function(opts) {
		if (opts)
			this.loadPage(opts.Path, opts.Params);
		else 
			this.back();
	},
	
	getHistory: function() {
		return this.__history;
	},
	
	clearHistory: function() {
		var layer = this;
		
		if (layer.__current) 
			layer.__current.onDestroy();
		
		layer.__current = null;
		
		var entry = this.__history.pop();
		
		while (entry) {
			entry.onDestroy();
			entry = this.__history.pop();
		}
	},
	
	back: function() {
		var entry = this.__history.pop();
		
		if (entry) {
			this.manifestPage(entry);
		}
		else {
			this.__current = null;
			this.close();
		}
	},
	
	open: function() {
		$(this.__content).show();
	},
	
	close: function() {
		$(this.__content).hide();
	},
	
	currentPageEntry: function() {
		return this.__current;
	},
	
	scrollPage: function(selector) {
		$(this.__content).velocity({ scrollTop: $(selector).get(0).getBoundingClientRect().top + window.pageYOffset }, "fast");
	},
	
	onResize: function(e) {
		var entry = this.__current;
		
		if (entry)
			entry.onResize(e);
	},
	
	onDestroy: function(e) {
		var entry = this.__current;
		
		if (entry)
			entry.onDestroy(e);
		
		$(this.__content).remove();
	},
	
	onFrame: function(e) {
		var entry = this.__current;
		
		if (entry)
			entry.onFrame(e);
	}
};

// ------------------- end Layer -------------------------------------------------------

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
	
	dc.pui.Loader.__ids[id] = this;
}

dc.pui.PageEntry.prototype = {
	getDefinition: function() {
		return dc.pui.Loader.__pages[this.Name];
	},
	onLoad: function(callback) {
		var page = dc.pui.Loader.__pages[this.Name];
		var entry = this;
		
		// after forms are loaded, notify caller
		var loadcntdwn3 = new dc.lang.CountDownCallback(1, function() { 
			entry.Loaded = true;
			
			if (callback)
				callback.call(entry);
		});
		
		// loadcntdwn3 can be used by the forms loads to delay load callback
		// if any form loader needs to do an async operation
		var loadcntdwn2 = new dc.lang.CountDownCallback(1, function() { 
			// now load forms, if any
			Object.getOwnPropertyNames(entry.Forms).forEach(function(name) {
				loadcntdwn3.inc();
				
				entry.Forms[name][entry.Loaded ? 'thaw' : 'load'](function() {
					loadcntdwn3.dec();
				});
			});
				
			loadcntdwn3.dec();	
		});
		
		// loadcntdwn2 can be used by the primary Load function to delay loading the forms
		// if Load needs to do an async operation
		var loadcntdwn1 = new dc.lang.CountDownCallback(1, function() { 
			entry.callPageFunc('Load', loadcntdwn2); 
			
			loadcntdwn2.dec();
		});
		
		// run all the LoadFunctions, they cannot depend on each other or on an order of execution
		// loadcntdwn1 can be used by the LoadFunctions to delay primary Load of the form
		// if any LoadFunctions needs to do an async operation
		for (var i = 0; i < page.LoadFunctions.length; i++) {
			if (dc.util.String.isString(page.LoadFunctions[i])) 
				entry.callPageFunc(page.LoadFunctions[i], loadcntdwn1); 
			else
				page.LoadFunctions[i].call(entry, loadcntdwn1);
		}
			
		loadcntdwn1.dec();		
	},
	
	callPageFunc: function(method) {
		var page = dc.pui.Loader.__pages[this.Name];
		
		if (page && page.Functions[method]) 
			return page.Functions[method].apply(this, Array.prototype.slice.call(arguments, 1));
			
		return null;
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
	
	// TODO rewrite
	addFormLayout: function(name, xtralayout) {
		this.layout(xtralayout, new dc.pui.LayoutEntry({
			Element: $('#frm' + name),
			Definition: { Element: 'Form', Name: name },
			PageEntry: this
		}));
		
		$('#frm' + name).enhanceWithin();
	},
	
	freeze: function() {
		var entry = this;
		var page = dc.pui.Loader.__pages[entry.Name];
		
		entry.FreezeTop = $(window).scrollTop();
		
		entry.callPageFunc('Freeze'); 
		
		Object.getOwnPropertyNames(entry.Forms).forEach(function(name) {
			entry.Forms[name].freeze();
		});
	},
	
	onResize: function(e) {
		var page = dc.pui.Loader.__pages[this.Name];
		
		this.callPageFunc('onResize', e); 
		
		for (var i = 0; i < this._onResize.length; i++) 
			this._onResize[i].call(this, e);
	},
	
	registerResize: function(callback) {
		this._onResize.push(callback);
	},
	
	onFrame: function(e) {
		var page = dc.pui.Loader.__pages[this.Name];
	    var now = Date.now();
		
		this.callPageFunc('onFrame', e); 
		
		for (var i = 0; i < this._onFrame.length; i++) {
			var render = this._onFrame[i];
			
		    var delta = now - render.__then;
		     
		    if (delta > render.__interval) {
		        // adjust so lag time is removed, produce even rendering 
		    	render.__then = now - (delta % render.__interval);
		         
				render.Run.call(this, e);
		    }
		    else {
				dc.pui.Loader.requestFrame();	// keep calling until we don't skip
		    }
		}
	},
	
	registerFrame: function(render) {
		render.__then = Date.now();
		render.__interval = 1000 / render.Fps;
		
		this._onFrame.push(render);
	},
			
	onDestroy: function() {
		var page = dc.pui.Loader.__pages[this.Name];

		// clear the old timers
		for (var x = 0; x < this.Timers.length; x++) {
			var tim = this.Timers[x];
			
			if (!tim)
				continue;
			
			if (tim.__tid)
				window.clearTimeout(tim.__tid);
			else if (tim.__iid)
				window.clearInterval(tim.__iid);
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
		
		options.__tid = window.setTimeout(function () {
				window.clearTimeout(options.__tid);		// no longer need to clear this later, it is called
				entry.Timers[pos] = null;
				
				options.Op.call(this, options.Data);
			}, 
			options.Period);
		
		this.Timers.push(options);		
	},
	
	allocateInterval: function(options) {
		var entry = this;
		
		options.__iid = window.setInterval(function () {
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

dc.pui.Form = function(pageEntry, options) {
	$.extend(this, { 
			Name: '[unknown]',
			PageEntry: pageEntry,		// page entry that form belongs to, null for default
			Inputs: { },
			InputOrder: [ ],
			RecordOrder: [ "Default" ],
			RecordMap: { }, 		 // map records to data types 
			AsNew: { },
			AlwaysNew: false,
			OnFocus: null,
			FreezeInfo: null		//  { [recordname]: { Originals: { [fld]: [value] }, Values: { [fld]: [value] } }, [records]... }
		}, options);
	
	this.Id = 'frm' + this.Name;
	
	if (dc.util.String.isString(options.RecordOrder))
		this.RecordOrder = options.RecordOrder.split(',');
	
	if (dc.util.String.isString(options.RecordMap)) {
		this.RecordMap = { };
		
		var maps = options.RecordMap.split(',');
		
		for (var im = 0; im < maps.length; im++) {
			var map = maps[im].split(':');
			
			if (map[0].length == 0)
				map[0] = 'Default';
			
			this.RecordMap[map[0]] = dc.schema.Manager.resolveType(map[1]);
		}
	}
	
	if (dc.util.String.isString(options.AlwaysNew))
		this.AlwaysNew = (options.AlwaysNew == 'true');
}

dc.pui.Form.prototype = {
	input: function(name) {
		return this.Inputs[name];
	},
	
	query: function(name) {
		var inp = this.Inputs[name];
		
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
		var fnode = $('#' + form.Id);
		
		if(!form.RecordOrder) {
			callback();
			return;
		}
		
		this.loadCommon();
		
		// build a queue of record names (copy array) to load 
		var rnames = form.RecordOrder.concat(); 
		
		var qAfter = function(event) {
			// handler will change Data if necessary
			form.raiseEvent('AfterLoadRecord', event);
		
			form.loadRecord(event.Record, event.Data, event.AsNew);
				
			// process next record in queue
			qProcess();
		};
		
		// define how to process the queue
		var qProcess = function() {
			// all done with loading
			if (rnames.length == 0) {
				form.raiseEvent('AfterLoad', event);
				
				callback();					
				return;
			}
				
			var rname = rnames.shift();
			
			var event = { 
				Record: rname
			};

			form.initChanges(event.Record);
			
			// handler will set Message if we need to load data from bus and Data if 
			// we need to load a record they provide
			// also they should set AsNew = true if this is a new record
			form.raiseEvent('LoadRecord', event);

			if (event.Stop) {
				callback();
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
							callback();
							return;
						}
					}
	
					event.Result = e;
					event.Data = e.Body;
					
					qAfter(event);
				}, null, true);
			}
			else {
				event.Result = 0;
				qAfter(event);
			}
		};
		
		// start the queue processing
		qProcess();
	},
	
	loadDefaultRecord: function(data, asNew) {
		return this.loadRecord('Default', data, asNew);
	},
	
	loadRecord: function(recname, data, asNew) {
		var form = this;
		
		if (asNew)
			form.AsNew[recname] = true;

		if (!data) 
			return;
		
		Object.getOwnPropertyNames(form.Inputs).forEach(function(name) {
			var iinfo = form.Inputs[name];
			
			if ((iinfo.Record == recname) && data.hasOwnProperty(iinfo.Field)) {
				iinfo.setValue(data[iinfo.Field]);
				iinfo.OriginalValue = data[iinfo.Field];  //iinfo.getValue();
			}
		});
	},
	
	thaw: function(callback) {
		var form = this;
		var fnode = $('#' + form.Id);
		
		if(!form.RecordOrder) {
			callback();
			return;
		}
		
		form.loadCommon();
		
		// build a queue of record names (copy array) to load 
		var rnames = form.RecordOrder.concat(); 
		
		// define how to process the queue
		var qProcess = function() {
			// all done with thaw
			if (rnames.length == 0) {
				form.FreezeInfo = null;
				callback();					
				return;
			}
				
			var rname = rnames.shift();
			
			var event = { 
				Record: rname,
				Result: 0,
				Data: form.FreezeInfo[rname].Values,
				Originals: form.FreezeInfo[rname].Originals
			};
			
			form.raiseEvent('ThawRecord', event);
		
			form.thawRecord(event.Record, event.Data, event.Originals);
				
			// process next record in queue
			qProcess();
		};
		
		// start the queue processing
		qProcess();
	},
	
	loadCommon: function() {
		var form = this;
		var fnode = $('#' + form.Id);
		
		// Add novalidate tag if HTML5.
		fnode.attr('novalidate', 'novalidate');
	
		// validate the form on submit
		fnode.submit(function(e) {
			if (dc.pui.Loader.busyCheck()) 		// proect against user submit such as Enter in a TextField
				return false;
			
			dc.pui.Loader.__busy = true;
			
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
							form.query(inp.Input.Field).focus();
					});
					
					vpass = false;
					break;
				}
			}
			
			if (vpass) {
				form.save(function() {
					form.PageEntry.callPageFunc('Save');
					
					dc.pui.Loader.__busy = false;
				});
			}
			else {
				dc.pui.Loader.__busy = false;
			}

			e.preventDefault();
			return false;
		});		
	},	
	
	thawRecord: function(recname, data, originals) {
		var form = this;

		if (!data) 
			return;
		
		Object.getOwnPropertyNames(form.Inputs).forEach(function(name) {
			var iinfo = form.Inputs[name];
			
			if ((iinfo.Record == recname) && data.hasOwnProperty(iinfo.Field)) {
				iinfo.setValue(data[iinfo.Field]);
				iinfo.OriginalValue = originals[iinfo.Field];
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
		
		var fnode = $('#' + form.Id);
					
		var event = { 
			Changed: false
		};
		
		// do before save record event
		form.raiseEvent('BeforeSave', event);

		if (event.Stop) {
			callback();
			return;
		}
		
		var anychanged = event.Changed;
				
		// build a queue of record names to load 
		var rnames = form.RecordOrder.concat(); 
			
		// define how to process the queue
		var qProcess = function() {
			// all done with loading
			if (rnames.length == 0) {
				// do after save record event
				var event = {
					NoChange: !anychanged
				}
		
				form.raiseEvent('AfterSave', event);
				
				if (event.Alert)
					dc.pui.Popup.alert(event.Alert);
				else if (event.DefaultSaved)
					dc.pui.Popup.alert(anychanged ? 'Saved' : 'No changes, nothing to save.');

				callback();
				return;
			}
			
			var rname = rnames.shift();

			if (!anychanged && !form.isChanged(rname)) {
				// process next record in queue
				qProcess();
				return;
			}
			
			anychanged = true;
			
			var event = {
				Record: rname,
				Data: form.getChanges(rname)
			}
			
			var savecntdwn = new dc.lang.CountDownCallback(1, function() { 
				if (event.Alert) {
					dc.pui.Popup.alert(event.Alert);
					callback();
					return;
				}
				else if (event.Stop) {
					callback();
					return;
				}
				
				if (event.Message) {					
					dc.comm.sendMessage(event.Message, function (e) {
						if (e.Result != 0) { 
							dc.pui.Popup.alert(e.Message);
							callback();
							return;
						}
					
						form.clearChanges(rname);
						
						var aftersavecntdwn = new dc.lang.CountDownCallback(1, function() { 
							// process next record in queue
							qProcess();
						});
				
						event.Result = e;
						event.Data = e.Body;
						event.CountDown = aftersavecntdwn;

						form.raiseEvent('AfterSaveRecord', event);
								
						if (event.Alert) {
							dc.pui.Popup.alert(event.Alert);
							callback();
							return;
						}
						else if (event.Stop) {
							callback();
							return;
						}
						
						aftersavecntdwn.dec();						
					});
				}
				else {
					form.clearChanges(rname);
					
					// process next record in queue
					qProcess();
				}
			});
	
			event.CountDown = savecntdwn;
	
			// handler will set Message if we need to save data to bus  
			form.raiseEvent('SaveRecord', event);
				
			savecntdwn.dec();						
		};
		
		// start the queue processing
		qProcess();
	},
	
	initChanges: function(recname) { 
		var form = this;
		
		Object.getOwnPropertyNames(form.Inputs).forEach(function(name) {
			var iinfo = form.Inputs[name];
			
			if (iinfo.Record == recname) {
				iinfo.setValue(null);
				iinfo.OriginalValue = iinfo.defaultValue;
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
			
			if ((iinfo.Record == recname) && dc.pui.controls.isChanged(iinfo))
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
			
			if (asNew || dc.pui.controls.isChanged(iinfo)) 
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
		this.PageEntry.callPageFunc(this.Prefix ? this.Prefix + name : name, event);
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
			var mr = form.validateInput(form.InputOrder[n]);
			
			res.Inputs.push(mr);
			
			if (mr.Code != 0) 
				res.Pass = false;
		}
		
		return res;
	},

	validateInput: function(name) {
		var form = this;
		
		var iinfo = form.Inputs[name];
		
		var mr = new dc.lang.OperationResult();
		
		mr.Input = iinfo;
		
		var value = iinfo.getValue();
		var vempty = dc.util.Struct.isEmpty(value);
		
		if (iinfo.Required && vempty) 
			mr.errorTr(424, [ value, iinfo.Field ]);

		if (!mr.hasErrors() && iinfo.Pattern && !vempty) {
			var ptr = new RegExp('^(?:' + iinfo.Pattern + ')$');
			
			if (!ptr.test(value)) 
				mr.error(1, iinfo.PatternMessage ? iinfo.PatternMessage : "Invalid format.");
		}						
		
		if (!mr.hasErrors() && iinfo.DataType && !vempty) {
			var dt = dc.schema.Manager.resolveType(iinfo.DataType); 
			
			if (!dt)
				mr.errorTr(436);		
			else {
				dt.validate(value, mr);
				
				if (mr.hasErrors() && iinfo.TypeErrorMessage)
					mr.Message = iinfo.TypeErrorMessage;
			}
		}
	
		if (iinfo.Schema && !vempty) {
			iinfo.Schema.validate(!vempty, value, mr);
			
			// TODO improve error messages for 424, 409, others?
		}
		
		return mr;
	},

	validateControl: function(e) {
		var form = this;
		
		var target = $(e.target);
		var cname = target.attr('name');
		
		if (e.type == "keyup") {
			var code = e.keyCode || e.which;
			
			if (code == '9') 
				return;
		}
		
		var mr = form.validateInput(cname);

		// detect a subset of the "isChanged" looking for value changes
		var changed = false;
		
		Object.getOwnPropertyNames(form.Inputs).forEach(function(name) {
			var iinfo = form.Inputs[name];
			
			if ((iinfo.Record == mr.Input.Record) && dc.pui.controls.isChanged(iinfo))
				changed = true;
		});
		
		if (changed)
			form.updateInputMessage(mr);
	},
	
	updateMessages: function(formresults) {
		var form = this;
	
		for (var i = 0; i < formresults.Inputs.length; i++) 
			form.updateInputMessage(formresults.Inputs[i]);
	},
	
	updateInputMessage: function(mr) {
		var form = this;
	
		var element = form.query(mr.Input.Field);
		var ischk = (/radio|checkbox/i).test(element.attr('type'));
		var name = ischk ? element.attr('name') : element.attr('id');
		
		var label = $('#lbl' + name + 'Message');
		
		// TODO there is still work to be done for radio/check/unusual inputs

		// create label if not already present
		if (!label.length) {
			label = $("<label />")
				.attr({ 'for':  name, id: 'lbl' + name + 'Message', generated: true })
				.addClass('error')
				.insertAfter(element);
		}
		
		if (mr.Message) {
			// manage the message on generated labels only
			if (label.attr("generated"))
				label.html(mr.Message);
				
			label.show();
		}			
		else {
			label.hide();
		}
	}
};

// ------------------- end Form -------------------------------------------------------

dc.pui.Tags = {
	Link: {
		enhanceNode: function(layer, node) {
			var click = $(node).attr('data-dc-click');
			var page = $(node).attr('data-dc-page');
			var link = $(node).attr('href');
			
			if (link && (link.startsWith('http:') || link.startsWith('https:') || link.endsWith('.pdf')))
				$(node).attr('target', '_blank');
			
			if (click || page) {
				$(node).click(function(e) {
					if (!dc.pui.Loader.busyCheck()) {
						if (click && layer)
							layer.PageEntry.callPageFunc(click, e, this);
						else if (click)
							dc.pui.Loader.currentPageEntry().callPageFunc(click, e, this);
						else if (layer)
							layer.loadPage(page);
						else 
							dc.pui.Loader.loadPage(page);
					}
					
					e.preventDefault();
					return false;
				});
			}
			else if (link && (link.length > 1) && (l.charAt(0) == '#')) {
				$(node).click(link, function(e) {
					layer.scrollPage(e.data);
					e.preventDefault();
					return false;
				});
			}
		}
	/*

	// TODO convert to enhanceNode

	},
	SubmitButton: {
		createNode: function(layentry, child) {
			var node = $('<input type="submit" data-theme="a" data-mini="true" data-inline="true">');
			
			if (dc.util.String.isString(child.Label))
				node.attr('value', child.Label);
			
			if (dc.util.String.isString(child.Icon))
				node.attr('data-icon', child.Icon);
		
			if (dc.util.String.isString(child.Click) || dc.util.String.isString(child.Page)) {
				node.click(function(e) {
					if (!dc.pui.Loader.busyCheck()) {
						if (dc.util.String.isString(child.Click))
							layentry.PageEntry.callPageFunc(child.Click, e, this);
						else if (layentry.PageEntry.Layer)
							layentry.PageEntry.Layer.loadPage(child.Page);
						else 
							dc.pui.Loader.loadPage(child.Page);
					}
					
					e.preventDefault();
					return false;
				});
			}
							
			return { Node: node };
		}
	},
	Form: {
		createNode: function(layentry, child) {
			var node = $('<form />');
			
			if (dc.util.String.isString(child.Name)) {
				node.attr('id', 'frm' + child.Name);

				// don't add form info on a thaw, reuse but rebuild the inputs
				if (layentry.PageEntry.Forms[child.Name]) 
					layentry.PageEntry.Forms[child.Name].Inputs = { };
				else 
					layentry.PageEntry.Forms[child.Name] = new dc.pui.Form(layentry.PageEntry, child);
			}
				
			return { Node: node };
		}
	},
	FieldContainer: {
		createNode: function(layentry, child) {
			var node = $('<div data-role="fieldcontain" />');
				
			return { Node: node };
		},
		postNode: function(layentry, child, nres) {
			var fndLabelTarget = false;
			var lblText = child.Label ? child.Label : '&nbsp;';
		
			if (child.Children && child.Children.length) {
				var fldname = child.Children[0].Name;
				
				var form = layentry.findForm();
				
				if (fldname && form && form.Inputs[fldname]) {
					var id = form.Inputs[fldname].Id;
					
					// not && lblText which will always be set, only if we have a real label
					if (form.Inputs[fldname].Required && child.Label)
						nres.Node.prepend('<label for="' + id + '">' + lblText + ' <span class="fldreq">*</span></label>');
					else
						nres.Node.prepend('<label for="' + id + '">' + lblText + ' </label>');
						
					fndLabelTarget = true;
				}
			}
			
			if (!fndLabelTarget) {
				var id = nres.Node.children().first().attr('id');
				
				if (id)
					nres.Node.prepend('<label for="' + id + '">' + lblText + ' </label>');
			}
		}
	},
	Label: {
		createNode: function(layentry, child) {
			var node = $('<div />');
			var id = child.Id;
			
			if (!id) 
				id = dc.util.Uuid.create();
				
			node.attr('id', id);
			
			return { Node: node };
		}
	},
	FormInstruction: {
		createNode: function(layentry, child) {
			var node = $('<div class="ui-input-text ui-body-c" style="border: none;" />');
			node.attr('id', dc.util.Uuid.create());
			return { Node: node };
		}
	},
	nbsp: {
		createNode: function(layentry, child) {
			layentry.Element.append('&nbsp;');
			return null;
		}
	},
	HorizRadioGroup: {
		createNode: function(layentry, child) {
			var form = layentry.findForm();
			
			if (!form || !child.Name) 
				return null;
			
			var fname = child.Name;
				
			var input = form.Inputs[fname];
			
			if (!input) {
				child.DataType = child.DataType ? child.DataType : (child.Element == 'HorizCheckGroup') ? 'List' : 'dcString';
				
				var itype = (child.Element == 'HorizCheckGroup') ? 'RadioCheck' : 'RadioSelect';

				input = new dc.pui.controls[itype](form, child);
				
				form.Inputs[fname] = input;
				form.InputOrder.push(fname);
			}
			
			var node = $('<div data-role="fieldcontain" />');
			
			fsnode = $('<fieldset data-role="controlgroup" data-type="horizontal" data-mini="true" />');

			fsnode.attr('id', input.Id);
			
			if (input.Required && child.Label)
				fsnode.append('<legend>' + child.Label + ' <span class="fldreq">*</span></legend>');
			else if (child.Label)
				fsnode.append('<legend>' + child.Label + '</legend>');
			
			if (child.Name) {
				fsnode.append(
					$("<label />")
						.attr({ 'for':  child.Name, id: 'lbl' + child.Name + 'Message', generated: true })
						.addClass('error')
						.hide()
				);
			}
			
			node.append(fsnode);
						
			return { Node: node };
		}
	},
	RadioGroup: {
		createNode: function(layentry, child) {
			var form = layentry.findForm();
			
			if (!form || !child.Name) 
				return null;
			
			var fname = child.Name;
				
			var input = form.Inputs[fname];
			
			if (!input) {
				child.DataType = child.DataType ? child.DataType : (child.Element == 'CheckGroup') ? 'List' : 'dcString';
				
				var itype = (child.Element == 'CheckGroup') ? 'RadioCheck' : 'RadioSelect';

				input = new dc.pui.controls[itype](form, child);
				
				form.Inputs[fname] = input;
				form.InputOrder.push(fname);
			}
			
			node = $('<div data-role="fieldcontain" />');
			
			fsnode = $('<fieldset data-role="controlgroup" data-mini="true" />');

			fsnode.attr('id', input.Id);
			
			if (input.Required && child.Label)
				fsnode.append('<legend>' + child.Label + ' <span class="fldreq">*</span></legend>');
			else if (child.Label)
				fsnode.append('<legend>' + child.Label + '</legend>');
			
			if (child.Name) {
				fsnode.append(
					$("<label />")
						.attr({ 'for':  child.Name, id: 'lbl' + child.Name + 'Message', generated: true })
						.addClass('error')
						.hide()
				);
			}
			
			node.append(fsnode);
						
			return { Node: node };
		}
	},
	
	RadioButton: {
		createNode: function(layentry, child) {
			var form = layentry.findForm();
			
			if (!form || !layentry.Definition.Name) 
				return null;
				
			var fname = layentry.Definition.Name;
				
			var input = form.Inputs[fname];
			
			if (!input) {
				child.DataType = layentry.Definition.DataType ? layentry.Definition.DataType : 'dcString';
				child.Record = layentry.Definition.Record ? layentry.Definition.Record : 'Default';
				child.Field = fname;
				
				if (layentry.Definition.Required == 'true')
					child.Required = true;

				input = new dc.pui.controls['RadioSelect'](form, child);
				
				form.Inputs[fname] = input;
				form.InputOrder.push(fname);
			}
			
			var val = dc.util.Struct.isEmpty(child.Value) ? child.Label : child.Value;
			var id = input.Id + '-' + md5(val);
			
			node = $('<input type="radio" />');
			node.attr('id', id);
			node.attr('name', fname);
			node.attr('value', val);
			
			layentry.Element.find('fieldset label').last()
				.before(node)
				.before('<label for="' + id + '">' + child.Label + '</label>');
			
			node.bind("click", function(e) { form.validateControl(e); });
			
			// skip further processing on this control
			return null;
		}
	},
	RadioCheck: {
		createNode: function(layentry, child) {
			var form = layentry.findForm();
			
			if (!form || !layentry.Definition.Name) 
				return null;
				
			var fname = layentry.Definition.Name;
				
			var input = form.Inputs[fname];
			
			if (!input) {
				child.DataType = layentry.Definition.DataType ? layentry.Definition.DataType : 'dcString';
				child.Record = layentry.Definition.Record ? layentry.Definition.Record : 'Default';
				child.Field = fname;
				
				if (layentry.Definition.Required == 'true')
					child.Required = true;

				input = new dc.pui.controls['RadioCheck'](form, child);
				
				form.Inputs[fname] = input;
				form.InputOrder.push(fname);
			}
			
			var val = dc.util.Struct.isEmpty(child.Value) ? child.Label : child.Value;
			var id = input.Id + '-' + md5(val);
			
			node = $('<input type="checkbox" />');
			node.attr('id', id);
			node.attr('name', fname);
			node.attr('value', val);
			
			if (child.Checked)
				node.attr('checked', 'checked');
			
			layentry.Element.find('fieldset label').last()
				.before(node)
				.before('<label for="' + id + '">' + child.Label + '</label>');
			
			node.bind("click", function(e) { form.validateControl(e); });
			
			// skip further processing on this control
			return null;
		}
	},
	GenericInput: {
		createNode: function(layentry, child) {
			var form = layentry.findForm();
			
			if (!form) 
				return null;
			
			var itype = child.Element;
			var node = null;
			
			if (itype == 'RadioSelect') {
				node = $('<input type="radio" data-mini="true" />');
				node.bind("click", function(e) { form.validateControl(e); });
			}
			else if (itype == 'Range') {
				node = $('<input type="range" data-mini="true" />');
				node.bind("focusout keyup", function(e) { form.validateControl(e); });
			}
			else if (itype == 'Select') {
				node = $('<select data-mini="true" data-native-menu="true" />');
				node.bind("click focusout keyup", function(e) { form.validateControl(e); });
			}
			else if (itype == 'YesNo') {
				node = $('<select data-role="flipswitch" data-mini="true"> \
						<option Value="false">No</option> \
						<option Value="true">Yes</option> \
					</select>');
			}
			else if (itype == 'TextArea') {
				itype = 'TextInput';
				node = $('<textarea data-mini="true" />');
				node.bind("focusout keyup", function(e) { form.validateControl(e); });
			}
			else if (itype == 'PasswordInput') {
				itype = 'TextInput';
				node = $('<input type="password" data-mini="true" />');
				node.bind("focusout keyup", function(e) { form.validateControl(e); });
			}
			else if (itype == 'HiddenInput') {
				itype = 'TextInput';
				node = $('<input type="hidden" data-mini="true" />');
			}
			else {
				itype = 'TextInput';
				node = $('<input type="text" data-mini="true" />');			
				node.bind("focusout keyup", function(e) { form.validateControl(e); });
			}
				
			var input = new dc.pui.controls[itype](form, child);
		
			form.Inputs[input.Field] = input;
			form.InputOrder.push(input.Field);
				
			node.attr('id', input.Id);
			node.attr('name', input.Field);
			
			node.focusin({ Form: form.Name, Field: input.Field }, function(e) {
				layentry.PageEntry.Store.__LastFocus = e.data;
			});
					
			return { Node: node };
		}
		*/
	}
};

dc.pui.TagAlias = {
	/* TODO
	CheckGroup: 'RadioGroup',
	HorizCheckGroup: 'HorizRadioGroup',
	TextInput: 'GenericInput',
	PasswordInput: 'GenericInput',
	RadioSelect: 'GenericInput',
	Range: 'GenericInput',
	Select: 'GenericInput',
	YesNo: 'GenericInput',
	TextArea: 'GenericInput',
	HiddenInput: 'GenericInput'
	*/
};

// ------------------- end Tags -------------------------------------------------------

dc.pui.controls = { 
	buildInput: function(form, child) {
		var id = child.Id;
		
		if (!id) 
			id = dc.util.Uuid.create();
	
		var fname = child.Name;
		
		if (!fname)
			return;
		
		var rec = child.Record;
		
		if (!rec) 
			rec = 'Default';

		var dtype = child.DataType;
		
		if (!dtype) 
			dtype = 'String';

		var input = {
			Id: id,
			Field: fname,
			Record: rec,
			DataType: dtype,
			Form: form,
			OriginalValue: null,
			Required: (child.Required == 'true'),
			Pattern: child.Pattern,
			PatternMessage: child.PatternErrorMessage,
			TypeMessage: child.TypeErrorMessage
		};
		
		// we can override at field level by setting DataType
		if (!child.DataType && form.RecordMap[rec] && form.RecordMap[rec].Fields[fname]) 
			input.Schema = form.RecordMap[rec].Fields[fname];
		
		if (!child.Required && input.Schema && (input.Schema.Required == 1))
			input.Required = true;
		
		return input;
	},
	isChanged: function(ctrl) {
		// handle lists differently, others are simple scalar
		if (!dc.util.Struct.isList(ctrl.OriginalValue))		
			return (ctrl.OriginalValue != ctrl.getValue());

		var values = ctrl.getValue();
	
		if (values.length != ctrl.OriginalValue.length)
			return true;
		
		for (var i = 0; i < values.length; i++) {
			var val = values[i];
			var contains = false;
			
			for (var i2 = 0; i2 < ctrl.OriginalValue.length; i2++) {
				var val2 = ctrl.OriginalValue[i2];
				
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
};

dc.pui.controls.TextInput = function(form, child) {
	$.extend(this, dc.pui.controls.buildInput(form, child));
}

dc.pui.controls.TextInput.prototype = {
	defaultValue: null,
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
	}
};

dc.pui.controls.RadioSelect = function(form, child) {
	$.extend(this, dc.pui.controls.buildInput(form, child));
}

dc.pui.controls.RadioSelect.prototype = {
	defaultValue: null,
	setValue: function(value) {
		value = dc.util.String.toString(value);
		
		if (!value) 
			value = 'NULL';
		
		$('#' + this.Id + '-' + md5(value)).prop('checked',true);
		$('#' + this.Form.Id + ' input[name=' + this.Field + ']').checkboxradio("refresh");
	},
	getValue: function() {
		var val = $('#frm' + this.Form.Name + ' input[name=' + this.Field + ']:checked').val();
		
		if ((val == 'NULL') || dc.util.Struct.isEmpty(val))
			val = null;
			
		return val;
	},
	removeAll: function() {
		$('#' + this.Id + " .ui-controlgroup-controls").empty();
		$('#' + this.Form.Id + ' input[name=' + this.Field + ']').checkboxradio("refresh");
	},
	add: function(values) {
		for (var i = 0; i < values.length; i++) {
			var opt = values[i];			
			var val = dc.util.Struct.isEmpty(opt.Value) ? 'NULL' : opt.Value;
			var oval = (val == 'NULL') ? null : val;
			// TODO fix md5 issue
			var id = this.Id + '-' + md5(val);
			
			if (this.OriginalValue == oval)
				$('#' + this.Id + " .ui-controlgroup-controls").append($('<input type="radio" id="' + id + '" value="' + val + '" checked="checked" name="' + this.Field + '" />'));
			else
				$('#' + this.Id + " .ui-controlgroup-controls").append($('<input type="radio" id="' + id + '" value="' + val + '" name="' + this.Field + '" />'));
			
			$('#' + this.Id + " .ui-controlgroup-controls").append('<label for="' + id + '">' + opt.Label + '</label>');
		}
		
		// TODO rework enhance, add
		$('#' + this.Id).enhanceWithin();
	}
};

dc.pui.controls.RadioCheck = function(form, child) {
	$.extend(this, dc.pui.controls.buildInput(form, child));
}

dc.pui.controls.RadioCheck.prototype = {
	defaultValue: [ ],
	setValue: function(values) {
		if (!dc.util.Struct.isList(values))
			values = [];
		
		for (var i = 0; i < values.length; i++)
			$('#' + this.Id + '-' + md5(values[i])).prop('checked',true).checkboxradio("refresh");
	},
	getValue: function() {
		return $('#' + this.Form.Id + ' input[name=' + this.Field + ']:checked').map(function() { return this.value; }).get();
	},
	removeAll: function() {
		$('#' + this.Id + " .ui-controlgroup-controls").empty();
		$('#' + this.Form.Id + ' input[name=' + this.Field + ']').checkboxradio("refresh");
	},
	add: function(values) {
		for (var i = 0; i < values.length; i++) {
			var opt = values[i];			
			var val = dc.util.Struct.isEmpty(opt.Value) ? 'NULL' : opt.Value;
			var oval = (val == 'NULL') ? null : val;
			var id = this.Id + '-' + md5(val);
			
			// TODO node.bind("click", function(e) { form.validateControl(e); });
			
			$('#' + this.Id + " .ui-controlgroup-controls").append('<label for="' + id + '">' + opt.Label + '</label>');
			
			var vmatch = false;
			
			if (this.OriginalValue) {
				for (var o = 0; o < this.OriginalValue.length; o++) {
					if (this.OriginalValue[o] == oval) {
						vmatch = true;
						break;
					}
				}
			}
					
			if (vmatch)
				$('#' + this.Id + " .ui-controlgroup-controls").append($('<input type="checkbox" id="' + id + '" value="' + val + '" checked="checked" name="' + this.Field + '" />'));
			else
				$('#' + this.Id + " .ui-controlgroup-controls").append($('<input type="checkbox" id="' + id + '" value="' + val + '" name="' + this.Field + '" />'));
		}
		
		$('#' + this.Id).enhanceWithin();
	}
};

dc.pui.controls.YesNo = function(form, child) {
	child.DataType = 'Boolean';
	
	$.extend(this, dc.pui.controls.buildInput(form, child));
}

dc.pui.controls.YesNo.prototype = {
	defaultValue: false,
	setValue: function(value) {
		value = dc.util.Boolean.toBoolean(value);
		
		$('#' + this.Id).val(value + '').flipswitch("refresh");
	},
	getValue: function() {
		var val = $('#' + this.Id).val();
		
		return (val == 'true');
	}
};

dc.pui.controls.Range = function(form, child) {
	$.extend(this, dc.pui.controls.buildInput(form, child));
}

dc.pui.controls.Range.prototype = {
	defaultValue: null,
	setValue: function(value) {
		value = dc.util.String.toString(value);

		if (!value)
			value = '';
		
		$('#' + this.Id).val(value);
		$('#' + this.Id).slider('refresh');
	},
	getValue: function() {
		var val = $('#' + this.Id).val();
		
		if (dc.util.Struct.isEmpty(val))
			val = null;
			
		return val;
	}
};

dc.pui.controls.Select = function(form, child) {
	$.extend(this, dc.pui.controls.buildInput(form, child));
}

dc.pui.controls.Select.prototype = {
	defaultValue: null,
	setValue: function(value) {
		value = dc.util.String.toString(value);
		
		if (!value) 
			value = 'NULL';
		
		$('#' + this.Id).val(value).selectmenu("refresh");
	},
	getValue: function() {
		var val = $('#' + this.Id).val();
		
		if ((val == 'NULL') || dc.util.Struct.isEmpty(val))
			val = null;
			
		return val;
	},
	add: function(values) {
		for (var i = 0; i < values.length; i++) {
			var opt = values[i];			
			var val = dc.util.Struct.isEmpty(opt.Value) ? 'NULL' : opt.Value;
			var oval = (val == 'NULL') ? null : val;
			
			if (this.OriginalValue == oval)
				$('#' + this.Id).append($('<option value="' + val + '" selected="selected">' + opt.Label + '</option>'));
			else
				$('#' + this.Id).append($('<option value="' + val + '">' + opt.Label + '</option>'));
		}
		
		$('#' + this.Id).selectmenu("refresh");
	}
};

// ------------------- end Controls -------------------------------------------------------
