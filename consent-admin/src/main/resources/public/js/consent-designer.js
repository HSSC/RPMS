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
			form: "form",
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
							Utils.Array.addAll(found, panes, true);
						}
					}
					else{
						Utils.Array.addAll(found, panes, true);
					}
				}
				else{
					Utils.Array.addAll(found, panes, true);
				}
				return found;
			},
			findProperties: function(name, props){
				var found = [];
				if(props != null){
					for(var i = 0; i < props.length; i++){
						if(props[i].key == name) found.push(props[i]);
					}
				}
				return found;
			},
			findProperty: function(name, props, empty){
				var found = utils.findProperties(name, props);
				if(found.length > 0){
					return found[0];
				}
				else if(empty){
					return {key: name, value: null, language: null};
				}
				return null;
			}
		}
	}
	
	var widgets = consent.Widgets;
	if(widgets == null){
		consent.Widgets = widgets = {};
		widgets.control = {id: keys.control, label: "Control", widgets: []};
		widgets.section = {id: keys.section, label: "Section", widgets: [], subtype: widgets.control};
		widgets.page = {id: keys.page, label: "Page", widgets: [], subtype: widgets.section};
		widgets.form = {id: keys.form, label: "Form", widgets: [], subtype: widgets.page};
		widgets.registerPage =  function(widget){
			widgets.page.widgets.push(widget);
		};
		widgets.registerSection =  function(widget){
			widgets.section.widgets.push(widget);
		};
		widgets.registerControl =  function(widget){
			widgets.control.widgets.push(widget);
		};
		widgets.findWidget = function(kind, type){
			var ws = widgets[kind];
			if(ws != null && ws.widgets.length > 0){
				return utils.findByKeyValue(ws.widgets, "type", type);
			}
			return null;
		};
		widgets.generateName = function(widget){
			return "New " + widget.label + new Date().getMilliseconds();
		};
	}
	
	var editors = consent.Editors;
	if(editors == null){
		consent.Editors = editors = {};
		editors.editors = {};
		editors.common = {};
		editors.base = {
				refresh: function(control, data){
					var attrs = editors.lift(control);
					var keyValue = Consent.Utils.findProperty(attrs.property.name, data.properties, true);
					control.data("state", keyValue);
				},
				created: function(control){return []},
				updated: function(control){return []},
				deleted: function(control){return []}
		};
		editors.register = function(id, edit){
			editors.editors[id] = $.extend({}, editors.base, edit);
		};
		editors.sync = function(container, property, data, operation, editable){
			container.data("property", property);
			container.data("data", data);
			container.data("operation", operation);
			container.data("editable", editable);
		};
		editors.lift = function(container){
			var props = {
				property: container.data("property"),
				data: container.data("data"),
				operation: container.data("operation"),
				editable: container.data("editable")
			};
			return props;
		};
		editors.valueIsNull = function(val){
			if(val == null || val == "null" || val == "{none}") return true;
			return false;
		};
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
			relativeData: function(item){
				return item.closest(".consent-designer-data");
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
					items[0].li.click();
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
			createFormData: function(container){
				var pane = $("<div class='consent-designer-data' />");
				pane.appendTo(container);
				var fields = $("<div class='consent-designer-data-fields' />");
				fields.appendTo(pane);
				if(designer.editable){
					var actions = $("<div class='consent-designer-data-actions' />");
					actions.appendTo(pane);
					var save = $("<div class='ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only consent-designer-data-save'>Save</div>");
					save.appendTo(actions);
					save.bind("click", designer.saveForm);
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

				var sort = $("<img src='" + Utils.Url.render("/image/sort.png") + "' />")
				sort.appendTo(dashboard);
				sort.bind("click", function(){sortAction(options);});

				var add = $("<img src='" + Utils.Url.render("/image/add.png") + "' />")
				add.appendTo(dashboard);
				add.bind("click", function(){addAction(options);});
			},
			createTextControl: function(container, label, property, texts){
				var control = $("<div class='form-control-wrapper i18ntext consent-designer-input' />");
				control.appendTo(container);
				control.data("editable", designer.editable);
				control.data("name", property);
				control.data("languages", designer.protocol.languages);
				control.data("value", texts);
				
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
				return control;
			},
			createMediaControl: function(container, property, data, operation, editable){
				var urlValue = utils.findProperty(property.name, data.properties, true);
				var posterValue = utils.findProperty(property.posterProp, data.properties, true);
				
				var control = ui.createControl(container, editable);
				control.data("state", {url: urlValue, poster: posterValue});
				ui.createLabel(control, property.name, property.label);
				
				control.data("editable", designer.editable);
				
				var table = $("<table class='consent-designer-media' />");
				table.appendTo(control);
				var header = $("<tr class='consent-designer-media'>" + 
							"<th class='consent-designer-media'>Media URL</th>" + 
							"<th class='consent-designer-media'>Poster URL</th></tr>")
				header.appendTo(table);
				var action = "";
				if(editable){
					var addth = $("<th class='consent-designer-media-action' />");
					addth.appendTo(header);
					var addimg = $("<img class='consent-designer-media-action' src='" +
								Utils.Url.render("/image/add.png") + "'/>");
					addimg.appendTo(addth);
					
					// add script
				}
				
				if(urlValue.value){
					for(var i = 0; i < urlValue.value.length; i++){
						var row = $("<tr class='consent-designer-media'>");
						row.appendTo(table);
						var url = urlValue.value[i];
						var poster = posterValue.value.length > i ? posterValue.value[i] : "" ;
						$("<td class='consent-designer-media'>" + url + "</td>").appendTo(row); 
						$("<td class='consent-designer-media'>" + poster + "</td>").appendTo(row);
						if(editable){
							var cell = $("<td class='consent-designer-media-action'>");
							cell.appendTo(row);
							var img = $("<img class='consent-designer-media' src='" + 
									Utils.Url.render("/image/delete.png") + "'/>");
							img.appendTo(cell);
						}
					}
				}
				return control;
			},
			createInputControl: function(container, editable, field, label, value){
				var wrap = ui.createControl(container);
				ui.createLabel(wrap, field, label);
				ui.createInput(wrap, editable, field, value);
				return wrap;
			},
			createControl: function(container){
				var wrap = $("<div class='consent-designer-input' />");
				wrap.appendTo(container);
				return wrap;
			},
			createInput: function(container, editable, field, value){
				var input = $("<input class='consent-designer-input' />");
				input.appendTo(container);
				input.val(value);
				input.data("field", field);
				input.data("state", value);
				if(!editable){
					input.attr("disabled", "disabled");
				}
				return input;
			},
			createCheckboxControl: function(container, editable, field, label, value, checkedValue, uncheckedValue, defaultValue){
				var wrap = ui.createControl(container);
				wrap.data("checkedValue", checkedValue);
				wrap.data("uncheckedValue", uncheckedValue);
				wrap.data("value", value);
				wrap.data("field", field);
				var input = $("<input type='checkbox' class='consent-designer-checkbox' />");
				input.appendTo(wrap);
				if(value == checkedValue || (value == null && checkedValue == defaultValue)){
					input.attr("checked", "checked");
				}
				if(!editable){
					input.attr("disabled", "disabled");
				}
				ui.createLabel(wrap, field, label).addClass("consent-designer-checkbox");
				return wrap;
			},
			createSelectControl: function(container, property, data, operation, editable, options){
				var keyValue = utils.findProperty(property.name, data.properties, true);
				var control = ui.createControl(container, editable);
				control.data("state", keyValue);
				ui.createLabel(control, property.name, property.label);
				ui.createSelect(control, editable, property.name, keyValue.value, options);
				return control;
			},
			createMultiSelectControl: function(container, property, data, operation, editable, options){
				var keyValue = utils.findProperty(property.name, data.properties, true);
				var control = ui.createControl(container, editable);
				control.data("state", keyValue);
				ui.createLabel(control, property.name, property.label);
				ui.createMultiSelect(control, editable, property.name, keyValue.value, options);
				return control;
			},
			createSelect: function(container, editable, field, value, options){
				var select = $("<select class='consent-designer-input' />");
				select.appendTo(container);
				if(options != null){
					$.each(options, function(i,o){
							var opt = $("<option>" + o.label + "</option>");
							if(o.value){
								opt[0].value = o.value;
							}
							if(value == o.value) opt.attr("selected", "selected");
							opt.appendTo(select);
						});
				}
				select.val(value);
				select.data("field", field);
				if(!editable){
					select.attr("disabled", "disabled");
				}
				return select;
			},
			createMultiSelect: function(container, editable, field, values, options){
				var select = $("<select class='consent-designer-input' multiple='multiple' size='4'/>");
				select.appendTo(container);
				var selected = []
				if(options && values){
					for(var o = (options.length - 1); o >= 0; o--){
						var opt = options[o];
						$.each(values, function(i, v){
							if(opt.value == v){
								opt.selected = true;
								selected.push(opt);
								options.splice(o, 1);
							}
						});
					}
					options = selected.concat(options);
				}
				if(options != null){
					$.each(options, function(i,o){
							var opt = $("<option>" + o.label + "</option>");
							if(o.value){
								opt[0].value = o.value;
							}
							opt.appendTo(select);
							if(o.selected){
								opt[0].selected = true;
							}
						});
				}
				select.data("field", field);
				if(!editable){
					select.attr("disabled", "disabled");
				}
				return select;
			},
			createLabel: function(container, field, label){
				var label = $("<label class='consent-designer-label' >" + label + "</label>");
				label.appendTo(container);
				return label;
			},
			createWidgetData: function(container, widget, parentData, parentType, data, type, operation, options){
				var pane = $("<div class='consent-designer-data' />");
				pane.appendTo(container);
				var fieldPane = $("<div class='consent-designer-data-fields' />");
				fieldPane.appendTo(pane);
				if(designer.editable){
					var actions = $("<div class='consent-designer-data-actions' />");
					actions.appendTo(pane);
					var save = $("<div class='ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only consent-designer-data-save'>Save</div>");
					save.appendTo(actions);
					save.bind("click", designer.saveWidget);
					var del = $("<div class='ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only consent-designer-data-delete'>Delete</div>");
					del.appendTo(actions);
					del.bind("click", designer.deleteWidget);
				}

				ui.createInputControl(fieldPane, designer.editable, "name", "Name", data.name);
				ui.createInputControl(fieldPane, false, "type", "Type", data.type);
				
				// Create Fields
				var fields = [];
				if(widget.properties != null){
					var editable = designer.editable;
					for(var i = 0; i < widget.properties.length; i++){
						var property = widget.properties[i];
						var editor = editors.editors[property.editor];
						var control = editor.generate(fieldPane, property, data, operation, editable);
						editors.sync(control, property, data, operation, editable);
						fields.push({editor: editor, control: control});
					}
				}
				if(data.id == null){
					data[parentType] = parentData;
				}
				pane.data("parentData", parentData);
				pane.data("parentType", parentType);
				pane.data("operation", operation);
				pane.data("data", data);
				pane.data("type", type);
				pane.data("widget", widget);
				pane.data("options", options);
				pane.data("fields", fields);
				return pane;
			}
		};
	}
	
	var designer = consent.Designer;
	if(designer == null){
		consent.Designer = designer = {
			reset: function(){
				if(designer.container){
					designer.container.empty();
				}
				designer.container = null;
				designer.main = null;
				designer.collect = null;
				designer.review = null;
				designer.preview = null;
				designer.protocol = null;
			},
			close: function(){
				Dialog.Progress.start();
				designer.reset();
				PaneManager.pop();
			},
			init: function(){
				designer.reset();
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
				
				main.closer = $("<img class='consent-designer-close' src='" + Utils.Url.render("/image/close.png") + "'/>");
				main.closer.appendTo(main.select);
				main.closer.bind("click", designer.close);
				
				// Create Main Selector
				var items = [{label: "Form", action: designer.showFormProperties},
				             {label: "Collect", action: designer.showCollectPane},
				             {label: "Review", action: designer.showReviewPane},
				             {label: "Preview", action: designer.showPreviewPane}];
				
				main.selector = ui.createMenu(main.select, items);
			},
			resize: function(){
				Dialog.inform({message: "Resizing of the layout manager is currently hibernating.  Try again later."})
			},
			showFormProperties: function(){
				if(designer.main.formPane == null){
					var form = designer.protocol.form;
					var pane = ui.createPane(designer.main.content);
					var data = ui.createFormData(pane, true);
					data.data("data", form);
					var fields = data.children().first();
					ui.createTextControl(fields, "Title", "titles", form.titles);
					designer.main.formPane = pane;
					designer.main.panes.push(pane);
				}
				ui.showPane(designer.main.formPane);
			},
			showPagesPane: function(operation){
				if(designer[operation] == null){
					var pages = designer[operation] = {};
					pages.pane = ui.createPane(designer.main.content);
					pages.selector = ui.createSelector(pages.pane);
					pages.content = ui.createContent(pages.pane);
					
					ui.createHeader(pages.selector, "Pages");
					var form = designer.protocol.form;
					var dashOptions = {
							parentType: "form", 
							parentData: form,
							operation: operation, 
							type: keys.page};
					ui.createWidgetDashboard(pages.selector, dashOptions, designer.addWidget, designer.sortWidgets);
					
					var items = [];
					// Get All Of The Page Widgets
					var start = form[operation + "-start"];
					var panes = utils.getPanePath(operation, (start || ""));
					for(var i = 0 ; i < panes.length; i ++){
						var p = panes[i];
						items.push({label: p.name, 
								action: designer.showWidgetPane,
								parentData: form, 
								parentType: "form", 
								data: p, 
								type: keys.page, 
								operation: operation});
					}
					dashOptions.list = ui.createMenu(pages.selector, items);
				}
				ui.showPane(designer[operation].pane);
				return designer[operation];
			},
			showCollectPane: function(){
				return designer.showPagesPane(keys.collect);
			},
			showReviewPane: function(){
				return designer.showPagesPane(keys.review);
			},
			showPreviewPane: function(){
				if(designer.preview == null){
					var preview = designer.preview = {};
					preview.pane = ui.createPane(designer.main.content);
					preview.pane.text("Coming to an application near you in 2012.");
				}
				ui.showPane(designer.preview.pane);
			},
			createWidgetView: function(container, item, li, options){
				var parentData = item.parentData;
				var parentType = item.parentType;
				var data = item.data;
				var type = item.type;
				var widgetType = widgets[type];
				var widgetSubtype = widgetType.subtype;
				var subtype = widgetSubtype != null ? widgetSubtype.id : null;
				
				var label = li.text();
				
				var operation = item.operation;
				
				var pane = ui.createPane(container);
				var selector = ui.createSelector(pane);
				var content = ui.createContent(pane);
				pane.data("menuitem", li);
				
				// Create header
				var header = ui.createHeader(selector, label);
				
				// Create Section Dashboard
				var dashOptions = {
						parentType: "widget", 
						parentData: data,
						operation: operation, 
						type: subtype};
				ui.createWidgetDashboard(selector, dashOptions, designer.addWidget, designer.sortWidgets);
				
				// Create 
				var items = [];
				items.push({label: "Details", action: designer.showWidgetProperties, 
					parentData: parentData, parentType: parentType,
					data: data, type: type, operation: operation, options: {namelink: [li, header]}});
				if(data != null && data.contains != null){
					var action = designer.showWidgetProperties;
					if(widgetSubtype != null){
						items.push({});
						items.push({label: widgetSubtype.label + "s"});
						if(widgetSubtype.subtype != null){
							action = designer.showWidgetPane;
						}
					}
					for(var i = 0; i < data.contains.length; i++){
						var widget = data.contains[i];
						items.push({label: widget.name, 
							action: action,
							parentData: data, 
							parentType: "widget",
							data: widget, 
							type: subtype,
							operation: operation});
					}
				}
				dashOptions.list = ui.createMenu(selector, items);
				return pane;
			},
			showWidgetPane: function(event){
				var li = $(event.target);
				var item = li.data("item");
				
				if(item.pane == null){
					var content = ui.relativeContent(li);
					item.pane = designer.createWidgetView(content, item, li);
				}
				ui.showPane(item.pane);
			},
			addWidget: function(options){
				var type = options.type;
				var widgetType = widgets[type];
				var parentData = options.parentData;
				var parentType = options.parentType;
				var operation = options.operation;
				var list = options.list;
				
				if(parentData == null || parentData.id == null){
					var message = "You must save the parent before you can create a new " + widgetType.label;
					Dialog.inform({message: message});
					return;
				}
				
				if(widgetType.widgets == null || widgetType.widgets.length == 0){
					Dialog.inform({message: "There are currently no " + widgetType.label + " widgets registered."});
					return;
				}
				
				var action = widgetType.subtype == null ? designer.showWidgetProperties : designer.showWidgetPane;
				var menuitem = {
					label: "New " + widgetType.label, 
					action: action,
					parentType: parentType, 
					parentData: parentData, 
					type: type,
					operation: operation, 
					options: {}
				};
				
				var temp = function(widget){
					var name = widgets.generateName(widget);
					menuitem.data = {name: name, type: widget.type};
					menuitem.label = name;
					var li = ui.addMenuItem(list, menuitem);
					menuitem.options.namelink = [li];
					li.click();
				};
				
				if(widgetType.widgets.length == 1){
					temp(widgetType.widgets[0]);
				}
				else{
					var items = [];
					$.each(widgetType.widgets, function(i,o){
						if(o.operation == null || o.operation == operation){
							items.push({value: o.type, label: o.label, data: o});
						}
					});
					Dialog.choose({
						title: "Choose " + widgetType.label,
						multiple: false,
						items: items,
						onchoose: function(widget){
							if(widget != null){
								temp(widget);
							}
						}
						});
				}
			},
			sortWidgets: function(options){
				
			},
			showWidgetProperties: function(event){
				var li = $(event.target);
				var item = li.data("item");
				
				if(item.pane == null){
					var type = item.type;
					var data = item.data;
					var widget = widgets.findWidget(type, data.type);
					if(widget == null){
						var message = "A '" + type + "' widget of type '" + data.type + "' is not registered.";
						Dialog.inform({message: message});
						return;
					}
					var content = ui.relativeContent(li);
					item.pane = ui.createWidgetData(content, widget, 
							item.parentData, item.parentType, data, type, item.operation, item.options);
					item.pane.data("menuitem", li);
				}
				ui.showPane(item.pane);
			},
			saveForm: function(event){
				var dataPane = ui.relativeData($(event.target));
				var textControl = dataPane.find("div.i18ntext");
				var values = textControl.data("value");
				var updated = textControl.data("updated");
				var deleted = textControl.data("deleted");
				
				var created = [];
				$.each(values, function(i,o){
					if(o.id == null){
						o.form = {id: designer.protocol.form.id};
						created.push(o);
					}
				});
				
				var body = {
						create:{title: created},
						update:{title: updated},
						"delete":{title: deleted}
				};
				
				var params = {"protocol-version": designer.protocol.id, 
						"protocol": designer.protocol.protocol.id, "form": designer.protocol.form.id};
				var url = Utils.Url.render("/api/form", params);
				var success = function(data, status, xhr){
					var form = $.parseJSON(data);
					textControl.data("value", form.titles);
					textControl.data("updated", []);
					textControl.data("deleted", []);
					Dialog.Progress.end();
				}
				RPMS.Action.doAjax("POST", url, JSON.stringify(body), null, null, {success: success});
			},
			saveWidget: function(event){
				var dataPane = ui.relativeData($(event.target));
				var nameField = dataPane.children().first().children().first().children("input");
				
				var fields = dataPane.data("fields");
				var data = dataPane.data("data");
				var operation = dataPane.data("operation");
				var parentData = dataPane.data("parentData");
				var parentType = dataPane.data("parentType");
				var params = {"protocol-version": designer.protocol.id, 
						"protocol": designer.protocol.protocol.id,
						"form": designer.protocol.form.id};
				if(parentType == "widget"){
					params.widget = parentData.id;
				}
				var method = "POST";
				var body = null;
				
				var crudData = {name: nameField.val(), type: data.type};
				if(data.id == null){
					method = "PUT";
					crudData.properties = [{key: "operation", value: operation}];
					$.each(fields, function(i,f){
						Utils.Array.addAll(crudData.properties, f.editor.created(f.control));
					});
					body = crudData;
				}
				else{
					params.widget = data.id;
					body = {
							create:{property:[]},
							update:{property:[]},
							delete:{property:[]}
					};
					if(crudData.name != data.name){
						body.update.widget = crudData;
					}
					$.each(fields, function(i,f){
						Utils.Array.addAll(body.create.property, f.editor.created(f.control));
						Utils.Array.addAll(body.update.property, f.editor.updated(f.control));
						Utils.Array.addAll(body.delete.property, f.editor.deleted(f.control));
					});
				}
				
				var success = function(data, status, xhr){
					var form = $.parseJSON(data);
 					designer.syncWidget(dataPane, parentData, data, false);
					Dialog.Progress.end();
				}
				var url = Utils.Url.render("/api/widget", params);
				RPMS.Action.doAjax(method, url, JSON.stringify(body), null, null, {success: success});
			},
			deleteWidget: function(event){
				var dataPane = ui.relativeData($(event.target));
				var data = dataPane.data("data");
				var parentData = dataPane.data("parentData");

				if(data.id == null){
					designer.syncWidget(dataPane, parentData, data, true);
				}
				else{
					var params = {"protocol-version": designer.protocol.id, 
							"protocol": designer.protocol.protocol.id,
							"widget": data.id};
					
					var success = function(data, status, xhr){
						var form = $.parseJSON(data);
	 					designer.syncWidget(dataPane, parentData, data, true);
						Dialog.Progress.end();
					}
					var url = Utils.Url.render("/api/widget", params);
					RPMS.Action.doAjax("DELETE", url, null, null, null, {success: success});
				}
			},
			syncWidget: function(pane, parentData, data, remove){
				if(remove){
					for(var i = parentData.contains.length - 1; i >=0 ; i--){
						var o = parentData.contains[i];
						if(o.id = data.id){
							parentData.contains.splice(i, 1);
						}
					}
					if(data.type == "section" | data.type == "page"){
						pane = pane.closest(".consent-designer-pane");
					}
					var li = pane.data("menuitem");
					if(li != null){
						li.remove();
					}
					pane.remove();
				}
				else{
					var container = pane.data("data");
					if(container.name != data.name){
						var options = pane.data("options");
						if(options && options.namelink){
							$.each(options.namelink, function(i,e){$(e).text(data.name)});
						}
					}
					container.id = data.id;
					container.name = data.name;
					container.type = container.type;
					
					var fields = pane.data("fields");
					$.each(fields, function(i,o){
						o.editor.refresh(o.control, data);
					});
				}
			}
		};
	}
})(window);
