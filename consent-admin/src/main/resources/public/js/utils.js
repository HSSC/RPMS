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
Utils.Misc = {
	nothing: function(){}
};
