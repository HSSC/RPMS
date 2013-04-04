(function( window, undefined ) {
	var consent = window.Consent;
	if(consent == null){
		consent = window.Consent = {};
	}
	
	var form = consent.Form;
	if(form == null){
		form = consent.Form = {};
	}
	
	form.submit = function(event){
		var triggeredBy = $(event.target);
		var targetForm = triggeredBy.parents("form").first();
		if(targetForm != null && targetForm.length > 0){
			var data =  form.dataModel(targetForm, false);
			var url = targetForm.attr("action");
			var method = targetForm.attr("method");
			method = (method ? method.toLowerCase() : "post");
			consent.Request.api(method, url, data);
		}
	};
	
	form.submitState = function(event){
		var target = $(event.target);
		var targetForm = target.parents("form").first();
		if(targetForm != null && targetForm.length > 0){
			var required = Utils.DataSet.getObject(target, "required");
			var data =  Utils.DataSet.get(targetForm, "state");
			if(required != null && data == null){
				if(required.type == "inform"){
					Consent.Dialog.inform(required.title, required.message);
				}
				else{
					Consent.Dialog.inform(required.title, required.message);
				}
			}
			else{
				var url = targetForm.attr("action");
				var method = targetForm.attr("method");
				method = (method ? method.toLowerCase() : "post");
				consent.Request.api(method, url, data);
			}
		}
	};
	
	form.isButton = function(element){
		if(element){
			var tag = element.tagName.toLowerCase();
			if(tag == "button"){
				return true;
			}
			else if(tag == "input"){
				var type = element.type.toLowerCase();
				if(type == "submit" || type == "button"){
					return true;
				}
			}
		}
		return false;
	};
	
	form.buttonFilter = function(inputs){
		if(inputs != null && inputs.length > 0){
			var list = [];
			for(var i = 0; i < inputs.length; i++){
				if(!form.isButton(inputs[i])){
					list.push(inputs[i]);
				}
			}
			return list;
		}
		return inputs;
	};
	
	form.radioFilter = function(inputs){
		if(inputs != null && inputs.length > 0){
			var list = [];
			for(var i = 0; i < inputs.length; i++){
				var input = inputs[i];
				var type = (input.type ? input.type.toLowerCase() : "nope");
				if(type != "radio" || input.checked){
					list.push(input);
				}
			}
			return list;
		}
		return inputs;
	};

	form.partOfCustomInputFilter = function(inputs){
		if(inputs != null && inputs.length > 0){
			var list = [];
			for(var i = 0; i < inputs.length; i++){
				var input = inputs[i];
				if(!Utils.DataSet.getBoolean(input, "within-custom")){
					list.push(input);
				}
			}
			return list;
		}
		return inputs;
	};
	
	form.applyData = function(targetForm, data, options){
		options = (options || {});
		var filter = (options.filter || function(x){return x});
		
		// Get Inputs
		var inputs = targetForm.find(":input");
		inputs = form.buttonFilter(inputs);
		inputs = filter(inputs);
		
		if(options.resetAll){
			$(inputs).each(function(i,e){$(e).val("")});
		}
		
		var apply = function(fields, key, value){
			for(var i = 0; i < fields.length; i++){
				var field = fields[i];
				if(field.id == key || field.name == key){
					$(field).val(value);
					return true;
				}
			}
			return false;
		};
		for(var key in data){
			var val = data[key];
			if(!apply(inputs, key, val) && options.addDataAsHiddens){
				
			}
		}
		Utils.DataSet.set(targetForm, "state", data);
	};
	
	form.dataModel = function(targetForm, options){
		options = (options || {});
		var changes = options.changes;
		var filter = (options.filter || function(x){return x});
		var data = {};
		
		// Get Inputs
		var inputs = targetForm.find(":input");
		inputs = form.buttonFilter(inputs);
		inputs = form.partOfCustomInputFilter(inputs);
		inputs = form.radioFilter(inputs);
		inputs = filter(inputs);
		
		$(inputs).each(function(i, e){
			var name = e.name;
			var origValue = $.data(e, "original-value");
			var value = null;
			var tag = e.tagName.toLowerCase();
			var type = (tag == "input" ? e.type.toLowerCase() : tag);
			if(form.types[type]){
				value = form.types[type].get(e);
			}
			else{
				value = $(e).val();
			}
			if(changes != true || value != origValue){
				data[name] = value;
			}
		});
		// Get Custom Inputs
		var customs = targetForm.find(".custom-input");
		customs = filter(customs);
		customs.each(function(i, e){
			var typeName = Utils.DataSet.get(e, "type");
			var type = form.customTypes[typeName];
			if(type != null){
				var name = type.getName(e);
				var value = type.getValue(e);
				data[name] = value;
			}
		});
		return data;
	};
	
	form.types = {};
	
	form.types.checkbox = {
			set: function(e, v){
				var checkVal = Utils.DataSet.get(e, "checked-value", true);
				var uncheckVal = Utils.DataSet.get(e, "unchecked-value", false);
				if(v == checkedVal){
					e.checked = true;
				}
				else{
					e.checked = false;
				}
			},
			get: function(e){
				var checkVal = Utils.DataSet.get(e, "checked-value", true);
				var uncheckVal = Utils.DataSet.get(e, "unchecked-value", false);
				if(e.checked){
					return checkVal;
				}
				else{
					return uncheckVal;
				}
			},
			disable: function(e){
				var parent = $(e).parent("div.ui-checkbox");
				if(parent != null){
					parent.addClass("ui-disabled");
				}
			},
			isA: function(e){
				if(e.tagName.toLowerCase() == "input" && e.type.toLowerCase() == "checkbox"){
					return true;
				}
				return false;
			}
	};
	
	form.types.select = {
			set: function(e, v){
				var options = $(e).children("option:selected");
				$(options).each(function(i, o){o.selected = false;});
				if(v != null){
					if(Utils.Element.getBooleanAttribute(e, "multiple", false)){
						var vals = v.first != null ? v : [v];
						for(var x = 0; x < vals.length; x++){
							var val = vals[x];
							$(options).each(function(i, o){if(o.value = value){o.selected = true};});
						}
						
					}
					else{
						var val = v.first != null ? v.first() : v;
						$(options).each(function(i, o){if(o.value = value){o.selected = true};});
					}
				}
			},
			get: function(e){
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
				var value = vals;
				if(!Utils.Element.getBooleanAttribute(e, "multiple", false)){
					value = vals.length > 0 ? vals[0] : null;
				}
				return value;
			},
			disable: function(e){},
			isA: function(e){
				if(e.tagName.toLowerCase() == "select"){
					return true;
				}
				return false;
			}
	};
	
	form.customTypes = {};
	
	form.customTypes.checklist = {
			getName: function(e){
				return Utils.DataSet.get(e, "name");
			},
			getValue: function(e){
				var cbs = $(e).find("input");
				var values = [];
				cbs.each(function(i, input){
					if(input.checked){
						values.push(Utils.DataSet.get(input, "checked-value"));
					}
				});
				return values;
			},
			setValue: function(e, value){
				value = (value && value.length != null && value.length > 0) ? value : [];
				var cbs = $(e).find("input");
				cbs.each(function(i, input){
					input.checked = value.contains(Utils.DataSet.get(input, "checked-value"));
				});
			}
	};
	
	form.preventSubmit = function(event){
		event.preventDefault();
	};
	
	form.checkForDisabled = function(index, form){
		var inputs = $(form).find("input");
		inputs.each(function(index, element){
			if(Utils.DataSet.getBoolean(element, "disabled")){
				if(Consent.Form.types.checkbox.isA(element)){
					Consent.Form.types.checkbox.disable(element);
				}
				else{
					$(element).attr('readonly', 'readonly');
				}
			}
		});
		var selects = $(form).find("select");
		selects.each(function(index, element){
			$(element).attr('readonly', 'readonly');
		});
	};
})( window );

$(document).on("pageinit", function(event){
	var page = $(event.target);
	var forms = page.find("form");
	forms.on("submit", Consent.Form.preventSubmit);
	forms.each(Consent.Form.checkForDisabled);
});
