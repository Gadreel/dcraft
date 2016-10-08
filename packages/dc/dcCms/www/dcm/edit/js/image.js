// dc.pui.Loader.addExtraLibs( ['/dcm/edit/js/image.js'] );
// var gall = null;
// dc.cms.image.Loader.loadGallery('/Idea-Book/Commercial-Interiors', function(g) { gall = g; console.log('g: ' + g); }); 

if (!dc.cms)
	dc.cms = {};

dc.cms.image = {
	// TODO static methods of image
	//__context: null,
	
	Loader: {
		/*
		getContext: function() {
			return dc.cms.image.__context;
		},
		*/
		loadGallery: function(galleryPath, callback) {
			dc.comm.sendMessage({ 
				Service: 'dcmCms',
				Feature: 'WebGallery',
				Op: 'LoadGallery',
				Body: {
					FolderPath: galleryPath
				}
			}, function(resp) {
				if (resp.Result > 0) 
					callback(null, resp);
				else 
					callback(new dc.cms.image.Gallery(galleryPath, resp.Body.Settings));
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
						"MaxWidth": 128, 
						"MaxHeight": 128, 
						"Alias": "thumb", 
						"Name": "Thumbnail"
					 } 
				 ] 
			 });
		}
	}
};

dc.cms.image.Gallery = function(path, meta) {
	this.__path = path;
	this.__meta = meta;
	this.__tranfer = null;   // current transfer bucket
};  

dc.cms.image.Gallery.prototype.topVariation = function() {
	if (this.__meta.Variations && this.__meta.Variations.length) 
		return this.__meta.Variations[0];
	
	return null;
};

dc.cms.image.Gallery.prototype.findVariation = function(alias) {
	if (this.__meta.Variations) {
		for (var i = 0; i < this.__meta.Variations.length; i++) {
			var v = this.__meta.Variations[i];
			
			if (v.Alias == alias)
				return v;
		}
	}
	
	return null;
};

dc.cms.image.Gallery.prototype.findShow = function(alias) {
	if (this.__meta.Shows) {
		for (var i = 0; i < this.__meta.Shows.length; i++) {
			var v = this.__meta.Shows[i];
			
			if (v.Alias == alias)
				return v;
		}
	}
	
	return null;
};

dc.cms.image.Gallery.prototype.findPlan = function(alias, enablegeneric) {
	if (this.__meta.UploadPlans) {
		for (var i = 0; i < this.__meta.UploadPlans.length; i++) {
			var v = this.__meta.UploadPlans[i];
			
			if (v.Alias == alias)
				return v;
		}
	}
	
	if (enablegeneric && (alias == 'default'))
		return this.addGenericPlan();
	
	return null;
};

dc.cms.image.Gallery.prototype.addGenericPlan = function() {
	if (!this.__meta.UploadPlans) 
		this.__meta.UploadPlans = [ ];
	
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
		
	this.__meta.UploadPlans.push(plan);

	if (this.__meta.Variations) {
		for (var i = 0; i < this.__meta.Variations.length; i++) {
			var v = this.__meta.Variations[i];
			varis.push(v.Alias);
		}
	}
	
	return plan;
};

dc.cms.image.Gallery.prototype.formatVariation = function(alias) {
	var vari = this.findVariation(alias);
	
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
};

dc.cms.image.Gallery.prototype.processBlob = function(blob, name, bucketopts, selectedvari, callback, plan) {
	var gallery = this;
	var or = new dc.lang.OperationResult();
	or.Result = [ ];
	
	if (!plan)
		plan = 'default';
		
	var planrec = gallery.findPlan(plan, true);

	if (!planrec) {
		or.error('Missing upload plan.');
		callback(or);
		return;
	}
	
	if (gallery.__tranfer != null) {
		or.error('Upload in progress, please wait.');
		callback(or);
		return;
	}
					
	loadImage.parseMetaData(blob, function (mdata) {
		// build a queue of record names (copy array) to load 
		var steps = planrec.Steps.concat(); 
			
		var doSteps = function() {		
			// all done with loading
			if (steps.length == 0) {
				delete gallery.__tranfer;
				callback(or);			
				return;
			}
				
			var step = steps.shift();
			
			if (step.Op == "AutoSizeUpload") {
				var varis = step.Variations.concat();
				
				if (!selectedvari && (step.Variations.length > 0))
					selectedvari = step.Variations[0];
				
				var doVari = function() {
					// all done with loading
					if (varis.length == 0) {
						doSteps();
						return;
					}
						
					var vari = varis.shift();
					var varirec =  gallery.findVariation(vari);
				
					var options = {
						maxWidth: 128,
						maxHeight: 128,
						canvas: true,
						downsamplingRatio: 0.5
					};
						
					if (mdata.exif) 
						options.orientation = mdata.exif.get('Orientation');
					
					if (varirec.ExactWidth) {
						options.maxWidth = varirec.ExactWidth;
						options.minWidth = varirec.ExactWidth;
						options.crop = true;
					}
					
					if (varirec.ExactHeight) {
						options.maxHeight = varirec.ExactHeight;
						options.minHeight = varirec.ExactHeight;
						options.crop = true;
					}
					
					if (varirec.MaxWidth) {
						options.maxWidth = varirec.MaxWidth;
					}
					
					if (varirec.MinWidth) {
						options.minWidth = varirec.MinWidth;
					}
					
					if (varirec.MaxHeight) {
						options.maxHeight = varirec.MaxHeight;
					}
					
					if (varirec.MinHeight) {
						options.minHeight = varirec.MinHeight;
					}
								
					loadImage(
						blob,
						function (can) {
							if(can.type === "error") {
								or.error('Unable to load as image.');
								callback(or);
							} 
							else {
								var fmt = varirec.Format ? varirec.Format : "image/jpeg";
								var qual = varirec.Quality ? varirec.Quality : 0.6;
							
								var fmt2 = 'jpg';
								
								if (fmt == 'image/gif')
									fmt2 = 'gif';
								else if (fmt == 'image/png')
									fmt2 = 'png';
								
								can.toBlob(function(blob) {
									if (!blob) {
										or.error('Image conversion failed.');
										callback(or);
										return;
									}
				
									var fullpath = gallery.__path + '/' + name + '.v/' + varirec.Alias + '.' + fmt2;
										
									bucketopts.Callback = function() {
										if (selectedvari == varirec.Alias) {
											or.Result.push({
													FileName: name,
													FullPath: '/galleries' + fullpath
												});
										}
										
										doVari();
									};
									
									gallery.__tranfer = new dcm.transfer.Bucket(bucketopts);
					
									// start/resume upload
									gallery.__tranfer.upload(blob, fullpath, bucketopts.Token, true);
								}, fmt, qual);
							}
						},
						options
					);		
				}
				
				doVari();
			}
			else {
				or.error('Unknown step: ' + step.Op);
				callback(or);
			}
		}
		
		doSteps();
	});		
};
