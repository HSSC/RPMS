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
		var data = Utils.DataSet.getObject(target, "item");
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
		var action = Utils.DataSet.get(target, "action");
		if(action != null){
			var data = Utils.DataSet.getObject(target, "item");
			PaneManager.cache("selected", data);
			target.parent().children().removeClass("selected");
			target.addClass("selected");
			RPMS.Action.doAction(action);
		}
	});

	// Register Event - Click Generic Push Action
	PaneManager.on("click", ".push-action", function(event){
		var target = RPMS.findTarget(event, "div.push-action");
		var url = Utils.DataSet.get(target, "url");
		var params = RPMS.getParamMap(target, "map");
		var confirm = Utils.DataSet.getObject(target, "confirm");
		RPMS.verify(target, function(){
			RPMS.Action.doPush(url, params, confirm);
		});
	});

	// Register Event - Click Generic Push Action
	PaneManager.on("click", ".ajax-action", function(event){
		var target = RPMS.findTarget(event, "div.ajax-action");
		
		var method = Utils.DataSet.get(target, "method");
		var url = Utils.DataSet.get(target, "url");
		var params = RPMS.getParamMap(target, "map");
		var fullUrl = Utils.Url.render(url, params);
		
		var confirm = Utils.DataSet.getObject(target, "confirm");
		var includeData = Utils.DataSet.getBoolean(target, "include-data");
		var actionOnSuccess = Utils.DataSet.get(target, "action-on-success");

		RPMS.verify(target, function(){
			RPMS.Action.doAjax(method, fullUrl, includeData ? RPMS.getDataMapString() : null, 
					confirm, {onsuccess: actionOnSuccess});
		});
	});
	
	// Register Event - Click Back Button
	PaneManager.on("click", ".back-action", function(event){
		var target = RPMS.findTarget(event, "div.back-action");
		var params = RPMS.getParamMap(target, "map");
		PaneManager.pop(params);
	});

	// Register Event - Click Generic Push List Action
	PaneManager.on("click", ".push-listaction", function(event){
		var target = RPMS.findTarget(event, "div.push-listaction");
		var onselect = Utils.DataSet.getBoolean(target, "onselect");
		var selected = target.parent().siblings().children(".selected");
		if(!onselect || selected.length > 0){
			var item = $(selected[0]);
			var url = Utils.DataSet.get(target, "url");
			var params = RPMS.getParamMap(target, "map");
			if(onselect){
				var sparams = RPMS.getParamMap($(selected[0]), "item");
				params = $.extend(params, sparams);
			}
			var confirm = Utils.DataSet.getObject(target, "confirm");
			RPMS.Action.doPush(url, params, confirm);
		}
	});

	// Register Event - Click Generic Push List Action
	PaneManager.on("click", ".ajax-listaction", function(event){
		var target = RPMS.findTarget(event, "div.ajax-listaction");
		var onselect = Utils.DataSet.getBoolean(target, "onselect");
		var selected = target.parent().siblings().children(".selected");
		if(!onselect || selected.length > 0){
			var item = $(selected[0]);
			var method = Utils.DataSet.get(target, "method");
			var url = Utils.DataSet.get(target, "url");
			var params = RPMS.getParamMap(target, "map");
			if(onselect){
				var sparams = RPMS.getParamMap($(selected[0]), "item");
				params = $.extend(params, sparams);
			}
			var fullUrl = Utils.Url.render(url, params);
			var confirm = Utils.DataSet.getObject(target, "confirm");
			var includeData = Utils.DataSet.getBoolean(target, "include-data");
			var actionOnSuccess = Utils.DataSet.get(target, "action-on-success");
			RPMS.Action.doAjax(method, fullUrl, includeData ? RPMS.getDataMapString() : null, 
					confirm, {onsuccess: actionOnSuccess});
		}
	});
	
	// Register Event - Click Add Image On i18n Text Control
	PaneManager.on("click", ".i18ntext-add", function(event){
		var target = RPMS.findTarget(event, "div.i18ntext");
		var editable = Utils.DataSet.getBoolean(target, "editable");
		if(editable){
			var langs = Utils.DataSet.getObject(target, "languages");
			var defaultLang = Utils.DataSet.getObject(target, "defaultlanguage");
			langs = [].concat(langs);
			var rows = target.find("tr");
			for(var r = 0; r < rows.length; r++){
				var text = Utils.DataSet.getObject(rows[r], "text");
				if(text != null){
					for(var l = (langs.length - 1); l >= 0; l--){
						if(text.language.code == langs[l].code){
							langs.splice(l, 1);
							break;
						}
					}
				}
			}
			if(langs.length == 0){
				Dialog.inform({message: "All of the available languages have text associated with them."});
				return;
			}
			var url = Utils.DataSet.get(target, "url");
			var params = Utils.DataSet.get(target, "params");
			var onchange = function(orig, text, lang){
				var item = {value: text, language: lang};
				
				var insertRow = function(x){
					var val = x.value != null ? x.value.join("</br>") : "" ;
					var row = $("<tr class='i18ntext'><td class='i18ntext-lang'>" + x.language.name + "</td>" +
							"<td class='i18ntext-text'>" + val + 
							"</td><td class='i18ntext-action'><img class='i18ntext-edit' src='" +
							Utils.Url.render("/image/edit.png") + "' /><img class='i18ntext-delete' src='" + 
							Utils.Url.render("/image/delete.png") + "' /></td></tr>");
					row.appendTo(target.children("table"))
					Utils.DataSet.set(row, "text", x);
				};
				
				if(url != null){
					var fullUrl = Utils.Url.render(url, params);
					RPMS.Action.doAjax("put", fullUrl, JSON.stringify(item), null, null, {success: function(data, status, xhr){
						var item = $.parseJSON(data);
						insertRow(item);
						Dialog.Progress.end();
					}});
				}
				else{
					var data = Utils.DataSet.get(target, "value", []);
					data.push(item);
					Utils.DataSet.set(target, "value", data);
					Utils.DataSet.set(target, "changed", true);
					insertRow(item);
				}
			}
			Dialog.text({languages: langs, defaultLanguage: defaultLang, onchange: onchange});
		}
	});
	
	// Register Event - Click Delete Image On i18n Text Control
	PaneManager.on("click", ".i18ntext-delete", function(event){
		var target = RPMS.findTarget(event, "div.i18ntext");
		var editable = Utils.DataSet.getBoolean(target, "editable");
		if(editable){
			var row = RPMS.findTarget(event, "tr");
			var text = Utils.DataSet.getObject(row, "text");
			var url = Utils.DataSet.get(target, "url");
			var params = Utils.DataSet.getObject(target, "params", {});
			if(url != null){
				params["text-i18n"] = text.id;
				var fullUrl = Utils.Url.render(url, params);
				var confirm = Utils.DataSet.getObject(target, "confirm");
				RPMS.Action.doAjax("delete", fullUrl, null, confirm, null, {success: function(data, status, xhr){
					row.remove();
					Dialog.Progress.end();
				}});
			}
			else{
				var texts = Utils.DataSet.getObject(target, "value", []);
				for(var t = (texts.length - 1); t >= 0; t--){
					if(text.language.code == texts[t].language.code){
						var node = texts.splice(t, 1)[0];
						if(node.id != null){
							var cache = Utils.DataSet.getObject(target, "deleted", []);
							cache.push(node);
							Utils.DataSet.set(target, "deleted", cache);
							Utils.DataSet.getObject(target, "updated",[]).remove(node);
						}
						break;
					}
				}
				Utils.DataSet.set(target, "value", texts);
				Utils.DataSet.set(target, "changed", true);
				row.remove();
			}
		}
	});
	
	// Register Event - Click Delete Image On i18n Text Control
	PaneManager.on("click", ".i18ntext-edit", function(event){
		var target = RPMS.findTarget(event, "div.i18ntext");
		var editable = Utils.DataSet.getBoolean(target, "editable");
		if(editable){
			var langs = Utils.DataSet.getObject(target, "languages");
			var row = RPMS.findTarget(event, "tr");
			var data = Utils.DataSet.getObject(row, "text");
			var url = Utils.DataSet.get(target, "url");
			var params = Utils.DataSet.getObject(target, "params", {});
			
			var updateText = function(text){
				data.value = text;
				var val = text != null ? text.join("</br>") : "" ;
				var cell = row.children("td.i18ntext-text");
				cell.html(val);
				var texts = Utils.DataSet.getObject(target, "value", []);
				for(var t = (texts.length - 1); t >= 0; t--){
					var node = texts[t];
					if(data.language.code == node.language.code){
						node.value = text;
						if(node.id != null){
							var cache = Utils.DataSet.getObject(target, "updated", []);
							if(!cache.contains(node)){
								cache.push(node);
								Utils.DataSet.set(target, "updated", cache);
							}
						}
						break;
					}
				}
			}

			var onchange = function(orig, text, lang){
				if(url != null){
					params["text-i18n"] = data.id;
					var fullUrl = Utils.Url.render(url, params);
					RPMS.Action.doAjax("post", fullUrl, JSON.stringify({value: text}), null, null, {success: function(data, status, xhr){
						updateText(text);
						Dialog.Progress.end();
					}});
				}
				else{
					updateText(text);
				}
			}
			Dialog.text({current: {value: data.value, language: data.language}, onchange: onchange});
		}
	});
});

var RPMS = {
	verify: function(target, action){
		var verify = Utils.DataSet.getObject(target, "verify", null);
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
		// Get Formal Inputs
		var inputs = form.find(":input");
		inputs.each(function(i, e){
			var name = e.name;
			var origValue = Utils.DataSet.get(e, "value");
			var value = null;
			var tag = e.tagName.toLowerCase();
			if(tag == "input" && e.type.toLowerCase() == "checkbox"){
				var checkVal = Utils.DataSet.get(e, "checked-value", true);
				var uncheckVal = Utils.DataSet.get(e, "unchecked-value", false);
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
						var item = Utils.DataSet.getObject(e, "item");
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
		// Get Custom Inputs
		var customs = form.find(".custom-input");
		customs.each(function(i, e){
			var persist = Utils.DataSet.getBoolean(e, "persist");
			if(persist){
				var name = Utils.DataSet.get(e, "name");
				var value = Utils.DataSet.getObject(e, "value");
				if(changes != true || Utils.DataSet.getBoolean(e, "changed")){
					data[name] = value;
				}
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
			catch(e){
				console.log(xhr.responseText);
				return "An unexpected failure occured on the server.  See the console log for more details.";
			}
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