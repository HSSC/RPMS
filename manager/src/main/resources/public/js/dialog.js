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
	choose: function(options){
		if(this.chooseDialog == null){
			this.chooseDialog = $("<div class='dialog' />");
			this.chooseDialog.appendTo($("body"));
			this.chooseSelect = $("<select class='dialog-choose' />")
			this.chooseSelect.appendTo(this.chooseDialog);
		}
		
		this.chooseSelect.empty();
		this.chooseSelect.val("");
		var select = this.chooseSelect;
		
		var items = Utils.Map.mapped$rq(options, "items");
		$.each(items, function(i,o){
			var opt = $("<option value='" + o.value + "'>" + (o.label || o.value) + "</option>");
			opt.appendTo(select);
			opt.data("data", (o.data || o));
			if(o.selected){
				opt.attr("selected", "selected");
			}
		});
		
		var title = Utils.Map.mapped(options, "title", "Choose");
		var multiple = Utils.Map.mapped(options, "multiple", false);
		if(multiple){
			select.attr("multiple", "multiple");
			select.attr("size", 4);
		}
		else{
			select.attr("multiple", false);
			select.attr("size", 1);
		}
		
		var chooseLabel = Utils.Map.mapped(options, "choose", "Choose");
		var cancelLabel = Utils.Map.mapped(options, "cancel", "Cancel");
		var height = Utils.Map.mapped(options, "height", "auto");
		
		var selected = function(){
			var sels = [];
			select.children("option:selected").each(function(){
				sels.push($(this).data("data"))});
			if(multiple){
				return sels;
			}
			if(sels.length == 0){
				return null;
			};
			return sels[0];
		};
		var buts = {};
		buts[chooseLabel] = function (){$(this).dialog( "close" ); if(options.onchoose) options.onchoose(selected());};
		buts[cancelLabel] = function (){$(this).dialog( "close" ); if(options.oncancel) options.oncancel();};
		this.chooseDialog.dialog({
			title: title,
			resizable: false,
			height: height,
			modal: true,
			buttons: buts
		});
	},
	
	order: function(options){
		if(this.orderDialog == null){
			this.orderDialog = $("<div class='dialog' />");
			this.orderDialog.appendTo($("body"));
			this.orderTable = $("<table class='dialog-order' />")
			this.orderTable.appendTo(this.orderDialog);
			this.orderTable.click(function(event){
				var img = $(event.target);
				var row = img.closest("tr");
				if(event.target.name == "up"){
					 row.insertBefore(row.prev());
				}
				else{
					row.insertAfter(row.next());
				}
			});
		}
		
		this.orderTable.empty();
		var table = this.orderTable;
		var items = Utils.Map.mapped$rq(options, "items");
		$.each(items, function(i, item){
			item.originalIndex = i;
			var row = $(table[0].insertRow(i));
			row.data("item", item);
			$("<td class='dialog-order-label'>" + item.label + "</td>").appendTo(row);
			$("<td class='dialog-order-action'><img name='up' src='" + 
					Utils.Url.render("/image/up.png") + "'/></td>").appendTo(row);
			$("<td class='dialog-order-action'><img name='down' src='" + 
					Utils.Url.render("/image/down.png") + "'/></td>").appendTo(row);
		});
		
		var title = Utils.Map.mapped(options, "title", "Order Items");
		var height = Utils.Map.mapped(options, "height", "auto");
		
		var okLabel = Utils.Map.mapped(options, "ok", "OK");
		var cancelLabel = Utils.Map.mapped(options, "cancel", "Cancel");
		
		var orderedItems = function(){
			var items = [];
			var rows = table[0].rows;
			$.each(rows, function(i,r){
				var item = $(r).data("item");
				item.newIndex = i;
				items.push(item);
			});
			return items;
		};
		
		var buts = {};
		buts[okLabel] = function (){$(this).dialog( "close" ); if(options.onok) options.onok(orderedItems());};
		buts[cancelLabel] = function (){$(this).dialog( "close" ); if(options.oncancel) options.oncancel();};
		this.orderDialog.dialog({
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
		
		var paragraphs = options.paragraphs == true ? true : false;
		var current = options.current;
		var currentText = current != null ? current.value : null;
		var originalText = null;
		var originalTextArray = null;
		if(currentText instanceof Array){
			originalTextArray = currentText;
			originalText = Dialog.Utils.arrayToText(currentText);
		}
		else{
			originalText = currentText;
			originalTextArray = Dialog.Utils.textToArray(currentText);
		}

		var currentLang = current != null ? current.language : null;
		var defaultLang = (options.defaultLanguage || {});
		var languages = options.languages;
		this.textLanguage.empty();
		this.textText.val("");
		
		if(current != null){
			this.textText.val(originalText);
			var option = new Option(currentLang.name, currentLang.code, true, true);
			$(option).data("language", currentLang);
			this.textLanguage.append(option);
		}
		else{
			var first = languages.length > 0 ? languages[0] : null;
			for(var i = 0; i < languages.length; i++){
				var lang = languages[i];
				var option = null;
				if(lang.id == defaultLang.id){
					first = null;
					option = new Option(lang.name, lang.code, true, true);
				} 
				else{
					option = new Option(lang.name, lang.code, false, false);
				}
				$(option).data("language", lang);
				this.textLanguage.append(option);
			}
			if(first) first.selected == true;
		}
		
		var title = Utils.Map.mapped(options, "title", "Provide Text");
		var confirmLabel = Utils.Map.mapped(options, "confirm", "OK");
		var cancelLabel = Utils.Map.mapped(options, "cancel", "Cancel");
		
		var buts = {};
		buts[confirmLabel] = function (){
				$(this).dialog( "close" );
				var text = textArea.val();
				var option = langSelect.children("option:selected");
				if((current == null && text != null) || text != originalText){
					if(options.onchange){
						options.onchange(current, paragraphs ? Dialog.Utils.textToArray(text) : text, option.data("language"));
					}
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
		if(this.progress){
			this.progress.hide();
		}
		Dialog.LightBox.end();
	}
};

Dialog.Utils = {
	validText: function(text){
		if(text != null && typeof text == "string" && text.trim().length > 0){
			return true;
		}
		return false;
	},
	textToArray: function(text){
		if(text != null){
			var items = text.split("\n");
			for(var i = items.length - 1; i >= 0; i--){
				if(!Dialog.Utils.validText(items[i])){
					items.splice(i, 1);
				}
			}
			return items;
		}
		return null;
	},
	arrayToText: function(texts){
		if(texts instanceof Array){
			var text = "";
			for(var i = 0; i < texts.length; i++){
				if(Dialog.Utils.validText(texts[i])){
					text = text + texts[i] + "\n\n";
				}
			}
			return text;
		}
		return null;
	}
}