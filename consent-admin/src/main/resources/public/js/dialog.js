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
	},
	text: function(options){
		if(this.textDialog == null){
			this.textDialog = $("<div class='dialog'><select class='language'></select><textarea class='i18ntext' /></div>");
			this.textDialog.appendTo($("body"));
			this.textLanguage = this.textDialog.children("select");
			this.textText = this.textDialog.children("textarea");
		}
		var textArea = this.textText;
		var langSelect = this.textLanguage;
		
		var current = options.current;
		var currentText = current != null ? current.value : null;
		var currentLang = current != null ? current.language : null;
		var languages = options.languages;
		this.textLanguage.empty();
		this.textText.val("");
		
		if(current != null){
			this.textText.val(currentText);
			var option = new Option(currentLang.name, currentLang.code, true, true);
			$(option).data("language", currentLang);
			this.textLanguage.append(option);
		}
		else{
			for(var i = 0; i < languages.length; i++){
				var lang = languages[i];
				var option = null;
				if(currentLang == null){
					currentLang = lang;
					option = new Option(lang.name, lang.code, true, true);
				} 
				else{
					option = new Option(lang.name, lang.code, false, false);
				}
				$(option).data("language", lang);
				this.textLanguage.append(option);
			}
		}
		
		var title = Utils.Map.mapped(options, "title", "Provide Text");
		var confirmLabel = Utils.Map.mapped(options, "confirm", "OK");
		var cancelLabel = Utils.Map.mapped(options, "cancel", "Cancel");
		
		var buts = {};
		buts[confirmLabel] = function (){
				$(this).dialog( "close" );
				var text = textArea.val();
				var option = langSelect.children("option:selected");
				if((current == null && text != null) || text != currentText){
					if(options.onchange) options.onchange(current, text, option.data("language"));
				}
			};
		buts[cancelLabel] = function (){$(this).dialog( "close" );};
		this.textDialog.dialog({
			title: title,
			width: ($(window).width() * .8),
			height: ($(window).height() * .8),
			resizable: true,
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

Dialog.test = {
	onchange: function(current, text, lang){
		var c = current;
		var t = text;
		var l = lang;
	}
};