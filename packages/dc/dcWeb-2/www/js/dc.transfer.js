if (!dc.transfer)
	dc.transfer = {};

dc.transfer = {
	// from http://stackoverflow.com/questions/10420352/converting-file-size-in-bytes-to-human-readable#answer-22023833
	fmtFileSize: function(bytes) {
		var exp = Math.log(bytes) / Math.log(1024) | 0;
		var result = (bytes / Math.pow(1024, exp)).toFixed(2);

		return result + ' ' + (exp == 0 ? 'bytes': 'KMGTPEZY'[exp - 1] + 'B');
	},
	
	Bucket: function(options) {			
		var defaults = {
			Service: 'dcmBucket',
			Feature: 'Buckets',
			Bucket: 'Default',
			Callback: null,
			ProgressBar: '#fileProgressBar',		// TODO rework into a component
			ProgressLabel: '#fileProgressLabel'
		};
		
		this.settings = $.extend( {}, defaults, options );
		
		this.data = {
			binding: null,
			file: null,
			path: null,
			token: null,
			params: null,
			lastxhr: null,
			canclick: false,
			chunk: 0,
			maxchunksize: 16 * 1024 *1024,		// 16 MB
			ftotal: 0,
			famt: 0,
			aamt: 0,
			finalsent: false
		};
		
		// ask the backend Uploader for a token to upload with
		this.uploadToken = function(params, cb) {
			console.log(new Date() + ': Upload Token');
			
			var tmsg = { 
				Service: this.settings.Service,
				Feature: this.settings.Feature,
				Op: 'AllocateUploadToken',
				Body: {
					Bucket: this.settings.Bucket,
					Params: params
				}
			};
			
			dc.comm.sendMessage(tmsg, function(rmsg) {
				if (cb)
					cb(rmsg);
			});
		};
	
		// ask the backend Uploader for a channel to connect to 
		this.upload = function(blob, path, token, ovrwrt, params) {
			console.log(new Date() + ': Requesting');
			
			this.data.file = blob;
			this.data.path = path;
			this.data.token = token;
			this.data.params = params;
			
			var tmsg = { 
				Service: this.settings.Service,
				Feature: this.settings.Feature,
				Op: 'StartUpload',
				Body: {
					Bucket: this.settings.Bucket,
					Path: this.data.path,
					Token: this.data.token,
					FileSize: this.data.file.size,
					ForceOverwrite: ovrwrt ? true : false,
					Params: this.data.params
				}
			};
			
			var cmsg = { 
				Service: 'Session',
				Feature: 'DataChannel',
				Op: 'Establish',
				Body: {
					Title: "Uploading " + this.data.file.name,
					StreamRequest: tmsg
				}
			};
			
			var buck = this;
			
			dc.comm.sendMessage(cmsg, function(rmsg) {
				//console.log('start: ' +  rmsg.Result);
				
				if (rmsg.Result == 0) { 
					console.log(new Date() + ': Streaming');
					
					// now Gateway and Uploader can talk to each other, so start sending the file
					buck.data.binding = rmsg.Body;
					buck.data.ftotal = buck.data.file.size;
					buck.data.famt = buck.data.binding.Size;
					
					buck.sendNextChunk();
				}
				else {			
					dc.pui.Popup.alert('Error requesting upload channel.');
				}
			});
		};
	
		// each chunk (16MB) starts in a fresh call stack - here
		this.sendNextChunk = function() {
			//var progressBar = $(this.settings.ProgressBar);
			//var progressLabel = $(this.settings.ProgressLabel);
	
			// we are done uploading so do a verify
			if ((this.data.famt == this.data.ftotal) && this.data.finalsent) {
				// don't finish the progress bar until verify is done
				//progressBar.css('width', '98%');
				//progressLabel.text('Finishing...');
				
				this.verifyFile();
				return;
			}
	
			console.log(new Date() + ': Sending chunk: ' + this.data.chunk);
			
			// chunk at 16MB or less each with offset...	
			var chunksize = this.data.ftotal - this.data.famt;
			var lastchunk = false;
			
			if (chunksize > this.data.maxchunksize)
				chunksize = this.data.maxchunksize;
			else 
				this.data.lastchunk = true;
			
			var content = null;	
			
			// we aren't going for support with very old browsers, so start with slice
			// and only fall back to the webkit or moz versions if not supported
			// browsers with the very old type of slice will not work, but likely don't 
			// support upload progress anyway
			if (this.data.file.slice) 
				content = this.data.file.slice(this.data.famt, this.data.famt + chunksize);	
			else if (this.data.file.webkitSlice) 
				content = this.data.file.webkitSlice(this.data.famt, this.data.famt + chunksize);	
			else if (this.data.file.mozSlice) 
				content = this.data.file.mozSlice(this.data.famt, this.data.famt + chunksize);	
				
			//var content = file.slice(famt, famt + chunksize);	
			
			var xhr = lastxhr = new XMLHttpRequest();
			
			var uri = '/upload/' + this.data.binding.ChannelId + (this.data.lastchunk ? '/Final' : '/Block');
			
			xhr.open("POST", uri, true);
			
			var buck = this;
			
			xhr.onreadystatechange = function() {
				console.log('state change: ' + xhr.readyState + ' status: ' + xhr.status);
			
				// do nothing if user clicked cancel
				if (buck.data.canclick)
					return;
			
				if (xhr.readyState == 4) {
					buck.data.lastxhr = null;
	
					// if already uploaded something, and get 400 or 0 then you have been cancelled
					if (((xhr.status == 400) || (xhr.status == 0)) && (buck.data.aamt > 0)) {
						dc.pui.Popup.alert('Processed Cancel Request.');
						return;
					}
					
					if (xhr.status != 200) {
						dc.pui.Popup.alert('Streaming halted with error.');
						return;
					}
					
					buck.data.famt += chunksize;
					buck.data.chunk++;
					
					if (buck.data.lastchunk)
						buck.data.finalsent = true;
					
					// reset call stack by calling next chunk later
					setTimeout(function() { buck.sendNextChunk() }, 1);
				}
			};	
			
			xhr.upload.onprogress = function(e) {
				//console.log('progress: ' +  e.loaded);
				
				if (e.lengthComputable) {
					buck.data.aamt = buck.data.famt + e.loaded;	 
				
					var p1 = (buck.data.aamt * 100 / buck.data.ftotal).toFixed();
					
					if (p1 > 96)
						p1 = 96;
					
					//progressBar.css('width', p1 + '%');
					//progressLabel.text(p1 + '%');
					//progressLabel.text(numeral(this.data.aamt / 1024).format('0,0') + 'kb of ' 
					//	+ numeral(ftotal / 1024).format('0,0') + 'kb'));
					
					//progressLabel.text(dc.transfer.fmtFileSize(buck.data.aamt) + ' of ' + dc.transfer.fmtFileSize(buck.data.ftotal));
				}
			};	
			
			xhr.send(content);	
		};
	
		this.verifyFile = function() {
			console.log(new Date() + ': Verifying');
			
			var tmsg = { 
				Service: this.settings.Service,
				Feature: this.settings.Feature,
				Op: 'FinishUpload',
				Body: {
					Bucket: this.settings.Bucket,
					Path: this.data.path,
					Token: this.data.token,
					Status: 'Success',
					Evidence: {
						Size: this.data.file.size
					},
					Params: this.data.params
				}
			};
			
			var buck = this;
			
			dc.comm.sendMessage(tmsg, function(rmsg) {
				console.log('verify: ' +  rmsg.Result);
				
				if (buck.settings.Callback)
					buck.settings.Callback(buck.data.remotePath);
			});
		};
		
		this.download = function(path, token, params) {
			console.log(new Date() + ': Requesting');
			
			this.data.path = path;
			this.data.token = token;
			this.data.params = params;
				
			dc.util.Cookies.deleteCookie('fileDownload');
			
			var tmsg = { 
				Service: this.settings.Service,
				Feature: this.settings.Feature,
				Op: 'StartDownload',
				Body: {
					Bucket: this.settings.Bucket,
					Path: this.data.path,
					Token: this.data.token,
					Params: this.data.params
				}
			};
			
			var cmsg = { 
				Service: 'Session',
				Feature: 'DataChannel',
				Op: 'Establish',
				Body: {
					Title: "Downloading " + this.data.path,
					StreamRequest: tmsg
				}
			};
			
			var buck = this;

			dc.comm.sendMessage(cmsg, function(rmsg) {
				if (rmsg.Result == 0) { 
					var binding = rmsg.Body;
		
					$.fileDownload('/download/' + binding.ChannelId, {
						httpMethod: 'GET', 
						successCallback: function(url) {
							// only means that it started, not finished
							console.log('download worked!');
							buck.settings.Callback(buck.data.path);
						},
						failCallback: function(html, url) {
							console.log('download failed!');
							buck.settings.Callback(buck.data.path);
						}
					});
				}
				else {			
					dc.pui.Popup.alert('Error requesting download channel.');
				}
			});
		}
	},
	
	Util: {
		uploadFiles: function(files, bucket, token, callback) {
			var steps = [ ];
			
			for (var i = 0; i < files.length; i++) {
				var file = files[i];
				
				if (! file.File) 
					file = {
						File: file
					};
					
				if (! file.Name)
					file.Name = dc.util.File.toCleanFilename(file.File.name);
				
				steps.push({
					Alias: 'UploadFile',
					Title: 'Upload File',
					Params: {
						File: file.File,
						Path: file.Path,
						FileName: file.Name
					},
					Func: function(step) {
						var task = this;

						// TODO support a callback on fail - do task.kill - handle own alerts
						step.Store.Transfer = new dc.transfer.Bucket({
							Bucket: bucket,
							Callback: function(e) {
								console.log('callback done!');
								
								delete step.Store.Transfer;

								task.resume();
							}
						});
						
						var path = '';
						
						if (task.Store.Token)
							path = '/' + task.Store.Token;
						
						if (step.Params.Path)
							path += step.Params.Path;
						
						// start/resume upload (basic token service requires that token be in the path)
						step.Store.Transfer.upload(step.Params.File, 
								path + '/' + step.Params.FileName, 
								task.Store.Token);
					}
				});
			}
			
			var uploadtask = new dc.lang.Task(steps, function(res) {
				callback();
			});
			
			uploadtask.Store = {
				Token: token
			};
			
			return uploadtask;
		}
	}
}
