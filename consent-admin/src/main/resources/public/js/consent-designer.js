(function(window, undefined){
	// Check If Designer Is There
	var consent = window.Consent;
	if(consent == null){
		window.Consent = consent = {};
	}
	
	var keys = consent.Keys;
	if(keys == null){
		consent.Keys = keys = {
			collect: "collect",
			review: "review",
			page: "page",
			section: "section",
			control: "control"
		};
	}
	
	var utils = consent.Utils;
	if(utils == null){
		consent.Utils = utils = {
			findByKeyValue: function(items, key, value, remove){
				if(items != null){
					for(var i = 0 ; i < items.length; i++){
						if(value == items[i][key]){
							if(remove){
								return items.splice(i, 1)[0];
							}
							return items[i];
						}
					}
				}
				return null;
			},
			findByName: function(items, name, remove){
				return utils.findByKeyValue(items, "name", name, remove);
			},
			findWidgetProperty: function(widget, name){
				if(widget != null && widget.properties != null){
					var props = widget.properties;
					for(var i = 0; i < props.length; i++){
						if(props[i].key == name) return props[i];
					}
				}
				return null;
			},
			getPanesByOperation: function(operation){
				var panes = consent.Designer.protocol.form.contains;
				var found = [];
				for(var i = 0; i < panes.length; i++){
					var prop = utils.findWidgetProperty(panes[i], "operation");
					if(prop != null && operation == prop.value){
						found.push(panes[i]);
					}
				}
				return found;
			},
			getPanePath: function(operation, name, panes, found){
				if(found == null) found = [];
				if(panes == null) panes = utils.getPanesByOperation(operation);
				if(utils.findByName(found, name) == null){
					var pane = utils.findByName(panes, name, true);
					if(pane != null){
						found.push(pane);
						var nextProp = utils.findWidgetProperty(pane, "next");
						if(nextProp != null){
							utils.getPanePath(operation, nextProp.value, panes, found);
						}
						else{
							found = found.concat(panes.splice(0, panes.length));
						}
					}
					else{
						found = found.concat(panes.splice(0, panes.length));
					}
				}
				else{
					found = found.concat(panes.splice(0, panes.length));
				}
				return found;
			}
		}
	}
	
	var widgets = consent.Widgets;
	if(widgets == null){
		consent.Widgets = widgets = {
			pages: [],
			sections: [],
			controls: [],
			registerPage: function(widget){
				widgets.pages.push(widget);
			},
			registerSection: function(widget){
				widgets.sections.push(widget);
			},
			registerControl: function(widget){
				widgets.controls.push(widget);
			},
			findWidget: function(kind, type){
				var ws = widgets[kind];
				if(ws != null && ws.length > 0){
					return utils.findByKeyValue(items, "type", type, remove);
				}
				return null;
			}
		}
	}
	
	var ui = consent.UI;
	if(ui == null){
		consent.UI = ui = {
			showPane: function(pane){
				pane.parent().children().hide();
				pane.show();
			},
			relativeContent: function(item){
				return item.closest(".consent-designer-pane").children(".consent-designer-content");
			},
			createPane: function(container){
				var pane = $("<div class='consent-designer-pane' />");
				pane.appendTo(container);
				return pane;
			},
			createSelector: function(container){
				var selector = $("<div class='consent-designer-selector' />");
				selector.appendTo(container);
				return selector;
			},
			createContent: function(container){
				var content = $("<div class='consent-designer-content' />");
				content.appendTo(container);
				Utils.Size.fillRight(content);
				return content;
			},
			createMenu: function(container, items){
				var list = $("<ul class='consent-designer-selector'>");
				list.appendTo(container);
				if(items != null && items.length > 0){
					for(var i = 0; i < items.length; i++){
						var item = items[i];
						item.li = ui.addMenuItem(list, item);
						item.li.appendTo(list);
					}
					if(items[0].launch){
						items[0].li.click();
					}
				}
				return list;
			},
			addMenuItem: function(menu, item){
				var label = item.label == null ? "" : item.label;
				var li = $("<li>" + label + "</li>");
				li.appendTo(menu);
				if(item.action != null){						
					li.bind("click", function(event){
						menu.children().removeClass("selected");
						$(event.target).addClass("selected");
					});
					li.bind("click", item.action);
					li.data("item", item);
				}
				else{
					li.addClass("spacer");
				}
				return li;
			},
			createData: function(container, noDelete){
				var pane = $("<div class='consent-designer-data' />");
				pane.appendTo(container);
				var fields = $("<div class='consent-designer-data-fields' />");
				fields.appendTo(pane);
				if(designer.editable){
					var actions = $("<div class='consent-designer-data-actions' />");
					actions.appendTo(pane);
					var save = $("<div class='ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only consent-designer-data-save'>Save</div>");
					save.appendTo(actions);
					save.bind("click", designer.save);
					if(noDelete != true){
						var del = $("<div class='ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only consent-designer-data-delete'>Delete</div>");
						del.appendTo(actions);
						del.bind("click", designer.delete);
					}
				}
				return pane;
			},
			createHeader: function(container, label){
				var dashboard = $("<div class='consent-designer-header'>" + label + "</div>");
				dashboard.appendTo(container);
			},
			createWidgetDashboard: function(container, options, addAction, sortAction){
				var dashboard = $("<div class='consent-designer-widget-dashboard' />");
				dashboard.appendTo(container);

				var add = $("<img src='" + Utils.Url.render("/image/add.png") + "' />")
				add.appendTo(dashboard);
				add.bind("click", function(){addAction(options);});
				var sort = $("<img src='" + Utils.Url.render("/image/sort.png") + "' />")
				sort.appendTo(dashboard);
				sort.bind("click", function(){sortAction(options);});
			},
			createTextControl: function(container, label, parentType, parentId, property, texts){
				var control = $("<div class='form-control-wrapper i18ntext consent-designer-input' />");
				control.appendTo(container);
				control.data("editable", designer.editable);
				control.data("name", property);
				control.data("languages", designer.protocol.languages);
				if(parentId != null){
					control.data("url", Utils.Url.render("/api/text/i18n"));
					control.data("params", {"parent-type": parentType, "parent-id": parentId, "property": property});
				}
				else{
					control.data("persist", designer.editable);
				}
				
				var label = $("<div class='form-label'>" + label + "</div>");
				label.appendTo(control);
				var table = $("<table class='i18ntext' />");
				table.appendTo(control);
				var action = "";
				if(designer.editable){
					action = "<th class='i18ntext-action'>" +  
						"<img class='i18ntext-add' src='" + Utils.Url.render("/image/add.png") + "'/></th>";
				}
				var header = $("<tr class='i18ntext'>" + 
								"<th class='i18ntext-lang'>Language</th>" + 
								"<th class='i18ntext-text'>Display Text</th>" + action + "</tr>");
				header.appendTo(table);
				if(texts != null){
					var action = "";
					if(designer.editable){
						action = "<td class='i18ntext-action'>" + 
							"<img class='i18ntext-edit' src='" + Utils.Url.render("/image/edit.png") + "'/>" + 
							"<img class='i18ntext-delete' src='" + Utils.Url.render("/image/delete.png") + "'/></td>";
					}
					for(var t = 0; t < texts.length; t++){
						var text = texts[t];
						var val = text.value instanceof Array ? text.value.join("<br/>") : text.value;
						var row = $("<tr class='i18ntext'>" + 
										"<td class='i18ntext-lang'>" + text.language.name + "</td>" + 
										"<td class='i18ntext-text'>" + val + "</td>" + action + "</tr>");
						row.appendTo(table);
						row.data("text", text);
					}
				}
			}
		}
	}
	var designer = consent.Designer;
	
	if(designer == null){
		consent.Designer = designer = {
			init: function(){
				var container = $("#consent-designer");
				if(container == null || container.length == 0){
					var message = "The Consent Designer requires a div#consent-designer element that contains all of the data.";
					Dialog.error({message: message});
					throw message;
				}
				designer.container = container;
				container.empty();
				container.addClass("consent-designer-pane");
				
				var protocol = Utils.DataSet.getObject(container, "data-protocol");
				if(protocol == null){
					var message = "The Consent Designer requires a data-protocol value on the div#consent-desigern element.";
					Dialog.error({message: message});
					throw message;
				}
				designer.protocol = protocol;
				var editable = Utils.DataSet.getBoolean(container, "data-editable");
				designer.editable = editable = (editable != null || editable);

				designer.formUrl = "/api/protocol/version/designer/form";
				designer.formParams = {"protocol-version": protocol.id};

				designer.widgetUrl = "/api/protocol/version/designer/widget";
				designer.widgetParams = {"protocol-version": protocol.id};
				designer.createUI();
			},
			createUI: function(){
				var main = {panes: []};
				designer.main = main;
				
				// Create Sections
				main.select = ui.createSelector(designer.container);
				main.content = ui.createContent(designer.container)
				
				// Create Expand Button
				main.resizer = $("<img class='consent-designer-resize' src='" + Utils.Url.render("/image/expand.png") + "'/>");
				main.resizer.appendTo(main.select);
				main.resizer.bind("click", designer.resize);
				
				// Create Main Selector
				var items = [{label: "Form", action: designer.formDetails},
				             {label: "Collect", action: designer.collectDetails},
				             {label: "Review", action: designer.reviewDetails},
				             {label: "Preview", action: designer.previewDetails}];
				
				main.selector = ui.createMenu(main.select, items);
			},
			resize: function(){
				Dialog.inform({message: "Resizing of the layout manager is currently hibernating.  Try again later."})
			},
			formDetails: function(){
				if(designer.main.formPane == null){
					var form = designer.protocol.form;
					var pane = ui.createPane(designer.main.content);
					var data = ui.createData(pane, true);
					data.data("data", form);
					var fields = data.children().first();
					ui.createTextControl(fields, "Title", "form", form.id, "titles", form.titles);
					designer.main.formPane = pane;
					designer.main.panes.push(pane);
				}
				ui.showPane(designer.main.formPane);
			},
			collectDetails: function(){
				if(designer.collect == null){
					var collect = designer.collect = {};
					collect.pane = ui.createPane(designer.main.content);
					collect.selector = ui.createSelector(collect.pane);
					collect.content = ui.createContent(collect.pane);
					
					ui.createHeader(collect.selector, "Pages");
					var form = designer.protocol.form;
					var dashOptions = {parentType: "form", parent: form,
							operation: keys.collect, type: keys.page};
					ui.createWidgetDashboard(collect.selector, dashOptions, designer.addPage, designer.sortPageList);
					
					var items = [];
					// Get All Of The Page Widgets
					if(form["collect-start"] != null){
						var panes = utils.getPanePath(keys.collect, form["collect-start"]);
						for(var i = 0 ; i < panes.length; i ++){
							var p = panes[i];
							items.push({label: p.name, 
								data: p, 
								action: designer.selectPageDetails, 
								operation: keys.collect, 
								type: keys.page,
								parent: form, 
								parentType: "form"});
						}
					}
					dashOptions.list = ui.createMenu(collect.selector, items);
				}
				ui.showPane(designer.collect.pane);
			},
			reviewDetails: function(){
				//utils.showPane(designer.review.pane);
			},
			createWidgetView: function(container, item, li, options){
				var data = item.data == null? {} : item.data;
				var label = li == null ? options.newlabel : li.text();
				var operation = item.operation;
				
				var pane = ui.createPane(container);
				var selector = ui.createSelector(pane);
				var content = ui.createContent(pane);
				
				// Create header
				ui.createHeader(selector, label);
				
				// Create Section Dashboard
				var dashOptions = {parentType: "widget", parent: data,
						operation: operation, type: keys.section};
				ui.createWidgetDashboard(selector, dashOptions, options.newaction, options.sortaction);
				
				// Create 
				var items = [];
				items.push({label: "Details", action: designer.widgetDetails, data: data});
				items.push({});
				items.push({label: options.sublabel});
				if(data != null && data.contains != null){
					for(var i = 0; i < data.contains.length; i++){
						var widget = data.contains[i];
						items.push({label: widget.name, 
							data: widget, 
							action: options.subaction, 
							operation: operation, 
							type: keys.section,
							parent: data, 
							parentType: "widget"});
					}
				}
				dashOptions.list = ui.createMenu(selector, items);
				return pane;
			},
			selectPageDetails: function(event){
				var li = $(event.target);
				var item = li.data("item");
				if(item.pane == null){
					var options = {newlabel: "New Page", sublabel: "Sections",
						subaction: designer.selectSectionDetails,
						newaction: designer.addSection,
						sortaction: designer.sortSectionList,
						widgets: widgets.pages}
					var content = ui.relativeContent(li);
					item.pane = designer.createWidgetView(content, item, li, options);
				}
				ui.showPane(item.pane);
			},
			addPage: function(options){
				var parentType = options.parentType;
				var parent = options.parent;
				var list = options.list;
				var li = ui.addMenuItem(list, {label: "New Page", action: designer.selectPageDetails});
				li.click();
			},
			sortPageList: function(options){
				
			},
			selectSectionDetails: function(event){
				var li = $(event.target);
				var item = li.data("item");
				if(item.pane == null){
					var options = {newlabel: "New Section", sublabel: "Controls",
						subaction: designer.selectControlDetails,
						newaction: designer.addControl,
						sortaction: designer.sortControlList,
						widgets: widgets.sections}
					var content = ui.relativeContent(li);
					item.pane = designer.createWidgetView(content, item, li, options);
				}
				ui.showPane(item.pane);
			},
			addSection: function(options){
				var parentType = options.parentType;
				var parent = options.parent;
				var list = options.list;
				var li = ui.addMenuItem(list, {label: "New Section", action: designer.selectSectionDetails});
				li.click();
			},
			sortSectionList: function(options){
				
			},
			selectControlDetails: function(event){
				
			},
			addControl: function(options){
				var parentType = options.parentType;
				var parent = options.parent;
				var list = options.list;
				var li = ui.addMenuItem(list, {label: "New Control", action: designer.selectControlDetails});
				li.click();
			},
			sortControlList: function(options){
				
			},
			widgetDetails: function(event){
				var li = $(event.target);
			},
			previewDetails: function(){
				//utils.showPane(designer.preview.pane);
			},
			save: function(event){
				
			},
			delete: function(event){
				
			}
		};
	}
})(window);
Consent.Designer.init();
// Generic Page
Consent.Widgets.registerPage({
		type: "page",
		properties: [{name: "title", datatype: "texti18n", required: true},
			         {name: "next", datatype: "pagelist", required: false},
			         {name: "previous", datatype: "pagelist", required: false}],
		operation: null});

