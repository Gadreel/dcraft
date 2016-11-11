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

dc.comm = {
	/**
	 * Only init once.
	 */
	_initFlag: false,
	_session: null,
	
	init: function(callback) {
		// only init once per page load
		if (dc.comm._initFlag) {
			callback();				
			return;
		}
		
		dc.comm._initFlag = true;	
				
		// check to see see if the user info was remembered
		// this is not so secure as we use a hard coded key for that, but at least it is
		// encrypted on disk.  'Remember' should only be used on devices with personal 
		// accounts - never shared accounts or public devices.
		dc.user.loadRememberedUser();
		
		/*
		// periodically keep session going - no longer needed/wanted!
		setInterval(function() {
			if (dc.user.isVerified()) {
				dc.comm.sendForgetMessage({ 
					Service: 'Session',
					Feature: 'Control',
					Op: 'Touch'
				});
				
				// TODO check for messages on server
				
				// also periodically run the timeout checker for replies
				
				// TODO check for timeouts in Replies
	
				// TODO		callbackfunc( { Result: 1, Message: "AJAX call failed or timed out." } );
			}
		}, 55000);
		*/

		callback();				
	},
	
	sendForgetMessage : function(msg) {
		msg.RespondTag = 'SendForget';
		
		dc.comm.sendMessage(msg);
	},
	
	sendMessage : function(msg, callbackfunc, timeout) {
		if (dc.comm._session)
			msg.Session = dc.comm._session;
		
		var onfail = function(rmsg) {
			if (callbackfunc) 
				callbackfunc(rmsg);
		};
		
		var onsuccess = function(rmsg) {
			var ee = dc.comm.Messages.findExitEntry(rmsg.Messages);
			
			// setup the "result" of the message based on the exit entry
			if (!ee) {
				rmsg.Result = 0;
			}
			else {
				rmsg.Result = ee.Code;
				rmsg.Message = ee.Message;
			}

			dc.comm._session = rmsg.Session; 
			
			if (rmsg.SessionChanged) {
				console.log('session changed');
				
				if (dc.pui && dc.pui.Loader)
					dc.pui.Loader.sessionChanged();
			}
			
			if (callbackfunc) 
				callbackfunc(rmsg);
		};
		
		var processRequest = function(e) {
		    if (xhr.readyState == 4) {
		    	try {
			    	if (xhr.status == 200) {
			    		var rmsg = JSON.parse(xhr.responseText);

			    		onsuccess(rmsg);
			    	}
			    	else {
			    		onfail({
							Result: 1,
							Message: 'Server responded with an error code.'
			    		});
			    	}
		    	}
		    	catch (x) {
		    		onfail({
						Result: 1,
						Message: 'Server responded with an invalid message.'
		    		});
		    	}
		    }
		};
		
		var xhr = new XMLHttpRequest();
		xhr.open('POST', '/rpc?nocache=' + dc.util.Crypto.makeSimpleKey(), true);
		
		xhr.timeout = timeout ? timeout : 60000;
		
		xhr.setRequestHeader('Content-Type', 'application/json; charset=utf-8');
		
		xhr.addEventListener("readystatechange", processRequest, false);
		
		xhr.addEventListener("ontimeout", function() { 
    		onfail({
				Result: 1,
				Message: 'Server timed out, no response.'
    		});
		}, false);
		
		xhr.send(JSON.stringify(msg));
	},
	
	sendTestMessage : function(msg) {
		dc.comm.sendMessage(msg, function(res) {
			console.log('Result: '); // + JSON.stringify(res));
			console.dir(res);
		});
	},
	Messages: {
		findExitEntry : function(list) {
			if (!dc.util.Struct.isList(list)) 
				return null;
		
			var firsterror = null;
			
			for (var i = list.length - 1; i >= 0; i--) {
				var msg = list[i];
				
				if ("Error" == msg.Level)
					firsterror = msg;
			
				if (dc.util.Struct.isList(msg.Tags)) {
					for (var t = 0; t < msg.Tags.length; t++) {
						if (msg.Tags[t] == 'Exit')
							return (firsterror != null) ? firsterror : msg;
					}
				}
			}

			return firsterror;
		}
	/* TODO
	},
	
	Tracker: {
		_list: [],
		_status: [],
		_handler: null,
		
		init: function() {
			setInterval(dc.comm.Tracker.refresh, 1000);
		},
		
		trackMessage: function(msg) {
			dc.comm.sendMessage(msg, function(e) {
				if (e.Result > 0) {
					dc.pui.Popup.alert(e.Message);
					return;
				}
				
				dc.comm.Tracker.add(e.Body);
			});
		},
		
		// to the top of the list
		add: function(task, work) {
			if (dc.util.Struct.isRecord(task))
				dc.comm.Tracker._list.unshift(task);
			else
				dc.comm.Tracker._list.unshift( { TaskId: task, WorkId: work } );
		},
		
		setWatcher: function(v) {
			dc.comm.Tracker._handler = v;
		},
		
		getStatus: function() {
			return dc.comm.Tracker._status;
		},
		
		getStatusFor: function(taskid) {
			for (var i = 0; i < dc.comm.Tracker._status.length; i++) {
				var task = dc.comm.Tracker._status[i];
				
				if (task.TaskId == taskid) 
					return task;
			}		
			
			return null;
		},
		
		clear: function(task) {
			for (var i = 0; i < dc.comm.Tracker._list.length; i++) {
				if (dc.comm.Tracker._list[i].TaskId == task) {
					dc.comm.Tracker._list[i].splice(i, 1);
					break;
				}
			}		
		},
		
		refresh: function() {
			if (dc.comm.Tracker._list.length == 0) {
				if (dc.comm.Tracker._handler)
					dc.comm.Tracker._handler.call(dc.comm.Tracker._status);
				
				return;
			}
			
			var chklist = [ ];
				
			var slist = dc.comm.Tracker._status;
			
			for (var i = 0; i < dc.comm.Tracker._list.length; i++) {
				var task = dc.comm.Tracker._list[i];
			
				var skiptask = false;
			
				for (var i = 0; i < slist.length; i++) {
					if (task.TaskId != slist[i].TaskId) 
						continue;
					
					skiptask = (slist[i].Status == 'Completed');
					break;
				}
				
				if (!skiptask)
					chklist.push(task);
			}		
			
			// no RPC if nothing to check
			if (chklist.length == 0) {
				if (dc.comm.Tracker._handler)
					dc.comm.Tracker._handler.call();
				
				return;
			}
			
			dc.comm.sendMessage({ 
				Service: 'Status',
				Feature: 'Info',
				Op: 'TaskStatus', 
				Body: chklist 
			}, function(e) {
				for (var i = 0; i < e.Body.length; i++) {
					var status = e.Body[i];
				
					var fnd = false;
				
					for (var i = 0; i < slist.length; i++) {
						if (status.TaskId != slist[i].TaskId) 
							continue;
						
						fnd = true;
						slist[i] = status;
						break;
					}
					
					if (!fnd)
						dc.comm.Tracker._status.push(status);
				}		
				
				if (dc.comm.Tracker._handler)
					dc.comm.Tracker._handler.call(dc.comm.Tracker._status);
			});
		}
		*/
	}
}