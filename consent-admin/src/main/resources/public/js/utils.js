var Utils = {};
Utils.Url = {
	basepath: null, // The base path to prepend to URL.  Typically, this is just the contextPath of the application
	
	initBasePath: function(path){
		this.basepath = path;
	},
	
	// Generates a value URL using the basepath, the provided path, as well as any arguments to the function after
	// the path provided.  All extra arguments are expected to be maps that are rendered into request parameters.
	render: function(path){
		// Create Base URL
		if(this.basepath != null && path.indexOf(this.basepath) != 0){
			path = this.basepath + path;
		}
		
		// Add Parameters
		var parms = {}
		for(var i = 1; i < arguments.length; i++){
			parms = $.extend(parms, arguments[i]);
		}
		
		if(parms != null && !$.isEmptyObject(parms)){
			var con = "?";
			if(path.indexOf(con) > 0){
				con = "&";
			}
			path = path + con + $.param(parms);
		}
		return path;
	}
};

// Array Prototype Changes
Array.prototype.last = function(){
	if(this.length > 0){
		return this[this.length - 1];
	}
	return null;
};
Array.prototype.clear = function(){
	return this.splice(0, this.length);
};

Array.prototype.addAll = function(coll){
	if(coll == null) return this;
	if(coll instanceof Array){
		for(var i = 0; i < coll.length; i++){
			this.push(coll[i]);
		}
	}
	else{
		this.push(coll);
	}
	return this;
};
Array.prototype.contains = function(val){
	if(this.indexOf(val) >= 0) return true;
	return false;
};
Array.prototype.remove = function(val){
	var i = this.indexOf(val);
	if(i >= 0) return this.splice(i,1)[0];
	return null;
};
Array.prototype.alike = function(coll){
	if(coll == null || !(coll instanceof Array) || coll.length != this.length) return false;
	if(this == coll) return true;
	var c1 = coll.concat([]);
	var c2 = this.concat([]);
	for(var fi = (c1.length - 1); fi >= 0; fi--){
		for(var si = (c2.length - 1); si >= 0; si--){
			if(c1[fi] == c2[si]){
				c1.splice(fi, 1);
				c2.splice(si, 1);
			}
		}
	}
	if((c1.length + c2.length) > 0) return false;
	return true;
};
Array.prototype.fillTo = function(val){
	while(this.length < val){
		this.push(null);
	}
	return this;
};

Utils.Map = {
	// Ensures that a valid map is available
	map: function(map){
		if(map == null) return {};
		return map;
	},

	// Gets a value from a map if it exists, else returns a default value.
	mapped: function(map, key, defaultValue){
		if(map != null && map[key] != null){
			return map[key];
		}
		return defaultValue;
	},
	// Gets a value from a map if it exists, else throws an exception.
	mapped$rq: function(map, key){
		if(map != null && map[key] != null){
			return map[key];
		}
		throw "The '" + key + "' value is required.";
	},
	// Navigates a tree of maps using the additional parameters as keys.
	getin: function(map){
		var value = map;
		if(arguments.length > 1){
			for(var i = 1; i < arguments.length; i++){
				value = value[arguments[i]];
				if(value == null){
					return null;
				}
			}
		}
		return value;
	}
};

