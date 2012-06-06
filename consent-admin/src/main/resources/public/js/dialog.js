var Dialog = {
	inform: function(options){
		if(this.informDialog == null){
			this.informDialog = $("<div class='dialog' />");
			this.informDialog.appendTo($("body"));
		}
		
		// Get Options
		var message = Utils.Map.mapped$rq(options, "message");
		var title = Utils.Map.mapped(options, "title", "Information");
		var closeLabel = Utils.Map.mapped(options, "close", "Close");
		var height = Utils.Map.mapped(options, "height", "auto");
		
		// Set Text
		this.informDialog.text(message);

		var buts = {};
		buts[closeLabel] = function (){$(this).dialog( "close" ); if(options.onclose) options.onclose();};
		this.informDialog.dialog({
			title: title,
			resizable: false,
			height: height,
			modal: true,
			buttons: buts
		});
	},
	error: function(options){
		Dialog.inform(options);
	},
	confirm: function(options){
		if(this.confirmDialog == null){
			this.confirmDialog = $("<div class='dialog' />");
			this.confirmDialog.appendTo($("body"));
		}
		var message = Utils.Map.mapped$rq(options, "message");
		var title = Utils.Map.mapped(options, "title", "Confirm");
		
		var confirmLabel = Utils.Map.mapped(options, "confirm", "Proceed");
		var cancelLabel = Utils.Map.mapped(options, "cancel", "Cancel");
		
		var height = Utils.Map.mapped(options, "height", "auto");
		
		this.confirmDialog.text(message);
		var buts = {};
		buts[confirmLabel] = function (){$(this).dialog( "close" ); if(options.onconfirm) options.onconfirm();};
		buts[cancelLabel] = function (){$(this).dialog( "close" ); if(options.oncancel) options.oncancel();};
		this.confirmDialog.dialog({
			title: title,
			resizable: false,
			height: height,
			modal: true,
			buttons: buts
		});
	}		
};

Dialog.LightBox={
	start: function(){
		if(this.lightBox == null){
			this.lightBox = $("<div class='ui-widget-overlay' />");
			$("body").append(this.lightBox);
		}
		this.lightBox.show();
	},
	end: function(){
		if(this.lightBox != null){
			this.lightBox.hide();
		}
	}
};

Dialog.Progress = {
	start: function(){
		Dialog.LightBox.start();
		if(this.progress == null){
			var imgUrl = Utils.Url.render("/image/loader.gif");
			this.progress = $("<img src='" + imgUrl + "' class='progress' />");
			$("body").append(this.progress);
		}
		this.progress.show();
		this.progress.position({of: Dialog.LightBox.lightBox});
	},
	end: function(){
		this.progress.hide();
		Dialog.LightBox.end();
	}
};
