(function( window, undefined ) {
	var consent = window.Consent;
	if(consent == null){
		consent = window.Consent = {};
	}
	
	var request = consent.Request;
	if(request == null){
		request = consent.Request = {};
	}
	
	request.page = function(url){
		document.location = url;
	};
	
	request.changePage = function(url, method, data, options){
		Consent.Dialog.loading();
		var pageRequest = {pageRequest: true};
		method = (method || "get")
		if(method == "get"){
			var data = $.extend(pageRequest, (data || {}));
			url = Utils.Url.render(url, data);
			data = null;
		}
		else{
			url = Utils.Url.render(url, pageRequest);
		}
		options = $.extend({type: method, data: data}, (options || {}));
		$.mobile.changePage(url, options);
	};
	
	request.resetSession = function(data){
		Consent.Collect.reset();
	};
	
	request.setLocation = function(data){
		var location = data.location;
		Consent.Collect.location(location);
	};
	
	request.setConsenter = function(data){
		var consenter = data.consenter;
		Consent.Collect.reset();
		Consent.Collect.consenter(consenter);
	};

	request.setEncounter = function(data){
		var encounter = data.encounter;
		Consent.Collect.encounter(encounter);
	};
	
	request.setProtocolIds = function(data){
		var protocolIds = data["protocol-ids"];
		Consent.Collect.protocolIds(protocolIds);
	};
	
	request.setLanguage = function(data){
		var language = data.language;
		Consent.Collect.language(language);
	};
	
	request.setProtocols = function(data){
		var protocols = data.protocols;
		Consent.Collect.protocols(protocols);
	};
	
	request.setMetaItems = function(data){
		var metaItems = data["meta-items"];
		Consent.Collect.metaItems(metaItems);
	};
	
	request.changeView = function(data){
		var url = Utils.Url.render(data["view-url"]);
		if(data.reset){
			request.changePage(url);
		}
		else{
			request.changePage(url);
		}
	};
	
	request.toRoot = function(data){
		var url = Utils.Url.render("");
		document.location = url;
	};
	
	request.inform = function(data){
		Consent.Dialog.inform(data.title, data.message);
	};
	
	request.onsuccess = function(data, status, xhr){
		if(data){
			data = $.parseJSON(data);
			if(data.actions && data.actions.length){
				var value = null;
				for(var i = 0 ; i < data.actions.length; i++){
					var fn = request[data.actions[i]]
					if(fn){
						value = fn(data, value);
					}
				}
			}
			else if(data.action){
				var fn = request[data.action];
				if(fn){
					fn(data);
				}
			}
		}
	};
	
	request.onfailure = function(xhr, status, text){
		if(xhr && xhr.responseText){
			var data = $.parseJSON(xhr.responseText);
			if(data.message){
				Consent.Dialog.error(data.title, data.message);
			}
			else{
				Consent.Dialog.error("Request Failed", "An error occured while trying to process the request.");
			}
		}
	};
	
	request.api = function(method, url, body, options){
		Consent.Dialog.loading();
		if(method == "get"){
			url = Utils.Url.render(url, body);
			body = null;
		}
		else{
			url = Utils.Url.render(url);
		}
		if(body && body.substring == null){
			body = JSON.stringify(body);
		}
		
		var settings = {
				data: body ,
				type: method,
				dataType: "text",
				processData: false, 
				accept: "application/json",
				contentType: "application/json",
				success: request.onsuccess,
				error: request.onfailure
		}
		if(options != null){
			settings = $.extend(settings, options);
		}
		$.ajax(url, settings);
	};
	
})( window );
