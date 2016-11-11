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


if (!dc.cms)
	dc.cms = {};

dc.cms.Loader = {
	init: function(options) {
		if (! options)
			options = { 
				Page: '/dcm/cms/page/Edit-Feed-Prop',
				Menu: 'dcmPageProps'
			};
	
		var itab = $('<div id="dcuiAppTab"><i class="fa fa-angle-double-right"></i></div>');
		
		itab.click(function (e) {
			dc.pui.App.start(options);
			
			e.preventDefault();
			return false;
		});
		
		$('body').append(itab);
		
		if (dc.user.isAuthorized(['Editor', 'Admin', 'Developer'])) { 
			// --------------------------------------------
			// part buttons
			// --------------------------------------------
			
			var sectbtn = $('<div class="dcuiPartButton"><i class="fa fa-cog"></i></div>');
			
			sectbtn.click(function (e) {
				var pel = $(this).closest('*[data-dccms-mode="edit"]').get(0);
				
				dc.pui.App.Context = {
					Menu: 'dcmPageProps',
					Params: {
						Part: { 
							Channel: $(pel).attr('data-dccms-channel'), 
							Path: $(pel).attr('data-dccms-path'),
							Part: $(pel).attr('id')
						}
					}
				};
				
				dc.pui.Popup.menu('dcmPartPopup');
				
				e.preventDefault();
				return false;
			});
			
			$('*[data-dccms-mode="edit"]').prepend(sectbtn);
			
			// --------------------------------------------
			// section buttons
			// --------------------------------------------

			var sectbtn = $('<div class="dcuiSectionButton"><i class="fa fa-pencil"></i></div>');
			
			sectbtn.click(function (e) {
				var sel = $(this).closest('div.dc-section').get(0);
				var pel = $(this).closest('*[data-dccms-mode="edit"]').get(0);
				
				dc.pui.App.Context = {
					Menu: 'dcmPagePartSection',
					Params: {
						Part: { 
							Channel: $(pel).attr('data-dccms-channel'), 
							Path: $(pel).attr('data-dccms-path'), 
							Part: $(pel).attr('id')
						},
						Section: {
							Id: $(sel).attr('id')
						} 
					}
				};
				
				dc.pui.Popup.menu('dcmSectionPopup');
				
				e.preventDefault();
				return false;
			});
			
			$('*[data-dccms-mode="edit"] div.dc-section[id][data-dccms-section="plugin"]').prepend(sectbtn);
		}
	}
};

dc.pui.Apps.Menus.dcmEmpty = {
	Options: [
	]
};

dc.pui.Apps.Menus.dcmPageProps = {
	Options: [
		{
			Title: 'Page Properties',
			Op: function(e) {
				// assumes the context is already set
				dc.pui.App.loadPage('/dcm/edit/page/Edit-Feed-Prop');
			}
		},
		{
			Title: 'Galleries',
			Op: function(e) {
				dc.pui.App.loadPage('/dcm/edit/galleries/Browser');
			}
		},
		{
			Title: 'Files',
			Op: function(e) {
				dc.pui.App.loadPage('/dcm/edit/files/Browser');
			}
		},
		{
			Title: 'Sign Out',
			Op: function(e) {
				dc.pui.Loader.signout();
			}
		}
	]
};
		
dc.pui.Apps.Menus.dcmPagePart = {
	Options: [
		{
			Title: 'Content',
			Auth: [ 'Developer' ],
			Op: function(e) {
				dc.pui.App.loadPage('/dcm/edit/page/Edit-Part-Content');
			}
		}
	]
};

dc.pui.Apps.Menus.dcmPartPopup = {
	Options: [
		{
			Title: 'Edit',
			Op: function(e) {
			    var params = dc.pui.App.Context.Params;
			    
				dc.pui.App.loadPage('/dcm/cms/page/Edit-Part-Content');
			}
		},
		{
			Title: 'Insert Section',
			Op: function(e) {
			    var params = dc.pui.App.Context.Params;
				
			    params.Action = {
			    	Op: 'InsertBottom'
			    };
			    
			    // create a new section id
			    params.Section = {
		    		Id: 'sectAuto' + dc.util.Crypto.makeSimpleKey()
			    };
				
			    dc.pui.App.Context.Menu = 'dcmEmpty';
			    dc.pui.App.Context.IsNew = true;
				
				dc.pui.App.loadPage('/dcm/edit/page/Insert-Section');
			}
		}
	]
};

dc.pui.Apps.Menus.dcmPagePartSection = {
	Options: [
		{
			Title: 'Content',
			Op: function(e) {
				dc.pui.App.loadPage('/dcm/edit/page/Edit-Section-Content');
			}
		},
		{
			Title: 'Properties',
			Op: function(e) {
			    var params = dc.pui.App.Context.Params;
			    
				dc.pui.App.loadPage('/dcm/edit/page/Edit-Section-Prop-' + params.Section.Plugin);
			}
		}
	]
};

dc.pui.Apps.Menus.dcmPagePartGallerySection = {
	Options: [
		{
			Title: 'List',
			Op: function(e) {
				dc.pui.App.loadPage('/dcm/edit/page/Edit-Section-Gallery');
			}
		},
		{
			Title: 'Template',
			Op: function(e) {
				dc.pui.App.loadPage('/dcm/edit/page/Edit-Section-Template');
			}
		},
		{
			Title: 'Properties',
			Op: function(e) {
			    var params = dc.pui.App.Context.Params;
			    
				dc.pui.App.loadPage('/dcm/edit/page/Edit-Section-Prop-' + params.Section.Plugin);
			}
		}
	]
};

