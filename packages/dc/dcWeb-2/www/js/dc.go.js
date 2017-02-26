
$(document).ready(function() {
	// stop with Googlebot.  Googlebot may load page and run script, but no further than this so indexing is correct (index this page)
	if (navigator.userAgent.indexOf('Googlebot') > -1) 
		return;

	var dmode = dc.util.Web.getQueryParam('_dcui');
	
	if ((dmode == 'html') || (dmode == 'print'))
		return;
	
	dc.pui.Loader.init();
	
	if (dc.handler && dc.handler.settings && dc.handler.settings.ga) 
		loadGA();
	
	dc.comm.init(function() {
		if (dc.handler && dc.handler.init)
			dc.handler.init();

		var info = dc.user.getUserInfo();

		if (info.Credentials) {
			dc.user.signin(info.Credentials.Username, info.Credentials.Password, info.RememberMe, function(msg) { 
				dc.pui.Loader.loadPage(dc.pui.Loader.OriginPage);
			});
		}
		else {
			// load user from current server session
			dc.user.updateUser(false, function() {
				dc.pui.Loader.loadPage(dc.pui.Loader.OriginPage);
			}, true);
		}
	});
});

function loadGA() {
	GoogleAnalyticsObject = 'ga';
	
	ga = function() {
		ga.q.push(arguments)
	};
	
	ga.q = [];
	ga.l = 1 * new Date();
	
	var script = document.createElement('script');
	script.src = 'https://www.google-analytics.com/analytics.js';
	script.async = true;  	
	document.head.appendChild(script);
	
	ga('create', dc.handler.settings.ga, 'auto');
	ga('set', 'forceSSL', true);
}