// Generic Form - Eventually
//Consent.Widgets.registerForm({
//		type: "form",
//		properties: [{label: "Title", name: "title", editor: "texti18n", required: true},
//			         {label: "Collect Start", name: "collect-start", editor: "pagelist", 
//						required: false, operation: Consent.Keys.collect},
//			         {label: "Review Start", name: "review-start", editor: "pagelist", 
//							required: false, operation: Consent.Keys.collect}],
//		operation: null});

//Generic Page
Consent.Widgets.registerPage({
		label: "Page",
		type: "page",
		properties: [{label: "Title", name: "title", editor: "texti18n", required: true},
			         {label: "Next", name: "next", editor: "pagelist", required: false},
			         {label: "Previous", name: "previous", editor: "pagelist", required: false}],
		operation: null});

// Generic Section
Consent.Widgets.registerSection({
		label: "Section",
		type: "section",
		properties: [],
		operation: null});

// Controls
Consent.Widgets.registerControl({
		label: "Data Change Control",
		type: "data-change",
		properties: [{label: "Meta Items", name: "meta-items", editor: "metaitems", required: true}],
		operation: Consent.Keys.collect});

Consent.Widgets.registerControl({
		label: "Media",
		type: "media",
		properties: [{label: "Title", name: "title", editor: "texti18n", required: true},
		             {label: "Sources", name: "sources", editor: "urls", required: true, posterProp: "posters"}],
		operation: Consent.Keys.collect});

Consent.Widgets.registerControl({
		label: "Policy Button",
		type: "policy-button",
		properties: [{label: "Label", name: "label", editor: "texti18n", required: true},
			         {label: "Policy", name: "policy", editor: "policies", required: true},
        			 {label: "Action Value", name: "action-value", editor: "boolean-picker", required: true}],
		operation: Consent.Keys.collect});

Consent.Widgets.registerControl({
		label: "Policy Checkbox",
		type: "policy-checkbox",
		properties: [{label: "Label", name: "label", editor: "texti18n", required: true},
			         {label: "Policy", name: "policy", editor: "policies", required: true},
			         {label: "Checked Value", name: "checked-value", editor: "boolean-picker", 
			        	 required: true, defaultValue: true},
			         {label: "Unchecked Value", name: "unchecked-value", editor: "boolean-picker", 
			        		 required: true, defaultValue: false}],
		operation: Consent.Keys.collect});

Consent.Widgets.registerControl({
		label: "Policy Choice Buttons",
		type: "policy-choice-buttons",
		properties: [{label: "Policy", name: "policy", editor: "policies", required: true},
		             {label: "True Label", name: "true-label", editor: "texti18n", required: true},
		             {label: "False Label", name: "false-label", editor: "texti18n", required: true}],
		operation: Consent.Keys.collect});

Consent.Widgets.registerControl({
		label: "Policy Text",
		type: "policy-text",
		properties: [{label: "Policy", name: "policy", editor: "policies", required: true},
		             {label: "Render Title", name: "render-title", editor: "boolean", required: true, defaultValue: true},
		             {label: "Render Text", name: "render-text", editor: "boolean", required: true, defaultValue: true},
		             {label: "Render Media", name: "render-media", editor: "boolean", required: true, defaultValue: false}],
		operation: Consent.Keys.collect});

Consent.Widgets.registerControl({
		label: "Signature",
		type: "signature",
		properties: [{label: "Endorsement", name: "endorsement", editor: "endorsement", required: true},
			         {label: "Clear Label", name: "clear-label", editor: "texti18n", required: true}],
		operation: Consent.Keys.collect});

Consent.Widgets.registerControl({
		label: "Text",
		type: "text",
		properties: [{label: "Title", name: "title", editor: "texti18n", required: true},
			         {label: "Text", name: "text", editor: "texti18n", required: true}],
		operation: Consent.Keys.collect});

Consent.Widgets.registerControl({
		label: "Review Endorsement",
		type: "review-endorsement",
		properties: [{label: "Title", name: "title", editor: "texti18n", required: false},
			         {label: "Endorsement", name: "endorsement", editor: "endorsement", required: true},
			         {label: "Label", name: "label", editor: "texti18n", required: true},
			         {label: "Return Page", name: "returnpage", editor: "pagelist",
			        	 required: true, operation: Consent.Keys.collect}],
		operation: Consent.Keys.review});

Consent.Widgets.registerControl({
		label: "Review Meta Item",
		type: "review-metaitem",
		properties: [{label: "Title", name: "title", editor: "texti18n", required: false},
			         {label: "Meta Item", name: "meta-item", editor: "metaitem", required: true},
			         {label: "Label", name: "label", editor: "texti18n", required: true}],
		operation: Consent.Keys.review});