dc.pui.Apps.Menus.dcmSectionPopup = {
	Options: [
		{
			Title: 'Edit',
			Op: function(e) {
			    var params = dc.pui.App.Context.Params;
				
			    params.Action = {
			    	Op: 'Edit'
			    };
			    
			    var $sect = $('#' + params.Section.Id);
			    
			    if ($sect.hasClass('dc-section-gallery')) {
					dc.pui.App.Context.Menu = 'dcmPagePartGallerySection';
					
					params.Show = {
						Path: $sect.attr('data-path'),
						Alias: $sect.attr('data-show')
					};
					
					dc.pui.App.loadPage('/dcm/edit/page/Edit-Section-Gallery');
			    }
			    else {
					dc.pui.App.loadPage('/dcm/edit/page/Edit-Section-Content');
				}
			}
		},
		{
			Title: 'Delete',
			Op: function(e) {
				dc.pui.Popup.confirm('Are you sure you want to remove this section?', function() {
				    var params = dc.pui.App.Context.Params;

				    params.Action = {
				    	Op: 'Delete',
				    	TargetSection: params.Section.Id,
				    	Publish: true
				    };
				    
				    delete params.Section;
				    
					dc.comm.sendMessage({ 
						Service: 'dcmCore', 
						Feature: 'Feeds', 
						Op: 'AlterFeedSection', 
						Body: params
					}, function(rmsg) {
						if (rmsg.Result != 0) 
							dc.pui.Popup.alert(rmsg.Message);
						else
							dc.pui.Popup.alert('Removed', function() { dc.pui.Loader.MainLayer.refreshPage(); });
					});
				});
			}
		},
		{
			Title: 'Insert Section Before',
			Op: function(e) {
			    var params = dc.pui.App.Context.Params;

			    params.Action = {
			    	Op: 'InsertAbove',
			    	TargetSection: params.Section.Id
			    };
			    
			    // create a new section id
			    params.Section.Id = 'sectAuto' + dc.util.Crypto.makeSimpleKey();
				
			    dc.pui.App.Context.Menu = 'dcmEmpty';
			    dc.pui.App.Context.IsNew = true;
				
				dc.pui.App.loadPage('/dcm/edit/page/Insert-Section');
			}
		},
		{
			Title: 'Insert Section After',
			Op: function(e) {
			    var params = dc.pui.App.Context.Params;
				
			    params.Action = {
			    	Op: 'InsertBelow',
			    	TargetSection: params.Section.Id
			    };
			    
			    // create a new section id
			    params.Section.Id = 'sectAuto' + dc.util.Crypto.makeSimpleKey();
				
			    dc.pui.App.Context.Menu = 'dcmEmpty';
			    dc.pui.App.Context.IsNew = true;
				
				dc.pui.App.loadPage('/dcm/edit/page/Insert-Section');
			}
		},
		{
			Title: 'Duplicate',
			Op: function(e) {
			    var params = dc.pui.App.Context.Params;
				
			    params.Action = {
			    	Op: 'InsertBelow',
			    	TargetSection: params.Section.Id,
			    	Publish: true
			    };
				
				params.Part.Section = params.Section.Id;
			    
				dc.comm.sendMessage({ 
					Service: 'dcmCore', 
					Feature: 'Feeds', 
					Op: 'LoadFeedSection', 
					Body: params.Part
				}, function(rmsg) {
					if (rmsg.Result != 0) {
						dc.pui.Popup.alert(rmsg.Message);
						return;
					}
					
					params.Section = rmsg.Body;
				    
				    // create a new section id - this is a dup
				    params.Section.Id = 'sectAuto' + dc.util.Crypto.makeSimpleKey();
					
				    // not allowed in save
				    delete params.Part.Section;
				    
					dc.comm.sendMessage({ 
						Service: 'dcmCore', 
						Feature: 'Feeds', 
						Op: 'AlterFeedSection', 
						Body: params
					}, function(rmsg) {
						if (rmsg.Result != 0) 
							dc.pui.Popup.alert(rmsg.Message);
						else
							dc.pui.Loader.currentPageEntry().callPageFunc('dcmCoreEvent', { Name: 'Close', Target: 'CMSPane' });
					});
				});
			}
		},
		{
			Title: 'Move Up',
			Op: function(e) {
				dc.pui.Popup.confirm('Are you sure you want to move this section up?', function() {
				    var params = dc.pui.App.Context.Params;

				    params.Action = {
				    	Op: 'MoveUp',
				    	TargetSection: params.Section.Id,
				    	Publish: true
				    };
				    
				    delete params.Section;
				    
					dc.comm.sendMessage({ 
						Service: 'dcmCore', 
						Feature: 'Feeds', 
						Op: 'AlterFeedSection', 
						Body: params
					}, function(rmsg) {
						if (rmsg.Result != 0) 
							dc.pui.Popup.alert(rmsg.Message);
						else
							dc.pui.Popup.alert('Moved', function() { dc.pui.Loader.MainLayer.refreshPage(); });
					});
				});
			}
		},
		{
			Title: 'Move Down',
			Op: function(e) {
				dc.pui.Popup.confirm('Are you sure you want to move this section down?', function() {
				    var params = dc.pui.App.Context.Params;

				    params.Action = {
				    	Op: 'MoveDown',
				    	TargetSection: params.Section.Id,
				    	Publish: true
				    };
				    
				    delete params.Section;
				    
					dc.comm.sendMessage({ 
						Service: 'dcmCore', 
						Feature: 'Feeds', 
						Op: 'AlterFeedSection', 
						Body: params
					}, function(rmsg) {
						if (rmsg.Result != 0) 
							dc.pui.Popup.alert(rmsg.Message);
						else
							dc.pui.Popup.alert('Moved', function() { dc.pui.Loader.MainLayer.refreshPage(); });
					});
				});
			}
		}
	]
};

// ------------------- end Tags -------------------------------------------------------