// Generic Section
Consent.Widgets.registerSection({
		type: "section",
		properties: [],
		operation: null});

// Controls
Consent.Widgets.registerControl({
		type: "data-change",
		properties: [{name: "meta-items", datatype: "metaitems", required: true}],
		operation: Consent.Keys.collect});

Consent.Widgets.registerControl({
	type: "media",
	properties: [{name: "title", datatype: "texti18n", required: true},
		         {name: "media", datatype: "urls", required: true}],
	operation: Consent.Keys.collect});

Consent.Widgets.registerControl({
		type: "policy-button",
		properties: [{name: "label", datatype: "texti18n", required: true},
			         {name: "policy", datatype: "policies", required: true},
        			 {name: "action-value", datatype: "boolean-picker", required: true}],
		operation: Consent.Keys.collect});

Consent.Widgets.registerControl({
		type: "policy-checkbox",
		properties: [{name: "label", datatype: "texti18n", required: true},
			         {name: "policy", datatype: "policies", required: true},
			         {name: "checked-value", datatype: "boolean-picker", required: true},
			         {name: "unchecked-value", datatype: "boolean-picker", required: true}],
		operation: Consent.Keys.collect});

Consent.Widgets.registerControl({
		type: "policy-choice-buttons",
		properties: [{name: "policy", datatype: "policies", required: true},
		             {name: "render-title", datatype: "boolean", required: true},
		             {name: "render-text", datatype: "boolean", required: true},
		             {name: "render-media", datatype: "boolean", required: true}],
		operation: Consent.Keys.collect});

