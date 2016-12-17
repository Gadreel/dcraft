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

// requestAnimationFrame polyfill
window.requestAnimationFrame = (function(){
  return  window.requestAnimationFrame       ||
          window.webkitRequestAnimationFrame ||
          window.mozRequestAnimationFrame    ||
          function( callback ){
            window.setTimeout(callback, 50);		// try 20 fps - very hacky - TODO can we work with velocity?
          };
})();

// origin polyfill
if (!window.location.origin) 
	window.location.origin = window.location.protocol + "//" + window.location.hostname + (window.location.port ? ':' + window.location.port: '');

// core utilities

var dc = {
	util: {
		Struct: {
			isScalar: function(v) {
				if (dc.util.String.isString(v))
					return true;
				
				if (dc.util.Number.isNumber(v))
					return true;
				
				if (dc.util.Binary.isBinary(v))
					return true;
				
				if (dc.util.Boolean.isBoolean(v))
					return true;
					
				return false;
			},
			isRecord: function(v) {
				if (dc.util.Struct.isEmpty(v) || dc.util.Struct.isScalar(v))
					return false;
					
				if (v instanceof Array)
					return false;
					
				if (v instanceof Object) 
					return true;
					
				return false;
			},
			isList: function(v) {
				if (dc.util.Struct.isEmpty(v) || dc.util.Struct.isScalar(v))
					return false;
					
				if (v instanceof Array)
					return true;
					
				return false;
			},
			isComposite: function(v) {
				if (dc.util.Struct.isEmpty(v) || dc.util.Struct.isScalar(v))
					return false;
					
				if ((v instanceof Object) || (v instanceof Array)) 
					return true;
					
				return false;
			},
			isNotEmpty: function(v) {
				return !dc.util.Struct.isEmpty(v);
			},
			isEmpty: function(v) {
				if ((typeof v == 'undefined') || (v == null))
					return true;
					
				if (dc.util.String.isString(v))
					return (dc.util.String.toString(v).length == 0);
				
				if (dc.util.Number.isNumber(v))
					return false;		// a number is not empty
				
				if (dc.util.Binary.isBinary(v))
					return false;		// TODO support and check length
				
				if (dc.util.Boolean.isBoolean(v))
					return false;		// a bool is not empty
				
				if (v instanceof Array)
					return (v.length == 0);
					
				if (v instanceof Object) {
				    for (var p in v) 
				        if (v.hasOwnProperty(p)) 
				        	return false;
			    }
			    
			    return true;
			}
		},
		List: {
			sortObjects: function(sfield) {
				return function(a, b) { 
					var av = a[sfield];
					var bv = b[sfield];
					
					if ((av === null) && (bv === null))
						return 0;
						
					if (av === null) 
						return -1;
						
					if (bv === null) 
						return -1;
						
					if (av < bv)
						return -1;
						
					if (av > bv)
						return 1;
						
					return 0;
				}
			},
			sortDescObjects: function(sfield) {
				return function(a, b) { 
					var av = a[sfield];
					var bv = b[sfield];
					
					if ((av === null) && (bv === null))
						return 0;
						
					if (av === null) 
						return 1;
						
					if (bv === null) 
						return 1;
						
					if (av < bv)
						return 1;
						
					if (av > bv)
						return -1;
						
					return 0;
				}
			}
		},
		Number: {
			isNumber: function(n) {
				// an array of 1 element with a number in it can get treated as number without check
				if ((n instanceof Object) || (n instanceof Array)) 
					return false;
				
				return !isNaN(parseFloat(n)) && isFinite(n);
			},
			toNumber: function(n) {
				if (isNaN(parseFloat(n)) || !isFinite(n))
					return null;
			
				return parseFloat(n);
			},
			toNumberStrict: function(n) {
				if (isNaN(parseFloat(n)) || !isFinite(n))
					return 0;
			
				return parseFloat(n);
			},
			formatMoney: function(total) {
				return dc.util.Number.toNumberStrict(total).toFixed(2);
			}			
		},
		String: {
			// in dc dates are strings because dates are strings during validation and during transport.
			isString: function(v) {
				return ((v instanceof Date) || (v instanceof String) || (typeof v == 'string'));
			},
			toString: function(v) {
				if (v instanceof Date) 
					v = v.formatStdDateTime();
				else if (dc.util.String.isString(v))
					v = v.toString();
				else if (dc.util.Number.isNumber(v))
					v = dc.util.Number.toNumber(v) + '';
				else if (dc.util.Boolean.isBoolean(v))
					v = dc.util.Boolean.toBoolean(v) ? 'True' : 'False';
					
				if (typeof v != 'string')
					return null;
				
				return v;
			},
			isEmpty: function(v) {
				v = dc.util.toString(v);
					
				if (v && (v.length == 0))
					return true;
			
				return (v == null);
			},
			//trimming space from both side of the string
			trim: function(v) {
				return v.replace(/^\s+|\s+$/g,"");
			},
			//trimming space from left side of the string
			ltrim: function(v) {
				return v.replace(/^\s+/,"");
			},
			//trimming space from right side of the string
			rtrim: function(v) {
				return v.replace(/\s+$/,"");
			},
			//pads left
			lpad: function(str, padString, length) {
			    while (str.length < length)
			        str = padString + str;
			    
			    return str;
			},
			//pads right
			rpad: function(str, padString, length) {
			    while (str.length < length)
			        str = str + padString;
			    
			    return str;
			},
			endsWith: function(subjectString, searchString, position) {
				if (position === undefined || position > subjectString.length) {
					position = subjectString.length;
				}

				position -= searchString.length;
				var lastIndex = subjectString.indexOf(searchString, position);
				return lastIndex !== -1 && lastIndex === position;
			},
			startsWith: function(subjectString, searchString, position) {
				position = position || 0;
				return subjectString.lastIndexOf(searchString, position) === position;
			},
			escapeQuotes: function(v) {
				return v.replace(/'/g,"\\\'").replace(/"/gi,"\\\"");
			},
			escapeSingleQuotes: function(v) {
				return v.replace(/'/g,"\\\'");
			},
			escapeDoubleQuotes: function(v) {
				return v.replace(/"/g,"\\\"");
			}			
		},
		// TODO add support
		Binary: {
			isBinary: function(v) {
				return false;
			},
			toBinary: function(v) {
				return null;
			}
		},
		Boolean: {
			isBoolean: function(v) {
				return ((v instanceof Boolean) || (typeof v == 'boolean'));
			},
			toBoolean: function(v) {
				if (v instanceof Boolean)
					return v.valueOf();
				else if (dc.util.String.isString(v))
					v = (v.toLowerCase().toString() == 'true');
					
				if (typeof v == 'boolean')
					return v;

				if (v)
					return true;
										
				return false;
			}
		},		
		Cookies: {
			getCookie: function(name) {
				var i,x,y,ARRcookies = document.cookie.split(";");
				
				for (var i=0;i<ARRcookies.length;i++) {
				  var x=ARRcookies[i].substr(0,ARRcookies[i].indexOf("="));
				  var y=ARRcookies[i].substr(ARRcookies[i].indexOf("=")+1);
				  x=x.replace(/^\s+|\s+$/g,"");
				  if (x==name)
					return unescape(y);					
				}
			},
			setCookie: function(name,value,exdays,path,domain,secure) {
				var exdate = new Date();
				
				if (exdays)
					exdate.setDate(exdate.getDate() + exdays);
				
				document.cookie=name + "=" + escape(value) 
					+ ( exdays ? "; expires="+exdate.toUTCString() : "")
					+ ( path ? ";path=" + path : "" ) 
					+ ( domain ? ";domain=" + domain : "" ) 
					+ ( secure ? ";secure" : "" );					
			},
			deleteCookie: function(name, path, domain) {
				document.cookie = name + "=" 
					+ ( path  ? ";path=" + path : "") 
					+ ( domain ? ";domain=" + domain : "" ) 
					+ ";expires=Thu, 01-Jan-1970 00:00:01 GMT";
			}
		},
		Date: {
			zToMoment: function(z) {
				if (!z)
					return '';
				
				return moment.utc(z, 'YYYYMMDDTHHmmssSSSZ', true);
			},
			formatZMomentLocal: function(m) {
				if (!z)
					return '';
				
				return m.local().format('MMM Do YYYY, h:mm:ss a Z');
			},
			formatZLocal: function(z) {
				if (!z)
					return '';
				
				return moment.utc(z, 'YYYYMMDDTHHmmssSSSZ', true).local().format('MMM Do YYYY, h:mm:ss a Z');
			},
		
			formatZLocalMedium: function(z) {
				if (!z)
					return '';
				
				if (z.indexOf('T') == -1)
					return z;
					 
				return moment.utc(z, 'YYYYMMDDTHHmmssSSSZ', true).local().format('MM-DD-YYYY h:mm:ss a');
			},
		
			toUtc : function(date) {
				var year = date.getUTCFullYear();
				var month = date.getUTCMonth();
				var day = date.getUTCDate();
				var hours = date.getUTCHours();
				var minutes = date.getUTCMinutes();
				var seconds = date.getUTCSeconds();
				var ms = date.getUTCMilliseconds();
				
				return new Date(year, month, day, hours, minutes, seconds, ms);
			},
			
			stamp: function() {
				return moment.utc().format('YYYYMMDDTHHmmssSSS') + 'Z';
			},
			
			// TODO use moments formating
			toMonth : function(date) {
				if (!date)
					return '';
				
				var month = date.getMonth();
				
				if (month == 0)
					return 'January';
				
				if (month == 1)
					return 'February';
				
				if (month == 2)
					return 'March';
				
				if (month == 3)
					return 'April';
				
				if (month == 4)
					return 'May';
				
				if (month == 5)
					return 'June';
				
				if (month == 6)
					return 'July';
				
				if (month == 7)
					return 'August';
				
				if (month == 8)
					return 'September';
				
				if (month == 9)
					return 'October';
				
				if (month == 10)
					return 'November';
				
				if (month == 11)
					return 'December';
					
				return '';
			},
			
			// TODO use moments formating
			toMonthAbbr : function(date) {
				if (!date)
					return '';
				
				var month = date.getMonth();
				
				if (month == 0)
					return 'Jan';
				
				if (month == 1)
					return 'Feb';
				
				if (month == 2)
					return 'Mar';
				
				if (month == 3)
					return 'Apr';
				
				if (month == 4)
					return 'May';
				
				if (month == 5)
					return 'Jun';
				
				if (month == 6)
					return 'Jul';
				
				if (month == 7)
					return 'Aug';
				
				if (month == 8)
					return 'Sep';
				
				if (month == 9)
					return 'Oct';
				
				if (month == 10)
					return 'Nov';
				
				if (month == 11)
					return 'Dec';
					
				return '';
			},
			
			// TODO use moments formating
			toShortTime: function(date) {
				if (!date)
					return '';
				
				var hrs = date.getHours();
				var mins = date.getMinutes();
				
				if (mins < 10)
					mins = '0' + mins;
				
				if (hrs > 12)
					return (hrs - 12) + ':' + mins + ' pm';
					
				return hrs + ':' + mins + ' am';
			},
			formatZone: function(z) {
				if (z == 'America/Anchorage')
					return 'Alaska';
				if (z == 'America/Phoenix')
					return 'Arizona';
				if (z == 'America/Chicago')
					return 'Central';
				if (z == 'America/New_York')
					return 'Eastern';
				if (z == 'Pacific/Honolulu')
					return 'Hawaii';
				if (z == 'America/Indiana/Indianapolis')
					return 'Indiana-East';
				if (z == 'America/Indiana/Knox')
					return 'Indiana-Starke';
				if (z == 'America/Detroit')
					return 'Michigan';
				if (z == 'America/Denver')
					return 'Mountain';
				if (z == 'America/Los_Angeles')
					return 'Pacific';
					
				return z;
			}
		},
		Web: {
			getQueryParam : function(name) {
			    var match = RegExp('[?&]' + name + '=([^&]*)').exec(window.location.search);
			    return match && decodeURIComponent(match[1].replace(/\+/g, ' '));
			},
			getHashParam : function(name) {
			    var match = RegExp('[?&#]' + name + '=([^&]*)').exec(window.location.hash);
			    return match && decodeURIComponent(match[1].replace(/\+/g, ' '));
			},
			getFromParams : function(params,name) {
			    var match = RegExp('[?&#]' + name + '=([^&]*)').exec(params);
			    return match && decodeURIComponent(match[1].replace(/\+/g, ' '));
			},
			urldecode: function(v) {
				return decodeURIComponent(v.replace(/\+/g, '%20'));
			},
			escapeHtml: function(val) {
				if (dc.util.Struct.isEmpty(val))
					return '';

				if (!dc.util.String.isString(val))
					val = '' + val;
					
				return val.replace(/&/g, '&amp;').replace(/>/g, '&gt;').replace(/</g, '&lt;').replace(/"/g, '&quot;').replace(/'/g, '&apos;');
			},
			unescapeHtml: function(val) {
				if (dc.util.Struct.isEmpty(val))
					return '';

				if (!dc.util.String.isString(val))
					val = '' + val;
					
				return val.replace(/&amp;/g, '&').replace(/&gt;/g, '>').replace(/&lt;/g, '<').replace(/&quot;/g, '"').replace(/&apos;/g, '\'');
			},
			// retrive 
			getPath : function(offset) {
				var path = location.pathname;
				offset++;  // for initial /

				while ((offset > 0) && (path.length > 0)) {
					if (path[0] == '/')
						offset--;

					path = (path.length > 1) ? path.substr(1) : '';
				}

				return path.split('/')[0];
			},
			getPathFrom : function(offset) {
				var path = location.pathname;
				offset++;  // for initial /

				while ((offset > 0) && (path.length > 0)) {
					if (path[0] == '/')
						offset--;

					path = (path.length > 1) ? path.substr(1) : '';
				}

				return path.split('/');
			},
			isTouchDevice: function() {
				 return (('ontouchstart' in window)
				      || (navigator.MaxTouchPoints > 0)
				      || (navigator.msMaxTouchPoints > 0));
			},
			isChrome: function() {
				return navigator.userAgent.indexOf('Chrome') > -1;
			},
			isExplorer: function() {
				return navigator.userAgent.indexOf('MSIE') > -1;
			},
			isFirefox: function() {
				return navigator.userAgent.indexOf('Firefox') > -1;
			},
			isSafari: function() {
				var is_chrome = navigator.userAgent.indexOf('Chrome') > -1;
				var is_safari = navigator.userAgent.indexOf("Safari") > -1;
				
				if (is_chrome && is_safari) 
					is_safari = false;
					
				return is_safari;
			},
			isOpera: function() {
				return navigator.userAgent.indexOf("Presto") > -1;
			},
			isWindows: function() {
				return navigator.userAgent.toLowerCase().indexOf("windows") > -1;			
			}
		},
		Image: {
			blobToUrl: function(blob) {
				return (window.URL || window.webkitURL).createObjectURL(blob);
			},
			load: function(url, callback, progcallback) {
				var completedPercentage = 0;
				var prevValue = 0;
				
				var thisImg = new Image();
				
				thisImg.onload = function(e) {
					if (callback) 
						callback(thisImg);
				};

				var xmlHTTP = new XMLHttpRequest();
				
				xmlHTTP.open('GET', url , true);
				xmlHTTP.responseType = 'arraybuffer';
			
			    xmlHTTP.onload = function( e ) {
					var h = xmlHTTP.getAllResponseHeaders();
					var m = h.match(/^Content-Type\:\s*(.*?)$/mi);
					var mimeType = m[1] || 'image/png';
					
					var blob = new Blob([ this.response ], { type: mimeType });
					
					thisImg.src = dc.util.Image.blobToUrl(blob);
			    };
			
			    xmlHTTP.onprogress = function(e) {
			        if (e.lengthComputable) {
			            completedPercentage = parseInt(( e.loaded / e.total ) * 100);
			
				        if (progcallback && (prevValue != completedPercentage)) 
				        	progcallback(thisImg, completedPercentage);
			        }
			    };
			
			    xmlHTTP.onloadstart = function() {
			        // Display your progress bar here, starting at 0
			        completedPercentage = 0;
			        
			        if (progcallback) 
			        	progcallback(thisImg, completedPercentage);
			    };
			
			    xmlHTTP.onloadend = function() {
			        // You can also remove your progress bar here, if you like.
			        completedPercentage = 100;
			        
			        if (progcallback) 
			        	progcallback(thisImg, completedPercentage);
			    }
			
			    xmlHTTP.send();
			    
			    return thisImg;
			}
		},
		Uuid: {
			EMPTY : '00000000-0000-0000-0000-000000000000',
			create : function() {
				var _padLeft = function (paddingString, width, replacementChar) {
					return paddingString.length >= width ? paddingString : _padLeft(replacementChar + paddingString, width, replacementChar || ' ');
				};

				var _s4 = function (number) {
					var hexadecimalResult = number.toString(16);
					return _padLeft(hexadecimalResult, 4, '0');
				};

				var _cryptoGuid = function () {
					var buffer = new window.Uint16Array(8);
					window.crypto.getRandomValues(buffer);
					return [_s4(buffer[0]) + _s4(buffer[1]), _s4(buffer[2]), _s4(buffer[3]), _s4(buffer[4]), _s4(buffer[5]) + _s4(buffer[6]) + _s4(buffer[7])].join('-');
				};

				var _guid = function () {
					var currentDateMilliseconds = new Date().getTime();
					return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (currentChar) {
						var randomChar = (currentDateMilliseconds + Math.random() * 16) % 16 | 0;
						currentDateMilliseconds = Math.floor(currentDateMilliseconds / 16);
						return (currentChar === 'x' ? randomChar : (randomChar & 0x7 | 0x8)).toString(16);
					});
				};

				return (window.crypto && window.crypto.getRandomValues) ? _cryptoGuid() : _guid();
			}
		},
		Crypto: {
			makeSimpleKey : function() {
				var text = "";
				var possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

				// TODO use window.crypto.getRandomValues if available
				
				for( var i=0; i < 16; i++ )
					text += possible.charAt(Math.floor(Math.random() * possible.length));

				return text;
			},
			makeSimpleHash: function(v) {
			    var hash = 0;
			    
			    if (! v) 
			    	return hash;
			    
			    for (var i = 0; i < v.length; i++) {
			        char = v.charCodeAt(i);
			        hash = ((hash<<5)-hash)+char;
			        hash = hash & hash; // Convert to 32bit integer
			    }
			    
			    return hash;
			}
		},
		Text: {
			// inspired by https://github.com/coolaj86/TextEncoderLite/
			// return an ArrayBuffer
			utf8Encode: function(str, units) {
				  units = units || Infinity;
				  var codePoint = 0;
				  var length = str.length;
				  var leadSurrogate = null;
				  var bytes = [];

				  for (var i = 0; i < length; i++) {
				    codePoint = str.charCodeAt(i);

				    // is surrogate component
				    if (codePoint > 0xD7FF && codePoint < 0xE000) {
				      // last char was a lead
				      if (leadSurrogate) {
				        // 2 leads in a row
				        if (codePoint < 0xDC00) {
				          if ((units -= 3) > -1) bytes.push(0xEF, 0xBF, 0xBD);
				          leadSurrogate = codePoint;
				          continue;
				        } 
				        else {
				          // valid surrogate pair
				          codePoint = leadSurrogate - 0xD800 << 10 | codePoint - 0xDC00 | 0x10000;
				          leadSurrogate = null;
				        }
				      } 
				      else {
				        // no lead yet

				        if (codePoint > 0xDBFF) {
				          // unexpected trail
				          if ((units -= 3) > -1) bytes.push(0xEF, 0xBF, 0xBD);
				          continue;
				        } 
				        else if (i + 1 === length) {
				          // unpaired lead
				          if ((units -= 3) > -1) bytes.push(0xEF, 0xBF, 0xBD);
				          continue;
				        } 
				        else {
				          // valid lead
				          leadSurrogate = codePoint;
				          continue;
				        }
				      }
				    } 
				    else if (leadSurrogate) {
				      // valid bmp char, but last char was a lead
				      if ((units -= 3) > -1) bytes.push(0xEF, 0xBF, 0xBD);
				      leadSurrogate = null;
				    }

				    // encode utf8
				    if (codePoint < 0x80) {
				      if ((units -= 1) < 0) break;
				      bytes.push(codePoint);
				    } 
				    else if (codePoint < 0x800) {
				      if ((units -= 2) < 0) break;
				      bytes.push(
				        codePoint >> 0x6 | 0xC0,
				        codePoint & 0x3F | 0x80
				      );
				    } 
				    else if (codePoint < 0x10000) {
				      if ((units -= 3) < 0) break;
				      bytes.push(
				        codePoint >> 0xC | 0xE0,
				        codePoint >> 0x6 & 0x3F | 0x80,
				        codePoint & 0x3F | 0x80
				      );
				    } 
				    else if (codePoint < 0x200000) {
				      if ((units -= 4) < 0) break;
				      bytes.push(
				        codePoint >> 0x12 | 0xF0,
				        codePoint >> 0xC & 0x3F | 0x80,
				        codePoint >> 0x6 & 0x3F | 0x80,
				        codePoint & 0x3F | 0x80
				      );
				    } 
				    else {
				      throw new Error('Invalid code point');
				    }
				  }
				
				return new Uint8Array(bytes).buffer;
			},
			// inspired by https://github.com/coolaj86/TextEncoderLite/
			// takes a ArrayBuffer
			utf8Decode: function(buffer, start, end) {
				var view = new DataView(buffer);
				
				end = Math.min(view.byteLength, end || Infinity)
				start = start || 0;
				  
				  var res = ''
				  var tmp = ''

				  for (var i = start; i < end; i++) {
					var byte = view.getUint8(i);
						
				    if (byte <= 0x7F) {
				      res += dc.util.Text.decodeUtf8Char(tmp) + String.fromCharCode(byte)
				      tmp = ''
				    } 
				    else {
				      tmp += '%' + byte.toString(16)
				    }
				  }

				  return res + dc.util.Text.decodeUtf8Char(tmp)
			},			
			decodeUtf8Char: function(str) {
			  try {
			    return decodeURIComponent(str)
			  } 
			  catch (err) {
			    return String.fromCharCode(0xFFFD) // UTF 8 invalid char
			  }
			}
		},
		Hex: {
			toHex: function(str) {
				return dc.util.Hex.bufferToHex(dc.util.Text.utf8Encode(str));
			},
			bufferToHex: function(buffer) {
				var view = new DataView(buffer);
				var hex = '';
				
				for (var i = 0; i < view.byteLength; i++) 
					hex += view.getUint8(i).toString(16);

				return hex;
			}			
		},
		File: {
			isLegalFilename: function(name) {
				if (! name)
					return false;
				
				// TODO
				
				//if (name.equals(".") || name.contains("..") || name.contains("*") || name.contains("\"") || name.contains("/") || name.contains("\\")
				//		 || name.contains("<") || name.contains(">") || name.contains(":") || name.contains("?") || name.contains("|"))
				//	return false;
				
				return true;
			},			
			toLegalFilename: function(name) {
				if (! name)
					return null;
				
				// must escape regex chars - [\^$.|?*+()
				name = name.replace(new RegExp("\\.\\.", 'g'), "_").replace(new RegExp("\\*", 'g'), "_").replace(new RegExp("\"", 'g'), "_")
							.replace(new RegExp("\\/", 'g'), "_").replace(new RegExp("\\\\", 'g'), "_").replace(new RegExp("<", 'g'), "(")
							.replace(new RegExp(">", 'g'), ")").replace(new RegExp(":", 'g'), "_").replace(new RegExp("\\?", 'g'), "_")
							.replace(new RegExp("\\|", 'g'), "_");
				
				return name;
			},			
			// allow only these: - _ . ( )
			toCleanFilename: function(name) {
				name = dc.util.File.toLegalFilename(name);
				
				if (! name)
					return null;
				
				name = name.replace(new RegExp(" ", 'g'), "-").replace(new RegExp("%", 'g'), "_").replace(new RegExp("@", 'g'), "_")
						.replace(new RegExp("#", 'g'), "_").replace(new RegExp(",", 'g'), "_")
						.replace(new RegExp("~", 'g'), "_").replace(new RegExp("`", 'g'), "_").replace(new RegExp("!", 'g'), "_")
						.replace(new RegExp("\\$", 'g'), "_").replace(new RegExp("\\^", 'g'), "_").replace(new RegExp("&", 'g'), "_")
						.replace(new RegExp("&", 'g'), "_").replace(new RegExp("=", 'g'), "_").replace(new RegExp("\\+", 'g'), "-")
						.replace(new RegExp("{", 'g'), "(").replace(new RegExp("}", 'g'), ")").replace(new RegExp("\\[", 'g'), "(")
						.replace(new RegExp("\\]", 'g'), ")").replace(new RegExp(";", 'g'), "_").replace(new RegExp("'", 'g'), "_")
						.replace(new RegExp("<", 'g'), "(").replace(new RegExp(">", 'g'), ")");
				
				var fname = '';
				var skipon = false;
				
				for (var i = 0; i < name.length; i++) {
					var c = name.charAt(i);
					
					if ((c == '-') || (c =='_')) {
						if (skipon)
							continue;
						
						skipon = true;
					}
					else {
						skipon = false;
					}
					
					fname += c;
				}
				
				return fname;
			}
		}
	},
	lang: {
		// TODO put DebugLevel enum in and use it (below)
		
		Dict: {
			_tokens: { },
			
			get: function(token) {
				return dc.lang.Dict._tokens[token];
			},
			load: function(tokens) {
				if (!tokens)
					return;
					
				for (var i in tokens) {
					var dt = tokens[i];
					dc.lang.Dict._tokens[dt.Token] = dt.Value;
				} 
			},
			tr: function(token, params) {
				var val = dc.lang.Dict._tokens[token];
				
				if (!dc.util.Struct.isList(params))
					params = []; 
				
				var msg = '';
		        var lpos = 0;
		        var bpos = val.indexOf('{$');
		
		        while (bpos != -1) {
		            var epos = val.indexOf("}", bpos);
		            if (epos == -1) 
		            	break;
		
		            msg += val.substring(lpos, bpos);
		
		            lpos = epos + 1;
		
		            var varname = val.substring(bpos + 2, epos).trim();
		
		            // TODO add some formatting features for numbers/datetimes
		            
		            var parampos = varname - 1;
		            
		            if (parampos <= params.length) 
		            	msg += params[parampos];
		            else 
		                msg += val.substring(bpos, epos + 1);
		
		            bpos = val.indexOf("{$", epos);
		        }
		
		        msg += val.substring(lpos);
				
				return msg;
			}
		},
		
		OperationResult: function(loglevel) {
			this.Code = 0;		// error code, non-zero means error, only first error code is tracked 
			this.Message = null;		// error code, non-zero means error, first code is tracked 
			this.Messages = [];
			this.LogLevel = loglevel ? loglevel : 0;
			this.Result = null;		// return value if any
    
			this.trace = function(code, msg) {
				this.log(4, code, msg);
			};
			
			this.info = function(code, msg) {		
				this.log(3, code, msg);
			};
			
			this.warn = function(code, msg) {
				this.log(2, code, msg);
			};
			
			this.error = function(code, msg) {
				this.log(1, code, msg);
			};
			
			this.exit = function(code, msg) {
				this.Code = code;
				this.Message = msg;
				
				this.log(3, code, msg);
			};
			
			this.log = function(lvl, code, msg) {
				this.Messages.push({
					Occurred: moment().format('MMM Do YYYY, h:mm:ss a Z'),
					Level: lvl,
					Code: code,
					Message: msg
				});
				
				if ((lvl == 1) && (this.Code == 0)) {
					this.Code = code;
					this.Message = msg;
				}		
			};
			
			this.traceTr = function(code, params) {
				this.logTr(4, code, params);
			};
			
			this.infoTr = function(code, params) {		
				this.logTr(3, code, params);
			};
			
			this.warnTr = function(code, params) {
				this.logTr(2, code, params);
			};
			
			this.errorTr = function(code, params) {
				this.logTr(1, code, params);
			};
			
			this.exitTr = function(code, params) {
				var msg = dc.lang.Dict.tr("_code_" + code, params)
				
				this.Code = code;
				this.Message = msg;
				
				this.logTr(3, code, params);
			};
			
			// params should be an array
			this.logTr = function(lvl, code, params) {
				var msg = dc.lang.Dict.tr("_code_" + code, params)
				
				this.Messages.push({
					Occurred: moment().format('MMM Do YYYY, h:mm:ss a Z'),
					Level: lvl,
					Code: code,
					Message: msg
				});
				
				if ((lvl == 1) && (this.Code == 0)) {
					this.Code = code;
					this.Message = msg;
				}		
			};
		
			// from another result
			this.copyMessages = function(res) {		
				for (var i = 0; i < res.Messages.length; i++) {
					var msg = res.Messages[i];
					this.Messages.push(msg);
					
					if ((msg.Level == 1) && (this.Code == 0)) {
						this.Code = msg.Code;
						this.Message = msg.Message;
					}
				}		
				
				// if not in list, still copy top error message
				if (this.Code == 0) {
					this.Code = res.Code;
					this.Message = res.Message;
				}
			};
		
			this.hasErrors = function() {
				return (this.Code != 0);
			};		
		},
		CountDownCallback: function(cnt, callback) {
			this.cnt = cnt;
			this.callback = callback;
			
			this.dec = function() {
				this.cnt--;
				
				if (this.cnt == 0)
					this.callback();
			};
			
			this.inc = function() {
				this.cnt++;
			};
		}
	}
}

dc.lang.Task = function(steps, observer) {
	this.init(steps, observer);
};  

dc.lang.Task.prototype = new dc.lang.OperationResult();

dc.lang.Task.prototype.init = function(steps, observer) {
	//dc.lang.OperationResult.prototype.init.call(this, entry, node);
	
	this.Result = null;
	this.Steps = steps;
	this.CurrentStep = 0;
	this.Observers = [ ];
	
	if (observer)
		this.Observers.push(observer);
}

dc.lang.Task.prototype.run = function() {
	if (this.Steps.length <= this.CurrentStep) {
		this.complete();
		return;
	}
	
	var step = this.Steps[this.CurrentStep];
	
	// TODO if step does not have Alias or Title add
	
	if (!step.Params)
		step.Params = { };
		
	if (!step.Store)
		step.Store = { };
		
	step.Func.call(this, step);
}

dc.lang.Task.prototype.kill = function() {
	this.error(1, 'Task could not complete');
	this.complete();
}

dc.lang.Task.prototype.resume = function() {
	if (this.hasErrors()) {
		this.complete();
		return;
	}
	
	var step = this.Steps[this.CurrentStep];

	if (! step.Repeat)
		this.CurrentStep++;

	var task = this;
	
	window.setTimeout(function() { task.run(); }, 1);	
}

dc.lang.Task.prototype.resumeNext = function() {
	if (this.hasErrors()) {
		this.complete();
		return;
	}
	
	this.CurrentStep++;

	var task = this;
	
	window.setTimeout(function() { task.run(); }, 1);	
}

dc.lang.Task.prototype.complete = function() {
	for (var i = 0; i < this.Observers.length; i++) 
		this.Observers[i](this);
}

