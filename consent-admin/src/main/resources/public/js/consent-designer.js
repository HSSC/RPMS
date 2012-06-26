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
			}
		};
	}
	
})(window)