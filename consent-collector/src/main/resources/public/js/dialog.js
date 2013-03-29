(function( window, undefined ) {
	var consent = window.Consent;
	if(consent == null){
		consent = window.Consent = {};
	}
	
	var dialog = consent.Dialog;
	if(dialog == null){
		dialog = consent.Dialog = {};
	}
	
	dialog.options = {
		informHeader: "Attention",
		informTheme: "a",
		informOverlayTheme: "d",
		confirmHeader: "Confirm",
		confirmTheme: "b",
		confirmOverlayTheme: "d",
		errorHeader: "Error",
		errorTheme: "e",
		errorOverlayTheme: "d",
		signatureHeader: "Sign Signature Pad",
		signatureTheme: "a",
		signatureOverlayTheme: "d",
		initialsHeader: "Initialize Initials Box",
		initialsTheme: "a",
		initialsOverlayTheme: "d",
		metaEditHeader: "Verify Data",
		metaEditTheme: "e",
		metaEditOverlayTheme: "d",
		lineWidth: 2,
		closeLabel: "Close",
		clearLabel: "Clear",
		cancelLabel: "Cancel",
		okLabel: "OK",
		layout: {
			horizontal: "horizontal",
			cgClasses : "dialog-horizontal-buttons  ui-corner-all ui-controlgroup ui-controlgroup-horizontal",
			cgcClasses: "ui-controlgroup-controls"
		}
	};

	dialog._prevent = function(event){
		event.stopPropagation();
		event.preventDefault();
	};

	dialog.createButton = function(state, parent, options){
		var html = "<a data-role='button' data-theme='" + options.theme + "' ";
		if(options.buttonAttributes){
			for(var key in options.buttonAttributes){
				html = html + key + "='" + options.buttonAttributes[key] + "' ";
			}
		}
		html = html + " >"+ options.label + "</a>"
		var button = $(html);
		button.appendTo(parent);
		if(options.buttonStyle){
			button.css(options.buttonStyle);
		}
		button.click(function(event){
			if(options.callback){
				options.callback(state);
			}
			if(options.keepOpen != true){
				state.close();
			}
		});
		return button;
	};

	dialog.grid = {
		grid: ["", "ui-grid-solo", "ui-grid-a", "ui-grid-b", "ui-grid-c", "ui-grid-d"],
		block: ["<div class='ui-block-a' />", "<div class='ui-block-b' />", "<div class='ui-block-c' />", "<div class='ui-block-d' />", "<div class='ui-block-e' />"]
	};

	dialog.createButtons = function(state, actions, options){
		state.actions.empty();
		if(actions && actions.length > 0){
			for(var i = 0; i < actions.length; i++){
				var opts = $.extend({}, options, actions[i]);
				dialog.createButton(state, state.actions, opts);
			}
		}
		else{
			var opts = $.extend({}, options, {label: dialog.options.closeLabel});
			dialog.createButton(state, state.actions, opts);
		}
	};
	
	dialog.createActionGroup = function(state, parent, actions, options){
		if(options.layout == dialog.options.layout.horizontal){
			state.actionsroot = $("<div class='" + dialog.options.layout.cgClasses + "' />");
			state.actionsroot.appendTo(parent);
			state.actions = $("<div class='" + dialog.options.layout.cgcClasses + "' />");
			state.actions.appendTo(state.actionsroot);
		}
		else{
			state.actions = $("<div />");
			state.actions.appendTo(parent);
		}
		dialog.createButtons(state, actions, options);
	};

	dialog.enableUnderlay = function(on){
		var state = dialog.underlay;
		if(state == null){
			state = dialog.underlay = {};
			state.container = $("<div class='dialog-underlay'/>");
			state.container.appendTo($("body"));
			state.container.click(dialog._prevent);
			state.container.on("gesturestart", dialog._prevent);
			state.container.on("gesturechange", dialog._prevent);
			state.container.on("gestureend", dialog._prevent);
		}
		if(on){
			state.container.show();
		}
		else{
			state.container.hide();
		}
	};

	dialog.state = {};

	dialog._create = function(stateId, header, title, message, actions, theme, overlayTheme){
		dialog.enableUnderlay(true);
		var state = dialog.state[stateId];
		if(state == null){
			state = dialog.state[stateId] = {};
			state.control = $("<div class='dialog-control ui-dialog-contain ui-overlay-shadow ui-corner-all'/>");
			state.control.appendTo($("body"));
			state.header = $("<div class='header ui-header ui-bar-" + theme + "'/>");
			state.header.appendTo(state.control);
			state.headerTitle = $("<h1 class='ui-title'>" + header + "</h1>");
			state.headerTitle.appendTo(state.header);
			state.content = $("<div class='ui-content ui-body-" + overlayTheme + "'/>");
			state.content.appendTo(state.control);
			state.title = $("<h4 class='dialog-title'>" + title + "</h4>");
			state.title.appendTo(state.content);
			state.text = $("<p>" + message + "</p>");
			state.text.appendTo(state.content);
			state.close = function(){
				state.control.hide();
				dialog.enableUnderlay(false);
			};
			dialog.createActionGroup(state, state.content, actions, {theme: theme});
			state.actions.trigger("create");
		}
		else{
			state.headerTitle.text(header);
			state.title.text(title);
			state.text.text(message);
			dialog.createButtons(state, actions, {theme: theme});
			state.actions.trigger("create");
		}
		state.control.show();
	};

	dialog.inform = function(title, message, options){
		options = (options || {});
		dialog._create("inform", 
			(options.header || dialog.options.informHeader),
			(title || "Attention"),
			(message || "There is no message."),
			options.actions,
			dialog.options.informTheme,
			dialog.options.informOverlayTheme);
	};

	dialog.error = function(title, message, options){
		options = (options || {});
		dialog._create("error", 
			(options.header || dialog.options.errorHeader),
			(title || "Attention"),
			(message || "There is no message."),
			options.actions,
			dialog.options.errorTheme,
			dialog.options.errorOverlayTheme);
	};

	dialog.confirm = function(title, message, options){
		options = (options || {});
		dialog._create("confirm", 
			(options.header || dialog.options.confirmHeader),
			(title || "Confirm"),
			(message || "There is no message."),
			options.actions,
			dialog.options.confirmTheme,
			dialog.options.confirmOverlayTheme);
	};

	dialog._createEndorsement = function(stateId, title, imageURL, height, width, lineWidth, callbacks, theme, overlayTheme){
		dialog.enableUnderlay(true);
		var buttonOptions = {theme: theme, buttonStyle: {width: "33%"}, layout: dialog.options.layout.horizontal};
		var state = dialog.state[stateId];
		var actions = [{label: (callbacks.cancelLabel || dialog.options.cancelLabel), callback: callbacks.oncancel},
					   {label: (callbacks.clearLabel || dialog.options.clearLabel), callback: (callbacks.onclear || function(s){s.clear();}), keepOpen: true},
					   {label: (callbacks.okLabel || dialog.options.okLabel), callback: callbacks.onok}];
		if(state == null){
			state = dialog.state[stateId] = {};
			state.control = $("<div class='dialog-control ui-dialog-contain ui-overlay-shadow ui-corner-all dialog-endorsement'/>");
			state.control.appendTo($("body"));
			state.header = $("<div class='header ui-header ui-bar-" + theme + "'/>");
			state.header.appendTo(state.control);
			state.headerTitle = $("<h1 class='ui-title'>" + title + "</h1>");
			state.headerTitle.appendTo(state.header);
			state.content = $("<div class='ui-content ui-body-" + overlayTheme + "'/>");
			state.content.appendTo(state.control);
			
			state.close = function(){
				state.clear();
				state.control.hide();
				dialog.enableUnderlay(false);
			};
			dialog.createActionGroup(state, state.content, actions, buttonOptions);

			// Create Canvas
			state.canvasbox = $("<div class='dialog-canvasbox' style='height:" + (height + 15) + "px' />")
			state.canvasbox.appendTo(state.content);

			state._canvas = $("<canvas class='dialog-canvas' style='margin-left:-" + (width/2) + "px' height='" +  height + "' width='" + width + "'></canvas>");
			state._canvas.appendTo(state.canvasbox);
			state.canvas = state._canvas[0];
			state.context = state.canvas.getContext("2d");
			state.hasData = false;

			state.image = new Image();
			state.image.onload = function() { state.context.drawImage(state.image, 0, 0); };
			if(imageURL != null && imageURL.length > 0){
				state.image.src = imageURL;
				state.hasData = true;
			}
			state.context.lineWidth = lineWidth;

			state.started = false;
			state.offset = null;
			state.clear = function(){
				state.image.src = "";
				state.context.clearRect(0, 0, width, height);
				state.hasData = false;
			};
			state.getDataURL = function(){
				if(state.hasData){
					return state.canvas.toDataURL();
				}
				return null;
			}
			state.setDataURL = function(url){
				if(url != null && url.length > 0){
					state.image.src = url;
				}
				else{
					state.clear();
				}
			}
			var coordinates = function(event){
				var coords = {x: 0, y: 0};
				if(state.offset == null){
					state.offset = state._canvas.offset();
				}
				if(typeof event.changedTouches != 'undefined') {
					coords.x = Math.floor(event.changedTouches[0].pageX - state.offset.left);
					coords.y = Math.floor(event.changedTouches[0].pageY - state.offset.top);
				}
				else if (event.pageX) { 
					coords.x = Math.floor(event.pageX - state.offset.left);
					coords.y = Math.floor(event.pageY - state.offset.top);
				} 	
				else {
					coords.x = eventArgs.x;
					coords.y = eventArgs.y;
				}
				return coords;
			};

			var start = function(event){
				dialog._prevent(event);
				var coord = coordinates(event);
				state.started = true;
				state.context.beginPath();
				state.context.moveTo(coord.x, coord.y);
				state.context.rect(coord.x, coord.y, 1, 1);
				state.context.stroke();
				state.hasData = true;
			};
			
			var move = function(event){
				if(state.started) {
					var coord = coordinates(event);
					state.context.lineTo(coord.x, coord.y);
					state.context.stroke();
					state.hasData = true;
				}
			};
			
			var stop = function(event){
				if(state.started){
					dialog._prevent(event);
					move(event);
					state.context.closePath();
					state.started = false;
				}
			};
			
			state.canvas.addEventListener("touchstart", start);
			state.canvas.addEventListener("mousedown", start);
			state.canvas.addEventListener("touchmove", move);
			state.canvas.addEventListener("mousemove", move);
			state.canvas.addEventListener("touchend", stop);
			state.canvas.addEventListener("mouseup", stop);
			state.canvas.addEventListener("mouseout", stop);
			$(document).scroll(function(event){state.offset = null;});
			$(window).bind('orientationchange', function(event){state.offset = null;});
		}
		else{
			state.offset = null;
			state.headerTitle.text(title);
			dialog.createButtons(state, actions, buttonOptions);
			state.setDataURL(imageURL);
		}
		state.control.show();
		state.actions.trigger("create");
	};

	dialog.signature = function(title, options){
		var width = 730;
		options = (options || {});
		dialog._createEndorsement("signature", 
			(title || dialog.options.signatureHeader), 
			options.imageURL, 
			width / 850 * 300, 
			width, 
			(options.lineWidth || dialog.options.lineWidth),
			options,
			dialog.options.signatureTheme, 
			dialog.options.signatureOverlayTheme)
	};
	

	dialog.metaedit = function(title, metaItem, options){
		title = (title || dialog.options.metaEditHeader)
		options = (options || {});
		dialog.enableUnderlay(true);
		var actions = [{label: "Cancel", callback: options.oncancel, image: "/images/status_cancel_64x64.png"},
		               {label: "Clear Flag", callback: options.onclear, image: "/images/status_refresh_64x64.png"},
		               {label: "Change", callback: options.onchange, image: "/images/status_success_64x64.png"}];
		var buttonOptions = {theme: dialog.options.metaEditTheme, buttonStyle: {width: "33%"}, layout: dialog.options.layout.horizontal};
		var state = dialog.state["metaedit"];
		if(state == null){
			state = dialog.state["metaedit"] = {};
			state.control = $("<div class='dialog-control ui-dialog-contain ui-overlay-shadow ui-corner-all'/>");
			state.control.appendTo($("body"));
			state.header = $("<div class='header ui-header ui-bar-" + dialog.options.metaEditTheme + "'/>");
			state.header.appendTo(state.control);
			state.headerTitle = $("<h1 class='ui-title'>" + title + "</h1>");
			state.headerTitle.appendTo(state.header);
			state.content = $("<div class='ui-content ui-body-" + dialog.options.metaEditOverlayTheme + "'/>");
			state.content.appendTo(state.control);
			state.close = function(){
				state.control.hide();
				dialog.enableUnderlay(false);
			};
			
			dialog.createActionGroup(state, state.content, actions, buttonOptions);
			state.form = $("<form onsubmit='return false;'/>");
			state.form.appendTo(state.content);
			state.onchange = function(value){state.value = value;};
			Consent.UI.doMetaWidget(state.form, metaItem, {onchange: state.onchange, contain: false});
		}
		else{
			state.value = null;
			state.headerTitle.text(title);
			dialog.createButtons(state, actions, {theme: dialog.options.metaEditTheme, buttonStyle: {width: "33%"}});
			state.form.empty();
			Consent.UI.doMetaWidget(state.form, metaItem, {onchange: state.onchange, contain: false});
		}
		state.control.show();
		state.content.trigger("create");
	};
})( window );
