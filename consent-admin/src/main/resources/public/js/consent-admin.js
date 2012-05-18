// Initialize the logout dialog
$(function(){
	PaneManager.confirmDialog = $("#dialog");
	PaneManager.confirmDialog.hide();
	PaneManager.content = $("#content");
		
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
		var url = PaneManager.getUrl(RPMS.get(target, "data-url"));
		var method = RPMS.get(target, "data-method");
		var params = RPMS.getParamMap(target, "data-map");
		var body = RPMS.getDataMap();
		// Do Ajax
	});
	
	// Register Event - Click New Action
	PaneManager.on("click", ".save-jquery-action", function(event){
		var target = RPMS.findTarget(event, "div.save-jquery-action");
		var url = RPMS.get(target, "data-url");
		var params = RPMS.getParamMap(target, "data-map");
		var fullUrl = PaneManager.getUrl(url, params, false);
		var method = RPMS.get(target, "data-method");
		var form = RPMS.getForm();
		
		var options = {
			url: fullUrl,
			target: "#progress-dialog",
			type: method
		};
		
		form.ajaxSubmit(options);
		$("#progress-dialog").show();
		setTimeout(function() {PaneManager.pop({})}, 1500);
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
		if(form == null || form.length == 0){
			return;
		}
		var data = {};
		var inputs = form.find(":input");
		for(var i = 0; i < inputs.length; i++){
			var input = inputs[i];
			data[input.name] = input.value;
		}
		return data;
	}
}