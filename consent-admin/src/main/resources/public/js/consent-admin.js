// Initialize the logout dialog
$(function(){
	PaneManager.initDialog($("#dialog"));
	PaneManager.initContent($("#content"));
	
	// Register Events
	// Register Event - Click Details Actions
	PaneManager.on("click", ".details-action", function(event){
		var target = RPMS.findTarget(event, "div.details-action");
		var url = RPMS.get(target, "data-url");
		var params = RPMS.getParamMap(target, "data-map");
		PaneManager.push(url, params, {postpop: function(o,p,c){c.refresh()}});
	});
	
	// Register Event - Click New Action
	PaneManager.on("click", ".new-action", function(event){
		var target = RPMS.findTarget(event, "div.new-action");
		var url = RPMS.get(target, "data-url");
		var params = RPMS.getParamMap(target, "data-map");
		PaneManager.push(url, params, {postpop: function(o,p,c){c.refresh()}});
	});
	
	// Register Event - Click New Action
	PaneManager.on("click", ".save-action", function(event){
		var target = RPMS.findTarget(event, "div.save-action");
		var url = RPMS.get(target, "data-url");
		var params = RPMS.getParamMap(target, "data-map");
		var hold = RPMS.get(target, "data-holdonsuccess");
		var fullUrl = PaneManager.getUrl(url, params, false);
		var method = RPMS.get(target, "data-method");
		var body = RPMS.getDataMap();
		
		var settings = {
				data: body,
				type: method,
				dataType: "text",
				success: function(data, status, xhr){
					RPMS.endProgress();
					PaneManager.cache("changed", true);
					if(hold == null){
						PaneManager.current.pane.find(".done-action").trigger("click");
					}
				},
				error: function(xhr, status, text){
					RPMS.endProgress();
					var message = RPMS.responseMessage(xhr, "Failed to save data.");
					RPMS.inform({title: "Error Encountered", message: message});
				}
		}
		RPMS.startProgress();
		$.ajax(fullUrl, settings);
	});
	
	// Register Event - Click Delete Actions
	PaneManager.on("click", ".delete-action", function(event){
		var target = RPMS.findTarget(event, "div.delete-action");
		var url = RPMS.get(target, "data-url");
		var params = RPMS.getParamMap(target, "data-map");
		
		var hold = RPMS.get(target, "data-holdonsuccess");
		var fullUrl = PaneManager.getUrl(url, params, false);
		
		var confirmmsg = RPMS.get(target, "data-confirm");
		
		var settings = {
				type: "delete",
				dataType: "text",
				success: function(data, status, xhr){
					RPMS.endProgress();
					PaneManager.cache("changed", true);
					if(hold == null){
						PaneManager.current.pane.find(".done-action").trigger("click");
					}
				},
				error: function(xhr, status, text){
					RPMS.endProgress();
					var message = RPMS.responseMessage(xhr, "Failed to delete data.");
					RPMS.inform({title: "Error Encountered", message: message});
				}
		}
		RPMS.startProgress();
		RPMS.confirm({title: "Confirm Delete", 
			message: confirmmsg || "Are you sure you want to delete this item?", 
			onconfirm: function(){$.ajax(fullUrl, settings)}});
	});
	
	// Register Event - Click Done Button
	PaneManager.on("click", ".done-action", function(event){
		var target = RPMS.findTarget(event, "div.done-action");
		var params = RPMS.getParamMap(target, "data-map");
		PaneManager.pop(params);
	});
	
	// Register Event - Select List Item
	PaneManager.on("click", ".selectlistitem", function(event){
		var target = RPMS.findTarget(event, ".selectlistitem");
		var data = RPMS.getObject(target, "data-item");
		PaneManager.cache("selected", data);
		target.parent().children().removeClass("selected");
		target.addClass("selected");
	});
});

