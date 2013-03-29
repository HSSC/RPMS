(function(window, undefined){
	// Check If Designer Is There
	var consent = window.Consent;
	if(consent == null){
		window.Consent = consent = {};
	}
	
	var keys = consent.Keys;
	if(keys == null){
		consent.Keys = keys = {};
		keys.collect = "collect";
		keys.review = "review";
		keys.form = "form";
		keys.page = "page";
		keys.section = "section";
		keys.control = "control";
	}
	
	var utils = consent.Utils;
	if(utils == null){
		consent.Utils = utils = {};
		utils.findByKeyValue = function(items, key, value, remove){
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
		};
		
		utils.findByName = function(items, name, remove){
			return utils.findByKeyValue(items, "name", name, remove);
		};

		utils.findProperties = function(name, props){
			var found = [];
			if(props != null){
				for(var i = 0; i < props.length; i++){
					if(props[i].key == name) found.push(props[i]);
				}
			}
			return found;
		};
		
		utils.findProperty = function(name, props, empty){
			var found = utils.findProperties(name, props);
			if(found.length > 0){
				return found[0];
			}
			else if(empty){
				return {key: name, value: null, language: null};
			}
			return null;
		};
		
		utils.findWidgetProperty = function(widget, name){
			if(widget != null && widget.properties != null){
				var props = widget.properties;
				for(var i = 0; i < props.length; i++){
					if(props[i].key == name) return props[i];
				}
			}
			return null;
		};
		
		utils.findWidgetPropertyById = function(widget, id){
			if(widget != null && widget.properties != null){
				var props = widget.properties;
				for(var i = 0; i < props.length; i++){
					if(props[i].id == id) return props[i];
				}
			}
			return null;
		};
		
		utils.getPagesByOperation = function(operation){
			var pages = consent.Designer.protocol.form.contains;
			var found = [];
			for(var i = 0; i < pages.length; i++){
				var prop = utils.findWidgetProperty(pages[i], "operation");
				if(prop != null && operation == prop.value){
					found.push(pages[i]);
				}
			}
			return found;
		};
		
		utils.getOrderedPages = function(operation, name, pageList, found){
			if(found == null) found = [];
			if(pageList == null) pageList = utils.getPagesByOperation(operation);
			if(utils.findByName(found, name) == null){
				var page = utils.findByName(pageList, name, true);
				if(page != null){
					found.push(page);
					var nextProp = utils.findWidgetProperty(page, "next");
					if(nextProp != null){
						utils.getOrderedPages(operation, nextProp.value, pageList, found);
					}
					else{
						found.addAll(pageList.clear());
					}
				}
				else{
					found.addAll(pageList.clear());
				}
			}
			else{
				found.addAll(pageList.clear());
			}
			return found;
		};
		
		utils.getWidgetValue = function(widget, property){
			var keyValue = Consent.Utils.findProperty(property.name, widget.properties, true);
			return keyValue.value;
		};
		
		utils.pageOptions = function(operation, value){
			var none = {value: null, label: "{none}", selected: true, data: null};
			var options = [none];
			var pages = Consent.Utils.getOrderedPages(operation, value);
			if(pages != null){
				$.each(pages, function(i,p){
					var option = {value: p.refid, label:p.name, data: p};
					if(value == p.refid){
						option.selected = true;
						none.selected = false;
					}
					options.push(option);
				});
			}
			return options;
		};
		
		utils.createOptions = function(coll, value){
			if(!(value instanceof Array)) value = [value];
			var options = [];
			$.each(coll, function(i, c){
				var option = {value: c.id, label: c.name, data: c};
				if(value.contains(c.id)){
					option.selected = true;
				}
				options.push(option);
			});
			return options;
		};
		
		utils.optionSorter = function(o1, o2){
			if(o1 && o2){
				if((o1.selected && o2.selected) || (!o1.selected && !o2.selected)){
					if(o1.label < o2.label){
						return -1;
					}
					else if(o1.label > o2.label){
						return 1;
					}
				}
				else if(o1.selected) return -1;
				else return 1;
			}
			else if(o1){
				return -1;
			}
			else if(o2){
				return 1;
			}
			return 0
		};
		
		utils.widgetSorter = function(o1, o2){
			if(o1 && o2){
				if(o1.order != null && o2.order != null){
					if(o1.order < o2.order){
						return -1;
					}
					else if(o1.order > o2.order){
						return 1;
					}
				}
				else if(o1.order != null) return -1;
				else if(o2.order != null) return 1;
			}
			else if(o1){
				return -1;
			}
			else if(o2){
				return 1;
			}
			return 0;
		};
		
		utils.LastRefID = 0;
		utils.nextRefID = function(){
			return utils.LastRefID = (utils.LastRefID + 1);
		};
		
		utils.accountForRefIDs = function(container){
			if(container.refid != null && container.refid > utils.LastRefID){
				utils.LastRefID = container.refid;
			}
			if($.isArray(container.contains)){
				$.each(container.contains, function(i, c){
					utils.accountForRefIDs(c);
				});
			}
		};
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
				deleted: function(control){return []},
				validate: function(control){return null}
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
		consent.UI = ui = {};
		
		ui.showPane = function(pane){
			pane.parent().children().hide();
			pane.show();
		};
	
		ui.relativeContent = function(item){
			return item.closest(".consent-designer-pane").children(".consent-designer-content");
		};
		
		ui.relativeData = function(item){
			return item.closest(".consent-designer-data");
		};
		
		ui.createPane = function(container){
			var pane = $("<div class='consent-designer-pane' />");
			pane.appendTo(container);
			return pane;
		};
		
		ui.createSelector = function(container){
			var selector = $("<div class='consent-designer-selector' />");
			selector.appendTo(container);
			return selector;
		};
		
		ui.createWidgetDashboard = function(container, options, addAction, sortAction){
			var dashboard = $("<div class='consent-designer-widget-dashboard' />");
			dashboard.appendTo(container);
			if(designer.editable){
				if(sortAction != null){
					var sort = $("<img src='" + Utils.Url.render("/image/sort.png") + "' />")
					sort.appendTo(dashboard);
					sort.bind("click", function(){sortAction(options);});
				}
				
				var add = $("<img src='" + Utils.Url.render("/image/add.png") + "' />")
				add.appendTo(dashboard);
				add.bind("click", function(){addAction(options);});
			}
			return dashboard;
		};
		
		ui.createHeader = function(container, label){
			var header = $("<div class='consent-designer-header'>" + label + "</div>");
			header.appendTo(container);
			return header;
		};
		
		ui.createMenuItem = function(menu, item){
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
		};
		
		ui.createMenu = function(container, items){
			var list = $("<ul class='consent-designer-selector'>");
			list.appendTo(container);
			if(items != null && items.length > 0){
				for(var i = 0; i < items.length; i++){
					var item = items[i];
					item.li = ui.createMenuItem(list, item);
					item.li.appendTo(list);
				}
				items[0].li.click();
			}
			return list;
		};
		
		ui.createContent = function(container){
			var content = $("<div class='consent-designer-content' />");
			content.appendTo(container);
			return content;
		};
		
		ui.createFormData = function(container){
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
		};
		
		ui.createControl = function(container){
			var wrap = $("<div class='consent-designer-input' />");
			wrap.appendTo(container);
			return wrap;
		};
		
		ui.createLabel = function(container, field, label){
			var label = $("<label class='consent-designer-label' >" + label + "</label>");
			label.appendTo(container);
			return label;
		},
		ui.createTextI18NControl = function(container, property, data, operation, editable){
			var keyValues = utils.findProperties(property.name, data.properties);
			var control =  ui.createTextControl(container, property.label, property.name, keyValues);
			control.data("paragraphs", property.paragraphs);
			return control;
		},
		ui.createTextControl = function(container, label, property, texts){
			var control = $("<div class='form-control-wrapper i18ntext consent-designer-input' />");
			control.appendTo(container);
			control.data("editable", designer.editable);
			control.data("name", property);
			control.data("languages", designer.protocol.languages);
			control.data("defaultlanguage", designer.protocol.organization.language);
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
		};
		
		ui.createMediaControl = function(container, property, data, operation, editable){
			// Get Properties
			var urlValue = utils.findProperty(property.name, data.properties, true);
			var posterValue = utils.findProperty(property.posterProp, data.properties, true);
			
			// Create Control And State
			var control = ui.createControl(container, editable);
			control.data("state", {url: urlValue, poster: posterValue});
			control.data("editable", designer.editable);
			
			ui.createLabel(control, property.name, property.label);
			
			// Create Table
			var table = $("<table class='consent-designer-media' />");
			table.appendTo(control);
			var header = $("<tr class='consent-designer-media'>" + 
				"<th class='consent-designer-media'>Media URL</th>" + 
				"<th class='consent-designer-media'>Poster URL</th></tr>")
			header.appendTo(table);
			
			// Reusable fn to add rows.
			var addRow = function(url, poster){
				var row = $("<tr class='consent-designer-media consent-designer-media-data'>");
				row.appendTo(table);
				$.each([url, poster], function(i, u){
					var cell = $("<td class='consent-designer-media'/>");
					cell.appendTo(row);
					var input = $("<input class='consent-designer-media' />");
					input.appendTo(cell);
					input.val(u);
					input.change(function(){control.data("changed", true);});
					if(!editable){
						input.attr("disabled", "disabled");
					}
				});
				if(editable){
					var cell = $("<td class='consent-designer-media-action'>");
					cell.appendTo(row);
					var img = $("<img class='consent-designer-media-action' src='" + 
							Utils.Url.render("/image/delete.png") + "'/>");
					img.appendTo(cell);
					img.click(function(){control.data("changed", true); row.remove();});
				}
			};
			
			if(editable){
				var addth = $("<th class='consent-designer-media-action' />");
				addth.appendTo(header);
				var img = $("<img class='consent-designer-media-action' src='" +
						Utils.Url.render("/image/add.png") + "'/>");
				img.appendTo(addth);
				img.click(function(){control.data("changed", true); addRow();});
			}
				
			if(urlValue.value){
				posterValue.value = [].addAll(posterValue.value).fillTo(urlValue.length);
				for(var i = 0; i < urlValue.value.length; i++){
					addRow(urlValue.value[i], posterValue.value[i]);
				}
			}
			return control;
		};
		
		ui.createInputControl = function(container, editable, field, label, value){
			var wrap = ui.createControl(container);
			ui.createLabel(wrap, field, label);
			ui.createInput(wrap, editable, field, value);
			return wrap;
		};
		
		ui.createInput = function(container, editable, field, value){
			var input = $("<input class='consent-designer-input' />");
			input.appendTo(container);
			input.val(value);
			input.data("field", field);
			input.data("state", value);
			if(!editable){
				input.attr("disabled", "disabled");
			}
			return input;
		};
		
		ui.createCheckbox = function(container, editable, name, value, checkedValue, uncheckedValue, defaultValue){
			var input = $("<input name='" + name + "' type='checkbox' class='consent-designer-checkbox' />");
			input.appendTo(container);
			input.data("checkedValue", checkedValue);
			input.data("uncheckedValue", uncheckedValue);
			input.data("defaultValue", defaultValue);
			if(value == checkedValue || (value == null && checkedValue == defaultValue)){
				input.attr("checked", "checked");
			}
			if(!editable){
				input.attr("disabled", "disabled");
			}
			return input;
		};
		
		ui.createCheckboxControl = function(container, property, data, operation, editable, checkedValue, uncheckedValue){
			var keyValue = Consent.Utils.findProperty(property.name, data.properties, true);
			var control = ui.createControl(container);
			control.data("state", keyValue);
			ui.createCheckbox(control, editable, keyValue.key, keyValue.value, 
					checkedValue, uncheckedValue, property.defaultValue)
			ui.createLabel(control, keyValue.key, property.label).addClass("consent-designer-checkbox");
			return control;
		};
		
		ui.createSelectOption  = function(select, option){
			var opt = $("<option>" + option.label + "</option>");
			opt.appendTo(select);
			
			if(option.value){
				opt[0].value = option.value;
			}
			if(option.selected){
				opt.attr("selected", "selected");
				if(option.value){
					select.value = option.value;
				}
			}
			opt.data("optionData", option.data);
		};
		
		ui.createPageSelect = function(container, editable, field, value, operation){
			var select = $("<select class='consent-designer-input' />");
			select.attr("name", field);
			select.appendTo(container);
			
			var options = utils.pageOptions(operation, value);
			if(options != null){
				$.each(options, function(i,o){
					ui.createSelectOption(select, o);
				});
			}
			if(!editable){
				select.attr("disabled", "disabled");
			}
			
			var updateOptions = function(e){
				var opts = select.children("option");
				var optsData = [];
				opts.each(function(i,o){
					o = $(o);
					var data = o.data("optionData");
					if(data != null){
						optsData.push({opt: o, data: data});
					}
				});
				var options = utils.pageOptions(operation, value);
				options.shift(); // Get Rid of {none}
				for(var i = (options.length - 1); i >= 0; i--){
					var option = options[i];
					for(var x = (optsData.length - 1); x >= 0; x--){
						var o = optsData[x];
						if(option.data.id == o.data.id){
							o.opt.value = option.value;
							o.opt.text(option.label);
							o.opt.data("optionData", option.data);
							options.splice(i, 1);
							optsData.splice(x, 1);
						}
					}
				}
				// Remove Old, Add New
				$.each(optsData, function(i, o){
					o.opt.remove();
				});
				$.each(options, function(i, o){
					ui.createSelectOption(select, o);
				});
			}
			select.bind("focus", updateOptions);
			return select;
		};
		
		ui.createPageSelectControl = function(container, property, data, operation, editable){
			var keyValue = utils.findProperty(property.name, data.properties, true);
			var control = ui.createControl(container, editable);
			control.data("state", keyValue);
			ui.createLabel(control, property.name, property.label);
			ui.createPageSelect(control, editable, property.name, keyValue.value, operation);
			return control;
		};
		
		ui.createSelect = function(container, editable, field, value, options){
			var select = $("<select class='consent-designer-input' />");
			select.attr("name", field);
			select.appendTo(container);
			
			options.sort(utils.optionSorter);
			
			options.unshift({value: null, label: "{none}"});
			
			$.each(options, function(i,o){
				ui.createSelectOption(select, o);
			});
			select.val(value);
			select.data("field", field);
			if(!editable){
				select.attr("disabled", "disabled");
			}
			return select;
		};
		
		ui.createSelectControl = function(container, property, data, operation, editable, options){
			var keyValue = utils.findProperty(property.name, data.properties, true);
			var control = ui.createControl(container, editable);
			control.data("state", keyValue);
			ui.createLabel(control, property.name, property.label);
			ui.createSelect(control, editable, property.name, keyValue.value, options);
			return control;
		};
		
		ui.createMultiSelect = function(container, editable, field, values, options){
			var select = $("<select class='consent-designer-input' multiple='multiple' size='4'/>");
			select.attr("name", field);
			select.appendTo(container);
			
			options.sort(utils.optionSorter);
			
			$.each(options, function(i,o){
				ui.createSelectOption(select, o);
			});
			select.data("field", field);
			if(!editable){
				select.attr("disabled", "disabled");
			}
			return select;
		};
		
		ui.createMultiSelectControl = function(container, property, data, operation, editable, options){
			var keyValue = utils.findProperty(property.name, data.properties, true);
			var control = ui.createControl(container, editable);
			control.data("state", keyValue);
			ui.createLabel(control, property.name, property.label);
			ui.createMultiSelect(control, editable, property.name, keyValue.value, options);
			return control;
		};
		
		ui.createWidgetData = function(container, widget, parentData, parentType, data, type, operation, options){
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
		};
	}
	
	var designer = consent.Designer;
	if(designer == null){
		consent.Designer = designer = {};
		
		designer.reset = function(){
			if(designer.container){
				designer.container.empty();
			}
			designer.container = null;
			designer.main = null;
			designer.collect = null;
			designer.review = null;
			designer.preview = null;
			designer.protocol = null;
		};
		
		designer.close = function(){
			Dialog.Progress.start();
			designer.reset();
			PaneManager.pop();
		};
		
		designer.init = function(){
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
			designer.editable = editable;

			utils.accountForRefIDs(designer.protocol.form);

			designer.formUrl = "/api/protocol/version/designer/form";
			designer.formParams = {"protocol-version": protocol.id};

			designer.widgetUrl = "/api/protocol/version/designer/widget";
			designer.widgetParams = {"protocol-version": protocol.id};
			designer.createUI();
		};
		
		designer.createUI = function(){
			var main = {panes: []};
			designer.main = main;
			
			// Create Sections
			main.select = ui.createSelector(designer.container);
			main.content = ui.createContent(designer.container);
			
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
		};
		
		designer.resize = function(){
			Dialog.inform({message: "Resizing of the layout manager is currently hibernating.  Try again later."})
		};
			
		designer.showFormProperties = function(){
			if(designer.main.formPane == null){
				var form = designer.protocol.form;
				var pane = ui.createPane(designer.main.content);
				var data = ui.createFormData(pane, true);
				data.data("data", form);
				var fields = data.children().first();
				ui.createTextControl(fields, "Title", "titles", form.titles);
				
				var makePageControl = function(property, label, operation){
					var value = form[property]
					var control = ui.createControl(fields, designer.editable);
					ui.createLabel(control, property, label);
					ui.createPageSelect(control, designer.editable, property, value, operation);
				}
				
				makePageControl("collect-start", "Collect Start Page", keys.collect);
				makePageControl("review-start", "Review Start Page", keys.review);
				
				var makeWitnessControl = function(property, label){
					var values = form[property];
					var control = ui.createControl(fields, designer.editable);
					var options = Consent.Utils.createOptions(Consent.Designer.protocol.endorsements, values);
					ui.createLabel(control, property, label);
					ui.createMultiSelect(control, designer.editable, property, values, options);
				}
				makeWitnessControl("witness-signatures", "Witness Signatures");
				
				designer.main.formPane = pane;
				designer.main.panes.push(pane);
			}
			ui.showPane(designer.main.formPane);
		};
		
		designer.showPagesPane = function(operation){
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
				ui.createWidgetDashboard(pages.selector, dashOptions, designer.addWidget, null);
				
				var items = [];
				// Get All Of The Page Widgets
				var start = form[operation + "-start"];
				var panes = utils.getOrderedPages(operation, (start || ""));
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
		};
		
		designer.showCollectPane = function(){
			return designer.showPagesPane(keys.collect);
		};
		
		designer.showReviewPane = function(){
			return designer.showPagesPane(keys.review);
		};
		
		designer.showPreviewPane = function(){
			if(designer.preview == null){
				var preview = designer.preview = {};
				preview.pane = ui.createPane(designer.main.content);
				preview.pane.text("Coming to an application near you in 2012.");
			}
			ui.showPane(designer.preview.pane);
		};
		
		designer.createWidgetView = function(container, item, li, options){
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
			
			// Check For RefID
			if(data && data.refid == null){
				data.refid = utils.nextRefID();
				data.hiddenMod = true;
			}
			
			// Create header
			var header = ui.createHeader(selector, label);
			
			// Create Section Dashboard
			var dashOptions = {
					parentType: "widget", 
					parentData: data,
					operation: operation, 
					type: subtype};
			ui.createWidgetDashboard(selector, dashOptions, designer.addWidget, designer.sortWidgets);
			
			// Create Details & SubType Header Link
			var items = [];
			items.push({label: "Details", action: designer.showWidgetProperties, 
				parentData: parentData, parentType: parentType,
				data: data, type: type, operation: operation, options: {namelink: [li, header]}});
			if(widgetSubtype != null){
				items.push({});
				items.push({label: widgetSubtype.label + "s"});
			}
			
			if(data != null && data.contains != null){
				var action = designer.showWidgetProperties;
				if(widgetSubtype && widgetSubtype.subtype){
					action = designer.showWidgetPane;
				}
				
				// Verify Order
				data.contains.sort(utils.widgetSorter);
				var last = data.contains.last();
				if(last && last.order == null && designer.editable){
					var body = {update: {widget: []}};
					$.each(data.contains, function(i,c){body.update.widget.push({id: c.id, order: i});});
					var params = {"protocol-version": designer.protocol.id, 
							"protocol": designer.protocol.protocol.id,
							"form": designer.protocol.form.id,
							"widget": data.id};
					var url = Utils.Url.render("/api/widget", params);
					Dialog.confirm({message: "Widgets without order have been found.  Confirm to apply default ordering.", onconfirm: function(){
						RPMS.Action.doAjax("POST", url, JSON.stringify(body), null, null, {});}});
				}
				
				// Create The Items
				for(var i = 0; i < data.contains.length; i++){
					var widget = data.contains[i];
					items.push({label: widget.name, 
						action: action,
						parentData: data, 
						parentType: "widget",
						data: widget, 
						type: subtype,
						operation: operation,
						orderable: true});
				}
			}
			
			dashOptions.list = ui.createMenu(selector, items);
			return pane;
		};
		
		designer.showWidgetPane = function(event){
			var li = $(event.target);
			var item = li.data("item");
			
			if(item.pane == null){
				var content = ui.relativeContent(li);
				item.pane = designer.createWidgetView(content, item, li);
			}
			ui.showPane(item.pane);
		};
		
		designer.addWidget = function(options){
			var type = options.type;
			var widgetType = widgets[type];
			var parentData = options.parentData;
			var parentType = options.parentType;
			var operation = options.operation;
			var list = options.list;
			var count = parentData.contains ? parentData.contains.length : 0;
			
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
			
			var reveal = function(widget){
				var name = widgets.generateName(widget);
				menuitem.data = {name: name, type: widget.type, order: count, refid: utils.nextRefID()};
				menuitem.label = name;
				var li = ui.createMenuItem(list, menuitem);
				menuitem.options.namelink = [li];
				li.click();
			};
			
			if(widgetType.widgets.length == 1){
				reveal(widgetType.widgets[0]);
			}
			else{
				var items = [];
				$.each(widgetType.widgets, function(i,o){
					if(o.operation == null || o.operation == operation){
						items.push({value: o.type, label: o.label, data: o});
					}
				});
				var chooseOptions = {};
				chooseOptions.title = "Choose " + widgetType.label;
				chooseOptions.items = items;
				chooseOptions.onchoose = function(w){(w && reveal(w))};
				Dialog.choose(chooseOptions);
			}
		};
		
		designer.sortWidgets = function(options){
			var list = options.list;
			var orderables = [];
			list.children().each(function(i, li){
				li = $(li);
				var item = li.data("item");
				if(item && item.orderable){
					orderables.push({label: li.text(), li: li});
				}
			});
			var order = function(ordered){
				var changed = [];
				$.each(ordered, function(i, item){
					if(item.originalIndex != item.newIndex){
						changed.push(item);
					}
				});
				if(changed.length > 0){
					var first = null;
					var widgets = [];
					$.each(ordered, function(i, item){
						var widget = item.li.data("item").data;
						widget.order = item.newIndex;
						if(widget.id){
							widgets.push(widget);
						}
						if(item.originalIndex == 0){
							first = item;
						}
					});
					
					var success = function(data, status, xhr){
						if(first != ordered[0]){
							ordered[0].li.insertBefore(first.li);
							first = ordered.shift();
						}
						while(ordered.length > 0){
							ordered[0].li.insertAfter(first.li);
							first = ordered.shift();
						}
						Dialog.Progress.end();
					}
					var body = {update: {widget: widgets}};
					var params = {"protocol-version": designer.protocol.id, 
							"protocol": designer.protocol.protocol.id,
							"form": designer.protocol.form.id,
							"widget": options.parentData.id};
					var url = Utils.Url.render("/api/widget", params);
					RPMS.Action.doAjax("POST", url, JSON.stringify(body), null, null, {success: success});
				}
			};
			Dialog.order({items: orderables, onok: order});
		};
			
		designer.showWidgetProperties = function(event){
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
				if(item.options == null){
					item.options = {namelink: [li]};
				}
				var content = ui.relativeContent(li);
				item.pane = ui.createWidgetData(content, widget, 
						item.parentData, item.parentType, data, type, item.operation, item.options);
				item.pane.data("menuitem", li);
			}
			ui.showPane(item.pane);
		};
		
		designer.saveForm = function(event){
			if(!designer.editable){
				return;
			}
			var form = designer.protocol.form;
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
			
			// Check Properties
			var formUpdates = {id: form.id};
			
			var selects = dataPane.find("select");
			selects.each(function(i,s){
				if(s.name != null){
					var val = form[s.name];
					var newVal = Utils.Element.getSelectValue(s);
					if(val != newVal){
						formUpdates[s.name] = newVal;
						body.update.form = formUpdates;
					}
				}
			});
			
			var params = {"protocol-version": designer.protocol.id, 
					"protocol": designer.protocol.protocol.id, "form": designer.protocol.form.id};
			var url = Utils.Url.render("/api/form", params);
			var success = function(data, status, xhr){
				var formData = $.parseJSON(data);
				textControl.data("value", formData.titles);
				textControl.data("updated", []);
				textControl.data("deleted", []);
				form["collect-start"] = formData["collect-start"];
				form["review-start"] = formData["review-start"];
				Dialog.Progress.end();
			}
			RPMS.Action.doAjax("POST", url, JSON.stringify(body), null, null, {success: success});
		};
		
		designer.saveWidget = function(event){
			if(!designer.editable){
				return;
			}
			var dataPane = ui.relativeData($(event.target));
			var nameField = dataPane.children().first().children().first().children("input");
			
			var fields = dataPane.data("fields");
			for(var i = 0; i < fields.length; i++){
				var response = fields[i].editor.validate(fields[i].control)
				if(response != null){
					Dialog.inform({title: "Validation Error", message: response});
					return;
				}
			}
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
			var action = "UPDATE";
			var body = null;
			var crudData = {name: nameField.val(), type: data.type, order: data.order, refid: data.refid};
			if(data.id == null){
				action = "CREATE";
				method = "PUT";
				crudData.properties = [{key: "operation", value: operation}];
				$.each(fields, function(i,f){
					crudData.properties.addAll(f.editor.created(f.control));
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
				if(crudData.name != data.name || data.hiddenMod == true){
					body.update.widget = crudData;
					data.hiddenMod = null;
				}
				$.each(fields, function(i,f){
					body.create.property.addAll(f.editor.created(f.control));
					body.update.property.addAll(f.editor.updated(f.control));
					body.delete.property.addAll(f.editor.deleted(f.control));
				});
			}
			
			var success = function(data, status, xhr){
				var dataObj = $.parseJSON(data);
				designer.syncWidget(dataPane, parentData, dataObj, action);
				Dialog.Progress.end();
			}
			var url = Utils.Url.render("/api/widget", params);
			RPMS.Action.doAjax(method, url, JSON.stringify(body), null, null, {success: success});
		};
	
		designer.deleteWidget = function(event){
			if(!designer.editable){
				return;
			}
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
				
				var success = function(responseText, status, xhr){
 					designer.syncWidget(dataPane, parentData, data, "DELETE");
					Dialog.Progress.end();
				}
				var url = Utils.Url.render("/api/widget", params);
				RPMS.Action.doAjax("DELETE", url, null, null, null, {success: success});
			}
		};
		
		designer.syncWidget = function(pane, parentData, data, action){
			if(action == "DELETE"){
				for(var i = parentData.contains.length - 1; i >=0 ; i--){
					var o = parentData.contains[i];
					if(o.id == data.id){
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
				if(container.properties == null){
					container.properties = [];
				}
				$.each(data.properties, function(i,p){
					var prop = utils.findWidgetPropertyById(container, p.id);
					if(prop == null){
						container.properties.push(p);
					}
					else{
						prop.key = p.key;
						prop.value = p.value;
						prop.language = p.language;
					}
				});
				
				var fields = pane.data("fields");
				$.each(fields, function(i,o){
					o.editor.refresh(o.control, container);
				});
				if(action == "CREATE"){
					if(parentData.contains){
						parentData.contains.push(container);
					}
					else{
						parentData.contains = [container];
					}
				}
			}
		};
	}
})(window);