Consent.Widgets.registerControl({
		label: "Review Policy",
		type: "review-policy",
		properties: [{label: "Title", name: "title", editor: "texti18n", required: false},
			         {label: "Policy", name: "policy", editor: "policy", required: true},
			         {label: "Label", name: "label", editor: "texti18n", required: true},
			         {label: "Return Page", name: "returnpage", editor: "pagelist", 
			        	 required: true, operation: Consent.Keys.collect}],
		operation: Consent.Keys.review});

// Editors
var ShowBogus = function(container, text, property, data, operation, editable){
	var widgetProperty = Consent.Utils.findProperty(property.name, data.properties);
	var nl = "<br /><br />"
	var message = "<div>You need to complete the " + text + " control." + nl;
	message = message + "Property: " + JSON.stringify(property) + nl;
	message = message + "WidgetProperty: " + JSON.stringify(widgetProperty) + nl;
	message = message + "</div>";
	var ctl = $(message);
	ctl.appendTo(container);
	return ctl;
}

Consent.Editors.common.selectCreated = function(control){
	var keyValue = control.data("state");
	var attrs = Consent.Editors.lift(control);
	var key = attrs.property.name;
	var items = [];
	if(keyValue.id == null){
		var options = control.find("select option:selected");
		var value = options[0].value;
		if(!Consent.Editors.valueIsNull(value)){
			items.push({key: key, value: value});
		}
	}
	return items;
};

Consent.Editors.common.selectUpdated = function(control){
	var items = [];
	var keyValue = control.data("state");
	if(keyValue.id != null){
		var options = control.find("select option:selected");
		var value = options[0].value;
		if(!Consent.Editors.valueIsNull(value)){
			items.push({id: keyValue.id, value: value});
		}
	}
	return items;
};

Consent.Editors.common.selectDeleted = function(control){
	var items = [];
	var keyValue = control.data("state");
	if(keyValue.id != null){
		var options = control.find("select option:selected");
		var value = options[0].value;
		if(Consent.Editors.valueIsNull(value)){
			items.push({id: keyValue.id});
		}
	}
	return items;
};

Consent.Editors.common.multiSelectCreated = function(control){
	var keyValue = control.data("state");
	var attrs = Consent.Editors.lift(control);
	var key = attrs.property.name;
	var items = [];
	if(keyValue.id == null){
		var options = control.find("select option:selected");
		var value = [];
		$.each(options, function(i,o){value.push(o.value)});
		if(!Consent.Editors.valueIsNull(value)){
			items.push({key: key, value: value});
		}
	}
	return items;
};

Consent.Editors.common.multiSelectUpdated = function(control){
	var items = [];
	var keyValue = control.data("state");
	if(keyValue.id != null){
		var options = control.find("select option:selected");
		var value = [];
		$.each(options, function(i,o){value.push(o.value)});
		if(!Utils.Array.compare(value, keyValue.value)){
			items.push({id: keyValue.id, value: value});
		}
	}
	return items;
};

Consent.Editors.register("texti18n", {
	generate: function(container, property, data, operation, editable){
		var keyValues = Consent.Utils.findProperties(property.name, data.properties);
		return Consent.UI.createTextControl(container, property.label, property.name, keyValues);},
	refresh: function(control, data){},
	created: function(control){
		var attrs = Consent.Editors.lift(control);
		var key = attrs.property.name;
		var found = [];
		var texts = Utils.DataSet.get(control, "value", []);
		if(texts != null && texts.length > 0){
			$.each(texts, function(i, o){
				if(o.id == null){
					o.key = key;
					found.push(o);
				}
			})
		}
		return found;
	},
	updated: function(control){return Utils.DataSet.get(control, "updated", [])},
	deleted: function(control){return Utils.DataSet.get(control, "deleted", [])}
});

Consent.Editors.register("pagelist", {
	generate: function(container, property, data, operation, editable){
		var options = [{value: null, label: "{none}"}];
		var panes = Consent.Utils.getPanePath((property.operation || operation), "");
		if(panes != null){
			$.each(panes, function(i,p){options.push({value: p.name, label:p.name, data: p})});
		}
		return Consent.UI.createSelectControl(container, property, data, operation, editable, options);},
	created: Consent.Editors.common.selectCreated,
	updated: Consent.Editors.common.selectUpdated,
	deleted: Consent.Editors.common.selectDeleted
});

