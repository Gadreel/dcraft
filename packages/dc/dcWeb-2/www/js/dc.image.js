
// see also dc.util.Image.load(url, callback, progcallback)

if (! dc.image)
	dc.image = { };

dc.image.Tasks = {
	createVariationsTask: function(blob, variants, format, quality) {
		var or = new dc.lang.OperationResult();
		
		var steps = [ ];
		
		steps.push({
			Alias: 'MetaData',
			Title: 'Collect Image MetaData',
			Params: {
			},
			Func: function(step) {
				var task = this;

				// blueimp function
				loadImage.parseMetaData(blob, function (mdata) {
					task.Store.MetaData = mdata;	// ok if this fails, though ideally would not
						
					task.resume();
				});					
			}
		});		
		
		for (var n = 0; n < variants.length; n++) {
			steps.push({
				Alias: 'Resize',
				Title: 'Resize and Scale Image',
				Params: {
					Sizing: variants[n]
				},
				Func: function(step) {
					var task = this;
		
					var options = {
						//maxWidth: 152,
						//maxHeight: 152,
						canvas: true,
						downsamplingRatio: 0.5
					};
					
					if (task.Store.MetaData && task.Store.MetaData.exif) 
						options.orientation = task.Store.MetaData.exif.get('Orientation');
					
					var sizing = step.Params.Sizing;
					
					if (sizing.ExactWidth) {
						options.maxWidth = sizing.ExactWidth;
						options.minWidth = sizing.ExactWidth;
						options.crop = true;
					}
					
					if (sizing.ExactHeight) {
						options.maxHeight = sizing.ExactHeight;
						options.minHeight = sizing.ExactHeight;
						options.crop = true;
					}
					
					if (sizing.MaxWidth) {
						options.maxWidth = sizing.MaxWidth;
					}
					
					if (sizing.MinWidth) {
						options.minWidth = sizing.MinWidth;
					}
					
					if (sizing.MaxHeight) {
						options.maxHeight = sizing.MaxHeight;
					}
					
					if (sizing.MinHeight) {
						options.minHeight = sizing.MinHeight;
					}
					
					// blueimp feature (modified)
					loadImage(
						blob,
						function (can) {
							if(can.type === "error") 
								task.error('Unable to load as image.');
							else 
								task.Store.Canvas = can;
							
							task.resume();
						},
						options
					);					
				}
			});
			
			steps.push({
				Alias: 'Filter',
				Title: 'Apply Image Filter',
				Params: {
					Sizing: variants[n]
				},
				Func: function(step) {
					var task = this;
					
					var sizing = step.Params.Sizing;
					
					var ctx = task.Store.Canvas.getContext('2d');
					
					// OpacityFilter
					
					if (sizing.OpacityFilter) {
						ctx.fillStyle = 'rgba(225, 225, 225,' + sizing.OpacityFilter + ')';
						ctx.fillRect(0, 0, task.Store.Canvas.width, task.Store.Canvas.height);
					}
					
					task.resume();
				}
			});	
			
			steps.push({
				Alias: 'Format',
				Title: 'Convert Image Format',
				Params: {
					Sizing: variants[n]
				},
				Func: function(step) {
					var task = this;
					
					var sizing = step.Params.Sizing;
					
					var fmt = sizing.Format ? sizing.Format : format ? format : "image/jpeg";
					var qual = sizing.Quality ? sizing.Quality : quality ? quality : 0.6;
				
					var fmt2 = 'jpg';
					
					if (fmt == 'image/gif')
						fmt2 = 'gif';
					else if (fmt == 'image/png')
						fmt2 = 'png';
					
					task.Store.Canvas.toBlob(function(blob) {
						if (!blob) 
							task.error('Image conversion failed.');
				
						task.Result.push({
							Alias: sizing.Alias, 
							FileName: sizing.Alias + '.' + fmt2, 
							Blob: blob
						});
						
						task.Store.Canvas = null;
						
						task.resume();
					}, fmt, qual);
				}
			});	
		}
		
		var createtask = new dc.lang.Task(steps);
		
		createtask.Store = {
			Canvas: null,
			MetaData: null
		};
		
		createtask.Result = [ ];
		
		or.Result = createtask;
		
		return or;
	}
};



/*
 * eTimeline - modification of Blue Imp's load-image.js
 * 
 * may wish to include /js/vendor/blueimp/* files
 * 
 * ----------------------------------------------------------
 * 
 * JavaScript Load Image
 * https://github.com/blueimp/JavaScript-Load-Image
 *
 * Copyright 2011, Sebastian Tschan
 * https://blueimp.net
 *
 * Licensed under the MIT license:
 * http://www.opensource.org/licenses/MIT
 */

/*global define, module, window, document, URL, webkitURL, FileReader */

