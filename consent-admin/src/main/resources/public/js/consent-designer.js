(function(window, undefined){
	// Check If Designer Is There
	var consent = window.Consent;
	if(consent == null){
		window.Consent = consent = {};
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
				
				// Create Sections
				main.select = designer.createSelector(designer.container);
				main.content = designer.createContent(designer.container)
				
				// Create Expand Button
				main.resizer = $("<img class='consent-designer-resize' src='" + Utils.Url.render("/image/expand.png") + "'/>");
				main.resizer.appendTo(main.select);
				main.resizer.bind("click", designer.resize);
				
				// Create Main Selector
				main.selector = $("<ul class='consent-designer-selector'>");
				main.selector.appendTo(main.select);
				
				main.form = $("<li>Form</li>");
				main.collect = $("<li>Collect</li>");
				main.review = $("<li>Review</li>");
				main.preview = $("<li>Preview</li>");
				
				main.form.bind("click", designer.formDetails);
				main.collect.bind("click", designer.collectDetails);
				main.review.bind("click", designer.reviewDetails);
				main.preview.bind("click", designer.previewDetails);

				main.form.appendTo(main.selector);
				main.collect.appendTo(main.selector);
				main.review.appendTo(main.selector);
				main.preview.appendTo(main.selector);
				
				designer.main = main;
				main.form.click();
				
			},
			resize: function(){
				Dialog.inform({message: "Resizing of the layout manager is currently hibernating.  Try again later."})
			},
			formDetails: function(){
				designer.main.selector.children().removeClass("selected");
				designer.main.form.addClass("selected");
				if(designer.main.formPane == null){
					var form = designer.protocol.form;
					var pane = designer.createPane(designer.main.content);
					var data = designer.createData(pane, true);
					data.data("data", form);
					var fields = data.children().first();
					designer.createTextControl(fields, "Title", "form", form.id, "titles", form.titles);
					designer.main.formPane = pane;
					designer.main.panes.push(pane);
				}
				$.each(designer.main.panes, function(i,v){v.hide();});
				designer.main.formPane.show();
			},
			collectDetails: function(){
				designer.main.selector.children().removeClass("selected");
				designer.main.collect.addClass("selected");
				

				$.each(designer.main.panes, function(i,v){v.hide();});
			},
			reviewDetails: function(){
				designer.main.selector.children().removeClass("selected");
				designer.main.review.addClass("selected");
				

				$.each(designer.main.panes, function(i,v){v.hide();});
			},
			previewDetails: function(){
				designer.main.selector.children().removeClass("selected");
				designer.main.preview.addClass("selected");
				

				$.each(designer.main.panes, function(i,v){v.hide();});
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
			save: function(event){
				
			},
			delete: function(event){
				
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
		};
	}
})(window);
Consent.Designer.init();