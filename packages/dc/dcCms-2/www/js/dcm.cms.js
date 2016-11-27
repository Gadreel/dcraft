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
		// TODO lookup options from dc.handler
		
		if (! options)
			options = { 
				AuthTags: [ 'Editor', 'Admin', 'Developer' ],
				Page: '/dcm/cms/feed/Page/Edit-Prop',
				Menu: 'dcmPageProps',
				Params: {
					Channel: 'Pages',
					Path: $('#dcuiMain').attr('data-dccms-path')
				}
			};
	
		var itab = $('<div id="dcuiAppTab"><i class="fa fa-angle-double-right"></i></div>');
		
		itab.click(function (e) {
			dc.pui.App.start(options);
			
			e.preventDefault();
			return false;
		});
		
		$('*[data-dccms-edit]').each(function() { 
			var auth = $(this).attr('data-dccms-edit');
			
			if (auth && dc.user.isAuthorized(auth.split(',')))
				$(this).addClass('dcuiCms');
		});
		
		if (! options.AuthTags || dc.user.isAuthorized(options.AuthTags))
			$('#dcuiMain').append(itab);
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
				dc.pui.App.loadPage('/dcm/cms/feed/Page/Edit-Prop');
			}
		},
		{
			Title: 'Galleries',
			Op: function(e) {
				dc.pui.App.loadPage('/dcm/cms/galleries/Browser');
			}
		},
		{
			Title: 'Files',
			Op: function(e) {
				dc.pui.App.loadPage('/dcm/cms/files/Browser');
			}
		},
		{
			Title: 'My Account',
			Op: function(e) {
				dc.pui.Dialog.loadPage('/dcw/EditSelf');
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
				dc.pui.App.loadPage('/dcm/cms/feed/Edit-Part-Content');
			}
		}
	]
};

dc.pui.Apps.Menus.dcmPartPopup = {
	Options: [
		{
			Title: 'Edit',
			Auth: [ 'Developer' ],
			Op: function(e) {
			    var params = dc.pui.App.Context.Params;
			    
				dc.pui.App.loadPage('/dcm/cms/feed/Edit-Part-Content');
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
				
			    dc.pui.Dialog.loadPage('/dcm/cms/feed/Insert-Section');
			}
		}
	]
};

dc.pui.Apps.Menus.dcmPartBasicPopup = {
	Options: [
		{
			Title: 'Edit',
			Op: function(e) {
			    var params = dc.pui.App.Context.Params;
			    
				dc.pui.App.loadPage('/dcm/cms/feed/Edit-Part-Content');
			}
		}
	]
};

dc.pui.Apps.Menus.dcmPagePartSection = {
	Options: [
		{
			Title: 'Content',
			Op: function(e) {
				dc.pui.App.loadPage('/dcm/cms/feed/Edit-Section-Content');
			}
		},
		{
			Title: 'Properties',
			Op: function(e) {
			    var params = dc.pui.App.Context.Params;
			    
				dc.pui.App.loadPage('/dcm/cms/feed/Edit-Section-Prop-' + params.Section.Plugin);
			}
		}
	]
};

