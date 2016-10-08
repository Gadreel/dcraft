// TODO the basics of upload and download are common to dc, not just dcm - perhaps make a general purpose
// library in dcWeb?
var dcm = {
	upload: {			// make this more OO design, one instance of upload tracker/helper can be created and used
		cid: null,
		binding: null,
		file: null,
		service: null,
		feature: null,
		remotePath: null,
		lastxhr: null,
		canclick: false,
		chunk: 0,
		maxchunksize: 16 * 1024 *1024,		// 16 MB
		ftotal: 0,
		famt: 0,
		aamt: 0,
		finalsent: false,
		lastchunk: false,
		
		init: function(service, feature) {
			dcm.upload.cid = null;
			dcm.upload.binding = null;
			dcm.upload.file = null;
			dcm.upload.service = service ? service : 'dcmCms';
			dcm.upload.feature = feature ? feature : null;
			dcm.upload.callback = null;
			dcm.upload.remotePath = null;
			dcm.upload.lastxhr = null;
			dcm.upload.canclick = false;
			dcm.upload.chunk = 0;
			dcm.upload.ftotal = 0;
			dcm.upload.famt = 0;
			dcm.upload.aamt = 0;
			dcm.upload.finalsent = false;
			dcm.upload.lastchunk = false;
		},

		// ask the backend Uploader for a channel to connect to (must present valid auth token to do this)
		requestUpload: function(path, feature, fullpath, callback, ovrwrt) {
			console.log(new Date() + ': Requesting');
			
			if (fullpath)
				dcm.upload.remotePath = fullpath;
			else
				dcm.upload.remotePath = path + '/' + dcm.upload.file.name;
			
			if (feature)			
				dcm.upload.feature = feature;
			
			dcm.upload.callback = callback;
			
			var tmsg = { 
				Service: dcm.upload.service ? dcm.upload.service : 'dcmCms',
				Feature: dcm.upload.feature,
				Op: 'StartUpload',
				Body: {
					FilePath: dcm.upload.remotePath,
					FileSize: dcm.upload.file.size,
					ForceOverwrite: ovrwrt ? true : false
				}
			};
			
			var cmsg = { 
				Service: 'Session',
				Feature: 'DataChannel',
				Op: 'Establish',
				Body: {
					Title: "Uploading " + dcm.upload.file.name,
					StreamRequest: tmsg
				}
			};
			
			dc.comm.sendMessage(cmsg, function(rmsg) {
				if (rmsg.Result == 0) { 
					dcm.upload.binding = rmsg.Body;
					dcm.upload.sendStream();
				}
				else {			
					dc.pui.Popup.alert('Error requesting upload channel.');
				}
			});
		},

		// now Gateway and Uploader can talk to each other, so start sending the file
		sendStream: function() {
			console.log(new Date() + ': Streaming');

			dcm.upload.ftotal = dcm.upload.file.size;
			dcm.upload.famt = dcm.upload.binding.Size;
			dcm.upload.cid = dcm.upload.binding.ChannelId;
			
			dcm.upload.sendNextChunk();
		},

		// each chunk (16MB) starts in a fresh call stack - here
		sendNextChunk: function() {
			var progressBar = $('#fileProgressBar');
			var progressLabel = $('#fileProgressLabel');

			// we are done uploading so do a verify
			if ((dcm.upload.famt == dcm.upload.ftotal) && dcm.upload.finalsent) {
				// don't finish the progress bar until verify is done
				progressBar.css('width', '98%');
				progressLabel.text('Finishing...');
				
				dcm.upload.verifyFile();
				return;
			}

			console.log(new Date() + ': Sending chunk: ' + dcm.upload.chunk);
			
			// chunk at 16MB or less each with offset...	
			var chunksize = dcm.upload.ftotal - dcm.upload.famt;
			var lastchunk = false;
			
			if (chunksize > dcm.upload.maxchunksize)
				chunksize = dcm.upload.maxchunksize;
			else 
				dcm.upload.lastchunk = true;
			
			var content = null;	
			
			// we aren't going for support with very old browsers, so start with slice
			// and only fall back to the webkit or moz versions if not supported
			// browsers with the very old type of slice will not work, but likely don't 
			// support upload progress anyway
			if (dcm.upload.file.slice) 
				content = dcm.upload.file.slice(dcm.upload.famt, dcm.upload.famt + chunksize);	
			else if (dcm.upload.file.webkitSlice) 
				content = dcm.upload.file.webkitSlice(dcm.upload.famt, dcm.upload.famt + chunksize);	
			else if (dcm.upload.file.mozSlice) 
				content = dcm.upload.file.mozSlice(dcm.upload.famt, dcm.upload.famt + chunksize);	
				
			//var content = file.slice(famt, famt + chunksize);	
			
			var xhr = lastxhr = new XMLHttpRequest();
			
			var uri = '/upload/' + dcm.upload.cid + (dcm.upload.lastchunk ? '/Final' : '/Block');
			
			xhr.open("POST", uri, true);
			
			xhr.onreadystatechange = function() {
				console.log('state change: ' + xhr.readyState + ' status: ' + xhr.status);
			
				// do nothing if user clicked cancel
				if (dcm.upload.canclick)
					return;
			
				if (xhr.readyState == 4) {
					dcm.upload.lastxhr = null;

					// if already uploaded something, and get 400 or 0 then you have been cancelled
					if (((xhr.status == 400) || (xhr.status == 0)) && (dcm.upload.aamt > 0)) {
						dc.pui.Popup.alert('Processed Cancel Request.');
						return;
					}
					
					if (xhr.status != 200) {
						dc.pui.Popup.alert('Streaming halted with error.');
						return;
					}
					
					dcm.upload.famt += chunksize;
					dcm.upload.chunk++;
					
					if (dcm.upload.lastchunk)
						dcm.upload.finalsent = true;
					
					// reset call stack by calling next chunk later
					setTimeout(dcm.upload.sendNextChunk, 1);
				}
			};	
			
			xhr.upload.onprogress = function(e) {
				if (e.lengthComputable) {
					dcm.upload.aamt = dcm.upload.famt + e.loaded;	 
				
					var p1 = (dcm.upload.aamt * 100 / dcm.upload.ftotal).toFixed();
					
					if (p1 > 96)
						p1 = 96;
					
					progressBar.css('width', p1 + '%');
					//progressLabel.text(p1 + '%');
					//progressLabel.text(numeral(dcm.upload.aamt / 1024).format('0,0') + 'kb of ' 
					//	+ numeral(ftotal / 1024).format('0,0') + 'kb'));
					
					progressLabel.text(dcm.upload.fmtFileSize(dcm.upload.aamt) + ' of ' + dcm.upload.fmtFileSize(dcm.upload.ftotal));
				}
			};	
			
			xhr.send(content);	
		},

		// from http://stackoverflow.com/questions/10420352/converting-file-size-in-bytes-to-human-readable#answer-22023833
		fmtFileSize: function(bytes) {
		    var exp = Math.log(bytes) / Math.log(1024) | 0;
		    var result = (bytes / Math.pow(1024, exp)).toFixed(2);

		    return result + ' ' + (exp == 0 ? 'bytes': 'KMGTPEZY'[exp - 1] + 'B');
		},

		verifyFile: function() {
			console.log(new Date() + ': Verifying');

			var tmsg = { 
				Service: dcm.upload.service ? dcm.upload.service : 'dcmCms',
				Feature: dcm.upload.feature,
				Op: 'FinishUpload',
				Body: {
					FilePath: dcm.upload.remotePath,
					Status: 'Success',
					Evidence: {
						Size: dcm.upload.file.size
					}
				}
			};
			
			dc.comm.sendMessage(tmsg, function(rmsg) {
				if (rmsg.Result != 0) { 
					dc.pui.Popup.alert('File rejected by server');
					return;
				}
				
				if (dcm.upload.callback)
					dcm.upload.callback(dcm.upload.remotePath);
				else
					window.history.back();
			});
		}
	},
	
	transfer: {

		// from http://stackoverflow.com/questions/10420352/converting-file-size-in-bytes-to-human-readable#answer-22023833
		fmtFileSize: function(bytes) {
		    var exp = Math.log(bytes) / Math.log(1024) | 0;
		    var result = (bytes / Math.pow(1024, exp)).toFixed(2);

		    return result + ' ' + (exp == 0 ? 'bytes': 'KMGTPEZY'[exp - 1] + 'B');
		},
		
		Bucket: function(options) {			
			var defaults = {
				Service: 'dcmCms',
				Feature: 'Buckets',
				Bucket: 'Default',
				Callback: null,
				ProgressBar: '#fileProgressBar',
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
				var progressBar = $(this.settings.ProgressBar);
				var progressLabel = $(this.settings.ProgressLabel);
		
				// we are done uploading so do a verify
				if ((this.data.famt == this.data.ftotal) && this.data.finalsent) {
					// don't finish the progress bar until verify is done
					progressBar.css('width', '98%');
					progressLabel.text('Finishing...');
					
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
					if (e.lengthComputable) {
						buck.data.aamt = buck.data.famt + e.loaded;	 
					
						var p1 = (buck.data.aamt * 100 / buck.data.ftotal).toFixed();
						
						if (p1 > 96)
							p1 = 96;
						
						progressBar.css('width', p1 + '%');
						//progressLabel.text(p1 + '%');
						//progressLabel.text(numeral(this.data.aamt / 1024).format('0,0') + 'kb of ' 
						//	+ numeral(ftotal / 1024).format('0,0') + 'kb'));
						
						progressLabel.text(dcm.transfer.fmtFileSize(buck.data.aamt) + ' of ' + dcm.transfer.fmtFileSize(buck.data.ftotal));
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
		}
	},
	// dc.pui.Loader.addExtraLibs( ['/dcm/cms/js/main.js'] );
	database: {
		Ping: function() {
			dc.comm.sendMessage({ 
				Service: 'dcCoreDataServices', 
				Feature: 'Database', 
				Op: 'ExecuteProc', 
				Body: { 
					Proc: 'dcPing'
				} 
			}, function(resp) {
				if (resp.Result > 0) {
					console.log('error: ' + resp.Message);
					return;
				}
				
				console.log('Reply: ' + resp.Body.Text);
			});
		},
		Echo: function(text) {
			dc.comm.sendMessage({ 
				Service: 'dcCoreDataServices', 
				Feature: 'Database', 
				Op: 'ExecuteProc', 
				Body: { 
					Proc: 'dcEcho', 
					Params: { 
						Text: text 
					} 
				} 
			}, function(resp) {
				if (resp.Result > 0) {
					console.log('error: ' + resp.Message);
					return;
				}
				
				console.log('Reply: ' + resp.Body.Text);
			});
		},
		Encrypt: function(text) {
			dc.comm.sendMessage({ 
				Service: 'dcCoreDataServices', 
				Feature: 'Database', 
				Op: 'ExecuteProc', 
				Body: { 
					Proc: 'dcEncrypt', 
					Params: { 
						Value: text 
					} 
				} 
			}, function(resp) {
				if (resp.Result > 0) {
					console.log('error: ' + resp.Message);
					return;
				}
				
				console.log('Reply: ' + resp.Body.Value);
			});
		},
		Hash: function(text) {
			dc.comm.sendMessage({ 
				Service: 'dcCoreDataServices', 
				Feature: 'Database', 
				Op: 'ExecuteProc', 
				Body: { 
					Proc: 'dcHash', 
					Params: { 
						Value: text 
					} 
				} 
			}, function(resp) {
				if (resp.Result > 0) {
					console.log('error: ' + resp.Message);
					return;
				}
				
				console.log('Reply: ' + resp.Body.Value);
			});
		},
		Select: function(params) {

			/*
			 dcm.database.Select({ 
			 	Table: 'dcUser', 
			 	Select: [
			 	 	{
			 	 		Field: 'Id'
			 	 	},
			 	 	{
			 	 		Field: 'dcUsername'
			 	 	},
			 	 	{
			 	 		Field: 'dcFirstName'
			 	 	},
			 	 	{
			 	 		Field: 'dcLastName'
			 	 	}
			 	],
			 	Where: {
	 				Expression: 'StartsWith',
	 				A: {
	 					Field: 'dcEmail'
	 				},
	 				B: {
	 					Value: 'andy'
	 				}
			 	}
			 })
			 * 
			 *
			 * 
			 dcm.database.Select({ 
			 	Table: 'dcUser', 
			 	Select: [
			 	 	{
			 	 		Field: 'Id'
			 	 	},
			 	 	{
			 	 		Field: 'dgaZipPrefix',
						Name: 'ZipPrefix'
			 	 	},
			 	 	{
			 	 		Field: 'dgaDisplayName',
						Name: 'DisplayName'
			 	 	},
			 	 	{
			 	 		Field: 'dgaIntro',
						Name: 'Intro'
			 	 	},
			 	 	{
			 	 		Field: 'dgaImageSource',
						Name: 'ImageSource'
			 	 	},
			 	 	{
			 	 		Field: 'dgaImageDiscreet',
						Name: 'ImageDiscreet'
			 	 	},
			 	 	{
			 	 		Field: 'dcmState',
						Name: 'State'
			 	 	},
			 	 	{
			 	 		Field: 'dgaVisibleToList',
						Name: 'VisibleToList'
			 	 	},
			 	 	{
			 	 		Field: 'dcAuthorizationTag',
						Name: 'Badges'
			 	 	}
			 	],
			 	Where: {
			 		Expression: 'And',
			 		Children: [
			 			{
			 				Expression: 'Equal',
			 				A: {
			 					Field: 'dgaAccountState'
			 				},
			 				B: {
			 					Value: 'Active'
			 				}
			 			},
			 			{
			 				Expression: 'Equal',
			 				A: {
			 					Field: 'dcConfirmed'
			 				},
			 				B: {
			 					Value: true
			 				}
			 			}
			 		]
			 	},
			 	Collector: {
		 			Field: 'dcAuthorizationTag',
		 			Values: [ 'ApprenticeCandidate' ]
			 	}
			 })
			  
			<Request>
				<Field Name="Table" Type="dcTinyString" Required="True" />
				<Field Name="When" Type="dcTinyString" />
				<Field Name="Select">
					<List Type="dcDbSelectField" />
				</Field>
				<Field Name="Where" Type="dcDbWhereClause" />
				<Field Name="Collector">
					<Record>
						<Field Name="Func" Type="dcTinyString" />
						<!-- or -->
						<Field Name="Field" Type="dcTinyString" />
						<Field Name="SubId" Type="dcTinyString" />
						<Field Name="From" Type="Any" />
						<Field Name="To" Type="Any" />
						<Field Name="Values">
							<List Type="Any" />
						</Field>
						<Field Name="Extras" Type="AnyRecord" />
					</Record>
				</Field>
				<Field Name="Historical" Type="Boolean" />
			</Request>
			*/
			
			if (!params.Select)
				params.Select = [ ];  // select all
			
			dc.comm.sendMessage({ 
				Service: 'dcCoreDataServices', 
				Feature: 'Database', 
				Op: 'ExecuteProc', 
				Body: { 
					Proc: 'dcSelectDirect', 
					Params: params
				} 
			}, function(resp) {
				if (resp.Result > 0) {
					console.log('error: ' + resp.Message);
					return;
				}
				
				console.table(resp.Body);
			});
		},
		Insert: function(params) {

			/*
			 dcm.database.Insert({ 
			 	Table: 'dcUser', 
			 	Fields: {
			 		dcLocale: {
			 			Data: 'es',
			 			UpdateOnly: true
			 		},
			 		dcAuthorizationTag: {
			 			Admin: {
			 				Data: 'Admin'
			 			},
			 			Staff: {
			 				Data: 'Staff'
			 			}
			 		}
			 	}
			 })
			 
			*/
			
			dc.comm.sendMessage({ 
				Service: 'dcCoreDataServices', 
				Feature: 'Database', 
				Op: 'ExecuteProc', 
				Body: { 
					Proc: 'dcInsertRecord', 
					Params: params
				} 
			}, function(resp) {
				if (resp.Result > 0) {
					console.log('error: ' + resp.Message);
					return;
				}
				
				console.table(resp.Body);
			});
		},
		Update: function(params) {

			/*
			 dcm.database.Update({ 
			 	Table: 'dcUser', 
			 	Id: 'xxx',
			 	Fields: {
			 		dcLocale: {
			 			Data: 'es',
			 			UpdateOnly: true
			 		},
			 		dcAuthorizationTag: {
			 			Admin: {
			 				Data: 'Admin'
			 			},
			 			Staff: {
			 				Data: 'Staff'
			 			}
			 		}
			 	}
			 })
			 
			*/
			
			dc.comm.sendMessage({ 
				Service: 'dcCoreDataServices', 
				Feature: 'Database', 
				Op: 'ExecuteProc', 
				Body: { 
					Proc: 'dcUpdateRecord', 
					Params: params
				} 
			}, function(resp) {
				if (resp.Result > 0) {
					console.log('error: ' + resp.Message);
					return;
				}
				
				console.table(resp.Body);
			});
		},
		Retire: function(table, id) {
			dc.comm.sendMessage({ 
				Service: 'dcCoreDataServices', 
				Feature: 'Database', 
				Op: 'ExecuteProc', 
				Body: { 
					Proc: 'dcRetireRecord', 
					Params: { Table: table, Id: id }
				} 
			}, function(resp) {
				if (resp.Result > 0) {
					console.log('error: ' + resp.Message);
					return;
				}
				
				console.table(resp.Body);
			});
		}
	}
}
