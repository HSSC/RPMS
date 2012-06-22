// Initialize the logout dialog
$(function(){
	PaneManager.initContent($("#content"));

	// Register Loaders - Kicked Off Every Time Content Is Loaded.
	// Register a loader to make TabControls.
	PaneManager.loader(function(content){
		var target = content.find(".tabcontrol");
		if(target.length > 0){
			var index = RPMS.Tab.getFirstSelected(target);
			target.tabs({show: function(e, ui){
				var panel = $(ui.panel);
				Utils.Size.refill(panel.parent());
				RPMS.Tab.cacheSelected(target, ui);
			},
			selected: index});
		}
	});
	
	// Register a loader that grows/shrinks elements accordingly.
	PaneManager.loader(Utils.Size.refill);
	
	// Register Events	
	// Register Event - Click Select List Item
	PaneManager.on("click", ".selectlistitem", function(event){
		var target = RPMS.findTarget(event, ".selectlistitem");
		var data = Utils.DataSet.getObject(target, "data-item");
		PaneManager.cache("selected", data);
		target.parent().children().removeClass("selected");
		target.addClass("selected");
	});

	// Register Event - Click Action Select List Item
	PaneManager.on("click", ".actionlistitem", function(event){
		var target = RPMS.findTarget(event, ".actionlistitem");
		target.parent().children().removeClass("selected");
		target.addClass("selected");
	});
	
	// Register Event - Double Click Select List Item
	PaneManager.on("dblclick", ".selectlistitem", function(event){
		var target = RPMS.findTarget(event, ".selectlistitem");
		var action = Utils.DataSet.get(target, "data-action");
		if(action != null){
			var data = Utils.DataSet.getObject(target, "data-item");
			PaneManager.cache("selected", data);
			target.parent().children().removeClass("selected");
			target.addClass("selected");
			RPMS.Action.doAction(action);
		}
	});

	// Register Event - Click Generic Push Action
	PaneManager.on("click", ".push-action", function(event){
		var target = RPMS.findTarget(event, "div.push-action");
		var url = Utils.DataSet.get(target, "data-url");
		var params = RPMS.getParamMap(target, "data-map");
		var confirm = Utils.DataSet.getObject(target, "data-confirm");
		RPMS.verify(target, function(){
			RPMS.Action.doPush(url, params, confirm);
		});
	});

	// Register Event - Click Generic Push Action
	PaneManager.on("click", ".ajax-action", function(event){
		var target = RPMS.findTarget(event, "div.ajax-action");
		
		var method = Utils.DataSet.get(target, "data-method");
		var url = Utils.DataSet.get(target, "data-url");
		var params = RPMS.getParamMap(target, "data-map");
		var fullUrl = Utils.Url.render(url, params);
		
		var confirm = Utils.DataSet.getObject(target, "data-confirm");
		var includeData = Utils.DataSet.getBoolean(target, "data-include-data");
		var actionOnSuccess = Utils.DataSet.get(target, "data-action-on-success");

		RPMS.verify(target, function(){
			RPMS.Action.doAjax(method, fullUrl, includeData ? RPMS.getDataMapString() : null, 
					confirm, {onsuccess: actionOnSuccess});
		});
	});
	
	// Register Event - Click Back Button
	PaneManager.on("click", ".back-action", function(event){
		var target = RPMS.findTarget(event, "div.back-action");
		var params = RPMS.getParamMap(target, "data-map");
		PaneManager.pop(params);
	});

	// Register Event - Click Generic Push List Action
	PaneManager.on("click", ".push-listaction", function(event){
		var target = RPMS.findTarget(event, "div.push-listaction");
		var onselect = Utils.DataSet.getBoolean(target, "data-onselect");
		var selected = target.parent().siblings().children(".selected");
		if(!onselect || selected.length > 0){
			var item = $(selected[0]);
			var url = Utils.DataSet.get(target, "data-url");
			var params = RPMS.getParamMap(target, "data-map");
			if(onselect){
				var sparams = RPMS.getParamMap($(selected[0]), "data-item");
				params = $.extend(params, sparams);
			}
			var confirm = Utils.DataSet.getObject(target, "data-confirm");
			RPMS.Action.doPush(url, params, confirm);
		}
	});

	// Register Event - Click Generic Push List Action
	PaneManager.on("click", ".ajax-listaction", function(event){
		var target = RPMS.findTarget(event, "div.ajax-listaction");
		var onselect = Utils.DataSet.getBoolean(target, "data-onselect");
		var selected = target.parent().siblings().children(".selected");
		if(!onselect || selected.length > 0){
			var item = $(selected[0]);
			var method = Utils.DataSet.get(target, "data-method");
			var url = Utils.DataSet.get(target, "data-url");
			var params = RPMS.getParamMap(target, "data-map");
			if(onselect){
				var sparams = RPMS.getParamMap($(selected[0]), "data-item");
				params = $.extend(params, sparams);
			}
			var fullUrl = Utils.Url.render(url, params);
			var confirm = Utils.DataSet.getObject(target, "data-confirm");
			var includeData = Utils.DataSet.getBoolean(target, "data-include-data");
			var actionOnSuccess = Utils.DataSet.get(target, "data-action-on-success");
			RPMS.Action.doAjax(method, fullUrl, includeData ? RPMS.getDataMapString() : null, 
					confirm, {onsuccess: actionOnSuccess});
		}
	});
});

