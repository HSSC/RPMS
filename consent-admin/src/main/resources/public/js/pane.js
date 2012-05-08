var PaneManager = {
		// Context Controllers.
		setContext: function(x){this.context = x},
		
		// Creates the URL to be used for JSON requests
		getUrl: function(x, ps){
			if(this.context != null && x.indexOf(this.context) != 0){
				x = this.context + x;
			}
			if(ps != null && !$.isEmptyObject(ps)){
				x = x + "?" + $.param(ps);
			}
			return x;
		},
		
		// Pane Controllers
		panes: [],
		current: null,
		content: null, 
		
		hasPanes: function(){return this.panes.length > 0 && this.current != null;},
		
		reset: function(){
			this.panes = [];
			this.current = null;
			this.content.empty();
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
			buts[ol] = of;
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
			}
		},
		
		// Starts a new stack of panes.  Clears all existing panes out and starts a new stack.
		stack: function(url, params, options){
			if(this.hasPanes()  && this.hasModifiedState()){
				settings = {};
				settings["onok"] = function(){
						options.onpreload = function(){PaneManager.reset();};
						PaneManager.push(url, params, options)
					};
				settings["message"] = this.text.pane.confirm$reset;
				this.confirm(settings);
			}
			else{
				this.push(url, params, options);
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
			var pane = {
					request: request
			};
			if(request.options.onpreload){
				request.options.onpreload();
			}
			
			$(content).html(data);
		},
		
		// Processes a failed request for a pane.
		onfailure: function(data, request, status, xhr){
			alert("Failed: " + data);
		},
		
		// Removes the current pane from the stack, making the previous pane the current one.
		pop: function(options){alert(this.getUrl(x))},
		
		// Refreshes the current pane in the stack.
		refresh: function(options){alert(this.getUrl(x))}
		
};
