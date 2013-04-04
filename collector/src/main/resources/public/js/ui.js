(function($) {
	$.fn.doubleTap = function(callback) {
		return this.each(function(){
			var el = this;
			var lastTap = 0;
			$(el).bind('touchend', function (e) {
				var now = (new Date()).valueOf();
				var diff = (now - lastTap);
				lastTap = now ;
				if (diff < 250) {
					e.stopPropagation();
					e.preventDefault();
					if($.isFunction( callback )){
						callback.call(e);
					}
				}      
			});
		});
	}
})(jQuery);

(function( window, undefined ) {
	var consent = window.Consent;
	if(consent == null){
		consent = window.Consent = {};
	}
	
	// UI Content Methods
	var ui = consent.UI;
	if(ui == null){
		ui = consent.UI = {};
	}
	
	ui.addBlockText = function(element, t1, t2){
		var adder = function(element, t, of, to){
			$("<div class='blocktext blocktext-" + of + "-" + to + "'>" + t + "</div>").appendTo(element);
		}
		if(t2 != null){
			adder(element, t1, 1, 2);
			adder(element, t2, 2, 2);
		}
		else{
			adder(element, t1, 1, 1);
		}
	};
	
	ui.addBlockTextSmall = function(element, t1, t2, t3){
		var adder = function(element, t, of, to){
			$("<div class='blocktextsmall blocktextsmall-" + of + "-" + to + "'>" + t + "</div>").appendTo(element);
		}
		if(t3 != null){
			adder(element, t1, 1, 3);
			adder(element, t2, 2, 3);
			adder(element, t3, 3, 3);
		}
		else if(t2 != null){
			adder(element, t1, 1, 2);
			adder(element, t2, 2, 2);
		}
		else{
			adder(element, t1, 1, 1);
		}
	};
	
	// Page Element Methods
	var page = ui.Page;
	if(page == null){
		page = ui.Page = {};
	}

	page.getPage = function(element){
		element = Utils.toJQO(element);
		if(element.hasClass("page")){
			return element;
		}
		else{
			return element.parents(".page:first");
		}
	};
	
	page.getHeader = function(element){
		element = Utils.toJQO(element);
		if(element.hasClass("header")){
			return element;
		}
		else{
			return page.getPage(element).find(".header:first");
		}
	};
	
	page.getHeaderLeft = function(element){
		return page.getHeader(element).find(".left-block:first");
	};
	
	page.getHeaderCenter = function(element){
		return page.getHeader(element).find(".center-block:first");
	};
	
	page.getHeaderRight = function(element){
		return page.getHeader(element).find(".right-block:first");
	};
	
	page.getContent = function(element){
		element = Utils.toJQO(element);
		if(element.hasClass("content")){
			return element;
		}
		else{
			return page.getPage(element).find(".content:first");
		}
	};
	
	page.getFooter = function(element){
		element = Utils.toJQO(element);
		if(element.hasClass("footer")){
			return element;
		}
		else{
			return page.getPage(element).find(".footer:first");
		}
	};
	
	page.getFooterLeft = function(element){
		return page.getFooter(element).find(".left-block:first");
	};
	
	page.getFooterCenter = function(element){
		return page.getFooter(element).find(".center-block:first");
	};
	
	page.getFooterRight = function(element){
		return page.getFooter(element).find(".right-block:first");
	};
	
	page.getLayout = function(element){
		return {
			content: page.getContent(element),
			header: page.getHeader(element),
			headerRight: page.getHeaderRight(element),
			headerCenter: page.getHeaderCenter(element),
			headerLeft: page.getHeaderLeft(element),
			footer: page.getFooter(element),
			footerRight: page.getFooterRight(element),
			footerCenter: page.getFooterCenter(element),
			footerLeft: page.getFooterLeft(element)
		};
	};
	
	// UI Form Generation
	var form = ui.Form;
	if(form == null){
		form = ui.Form = {};
	}
	
	var formWidgets = form.Widgets;
	if(formWidgets == null){
		formWidgets = form.Widgets = {};
	}
	
	var metaWidgets = form.MetaWidgets;
	if(metaWidgets == null){
		metaWidgets = form.MetaWidgets = {};
	}

	form.properties = function(item, key){
		return item.properties.filter("key", key);
	};

	form.property = function(item, key){
		return form.properties(item, key).first();
	};

	form.propertyValue = function(item, key){
		var prop = form.properties(item, key).first();
		if(prop){
			return prop.value;
		}
		return null;
	};

	form.propertyBooleanValue = function(item, key){
		return (form.propertyValue(item, key) == "true");
	};

	form.propertyTextValue = function(item, key, delim){
		var value = form.propertyValue(item, key);
		if($.isArray(value)){
			return value.join((delim || " "));
		}
		return value;
	};
	
	form.text = function(textValue, textDefault, joiner){
		var t = (textValue || textDefault);
		if($.isArray(t)){
			t = t.join((joiner || " "));
		}
		return t;
	};
	
	form.boolean = function(value){
		if(value == true || value ==  "true" || (value != null && value != "" && value != false && value != "false")){
			return true;
		}
		return false;
	};

	form.signature = function(parentElement, titleText, actionLabel, instructions, callbacks){
		var control = {};
		control.container = $("<div class='form-signature' />");
		control.container.appendTo(parentElement);
		
		control.header = $("<div class='ui-bar ui-bar-d' />");
		control.header.appendTo(control.container);
		
		control.title = $("<h4>" + titleText + "</h4>");
		control.title.appendTo(control.header);
		
		control.actionButton = $("<a href='#' data-mini='true' data-inline='true' data-role='button' data-icon='edit'>" + actionLabel + "</a>");
		control.actionButton.appendTo(control.header);
		
		control.image = $("<img class='form-signature-image' src='" + 
				(callbacks.getImageURL() || Utils.Url.render("/images/white.gif")) + "'/>");
		control.image.appendTo(control.container);
		
		control.info = $("<h6>" + instructions + "</h6>");
		control.info.appendTo(control.container);
		
		var launchFn = callbacks.onaction;
		if(launchFn == null){
			var options = {};
			if(callbacks.oncancel){options.oncancel = function(s){callbacks.oncancel(s, control);}};
			if(callbacks.onclear){options.onclear = function(s){callbacks.onclear(s, control);}};
			if(callbacks.onok){options.onok = function(s){options.imageURL = s.getDataURL(); callbacks.onok(s, control);}};
			launchFn = function(event){
				options.imageURL = callbacks.getImageURL();
				Consent.Dialog.signature("Sign Below", options);
			};
		}
		control.image.dblclick(launchFn);
		control.image.doubleTap(launchFn);
		control.actionButton.click(launchFn);
	};
	
	form.submit = function(state){
		var url = state.submitUrl;
		var method = state.submitMethod;
		var data = Consent.Collect.getData();
		url = Utils.Url.render(url);
		Consent.Request.api(method, url, data);
	};
	
	form.nextProtocol = function(state){
		var protocol = null;
		if(state.protocol != null){
			protocol = state.protocol.next;
			state.page = null;
		}
		else{
			protocol = state.protocols.first();
		}
		if(protocol != null){
			protocol.history = [];
			state.protocol = protocol;
			state.layout.headerCenter.empty();
			ui.addBlockText(state.layout.headerCenter, protocol.form.title.join(" "));
			form.nextPage(state);
		}
		else{
			form.submit(state);
		}
	};
	
	form.backProtocol = function(state){
		var protocol = state.protocol;
		protocol.history.clear();
		protocol = protocol.previous;
		if(protocol != null){
			state.protocol = protocol;
			state.layout.headerCenter.empty();
			ui.addBlockText(state.layout.headerCenter, protocol.form.title.join(" "));
			form.backPage(state);
		}
	};

	form.nextPage = function(state){
		var form = state.protocol.form;
		var pageId = null;
		if(state.page != null){
			state.protocol.history.push(state.page);
			pageId = ui.Form.propertyValue(state.page, "next");
			state.page = null;
		}
		else if(state.tangent != null){
			if(state.tangent.page != null){
				var next = ui.Form.propertyValue(state.tangent.page, "next");
				if(state.tangent.endPage == state.tangent.page.refid || next == null || next == "{none}"){
					return ui.Form.regress(state);
				}
				else{
					state.tangent.history.push(state.tangent.page);
					pageIe = next;
				}
			}
			else{
				pageId = state.tangent.startPage;
			}
		}
		else{
			pageId = form[state.startPageKey];
		}
		if(pageId != null && pageId != "{none}"){
			var page = form.contains.filter(function(entry){return (entry.refid == pageId)}).first();
			if(page != null){
				if(state.tangent != null){
					state.tangent.page = page;
				}
				else{
					state.page = page;
				}
				state.layout.content.empty();
				ui.Form.doWidget(state, page, form, state.layout.content);
			}
			else{
				Consent.Dialog.error("Development Error", "The page with ID '" + pageId + "' does not exist.");
			}
		}
		else{
			ui.Form.nextProtocol(state);
		}
	};
	
	form.backPage = function(state){
		if(state.tangent != null){
			if(state.tangent.history.length > 0){
				var page = state.tangent.history.pop();
				state.tangent.page = page;
				state.layout.content.empty();
				form.doWidget(state, page, state.protocol.form, state.layout.content);
			}
			else{
				form.regress(state);
			}
		}
		else if(state.protocol.history.length > 0){
			var page = state.protocol.history.pop();
			state.page = page;
			state.layout.content.empty();
			form.doWidget(state, page, state.protocol.form, state.layout.content);
		}
		else{
			form.backProtocol(state);
		}
	};
	
	form.digress = function(state, startPage, endPage){
		if(state.page){
			state.protocol.history.push(state.page);
			state.page = null;
		}
		state.tangent = {
			startPage: startPage,
			endPage: (endPage || startPage),
			history: []
		};
		form.nextPage(state);
	};
	
	form.regress = function(state){
		state.tangent = null;
		form.backPage(state);
	};
	
	form.initialize = function(element, options){
		var protocols = Consent.Collect.session.protocols;
		if(protocols && protocols.length > 0){
			var url = Utils.DataSet.get(element, "submit-url");
			var method = Utils.DataSet.get(element, "submit-method");
			var backLabel = (Utils.DataSet.get(element, "back-label") || "Back");
			var nextLabel = (Utils.DataSet.get(element, "next-label") || "Next");
			var state = $.extend({layout: Consent.UI.Page.getLayout(element), 
					protocols: protocols, 
					submitUrl: url,
					submitMethod: method,
					backLabel: backLabel,
					nextLabel: nextLabel}, options);
			form.nextProtocol(state);
		}
		else{
			Consent.Dialog.error("Development Error", "The protocols have not been set in the client session.");
		}
	};
	
	form.hasHistory = function(state){
		var protocol = state.protocol;
		while(protocol != null){
			if(protocol.history && protocol.history.length > 0){
				return true;
			}
			protocol = protocol.previous;
		}
		return false;
	};
	
	form.doWidget = function(state, node, parentNode, parentElement){
		var widget = formWidgets[node.type];
		if(widget != null){
			return widget.generate(state, node, parentNode, parentElement);
		}
		else{
			Consent.Dialog.error("Development Error", "The application does not have a form widget of type '" + node.type + "' registered.");
		}
	};
	
	form.doWidgetsContains = function(state, node, parentElement){
		var postTrigger = [];
		if(node.contains != null && node.contains.length > 0){
			$.each(node.contains, function(i, n){
				var ret = form.doWidget(state, n, node, parentElement);
				if($.isArray(ret)){
					postTrigger.addAll(ret);
				}
				else if($.isFunction(ret)){
					postTrigger.push(ret);
				}
			});
		}
		return postTrigger;
	};
	

	// Dynamic UI Generators
	var generators = ui.Generators;
	if(generators == null){
		generators = ui.Generators = {};
	}
	
	generators.collect = {
		build: function(element){
			form.initialize(element, {operation: "collect", startPageKey: "collect-start"});
		}
	};
	
	generators.review = {
		build: function(element){
			form.initialize(element, {operation: "review", startPageKey: "review-start"});
		}
	};
	
	generators["meta-items"] = {
		build: function(element){
			var url = Utils.DataSet.get(element, "submit-url");
			var method = (Utils.DataSet.get(element, "submit-method") || "POST");
			var label = (Utils.DataSet.get(element, "submit-label") || "Next");
			var metaItems = Consent.Collect.orderedMetaItems();
			if(metaItems != null && metaItems.length > 0){
				var form = $("<form data-ajax='false' data-theme='a' onsubmit='return false;' action='" +
						url + "' method='" + method + "' >");
				form.appendTo(element);
				$.each(metaItems, function(i, mi){
					ui.doMetaWidget(form, mi);
				});
				var button = $("<button class='action-form-submit' data-ajax='false' data-role='button' data-theme='a' type='button'>" + label + "</button>");
				button.appendTo(form);
				button.click(Consent.Form.submit);
				form.trigger("create");
			}
			else{
				Consent.Request.api(method, url);
			}
		}
	};
	
	generators["witness-signatures"] = {
		build: function(element){
			var parent = $(element);
			var url = Utils.DataSet.get(element, "submit-url");
			var method = (Utils.DataSet.get(element, "submit-method") || "POST");
			var save = function(){
				form.submit({submitUrl: url, submitMethod: method});
			};

			var message = Utils.DataSet.get(element, "message");
			if(message != null){
				$("<h4>" + message + "</h4>").appendTo(parent);
			}

			var protocols = Consent.Collect.protocols();
			var ids = [];
			var endorsements = {};
			for(var p = 0; p < protocols.length; p++){
				if($.isArray(protocols[p].form["witness-signatures"])){
					for(var w = 0; w < protocols[p].form["witness-signatures"].length; w++){
						var id = protocols[p].form["witness-signatures"][w];
						if(!ids.contains(id)){
							ids.push(id);
							endorsements[id] = [protocols[p].endorsements[id]];
						}
						else{
							endorsements[id].push(protocols[p].endorsements[id]);
						}
					}
				}
			}
			if(ids != null && ids.length > 0){
				$.each(ids, function(i, id){
					var endorsement = endorsements[id];
					var label = endorsement[0].label;
					var callbacks = {};
					callbacks.getImageURL = function(){return endorsement[0].value;}
					callbacks.onok = function(state, control){
						var data = state.getDataURL();
						control.image.attr("src", (data || Utils.Url.render("/images/white.gif")));
						for(var e = 0; e < endorsement.length; e++){
							endorsement[e].value = data;
						}
					};
					form.signature(parent, label, "Sign", "Doubletap or doubleclick to sign.", callbacks);
				});
				
				var label = (Utils.DataSet.get(element, "submit-label") || "Save");
				var button = $("<a data-role='button' href='#'>" + label + "</a>");
				button.appendTo(parent);
				button.click(function(event){
					var allSigned = true;
					$.each(ids, function(i, id){
						$.each(endorsements[id], function(i, endorsement){
							if(endorsement.value == null){
								allSigned = false;
							}
						});
					});
					if(allSigned){
						save();
					}
					else{
						var message = "The witness signatures have not been completed.  Selecting OK will save the forms without any witness signatures attached.";
						var options = {actions:[{label: "Cancel"}, {label: "Seriously, No"}, {label: "OK", callback: save}]};
						Consent.Dialog.error("Warning", message, options);
					}
				});
				parent.trigger("create");
			}
			else{
				save();
			}
		}
	};
	
	formWidgets["page"] = {
		generate: function(state, node, parentNode, parentElement){
			var page = $("<div class='form-page' />");
			page.appendTo(parentElement);
			if(node.title  && node.title.length > 0){
				var header = $("<div class='form-page-header'/>");
				header.appendTo(page);
				if(node.title.length == 1){
					ui.addBlockText(header, node.title[0]);
				}
				else{
					ui.addBlockText(header, node.title[0], node.title[1]);
				}
			}
			var content = $("<div class='form-page-content'/>");
			content.appendTo(page);
			var navigation = $("<div class='form-page-navigation ui-grid-a'/>");
			navigation.appendTo(page);
			var back = $("<div class='form-page-navigation-back ui-block-a'/>");
			back.appendTo(navigation);
			if(form.hasHistory(state)){
				var backAction = $("<button>" + state.backLabel + "</button>");
				backAction.appendTo(back);
				backAction.click(function(e){form.backPage(state)});
				page.on("swiperight", function(e){backAction.click()});
			}
			var next = $("<div class='form-page-navigation-back ui-block-b'/>");
			next.appendTo(navigation);
			var nextAction = $("<button>" + state.nextLabel + "</button>");
			nextAction.appendTo(next);
			nextAction.click(function(e){form.nextPage(state)});
			var postTrigger = form.doWidgetsContains(state, node, content);
			page.on("swipeleft", function(e){nextAction.click()});
			page.trigger("create");
			for(var i = 0; i < postTrigger.length; i++){
				postTrigger[i]();
			}
		}
	};
	
	formWidgets["section"] = {
		generate: function(state, node, parentNode, parentElement){
			var element = $("<div class='form-section'/>");
			element.appendTo(parentElement);
			return form.doWidgetsContains(state, node, element);
		}
	};

	formWidgets["data-change"] = {
		generate: function(state, node, parentNode, parentElement){
			var metaItemIds = form.propertyValue(node, "meta-items");
			var title = form.text(form.propertyTextValue(node, "title"), "Flag Information As Incorrect");
			
			var container = $("<div class='form-data-change'>");
			container.appendTo(parentElement);
	        
			var table = $("<table class='ui-body-d ui-shadow table-stripe ui-table'/>");
			table.appendTo(container);
			$("<thead><tr class='ui-bar-d'><th colspan='3'>" + title + "</th></tr></thead>").appendTo(table);
			var grid = $("<tbody />");
			grid.appendTo(table);
			
			if(metaItemIds != null && metaItemIds.length > 0){
				$.each(metaItemIds, function(index, id){
					var row = $("<tr />");
					row.appendTo(grid);
					var metaItem = Consent.Collect.metaItem(id);
					var text = ui.getMetaWidgetConsentingLabel(metaItem);
					$("<td><span>" + text + "</span></td>").appendTo(row);
					$("<td><span>" + metaItem.value + "</span></td>").appendTo(row);
					var block = $("<td />");
					block.appendTo(row);
					var val = "off";
					var options = "<option value='off' selected='selected'>Correct</option><option value='on'>Incorrect</option>";
					if(metaItem.flagged){
						val = "on";
						options = "<option value='off'>Correct</option><option value='on' selected='selected'>Incorrect</option>";
					}
					var toggle = $("<select data-role='slider' value='" + val + "'>" + options + "</div>");
					toggle.appendTo(block);
					toggle.change(function(event){
						if(event.target.value == "on"){
							Consent.Collect.metaItem(id).flagged = true;
						}
						else{
							Consent.Collect.metaItem(id).flagged = false;
						}
					});
				});
			}
			else{
				var title = $("<h1>NO META ITEMS HAVE BEEN SELECTED.</h1>");
				title.appendTo(container);
			}
		}
	};

	formWidgets["media"] = {
		generate: function(state, node, parentNode, parentElement){
			var element = $("<div class='form-media'>media: " + node.name + "</div>");
			element.appendTo(parentElement);
		}
	};

	formWidgets["policy-button"] = {
		generate: function(state, node, parentNode, parentElement){
			var label = form.text(form.propertyTextValue(node, "label"), "NO LABEL HAS BEEN CONFIGURED");
			var policyIds = form.propertyValue(node, "policy");
			var initializeAs = form.propertyValue(node, "initialize-as");
			var value = form.propertyBooleanValue(node, "action-value");
			if(!$.isArray(policyIds)){
				policyIds = [policyIds];
			}
			var button = $("<input type='button' data-role='button' value='" + label.escape() + "'/>");
			button.appendTo(parentElement);
			button.click(function(event){
				$.each(policyIds, function(index, policyId){
					state.protocol.policies[policyId].value = value;
				});
				button.parent().addClass($.mobile.activeBtnClass);
			});
			if(initializeAs != null && initializeAs != "" && initializeAs != "{none}"){
				initializeAs = form.boolean(initializeAs);
				$.each(policyIds, function(index, policyId){
					if(state.protocol.policies[policyId].value == null){
						state.protocol.policies[policyId].value = initializeAs;
					};
				});
			}
			var valueSet = true;
			$.each(policyIds, function(index, policyId){
				if(state.protocol.policies[policyId].value != value){
					valueSet = false;
				};
			});
			if(valueSet){
				return function(){button.parent().addClass($.mobile.activeBtnClass)};
			}
		}
	};

	formWidgets["policy-checkbox"] = {
		generate: function(state, node, parentNode, parentElement){
			var label = form.text(form.propertyTextValue(node, "label"), "NO LABEL HAS BEEN CONFIGURED");
			var policyIds = form.propertyValue(node, "policy");
			var initializeAs = form.propertyValue(node, "initialize-as");
			var checkedValue = form.propertyBooleanValue(node, "checked-value");
			var uncheckedValue = form.propertyBooleanValue(node, "unchecked-value");
			if(!$.isArray(policyIds)){
				policyIds = [policyIds];
			}
			var checked = true;
			if(initializeAs != null && initializeAs != "" && initializeAs != "{none}"){
				initializeAs = form.boolean(initializeAs);
				$.each(policyIds, function(index, policyId){
					if(state.protocol.policies[policyId].value == null){
						state.protocol.policies[policyId].value = initializeAs;
					};
				});
			}
			$.each(policyIds, function(index, policyId){
				if(state.protocol.policies[policyId].value != checkedValue){
					checked = false;
				};
			});
			var f = $("<form />");
			f.appendTo(parentElement);
			var lab = $("<label />");
			lab.appendTo(f);
			var input = $("<input type='checkbox' name='" + node.id + "' " + (checked ? " checked " : "") + " />");
			input.appendTo(lab);
			lab.append(label);
			input.click(function(event){
				$.each(policyIds, function(index, policyId){
					state.protocol.policies[policyId].value = (input.is(':checked') ? checkedValue : uncheckedValue);
				});
			});
		}
	};

	formWidgets["policy-choice-buttons"] = {
		generate: function(state, node, parentNode, parentElement){
			var trueLabel = form.text(form.propertyTextValue(node, "true-label"), "NO TRUE LABEL HAS BEEN CONFIGURED");
			var falseLabel = form.text(form.propertyTextValue(node, "false-label"), "NO FALSE LABEL HAS BEEN CONFIGURED");
			var policyIds = form.propertyValue(node, "policy");
			var initializeAs = form.propertyValue(node, "initialize-as");
			if(!$.isArray(policyIds)){
				policyIds = [policyIds];
			}
			var grid = $("<div class='ui-grid-a'/>");
			grid.appendTo(parentElement);
			var trueBlock = $("<div class='ui-block-a'/>");
			trueBlock.appendTo(grid);
			var falseBlock = $("<div class='ui-block-b'/>");
			falseBlock.appendTo(grid);
			
			var trueButton = $("<input type='button' data-role='button' value='" + trueLabel.escape() + "'/>");
			trueButton.appendTo(trueBlock);
			
			var falseButton = $("<input type='button' data-role='button' value='" + falseLabel.escape() + "'/>");
			falseButton.appendTo(falseBlock);

			trueButton.click(function(event){
				$.each(policyIds, function(index, policyId){
					state.protocol.policies[policyId].value = true;
				});
				trueButton.parent().addClass($.mobile.activeBtnClass);
				falseButton.parent().removeClass($.mobile.activeBtnClass);
			});
			falseButton.click(function(event){
				$.each(policyIds, function(index, policyId){
					state.protocol.policies[policyId].value = false;
				});
				falseButton.parent().addClass($.mobile.activeBtnClass);
				trueButton.parent().removeClass($.mobile.activeBtnClass);
			});

			if(initializeAs != null && initializeAs != "" && initializeAs != "{none}"){
				initializeAs = form.boolean(initializeAs);
				$.each(policyIds, function(index, policyId){
					if(state.protocol.policies[policyId].value == null){
						state.protocol.policies[policyId].value = initializeAs;
					};
				});
			}
			var setTrue = true;
			$.each(policyIds, function(index, policyId){
				if(state.protocol.policies[policyId].value != true){
					setTrue = false;
				};
			});
			if(setTrue){
				return function(){trueButton.parent().addClass($.mobile.activeBtnClass)};
			}
			else{
				var setFalse = true;
				$.each(policyIds, function(index, policyId){
					if(state.protocol.policies[policyId].value != false){
						setFalse = false;
					};
				});
				if(setFalse){
					return function(){falseButton.parent().addClass($.mobile.activeBtnClass)};
				}
			}
		}
	};
	
	formWidgets["policy-text"] = {
		generate: function(state, node, parentNode, parentElement){
			var container = $("<div class='form-policy-text'/>");
			container.appendTo(parentElement);
			var policyId = form.propertyValue(node, "policy");
			var policy = state.protocol.policies[policyId];
			if(form.propertyValue(node, "render-title") && policy.title != null){
				var text = form.text(policy.title, "No Policy Title Available", "<br/>");
				var title = $("<h4>" + text + "</h4>");
				title.appendTo(container);
			}
			if(form.propertyValue(node, "render-text") && policy.text != null){
				if(policy.text.length != null){
					for(var i = 0; i < policy.text.length; i++){
						var text = $("<p>" + policy.text[i] + "</p>");
						text.appendTo(container);
					}
				}
				else{
					var text = $("<p>" + policy.text + "</p>");
					text.appendTo(container);
				}	
			}
			if(form.propertyValue(node, "render-media") && policy.media != null){
				// To Be Completed
			}
		}
	};

	formWidgets["signature"] = {
		generate: function(state, node, parentNode, parentElement){
			var endorsementId = form.propertyValue(node, "endorsement");
			var label = form.text(state.protocol.endorsements[endorsementId].label, "No Label Configured.");
			var blankImage = Utils.Url.render("/images/white.gif");
			var callbacks = {
				getImageURL: function(){return state.protocol.endorsements[endorsementId].value;},
				onok: function(s, control){
					var data = s.getDataURL();
					if(data != null){
						state.protocol.endorsements[endorsementId].value = data;
						control.image.attr("src", data);
					}
					else{
						state.protocol.endorsements[endorsementId].value = null;
						control.image.attr("src", blankImage);
					}
				}
			};
			
			form.signature(parentElement, label, "Sign", "Doubletap or doubleclick to sign.", callbacks);
		}
	};

	formWidgets["text"] = {
		generate: function(state, node, parentNode, parentElement){
			var titleProp = form.propertyValue(node, "title");
			var textProp = form.propertyValue(node, "text");
			if(titleProp){
				var text = form.text(titleProp, titleProp, "<br/>");
				var title = $("<h4 class='form-text'>" + text + "</h4>");
				title.appendTo(parentElement);
			}
			if(textProp){
				if($.isArray(textProp)){
					for(var i = 0; i < textProp.length; i++){
						var text = $("<p class='form-text'>" + textProp[i] + "</p>");
						text.appendTo(parentElement);
					}
				}
				else{
					var text = $("<p class='form-text'>" + textProp + "</p>");
					text.appendTo(parentElement);
				}	
			}
		}
	};

	formWidgets["review-signature"] = {
		generate: function(state, node, parentNode, parentElement){
			var endorsementId = form.propertyValue(node, "endorsement");
			var title = form.text(form.propertyValue(node, "title"), "Review");
			var actionLabel = form.text(form.propertyValue(node, "label"), "Review");
			var returnPage = form.propertyValue(node, "returnpage");
			var thruPage = form.propertyValue(node, "thrupage");
			
			var titleText = title + ": " + state.protocol.endorsements[endorsementId].label;

			var callbacks = {
				getImageURL: function(){return state.protocol.endorsements[endorsementId].value;}
			};
			
			if(returnPage != null){
				callbacks.onaction = function(event){form.digress(state, returnPage, thruPage);};
			}
			else{
				callbacks.onok = function(s, control){
					var data = s.getDataURL();
					if(data != null){
						state.protocol.endorsements[endorsementId].value = data;
						control.image.attr("src", data);
					}
					else{
						state.protocol.endorsements[endorsementId].value = null;
						control.image.attr("src", blankImage);
					}
				};
			}
			form.signature(parentElement, titleText, actionLabel, "Doubletap or doubleclick to edit.", callbacks);
		}
	};
	
	formWidgets["review-endorsement"] = {
			generate: function(state, node, parentNode, parentElement){
				return formWidgets["review-signature"].generate(state, node, parentNode, parentElement);
			}
	};
	
	formWidgets["review-metaitem"] = {
		generate: function(state, node, parentNode, parentElement){
			var title = form.text(form.propertyValue(node, "title"), "No Review Meta Item Title Available");
			var metaItemId = form.propertyValue(node, "meta-item");
			var label = form.text(form.propertyValue(node, "label"), "Review");
			
			var container = $("<div class='form-review-metaitem'>");
			container.appendTo(parentElement);
	        
			var table = $("<table class='ui-body-d ui-shadow table-stripe ui-table'/>");
			table.appendTo(container);
			$("<thead><tr class='ui-bar-d'><th colspan='3'>" + title + "</th></tr></thead>").appendTo(table);
			var grid = $("<tbody />");
			grid.appendTo(table);
			
			if(metaItemId != null){
				var metaItem = Consent.Collect.metaItem(metaItemId);
				var text = ui.getMetaWidgetDefaultLabel(metaItem);
				var row = $("<tr />");
				row.appendTo(grid);
				$("<td>" + text + "</td>").appendTo(row);
				var valueCell = $("<td>" + metaItem.value + "</td>");
				valueCell.appendTo(row);
				var block = $("<td/>");
				block.appendTo(row);
				if(metaItem.flagged){
					var reviewButton = $("<a href='#' data-mini='true' data-inline='true' data-role='button' data-icon='edit'>" + label + "</a>");
					reviewButton.appendTo(block);
					reviewButton.click(function(event){
						Consent.Dialog.metaedit(null, metaItem, {onclear: function(state){metaItem.flagged = false;},
							onchange: function(state){
								metaItem.flagged = false;
								if(state.value != null){
									metaItem.value = state.value;
									valueCell.text(state.value);
								}
							}});
					});
				}
			}
			else{
				var title = $("<h1>A METAITEM HAS NOT BEEN HAVE BEEN SELECTED.</h1>");
				title.appendTo(container);
			}
		}
	};

	formWidgets["review-policy"] = {
		generate: function(state, node, parentNode, parentElement){
			var title = form.text(form.propertyValue(node, "title"), "No Review Policy Title Available");
			var policyId = form.propertyValue(node, "policy");
			var label = form.text(form.propertyValue(node, "label"), "Review");
			var returnPage = form.propertyValue(node, "returnpage");
			var thruPage = form.propertyValue(node, "thrupage");
			
			var container = $("<div class='form-review-policy'>");
			container.appendTo(parentElement);
	        
			var table = $("<table class='ui-body-d ui-shadow table-stripe ui-table'/>");
			table.appendTo(container);
			$("<thead><tr class='ui-bar-d'><th colspan='3'>" + title + "</th></tr></thead>").appendTo(table);
			var grid = $("<tbody />");
			grid.appendTo(table);
			
			if(policyId != null){
				var policy = state.protocol.policies[policyId];
				var text = policy.title.join != null ? policy.title.join("<br/>") : policy.title;
				var row = $("<tr />");
				row.appendTo(grid);
				$("<td>" + text + "</td>").appendTo(row);
				$("<td>" + policy.value + "</td>").appendTo(row);
				var block = $("<td/>");
				block.appendTo(row);
				var reviewButton = $("<a href='#' data-mini='true' data-inline='true' data-role='button' data-icon='edit'>" + label + "</a>");
				reviewButton.appendTo(block);
				reviewButton.click(function(event){
					form.digress(state, returnPage, thruPage);
				});
			}
			else{
				var title = $("<h1>A POLICY HAS NOT BEEN HAVE BEEN SELECTED.</h1>");
				title.appendTo(container);
			}
		}
	};
	
	ui.doMetaWidget = function(parent, metaItem, options){
		options = (options || {});
		var fn = metaWidgets[metaItem["data-type"]];
		if(fn != null){
			return fn(parent, metaItem, options);
		}
		else{
			return metaWidgets.badDataType(parent, metaItem, options);
		}
	};

	
	ui.getMetaWidgetLabel = function(metaItem, language){
		if($.isArray(metaItem.labels)){
			for(var i = 0; i < metaItem.labels.length; i++){
				if(metaItem.labels[i].language.id == language.id){
					return metaItem.labels[i].value;
				}
			}
		}
		return null;
	};
	
	ui.getMetaWidgetDefaultLabel = function(metaItem){
		var text = ui.getMetaWidgetLabel(metaItem, Consent.Collect.defaultLanguage());
		if(text == null){
			return metaItem.name;
		}
		if($.isArray(metaItem.labels) && metaItem.labels.length > 0){
			return metaItem.labels[0].value;
		}
		return text
	};
	
	ui.getMetaWidgetConsentingLabel = function(metaItem){
		var text = ui.getMetaWidgetLabel(metaItem, Consent.Collect.language());
		if(text == null){
			return ui.getMetaWidgetDefaultLabel(metaItem);
		}
		return text;
	};
	
	metaWidgets.container = function(parent){
		var div = $("<div data-role='fieldcontain'/>");
		div.appendTo(parent);
		return div;
	};
	
	metaWidgets.label = function(parent, metaItem, options){
		var text = (options != null && options.consenting == true) ? ui.getMetaWidgetConsentingLabel(metaItem) : ui.getMetaWidgetDefaultLabel(metaItem);
		var label = $("<label for='" + metaItem.id + "'>" + text + "</label>");
		label.appendTo(parent);
		return label;
	};

	metaWidgets.input = function(parent, metaItem, options, type, attributes){
		var value = (metaItem.value || metaItem["default-value"] || "");
		metaItem.value = value;
		type = (type || "text");
		options = (options || {});
		var html = "<input id='" + metaItem.id + "' name='" + metaItem.id + "' type='" + type + "' ";
		if(options.novalue != true){
			html = html + "value='" + value.escape() + "' ";
		}
		if(attributes){
			for(var key in attributes){
				var val = (attributes[key] || "");
				html = html + key + "='" + val.escape() + "' ";
			}
		}
		html = html + ">";
		var input = $(html);
		input.appendTo(parent);
		if(options.onchangeevent != null){
			input.change(options.onchangeevent);
		}
		else{
			var callback = (options.onchange || function(val){metaItem.value = val;});
			input.change(function(event){callback(input.val())});
		}
		return input;
	};
	
	metaWidgets.badDataType = function(parent, metaItem, options){
		options = (options || {});
		var container = metaWidgets.container(parent);
		metaWidgets.label(container, metaItem);
		metaWidgets.input(container, metaItem, options, null, {placeholder: "Datatype of metaitem has no editor"});
		return container;
	};
	
	metaWidgets.string = function(parent, metaItem, options){
		options = (options || {});
		var value = (metaItem.value || metaItem["default-value"] || "");
		var container = options.contain == false ? parent : metaWidgets.container(parent);
		metaWidgets.label(container, metaItem);
		metaWidgets.input(container, metaItem, options);
		return container;
	};
	
	metaWidgets.number = function(parent, metaItem, options){
		options = (options || {});
		var value = (metaItem.value || metaItem["default-value"] || "");
		var container = options.contain == false ? parent : metaWidgets.container(parent);
		metaWidgets.label(container, metaItem);
		metaWidgets.input(container, metaItem, options, "number", {pattern: "[0-9]*"});
		return container;
	};
	
	metaWidgets.date = function(parent, metaItem, options){
		options = (options || {});
		var value = (metaItem.value || metaItem["default-value"] || "");
		var container = options.contain == false ? parent : metaWidgets.container(parent);
		metaWidgets.label(container, metaItem);
		metaWidgets.input(container, metaItem, options, "date");
		return container;
	};
	
	metaWidgets.boolean = function(parent, metaItem, options){
		options = (options || {});
		var value = form.boolean((metaItem.value || metaItem["default-value"]));
		var attrs = {"data-checked-value": "true", "data-unchecked-value": "false"}
		if(value){
			attrs.checked = "checked";
		}
		options.novalue = true;
		if(options.onchangeevent == null){
			var callback = (options.onchange || function(val){metaItem.value = val;});
			options.onchangeevent = function(event){
				callback($(event.target).is(":checked"));
			};
		}

		var container = options.contain == false ? parent : metaWidgets.container(parent);
		metaWidgets.input(container, metaItem, options, "checkbox", attrs);
		metaWidgets.label(container, metaItem);
		return container;
	};
	
})( window );

$(document).on("pageinit", function(event){
	var generators = $(event.target).find(".clientuigenerator");
	if(generators && generators.length > 0){
		generators.each(function(index, element){
			var name = Utils.DataSet.get(element, "uigenerator");
			var generator = Consent.UI.Generators[name];
			if(generator != null){
				generator.build(element);
			}
			else{
				Consent.Dialog.error("Development Error", "'" + name + "' is not a valid UIGenerator.");
			}
		})
	}
});