Consent.Widgets.registerControl({
		type: "signature",
		properties: [{name: "endorsement", datatype: "endorsement", required: true},
			         {name: "clear-label:", datatype: "texti18n", required: true}],
		operation: Consent.Keys.collect});

Consent.Widgets.registerControl({
		type: "text",
		properties: [{name: "title", datatype: "texti18n", required: true},
			         {name: "text", datatype: "texti18n", required: true}],
		operation: Consent.Keys.collect});

Consent.Widgets.registerControl({
		type: "review-endorsement",
		properties: [{name: "title", datatype: "texti18n", required: false},
			         {name: "endorsement", datatype: "endorsement", required: true},
			         {name: "label", datatype: "texti18n", required: true},
			         {name: "returnpage", datatype: "pagelist", required: true}],
		operation: Consent.Keys.review});

Consent.Widgets.registerControl({
		type: "review-metaitem",
		properties: [{name: "title", datatype: "texti18n", required: false},
			         {name: "meta-item", datatype: "metaitem", required: true},
			         {name: "label", datatype: "texti18n", required: true}],
		operation: Consent.Keys.review});

Consent.Widgets.registerControl({
		type: "review-policy",
		properties: [{name: "title", datatype: "texti18n", required: false},
			         {name: "policy", datatype: "policy", required: true},
			         {name: "label", datatype: "texti18n", required: true},
			         {name: "returnpage", datatype: "pagelist", required: true}],
		operation: Consent.Keys.review});