Consent.Editors.register("boolean", {
	generate: function(container, property, data, operation, editable){
		var keyValue = Consent.Utils.findProperty(property.name, data.properties, true);
		var ctl =  Consent.UI.createCheckboxControl(container, editable, keyValue.key, property.label, keyValue.value, 
				true, false, property.defaultValue);
		ctl.data("state", keyValue);
		return ctl;},
	created: function(control){
		var keyValue = control.data("state");
		var items = [];
		if(keyValue.id == null){
			var attrs = Consent.Editors.lift(control);
			var key = attrs.property.name;
			var value = (control.find("input:checked").length > 0);
			items.push({key: key, value: value});
		}
		return items;
	},
	updated: function(control){
		var keyValue = control.data("state");
		var items = [];
		if(keyValue.id != null){
			var attrs = Consent.Editors.lift(control);
			var key = attrs.property.name;
			var value = (control.find("input:checked").length > 0);
			if(keyValue != value){
				items.push({id: keyValue.id, key: key, value: (checkbox.length > 0)});
			}
		}
		return items;
	}
});

Consent.Editors.register("boolean-picker", {
	generate: function(container, property, data, operation, editable){
		var options = [{value: null, label: "{none}"}, {value: true, label: "true"}, {value: false, label: "false"}];
		return Consent.UI.createSelectControl(container, property, data, operation, editable, options);},
		created: Consent.Editors.common.selectCreated,
		updated: Consent.Editors.common.selectUpdated,
		deleted: Consent.Editors.common.selectDeleted
});

Consent.Editors.register("urls", {
	generate: function(container, property, data, operation, editable){
		return ShowBogus(container, "urls", property, data, operation, editable);}
});

Consent.Editors.register("endorsement", {
	generate: function(container, property, data, operation, editable){
		var options = [{value: null, label: "{none}"}];
		var nodes = Consent.Designer.protocol.endorsements;
		if(nodes != null){
			$.each(nodes, function(i,n){options.push({value: n.id, label:n.name, data:n})});
		}
		return Consent.UI.createSelectControl(container, property, data, operation, editable, options);},
		created: Consent.Editors.common.selectCreated,
		updated: Consent.Editors.common.selectUpdated,
		deleted: Consent.Editors.common.selectDeleted
});

Consent.Editors.register("policy", {
	generate: function(container, property, data, operation, editable){
		var options = [{value: null, label: "{none}"}];
		var nodes = Consent.Designer.protocol.policies;
		if(nodes != null){
			$.each(nodes, function(i,n){options.push({value: n.id, label:n.name, data:n})});
		}
		return Consent.UI.createSelectControl(container, property, data, operation, editable, options);},
		created: Consent.Editors.common.selectCreated,
		updated: Consent.Editors.common.selectUpdated,
		deleted: Consent.Editors.common.selectDeleted
});

Consent.Editors.register("policies", {
	generate: function(container, property, data, operation, editable){
		var options = [];
		var nodes = Consent.Designer.protocol.policies;
		if(nodes != null){
			$.each(nodes, function(i,n){options.push({value: n.id, label:n.name, data:n})});
		}
		return Consent.UI.createMultiSelectControl(container, property, data, operation, editable, options);},
		created: Consent.Editors.common.multiSelectCreated,
		updated: Consent.Editors.common.multiSelectUpdated
});

Consent.Editors.register("metaitem", {
	generate: function(container, property, data, operation, editable){
		var options = [];
		var nodes = Consent.Designer.protocol["meta-items"];
		if(nodes != null){
			$.each(nodes, function(i,n){options.push({value: n.id, label:n.name, data:n})});
		}
		return Consent.UI.createSelectControl(container, property, data, operation, editable, options);},
		created: Consent.Editors.common.selectCreated,
		updated: Consent.Editors.common.selectUpdated,
		deleted: Consent.Editors.common.selectDeleted
});

Consent.Editors.register("metaitems", {
	generate: function(container, property, data, operation, editable){
		var options = [{value: null, label: "{none}"}];
		var nodes = Consent.Designer.protocol["meta-items"];
		if(nodes != null){
			$.each(nodes, function(i,n){options.push({value: n.id, label:n.name, data:n})});
		}
		return Consent.UI.createSelectControl(container, property, data, operation, editable, options);},
		created: Consent.Editors.common.multiSelectCreated,
		updated: Consent.Editors.common.multiSelectUpdated
});

Consent.Editors.register("input", {
	generate: function(container, property, data, operation, editable){
		var keyValue = Consent.Utils.findProperty(property.name, data.properties);
		return Consent.UI.createInputControl(container, editable, property.label, keyValue.value, data);
	}
}); 
