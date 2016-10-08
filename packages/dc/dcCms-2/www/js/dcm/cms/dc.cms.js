
// dc.pui.Loader.addExtraLibs( ['/dcm/edit/js/main.js'] );
// dc.pui.Loader.addExtraStyles( ['/dcm/edit/css/main.css'] );

if (!dc.cms)
	dc.cms = {};

dc.cms.ui = {
	__context: null,
	
	Loader: {
		getContext: function() {
			return dc.cms.ui.__context;
		},
		setContext: function(v) {
			dc.cms.ui.__context = v;
		},
		init: function(callback) {
			var itab = $('<div id="dcuiIntegratedTab" class="ui-content"><i class="fa fa-angle-double-right"></i></div>');
			
			itab.click(function (e) {
				if (callback)
					callback.call(dc.cms.ui.Loader);
				else
					dc.cms.ui.Loader.loadMenu();
				
				e.preventDefault();
				return false;
			});
			
			$('body').append(itab);
			
			if (dc.user.isAuthorized(['Editor', 'Admin'])) { 
				var sectbtn = $('<div class="ui-content dcuiPartButton"><i class="fa fa-cog"></i></div>');
				
				var peid = dc.cms.ui.Loader.addPopupMenu(dc.cms.ui.MenuEnum.PartPopup);
				
				sectbtn.click(function (e) {
					var pel = $(this).closest('*[data-dccms-mode="edit"]').get(0);
					
					dc.cms.ui.__context = {
						Menu: dc.cms.ui.MenuEnum.PagePart,
						Params: {
							Part: { 
								Channel: $(pel).attr('data-dccms-channel'), 
								Path: $(pel).attr('data-dccms-path'),
								Part: $(pel).attr('id')
							}
						}
					};
					
					$('#' + peid).popup('open', { positionTo: e.currentTarget });
					
					e.preventDefault();
					return false;
				});
				
				$('*[data-dccms-mode="edit"]').prepend(sectbtn);
			}
				
			if (dc.user.isAuthorized(['Editor', 'Admin'])) { 
				var sectbtn = $('<div class="ui-content dcuiSectionButton"><i class="fa fa-pencil"></i></div>');
				
				// --------------------------------------------
				// create the popup menu for section button
				// --------------------------------------------
				
				var seid = dc.cms.ui.Loader.addPopupMenu(dc.cms.ui.MenuEnum.SectionPopup);
				
				sectbtn.click(function (e) {
					var sel = $(this).closest('div.dc-section').get(0);
					var pel = $(this).closest('*[data-dccms-mode="edit"]').get(0);
					
					dc.cms.ui.__context = {
						Menu: dc.cms.ui.MenuEnum.PagePartSection,
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
					
					$('#' + seid).popup('open', { positionTo: e.currentTarget });
					
					e.preventDefault();
					return false;
				});
				
				$('*[data-dccms-mode="edit"] div.dc-section[id][data-dccms-section="plugin"]').prepend(sectbtn);
			}
		},
		start: function() {
			dc.cms.ui.Loader.loadMenu();
		},
		getPane: function() {
			var pane = dc.pui.Loader.getLayer("CMSPane");
			
			if (!pane) {
				// TODO maybe override Pane class for CMS
				
				$('#dcuiIntegratedCms').remove();
	
				$('body').append('<div id="dcuiIntegratedCms" class="ui-content">'
					+ '<div id="dcuiIntegratedMenu"></div><div id="dcuiIntegratedPane"></div>'
					+ '</div>');
				
				pane = new dc.cms.ui.Layer('#dcuiIntegratedPane', { Name: 'CMSPane' });
				
				pane.setObserver('CMSPane', function() {
					dc.cms.ui.Loader.loadMenu();
				});
				
				dc.pui.Loader.addLayer(pane);
			}
			
			return pane;
		},
		// area from MenuEnum above (or from custom defined)
		loadMenu: function() {
			var pane = dc.cms.ui.Loader.getPane();
			
			$('#dcuiIntegratedCms').show();
			
			var area = null;
			
			// get area from current pane, if any
			var entry = pane.currentPageEntry();
			
			if (entry && entry.Params._Menu)
				area = entry.Params._Menu;
			
			if (!area && dc.cms.ui.__context && dc.cms.ui.__context.Menu)
				area = dc.cms.ui.__context.Menu;
			
			if (!area)
				area = dc.cms.ui.MenuEnum.PageProps;
				
			//console.log('menu area: ' + area);
			
			$('#dcuiIntegratedMenu').empty();
			
			// if any page is loaded, use back
			var hist = pane.getHistory();
			
			var mcntrl = $('<div id="dcuiIntegratedMenuCntrl"></div>');
			
			// mobile popup menu
			var node1 = $('<a href="#"></a>');
			node1.append('<i class="fa fa-bars"></i>');
			node1.addClass('mobile');
			
			// the CMS popups don't get officially destroyed since they get erased on page loads
			var puid = dc.cms.ui.Loader.addPopupMenu(area);
			
			node1.click(function(e) {
				$('#' + puid).popup('open', { positionTo: e.currentTarget });
				
				e.preventDefault();
				return false;
			});
			
			mcntrl.append(node1);
			
			// help menu
			var node0 = $('<a href="#"></a>');
			node0.append('<i class="fa fa-question"></i>');
			
			node0.click(function(e) {
				var hpage = dc.pui.Loader.getLayer("CMSPane").currentPageEntry().Name + "-help";
				
				dc.cms.ui.Loader.loadPane(hpage);
			});
			
			mcntrl.append(node0);
			
			// back menu
			var node2 = $('<a id="dcuiIntegratedMenuBack" href="#"></a>');
			node2.append('<i class="fa fa-chevron-left"></i>');
			
			node2.click(function(e) {
				if (! $(this).hasClass('disabled')) {
					dc.cms.ui.Loader.closePane();
				}
			});
			
			if (hist.length == 0) 
				node2.addClass('disabled');
			
			mcntrl.append(node2);
			
			// close menu
			var node3 = $('<a href="#"></a>');
			node3.append('<i class="fa fa-times"></i>');
			
			node3.click(function(e) {
				dc.cms.ui.Loader.closeAll();
			});
			
			mcntrl.append(node3);
			
			$('#dcuiIntegratedMenu').append(mcntrl);
			
			var addbMenu = function(mnu) {
				if (mnu.Auth && !dc.user.isAuthorized(mnu.Auth))
					return;
				
				var node = $('<a href="#" data-role="button" data-theme="a" data-mini="true"></a>');
				
				node.text(mnu.Title);
				node.addClass(mnu.Kind);
				
				node.click(mnu, function(e) {
					if (!dc.pui.Loader.busyCheck()) 
						e.data.Op.call(dc.cms.ui.Loader, e);
					
					e.preventDefault();
					return false;
				});
			
				$('#dcuiIntegratedMenu').append(node);
			};
			
			if (dc.cms.ui.Menu[area]) {
				for (var i = 0; i < dc.cms.ui.Menu[area].Menu.length; i++) 
					addbMenu(dc.cms.ui.Menu[area].Menu[i]);
			}
			
			if (dc.handler.cms && dc.handler.cms.Menu[area]) {
				for (var i = 0; i < dc.handler.cms.Menu[area].Menu.length; i++) 
					addbMenu(dc.handler.cms.Menu[area].Menu[i]);
			}
			
			$('#dcuiIntegratedMenu').enhanceWithin();
		},
		addPopupMenu: function(area, id) {
			if (!id) 
				id = dc.util.Uuid.create();
			
			var popSectBtn = $('<div data-role="popup" data-theme="b"></div>');
			popSectBtn.attr('id', id);
			
			var lstSectBtn = $('<ul data-role="listview" data-inset="true" style="min-width:210px;" data-icon="false"></ul>');
			
			popSectBtn.append(lstSectBtn);

			var pclick = function (e) {
				popSectBtn.popup('close');
				
				try {
					e.data.Op.call(dc.cms.ui.Loader, e);
				}
				catch (x) {
					console.log('issue running option: ' + e.data.Title + ', error: ' + x);
				}
				
				e.preventDefault();
				return false;
			};
			
			var addpmenu = function(mnu) {
				if (mnu.Auth && !dc.user.isAuthorized(mnu.Auth))
					return;
				
				var node = $('<a href="#"></a>');
				node.text(mnu.Title);
				node.addClass(mnu.Kind);
				
				node.click(mnu, pclick);
				
				lstSectBtn.append($('<li></li>').append(node));
			};
			
			if (dc.cms.ui.Menu[area]) {
				for (var i = 0; i < dc.cms.ui.Menu[area].Menu.length; i++) 
					addpmenu(dc.cms.ui.Menu[area].Menu[i]);
			}
			
			if (dc.handler.cms && dc.handler.cms.Menu[area]) {
				for (var i = 0; i < dc.handler.cms.Menu[area].Menu.length; i++) 
					addpmenu(dc.handler.cms.Menu[area].Menu[i]);
			}
			
			$('body').append(popSectBtn).promise().then(function() {
				$(popSectBtn).popup().enhanceWithin();
			}); 
			
			return id;
		},
		// start with a fresh new pane
		openPane: function(page, params, area) {
			var layer = dc.cms.ui.Loader.getPane();
			
			layer.clearHistory();
			
			dc.cms.ui.Loader.loadPane(page, params, area);
		},
		loadPane: function(page, params, area, replaceState) {
			var layer = dc.cms.ui.Loader.getPane();
			
			$('#dcuiIntegratedCms').show();
			
			if (!params)
				params = { };
				
			params._Menu = area;
			
			layer.loadPage(page, params, replaceState);
		},
		closePane: function() {
			var pane = dc.cms.ui.Loader.getPane();
			
			if (pane) 
				pane.closePage();
		},
		closeAll: function(dmode) {
			var pane = dc.pui.Loader.getLayer("CMSPane");
			
			if (pane) 
				pane.clearHistory();
			
			$('#dcuiIntegratedCms').hide();

			if (!dmode)
				dc.pui.Loader.currentPageEntry().callPageFunc('dcmCMSEvent', { Name: 'Close', Target: 'CMSPane' });
		},
		destroy: function() {
			dc.cms.ui.Loader.closeAll(true);
			
			$('#dcuiIntegratedCms,#dcuiIntegratedTab').remove();
		}
	},
	// this is not all areas, just the common ones
	MenuEnum: {
		Empty: 'dcmEmpty',		// just close
		Start: 'dcmStart',		// current viewed page
		Gallery: 'dcmGallery',		// gallery browser/picker
		Show: 'dcmShow',		// show organizer
		ImageDetail: 'dcmImageDetail',    // info about an specific image
		Files: 'dcmFiles',		// file browser/picker
		FileDetail: 'dcmFileDetail',    // info about an specific file
		Pages: 'dcmPages',		// page browser/picker
		PageProps: 'dcmPageProps',		// current viewed page
		PagePart: 'dcmPagePart',
		PartPopup: 'dcmPartPopup',
		PagePartSection: 'dcmPagePartSection',
		PagePartGallerySection: 'dcmPagePartGallerySection',
		SectionPopup: 'dcmSectionPopup',
		More: 'dcmMore'
	},
	Menu: {
		dcmEmpty: {
			Menu: [
			],
			Help: {
			}
		},
		dcmStart: {
			Menu: [
				{
					Title: 'Properties',
					Kind: 'Option',
					Op: function(e) {
						dc.cms.ui.Loader.openPane('/dcm/edit/page/Properties');
					}
				},
				{
					Title: 'Page Notes',
					Kind: 'Option',
					Op: function(e) {
						// TODO dc.cms.help.Loader.open('CMS-Editor', dc.cms.ui.MenuEnum.Start);
					}
				},
				{
					Title: 'More Options',
					Kind: 'Option',
					Op: function(e) {
						dc.cms.ui.Loader.loadPane('/dcm/edit/More', null, dc.cms.ui.MenuEnum.More);
					}
				}
			],
			Help: {
			}
		},
		dcmMore: {
			Menu: [
				{
					Title: 'New Page',
					Kind: 'Option',
					Op: function(e) {
						dc.cms.ui.Loader.loadPane('/dcm/edit/page/Add-Page');
					}
				},
				{
					Title: 'Galleries',
					Kind: 'Option',
					Op: function(e) {
						dc.cms.ui.Loader.loadPane('/dcm/edit/galleries/Browser', null, dc.cms.ui.MenuEnum.Gallery);
					}
				},
				{
					Title: 'Files',
					Kind: 'Option',
					Op: function(e) {
						dc.cms.ui.Loader.loadPane('/dcm/edit/files/Browser');
					}
				}
			],
			Help: {
			}
		},
		dcmGallery: {
			Menu: [
				{
					Title: 'Add Image(s)',
					Kind: 'Option',
					Op: function(e) {
						dc.cms.ui.Loader.loadPane('/dcm/edit/galleries/Add-Image');
					}
				},
				{
					Title: 'Quick Upload',
					Kind: 'Option',
					Op: function(e) {
						// TODO dc.cms.ui.Loader.loadPane('/dcm/edit/galleries/Add-Image');
					}
				},
				{
					Title: 'Add Folder',
					Kind: 'Option',
					Op: function(e) {
						dc.cms.ui.Loader.loadPane('/dcm/edit/galleries/Add-Folder');
					}
				},
				{
					Title: 'Delete Folder',
					Kind: 'Option',
					Op: function(e) {
						dc.cms.ui.Loader.loadPane('/dcm/edit/galleries/Delete-Folder');
					}
				},
				{
					Title: 'Variations',
					Kind: 'Option',
					Op: function(e) {
						// TODO dc.cms.ui.Loader.loadPane('/dcm/edit/galleries/Delete-Folder');
					}
				},
				{
					Title: 'Image Lists',
					Kind: 'Option',
					Op: function(e) {
						// TODO dc.cms.ui.Loader.loadPane('/dcm/edit/galleries/Delete-Folder');
					}
				},
				{
					Title: 'Data File',
					Kind: 'Option',
					Op: function(e) {
						// TODO dc.cms.ui.Loader.loadPane('/dcm/edit/galleries/Delete-Folder');
					}
				},
				{
					Title: 'Gallery Notes',
					Kind: 'Option',
					Op: function(e) {
						// TODO dc.cms.help.Loader.open('CMS-Editor', dc.cms.ui.MenuEnum.Gallery);
					}
				}
			],
			Help: {
			}
		},
		dcmImageDetail: {
			Menu: [
				{
					Title: 'Download',
					Kind: 'Option',
					Op: function(e) {
						//dc.cms.ui.Loader.loadPageInPane('/dcm/edit/galleries/Add-Folder');
						
						// TODO look at pane context for current image
						// do a download
					}
				},
				{
					Title: 'Delete Image',
					Kind: 'Option',
					Op: function(e) {
						//dc.cms.ui.Loader.loadPageInPane('/dcm/edit/galleries/Delete-Folder');
						
						// TODO look at pane context for current image
						// confirm delete popup
					}
				}
			],
			Help: {
			}
		},
		dcmShow: {
			Menu: [
				{
					Title: 'Show',
					Kind: 'Option',
					Op: function(e) {
						dc.cms.ui.Loader.openPane('/dcm/edit/show/Edit');
					}
				}
			],
			Help: {
			}
		},
		dcmFiles: {
			Menu: [
				{
					Title: 'Add Files(s)',
					Kind: 'Option',
					Op: function(e) {
						dc.cms.ui.Loader.loadPageInPane('/dcm/edit/files/Add-File');
					}
				},
				{
					Title: 'Add Folder',
					Kind: 'Option',
					Op: function(e) {
						dc.cms.ui.Loader.loadPageInPane('/dcm/edit/files/Add-Folder');
					}
				},
				{
					Title: 'Delete Folder',
					Kind: 'Option',
					Op: function(e) {
						dc.cms.ui.Loader.loadPageInPane('/dcm/edit/files/Delete-Folder');
					}
				}
			],
			Help: {
			}
		},
		dcmFileDetail: {
			Menu: [
				{
					Title: 'Download',
					Kind: 'Option',
					Op: function(e) {
						//dc.cms.ui.Loader.loadPageInPane('/dcm/edit/files/Add-Folder');
						
						// TODO look at pane context for current file
						// download if found
					}
				},
				{
					Title: 'Delete File',
					Kind: 'Option',
					Op: function(e) {
						//dc.cms.ui.Loader.loadPageInPane('/dcm/edit/files/Delete-File');
						
						// TODO look at pane context for current file
						// confirm delete popup
					}
				},
				{
					Title: 'Edit File',
					Kind: 'Option',
					Auth: [ 'Developer' ],
					Op: function(e) {
						// TODO look at pane context for current file, check that it is a
						// format we know and accept
						dc.cms.ui.Loader.loadPageInPane('/dcm/edit/files/Edit-Text-File');
					}
				}
			],
			Help: {
			}
		},
		dcmPages: {
			Menu: [
				{
					Title: 'Add Page',
					Kind: 'Option',
					Op: function(e) {
						dc.cms.ui.Loader.loadPageInPane('/dcm/edit/page/Add-Page');
					}
				},
				{
					Title: 'Add Folder',
					Kind: 'Option',
					Op: function(e) {
						dc.cms.ui.Loader.loadPageInPane('/dcm/edit/page/Add-Folder');
					}
				/*
				},
				{
					Title: 'Delete Folder',
					Kind: 'Option',
					Op: function(e) {
						// TODO recursive, complex task, future
						dc.cms.ui.Loader.loadPageInPane('/dcm/edit/page/Delete-Folder');
					}
				},
				{
					Title: 'Help',
					Kind: 'Option',
					Op: function(e) {
						dc.cms.help.Loader.open('CMS-Editor', dc.cms.ui.MenuEnum.Pages);
					}
				*/
				}
			],
			Help: {
			}
		},
		dcmPageProps: {
			Menu: [
				{
					Title: 'Page Properties',
					Kind: 'Option',
					Op: function(e) {
						// assumes the context is already set
						dc.cms.ui.Loader.openPane('/dcm/edit/page/Edit-Feed-Prop');
					}
				},
				{
					Title: 'Galleries',
					Kind: 'Option',
					Op: function(e) {
						dc.cms.ui.Loader.openPane('/dcm/edit/galleries/Browser');
					}
				},
				{
					Title: 'Files',
					Kind: 'Option',
					Op: function(e) {
						dc.cms.ui.Loader.openPane('/dcm/edit/files/Browser');
					}
				},
				{
					Title: 'Sign Out',
					Kind: 'Option',
					Op: function(e) {
						dc.pui.Loader.signout();
					}
				}
			],
			Help: {
			}
		},
		dcmPagePart: {
			Menu: [
				{
					Title: 'Content',
					Kind: 'Option',
					Auth: [ 'Developer' ],
					Op: function(e) {
						dc.cms.ui.Loader.openPane('/dcm/edit/page/Edit-Part-Content');
					}
				}
			],
			Help: {
			}
		},
		dcmPartPopup: {
			Menu: [
				{
					Title: 'Edit',
					Kind: 'Option',
					Op: function(e) {
					    var params = dc.cms.ui.Loader.getContext().Params;
					    
						dc.cms.ui.Loader.openPane('/dcm/edit/page/Edit-Part-Content');
					}
				},
				{
					Title: 'Insert Section',
					Kind: 'Option',
					Op: function(e) {
					    var params = dc.cms.ui.Loader.getContext().Params;
						
					    params.Action = {
					    	Op: 'InsertBottom'
					    };
					    
					    // create a new section id
					    params.Section = {
				    		Id: 'sectAuto' + dc.util.Crypto.makeSimpleKey()
					    };
						
					    dc.cms.ui.Loader.getContext().Menu = dc.cms.ui.MenuEnum.Empty;
					    dc.cms.ui.Loader.getContext().IsNew = true;
						
						dc.cms.ui.Loader.openPane('/dcm/edit/page/Insert-Section');
					}
				}
			],
			Help: {
			}
		},
		
		dcmPagePartSection: {
			Menu: [
				{
					Title: 'Content',
					Kind: 'Option',
					Op: function(e) {
						dc.cms.ui.Loader.openPane('/dcm/edit/page/Edit-Section-Content');
					}
				},
				{
					Title: 'Properties',
					Kind: 'Option',
					Op: function(e) {
					    var params = dc.cms.ui.Loader.getContext().Params;
					    
						dc.cms.ui.Loader.openPane('/dcm/edit/page/Edit-Section-Prop-' + params.Section.Plugin);
					}
				}
			],
			Help: {
			}
		},
		dcmPagePartGallerySection: {
			Menu: [
				{
					Title: 'List',
					Kind: 'Option',
					Op: function(e) {
						dc.cms.ui.Loader.openPane('/dcm/edit/page/Edit-Section-Gallery');
					}
				},
				{
					Title: 'Template',
					Kind: 'Option',
					Op: function(e) {
						dc.cms.ui.Loader.openPane('/dcm/edit/page/Edit-Section-Template');
					}
				},
				{
					Title: 'Properties',
					Kind: 'Option',
					Op: function(e) {
					    var params = dc.cms.ui.Loader.getContext().Params;
					    
						dc.cms.ui.Loader.openPane('/dcm/edit/page/Edit-Section-Prop-' + params.Section.Plugin);
					}
				}
			],
			Help: {
			}
		},
		dcmSectionPopup: {
			Menu: [
				{
					Title: 'Edit',
					Kind: 'Option',
					Op: function(e) {
					    var params = dc.cms.ui.Loader.getContext().Params;
						
					    params.Action = {
					    	Op: 'Edit'
					    };
					    
					    var $sect = $('#' + params.Section.Id);
					    
					    if ($sect.hasClass('dc-section-gallery')) {
							dc.cms.ui.Loader.getContext().Menu = dc.cms.ui.MenuEnum.PagePartGallerySection;
							
							params.Show = {
								Path: $sect.attr('data-path'),
								Alias: $sect.attr('data-show')
							};
							
							dc.cms.ui.Loader.openPane('/dcm/edit/page/Edit-Section-Gallery');
					    }
					    else {
							dc.cms.ui.Loader.openPane('/dcm/edit/page/Edit-Section-Content');
						}
					}
				},
				{
					Title: 'Delete',
					Kind: 'Option',
					Op: function(e) {
						dc.pui.Popup.confirm('Are you sure you want to remove this section?', function() {
						    var params = dc.cms.ui.Loader.getContext().Params;
		
						    params.Action = {
						    	Op: 'Delete',
						    	TargetSection: params.Section.Id,
						    	Publish: true
						    };
						    
						    delete params.Section;
						    
							dc.comm.sendMessage({ 
								Service: 'dcmCms', 
								Feature: 'Feeds', 
								Op: 'AlterFeedSection', 
								Body: params
							}, function(rmsg) {
								if (rmsg.Result != 0) 
									dc.pui.Popup.alert(rmsg.Message);
								else
									dc.pui.Popup.alert('Removed', function() { dc.pui.Loader.refreshPage(); });
							});
						});
					}
				},
				{
					Title: 'Insert Section Before',
					Kind: 'Option',
					Op: function(e) {
					    var params = dc.cms.ui.Loader.getContext().Params;
		
					    params.Action = {
					    	Op: 'InsertAbove',
					    	TargetSection: params.Section.Id
					    };
					    
					    // create a new section id
					    params.Section.Id = 'sectAuto' + dc.util.Crypto.makeSimpleKey();
						
					    dc.cms.ui.Loader.getContext().Menu = dc.cms.ui.MenuEnum.Empty;
					    dc.cms.ui.Loader.getContext().IsNew = true;
						
						dc.cms.ui.Loader.openPane('/dcm/edit/page/Insert-Section');
					}
				},
				{
					Title: 'Insert Section After',
					Kind: 'Option',
					Op: function(e) {
					    var params = dc.cms.ui.Loader.getContext().Params;
						
					    params.Action = {
					    	Op: 'InsertBelow',
					    	TargetSection: params.Section.Id
					    };
					    
					    // create a new section id
					    params.Section.Id = 'sectAuto' + dc.util.Crypto.makeSimpleKey();
						
					    dc.cms.ui.Loader.getContext().Menu = dc.cms.ui.MenuEnum.Empty;
					    dc.cms.ui.Loader.getContext().IsNew = true;
						
						dc.cms.ui.Loader.openPane('/dcm/edit/page/Insert-Section');
					}
				},
				{
					Title: 'Duplicate',
					Kind: 'Option',
					Op: function(e) {
					    var params = dc.cms.ui.Loader.getContext().Params;
						
					    params.Action = {
					    	Op: 'InsertBelow',
					    	TargetSection: params.Section.Id,
					    	Publish: true
					    };
						
						params.Part.Section = params.Section.Id;
					    
						dc.comm.sendMessage({ 
							Service: 'dcmCms', 
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
								Service: 'dcmCms', 
								Feature: 'Feeds', 
								Op: 'AlterFeedSection', 
								Body: params
							}, function(rmsg) {
								if (rmsg.Result != 0) 
									dc.pui.Popup.alert(rmsg.Message);
								else
									dc.pui.Loader.currentPageEntry().callPageFunc('dcmCMSEvent', { Name: 'Close', Target: 'CMSPane' });
							});
						});
					}
				},
				{
					Title: 'Move Up',
					Kind: 'Option',
					Op: function(e) {
						dc.pui.Popup.confirm('Are you sure you want to move this section up?', function() {
						    var params = dc.cms.ui.Loader.getContext().Params;
		
						    params.Action = {
						    	Op: 'MoveUp',
						    	TargetSection: params.Section.Id,
						    	Publish: true
						    };
						    
						    delete params.Section;
						    
							dc.comm.sendMessage({ 
								Service: 'dcmCms', 
								Feature: 'Feeds', 
								Op: 'AlterFeedSection', 
								Body: params
							}, function(rmsg) {
								if (rmsg.Result != 0) 
									dc.pui.Popup.alert(rmsg.Message);
								else
									dc.pui.Popup.alert('Moved', function() { dc.pui.Loader.refreshPage(); });
							});
						});
					}
				},
				{
					Title: 'Move Down',
					Kind: 'Option',
					Op: function(e) {
						dc.pui.Popup.confirm('Are you sure you want to move this section down?', function() {
						    var params = dc.cms.ui.Loader.getContext().Params;
		
						    params.Action = {
						    	Op: 'MoveDown',
						    	TargetSection: params.Section.Id,
						    	Publish: true
						    };
						    
						    delete params.Section;
						    
							dc.comm.sendMessage({ 
								Service: 'dcmCms', 
								Feature: 'Feeds', 
								Op: 'AlterFeedSection', 
								Body: params
							}, function(rmsg) {
								if (rmsg.Result != 0) 
									dc.pui.Popup.alert(rmsg.Message);
								else
									dc.pui.Popup.alert('Moved', function() { dc.pui.Loader.refreshPage(); });
							});
						});
					}
				}
			],
			Help: {
			}
		}
	}
};