dc.pui.Apps.Menus.dcmPagePartGallerySection = {
	Options: [
		{
			Title: 'List',
			Op: function(e) {
				dc.pui.App.loadPage('/dcm/cms/feed/Edit-Section-Gallery');
			}
		},
		{
			Title: 'Template',
			Op: function(e) {
				dc.pui.App.loadPage('/dcm/cms/feed/Edit-Section-Template');
			}
		},
		{
			Title: 'Properties',
			Op: function(e) {
			    var params = dc.pui.App.Context.Params;
			    
				dc.pui.App.loadPage('/dcm/cms/feed/Edit-Section-Prop-' + params.Section.Plugin);
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
			    
			    // TODO enhance how this works and how it works in Insert Section too
			    if ($sect.hasClass('dc-section-gallery')) {
					dc.pui.App.Context.Menu = 'dcmPagePartGallerySection';
					
					params.Show = {
						Path: $sect.attr('data-path'),
						Alias: $sect.attr('data-show')
					};
					
					dc.pui.App.loadPage('/dcm/cms/feed/Edit-Section-Gallery');
			    }
			    else {
					dc.pui.App.loadPage('/dcm/cms/feed/Edit-Section-Content');
				}
			}
		},
		{
			Title: 'Delete',
			Op: function(e) {
				dc.pui.Popup.confirm('Are you sure you want to remove this section?', function(confirm) {
					if (!confirm)
						return;
					
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
				
			    dc.pui.Dialog.loadPage('/dcm/cms/feed/Insert-Section');
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
				
			    dc.pui.Dialog.loadPage('/dcm/cms/feed/Insert-Section');
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
							dc.pui.Popup.alert('Duplicated', function() { dc.pui.Loader.MainLayer.refreshPage(); });
					});
				});
			}
		},
		{
			Title: 'Move Up',
			Op: function(e) {
				dc.pui.Popup.confirm('Are you sure you want to move this section up?', function(confirm) {
					if (!confirm)
						return;
					
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
				dc.pui.Popup.confirm('Are you sure you want to move this section down?', function(confirm) {
					if (!confirm)
						return;
					
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

dc.cms.image = {
	// TODO static methods of image
		
	Loader: {
		loadGallery: function(galleryPath, callback) {
			dc.comm.sendMessage({ 
				Service: 'dcmBucket',
				Feature: 'Buckets',
				Op: 'Custom',
				Body: { 
					Bucket: 'WebGallery',
					Command: 'LoadMeta',
					Path: galleryPath
				}
			}, function(resp) {
				var gallery = null;
				
				if ((resp.Result > 0) || ! resp.Body || ! resp.Body.Extra)
					gallery = new dc.cms.image.Loader.defaultGallery(galleryPath);
				else 
					gallery = new dc.cms.image.Gallery(galleryPath, resp.Body.Extra);
				
				var thv = gallery.findVariation('thumb');
				
				// make sure there is always a thumb variation
				if (! thv)
					gallery.updateVariation({ 
						"ExactWidth": 152, 
						"ExactHeight": 152, 
						"Alias": "thumb", 
						"Name": "Thumbnail"
					 });
				
				// TODO make sure there is always an automatic upload plan too
				
				callback(gallery, resp);
			});
		},
		defaultGallery: function(galleryPath) {
			return new dc.cms.image.Gallery(galleryPath, { 
				"UploadPlans":  [ 
					 { 
						"Steps":  [ 
							 { 
								"Op": "AutoSizeUpload", 
								"Variations":  [ 
									"original", 
									"thumb"
								 ] 
							 } 
						 ] , 
						"Alias": "default", 
						"Title": "Automatic"
					 } 
				 ] , 
				"Variations":  [ 
					 { 
						"MaxWidth": 2000, 
						"MaxHeight": 2000, 
						"Alias": "original", 
						"Name": "Original"
					 } , 
					 { 
						"ExactWidth": 152, 
						"ExactHeight": 152, 
						"Alias": "thumb", 
						"Name": "Thumbnail"
					 } 
				 ] 
			 });
		}
	},
	Util: {
		formatVariation: function(vari) {
			if (!vari)
				return 'missing';
			
			var desc = 'Width ';
			
			if (vari.ExactWidth)
				desc += vari.ExactWidth;
			else if (vari.MaxWidth && vari.MinWidth)
				desc += 'between ' + vari.MinWidth + ' and ' + vari.MaxWidth;
			else if (vari.MaxWidth)
				desc += 'no more than ' + vari.MaxWidth;
			else if (vari.MinWidth)
				desc += 'at least ' + vari.MinWidth;
			else 
				desc += 'unrestricted';
			
			desc += ' x Height ';
			
			if (vari.ExactHeight)
				desc += vari.ExactHeight;
			else if (vari.MaxHeight && vari.MinHeight)
				desc += 'between ' + vari.MinHeight + ' and ' + vari.MaxHeight;
			else if (vari.MaxHeight)
				desc += 'no more than ' + vari.MaxHeight;
			else if (vari.MinHeight)
				desc += 'at least ' + vari.MinHeight;
			else 
				desc += 'unrestricted';
			
			return desc;
		}
	}
};
	
dc.cms.image.Gallery = function(path, meta) {
	this.Path = path;
	this.Meta = meta;
	this.Transfer = null;   // current transfer bucket
};  

dc.cms.image.Gallery.prototype.topVariation = function(reqname) {
	var vari = this.findVariation(reqname);

	if (vari)
		return vari;
	
	vari = this.findVariation('main');

	if (vari)
		return vari;

	vari = this.findVariation('full');

	if (vari)
		return vari;

	vari = this.findVariation('original');

	if (vari)
		return vari;
	
	if (this.Meta.Variations) 
		return this.Meta.Variations[0];
	
	return null;
};

dc.cms.image.Gallery.prototype.findVariation = function(alias) {
	if (this.Meta.Variations) {
		for (var i = 0; i < this.Meta.Variations.length; i++) {
			var v = this.Meta.Variations[i];
			
			if (v.Alias == alias)
				return v;
		}
	}
	
	return null;
};

// replace or add the variation (unique alias)
dc.cms.image.Gallery.prototype.updateVariation = function(data) {
	if (! this.Meta.Variations) 
		this.Meta.Variations = [ ];
	
	for (var i = 0; i < this.Meta.Variations.length; i++) {
		var v = this.Meta.Variations[i];
		
		if (v.Alias == data.Alias) {
			this.Meta.Variations[i] = data;
			return;
		}
	}
	
	this.Meta.Variations.push(data);
};

dc.cms.image.Gallery.prototype.removeVariation = function(alias) {
	if (! this.Meta.Variations) 
		return;
	
	for (var i1 = 0; i1 < this.Meta.Variations.length; i1++) {
		var v = this.Meta.Variations[i1]; 
	
		if (v.Alias == alias) {
			this.Meta.Variations.splice(i1, 1);
			return;
		}
	}
};

dc.cms.image.Gallery.prototype.findShow = function(alias) {
	if (this.Meta.Shows) {
		for (var i = 0; i < this.Meta.Shows.length; i++) {
			var v = this.Meta.Shows[i];
			
			if (v.Alias == alias)
				return v;
		}
	}
	
	return null;
};

// replace or add the show (unique alias)
dc.cms.image.Gallery.prototype.updateShow = function(data) {
	if (! this.Meta.Shows) 
		this.Meta.Shows = [ ];
	
	for (var i = 0; i < this.Meta.Shows.length; i++) {
		var v = this.Meta.Shows[i];
		
		if (v.Alias == data.Alias) {
			this.Meta.Shows[i] = data;
			return;
		}
	}
	
	this.Meta.Shows.push(data);
};

dc.cms.image.Gallery.prototype.removeShow = function(alias) {
	if (! this.Meta.Shows) 
		return;
	
	for (var i1 = 0; i1 < this.Meta.Shows.length; i1++) {
		var v = this.Meta.Shows[i1]; 
	
		if (v.Alias == alias) {
			this.Meta.Shows.splice(i1, 1);
			return;
		}
	}
};

dc.cms.image.Gallery.prototype.findPlan = function(alias, enablegeneric) {
	if (this.Meta.UploadPlans) {
		for (var i = 0; i < this.Meta.UploadPlans.length; i++) {
			var v = this.Meta.UploadPlans[i];
			
			if (v.Alias == alias)
				return v;
		}
	}
	
	if (enablegeneric && (alias == 'default'))
		return this.addGenericPlan();
	
	return null;
};

dc.cms.image.Gallery.prototype.addGenericPlan = function() {
	if (!this.Meta.UploadPlans) 
		this.Meta.UploadPlans = [ ];
	
	var varis = [ ];
	var plan = { 
		"Steps":  [ 
			 { 
				"Op": "AutoSizeUpload", 
				"Variations": varis
			 } 
		 ] , 
		"Alias": "default", 
		"Title": "Automatic"
	};
		
	this.Meta.UploadPlans.push(plan);

	if (this.Meta.Variations) {
		for (var i = 0; i < this.Meta.Variations.length; i++) {
			var v = this.Meta.Variations[i];
			varis.push(v.Alias);
		}
	}
	
	return plan;
};

dc.cms.image.Gallery.prototype.formatVariation = function(alias) {
	var vari = this.findVariation(alias);
	
	return dc.cms.image.Util.formatVariation(vari);
};

dc.cms.image.Gallery.prototype.imageDetail = function(name, cb) {
	dc.comm.sendMessage({ 
		Service: 'dcmBucket',
		Feature: 'Buckets',
		Op: 'Custom',
		Body: { 
			Bucket: 'WebGallery',
			Command: 'ImageDetail',
			Path: this.Path + '/' + name + '.v'
		}
	}, function(resp) {
		if (cb)
			cb(resp);
	});
};
dc.cms.image.Gallery.prototype.save = function(cb) {
	dc.comm.sendMessage({ 
		Service: 'dcmBucket',
		Feature: 'Buckets',
		Op: 'Custom',
		Body: { 
			Bucket: 'WebGallery',
			Command: 'SaveMeta',
			Path: this.Path,
			Params: this.Meta
		}
	}, function(resp) {
		if (cb)
			cb(resp);
	});
};

dc.cms.image.Gallery.prototype.createProcessUploadTask = function(blobs, plan, token, bucket) {
	var or = new dc.lang.OperationResult();
	
	var steps = [ ];
	
	for (var i = 0; i < blobs.length; i++) {
		var blob = blobs[i];
		
		var bname = blob.Name ? blob.Name : null;
		
		if (! bname) {
			bname = 'unknown';

			if (blob.name) {
				bname = dc.util.File.toCleanFilename(blob.name);
				
				// remove the extension
				var bpos = bname.lastIndexOf('.');
				
				if (bpos)
					bname = bname.substr(0, bpos);
			}
		}
		
		steps.push({
			Alias: 'ProcessImage',
			Title: 'Process Image',
			Params: {
				Blob: blob,
				Name: bname
			},
			Func: function(step) {
				var task = this;

				var pres = task.Store.Gallery.createPlanTask(step.Params.Blob, step.Params.Name);
				
				if (pres.hasErrors()) {
					task.error('Unable to create plan');
					task.resume();
					return;
				}
				
				pres.Result.Observers.push(function(ctask) {
					if (ctask.Result) {
						task.Result.push({
							Name: step.Params.Name,
							Variants: ctask.Result 
						});
					}
					
					task.resume();
				});

				pres.Result.run();
			}
		});
	}
	
	steps.push({
		Alias: 'UploadImages',
		Title: 'Upload Images',
		Params: {
		},
		Func: function(step) {
			var task = this;

			var pres = task.Store.Gallery.createUploadTask(task.Result);
			
			if (pres.hasErrors()) {
				task.error('Unable to upload task');
				task.resume();
				return;
			}
		
			pres.Result.Observers.push(function(ctask) {
				task.resume();
			});

			pres.Result.run();
		}
	});
	
	var processtask = new dc.lang.Task(steps);
	
	processtask.Store = {
		Plan: plan,
		Bucket: bucket,
		Gallery: this,
		Token: token
	};
	
	processtask.Result = [ ];
	
	or.Result = processtask;
	
	return or;
};

dc.cms.image.Gallery.prototype.createUploadTask = function(files, token, bucket) {
	var or = new dc.lang.OperationResult();
	
	var steps = [ ];
	
	for (var i = 0; i < files.length; i++) {
		var file = files[i];
		
		for (var n = 0; n < file.Variants.length; n++) {
			var vari = file.Variants[n];
			
			if ((n == 0) && ! file.Name && file.Blob && file.Blob.name)
				file.Name = dc.util.File.toCleanFilename(file.Blob.name);
			
			steps.push({
				Alias: 'UploadImage',
				Title: 'Upload Image',
				Params: {
					File: file,
					Variant: vari
				},
				Func: function(step) {
					var task = this;
	
					// TODO support a callback on fail - do task.kill - handle own alerts
					step.Store.Transfer = new dc.transfer.Bucket({
						Bucket: task.Store.Bucket,
						Callback: function(e) {
							console.log('callback done!');
							
							delete step.Store.Transfer;
	
							task.resume();
						}
					});
					
					var path = task.Store.Gallery.Path + '/' + step.Params.File.Name 
						+ '.v/' + step.Params.Variant.FileName;
					
					step.Store.Transfer.upload(step.Params.Variant.Blob, path, task.Store.Token);
				}
			});
		}
	}
	
	var uploadtask = new dc.lang.Task(steps);
	
	uploadtask.Store = {
		Bucket: bucket ? bucket : 'WebGallery',
		Gallery: this,
		Token: token,
		Blob: null
	};
	
	or.Result = uploadtask;
	
	return or;
};

dc.cms.image.Gallery.prototype.createPlanTask = function(blob, name, plan) {
	var or = new dc.lang.OperationResult();
	
	if (!plan)
		plan = 'default';
		
	var planrec = this.findPlan(plan, true);

	if (!planrec) {
		or.error('Missing upload plan.');
		return or;
	}

	var steps = [ ];
	
	for (var i = 0; i < planrec.Steps.length; i++) {
		var sinfo = planrec.Steps[i];
	
		if (sinfo.Op == "AutoSizeUpload") {
			steps.push({
				Alias: 'Resize',
				Title: 'Resize and Scale Image',
				Params: {
					StepInfo: sinfo
				},
				Func: function(step) {
					var task = this;		
					
					var vlist = step.Params.StepInfo.Variations;
					var varis = [];
					
					for (var n = 0; n < vlist.length; n++) 
						varis.push(task.Store.Gallery.findVariation(vlist[n]));
					
					var ctaskres = dc.image.Tasks.createVariationsTask(task.Store.Blob, varis, 
						task.Store.Gallery.Meta.Extension, task.Store.Gallery.Meta.Quality);
					
					if (ctaskres.hasErrors()) {
						task.error('Unable to create variations.');
						task.resume();
						return;
					}
					
					ctaskres.Result.Observers.push(function(ctask) {
						task.Result = ctask.Result;
						task.resume();
					});
		
					ctaskres.Result.run();
				}
			});
		}
		// TODO add other operations
		else {
			or.error('Unknown step: ' + step.Op);
			return or;
		}
	}
	
	var plantask = new dc.lang.Task(steps);
	
	plantask.Store = {
		Blob: blob,
		Name: name,
		Plan: planrec,
		Gallery: this
	};
	
	plantask.Result = [ ];
	
	or.Result = plantask;
	
	return or;
};