var RPMS = {
	verify: function(target, action){
		var verify = Utils.DataSet.getObject(target, "data-verify", null);
		if(verify == null || RPMS.Action.doAction(verify.action, target)){
			action();
		}
		else{
			Dialog.inform(verify);
		}
	},
	
	findTarget: function(event, selector){
		var element = $(event.target);
		if(element.filter(selector).length > 0){
			return element.first();
		}
		else{
			return element.parents(selector).first();
		}
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
		var map = Utils.DataSet.getObject(element, attr);
		if(map){
			return RPMS.mapToParams(map);
		}
		return {};
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
			var origValue = Utils.DataSet.get(e, "data-value");
			var value = null;
			var tag = e.tagName.toLowerCase();
			if(tag == "input" && e.type.toLowerCase() == "checkbox"){
				var checkVal = Utils.DataSet.get(e, "data-checked-value", true);
				var uncheckVal = Utils.DataSet.get(e, "data-unchecked-value", false);
				if(e.checked){
					value = checkVal;
				}
				else{
					value = uncheckVal;
				}
			}
			else if(tag == "select"){
				var vals = [];
				var options = $(e).children("option:selected");
				if(options != null){
					options.each(function(index, e){
						var item = Utils.DataSet.getObject(e, "data-item");
						if(item == null){
							item = e.value;
						}
						vals.push(item);
					});
				}
				if(Utils.Element.getBooleanAttribute(e, "multiple", false)){
					value = vals;
				}
				else{
					value = vals.length > 0 ? vals[0] : null;
				}
			}
			else{
				value = $(e).val();
			}
			if(changes != true || value != origValue){
				data[name] = value;
			}
		});
		return data;
	},
	getDataMapString:function(changes){
		return JSON.stringify(this.getDataMap(changes));
	},
	responseMessage: function(xhr, ifnull){
		if(xhr && xhr.responseText){
			try{
				var body = $.parseJSON(xhr.responseText);
				if(body && body.message){
					return body.message;
				}
			}
			catch(e){}
			return xhr.responseText;
		}
		return ifnull;
	}
};

RPMS.Action = {
	actions: {
		selected: function(){
			if(PaneManager.cache("selected") != null){
				return true;
			}
			else{
				return false;
			}
		}
	},
	confirmAction: function(action, confirm){
		if(confirm != null){
			Dialog.confirm({title: confirm.title, 
				message: confirm.message, 
				onconfirm: action,
				oncancel: Dialog.Progress.end});
		}
		else{
			action();
		}
	},
	doPush: function(url, params, confirm){
		var action = function(){
			Dialog.Progress.start();
			PaneManager.push(url, params, {postpop: function(o,p,c){c.refresh()}});
		}
		this.confirmAction(action, confirm);
	},
	doAjax: function(method, url, body, confirm, actions, options){
		var settings = {
				data: body ,
				type: method,
				dataType: "text",
				processData: false, 
				contentType: "application/json",
				success: function(data, status, xhr){
					Dialog.Progress.end();
					if(actions != null && actions.onsuccess != null){
						RPMS.Action.doAction(actions.onsuccess);
					}
				},
				error: function(xhr, status, text){
					Dialog.Progress.end();
					var message = RPMS.responseMessage(xhr, "Failed to save data.");
					Dialog.inform({title: "Error Encountered", message: message});
				}
		}
		if(options != null){
			settings = $.extend(settings, options);
		}
		var action = function(){
			Dialog.Progress.start();
			$.ajax(url, settings);
		}
		this.confirmAction(action, confirm);
	},
	doAction: function(action, target){
		var parms = action.split("::");
		action = parms.shift();
		for(var i = 0; i < parms.length; i++){
			var parm = parms[i];
			if(parm.indexOf("[") == 0 || parm.indexOf("{") == 0){
				parms[i] = JSON.parse(parm);
			}
		}
		if(RPMS.Action.actions[action] != null){
			return RPMS.Action.actions[action](parms, target);
		}
		
		if(PaneManager[action] != null){
			return PaneManager[action](parms);
		}
		
		var buttons = PaneManager.current.pane.find(action);
		if(buttons.length > 0){
			return buttons.trigger("click");
		}
	}
};
RPMS.List = {
	getParams: function(action, selected){
		return {};
	}
};
RPMS.Tab = {
	cacheKey: "currenttabs",
	cacheSelected: function(target, ui){
		var id = Utils.Element.getAttribute(target, "id");
		if(id == null) return;
		var current = PaneManager.cache(RPMS.Tab.cacheKey);
		if(current == null){
			PaneManager.cache(RPMS.Tab.cacheKey, [{target: id, ui: ui}]);
		}
		else{
			for(var i = 0; i < current.length; i++){
				if(current[i].target == id){
					current[i].ui = ui;
					return;
				}
			}
			current.push({target: id, ui: ui});
		}
	},
	getFirstSelected: function(target){
		var index = 0;
		var id = Utils.Element.getAttribute(target, "id");
		if(id != null){
			var current = PaneManager.cache(RPMS.Tab.cacheKey);
			if(current != null){
				for(var i = 0; i < current.length; i++){
					index = current[i].ui.index;
				}
			}
		}
		return index;
	}
};