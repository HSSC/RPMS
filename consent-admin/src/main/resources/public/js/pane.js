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
		
		getUrl: function(x, ps){
			if(this.basepath != null && x.indexOf(this.basepath) != 0){
				x = this.basepath + x;
			}
			if(ps != null && !$.isEmptyObject(ps)){
				x = x + "?" + $.param(ps);
			}
			return x;
		},
		
		// Pane References
		current: null,
		content: null, // This Should Be Set Externally
		
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
			url = this.context + "/logout";
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
			nothing: function(){}
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
					error: function(data, status, xhr){
						PaneManager.onfailure(data, request, status, xhr);
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
			var pane = {request: request};
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
		
		// Processes a failed request for a pane.
		onfailure: function(data, request, status, xhr){
			alert("Failed: " + data);
		},
		
		// Removes the current pane from the stack, making the previous pane the current one.
		pop: function(options){
			var poppane = this.current;
			this.current = null;
			if(poppane){
				// Define The Post Hide Current Function
				var postHide = function(){
					poppane.pane.remove();
				};
				poppane.pane.hide("slide", {direction: "right"}, this.settings.duration, postHide);
				
				// Define The Show Previous Function
				var previous = poppane.previous;
				if(previous){
					this.current = previous;
					previous.pane.show("slide", {direction: "left"}, this.settings.duration);
				}
			}
		},
		
		// Refreshes the current pane in the stack.
		refresh: function(options){alert(this.getUrl(x))}
		
};
