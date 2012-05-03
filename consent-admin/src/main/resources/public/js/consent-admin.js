var RPMS = {
		// Context Controllers.
		setContext: function(x){this.context = x},
		getUrl: function(x){
			if(this.context != null && x.indexOf(this.context) != 0){
				return this.context + x;
			}
			return x;
		},
		
		// Pane Controllers
		panes: [],
		hasPanes: function(){return this.panes.length > 0;},
		clearPanes: function(){this.panes = []; this.content.empty()},
		hasModifiedState: function(){return this.panes.length > 0;},
		launchInPane: function(x, params){
			var fx = function(){RPMS.clearPanes(); RPMS.nextPane(x, params)};
			if(this.hasPanes()  && this.hasModifiedState()){
				options = {};
				options["onok"] = fx;
				options["message"] = this.text.pane.confirm$reset;
				this.confirm(options);
			}
			else{
				fx();
			}
		},
		nextPane: function(x, params){alert(this.getUrl(x))},
		
		// Text that can be overridden on init.  For
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
			}
		}
};

// Initialize the logout dialog
$(function(){
	RPMS.confirmDialog = $("#dialog");
	RPMS.confirmDialog.hide();
	RPMS.content = $("#content");
});