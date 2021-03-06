
$(document).ready(function() {
	// stop with Googlebot.  Googlebot may load page and run script, but no further than this so indexing is correct (index this page)
	if (navigator.userAgent.indexOf('Googlebot') > -1) 
		return;

	var dmode = dc.util.Http.getParam('_dcui');
	
	if ((dmode == 'html') || (dmode == 'print'))
		return;
	
	$.animate = $.velocity;
	
	dc.pui.Loader.init();
	
	if (typeof ga == 'function') {
		ga('create', dc.handler.settings.ga, 'auto');
		ga('set', 'forceSSL', true);
	}
	
	dc.comm.init(function() {
		if (dc.handler.init)
			dc.handler.init();

		//dc.pui.Loader.loadDestPage();

		//* TODO
		var info = dc.user.getUserInfo();

		if (info.Credentials) {
			dc.user.signin(info.Credentials.Username, info.Credentials.Password, info.RememberMe, function(msg) { 
				dc.pui.Loader.loadDestPage();
			});
		}
		else {
			// load user from current server session
			dc.user.updateUser(false, function() {
				dc.pui.Loader.loadDestPage();
			}, true);
		}
		//*/
	});
});