;(function ($) {
  'use strict'

  // Loads an image for a given File object.
  // Invokes the callback with an img or optional canvas
  // element (if supported by the browser) as parameter:
  var loadImage = function (file, callback, options) {
    var img = document.createElement('img')
    var url
    var oUrl
    img.onerror = callback
    img.onload = function () {
      if (oUrl && !(options && options.noRevoke)) {
        loadImage.revokeObjectURL(oUrl)
      }
      if (callback) {
        callback(loadImage.scale(img, options))
      }
    }
    if (loadImage.isInstanceOf('Blob', file) ||
      // Files are also Blob instances, but some browsers
      // (Firefox 3.6) support the File API but not Blobs:
      loadImage.isInstanceOf('File', file)) {
      url = oUrl = loadImage.createObjectURL(file)
      // Store the file type for resize processing:
      img._type = file.type
    } else if (typeof file === 'string') {
      url = file
      if (options && options.crossOrigin) {
        img.crossOrigin = options.crossOrigin
      }
    } else {
      return false
    }
    if (url) {
      img.src = url
      return img
    }
    return loadImage.readFile(file, function (e) {
      var target = e.target
      if (target && target.result) {
        img.src = target.result
      } else {
        if (callback) {
          callback(e)
        }
      }
    })
  }
  // The check for URL.revokeObjectURL fixes an issue with Opera 12,
  // which provides URL.createObjectURL but doesn't properly implement it:
  var urlAPI = (window.createObjectURL && window) ||
                (window.URL && URL.revokeObjectURL && URL) ||
                (window.webkitURL && webkitURL)

  loadImage.isInstanceOf = function (type, obj) {
    // Cross-frame instanceof check
    return Object.prototype.toString.call(obj) === '[object ' + type + ']'
  }

  // Transform image coordinates, allows to override e.g.
  // the canvas orientation based on the orientation option,
  // gets canvas, options passed as arguments:
  loadImage.transformCoordinates = function () {
    return
  }

  // Returns transformed options, allows to override e.g.
  // maxWidth, maxHeight and crop options based on the aspectRatio.
  // gets img, options passed as arguments:
  loadImage.getTransformedOptions = function (img, options) {
    var aspectRatio = options.aspectRatio
    var newOptions
    var i
    var width
    var height
    if (!aspectRatio) {
      return options
    }
    newOptions = {}
    for (i in options) {
      if (options.hasOwnProperty(i)) {
        newOptions[i] = options[i]
      }
    }
    newOptions.crop = true
    width = img.naturalWidth || img.width
    height = img.naturalHeight || img.height
    if (width / height > aspectRatio) {
      newOptions.maxWidth = height * aspectRatio
      newOptions.maxHeight = height
    } else {
      newOptions.maxWidth = width
      newOptions.maxHeight = width / aspectRatio
    }
    return newOptions
  }

  // Canvas render method, allows to implement a different rendering algorithm:
  loadImage.renderImageToCanvas = function (
    canvas,
    img,
    sourceX,
    sourceY,
    sourceWidth,
    sourceHeight,
    destX,
    destY,
    destWidth,
    destHeight
  ) {
  	// %%% APW
  	//console.log('render: ' + canvas.width + ',' + canvas.height + ' - '
  	//	+ img.width + ',' + img.height + ' - ' 
  	//	+ sourceX + ',' + sourceY + ',' + sourceWidth + ',' + sourceHeight + ' - '
	//	+ destX + ',' + destY + ',' + destWidth + ',' + destHeight);
  
	var ctx = canvas.getContext('2d');
	
    // %%% APW fill back color
  	ctx.fillStyle = 'white';

	//draw background / rect on entire canvas
	ctx.fillRect(destX, destY, destWidth, destHeight);
			
	ctx.drawImage(
		img, sourceX, sourceY, sourceWidth, sourceHeight,
		destX, destY, destWidth, destHeight
	);
	
	return canvas;
  }

  // This method is used to determine if the target image
  // should be a canvas element:
  loadImage.hasCanvasOption = function (options) {
    return options.canvas || options.crop || !!options.aspectRatio
  }

  // Scales and/or crops the given image (img or canvas HTML element)
  // using the given options.
  // Returns a canvas object if the browser supports canvas
  // and the hasCanvasOption method returns true or a canvas
  // object is passed as image, else the scaled image:
  loadImage.scale = function (img, options) {
  	//console.log('scale: ' + img.width + ',' + img.height + ' - ');  // %%% APW
  
    options = options || {}
    var canvas = document.createElement('canvas')
    var useCanvas = img.getContext ||
                    (loadImage.hasCanvasOption(options) && canvas.getContext)
    var width = img.naturalWidth || img.width
    var height = img.naturalHeight || img.height
    var destWidth = width
    var destHeight = height
    var maxWidth
    var maxHeight
    var minWidth
    var minHeight
    var sourceWidth
    var sourceHeight
    var sourceX
    var sourceY
    var pixelRatio
    var downsamplingRatio
    var tmp
    function scaleUp () {
      var scale = Math.max(
        (minWidth || destWidth) / destWidth,
        (minHeight || destHeight) / destHeight
      )
      if (scale > 1) {
        destWidth *= scale
        destHeight *= scale
      }
    }
    function scaleDown () {
      var scale = Math.min(
        (maxWidth || destWidth) / destWidth,
        (maxHeight || destHeight) / destHeight
      )
      if (scale < 1) {
        destWidth *= scale
        destHeight *= scale
      }
    }
    if (useCanvas) {
      options = loadImage.getTransformedOptions(img, options)
      sourceX = options.left || 0
      sourceY = options.top || 0
      if (options.sourceWidth) {
        sourceWidth = options.sourceWidth
        if (options.right !== undefined && options.left === undefined) {
          sourceX = width - sourceWidth - options.right
        }
      } else {
        sourceWidth = width - sourceX - (options.right || 0)
      }
      if (options.sourceHeight) {
        sourceHeight = options.sourceHeight
        if (options.bottom !== undefined && options.top === undefined) {
          sourceY = height - sourceHeight - options.bottom
        }
      } else {
        sourceHeight = height - sourceY - (options.bottom || 0)
      }
      destWidth = sourceWidth;
      destHeight = sourceHeight;
    }
    maxWidth = options.maxWidth;
    maxHeight = options.maxHeight;
    minWidth = options.minWidth;
    minHeight = options.minHeight;
    if (useCanvas && maxWidth && maxHeight && options.crop) {
      destWidth = maxWidth;
      destHeight = maxHeight;
      tmp = sourceWidth / sourceHeight - maxWidth / maxHeight;
      if (tmp < 0) {
        sourceHeight = maxHeight * sourceWidth / maxWidth;
        if (options.top === undefined && options.bottom === undefined) {
          sourceY = (height - sourceHeight) / 2;
        }
      } else if (tmp > 0) {
        sourceWidth = maxWidth * sourceHeight / maxHeight;
        if (options.left === undefined && options.right === undefined) {
          sourceX = (width - sourceWidth) / 2;
        }
      }
    } else {
      if (options.contain || options.cover) {
        minWidth = maxWidth = maxWidth || minWidth;
        minHeight = maxHeight = maxHeight || minHeight;
      }
      if (options.cover) {
        scaleDown();
        scaleUp();
      } else {
        scaleUp();
        scaleDown();
      }
    }
    if (useCanvas) {
      pixelRatio = options.pixelRatio;
      if (pixelRatio > 1) {
        canvas.style.width = destWidth + 'px';
        canvas.style.height = destHeight + 'px';
        destWidth *= pixelRatio;
        destHeight *= pixelRatio;
        canvas.getContext('2d').scale(pixelRatio, pixelRatio);
      }
      downsamplingRatio = options.downsamplingRatio;
      if (downsamplingRatio > 0 && downsamplingRatio < 1 &&
            destWidth < sourceWidth && destHeight < sourceHeight) {
        while (sourceWidth * downsamplingRatio > destWidth) {
          //console.log('downsize');  // %%% APW
          
          canvas.width = sourceWidth * downsamplingRatio;
          canvas.height = sourceHeight * downsamplingRatio;
          loadImage.renderImageToCanvas(
            canvas,
            img,
            sourceX,
            sourceY,
            sourceWidth,
            sourceHeight,
            0,
            0,
            canvas.width,
            canvas.height
          );
          
          //console.log('copy');  // %%% APW
          
          sourceWidth = canvas.width;
          sourceHeight = canvas.height;
          sourceX = 0;		// %%% May 4 APW
          sourceY = 0;		// %%% May 4 APW
          img = document.createElement('canvas');
          img.width = sourceWidth;
          img.height = sourceHeight;
          loadImage.renderImageToCanvas(
            img,
            canvas,
            0,
            0,
            sourceWidth,
            sourceHeight,
            0,
            0,
            sourceWidth,
            sourceHeight
          );
        }
      }
      canvas.width = destWidth;
      canvas.height = destHeight;
      loadImage.transformCoordinates(
        canvas,
        options
      );
      return loadImage.renderImageToCanvas(
        canvas,
        img,
        sourceX,
        sourceY,
        sourceWidth,
        sourceHeight,
        0,
        0,
        destWidth,
        destHeight
      );
    }
    img.width = destWidth;
    img.height = destHeight;
    return img;
  }

  loadImage.createObjectURL = function (file) {
    return urlAPI ? urlAPI.createObjectURL(file) : false
  }

  loadImage.revokeObjectURL = function (url) {
    return urlAPI ? urlAPI.revokeObjectURL(url) : false
  }

  // Loads a given File object via FileReader interface,
  // invokes the callback with the event object (load or error).
  // The result can be read via event.target.result:
  loadImage.readFile = function (file, callback, method) {
    if (window.FileReader) {
      var fileReader = new FileReader()
      fileReader.onload = fileReader.onerror = callback
      method = method || 'readAsDataURL'
      if (fileReader[method]) {
        fileReader[method](file)
        return fileReader
      }
    }
    return false
  }

  if (typeof define === 'function' && define.amd) {
    define(function () {
      return loadImage
    })
  } else if (typeof module === 'object' && module.exports) {
    module.exports = loadImage
  } else {
    $.loadImage = loadImage
  }
}(window))