Utils.Size = {
	fill: function(target){
		this.fillDown(target);
		this.fillRight(target);
		
	},
	fillRight: function(target){
		var parent = target.parent();
		var parentWidth = parent.width();
		var targetPosition = target.position();
		var availableSpace = parentWidth - targetPosition.left;
		var outerWidth = target.outerWidth(true);
		var width = target.width();
		var widthDiff = availableSpace - outerWidth;
		target.width(width + widthDiff);
	},
	fillDown: function(target){
		var parent = target.parent();
		var parentHeight = parent.height();
		var targetPosition = target.position();
		var availableSpace = parentHeight - targetPosition.top;
		var outerHeight = target.outerHeight(true);
		var height = target.height();
		var heightDiff = availableSpace - outerHeight;
		target.height(height + heightDiff);
	},
	fillTo: function(target, sibling, buffer){
		this.fillDownTo(target, sibling, buffer);
		this.fillRightTo(target, sibling, buffer);
	},
	fillRightTo: function(target, sibling, buffer){
		if(isNaN(buffer)){
			buffer = 0;
		}
		var targetPosition = target.position();
		var siblingPosition = sibling.position();
		var availableSpace = siblingPosition.left - (targetPosition.left + buffer);
		var targetWidth = target.outerWidth(true);
		var widthDiff = availableSpace - targetWidth;
		var width = target.width();
		target.width(width + widthDiff);
	},
	fillDownTo: function(target, sibling, buffer){
		if(isNaN(buffer)){
			buffer = 0;
		}
		var targetPosition = target.position();
		var siblingPosition = sibling.position();
		var availableSpace = siblingPosition.top - (targetPosition.top + buffer);
		var targetHeight = target.outerHeight(true);
		var heightDiff = availableSpace - targetHeight;
		var height = target.height();
		target.width(height + heightDiff);
	},
	refill: function(target){
		target.find(".fill").each(function(index, e){
			Utils.Size.fill($(e));
		});
		target.find(".fill-down").each(function(index, e){
			Utils.Size.fillDown($(e));
		});
		target.find(".fill-right").each(function(index, e){
			Utils.Size.fillRight($(e));
		});
		target.find(".fill-to").each(function(index, e){
			e = $(e);
			var sibling = e.siblings(Utils.DataSet.get(e, "data-fillto"));
			var buffer = Utils.DataSet.getNumber(e, "data-fillbuffer", 0);
			Utils.Size.fillTo(e, sibling, buffer);
		});
		target.find(".fill-downto").each(function(index, e){
			e = $(e);
			var sibling = e.siblings(Utils.DataSet.get(e, "data-fillto"));
			var buffer = Utils.DataSet.getNumber(e, "data-fillbuffer", 0);
			Utils.Size.fillDownTo(e, sibling, buffer);
		});
		target.find(".fill-rightto").each(function(index, e){
			e = $(e);
			var sibling = e.siblings(Utils.DataSet.get(e, "data-fillto"));
			var buffer = Utils.DataSet.getNumber(e, "data-fillbuffer", 0);
			Utils.Size.fillRightTo(e, sibling, buffer);
		});
	}
};
Utils.DataSet = {
	keyName: function(key){
		if(key.indexOf("data-") == 0){
			key = key.substring(5);
		}
		return key;
	},
	get: function(target, key, defaultValue){
		key = this.keyName(key);
		var value = null;
		if(target.data){
			value = target.data(key)
		}
		else{
			value = $(target).data(key);
		}
		if(value == null){
			return defaultValue;
		}
		return value;
	},
	
	getBoolean: function(target, key, defaultValue){
		var val = this.get(target, key, defaultValue);
		if(val != null && (val == true || val == "true" || val == key || val == ("data-" + key))){
			return true;
		}
		return false;
	},
	
	getObject: function(target, key, defaultValue){
		var value = this.get(target, key);
		if(value != null && typeof value == 'string'){
			return $.parseJSON(value);
		}
		if(value == null){
			return defaultValue;
		}
		return value;
	},
	getNumber: function(target, key, defaultValue){
		var val = this.get(target, key);
		if(!isNaN(val)){
			return new Number(val).valueOf();
		}
		return defaultValue;
	},
	set: function(target, key, value){
		key = this.keyName(key);
		if(target.data){
			target.data(key, value);
		}
		else{
			$(target).data(key, value);
		}
	}
};
Utils.Element = {
	getAttribute: function(target, attribute, defaultValue){
		var value = null;
		if(target.getAttribute){
			value = target.getAttribute(attribute);
		}
		else if(target.attr){
			value = target.attr(attribute);
		} 
		if(value == null){
			return defaultValue;
		}
		return value;
	},
	getBooleanAttribute: function(target, attribute, defaultValue){
		var value = this.getAttribute(target, attribute, defaultValue);
		if(value != null && (value == true || value == "true" || value == attribute)){
			return true;
		}
		return false;
	},
	getObjectAttribute: function(target, attribute, defaultValue){
		var value = this.getAttribute(target, attribute, defaultValue);
		if(value != null){
			if(typeof value == 'string'){
				return $.parseJSON(value);
			}
			else{
				return value;
			}
		}
		return defaultValue;
	},
	getNumberAttribute: function(target, attribute, defaultValue){
		var value = this.get(target, attribute);
		if(!isNaN(value)){
			return new Number(value).valueOf();
		}
		return defaultValue;
	},
	setAttribute: function(target, attribute, value){
		if(target.setAttribute){
			target.setAttribute(attribute, value);
		}
		else if(target.attr){
			target.attr(attribute, value);
		}
		else{
			throw "You are using the Utils.Element.setAttribute method wrong.";
		}
	}
}
