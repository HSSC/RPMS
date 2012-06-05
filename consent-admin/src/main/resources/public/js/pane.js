// The PaneManager allows for treating requests for viewable content as viewable within an existing
// container that is referred to as a Pane.  A Pane represents a single view of data that can be 
// chained to other Panes in order to go back and forth through the stack of views.  A Pane is structured
// as follwows:
//
// pane = {
// 	request: {
// 		url: "",	// The original URL that requested the Pane. This is the application path, meaning it starts after the context path.
// 		params: {},	// The original parameters that are added to the URL.  These are added to the request as query parameters.
// 		options: {}	// The original options that accompanied.
// 	},
// 	previous: , 	// The Pane that opened this Pane using a 'push'.  Allows for this Pane to 'pop' back to the previous Pane.
// 	pane: 			// Reference to the DIV that is the top container for the Pane content.
// }
//
//

var PaneManager = {
	// Request References/Methods
	basepath: null, // The base path to prepend to URL.  Typically, this is just the contextPath of the application
	content: null, // The element where all content goes.
	confirmDialog: null, // The dialog to use for informing/questioning user.
	
	initContent: function(content){
		this.content = content;
		this.content.empty();
		var r = this.initRequest;
		if(r != null){
			this.stack(r.url, r.params, r.options);
		}
		this.initialized = true;
	},
	
	initDialog: function(dialog){
		this.confirmDialog = dialog;
		dialog.hide();
	},
	
	initBasePath: function(path){
		this.basepath = path;
	},
	
	triggerOnInit: function(url, params, options){
		if(this.initialized){
			this.stack(url, params, options);
		}
		else{
			this.initRequest = {url: url, params: params, options: options};
		}
	},
	
	getUrl: function(x, ps, pane){
		if(pane == null){
			pane = true;
		}
		ps = this.util.map(ps);
		if(this.basepath != null && x.indexOf(this.basepath) != 0){
			x = this.basepath + x;
		}
		if(pane){
			ps["view-mode"] = "pane";
		}
		var con = "?";
		if(x.indexOf(con) > 0){
			con = "&";
		}
		if(ps != null && !$.isEmptyObject(ps)){
			x = x + con + $.param(ps);
		}
		return x;
	},
	
	// Pane References
	current: null,
	
	hasPanes: function(){return this.current != null;},
	
	reset: function(){
		var t = this.current;
		var c = this.content;
		this.current = null;
		var callback = null;
		if(arguments.length > 0){
			callback = arguments[0];
		}
		if(t != null){
			t.pane.hide("slide", {direction: "down"}, this.settings.duration, function(){
				c.empty();
				if(callback){
					callback();
				}
			});
		}
	},
	
	// Text that is used in the UI.  Can be overridden on init.
	text:{
		pane:{
			confirm$reset: "Are you sure you want to clear out the current workspace?"
		},
		logout: {
			title: "Confirm Logout",
			ok: "Logout",
			message: "Are you sure you want to end this session?"
		},
		error: {
			title: "Failure Occurred",
			message: "An error occured while processing the request.  Please inform your application administrator.",
			close: "Close"
		},
		label: {
			cancel: "Cancel",
			ok: "OK",
			confirm: "Confirm"
		}
	},
	
	settings: {
		duration: 500	// The duration of a transition between slides.
	},
	
	// Confirm Method
	confirm: function(options){
		var m = this.util.mapped$rq(options, "message");
		var of = this.util.mapped$rq(options, "onok");
		var t = this.util.mapped(options, "title", this.text.label.confirm);
		var h = this.util.mapped(options, "height", "auto");
		var ol = this.util.mapped(options, "oklabel", this.text.label.ok);
		var cl = this.util.mapped(options, "cancellabel", this.text.label.cancel);
		var cf = this.util.mapped(options, "oncancel", function(){$( this ).dialog( "close" );});
		this.confirmDialog.text(m);

		var buts = {};
		buts[ol] = function (){of(); $(this).dialog( "close" )};
		buts[cl] = cf;
		this.confirmDialog.dialog({
			title: t,
			resizable: false,
			height: h,
			modal: true,
			buttons: buts
		});
	},
	
	// Logout Method
	logout: function(){
		url = this.basepath + "/logout";
		options = {};
		options["message"] = this.text.logout.message;
		options["onok"] = function() {document.location = url;};
		options["title"] = this.text.logout.title;
		options["oklabel"] = this.text.logout.ok;
		this.confirm(options);
	},
	
	// Utility Methods
	util: {
		mapped: function(m, k, d){
			if(m != null && m[k] != null){
				return m[k];
			}
			return d;
		},
		mapped$rq: function(m, k){
			if(m != null && m[k] != null){
				return m[k];
			}
			throw "The '" + k + "' option is required.";
		},
		map: function(m){
			if(m == null) return {};
			return m;
		},
		nothing: function(){},
		getin: function(map){
			var value = map;
			if(arguments.length > 1){
				for(var i = 1; i < arguments.length; i++){
					value = value[arguments[i]];
					if(value == null){
						return null;
					}
				}
			}
			return value;
		}
	},
	
	// Starts a new stack of panes.  Clears all existing panes out and starts a new stack.
	stack: function(url, params, options){
		var pushfx = function(){PaneManager.push(url, params, options)};
		
		if(this.hasPanes()){
			this.confirm({
				onok: function(){PaneManager.reset(pushfx)}, 
				message: this.text.pane.confirm$reset});
		}
		else{
			pushfx();
		}
	},
	
	// Adds a new pane to the current stack.
	push: function(url, params, options){
		options = this.util.map(options);
		params = this.util.map(params);
		var request = {
			url: url,
			params: params,
			options: options
		}
		var fullUrl = this.getUrl(url, params);
		
		var settings = {
				dataType: "html",
				success: function(data, status, xhr){
					PaneManager.onsuccess(data, request, status, xhr);
				},
				error: function(xhr, status, thrown){
					var body;
					try{
						body = $.parseJSON(xhr.responseText);
					}
					catch(e){
						if(xhr.status == 404){
							body = {message: "Unable to find process matching '" + fullUrl + "'"};
						}
						else{
							body = {message: "Unable to execute process matching '" + fullUrl + "'"};
						}
					}
					PaneManager.onfailure(body, request, status, xhr);
				}
		}
		if(options != null && options.ajax != null){
			settings = $.extend(settings, options['ajax']);
		}
		$.ajax(fullUrl, settings);
	},
	
	// Processes a successful retrieve of a pane.
	onsuccess: function(data, request, status, xhr){
		// Create The Pane - Expects that any prepane processing has already been done.
		var pane = {request: request, cache: {}};
		pane.refresh = function(){PaneManager.refresh({pane: pane})};
		if(this.current != null){
			pane.previous = this.current;
			$(this.current.pane).hide("slide", {direction: "left"}, this.settings.duration);
		}
		
		$(this.content).append("<div class='pane' style='display:none;'></div>");
		pane.pane = $(content).children().last();
		pane.pane.html(data);
		pane.pane.show("slide", {direction: "right"}, this.settings.duration);
		this.current = pane;
	},
	
	// When content is refreshed for a pane.
	onrefresh: function(data, target, status, xhr){
		target.pane.empty();
		target.pane.html(data);
	},
	
	// Processes a failed request for a pane.
	onfailure: function(data, request, status, xhr){
		if(this.errorDialog == null){
			this.errorDialog = $("<div class='dialog errordialog'/>");
			this.errorDialog.appendTo($("body"));
		}
		var message = this.text.error.message;
		if(data != null){
			if(data.message != null){
				message = data.message;
			}
			else if (typeof data == "string"){
				message = data;
			}
		}
		this.errorDialog.text(message);
		var buts = {};
		buts[this.text.error.close] = function (){$(this).dialog( "close" )};
		this.errorDialog.dialog({
			title: this.text.error.title,
			resizable: false,
			modal: true,
			buttons: buts
		});
	},
	
	// Removes the current pane from the stack, making the previous pane the current one.
	// onpop, postpop
	pop: function(options){
		options = this.util.map(options);
		var poppane = this.current;
		
		if(poppane){
			var previous = poppane.previous;
			
			// Define The Post Hide Current Function
			if(poppane.request.options.onpop){
				if(false === poppane.request.options.onpop(options, poppane, previous)){
					return;
				}
			}
			
			// Poppin Is A Go
			this.current = null;
			var postHide = function(){
				poppane.pane.remove();
			};
			poppane.pane.hide("slide", {direction: "right"}, this.settings.duration, postHide);
			
			// Define The Show Previous Function
			if(previous){
				this.current = previous;
				if(poppane.request.options.postpop){
					poppane.request.options.postpop(options, poppane, previous);
				}
				//PaneManager.applyBehaviors();
				previous.pane.show("slide", {direction: "left"}, this.settings.duration);
			}
		}
	},
	
	// Refreshes the current pane in the stack.
	refresh: function(options){
		options = this.util.map(options);
		target = this.util.mapped(options, "pane", this.current);
		if(target){
			var fullUrl = this.getUrl(target.request.url, target.request.params);
			var settings = {
					dataType: "html",
					success: function(data, status, xhr){
						PaneManager.onrefresh(data, target, status, xhr);
					},
					error: function(data, status, xhr){
						PaneManager.onfailure(data, target.request, status, xhr);
					}
			}
			if(options != null && options.ajax != null){
				settings = $.extend(settings, options['ajax']);
			}
			$.ajax(fullUrl, settings);
		}
	},
	
	// Provides a way to cache values specifically on the pane.  When a pane goes out of scope/popped so does the cache.
	cache: function(){
		if(arguments.length == 0){
			return this.current.cache;
		}
		else if(arguments.length == 1){
			var keys = arguments[0].split("#");
			var value = this.current.cache;
			for(var i = 0; i < keys.length; i++){
				var key = keys[i];
				value = value[key];
				if(value == null) return null;
			}
			return value;
		}
		else{
			for(var i = 0 ; (i+1) < arguments.length; i=i+2){
				this.current.cache[arguments[i]] = arguments[i+1];
			}
			return this.current.cache;
		}
	},
	
	// Merges a value in the cache with an existing one.  The value is expected to be an object/hash.
	merge: function(key, value){
		if(key != null && value != null){
			var val = this.cache(key);
			if(val != null ){
				value = $.extend(val, value);
			}
			this.cache(key, value);
			return value;
		}
		return null;
	},
	
	on: function(type, selector, method){
		this.content.on(type, selector, method);
	}
};