dc.cms.ui.Layer = function(contentsel, options) {
	this.init(contentsel, options);
};  

dc.cms.ui.Layer.prototype = new dc.pui.Layer();

dc.cms.ui.Layer.prototype.manifestPage = function(entry, killpane) {
	var layer = this;
	
	if (!entry)
		return;
	
	// only destroy if current was lost due to a back button 
	// otherwise we should expect we'll want the entry again later
	if (layer.__current && (killpane || entry.ReplaceState)) {
		layer.__current.onDestroy();
		
		try {
			$('#' + layer.__current.__cmsid).remove();
		}
		catch (x) {
			console.log('Unable to remove: ' + x);
		}
		
		// get rid of current spot in history
		if (this.__history.length && (this.__history[this.__history.length - 1] == layer.__current)) {
			this.__history.pop();
			
			if (this.__history.length == 0) 
				$('#dcuiIntegratedMenuBack').addClass('disabled');
		}
	}
	else if (layer.__current) {
		$('#' + layer.__current.__cmsid).hide();
	}
	
	layer.__current = entry;
	
	if (entry.__cmsid) {
		$('#' + entry.__cmsid).show();
		
		if (entry.Loaded && entry.FreezeTop)
			$(layer.__content).animate({ scrollTop: entry.FreezeTop }, "fast");
		else if (entry.TargetElement)
			$(layer.__content).animate({ scrollTop: $('#' + entry.TargetElement).get(0).getBoundingClientRect().top + window.pageYOffset }, "fast");
		else
			$(layer.__content).animate({ scrollTop: 0 }, "fast");
		
		entry.onLoad();
		
		return;
	}
	
	entry.__cmsid = dc.util.Uuid.create();
	
	this.open();
	
	var pane = $('<div></div>');
	pane.attr('id', entry.__cmsid);
	
	$(layer.__content).append(pane).promise().then(function() {
		var page = dc.pui.Loader.__pages[entry.Name];
	
		// layout using 'pageContent' as the top of the chain of parents
		entry.layout(page.Layout, new dc.pui.LayoutEntry({
			Element: $('#' + layer.__current.__cmsid),
			PageEntry: entry
		}));
		
		if (layer.__history.length == 0) 
			$('#dcuiIntegratedMenuBack').addClass('disabled');
		else
			$('#dcuiIntegratedMenuBack').removeClass('disabled');
					
		$('#' + layer.__current.__cmsid).enhanceWithin().promise().then(function() {	
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


dc.cms.ui.Layer.prototype.clearHistory = function() {
	dc.pui.Layer.prototype.clearHistory.call(this);
	
	$(this.__content).empty();
};

dc.cms.ui.Layer.prototype.back = function() {
	var entry = this.__history.pop();
	
	if (this.__history.length == 0) 
		$('#dcuiIntegratedMenuBack').addClass('disabled');
	else
		$('#dcuiIntegratedMenuBack').removeClass('disabled');
	
	if (entry) {
		this.manifestPage(entry, true);
	}
	else {
		this.__current = null;
		this.close();
	}
};

// need to override so that we only enhance current section not the entire layer
dc.cms.ui.Layer.prototype.enhancePage = function() {
	var layer = this;
	
	$('#' + layer.__current.__cmsid + ' *[data-dcui-mode="enhance"] a').each(function() { 
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
					dc.cms.ui.Loader.loadPane(e.data.substr(1));
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

dc.cms.ui.Layer.prototype.close = function() {
	$(this.__content).hide();
	dc.cms.ui.Loader.closeAll();
};


dc.cms.ui.Layer.prototype.scrollPage = function(selector) {
	$("#" + this.__current.__cmsid).animate({ scrollTop: $(selector).get(0).getBoundingClientRect().top + window.pageYOffset }, "fast");
};


/*
dc.cms.ui.Layer.prototype.init = function(contentsel, options) {
	dc.pui.Layer.prototype.init.call(this, contentsel, options);
};
*/

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