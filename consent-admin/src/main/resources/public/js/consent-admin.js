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
		var holdOnSave = RPMS.get(target, "data-holdonsave");
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
					if(holdOnSave == null){
						PaneManager.current.pane.find(".done-action").trigger("click");
					}
				},
				error: function(data, status, xhr){
					RPMS.endProgress();
					alert("Failed!");
				}
		}
		RPMS.startProgress();
		$.ajax(fullUrl, settings);
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
	
	get: function(element, attribute){
		return element[0].getAttribute(attribute);
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
	getDataMap: function(){
		var form = RPMS.getForm();
		var elements = form.serializeArray();
		var data = {};
		for(var i = 0; i < elements.length; i++){
			var e = elements[i]
			data[e.name] = e.value;
		}
		return data;
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
	}
}