var RPMS = {
	findTarget: function(event, selector){
		var element = $(event.target);
		if(element.filter(selector).length > 0){
			return element.first();
		}
		else{
			return element.parents(selector).first();
		}
	},
	getObject: function(element, attribute){
		var value = RPMS.get(element, attribute);
		if(value != null && typeof value == 'string'){
			return $.parseJSON(value);
		}
		return value;
	},
	
	get: function(target, attribute, defaultValue){
		if(attribute.indexOf("data-") != 0){
			attribute = "data-" + attribute;
		}
		var value = null;
		if(target.getAttribute){
			value = target.getAttribute(attribute);
		}
		else if(target.attr){
			value = target.attr(attribute);
		} 
		if(value == null){
			return defaultValue;
		}
		return value;
	},
	
	mapToParams: function(map){
		var params = {};
		if(map){
			for(var key in map){
				var val = PaneManager.cache(map[key]);
				if(val != null){
					params[key] = val;
				}
				else{
					params[key] = map[key];
				}
			}
		}
		return params;
	},
	getParamMap: function(element, attr){
		var map = RPMS.getObject(element, attr);
		if(map){
			return RPMS.mapToParams(map);
		}
		return PaneManager.cache();
	},
	getForm: function(){
		return PaneManager.current.pane.find("form").first();
	},
	getDataMap: function(changes){
		var data = {};
		var form = RPMS.getForm();
		var inputs = form.find(":input");
		inputs.each(function(i, e){
			var name = e.name;
			var origValue = RPMS.get(e, "data-value");
			var value = null;
			if(e.tagName.toLowerCase() == "input" && e.type.toLowerCase() == "checkbox"){
				var checkVal = RPMS.get(e, "data-checked-value", true);
				var uncheckVal = RPMS.get(e, "data-checked-value", false);
				if(e.checked){
					value = checkVal;
				}
				else{
					value = uncheckVal;
				}
			}
			else{
				value = $(e).val();
			}
			if(changes != true || value != origValue){
				data[name] = value;
			}
		});
		
		//var elements = form.serializeArray();
		//for(var i = 0; i < elements.length; i++){
		//	var e = elements[i]
		//	data[e.name] = e.value;
		//}
		return data;
	},
	inform: function(options){
		if(this.informDialog == null){
			this.informDialog = $("<div class='dialog' />");
			this.informDialog.appendTo($("body"));
		}
		this.informDialog.text(options.message);
		var buts = {};
		buts["Close"] = function (){$(this).dialog( "close" ); if(options.onclose) options.onclose();};
		this.informDialog.dialog({
			title: options.title,
			resizable: false,
			modal: true,
			buttons: buts
		});
	},
	confirm: function(options){
		if(this.confirmDialog == null){
			this.confirmDialog = $("<div class='dialog' />");
			this.confirmDialog.appendTo($("body"));
		}
		this.confirmDialog.text(options.message);
		var buts = {};
		buts["Proceed"] = function (){$(this).dialog( "close" ); if(options.onconfirm) options.onconfirm();};
		buts["Cancel"] = function (){$(this).dialog( "close" ); if(options.oncancel) options.oncancel();};
		this.confirmDialog.dialog({
			title: options.title,
			resizable: false,
			modal: true,
			buttons: buts
		});
	},
	startProgress: function(){
		this.pause();
		if(this.progress == null){
			var imgUrl = PaneManager.getUrl("/image/loader.gif", {}, false);
			this.progress = $("<img src='" + imgUrl + "' class='progress' />");
			$("body").append(this.progress);
		}
		this.progress.show();
		this.progress.position({of: this.lightBox});
	},
	endProgress: function(){
		this.progress.hide();
		this.continue();
	},
	pause: function(){
		if(this.lightBox == null){
			this.lightBox = $("<div class='ui-widget-overlay' />");
			$("body").append(this.lightBox);
		}
		this.lightBox.show();
	},
	continue: function(){
		if(this.lightBox != null){
			this.lightBox.hide();
		}
	},
	responseMessage: function(xhr, ifnull){
		if(xhr && xhr.responseText){
			var body = $.parseJSON(xhr.responseText);
			if(body && body.message){
				return body.message;
			}
		}
		return ifnull;
	}
}
