var COMPILED = false;
var goog = goog || {};
goog.global = this;
goog.DEBUG = true;
goog.LOCALE = "en";
goog.evalWorksForGlobals_ = null;
goog.provide = function(name) {
  if(!COMPILED) {
    if(goog.getObjectByName(name) && !goog.implicitNamespaces_[name]) {
      throw Error('Namespace "' + name + '" already declared.');
    }
    var namespace = name;
    while(namespace = namespace.substring(0, namespace.lastIndexOf("."))) {
      goog.implicitNamespaces_[namespace] = true
    }
  }
  goog.exportPath_(name)
};
goog.setTestOnly = function(opt_message) {
  if(COMPILED && !goog.DEBUG) {
    opt_message = opt_message || "";
    throw Error("Importing test-only code into non-debug environment" + opt_message ? ": " + opt_message : ".");
  }
};
if(!COMPILED) {
  goog.implicitNamespaces_ = {}
}
goog.exportPath_ = function(name, opt_object, opt_objectToExportTo) {
  var parts = name.split(".");
  var cur = opt_objectToExportTo || goog.global;
  if(!(parts[0] in cur) && cur.execScript) {
    cur.execScript("var " + parts[0])
  }
  for(var part;parts.length && (part = parts.shift());) {
    if(!parts.length && goog.isDef(opt_object)) {
      cur[part] = opt_object
    }else {
      if(cur[part]) {
        cur = cur[part]
      }else {
        cur = cur[part] = {}
      }
    }
  }
};
goog.getObjectByName = function(name, opt_obj) {
  var parts = name.split(".");
  var cur = opt_obj || goog.global;
  for(var part;part = parts.shift();) {
    if(goog.isDefAndNotNull(cur[part])) {
      cur = cur[part]
    }else {
      return null
    }
  }
  return cur
};
goog.globalize = function(obj, opt_global) {
  var global = opt_global || goog.global;
  for(var x in obj) {
    global[x] = obj[x]
  }
};
goog.addDependency = function(relPath, provides, requires) {
  if(!COMPILED) {
    var provide, require;
    var path = relPath.replace(/\\/g, "/");
    var deps = goog.dependencies_;
    for(var i = 0;provide = provides[i];i++) {
      deps.nameToPath[provide] = path;
      if(!(path in deps.pathToNames)) {
        deps.pathToNames[path] = {}
      }
      deps.pathToNames[path][provide] = true
    }
    for(var j = 0;require = requires[j];j++) {
      if(!(path in deps.requires)) {
        deps.requires[path] = {}
      }
      deps.requires[path][require] = true
    }
  }
};
goog.require = function(rule) {
  if(!COMPILED) {
    if(goog.getObjectByName(rule)) {
      return
    }
    var path = goog.getPathFromDeps_(rule);
    if(path) {
      goog.included_[path] = true;
      goog.writeScripts_()
    }else {
      var errorMessage = "goog.require could not find: " + rule;
      if(goog.global.console) {
        goog.global.console["error"](errorMessage)
      }
      throw Error(errorMessage);
    }
  }
};
goog.basePath = "";
goog.global.CLOSURE_BASE_PATH;
goog.global.CLOSURE_NO_DEPS;
goog.global.CLOSURE_IMPORT_SCRIPT;
goog.nullFunction = function() {
};
goog.identityFunction = function(var_args) {
  return arguments[0]
};
goog.abstractMethod = function() {
  throw Error("unimplemented abstract method");
};
goog.addSingletonGetter = function(ctor) {
  ctor.getInstance = function() {
    return ctor.instance_ || (ctor.instance_ = new ctor)
  }
};
if(!COMPILED) {
  goog.included_ = {};
  goog.dependencies_ = {pathToNames:{}, nameToPath:{}, requires:{}, visited:{}, written:{}};
  goog.inHtmlDocument_ = function() {
    var doc = goog.global.document;
    return typeof doc != "undefined" && "write" in doc
  };
  goog.findBasePath_ = function() {
    if(goog.global.CLOSURE_BASE_PATH) {
      goog.basePath = goog.global.CLOSURE_BASE_PATH;
      return
    }else {
      if(!goog.inHtmlDocument_()) {
        return
      }
    }
    var doc = goog.global.document;
    var scripts = doc.getElementsByTagName("script");
    for(var i = scripts.length - 1;i >= 0;--i) {
      var src = scripts[i].src;
      var qmark = src.lastIndexOf("?");
      var l = qmark == -1 ? src.length : qmark;
      if(src.substr(l - 7, 7) == "base.js") {
        goog.basePath = src.substr(0, l - 7);
        return
      }
    }
  };
  goog.importScript_ = function(src) {
    var importScript = goog.global.CLOSURE_IMPORT_SCRIPT || goog.writeScriptTag_;
    if(!goog.dependencies_.written[src] && importScript(src)) {
      goog.dependencies_.written[src] = true
    }
  };
  goog.writeScriptTag_ = function(src) {
    if(goog.inHtmlDocument_()) {
      var doc = goog.global.document;
      doc.write('<script type="text/javascript" src="' + src + '"></' + "script>");
      return true
    }else {
      return false
    }
  };
  goog.writeScripts_ = function() {
    var scripts = [];
    var seenScript = {};
    var deps = goog.dependencies_;
    function visitNode(path) {
      if(path in deps.written) {
        return
      }
      if(path in deps.visited) {
        if(!(path in seenScript)) {
          seenScript[path] = true;
          scripts.push(path)
        }
        return
      }
      deps.visited[path] = true;
      if(path in deps.requires) {
        for(var requireName in deps.requires[path]) {
          if(requireName in deps.nameToPath) {
            visitNode(deps.nameToPath[requireName])
          }else {
            if(!goog.getObjectByName(requireName)) {
              throw Error("Undefined nameToPath for " + requireName);
            }
          }
        }
      }
      if(!(path in seenScript)) {
        seenScript[path] = true;
        scripts.push(path)
      }
    }
    for(var path in goog.included_) {
      if(!deps.written[path]) {
        visitNode(path)
      }
    }
    for(var i = 0;i < scripts.length;i++) {
      if(scripts[i]) {
        goog.importScript_(goog.basePath + scripts[i])
      }else {
        throw Error("Undefined script input");
      }
    }
  };
  goog.getPathFromDeps_ = function(rule) {
    if(rule in goog.dependencies_.nameToPath) {
      return goog.dependencies_.nameToPath[rule]
    }else {
      return null
    }
  };
  goog.findBasePath_();
  if(!goog.global.CLOSURE_NO_DEPS) {
    goog.importScript_(goog.basePath + "deps.js")
  }
}
goog.typeOf = function(value) {
  var s = typeof value;
  if(s == "object") {
    if(value) {
      if(value instanceof Array) {
        return"array"
      }else {
        if(value instanceof Object) {
          return s
        }
      }
      var className = Object.prototype.toString.call(value);
      if(className == "[object Window]") {
        return"object"
      }
      if(className == "[object Array]" || typeof value.length == "number" && typeof value.splice != "undefined" && typeof value.propertyIsEnumerable != "undefined" && !value.propertyIsEnumerable("splice")) {
        return"array"
      }
      if(className == "[object Function]" || typeof value.call != "undefined" && typeof value.propertyIsEnumerable != "undefined" && !value.propertyIsEnumerable("call")) {
        return"function"
      }
    }else {
      return"null"
    }
  }else {
    if(s == "function" && typeof value.call == "undefined") {
      return"object"
    }
  }
  return s
};
goog.propertyIsEnumerableCustom_ = function(object, propName) {
  if(propName in object) {
    for(var key in object) {
      if(key == propName && Object.prototype.hasOwnProperty.call(object, propName)) {
        return true
      }
    }
  }
  return false
};
goog.propertyIsEnumerable_ = function(object, propName) {
  if(object instanceof Object) {
    return Object.prototype.propertyIsEnumerable.call(object, propName)
  }else {
    return goog.propertyIsEnumerableCustom_(object, propName)
  }
};
goog.isDef = function(val) {
  return val !== undefined
};
goog.isNull = function(val) {
  return val === null
};
goog.isDefAndNotNull = function(val) {
  return val != null
};
goog.isArray = function(val) {
  return goog.typeOf(val) == "array"
};
goog.isArrayLike = function(val) {
  var type = goog.typeOf(val);
  return type == "array" || type == "object" && typeof val.length == "number"
};
goog.isDateLike = function(val) {
  return goog.isObject(val) && typeof val.getFullYear == "function"
};
goog.isString = function(val) {
  return typeof val == "string"
};
goog.isBoolean = function(val) {
  return typeof val == "boolean"
};
goog.isNumber = function(val) {
  return typeof val == "number"
};
goog.isFunction = function(val) {
  return goog.typeOf(val) == "function"
};
goog.isObject = function(val) {
  var type = goog.typeOf(val);
  return type == "object" || type == "array" || type == "function"
};
goog.getUid = function(obj) {
  return obj[goog.UID_PROPERTY_] || (obj[goog.UID_PROPERTY_] = ++goog.uidCounter_)
};
goog.removeUid = function(obj) {
  if("removeAttribute" in obj) {
    obj.removeAttribute(goog.UID_PROPERTY_)
  }
  try {
    delete obj[goog.UID_PROPERTY_]
  }catch(ex) {
  }
};
goog.UID_PROPERTY_ = "closure_uid_" + Math.floor(Math.random() * 2147483648).toString(36);
goog.uidCounter_ = 0;
goog.getHashCode = goog.getUid;
goog.removeHashCode = goog.removeUid;
goog.cloneObject = function(obj) {
  var type = goog.typeOf(obj);
  if(type == "object" || type == "array") {
    if(obj.clone) {
      return obj.clone()
    }
    var clone = type == "array" ? [] : {};
    for(var key in obj) {
      clone[key] = goog.cloneObject(obj[key])
    }
    return clone
  }
  return obj
};
Object.prototype.clone;
goog.bindNative_ = function(fn, selfObj, var_args) {
  return fn.call.apply(fn.bind, arguments)
};
goog.bindJs_ = function(fn, selfObj, var_args) {
  var context = selfObj || goog.global;
  if(arguments.length > 2) {
    var boundArgs = Array.prototype.slice.call(arguments, 2);
    return function() {
      var newArgs = Array.prototype.slice.call(arguments);
      Array.prototype.unshift.apply(newArgs, boundArgs);
      return fn.apply(context, newArgs)
    }
  }else {
    return function() {
      return fn.apply(context, arguments)
    }
  }
};
goog.bind = function(fn, selfObj, var_args) {
  if(Function.prototype.bind && Function.prototype.bind.toString().indexOf("native code") != -1) {
    goog.bind = goog.bindNative_
  }else {
    goog.bind = goog.bindJs_
  }
  return goog.bind.apply(null, arguments)
};
goog.partial = function(fn, var_args) {
  var args = Array.prototype.slice.call(arguments, 1);
  return function() {
    var newArgs = Array.prototype.slice.call(arguments);
    newArgs.unshift.apply(newArgs, args);
    return fn.apply(this, newArgs)
  }
};
goog.mixin = function(target, source) {
  for(var x in source) {
    target[x] = source[x]
  }
};
goog.now = Date.now || function() {
  return+new Date
};
goog.globalEval = function(script) {
  if(goog.global.execScript) {
    goog.global.execScript(script, "JavaScript")
  }else {
    if(goog.global.eval) {
      if(goog.evalWorksForGlobals_ == null) {
        goog.global.eval("var _et_ = 1;");
        if(typeof goog.global["_et_"] != "undefined") {
          delete goog.global["_et_"];
          goog.evalWorksForGlobals_ = true
        }else {
          goog.evalWorksForGlobals_ = false
        }
      }
      if(goog.evalWorksForGlobals_) {
        goog.global.eval(script)
      }else {
        var doc = goog.global.document;
        var scriptElt = doc.createElement("script");
        scriptElt.type = "text/javascript";
        scriptElt.defer = false;
        scriptElt.appendChild(doc.createTextNode(script));
        doc.body.appendChild(scriptElt);
        doc.body.removeChild(scriptElt)
      }
    }else {
      throw Error("goog.globalEval not available");
    }
  }
};
goog.cssNameMapping_;
goog.cssNameMappingStyle_;
goog.getCssName = function(className, opt_modifier) {
  var getMapping = function(cssName) {
    return goog.cssNameMapping_[cssName] || cssName
  };
  var renameByParts = function(cssName) {
    var parts = cssName.split("-");
    var mapped = [];
    for(var i = 0;i < parts.length;i++) {
      mapped.push(getMapping(parts[i]))
    }
    return mapped.join("-")
  };
  var rename;
  if(goog.cssNameMapping_) {
    rename = goog.cssNameMappingStyle_ == "BY_WHOLE" ? getMapping : renameByParts
  }else {
    rename = function(a) {
      return a
    }
  }
  if(opt_modifier) {
    return className + "-" + rename(opt_modifier)
  }else {
    return rename(className)
  }
};
goog.setCssNameMapping = function(mapping, style) {
  goog.cssNameMapping_ = mapping;
  goog.cssNameMappingStyle_ = style
};
goog.getMsg = function(str, opt_values) {
  var values = opt_values || {};
  for(var key in values) {
    var value = ("" + values[key]).replace(/\$/g, "$$$$");
    str = str.replace(new RegExp("\\{\\$" + key + "\\}", "gi"), value)
  }
  return str
};
goog.exportSymbol = function(publicPath, object, opt_objectToExportTo) {
  goog.exportPath_(publicPath, object, opt_objectToExportTo)
};
goog.exportProperty = function(object, publicName, symbol) {
  object[publicName] = symbol
};
goog.inherits = function(childCtor, parentCtor) {
  function tempCtor() {
  }
  tempCtor.prototype = parentCtor.prototype;
  childCtor.superClass_ = parentCtor.prototype;
  childCtor.prototype = new tempCtor;
  childCtor.prototype.constructor = childCtor
};
goog.base = function(me, opt_methodName, var_args) {
  var caller = arguments.callee.caller;
  if(caller.superClass_) {
    return caller.superClass_.constructor.apply(me, Array.prototype.slice.call(arguments, 1))
  }
  var args = Array.prototype.slice.call(arguments, 2);
  var foundCaller = false;
  for(var ctor = me.constructor;ctor;ctor = ctor.superClass_ && ctor.superClass_.constructor) {
    if(ctor.prototype[opt_methodName] === caller) {
      foundCaller = true
    }else {
      if(foundCaller) {
        return ctor.prototype[opt_methodName].apply(me, args)
      }
    }
  }
  if(me[opt_methodName] === caller) {
    return me.constructor.prototype[opt_methodName].apply(me, args)
  }else {
    throw Error("goog.base called from a method of one name " + "to a method of a different name");
  }
};
goog.scope = function(fn) {
  fn.call(goog.global)
};
goog.provide("goog.string");
goog.provide("goog.string.Unicode");
goog.string.Unicode = {NBSP:"\u00a0"};
goog.string.startsWith = function(str, prefix) {
  return str.lastIndexOf(prefix, 0) == 0
};
goog.string.endsWith = function(str, suffix) {
  var l = str.length - suffix.length;
  return l >= 0 && str.indexOf(suffix, l) == l
};
goog.string.caseInsensitiveStartsWith = function(str, prefix) {
  return goog.string.caseInsensitiveCompare(prefix, str.substr(0, prefix.length)) == 0
};
goog.string.caseInsensitiveEndsWith = function(str, suffix) {
  return goog.string.caseInsensitiveCompare(suffix, str.substr(str.length - suffix.length, suffix.length)) == 0
};
goog.string.subs = function(str, var_args) {
  for(var i = 1;i < arguments.length;i++) {
    var replacement = String(arguments[i]).replace(/\$/g, "$$$$");
    str = str.replace(/\%s/, replacement)
  }
  return str
};
goog.string.collapseWhitespace = function(str) {
  return str.replace(/[\s\xa0]+/g, " ").replace(/^\s+|\s+$/g, "")
};
goog.string.isEmpty = function(str) {
  return/^[\s\xa0]*$/.test(str)
};
goog.string.isEmptySafe = function(str) {
  return goog.string.isEmpty(goog.string.makeSafe(str))
};
goog.string.isBreakingWhitespace = function(str) {
  return!/[^\t\n\r ]/.test(str)
};
goog.string.isAlpha = function(str) {
  return!/[^a-zA-Z]/.test(str)
};
goog.string.isNumeric = function(str) {
  return!/[^0-9]/.test(str)
};
goog.string.isAlphaNumeric = function(str) {
  return!/[^a-zA-Z0-9]/.test(str)
};
goog.string.isSpace = function(ch) {
  return ch == " "
};
goog.string.isUnicodeChar = function(ch) {
  return ch.length == 1 && ch >= " " && ch <= "~" || ch >= "\u0080" && ch <= "\ufffd"
};
goog.string.stripNewlines = function(str) {
  return str.replace(/(\r\n|\r|\n)+/g, " ")
};
goog.string.canonicalizeNewlines = function(str) {
  return str.replace(/(\r\n|\r|\n)/g, "\n")
};
goog.string.normalizeWhitespace = function(str) {
  return str.replace(/\xa0|\s/g, " ")
};
goog.string.normalizeSpaces = function(str) {
  return str.replace(/\xa0|[ \t]+/g, " ")
};
goog.string.trim = function(str) {
  return str.replace(/^[\s\xa0]+|[\s\xa0]+$/g, "")
};
goog.string.trimLeft = function(str) {
  return str.replace(/^[\s\xa0]+/, "")
};
goog.string.trimRight = function(str) {
  return str.replace(/[\s\xa0]+$/, "")
};
goog.string.caseInsensitiveCompare = function(str1, str2) {
  var test1 = String(str1).toLowerCase();
  var test2 = String(str2).toLowerCase();
  if(test1 < test2) {
    return-1
  }else {
    if(test1 == test2) {
      return 0
    }else {
      return 1
    }
  }
};
goog.string.numerateCompareRegExp_ = /(\.\d+)|(\d+)|(\D+)/g;
goog.string.numerateCompare = function(str1, str2) {
  if(str1 == str2) {
    return 0
  }
  if(!str1) {
    return-1
  }
  if(!str2) {
    return 1
  }
  var tokens1 = str1.toLowerCase().match(goog.string.numerateCompareRegExp_);
  var tokens2 = str2.toLowerCase().match(goog.string.numerateCompareRegExp_);
  var count = Math.min(tokens1.length, tokens2.length);
  for(var i = 0;i < count;i++) {
    var a = tokens1[i];
    var b = tokens2[i];
    if(a != b) {
      var num1 = parseInt(a, 10);
      if(!isNaN(num1)) {
        var num2 = parseInt(b, 10);
        if(!isNaN(num2) && num1 - num2) {
          return num1 - num2
        }
      }
      return a < b ? -1 : 1
    }
  }
  if(tokens1.length != tokens2.length) {
    return tokens1.length - tokens2.length
  }
  return str1 < str2 ? -1 : 1
};
goog.string.encodeUriRegExp_ = /^[a-zA-Z0-9\-_.!~*'()]*$/;
goog.string.urlEncode = function(str) {
  str = String(str);
  if(!goog.string.encodeUriRegExp_.test(str)) {
    return encodeURIComponent(str)
  }
  return str
};
goog.string.urlDecode = function(str) {
  return decodeURIComponent(str.replace(/\+/g, " "))
};
goog.string.newLineToBr = function(str, opt_xml) {
  return str.replace(/(\r\n|\r|\n)/g, opt_xml ? "<br />" : "<br>")
};
goog.string.htmlEscape = function(str, opt_isLikelyToContainHtmlChars) {
  if(opt_isLikelyToContainHtmlChars) {
    return str.replace(goog.string.amperRe_, "&amp;").replace(goog.string.ltRe_, "&lt;").replace(goog.string.gtRe_, "&gt;").replace(goog.string.quotRe_, "&quot;")
  }else {
    if(!goog.string.allRe_.test(str)) {
      return str
    }
    if(str.indexOf("&") != -1) {
      str = str.replace(goog.string.amperRe_, "&amp;")
    }
    if(str.indexOf("<") != -1) {
      str = str.replace(goog.string.ltRe_, "&lt;")
    }
    if(str.indexOf(">") != -1) {
      str = str.replace(goog.string.gtRe_, "&gt;")
    }
    if(str.indexOf('"') != -1) {
      str = str.replace(goog.string.quotRe_, "&quot;")
    }
    return str
  }
};
goog.string.amperRe_ = /&/g;
goog.string.ltRe_ = /</g;
goog.string.gtRe_ = />/g;
goog.string.quotRe_ = /\"/g;
goog.string.allRe_ = /[&<>\"]/;
goog.string.unescapeEntities = function(str) {
  if(goog.string.contains(str, "&")) {
    if("document" in goog.global && !goog.string.contains(str, "<")) {
      return goog.string.unescapeEntitiesUsingDom_(str)
    }else {
      return goog.string.unescapePureXmlEntities_(str)
    }
  }
  return str
};
goog.string.unescapeEntitiesUsingDom_ = function(str) {
  var el = goog.global["document"]["createElement"]("div");
  el["innerHTML"] = "<pre>x" + str + "</pre>";
  if(el["firstChild"][goog.string.NORMALIZE_FN_]) {
    el["firstChild"][goog.string.NORMALIZE_FN_]()
  }
  str = el["firstChild"]["firstChild"]["nodeValue"].slice(1);
  el["innerHTML"] = "";
  return goog.string.canonicalizeNewlines(str)
};
goog.string.unescapePureXmlEntities_ = function(str) {
  return str.replace(/&([^;]+);/g, function(s, entity) {
    switch(entity) {
      case "amp":
        return"&";
      case "lt":
        return"<";
      case "gt":
        return">";
      case "quot":
        return'"';
      default:
        if(entity.charAt(0) == "#") {
          var n = Number("0" + entity.substr(1));
          if(!isNaN(n)) {
            return String.fromCharCode(n)
          }
        }
        return s
    }
  })
};
goog.string.NORMALIZE_FN_ = "normalize";
goog.string.whitespaceEscape = function(str, opt_xml) {
  return goog.string.newLineToBr(str.replace(/  /g, " &#160;"), opt_xml)
};
goog.string.stripQuotes = function(str, quoteChars) {
  var length = quoteChars.length;
  for(var i = 0;i < length;i++) {
    var quoteChar = length == 1 ? quoteChars : quoteChars.charAt(i);
    if(str.charAt(0) == quoteChar && str.charAt(str.length - 1) == quoteChar) {
      return str.substring(1, str.length - 1)
    }
  }
  return str
};
goog.string.truncate = function(str, chars, opt_protectEscapedCharacters) {
  if(opt_protectEscapedCharacters) {
    str = goog.string.unescapeEntities(str)
  }
  if(str.length > chars) {
    str = str.substring(0, chars - 3) + "..."
  }
  if(opt_protectEscapedCharacters) {
    str = goog.string.htmlEscape(str)
  }
  return str
};
goog.string.truncateMiddle = function(str, chars, opt_protectEscapedCharacters, opt_trailingChars) {
  if(opt_protectEscapedCharacters) {
    str = goog.string.unescapeEntities(str)
  }
  if(opt_trailingChars) {
    if(opt_trailingChars > chars) {
      opt_trailingChars = chars
    }
    var endPoint = str.length - opt_trailingChars;
    var startPoint = chars - opt_trailingChars;
    str = str.substring(0, startPoint) + "..." + str.substring(endPoint)
  }else {
    if(str.length > chars) {
      var half = Math.floor(chars / 2);
      var endPos = str.length - half;
      half += chars % 2;
      str = str.substring(0, half) + "..." + str.substring(endPos)
    }
  }
  if(opt_protectEscapedCharacters) {
    str = goog.string.htmlEscape(str)
  }
  return str
};
goog.string.specialEscapeChars_ = {"\x00":"\\0", "\u0008":"\\b", "\u000c":"\\f", "\n":"\\n", "\r":"\\r", "\t":"\\t", "\u000b":"\\x0B", '"':'\\"', "\\":"\\\\"};
goog.string.jsEscapeCache_ = {"'":"\\'"};
goog.string.quote = function(s) {
  s = String(s);
  if(s.quote) {
    return s.quote()
  }else {
    var sb = ['"'];
    for(var i = 0;i < s.length;i++) {
      var ch = s.charAt(i);
      var cc = ch.charCodeAt(0);
      sb[i + 1] = goog.string.specialEscapeChars_[ch] || (cc > 31 && cc < 127 ? ch : goog.string.escapeChar(ch))
    }
    sb.push('"');
    return sb.join("")
  }
};
goog.string.escapeString = function(str) {
  var sb = [];
  for(var i = 0;i < str.length;i++) {
    sb[i] = goog.string.escapeChar(str.charAt(i))
  }
  return sb.join("")
};
goog.string.escapeChar = function(c) {
  if(c in goog.string.jsEscapeCache_) {
    return goog.string.jsEscapeCache_[c]
  }
  if(c in goog.string.specialEscapeChars_) {
    return goog.string.jsEscapeCache_[c] = goog.string.specialEscapeChars_[c]
  }
  var rv = c;
  var cc = c.charCodeAt(0);
  if(cc > 31 && cc < 127) {
    rv = c
  }else {
    if(cc < 256) {
      rv = "\\x";
      if(cc < 16 || cc > 256) {
        rv += "0"
      }
    }else {
      rv = "\\u";
      if(cc < 4096) {
        rv += "0"
      }
    }
    rv += cc.toString(16).toUpperCase()
  }
  return goog.string.jsEscapeCache_[c] = rv
};
goog.string.toMap = function(s) {
  var rv = {};
  for(var i = 0;i < s.length;i++) {
    rv[s.charAt(i)] = true
  }
  return rv
};
goog.string.contains = function(s, ss) {
  return s.indexOf(ss) != -1
};
goog.string.removeAt = function(s, index, stringLength) {
  var resultStr = s;
  if(index >= 0 && index < s.length && stringLength > 0) {
    resultStr = s.substr(0, index) + s.substr(index + stringLength, s.length - index - stringLength)
  }
  return resultStr
};
goog.string.remove = function(s, ss) {
  var re = new RegExp(goog.string.regExpEscape(ss), "");
  return s.replace(re, "")
};
goog.string.removeAll = function(s, ss) {
  var re = new RegExp(goog.string.regExpEscape(ss), "g");
  return s.replace(re, "")
};
goog.string.regExpEscape = function(s) {
  return String(s).replace(/([-()\[\]{}+?*.$\^|,:#<!\\])/g, "\\$1").replace(/\x08/g, "\\x08")
};
goog.string.repeat = function(string, length) {
  return(new Array(length + 1)).join(string)
};
goog.string.padNumber = function(num, length, opt_precision) {
  var s = goog.isDef(opt_precision) ? num.toFixed(opt_precision) : String(num);
  var index = s.indexOf(".");
  if(index == -1) {
    index = s.length
  }
  return goog.string.repeat("0", Math.max(0, length - index)) + s
};
goog.string.makeSafe = function(obj) {
  return obj == null ? "" : String(obj)
};
goog.string.buildString = function(var_args) {
  return Array.prototype.join.call(arguments, "")
};
goog.string.getRandomString = function() {
  var x = 2147483648;
  return Math.floor(Math.random() * x).toString(36) + Math.abs(Math.floor(Math.random() * x) ^ goog.now()).toString(36)
};
goog.string.compareVersions = function(version1, version2) {
  var order = 0;
  var v1Subs = goog.string.trim(String(version1)).split(".");
  var v2Subs = goog.string.trim(String(version2)).split(".");
  var subCount = Math.max(v1Subs.length, v2Subs.length);
  for(var subIdx = 0;order == 0 && subIdx < subCount;subIdx++) {
    var v1Sub = v1Subs[subIdx] || "";
    var v2Sub = v2Subs[subIdx] || "";
    var v1CompParser = new RegExp("(\\d*)(\\D*)", "g");
    var v2CompParser = new RegExp("(\\d*)(\\D*)", "g");
    do {
      var v1Comp = v1CompParser.exec(v1Sub) || ["", "", ""];
      var v2Comp = v2CompParser.exec(v2Sub) || ["", "", ""];
      if(v1Comp[0].length == 0 && v2Comp[0].length == 0) {
        break
      }
      var v1CompNum = v1Comp[1].length == 0 ? 0 : parseInt(v1Comp[1], 10);
      var v2CompNum = v2Comp[1].length == 0 ? 0 : parseInt(v2Comp[1], 10);
      order = goog.string.compareElements_(v1CompNum, v2CompNum) || goog.string.compareElements_(v1Comp[2].length == 0, v2Comp[2].length == 0) || goog.string.compareElements_(v1Comp[2], v2Comp[2])
    }while(order == 0)
  }
  return order
};
goog.string.compareElements_ = function(left, right) {
  if(left < right) {
    return-1
  }else {
    if(left > right) {
      return 1
    }
  }
  return 0
};
goog.string.HASHCODE_MAX_ = 4294967296;
goog.string.hashCode = function(str) {
  var result = 0;
  for(var i = 0;i < str.length;++i) {
    result = 31 * result + str.charCodeAt(i);
    result %= goog.string.HASHCODE_MAX_
  }
  return result
};
goog.string.uniqueStringCounter_ = Math.random() * 2147483648 | 0;
goog.string.createUniqueString = function() {
  return"goog_" + goog.string.uniqueStringCounter_++
};
goog.string.toNumber = function(str) {
  var num = Number(str);
  if(num == 0 && goog.string.isEmpty(str)) {
    return NaN
  }
  return num
};
goog.string.toCamelCaseCache_ = {};
goog.string.toCamelCase = function(str) {
  return goog.string.toCamelCaseCache_[str] || (goog.string.toCamelCaseCache_[str] = String(str).replace(/\-([a-z])/g, function(all, match) {
    return match.toUpperCase()
  }))
};
goog.string.toSelectorCaseCache_ = {};
goog.string.toSelectorCase = function(str) {
  return goog.string.toSelectorCaseCache_[str] || (goog.string.toSelectorCaseCache_[str] = String(str).replace(/([A-Z])/g, "-$1").toLowerCase())
};
goog.provide("goog.userAgent.jscript");
goog.require("goog.string");
goog.userAgent.jscript.ASSUME_NO_JSCRIPT = false;
goog.userAgent.jscript.init_ = function() {
  var hasScriptEngine = "ScriptEngine" in goog.global;
  goog.userAgent.jscript.DETECTED_HAS_JSCRIPT_ = hasScriptEngine && goog.global["ScriptEngine"]() == "JScript";
  goog.userAgent.jscript.DETECTED_VERSION_ = goog.userAgent.jscript.DETECTED_HAS_JSCRIPT_ ? goog.global["ScriptEngineMajorVersion"]() + "." + goog.global["ScriptEngineMinorVersion"]() + "." + goog.global["ScriptEngineBuildVersion"]() : "0"
};
if(!goog.userAgent.jscript.ASSUME_NO_JSCRIPT) {
  goog.userAgent.jscript.init_()
}
goog.userAgent.jscript.HAS_JSCRIPT = goog.userAgent.jscript.ASSUME_NO_JSCRIPT ? false : goog.userAgent.jscript.DETECTED_HAS_JSCRIPT_;
goog.userAgent.jscript.VERSION = goog.userAgent.jscript.ASSUME_NO_JSCRIPT ? "0" : goog.userAgent.jscript.DETECTED_VERSION_;
goog.userAgent.jscript.isVersion = function(version) {
  return goog.string.compareVersions(goog.userAgent.jscript.VERSION, version) >= 0
};
goog.provide("goog.string.StringBuffer");
goog.require("goog.userAgent.jscript");
goog.string.StringBuffer = function(opt_a1, var_args) {
  this.buffer_ = goog.userAgent.jscript.HAS_JSCRIPT ? [] : "";
  if(opt_a1 != null) {
    this.append.apply(this, arguments)
  }
};
goog.string.StringBuffer.prototype.set = function(s) {
  this.clear();
  this.append(s)
};
if(goog.userAgent.jscript.HAS_JSCRIPT) {
  goog.string.StringBuffer.prototype.bufferLength_ = 0;
  goog.string.StringBuffer.prototype.append = function(a1, opt_a2, var_args) {
    if(opt_a2 == null) {
      this.buffer_[this.bufferLength_++] = a1
    }else {
      this.buffer_.push.apply(this.buffer_, arguments);
      this.bufferLength_ = this.buffer_.length
    }
    return this
  }
}else {
  goog.string.StringBuffer.prototype.append = function(a1, opt_a2, var_args) {
    this.buffer_ += a1;
    if(opt_a2 != null) {
      for(var i = 1;i < arguments.length;i++) {
        this.buffer_ += arguments[i]
      }
    }
    return this
  }
}
goog.string.StringBuffer.prototype.clear = function() {
  if(goog.userAgent.jscript.HAS_JSCRIPT) {
    this.buffer_.length = 0;
    this.bufferLength_ = 0
  }else {
    this.buffer_ = ""
  }
};
goog.string.StringBuffer.prototype.getLength = function() {
  return this.toString().length
};
goog.string.StringBuffer.prototype.toString = function() {
  if(goog.userAgent.jscript.HAS_JSCRIPT) {
    var str = this.buffer_.join("");
    this.clear();
    if(str) {
      this.append(str)
    }
    return str
  }else {
    return this.buffer_
  }
};
goog.provide("goog.debug.Error");
goog.debug.Error = function(opt_msg) {
  this.stack = (new Error).stack || "";
  if(opt_msg) {
    this.message = String(opt_msg)
  }
};
goog.inherits(goog.debug.Error, Error);
goog.debug.Error.prototype.name = "CustomError";
goog.provide("goog.asserts");
goog.provide("goog.asserts.AssertionError");
goog.require("goog.debug.Error");
goog.require("goog.string");
goog.asserts.ENABLE_ASSERTS = goog.DEBUG;
goog.asserts.AssertionError = function(messagePattern, messageArgs) {
  messageArgs.unshift(messagePattern);
  goog.debug.Error.call(this, goog.string.subs.apply(null, messageArgs));
  messageArgs.shift();
  this.messagePattern = messagePattern
};
goog.inherits(goog.asserts.AssertionError, goog.debug.Error);
goog.asserts.AssertionError.prototype.name = "AssertionError";
goog.asserts.doAssertFailure_ = function(defaultMessage, defaultArgs, givenMessage, givenArgs) {
  var message = "Assertion failed";
  if(givenMessage) {
    message += ": " + givenMessage;
    var args = givenArgs
  }else {
    if(defaultMessage) {
      message += ": " + defaultMessage;
      args = defaultArgs
    }
  }
  throw new goog.asserts.AssertionError("" + message, args || []);
};
goog.asserts.assert = function(condition, opt_message, var_args) {
  if(goog.asserts.ENABLE_ASSERTS && !condition) {
    goog.asserts.doAssertFailure_("", null, opt_message, Array.prototype.slice.call(arguments, 2))
  }
  return condition
};
goog.asserts.fail = function(opt_message, var_args) {
  if(goog.asserts.ENABLE_ASSERTS) {
    throw new goog.asserts.AssertionError("Failure" + (opt_message ? ": " + opt_message : ""), Array.prototype.slice.call(arguments, 1));
  }
};
goog.asserts.assertNumber = function(value, opt_message, var_args) {
  if(goog.asserts.ENABLE_ASSERTS && !goog.isNumber(value)) {
    goog.asserts.doAssertFailure_("Expected number but got %s: %s.", [goog.typeOf(value), value], opt_message, Array.prototype.slice.call(arguments, 2))
  }
  return value
};
goog.asserts.assertString = function(value, opt_message, var_args) {
  if(goog.asserts.ENABLE_ASSERTS && !goog.isString(value)) {
    goog.asserts.doAssertFailure_("Expected string but got %s: %s.", [goog.typeOf(value), value], opt_message, Array.prototype.slice.call(arguments, 2))
  }
  return value
};
goog.asserts.assertFunction = function(value, opt_message, var_args) {
  if(goog.asserts.ENABLE_ASSERTS && !goog.isFunction(value)) {
    goog.asserts.doAssertFailure_("Expected function but got %s: %s.", [goog.typeOf(value), value], opt_message, Array.prototype.slice.call(arguments, 2))
  }
  return value
};
goog.asserts.assertObject = function(value, opt_message, var_args) {
  if(goog.asserts.ENABLE_ASSERTS && !goog.isObject(value)) {
    goog.asserts.doAssertFailure_("Expected object but got %s: %s.", [goog.typeOf(value), value], opt_message, Array.prototype.slice.call(arguments, 2))
  }
  return value
};
goog.asserts.assertArray = function(value, opt_message, var_args) {
  if(goog.asserts.ENABLE_ASSERTS && !goog.isArray(value)) {
    goog.asserts.doAssertFailure_("Expected array but got %s: %s.", [goog.typeOf(value), value], opt_message, Array.prototype.slice.call(arguments, 2))
  }
  return value
};
goog.asserts.assertBoolean = function(value, opt_message, var_args) {
  if(goog.asserts.ENABLE_ASSERTS && !goog.isBoolean(value)) {
    goog.asserts.doAssertFailure_("Expected boolean but got %s: %s.", [goog.typeOf(value), value], opt_message, Array.prototype.slice.call(arguments, 2))
  }
  return value
};
goog.asserts.assertInstanceof = function(value, type, opt_message, var_args) {
  if(goog.asserts.ENABLE_ASSERTS && !(value instanceof type)) {
    goog.asserts.doAssertFailure_("instanceof check failed.", null, opt_message, Array.prototype.slice.call(arguments, 3))
  }
};
goog.provide("goog.array");
goog.provide("goog.array.ArrayLike");
goog.require("goog.asserts");
goog.NATIVE_ARRAY_PROTOTYPES = true;
goog.array.ArrayLike;
goog.array.peek = function(array) {
  return array[array.length - 1]
};
goog.array.ARRAY_PROTOTYPE_ = Array.prototype;
goog.array.indexOf = goog.NATIVE_ARRAY_PROTOTYPES && goog.array.ARRAY_PROTOTYPE_.indexOf ? function(arr, obj, opt_fromIndex) {
  goog.asserts.assert(arr.length != null);
  return goog.array.ARRAY_PROTOTYPE_.indexOf.call(arr, obj, opt_fromIndex)
} : function(arr, obj, opt_fromIndex) {
  var fromIndex = opt_fromIndex == null ? 0 : opt_fromIndex < 0 ? Math.max(0, arr.length + opt_fromIndex) : opt_fromIndex;
  if(goog.isString(arr)) {
    if(!goog.isString(obj) || obj.length != 1) {
      return-1
    }
    return arr.indexOf(obj, fromIndex)
  }
  for(var i = fromIndex;i < arr.length;i++) {
    if(i in arr && arr[i] === obj) {
      return i
    }
  }
  return-1
};
goog.array.lastIndexOf = goog.NATIVE_ARRAY_PROTOTYPES && goog.array.ARRAY_PROTOTYPE_.lastIndexOf ? function(arr, obj, opt_fromIndex) {
  goog.asserts.assert(arr.length != null);
  var fromIndex = opt_fromIndex == null ? arr.length - 1 : opt_fromIndex;
  return goog.array.ARRAY_PROTOTYPE_.lastIndexOf.call(arr, obj, fromIndex)
} : function(arr, obj, opt_fromIndex) {
  var fromIndex = opt_fromIndex == null ? arr.length - 1 : opt_fromIndex;
  if(fromIndex < 0) {
    fromIndex = Math.max(0, arr.length + fromIndex)
  }
  if(goog.isString(arr)) {
    if(!goog.isString(obj) || obj.length != 1) {
      return-1
    }
    return arr.lastIndexOf(obj, fromIndex)
  }
  for(var i = fromIndex;i >= 0;i--) {
    if(i in arr && arr[i] === obj) {
      return i
    }
  }
  return-1
};
goog.array.forEach = goog.NATIVE_ARRAY_PROTOTYPES && goog.array.ARRAY_PROTOTYPE_.forEach ? function(arr, f, opt_obj) {
  goog.asserts.assert(arr.length != null);
  goog.array.ARRAY_PROTOTYPE_.forEach.call(arr, f, opt_obj)
} : function(arr, f, opt_obj) {
  var l = arr.length;
  var arr2 = goog.isString(arr) ? arr.split("") : arr;
  for(var i = 0;i < l;i++) {
    if(i in arr2) {
      f.call(opt_obj, arr2[i], i, arr)
    }
  }
};
goog.array.forEachRight = function(arr, f, opt_obj) {
  var l = arr.length;
  var arr2 = goog.isString(arr) ? arr.split("") : arr;
  for(var i = l - 1;i >= 0;--i) {
    if(i in arr2) {
      f.call(opt_obj, arr2[i], i, arr)
    }
  }
};
goog.array.filter = goog.NATIVE_ARRAY_PROTOTYPES && goog.array.ARRAY_PROTOTYPE_.filter ? function(arr, f, opt_obj) {
  goog.asserts.assert(arr.length != null);
  return goog.array.ARRAY_PROTOTYPE_.filter.call(arr, f, opt_obj)
} : function(arr, f, opt_obj) {
  var l = arr.length;
  var res = [];
  var resLength = 0;
  var arr2 = goog.isString(arr) ? arr.split("") : arr;
  for(var i = 0;i < l;i++) {
    if(i in arr2) {
      var val = arr2[i];
      if(f.call(opt_obj, val, i, arr)) {
        res[resLength++] = val
      }
    }
  }
  return res
};
goog.array.map = goog.NATIVE_ARRAY_PROTOTYPES && goog.array.ARRAY_PROTOTYPE_.map ? function(arr, f, opt_obj) {
  goog.asserts.assert(arr.length != null);
  return goog.array.ARRAY_PROTOTYPE_.map.call(arr, f, opt_obj)
} : function(arr, f, opt_obj) {
  var l = arr.length;
  var res = new Array(l);
  var arr2 = goog.isString(arr) ? arr.split("") : arr;
  for(var i = 0;i < l;i++) {
    if(i in arr2) {
      res[i] = f.call(opt_obj, arr2[i], i, arr)
    }
  }
  return res
};
goog.array.reduce = function(arr, f, val, opt_obj) {
  if(arr.reduce) {
    if(opt_obj) {
      return arr.reduce(goog.bind(f, opt_obj), val)
    }else {
      return arr.reduce(f, val)
    }
  }
  var rval = val;
  goog.array.forEach(arr, function(val, index) {
    rval = f.call(opt_obj, rval, val, index, arr)
  });
  return rval
};
goog.array.reduceRight = function(arr, f, val, opt_obj) {
  if(arr.reduceRight) {
    if(opt_obj) {
      return arr.reduceRight(goog.bind(f, opt_obj), val)
    }else {
      return arr.reduceRight(f, val)
    }
  }
  var rval = val;
  goog.array.forEachRight(arr, function(val, index) {
    rval = f.call(opt_obj, rval, val, index, arr)
  });
  return rval
};
goog.array.some = goog.NATIVE_ARRAY_PROTOTYPES && goog.array.ARRAY_PROTOTYPE_.some ? function(arr, f, opt_obj) {
  goog.asserts.assert(arr.length != null);
  return goog.array.ARRAY_PROTOTYPE_.some.call(arr, f, opt_obj)
} : function(arr, f, opt_obj) {
  var l = arr.length;
  var arr2 = goog.isString(arr) ? arr.split("") : arr;
  for(var i = 0;i < l;i++) {
    if(i in arr2 && f.call(opt_obj, arr2[i], i, arr)) {
      return true
    }
  }
  return false
};
goog.array.every = goog.NATIVE_ARRAY_PROTOTYPES && goog.array.ARRAY_PROTOTYPE_.every ? function(arr, f, opt_obj) {
  goog.asserts.assert(arr.length != null);
  return goog.array.ARRAY_PROTOTYPE_.every.call(arr, f, opt_obj)
} : function(arr, f, opt_obj) {
  var l = arr.length;
  var arr2 = goog.isString(arr) ? arr.split("") : arr;
  for(var i = 0;i < l;i++) {
    if(i in arr2 && !f.call(opt_obj, arr2[i], i, arr)) {
      return false
    }
  }
  return true
};
goog.array.find = function(arr, f, opt_obj) {
  var i = goog.array.findIndex(arr, f, opt_obj);
  return i < 0 ? null : goog.isString(arr) ? arr.charAt(i) : arr[i]
};
goog.array.findIndex = function(arr, f, opt_obj) {
  var l = arr.length;
  var arr2 = goog.isString(arr) ? arr.split("") : arr;
  for(var i = 0;i < l;i++) {
    if(i in arr2 && f.call(opt_obj, arr2[i], i, arr)) {
      return i
    }
  }
  return-1
};
goog.array.findRight = function(arr, f, opt_obj) {
  var i = goog.array.findIndexRight(arr, f, opt_obj);
  return i < 0 ? null : goog.isString(arr) ? arr.charAt(i) : arr[i]
};
goog.array.findIndexRight = function(arr, f, opt_obj) {
  var l = arr.length;
  var arr2 = goog.isString(arr) ? arr.split("") : arr;
  for(var i = l - 1;i >= 0;i--) {
    if(i in arr2 && f.call(opt_obj, arr2[i], i, arr)) {
      return i
    }
  }
  return-1
};
goog.array.contains = function(arr, obj) {
  return goog.array.indexOf(arr, obj) >= 0
};
goog.array.isEmpty = function(arr) {
  return arr.length == 0
};
goog.array.clear = function(arr) {
  if(!goog.isArray(arr)) {
    for(var i = arr.length - 1;i >= 0;i--) {
      delete arr[i]
    }
  }
  arr.length = 0
};
goog.array.insert = function(arr, obj) {
  if(!goog.array.contains(arr, obj)) {
    arr.push(obj)
  }
};
goog.array.insertAt = function(arr, obj, opt_i) {
  goog.array.splice(arr, opt_i, 0, obj)
};
goog.array.insertArrayAt = function(arr, elementsToAdd, opt_i) {
  goog.partial(goog.array.splice, arr, opt_i, 0).apply(null, elementsToAdd)
};
goog.array.insertBefore = function(arr, obj, opt_obj2) {
  var i;
  if(arguments.length == 2 || (i = goog.array.indexOf(arr, opt_obj2)) < 0) {
    arr.push(obj)
  }else {
    goog.array.insertAt(arr, obj, i)
  }
};
goog.array.remove = function(arr, obj) {
  var i = goog.array.indexOf(arr, obj);
  var rv;
  if(rv = i >= 0) {
    goog.array.removeAt(arr, i)
  }
  return rv
};
goog.array.removeAt = function(arr, i) {
  goog.asserts.assert(arr.length != null);
  return goog.array.ARRAY_PROTOTYPE_.splice.call(arr, i, 1).length == 1
};
goog.array.removeIf = function(arr, f, opt_obj) {
  var i = goog.array.findIndex(arr, f, opt_obj);
  if(i >= 0) {
    goog.array.removeAt(arr, i);
    return true
  }
  return false
};
goog.array.concat = function(var_args) {
  return goog.array.ARRAY_PROTOTYPE_.concat.apply(goog.array.ARRAY_PROTOTYPE_, arguments)
};
goog.array.clone = function(arr) {
  if(goog.isArray(arr)) {
    return goog.array.concat(arr)
  }else {
    var rv = [];
    for(var i = 0, len = arr.length;i < len;i++) {
      rv[i] = arr[i]
    }
    return rv
  }
};
goog.array.toArray = function(object) {
  if(goog.isArray(object)) {
    return goog.array.concat(object)
  }
  return goog.array.clone(object)
};
goog.array.extend = function(arr1, var_args) {
  for(var i = 1;i < arguments.length;i++) {
    var arr2 = arguments[i];
    var isArrayLike;
    if(goog.isArray(arr2) || (isArrayLike = goog.isArrayLike(arr2)) && arr2.hasOwnProperty("callee")) {
      arr1.push.apply(arr1, arr2)
    }else {
      if(isArrayLike) {
        var len1 = arr1.length;
        var len2 = arr2.length;
        for(var j = 0;j < len2;j++) {
          arr1[len1 + j] = arr2[j]
        }
      }else {
        arr1.push(arr2)
      }
    }
  }
};
goog.array.splice = function(arr, index, howMany, var_args) {
  goog.asserts.assert(arr.length != null);
  return goog.array.ARRAY_PROTOTYPE_.splice.apply(arr, goog.array.slice(arguments, 1))
};
goog.array.slice = function(arr, start, opt_end) {
  goog.asserts.assert(arr.length != null);
  if(arguments.length <= 2) {
    return goog.array.ARRAY_PROTOTYPE_.slice.call(arr, start)
  }else {
    return goog.array.ARRAY_PROTOTYPE_.slice.call(arr, start, opt_end)
  }
};
goog.array.removeDuplicates = function(arr, opt_rv) {
  var returnArray = opt_rv || arr;
  var seen = {}, cursorInsert = 0, cursorRead = 0;
  while(cursorRead < arr.length) {
    var current = arr[cursorRead++];
    var key = goog.isObject(current) ? "o" + goog.getUid(current) : (typeof current).charAt(0) + current;
    if(!Object.prototype.hasOwnProperty.call(seen, key)) {
      seen[key] = true;
      returnArray[cursorInsert++] = current
    }
  }
  returnArray.length = cursorInsert
};
goog.array.binarySearch = function(arr, target, opt_compareFn) {
  return goog.array.binarySearch_(arr, opt_compareFn || goog.array.defaultCompare, false, target)
};
goog.array.binarySelect = function(arr, evaluator, opt_obj) {
  return goog.array.binarySearch_(arr, evaluator, true, undefined, opt_obj)
};
goog.array.binarySearch_ = function(arr, compareFn, isEvaluator, opt_target, opt_selfObj) {
  var left = 0;
  var right = arr.length;
  var found;
  while(left < right) {
    var middle = left + right >> 1;
    var compareResult;
    if(isEvaluator) {
      compareResult = compareFn.call(opt_selfObj, arr[middle], middle, arr)
    }else {
      compareResult = compareFn(opt_target, arr[middle])
    }
    if(compareResult > 0) {
      left = middle + 1
    }else {
      right = middle;
      found = !compareResult
    }
  }
  return found ? left : ~left
};
goog.array.sort = function(arr, opt_compareFn) {
  goog.asserts.assert(arr.length != null);
  goog.array.ARRAY_PROTOTYPE_.sort.call(arr, opt_compareFn || goog.array.defaultCompare)
};
goog.array.stableSort = function(arr, opt_compareFn) {
  for(var i = 0;i < arr.length;i++) {
    arr[i] = {index:i, value:arr[i]}
  }
  var valueCompareFn = opt_compareFn || goog.array.defaultCompare;
  function stableCompareFn(obj1, obj2) {
    return valueCompareFn(obj1.value, obj2.value) || obj1.index - obj2.index
  }
  goog.array.sort(arr, stableCompareFn);
  for(var i = 0;i < arr.length;i++) {
    arr[i] = arr[i].value
  }
};
goog.array.sortObjectsByKey = function(arr, key, opt_compareFn) {
  var compare = opt_compareFn || goog.array.defaultCompare;
  goog.array.sort(arr, function(a, b) {
    return compare(a[key], b[key])
  })
};
goog.array.isSorted = function(arr, opt_compareFn, opt_strict) {
  var compare = opt_compareFn || goog.array.defaultCompare;
  for(var i = 1;i < arr.length;i++) {
    var compareResult = compare(arr[i - 1], arr[i]);
    if(compareResult > 0 || compareResult == 0 && opt_strict) {
      return false
    }
  }
  return true
};
goog.array.equals = function(arr1, arr2, opt_equalsFn) {
  if(!goog.isArrayLike(arr1) || !goog.isArrayLike(arr2) || arr1.length != arr2.length) {
    return false
  }
  var l = arr1.length;
  var equalsFn = opt_equalsFn || goog.array.defaultCompareEquality;
  for(var i = 0;i < l;i++) {
    if(!equalsFn(arr1[i], arr2[i])) {
      return false
    }
  }
  return true
};
goog.array.compare = function(arr1, arr2, opt_equalsFn) {
  return goog.array.equals(arr1, arr2, opt_equalsFn)
};
goog.array.defaultCompare = function(a, b) {
  return a > b ? 1 : a < b ? -1 : 0
};
goog.array.defaultCompareEquality = function(a, b) {
  return a === b
};
goog.array.binaryInsert = function(array, value, opt_compareFn) {
  var index = goog.array.binarySearch(array, value, opt_compareFn);
  if(index < 0) {
    goog.array.insertAt(array, value, -(index + 1));
    return true
  }
  return false
};
goog.array.binaryRemove = function(array, value, opt_compareFn) {
  var index = goog.array.binarySearch(array, value, opt_compareFn);
  return index >= 0 ? goog.array.removeAt(array, index) : false
};
goog.array.bucket = function(array, sorter) {
  var buckets = {};
  for(var i = 0;i < array.length;i++) {
    var value = array[i];
    var key = sorter(value, i, array);
    if(goog.isDef(key)) {
      var bucket = buckets[key] || (buckets[key] = []);
      bucket.push(value)
    }
  }
  return buckets
};
goog.array.repeat = function(value, n) {
  var array = [];
  for(var i = 0;i < n;i++) {
    array[i] = value
  }
  return array
};
goog.array.flatten = function(var_args) {
  var result = [];
  for(var i = 0;i < arguments.length;i++) {
    var element = arguments[i];
    if(goog.isArray(element)) {
      result.push.apply(result, goog.array.flatten.apply(null, element))
    }else {
      result.push(element)
    }
  }
  return result
};
goog.array.rotate = function(array, n) {
  goog.asserts.assert(array.length != null);
  if(array.length) {
    n %= array.length;
    if(n > 0) {
      goog.array.ARRAY_PROTOTYPE_.unshift.apply(array, array.splice(-n, n))
    }else {
      if(n < 0) {
        goog.array.ARRAY_PROTOTYPE_.push.apply(array, array.splice(0, -n))
      }
    }
  }
  return array
};
goog.array.zip = function(var_args) {
  if(!arguments.length) {
    return[]
  }
  var result = [];
  for(var i = 0;true;i++) {
    var value = [];
    for(var j = 0;j < arguments.length;j++) {
      var arr = arguments[j];
      if(i >= arr.length) {
        return result
      }
      value.push(arr[i])
    }
    result.push(value)
  }
};
goog.array.shuffle = function(arr, opt_randFn) {
  var randFn = opt_randFn || Math.random;
  for(var i = arr.length - 1;i > 0;i--) {
    var j = Math.floor(randFn() * (i + 1));
    var tmp = arr[i];
    arr[i] = arr[j];
    arr[j] = tmp
  }
};
goog.provide("goog.object");
goog.object.forEach = function(obj, f, opt_obj) {
  for(var key in obj) {
    f.call(opt_obj, obj[key], key, obj)
  }
};
goog.object.filter = function(obj, f, opt_obj) {
  var res = {};
  for(var key in obj) {
    if(f.call(opt_obj, obj[key], key, obj)) {
      res[key] = obj[key]
    }
  }
  return res
};
goog.object.map = function(obj, f, opt_obj) {
  var res = {};
  for(var key in obj) {
    res[key] = f.call(opt_obj, obj[key], key, obj)
  }
  return res
};
goog.object.some = function(obj, f, opt_obj) {
  for(var key in obj) {
    if(f.call(opt_obj, obj[key], key, obj)) {
      return true
    }
  }
  return false
};
goog.object.every = function(obj, f, opt_obj) {
  for(var key in obj) {
    if(!f.call(opt_obj, obj[key], key, obj)) {
      return false
    }
  }
  return true
};
goog.object.getCount = function(obj) {
  var rv = 0;
  for(var key in obj) {
    rv++
  }
  return rv
};
goog.object.getAnyKey = function(obj) {
  for(var key in obj) {
    return key
  }
};
goog.object.getAnyValue = function(obj) {
  for(var key in obj) {
    return obj[key]
  }
};
goog.object.contains = function(obj, val) {
  return goog.object.containsValue(obj, val)
};
goog.object.getValues = function(obj) {
  var res = [];
  var i = 0;
  for(var key in obj) {
    res[i++] = obj[key]
  }
  return res
};
goog.object.getKeys = function(obj) {
  var res = [];
  var i = 0;
  for(var key in obj) {
    res[i++] = key
  }
  return res
};
goog.object.getValueByKeys = function(obj, var_args) {
  var isArrayLike = goog.isArrayLike(var_args);
  var keys = isArrayLike ? var_args : arguments;
  for(var i = isArrayLike ? 0 : 1;i < keys.length;i++) {
    obj = obj[keys[i]];
    if(!goog.isDef(obj)) {
      break
    }
  }
  return obj
};
goog.object.containsKey = function(obj, key) {
  return key in obj
};
goog.object.containsValue = function(obj, val) {
  for(var key in obj) {
    if(obj[key] == val) {
      return true
    }
  }
  return false
};
goog.object.findKey = function(obj, f, opt_this) {
  for(var key in obj) {
    if(f.call(opt_this, obj[key], key, obj)) {
      return key
    }
  }
  return undefined
};
goog.object.findValue = function(obj, f, opt_this) {
  var key = goog.object.findKey(obj, f, opt_this);
  return key && obj[key]
};
goog.object.isEmpty = function(obj) {
  for(var key in obj) {
    return false
  }
  return true
};
goog.object.clear = function(obj) {
  for(var i in obj) {
    delete obj[i]
  }
};
goog.object.remove = function(obj, key) {
  var rv;
  if(rv = key in obj) {
    delete obj[key]
  }
  return rv
};
goog.object.add = function(obj, key, val) {
  if(key in obj) {
    throw Error('The object already contains the key "' + key + '"');
  }
  goog.object.set(obj, key, val)
};
goog.object.get = function(obj, key, opt_val) {
  if(key in obj) {
    return obj[key]
  }
  return opt_val
};
goog.object.set = function(obj, key, value) {
  obj[key] = value
};
goog.object.setIfUndefined = function(obj, key, value) {
  return key in obj ? obj[key] : obj[key] = value
};
goog.object.clone = function(obj) {
  var res = {};
  for(var key in obj) {
    res[key] = obj[key]
  }
  return res
};
goog.object.unsafeClone = function(obj) {
  var type = goog.typeOf(obj);
  if(type == "object" || type == "array") {
    if(obj.clone) {
      return obj.clone()
    }
    var clone = type == "array" ? [] : {};
    for(var key in obj) {
      clone[key] = goog.object.unsafeClone(obj[key])
    }
    return clone
  }
  return obj
};
goog.object.transpose = function(obj) {
  var transposed = {};
  for(var key in obj) {
    transposed[obj[key]] = key
  }
  return transposed
};
goog.object.PROTOTYPE_FIELDS_ = ["constructor", "hasOwnProperty", "isPrototypeOf", "propertyIsEnumerable", "toLocaleString", "toString", "valueOf"];
goog.object.extend = function(target, var_args) {
  var key, source;
  for(var i = 1;i < arguments.length;i++) {
    source = arguments[i];
    for(key in source) {
      target[key] = source[key]
    }
    for(var j = 0;j < goog.object.PROTOTYPE_FIELDS_.length;j++) {
      key = goog.object.PROTOTYPE_FIELDS_[j];
      if(Object.prototype.hasOwnProperty.call(source, key)) {
        target[key] = source[key]
      }
    }
  }
};
goog.object.create = function(var_args) {
  var argLength = arguments.length;
  if(argLength == 1 && goog.isArray(arguments[0])) {
    return goog.object.create.apply(null, arguments[0])
  }
  if(argLength % 2) {
    throw Error("Uneven number of arguments");
  }
  var rv = {};
  for(var i = 0;i < argLength;i += 2) {
    rv[arguments[i]] = arguments[i + 1]
  }
  return rv
};
goog.object.createSet = function(var_args) {
  var argLength = arguments.length;
  if(argLength == 1 && goog.isArray(arguments[0])) {
    return goog.object.createSet.apply(null, arguments[0])
  }
  var rv = {};
  for(var i = 0;i < argLength;i++) {
    rv[arguments[i]] = true
  }
  return rv
};
goog.provide("cljs.core");
goog.require("goog.string");
goog.require("goog.string.StringBuffer");
goog.require("goog.object");
goog.require("goog.array");
cljs.core._STAR_print_fn_STAR_ = function _STAR_print_fn_STAR_(_) {
  throw new Error("No *print-fn* fn set for evaluation environment");
};
cljs.core.truth_ = function truth_(x) {
  return x != null && x !== false
};
cljs.core.type_satisfies_ = function type_satisfies_(p, x) {
  var or__3548__auto____2735 = p[goog.typeOf.call(null, x)];
  if(cljs.core.truth_(or__3548__auto____2735)) {
    return or__3548__auto____2735
  }else {
    var or__3548__auto____2736 = p["_"];
    if(cljs.core.truth_(or__3548__auto____2736)) {
      return or__3548__auto____2736
    }else {
      return false
    }
  }
};
cljs.core.is_proto_ = function is_proto_(x) {
  return x.constructor.prototype === x
};
cljs.core._STAR_main_cli_fn_STAR_ = null;
cljs.core.missing_protocol = function missing_protocol(proto, obj) {
  return Error.call(null, "No protocol method " + proto + " defined for type " + goog.typeOf.call(null, obj) + ": " + obj)
};
cljs.core.aclone = function aclone(array_like) {
  return Array.prototype.slice.call(array_like)
};
cljs.core.array = function array(var_args) {
  return Array.prototype.slice.call(arguments)
};
cljs.core.aget = function aget(array, i) {
  return array[i]
};
cljs.core.aset = function aset(array, i, val) {
  return array[i] = val
};
cljs.core.alength = function alength(array) {
  return array.length
};
cljs.core.IFn = {};
cljs.core._invoke = function() {
  var _invoke = null;
  var _invoke__2800 = function(this$) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2737 = this$;
      if(cljs.core.truth_(and__3546__auto____2737)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2737
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$)
    }else {
      return function() {
        var or__3548__auto____2738 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2738)) {
          return or__3548__auto____2738
        }else {
          var or__3548__auto____2739 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2739)) {
            return or__3548__auto____2739
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$)
    }
  };
  var _invoke__2801 = function(this$, a) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2740 = this$;
      if(cljs.core.truth_(and__3546__auto____2740)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2740
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a)
    }else {
      return function() {
        var or__3548__auto____2741 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2741)) {
          return or__3548__auto____2741
        }else {
          var or__3548__auto____2742 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2742)) {
            return or__3548__auto____2742
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a)
    }
  };
  var _invoke__2802 = function(this$, a, b) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2743 = this$;
      if(cljs.core.truth_(and__3546__auto____2743)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2743
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b)
    }else {
      return function() {
        var or__3548__auto____2744 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2744)) {
          return or__3548__auto____2744
        }else {
          var or__3548__auto____2745 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2745)) {
            return or__3548__auto____2745
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b)
    }
  };
  var _invoke__2803 = function(this$, a, b, c) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2746 = this$;
      if(cljs.core.truth_(and__3546__auto____2746)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2746
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c)
    }else {
      return function() {
        var or__3548__auto____2747 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2747)) {
          return or__3548__auto____2747
        }else {
          var or__3548__auto____2748 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2748)) {
            return or__3548__auto____2748
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c)
    }
  };
  var _invoke__2804 = function(this$, a, b, c, d) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2749 = this$;
      if(cljs.core.truth_(and__3546__auto____2749)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2749
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d)
    }else {
      return function() {
        var or__3548__auto____2750 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2750)) {
          return or__3548__auto____2750
        }else {
          var or__3548__auto____2751 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2751)) {
            return or__3548__auto____2751
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c, d)
    }
  };
  var _invoke__2805 = function(this$, a, b, c, d, e) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2752 = this$;
      if(cljs.core.truth_(and__3546__auto____2752)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2752
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d, e)
    }else {
      return function() {
        var or__3548__auto____2753 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2753)) {
          return or__3548__auto____2753
        }else {
          var or__3548__auto____2754 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2754)) {
            return or__3548__auto____2754
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c, d, e)
    }
  };
  var _invoke__2806 = function(this$, a, b, c, d, e, f) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2755 = this$;
      if(cljs.core.truth_(and__3546__auto____2755)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2755
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d, e, f)
    }else {
      return function() {
        var or__3548__auto____2756 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2756)) {
          return or__3548__auto____2756
        }else {
          var or__3548__auto____2757 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2757)) {
            return or__3548__auto____2757
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c, d, e, f)
    }
  };
  var _invoke__2807 = function(this$, a, b, c, d, e, f, g) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2758 = this$;
      if(cljs.core.truth_(and__3546__auto____2758)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2758
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d, e, f, g)
    }else {
      return function() {
        var or__3548__auto____2759 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2759)) {
          return or__3548__auto____2759
        }else {
          var or__3548__auto____2760 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2760)) {
            return or__3548__auto____2760
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c, d, e, f, g)
    }
  };
  var _invoke__2808 = function(this$, a, b, c, d, e, f, g, h) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2761 = this$;
      if(cljs.core.truth_(and__3546__auto____2761)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2761
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d, e, f, g, h)
    }else {
      return function() {
        var or__3548__auto____2762 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2762)) {
          return or__3548__auto____2762
        }else {
          var or__3548__auto____2763 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2763)) {
            return or__3548__auto____2763
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c, d, e, f, g, h)
    }
  };
  var _invoke__2809 = function(this$, a, b, c, d, e, f, g, h, i) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2764 = this$;
      if(cljs.core.truth_(and__3546__auto____2764)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2764
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d, e, f, g, h, i)
    }else {
      return function() {
        var or__3548__auto____2765 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2765)) {
          return or__3548__auto____2765
        }else {
          var or__3548__auto____2766 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2766)) {
            return or__3548__auto____2766
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c, d, e, f, g, h, i)
    }
  };
  var _invoke__2810 = function(this$, a, b, c, d, e, f, g, h, i, j) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2767 = this$;
      if(cljs.core.truth_(and__3546__auto____2767)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2767
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d, e, f, g, h, i, j)
    }else {
      return function() {
        var or__3548__auto____2768 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2768)) {
          return or__3548__auto____2768
        }else {
          var or__3548__auto____2769 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2769)) {
            return or__3548__auto____2769
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c, d, e, f, g, h, i, j)
    }
  };
  var _invoke__2811 = function(this$, a, b, c, d, e, f, g, h, i, j, k) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2770 = this$;
      if(cljs.core.truth_(and__3546__auto____2770)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2770
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d, e, f, g, h, i, j, k)
    }else {
      return function() {
        var or__3548__auto____2771 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2771)) {
          return or__3548__auto____2771
        }else {
          var or__3548__auto____2772 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2772)) {
            return or__3548__auto____2772
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c, d, e, f, g, h, i, j, k)
    }
  };
  var _invoke__2812 = function(this$, a, b, c, d, e, f, g, h, i, j, k, l) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2773 = this$;
      if(cljs.core.truth_(and__3546__auto____2773)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2773
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d, e, f, g, h, i, j, k, l)
    }else {
      return function() {
        var or__3548__auto____2774 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2774)) {
          return or__3548__auto____2774
        }else {
          var or__3548__auto____2775 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2775)) {
            return or__3548__auto____2775
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c, d, e, f, g, h, i, j, k, l)
    }
  };
  var _invoke__2813 = function(this$, a, b, c, d, e, f, g, h, i, j, k, l, m) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2776 = this$;
      if(cljs.core.truth_(and__3546__auto____2776)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2776
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d, e, f, g, h, i, j, k, l, m)
    }else {
      return function() {
        var or__3548__auto____2777 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2777)) {
          return or__3548__auto____2777
        }else {
          var or__3548__auto____2778 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2778)) {
            return or__3548__auto____2778
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c, d, e, f, g, h, i, j, k, l, m)
    }
  };
  var _invoke__2814 = function(this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2779 = this$;
      if(cljs.core.truth_(and__3546__auto____2779)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2779
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n)
    }else {
      return function() {
        var or__3548__auto____2780 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2780)) {
          return or__3548__auto____2780
        }else {
          var or__3548__auto____2781 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2781)) {
            return or__3548__auto____2781
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n)
    }
  };
  var _invoke__2815 = function(this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2782 = this$;
      if(cljs.core.truth_(and__3546__auto____2782)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2782
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o)
    }else {
      return function() {
        var or__3548__auto____2783 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2783)) {
          return or__3548__auto____2783
        }else {
          var or__3548__auto____2784 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2784)) {
            return or__3548__auto____2784
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o)
    }
  };
  var _invoke__2816 = function(this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2785 = this$;
      if(cljs.core.truth_(and__3546__auto____2785)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2785
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p)
    }else {
      return function() {
        var or__3548__auto____2786 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2786)) {
          return or__3548__auto____2786
        }else {
          var or__3548__auto____2787 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2787)) {
            return or__3548__auto____2787
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p)
    }
  };
  var _invoke__2817 = function(this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2788 = this$;
      if(cljs.core.truth_(and__3546__auto____2788)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2788
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q)
    }else {
      return function() {
        var or__3548__auto____2789 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2789)) {
          return or__3548__auto____2789
        }else {
          var or__3548__auto____2790 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2790)) {
            return or__3548__auto____2790
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q)
    }
  };
  var _invoke__2818 = function(this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, s) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2791 = this$;
      if(cljs.core.truth_(and__3546__auto____2791)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2791
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, s)
    }else {
      return function() {
        var or__3548__auto____2792 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2792)) {
          return or__3548__auto____2792
        }else {
          var or__3548__auto____2793 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2793)) {
            return or__3548__auto____2793
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, s)
    }
  };
  var _invoke__2819 = function(this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, s, t) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2794 = this$;
      if(cljs.core.truth_(and__3546__auto____2794)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2794
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, s, t)
    }else {
      return function() {
        var or__3548__auto____2795 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2795)) {
          return or__3548__auto____2795
        }else {
          var or__3548__auto____2796 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2796)) {
            return or__3548__auto____2796
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, s, t)
    }
  };
  var _invoke__2820 = function(this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, s, t, rest) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2797 = this$;
      if(cljs.core.truth_(and__3546__auto____2797)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2797
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, s, t, rest)
    }else {
      return function() {
        var or__3548__auto____2798 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2798)) {
          return or__3548__auto____2798
        }else {
          var or__3548__auto____2799 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2799)) {
            return or__3548__auto____2799
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, s, t, rest)
    }
  };
  _invoke = function(this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, s, t, rest) {
    switch(arguments.length) {
      case 1:
        return _invoke__2800.call(this, this$);
      case 2:
        return _invoke__2801.call(this, this$, a);
      case 3:
        return _invoke__2802.call(this, this$, a, b);
      case 4:
        return _invoke__2803.call(this, this$, a, b, c);
      case 5:
        return _invoke__2804.call(this, this$, a, b, c, d);
      case 6:
        return _invoke__2805.call(this, this$, a, b, c, d, e);
      case 7:
        return _invoke__2806.call(this, this$, a, b, c, d, e, f);
      case 8:
        return _invoke__2807.call(this, this$, a, b, c, d, e, f, g);
      case 9:
        return _invoke__2808.call(this, this$, a, b, c, d, e, f, g, h);
      case 10:
        return _invoke__2809.call(this, this$, a, b, c, d, e, f, g, h, i);
      case 11:
        return _invoke__2810.call(this, this$, a, b, c, d, e, f, g, h, i, j);
      case 12:
        return _invoke__2811.call(this, this$, a, b, c, d, e, f, g, h, i, j, k);
      case 13:
        return _invoke__2812.call(this, this$, a, b, c, d, e, f, g, h, i, j, k, l);
      case 14:
        return _invoke__2813.call(this, this$, a, b, c, d, e, f, g, h, i, j, k, l, m);
      case 15:
        return _invoke__2814.call(this, this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n);
      case 16:
        return _invoke__2815.call(this, this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o);
      case 17:
        return _invoke__2816.call(this, this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p);
      case 18:
        return _invoke__2817.call(this, this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q);
      case 19:
        return _invoke__2818.call(this, this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, s);
      case 20:
        return _invoke__2819.call(this, this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, s, t);
      case 21:
        return _invoke__2820.call(this, this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, s, t, rest)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return _invoke
}();
cljs.core.ICounted = {};
cljs.core._count = function _count(coll) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____2822 = coll;
    if(cljs.core.truth_(and__3546__auto____2822)) {
      return coll.cljs$core$ICounted$_count
    }else {
      return and__3546__auto____2822
    }
  }())) {
    return coll.cljs$core$ICounted$_count(coll)
  }else {
    return function() {
      var or__3548__auto____2823 = cljs.core._count[goog.typeOf.call(null, coll)];
      if(cljs.core.truth_(or__3548__auto____2823)) {
        return or__3548__auto____2823
      }else {
        var or__3548__auto____2824 = cljs.core._count["_"];
        if(cljs.core.truth_(or__3548__auto____2824)) {
          return or__3548__auto____2824
        }else {
          throw cljs.core.missing_protocol.call(null, "ICounted.-count", coll);
        }
      }
    }().call(null, coll)
  }
};
cljs.core.IEmptyableCollection = {};
cljs.core._empty = function _empty(coll) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____2825 = coll;
    if(cljs.core.truth_(and__3546__auto____2825)) {
      return coll.cljs$core$IEmptyableCollection$_empty
    }else {
      return and__3546__auto____2825
    }
  }())) {
    return coll.cljs$core$IEmptyableCollection$_empty(coll)
  }else {
    return function() {
      var or__3548__auto____2826 = cljs.core._empty[goog.typeOf.call(null, coll)];
      if(cljs.core.truth_(or__3548__auto____2826)) {
        return or__3548__auto____2826
      }else {
        var or__3548__auto____2827 = cljs.core._empty["_"];
        if(cljs.core.truth_(or__3548__auto____2827)) {
          return or__3548__auto____2827
        }else {
          throw cljs.core.missing_protocol.call(null, "IEmptyableCollection.-empty", coll);
        }
      }
    }().call(null, coll)
  }
};
cljs.core.ICollection = {};
cljs.core._conj = function _conj(coll, o) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____2828 = coll;
    if(cljs.core.truth_(and__3546__auto____2828)) {
      return coll.cljs$core$ICollection$_conj
    }else {
      return and__3546__auto____2828
    }
  }())) {
    return coll.cljs$core$ICollection$_conj(coll, o)
  }else {
    return function() {
      var or__3548__auto____2829 = cljs.core._conj[goog.typeOf.call(null, coll)];
      if(cljs.core.truth_(or__3548__auto____2829)) {
        return or__3548__auto____2829
      }else {
        var or__3548__auto____2830 = cljs.core._conj["_"];
        if(cljs.core.truth_(or__3548__auto____2830)) {
          return or__3548__auto____2830
        }else {
          throw cljs.core.missing_protocol.call(null, "ICollection.-conj", coll);
        }
      }
    }().call(null, coll, o)
  }
};
cljs.core.IIndexed = {};
cljs.core._nth = function() {
  var _nth = null;
  var _nth__2837 = function(coll, n) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2831 = coll;
      if(cljs.core.truth_(and__3546__auto____2831)) {
        return coll.cljs$core$IIndexed$_nth
      }else {
        return and__3546__auto____2831
      }
    }())) {
      return coll.cljs$core$IIndexed$_nth(coll, n)
    }else {
      return function() {
        var or__3548__auto____2832 = cljs.core._nth[goog.typeOf.call(null, coll)];
        if(cljs.core.truth_(or__3548__auto____2832)) {
          return or__3548__auto____2832
        }else {
          var or__3548__auto____2833 = cljs.core._nth["_"];
          if(cljs.core.truth_(or__3548__auto____2833)) {
            return or__3548__auto____2833
          }else {
            throw cljs.core.missing_protocol.call(null, "IIndexed.-nth", coll);
          }
        }
      }().call(null, coll, n)
    }
  };
  var _nth__2838 = function(coll, n, not_found) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2834 = coll;
      if(cljs.core.truth_(and__3546__auto____2834)) {
        return coll.cljs$core$IIndexed$_nth
      }else {
        return and__3546__auto____2834
      }
    }())) {
      return coll.cljs$core$IIndexed$_nth(coll, n, not_found)
    }else {
      return function() {
        var or__3548__auto____2835 = cljs.core._nth[goog.typeOf.call(null, coll)];
        if(cljs.core.truth_(or__3548__auto____2835)) {
          return or__3548__auto____2835
        }else {
          var or__3548__auto____2836 = cljs.core._nth["_"];
          if(cljs.core.truth_(or__3548__auto____2836)) {
            return or__3548__auto____2836
          }else {
            throw cljs.core.missing_protocol.call(null, "IIndexed.-nth", coll);
          }
        }
      }().call(null, coll, n, not_found)
    }
  };
  _nth = function(coll, n, not_found) {
    switch(arguments.length) {
      case 2:
        return _nth__2837.call(this, coll, n);
      case 3:
        return _nth__2838.call(this, coll, n, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return _nth
}();
cljs.core.ISeq = {};
cljs.core._first = function _first(coll) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____2840 = coll;
    if(cljs.core.truth_(and__3546__auto____2840)) {
      return coll.cljs$core$ISeq$_first
    }else {
      return and__3546__auto____2840
    }
  }())) {
    return coll.cljs$core$ISeq$_first(coll)
  }else {
    return function() {
      var or__3548__auto____2841 = cljs.core._first[goog.typeOf.call(null, coll)];
      if(cljs.core.truth_(or__3548__auto____2841)) {
        return or__3548__auto____2841
      }else {
        var or__3548__auto____2842 = cljs.core._first["_"];
        if(cljs.core.truth_(or__3548__auto____2842)) {
          return or__3548__auto____2842
        }else {
          throw cljs.core.missing_protocol.call(null, "ISeq.-first", coll);
        }
      }
    }().call(null, coll)
  }
};
cljs.core._rest = function _rest(coll) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____2843 = coll;
    if(cljs.core.truth_(and__3546__auto____2843)) {
      return coll.cljs$core$ISeq$_rest
    }else {
      return and__3546__auto____2843
    }
  }())) {
    return coll.cljs$core$ISeq$_rest(coll)
  }else {
    return function() {
      var or__3548__auto____2844 = cljs.core._rest[goog.typeOf.call(null, coll)];
      if(cljs.core.truth_(or__3548__auto____2844)) {
        return or__3548__auto____2844
      }else {
        var or__3548__auto____2845 = cljs.core._rest["_"];
        if(cljs.core.truth_(or__3548__auto____2845)) {
          return or__3548__auto____2845
        }else {
          throw cljs.core.missing_protocol.call(null, "ISeq.-rest", coll);
        }
      }
    }().call(null, coll)
  }
};
cljs.core.ILookup = {};
cljs.core._lookup = function() {
  var _lookup = null;
  var _lookup__2852 = function(o, k) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2846 = o;
      if(cljs.core.truth_(and__3546__auto____2846)) {
        return o.cljs$core$ILookup$_lookup
      }else {
        return and__3546__auto____2846
      }
    }())) {
      return o.cljs$core$ILookup$_lookup(o, k)
    }else {
      return function() {
        var or__3548__auto____2847 = cljs.core._lookup[goog.typeOf.call(null, o)];
        if(cljs.core.truth_(or__3548__auto____2847)) {
          return or__3548__auto____2847
        }else {
          var or__3548__auto____2848 = cljs.core._lookup["_"];
          if(cljs.core.truth_(or__3548__auto____2848)) {
            return or__3548__auto____2848
          }else {
            throw cljs.core.missing_protocol.call(null, "ILookup.-lookup", o);
          }
        }
      }().call(null, o, k)
    }
  };
  var _lookup__2853 = function(o, k, not_found) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2849 = o;
      if(cljs.core.truth_(and__3546__auto____2849)) {
        return o.cljs$core$ILookup$_lookup
      }else {
        return and__3546__auto____2849
      }
    }())) {
      return o.cljs$core$ILookup$_lookup(o, k, not_found)
    }else {
      return function() {
        var or__3548__auto____2850 = cljs.core._lookup[goog.typeOf.call(null, o)];
        if(cljs.core.truth_(or__3548__auto____2850)) {
          return or__3548__auto____2850
        }else {
          var or__3548__auto____2851 = cljs.core._lookup["_"];
          if(cljs.core.truth_(or__3548__auto____2851)) {
            return or__3548__auto____2851
          }else {
            throw cljs.core.missing_protocol.call(null, "ILookup.-lookup", o);
          }
        }
      }().call(null, o, k, not_found)
    }
  };
  _lookup = function(o, k, not_found) {
    switch(arguments.length) {
      case 2:
        return _lookup__2852.call(this, o, k);
      case 3:
        return _lookup__2853.call(this, o, k, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return _lookup
}();
cljs.core.IAssociative = {};
cljs.core._contains_key_QMARK_ = function _contains_key_QMARK_(coll, k) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____2855 = coll;
    if(cljs.core.truth_(and__3546__auto____2855)) {
      return coll.cljs$core$IAssociative$_contains_key_QMARK_
    }else {
      return and__3546__auto____2855
    }
  }())) {
    return coll.cljs$core$IAssociative$_contains_key_QMARK_(coll, k)
  }else {
    return function() {
      var or__3548__auto____2856 = cljs.core._contains_key_QMARK_[goog.typeOf.call(null, coll)];
      if(cljs.core.truth_(or__3548__auto____2856)) {
        return or__3548__auto____2856
      }else {
        var or__3548__auto____2857 = cljs.core._contains_key_QMARK_["_"];
        if(cljs.core.truth_(or__3548__auto____2857)) {
          return or__3548__auto____2857
        }else {
          throw cljs.core.missing_protocol.call(null, "IAssociative.-contains-key?", coll);
        }
      }
    }().call(null, coll, k)
  }
};
cljs.core._assoc = function _assoc(coll, k, v) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____2858 = coll;
    if(cljs.core.truth_(and__3546__auto____2858)) {
      return coll.cljs$core$IAssociative$_assoc
    }else {
      return and__3546__auto____2858
    }
  }())) {
    return coll.cljs$core$IAssociative$_assoc(coll, k, v)
  }else {
    return function() {
      var or__3548__auto____2859 = cljs.core._assoc[goog.typeOf.call(null, coll)];
      if(cljs.core.truth_(or__3548__auto____2859)) {
        return or__3548__auto____2859
      }else {
        var or__3548__auto____2860 = cljs.core._assoc["_"];
        if(cljs.core.truth_(or__3548__auto____2860)) {
          return or__3548__auto____2860
        }else {
          throw cljs.core.missing_protocol.call(null, "IAssociative.-assoc", coll);
        }
      }
    }().call(null, coll, k, v)
  }
};
cljs.core.IMap = {};
cljs.core._dissoc = function _dissoc(coll, k) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____2861 = coll;
    if(cljs.core.truth_(and__3546__auto____2861)) {
      return coll.cljs$core$IMap$_dissoc
    }else {
      return and__3546__auto____2861
    }
  }())) {
    return coll.cljs$core$IMap$_dissoc(coll, k)
  }else {
    return function() {
      var or__3548__auto____2862 = cljs.core._dissoc[goog.typeOf.call(null, coll)];
      if(cljs.core.truth_(or__3548__auto____2862)) {
        return or__3548__auto____2862
      }else {
        var or__3548__auto____2863 = cljs.core._dissoc["_"];
        if(cljs.core.truth_(or__3548__auto____2863)) {
          return or__3548__auto____2863
        }else {
          throw cljs.core.missing_protocol.call(null, "IMap.-dissoc", coll);
        }
      }
    }().call(null, coll, k)
  }
};
cljs.core.ISet = {};
cljs.core._disjoin = function _disjoin(coll, v) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____2864 = coll;
    if(cljs.core.truth_(and__3546__auto____2864)) {
      return coll.cljs$core$ISet$_disjoin
    }else {
      return and__3546__auto____2864
    }
  }())) {
    return coll.cljs$core$ISet$_disjoin(coll, v)
  }else {
    return function() {
      var or__3548__auto____2865 = cljs.core._disjoin[goog.typeOf.call(null, coll)];
      if(cljs.core.truth_(or__3548__auto____2865)) {
        return or__3548__auto____2865
      }else {
        var or__3548__auto____2866 = cljs.core._disjoin["_"];
        if(cljs.core.truth_(or__3548__auto____2866)) {
          return or__3548__auto____2866
        }else {
          throw cljs.core.missing_protocol.call(null, "ISet.-disjoin", coll);
        }
      }
    }().call(null, coll, v)
  }
};
cljs.core.IStack = {};
cljs.core._peek = function _peek(coll) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____2867 = coll;
    if(cljs.core.truth_(and__3546__auto____2867)) {
      return coll.cljs$core$IStack$_peek
    }else {
      return and__3546__auto____2867
    }
  }())) {
    return coll.cljs$core$IStack$_peek(coll)
  }else {
    return function() {
      var or__3548__auto____2868 = cljs.core._peek[goog.typeOf.call(null, coll)];
      if(cljs.core.truth_(or__3548__auto____2868)) {
        return or__3548__auto____2868
      }else {
        var or__3548__auto____2869 = cljs.core._peek["_"];
        if(cljs.core.truth_(or__3548__auto____2869)) {
          return or__3548__auto____2869
        }else {
          throw cljs.core.missing_protocol.call(null, "IStack.-peek", coll);
        }
      }
    }().call(null, coll)
  }
};
cljs.core._pop = function _pop(coll) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____2870 = coll;
    if(cljs.core.truth_(and__3546__auto____2870)) {
      return coll.cljs$core$IStack$_pop
    }else {
      return and__3546__auto____2870
    }
  }())) {
    return coll.cljs$core$IStack$_pop(coll)
  }else {
    return function() {
      var or__3548__auto____2871 = cljs.core._pop[goog.typeOf.call(null, coll)];
      if(cljs.core.truth_(or__3548__auto____2871)) {
        return or__3548__auto____2871
      }else {
        var or__3548__auto____2872 = cljs.core._pop["_"];
        if(cljs.core.truth_(or__3548__auto____2872)) {
          return or__3548__auto____2872
        }else {
          throw cljs.core.missing_protocol.call(null, "IStack.-pop", coll);
        }
      }
    }().call(null, coll)
  }
};
cljs.core.IVector = {};
cljs.core._assoc_n = function _assoc_n(coll, n, val) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____2873 = coll;
    if(cljs.core.truth_(and__3546__auto____2873)) {
      return coll.cljs$core$IVector$_assoc_n
    }else {
      return and__3546__auto____2873
    }
  }())) {
    return coll.cljs$core$IVector$_assoc_n(coll, n, val)
  }else {
    return function() {
      var or__3548__auto____2874 = cljs.core._assoc_n[goog.typeOf.call(null, coll)];
      if(cljs.core.truth_(or__3548__auto____2874)) {
        return or__3548__auto____2874
      }else {
        var or__3548__auto____2875 = cljs.core._assoc_n["_"];
        if(cljs.core.truth_(or__3548__auto____2875)) {
          return or__3548__auto____2875
        }else {
          throw cljs.core.missing_protocol.call(null, "IVector.-assoc-n", coll);
        }
      }
    }().call(null, coll, n, val)
  }
};
cljs.core.IDeref = {};
cljs.core._deref = function _deref(o) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____2876 = o;
    if(cljs.core.truth_(and__3546__auto____2876)) {
      return o.cljs$core$IDeref$_deref
    }else {
      return and__3546__auto____2876
    }
  }())) {
    return o.cljs$core$IDeref$_deref(o)
  }else {
    return function() {
      var or__3548__auto____2877 = cljs.core._deref[goog.typeOf.call(null, o)];
      if(cljs.core.truth_(or__3548__auto____2877)) {
        return or__3548__auto____2877
      }else {
        var or__3548__auto____2878 = cljs.core._deref["_"];
        if(cljs.core.truth_(or__3548__auto____2878)) {
          return or__3548__auto____2878
        }else {
          throw cljs.core.missing_protocol.call(null, "IDeref.-deref", o);
        }
      }
    }().call(null, o)
  }
};
cljs.core.IDerefWithTimeout = {};
cljs.core._deref_with_timeout = function _deref_with_timeout(o, msec, timeout_val) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____2879 = o;
    if(cljs.core.truth_(and__3546__auto____2879)) {
      return o.cljs$core$IDerefWithTimeout$_deref_with_timeout
    }else {
      return and__3546__auto____2879
    }
  }())) {
    return o.cljs$core$IDerefWithTimeout$_deref_with_timeout(o, msec, timeout_val)
  }else {
    return function() {
      var or__3548__auto____2880 = cljs.core._deref_with_timeout[goog.typeOf.call(null, o)];
      if(cljs.core.truth_(or__3548__auto____2880)) {
        return or__3548__auto____2880
      }else {
        var or__3548__auto____2881 = cljs.core._deref_with_timeout["_"];
        if(cljs.core.truth_(or__3548__auto____2881)) {
          return or__3548__auto____2881
        }else {
          throw cljs.core.missing_protocol.call(null, "IDerefWithTimeout.-deref-with-timeout", o);
        }
      }
    }().call(null, o, msec, timeout_val)
  }
};
cljs.core.IMeta = {};
cljs.core._meta = function _meta(o) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____2882 = o;
    if(cljs.core.truth_(and__3546__auto____2882)) {
      return o.cljs$core$IMeta$_meta
    }else {
      return and__3546__auto____2882
    }
  }())) {
    return o.cljs$core$IMeta$_meta(o)
  }else {
    return function() {
      var or__3548__auto____2883 = cljs.core._meta[goog.typeOf.call(null, o)];
      if(cljs.core.truth_(or__3548__auto____2883)) {
        return or__3548__auto____2883
      }else {
        var or__3548__auto____2884 = cljs.core._meta["_"];
        if(cljs.core.truth_(or__3548__auto____2884)) {
          return or__3548__auto____2884
        }else {
          throw cljs.core.missing_protocol.call(null, "IMeta.-meta", o);
        }
      }
    }().call(null, o)
  }
};
cljs.core.IWithMeta = {};
cljs.core._with_meta = function _with_meta(o, meta) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____2885 = o;
    if(cljs.core.truth_(and__3546__auto____2885)) {
      return o.cljs$core$IWithMeta$_with_meta
    }else {
      return and__3546__auto____2885
    }
  }())) {
    return o.cljs$core$IWithMeta$_with_meta(o, meta)
  }else {
    return function() {
      var or__3548__auto____2886 = cljs.core._with_meta[goog.typeOf.call(null, o)];
      if(cljs.core.truth_(or__3548__auto____2886)) {
        return or__3548__auto____2886
      }else {
        var or__3548__auto____2887 = cljs.core._with_meta["_"];
        if(cljs.core.truth_(or__3548__auto____2887)) {
          return or__3548__auto____2887
        }else {
          throw cljs.core.missing_protocol.call(null, "IWithMeta.-with-meta", o);
        }
      }
    }().call(null, o, meta)
  }
};
cljs.core.IReduce = {};
cljs.core._reduce = function() {
  var _reduce = null;
  var _reduce__2894 = function(coll, f) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2888 = coll;
      if(cljs.core.truth_(and__3546__auto____2888)) {
        return coll.cljs$core$IReduce$_reduce
      }else {
        return and__3546__auto____2888
      }
    }())) {
      return coll.cljs$core$IReduce$_reduce(coll, f)
    }else {
      return function() {
        var or__3548__auto____2889 = cljs.core._reduce[goog.typeOf.call(null, coll)];
        if(cljs.core.truth_(or__3548__auto____2889)) {
          return or__3548__auto____2889
        }else {
          var or__3548__auto____2890 = cljs.core._reduce["_"];
          if(cljs.core.truth_(or__3548__auto____2890)) {
            return or__3548__auto____2890
          }else {
            throw cljs.core.missing_protocol.call(null, "IReduce.-reduce", coll);
          }
        }
      }().call(null, coll, f)
    }
  };
  var _reduce__2895 = function(coll, f, start) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2891 = coll;
      if(cljs.core.truth_(and__3546__auto____2891)) {
        return coll.cljs$core$IReduce$_reduce
      }else {
        return and__3546__auto____2891
      }
    }())) {
      return coll.cljs$core$IReduce$_reduce(coll, f, start)
    }else {
      return function() {
        var or__3548__auto____2892 = cljs.core._reduce[goog.typeOf.call(null, coll)];
        if(cljs.core.truth_(or__3548__auto____2892)) {
          return or__3548__auto____2892
        }else {
          var or__3548__auto____2893 = cljs.core._reduce["_"];
          if(cljs.core.truth_(or__3548__auto____2893)) {
            return or__3548__auto____2893
          }else {
            throw cljs.core.missing_protocol.call(null, "IReduce.-reduce", coll);
          }
        }
      }().call(null, coll, f, start)
    }
  };
  _reduce = function(coll, f, start) {
    switch(arguments.length) {
      case 2:
        return _reduce__2894.call(this, coll, f);
      case 3:
        return _reduce__2895.call(this, coll, f, start)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return _reduce
}();
cljs.core.IEquiv = {};
cljs.core._equiv = function _equiv(o, other) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____2897 = o;
    if(cljs.core.truth_(and__3546__auto____2897)) {
      return o.cljs$core$IEquiv$_equiv
    }else {
      return and__3546__auto____2897
    }
  }())) {
    return o.cljs$core$IEquiv$_equiv(o, other)
  }else {
    return function() {
      var or__3548__auto____2898 = cljs.core._equiv[goog.typeOf.call(null, o)];
      if(cljs.core.truth_(or__3548__auto____2898)) {
        return or__3548__auto____2898
      }else {
        var or__3548__auto____2899 = cljs.core._equiv["_"];
        if(cljs.core.truth_(or__3548__auto____2899)) {
          return or__3548__auto____2899
        }else {
          throw cljs.core.missing_protocol.call(null, "IEquiv.-equiv", o);
        }
      }
    }().call(null, o, other)
  }
};
cljs.core.IHash = {};
cljs.core._hash = function _hash(o) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____2900 = o;
    if(cljs.core.truth_(and__3546__auto____2900)) {
      return o.cljs$core$IHash$_hash
    }else {
      return and__3546__auto____2900
    }
  }())) {
    return o.cljs$core$IHash$_hash(o)
  }else {
    return function() {
      var or__3548__auto____2901 = cljs.core._hash[goog.typeOf.call(null, o)];
      if(cljs.core.truth_(or__3548__auto____2901)) {
        return or__3548__auto____2901
      }else {
        var or__3548__auto____2902 = cljs.core._hash["_"];
        if(cljs.core.truth_(or__3548__auto____2902)) {
          return or__3548__auto____2902
        }else {
          throw cljs.core.missing_protocol.call(null, "IHash.-hash", o);
        }
      }
    }().call(null, o)
  }
};
cljs.core.ISeqable = {};
cljs.core._seq = function _seq(o) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____2903 = o;
    if(cljs.core.truth_(and__3546__auto____2903)) {
      return o.cljs$core$ISeqable$_seq
    }else {
      return and__3546__auto____2903
    }
  }())) {
    return o.cljs$core$ISeqable$_seq(o)
  }else {
    return function() {
      var or__3548__auto____2904 = cljs.core._seq[goog.typeOf.call(null, o)];
      if(cljs.core.truth_(or__3548__auto____2904)) {
        return or__3548__auto____2904
      }else {
        var or__3548__auto____2905 = cljs.core._seq["_"];
        if(cljs.core.truth_(or__3548__auto____2905)) {
          return or__3548__auto____2905
        }else {
          throw cljs.core.missing_protocol.call(null, "ISeqable.-seq", o);
        }
      }
    }().call(null, o)
  }
};
cljs.core.ISequential = {};
cljs.core.IRecord = {};
cljs.core.IPrintable = {};
cljs.core._pr_seq = function _pr_seq(o, opts) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____2906 = o;
    if(cljs.core.truth_(and__3546__auto____2906)) {
      return o.cljs$core$IPrintable$_pr_seq
    }else {
      return and__3546__auto____2906
    }
  }())) {
    return o.cljs$core$IPrintable$_pr_seq(o, opts)
  }else {
    return function() {
      var or__3548__auto____2907 = cljs.core._pr_seq[goog.typeOf.call(null, o)];
      if(cljs.core.truth_(or__3548__auto____2907)) {
        return or__3548__auto____2907
      }else {
        var or__3548__auto____2908 = cljs.core._pr_seq["_"];
        if(cljs.core.truth_(or__3548__auto____2908)) {
          return or__3548__auto____2908
        }else {
          throw cljs.core.missing_protocol.call(null, "IPrintable.-pr-seq", o);
        }
      }
    }().call(null, o, opts)
  }
};
cljs.core.IPending = {};
cljs.core._realized_QMARK_ = function _realized_QMARK_(d) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____2909 = d;
    if(cljs.core.truth_(and__3546__auto____2909)) {
      return d.cljs$core$IPending$_realized_QMARK_
    }else {
      return and__3546__auto____2909
    }
  }())) {
    return d.cljs$core$IPending$_realized_QMARK_(d)
  }else {
    return function() {
      var or__3548__auto____2910 = cljs.core._realized_QMARK_[goog.typeOf.call(null, d)];
      if(cljs.core.truth_(or__3548__auto____2910)) {
        return or__3548__auto____2910
      }else {
        var or__3548__auto____2911 = cljs.core._realized_QMARK_["_"];
        if(cljs.core.truth_(or__3548__auto____2911)) {
          return or__3548__auto____2911
        }else {
          throw cljs.core.missing_protocol.call(null, "IPending.-realized?", d);
        }
      }
    }().call(null, d)
  }
};
cljs.core.IWatchable = {};
cljs.core._notify_watches = function _notify_watches(this$, oldval, newval) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____2912 = this$;
    if(cljs.core.truth_(and__3546__auto____2912)) {
      return this$.cljs$core$IWatchable$_notify_watches
    }else {
      return and__3546__auto____2912
    }
  }())) {
    return this$.cljs$core$IWatchable$_notify_watches(this$, oldval, newval)
  }else {
    return function() {
      var or__3548__auto____2913 = cljs.core._notify_watches[goog.typeOf.call(null, this$)];
      if(cljs.core.truth_(or__3548__auto____2913)) {
        return or__3548__auto____2913
      }else {
        var or__3548__auto____2914 = cljs.core._notify_watches["_"];
        if(cljs.core.truth_(or__3548__auto____2914)) {
          return or__3548__auto____2914
        }else {
          throw cljs.core.missing_protocol.call(null, "IWatchable.-notify-watches", this$);
        }
      }
    }().call(null, this$, oldval, newval)
  }
};
cljs.core._add_watch = function _add_watch(this$, key, f) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____2915 = this$;
    if(cljs.core.truth_(and__3546__auto____2915)) {
      return this$.cljs$core$IWatchable$_add_watch
    }else {
      return and__3546__auto____2915
    }
  }())) {
    return this$.cljs$core$IWatchable$_add_watch(this$, key, f)
  }else {
    return function() {
      var or__3548__auto____2916 = cljs.core._add_watch[goog.typeOf.call(null, this$)];
      if(cljs.core.truth_(or__3548__auto____2916)) {
        return or__3548__auto____2916
      }else {
        var or__3548__auto____2917 = cljs.core._add_watch["_"];
        if(cljs.core.truth_(or__3548__auto____2917)) {
          return or__3548__auto____2917
        }else {
          throw cljs.core.missing_protocol.call(null, "IWatchable.-add-watch", this$);
        }
      }
    }().call(null, this$, key, f)
  }
};
cljs.core._remove_watch = function _remove_watch(this$, key) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____2918 = this$;
    if(cljs.core.truth_(and__3546__auto____2918)) {
      return this$.cljs$core$IWatchable$_remove_watch
    }else {
      return and__3546__auto____2918
    }
  }())) {
    return this$.cljs$core$IWatchable$_remove_watch(this$, key)
  }else {
    return function() {
      var or__3548__auto____2919 = cljs.core._remove_watch[goog.typeOf.call(null, this$)];
      if(cljs.core.truth_(or__3548__auto____2919)) {
        return or__3548__auto____2919
      }else {
        var or__3548__auto____2920 = cljs.core._remove_watch["_"];
        if(cljs.core.truth_(or__3548__auto____2920)) {
          return or__3548__auto____2920
        }else {
          throw cljs.core.missing_protocol.call(null, "IWatchable.-remove-watch", this$);
        }
      }
    }().call(null, this$, key)
  }
};
cljs.core.identical_QMARK_ = function identical_QMARK_(x, y) {
  return x === y
};
cljs.core._EQ_ = function _EQ_(x, y) {
  return cljs.core._equiv.call(null, x, y)
};
cljs.core.nil_QMARK_ = function nil_QMARK_(x) {
  return x === null
};
cljs.core.type = function type(x) {
  return x.constructor
};
cljs.core.IHash["null"] = true;
cljs.core._hash["null"] = function(o) {
  return 0
};
cljs.core.ILookup["null"] = true;
cljs.core._lookup["null"] = function() {
  var G__2921 = null;
  var G__2921__2922 = function(o, k) {
    return null
  };
  var G__2921__2923 = function(o, k, not_found) {
    return not_found
  };
  G__2921 = function(o, k, not_found) {
    switch(arguments.length) {
      case 2:
        return G__2921__2922.call(this, o, k);
      case 3:
        return G__2921__2923.call(this, o, k, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__2921
}();
cljs.core.IAssociative["null"] = true;
cljs.core._assoc["null"] = function(_, k, v) {
  return cljs.core.hash_map.call(null, k, v)
};
cljs.core.ICollection["null"] = true;
cljs.core._conj["null"] = function(_, o) {
  return cljs.core.list.call(null, o)
};
cljs.core.IReduce["null"] = true;
cljs.core._reduce["null"] = function() {
  var G__2925 = null;
  var G__2925__2926 = function(_, f) {
    return f.call(null)
  };
  var G__2925__2927 = function(_, f, start) {
    return start
  };
  G__2925 = function(_, f, start) {
    switch(arguments.length) {
      case 2:
        return G__2925__2926.call(this, _, f);
      case 3:
        return G__2925__2927.call(this, _, f, start)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__2925
}();
cljs.core.IPrintable["null"] = true;
cljs.core._pr_seq["null"] = function(o) {
  return cljs.core.list.call(null, "nil")
};
cljs.core.ISet["null"] = true;
cljs.core._disjoin["null"] = function(_, v) {
  return null
};
cljs.core.ICounted["null"] = true;
cljs.core._count["null"] = function(_) {
  return 0
};
cljs.core.IStack["null"] = true;
cljs.core._peek["null"] = function(_) {
  return null
};
cljs.core._pop["null"] = function(_) {
  return null
};
cljs.core.ISeq["null"] = true;
cljs.core._first["null"] = function(_) {
  return null
};
cljs.core._rest["null"] = function(_) {
  return cljs.core.list.call(null)
};
cljs.core.IEquiv["null"] = true;
cljs.core._equiv["null"] = function(_, o) {
  return o === null
};
cljs.core.IWithMeta["null"] = true;
cljs.core._with_meta["null"] = function(_, meta) {
  return null
};
cljs.core.IMeta["null"] = true;
cljs.core._meta["null"] = function(_) {
  return null
};
cljs.core.IIndexed["null"] = true;
cljs.core._nth["null"] = function() {
  var G__2929 = null;
  var G__2929__2930 = function(_, n) {
    return null
  };
  var G__2929__2931 = function(_, n, not_found) {
    return not_found
  };
  G__2929 = function(_, n, not_found) {
    switch(arguments.length) {
      case 2:
        return G__2929__2930.call(this, _, n);
      case 3:
        return G__2929__2931.call(this, _, n, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__2929
}();
cljs.core.IEmptyableCollection["null"] = true;
cljs.core._empty["null"] = function(_) {
  return null
};
cljs.core.IMap["null"] = true;
cljs.core._dissoc["null"] = function(_, k) {
  return null
};
Date.prototype.cljs$core$IEquiv$ = true;
Date.prototype.cljs$core$IEquiv$_equiv = function(o, other) {
  return o.toString() === other.toString()
};
cljs.core.IHash["number"] = true;
cljs.core._hash["number"] = function(o) {
  return o
};
cljs.core.IEquiv["number"] = true;
cljs.core._equiv["number"] = function(x, o) {
  return x === o
};
cljs.core.IHash["boolean"] = true;
cljs.core._hash["boolean"] = function(o) {
  return o === true ? 1 : 0
};
cljs.core.IHash["function"] = true;
cljs.core._hash["function"] = function(o) {
  return goog.getUid.call(null, o)
};
cljs.core.inc = function inc(x) {
  return x + 1
};
cljs.core.ci_reduce = function() {
  var ci_reduce = null;
  var ci_reduce__2939 = function(cicoll, f) {
    if(cljs.core.truth_(cljs.core._EQ_.call(null, 0, cljs.core._count.call(null, cicoll)))) {
      return f.call(null)
    }else {
      var val__2933 = cljs.core._nth.call(null, cicoll, 0);
      var n__2934 = 1;
      while(true) {
        if(cljs.core.truth_(n__2934 < cljs.core._count.call(null, cicoll))) {
          var G__2943 = f.call(null, val__2933, cljs.core._nth.call(null, cicoll, n__2934));
          var G__2944 = n__2934 + 1;
          val__2933 = G__2943;
          n__2934 = G__2944;
          continue
        }else {
          return val__2933
        }
        break
      }
    }
  };
  var ci_reduce__2940 = function(cicoll, f, val) {
    var val__2935 = val;
    var n__2936 = 0;
    while(true) {
      if(cljs.core.truth_(n__2936 < cljs.core._count.call(null, cicoll))) {
        var G__2945 = f.call(null, val__2935, cljs.core._nth.call(null, cicoll, n__2936));
        var G__2946 = n__2936 + 1;
        val__2935 = G__2945;
        n__2936 = G__2946;
        continue
      }else {
        return val__2935
      }
      break
    }
  };
  var ci_reduce__2941 = function(cicoll, f, val, idx) {
    var val__2937 = val;
    var n__2938 = idx;
    while(true) {
      if(cljs.core.truth_(n__2938 < cljs.core._count.call(null, cicoll))) {
        var G__2947 = f.call(null, val__2937, cljs.core._nth.call(null, cicoll, n__2938));
        var G__2948 = n__2938 + 1;
        val__2937 = G__2947;
        n__2938 = G__2948;
        continue
      }else {
        return val__2937
      }
      break
    }
  };
  ci_reduce = function(cicoll, f, val, idx) {
    switch(arguments.length) {
      case 2:
        return ci_reduce__2939.call(this, cicoll, f);
      case 3:
        return ci_reduce__2940.call(this, cicoll, f, val);
      case 4:
        return ci_reduce__2941.call(this, cicoll, f, val, idx)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return ci_reduce
}();
cljs.core.IndexedSeq = function(a, i) {
  this.a = a;
  this.i = i
};
cljs.core.IndexedSeq.cljs$core$IPrintable$_pr_seq = function(this__267__auto__) {
  return cljs.core.list.call(null, "cljs.core.IndexedSeq")
};
cljs.core.IndexedSeq.prototype.cljs$core$IHash$ = true;
cljs.core.IndexedSeq.prototype.cljs$core$IHash$_hash = function(coll) {
  var this__2949 = this;
  return cljs.core.hash_coll.call(null, coll)
};
cljs.core.IndexedSeq.prototype.cljs$core$IReduce$ = true;
cljs.core.IndexedSeq.prototype.cljs$core$IReduce$_reduce = function() {
  var G__2962 = null;
  var G__2962__2963 = function(_, f) {
    var this__2950 = this;
    return cljs.core.ci_reduce.call(null, this__2950.a, f, this__2950.a[this__2950.i], this__2950.i + 1)
  };
  var G__2962__2964 = function(_, f, start) {
    var this__2951 = this;
    return cljs.core.ci_reduce.call(null, this__2951.a, f, start, this__2951.i)
  };
  G__2962 = function(_, f, start) {
    switch(arguments.length) {
      case 2:
        return G__2962__2963.call(this, _, f);
      case 3:
        return G__2962__2964.call(this, _, f, start)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__2962
}();
cljs.core.IndexedSeq.prototype.cljs$core$ICollection$ = true;
cljs.core.IndexedSeq.prototype.cljs$core$ICollection$_conj = function(coll, o) {
  var this__2952 = this;
  return cljs.core.cons.call(null, o, coll)
};
cljs.core.IndexedSeq.prototype.cljs$core$IEquiv$ = true;
cljs.core.IndexedSeq.prototype.cljs$core$IEquiv$_equiv = function(coll, other) {
  var this__2953 = this;
  return cljs.core.equiv_sequential.call(null, coll, other)
};
cljs.core.IndexedSeq.prototype.cljs$core$ISequential$ = true;
cljs.core.IndexedSeq.prototype.cljs$core$IIndexed$ = true;
cljs.core.IndexedSeq.prototype.cljs$core$IIndexed$_nth = function() {
  var G__2966 = null;
  var G__2966__2967 = function(coll, n) {
    var this__2954 = this;
    var i__2955 = n + this__2954.i;
    if(cljs.core.truth_(i__2955 < this__2954.a.length)) {
      return this__2954.a[i__2955]
    }else {
      return null
    }
  };
  var G__2966__2968 = function(coll, n, not_found) {
    var this__2956 = this;
    var i__2957 = n + this__2956.i;
    if(cljs.core.truth_(i__2957 < this__2956.a.length)) {
      return this__2956.a[i__2957]
    }else {
      return not_found
    }
  };
  G__2966 = function(coll, n, not_found) {
    switch(arguments.length) {
      case 2:
        return G__2966__2967.call(this, coll, n);
      case 3:
        return G__2966__2968.call(this, coll, n, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__2966
}();
cljs.core.IndexedSeq.prototype.cljs$core$ICounted$ = true;
cljs.core.IndexedSeq.prototype.cljs$core$ICounted$_count = function(_) {
  var this__2958 = this;
  return this__2958.a.length - this__2958.i
};
cljs.core.IndexedSeq.prototype.cljs$core$ISeq$ = true;
cljs.core.IndexedSeq.prototype.cljs$core$ISeq$_first = function(_) {
  var this__2959 = this;
  return this__2959.a[this__2959.i]
};
cljs.core.IndexedSeq.prototype.cljs$core$ISeq$_rest = function(_) {
  var this__2960 = this;
  if(cljs.core.truth_(this__2960.i + 1 < this__2960.a.length)) {
    return new cljs.core.IndexedSeq(this__2960.a, this__2960.i + 1)
  }else {
    return cljs.core.list.call(null)
  }
};
cljs.core.IndexedSeq.prototype.cljs$core$ISeqable$ = true;
cljs.core.IndexedSeq.prototype.cljs$core$ISeqable$_seq = function(this$) {
  var this__2961 = this;
  return this$
};
cljs.core.IndexedSeq;
cljs.core.prim_seq = function prim_seq(prim, i) {
  if(cljs.core.truth_(cljs.core._EQ_.call(null, 0, prim.length))) {
    return null
  }else {
    return new cljs.core.IndexedSeq(prim, i)
  }
};
cljs.core.array_seq = function array_seq(array, i) {
  return cljs.core.prim_seq.call(null, array, i)
};
cljs.core.IReduce["array"] = true;
cljs.core._reduce["array"] = function() {
  var G__2970 = null;
  var G__2970__2971 = function(array, f) {
    return cljs.core.ci_reduce.call(null, array, f)
  };
  var G__2970__2972 = function(array, f, start) {
    return cljs.core.ci_reduce.call(null, array, f, start)
  };
  G__2970 = function(array, f, start) {
    switch(arguments.length) {
      case 2:
        return G__2970__2971.call(this, array, f);
      case 3:
        return G__2970__2972.call(this, array, f, start)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__2970
}();
cljs.core.ILookup["array"] = true;
cljs.core._lookup["array"] = function() {
  var G__2974 = null;
  var G__2974__2975 = function(array, k) {
    return array[k]
  };
  var G__2974__2976 = function(array, k, not_found) {
    return cljs.core._nth.call(null, array, k, not_found)
  };
  G__2974 = function(array, k, not_found) {
    switch(arguments.length) {
      case 2:
        return G__2974__2975.call(this, array, k);
      case 3:
        return G__2974__2976.call(this, array, k, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__2974
}();
cljs.core.IIndexed["array"] = true;
cljs.core._nth["array"] = function() {
  var G__2978 = null;
  var G__2978__2979 = function(array, n) {
    if(cljs.core.truth_(n < array.length)) {
      return array[n]
    }else {
      return null
    }
  };
  var G__2978__2980 = function(array, n, not_found) {
    if(cljs.core.truth_(n < array.length)) {
      return array[n]
    }else {
      return not_found
    }
  };
  G__2978 = function(array, n, not_found) {
    switch(arguments.length) {
      case 2:
        return G__2978__2979.call(this, array, n);
      case 3:
        return G__2978__2980.call(this, array, n, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__2978
}();
cljs.core.ICounted["array"] = true;
cljs.core._count["array"] = function(a) {
  return a.length
};
cljs.core.ISeqable["array"] = true;
cljs.core._seq["array"] = function(array) {
  return cljs.core.array_seq.call(null, array, 0)
};
cljs.core.seq = function seq(coll) {
  if(cljs.core.truth_(coll)) {
    return cljs.core._seq.call(null, coll)
  }else {
    return null
  }
};
cljs.core.first = function first(coll) {
  var temp__3698__auto____2982 = cljs.core.seq.call(null, coll);
  if(cljs.core.truth_(temp__3698__auto____2982)) {
    var s__2983 = temp__3698__auto____2982;
    return cljs.core._first.call(null, s__2983)
  }else {
    return null
  }
};
cljs.core.rest = function rest(coll) {
  return cljs.core._rest.call(null, cljs.core.seq.call(null, coll))
};
cljs.core.next = function next(coll) {
  if(cljs.core.truth_(coll)) {
    return cljs.core.seq.call(null, cljs.core.rest.call(null, coll))
  }else {
    return null
  }
};
cljs.core.second = function second(coll) {
  return cljs.core.first.call(null, cljs.core.next.call(null, coll))
};
cljs.core.ffirst = function ffirst(coll) {
  return cljs.core.first.call(null, cljs.core.first.call(null, coll))
};
cljs.core.nfirst = function nfirst(coll) {
  return cljs.core.next.call(null, cljs.core.first.call(null, coll))
};
cljs.core.fnext = function fnext(coll) {
  return cljs.core.first.call(null, cljs.core.next.call(null, coll))
};
cljs.core.nnext = function nnext(coll) {
  return cljs.core.next.call(null, cljs.core.next.call(null, coll))
};
cljs.core.last = function last(s) {
  while(true) {
    if(cljs.core.truth_(cljs.core.next.call(null, s))) {
      var G__2984 = cljs.core.next.call(null, s);
      s = G__2984;
      continue
    }else {
      return cljs.core.first.call(null, s)
    }
    break
  }
};
cljs.core.ICounted["_"] = true;
cljs.core._count["_"] = function(x) {
  var s__2985 = cljs.core.seq.call(null, x);
  var n__2986 = 0;
  while(true) {
    if(cljs.core.truth_(s__2985)) {
      var G__2987 = cljs.core.next.call(null, s__2985);
      var G__2988 = n__2986 + 1;
      s__2985 = G__2987;
      n__2986 = G__2988;
      continue
    }else {
      return n__2986
    }
    break
  }
};
cljs.core.IEquiv["_"] = true;
cljs.core._equiv["_"] = function(x, o) {
  return x === o
};
cljs.core.not = function not(x) {
  if(cljs.core.truth_(x)) {
    return false
  }else {
    return true
  }
};
cljs.core.conj = function() {
  var conj = null;
  var conj__2989 = function(coll, x) {
    return cljs.core._conj.call(null, coll, x)
  };
  var conj__2990 = function() {
    var G__2992__delegate = function(coll, x, xs) {
      while(true) {
        if(cljs.core.truth_(xs)) {
          var G__2993 = conj.call(null, coll, x);
          var G__2994 = cljs.core.first.call(null, xs);
          var G__2995 = cljs.core.next.call(null, xs);
          coll = G__2993;
          x = G__2994;
          xs = G__2995;
          continue
        }else {
          return conj.call(null, coll, x)
        }
        break
      }
    };
    var G__2992 = function(coll, x, var_args) {
      var xs = null;
      if(goog.isDef(var_args)) {
        xs = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__2992__delegate.call(this, coll, x, xs)
    };
    G__2992.cljs$lang$maxFixedArity = 2;
    G__2992.cljs$lang$applyTo = function(arglist__2996) {
      var coll = cljs.core.first(arglist__2996);
      var x = cljs.core.first(cljs.core.next(arglist__2996));
      var xs = cljs.core.rest(cljs.core.next(arglist__2996));
      return G__2992__delegate.call(this, coll, x, xs)
    };
    return G__2992
  }();
  conj = function(coll, x, var_args) {
    var xs = var_args;
    switch(arguments.length) {
      case 2:
        return conj__2989.call(this, coll, x);
      default:
        return conj__2990.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  conj.cljs$lang$maxFixedArity = 2;
  conj.cljs$lang$applyTo = conj__2990.cljs$lang$applyTo;
  return conj
}();
cljs.core.empty = function empty(coll) {
  return cljs.core._empty.call(null, coll)
};
cljs.core.count = function count(coll) {
  return cljs.core._count.call(null, coll)
};
cljs.core.nth = function() {
  var nth = null;
  var nth__2997 = function(coll, n) {
    return cljs.core._nth.call(null, coll, Math.floor(n))
  };
  var nth__2998 = function(coll, n, not_found) {
    return cljs.core._nth.call(null, coll, Math.floor(n), not_found)
  };
  nth = function(coll, n, not_found) {
    switch(arguments.length) {
      case 2:
        return nth__2997.call(this, coll, n);
      case 3:
        return nth__2998.call(this, coll, n, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return nth
}();
cljs.core.get = function() {
  var get = null;
  var get__3000 = function(o, k) {
    return cljs.core._lookup.call(null, o, k)
  };
  var get__3001 = function(o, k, not_found) {
    return cljs.core._lookup.call(null, o, k, not_found)
  };
  get = function(o, k, not_found) {
    switch(arguments.length) {
      case 2:
        return get__3000.call(this, o, k);
      case 3:
        return get__3001.call(this, o, k, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return get
}();
cljs.core.assoc = function() {
  var assoc = null;
  var assoc__3004 = function(coll, k, v) {
    return cljs.core._assoc.call(null, coll, k, v)
  };
  var assoc__3005 = function() {
    var G__3007__delegate = function(coll, k, v, kvs) {
      while(true) {
        var ret__3003 = assoc.call(null, coll, k, v);
        if(cljs.core.truth_(kvs)) {
          var G__3008 = ret__3003;
          var G__3009 = cljs.core.first.call(null, kvs);
          var G__3010 = cljs.core.second.call(null, kvs);
          var G__3011 = cljs.core.nnext.call(null, kvs);
          coll = G__3008;
          k = G__3009;
          v = G__3010;
          kvs = G__3011;
          continue
        }else {
          return ret__3003
        }
        break
      }
    };
    var G__3007 = function(coll, k, v, var_args) {
      var kvs = null;
      if(goog.isDef(var_args)) {
        kvs = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
      }
      return G__3007__delegate.call(this, coll, k, v, kvs)
    };
    G__3007.cljs$lang$maxFixedArity = 3;
    G__3007.cljs$lang$applyTo = function(arglist__3012) {
      var coll = cljs.core.first(arglist__3012);
      var k = cljs.core.first(cljs.core.next(arglist__3012));
      var v = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3012)));
      var kvs = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3012)));
      return G__3007__delegate.call(this, coll, k, v, kvs)
    };
    return G__3007
  }();
  assoc = function(coll, k, v, var_args) {
    var kvs = var_args;
    switch(arguments.length) {
      case 3:
        return assoc__3004.call(this, coll, k, v);
      default:
        return assoc__3005.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  assoc.cljs$lang$maxFixedArity = 3;
  assoc.cljs$lang$applyTo = assoc__3005.cljs$lang$applyTo;
  return assoc
}();
cljs.core.dissoc = function() {
  var dissoc = null;
  var dissoc__3014 = function(coll) {
    return coll
  };
  var dissoc__3015 = function(coll, k) {
    return cljs.core._dissoc.call(null, coll, k)
  };
  var dissoc__3016 = function() {
    var G__3018__delegate = function(coll, k, ks) {
      while(true) {
        var ret__3013 = dissoc.call(null, coll, k);
        if(cljs.core.truth_(ks)) {
          var G__3019 = ret__3013;
          var G__3020 = cljs.core.first.call(null, ks);
          var G__3021 = cljs.core.next.call(null, ks);
          coll = G__3019;
          k = G__3020;
          ks = G__3021;
          continue
        }else {
          return ret__3013
        }
        break
      }
    };
    var G__3018 = function(coll, k, var_args) {
      var ks = null;
      if(goog.isDef(var_args)) {
        ks = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3018__delegate.call(this, coll, k, ks)
    };
    G__3018.cljs$lang$maxFixedArity = 2;
    G__3018.cljs$lang$applyTo = function(arglist__3022) {
      var coll = cljs.core.first(arglist__3022);
      var k = cljs.core.first(cljs.core.next(arglist__3022));
      var ks = cljs.core.rest(cljs.core.next(arglist__3022));
      return G__3018__delegate.call(this, coll, k, ks)
    };
    return G__3018
  }();
  dissoc = function(coll, k, var_args) {
    var ks = var_args;
    switch(arguments.length) {
      case 1:
        return dissoc__3014.call(this, coll);
      case 2:
        return dissoc__3015.call(this, coll, k);
      default:
        return dissoc__3016.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  dissoc.cljs$lang$maxFixedArity = 2;
  dissoc.cljs$lang$applyTo = dissoc__3016.cljs$lang$applyTo;
  return dissoc
}();
cljs.core.with_meta = function with_meta(o, meta) {
  return cljs.core._with_meta.call(null, o, meta)
};
cljs.core.meta = function meta(o) {
  if(cljs.core.truth_(function() {
    var x__352__auto____3023 = o;
    if(cljs.core.truth_(function() {
      var and__3546__auto____3024 = x__352__auto____3023;
      if(cljs.core.truth_(and__3546__auto____3024)) {
        var and__3546__auto____3025 = x__352__auto____3023.cljs$core$IMeta$;
        if(cljs.core.truth_(and__3546__auto____3025)) {
          return cljs.core.not.call(null, x__352__auto____3023.hasOwnProperty("cljs$core$IMeta$"))
        }else {
          return and__3546__auto____3025
        }
      }else {
        return and__3546__auto____3024
      }
    }())) {
      return true
    }else {
      return cljs.core.type_satisfies_.call(null, cljs.core.IMeta, x__352__auto____3023)
    }
  }())) {
    return cljs.core._meta.call(null, o)
  }else {
    return null
  }
};
cljs.core.peek = function peek(coll) {
  return cljs.core._peek.call(null, coll)
};
cljs.core.pop = function pop(coll) {
  return cljs.core._pop.call(null, coll)
};
cljs.core.disj = function() {
  var disj = null;
  var disj__3027 = function(coll) {
    return coll
  };
  var disj__3028 = function(coll, k) {
    return cljs.core._disjoin.call(null, coll, k)
  };
  var disj__3029 = function() {
    var G__3031__delegate = function(coll, k, ks) {
      while(true) {
        var ret__3026 = disj.call(null, coll, k);
        if(cljs.core.truth_(ks)) {
          var G__3032 = ret__3026;
          var G__3033 = cljs.core.first.call(null, ks);
          var G__3034 = cljs.core.next.call(null, ks);
          coll = G__3032;
          k = G__3033;
          ks = G__3034;
          continue
        }else {
          return ret__3026
        }
        break
      }
    };
    var G__3031 = function(coll, k, var_args) {
      var ks = null;
      if(goog.isDef(var_args)) {
        ks = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3031__delegate.call(this, coll, k, ks)
    };
    G__3031.cljs$lang$maxFixedArity = 2;
    G__3031.cljs$lang$applyTo = function(arglist__3035) {
      var coll = cljs.core.first(arglist__3035);
      var k = cljs.core.first(cljs.core.next(arglist__3035));
      var ks = cljs.core.rest(cljs.core.next(arglist__3035));
      return G__3031__delegate.call(this, coll, k, ks)
    };
    return G__3031
  }();
  disj = function(coll, k, var_args) {
    var ks = var_args;
    switch(arguments.length) {
      case 1:
        return disj__3027.call(this, coll);
      case 2:
        return disj__3028.call(this, coll, k);
      default:
        return disj__3029.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  disj.cljs$lang$maxFixedArity = 2;
  disj.cljs$lang$applyTo = disj__3029.cljs$lang$applyTo;
  return disj
}();
cljs.core.hash = function hash(o) {
  return cljs.core._hash.call(null, o)
};
cljs.core.empty_QMARK_ = function empty_QMARK_(coll) {
  return cljs.core.not.call(null, cljs.core.seq.call(null, coll))
};
cljs.core.coll_QMARK_ = function coll_QMARK_(x) {
  if(cljs.core.truth_(x === null)) {
    return false
  }else {
    var x__352__auto____3036 = x;
    if(cljs.core.truth_(function() {
      var and__3546__auto____3037 = x__352__auto____3036;
      if(cljs.core.truth_(and__3546__auto____3037)) {
        var and__3546__auto____3038 = x__352__auto____3036.cljs$core$ICollection$;
        if(cljs.core.truth_(and__3546__auto____3038)) {
          return cljs.core.not.call(null, x__352__auto____3036.hasOwnProperty("cljs$core$ICollection$"))
        }else {
          return and__3546__auto____3038
        }
      }else {
        return and__3546__auto____3037
      }
    }())) {
      return true
    }else {
      return cljs.core.type_satisfies_.call(null, cljs.core.ICollection, x__352__auto____3036)
    }
  }
};
cljs.core.set_QMARK_ = function set_QMARK_(x) {
  if(cljs.core.truth_(x === null)) {
    return false
  }else {
    var x__352__auto____3039 = x;
    if(cljs.core.truth_(function() {
      var and__3546__auto____3040 = x__352__auto____3039;
      if(cljs.core.truth_(and__3546__auto____3040)) {
        var and__3546__auto____3041 = x__352__auto____3039.cljs$core$ISet$;
        if(cljs.core.truth_(and__3546__auto____3041)) {
          return cljs.core.not.call(null, x__352__auto____3039.hasOwnProperty("cljs$core$ISet$"))
        }else {
          return and__3546__auto____3041
        }
      }else {
        return and__3546__auto____3040
      }
    }())) {
      return true
    }else {
      return cljs.core.type_satisfies_.call(null, cljs.core.ISet, x__352__auto____3039)
    }
  }
};
cljs.core.associative_QMARK_ = function associative_QMARK_(x) {
  var x__352__auto____3042 = x;
  if(cljs.core.truth_(function() {
    var and__3546__auto____3043 = x__352__auto____3042;
    if(cljs.core.truth_(and__3546__auto____3043)) {
      var and__3546__auto____3044 = x__352__auto____3042.cljs$core$IAssociative$;
      if(cljs.core.truth_(and__3546__auto____3044)) {
        return cljs.core.not.call(null, x__352__auto____3042.hasOwnProperty("cljs$core$IAssociative$"))
      }else {
        return and__3546__auto____3044
      }
    }else {
      return and__3546__auto____3043
    }
  }())) {
    return true
  }else {
    return cljs.core.type_satisfies_.call(null, cljs.core.IAssociative, x__352__auto____3042)
  }
};
cljs.core.sequential_QMARK_ = function sequential_QMARK_(x) {
  var x__352__auto____3045 = x;
  if(cljs.core.truth_(function() {
    var and__3546__auto____3046 = x__352__auto____3045;
    if(cljs.core.truth_(and__3546__auto____3046)) {
      var and__3546__auto____3047 = x__352__auto____3045.cljs$core$ISequential$;
      if(cljs.core.truth_(and__3546__auto____3047)) {
        return cljs.core.not.call(null, x__352__auto____3045.hasOwnProperty("cljs$core$ISequential$"))
      }else {
        return and__3546__auto____3047
      }
    }else {
      return and__3546__auto____3046
    }
  }())) {
    return true
  }else {
    return cljs.core.type_satisfies_.call(null, cljs.core.ISequential, x__352__auto____3045)
  }
};
cljs.core.counted_QMARK_ = function counted_QMARK_(x) {
  var x__352__auto____3048 = x;
  if(cljs.core.truth_(function() {
    var and__3546__auto____3049 = x__352__auto____3048;
    if(cljs.core.truth_(and__3546__auto____3049)) {
      var and__3546__auto____3050 = x__352__auto____3048.cljs$core$ICounted$;
      if(cljs.core.truth_(and__3546__auto____3050)) {
        return cljs.core.not.call(null, x__352__auto____3048.hasOwnProperty("cljs$core$ICounted$"))
      }else {
        return and__3546__auto____3050
      }
    }else {
      return and__3546__auto____3049
    }
  }())) {
    return true
  }else {
    return cljs.core.type_satisfies_.call(null, cljs.core.ICounted, x__352__auto____3048)
  }
};
cljs.core.map_QMARK_ = function map_QMARK_(x) {
  if(cljs.core.truth_(x === null)) {
    return false
  }else {
    var x__352__auto____3051 = x;
    if(cljs.core.truth_(function() {
      var and__3546__auto____3052 = x__352__auto____3051;
      if(cljs.core.truth_(and__3546__auto____3052)) {
        var and__3546__auto____3053 = x__352__auto____3051.cljs$core$IMap$;
        if(cljs.core.truth_(and__3546__auto____3053)) {
          return cljs.core.not.call(null, x__352__auto____3051.hasOwnProperty("cljs$core$IMap$"))
        }else {
          return and__3546__auto____3053
        }
      }else {
        return and__3546__auto____3052
      }
    }())) {
      return true
    }else {
      return cljs.core.type_satisfies_.call(null, cljs.core.IMap, x__352__auto____3051)
    }
  }
};
cljs.core.vector_QMARK_ = function vector_QMARK_(x) {
  var x__352__auto____3054 = x;
  if(cljs.core.truth_(function() {
    var and__3546__auto____3055 = x__352__auto____3054;
    if(cljs.core.truth_(and__3546__auto____3055)) {
      var and__3546__auto____3056 = x__352__auto____3054.cljs$core$IVector$;
      if(cljs.core.truth_(and__3546__auto____3056)) {
        return cljs.core.not.call(null, x__352__auto____3054.hasOwnProperty("cljs$core$IVector$"))
      }else {
        return and__3546__auto____3056
      }
    }else {
      return and__3546__auto____3055
    }
  }())) {
    return true
  }else {
    return cljs.core.type_satisfies_.call(null, cljs.core.IVector, x__352__auto____3054)
  }
};
cljs.core.js_obj = function js_obj() {
  return{}
};
cljs.core.js_keys = function js_keys(obj) {
  var keys__3057 = cljs.core.array.call(null);
  goog.object.forEach.call(null, obj, function(val, key, obj) {
    return keys__3057.push(key)
  });
  return keys__3057
};
cljs.core.js_delete = function js_delete(obj, key) {
  return delete obj[key]
};
cljs.core.lookup_sentinel = cljs.core.js_obj.call(null);
cljs.core.false_QMARK_ = function false_QMARK_(x) {
  return x === false
};
cljs.core.true_QMARK_ = function true_QMARK_(x) {
  return x === true
};
cljs.core.undefined_QMARK_ = function undefined_QMARK_(x) {
  return void 0 === x
};
cljs.core.instance_QMARK_ = function instance_QMARK_(t, o) {
  return o != null && (o instanceof t || o.constructor === t || t === Object)
};
cljs.core.seq_QMARK_ = function seq_QMARK_(s) {
  if(cljs.core.truth_(s === null)) {
    return false
  }else {
    var x__352__auto____3058 = s;
    if(cljs.core.truth_(function() {
      var and__3546__auto____3059 = x__352__auto____3058;
      if(cljs.core.truth_(and__3546__auto____3059)) {
        var and__3546__auto____3060 = x__352__auto____3058.cljs$core$ISeq$;
        if(cljs.core.truth_(and__3546__auto____3060)) {
          return cljs.core.not.call(null, x__352__auto____3058.hasOwnProperty("cljs$core$ISeq$"))
        }else {
          return and__3546__auto____3060
        }
      }else {
        return and__3546__auto____3059
      }
    }())) {
      return true
    }else {
      return cljs.core.type_satisfies_.call(null, cljs.core.ISeq, x__352__auto____3058)
    }
  }
};
cljs.core.boolean$ = function boolean$(x) {
  if(cljs.core.truth_(x)) {
    return true
  }else {
    return false
  }
};
cljs.core.string_QMARK_ = function string_QMARK_(x) {
  var and__3546__auto____3061 = goog.isString.call(null, x);
  if(cljs.core.truth_(and__3546__auto____3061)) {
    return cljs.core.not.call(null, function() {
      var or__3548__auto____3062 = cljs.core._EQ_.call(null, x.charAt(0), "\ufdd0");
      if(cljs.core.truth_(or__3548__auto____3062)) {
        return or__3548__auto____3062
      }else {
        return cljs.core._EQ_.call(null, x.charAt(0), "\ufdd1")
      }
    }())
  }else {
    return and__3546__auto____3061
  }
};
cljs.core.keyword_QMARK_ = function keyword_QMARK_(x) {
  var and__3546__auto____3063 = goog.isString.call(null, x);
  if(cljs.core.truth_(and__3546__auto____3063)) {
    return cljs.core._EQ_.call(null, x.charAt(0), "\ufdd0")
  }else {
    return and__3546__auto____3063
  }
};
cljs.core.symbol_QMARK_ = function symbol_QMARK_(x) {
  var and__3546__auto____3064 = goog.isString.call(null, x);
  if(cljs.core.truth_(and__3546__auto____3064)) {
    return cljs.core._EQ_.call(null, x.charAt(0), "\ufdd1")
  }else {
    return and__3546__auto____3064
  }
};
cljs.core.number_QMARK_ = function number_QMARK_(n) {
  return goog.isNumber.call(null, n)
};
cljs.core.fn_QMARK_ = function fn_QMARK_(f) {
  return goog.isFunction.call(null, f)
};
cljs.core.integer_QMARK_ = function integer_QMARK_(n) {
  var and__3546__auto____3065 = cljs.core.number_QMARK_.call(null, n);
  if(cljs.core.truth_(and__3546__auto____3065)) {
    return n == n.toFixed()
  }else {
    return and__3546__auto____3065
  }
};
cljs.core.contains_QMARK_ = function contains_QMARK_(coll, v) {
  if(cljs.core.truth_(cljs.core._lookup.call(null, coll, v, cljs.core.lookup_sentinel) === cljs.core.lookup_sentinel)) {
    return false
  }else {
    return true
  }
};
cljs.core.find = function find(coll, k) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____3066 = coll;
    if(cljs.core.truth_(and__3546__auto____3066)) {
      var and__3546__auto____3067 = cljs.core.associative_QMARK_.call(null, coll);
      if(cljs.core.truth_(and__3546__auto____3067)) {
        return cljs.core.contains_QMARK_.call(null, coll, k)
      }else {
        return and__3546__auto____3067
      }
    }else {
      return and__3546__auto____3066
    }
  }())) {
    return cljs.core.Vector.fromArray([k, cljs.core._lookup.call(null, coll, k)])
  }else {
    return null
  }
};
cljs.core.distinct_QMARK_ = function() {
  var distinct_QMARK_ = null;
  var distinct_QMARK___3072 = function(x) {
    return true
  };
  var distinct_QMARK___3073 = function(x, y) {
    return cljs.core.not.call(null, cljs.core._EQ_.call(null, x, y))
  };
  var distinct_QMARK___3074 = function() {
    var G__3076__delegate = function(x, y, more) {
      if(cljs.core.truth_(cljs.core.not.call(null, cljs.core._EQ_.call(null, x, y)))) {
        var s__3068 = cljs.core.set([y, x]);
        var xs__3069 = more;
        while(true) {
          var x__3070 = cljs.core.first.call(null, xs__3069);
          var etc__3071 = cljs.core.next.call(null, xs__3069);
          if(cljs.core.truth_(xs__3069)) {
            if(cljs.core.truth_(cljs.core.contains_QMARK_.call(null, s__3068, x__3070))) {
              return false
            }else {
              var G__3077 = cljs.core.conj.call(null, s__3068, x__3070);
              var G__3078 = etc__3071;
              s__3068 = G__3077;
              xs__3069 = G__3078;
              continue
            }
          }else {
            return true
          }
          break
        }
      }else {
        return false
      }
    };
    var G__3076 = function(x, y, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3076__delegate.call(this, x, y, more)
    };
    G__3076.cljs$lang$maxFixedArity = 2;
    G__3076.cljs$lang$applyTo = function(arglist__3079) {
      var x = cljs.core.first(arglist__3079);
      var y = cljs.core.first(cljs.core.next(arglist__3079));
      var more = cljs.core.rest(cljs.core.next(arglist__3079));
      return G__3076__delegate.call(this, x, y, more)
    };
    return G__3076
  }();
  distinct_QMARK_ = function(x, y, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 1:
        return distinct_QMARK___3072.call(this, x);
      case 2:
        return distinct_QMARK___3073.call(this, x, y);
      default:
        return distinct_QMARK___3074.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  distinct_QMARK_.cljs$lang$maxFixedArity = 2;
  distinct_QMARK_.cljs$lang$applyTo = distinct_QMARK___3074.cljs$lang$applyTo;
  return distinct_QMARK_
}();
cljs.core.compare = function compare(x, y) {
  return goog.array.defaultCompare.call(null, x, y)
};
cljs.core.fn__GT_comparator = function fn__GT_comparator(f) {
  if(cljs.core.truth_(cljs.core._EQ_.call(null, f, cljs.core.compare))) {
    return cljs.core.compare
  }else {
    return function(x, y) {
      var r__3080 = f.call(null, x, y);
      if(cljs.core.truth_(cljs.core.number_QMARK_.call(null, r__3080))) {
        return r__3080
      }else {
        if(cljs.core.truth_(r__3080)) {
          return-1
        }else {
          if(cljs.core.truth_(f.call(null, y, x))) {
            return 1
          }else {
            return 0
          }
        }
      }
    }
  }
};
cljs.core.sort = function() {
  var sort = null;
  var sort__3082 = function(coll) {
    return sort.call(null, cljs.core.compare, coll)
  };
  var sort__3083 = function(comp, coll) {
    if(cljs.core.truth_(cljs.core.seq.call(null, coll))) {
      var a__3081 = cljs.core.to_array.call(null, coll);
      goog.array.stableSort.call(null, a__3081, cljs.core.fn__GT_comparator.call(null, comp));
      return cljs.core.seq.call(null, a__3081)
    }else {
      return cljs.core.List.EMPTY
    }
  };
  sort = function(comp, coll) {
    switch(arguments.length) {
      case 1:
        return sort__3082.call(this, comp);
      case 2:
        return sort__3083.call(this, comp, coll)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return sort
}();
cljs.core.sort_by = function() {
  var sort_by = null;
  var sort_by__3085 = function(keyfn, coll) {
    return sort_by.call(null, keyfn, cljs.core.compare, coll)
  };
  var sort_by__3086 = function(keyfn, comp, coll) {
    return cljs.core.sort.call(null, function(x, y) {
      return cljs.core.fn__GT_comparator.call(null, comp).call(null, keyfn.call(null, x), keyfn.call(null, y))
    }, coll)
  };
  sort_by = function(keyfn, comp, coll) {
    switch(arguments.length) {
      case 2:
        return sort_by__3085.call(this, keyfn, comp);
      case 3:
        return sort_by__3086.call(this, keyfn, comp, coll)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return sort_by
}();
cljs.core.reduce = function() {
  var reduce = null;
  var reduce__3088 = function(f, coll) {
    return cljs.core._reduce.call(null, coll, f)
  };
  var reduce__3089 = function(f, val, coll) {
    return cljs.core._reduce.call(null, coll, f, val)
  };
  reduce = function(f, val, coll) {
    switch(arguments.length) {
      case 2:
        return reduce__3088.call(this, f, val);
      case 3:
        return reduce__3089.call(this, f, val, coll)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return reduce
}();
cljs.core.seq_reduce = function() {
  var seq_reduce = null;
  var seq_reduce__3095 = function(f, coll) {
    var temp__3695__auto____3091 = cljs.core.seq.call(null, coll);
    if(cljs.core.truth_(temp__3695__auto____3091)) {
      var s__3092 = temp__3695__auto____3091;
      return cljs.core.reduce.call(null, f, cljs.core.first.call(null, s__3092), cljs.core.next.call(null, s__3092))
    }else {
      return f.call(null)
    }
  };
  var seq_reduce__3096 = function(f, val, coll) {
    var val__3093 = val;
    var coll__3094 = cljs.core.seq.call(null, coll);
    while(true) {
      if(cljs.core.truth_(coll__3094)) {
        var G__3098 = f.call(null, val__3093, cljs.core.first.call(null, coll__3094));
        var G__3099 = cljs.core.next.call(null, coll__3094);
        val__3093 = G__3098;
        coll__3094 = G__3099;
        continue
      }else {
        return val__3093
      }
      break
    }
  };
  seq_reduce = function(f, val, coll) {
    switch(arguments.length) {
      case 2:
        return seq_reduce__3095.call(this, f, val);
      case 3:
        return seq_reduce__3096.call(this, f, val, coll)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return seq_reduce
}();
cljs.core.IReduce["_"] = true;
cljs.core._reduce["_"] = function() {
  var G__3100 = null;
  var G__3100__3101 = function(coll, f) {
    return cljs.core.seq_reduce.call(null, f, coll)
  };
  var G__3100__3102 = function(coll, f, start) {
    return cljs.core.seq_reduce.call(null, f, start, coll)
  };
  G__3100 = function(coll, f, start) {
    switch(arguments.length) {
      case 2:
        return G__3100__3101.call(this, coll, f);
      case 3:
        return G__3100__3102.call(this, coll, f, start)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3100
}();
cljs.core._PLUS_ = function() {
  var _PLUS_ = null;
  var _PLUS___3104 = function() {
    return 0
  };
  var _PLUS___3105 = function(x) {
    return x
  };
  var _PLUS___3106 = function(x, y) {
    return x + y
  };
  var _PLUS___3107 = function() {
    var G__3109__delegate = function(x, y, more) {
      return cljs.core.reduce.call(null, _PLUS_, x + y, more)
    };
    var G__3109 = function(x, y, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3109__delegate.call(this, x, y, more)
    };
    G__3109.cljs$lang$maxFixedArity = 2;
    G__3109.cljs$lang$applyTo = function(arglist__3110) {
      var x = cljs.core.first(arglist__3110);
      var y = cljs.core.first(cljs.core.next(arglist__3110));
      var more = cljs.core.rest(cljs.core.next(arglist__3110));
      return G__3109__delegate.call(this, x, y, more)
    };
    return G__3109
  }();
  _PLUS_ = function(x, y, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 0:
        return _PLUS___3104.call(this);
      case 1:
        return _PLUS___3105.call(this, x);
      case 2:
        return _PLUS___3106.call(this, x, y);
      default:
        return _PLUS___3107.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  _PLUS_.cljs$lang$maxFixedArity = 2;
  _PLUS_.cljs$lang$applyTo = _PLUS___3107.cljs$lang$applyTo;
  return _PLUS_
}();
cljs.core._ = function() {
  var _ = null;
  var ___3111 = function(x) {
    return-x
  };
  var ___3112 = function(x, y) {
    return x - y
  };
  var ___3113 = function() {
    var G__3115__delegate = function(x, y, more) {
      return cljs.core.reduce.call(null, _, x - y, more)
    };
    var G__3115 = function(x, y, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3115__delegate.call(this, x, y, more)
    };
    G__3115.cljs$lang$maxFixedArity = 2;
    G__3115.cljs$lang$applyTo = function(arglist__3116) {
      var x = cljs.core.first(arglist__3116);
      var y = cljs.core.first(cljs.core.next(arglist__3116));
      var more = cljs.core.rest(cljs.core.next(arglist__3116));
      return G__3115__delegate.call(this, x, y, more)
    };
    return G__3115
  }();
  _ = function(x, y, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 1:
        return ___3111.call(this, x);
      case 2:
        return ___3112.call(this, x, y);
      default:
        return ___3113.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  _.cljs$lang$maxFixedArity = 2;
  _.cljs$lang$applyTo = ___3113.cljs$lang$applyTo;
  return _
}();
cljs.core._STAR_ = function() {
  var _STAR_ = null;
  var _STAR___3117 = function() {
    return 1
  };
  var _STAR___3118 = function(x) {
    return x
  };
  var _STAR___3119 = function(x, y) {
    return x * y
  };
  var _STAR___3120 = function() {
    var G__3122__delegate = function(x, y, more) {
      return cljs.core.reduce.call(null, _STAR_, x * y, more)
    };
    var G__3122 = function(x, y, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3122__delegate.call(this, x, y, more)
    };
    G__3122.cljs$lang$maxFixedArity = 2;
    G__3122.cljs$lang$applyTo = function(arglist__3123) {
      var x = cljs.core.first(arglist__3123);
      var y = cljs.core.first(cljs.core.next(arglist__3123));
      var more = cljs.core.rest(cljs.core.next(arglist__3123));
      return G__3122__delegate.call(this, x, y, more)
    };
    return G__3122
  }();
  _STAR_ = function(x, y, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 0:
        return _STAR___3117.call(this);
      case 1:
        return _STAR___3118.call(this, x);
      case 2:
        return _STAR___3119.call(this, x, y);
      default:
        return _STAR___3120.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  _STAR_.cljs$lang$maxFixedArity = 2;
  _STAR_.cljs$lang$applyTo = _STAR___3120.cljs$lang$applyTo;
  return _STAR_
}();
cljs.core._SLASH_ = function() {
  var _SLASH_ = null;
  var _SLASH___3124 = function(x) {
    return _SLASH_.call(null, 1, x)
  };
  var _SLASH___3125 = function(x, y) {
    return x / y
  };
  var _SLASH___3126 = function() {
    var G__3128__delegate = function(x, y, more) {
      return cljs.core.reduce.call(null, _SLASH_, _SLASH_.call(null, x, y), more)
    };
    var G__3128 = function(x, y, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3128__delegate.call(this, x, y, more)
    };
    G__3128.cljs$lang$maxFixedArity = 2;
    G__3128.cljs$lang$applyTo = function(arglist__3129) {
      var x = cljs.core.first(arglist__3129);
      var y = cljs.core.first(cljs.core.next(arglist__3129));
      var more = cljs.core.rest(cljs.core.next(arglist__3129));
      return G__3128__delegate.call(this, x, y, more)
    };
    return G__3128
  }();
  _SLASH_ = function(x, y, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 1:
        return _SLASH___3124.call(this, x);
      case 2:
        return _SLASH___3125.call(this, x, y);
      default:
        return _SLASH___3126.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  _SLASH_.cljs$lang$maxFixedArity = 2;
  _SLASH_.cljs$lang$applyTo = _SLASH___3126.cljs$lang$applyTo;
  return _SLASH_
}();
cljs.core._LT_ = function() {
  var _LT_ = null;
  var _LT___3130 = function(x) {
    return true
  };
  var _LT___3131 = function(x, y) {
    return x < y
  };
  var _LT___3132 = function() {
    var G__3134__delegate = function(x, y, more) {
      while(true) {
        if(cljs.core.truth_(x < y)) {
          if(cljs.core.truth_(cljs.core.next.call(null, more))) {
            var G__3135 = y;
            var G__3136 = cljs.core.first.call(null, more);
            var G__3137 = cljs.core.next.call(null, more);
            x = G__3135;
            y = G__3136;
            more = G__3137;
            continue
          }else {
            return y < cljs.core.first.call(null, more)
          }
        }else {
          return false
        }
        break
      }
    };
    var G__3134 = function(x, y, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3134__delegate.call(this, x, y, more)
    };
    G__3134.cljs$lang$maxFixedArity = 2;
    G__3134.cljs$lang$applyTo = function(arglist__3138) {
      var x = cljs.core.first(arglist__3138);
      var y = cljs.core.first(cljs.core.next(arglist__3138));
      var more = cljs.core.rest(cljs.core.next(arglist__3138));
      return G__3134__delegate.call(this, x, y, more)
    };
    return G__3134
  }();
  _LT_ = function(x, y, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 1:
        return _LT___3130.call(this, x);
      case 2:
        return _LT___3131.call(this, x, y);
      default:
        return _LT___3132.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  _LT_.cljs$lang$maxFixedArity = 2;
  _LT_.cljs$lang$applyTo = _LT___3132.cljs$lang$applyTo;
  return _LT_
}();
cljs.core._LT__EQ_ = function() {
  var _LT__EQ_ = null;
  var _LT__EQ___3139 = function(x) {
    return true
  };
  var _LT__EQ___3140 = function(x, y) {
    return x <= y
  };
  var _LT__EQ___3141 = function() {
    var G__3143__delegate = function(x, y, more) {
      while(true) {
        if(cljs.core.truth_(x <= y)) {
          if(cljs.core.truth_(cljs.core.next.call(null, more))) {
            var G__3144 = y;
            var G__3145 = cljs.core.first.call(null, more);
            var G__3146 = cljs.core.next.call(null, more);
            x = G__3144;
            y = G__3145;
            more = G__3146;
            continue
          }else {
            return y <= cljs.core.first.call(null, more)
          }
        }else {
          return false
        }
        break
      }
    };
    var G__3143 = function(x, y, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3143__delegate.call(this, x, y, more)
    };
    G__3143.cljs$lang$maxFixedArity = 2;
    G__3143.cljs$lang$applyTo = function(arglist__3147) {
      var x = cljs.core.first(arglist__3147);
      var y = cljs.core.first(cljs.core.next(arglist__3147));
      var more = cljs.core.rest(cljs.core.next(arglist__3147));
      return G__3143__delegate.call(this, x, y, more)
    };
    return G__3143
  }();
  _LT__EQ_ = function(x, y, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 1:
        return _LT__EQ___3139.call(this, x);
      case 2:
        return _LT__EQ___3140.call(this, x, y);
      default:
        return _LT__EQ___3141.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  _LT__EQ_.cljs$lang$maxFixedArity = 2;
  _LT__EQ_.cljs$lang$applyTo = _LT__EQ___3141.cljs$lang$applyTo;
  return _LT__EQ_
}();
cljs.core._GT_ = function() {
  var _GT_ = null;
  var _GT___3148 = function(x) {
    return true
  };
  var _GT___3149 = function(x, y) {
    return x > y
  };
  var _GT___3150 = function() {
    var G__3152__delegate = function(x, y, more) {
      while(true) {
        if(cljs.core.truth_(x > y)) {
          if(cljs.core.truth_(cljs.core.next.call(null, more))) {
            var G__3153 = y;
            var G__3154 = cljs.core.first.call(null, more);
            var G__3155 = cljs.core.next.call(null, more);
            x = G__3153;
            y = G__3154;
            more = G__3155;
            continue
          }else {
            return y > cljs.core.first.call(null, more)
          }
        }else {
          return false
        }
        break
      }
    };
    var G__3152 = function(x, y, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3152__delegate.call(this, x, y, more)
    };
    G__3152.cljs$lang$maxFixedArity = 2;
    G__3152.cljs$lang$applyTo = function(arglist__3156) {
      var x = cljs.core.first(arglist__3156);
      var y = cljs.core.first(cljs.core.next(arglist__3156));
      var more = cljs.core.rest(cljs.core.next(arglist__3156));
      return G__3152__delegate.call(this, x, y, more)
    };
    return G__3152
  }();
  _GT_ = function(x, y, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 1:
        return _GT___3148.call(this, x);
      case 2:
        return _GT___3149.call(this, x, y);
      default:
        return _GT___3150.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  _GT_.cljs$lang$maxFixedArity = 2;
  _GT_.cljs$lang$applyTo = _GT___3150.cljs$lang$applyTo;
  return _GT_
}();
cljs.core._GT__EQ_ = function() {
  var _GT__EQ_ = null;
  var _GT__EQ___3157 = function(x) {
    return true
  };
  var _GT__EQ___3158 = function(x, y) {
    return x >= y
  };
  var _GT__EQ___3159 = function() {
    var G__3161__delegate = function(x, y, more) {
      while(true) {
        if(cljs.core.truth_(x >= y)) {
          if(cljs.core.truth_(cljs.core.next.call(null, more))) {
            var G__3162 = y;
            var G__3163 = cljs.core.first.call(null, more);
            var G__3164 = cljs.core.next.call(null, more);
            x = G__3162;
            y = G__3163;
            more = G__3164;
            continue
          }else {
            return y >= cljs.core.first.call(null, more)
          }
        }else {
          return false
        }
        break
      }
    };
    var G__3161 = function(x, y, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3161__delegate.call(this, x, y, more)
    };
    G__3161.cljs$lang$maxFixedArity = 2;
    G__3161.cljs$lang$applyTo = function(arglist__3165) {
      var x = cljs.core.first(arglist__3165);
      var y = cljs.core.first(cljs.core.next(arglist__3165));
      var more = cljs.core.rest(cljs.core.next(arglist__3165));
      return G__3161__delegate.call(this, x, y, more)
    };
    return G__3161
  }();
  _GT__EQ_ = function(x, y, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 1:
        return _GT__EQ___3157.call(this, x);
      case 2:
        return _GT__EQ___3158.call(this, x, y);
      default:
        return _GT__EQ___3159.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  _GT__EQ_.cljs$lang$maxFixedArity = 2;
  _GT__EQ_.cljs$lang$applyTo = _GT__EQ___3159.cljs$lang$applyTo;
  return _GT__EQ_
}();
cljs.core.dec = function dec(x) {
  return x - 1
};
cljs.core.max = function() {
  var max = null;
  var max__3166 = function(x) {
    return x
  };
  var max__3167 = function(x, y) {
    return x > y ? x : y
  };
  var max__3168 = function() {
    var G__3170__delegate = function(x, y, more) {
      return cljs.core.reduce.call(null, max, x > y ? x : y, more)
    };
    var G__3170 = function(x, y, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3170__delegate.call(this, x, y, more)
    };
    G__3170.cljs$lang$maxFixedArity = 2;
    G__3170.cljs$lang$applyTo = function(arglist__3171) {
      var x = cljs.core.first(arglist__3171);
      var y = cljs.core.first(cljs.core.next(arglist__3171));
      var more = cljs.core.rest(cljs.core.next(arglist__3171));
      return G__3170__delegate.call(this, x, y, more)
    };
    return G__3170
  }();
  max = function(x, y, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 1:
        return max__3166.call(this, x);
      case 2:
        return max__3167.call(this, x, y);
      default:
        return max__3168.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  max.cljs$lang$maxFixedArity = 2;
  max.cljs$lang$applyTo = max__3168.cljs$lang$applyTo;
  return max
}();
cljs.core.min = function() {
  var min = null;
  var min__3172 = function(x) {
    return x
  };
  var min__3173 = function(x, y) {
    return x < y ? x : y
  };
  var min__3174 = function() {
    var G__3176__delegate = function(x, y, more) {
      return cljs.core.reduce.call(null, min, x < y ? x : y, more)
    };
    var G__3176 = function(x, y, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3176__delegate.call(this, x, y, more)
    };
    G__3176.cljs$lang$maxFixedArity = 2;
    G__3176.cljs$lang$applyTo = function(arglist__3177) {
      var x = cljs.core.first(arglist__3177);
      var y = cljs.core.first(cljs.core.next(arglist__3177));
      var more = cljs.core.rest(cljs.core.next(arglist__3177));
      return G__3176__delegate.call(this, x, y, more)
    };
    return G__3176
  }();
  min = function(x, y, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 1:
        return min__3172.call(this, x);
      case 2:
        return min__3173.call(this, x, y);
      default:
        return min__3174.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  min.cljs$lang$maxFixedArity = 2;
  min.cljs$lang$applyTo = min__3174.cljs$lang$applyTo;
  return min
}();
cljs.core.fix = function fix(q) {
  if(cljs.core.truth_(q >= 0)) {
    return Math.floor.call(null, q)
  }else {
    return Math.ceil.call(null, q)
  }
};
cljs.core.mod = function mod(n, d) {
  return n % d
};
cljs.core.quot = function quot(n, d) {
  var rem__3178 = n % d;
  return cljs.core.fix.call(null, (n - rem__3178) / d)
};
cljs.core.rem = function rem(n, d) {
  var q__3179 = cljs.core.quot.call(null, n, d);
  return n - d * q__3179
};
cljs.core.rand = function() {
  var rand = null;
  var rand__3180 = function() {
    return Math.random.call(null)
  };
  var rand__3181 = function(n) {
    return n * rand.call(null)
  };
  rand = function(n) {
    switch(arguments.length) {
      case 0:
        return rand__3180.call(this);
      case 1:
        return rand__3181.call(this, n)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return rand
}();
cljs.core.rand_int = function rand_int(n) {
  return cljs.core.fix.call(null, cljs.core.rand.call(null, n))
};
cljs.core.bit_xor = function bit_xor(x, y) {
  return x ^ y
};
cljs.core.bit_and = function bit_and(x, y) {
  return x & y
};
cljs.core.bit_or = function bit_or(x, y) {
  return x | y
};
cljs.core.bit_and_not = function bit_and_not(x, y) {
  return x & ~y
};
cljs.core.bit_clear = function bit_clear(x, n) {
  return x & ~(1 << n)
};
cljs.core.bit_flip = function bit_flip(x, n) {
  return x ^ 1 << n
};
cljs.core.bit_not = function bit_not(x) {
  return~x
};
cljs.core.bit_set = function bit_set(x, n) {
  return x | 1 << n
};
cljs.core.bit_test = function bit_test(x, n) {
  return(x & 1 << n) != 0
};
cljs.core.bit_shift_left = function bit_shift_left(x, n) {
  return x << n
};
cljs.core.bit_shift_right = function bit_shift_right(x, n) {
  return x >> n
};
cljs.core._EQ__EQ_ = function() {
  var _EQ__EQ_ = null;
  var _EQ__EQ___3183 = function(x) {
    return true
  };
  var _EQ__EQ___3184 = function(x, y) {
    return cljs.core._equiv.call(null, x, y)
  };
  var _EQ__EQ___3185 = function() {
    var G__3187__delegate = function(x, y, more) {
      while(true) {
        if(cljs.core.truth_(_EQ__EQ_.call(null, x, y))) {
          if(cljs.core.truth_(cljs.core.next.call(null, more))) {
            var G__3188 = y;
            var G__3189 = cljs.core.first.call(null, more);
            var G__3190 = cljs.core.next.call(null, more);
            x = G__3188;
            y = G__3189;
            more = G__3190;
            continue
          }else {
            return _EQ__EQ_.call(null, y, cljs.core.first.call(null, more))
          }
        }else {
          return false
        }
        break
      }
    };
    var G__3187 = function(x, y, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3187__delegate.call(this, x, y, more)
    };
    G__3187.cljs$lang$maxFixedArity = 2;
    G__3187.cljs$lang$applyTo = function(arglist__3191) {
      var x = cljs.core.first(arglist__3191);
      var y = cljs.core.first(cljs.core.next(arglist__3191));
      var more = cljs.core.rest(cljs.core.next(arglist__3191));
      return G__3187__delegate.call(this, x, y, more)
    };
    return G__3187
  }();
  _EQ__EQ_ = function(x, y, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 1:
        return _EQ__EQ___3183.call(this, x);
      case 2:
        return _EQ__EQ___3184.call(this, x, y);
      default:
        return _EQ__EQ___3185.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  _EQ__EQ_.cljs$lang$maxFixedArity = 2;
  _EQ__EQ_.cljs$lang$applyTo = _EQ__EQ___3185.cljs$lang$applyTo;
  return _EQ__EQ_
}();
cljs.core.pos_QMARK_ = function pos_QMARK_(n) {
  return n > 0
};
cljs.core.zero_QMARK_ = function zero_QMARK_(n) {
  return n === 0
};
cljs.core.neg_QMARK_ = function neg_QMARK_(x) {
  return x < 0
};
cljs.core.nthnext = function nthnext(coll, n) {
  var n__3192 = n;
  var xs__3193 = cljs.core.seq.call(null, coll);
  while(true) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____3194 = xs__3193;
      if(cljs.core.truth_(and__3546__auto____3194)) {
        return n__3192 > 0
      }else {
        return and__3546__auto____3194
      }
    }())) {
      var G__3195 = n__3192 - 1;
      var G__3196 = cljs.core.next.call(null, xs__3193);
      n__3192 = G__3195;
      xs__3193 = G__3196;
      continue
    }else {
      return xs__3193
    }
    break
  }
};
cljs.core.IIndexed["_"] = true;
cljs.core._nth["_"] = function() {
  var G__3201 = null;
  var G__3201__3202 = function(coll, n) {
    var temp__3695__auto____3197 = cljs.core.nthnext.call(null, coll, n);
    if(cljs.core.truth_(temp__3695__auto____3197)) {
      var xs__3198 = temp__3695__auto____3197;
      return cljs.core.first.call(null, xs__3198)
    }else {
      throw new Error("Index out of bounds");
    }
  };
  var G__3201__3203 = function(coll, n, not_found) {
    var temp__3695__auto____3199 = cljs.core.nthnext.call(null, coll, n);
    if(cljs.core.truth_(temp__3695__auto____3199)) {
      var xs__3200 = temp__3695__auto____3199;
      return cljs.core.first.call(null, xs__3200)
    }else {
      return not_found
    }
  };
  G__3201 = function(coll, n, not_found) {
    switch(arguments.length) {
      case 2:
        return G__3201__3202.call(this, coll, n);
      case 3:
        return G__3201__3203.call(this, coll, n, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3201
}();
cljs.core.str_STAR_ = function() {
  var str_STAR_ = null;
  var str_STAR___3205 = function() {
    return""
  };
  var str_STAR___3206 = function(x) {
    if(cljs.core.truth_(x === null)) {
      return""
    }else {
      if(cljs.core.truth_("\ufdd0'else")) {
        return x.toString()
      }else {
        return null
      }
    }
  };
  var str_STAR___3207 = function() {
    var G__3209__delegate = function(x, ys) {
      return function(sb, more) {
        while(true) {
          if(cljs.core.truth_(more)) {
            var G__3210 = sb.append(str_STAR_.call(null, cljs.core.first.call(null, more)));
            var G__3211 = cljs.core.next.call(null, more);
            sb = G__3210;
            more = G__3211;
            continue
          }else {
            return str_STAR_.call(null, sb)
          }
          break
        }
      }.call(null, new goog.string.StringBuffer(str_STAR_.call(null, x)), ys)
    };
    var G__3209 = function(x, var_args) {
      var ys = null;
      if(goog.isDef(var_args)) {
        ys = cljs.core.array_seq(Array.prototype.slice.call(arguments, 1), 0)
      }
      return G__3209__delegate.call(this, x, ys)
    };
    G__3209.cljs$lang$maxFixedArity = 1;
    G__3209.cljs$lang$applyTo = function(arglist__3212) {
      var x = cljs.core.first(arglist__3212);
      var ys = cljs.core.rest(arglist__3212);
      return G__3209__delegate.call(this, x, ys)
    };
    return G__3209
  }();
  str_STAR_ = function(x, var_args) {
    var ys = var_args;
    switch(arguments.length) {
      case 0:
        return str_STAR___3205.call(this);
      case 1:
        return str_STAR___3206.call(this, x);
      default:
        return str_STAR___3207.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  str_STAR_.cljs$lang$maxFixedArity = 1;
  str_STAR_.cljs$lang$applyTo = str_STAR___3207.cljs$lang$applyTo;
  return str_STAR_
}();
cljs.core.str = function() {
  var str = null;
  var str__3213 = function() {
    return""
  };
  var str__3214 = function(x) {
    if(cljs.core.truth_(cljs.core.symbol_QMARK_.call(null, x))) {
      return x.substring(2, x.length)
    }else {
      if(cljs.core.truth_(cljs.core.keyword_QMARK_.call(null, x))) {
        return cljs.core.str_STAR_.call(null, ":", x.substring(2, x.length))
      }else {
        if(cljs.core.truth_(x === null)) {
          return""
        }else {
          if(cljs.core.truth_("\ufdd0'else")) {
            return x.toString()
          }else {
            return null
          }
        }
      }
    }
  };
  var str__3215 = function() {
    var G__3217__delegate = function(x, ys) {
      return cljs.core.apply.call(null, cljs.core.str_STAR_, x, ys)
    };
    var G__3217 = function(x, var_args) {
      var ys = null;
      if(goog.isDef(var_args)) {
        ys = cljs.core.array_seq(Array.prototype.slice.call(arguments, 1), 0)
      }
      return G__3217__delegate.call(this, x, ys)
    };
    G__3217.cljs$lang$maxFixedArity = 1;
    G__3217.cljs$lang$applyTo = function(arglist__3218) {
      var x = cljs.core.first(arglist__3218);
      var ys = cljs.core.rest(arglist__3218);
      return G__3217__delegate.call(this, x, ys)
    };
    return G__3217
  }();
  str = function(x, var_args) {
    var ys = var_args;
    switch(arguments.length) {
      case 0:
        return str__3213.call(this);
      case 1:
        return str__3214.call(this, x);
      default:
        return str__3215.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  str.cljs$lang$maxFixedArity = 1;
  str.cljs$lang$applyTo = str__3215.cljs$lang$applyTo;
  return str
}();
cljs.core.subs = function() {
  var subs = null;
  var subs__3219 = function(s, start) {
    return s.substring(start)
  };
  var subs__3220 = function(s, start, end) {
    return s.substring(start, end)
  };
  subs = function(s, start, end) {
    switch(arguments.length) {
      case 2:
        return subs__3219.call(this, s, start);
      case 3:
        return subs__3220.call(this, s, start, end)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return subs
}();
cljs.core.symbol = function() {
  var symbol = null;
  var symbol__3222 = function(name) {
    if(cljs.core.truth_(cljs.core.symbol_QMARK_.call(null, name))) {
      name
    }else {
      if(cljs.core.truth_(cljs.core.keyword_QMARK_.call(null, name))) {
        cljs.core.str_STAR_.call(null, "\ufdd1", "'", cljs.core.subs.call(null, name, 2))
      }else {
      }
    }
    return cljs.core.str_STAR_.call(null, "\ufdd1", "'", name)
  };
  var symbol__3223 = function(ns, name) {
    return symbol.call(null, cljs.core.str_STAR_.call(null, ns, "/", name))
  };
  symbol = function(ns, name) {
    switch(arguments.length) {
      case 1:
        return symbol__3222.call(this, ns);
      case 2:
        return symbol__3223.call(this, ns, name)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return symbol
}();
cljs.core.keyword = function() {
  var keyword = null;
  var keyword__3225 = function(name) {
    if(cljs.core.truth_(cljs.core.keyword_QMARK_.call(null, name))) {
      return name
    }else {
      if(cljs.core.truth_(cljs.core.symbol_QMARK_.call(null, name))) {
        return cljs.core.str_STAR_.call(null, "\ufdd0", "'", cljs.core.subs.call(null, name, 2))
      }else {
        if(cljs.core.truth_("\ufdd0'else")) {
          return cljs.core.str_STAR_.call(null, "\ufdd0", "'", name)
        }else {
          return null
        }
      }
    }
  };
  var keyword__3226 = function(ns, name) {
    return keyword.call(null, cljs.core.str_STAR_.call(null, ns, "/", name))
  };
  keyword = function(ns, name) {
    switch(arguments.length) {
      case 1:
        return keyword__3225.call(this, ns);
      case 2:
        return keyword__3226.call(this, ns, name)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return keyword
}();
cljs.core.equiv_sequential = function equiv_sequential(x, y) {
  return cljs.core.boolean$.call(null, cljs.core.truth_(cljs.core.sequential_QMARK_.call(null, y)) ? function() {
    var xs__3228 = cljs.core.seq.call(null, x);
    var ys__3229 = cljs.core.seq.call(null, y);
    while(true) {
      if(cljs.core.truth_(xs__3228 === null)) {
        return ys__3229 === null
      }else {
        if(cljs.core.truth_(ys__3229 === null)) {
          return false
        }else {
          if(cljs.core.truth_(cljs.core._EQ_.call(null, cljs.core.first.call(null, xs__3228), cljs.core.first.call(null, ys__3229)))) {
            var G__3230 = cljs.core.next.call(null, xs__3228);
            var G__3231 = cljs.core.next.call(null, ys__3229);
            xs__3228 = G__3230;
            ys__3229 = G__3231;
            continue
          }else {
            if(cljs.core.truth_("\ufdd0'else")) {
              return false
            }else {
              return null
            }
          }
        }
      }
      break
    }
  }() : null)
};
cljs.core.hash_combine = function hash_combine(seed, hash) {
  return seed ^ hash + 2654435769 + (seed << 6) + (seed >> 2)
};
cljs.core.hash_coll = function hash_coll(coll) {
  return cljs.core.reduce.call(null, function(p1__3232_SHARP_, p2__3233_SHARP_) {
    return cljs.core.hash_combine.call(null, p1__3232_SHARP_, cljs.core.hash.call(null, p2__3233_SHARP_))
  }, cljs.core.hash.call(null, cljs.core.first.call(null, coll)), cljs.core.next.call(null, coll))
};
cljs.core.extend_object_BANG_ = function extend_object_BANG_(obj, fn_map) {
  var G__3234__3235 = cljs.core.seq.call(null, fn_map);
  if(cljs.core.truth_(G__3234__3235)) {
    var G__3237__3239 = cljs.core.first.call(null, G__3234__3235);
    var vec__3238__3240 = G__3237__3239;
    var key_name__3241 = cljs.core.nth.call(null, vec__3238__3240, 0, null);
    var f__3242 = cljs.core.nth.call(null, vec__3238__3240, 1, null);
    var G__3234__3243 = G__3234__3235;
    var G__3237__3244 = G__3237__3239;
    var G__3234__3245 = G__3234__3243;
    while(true) {
      var vec__3246__3247 = G__3237__3244;
      var key_name__3248 = cljs.core.nth.call(null, vec__3246__3247, 0, null);
      var f__3249 = cljs.core.nth.call(null, vec__3246__3247, 1, null);
      var G__3234__3250 = G__3234__3245;
      var str_name__3251 = cljs.core.name.call(null, key_name__3248);
      obj[str_name__3251] = f__3249;
      var temp__3698__auto____3252 = cljs.core.next.call(null, G__3234__3250);
      if(cljs.core.truth_(temp__3698__auto____3252)) {
        var G__3234__3253 = temp__3698__auto____3252;
        var G__3254 = cljs.core.first.call(null, G__3234__3253);
        var G__3255 = G__3234__3253;
        G__3237__3244 = G__3254;
        G__3234__3245 = G__3255;
        continue
      }else {
      }
      break
    }
  }else {
  }
  return obj
};
cljs.core.List = function(meta, first, rest, count) {
  this.meta = meta;
  this.first = first;
  this.rest = rest;
  this.count = count
};
cljs.core.List.cljs$core$IPrintable$_pr_seq = function(this__267__auto__) {
  return cljs.core.list.call(null, "cljs.core.List")
};
cljs.core.List.prototype.cljs$core$IHash$ = true;
cljs.core.List.prototype.cljs$core$IHash$_hash = function(coll) {
  var this__3256 = this;
  return cljs.core.hash_coll.call(null, coll)
};
cljs.core.List.prototype.cljs$core$ISequential$ = true;
cljs.core.List.prototype.cljs$core$ICollection$ = true;
cljs.core.List.prototype.cljs$core$ICollection$_conj = function(coll, o) {
  var this__3257 = this;
  return new cljs.core.List(this__3257.meta, o, coll, this__3257.count + 1)
};
cljs.core.List.prototype.cljs$core$ISeqable$ = true;
cljs.core.List.prototype.cljs$core$ISeqable$_seq = function(coll) {
  var this__3258 = this;
  return coll
};
cljs.core.List.prototype.cljs$core$ICounted$ = true;
cljs.core.List.prototype.cljs$core$ICounted$_count = function(coll) {
  var this__3259 = this;
  return this__3259.count
};
cljs.core.List.prototype.cljs$core$IStack$ = true;
cljs.core.List.prototype.cljs$core$IStack$_peek = function(coll) {
  var this__3260 = this;
  return this__3260.first
};
cljs.core.List.prototype.cljs$core$IStack$_pop = function(coll) {
  var this__3261 = this;
  return cljs.core._rest.call(null, coll)
};
cljs.core.List.prototype.cljs$core$ISeq$ = true;
cljs.core.List.prototype.cljs$core$ISeq$_first = function(coll) {
  var this__3262 = this;
  return this__3262.first
};
cljs.core.List.prototype.cljs$core$ISeq$_rest = function(coll) {
  var this__3263 = this;
  return this__3263.rest
};
cljs.core.List.prototype.cljs$core$IEquiv$ = true;
cljs.core.List.prototype.cljs$core$IEquiv$_equiv = function(coll, other) {
  var this__3264 = this;
  return cljs.core.equiv_sequential.call(null, coll, other)
};
cljs.core.List.prototype.cljs$core$IWithMeta$ = true;
cljs.core.List.prototype.cljs$core$IWithMeta$_with_meta = function(coll, meta) {
  var this__3265 = this;
  return new cljs.core.List(meta, this__3265.first, this__3265.rest, this__3265.count)
};
cljs.core.List.prototype.cljs$core$IMeta$ = true;
cljs.core.List.prototype.cljs$core$IMeta$_meta = function(coll) {
  var this__3266 = this;
  return this__3266.meta
};
cljs.core.List.prototype.cljs$core$IEmptyableCollection$ = true;
cljs.core.List.prototype.cljs$core$IEmptyableCollection$_empty = function(coll) {
  var this__3267 = this;
  return cljs.core.List.EMPTY
};
cljs.core.List;
cljs.core.EmptyList = function(meta) {
  this.meta = meta
};
cljs.core.EmptyList.cljs$core$IPrintable$_pr_seq = function(this__267__auto__) {
  return cljs.core.list.call(null, "cljs.core.EmptyList")
};
cljs.core.EmptyList.prototype.cljs$core$IHash$ = true;
cljs.core.EmptyList.prototype.cljs$core$IHash$_hash = function(coll) {
  var this__3268 = this;
  return cljs.core.hash_coll.call(null, coll)
};
cljs.core.EmptyList.prototype.cljs$core$ISequential$ = true;
cljs.core.EmptyList.prototype.cljs$core$ICollection$ = true;
cljs.core.EmptyList.prototype.cljs$core$ICollection$_conj = function(coll, o) {
  var this__3269 = this;
  return new cljs.core.List(this__3269.meta, o, null, 1)
};
cljs.core.EmptyList.prototype.cljs$core$ISeqable$ = true;
cljs.core.EmptyList.prototype.cljs$core$ISeqable$_seq = function(coll) {
  var this__3270 = this;
  return null
};
cljs.core.EmptyList.prototype.cljs$core$ICounted$ = true;
cljs.core.EmptyList.prototype.cljs$core$ICounted$_count = function(coll) {
  var this__3271 = this;
  return 0
};
cljs.core.EmptyList.prototype.cljs$core$IStack$ = true;
cljs.core.EmptyList.prototype.cljs$core$IStack$_peek = function(coll) {
  var this__3272 = this;
  return null
};
cljs.core.EmptyList.prototype.cljs$core$IStack$_pop = function(coll) {
  var this__3273 = this;
  return null
};
cljs.core.EmptyList.prototype.cljs$core$ISeq$ = true;
cljs.core.EmptyList.prototype.cljs$core$ISeq$_first = function(coll) {
  var this__3274 = this;
  return null
};
cljs.core.EmptyList.prototype.cljs$core$ISeq$_rest = function(coll) {
  var this__3275 = this;
  return null
};
cljs.core.EmptyList.prototype.cljs$core$IEquiv$ = true;
cljs.core.EmptyList.prototype.cljs$core$IEquiv$_equiv = function(coll, other) {
  var this__3276 = this;
  return cljs.core.equiv_sequential.call(null, coll, other)
};
cljs.core.EmptyList.prototype.cljs$core$IWithMeta$ = true;
cljs.core.EmptyList.prototype.cljs$core$IWithMeta$_with_meta = function(coll, meta) {
  var this__3277 = this;
  return new cljs.core.EmptyList(meta)
};
cljs.core.EmptyList.prototype.cljs$core$IMeta$ = true;
cljs.core.EmptyList.prototype.cljs$core$IMeta$_meta = function(coll) {
  var this__3278 = this;
  return this__3278.meta
};
cljs.core.EmptyList.prototype.cljs$core$IEmptyableCollection$ = true;
cljs.core.EmptyList.prototype.cljs$core$IEmptyableCollection$_empty = function(coll) {
  var this__3279 = this;
  return coll
};
cljs.core.EmptyList;
cljs.core.List.EMPTY = new cljs.core.EmptyList(null);
cljs.core.reverse = function reverse(coll) {
  return cljs.core.reduce.call(null, cljs.core.conj, cljs.core.List.EMPTY, coll)
};
cljs.core.list = function() {
  var list__delegate = function(items) {
    return cljs.core.reduce.call(null, cljs.core.conj, cljs.core.List.EMPTY, cljs.core.reverse.call(null, items))
  };
  var list = function(var_args) {
    var items = null;
    if(goog.isDef(var_args)) {
      items = cljs.core.array_seq(Array.prototype.slice.call(arguments, 0), 0)
    }
    return list__delegate.call(this, items)
  };
  list.cljs$lang$maxFixedArity = 0;
  list.cljs$lang$applyTo = function(arglist__3280) {
    var items = cljs.core.seq(arglist__3280);
    return list__delegate.call(this, items)
  };
  return list
}();
cljs.core.Cons = function(meta, first, rest) {
  this.meta = meta;
  this.first = first;
  this.rest = rest
};
cljs.core.Cons.cljs$core$IPrintable$_pr_seq = function(this__267__auto__) {
  return cljs.core.list.call(null, "cljs.core.Cons")
};
cljs.core.Cons.prototype.cljs$core$ISeqable$ = true;
cljs.core.Cons.prototype.cljs$core$ISeqable$_seq = function(coll) {
  var this__3281 = this;
  return coll
};
cljs.core.Cons.prototype.cljs$core$IHash$ = true;
cljs.core.Cons.prototype.cljs$core$IHash$_hash = function(coll) {
  var this__3282 = this;
  return cljs.core.hash_coll.call(null, coll)
};
cljs.core.Cons.prototype.cljs$core$IEquiv$ = true;
cljs.core.Cons.prototype.cljs$core$IEquiv$_equiv = function(coll, other) {
  var this__3283 = this;
  return cljs.core.equiv_sequential.call(null, coll, other)
};
cljs.core.Cons.prototype.cljs$core$ISequential$ = true;
cljs.core.Cons.prototype.cljs$core$IEmptyableCollection$ = true;
cljs.core.Cons.prototype.cljs$core$IEmptyableCollection$_empty = function(coll) {
  var this__3284 = this;
  return cljs.core.with_meta.call(null, cljs.core.List.EMPTY, this__3284.meta)
};
cljs.core.Cons.prototype.cljs$core$ICollection$ = true;
cljs.core.Cons.prototype.cljs$core$ICollection$_conj = function(coll, o) {
  var this__3285 = this;
  return new cljs.core.Cons(null, o, coll)
};
cljs.core.Cons.prototype.cljs$core$ISeq$ = true;
cljs.core.Cons.prototype.cljs$core$ISeq$_first = function(coll) {
  var this__3286 = this;
  return this__3286.first
};
cljs.core.Cons.prototype.cljs$core$ISeq$_rest = function(coll) {
  var this__3287 = this;
  if(cljs.core.truth_(this__3287.rest === null)) {
    return cljs.core.List.EMPTY
  }else {
    return this__3287.rest
  }
};
cljs.core.Cons.prototype.cljs$core$IMeta$ = true;
cljs.core.Cons.prototype.cljs$core$IMeta$_meta = function(coll) {
  var this__3288 = this;
  return this__3288.meta
};
cljs.core.Cons.prototype.cljs$core$IWithMeta$ = true;
cljs.core.Cons.prototype.cljs$core$IWithMeta$_with_meta = function(coll, meta) {
  var this__3289 = this;
  return new cljs.core.Cons(meta, this__3289.first, this__3289.rest)
};
cljs.core.Cons;
cljs.core.cons = function cons(x, seq) {
  return new cljs.core.Cons(null, x, seq)
};
cljs.core.IReduce["string"] = true;
cljs.core._reduce["string"] = function() {
  var G__3290 = null;
  var G__3290__3291 = function(string, f) {
    return cljs.core.ci_reduce.call(null, string, f)
  };
  var G__3290__3292 = function(string, f, start) {
    return cljs.core.ci_reduce.call(null, string, f, start)
  };
  G__3290 = function(string, f, start) {
    switch(arguments.length) {
      case 2:
        return G__3290__3291.call(this, string, f);
      case 3:
        return G__3290__3292.call(this, string, f, start)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3290
}();
cljs.core.ILookup["string"] = true;
cljs.core._lookup["string"] = function() {
  var G__3294 = null;
  var G__3294__3295 = function(string, k) {
    return cljs.core._nth.call(null, string, k)
  };
  var G__3294__3296 = function(string, k, not_found) {
    return cljs.core._nth.call(null, string, k, not_found)
  };
  G__3294 = function(string, k, not_found) {
    switch(arguments.length) {
      case 2:
        return G__3294__3295.call(this, string, k);
      case 3:
        return G__3294__3296.call(this, string, k, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3294
}();
cljs.core.IIndexed["string"] = true;
cljs.core._nth["string"] = function() {
  var G__3298 = null;
  var G__3298__3299 = function(string, n) {
    if(cljs.core.truth_(n < cljs.core._count.call(null, string))) {
      return string.charAt(n)
    }else {
      return null
    }
  };
  var G__3298__3300 = function(string, n, not_found) {
    if(cljs.core.truth_(n < cljs.core._count.call(null, string))) {
      return string.charAt(n)
    }else {
      return not_found
    }
  };
  G__3298 = function(string, n, not_found) {
    switch(arguments.length) {
      case 2:
        return G__3298__3299.call(this, string, n);
      case 3:
        return G__3298__3300.call(this, string, n, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3298
}();
cljs.core.ICounted["string"] = true;
cljs.core._count["string"] = function(s) {
  return s.length
};
cljs.core.ISeqable["string"] = true;
cljs.core._seq["string"] = function(string) {
  return cljs.core.prim_seq.call(null, string, 0)
};
cljs.core.IHash["string"] = true;
cljs.core._hash["string"] = function(o) {
  return goog.string.hashCode.call(null, o)
};
String.prototype.cljs$core$IFn$ = true;
String.prototype.call = function() {
  var G__3308 = null;
  var G__3308__3309 = function(tsym3302, coll) {
    var tsym3302__3304 = this;
    var this$__3305 = tsym3302__3304;
    return cljs.core.get.call(null, coll, this$__3305.toString())
  };
  var G__3308__3310 = function(tsym3303, coll, not_found) {
    var tsym3303__3306 = this;
    var this$__3307 = tsym3303__3306;
    return cljs.core.get.call(null, coll, this$__3307.toString(), not_found)
  };
  G__3308 = function(tsym3303, coll, not_found) {
    switch(arguments.length) {
      case 2:
        return G__3308__3309.call(this, tsym3303, coll);
      case 3:
        return G__3308__3310.call(this, tsym3303, coll, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3308
}();
String["prototype"]["apply"] = function(s, args) {
  if(cljs.core.truth_(cljs.core.count.call(null, args) < 2)) {
    return cljs.core.get.call(null, args[0], s)
  }else {
    return cljs.core.get.call(null, args[0], s, args[1])
  }
};
cljs.core.lazy_seq_value = function lazy_seq_value(lazy_seq) {
  var x__3312 = lazy_seq.x;
  if(cljs.core.truth_(lazy_seq.realized)) {
    return x__3312
  }else {
    lazy_seq.x = x__3312.call(null);
    lazy_seq.realized = true;
    return lazy_seq.x
  }
};
cljs.core.LazySeq = function(meta, realized, x) {
  this.meta = meta;
  this.realized = realized;
  this.x = x
};
cljs.core.LazySeq.cljs$core$IPrintable$_pr_seq = function(this__267__auto__) {
  return cljs.core.list.call(null, "cljs.core.LazySeq")
};
cljs.core.LazySeq.prototype.cljs$core$ISeqable$ = true;
cljs.core.LazySeq.prototype.cljs$core$ISeqable$_seq = function(coll) {
  var this__3313 = this;
  return cljs.core.seq.call(null, cljs.core.lazy_seq_value.call(null, coll))
};
cljs.core.LazySeq.prototype.cljs$core$IHash$ = true;
cljs.core.LazySeq.prototype.cljs$core$IHash$_hash = function(coll) {
  var this__3314 = this;
  return cljs.core.hash_coll.call(null, coll)
};
cljs.core.LazySeq.prototype.cljs$core$IEquiv$ = true;
cljs.core.LazySeq.prototype.cljs$core$IEquiv$_equiv = function(coll, other) {
  var this__3315 = this;
  return cljs.core.equiv_sequential.call(null, coll, other)
};
cljs.core.LazySeq.prototype.cljs$core$ISequential$ = true;
cljs.core.LazySeq.prototype.cljs$core$IEmptyableCollection$ = true;
cljs.core.LazySeq.prototype.cljs$core$IEmptyableCollection$_empty = function(coll) {
  var this__3316 = this;
  return cljs.core.with_meta.call(null, cljs.core.List.EMPTY, this__3316.meta)
};
cljs.core.LazySeq.prototype.cljs$core$ICollection$ = true;
cljs.core.LazySeq.prototype.cljs$core$ICollection$_conj = function(coll, o) {
  var this__3317 = this;
  return cljs.core.cons.call(null, o, coll)
};
cljs.core.LazySeq.prototype.cljs$core$ISeq$ = true;
cljs.core.LazySeq.prototype.cljs$core$ISeq$_first = function(coll) {
  var this__3318 = this;
  return cljs.core.first.call(null, cljs.core.lazy_seq_value.call(null, coll))
};
cljs.core.LazySeq.prototype.cljs$core$ISeq$_rest = function(coll) {
  var this__3319 = this;
  return cljs.core.rest.call(null, cljs.core.lazy_seq_value.call(null, coll))
};
cljs.core.LazySeq.prototype.cljs$core$IMeta$ = true;
cljs.core.LazySeq.prototype.cljs$core$IMeta$_meta = function(coll) {
  var this__3320 = this;
  return this__3320.meta
};
cljs.core.LazySeq.prototype.cljs$core$IWithMeta$ = true;
cljs.core.LazySeq.prototype.cljs$core$IWithMeta$_with_meta = function(coll, meta) {
  var this__3321 = this;
  return new cljs.core.LazySeq(meta, this__3321.realized, this__3321.x)
};
cljs.core.LazySeq;
cljs.core.to_array = function to_array(s) {
  var ary__3322 = cljs.core.array.call(null);
  var s__3323 = s;
  while(true) {
    if(cljs.core.truth_(cljs.core.seq.call(null, s__3323))) {
      ary__3322.push(cljs.core.first.call(null, s__3323));
      var G__3324 = cljs.core.next.call(null, s__3323);
      s__3323 = G__3324;
      continue
    }else {
      return ary__3322
    }
    break
  }
};
cljs.core.bounded_count = function bounded_count(s, n) {
  var s__3325 = s;
  var i__3326 = n;
  var sum__3327 = 0;
  while(true) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____3328 = i__3326 > 0;
      if(cljs.core.truth_(and__3546__auto____3328)) {
        return cljs.core.seq.call(null, s__3325)
      }else {
        return and__3546__auto____3328
      }
    }())) {
      var G__3329 = cljs.core.next.call(null, s__3325);
      var G__3330 = i__3326 - 1;
      var G__3331 = sum__3327 + 1;
      s__3325 = G__3329;
      i__3326 = G__3330;
      sum__3327 = G__3331;
      continue
    }else {
      return sum__3327
    }
    break
  }
};
cljs.core.spread = function spread(arglist) {
  if(cljs.core.truth_(arglist === null)) {
    return null
  }else {
    if(cljs.core.truth_(cljs.core.next.call(null, arglist) === null)) {
      return cljs.core.seq.call(null, cljs.core.first.call(null, arglist))
    }else {
      if(cljs.core.truth_("\ufdd0'else")) {
        return cljs.core.cons.call(null, cljs.core.first.call(null, arglist), spread.call(null, cljs.core.next.call(null, arglist)))
      }else {
        return null
      }
    }
  }
};
cljs.core.concat = function() {
  var concat = null;
  var concat__3335 = function() {
    return new cljs.core.LazySeq(null, false, function() {
      return null
    })
  };
  var concat__3336 = function(x) {
    return new cljs.core.LazySeq(null, false, function() {
      return x
    })
  };
  var concat__3337 = function(x, y) {
    return new cljs.core.LazySeq(null, false, function() {
      var s__3332 = cljs.core.seq.call(null, x);
      if(cljs.core.truth_(s__3332)) {
        return cljs.core.cons.call(null, cljs.core.first.call(null, s__3332), concat.call(null, cljs.core.rest.call(null, s__3332), y))
      }else {
        return y
      }
    })
  };
  var concat__3338 = function() {
    var G__3340__delegate = function(x, y, zs) {
      var cat__3334 = function cat(xys, zs) {
        return new cljs.core.LazySeq(null, false, function() {
          var xys__3333 = cljs.core.seq.call(null, xys);
          if(cljs.core.truth_(xys__3333)) {
            return cljs.core.cons.call(null, cljs.core.first.call(null, xys__3333), cat.call(null, cljs.core.rest.call(null, xys__3333), zs))
          }else {
            if(cljs.core.truth_(zs)) {
              return cat.call(null, cljs.core.first.call(null, zs), cljs.core.next.call(null, zs))
            }else {
              return null
            }
          }
        })
      };
      return cat__3334.call(null, concat.call(null, x, y), zs)
    };
    var G__3340 = function(x, y, var_args) {
      var zs = null;
      if(goog.isDef(var_args)) {
        zs = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3340__delegate.call(this, x, y, zs)
    };
    G__3340.cljs$lang$maxFixedArity = 2;
    G__3340.cljs$lang$applyTo = function(arglist__3341) {
      var x = cljs.core.first(arglist__3341);
      var y = cljs.core.first(cljs.core.next(arglist__3341));
      var zs = cljs.core.rest(cljs.core.next(arglist__3341));
      return G__3340__delegate.call(this, x, y, zs)
    };
    return G__3340
  }();
  concat = function(x, y, var_args) {
    var zs = var_args;
    switch(arguments.length) {
      case 0:
        return concat__3335.call(this);
      case 1:
        return concat__3336.call(this, x);
      case 2:
        return concat__3337.call(this, x, y);
      default:
        return concat__3338.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  concat.cljs$lang$maxFixedArity = 2;
  concat.cljs$lang$applyTo = concat__3338.cljs$lang$applyTo;
  return concat
}();
cljs.core.list_STAR_ = function() {
  var list_STAR_ = null;
  var list_STAR___3342 = function(args) {
    return cljs.core.seq.call(null, args)
  };
  var list_STAR___3343 = function(a, args) {
    return cljs.core.cons.call(null, a, args)
  };
  var list_STAR___3344 = function(a, b, args) {
    return cljs.core.cons.call(null, a, cljs.core.cons.call(null, b, args))
  };
  var list_STAR___3345 = function(a, b, c, args) {
    return cljs.core.cons.call(null, a, cljs.core.cons.call(null, b, cljs.core.cons.call(null, c, args)))
  };
  var list_STAR___3346 = function() {
    var G__3348__delegate = function(a, b, c, d, more) {
      return cljs.core.cons.call(null, a, cljs.core.cons.call(null, b, cljs.core.cons.call(null, c, cljs.core.cons.call(null, d, cljs.core.spread.call(null, more)))))
    };
    var G__3348 = function(a, b, c, d, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 4), 0)
      }
      return G__3348__delegate.call(this, a, b, c, d, more)
    };
    G__3348.cljs$lang$maxFixedArity = 4;
    G__3348.cljs$lang$applyTo = function(arglist__3349) {
      var a = cljs.core.first(arglist__3349);
      var b = cljs.core.first(cljs.core.next(arglist__3349));
      var c = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3349)));
      var d = cljs.core.first(cljs.core.next(cljs.core.next(cljs.core.next(arglist__3349))));
      var more = cljs.core.rest(cljs.core.next(cljs.core.next(cljs.core.next(arglist__3349))));
      return G__3348__delegate.call(this, a, b, c, d, more)
    };
    return G__3348
  }();
  list_STAR_ = function(a, b, c, d, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 1:
        return list_STAR___3342.call(this, a);
      case 2:
        return list_STAR___3343.call(this, a, b);
      case 3:
        return list_STAR___3344.call(this, a, b, c);
      case 4:
        return list_STAR___3345.call(this, a, b, c, d);
      default:
        return list_STAR___3346.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  list_STAR_.cljs$lang$maxFixedArity = 4;
  list_STAR_.cljs$lang$applyTo = list_STAR___3346.cljs$lang$applyTo;
  return list_STAR_
}();
cljs.core.apply = function() {
  var apply = null;
  var apply__3359 = function(f, args) {
    var fixed_arity__3350 = f.cljs$lang$maxFixedArity;
    if(cljs.core.truth_(f.cljs$lang$applyTo)) {
      if(cljs.core.truth_(cljs.core.bounded_count.call(null, args, fixed_arity__3350 + 1) <= fixed_arity__3350)) {
        return f.apply(f, cljs.core.to_array.call(null, args))
      }else {
        return f.cljs$lang$applyTo(args)
      }
    }else {
      return f.apply(f, cljs.core.to_array.call(null, args))
    }
  };
  var apply__3360 = function(f, x, args) {
    var arglist__3351 = cljs.core.list_STAR_.call(null, x, args);
    var fixed_arity__3352 = f.cljs$lang$maxFixedArity;
    if(cljs.core.truth_(f.cljs$lang$applyTo)) {
      if(cljs.core.truth_(cljs.core.bounded_count.call(null, arglist__3351, fixed_arity__3352) <= fixed_arity__3352)) {
        return f.apply(f, cljs.core.to_array.call(null, arglist__3351))
      }else {
        return f.cljs$lang$applyTo(arglist__3351)
      }
    }else {
      return f.apply(f, cljs.core.to_array.call(null, arglist__3351))
    }
  };
  var apply__3361 = function(f, x, y, args) {
    var arglist__3353 = cljs.core.list_STAR_.call(null, x, y, args);
    var fixed_arity__3354 = f.cljs$lang$maxFixedArity;
    if(cljs.core.truth_(f.cljs$lang$applyTo)) {
      if(cljs.core.truth_(cljs.core.bounded_count.call(null, arglist__3353, fixed_arity__3354) <= fixed_arity__3354)) {
        return f.apply(f, cljs.core.to_array.call(null, arglist__3353))
      }else {
        return f.cljs$lang$applyTo(arglist__3353)
      }
    }else {
      return f.apply(f, cljs.core.to_array.call(null, arglist__3353))
    }
  };
  var apply__3362 = function(f, x, y, z, args) {
    var arglist__3355 = cljs.core.list_STAR_.call(null, x, y, z, args);
    var fixed_arity__3356 = f.cljs$lang$maxFixedArity;
    if(cljs.core.truth_(f.cljs$lang$applyTo)) {
      if(cljs.core.truth_(cljs.core.bounded_count.call(null, arglist__3355, fixed_arity__3356) <= fixed_arity__3356)) {
        return f.apply(f, cljs.core.to_array.call(null, arglist__3355))
      }else {
        return f.cljs$lang$applyTo(arglist__3355)
      }
    }else {
      return f.apply(f, cljs.core.to_array.call(null, arglist__3355))
    }
  };
  var apply__3363 = function() {
    var G__3365__delegate = function(f, a, b, c, d, args) {
      var arglist__3357 = cljs.core.cons.call(null, a, cljs.core.cons.call(null, b, cljs.core.cons.call(null, c, cljs.core.cons.call(null, d, cljs.core.spread.call(null, args)))));
      var fixed_arity__3358 = f.cljs$lang$maxFixedArity;
      if(cljs.core.truth_(f.cljs$lang$applyTo)) {
        if(cljs.core.truth_(cljs.core.bounded_count.call(null, arglist__3357, fixed_arity__3358) <= fixed_arity__3358)) {
          return f.apply(f, cljs.core.to_array.call(null, arglist__3357))
        }else {
          return f.cljs$lang$applyTo(arglist__3357)
        }
      }else {
        return f.apply(f, cljs.core.to_array.call(null, arglist__3357))
      }
    };
    var G__3365 = function(f, a, b, c, d, var_args) {
      var args = null;
      if(goog.isDef(var_args)) {
        args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 5), 0)
      }
      return G__3365__delegate.call(this, f, a, b, c, d, args)
    };
    G__3365.cljs$lang$maxFixedArity = 5;
    G__3365.cljs$lang$applyTo = function(arglist__3366) {
      var f = cljs.core.first(arglist__3366);
      var a = cljs.core.first(cljs.core.next(arglist__3366));
      var b = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3366)));
      var c = cljs.core.first(cljs.core.next(cljs.core.next(cljs.core.next(arglist__3366))));
      var d = cljs.core.first(cljs.core.next(cljs.core.next(cljs.core.next(cljs.core.next(arglist__3366)))));
      var args = cljs.core.rest(cljs.core.next(cljs.core.next(cljs.core.next(cljs.core.next(arglist__3366)))));
      return G__3365__delegate.call(this, f, a, b, c, d, args)
    };
    return G__3365
  }();
  apply = function(f, a, b, c, d, var_args) {
    var args = var_args;
    switch(arguments.length) {
      case 2:
        return apply__3359.call(this, f, a);
      case 3:
        return apply__3360.call(this, f, a, b);
      case 4:
        return apply__3361.call(this, f, a, b, c);
      case 5:
        return apply__3362.call(this, f, a, b, c, d);
      default:
        return apply__3363.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  apply.cljs$lang$maxFixedArity = 5;
  apply.cljs$lang$applyTo = apply__3363.cljs$lang$applyTo;
  return apply
}();
cljs.core.vary_meta = function() {
  var vary_meta__delegate = function(obj, f, args) {
    return cljs.core.with_meta.call(null, obj, cljs.core.apply.call(null, f, cljs.core.meta.call(null, obj), args))
  };
  var vary_meta = function(obj, f, var_args) {
    var args = null;
    if(goog.isDef(var_args)) {
      args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
    }
    return vary_meta__delegate.call(this, obj, f, args)
  };
  vary_meta.cljs$lang$maxFixedArity = 2;
  vary_meta.cljs$lang$applyTo = function(arglist__3367) {
    var obj = cljs.core.first(arglist__3367);
    var f = cljs.core.first(cljs.core.next(arglist__3367));
    var args = cljs.core.rest(cljs.core.next(arglist__3367));
    return vary_meta__delegate.call(this, obj, f, args)
  };
  return vary_meta
}();
cljs.core.not_EQ_ = function() {
  var not_EQ_ = null;
  var not_EQ___3368 = function(x) {
    return false
  };
  var not_EQ___3369 = function(x, y) {
    return cljs.core.not.call(null, cljs.core._EQ_.call(null, x, y))
  };
  var not_EQ___3370 = function() {
    var G__3372__delegate = function(x, y, more) {
      return cljs.core.not.call(null, cljs.core.apply.call(null, cljs.core._EQ_, x, y, more))
    };
    var G__3372 = function(x, y, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3372__delegate.call(this, x, y, more)
    };
    G__3372.cljs$lang$maxFixedArity = 2;
    G__3372.cljs$lang$applyTo = function(arglist__3373) {
      var x = cljs.core.first(arglist__3373);
      var y = cljs.core.first(cljs.core.next(arglist__3373));
      var more = cljs.core.rest(cljs.core.next(arglist__3373));
      return G__3372__delegate.call(this, x, y, more)
    };
    return G__3372
  }();
  not_EQ_ = function(x, y, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 1:
        return not_EQ___3368.call(this, x);
      case 2:
        return not_EQ___3369.call(this, x, y);
      default:
        return not_EQ___3370.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  not_EQ_.cljs$lang$maxFixedArity = 2;
  not_EQ_.cljs$lang$applyTo = not_EQ___3370.cljs$lang$applyTo;
  return not_EQ_
}();
cljs.core.not_empty = function not_empty(coll) {
  if(cljs.core.truth_(cljs.core.seq.call(null, coll))) {
    return coll
  }else {
    return null
  }
};
cljs.core.every_QMARK_ = function every_QMARK_(pred, coll) {
  while(true) {
    if(cljs.core.truth_(cljs.core.seq.call(null, coll) === null)) {
      return true
    }else {
      if(cljs.core.truth_(pred.call(null, cljs.core.first.call(null, coll)))) {
        var G__3374 = pred;
        var G__3375 = cljs.core.next.call(null, coll);
        pred = G__3374;
        coll = G__3375;
        continue
      }else {
        if(cljs.core.truth_("\ufdd0'else")) {
          return false
        }else {
          return null
        }
      }
    }
    break
  }
};
cljs.core.not_every_QMARK_ = function not_every_QMARK_(pred, coll) {
  return cljs.core.not.call(null, cljs.core.every_QMARK_.call(null, pred, coll))
};
cljs.core.some = function some(pred, coll) {
  while(true) {
    if(cljs.core.truth_(cljs.core.seq.call(null, coll))) {
      var or__3548__auto____3376 = pred.call(null, cljs.core.first.call(null, coll));
      if(cljs.core.truth_(or__3548__auto____3376)) {
        return or__3548__auto____3376
      }else {
        var G__3377 = pred;
        var G__3378 = cljs.core.next.call(null, coll);
        pred = G__3377;
        coll = G__3378;
        continue
      }
    }else {
      return null
    }
    break
  }
};
cljs.core.not_any_QMARK_ = function not_any_QMARK_(pred, coll) {
  return cljs.core.not.call(null, cljs.core.some.call(null, pred, coll))
};
cljs.core.even_QMARK_ = function even_QMARK_(n) {
  if(cljs.core.truth_(cljs.core.integer_QMARK_.call(null, n))) {
    return(n & 1) === 0
  }else {
    throw new Error(cljs.core.str.call(null, "Argument must be an integer: ", n));
  }
};
cljs.core.odd_QMARK_ = function odd_QMARK_(n) {
  return cljs.core.not.call(null, cljs.core.even_QMARK_.call(null, n))
};
cljs.core.identity = function identity(x) {
  return x
};
cljs.core.complement = function complement(f) {
  return function() {
    var G__3379 = null;
    var G__3379__3380 = function() {
      return cljs.core.not.call(null, f.call(null))
    };
    var G__3379__3381 = function(x) {
      return cljs.core.not.call(null, f.call(null, x))
    };
    var G__3379__3382 = function(x, y) {
      return cljs.core.not.call(null, f.call(null, x, y))
    };
    var G__3379__3383 = function() {
      var G__3385__delegate = function(x, y, zs) {
        return cljs.core.not.call(null, cljs.core.apply.call(null, f, x, y, zs))
      };
      var G__3385 = function(x, y, var_args) {
        var zs = null;
        if(goog.isDef(var_args)) {
          zs = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
        }
        return G__3385__delegate.call(this, x, y, zs)
      };
      G__3385.cljs$lang$maxFixedArity = 2;
      G__3385.cljs$lang$applyTo = function(arglist__3386) {
        var x = cljs.core.first(arglist__3386);
        var y = cljs.core.first(cljs.core.next(arglist__3386));
        var zs = cljs.core.rest(cljs.core.next(arglist__3386));
        return G__3385__delegate.call(this, x, y, zs)
      };
      return G__3385
    }();
    G__3379 = function(x, y, var_args) {
      var zs = var_args;
      switch(arguments.length) {
        case 0:
          return G__3379__3380.call(this);
        case 1:
          return G__3379__3381.call(this, x);
        case 2:
          return G__3379__3382.call(this, x, y);
        default:
          return G__3379__3383.apply(this, arguments)
      }
      throw"Invalid arity: " + arguments.length;
    };
    G__3379.cljs$lang$maxFixedArity = 2;
    G__3379.cljs$lang$applyTo = G__3379__3383.cljs$lang$applyTo;
    return G__3379
  }()
};
cljs.core.constantly = function constantly(x) {
  return function() {
    var G__3387__delegate = function(args) {
      return x
    };
    var G__3387 = function(var_args) {
      var args = null;
      if(goog.isDef(var_args)) {
        args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 0), 0)
      }
      return G__3387__delegate.call(this, args)
    };
    G__3387.cljs$lang$maxFixedArity = 0;
    G__3387.cljs$lang$applyTo = function(arglist__3388) {
      var args = cljs.core.seq(arglist__3388);
      return G__3387__delegate.call(this, args)
    };
    return G__3387
  }()
};
cljs.core.comp = function() {
  var comp = null;
  var comp__3392 = function() {
    return cljs.core.identity
  };
  var comp__3393 = function(f) {
    return f
  };
  var comp__3394 = function(f, g) {
    return function() {
      var G__3398 = null;
      var G__3398__3399 = function() {
        return f.call(null, g.call(null))
      };
      var G__3398__3400 = function(x) {
        return f.call(null, g.call(null, x))
      };
      var G__3398__3401 = function(x, y) {
        return f.call(null, g.call(null, x, y))
      };
      var G__3398__3402 = function(x, y, z) {
        return f.call(null, g.call(null, x, y, z))
      };
      var G__3398__3403 = function() {
        var G__3405__delegate = function(x, y, z, args) {
          return f.call(null, cljs.core.apply.call(null, g, x, y, z, args))
        };
        var G__3405 = function(x, y, z, var_args) {
          var args = null;
          if(goog.isDef(var_args)) {
            args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
          }
          return G__3405__delegate.call(this, x, y, z, args)
        };
        G__3405.cljs$lang$maxFixedArity = 3;
        G__3405.cljs$lang$applyTo = function(arglist__3406) {
          var x = cljs.core.first(arglist__3406);
          var y = cljs.core.first(cljs.core.next(arglist__3406));
          var z = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3406)));
          var args = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3406)));
          return G__3405__delegate.call(this, x, y, z, args)
        };
        return G__3405
      }();
      G__3398 = function(x, y, z, var_args) {
        var args = var_args;
        switch(arguments.length) {
          case 0:
            return G__3398__3399.call(this);
          case 1:
            return G__3398__3400.call(this, x);
          case 2:
            return G__3398__3401.call(this, x, y);
          case 3:
            return G__3398__3402.call(this, x, y, z);
          default:
            return G__3398__3403.apply(this, arguments)
        }
        throw"Invalid arity: " + arguments.length;
      };
      G__3398.cljs$lang$maxFixedArity = 3;
      G__3398.cljs$lang$applyTo = G__3398__3403.cljs$lang$applyTo;
      return G__3398
    }()
  };
  var comp__3395 = function(f, g, h) {
    return function() {
      var G__3407 = null;
      var G__3407__3408 = function() {
        return f.call(null, g.call(null, h.call(null)))
      };
      var G__3407__3409 = function(x) {
        return f.call(null, g.call(null, h.call(null, x)))
      };
      var G__3407__3410 = function(x, y) {
        return f.call(null, g.call(null, h.call(null, x, y)))
      };
      var G__3407__3411 = function(x, y, z) {
        return f.call(null, g.call(null, h.call(null, x, y, z)))
      };
      var G__3407__3412 = function() {
        var G__3414__delegate = function(x, y, z, args) {
          return f.call(null, g.call(null, cljs.core.apply.call(null, h, x, y, z, args)))
        };
        var G__3414 = function(x, y, z, var_args) {
          var args = null;
          if(goog.isDef(var_args)) {
            args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
          }
          return G__3414__delegate.call(this, x, y, z, args)
        };
        G__3414.cljs$lang$maxFixedArity = 3;
        G__3414.cljs$lang$applyTo = function(arglist__3415) {
          var x = cljs.core.first(arglist__3415);
          var y = cljs.core.first(cljs.core.next(arglist__3415));
          var z = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3415)));
          var args = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3415)));
          return G__3414__delegate.call(this, x, y, z, args)
        };
        return G__3414
      }();
      G__3407 = function(x, y, z, var_args) {
        var args = var_args;
        switch(arguments.length) {
          case 0:
            return G__3407__3408.call(this);
          case 1:
            return G__3407__3409.call(this, x);
          case 2:
            return G__3407__3410.call(this, x, y);
          case 3:
            return G__3407__3411.call(this, x, y, z);
          default:
            return G__3407__3412.apply(this, arguments)
        }
        throw"Invalid arity: " + arguments.length;
      };
      G__3407.cljs$lang$maxFixedArity = 3;
      G__3407.cljs$lang$applyTo = G__3407__3412.cljs$lang$applyTo;
      return G__3407
    }()
  };
  var comp__3396 = function() {
    var G__3416__delegate = function(f1, f2, f3, fs) {
      var fs__3389 = cljs.core.reverse.call(null, cljs.core.list_STAR_.call(null, f1, f2, f3, fs));
      return function() {
        var G__3417__delegate = function(args) {
          var ret__3390 = cljs.core.apply.call(null, cljs.core.first.call(null, fs__3389), args);
          var fs__3391 = cljs.core.next.call(null, fs__3389);
          while(true) {
            if(cljs.core.truth_(fs__3391)) {
              var G__3418 = cljs.core.first.call(null, fs__3391).call(null, ret__3390);
              var G__3419 = cljs.core.next.call(null, fs__3391);
              ret__3390 = G__3418;
              fs__3391 = G__3419;
              continue
            }else {
              return ret__3390
            }
            break
          }
        };
        var G__3417 = function(var_args) {
          var args = null;
          if(goog.isDef(var_args)) {
            args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 0), 0)
          }
          return G__3417__delegate.call(this, args)
        };
        G__3417.cljs$lang$maxFixedArity = 0;
        G__3417.cljs$lang$applyTo = function(arglist__3420) {
          var args = cljs.core.seq(arglist__3420);
          return G__3417__delegate.call(this, args)
        };
        return G__3417
      }()
    };
    var G__3416 = function(f1, f2, f3, var_args) {
      var fs = null;
      if(goog.isDef(var_args)) {
        fs = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
      }
      return G__3416__delegate.call(this, f1, f2, f3, fs)
    };
    G__3416.cljs$lang$maxFixedArity = 3;
    G__3416.cljs$lang$applyTo = function(arglist__3421) {
      var f1 = cljs.core.first(arglist__3421);
      var f2 = cljs.core.first(cljs.core.next(arglist__3421));
      var f3 = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3421)));
      var fs = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3421)));
      return G__3416__delegate.call(this, f1, f2, f3, fs)
    };
    return G__3416
  }();
  comp = function(f1, f2, f3, var_args) {
    var fs = var_args;
    switch(arguments.length) {
      case 0:
        return comp__3392.call(this);
      case 1:
        return comp__3393.call(this, f1);
      case 2:
        return comp__3394.call(this, f1, f2);
      case 3:
        return comp__3395.call(this, f1, f2, f3);
      default:
        return comp__3396.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  comp.cljs$lang$maxFixedArity = 3;
  comp.cljs$lang$applyTo = comp__3396.cljs$lang$applyTo;
  return comp
}();
cljs.core.partial = function() {
  var partial = null;
  var partial__3422 = function(f, arg1) {
    return function() {
      var G__3427__delegate = function(args) {
        return cljs.core.apply.call(null, f, arg1, args)
      };
      var G__3427 = function(var_args) {
        var args = null;
        if(goog.isDef(var_args)) {
          args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 0), 0)
        }
        return G__3427__delegate.call(this, args)
      };
      G__3427.cljs$lang$maxFixedArity = 0;
      G__3427.cljs$lang$applyTo = function(arglist__3428) {
        var args = cljs.core.seq(arglist__3428);
        return G__3427__delegate.call(this, args)
      };
      return G__3427
    }()
  };
  var partial__3423 = function(f, arg1, arg2) {
    return function() {
      var G__3429__delegate = function(args) {
        return cljs.core.apply.call(null, f, arg1, arg2, args)
      };
      var G__3429 = function(var_args) {
        var args = null;
        if(goog.isDef(var_args)) {
          args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 0), 0)
        }
        return G__3429__delegate.call(this, args)
      };
      G__3429.cljs$lang$maxFixedArity = 0;
      G__3429.cljs$lang$applyTo = function(arglist__3430) {
        var args = cljs.core.seq(arglist__3430);
        return G__3429__delegate.call(this, args)
      };
      return G__3429
    }()
  };
  var partial__3424 = function(f, arg1, arg2, arg3) {
    return function() {
      var G__3431__delegate = function(args) {
        return cljs.core.apply.call(null, f, arg1, arg2, arg3, args)
      };
      var G__3431 = function(var_args) {
        var args = null;
        if(goog.isDef(var_args)) {
          args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 0), 0)
        }
        return G__3431__delegate.call(this, args)
      };
      G__3431.cljs$lang$maxFixedArity = 0;
      G__3431.cljs$lang$applyTo = function(arglist__3432) {
        var args = cljs.core.seq(arglist__3432);
        return G__3431__delegate.call(this, args)
      };
      return G__3431
    }()
  };
  var partial__3425 = function() {
    var G__3433__delegate = function(f, arg1, arg2, arg3, more) {
      return function() {
        var G__3434__delegate = function(args) {
          return cljs.core.apply.call(null, f, arg1, arg2, arg3, cljs.core.concat.call(null, more, args))
        };
        var G__3434 = function(var_args) {
          var args = null;
          if(goog.isDef(var_args)) {
            args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 0), 0)
          }
          return G__3434__delegate.call(this, args)
        };
        G__3434.cljs$lang$maxFixedArity = 0;
        G__3434.cljs$lang$applyTo = function(arglist__3435) {
          var args = cljs.core.seq(arglist__3435);
          return G__3434__delegate.call(this, args)
        };
        return G__3434
      }()
    };
    var G__3433 = function(f, arg1, arg2, arg3, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 4), 0)
      }
      return G__3433__delegate.call(this, f, arg1, arg2, arg3, more)
    };
    G__3433.cljs$lang$maxFixedArity = 4;
    G__3433.cljs$lang$applyTo = function(arglist__3436) {
      var f = cljs.core.first(arglist__3436);
      var arg1 = cljs.core.first(cljs.core.next(arglist__3436));
      var arg2 = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3436)));
      var arg3 = cljs.core.first(cljs.core.next(cljs.core.next(cljs.core.next(arglist__3436))));
      var more = cljs.core.rest(cljs.core.next(cljs.core.next(cljs.core.next(arglist__3436))));
      return G__3433__delegate.call(this, f, arg1, arg2, arg3, more)
    };
    return G__3433
  }();
  partial = function(f, arg1, arg2, arg3, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 2:
        return partial__3422.call(this, f, arg1);
      case 3:
        return partial__3423.call(this, f, arg1, arg2);
      case 4:
        return partial__3424.call(this, f, arg1, arg2, arg3);
      default:
        return partial__3425.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  partial.cljs$lang$maxFixedArity = 4;
  partial.cljs$lang$applyTo = partial__3425.cljs$lang$applyTo;
  return partial
}();
cljs.core.fnil = function() {
  var fnil = null;
  var fnil__3437 = function(f, x) {
    return function() {
      var G__3441 = null;
      var G__3441__3442 = function(a) {
        return f.call(null, cljs.core.truth_(a === null) ? x : a)
      };
      var G__3441__3443 = function(a, b) {
        return f.call(null, cljs.core.truth_(a === null) ? x : a, b)
      };
      var G__3441__3444 = function(a, b, c) {
        return f.call(null, cljs.core.truth_(a === null) ? x : a, b, c)
      };
      var G__3441__3445 = function() {
        var G__3447__delegate = function(a, b, c, ds) {
          return cljs.core.apply.call(null, f, cljs.core.truth_(a === null) ? x : a, b, c, ds)
        };
        var G__3447 = function(a, b, c, var_args) {
          var ds = null;
          if(goog.isDef(var_args)) {
            ds = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
          }
          return G__3447__delegate.call(this, a, b, c, ds)
        };
        G__3447.cljs$lang$maxFixedArity = 3;
        G__3447.cljs$lang$applyTo = function(arglist__3448) {
          var a = cljs.core.first(arglist__3448);
          var b = cljs.core.first(cljs.core.next(arglist__3448));
          var c = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3448)));
          var ds = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3448)));
          return G__3447__delegate.call(this, a, b, c, ds)
        };
        return G__3447
      }();
      G__3441 = function(a, b, c, var_args) {
        var ds = var_args;
        switch(arguments.length) {
          case 1:
            return G__3441__3442.call(this, a);
          case 2:
            return G__3441__3443.call(this, a, b);
          case 3:
            return G__3441__3444.call(this, a, b, c);
          default:
            return G__3441__3445.apply(this, arguments)
        }
        throw"Invalid arity: " + arguments.length;
      };
      G__3441.cljs$lang$maxFixedArity = 3;
      G__3441.cljs$lang$applyTo = G__3441__3445.cljs$lang$applyTo;
      return G__3441
    }()
  };
  var fnil__3438 = function(f, x, y) {
    return function() {
      var G__3449 = null;
      var G__3449__3450 = function(a, b) {
        return f.call(null, cljs.core.truth_(a === null) ? x : a, cljs.core.truth_(b === null) ? y : b)
      };
      var G__3449__3451 = function(a, b, c) {
        return f.call(null, cljs.core.truth_(a === null) ? x : a, cljs.core.truth_(b === null) ? y : b, c)
      };
      var G__3449__3452 = function() {
        var G__3454__delegate = function(a, b, c, ds) {
          return cljs.core.apply.call(null, f, cljs.core.truth_(a === null) ? x : a, cljs.core.truth_(b === null) ? y : b, c, ds)
        };
        var G__3454 = function(a, b, c, var_args) {
          var ds = null;
          if(goog.isDef(var_args)) {
            ds = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
          }
          return G__3454__delegate.call(this, a, b, c, ds)
        };
        G__3454.cljs$lang$maxFixedArity = 3;
        G__3454.cljs$lang$applyTo = function(arglist__3455) {
          var a = cljs.core.first(arglist__3455);
          var b = cljs.core.first(cljs.core.next(arglist__3455));
          var c = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3455)));
          var ds = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3455)));
          return G__3454__delegate.call(this, a, b, c, ds)
        };
        return G__3454
      }();
      G__3449 = function(a, b, c, var_args) {
        var ds = var_args;
        switch(arguments.length) {
          case 2:
            return G__3449__3450.call(this, a, b);
          case 3:
            return G__3449__3451.call(this, a, b, c);
          default:
            return G__3449__3452.apply(this, arguments)
        }
        throw"Invalid arity: " + arguments.length;
      };
      G__3449.cljs$lang$maxFixedArity = 3;
      G__3449.cljs$lang$applyTo = G__3449__3452.cljs$lang$applyTo;
      return G__3449
    }()
  };
  var fnil__3439 = function(f, x, y, z) {
    return function() {
      var G__3456 = null;
      var G__3456__3457 = function(a, b) {
        return f.call(null, cljs.core.truth_(a === null) ? x : a, cljs.core.truth_(b === null) ? y : b)
      };
      var G__3456__3458 = function(a, b, c) {
        return f.call(null, cljs.core.truth_(a === null) ? x : a, cljs.core.truth_(b === null) ? y : b, cljs.core.truth_(c === null) ? z : c)
      };
      var G__3456__3459 = function() {
        var G__3461__delegate = function(a, b, c, ds) {
          return cljs.core.apply.call(null, f, cljs.core.truth_(a === null) ? x : a, cljs.core.truth_(b === null) ? y : b, cljs.core.truth_(c === null) ? z : c, ds)
        };
        var G__3461 = function(a, b, c, var_args) {
          var ds = null;
          if(goog.isDef(var_args)) {
            ds = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
          }
          return G__3461__delegate.call(this, a, b, c, ds)
        };
        G__3461.cljs$lang$maxFixedArity = 3;
        G__3461.cljs$lang$applyTo = function(arglist__3462) {
          var a = cljs.core.first(arglist__3462);
          var b = cljs.core.first(cljs.core.next(arglist__3462));
          var c = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3462)));
          var ds = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3462)));
          return G__3461__delegate.call(this, a, b, c, ds)
        };
        return G__3461
      }();
      G__3456 = function(a, b, c, var_args) {
        var ds = var_args;
        switch(arguments.length) {
          case 2:
            return G__3456__3457.call(this, a, b);
          case 3:
            return G__3456__3458.call(this, a, b, c);
          default:
            return G__3456__3459.apply(this, arguments)
        }
        throw"Invalid arity: " + arguments.length;
      };
      G__3456.cljs$lang$maxFixedArity = 3;
      G__3456.cljs$lang$applyTo = G__3456__3459.cljs$lang$applyTo;
      return G__3456
    }()
  };
  fnil = function(f, x, y, z) {
    switch(arguments.length) {
      case 2:
        return fnil__3437.call(this, f, x);
      case 3:
        return fnil__3438.call(this, f, x, y);
      case 4:
        return fnil__3439.call(this, f, x, y, z)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return fnil
}();
cljs.core.map_indexed = function map_indexed(f, coll) {
  var mapi__3465 = function mpi(idx, coll) {
    return new cljs.core.LazySeq(null, false, function() {
      var temp__3698__auto____3463 = cljs.core.seq.call(null, coll);
      if(cljs.core.truth_(temp__3698__auto____3463)) {
        var s__3464 = temp__3698__auto____3463;
        return cljs.core.cons.call(null, f.call(null, idx, cljs.core.first.call(null, s__3464)), mpi.call(null, idx + 1, cljs.core.rest.call(null, s__3464)))
      }else {
        return null
      }
    })
  };
  return mapi__3465.call(null, 0, coll)
};
cljs.core.keep = function keep(f, coll) {
  return new cljs.core.LazySeq(null, false, function() {
    var temp__3698__auto____3466 = cljs.core.seq.call(null, coll);
    if(cljs.core.truth_(temp__3698__auto____3466)) {
      var s__3467 = temp__3698__auto____3466;
      var x__3468 = f.call(null, cljs.core.first.call(null, s__3467));
      if(cljs.core.truth_(x__3468 === null)) {
        return keep.call(null, f, cljs.core.rest.call(null, s__3467))
      }else {
        return cljs.core.cons.call(null, x__3468, keep.call(null, f, cljs.core.rest.call(null, s__3467)))
      }
    }else {
      return null
    }
  })
};
cljs.core.keep_indexed = function keep_indexed(f, coll) {
  var keepi__3478 = function kpi(idx, coll) {
    return new cljs.core.LazySeq(null, false, function() {
      var temp__3698__auto____3475 = cljs.core.seq.call(null, coll);
      if(cljs.core.truth_(temp__3698__auto____3475)) {
        var s__3476 = temp__3698__auto____3475;
        var x__3477 = f.call(null, idx, cljs.core.first.call(null, s__3476));
        if(cljs.core.truth_(x__3477 === null)) {
          return kpi.call(null, idx + 1, cljs.core.rest.call(null, s__3476))
        }else {
          return cljs.core.cons.call(null, x__3477, kpi.call(null, idx + 1, cljs.core.rest.call(null, s__3476)))
        }
      }else {
        return null
      }
    })
  };
  return keepi__3478.call(null, 0, coll)
};
cljs.core.every_pred = function() {
  var every_pred = null;
  var every_pred__3523 = function(p) {
    return function() {
      var ep1 = null;
      var ep1__3528 = function() {
        return true
      };
      var ep1__3529 = function(x) {
        return cljs.core.boolean$.call(null, p.call(null, x))
      };
      var ep1__3530 = function(x, y) {
        return cljs.core.boolean$.call(null, function() {
          var and__3546__auto____3485 = p.call(null, x);
          if(cljs.core.truth_(and__3546__auto____3485)) {
            return p.call(null, y)
          }else {
            return and__3546__auto____3485
          }
        }())
      };
      var ep1__3531 = function(x, y, z) {
        return cljs.core.boolean$.call(null, function() {
          var and__3546__auto____3486 = p.call(null, x);
          if(cljs.core.truth_(and__3546__auto____3486)) {
            var and__3546__auto____3487 = p.call(null, y);
            if(cljs.core.truth_(and__3546__auto____3487)) {
              return p.call(null, z)
            }else {
              return and__3546__auto____3487
            }
          }else {
            return and__3546__auto____3486
          }
        }())
      };
      var ep1__3532 = function() {
        var G__3534__delegate = function(x, y, z, args) {
          return cljs.core.boolean$.call(null, function() {
            var and__3546__auto____3488 = ep1.call(null, x, y, z);
            if(cljs.core.truth_(and__3546__auto____3488)) {
              return cljs.core.every_QMARK_.call(null, p, args)
            }else {
              return and__3546__auto____3488
            }
          }())
        };
        var G__3534 = function(x, y, z, var_args) {
          var args = null;
          if(goog.isDef(var_args)) {
            args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
          }
          return G__3534__delegate.call(this, x, y, z, args)
        };
        G__3534.cljs$lang$maxFixedArity = 3;
        G__3534.cljs$lang$applyTo = function(arglist__3535) {
          var x = cljs.core.first(arglist__3535);
          var y = cljs.core.first(cljs.core.next(arglist__3535));
          var z = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3535)));
          var args = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3535)));
          return G__3534__delegate.call(this, x, y, z, args)
        };
        return G__3534
      }();
      ep1 = function(x, y, z, var_args) {
        var args = var_args;
        switch(arguments.length) {
          case 0:
            return ep1__3528.call(this);
          case 1:
            return ep1__3529.call(this, x);
          case 2:
            return ep1__3530.call(this, x, y);
          case 3:
            return ep1__3531.call(this, x, y, z);
          default:
            return ep1__3532.apply(this, arguments)
        }
        throw"Invalid arity: " + arguments.length;
      };
      ep1.cljs$lang$maxFixedArity = 3;
      ep1.cljs$lang$applyTo = ep1__3532.cljs$lang$applyTo;
      return ep1
    }()
  };
  var every_pred__3524 = function(p1, p2) {
    return function() {
      var ep2 = null;
      var ep2__3536 = function() {
        return true
      };
      var ep2__3537 = function(x) {
        return cljs.core.boolean$.call(null, function() {
          var and__3546__auto____3489 = p1.call(null, x);
          if(cljs.core.truth_(and__3546__auto____3489)) {
            return p2.call(null, x)
          }else {
            return and__3546__auto____3489
          }
        }())
      };
      var ep2__3538 = function(x, y) {
        return cljs.core.boolean$.call(null, function() {
          var and__3546__auto____3490 = p1.call(null, x);
          if(cljs.core.truth_(and__3546__auto____3490)) {
            var and__3546__auto____3491 = p1.call(null, y);
            if(cljs.core.truth_(and__3546__auto____3491)) {
              var and__3546__auto____3492 = p2.call(null, x);
              if(cljs.core.truth_(and__3546__auto____3492)) {
                return p2.call(null, y)
              }else {
                return and__3546__auto____3492
              }
            }else {
              return and__3546__auto____3491
            }
          }else {
            return and__3546__auto____3490
          }
        }())
      };
      var ep2__3539 = function(x, y, z) {
        return cljs.core.boolean$.call(null, function() {
          var and__3546__auto____3493 = p1.call(null, x);
          if(cljs.core.truth_(and__3546__auto____3493)) {
            var and__3546__auto____3494 = p1.call(null, y);
            if(cljs.core.truth_(and__3546__auto____3494)) {
              var and__3546__auto____3495 = p1.call(null, z);
              if(cljs.core.truth_(and__3546__auto____3495)) {
                var and__3546__auto____3496 = p2.call(null, x);
                if(cljs.core.truth_(and__3546__auto____3496)) {
                  var and__3546__auto____3497 = p2.call(null, y);
                  if(cljs.core.truth_(and__3546__auto____3497)) {
                    return p2.call(null, z)
                  }else {
                    return and__3546__auto____3497
                  }
                }else {
                  return and__3546__auto____3496
                }
              }else {
                return and__3546__auto____3495
              }
            }else {
              return and__3546__auto____3494
            }
          }else {
            return and__3546__auto____3493
          }
        }())
      };
      var ep2__3540 = function() {
        var G__3542__delegate = function(x, y, z, args) {
          return cljs.core.boolean$.call(null, function() {
            var and__3546__auto____3498 = ep2.call(null, x, y, z);
            if(cljs.core.truth_(and__3546__auto____3498)) {
              return cljs.core.every_QMARK_.call(null, function(p1__3469_SHARP_) {
                var and__3546__auto____3499 = p1.call(null, p1__3469_SHARP_);
                if(cljs.core.truth_(and__3546__auto____3499)) {
                  return p2.call(null, p1__3469_SHARP_)
                }else {
                  return and__3546__auto____3499
                }
              }, args)
            }else {
              return and__3546__auto____3498
            }
          }())
        };
        var G__3542 = function(x, y, z, var_args) {
          var args = null;
          if(goog.isDef(var_args)) {
            args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
          }
          return G__3542__delegate.call(this, x, y, z, args)
        };
        G__3542.cljs$lang$maxFixedArity = 3;
        G__3542.cljs$lang$applyTo = function(arglist__3543) {
          var x = cljs.core.first(arglist__3543);
          var y = cljs.core.first(cljs.core.next(arglist__3543));
          var z = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3543)));
          var args = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3543)));
          return G__3542__delegate.call(this, x, y, z, args)
        };
        return G__3542
      }();
      ep2 = function(x, y, z, var_args) {
        var args = var_args;
        switch(arguments.length) {
          case 0:
            return ep2__3536.call(this);
          case 1:
            return ep2__3537.call(this, x);
          case 2:
            return ep2__3538.call(this, x, y);
          case 3:
            return ep2__3539.call(this, x, y, z);
          default:
            return ep2__3540.apply(this, arguments)
        }
        throw"Invalid arity: " + arguments.length;
      };
      ep2.cljs$lang$maxFixedArity = 3;
      ep2.cljs$lang$applyTo = ep2__3540.cljs$lang$applyTo;
      return ep2
    }()
  };
  var every_pred__3525 = function(p1, p2, p3) {
    return function() {
      var ep3 = null;
      var ep3__3544 = function() {
        return true
      };
      var ep3__3545 = function(x) {
        return cljs.core.boolean$.call(null, function() {
          var and__3546__auto____3500 = p1.call(null, x);
          if(cljs.core.truth_(and__3546__auto____3500)) {
            var and__3546__auto____3501 = p2.call(null, x);
            if(cljs.core.truth_(and__3546__auto____3501)) {
              return p3.call(null, x)
            }else {
              return and__3546__auto____3501
            }
          }else {
            return and__3546__auto____3500
          }
        }())
      };
      var ep3__3546 = function(x, y) {
        return cljs.core.boolean$.call(null, function() {
          var and__3546__auto____3502 = p1.call(null, x);
          if(cljs.core.truth_(and__3546__auto____3502)) {
            var and__3546__auto____3503 = p2.call(null, x);
            if(cljs.core.truth_(and__3546__auto____3503)) {
              var and__3546__auto____3504 = p3.call(null, x);
              if(cljs.core.truth_(and__3546__auto____3504)) {
                var and__3546__auto____3505 = p1.call(null, y);
                if(cljs.core.truth_(and__3546__auto____3505)) {
                  var and__3546__auto____3506 = p2.call(null, y);
                  if(cljs.core.truth_(and__3546__auto____3506)) {
                    return p3.call(null, y)
                  }else {
                    return and__3546__auto____3506
                  }
                }else {
                  return and__3546__auto____3505
                }
              }else {
                return and__3546__auto____3504
              }
            }else {
              return and__3546__auto____3503
            }
          }else {
            return and__3546__auto____3502
          }
        }())
      };
      var ep3__3547 = function(x, y, z) {
        return cljs.core.boolean$.call(null, function() {
          var and__3546__auto____3507 = p1.call(null, x);
          if(cljs.core.truth_(and__3546__auto____3507)) {
            var and__3546__auto____3508 = p2.call(null, x);
            if(cljs.core.truth_(and__3546__auto____3508)) {
              var and__3546__auto____3509 = p3.call(null, x);
              if(cljs.core.truth_(and__3546__auto____3509)) {
                var and__3546__auto____3510 = p1.call(null, y);
                if(cljs.core.truth_(and__3546__auto____3510)) {
                  var and__3546__auto____3511 = p2.call(null, y);
                  if(cljs.core.truth_(and__3546__auto____3511)) {
                    var and__3546__auto____3512 = p3.call(null, y);
                    if(cljs.core.truth_(and__3546__auto____3512)) {
                      var and__3546__auto____3513 = p1.call(null, z);
                      if(cljs.core.truth_(and__3546__auto____3513)) {
                        var and__3546__auto____3514 = p2.call(null, z);
                        if(cljs.core.truth_(and__3546__auto____3514)) {
                          return p3.call(null, z)
                        }else {
                          return and__3546__auto____3514
                        }
                      }else {
                        return and__3546__auto____3513
                      }
                    }else {
                      return and__3546__auto____3512
                    }
                  }else {
                    return and__3546__auto____3511
                  }
                }else {
                  return and__3546__auto____3510
                }
              }else {
                return and__3546__auto____3509
              }
            }else {
              return and__3546__auto____3508
            }
          }else {
            return and__3546__auto____3507
          }
        }())
      };
      var ep3__3548 = function() {
        var G__3550__delegate = function(x, y, z, args) {
          return cljs.core.boolean$.call(null, function() {
            var and__3546__auto____3515 = ep3.call(null, x, y, z);
            if(cljs.core.truth_(and__3546__auto____3515)) {
              return cljs.core.every_QMARK_.call(null, function(p1__3470_SHARP_) {
                var and__3546__auto____3516 = p1.call(null, p1__3470_SHARP_);
                if(cljs.core.truth_(and__3546__auto____3516)) {
                  var and__3546__auto____3517 = p2.call(null, p1__3470_SHARP_);
                  if(cljs.core.truth_(and__3546__auto____3517)) {
                    return p3.call(null, p1__3470_SHARP_)
                  }else {
                    return and__3546__auto____3517
                  }
                }else {
                  return and__3546__auto____3516
                }
              }, args)
            }else {
              return and__3546__auto____3515
            }
          }())
        };
        var G__3550 = function(x, y, z, var_args) {
          var args = null;
          if(goog.isDef(var_args)) {
            args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
          }
          return G__3550__delegate.call(this, x, y, z, args)
        };
        G__3550.cljs$lang$maxFixedArity = 3;
        G__3550.cljs$lang$applyTo = function(arglist__3551) {
          var x = cljs.core.first(arglist__3551);
          var y = cljs.core.first(cljs.core.next(arglist__3551));
          var z = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3551)));
          var args = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3551)));
          return G__3550__delegate.call(this, x, y, z, args)
        };
        return G__3550
      }();
      ep3 = function(x, y, z, var_args) {
        var args = var_args;
        switch(arguments.length) {
          case 0:
            return ep3__3544.call(this);
          case 1:
            return ep3__3545.call(this, x);
          case 2:
            return ep3__3546.call(this, x, y);
          case 3:
            return ep3__3547.call(this, x, y, z);
          default:
            return ep3__3548.apply(this, arguments)
        }
        throw"Invalid arity: " + arguments.length;
      };
      ep3.cljs$lang$maxFixedArity = 3;
      ep3.cljs$lang$applyTo = ep3__3548.cljs$lang$applyTo;
      return ep3
    }()
  };
  var every_pred__3526 = function() {
    var G__3552__delegate = function(p1, p2, p3, ps) {
      var ps__3518 = cljs.core.list_STAR_.call(null, p1, p2, p3, ps);
      return function() {
        var epn = null;
        var epn__3553 = function() {
          return true
        };
        var epn__3554 = function(x) {
          return cljs.core.every_QMARK_.call(null, function(p1__3471_SHARP_) {
            return p1__3471_SHARP_.call(null, x)
          }, ps__3518)
        };
        var epn__3555 = function(x, y) {
          return cljs.core.every_QMARK_.call(null, function(p1__3472_SHARP_) {
            var and__3546__auto____3519 = p1__3472_SHARP_.call(null, x);
            if(cljs.core.truth_(and__3546__auto____3519)) {
              return p1__3472_SHARP_.call(null, y)
            }else {
              return and__3546__auto____3519
            }
          }, ps__3518)
        };
        var epn__3556 = function(x, y, z) {
          return cljs.core.every_QMARK_.call(null, function(p1__3473_SHARP_) {
            var and__3546__auto____3520 = p1__3473_SHARP_.call(null, x);
            if(cljs.core.truth_(and__3546__auto____3520)) {
              var and__3546__auto____3521 = p1__3473_SHARP_.call(null, y);
              if(cljs.core.truth_(and__3546__auto____3521)) {
                return p1__3473_SHARP_.call(null, z)
              }else {
                return and__3546__auto____3521
              }
            }else {
              return and__3546__auto____3520
            }
          }, ps__3518)
        };
        var epn__3557 = function() {
          var G__3559__delegate = function(x, y, z, args) {
            return cljs.core.boolean$.call(null, function() {
              var and__3546__auto____3522 = epn.call(null, x, y, z);
              if(cljs.core.truth_(and__3546__auto____3522)) {
                return cljs.core.every_QMARK_.call(null, function(p1__3474_SHARP_) {
                  return cljs.core.every_QMARK_.call(null, p1__3474_SHARP_, args)
                }, ps__3518)
              }else {
                return and__3546__auto____3522
              }
            }())
          };
          var G__3559 = function(x, y, z, var_args) {
            var args = null;
            if(goog.isDef(var_args)) {
              args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
            }
            return G__3559__delegate.call(this, x, y, z, args)
          };
          G__3559.cljs$lang$maxFixedArity = 3;
          G__3559.cljs$lang$applyTo = function(arglist__3560) {
            var x = cljs.core.first(arglist__3560);
            var y = cljs.core.first(cljs.core.next(arglist__3560));
            var z = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3560)));
            var args = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3560)));
            return G__3559__delegate.call(this, x, y, z, args)
          };
          return G__3559
        }();
        epn = function(x, y, z, var_args) {
          var args = var_args;
          switch(arguments.length) {
            case 0:
              return epn__3553.call(this);
            case 1:
              return epn__3554.call(this, x);
            case 2:
              return epn__3555.call(this, x, y);
            case 3:
              return epn__3556.call(this, x, y, z);
            default:
              return epn__3557.apply(this, arguments)
          }
          throw"Invalid arity: " + arguments.length;
        };
        epn.cljs$lang$maxFixedArity = 3;
        epn.cljs$lang$applyTo = epn__3557.cljs$lang$applyTo;
        return epn
      }()
    };
    var G__3552 = function(p1, p2, p3, var_args) {
      var ps = null;
      if(goog.isDef(var_args)) {
        ps = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
      }
      return G__3552__delegate.call(this, p1, p2, p3, ps)
    };
    G__3552.cljs$lang$maxFixedArity = 3;
    G__3552.cljs$lang$applyTo = function(arglist__3561) {
      var p1 = cljs.core.first(arglist__3561);
      var p2 = cljs.core.first(cljs.core.next(arglist__3561));
      var p3 = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3561)));
      var ps = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3561)));
      return G__3552__delegate.call(this, p1, p2, p3, ps)
    };
    return G__3552
  }();
  every_pred = function(p1, p2, p3, var_args) {
    var ps = var_args;
    switch(arguments.length) {
      case 1:
        return every_pred__3523.call(this, p1);
      case 2:
        return every_pred__3524.call(this, p1, p2);
      case 3:
        return every_pred__3525.call(this, p1, p2, p3);
      default:
        return every_pred__3526.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  every_pred.cljs$lang$maxFixedArity = 3;
  every_pred.cljs$lang$applyTo = every_pred__3526.cljs$lang$applyTo;
  return every_pred
}();
cljs.core.some_fn = function() {
  var some_fn = null;
  var some_fn__3601 = function(p) {
    return function() {
      var sp1 = null;
      var sp1__3606 = function() {
        return null
      };
      var sp1__3607 = function(x) {
        return p.call(null, x)
      };
      var sp1__3608 = function(x, y) {
        var or__3548__auto____3563 = p.call(null, x);
        if(cljs.core.truth_(or__3548__auto____3563)) {
          return or__3548__auto____3563
        }else {
          return p.call(null, y)
        }
      };
      var sp1__3609 = function(x, y, z) {
        var or__3548__auto____3564 = p.call(null, x);
        if(cljs.core.truth_(or__3548__auto____3564)) {
          return or__3548__auto____3564
        }else {
          var or__3548__auto____3565 = p.call(null, y);
          if(cljs.core.truth_(or__3548__auto____3565)) {
            return or__3548__auto____3565
          }else {
            return p.call(null, z)
          }
        }
      };
      var sp1__3610 = function() {
        var G__3612__delegate = function(x, y, z, args) {
          var or__3548__auto____3566 = sp1.call(null, x, y, z);
          if(cljs.core.truth_(or__3548__auto____3566)) {
            return or__3548__auto____3566
          }else {
            return cljs.core.some.call(null, p, args)
          }
        };
        var G__3612 = function(x, y, z, var_args) {
          var args = null;
          if(goog.isDef(var_args)) {
            args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
          }
          return G__3612__delegate.call(this, x, y, z, args)
        };
        G__3612.cljs$lang$maxFixedArity = 3;
        G__3612.cljs$lang$applyTo = function(arglist__3613) {
          var x = cljs.core.first(arglist__3613);
          var y = cljs.core.first(cljs.core.next(arglist__3613));
          var z = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3613)));
          var args = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3613)));
          return G__3612__delegate.call(this, x, y, z, args)
        };
        return G__3612
      }();
      sp1 = function(x, y, z, var_args) {
        var args = var_args;
        switch(arguments.length) {
          case 0:
            return sp1__3606.call(this);
          case 1:
            return sp1__3607.call(this, x);
          case 2:
            return sp1__3608.call(this, x, y);
          case 3:
            return sp1__3609.call(this, x, y, z);
          default:
            return sp1__3610.apply(this, arguments)
        }
        throw"Invalid arity: " + arguments.length;
      };
      sp1.cljs$lang$maxFixedArity = 3;
      sp1.cljs$lang$applyTo = sp1__3610.cljs$lang$applyTo;
      return sp1
    }()
  };
  var some_fn__3602 = function(p1, p2) {
    return function() {
      var sp2 = null;
      var sp2__3614 = function() {
        return null
      };
      var sp2__3615 = function(x) {
        var or__3548__auto____3567 = p1.call(null, x);
        if(cljs.core.truth_(or__3548__auto____3567)) {
          return or__3548__auto____3567
        }else {
          return p2.call(null, x)
        }
      };
      var sp2__3616 = function(x, y) {
        var or__3548__auto____3568 = p1.call(null, x);
        if(cljs.core.truth_(or__3548__auto____3568)) {
          return or__3548__auto____3568
        }else {
          var or__3548__auto____3569 = p1.call(null, y);
          if(cljs.core.truth_(or__3548__auto____3569)) {
            return or__3548__auto____3569
          }else {
            var or__3548__auto____3570 = p2.call(null, x);
            if(cljs.core.truth_(or__3548__auto____3570)) {
              return or__3548__auto____3570
            }else {
              return p2.call(null, y)
            }
          }
        }
      };
      var sp2__3617 = function(x, y, z) {
        var or__3548__auto____3571 = p1.call(null, x);
        if(cljs.core.truth_(or__3548__auto____3571)) {
          return or__3548__auto____3571
        }else {
          var or__3548__auto____3572 = p1.call(null, y);
          if(cljs.core.truth_(or__3548__auto____3572)) {
            return or__3548__auto____3572
          }else {
            var or__3548__auto____3573 = p1.call(null, z);
            if(cljs.core.truth_(or__3548__auto____3573)) {
              return or__3548__auto____3573
            }else {
              var or__3548__auto____3574 = p2.call(null, x);
              if(cljs.core.truth_(or__3548__auto____3574)) {
                return or__3548__auto____3574
              }else {
                var or__3548__auto____3575 = p2.call(null, y);
                if(cljs.core.truth_(or__3548__auto____3575)) {
                  return or__3548__auto____3575
                }else {
                  return p2.call(null, z)
                }
              }
            }
          }
        }
      };
      var sp2__3618 = function() {
        var G__3620__delegate = function(x, y, z, args) {
          var or__3548__auto____3576 = sp2.call(null, x, y, z);
          if(cljs.core.truth_(or__3548__auto____3576)) {
            return or__3548__auto____3576
          }else {
            return cljs.core.some.call(null, function(p1__3479_SHARP_) {
              var or__3548__auto____3577 = p1.call(null, p1__3479_SHARP_);
              if(cljs.core.truth_(or__3548__auto____3577)) {
                return or__3548__auto____3577
              }else {
                return p2.call(null, p1__3479_SHARP_)
              }
            }, args)
          }
        };
        var G__3620 = function(x, y, z, var_args) {
          var args = null;
          if(goog.isDef(var_args)) {
            args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
          }
          return G__3620__delegate.call(this, x, y, z, args)
        };
        G__3620.cljs$lang$maxFixedArity = 3;
        G__3620.cljs$lang$applyTo = function(arglist__3621) {
          var x = cljs.core.first(arglist__3621);
          var y = cljs.core.first(cljs.core.next(arglist__3621));
          var z = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3621)));
          var args = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3621)));
          return G__3620__delegate.call(this, x, y, z, args)
        };
        return G__3620
      }();
      sp2 = function(x, y, z, var_args) {
        var args = var_args;
        switch(arguments.length) {
          case 0:
            return sp2__3614.call(this);
          case 1:
            return sp2__3615.call(this, x);
          case 2:
            return sp2__3616.call(this, x, y);
          case 3:
            return sp2__3617.call(this, x, y, z);
          default:
            return sp2__3618.apply(this, arguments)
        }
        throw"Invalid arity: " + arguments.length;
      };
      sp2.cljs$lang$maxFixedArity = 3;
      sp2.cljs$lang$applyTo = sp2__3618.cljs$lang$applyTo;
      return sp2
    }()
  };
  var some_fn__3603 = function(p1, p2, p3) {
    return function() {
      var sp3 = null;
      var sp3__3622 = function() {
        return null
      };
      var sp3__3623 = function(x) {
        var or__3548__auto____3578 = p1.call(null, x);
        if(cljs.core.truth_(or__3548__auto____3578)) {
          return or__3548__auto____3578
        }else {
          var or__3548__auto____3579 = p2.call(null, x);
          if(cljs.core.truth_(or__3548__auto____3579)) {
            return or__3548__auto____3579
          }else {
            return p3.call(null, x)
          }
        }
      };
      var sp3__3624 = function(x, y) {
        var or__3548__auto____3580 = p1.call(null, x);
        if(cljs.core.truth_(or__3548__auto____3580)) {
          return or__3548__auto____3580
        }else {
          var or__3548__auto____3581 = p2.call(null, x);
          if(cljs.core.truth_(or__3548__auto____3581)) {
            return or__3548__auto____3581
          }else {
            var or__3548__auto____3582 = p3.call(null, x);
            if(cljs.core.truth_(or__3548__auto____3582)) {
              return or__3548__auto____3582
            }else {
              var or__3548__auto____3583 = p1.call(null, y);
              if(cljs.core.truth_(or__3548__auto____3583)) {
                return or__3548__auto____3583
              }else {
                var or__3548__auto____3584 = p2.call(null, y);
                if(cljs.core.truth_(or__3548__auto____3584)) {
                  return or__3548__auto____3584
                }else {
                  return p3.call(null, y)
                }
              }
            }
          }
        }
      };
      var sp3__3625 = function(x, y, z) {
        var or__3548__auto____3585 = p1.call(null, x);
        if(cljs.core.truth_(or__3548__auto____3585)) {
          return or__3548__auto____3585
        }else {
          var or__3548__auto____3586 = p2.call(null, x);
          if(cljs.core.truth_(or__3548__auto____3586)) {
            return or__3548__auto____3586
          }else {
            var or__3548__auto____3587 = p3.call(null, x);
            if(cljs.core.truth_(or__3548__auto____3587)) {
              return or__3548__auto____3587
            }else {
              var or__3548__auto____3588 = p1.call(null, y);
              if(cljs.core.truth_(or__3548__auto____3588)) {
                return or__3548__auto____3588
              }else {
                var or__3548__auto____3589 = p2.call(null, y);
                if(cljs.core.truth_(or__3548__auto____3589)) {
                  return or__3548__auto____3589
                }else {
                  var or__3548__auto____3590 = p3.call(null, y);
                  if(cljs.core.truth_(or__3548__auto____3590)) {
                    return or__3548__auto____3590
                  }else {
                    var or__3548__auto____3591 = p1.call(null, z);
                    if(cljs.core.truth_(or__3548__auto____3591)) {
                      return or__3548__auto____3591
                    }else {
                      var or__3548__auto____3592 = p2.call(null, z);
                      if(cljs.core.truth_(or__3548__auto____3592)) {
                        return or__3548__auto____3592
                      }else {
                        return p3.call(null, z)
                      }
                    }
                  }
                }
              }
            }
          }
        }
      };
      var sp3__3626 = function() {
        var G__3628__delegate = function(x, y, z, args) {
          var or__3548__auto____3593 = sp3.call(null, x, y, z);
          if(cljs.core.truth_(or__3548__auto____3593)) {
            return or__3548__auto____3593
          }else {
            return cljs.core.some.call(null, function(p1__3480_SHARP_) {
              var or__3548__auto____3594 = p1.call(null, p1__3480_SHARP_);
              if(cljs.core.truth_(or__3548__auto____3594)) {
                return or__3548__auto____3594
              }else {
                var or__3548__auto____3595 = p2.call(null, p1__3480_SHARP_);
                if(cljs.core.truth_(or__3548__auto____3595)) {
                  return or__3548__auto____3595
                }else {
                  return p3.call(null, p1__3480_SHARP_)
                }
              }
            }, args)
          }
        };
        var G__3628 = function(x, y, z, var_args) {
          var args = null;
          if(goog.isDef(var_args)) {
            args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
          }
          return G__3628__delegate.call(this, x, y, z, args)
        };
        G__3628.cljs$lang$maxFixedArity = 3;
        G__3628.cljs$lang$applyTo = function(arglist__3629) {
          var x = cljs.core.first(arglist__3629);
          var y = cljs.core.first(cljs.core.next(arglist__3629));
          var z = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3629)));
          var args = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3629)));
          return G__3628__delegate.call(this, x, y, z, args)
        };
        return G__3628
      }();
      sp3 = function(x, y, z, var_args) {
        var args = var_args;
        switch(arguments.length) {
          case 0:
            return sp3__3622.call(this);
          case 1:
            return sp3__3623.call(this, x);
          case 2:
            return sp3__3624.call(this, x, y);
          case 3:
            return sp3__3625.call(this, x, y, z);
          default:
            return sp3__3626.apply(this, arguments)
        }
        throw"Invalid arity: " + arguments.length;
      };
      sp3.cljs$lang$maxFixedArity = 3;
      sp3.cljs$lang$applyTo = sp3__3626.cljs$lang$applyTo;
      return sp3
    }()
  };
  var some_fn__3604 = function() {
    var G__3630__delegate = function(p1, p2, p3, ps) {
      var ps__3596 = cljs.core.list_STAR_.call(null, p1, p2, p3, ps);
      return function() {
        var spn = null;
        var spn__3631 = function() {
          return null
        };
        var spn__3632 = function(x) {
          return cljs.core.some.call(null, function(p1__3481_SHARP_) {
            return p1__3481_SHARP_.call(null, x)
          }, ps__3596)
        };
        var spn__3633 = function(x, y) {
          return cljs.core.some.call(null, function(p1__3482_SHARP_) {
            var or__3548__auto____3597 = p1__3482_SHARP_.call(null, x);
            if(cljs.core.truth_(or__3548__auto____3597)) {
              return or__3548__auto____3597
            }else {
              return p1__3482_SHARP_.call(null, y)
            }
          }, ps__3596)
        };
        var spn__3634 = function(x, y, z) {
          return cljs.core.some.call(null, function(p1__3483_SHARP_) {
            var or__3548__auto____3598 = p1__3483_SHARP_.call(null, x);
            if(cljs.core.truth_(or__3548__auto____3598)) {
              return or__3548__auto____3598
            }else {
              var or__3548__auto____3599 = p1__3483_SHARP_.call(null, y);
              if(cljs.core.truth_(or__3548__auto____3599)) {
                return or__3548__auto____3599
              }else {
                return p1__3483_SHARP_.call(null, z)
              }
            }
          }, ps__3596)
        };
        var spn__3635 = function() {
          var G__3637__delegate = function(x, y, z, args) {
            var or__3548__auto____3600 = spn.call(null, x, y, z);
            if(cljs.core.truth_(or__3548__auto____3600)) {
              return or__3548__auto____3600
            }else {
              return cljs.core.some.call(null, function(p1__3484_SHARP_) {
                return cljs.core.some.call(null, p1__3484_SHARP_, args)
              }, ps__3596)
            }
          };
          var G__3637 = function(x, y, z, var_args) {
            var args = null;
            if(goog.isDef(var_args)) {
              args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
            }
            return G__3637__delegate.call(this, x, y, z, args)
          };
          G__3637.cljs$lang$maxFixedArity = 3;
          G__3637.cljs$lang$applyTo = function(arglist__3638) {
            var x = cljs.core.first(arglist__3638);
            var y = cljs.core.first(cljs.core.next(arglist__3638));
            var z = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3638)));
            var args = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3638)));
            return G__3637__delegate.call(this, x, y, z, args)
          };
          return G__3637
        }();
        spn = function(x, y, z, var_args) {
          var args = var_args;
          switch(arguments.length) {
            case 0:
              return spn__3631.call(this);
            case 1:
              return spn__3632.call(this, x);
            case 2:
              return spn__3633.call(this, x, y);
            case 3:
              return spn__3634.call(this, x, y, z);
            default:
              return spn__3635.apply(this, arguments)
          }
          throw"Invalid arity: " + arguments.length;
        };
        spn.cljs$lang$maxFixedArity = 3;
        spn.cljs$lang$applyTo = spn__3635.cljs$lang$applyTo;
        return spn
      }()
    };
    var G__3630 = function(p1, p2, p3, var_args) {
      var ps = null;
      if(goog.isDef(var_args)) {
        ps = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
      }
      return G__3630__delegate.call(this, p1, p2, p3, ps)
    };
    G__3630.cljs$lang$maxFixedArity = 3;
    G__3630.cljs$lang$applyTo = function(arglist__3639) {
      var p1 = cljs.core.first(arglist__3639);
      var p2 = cljs.core.first(cljs.core.next(arglist__3639));
      var p3 = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3639)));
      var ps = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3639)));
      return G__3630__delegate.call(this, p1, p2, p3, ps)
    };
    return G__3630
  }();
  some_fn = function(p1, p2, p3, var_args) {
    var ps = var_args;
    switch(arguments.length) {
      case 1:
        return some_fn__3601.call(this, p1);
      case 2:
        return some_fn__3602.call(this, p1, p2);
      case 3:
        return some_fn__3603.call(this, p1, p2, p3);
      default:
        return some_fn__3604.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  some_fn.cljs$lang$maxFixedArity = 3;
  some_fn.cljs$lang$applyTo = some_fn__3604.cljs$lang$applyTo;
  return some_fn
}();
cljs.core.map = function() {
  var map = null;
  var map__3652 = function(f, coll) {
    return new cljs.core.LazySeq(null, false, function() {
      var temp__3698__auto____3640 = cljs.core.seq.call(null, coll);
      if(cljs.core.truth_(temp__3698__auto____3640)) {
        var s__3641 = temp__3698__auto____3640;
        return cljs.core.cons.call(null, f.call(null, cljs.core.first.call(null, s__3641)), map.call(null, f, cljs.core.rest.call(null, s__3641)))
      }else {
        return null
      }
    })
  };
  var map__3653 = function(f, c1, c2) {
    return new cljs.core.LazySeq(null, false, function() {
      var s1__3642 = cljs.core.seq.call(null, c1);
      var s2__3643 = cljs.core.seq.call(null, c2);
      if(cljs.core.truth_(function() {
        var and__3546__auto____3644 = s1__3642;
        if(cljs.core.truth_(and__3546__auto____3644)) {
          return s2__3643
        }else {
          return and__3546__auto____3644
        }
      }())) {
        return cljs.core.cons.call(null, f.call(null, cljs.core.first.call(null, s1__3642), cljs.core.first.call(null, s2__3643)), map.call(null, f, cljs.core.rest.call(null, s1__3642), cljs.core.rest.call(null, s2__3643)))
      }else {
        return null
      }
    })
  };
  var map__3654 = function(f, c1, c2, c3) {
    return new cljs.core.LazySeq(null, false, function() {
      var s1__3645 = cljs.core.seq.call(null, c1);
      var s2__3646 = cljs.core.seq.call(null, c2);
      var s3__3647 = cljs.core.seq.call(null, c3);
      if(cljs.core.truth_(function() {
        var and__3546__auto____3648 = s1__3645;
        if(cljs.core.truth_(and__3546__auto____3648)) {
          var and__3546__auto____3649 = s2__3646;
          if(cljs.core.truth_(and__3546__auto____3649)) {
            return s3__3647
          }else {
            return and__3546__auto____3649
          }
        }else {
          return and__3546__auto____3648
        }
      }())) {
        return cljs.core.cons.call(null, f.call(null, cljs.core.first.call(null, s1__3645), cljs.core.first.call(null, s2__3646), cljs.core.first.call(null, s3__3647)), map.call(null, f, cljs.core.rest.call(null, s1__3645), cljs.core.rest.call(null, s2__3646), cljs.core.rest.call(null, s3__3647)))
      }else {
        return null
      }
    })
  };
  var map__3655 = function() {
    var G__3657__delegate = function(f, c1, c2, c3, colls) {
      var step__3651 = function step(cs) {
        return new cljs.core.LazySeq(null, false, function() {
          var ss__3650 = map.call(null, cljs.core.seq, cs);
          if(cljs.core.truth_(cljs.core.every_QMARK_.call(null, cljs.core.identity, ss__3650))) {
            return cljs.core.cons.call(null, map.call(null, cljs.core.first, ss__3650), step.call(null, map.call(null, cljs.core.rest, ss__3650)))
          }else {
            return null
          }
        })
      };
      return map.call(null, function(p1__3562_SHARP_) {
        return cljs.core.apply.call(null, f, p1__3562_SHARP_)
      }, step__3651.call(null, cljs.core.conj.call(null, colls, c3, c2, c1)))
    };
    var G__3657 = function(f, c1, c2, c3, var_args) {
      var colls = null;
      if(goog.isDef(var_args)) {
        colls = cljs.core.array_seq(Array.prototype.slice.call(arguments, 4), 0)
      }
      return G__3657__delegate.call(this, f, c1, c2, c3, colls)
    };
    G__3657.cljs$lang$maxFixedArity = 4;
    G__3657.cljs$lang$applyTo = function(arglist__3658) {
      var f = cljs.core.first(arglist__3658);
      var c1 = cljs.core.first(cljs.core.next(arglist__3658));
      var c2 = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3658)));
      var c3 = cljs.core.first(cljs.core.next(cljs.core.next(cljs.core.next(arglist__3658))));
      var colls = cljs.core.rest(cljs.core.next(cljs.core.next(cljs.core.next(arglist__3658))));
      return G__3657__delegate.call(this, f, c1, c2, c3, colls)
    };
    return G__3657
  }();
  map = function(f, c1, c2, c3, var_args) {
    var colls = var_args;
    switch(arguments.length) {
      case 2:
        return map__3652.call(this, f, c1);
      case 3:
        return map__3653.call(this, f, c1, c2);
      case 4:
        return map__3654.call(this, f, c1, c2, c3);
      default:
        return map__3655.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  map.cljs$lang$maxFixedArity = 4;
  map.cljs$lang$applyTo = map__3655.cljs$lang$applyTo;
  return map
}();
cljs.core.take = function take(n, coll) {
  return new cljs.core.LazySeq(null, false, function() {
    if(cljs.core.truth_(n > 0)) {
      var temp__3698__auto____3659 = cljs.core.seq.call(null, coll);
      if(cljs.core.truth_(temp__3698__auto____3659)) {
        var s__3660 = temp__3698__auto____3659;
        return cljs.core.cons.call(null, cljs.core.first.call(null, s__3660), take.call(null, n - 1, cljs.core.rest.call(null, s__3660)))
      }else {
        return null
      }
    }else {
      return null
    }
  })
};
cljs.core.drop = function drop(n, coll) {
  var step__3663 = function(n, coll) {
    while(true) {
      var s__3661 = cljs.core.seq.call(null, coll);
      if(cljs.core.truth_(function() {
        var and__3546__auto____3662 = n > 0;
        if(cljs.core.truth_(and__3546__auto____3662)) {
          return s__3661
        }else {
          return and__3546__auto____3662
        }
      }())) {
        var G__3664 = n - 1;
        var G__3665 = cljs.core.rest.call(null, s__3661);
        n = G__3664;
        coll = G__3665;
        continue
      }else {
        return s__3661
      }
      break
    }
  };
  return new cljs.core.LazySeq(null, false, function() {
    return step__3663.call(null, n, coll)
  })
};
cljs.core.drop_last = function() {
  var drop_last = null;
  var drop_last__3666 = function(s) {
    return drop_last.call(null, 1, s)
  };
  var drop_last__3667 = function(n, s) {
    return cljs.core.map.call(null, function(x, _) {
      return x
    }, s, cljs.core.drop.call(null, n, s))
  };
  drop_last = function(n, s) {
    switch(arguments.length) {
      case 1:
        return drop_last__3666.call(this, n);
      case 2:
        return drop_last__3667.call(this, n, s)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return drop_last
}();
cljs.core.take_last = function take_last(n, coll) {
  var s__3669 = cljs.core.seq.call(null, coll);
  var lead__3670 = cljs.core.seq.call(null, cljs.core.drop.call(null, n, coll));
  while(true) {
    if(cljs.core.truth_(lead__3670)) {
      var G__3671 = cljs.core.next.call(null, s__3669);
      var G__3672 = cljs.core.next.call(null, lead__3670);
      s__3669 = G__3671;
      lead__3670 = G__3672;
      continue
    }else {
      return s__3669
    }
    break
  }
};
cljs.core.drop_while = function drop_while(pred, coll) {
  var step__3675 = function(pred, coll) {
    while(true) {
      var s__3673 = cljs.core.seq.call(null, coll);
      if(cljs.core.truth_(function() {
        var and__3546__auto____3674 = s__3673;
        if(cljs.core.truth_(and__3546__auto____3674)) {
          return pred.call(null, cljs.core.first.call(null, s__3673))
        }else {
          return and__3546__auto____3674
        }
      }())) {
        var G__3676 = pred;
        var G__3677 = cljs.core.rest.call(null, s__3673);
        pred = G__3676;
        coll = G__3677;
        continue
      }else {
        return s__3673
      }
      break
    }
  };
  return new cljs.core.LazySeq(null, false, function() {
    return step__3675.call(null, pred, coll)
  })
};
cljs.core.cycle = function cycle(coll) {
  return new cljs.core.LazySeq(null, false, function() {
    var temp__3698__auto____3678 = cljs.core.seq.call(null, coll);
    if(cljs.core.truth_(temp__3698__auto____3678)) {
      var s__3679 = temp__3698__auto____3678;
      return cljs.core.concat.call(null, s__3679, cycle.call(null, s__3679))
    }else {
      return null
    }
  })
};
cljs.core.split_at = function split_at(n, coll) {
  return cljs.core.Vector.fromArray([cljs.core.take.call(null, n, coll), cljs.core.drop.call(null, n, coll)])
};
cljs.core.repeat = function() {
  var repeat = null;
  var repeat__3680 = function(x) {
    return new cljs.core.LazySeq(null, false, function() {
      return cljs.core.cons.call(null, x, repeat.call(null, x))
    })
  };
  var repeat__3681 = function(n, x) {
    return cljs.core.take.call(null, n, repeat.call(null, x))
  };
  repeat = function(n, x) {
    switch(arguments.length) {
      case 1:
        return repeat__3680.call(this, n);
      case 2:
        return repeat__3681.call(this, n, x)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return repeat
}();
cljs.core.replicate = function replicate(n, x) {
  return cljs.core.take.call(null, n, cljs.core.repeat.call(null, x))
};
cljs.core.repeatedly = function() {
  var repeatedly = null;
  var repeatedly__3683 = function(f) {
    return new cljs.core.LazySeq(null, false, function() {
      return cljs.core.cons.call(null, f.call(null), repeatedly.call(null, f))
    })
  };
  var repeatedly__3684 = function(n, f) {
    return cljs.core.take.call(null, n, repeatedly.call(null, f))
  };
  repeatedly = function(n, f) {
    switch(arguments.length) {
      case 1:
        return repeatedly__3683.call(this, n);
      case 2:
        return repeatedly__3684.call(this, n, f)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return repeatedly
}();
cljs.core.iterate = function iterate(f, x) {
  return cljs.core.cons.call(null, x, new cljs.core.LazySeq(null, false, function() {
    return iterate.call(null, f, f.call(null, x))
  }))
};
cljs.core.interleave = function() {
  var interleave = null;
  var interleave__3690 = function(c1, c2) {
    return new cljs.core.LazySeq(null, false, function() {
      var s1__3686 = cljs.core.seq.call(null, c1);
      var s2__3687 = cljs.core.seq.call(null, c2);
      if(cljs.core.truth_(function() {
        var and__3546__auto____3688 = s1__3686;
        if(cljs.core.truth_(and__3546__auto____3688)) {
          return s2__3687
        }else {
          return and__3546__auto____3688
        }
      }())) {
        return cljs.core.cons.call(null, cljs.core.first.call(null, s1__3686), cljs.core.cons.call(null, cljs.core.first.call(null, s2__3687), interleave.call(null, cljs.core.rest.call(null, s1__3686), cljs.core.rest.call(null, s2__3687))))
      }else {
        return null
      }
    })
  };
  var interleave__3691 = function() {
    var G__3693__delegate = function(c1, c2, colls) {
      return new cljs.core.LazySeq(null, false, function() {
        var ss__3689 = cljs.core.map.call(null, cljs.core.seq, cljs.core.conj.call(null, colls, c2, c1));
        if(cljs.core.truth_(cljs.core.every_QMARK_.call(null, cljs.core.identity, ss__3689))) {
          return cljs.core.concat.call(null, cljs.core.map.call(null, cljs.core.first, ss__3689), cljs.core.apply.call(null, interleave, cljs.core.map.call(null, cljs.core.rest, ss__3689)))
        }else {
          return null
        }
      })
    };
    var G__3693 = function(c1, c2, var_args) {
      var colls = null;
      if(goog.isDef(var_args)) {
        colls = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3693__delegate.call(this, c1, c2, colls)
    };
    G__3693.cljs$lang$maxFixedArity = 2;
    G__3693.cljs$lang$applyTo = function(arglist__3694) {
      var c1 = cljs.core.first(arglist__3694);
      var c2 = cljs.core.first(cljs.core.next(arglist__3694));
      var colls = cljs.core.rest(cljs.core.next(arglist__3694));
      return G__3693__delegate.call(this, c1, c2, colls)
    };
    return G__3693
  }();
  interleave = function(c1, c2, var_args) {
    var colls = var_args;
    switch(arguments.length) {
      case 2:
        return interleave__3690.call(this, c1, c2);
      default:
        return interleave__3691.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  interleave.cljs$lang$maxFixedArity = 2;
  interleave.cljs$lang$applyTo = interleave__3691.cljs$lang$applyTo;
  return interleave
}();
cljs.core.interpose = function interpose(sep, coll) {
  return cljs.core.drop.call(null, 1, cljs.core.interleave.call(null, cljs.core.repeat.call(null, sep), coll))
};
cljs.core.flatten1 = function flatten1(colls) {
  var cat__3697 = function cat(coll, colls) {
    return new cljs.core.LazySeq(null, false, function() {
      var temp__3695__auto____3695 = cljs.core.seq.call(null, coll);
      if(cljs.core.truth_(temp__3695__auto____3695)) {
        var coll__3696 = temp__3695__auto____3695;
        return cljs.core.cons.call(null, cljs.core.first.call(null, coll__3696), cat.call(null, cljs.core.rest.call(null, coll__3696), colls))
      }else {
        if(cljs.core.truth_(cljs.core.seq.call(null, colls))) {
          return cat.call(null, cljs.core.first.call(null, colls), cljs.core.rest.call(null, colls))
        }else {
          return null
        }
      }
    })
  };
  return cat__3697.call(null, null, colls)
};
cljs.core.mapcat = function() {
  var mapcat = null;
  var mapcat__3698 = function(f, coll) {
    return cljs.core.flatten1.call(null, cljs.core.map.call(null, f, coll))
  };
  var mapcat__3699 = function() {
    var G__3701__delegate = function(f, coll, colls) {
      return cljs.core.flatten1.call(null, cljs.core.apply.call(null, cljs.core.map, f, coll, colls))
    };
    var G__3701 = function(f, coll, var_args) {
      var colls = null;
      if(goog.isDef(var_args)) {
        colls = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3701__delegate.call(this, f, coll, colls)
    };
    G__3701.cljs$lang$maxFixedArity = 2;
    G__3701.cljs$lang$applyTo = function(arglist__3702) {
      var f = cljs.core.first(arglist__3702);
      var coll = cljs.core.first(cljs.core.next(arglist__3702));
      var colls = cljs.core.rest(cljs.core.next(arglist__3702));
      return G__3701__delegate.call(this, f, coll, colls)
    };
    return G__3701
  }();
  mapcat = function(f, coll, var_args) {
    var colls = var_args;
    switch(arguments.length) {
      case 2:
        return mapcat__3698.call(this, f, coll);
      default:
        return mapcat__3699.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  mapcat.cljs$lang$maxFixedArity = 2;
  mapcat.cljs$lang$applyTo = mapcat__3699.cljs$lang$applyTo;
  return mapcat
}();
cljs.core.filter = function filter(pred, coll) {
  return new cljs.core.LazySeq(null, false, function() {
    var temp__3698__auto____3703 = cljs.core.seq.call(null, coll);
    if(cljs.core.truth_(temp__3698__auto____3703)) {
      var s__3704 = temp__3698__auto____3703;
      var f__3705 = cljs.core.first.call(null, s__3704);
      var r__3706 = cljs.core.rest.call(null, s__3704);
      if(cljs.core.truth_(pred.call(null, f__3705))) {
        return cljs.core.cons.call(null, f__3705, filter.call(null, pred, r__3706))
      }else {
        return filter.call(null, pred, r__3706)
      }
    }else {
      return null
    }
  })
};
cljs.core.remove = function remove(pred, coll) {
  return cljs.core.filter.call(null, cljs.core.complement.call(null, pred), coll)
};
cljs.core.tree_seq = function tree_seq(branch_QMARK_, children, root) {
  var walk__3708 = function walk(node) {
    return new cljs.core.LazySeq(null, false, function() {
      return cljs.core.cons.call(null, node, cljs.core.truth_(branch_QMARK_.call(null, node)) ? cljs.core.mapcat.call(null, walk, children.call(null, node)) : null)
    })
  };
  return walk__3708.call(null, root)
};
cljs.core.flatten = function flatten(x) {
  return cljs.core.filter.call(null, function(p1__3707_SHARP_) {
    return cljs.core.not.call(null, cljs.core.sequential_QMARK_.call(null, p1__3707_SHARP_))
  }, cljs.core.rest.call(null, cljs.core.tree_seq.call(null, cljs.core.sequential_QMARK_, cljs.core.seq, x)))
};
cljs.core.into = function into(to, from) {
  return cljs.core.reduce.call(null, cljs.core._conj, to, from)
};
cljs.core.partition = function() {
  var partition = null;
  var partition__3715 = function(n, coll) {
    return partition.call(null, n, n, coll)
  };
  var partition__3716 = function(n, step, coll) {
    return new cljs.core.LazySeq(null, false, function() {
      var temp__3698__auto____3709 = cljs.core.seq.call(null, coll);
      if(cljs.core.truth_(temp__3698__auto____3709)) {
        var s__3710 = temp__3698__auto____3709;
        var p__3711 = cljs.core.take.call(null, n, s__3710);
        if(cljs.core.truth_(cljs.core._EQ_.call(null, n, cljs.core.count.call(null, p__3711)))) {
          return cljs.core.cons.call(null, p__3711, partition.call(null, n, step, cljs.core.drop.call(null, step, s__3710)))
        }else {
          return null
        }
      }else {
        return null
      }
    })
  };
  var partition__3717 = function(n, step, pad, coll) {
    return new cljs.core.LazySeq(null, false, function() {
      var temp__3698__auto____3712 = cljs.core.seq.call(null, coll);
      if(cljs.core.truth_(temp__3698__auto____3712)) {
        var s__3713 = temp__3698__auto____3712;
        var p__3714 = cljs.core.take.call(null, n, s__3713);
        if(cljs.core.truth_(cljs.core._EQ_.call(null, n, cljs.core.count.call(null, p__3714)))) {
          return cljs.core.cons.call(null, p__3714, partition.call(null, n, step, pad, cljs.core.drop.call(null, step, s__3713)))
        }else {
          return cljs.core.list.call(null, cljs.core.take.call(null, n, cljs.core.concat.call(null, p__3714, pad)))
        }
      }else {
        return null
      }
    })
  };
  partition = function(n, step, pad, coll) {
    switch(arguments.length) {
      case 2:
        return partition__3715.call(this, n, step);
      case 3:
        return partition__3716.call(this, n, step, pad);
      case 4:
        return partition__3717.call(this, n, step, pad, coll)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return partition
}();
cljs.core.get_in = function() {
  var get_in = null;
  var get_in__3723 = function(m, ks) {
    return cljs.core.reduce.call(null, cljs.core.get, m, ks)
  };
  var get_in__3724 = function(m, ks, not_found) {
    var sentinel__3719 = cljs.core.lookup_sentinel;
    var m__3720 = m;
    var ks__3721 = cljs.core.seq.call(null, ks);
    while(true) {
      if(cljs.core.truth_(ks__3721)) {
        var m__3722 = cljs.core.get.call(null, m__3720, cljs.core.first.call(null, ks__3721), sentinel__3719);
        if(cljs.core.truth_(sentinel__3719 === m__3722)) {
          return not_found
        }else {
          var G__3726 = sentinel__3719;
          var G__3727 = m__3722;
          var G__3728 = cljs.core.next.call(null, ks__3721);
          sentinel__3719 = G__3726;
          m__3720 = G__3727;
          ks__3721 = G__3728;
          continue
        }
      }else {
        return m__3720
      }
      break
    }
  };
  get_in = function(m, ks, not_found) {
    switch(arguments.length) {
      case 2:
        return get_in__3723.call(this, m, ks);
      case 3:
        return get_in__3724.call(this, m, ks, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return get_in
}();
cljs.core.assoc_in = function assoc_in(m, p__3729, v) {
  var vec__3730__3731 = p__3729;
  var k__3732 = cljs.core.nth.call(null, vec__3730__3731, 0, null);
  var ks__3733 = cljs.core.nthnext.call(null, vec__3730__3731, 1);
  if(cljs.core.truth_(ks__3733)) {
    return cljs.core.assoc.call(null, m, k__3732, assoc_in.call(null, cljs.core.get.call(null, m, k__3732), ks__3733, v))
  }else {
    return cljs.core.assoc.call(null, m, k__3732, v)
  }
};
cljs.core.update_in = function() {
  var update_in__delegate = function(m, p__3734, f, args) {
    var vec__3735__3736 = p__3734;
    var k__3737 = cljs.core.nth.call(null, vec__3735__3736, 0, null);
    var ks__3738 = cljs.core.nthnext.call(null, vec__3735__3736, 1);
    if(cljs.core.truth_(ks__3738)) {
      return cljs.core.assoc.call(null, m, k__3737, cljs.core.apply.call(null, update_in, cljs.core.get.call(null, m, k__3737), ks__3738, f, args))
    }else {
      return cljs.core.assoc.call(null, m, k__3737, cljs.core.apply.call(null, f, cljs.core.get.call(null, m, k__3737), args))
    }
  };
  var update_in = function(m, p__3734, f, var_args) {
    var args = null;
    if(goog.isDef(var_args)) {
      args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
    }
    return update_in__delegate.call(this, m, p__3734, f, args)
  };
  update_in.cljs$lang$maxFixedArity = 3;
  update_in.cljs$lang$applyTo = function(arglist__3739) {
    var m = cljs.core.first(arglist__3739);
    var p__3734 = cljs.core.first(cljs.core.next(arglist__3739));
    var f = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3739)));
    var args = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3739)));
    return update_in__delegate.call(this, m, p__3734, f, args)
  };
  return update_in
}();
cljs.core.Vector = function(meta, array) {
  this.meta = meta;
  this.array = array
};
cljs.core.Vector.cljs$core$IPrintable$_pr_seq = function(this__267__auto__) {
  return cljs.core.list.call(null, "cljs.core.Vector")
};
cljs.core.Vector.prototype.cljs$core$IHash$ = true;
cljs.core.Vector.prototype.cljs$core$IHash$_hash = function(coll) {
  var this__3740 = this;
  return cljs.core.hash_coll.call(null, coll)
};
cljs.core.Vector.prototype.cljs$core$ILookup$ = true;
cljs.core.Vector.prototype.cljs$core$ILookup$_lookup = function() {
  var G__3773 = null;
  var G__3773__3774 = function(coll, k) {
    var this__3741 = this;
    return cljs.core._nth.call(null, coll, k, null)
  };
  var G__3773__3775 = function(coll, k, not_found) {
    var this__3742 = this;
    return cljs.core._nth.call(null, coll, k, not_found)
  };
  G__3773 = function(coll, k, not_found) {
    switch(arguments.length) {
      case 2:
        return G__3773__3774.call(this, coll, k);
      case 3:
        return G__3773__3775.call(this, coll, k, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3773
}();
cljs.core.Vector.prototype.cljs$core$IAssociative$ = true;
cljs.core.Vector.prototype.cljs$core$IAssociative$_assoc = function(coll, k, v) {
  var this__3743 = this;
  var new_array__3744 = cljs.core.aclone.call(null, this__3743.array);
  new_array__3744[k] = v;
  return new cljs.core.Vector(this__3743.meta, new_array__3744)
};
cljs.core.Vector.prototype.cljs$core$IFn$ = true;
cljs.core.Vector.prototype.call = function() {
  var G__3777 = null;
  var G__3777__3778 = function(tsym3745, k) {
    var this__3747 = this;
    var tsym3745__3748 = this;
    var coll__3749 = tsym3745__3748;
    return cljs.core._lookup.call(null, coll__3749, k)
  };
  var G__3777__3779 = function(tsym3746, k, not_found) {
    var this__3750 = this;
    var tsym3746__3751 = this;
    var coll__3752 = tsym3746__3751;
    return cljs.core._lookup.call(null, coll__3752, k, not_found)
  };
  G__3777 = function(tsym3746, k, not_found) {
    switch(arguments.length) {
      case 2:
        return G__3777__3778.call(this, tsym3746, k);
      case 3:
        return G__3777__3779.call(this, tsym3746, k, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3777
}();
cljs.core.Vector.prototype.cljs$core$ISequential$ = true;
cljs.core.Vector.prototype.cljs$core$ICollection$ = true;
cljs.core.Vector.prototype.cljs$core$ICollection$_conj = function(coll, o) {
  var this__3753 = this;
  var new_array__3754 = cljs.core.aclone.call(null, this__3753.array);
  new_array__3754.push(o);
  return new cljs.core.Vector(this__3753.meta, new_array__3754)
};
cljs.core.Vector.prototype.cljs$core$IReduce$ = true;
cljs.core.Vector.prototype.cljs$core$IReduce$_reduce = function() {
  var G__3781 = null;
  var G__3781__3782 = function(v, f) {
    var this__3755 = this;
    return cljs.core.ci_reduce.call(null, this__3755.array, f)
  };
  var G__3781__3783 = function(v, f, start) {
    var this__3756 = this;
    return cljs.core.ci_reduce.call(null, this__3756.array, f, start)
  };
  G__3781 = function(v, f, start) {
    switch(arguments.length) {
      case 2:
        return G__3781__3782.call(this, v, f);
      case 3:
        return G__3781__3783.call(this, v, f, start)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3781
}();
cljs.core.Vector.prototype.cljs$core$ISeqable$ = true;
cljs.core.Vector.prototype.cljs$core$ISeqable$_seq = function(coll) {
  var this__3757 = this;
  if(cljs.core.truth_(this__3757.array.length > 0)) {
    var vector_seq__3758 = function vector_seq(i) {
      return new cljs.core.LazySeq(null, false, function() {
        if(cljs.core.truth_(i < this__3757.array.length)) {
          return cljs.core.cons.call(null, this__3757.array[i], vector_seq.call(null, i + 1))
        }else {
          return null
        }
      })
    };
    return vector_seq__3758.call(null, 0)
  }else {
    return null
  }
};
cljs.core.Vector.prototype.cljs$core$ICounted$ = true;
cljs.core.Vector.prototype.cljs$core$ICounted$_count = function(coll) {
  var this__3759 = this;
  return this__3759.array.length
};
cljs.core.Vector.prototype.cljs$core$IStack$ = true;
cljs.core.Vector.prototype.cljs$core$IStack$_peek = function(coll) {
  var this__3760 = this;
  var count__3761 = this__3760.array.length;
  if(cljs.core.truth_(count__3761 > 0)) {
    return this__3760.array[count__3761 - 1]
  }else {
    return null
  }
};
cljs.core.Vector.prototype.cljs$core$IStack$_pop = function(coll) {
  var this__3762 = this;
  if(cljs.core.truth_(this__3762.array.length > 0)) {
    var new_array__3763 = cljs.core.aclone.call(null, this__3762.array);
    new_array__3763.pop();
    return new cljs.core.Vector(this__3762.meta, new_array__3763)
  }else {
    throw new Error("Can't pop empty vector");
  }
};
cljs.core.Vector.prototype.cljs$core$IVector$ = true;
cljs.core.Vector.prototype.cljs$core$IVector$_assoc_n = function(coll, n, val) {
  var this__3764 = this;
  return cljs.core._assoc.call(null, coll, n, val)
};
cljs.core.Vector.prototype.cljs$core$IEquiv$ = true;
cljs.core.Vector.prototype.cljs$core$IEquiv$_equiv = function(coll, other) {
  var this__3765 = this;
  return cljs.core.equiv_sequential.call(null, coll, other)
};
cljs.core.Vector.prototype.cljs$core$IWithMeta$ = true;
cljs.core.Vector.prototype.cljs$core$IWithMeta$_with_meta = function(coll, meta) {
  var this__3766 = this;
  return new cljs.core.Vector(meta, this__3766.array)
};
cljs.core.Vector.prototype.cljs$core$IMeta$ = true;
cljs.core.Vector.prototype.cljs$core$IMeta$_meta = function(coll) {
  var this__3767 = this;
  return this__3767.meta
};
cljs.core.Vector.prototype.cljs$core$IIndexed$ = true;
cljs.core.Vector.prototype.cljs$core$IIndexed$_nth = function() {
  var G__3785 = null;
  var G__3785__3786 = function(coll, n) {
    var this__3768 = this;
    if(cljs.core.truth_(function() {
      var and__3546__auto____3769 = 0 <= n;
      if(cljs.core.truth_(and__3546__auto____3769)) {
        return n < this__3768.array.length
      }else {
        return and__3546__auto____3769
      }
    }())) {
      return this__3768.array[n]
    }else {
      return null
    }
  };
  var G__3785__3787 = function(coll, n, not_found) {
    var this__3770 = this;
    if(cljs.core.truth_(function() {
      var and__3546__auto____3771 = 0 <= n;
      if(cljs.core.truth_(and__3546__auto____3771)) {
        return n < this__3770.array.length
      }else {
        return and__3546__auto____3771
      }
    }())) {
      return this__3770.array[n]
    }else {
      return not_found
    }
  };
  G__3785 = function(coll, n, not_found) {
    switch(arguments.length) {
      case 2:
        return G__3785__3786.call(this, coll, n);
      case 3:
        return G__3785__3787.call(this, coll, n, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3785
}();
cljs.core.Vector.prototype.cljs$core$IEmptyableCollection$ = true;
cljs.core.Vector.prototype.cljs$core$IEmptyableCollection$_empty = function(coll) {
  var this__3772 = this;
  return cljs.core.with_meta.call(null, cljs.core.Vector.EMPTY, this__3772.meta)
};
cljs.core.Vector;
cljs.core.Vector.EMPTY = new cljs.core.Vector(null, cljs.core.array.call(null));
cljs.core.Vector.fromArray = function(xs) {
  return new cljs.core.Vector(null, xs)
};
cljs.core.vec = function vec(coll) {
  return cljs.core.reduce.call(null, cljs.core.conj, cljs.core.Vector.EMPTY, coll)
};
cljs.core.vector = function() {
  var vector__delegate = function(args) {
    return cljs.core.vec.call(null, args)
  };
  var vector = function(var_args) {
    var args = null;
    if(goog.isDef(var_args)) {
      args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 0), 0)
    }
    return vector__delegate.call(this, args)
  };
  vector.cljs$lang$maxFixedArity = 0;
  vector.cljs$lang$applyTo = function(arglist__3789) {
    var args = cljs.core.seq(arglist__3789);
    return vector__delegate.call(this, args)
  };
  return vector
}();
cljs.core.Subvec = function(meta, v, start, end) {
  this.meta = meta;
  this.v = v;
  this.start = start;
  this.end = end
};
cljs.core.Subvec.cljs$core$IPrintable$_pr_seq = function(this__267__auto__) {
  return cljs.core.list.call(null, "cljs.core.Subvec")
};
cljs.core.Subvec.prototype.cljs$core$IHash$ = true;
cljs.core.Subvec.prototype.cljs$core$IHash$_hash = function(coll) {
  var this__3790 = this;
  return cljs.core.hash_coll.call(null, coll)
};
cljs.core.Subvec.prototype.cljs$core$ILookup$ = true;
cljs.core.Subvec.prototype.cljs$core$ILookup$_lookup = function() {
  var G__3818 = null;
  var G__3818__3819 = function(coll, k) {
    var this__3791 = this;
    return cljs.core._nth.call(null, coll, k, null)
  };
  var G__3818__3820 = function(coll, k, not_found) {
    var this__3792 = this;
    return cljs.core._nth.call(null, coll, k, not_found)
  };
  G__3818 = function(coll, k, not_found) {
    switch(arguments.length) {
      case 2:
        return G__3818__3819.call(this, coll, k);
      case 3:
        return G__3818__3820.call(this, coll, k, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3818
}();
cljs.core.Subvec.prototype.cljs$core$IAssociative$ = true;
cljs.core.Subvec.prototype.cljs$core$IAssociative$_assoc = function(coll, key, val) {
  var this__3793 = this;
  var v_pos__3794 = this__3793.start + key;
  return new cljs.core.Subvec(this__3793.meta, cljs.core._assoc.call(null, this__3793.v, v_pos__3794, val), this__3793.start, this__3793.end > v_pos__3794 + 1 ? this__3793.end : v_pos__3794 + 1)
};
cljs.core.Subvec.prototype.cljs$core$IFn$ = true;
cljs.core.Subvec.prototype.call = function() {
  var G__3822 = null;
  var G__3822__3823 = function(tsym3795, k) {
    var this__3797 = this;
    var tsym3795__3798 = this;
    var coll__3799 = tsym3795__3798;
    return cljs.core._lookup.call(null, coll__3799, k)
  };
  var G__3822__3824 = function(tsym3796, k, not_found) {
    var this__3800 = this;
    var tsym3796__3801 = this;
    var coll__3802 = tsym3796__3801;
    return cljs.core._lookup.call(null, coll__3802, k, not_found)
  };
  G__3822 = function(tsym3796, k, not_found) {
    switch(arguments.length) {
      case 2:
        return G__3822__3823.call(this, tsym3796, k);
      case 3:
        return G__3822__3824.call(this, tsym3796, k, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3822
}();
cljs.core.Subvec.prototype.cljs$core$ISequential$ = true;
cljs.core.Subvec.prototype.cljs$core$ICollection$ = true;
cljs.core.Subvec.prototype.cljs$core$ICollection$_conj = function(coll, o) {
  var this__3803 = this;
  return new cljs.core.Subvec(this__3803.meta, cljs.core._assoc_n.call(null, this__3803.v, this__3803.end, o), this__3803.start, this__3803.end + 1)
};
cljs.core.Subvec.prototype.cljs$core$IReduce$ = true;
cljs.core.Subvec.prototype.cljs$core$IReduce$_reduce = function() {
  var G__3826 = null;
  var G__3826__3827 = function(coll, f) {
    var this__3804 = this;
    return cljs.core.ci_reduce.call(null, coll, f)
  };
  var G__3826__3828 = function(coll, f, start) {
    var this__3805 = this;
    return cljs.core.ci_reduce.call(null, coll, f, start)
  };
  G__3826 = function(coll, f, start) {
    switch(arguments.length) {
      case 2:
        return G__3826__3827.call(this, coll, f);
      case 3:
        return G__3826__3828.call(this, coll, f, start)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3826
}();
cljs.core.Subvec.prototype.cljs$core$ISeqable$ = true;
cljs.core.Subvec.prototype.cljs$core$ISeqable$_seq = function(coll) {
  var this__3806 = this;
  var subvec_seq__3807 = function subvec_seq(i) {
    if(cljs.core.truth_(cljs.core._EQ_.call(null, i, this__3806.end))) {
      return null
    }else {
      return cljs.core.cons.call(null, cljs.core._nth.call(null, this__3806.v, i), new cljs.core.LazySeq(null, false, function() {
        return subvec_seq.call(null, i + 1)
      }))
    }
  };
  return subvec_seq__3807.call(null, this__3806.start)
};
cljs.core.Subvec.prototype.cljs$core$ICounted$ = true;
cljs.core.Subvec.prototype.cljs$core$ICounted$_count = function(coll) {
  var this__3808 = this;
  return this__3808.end - this__3808.start
};
cljs.core.Subvec.prototype.cljs$core$IStack$ = true;
cljs.core.Subvec.prototype.cljs$core$IStack$_peek = function(coll) {
  var this__3809 = this;
  return cljs.core._nth.call(null, this__3809.v, this__3809.end - 1)
};
cljs.core.Subvec.prototype.cljs$core$IStack$_pop = function(coll) {
  var this__3810 = this;
  if(cljs.core.truth_(cljs.core._EQ_.call(null, this__3810.start, this__3810.end))) {
    throw new Error("Can't pop empty vector");
  }else {
    return new cljs.core.Subvec(this__3810.meta, this__3810.v, this__3810.start, this__3810.end - 1)
  }
};
cljs.core.Subvec.prototype.cljs$core$IVector$ = true;
cljs.core.Subvec.prototype.cljs$core$IVector$_assoc_n = function(coll, n, val) {
  var this__3811 = this;
  return cljs.core._assoc.call(null, coll, n, val)
};
cljs.core.Subvec.prototype.cljs$core$IEquiv$ = true;
cljs.core.Subvec.prototype.cljs$core$IEquiv$_equiv = function(coll, other) {
  var this__3812 = this;
  return cljs.core.equiv_sequential.call(null, coll, other)
};
cljs.core.Subvec.prototype.cljs$core$IWithMeta$ = true;
cljs.core.Subvec.prototype.cljs$core$IWithMeta$_with_meta = function(coll, meta) {
  var this__3813 = this;
  return new cljs.core.Subvec(meta, this__3813.v, this__3813.start, this__3813.end)
};
cljs.core.Subvec.prototype.cljs$core$IMeta$ = true;
cljs.core.Subvec.prototype.cljs$core$IMeta$_meta = function(coll) {
  var this__3814 = this;
  return this__3814.meta
};
cljs.core.Subvec.prototype.cljs$core$IIndexed$ = true;
cljs.core.Subvec.prototype.cljs$core$IIndexed$_nth = function() {
  var G__3830 = null;
  var G__3830__3831 = function(coll, n) {
    var this__3815 = this;
    return cljs.core._nth.call(null, this__3815.v, this__3815.start + n)
  };
  var G__3830__3832 = function(coll, n, not_found) {
    var this__3816 = this;
    return cljs.core._nth.call(null, this__3816.v, this__3816.start + n, not_found)
  };
  G__3830 = function(coll, n, not_found) {
    switch(arguments.length) {
      case 2:
        return G__3830__3831.call(this, coll, n);
      case 3:
        return G__3830__3832.call(this, coll, n, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3830
}();
cljs.core.Subvec.prototype.cljs$core$IEmptyableCollection$ = true;
cljs.core.Subvec.prototype.cljs$core$IEmptyableCollection$_empty = function(coll) {
  var this__3817 = this;
  return cljs.core.with_meta.call(null, cljs.core.Vector.EMPTY, this__3817.meta)
};
cljs.core.Subvec;
cljs.core.subvec = function() {
  var subvec = null;
  var subvec__3834 = function(v, start) {
    return subvec.call(null, v, start, cljs.core.count.call(null, v))
  };
  var subvec__3835 = function(v, start, end) {
    return new cljs.core.Subvec(null, v, start, end)
  };
  subvec = function(v, start, end) {
    switch(arguments.length) {
      case 2:
        return subvec__3834.call(this, v, start);
      case 3:
        return subvec__3835.call(this, v, start, end)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return subvec
}();
cljs.core.PersistentQueueSeq = function(meta, front, rear) {
  this.meta = meta;
  this.front = front;
  this.rear = rear
};
cljs.core.PersistentQueueSeq.cljs$core$IPrintable$_pr_seq = function(this__267__auto__) {
  return cljs.core.list.call(null, "cljs.core.PersistentQueueSeq")
};
cljs.core.PersistentQueueSeq.prototype.cljs$core$ISeqable$ = true;
cljs.core.PersistentQueueSeq.prototype.cljs$core$ISeqable$_seq = function(coll) {
  var this__3837 = this;
  return coll
};
cljs.core.PersistentQueueSeq.prototype.cljs$core$IHash$ = true;
cljs.core.PersistentQueueSeq.prototype.cljs$core$IHash$_hash = function(coll) {
  var this__3838 = this;
  return cljs.core.hash_coll.call(null, coll)
};
cljs.core.PersistentQueueSeq.prototype.cljs$core$IEquiv$ = true;
cljs.core.PersistentQueueSeq.prototype.cljs$core$IEquiv$_equiv = function(coll, other) {
  var this__3839 = this;
  return cljs.core.equiv_sequential.call(null, coll, other)
};
cljs.core.PersistentQueueSeq.prototype.cljs$core$ISequential$ = true;
cljs.core.PersistentQueueSeq.prototype.cljs$core$IEmptyableCollection$ = true;
cljs.core.PersistentQueueSeq.prototype.cljs$core$IEmptyableCollection$_empty = function(coll) {
  var this__3840 = this;
  return cljs.core.with_meta.call(null, cljs.core.List.EMPTY, this__3840.meta)
};
cljs.core.PersistentQueueSeq.prototype.cljs$core$ICollection$ = true;
cljs.core.PersistentQueueSeq.prototype.cljs$core$ICollection$_conj = function(coll, o) {
  var this__3841 = this;
  return cljs.core.cons.call(null, o, coll)
};
cljs.core.PersistentQueueSeq.prototype.cljs$core$ISeq$ = true;
cljs.core.PersistentQueueSeq.prototype.cljs$core$ISeq$_first = function(coll) {
  var this__3842 = this;
  return cljs.core._first.call(null, this__3842.front)
};
cljs.core.PersistentQueueSeq.prototype.cljs$core$ISeq$_rest = function(coll) {
  var this__3843 = this;
  var temp__3695__auto____3844 = cljs.core.next.call(null, this__3843.front);
  if(cljs.core.truth_(temp__3695__auto____3844)) {
    var f1__3845 = temp__3695__auto____3844;
    return new cljs.core.PersistentQueueSeq(this__3843.meta, f1__3845, this__3843.rear)
  }else {
    if(cljs.core.truth_(this__3843.rear === null)) {
      return cljs.core._empty.call(null, coll)
    }else {
      return new cljs.core.PersistentQueueSeq(this__3843.meta, this__3843.rear, null)
    }
  }
};
cljs.core.PersistentQueueSeq.prototype.cljs$core$IMeta$ = true;
cljs.core.PersistentQueueSeq.prototype.cljs$core$IMeta$_meta = function(coll) {
  var this__3846 = this;
  return this__3846.meta
};
cljs.core.PersistentQueueSeq.prototype.cljs$core$IWithMeta$ = true;
cljs.core.PersistentQueueSeq.prototype.cljs$core$IWithMeta$_with_meta = function(coll, meta) {
  var this__3847 = this;
  return new cljs.core.PersistentQueueSeq(meta, this__3847.front, this__3847.rear)
};
cljs.core.PersistentQueueSeq;
cljs.core.PersistentQueue = function(meta, count, front, rear) {
  this.meta = meta;
  this.count = count;
  this.front = front;
  this.rear = rear
};
cljs.core.PersistentQueue.cljs$core$IPrintable$_pr_seq = function(this__267__auto__) {
  return cljs.core.list.call(null, "cljs.core.PersistentQueue")
};
cljs.core.PersistentQueue.prototype.cljs$core$IHash$ = true;
cljs.core.PersistentQueue.prototype.cljs$core$IHash$_hash = function(coll) {
  var this__3848 = this;
  return cljs.core.hash_coll.call(null, coll)
};
cljs.core.PersistentQueue.prototype.cljs$core$ISequential$ = true;
cljs.core.PersistentQueue.prototype.cljs$core$ICollection$ = true;
cljs.core.PersistentQueue.prototype.cljs$core$ICollection$_conj = function(coll, o) {
  var this__3849 = this;
  if(cljs.core.truth_(this__3849.front)) {
    return new cljs.core.PersistentQueue(this__3849.meta, this__3849.count + 1, this__3849.front, cljs.core.conj.call(null, function() {
      var or__3548__auto____3850 = this__3849.rear;
      if(cljs.core.truth_(or__3548__auto____3850)) {
        return or__3548__auto____3850
      }else {
        return cljs.core.Vector.fromArray([])
      }
    }(), o))
  }else {
    return new cljs.core.PersistentQueue(this__3849.meta, this__3849.count + 1, cljs.core.conj.call(null, this__3849.front, o), cljs.core.Vector.fromArray([]))
  }
};
cljs.core.PersistentQueue.prototype.cljs$core$ISeqable$ = true;
cljs.core.PersistentQueue.prototype.cljs$core$ISeqable$_seq = function(coll) {
  var this__3851 = this;
  var rear__3852 = cljs.core.seq.call(null, this__3851.rear);
  if(cljs.core.truth_(function() {
    var or__3548__auto____3853 = this__3851.front;
    if(cljs.core.truth_(or__3548__auto____3853)) {
      return or__3548__auto____3853
    }else {
      return rear__3852
    }
  }())) {
    return new cljs.core.PersistentQueueSeq(null, this__3851.front, cljs.core.seq.call(null, rear__3852))
  }else {
    return cljs.core.List.EMPTY
  }
};
cljs.core.PersistentQueue.prototype.cljs$core$ICounted$ = true;
cljs.core.PersistentQueue.prototype.cljs$core$ICounted$_count = function(coll) {
  var this__3854 = this;
  return this__3854.count
};
cljs.core.PersistentQueue.prototype.cljs$core$IStack$ = true;
cljs.core.PersistentQueue.prototype.cljs$core$IStack$_peek = function(coll) {
  var this__3855 = this;
  return cljs.core._first.call(null, this__3855.front)
};
cljs.core.PersistentQueue.prototype.cljs$core$IStack$_pop = function(coll) {
  var this__3856 = this;
  if(cljs.core.truth_(this__3856.front)) {
    var temp__3695__auto____3857 = cljs.core.next.call(null, this__3856.front);
    if(cljs.core.truth_(temp__3695__auto____3857)) {
      var f1__3858 = temp__3695__auto____3857;
      return new cljs.core.PersistentQueue(this__3856.meta, this__3856.count - 1, f1__3858, this__3856.rear)
    }else {
      return new cljs.core.PersistentQueue(this__3856.meta, this__3856.count - 1, cljs.core.seq.call(null, this__3856.rear), cljs.core.Vector.fromArray([]))
    }
  }else {
    return coll
  }
};
cljs.core.PersistentQueue.prototype.cljs$core$ISeq$ = true;
cljs.core.PersistentQueue.prototype.cljs$core$ISeq$_first = function(coll) {
  var this__3859 = this;
  return cljs.core.first.call(null, this__3859.front)
};
cljs.core.PersistentQueue.prototype.cljs$core$ISeq$_rest = function(coll) {
  var this__3860 = this;
  return cljs.core.rest.call(null, cljs.core.seq.call(null, coll))
};
cljs.core.PersistentQueue.prototype.cljs$core$IEquiv$ = true;
cljs.core.PersistentQueue.prototype.cljs$core$IEquiv$_equiv = function(coll, other) {
  var this__3861 = this;
  return cljs.core.equiv_sequential.call(null, coll, other)
};
cljs.core.PersistentQueue.prototype.cljs$core$IWithMeta$ = true;
cljs.core.PersistentQueue.prototype.cljs$core$IWithMeta$_with_meta = function(coll, meta) {
  var this__3862 = this;
  return new cljs.core.PersistentQueue(meta, this__3862.count, this__3862.front, this__3862.rear)
};
cljs.core.PersistentQueue.prototype.cljs$core$IMeta$ = true;
cljs.core.PersistentQueue.prototype.cljs$core$IMeta$_meta = function(coll) {
  var this__3863 = this;
  return this__3863.meta
};
cljs.core.PersistentQueue.prototype.cljs$core$IEmptyableCollection$ = true;
cljs.core.PersistentQueue.prototype.cljs$core$IEmptyableCollection$_empty = function(coll) {
  var this__3864 = this;
  return cljs.core.PersistentQueue.EMPTY
};
cljs.core.PersistentQueue;
cljs.core.PersistentQueue.EMPTY = new cljs.core.PersistentQueue(null, 0, null, cljs.core.Vector.fromArray([]));
cljs.core.NeverEquiv = function() {
};
cljs.core.NeverEquiv.cljs$core$IPrintable$_pr_seq = function(this__267__auto__) {
  return cljs.core.list.call(null, "cljs.core.NeverEquiv")
};
cljs.core.NeverEquiv.prototype.cljs$core$IEquiv$ = true;
cljs.core.NeverEquiv.prototype.cljs$core$IEquiv$_equiv = function(o, other) {
  var this__3865 = this;
  return false
};
cljs.core.NeverEquiv;
cljs.core.never_equiv = new cljs.core.NeverEquiv;
cljs.core.equiv_map = function equiv_map(x, y) {
  return cljs.core.boolean$.call(null, cljs.core.truth_(cljs.core.map_QMARK_.call(null, y)) ? cljs.core.truth_(cljs.core._EQ_.call(null, cljs.core.count.call(null, x), cljs.core.count.call(null, y))) ? cljs.core.every_QMARK_.call(null, cljs.core.identity, cljs.core.map.call(null, function(xkv) {
    return cljs.core._EQ_.call(null, cljs.core.get.call(null, y, cljs.core.first.call(null, xkv), cljs.core.never_equiv), cljs.core.second.call(null, xkv))
  }, x)) : null : null)
};
cljs.core.scan_array = function scan_array(incr, k, array) {
  var len__3866 = array.length;
  var i__3867 = 0;
  while(true) {
    if(cljs.core.truth_(i__3867 < len__3866)) {
      if(cljs.core.truth_(cljs.core._EQ_.call(null, k, array[i__3867]))) {
        return i__3867
      }else {
        var G__3868 = i__3867 + incr;
        i__3867 = G__3868;
        continue
      }
    }else {
      return null
    }
    break
  }
};
cljs.core.obj_map_contains_key_QMARK_ = function() {
  var obj_map_contains_key_QMARK_ = null;
  var obj_map_contains_key_QMARK___3870 = function(k, strobj) {
    return obj_map_contains_key_QMARK_.call(null, k, strobj, true, false)
  };
  var obj_map_contains_key_QMARK___3871 = function(k, strobj, true_val, false_val) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____3869 = goog.isString.call(null, k);
      if(cljs.core.truth_(and__3546__auto____3869)) {
        return strobj.hasOwnProperty(k)
      }else {
        return and__3546__auto____3869
      }
    }())) {
      return true_val
    }else {
      return false_val
    }
  };
  obj_map_contains_key_QMARK_ = function(k, strobj, true_val, false_val) {
    switch(arguments.length) {
      case 2:
        return obj_map_contains_key_QMARK___3870.call(this, k, strobj);
      case 4:
        return obj_map_contains_key_QMARK___3871.call(this, k, strobj, true_val, false_val)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return obj_map_contains_key_QMARK_
}();
cljs.core.obj_map_compare_keys = function obj_map_compare_keys(a, b) {
  var a__3874 = cljs.core.hash.call(null, a);
  var b__3875 = cljs.core.hash.call(null, b);
  if(cljs.core.truth_(a__3874 < b__3875)) {
    return-1
  }else {
    if(cljs.core.truth_(a__3874 > b__3875)) {
      return 1
    }else {
      if(cljs.core.truth_("\ufdd0'else")) {
        return 0
      }else {
        return null
      }
    }
  }
};
cljs.core.ObjMap = function(meta, keys, strobj) {
  this.meta = meta;
  this.keys = keys;
  this.strobj = strobj
};
cljs.core.ObjMap.cljs$core$IPrintable$_pr_seq = function(this__267__auto__) {
  return cljs.core.list.call(null, "cljs.core.ObjMap")
};
cljs.core.ObjMap.prototype.cljs$core$IHash$ = true;
cljs.core.ObjMap.prototype.cljs$core$IHash$_hash = function(coll) {
  var this__3876 = this;
  return cljs.core.hash_coll.call(null, coll)
};
cljs.core.ObjMap.prototype.cljs$core$ILookup$ = true;
cljs.core.ObjMap.prototype.cljs$core$ILookup$_lookup = function() {
  var G__3903 = null;
  var G__3903__3904 = function(coll, k) {
    var this__3877 = this;
    return cljs.core._lookup.call(null, coll, k, null)
  };
  var G__3903__3905 = function(coll, k, not_found) {
    var this__3878 = this;
    return cljs.core.obj_map_contains_key_QMARK_.call(null, k, this__3878.strobj, this__3878.strobj[k], not_found)
  };
  G__3903 = function(coll, k, not_found) {
    switch(arguments.length) {
      case 2:
        return G__3903__3904.call(this, coll, k);
      case 3:
        return G__3903__3905.call(this, coll, k, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3903
}();
cljs.core.ObjMap.prototype.cljs$core$IAssociative$ = true;
cljs.core.ObjMap.prototype.cljs$core$IAssociative$_assoc = function(coll, k, v) {
  var this__3879 = this;
  if(cljs.core.truth_(goog.isString.call(null, k))) {
    var new_strobj__3880 = goog.object.clone.call(null, this__3879.strobj);
    var overwrite_QMARK___3881 = new_strobj__3880.hasOwnProperty(k);
    new_strobj__3880[k] = v;
    if(cljs.core.truth_(overwrite_QMARK___3881)) {
      return new cljs.core.ObjMap(this__3879.meta, this__3879.keys, new_strobj__3880)
    }else {
      var new_keys__3882 = cljs.core.aclone.call(null, this__3879.keys);
      new_keys__3882.push(k);
      return new cljs.core.ObjMap(this__3879.meta, new_keys__3882, new_strobj__3880)
    }
  }else {
    return cljs.core.with_meta.call(null, cljs.core.into.call(null, cljs.core.hash_map.call(null, k, v), cljs.core.seq.call(null, coll)), this__3879.meta)
  }
};
cljs.core.ObjMap.prototype.cljs$core$IAssociative$_contains_key_QMARK_ = function(coll, k) {
  var this__3883 = this;
  return cljs.core.obj_map_contains_key_QMARK_.call(null, k, this__3883.strobj)
};
cljs.core.ObjMap.prototype.cljs$core$IFn$ = true;
cljs.core.ObjMap.prototype.call = function() {
  var G__3907 = null;
  var G__3907__3908 = function(tsym3884, k) {
    var this__3886 = this;
    var tsym3884__3887 = this;
    var coll__3888 = tsym3884__3887;
    return cljs.core._lookup.call(null, coll__3888, k)
  };
  var G__3907__3909 = function(tsym3885, k, not_found) {
    var this__3889 = this;
    var tsym3885__3890 = this;
    var coll__3891 = tsym3885__3890;
    return cljs.core._lookup.call(null, coll__3891, k, not_found)
  };
  G__3907 = function(tsym3885, k, not_found) {
    switch(arguments.length) {
      case 2:
        return G__3907__3908.call(this, tsym3885, k);
      case 3:
        return G__3907__3909.call(this, tsym3885, k, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3907
}();
cljs.core.ObjMap.prototype.cljs$core$ICollection$ = true;
cljs.core.ObjMap.prototype.cljs$core$ICollection$_conj = function(coll, entry) {
  var this__3892 = this;
  if(cljs.core.truth_(cljs.core.vector_QMARK_.call(null, entry))) {
    return cljs.core._assoc.call(null, coll, cljs.core._nth.call(null, entry, 0), cljs.core._nth.call(null, entry, 1))
  }else {
    return cljs.core.reduce.call(null, cljs.core._conj, coll, entry)
  }
};
cljs.core.ObjMap.prototype.cljs$core$ISeqable$ = true;
cljs.core.ObjMap.prototype.cljs$core$ISeqable$_seq = function(coll) {
  var this__3893 = this;
  if(cljs.core.truth_(this__3893.keys.length > 0)) {
    return cljs.core.map.call(null, function(p1__3873_SHARP_) {
      return cljs.core.vector.call(null, p1__3873_SHARP_, this__3893.strobj[p1__3873_SHARP_])
    }, this__3893.keys.sort(cljs.core.obj_map_compare_keys))
  }else {
    return null
  }
};
cljs.core.ObjMap.prototype.cljs$core$ICounted$ = true;
cljs.core.ObjMap.prototype.cljs$core$ICounted$_count = function(coll) {
  var this__3894 = this;
  return this__3894.keys.length
};
cljs.core.ObjMap.prototype.cljs$core$IEquiv$ = true;
cljs.core.ObjMap.prototype.cljs$core$IEquiv$_equiv = function(coll, other) {
  var this__3895 = this;
  return cljs.core.equiv_map.call(null, coll, other)
};
cljs.core.ObjMap.prototype.cljs$core$IWithMeta$ = true;
cljs.core.ObjMap.prototype.cljs$core$IWithMeta$_with_meta = function(coll, meta) {
  var this__3896 = this;
  return new cljs.core.ObjMap(meta, this__3896.keys, this__3896.strobj)
};
cljs.core.ObjMap.prototype.cljs$core$IMeta$ = true;
cljs.core.ObjMap.prototype.cljs$core$IMeta$_meta = function(coll) {
  var this__3897 = this;
  return this__3897.meta
};
cljs.core.ObjMap.prototype.cljs$core$IEmptyableCollection$ = true;
cljs.core.ObjMap.prototype.cljs$core$IEmptyableCollection$_empty = function(coll) {
  var this__3898 = this;
  return cljs.core.with_meta.call(null, cljs.core.ObjMap.EMPTY, this__3898.meta)
};
cljs.core.ObjMap.prototype.cljs$core$IMap$ = true;
cljs.core.ObjMap.prototype.cljs$core$IMap$_dissoc = function(coll, k) {
  var this__3899 = this;
  if(cljs.core.truth_(function() {
    var and__3546__auto____3900 = goog.isString.call(null, k);
    if(cljs.core.truth_(and__3546__auto____3900)) {
      return this__3899.strobj.hasOwnProperty(k)
    }else {
      return and__3546__auto____3900
    }
  }())) {
    var new_keys__3901 = cljs.core.aclone.call(null, this__3899.keys);
    var new_strobj__3902 = goog.object.clone.call(null, this__3899.strobj);
    new_keys__3901.splice(cljs.core.scan_array.call(null, 1, k, new_keys__3901), 1);
    cljs.core.js_delete.call(null, new_strobj__3902, k);
    return new cljs.core.ObjMap(this__3899.meta, new_keys__3901, new_strobj__3902)
  }else {
    return coll
  }
};
cljs.core.ObjMap;
cljs.core.ObjMap.EMPTY = new cljs.core.ObjMap(null, cljs.core.array.call(null), cljs.core.js_obj.call(null));
cljs.core.ObjMap.fromObject = function(ks, obj) {
  return new cljs.core.ObjMap(null, ks, obj)
};
cljs.core.HashMap = function(meta, count, hashobj) {
  this.meta = meta;
  this.count = count;
  this.hashobj = hashobj
};
cljs.core.HashMap.cljs$core$IPrintable$_pr_seq = function(this__267__auto__) {
  return cljs.core.list.call(null, "cljs.core.HashMap")
};
cljs.core.HashMap.prototype.cljs$core$IHash$ = true;
cljs.core.HashMap.prototype.cljs$core$IHash$_hash = function(coll) {
  var this__3912 = this;
  return cljs.core.hash_coll.call(null, coll)
};
cljs.core.HashMap.prototype.cljs$core$ILookup$ = true;
cljs.core.HashMap.prototype.cljs$core$ILookup$_lookup = function() {
  var G__3950 = null;
  var G__3950__3951 = function(coll, k) {
    var this__3913 = this;
    return cljs.core._lookup.call(null, coll, k, null)
  };
  var G__3950__3952 = function(coll, k, not_found) {
    var this__3914 = this;
    var bucket__3915 = this__3914.hashobj[cljs.core.hash.call(null, k)];
    var i__3916 = cljs.core.truth_(bucket__3915) ? cljs.core.scan_array.call(null, 2, k, bucket__3915) : null;
    if(cljs.core.truth_(i__3916)) {
      return bucket__3915[i__3916 + 1]
    }else {
      return not_found
    }
  };
  G__3950 = function(coll, k, not_found) {
    switch(arguments.length) {
      case 2:
        return G__3950__3951.call(this, coll, k);
      case 3:
        return G__3950__3952.call(this, coll, k, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3950
}();
cljs.core.HashMap.prototype.cljs$core$IAssociative$ = true;
cljs.core.HashMap.prototype.cljs$core$IAssociative$_assoc = function(coll, k, v) {
  var this__3917 = this;
  var h__3918 = cljs.core.hash.call(null, k);
  var bucket__3919 = this__3917.hashobj[h__3918];
  if(cljs.core.truth_(bucket__3919)) {
    var new_bucket__3920 = cljs.core.aclone.call(null, bucket__3919);
    var new_hashobj__3921 = goog.object.clone.call(null, this__3917.hashobj);
    new_hashobj__3921[h__3918] = new_bucket__3920;
    var temp__3695__auto____3922 = cljs.core.scan_array.call(null, 2, k, new_bucket__3920);
    if(cljs.core.truth_(temp__3695__auto____3922)) {
      var i__3923 = temp__3695__auto____3922;
      new_bucket__3920[i__3923 + 1] = v;
      return new cljs.core.HashMap(this__3917.meta, this__3917.count, new_hashobj__3921)
    }else {
      new_bucket__3920.push(k, v);
      return new cljs.core.HashMap(this__3917.meta, this__3917.count + 1, new_hashobj__3921)
    }
  }else {
    var new_hashobj__3924 = goog.object.clone.call(null, this__3917.hashobj);
    new_hashobj__3924[h__3918] = cljs.core.array.call(null, k, v);
    return new cljs.core.HashMap(this__3917.meta, this__3917.count + 1, new_hashobj__3924)
  }
};
cljs.core.HashMap.prototype.cljs$core$IAssociative$_contains_key_QMARK_ = function(coll, k) {
  var this__3925 = this;
  var bucket__3926 = this__3925.hashobj[cljs.core.hash.call(null, k)];
  var i__3927 = cljs.core.truth_(bucket__3926) ? cljs.core.scan_array.call(null, 2, k, bucket__3926) : null;
  if(cljs.core.truth_(i__3927)) {
    return true
  }else {
    return false
  }
};
cljs.core.HashMap.prototype.cljs$core$IFn$ = true;
cljs.core.HashMap.prototype.call = function() {
  var G__3954 = null;
  var G__3954__3955 = function(tsym3928, k) {
    var this__3930 = this;
    var tsym3928__3931 = this;
    var coll__3932 = tsym3928__3931;
    return cljs.core._lookup.call(null, coll__3932, k)
  };
  var G__3954__3956 = function(tsym3929, k, not_found) {
    var this__3933 = this;
    var tsym3929__3934 = this;
    var coll__3935 = tsym3929__3934;
    return cljs.core._lookup.call(null, coll__3935, k, not_found)
  };
  G__3954 = function(tsym3929, k, not_found) {
    switch(arguments.length) {
      case 2:
        return G__3954__3955.call(this, tsym3929, k);
      case 3:
        return G__3954__3956.call(this, tsym3929, k, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3954
}();
cljs.core.HashMap.prototype.cljs$core$ICollection$ = true;
cljs.core.HashMap.prototype.cljs$core$ICollection$_conj = function(coll, entry) {
  var this__3936 = this;
  if(cljs.core.truth_(cljs.core.vector_QMARK_.call(null, entry))) {
    return cljs.core._assoc.call(null, coll, cljs.core._nth.call(null, entry, 0), cljs.core._nth.call(null, entry, 1))
  }else {
    return cljs.core.reduce.call(null, cljs.core._conj, coll, entry)
  }
};
cljs.core.HashMap.prototype.cljs$core$ISeqable$ = true;
cljs.core.HashMap.prototype.cljs$core$ISeqable$_seq = function(coll) {
  var this__3937 = this;
  if(cljs.core.truth_(this__3937.count > 0)) {
    var hashes__3938 = cljs.core.js_keys.call(null, this__3937.hashobj).sort();
    return cljs.core.mapcat.call(null, function(p1__3911_SHARP_) {
      return cljs.core.map.call(null, cljs.core.vec, cljs.core.partition.call(null, 2, this__3937.hashobj[p1__3911_SHARP_]))
    }, hashes__3938)
  }else {
    return null
  }
};
cljs.core.HashMap.prototype.cljs$core$ICounted$ = true;
cljs.core.HashMap.prototype.cljs$core$ICounted$_count = function(coll) {
  var this__3939 = this;
  return this__3939.count
};
cljs.core.HashMap.prototype.cljs$core$IEquiv$ = true;
cljs.core.HashMap.prototype.cljs$core$IEquiv$_equiv = function(coll, other) {
  var this__3940 = this;
  return cljs.core.equiv_map.call(null, coll, other)
};
cljs.core.HashMap.prototype.cljs$core$IWithMeta$ = true;
cljs.core.HashMap.prototype.cljs$core$IWithMeta$_with_meta = function(coll, meta) {
  var this__3941 = this;
  return new cljs.core.HashMap(meta, this__3941.count, this__3941.hashobj)
};
cljs.core.HashMap.prototype.cljs$core$IMeta$ = true;
cljs.core.HashMap.prototype.cljs$core$IMeta$_meta = function(coll) {
  var this__3942 = this;
  return this__3942.meta
};
cljs.core.HashMap.prototype.cljs$core$IEmptyableCollection$ = true;
cljs.core.HashMap.prototype.cljs$core$IEmptyableCollection$_empty = function(coll) {
  var this__3943 = this;
  return cljs.core.with_meta.call(null, cljs.core.HashMap.EMPTY, this__3943.meta)
};
cljs.core.HashMap.prototype.cljs$core$IMap$ = true;
cljs.core.HashMap.prototype.cljs$core$IMap$_dissoc = function(coll, k) {
  var this__3944 = this;
  var h__3945 = cljs.core.hash.call(null, k);
  var bucket__3946 = this__3944.hashobj[h__3945];
  var i__3947 = cljs.core.truth_(bucket__3946) ? cljs.core.scan_array.call(null, 2, k, bucket__3946) : null;
  if(cljs.core.truth_(cljs.core.not.call(null, i__3947))) {
    return coll
  }else {
    var new_hashobj__3948 = goog.object.clone.call(null, this__3944.hashobj);
    if(cljs.core.truth_(3 > bucket__3946.length)) {
      cljs.core.js_delete.call(null, new_hashobj__3948, h__3945)
    }else {
      var new_bucket__3949 = cljs.core.aclone.call(null, bucket__3946);
      new_bucket__3949.splice(i__3947, 2);
      new_hashobj__3948[h__3945] = new_bucket__3949
    }
    return new cljs.core.HashMap(this__3944.meta, this__3944.count - 1, new_hashobj__3948)
  }
};
cljs.core.HashMap;
cljs.core.HashMap.EMPTY = new cljs.core.HashMap(null, 0, cljs.core.js_obj.call(null));
cljs.core.HashMap.fromArrays = function(ks, vs) {
  var len__3958 = ks.length;
  var i__3959 = 0;
  var out__3960 = cljs.core.HashMap.EMPTY;
  while(true) {
    if(cljs.core.truth_(i__3959 < len__3958)) {
      var G__3961 = i__3959 + 1;
      var G__3962 = cljs.core.assoc.call(null, out__3960, ks[i__3959], vs[i__3959]);
      i__3959 = G__3961;
      out__3960 = G__3962;
      continue
    }else {
      return out__3960
    }
    break
  }
};
cljs.core.hash_map = function() {
  var hash_map__delegate = function(keyvals) {
    var in$__3963 = cljs.core.seq.call(null, keyvals);
    var out__3964 = cljs.core.HashMap.EMPTY;
    while(true) {
      if(cljs.core.truth_(in$__3963)) {
        var G__3965 = cljs.core.nnext.call(null, in$__3963);
        var G__3966 = cljs.core.assoc.call(null, out__3964, cljs.core.first.call(null, in$__3963), cljs.core.second.call(null, in$__3963));
        in$__3963 = G__3965;
        out__3964 = G__3966;
        continue
      }else {
        return out__3964
      }
      break
    }
  };
  var hash_map = function(var_args) {
    var keyvals = null;
    if(goog.isDef(var_args)) {
      keyvals = cljs.core.array_seq(Array.prototype.slice.call(arguments, 0), 0)
    }
    return hash_map__delegate.call(this, keyvals)
  };
  hash_map.cljs$lang$maxFixedArity = 0;
  hash_map.cljs$lang$applyTo = function(arglist__3967) {
    var keyvals = cljs.core.seq(arglist__3967);
    return hash_map__delegate.call(this, keyvals)
  };
  return hash_map
}();
cljs.core.keys = function keys(hash_map) {
  return cljs.core.seq.call(null, cljs.core.map.call(null, cljs.core.first, hash_map))
};
cljs.core.vals = function vals(hash_map) {
  return cljs.core.seq.call(null, cljs.core.map.call(null, cljs.core.second, hash_map))
};
cljs.core.merge = function() {
  var merge__delegate = function(maps) {
    if(cljs.core.truth_(cljs.core.some.call(null, cljs.core.identity, maps))) {
      return cljs.core.reduce.call(null, function(p1__3968_SHARP_, p2__3969_SHARP_) {
        return cljs.core.conj.call(null, function() {
          var or__3548__auto____3970 = p1__3968_SHARP_;
          if(cljs.core.truth_(or__3548__auto____3970)) {
            return or__3548__auto____3970
          }else {
            return cljs.core.ObjMap.fromObject([], {})
          }
        }(), p2__3969_SHARP_)
      }, maps)
    }else {
      return null
    }
  };
  var merge = function(var_args) {
    var maps = null;
    if(goog.isDef(var_args)) {
      maps = cljs.core.array_seq(Array.prototype.slice.call(arguments, 0), 0)
    }
    return merge__delegate.call(this, maps)
  };
  merge.cljs$lang$maxFixedArity = 0;
  merge.cljs$lang$applyTo = function(arglist__3971) {
    var maps = cljs.core.seq(arglist__3971);
    return merge__delegate.call(this, maps)
  };
  return merge
}();
cljs.core.merge_with = function() {
  var merge_with__delegate = function(f, maps) {
    if(cljs.core.truth_(cljs.core.some.call(null, cljs.core.identity, maps))) {
      var merge_entry__3974 = function(m, e) {
        var k__3972 = cljs.core.first.call(null, e);
        var v__3973 = cljs.core.second.call(null, e);
        if(cljs.core.truth_(cljs.core.contains_QMARK_.call(null, m, k__3972))) {
          return cljs.core.assoc.call(null, m, k__3972, f.call(null, cljs.core.get.call(null, m, k__3972), v__3973))
        }else {
          return cljs.core.assoc.call(null, m, k__3972, v__3973)
        }
      };
      var merge2__3976 = function(m1, m2) {
        return cljs.core.reduce.call(null, merge_entry__3974, function() {
          var or__3548__auto____3975 = m1;
          if(cljs.core.truth_(or__3548__auto____3975)) {
            return or__3548__auto____3975
          }else {
            return cljs.core.ObjMap.fromObject([], {})
          }
        }(), cljs.core.seq.call(null, m2))
      };
      return cljs.core.reduce.call(null, merge2__3976, maps)
    }else {
      return null
    }
  };
  var merge_with = function(f, var_args) {
    var maps = null;
    if(goog.isDef(var_args)) {
      maps = cljs.core.array_seq(Array.prototype.slice.call(arguments, 1), 0)
    }
    return merge_with__delegate.call(this, f, maps)
  };
  merge_with.cljs$lang$maxFixedArity = 1;
  merge_with.cljs$lang$applyTo = function(arglist__3977) {
    var f = cljs.core.first(arglist__3977);
    var maps = cljs.core.rest(arglist__3977);
    return merge_with__delegate.call(this, f, maps)
  };
  return merge_with
}();
cljs.core.select_keys = function select_keys(map, keyseq) {
  var ret__3979 = cljs.core.ObjMap.fromObject([], {});
  var keys__3980 = cljs.core.seq.call(null, keyseq);
  while(true) {
    if(cljs.core.truth_(keys__3980)) {
      var key__3981 = cljs.core.first.call(null, keys__3980);
      var entry__3982 = cljs.core.get.call(null, map, key__3981, "\ufdd0'user/not-found");
      var G__3983 = cljs.core.truth_(cljs.core.not_EQ_.call(null, entry__3982, "\ufdd0'user/not-found")) ? cljs.core.assoc.call(null, ret__3979, key__3981, entry__3982) : ret__3979;
      var G__3984 = cljs.core.next.call(null, keys__3980);
      ret__3979 = G__3983;
      keys__3980 = G__3984;
      continue
    }else {
      return ret__3979
    }
    break
  }
};
cljs.core.Set = function(meta, hash_map) {
  this.meta = meta;
  this.hash_map = hash_map
};
cljs.core.Set.cljs$core$IPrintable$_pr_seq = function(this__267__auto__) {
  return cljs.core.list.call(null, "cljs.core.Set")
};
cljs.core.Set.prototype.cljs$core$IHash$ = true;
cljs.core.Set.prototype.cljs$core$IHash$_hash = function(coll) {
  var this__3985 = this;
  return cljs.core.hash_coll.call(null, coll)
};
cljs.core.Set.prototype.cljs$core$ILookup$ = true;
cljs.core.Set.prototype.cljs$core$ILookup$_lookup = function() {
  var G__4006 = null;
  var G__4006__4007 = function(coll, v) {
    var this__3986 = this;
    return cljs.core._lookup.call(null, coll, v, null)
  };
  var G__4006__4008 = function(coll, v, not_found) {
    var this__3987 = this;
    if(cljs.core.truth_(cljs.core._contains_key_QMARK_.call(null, this__3987.hash_map, v))) {
      return v
    }else {
      return not_found
    }
  };
  G__4006 = function(coll, v, not_found) {
    switch(arguments.length) {
      case 2:
        return G__4006__4007.call(this, coll, v);
      case 3:
        return G__4006__4008.call(this, coll, v, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__4006
}();
cljs.core.Set.prototype.cljs$core$IFn$ = true;
cljs.core.Set.prototype.call = function() {
  var G__4010 = null;
  var G__4010__4011 = function(tsym3988, k) {
    var this__3990 = this;
    var tsym3988__3991 = this;
    var coll__3992 = tsym3988__3991;
    return cljs.core._lookup.call(null, coll__3992, k)
  };
  var G__4010__4012 = function(tsym3989, k, not_found) {
    var this__3993 = this;
    var tsym3989__3994 = this;
    var coll__3995 = tsym3989__3994;
    return cljs.core._lookup.call(null, coll__3995, k, not_found)
  };
  G__4010 = function(tsym3989, k, not_found) {
    switch(arguments.length) {
      case 2:
        return G__4010__4011.call(this, tsym3989, k);
      case 3:
        return G__4010__4012.call(this, tsym3989, k, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__4010
}();
cljs.core.Set.prototype.cljs$core$ICollection$ = true;
cljs.core.Set.prototype.cljs$core$ICollection$_conj = function(coll, o) {
  var this__3996 = this;
  return new cljs.core.Set(this__3996.meta, cljs.core.assoc.call(null, this__3996.hash_map, o, null))
};
cljs.core.Set.prototype.cljs$core$ISeqable$ = true;
cljs.core.Set.prototype.cljs$core$ISeqable$_seq = function(coll) {
  var this__3997 = this;
  return cljs.core.keys.call(null, this__3997.hash_map)
};
cljs.core.Set.prototype.cljs$core$ISet$ = true;
cljs.core.Set.prototype.cljs$core$ISet$_disjoin = function(coll, v) {
  var this__3998 = this;
  return new cljs.core.Set(this__3998.meta, cljs.core.dissoc.call(null, this__3998.hash_map, v))
};
cljs.core.Set.prototype.cljs$core$ICounted$ = true;
cljs.core.Set.prototype.cljs$core$ICounted$_count = function(coll) {
  var this__3999 = this;
  return cljs.core.count.call(null, cljs.core.seq.call(null, coll))
};
cljs.core.Set.prototype.cljs$core$IEquiv$ = true;
cljs.core.Set.prototype.cljs$core$IEquiv$_equiv = function(coll, other) {
  var this__4000 = this;
  var and__3546__auto____4001 = cljs.core.set_QMARK_.call(null, other);
  if(cljs.core.truth_(and__3546__auto____4001)) {
    var and__3546__auto____4002 = cljs.core._EQ_.call(null, cljs.core.count.call(null, coll), cljs.core.count.call(null, other));
    if(cljs.core.truth_(and__3546__auto____4002)) {
      return cljs.core.every_QMARK_.call(null, function(p1__3978_SHARP_) {
        return cljs.core.contains_QMARK_.call(null, coll, p1__3978_SHARP_)
      }, other)
    }else {
      return and__3546__auto____4002
    }
  }else {
    return and__3546__auto____4001
  }
};
cljs.core.Set.prototype.cljs$core$IWithMeta$ = true;
cljs.core.Set.prototype.cljs$core$IWithMeta$_with_meta = function(coll, meta) {
  var this__4003 = this;
  return new cljs.core.Set(meta, this__4003.hash_map)
};
cljs.core.Set.prototype.cljs$core$IMeta$ = true;
cljs.core.Set.prototype.cljs$core$IMeta$_meta = function(coll) {
  var this__4004 = this;
  return this__4004.meta
};
cljs.core.Set.prototype.cljs$core$IEmptyableCollection$ = true;
cljs.core.Set.prototype.cljs$core$IEmptyableCollection$_empty = function(coll) {
  var this__4005 = this;
  return cljs.core.with_meta.call(null, cljs.core.Set.EMPTY, this__4005.meta)
};
cljs.core.Set;
cljs.core.Set.EMPTY = new cljs.core.Set(null, cljs.core.hash_map.call(null));
cljs.core.set = function set(coll) {
  var in$__4015 = cljs.core.seq.call(null, coll);
  var out__4016 = cljs.core.Set.EMPTY;
  while(true) {
    if(cljs.core.truth_(cljs.core.not.call(null, cljs.core.empty_QMARK_.call(null, in$__4015)))) {
      var G__4017 = cljs.core.rest.call(null, in$__4015);
      var G__4018 = cljs.core.conj.call(null, out__4016, cljs.core.first.call(null, in$__4015));
      in$__4015 = G__4017;
      out__4016 = G__4018;
      continue
    }else {
      return out__4016
    }
    break
  }
};
cljs.core.replace = function replace(smap, coll) {
  if(cljs.core.truth_(cljs.core.vector_QMARK_.call(null, coll))) {
    var n__4019 = cljs.core.count.call(null, coll);
    return cljs.core.reduce.call(null, function(v, i) {
      var temp__3695__auto____4020 = cljs.core.find.call(null, smap, cljs.core.nth.call(null, v, i));
      if(cljs.core.truth_(temp__3695__auto____4020)) {
        var e__4021 = temp__3695__auto____4020;
        return cljs.core.assoc.call(null, v, i, cljs.core.second.call(null, e__4021))
      }else {
        return v
      }
    }, coll, cljs.core.take.call(null, n__4019, cljs.core.iterate.call(null, cljs.core.inc, 0)))
  }else {
    return cljs.core.map.call(null, function(p1__4014_SHARP_) {
      var temp__3695__auto____4022 = cljs.core.find.call(null, smap, p1__4014_SHARP_);
      if(cljs.core.truth_(temp__3695__auto____4022)) {
        var e__4023 = temp__3695__auto____4022;
        return cljs.core.second.call(null, e__4023)
      }else {
        return p1__4014_SHARP_
      }
    }, coll)
  }
};
cljs.core.distinct = function distinct(coll) {
  var step__4031 = function step(xs, seen) {
    return new cljs.core.LazySeq(null, false, function() {
      return function(p__4024, seen) {
        while(true) {
          var vec__4025__4026 = p__4024;
          var f__4027 = cljs.core.nth.call(null, vec__4025__4026, 0, null);
          var xs__4028 = vec__4025__4026;
          var temp__3698__auto____4029 = cljs.core.seq.call(null, xs__4028);
          if(cljs.core.truth_(temp__3698__auto____4029)) {
            var s__4030 = temp__3698__auto____4029;
            if(cljs.core.truth_(cljs.core.contains_QMARK_.call(null, seen, f__4027))) {
              var G__4032 = cljs.core.rest.call(null, s__4030);
              var G__4033 = seen;
              p__4024 = G__4032;
              seen = G__4033;
              continue
            }else {
              return cljs.core.cons.call(null, f__4027, step.call(null, cljs.core.rest.call(null, s__4030), cljs.core.conj.call(null, seen, f__4027)))
            }
          }else {
            return null
          }
          break
        }
      }.call(null, xs, seen)
    })
  };
  return step__4031.call(null, coll, cljs.core.set([]))
};
cljs.core.butlast = function butlast(s) {
  var ret__4034 = cljs.core.Vector.fromArray([]);
  var s__4035 = s;
  while(true) {
    if(cljs.core.truth_(cljs.core.next.call(null, s__4035))) {
      var G__4036 = cljs.core.conj.call(null, ret__4034, cljs.core.first.call(null, s__4035));
      var G__4037 = cljs.core.next.call(null, s__4035);
      ret__4034 = G__4036;
      s__4035 = G__4037;
      continue
    }else {
      return cljs.core.seq.call(null, ret__4034)
    }
    break
  }
};
cljs.core.name = function name(x) {
  if(cljs.core.truth_(cljs.core.string_QMARK_.call(null, x))) {
    return x
  }else {
    if(cljs.core.truth_(function() {
      var or__3548__auto____4038 = cljs.core.keyword_QMARK_.call(null, x);
      if(cljs.core.truth_(or__3548__auto____4038)) {
        return or__3548__auto____4038
      }else {
        return cljs.core.symbol_QMARK_.call(null, x)
      }
    }())) {
      var i__4039 = x.lastIndexOf("/");
      if(cljs.core.truth_(i__4039 < 0)) {
        return cljs.core.subs.call(null, x, 2)
      }else {
        return cljs.core.subs.call(null, x, i__4039 + 1)
      }
    }else {
      if(cljs.core.truth_("\ufdd0'else")) {
        throw new Error(cljs.core.str.call(null, "Doesn't support name: ", x));
      }else {
        return null
      }
    }
  }
};
cljs.core.namespace = function namespace(x) {
  if(cljs.core.truth_(function() {
    var or__3548__auto____4040 = cljs.core.keyword_QMARK_.call(null, x);
    if(cljs.core.truth_(or__3548__auto____4040)) {
      return or__3548__auto____4040
    }else {
      return cljs.core.symbol_QMARK_.call(null, x)
    }
  }())) {
    var i__4041 = x.lastIndexOf("/");
    if(cljs.core.truth_(i__4041 > -1)) {
      return cljs.core.subs.call(null, x, 2, i__4041)
    }else {
      return null
    }
  }else {
    throw new Error(cljs.core.str.call(null, "Doesn't support namespace: ", x));
  }
};
cljs.core.zipmap = function zipmap(keys, vals) {
  var map__4044 = cljs.core.ObjMap.fromObject([], {});
  var ks__4045 = cljs.core.seq.call(null, keys);
  var vs__4046 = cljs.core.seq.call(null, vals);
  while(true) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____4047 = ks__4045;
      if(cljs.core.truth_(and__3546__auto____4047)) {
        return vs__4046
      }else {
        return and__3546__auto____4047
      }
    }())) {
      var G__4048 = cljs.core.assoc.call(null, map__4044, cljs.core.first.call(null, ks__4045), cljs.core.first.call(null, vs__4046));
      var G__4049 = cljs.core.next.call(null, ks__4045);
      var G__4050 = cljs.core.next.call(null, vs__4046);
      map__4044 = G__4048;
      ks__4045 = G__4049;
      vs__4046 = G__4050;
      continue
    }else {
      return map__4044
    }
    break
  }
};
cljs.core.max_key = function() {
  var max_key = null;
  var max_key__4053 = function(k, x) {
    return x
  };
  var max_key__4054 = function(k, x, y) {
    if(cljs.core.truth_(k.call(null, x) > k.call(null, y))) {
      return x
    }else {
      return y
    }
  };
  var max_key__4055 = function() {
    var G__4057__delegate = function(k, x, y, more) {
      return cljs.core.reduce.call(null, function(p1__4042_SHARP_, p2__4043_SHARP_) {
        return max_key.call(null, k, p1__4042_SHARP_, p2__4043_SHARP_)
      }, max_key.call(null, k, x, y), more)
    };
    var G__4057 = function(k, x, y, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
      }
      return G__4057__delegate.call(this, k, x, y, more)
    };
    G__4057.cljs$lang$maxFixedArity = 3;
    G__4057.cljs$lang$applyTo = function(arglist__4058) {
      var k = cljs.core.first(arglist__4058);
      var x = cljs.core.first(cljs.core.next(arglist__4058));
      var y = cljs.core.first(cljs.core.next(cljs.core.next(arglist__4058)));
      var more = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__4058)));
      return G__4057__delegate.call(this, k, x, y, more)
    };
    return G__4057
  }();
  max_key = function(k, x, y, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 2:
        return max_key__4053.call(this, k, x);
      case 3:
        return max_key__4054.call(this, k, x, y);
      default:
        return max_key__4055.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  max_key.cljs$lang$maxFixedArity = 3;
  max_key.cljs$lang$applyTo = max_key__4055.cljs$lang$applyTo;
  return max_key
}();
cljs.core.min_key = function() {
  var min_key = null;
  var min_key__4059 = function(k, x) {
    return x
  };
  var min_key__4060 = function(k, x, y) {
    if(cljs.core.truth_(k.call(null, x) < k.call(null, y))) {
      return x
    }else {
      return y
    }
  };
  var min_key__4061 = function() {
    var G__4063__delegate = function(k, x, y, more) {
      return cljs.core.reduce.call(null, function(p1__4051_SHARP_, p2__4052_SHARP_) {
        return min_key.call(null, k, p1__4051_SHARP_, p2__4052_SHARP_)
      }, min_key.call(null, k, x, y), more)
    };
    var G__4063 = function(k, x, y, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
      }
      return G__4063__delegate.call(this, k, x, y, more)
    };
    G__4063.cljs$lang$maxFixedArity = 3;
    G__4063.cljs$lang$applyTo = function(arglist__4064) {
      var k = cljs.core.first(arglist__4064);
      var x = cljs.core.first(cljs.core.next(arglist__4064));
      var y = cljs.core.first(cljs.core.next(cljs.core.next(arglist__4064)));
      var more = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__4064)));
      return G__4063__delegate.call(this, k, x, y, more)
    };
    return G__4063
  }();
  min_key = function(k, x, y, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 2:
        return min_key__4059.call(this, k, x);
      case 3:
        return min_key__4060.call(this, k, x, y);
      default:
        return min_key__4061.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  min_key.cljs$lang$maxFixedArity = 3;
  min_key.cljs$lang$applyTo = min_key__4061.cljs$lang$applyTo;
  return min_key
}();
cljs.core.partition_all = function() {
  var partition_all = null;
  var partition_all__4067 = function(n, coll) {
    return partition_all.call(null, n, n, coll)
  };
  var partition_all__4068 = function(n, step, coll) {
    return new cljs.core.LazySeq(null, false, function() {
      var temp__3698__auto____4065 = cljs.core.seq.call(null, coll);
      if(cljs.core.truth_(temp__3698__auto____4065)) {
        var s__4066 = temp__3698__auto____4065;
        return cljs.core.cons.call(null, cljs.core.take.call(null, n, s__4066), partition_all.call(null, n, step, cljs.core.drop.call(null, step, s__4066)))
      }else {
        return null
      }
    })
  };
  partition_all = function(n, step, coll) {
    switch(arguments.length) {
      case 2:
        return partition_all__4067.call(this, n, step);
      case 3:
        return partition_all__4068.call(this, n, step, coll)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return partition_all
}();
cljs.core.take_while = function take_while(pred, coll) {
  return new cljs.core.LazySeq(null, false, function() {
    var temp__3698__auto____4070 = cljs.core.seq.call(null, coll);
    if(cljs.core.truth_(temp__3698__auto____4070)) {
      var s__4071 = temp__3698__auto____4070;
      if(cljs.core.truth_(pred.call(null, cljs.core.first.call(null, s__4071)))) {
        return cljs.core.cons.call(null, cljs.core.first.call(null, s__4071), take_while.call(null, pred, cljs.core.rest.call(null, s__4071)))
      }else {
        return null
      }
    }else {
      return null
    }
  })
};
cljs.core.Range = function(meta, start, end, step) {
  this.meta = meta;
  this.start = start;
  this.end = end;
  this.step = step
};
cljs.core.Range.cljs$core$IPrintable$_pr_seq = function(this__267__auto__) {
  return cljs.core.list.call(null, "cljs.core.Range")
};
cljs.core.Range.prototype.cljs$core$IHash$ = true;
cljs.core.Range.prototype.cljs$core$IHash$_hash = function(rng) {
  var this__4072 = this;
  return cljs.core.hash_coll.call(null, rng)
};
cljs.core.Range.prototype.cljs$core$ISequential$ = true;
cljs.core.Range.prototype.cljs$core$ICollection$ = true;
cljs.core.Range.prototype.cljs$core$ICollection$_conj = function(rng, o) {
  var this__4073 = this;
  return cljs.core.cons.call(null, o, rng)
};
cljs.core.Range.prototype.cljs$core$IReduce$ = true;
cljs.core.Range.prototype.cljs$core$IReduce$_reduce = function() {
  var G__4089 = null;
  var G__4089__4090 = function(rng, f) {
    var this__4074 = this;
    return cljs.core.ci_reduce.call(null, rng, f)
  };
  var G__4089__4091 = function(rng, f, s) {
    var this__4075 = this;
    return cljs.core.ci_reduce.call(null, rng, f, s)
  };
  G__4089 = function(rng, f, s) {
    switch(arguments.length) {
      case 2:
        return G__4089__4090.call(this, rng, f);
      case 3:
        return G__4089__4091.call(this, rng, f, s)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__4089
}();
cljs.core.Range.prototype.cljs$core$ISeqable$ = true;
cljs.core.Range.prototype.cljs$core$ISeqable$_seq = function(rng) {
  var this__4076 = this;
  var comp__4077 = cljs.core.truth_(this__4076.step > 0) ? cljs.core._LT_ : cljs.core._GT_;
  if(cljs.core.truth_(comp__4077.call(null, this__4076.start, this__4076.end))) {
    return rng
  }else {
    return null
  }
};
cljs.core.Range.prototype.cljs$core$ICounted$ = true;
cljs.core.Range.prototype.cljs$core$ICounted$_count = function(rng) {
  var this__4078 = this;
  if(cljs.core.truth_(cljs.core.not.call(null, cljs.core._seq.call(null, rng)))) {
    return 0
  }else {
    return Math["ceil"].call(null, (this__4078.end - this__4078.start) / this__4078.step)
  }
};
cljs.core.Range.prototype.cljs$core$ISeq$ = true;
cljs.core.Range.prototype.cljs$core$ISeq$_first = function(rng) {
  var this__4079 = this;
  return this__4079.start
};
cljs.core.Range.prototype.cljs$core$ISeq$_rest = function(rng) {
  var this__4080 = this;
  if(cljs.core.truth_(cljs.core._seq.call(null, rng))) {
    return new cljs.core.Range(this__4080.meta, this__4080.start + this__4080.step, this__4080.end, this__4080.step)
  }else {
    return cljs.core.list.call(null)
  }
};
cljs.core.Range.prototype.cljs$core$IEquiv$ = true;
cljs.core.Range.prototype.cljs$core$IEquiv$_equiv = function(rng, other) {
  var this__4081 = this;
  return cljs.core.equiv_sequential.call(null, rng, other)
};
cljs.core.Range.prototype.cljs$core$IWithMeta$ = true;
cljs.core.Range.prototype.cljs$core$IWithMeta$_with_meta = function(rng, meta) {
  var this__4082 = this;
  return new cljs.core.Range(meta, this__4082.start, this__4082.end, this__4082.step)
};
cljs.core.Range.prototype.cljs$core$IMeta$ = true;
cljs.core.Range.prototype.cljs$core$IMeta$_meta = function(rng) {
  var this__4083 = this;
  return this__4083.meta
};
cljs.core.Range.prototype.cljs$core$IIndexed$ = true;
cljs.core.Range.prototype.cljs$core$IIndexed$_nth = function() {
  var G__4093 = null;
  var G__4093__4094 = function(rng, n) {
    var this__4084 = this;
    if(cljs.core.truth_(n < cljs.core._count.call(null, rng))) {
      return this__4084.start + n * this__4084.step
    }else {
      if(cljs.core.truth_(function() {
        var and__3546__auto____4085 = this__4084.start > this__4084.end;
        if(cljs.core.truth_(and__3546__auto____4085)) {
          return cljs.core._EQ_.call(null, this__4084.step, 0)
        }else {
          return and__3546__auto____4085
        }
      }())) {
        return this__4084.start
      }else {
        throw new Error("Index out of bounds");
      }
    }
  };
  var G__4093__4095 = function(rng, n, not_found) {
    var this__4086 = this;
    if(cljs.core.truth_(n < cljs.core._count.call(null, rng))) {
      return this__4086.start + n * this__4086.step
    }else {
      if(cljs.core.truth_(function() {
        var and__3546__auto____4087 = this__4086.start > this__4086.end;
        if(cljs.core.truth_(and__3546__auto____4087)) {
          return cljs.core._EQ_.call(null, this__4086.step, 0)
        }else {
          return and__3546__auto____4087
        }
      }())) {
        return this__4086.start
      }else {
        return not_found
      }
    }
  };
  G__4093 = function(rng, n, not_found) {
    switch(arguments.length) {
      case 2:
        return G__4093__4094.call(this, rng, n);
      case 3:
        return G__4093__4095.call(this, rng, n, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__4093
}();
cljs.core.Range.prototype.cljs$core$IEmptyableCollection$ = true;
cljs.core.Range.prototype.cljs$core$IEmptyableCollection$_empty = function(rng) {
  var this__4088 = this;
  return cljs.core.with_meta.call(null, cljs.core.List.EMPTY, this__4088.meta)
};
cljs.core.Range;
cljs.core.range = function() {
  var range = null;
  var range__4097 = function() {
    return range.call(null, 0, Number["MAX_VALUE"], 1)
  };
  var range__4098 = function(end) {
    return range.call(null, 0, end, 1)
  };
  var range__4099 = function(start, end) {
    return range.call(null, start, end, 1)
  };
  var range__4100 = function(start, end, step) {
    return new cljs.core.Range(null, start, end, step)
  };
  range = function(start, end, step) {
    switch(arguments.length) {
      case 0:
        return range__4097.call(this);
      case 1:
        return range__4098.call(this, start);
      case 2:
        return range__4099.call(this, start, end);
      case 3:
        return range__4100.call(this, start, end, step)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return range
}();
cljs.core.take_nth = function take_nth(n, coll) {
  return new cljs.core.LazySeq(null, false, function() {
    var temp__3698__auto____4102 = cljs.core.seq.call(null, coll);
    if(cljs.core.truth_(temp__3698__auto____4102)) {
      var s__4103 = temp__3698__auto____4102;
      return cljs.core.cons.call(null, cljs.core.first.call(null, s__4103), take_nth.call(null, n, cljs.core.drop.call(null, n, s__4103)))
    }else {
      return null
    }
  })
};
cljs.core.split_with = function split_with(pred, coll) {
  return cljs.core.Vector.fromArray([cljs.core.take_while.call(null, pred, coll), cljs.core.drop_while.call(null, pred, coll)])
};
cljs.core.partition_by = function partition_by(f, coll) {
  return new cljs.core.LazySeq(null, false, function() {
    var temp__3698__auto____4105 = cljs.core.seq.call(null, coll);
    if(cljs.core.truth_(temp__3698__auto____4105)) {
      var s__4106 = temp__3698__auto____4105;
      var fst__4107 = cljs.core.first.call(null, s__4106);
      var fv__4108 = f.call(null, fst__4107);
      var run__4109 = cljs.core.cons.call(null, fst__4107, cljs.core.take_while.call(null, function(p1__4104_SHARP_) {
        return cljs.core._EQ_.call(null, fv__4108, f.call(null, p1__4104_SHARP_))
      }, cljs.core.next.call(null, s__4106)));
      return cljs.core.cons.call(null, run__4109, partition_by.call(null, f, cljs.core.seq.call(null, cljs.core.drop.call(null, cljs.core.count.call(null, run__4109), s__4106))))
    }else {
      return null
    }
  })
};
cljs.core.frequencies = function frequencies(coll) {
  return cljs.core.reduce.call(null, function(counts, x) {
    return cljs.core.assoc.call(null, counts, x, cljs.core.get.call(null, counts, x, 0) + 1)
  }, cljs.core.ObjMap.fromObject([], {}), coll)
};
cljs.core.reductions = function() {
  var reductions = null;
  var reductions__4124 = function(f, coll) {
    return new cljs.core.LazySeq(null, false, function() {
      var temp__3695__auto____4120 = cljs.core.seq.call(null, coll);
      if(cljs.core.truth_(temp__3695__auto____4120)) {
        var s__4121 = temp__3695__auto____4120;
        return reductions.call(null, f, cljs.core.first.call(null, s__4121), cljs.core.rest.call(null, s__4121))
      }else {
        return cljs.core.list.call(null, f.call(null))
      }
    })
  };
  var reductions__4125 = function(f, init, coll) {
    return cljs.core.cons.call(null, init, new cljs.core.LazySeq(null, false, function() {
      var temp__3698__auto____4122 = cljs.core.seq.call(null, coll);
      if(cljs.core.truth_(temp__3698__auto____4122)) {
        var s__4123 = temp__3698__auto____4122;
        return reductions.call(null, f, f.call(null, init, cljs.core.first.call(null, s__4123)), cljs.core.rest.call(null, s__4123))
      }else {
        return null
      }
    }))
  };
  reductions = function(f, init, coll) {
    switch(arguments.length) {
      case 2:
        return reductions__4124.call(this, f, init);
      case 3:
        return reductions__4125.call(this, f, init, coll)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return reductions
}();
cljs.core.juxt = function() {
  var juxt = null;
  var juxt__4128 = function(f) {
    return function() {
      var G__4133 = null;
      var G__4133__4134 = function() {
        return cljs.core.vector.call(null, f.call(null))
      };
      var G__4133__4135 = function(x) {
        return cljs.core.vector.call(null, f.call(null, x))
      };
      var G__4133__4136 = function(x, y) {
        return cljs.core.vector.call(null, f.call(null, x, y))
      };
      var G__4133__4137 = function(x, y, z) {
        return cljs.core.vector.call(null, f.call(null, x, y, z))
      };
      var G__4133__4138 = function() {
        var G__4140__delegate = function(x, y, z, args) {
          return cljs.core.vector.call(null, cljs.core.apply.call(null, f, x, y, z, args))
        };
        var G__4140 = function(x, y, z, var_args) {
          var args = null;
          if(goog.isDef(var_args)) {
            args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
          }
          return G__4140__delegate.call(this, x, y, z, args)
        };
        G__4140.cljs$lang$maxFixedArity = 3;
        G__4140.cljs$lang$applyTo = function(arglist__4141) {
          var x = cljs.core.first(arglist__4141);
          var y = cljs.core.first(cljs.core.next(arglist__4141));
          var z = cljs.core.first(cljs.core.next(cljs.core.next(arglist__4141)));
          var args = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__4141)));
          return G__4140__delegate.call(this, x, y, z, args)
        };
        return G__4140
      }();
      G__4133 = function(x, y, z, var_args) {
        var args = var_args;
        switch(arguments.length) {
          case 0:
            return G__4133__4134.call(this);
          case 1:
            return G__4133__4135.call(this, x);
          case 2:
            return G__4133__4136.call(this, x, y);
          case 3:
            return G__4133__4137.call(this, x, y, z);
          default:
            return G__4133__4138.apply(this, arguments)
        }
        throw"Invalid arity: " + arguments.length;
      };
      G__4133.cljs$lang$maxFixedArity = 3;
      G__4133.cljs$lang$applyTo = G__4133__4138.cljs$lang$applyTo;
      return G__4133
    }()
  };
  var juxt__4129 = function(f, g) {
    return function() {
      var G__4142 = null;
      var G__4142__4143 = function() {
        return cljs.core.vector.call(null, f.call(null), g.call(null))
      };
      var G__4142__4144 = function(x) {
        return cljs.core.vector.call(null, f.call(null, x), g.call(null, x))
      };
      var G__4142__4145 = function(x, y) {
        return cljs.core.vector.call(null, f.call(null, x, y), g.call(null, x, y))
      };
      var G__4142__4146 = function(x, y, z) {
        return cljs.core.vector.call(null, f.call(null, x, y, z), g.call(null, x, y, z))
      };
      var G__4142__4147 = function() {
        var G__4149__delegate = function(x, y, z, args) {
          return cljs.core.vector.call(null, cljs.core.apply.call(null, f, x, y, z, args), cljs.core.apply.call(null, g, x, y, z, args))
        };
        var G__4149 = function(x, y, z, var_args) {
          var args = null;
          if(goog.isDef(var_args)) {
            args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
          }
          return G__4149__delegate.call(this, x, y, z, args)
        };
        G__4149.cljs$lang$maxFixedArity = 3;
        G__4149.cljs$lang$applyTo = function(arglist__4150) {
          var x = cljs.core.first(arglist__4150);
          var y = cljs.core.first(cljs.core.next(arglist__4150));
          var z = cljs.core.first(cljs.core.next(cljs.core.next(arglist__4150)));
          var args = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__4150)));
          return G__4149__delegate.call(this, x, y, z, args)
        };
        return G__4149
      }();
      G__4142 = function(x, y, z, var_args) {
        var args = var_args;
        switch(arguments.length) {
          case 0:
            return G__4142__4143.call(this);
          case 1:
            return G__4142__4144.call(this, x);
          case 2:
            return G__4142__4145.call(this, x, y);
          case 3:
            return G__4142__4146.call(this, x, y, z);
          default:
            return G__4142__4147.apply(this, arguments)
        }
        throw"Invalid arity: " + arguments.length;
      };
      G__4142.cljs$lang$maxFixedArity = 3;
      G__4142.cljs$lang$applyTo = G__4142__4147.cljs$lang$applyTo;
      return G__4142
    }()
  };
  var juxt__4130 = function(f, g, h) {
    return function() {
      var G__4151 = null;
      var G__4151__4152 = function() {
        return cljs.core.vector.call(null, f.call(null), g.call(null), h.call(null))
      };
      var G__4151__4153 = function(x) {
        return cljs.core.vector.call(null, f.call(null, x), g.call(null, x), h.call(null, x))
      };
      var G__4151__4154 = function(x, y) {
        return cljs.core.vector.call(null, f.call(null, x, y), g.call(null, x, y), h.call(null, x, y))
      };
      var G__4151__4155 = function(x, y, z) {
        return cljs.core.vector.call(null, f.call(null, x, y, z), g.call(null, x, y, z), h.call(null, x, y, z))
      };
      var G__4151__4156 = function() {
        var G__4158__delegate = function(x, y, z, args) {
          return cljs.core.vector.call(null, cljs.core.apply.call(null, f, x, y, z, args), cljs.core.apply.call(null, g, x, y, z, args), cljs.core.apply.call(null, h, x, y, z, args))
        };
        var G__4158 = function(x, y, z, var_args) {
          var args = null;
          if(goog.isDef(var_args)) {
            args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
          }
          return G__4158__delegate.call(this, x, y, z, args)
        };
        G__4158.cljs$lang$maxFixedArity = 3;
        G__4158.cljs$lang$applyTo = function(arglist__4159) {
          var x = cljs.core.first(arglist__4159);
          var y = cljs.core.first(cljs.core.next(arglist__4159));
          var z = cljs.core.first(cljs.core.next(cljs.core.next(arglist__4159)));
          var args = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__4159)));
          return G__4158__delegate.call(this, x, y, z, args)
        };
        return G__4158
      }();
      G__4151 = function(x, y, z, var_args) {
        var args = var_args;
        switch(arguments.length) {
          case 0:
            return G__4151__4152.call(this);
          case 1:
            return G__4151__4153.call(this, x);
          case 2:
            return G__4151__4154.call(this, x, y);
          case 3:
            return G__4151__4155.call(this, x, y, z);
          default:
            return G__4151__4156.apply(this, arguments)
        }
        throw"Invalid arity: " + arguments.length;
      };
      G__4151.cljs$lang$maxFixedArity = 3;
      G__4151.cljs$lang$applyTo = G__4151__4156.cljs$lang$applyTo;
      return G__4151
    }()
  };
  var juxt__4131 = function() {
    var G__4160__delegate = function(f, g, h, fs) {
      var fs__4127 = cljs.core.list_STAR_.call(null, f, g, h, fs);
      return function() {
        var G__4161 = null;
        var G__4161__4162 = function() {
          return cljs.core.reduce.call(null, function(p1__4110_SHARP_, p2__4111_SHARP_) {
            return cljs.core.conj.call(null, p1__4110_SHARP_, p2__4111_SHARP_.call(null))
          }, cljs.core.Vector.fromArray([]), fs__4127)
        };
        var G__4161__4163 = function(x) {
          return cljs.core.reduce.call(null, function(p1__4112_SHARP_, p2__4113_SHARP_) {
            return cljs.core.conj.call(null, p1__4112_SHARP_, p2__4113_SHARP_.call(null, x))
          }, cljs.core.Vector.fromArray([]), fs__4127)
        };
        var G__4161__4164 = function(x, y) {
          return cljs.core.reduce.call(null, function(p1__4114_SHARP_, p2__4115_SHARP_) {
            return cljs.core.conj.call(null, p1__4114_SHARP_, p2__4115_SHARP_.call(null, x, y))
          }, cljs.core.Vector.fromArray([]), fs__4127)
        };
        var G__4161__4165 = function(x, y, z) {
          return cljs.core.reduce.call(null, function(p1__4116_SHARP_, p2__4117_SHARP_) {
            return cljs.core.conj.call(null, p1__4116_SHARP_, p2__4117_SHARP_.call(null, x, y, z))
          }, cljs.core.Vector.fromArray([]), fs__4127)
        };
        var G__4161__4166 = function() {
          var G__4168__delegate = function(x, y, z, args) {
            return cljs.core.reduce.call(null, function(p1__4118_SHARP_, p2__4119_SHARP_) {
              return cljs.core.conj.call(null, p1__4118_SHARP_, cljs.core.apply.call(null, p2__4119_SHARP_, x, y, z, args))
            }, cljs.core.Vector.fromArray([]), fs__4127)
          };
          var G__4168 = function(x, y, z, var_args) {
            var args = null;
            if(goog.isDef(var_args)) {
              args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
            }
            return G__4168__delegate.call(this, x, y, z, args)
          };
          G__4168.cljs$lang$maxFixedArity = 3;
          G__4168.cljs$lang$applyTo = function(arglist__4169) {
            var x = cljs.core.first(arglist__4169);
            var y = cljs.core.first(cljs.core.next(arglist__4169));
            var z = cljs.core.first(cljs.core.next(cljs.core.next(arglist__4169)));
            var args = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__4169)));
            return G__4168__delegate.call(this, x, y, z, args)
          };
          return G__4168
        }();
        G__4161 = function(x, y, z, var_args) {
          var args = var_args;
          switch(arguments.length) {
            case 0:
              return G__4161__4162.call(this);
            case 1:
              return G__4161__4163.call(this, x);
            case 2:
              return G__4161__4164.call(this, x, y);
            case 3:
              return G__4161__4165.call(this, x, y, z);
            default:
              return G__4161__4166.apply(this, arguments)
          }
          throw"Invalid arity: " + arguments.length;
        };
        G__4161.cljs$lang$maxFixedArity = 3;
        G__4161.cljs$lang$applyTo = G__4161__4166.cljs$lang$applyTo;
        return G__4161
      }()
    };
    var G__4160 = function(f, g, h, var_args) {
      var fs = null;
      if(goog.isDef(var_args)) {
        fs = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
      }
      return G__4160__delegate.call(this, f, g, h, fs)
    };
    G__4160.cljs$lang$maxFixedArity = 3;
    G__4160.cljs$lang$applyTo = function(arglist__4170) {
      var f = cljs.core.first(arglist__4170);
      var g = cljs.core.first(cljs.core.next(arglist__4170));
      var h = cljs.core.first(cljs.core.next(cljs.core.next(arglist__4170)));
      var fs = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__4170)));
      return G__4160__delegate.call(this, f, g, h, fs)
    };
    return G__4160
  }();
  juxt = function(f, g, h, var_args) {
    var fs = var_args;
    switch(arguments.length) {
      case 1:
        return juxt__4128.call(this, f);
      case 2:
        return juxt__4129.call(this, f, g);
      case 3:
        return juxt__4130.call(this, f, g, h);
      default:
        return juxt__4131.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  juxt.cljs$lang$maxFixedArity = 3;
  juxt.cljs$lang$applyTo = juxt__4131.cljs$lang$applyTo;
  return juxt
}();
cljs.core.dorun = function() {
  var dorun = null;
  var dorun__4172 = function(coll) {
    while(true) {
      if(cljs.core.truth_(cljs.core.seq.call(null, coll))) {
        var G__4175 = cljs.core.next.call(null, coll);
        coll = G__4175;
        continue
      }else {
        return null
      }
      break
    }
  };
  var dorun__4173 = function(n, coll) {
    while(true) {
      if(cljs.core.truth_(function() {
        var and__3546__auto____4171 = cljs.core.seq.call(null, coll);
        if(cljs.core.truth_(and__3546__auto____4171)) {
          return n > 0
        }else {
          return and__3546__auto____4171
        }
      }())) {
        var G__4176 = n - 1;
        var G__4177 = cljs.core.next.call(null, coll);
        n = G__4176;
        coll = G__4177;
        continue
      }else {
        return null
      }
      break
    }
  };
  dorun = function(n, coll) {
    switch(arguments.length) {
      case 1:
        return dorun__4172.call(this, n);
      case 2:
        return dorun__4173.call(this, n, coll)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return dorun
}();
cljs.core.doall = function() {
  var doall = null;
  var doall__4178 = function(coll) {
    cljs.core.dorun.call(null, coll);
    return coll
  };
  var doall__4179 = function(n, coll) {
    cljs.core.dorun.call(null, n, coll);
    return coll
  };
  doall = function(n, coll) {
    switch(arguments.length) {
      case 1:
        return doall__4178.call(this, n);
      case 2:
        return doall__4179.call(this, n, coll)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return doall
}();
cljs.core.re_matches = function re_matches(re, s) {
  var matches__4181 = re.exec(s);
  if(cljs.core.truth_(cljs.core._EQ_.call(null, cljs.core.first.call(null, matches__4181), s))) {
    if(cljs.core.truth_(cljs.core._EQ_.call(null, cljs.core.count.call(null, matches__4181), 1))) {
      return cljs.core.first.call(null, matches__4181)
    }else {
      return cljs.core.vec.call(null, matches__4181)
    }
  }else {
    return null
  }
};
cljs.core.re_find = function re_find(re, s) {
  var matches__4182 = re.exec(s);
  if(cljs.core.truth_(matches__4182 === null)) {
    return null
  }else {
    if(cljs.core.truth_(cljs.core._EQ_.call(null, cljs.core.count.call(null, matches__4182), 1))) {
      return cljs.core.first.call(null, matches__4182)
    }else {
      return cljs.core.vec.call(null, matches__4182)
    }
  }
};
cljs.core.re_seq = function re_seq(re, s) {
  var match_data__4183 = cljs.core.re_find.call(null, re, s);
  var match_idx__4184 = s.search(re);
  var match_str__4185 = cljs.core.truth_(cljs.core.coll_QMARK_.call(null, match_data__4183)) ? cljs.core.first.call(null, match_data__4183) : match_data__4183;
  var post_match__4186 = cljs.core.subs.call(null, s, match_idx__4184 + cljs.core.count.call(null, match_str__4185));
  if(cljs.core.truth_(match_data__4183)) {
    return new cljs.core.LazySeq(null, false, function() {
      return cljs.core.cons.call(null, match_data__4183, re_seq.call(null, re, post_match__4186))
    })
  }else {
    return null
  }
};
cljs.core.re_pattern = function re_pattern(s) {
  var vec__4188__4189 = cljs.core.re_find.call(null, /^(?:\(\?([idmsux]*)\))?(.*)/, s);
  var ___4190 = cljs.core.nth.call(null, vec__4188__4189, 0, null);
  var flags__4191 = cljs.core.nth.call(null, vec__4188__4189, 1, null);
  var pattern__4192 = cljs.core.nth.call(null, vec__4188__4189, 2, null);
  return new RegExp(pattern__4192, flags__4191)
};
cljs.core.pr_sequential = function pr_sequential(print_one, begin, sep, end, opts, coll) {
  return cljs.core.concat.call(null, cljs.core.Vector.fromArray([begin]), cljs.core.flatten1.call(null, cljs.core.interpose.call(null, cljs.core.Vector.fromArray([sep]), cljs.core.map.call(null, function(p1__4187_SHARP_) {
    return print_one.call(null, p1__4187_SHARP_, opts)
  }, coll))), cljs.core.Vector.fromArray([end]))
};
cljs.core.string_print = function string_print(x) {
  cljs.core._STAR_print_fn_STAR_.call(null, x);
  return null
};
cljs.core.flush = function flush() {
  return null
};
cljs.core.pr_seq = function pr_seq(obj, opts) {
  if(cljs.core.truth_(obj === null)) {
    return cljs.core.list.call(null, "nil")
  }else {
    if(cljs.core.truth_(void 0 === obj)) {
      return cljs.core.list.call(null, "#<undefined>")
    }else {
      if(cljs.core.truth_("\ufdd0'else")) {
        return cljs.core.concat.call(null, cljs.core.truth_(function() {
          var and__3546__auto____4193 = cljs.core.get.call(null, opts, "\ufdd0'meta");
          if(cljs.core.truth_(and__3546__auto____4193)) {
            var and__3546__auto____4197 = function() {
              var x__352__auto____4194 = obj;
              if(cljs.core.truth_(function() {
                var and__3546__auto____4195 = x__352__auto____4194;
                if(cljs.core.truth_(and__3546__auto____4195)) {
                  var and__3546__auto____4196 = x__352__auto____4194.cljs$core$IMeta$;
                  if(cljs.core.truth_(and__3546__auto____4196)) {
                    return cljs.core.not.call(null, x__352__auto____4194.hasOwnProperty("cljs$core$IMeta$"))
                  }else {
                    return and__3546__auto____4196
                  }
                }else {
                  return and__3546__auto____4195
                }
              }())) {
                return true
              }else {
                return cljs.core.type_satisfies_.call(null, cljs.core.IMeta, x__352__auto____4194)
              }
            }();
            if(cljs.core.truth_(and__3546__auto____4197)) {
              return cljs.core.meta.call(null, obj)
            }else {
              return and__3546__auto____4197
            }
          }else {
            return and__3546__auto____4193
          }
        }()) ? cljs.core.concat.call(null, cljs.core.Vector.fromArray(["^"]), pr_seq.call(null, cljs.core.meta.call(null, obj), opts), cljs.core.Vector.fromArray([" "])) : null, cljs.core.truth_(function() {
          var x__352__auto____4198 = obj;
          if(cljs.core.truth_(function() {
            var and__3546__auto____4199 = x__352__auto____4198;
            if(cljs.core.truth_(and__3546__auto____4199)) {
              var and__3546__auto____4200 = x__352__auto____4198.cljs$core$IPrintable$;
              if(cljs.core.truth_(and__3546__auto____4200)) {
                return cljs.core.not.call(null, x__352__auto____4198.hasOwnProperty("cljs$core$IPrintable$"))
              }else {
                return and__3546__auto____4200
              }
            }else {
              return and__3546__auto____4199
            }
          }())) {
            return true
          }else {
            return cljs.core.type_satisfies_.call(null, cljs.core.IPrintable, x__352__auto____4198)
          }
        }()) ? cljs.core._pr_seq.call(null, obj, opts) : cljs.core.list.call(null, "#<", cljs.core.str.call(null, obj), ">"))
      }else {
        return null
      }
    }
  }
};
cljs.core.pr_str_with_opts = function pr_str_with_opts(objs, opts) {
  var first_obj__4201 = cljs.core.first.call(null, objs);
  var sb__4202 = new goog.string.StringBuffer;
  var G__4203__4204 = cljs.core.seq.call(null, objs);
  if(cljs.core.truth_(G__4203__4204)) {
    var obj__4205 = cljs.core.first.call(null, G__4203__4204);
    var G__4203__4206 = G__4203__4204;
    while(true) {
      if(cljs.core.truth_(obj__4205 === first_obj__4201)) {
      }else {
        sb__4202.append(" ")
      }
      var G__4207__4208 = cljs.core.seq.call(null, cljs.core.pr_seq.call(null, obj__4205, opts));
      if(cljs.core.truth_(G__4207__4208)) {
        var string__4209 = cljs.core.first.call(null, G__4207__4208);
        var G__4207__4210 = G__4207__4208;
        while(true) {
          sb__4202.append(string__4209);
          var temp__3698__auto____4211 = cljs.core.next.call(null, G__4207__4210);
          if(cljs.core.truth_(temp__3698__auto____4211)) {
            var G__4207__4212 = temp__3698__auto____4211;
            var G__4215 = cljs.core.first.call(null, G__4207__4212);
            var G__4216 = G__4207__4212;
            string__4209 = G__4215;
            G__4207__4210 = G__4216;
            continue
          }else {
          }
          break
        }
      }else {
      }
      var temp__3698__auto____4213 = cljs.core.next.call(null, G__4203__4206);
      if(cljs.core.truth_(temp__3698__auto____4213)) {
        var G__4203__4214 = temp__3698__auto____4213;
        var G__4217 = cljs.core.first.call(null, G__4203__4214);
        var G__4218 = G__4203__4214;
        obj__4205 = G__4217;
        G__4203__4206 = G__4218;
        continue
      }else {
      }
      break
    }
  }else {
  }
  return cljs.core.str.call(null, sb__4202)
};
cljs.core.pr_with_opts = function pr_with_opts(objs, opts) {
  var first_obj__4219 = cljs.core.first.call(null, objs);
  var G__4220__4221 = cljs.core.seq.call(null, objs);
  if(cljs.core.truth_(G__4220__4221)) {
    var obj__4222 = cljs.core.first.call(null, G__4220__4221);
    var G__4220__4223 = G__4220__4221;
    while(true) {
      if(cljs.core.truth_(obj__4222 === first_obj__4219)) {
      }else {
        cljs.core.string_print.call(null, " ")
      }
      var G__4224__4225 = cljs.core.seq.call(null, cljs.core.pr_seq.call(null, obj__4222, opts));
      if(cljs.core.truth_(G__4224__4225)) {
        var string__4226 = cljs.core.first.call(null, G__4224__4225);
        var G__4224__4227 = G__4224__4225;
        while(true) {
          cljs.core.string_print.call(null, string__4226);
          var temp__3698__auto____4228 = cljs.core.next.call(null, G__4224__4227);
          if(cljs.core.truth_(temp__3698__auto____4228)) {
            var G__4224__4229 = temp__3698__auto____4228;
            var G__4232 = cljs.core.first.call(null, G__4224__4229);
            var G__4233 = G__4224__4229;
            string__4226 = G__4232;
            G__4224__4227 = G__4233;
            continue
          }else {
          }
          break
        }
      }else {
      }
      var temp__3698__auto____4230 = cljs.core.next.call(null, G__4220__4223);
      if(cljs.core.truth_(temp__3698__auto____4230)) {
        var G__4220__4231 = temp__3698__auto____4230;
        var G__4234 = cljs.core.first.call(null, G__4220__4231);
        var G__4235 = G__4220__4231;
        obj__4222 = G__4234;
        G__4220__4223 = G__4235;
        continue
      }else {
        return null
      }
      break
    }
  }else {
    return null
  }
};
cljs.core.newline = function newline(opts) {
  cljs.core.string_print.call(null, "\n");
  if(cljs.core.truth_(cljs.core.get.call(null, opts, "\ufdd0'flush-on-newline"))) {
    return cljs.core.flush.call(null)
  }else {
    return null
  }
};
cljs.core._STAR_flush_on_newline_STAR_ = true;
cljs.core._STAR_print_readably_STAR_ = true;
cljs.core._STAR_print_meta_STAR_ = false;
cljs.core._STAR_print_dup_STAR_ = false;
cljs.core.pr_opts = function pr_opts() {
  return cljs.core.ObjMap.fromObject(["\ufdd0'flush-on-newline", "\ufdd0'readably", "\ufdd0'meta", "\ufdd0'dup"], {"\ufdd0'flush-on-newline":cljs.core._STAR_flush_on_newline_STAR_, "\ufdd0'readably":cljs.core._STAR_print_readably_STAR_, "\ufdd0'meta":cljs.core._STAR_print_meta_STAR_, "\ufdd0'dup":cljs.core._STAR_print_dup_STAR_})
};
cljs.core.pr_str = function() {
  var pr_str__delegate = function(objs) {
    return cljs.core.pr_str_with_opts.call(null, objs, cljs.core.pr_opts.call(null))
  };
  var pr_str = function(var_args) {
    var objs = null;
    if(goog.isDef(var_args)) {
      objs = cljs.core.array_seq(Array.prototype.slice.call(arguments, 0), 0)
    }
    return pr_str__delegate.call(this, objs)
  };
  pr_str.cljs$lang$maxFixedArity = 0;
  pr_str.cljs$lang$applyTo = function(arglist__4236) {
    var objs = cljs.core.seq(arglist__4236);
    return pr_str__delegate.call(this, objs)
  };
  return pr_str
}();
cljs.core.pr = function() {
  var pr__delegate = function(objs) {
    return cljs.core.pr_with_opts.call(null, objs, cljs.core.pr_opts.call(null))
  };
  var pr = function(var_args) {
    var objs = null;
    if(goog.isDef(var_args)) {
      objs = cljs.core.array_seq(Array.prototype.slice.call(arguments, 0), 0)
    }
    return pr__delegate.call(this, objs)
  };
  pr.cljs$lang$maxFixedArity = 0;
  pr.cljs$lang$applyTo = function(arglist__4237) {
    var objs = cljs.core.seq(arglist__4237);
    return pr__delegate.call(this, objs)
  };
  return pr
}();
cljs.core.print = function() {
  var cljs_core_print__delegate = function(objs) {
    return cljs.core.pr_with_opts.call(null, objs, cljs.core.assoc.call(null, cljs.core.pr_opts.call(null), "\ufdd0'readably", false))
  };
  var cljs_core_print = function(var_args) {
    var objs = null;
    if(goog.isDef(var_args)) {
      objs = cljs.core.array_seq(Array.prototype.slice.call(arguments, 0), 0)
    }
    return cljs_core_print__delegate.call(this, objs)
  };
  cljs_core_print.cljs$lang$maxFixedArity = 0;
  cljs_core_print.cljs$lang$applyTo = function(arglist__4238) {
    var objs = cljs.core.seq(arglist__4238);
    return cljs_core_print__delegate.call(this, objs)
  };
  return cljs_core_print
}();
cljs.core.println = function() {
  var println__delegate = function(objs) {
    cljs.core.pr_with_opts.call(null, objs, cljs.core.assoc.call(null, cljs.core.pr_opts.call(null), "\ufdd0'readably", false));
    return cljs.core.newline.call(null, cljs.core.pr_opts.call(null))
  };
  var println = function(var_args) {
    var objs = null;
    if(goog.isDef(var_args)) {
      objs = cljs.core.array_seq(Array.prototype.slice.call(arguments, 0), 0)
    }
    return println__delegate.call(this, objs)
  };
  println.cljs$lang$maxFixedArity = 0;
  println.cljs$lang$applyTo = function(arglist__4239) {
    var objs = cljs.core.seq(arglist__4239);
    return println__delegate.call(this, objs)
  };
  return println
}();
cljs.core.prn = function() {
  var prn__delegate = function(objs) {
    cljs.core.pr_with_opts.call(null, objs, cljs.core.pr_opts.call(null));
    return cljs.core.newline.call(null, cljs.core.pr_opts.call(null))
  };
  var prn = function(var_args) {
    var objs = null;
    if(goog.isDef(var_args)) {
      objs = cljs.core.array_seq(Array.prototype.slice.call(arguments, 0), 0)
    }
    return prn__delegate.call(this, objs)
  };
  prn.cljs$lang$maxFixedArity = 0;
  prn.cljs$lang$applyTo = function(arglist__4240) {
    var objs = cljs.core.seq(arglist__4240);
    return prn__delegate.call(this, objs)
  };
  return prn
}();
cljs.core.HashMap.prototype.cljs$core$IPrintable$ = true;
cljs.core.HashMap.prototype.cljs$core$IPrintable$_pr_seq = function(coll, opts) {
  var pr_pair__4241 = function(keyval) {
    return cljs.core.pr_sequential.call(null, cljs.core.pr_seq, "", " ", "", opts, keyval)
  };
  return cljs.core.pr_sequential.call(null, pr_pair__4241, "{", ", ", "}", opts, coll)
};
cljs.core.IPrintable["number"] = true;
cljs.core._pr_seq["number"] = function(n, opts) {
  return cljs.core.list.call(null, cljs.core.str.call(null, n))
};
cljs.core.IndexedSeq.prototype.cljs$core$IPrintable$ = true;
cljs.core.IndexedSeq.prototype.cljs$core$IPrintable$_pr_seq = function(coll, opts) {
  return cljs.core.pr_sequential.call(null, cljs.core.pr_seq, "(", " ", ")", opts, coll)
};
cljs.core.Subvec.prototype.cljs$core$IPrintable$ = true;
cljs.core.Subvec.prototype.cljs$core$IPrintable$_pr_seq = function(coll, opts) {
  return cljs.core.pr_sequential.call(null, cljs.core.pr_seq, "[", " ", "]", opts, coll)
};
cljs.core.LazySeq.prototype.cljs$core$IPrintable$ = true;
cljs.core.LazySeq.prototype.cljs$core$IPrintable$_pr_seq = function(coll, opts) {
  return cljs.core.pr_sequential.call(null, cljs.core.pr_seq, "(", " ", ")", opts, coll)
};
cljs.core.IPrintable["boolean"] = true;
cljs.core._pr_seq["boolean"] = function(bool, opts) {
  return cljs.core.list.call(null, cljs.core.str.call(null, bool))
};
cljs.core.Set.prototype.cljs$core$IPrintable$ = true;
cljs.core.Set.prototype.cljs$core$IPrintable$_pr_seq = function(coll, opts) {
  return cljs.core.pr_sequential.call(null, cljs.core.pr_seq, "#{", " ", "}", opts, coll)
};
cljs.core.IPrintable["string"] = true;
cljs.core._pr_seq["string"] = function(obj, opts) {
  if(cljs.core.truth_(cljs.core.keyword_QMARK_.call(null, obj))) {
    return cljs.core.list.call(null, cljs.core.str.call(null, ":", function() {
      var temp__3698__auto____4242 = cljs.core.namespace.call(null, obj);
      if(cljs.core.truth_(temp__3698__auto____4242)) {
        var nspc__4243 = temp__3698__auto____4242;
        return cljs.core.str.call(null, nspc__4243, "/")
      }else {
        return null
      }
    }(), cljs.core.name.call(null, obj)))
  }else {
    if(cljs.core.truth_(cljs.core.symbol_QMARK_.call(null, obj))) {
      return cljs.core.list.call(null, cljs.core.str.call(null, function() {
        var temp__3698__auto____4244 = cljs.core.namespace.call(null, obj);
        if(cljs.core.truth_(temp__3698__auto____4244)) {
          var nspc__4245 = temp__3698__auto____4244;
          return cljs.core.str.call(null, nspc__4245, "/")
        }else {
          return null
        }
      }(), cljs.core.name.call(null, obj)))
    }else {
      if(cljs.core.truth_("\ufdd0'else")) {
        return cljs.core.list.call(null, cljs.core.truth_("\ufdd0'readably".call(null, opts)) ? goog.string.quote.call(null, obj) : obj)
      }else {
        return null
      }
    }
  }
};
cljs.core.Vector.prototype.cljs$core$IPrintable$ = true;
cljs.core.Vector.prototype.cljs$core$IPrintable$_pr_seq = function(coll, opts) {
  return cljs.core.pr_sequential.call(null, cljs.core.pr_seq, "[", " ", "]", opts, coll)
};
cljs.core.List.prototype.cljs$core$IPrintable$ = true;
cljs.core.List.prototype.cljs$core$IPrintable$_pr_seq = function(coll, opts) {
  return cljs.core.pr_sequential.call(null, cljs.core.pr_seq, "(", " ", ")", opts, coll)
};
cljs.core.IPrintable["array"] = true;
cljs.core._pr_seq["array"] = function(a, opts) {
  return cljs.core.pr_sequential.call(null, cljs.core.pr_seq, "#<Array [", ", ", "]>", opts, a)
};
cljs.core.PersistentQueueSeq.prototype.cljs$core$IPrintable$ = true;
cljs.core.PersistentQueueSeq.prototype.cljs$core$IPrintable$_pr_seq = function(coll, opts) {
  return cljs.core.pr_sequential.call(null, cljs.core.pr_seq, "(", " ", ")", opts, coll)
};
cljs.core.IPrintable["function"] = true;
cljs.core._pr_seq["function"] = function(this$) {
  return cljs.core.list.call(null, "#<", cljs.core.str.call(null, this$), ">")
};
cljs.core.EmptyList.prototype.cljs$core$IPrintable$ = true;
cljs.core.EmptyList.prototype.cljs$core$IPrintable$_pr_seq = function(coll, opts) {
  return cljs.core.list.call(null, "()")
};
cljs.core.Cons.prototype.cljs$core$IPrintable$ = true;
cljs.core.Cons.prototype.cljs$core$IPrintable$_pr_seq = function(coll, opts) {
  return cljs.core.pr_sequential.call(null, cljs.core.pr_seq, "(", " ", ")", opts, coll)
};
cljs.core.Range.prototype.cljs$core$IPrintable$ = true;
cljs.core.Range.prototype.cljs$core$IPrintable$_pr_seq = function(coll, opts) {
  return cljs.core.pr_sequential.call(null, cljs.core.pr_seq, "(", " ", ")", opts, coll)
};
cljs.core.ObjMap.prototype.cljs$core$IPrintable$ = true;
cljs.core.ObjMap.prototype.cljs$core$IPrintable$_pr_seq = function(coll, opts) {
  var pr_pair__4246 = function(keyval) {
    return cljs.core.pr_sequential.call(null, cljs.core.pr_seq, "", " ", "", opts, keyval)
  };
  return cljs.core.pr_sequential.call(null, pr_pair__4246, "{", ", ", "}", opts, coll)
};
cljs.core.Atom = function(state, meta, validator, watches) {
  this.state = state;
  this.meta = meta;
  this.validator = validator;
  this.watches = watches
};
cljs.core.Atom.cljs$core$IPrintable$_pr_seq = function(this__267__auto__) {
  return cljs.core.list.call(null, "cljs.core.Atom")
};
cljs.core.Atom.prototype.cljs$core$IHash$ = true;
cljs.core.Atom.prototype.cljs$core$IHash$_hash = function(this$) {
  var this__4247 = this;
  return goog.getUid.call(null, this$)
};
cljs.core.Atom.prototype.cljs$core$IWatchable$ = true;
cljs.core.Atom.prototype.cljs$core$IWatchable$_notify_watches = function(this$, oldval, newval) {
  var this__4248 = this;
  var G__4249__4250 = cljs.core.seq.call(null, this__4248.watches);
  if(cljs.core.truth_(G__4249__4250)) {
    var G__4252__4254 = cljs.core.first.call(null, G__4249__4250);
    var vec__4253__4255 = G__4252__4254;
    var key__4256 = cljs.core.nth.call(null, vec__4253__4255, 0, null);
    var f__4257 = cljs.core.nth.call(null, vec__4253__4255, 1, null);
    var G__4249__4258 = G__4249__4250;
    var G__4252__4259 = G__4252__4254;
    var G__4249__4260 = G__4249__4258;
    while(true) {
      var vec__4261__4262 = G__4252__4259;
      var key__4263 = cljs.core.nth.call(null, vec__4261__4262, 0, null);
      var f__4264 = cljs.core.nth.call(null, vec__4261__4262, 1, null);
      var G__4249__4265 = G__4249__4260;
      f__4264.call(null, key__4263, this$, oldval, newval);
      var temp__3698__auto____4266 = cljs.core.next.call(null, G__4249__4265);
      if(cljs.core.truth_(temp__3698__auto____4266)) {
        var G__4249__4267 = temp__3698__auto____4266;
        var G__4274 = cljs.core.first.call(null, G__4249__4267);
        var G__4275 = G__4249__4267;
        G__4252__4259 = G__4274;
        G__4249__4260 = G__4275;
        continue
      }else {
        return null
      }
      break
    }
  }else {
    return null
  }
};
cljs.core.Atom.prototype.cljs$core$IWatchable$_add_watch = function(this$, key, f) {
  var this__4268 = this;
  return this$.watches = cljs.core.assoc.call(null, this__4268.watches, key, f)
};
cljs.core.Atom.prototype.cljs$core$IWatchable$_remove_watch = function(this$, key) {
  var this__4269 = this;
  return this$.watches = cljs.core.dissoc.call(null, this__4269.watches, key)
};
cljs.core.Atom.prototype.cljs$core$IPrintable$ = true;
cljs.core.Atom.prototype.cljs$core$IPrintable$_pr_seq = function(a, opts) {
  var this__4270 = this;
  return cljs.core.concat.call(null, cljs.core.Vector.fromArray(["#<Atom: "]), cljs.core._pr_seq.call(null, this__4270.state, opts), ">")
};
cljs.core.Atom.prototype.cljs$core$IMeta$ = true;
cljs.core.Atom.prototype.cljs$core$IMeta$_meta = function(_) {
  var this__4271 = this;
  return this__4271.meta
};
cljs.core.Atom.prototype.cljs$core$IDeref$ = true;
cljs.core.Atom.prototype.cljs$core$IDeref$_deref = function(_) {
  var this__4272 = this;
  return this__4272.state
};
cljs.core.Atom.prototype.cljs$core$IEquiv$ = true;
cljs.core.Atom.prototype.cljs$core$IEquiv$_equiv = function(o, other) {
  var this__4273 = this;
  return o === other
};
cljs.core.Atom;
cljs.core.atom = function() {
  var atom = null;
  var atom__4282 = function(x) {
    return new cljs.core.Atom(x, null, null, null)
  };
  var atom__4283 = function() {
    var G__4285__delegate = function(x, p__4276) {
      var map__4277__4278 = p__4276;
      var map__4277__4279 = cljs.core.truth_(cljs.core.seq_QMARK_.call(null, map__4277__4278)) ? cljs.core.apply.call(null, cljs.core.hash_map, map__4277__4278) : map__4277__4278;
      var validator__4280 = cljs.core.get.call(null, map__4277__4279, "\ufdd0'validator");
      var meta__4281 = cljs.core.get.call(null, map__4277__4279, "\ufdd0'meta");
      return new cljs.core.Atom(x, meta__4281, validator__4280, null)
    };
    var G__4285 = function(x, var_args) {
      var p__4276 = null;
      if(goog.isDef(var_args)) {
        p__4276 = cljs.core.array_seq(Array.prototype.slice.call(arguments, 1), 0)
      }
      return G__4285__delegate.call(this, x, p__4276)
    };
    G__4285.cljs$lang$maxFixedArity = 1;
    G__4285.cljs$lang$applyTo = function(arglist__4286) {
      var x = cljs.core.first(arglist__4286);
      var p__4276 = cljs.core.rest(arglist__4286);
      return G__4285__delegate.call(this, x, p__4276)
    };
    return G__4285
  }();
  atom = function(x, var_args) {
    var p__4276 = var_args;
    switch(arguments.length) {
      case 1:
        return atom__4282.call(this, x);
      default:
        return atom__4283.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  atom.cljs$lang$maxFixedArity = 1;
  atom.cljs$lang$applyTo = atom__4283.cljs$lang$applyTo;
  return atom
}();
cljs.core.reset_BANG_ = function reset_BANG_(a, new_value) {
  var temp__3698__auto____4287 = a.validator;
  if(cljs.core.truth_(temp__3698__auto____4287)) {
    var validate__4288 = temp__3698__auto____4287;
    if(cljs.core.truth_(validate__4288.call(null, new_value))) {
    }else {
      throw new Error(cljs.core.str.call(null, "Assert failed: ", "Validator rejected reference state", "\n", cljs.core.pr_str.call(null, cljs.core.with_meta(cljs.core.list("\ufdd1'validate", "\ufdd1'new-value"), cljs.core.hash_map("\ufdd0'line", 3073)))));
    }
  }else {
  }
  var old_value__4289 = a.state;
  a.state = new_value;
  cljs.core._notify_watches.call(null, a, old_value__4289, new_value);
  return new_value
};
cljs.core.swap_BANG_ = function() {
  var swap_BANG_ = null;
  var swap_BANG___4290 = function(a, f) {
    return cljs.core.reset_BANG_.call(null, a, f.call(null, a.state))
  };
  var swap_BANG___4291 = function(a, f, x) {
    return cljs.core.reset_BANG_.call(null, a, f.call(null, a.state, x))
  };
  var swap_BANG___4292 = function(a, f, x, y) {
    return cljs.core.reset_BANG_.call(null, a, f.call(null, a.state, x, y))
  };
  var swap_BANG___4293 = function(a, f, x, y, z) {
    return cljs.core.reset_BANG_.call(null, a, f.call(null, a.state, x, y, z))
  };
  var swap_BANG___4294 = function() {
    var G__4296__delegate = function(a, f, x, y, z, more) {
      return cljs.core.reset_BANG_.call(null, a, cljs.core.apply.call(null, f, a.state, x, y, z, more))
    };
    var G__4296 = function(a, f, x, y, z, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 5), 0)
      }
      return G__4296__delegate.call(this, a, f, x, y, z, more)
    };
    G__4296.cljs$lang$maxFixedArity = 5;
    G__4296.cljs$lang$applyTo = function(arglist__4297) {
      var a = cljs.core.first(arglist__4297);
      var f = cljs.core.first(cljs.core.next(arglist__4297));
      var x = cljs.core.first(cljs.core.next(cljs.core.next(arglist__4297)));
      var y = cljs.core.first(cljs.core.next(cljs.core.next(cljs.core.next(arglist__4297))));
      var z = cljs.core.first(cljs.core.next(cljs.core.next(cljs.core.next(cljs.core.next(arglist__4297)))));
      var more = cljs.core.rest(cljs.core.next(cljs.core.next(cljs.core.next(cljs.core.next(arglist__4297)))));
      return G__4296__delegate.call(this, a, f, x, y, z, more)
    };
    return G__4296
  }();
  swap_BANG_ = function(a, f, x, y, z, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 2:
        return swap_BANG___4290.call(this, a, f);
      case 3:
        return swap_BANG___4291.call(this, a, f, x);
      case 4:
        return swap_BANG___4292.call(this, a, f, x, y);
      case 5:
        return swap_BANG___4293.call(this, a, f, x, y, z);
      default:
        return swap_BANG___4294.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  swap_BANG_.cljs$lang$maxFixedArity = 5;
  swap_BANG_.cljs$lang$applyTo = swap_BANG___4294.cljs$lang$applyTo;
  return swap_BANG_
}();
cljs.core.compare_and_set_BANG_ = function compare_and_set_BANG_(a, oldval, newval) {
  if(cljs.core.truth_(cljs.core._EQ_.call(null, a.state, oldval))) {
    cljs.core.reset_BANG_.call(null, a, newval);
    return true
  }else {
    return false
  }
};
cljs.core.deref = function deref(o) {
  return cljs.core._deref.call(null, o)
};
cljs.core.set_validator_BANG_ = function set_validator_BANG_(iref, val) {
  return iref.validator = val
};
cljs.core.get_validator = function get_validator(iref) {
  return iref.validator
};
cljs.core.alter_meta_BANG_ = function() {
  var alter_meta_BANG___delegate = function(iref, f, args) {
    return iref.meta = cljs.core.apply.call(null, f, iref.meta, args)
  };
  var alter_meta_BANG_ = function(iref, f, var_args) {
    var args = null;
    if(goog.isDef(var_args)) {
      args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
    }
    return alter_meta_BANG___delegate.call(this, iref, f, args)
  };
  alter_meta_BANG_.cljs$lang$maxFixedArity = 2;
  alter_meta_BANG_.cljs$lang$applyTo = function(arglist__4298) {
    var iref = cljs.core.first(arglist__4298);
    var f = cljs.core.first(cljs.core.next(arglist__4298));
    var args = cljs.core.rest(cljs.core.next(arglist__4298));
    return alter_meta_BANG___delegate.call(this, iref, f, args)
  };
  return alter_meta_BANG_
}();
cljs.core.reset_meta_BANG_ = function reset_meta_BANG_(iref, m) {
  return iref.meta = m
};
cljs.core.add_watch = function add_watch(iref, key, f) {
  return cljs.core._add_watch.call(null, iref, key, f)
};
cljs.core.remove_watch = function remove_watch(iref, key) {
  return cljs.core._remove_watch.call(null, iref, key)
};
cljs.core.gensym_counter = null;
cljs.core.gensym = function() {
  var gensym = null;
  var gensym__4299 = function() {
    return gensym.call(null, "G__")
  };
  var gensym__4300 = function(prefix_string) {
    if(cljs.core.truth_(cljs.core.gensym_counter === null)) {
      cljs.core.gensym_counter = cljs.core.atom.call(null, 0)
    }else {
    }
    return cljs.core.symbol.call(null, cljs.core.str.call(null, prefix_string, cljs.core.swap_BANG_.call(null, cljs.core.gensym_counter, cljs.core.inc)))
  };
  gensym = function(prefix_string) {
    switch(arguments.length) {
      case 0:
        return gensym__4299.call(this);
      case 1:
        return gensym__4300.call(this, prefix_string)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return gensym
}();
cljs.core.fixture1 = 1;
cljs.core.fixture2 = 2;
cljs.core.Delay = function(f, state) {
  this.f = f;
  this.state = state
};
cljs.core.Delay.cljs$core$IPrintable$_pr_seq = function(this__267__auto__) {
  return cljs.core.list.call(null, "cljs.core.Delay")
};
cljs.core.Delay.prototype.cljs$core$IPending$ = true;
cljs.core.Delay.prototype.cljs$core$IPending$_realized_QMARK_ = function(d) {
  var this__4302 = this;
  return cljs.core.not.call(null, cljs.core.deref.call(null, this__4302.state) === null)
};
cljs.core.Delay.prototype.cljs$core$IDeref$ = true;
cljs.core.Delay.prototype.cljs$core$IDeref$_deref = function(_) {
  var this__4303 = this;
  if(cljs.core.truth_(cljs.core.deref.call(null, this__4303.state))) {
  }else {
    cljs.core.swap_BANG_.call(null, this__4303.state, this__4303.f)
  }
  return cljs.core.deref.call(null, this__4303.state)
};
cljs.core.Delay;
cljs.core.delay = function() {
  var delay__delegate = function(body) {
    return new cljs.core.Delay(function() {
      return cljs.core.apply.call(null, cljs.core.identity, body)
    }, cljs.core.atom.call(null, null))
  };
  var delay = function(var_args) {
    var body = null;
    if(goog.isDef(var_args)) {
      body = cljs.core.array_seq(Array.prototype.slice.call(arguments, 0), 0)
    }
    return delay__delegate.call(this, body)
  };
  delay.cljs$lang$maxFixedArity = 0;
  delay.cljs$lang$applyTo = function(arglist__4304) {
    var body = cljs.core.seq(arglist__4304);
    return delay__delegate.call(this, body)
  };
  return delay
}();
cljs.core.delay_QMARK_ = function delay_QMARK_(x) {
  return cljs.core.instance_QMARK_.call(null, cljs.core.Delay, x)
};
cljs.core.force = function force(x) {
  if(cljs.core.truth_(cljs.core.delay_QMARK_.call(null, x))) {
    return cljs.core.deref.call(null, x)
  }else {
    return x
  }
};
cljs.core.realized_QMARK_ = function realized_QMARK_(d) {
  return cljs.core._realized_QMARK_.call(null, d)
};
cljs.core.js__GT_clj = function() {
  var js__GT_clj__delegate = function(x, options) {
    var map__4305__4306 = options;
    var map__4305__4307 = cljs.core.truth_(cljs.core.seq_QMARK_.call(null, map__4305__4306)) ? cljs.core.apply.call(null, cljs.core.hash_map, map__4305__4306) : map__4305__4306;
    var keywordize_keys__4308 = cljs.core.get.call(null, map__4305__4307, "\ufdd0'keywordize-keys");
    var keyfn__4309 = cljs.core.truth_(keywordize_keys__4308) ? cljs.core.keyword : cljs.core.str;
    var f__4315 = function thisfn(x) {
      if(cljs.core.truth_(cljs.core.seq_QMARK_.call(null, x))) {
        return cljs.core.doall.call(null, cljs.core.map.call(null, thisfn, x))
      }else {
        if(cljs.core.truth_(cljs.core.coll_QMARK_.call(null, x))) {
          return cljs.core.into.call(null, cljs.core.empty.call(null, x), cljs.core.map.call(null, thisfn, x))
        }else {
          if(cljs.core.truth_(goog.isArray.call(null, x))) {
            return cljs.core.vec.call(null, cljs.core.map.call(null, thisfn, x))
          }else {
            if(cljs.core.truth_(goog.isObject.call(null, x))) {
              return cljs.core.into.call(null, cljs.core.ObjMap.fromObject([], {}), function() {
                var iter__416__auto____4314 = function iter__4310(s__4311) {
                  return new cljs.core.LazySeq(null, false, function() {
                    var s__4311__4312 = s__4311;
                    while(true) {
                      if(cljs.core.truth_(cljs.core.seq.call(null, s__4311__4312))) {
                        var k__4313 = cljs.core.first.call(null, s__4311__4312);
                        return cljs.core.cons.call(null, cljs.core.Vector.fromArray([keyfn__4309.call(null, k__4313), thisfn.call(null, x[k__4313])]), iter__4310.call(null, cljs.core.rest.call(null, s__4311__4312)))
                      }else {
                        return null
                      }
                      break
                    }
                  })
                };
                return iter__416__auto____4314.call(null, cljs.core.js_keys.call(null, x))
              }())
            }else {
              if(cljs.core.truth_("\ufdd0'else")) {
                return x
              }else {
                return null
              }
            }
          }
        }
      }
    };
    return f__4315.call(null, x)
  };
  var js__GT_clj = function(x, var_args) {
    var options = null;
    if(goog.isDef(var_args)) {
      options = cljs.core.array_seq(Array.prototype.slice.call(arguments, 1), 0)
    }
    return js__GT_clj__delegate.call(this, x, options)
  };
  js__GT_clj.cljs$lang$maxFixedArity = 1;
  js__GT_clj.cljs$lang$applyTo = function(arglist__4316) {
    var x = cljs.core.first(arglist__4316);
    var options = cljs.core.rest(arglist__4316);
    return js__GT_clj__delegate.call(this, x, options)
  };
  return js__GT_clj
}();
cljs.core.memoize = function memoize(f) {
  var mem__4317 = cljs.core.atom.call(null, cljs.core.ObjMap.fromObject([], {}));
  return function() {
    var G__4321__delegate = function(args) {
      var temp__3695__auto____4318 = cljs.core.get.call(null, cljs.core.deref.call(null, mem__4317), args);
      if(cljs.core.truth_(temp__3695__auto____4318)) {
        var v__4319 = temp__3695__auto____4318;
        return v__4319
      }else {
        var ret__4320 = cljs.core.apply.call(null, f, args);
        cljs.core.swap_BANG_.call(null, mem__4317, cljs.core.assoc, args, ret__4320);
        return ret__4320
      }
    };
    var G__4321 = function(var_args) {
      var args = null;
      if(goog.isDef(var_args)) {
        args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 0), 0)
      }
      return G__4321__delegate.call(this, args)
    };
    G__4321.cljs$lang$maxFixedArity = 0;
    G__4321.cljs$lang$applyTo = function(arglist__4322) {
      var args = cljs.core.seq(arglist__4322);
      return G__4321__delegate.call(this, args)
    };
    return G__4321
  }()
};
cljs.core.trampoline = function() {
  var trampoline = null;
  var trampoline__4324 = function(f) {
    while(true) {
      var ret__4323 = f.call(null);
      if(cljs.core.truth_(cljs.core.fn_QMARK_.call(null, ret__4323))) {
        var G__4327 = ret__4323;
        f = G__4327;
        continue
      }else {
        return ret__4323
      }
      break
    }
  };
  var trampoline__4325 = function() {
    var G__4328__delegate = function(f, args) {
      return trampoline.call(null, function() {
        return cljs.core.apply.call(null, f, args)
      })
    };
    var G__4328 = function(f, var_args) {
      var args = null;
      if(goog.isDef(var_args)) {
        args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 1), 0)
      }
      return G__4328__delegate.call(this, f, args)
    };
    G__4328.cljs$lang$maxFixedArity = 1;
    G__4328.cljs$lang$applyTo = function(arglist__4329) {
      var f = cljs.core.first(arglist__4329);
      var args = cljs.core.rest(arglist__4329);
      return G__4328__delegate.call(this, f, args)
    };
    return G__4328
  }();
  trampoline = function(f, var_args) {
    var args = var_args;
    switch(arguments.length) {
      case 1:
        return trampoline__4324.call(this, f);
      default:
        return trampoline__4325.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  trampoline.cljs$lang$maxFixedArity = 1;
  trampoline.cljs$lang$applyTo = trampoline__4325.cljs$lang$applyTo;
  return trampoline
}();
cljs.core.rand = function() {
  var rand = null;
  var rand__4330 = function() {
    return rand.call(null, 1)
  };
  var rand__4331 = function(n) {
    return Math.random() * n
  };
  rand = function(n) {
    switch(arguments.length) {
      case 0:
        return rand__4330.call(this);
      case 1:
        return rand__4331.call(this, n)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return rand
}();
cljs.core.rand_int = function rand_int(n) {
  return Math.floor(Math.random() * n)
};
cljs.core.rand_nth = function rand_nth(coll) {
  return cljs.core.nth.call(null, coll, cljs.core.rand_int.call(null, cljs.core.count.call(null, coll)))
};
cljs.core.group_by = function group_by(f, coll) {
  return cljs.core.reduce.call(null, function(ret, x) {
    var k__4333 = f.call(null, x);
    return cljs.core.assoc.call(null, ret, k__4333, cljs.core.conj.call(null, cljs.core.get.call(null, ret, k__4333, cljs.core.Vector.fromArray([])), x))
  }, cljs.core.ObjMap.fromObject([], {}), coll)
};
cljs.core.make_hierarchy = function make_hierarchy() {
  return cljs.core.ObjMap.fromObject(["\ufdd0'parents", "\ufdd0'descendants", "\ufdd0'ancestors"], {"\ufdd0'parents":cljs.core.ObjMap.fromObject([], {}), "\ufdd0'descendants":cljs.core.ObjMap.fromObject([], {}), "\ufdd0'ancestors":cljs.core.ObjMap.fromObject([], {})})
};
cljs.core.global_hierarchy = cljs.core.atom.call(null, cljs.core.make_hierarchy.call(null));
cljs.core.isa_QMARK_ = function() {
  var isa_QMARK_ = null;
  var isa_QMARK___4342 = function(child, parent) {
    return isa_QMARK_.call(null, cljs.core.deref.call(null, cljs.core.global_hierarchy), child, parent)
  };
  var isa_QMARK___4343 = function(h, child, parent) {
    var or__3548__auto____4334 = cljs.core._EQ_.call(null, child, parent);
    if(cljs.core.truth_(or__3548__auto____4334)) {
      return or__3548__auto____4334
    }else {
      var or__3548__auto____4335 = cljs.core.contains_QMARK_.call(null, "\ufdd0'ancestors".call(null, h).call(null, child), parent);
      if(cljs.core.truth_(or__3548__auto____4335)) {
        return or__3548__auto____4335
      }else {
        var and__3546__auto____4336 = cljs.core.vector_QMARK_.call(null, parent);
        if(cljs.core.truth_(and__3546__auto____4336)) {
          var and__3546__auto____4337 = cljs.core.vector_QMARK_.call(null, child);
          if(cljs.core.truth_(and__3546__auto____4337)) {
            var and__3546__auto____4338 = cljs.core._EQ_.call(null, cljs.core.count.call(null, parent), cljs.core.count.call(null, child));
            if(cljs.core.truth_(and__3546__auto____4338)) {
              var ret__4339 = true;
              var i__4340 = 0;
              while(true) {
                if(cljs.core.truth_(function() {
                  var or__3548__auto____4341 = cljs.core.not.call(null, ret__4339);
                  if(cljs.core.truth_(or__3548__auto____4341)) {
                    return or__3548__auto____4341
                  }else {
                    return cljs.core._EQ_.call(null, i__4340, cljs.core.count.call(null, parent))
                  }
                }())) {
                  return ret__4339
                }else {
                  var G__4345 = isa_QMARK_.call(null, h, child.call(null, i__4340), parent.call(null, i__4340));
                  var G__4346 = i__4340 + 1;
                  ret__4339 = G__4345;
                  i__4340 = G__4346;
                  continue
                }
                break
              }
            }else {
              return and__3546__auto____4338
            }
          }else {
            return and__3546__auto____4337
          }
        }else {
          return and__3546__auto____4336
        }
      }
    }
  };
  isa_QMARK_ = function(h, child, parent) {
    switch(arguments.length) {
      case 2:
        return isa_QMARK___4342.call(this, h, child);
      case 3:
        return isa_QMARK___4343.call(this, h, child, parent)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return isa_QMARK_
}();
cljs.core.parents = function() {
  var parents = null;
  var parents__4347 = function(tag) {
    return parents.call(null, cljs.core.deref.call(null, cljs.core.global_hierarchy), tag)
  };
  var parents__4348 = function(h, tag) {
    return cljs.core.not_empty.call(null, cljs.core.get.call(null, "\ufdd0'parents".call(null, h), tag))
  };
  parents = function(h, tag) {
    switch(arguments.length) {
      case 1:
        return parents__4347.call(this, h);
      case 2:
        return parents__4348.call(this, h, tag)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return parents
}();
cljs.core.ancestors = function() {
  var ancestors = null;
  var ancestors__4350 = function(tag) {
    return ancestors.call(null, cljs.core.deref.call(null, cljs.core.global_hierarchy), tag)
  };
  var ancestors__4351 = function(h, tag) {
    return cljs.core.not_empty.call(null, cljs.core.get.call(null, "\ufdd0'ancestors".call(null, h), tag))
  };
  ancestors = function(h, tag) {
    switch(arguments.length) {
      case 1:
        return ancestors__4350.call(this, h);
      case 2:
        return ancestors__4351.call(this, h, tag)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return ancestors
}();
cljs.core.descendants = function() {
  var descendants = null;
  var descendants__4353 = function(tag) {
    return descendants.call(null, cljs.core.deref.call(null, cljs.core.global_hierarchy), tag)
  };
  var descendants__4354 = function(h, tag) {
    return cljs.core.not_empty.call(null, cljs.core.get.call(null, "\ufdd0'descendants".call(null, h), tag))
  };
  descendants = function(h, tag) {
    switch(arguments.length) {
      case 1:
        return descendants__4353.call(this, h);
      case 2:
        return descendants__4354.call(this, h, tag)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return descendants
}();
cljs.core.derive = function() {
  var derive = null;
  var derive__4364 = function(tag, parent) {
    if(cljs.core.truth_(cljs.core.namespace.call(null, parent))) {
    }else {
      throw new Error(cljs.core.str.call(null, "Assert failed: ", cljs.core.pr_str.call(null, cljs.core.with_meta(cljs.core.list("\ufdd1'namespace", "\ufdd1'parent"), cljs.core.hash_map("\ufdd0'line", 3365)))));
    }
    cljs.core.swap_BANG_.call(null, cljs.core.global_hierarchy, derive, tag, parent);
    return null
  };
  var derive__4365 = function(h, tag, parent) {
    if(cljs.core.truth_(cljs.core.not_EQ_.call(null, tag, parent))) {
    }else {
      throw new Error(cljs.core.str.call(null, "Assert failed: ", cljs.core.pr_str.call(null, cljs.core.with_meta(cljs.core.list("\ufdd1'not=", "\ufdd1'tag", "\ufdd1'parent"), cljs.core.hash_map("\ufdd0'line", 3369)))));
    }
    var tp__4359 = "\ufdd0'parents".call(null, h);
    var td__4360 = "\ufdd0'descendants".call(null, h);
    var ta__4361 = "\ufdd0'ancestors".call(null, h);
    var tf__4362 = function(m, source, sources, target, targets) {
      return cljs.core.reduce.call(null, function(ret, k) {
        return cljs.core.assoc.call(null, ret, k, cljs.core.reduce.call(null, cljs.core.conj, cljs.core.get.call(null, targets, k, cljs.core.set([])), cljs.core.cons.call(null, target, targets.call(null, target))))
      }, m, cljs.core.cons.call(null, source, sources.call(null, source)))
    };
    var or__3548__auto____4363 = cljs.core.truth_(cljs.core.contains_QMARK_.call(null, tp__4359.call(null, tag), parent)) ? null : function() {
      if(cljs.core.truth_(cljs.core.contains_QMARK_.call(null, ta__4361.call(null, tag), parent))) {
        throw new Error(cljs.core.str.call(null, tag, "already has", parent, "as ancestor"));
      }else {
      }
      if(cljs.core.truth_(cljs.core.contains_QMARK_.call(null, ta__4361.call(null, parent), tag))) {
        throw new Error(cljs.core.str.call(null, "Cyclic derivation:", parent, "has", tag, "as ancestor"));
      }else {
      }
      return cljs.core.ObjMap.fromObject(["\ufdd0'parents", "\ufdd0'ancestors", "\ufdd0'descendants"], {"\ufdd0'parents":cljs.core.assoc.call(null, "\ufdd0'parents".call(null, h), tag, cljs.core.conj.call(null, cljs.core.get.call(null, tp__4359, tag, cljs.core.set([])), parent)), "\ufdd0'ancestors":tf__4362.call(null, "\ufdd0'ancestors".call(null, h), tag, td__4360, parent, ta__4361), "\ufdd0'descendants":tf__4362.call(null, "\ufdd0'descendants".call(null, h), parent, ta__4361, tag, td__4360)})
    }();
    if(cljs.core.truth_(or__3548__auto____4363)) {
      return or__3548__auto____4363
    }else {
      return h
    }
  };
  derive = function(h, tag, parent) {
    switch(arguments.length) {
      case 2:
        return derive__4364.call(this, h, tag);
      case 3:
        return derive__4365.call(this, h, tag, parent)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return derive
}();
cljs.core.underive = function() {
  var underive = null;
  var underive__4371 = function(tag, parent) {
    cljs.core.swap_BANG_.call(null, cljs.core.global_hierarchy, underive, tag, parent);
    return null
  };
  var underive__4372 = function(h, tag, parent) {
    var parentMap__4367 = "\ufdd0'parents".call(null, h);
    var childsParents__4368 = cljs.core.truth_(parentMap__4367.call(null, tag)) ? cljs.core.disj.call(null, parentMap__4367.call(null, tag), parent) : cljs.core.set([]);
    var newParents__4369 = cljs.core.truth_(cljs.core.not_empty.call(null, childsParents__4368)) ? cljs.core.assoc.call(null, parentMap__4367, tag, childsParents__4368) : cljs.core.dissoc.call(null, parentMap__4367, tag);
    var deriv_seq__4370 = cljs.core.flatten.call(null, cljs.core.map.call(null, function(p1__4356_SHARP_) {
      return cljs.core.cons.call(null, cljs.core.first.call(null, p1__4356_SHARP_), cljs.core.interpose.call(null, cljs.core.first.call(null, p1__4356_SHARP_), cljs.core.second.call(null, p1__4356_SHARP_)))
    }, cljs.core.seq.call(null, newParents__4369)));
    if(cljs.core.truth_(cljs.core.contains_QMARK_.call(null, parentMap__4367.call(null, tag), parent))) {
      return cljs.core.reduce.call(null, function(p1__4357_SHARP_, p2__4358_SHARP_) {
        return cljs.core.apply.call(null, cljs.core.derive, p1__4357_SHARP_, p2__4358_SHARP_)
      }, cljs.core.make_hierarchy.call(null), cljs.core.partition.call(null, 2, deriv_seq__4370))
    }else {
      return h
    }
  };
  underive = function(h, tag, parent) {
    switch(arguments.length) {
      case 2:
        return underive__4371.call(this, h, tag);
      case 3:
        return underive__4372.call(this, h, tag, parent)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return underive
}();
cljs.core.reset_cache = function reset_cache(method_cache, method_table, cached_hierarchy, hierarchy) {
  cljs.core.swap_BANG_.call(null, method_cache, function(_) {
    return cljs.core.deref.call(null, method_table)
  });
  return cljs.core.swap_BANG_.call(null, cached_hierarchy, function(_) {
    return cljs.core.deref.call(null, hierarchy)
  })
};
cljs.core.prefers_STAR_ = function prefers_STAR_(x, y, prefer_table) {
  var xprefs__4374 = cljs.core.deref.call(null, prefer_table).call(null, x);
  var or__3548__auto____4376 = cljs.core.truth_(function() {
    var and__3546__auto____4375 = xprefs__4374;
    if(cljs.core.truth_(and__3546__auto____4375)) {
      return xprefs__4374.call(null, y)
    }else {
      return and__3546__auto____4375
    }
  }()) ? true : null;
  if(cljs.core.truth_(or__3548__auto____4376)) {
    return or__3548__auto____4376
  }else {
    var or__3548__auto____4378 = function() {
      var ps__4377 = cljs.core.parents.call(null, y);
      while(true) {
        if(cljs.core.truth_(cljs.core.count.call(null, ps__4377) > 0)) {
          if(cljs.core.truth_(prefers_STAR_.call(null, x, cljs.core.first.call(null, ps__4377), prefer_table))) {
          }else {
          }
          var G__4381 = cljs.core.rest.call(null, ps__4377);
          ps__4377 = G__4381;
          continue
        }else {
          return null
        }
        break
      }
    }();
    if(cljs.core.truth_(or__3548__auto____4378)) {
      return or__3548__auto____4378
    }else {
      var or__3548__auto____4380 = function() {
        var ps__4379 = cljs.core.parents.call(null, x);
        while(true) {
          if(cljs.core.truth_(cljs.core.count.call(null, ps__4379) > 0)) {
            if(cljs.core.truth_(prefers_STAR_.call(null, cljs.core.first.call(null, ps__4379), y, prefer_table))) {
            }else {
            }
            var G__4382 = cljs.core.rest.call(null, ps__4379);
            ps__4379 = G__4382;
            continue
          }else {
            return null
          }
          break
        }
      }();
      if(cljs.core.truth_(or__3548__auto____4380)) {
        return or__3548__auto____4380
      }else {
        return false
      }
    }
  }
};
cljs.core.dominates = function dominates(x, y, prefer_table) {
  var or__3548__auto____4383 = cljs.core.prefers_STAR_.call(null, x, y, prefer_table);
  if(cljs.core.truth_(or__3548__auto____4383)) {
    return or__3548__auto____4383
  }else {
    return cljs.core.isa_QMARK_.call(null, x, y)
  }
};
cljs.core.find_and_cache_best_method = function find_and_cache_best_method(name, dispatch_val, hierarchy, method_table, prefer_table, method_cache, cached_hierarchy) {
  var best_entry__4392 = cljs.core.reduce.call(null, function(be, p__4384) {
    var vec__4385__4386 = p__4384;
    var k__4387 = cljs.core.nth.call(null, vec__4385__4386, 0, null);
    var ___4388 = cljs.core.nth.call(null, vec__4385__4386, 1, null);
    var e__4389 = vec__4385__4386;
    if(cljs.core.truth_(cljs.core.isa_QMARK_.call(null, dispatch_val, k__4387))) {
      var be2__4391 = cljs.core.truth_(function() {
        var or__3548__auto____4390 = be === null;
        if(cljs.core.truth_(or__3548__auto____4390)) {
          return or__3548__auto____4390
        }else {
          return cljs.core.dominates.call(null, k__4387, cljs.core.first.call(null, be), prefer_table)
        }
      }()) ? e__4389 : be;
      if(cljs.core.truth_(cljs.core.dominates.call(null, cljs.core.first.call(null, be2__4391), k__4387, prefer_table))) {
      }else {
        throw new Error(cljs.core.str.call(null, "Multiple methods in multimethod '", name, "' match dispatch value: ", dispatch_val, " -> ", k__4387, " and ", cljs.core.first.call(null, be2__4391), ", and neither is preferred"));
      }
      return be2__4391
    }else {
      return be
    }
  }, null, cljs.core.deref.call(null, method_table));
  if(cljs.core.truth_(best_entry__4392)) {
    if(cljs.core.truth_(cljs.core._EQ_.call(null, cljs.core.deref.call(null, cached_hierarchy), cljs.core.deref.call(null, hierarchy)))) {
      cljs.core.swap_BANG_.call(null, method_cache, cljs.core.assoc, dispatch_val, cljs.core.second.call(null, best_entry__4392));
      return cljs.core.second.call(null, best_entry__4392)
    }else {
      cljs.core.reset_cache.call(null, method_cache, method_table, cached_hierarchy, hierarchy);
      return find_and_cache_best_method.call(null, name, dispatch_val, hierarchy, method_table, prefer_table, method_cache, cached_hierarchy)
    }
  }else {
    return null
  }
};
cljs.core.IMultiFn = {};
cljs.core._reset = function _reset(mf) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____4393 = mf;
    if(cljs.core.truth_(and__3546__auto____4393)) {
      return mf.cljs$core$IMultiFn$_reset
    }else {
      return and__3546__auto____4393
    }
  }())) {
    return mf.cljs$core$IMultiFn$_reset(mf)
  }else {
    return function() {
      var or__3548__auto____4394 = cljs.core._reset[goog.typeOf.call(null, mf)];
      if(cljs.core.truth_(or__3548__auto____4394)) {
        return or__3548__auto____4394
      }else {
        var or__3548__auto____4395 = cljs.core._reset["_"];
        if(cljs.core.truth_(or__3548__auto____4395)) {
          return or__3548__auto____4395
        }else {
          throw cljs.core.missing_protocol.call(null, "IMultiFn.-reset", mf);
        }
      }
    }().call(null, mf)
  }
};
cljs.core._add_method = function _add_method(mf, dispatch_val, method) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____4396 = mf;
    if(cljs.core.truth_(and__3546__auto____4396)) {
      return mf.cljs$core$IMultiFn$_add_method
    }else {
      return and__3546__auto____4396
    }
  }())) {
    return mf.cljs$core$IMultiFn$_add_method(mf, dispatch_val, method)
  }else {
    return function() {
      var or__3548__auto____4397 = cljs.core._add_method[goog.typeOf.call(null, mf)];
      if(cljs.core.truth_(or__3548__auto____4397)) {
        return or__3548__auto____4397
      }else {
        var or__3548__auto____4398 = cljs.core._add_method["_"];
        if(cljs.core.truth_(or__3548__auto____4398)) {
          return or__3548__auto____4398
        }else {
          throw cljs.core.missing_protocol.call(null, "IMultiFn.-add-method", mf);
        }
      }
    }().call(null, mf, dispatch_val, method)
  }
};
cljs.core._remove_method = function _remove_method(mf, dispatch_val) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____4399 = mf;
    if(cljs.core.truth_(and__3546__auto____4399)) {
      return mf.cljs$core$IMultiFn$_remove_method
    }else {
      return and__3546__auto____4399
    }
  }())) {
    return mf.cljs$core$IMultiFn$_remove_method(mf, dispatch_val)
  }else {
    return function() {
      var or__3548__auto____4400 = cljs.core._remove_method[goog.typeOf.call(null, mf)];
      if(cljs.core.truth_(or__3548__auto____4400)) {
        return or__3548__auto____4400
      }else {
        var or__3548__auto____4401 = cljs.core._remove_method["_"];
        if(cljs.core.truth_(or__3548__auto____4401)) {
          return or__3548__auto____4401
        }else {
          throw cljs.core.missing_protocol.call(null, "IMultiFn.-remove-method", mf);
        }
      }
    }().call(null, mf, dispatch_val)
  }
};
cljs.core._prefer_method = function _prefer_method(mf, dispatch_val, dispatch_val_y) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____4402 = mf;
    if(cljs.core.truth_(and__3546__auto____4402)) {
      return mf.cljs$core$IMultiFn$_prefer_method
    }else {
      return and__3546__auto____4402
    }
  }())) {
    return mf.cljs$core$IMultiFn$_prefer_method(mf, dispatch_val, dispatch_val_y)
  }else {
    return function() {
      var or__3548__auto____4403 = cljs.core._prefer_method[goog.typeOf.call(null, mf)];
      if(cljs.core.truth_(or__3548__auto____4403)) {
        return or__3548__auto____4403
      }else {
        var or__3548__auto____4404 = cljs.core._prefer_method["_"];
        if(cljs.core.truth_(or__3548__auto____4404)) {
          return or__3548__auto____4404
        }else {
          throw cljs.core.missing_protocol.call(null, "IMultiFn.-prefer-method", mf);
        }
      }
    }().call(null, mf, dispatch_val, dispatch_val_y)
  }
};
cljs.core._get_method = function _get_method(mf, dispatch_val) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____4405 = mf;
    if(cljs.core.truth_(and__3546__auto____4405)) {
      return mf.cljs$core$IMultiFn$_get_method
    }else {
      return and__3546__auto____4405
    }
  }())) {
    return mf.cljs$core$IMultiFn$_get_method(mf, dispatch_val)
  }else {
    return function() {
      var or__3548__auto____4406 = cljs.core._get_method[goog.typeOf.call(null, mf)];
      if(cljs.core.truth_(or__3548__auto____4406)) {
        return or__3548__auto____4406
      }else {
        var or__3548__auto____4407 = cljs.core._get_method["_"];
        if(cljs.core.truth_(or__3548__auto____4407)) {
          return or__3548__auto____4407
        }else {
          throw cljs.core.missing_protocol.call(null, "IMultiFn.-get-method", mf);
        }
      }
    }().call(null, mf, dispatch_val)
  }
};
cljs.core._methods = function _methods(mf) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____4408 = mf;
    if(cljs.core.truth_(and__3546__auto____4408)) {
      return mf.cljs$core$IMultiFn$_methods
    }else {
      return and__3546__auto____4408
    }
  }())) {
    return mf.cljs$core$IMultiFn$_methods(mf)
  }else {
    return function() {
      var or__3548__auto____4409 = cljs.core._methods[goog.typeOf.call(null, mf)];
      if(cljs.core.truth_(or__3548__auto____4409)) {
        return or__3548__auto____4409
      }else {
        var or__3548__auto____4410 = cljs.core._methods["_"];
        if(cljs.core.truth_(or__3548__auto____4410)) {
          return or__3548__auto____4410
        }else {
          throw cljs.core.missing_protocol.call(null, "IMultiFn.-methods", mf);
        }
      }
    }().call(null, mf)
  }
};
cljs.core._prefers = function _prefers(mf) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____4411 = mf;
    if(cljs.core.truth_(and__3546__auto____4411)) {
      return mf.cljs$core$IMultiFn$_prefers
    }else {
      return and__3546__auto____4411
    }
  }())) {
    return mf.cljs$core$IMultiFn$_prefers(mf)
  }else {
    return function() {
      var or__3548__auto____4412 = cljs.core._prefers[goog.typeOf.call(null, mf)];
      if(cljs.core.truth_(or__3548__auto____4412)) {
        return or__3548__auto____4412
      }else {
        var or__3548__auto____4413 = cljs.core._prefers["_"];
        if(cljs.core.truth_(or__3548__auto____4413)) {
          return or__3548__auto____4413
        }else {
          throw cljs.core.missing_protocol.call(null, "IMultiFn.-prefers", mf);
        }
      }
    }().call(null, mf)
  }
};
cljs.core._dispatch = function _dispatch(mf, args) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____4414 = mf;
    if(cljs.core.truth_(and__3546__auto____4414)) {
      return mf.cljs$core$IMultiFn$_dispatch
    }else {
      return and__3546__auto____4414
    }
  }())) {
    return mf.cljs$core$IMultiFn$_dispatch(mf, args)
  }else {
    return function() {
      var or__3548__auto____4415 = cljs.core._dispatch[goog.typeOf.call(null, mf)];
      if(cljs.core.truth_(or__3548__auto____4415)) {
        return or__3548__auto____4415
      }else {
        var or__3548__auto____4416 = cljs.core._dispatch["_"];
        if(cljs.core.truth_(or__3548__auto____4416)) {
          return or__3548__auto____4416
        }else {
          throw cljs.core.missing_protocol.call(null, "IMultiFn.-dispatch", mf);
        }
      }
    }().call(null, mf, args)
  }
};
cljs.core.do_dispatch = function do_dispatch(mf, dispatch_fn, args) {
  var dispatch_val__4417 = cljs.core.apply.call(null, dispatch_fn, args);
  var target_fn__4418 = cljs.core._get_method.call(null, mf, dispatch_val__4417);
  if(cljs.core.truth_(target_fn__4418)) {
  }else {
    throw new Error(cljs.core.str.call(null, "No method in multimethod '", cljs.core.name, "' for dispatch value: ", dispatch_val__4417));
  }
  return cljs.core.apply.call(null, target_fn__4418, args)
};
cljs.core.MultiFn = function(name, dispatch_fn, default_dispatch_val, hierarchy, method_table, prefer_table, method_cache, cached_hierarchy) {
  this.name = name;
  this.dispatch_fn = dispatch_fn;
  this.default_dispatch_val = default_dispatch_val;
  this.hierarchy = hierarchy;
  this.method_table = method_table;
  this.prefer_table = prefer_table;
  this.method_cache = method_cache;
  this.cached_hierarchy = cached_hierarchy
};
cljs.core.MultiFn.cljs$core$IPrintable$_pr_seq = function(this__267__auto__) {
  return cljs.core.list.call(null, "cljs.core.MultiFn")
};
cljs.core.MultiFn.prototype.cljs$core$IHash$ = true;
cljs.core.MultiFn.prototype.cljs$core$IHash$_hash = function(this$) {
  var this__4419 = this;
  return goog.getUid.call(null, this$)
};
cljs.core.MultiFn.prototype.cljs$core$IMultiFn$ = true;
cljs.core.MultiFn.prototype.cljs$core$IMultiFn$_reset = function(mf) {
  var this__4420 = this;
  cljs.core.swap_BANG_.call(null, this__4420.method_table, function(mf) {
    return cljs.core.ObjMap.fromObject([], {})
  });
  cljs.core.swap_BANG_.call(null, this__4420.method_cache, function(mf) {
    return cljs.core.ObjMap.fromObject([], {})
  });
  cljs.core.swap_BANG_.call(null, this__4420.prefer_table, function(mf) {
    return cljs.core.ObjMap.fromObject([], {})
  });
  cljs.core.swap_BANG_.call(null, this__4420.cached_hierarchy, function(mf) {
    return null
  });
  return mf
};
cljs.core.MultiFn.prototype.cljs$core$IMultiFn$_add_method = function(mf, dispatch_val, method) {
  var this__4421 = this;
  cljs.core.swap_BANG_.call(null, this__4421.method_table, cljs.core.assoc, dispatch_val, method);
  cljs.core.reset_cache.call(null, this__4421.method_cache, this__4421.method_table, this__4421.cached_hierarchy, this__4421.hierarchy);
  return mf
};
cljs.core.MultiFn.prototype.cljs$core$IMultiFn$_remove_method = function(mf, dispatch_val) {
  var this__4422 = this;
  cljs.core.swap_BANG_.call(null, this__4422.method_table, cljs.core.dissoc, dispatch_val);
  cljs.core.reset_cache.call(null, this__4422.method_cache, this__4422.method_table, this__4422.cached_hierarchy, this__4422.hierarchy);
  return mf
};
cljs.core.MultiFn.prototype.cljs$core$IMultiFn$_get_method = function(mf, dispatch_val) {
  var this__4423 = this;
  if(cljs.core.truth_(cljs.core._EQ_.call(null, cljs.core.deref.call(null, this__4423.cached_hierarchy), cljs.core.deref.call(null, this__4423.hierarchy)))) {
  }else {
    cljs.core.reset_cache.call(null, this__4423.method_cache, this__4423.method_table, this__4423.cached_hierarchy, this__4423.hierarchy)
  }
  var temp__3695__auto____4424 = cljs.core.deref.call(null, this__4423.method_cache).call(null, dispatch_val);
  if(cljs.core.truth_(temp__3695__auto____4424)) {
    var target_fn__4425 = temp__3695__auto____4424;
    return target_fn__4425
  }else {
    var temp__3695__auto____4426 = cljs.core.find_and_cache_best_method.call(null, this__4423.name, dispatch_val, this__4423.hierarchy, this__4423.method_table, this__4423.prefer_table, this__4423.method_cache, this__4423.cached_hierarchy);
    if(cljs.core.truth_(temp__3695__auto____4426)) {
      var target_fn__4427 = temp__3695__auto____4426;
      return target_fn__4427
    }else {
      return cljs.core.deref.call(null, this__4423.method_table).call(null, this__4423.default_dispatch_val)
    }
  }
};
cljs.core.MultiFn.prototype.cljs$core$IMultiFn$_prefer_method = function(mf, dispatch_val_x, dispatch_val_y) {
  var this__4428 = this;
  if(cljs.core.truth_(cljs.core.prefers_STAR_.call(null, dispatch_val_x, dispatch_val_y, this__4428.prefer_table))) {
    throw new Error(cljs.core.str.call(null, "Preference conflict in multimethod '", this__4428.name, "': ", dispatch_val_y, " is already preferred to ", dispatch_val_x));
  }else {
  }
  cljs.core.swap_BANG_.call(null, this__4428.prefer_table, function(old) {
    return cljs.core.assoc.call(null, old, dispatch_val_x, cljs.core.conj.call(null, cljs.core.get.call(null, old, dispatch_val_x, cljs.core.set([])), dispatch_val_y))
  });
  return cljs.core.reset_cache.call(null, this__4428.method_cache, this__4428.method_table, this__4428.cached_hierarchy, this__4428.hierarchy)
};
cljs.core.MultiFn.prototype.cljs$core$IMultiFn$_methods = function(mf) {
  var this__4429 = this;
  return cljs.core.deref.call(null, this__4429.method_table)
};
cljs.core.MultiFn.prototype.cljs$core$IMultiFn$_prefers = function(mf) {
  var this__4430 = this;
  return cljs.core.deref.call(null, this__4430.prefer_table)
};
cljs.core.MultiFn.prototype.cljs$core$IMultiFn$_dispatch = function(mf, args) {
  var this__4431 = this;
  return cljs.core.do_dispatch.call(null, mf, this__4431.dispatch_fn, args)
};
cljs.core.MultiFn;
cljs.core.MultiFn.prototype.call = function() {
  var G__4432__delegate = function(_, args) {
    return cljs.core._dispatch.call(null, this, args)
  };
  var G__4432 = function(_, var_args) {
    var args = null;
    if(goog.isDef(var_args)) {
      args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 1), 0)
    }
    return G__4432__delegate.call(this, _, args)
  };
  G__4432.cljs$lang$maxFixedArity = 1;
  G__4432.cljs$lang$applyTo = function(arglist__4433) {
    var _ = cljs.core.first(arglist__4433);
    var args = cljs.core.rest(arglist__4433);
    return G__4432__delegate.call(this, _, args)
  };
  return G__4432
}();
cljs.core.MultiFn.prototype.apply = function(_, args) {
  return cljs.core._dispatch.call(null, this, args)
};
cljs.core.remove_all_methods = function remove_all_methods(multifn) {
  return cljs.core._reset.call(null, multifn)
};
cljs.core.remove_method = function remove_method(multifn, dispatch_val) {
  return cljs.core._remove_method.call(null, multifn, dispatch_val)
};
cljs.core.prefer_method = function prefer_method(multifn, dispatch_val_x, dispatch_val_y) {
  return cljs.core._prefer_method.call(null, multifn, dispatch_val_x, dispatch_val_y)
};
cljs.core.methods$ = function methods$(multifn) {
  return cljs.core._methods.call(null, multifn)
};
cljs.core.get_method = function get_method(multifn, dispatch_val) {
  return cljs.core._get_method.call(null, multifn, dispatch_val)
};
cljs.core.prefers = function prefers(multifn) {
  return cljs.core._prefers.call(null, multifn)
};
goog.provide("cljs.reader");
goog.require("cljs.core");
goog.require("goog.string");
cljs.reader.PushbackReader = {};
cljs.reader.read_char = function read_char(reader) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____4435 = reader;
    if(cljs.core.truth_(and__3546__auto____4435)) {
      return reader.cljs$reader$PushbackReader$read_char
    }else {
      return and__3546__auto____4435
    }
  }())) {
    return reader.cljs$reader$PushbackReader$read_char(reader)
  }else {
    return function() {
      var or__3548__auto____4436 = cljs.reader.read_char[goog.typeOf.call(null, reader)];
      if(cljs.core.truth_(or__3548__auto____4436)) {
        return or__3548__auto____4436
      }else {
        var or__3548__auto____4437 = cljs.reader.read_char["_"];
        if(cljs.core.truth_(or__3548__auto____4437)) {
          return or__3548__auto____4437
        }else {
          throw cljs.core.missing_protocol.call(null, "PushbackReader.read-char", reader);
        }
      }
    }().call(null, reader)
  }
};
cljs.reader.unread = function unread(reader, ch) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____4438 = reader;
    if(cljs.core.truth_(and__3546__auto____4438)) {
      return reader.cljs$reader$PushbackReader$unread
    }else {
      return and__3546__auto____4438
    }
  }())) {
    return reader.cljs$reader$PushbackReader$unread(reader, ch)
  }else {
    return function() {
      var or__3548__auto____4439 = cljs.reader.unread[goog.typeOf.call(null, reader)];
      if(cljs.core.truth_(or__3548__auto____4439)) {
        return or__3548__auto____4439
      }else {
        var or__3548__auto____4440 = cljs.reader.unread["_"];
        if(cljs.core.truth_(or__3548__auto____4440)) {
          return or__3548__auto____4440
        }else {
          throw cljs.core.missing_protocol.call(null, "PushbackReader.unread", reader);
        }
      }
    }().call(null, reader, ch)
  }
};
cljs.reader.StringPushbackReader = function(s, index_atom, buffer_atom) {
  this.s = s;
  this.index_atom = index_atom;
  this.buffer_atom = buffer_atom
};
cljs.reader.StringPushbackReader.cljs$core$IPrintable$_pr_seq = function(this__267__auto__) {
  return cljs.core.list.call(null, "cljs.reader.StringPushbackReader")
};
cljs.reader.StringPushbackReader.prototype.cljs$reader$PushbackReader$ = true;
cljs.reader.StringPushbackReader.prototype.cljs$reader$PushbackReader$read_char = function(reader) {
  var this__4441 = this;
  if(cljs.core.truth_(cljs.core.empty_QMARK_.call(null, cljs.core.deref.call(null, this__4441.buffer_atom)))) {
    var idx__4442 = cljs.core.deref.call(null, this__4441.index_atom);
    cljs.core.swap_BANG_.call(null, this__4441.index_atom, cljs.core.inc);
    return cljs.core.nth.call(null, this__4441.s, idx__4442)
  }else {
    var buf__4443 = cljs.core.deref.call(null, this__4441.buffer_atom);
    cljs.core.swap_BANG_.call(null, this__4441.buffer_atom, cljs.core.rest);
    return cljs.core.first.call(null, buf__4443)
  }
};
cljs.reader.StringPushbackReader.prototype.cljs$reader$PushbackReader$unread = function(reader, ch) {
  var this__4444 = this;
  return cljs.core.swap_BANG_.call(null, this__4444.buffer_atom, function(p1__4434_SHARP_) {
    return cljs.core.cons.call(null, ch, p1__4434_SHARP_)
  })
};
cljs.reader.StringPushbackReader;
cljs.reader.push_back_reader = function push_back_reader(s) {
  return new cljs.reader.StringPushbackReader(s, cljs.core.atom.call(null, 0), cljs.core.atom.call(null, null))
};
cljs.reader.whitespace_QMARK_ = function whitespace_QMARK_(ch) {
  var or__3548__auto____4445 = goog.string.isBreakingWhitespace.call(null, ch);
  if(cljs.core.truth_(or__3548__auto____4445)) {
    return or__3548__auto____4445
  }else {
    return cljs.core._EQ_.call(null, ",", ch)
  }
};
cljs.reader.numeric_QMARK_ = function numeric_QMARK_(ch) {
  return goog.string.isNumeric.call(null, ch)
};
cljs.reader.comment_prefix_QMARK_ = function comment_prefix_QMARK_(ch) {
  return cljs.core._EQ_.call(null, ";", ch)
};
cljs.reader.number_literal_QMARK_ = function number_literal_QMARK_(reader, initch) {
  var or__3548__auto____4446 = cljs.reader.numeric_QMARK_.call(null, initch);
  if(cljs.core.truth_(or__3548__auto____4446)) {
    return or__3548__auto____4446
  }else {
    var and__3546__auto____4448 = function() {
      var or__3548__auto____4447 = cljs.core._EQ_.call(null, "+", initch);
      if(cljs.core.truth_(or__3548__auto____4447)) {
        return or__3548__auto____4447
      }else {
        return cljs.core._EQ_.call(null, "-", initch)
      }
    }();
    if(cljs.core.truth_(and__3546__auto____4448)) {
      return cljs.reader.numeric_QMARK_.call(null, function() {
        var next_ch__4449 = cljs.reader.read_char.call(null, reader);
        cljs.reader.unread.call(null, reader, next_ch__4449);
        return next_ch__4449
      }())
    }else {
      return and__3546__auto____4448
    }
  }
};
cljs.reader.reader_error = function() {
  var reader_error__delegate = function(rdr, msg) {
    throw cljs.core.apply.call(null, cljs.core.str, msg);
  };
  var reader_error = function(rdr, var_args) {
    var msg = null;
    if(goog.isDef(var_args)) {
      msg = cljs.core.array_seq(Array.prototype.slice.call(arguments, 1), 0)
    }
    return reader_error__delegate.call(this, rdr, msg)
  };
  reader_error.cljs$lang$maxFixedArity = 1;
  reader_error.cljs$lang$applyTo = function(arglist__4450) {
    var rdr = cljs.core.first(arglist__4450);
    var msg = cljs.core.rest(arglist__4450);
    return reader_error__delegate.call(this, rdr, msg)
  };
  return reader_error
}();
cljs.reader.macro_terminating_QMARK_ = function macro_terminating_QMARK_(ch) {
  var and__3546__auto____4451 = cljs.core.not_EQ_.call(null, ch, "#");
  if(cljs.core.truth_(and__3546__auto____4451)) {
    var and__3546__auto____4452 = cljs.core.not_EQ_.call(null, ch, "'");
    if(cljs.core.truth_(and__3546__auto____4452)) {
      var and__3546__auto____4453 = cljs.core.not_EQ_.call(null, ch, ":");
      if(cljs.core.truth_(and__3546__auto____4453)) {
        return cljs.core.contains_QMARK_.call(null, cljs.reader.macros, ch)
      }else {
        return and__3546__auto____4453
      }
    }else {
      return and__3546__auto____4452
    }
  }else {
    return and__3546__auto____4451
  }
};
cljs.reader.read_token = function read_token(rdr, initch) {
  var sb__4454 = new goog.string.StringBuffer(initch);
  var ch__4455 = cljs.reader.read_char.call(null, rdr);
  while(true) {
    if(cljs.core.truth_(function() {
      var or__3548__auto____4456 = ch__4455 === null;
      if(cljs.core.truth_(or__3548__auto____4456)) {
        return or__3548__auto____4456
      }else {
        var or__3548__auto____4457 = cljs.reader.whitespace_QMARK_.call(null, ch__4455);
        if(cljs.core.truth_(or__3548__auto____4457)) {
          return or__3548__auto____4457
        }else {
          return cljs.reader.macro_terminating_QMARK_.call(null, ch__4455)
        }
      }
    }())) {
      cljs.reader.unread.call(null, rdr, ch__4455);
      return sb__4454.toString()
    }else {
      var G__4458 = function() {
        sb__4454.append(ch__4455);
        return sb__4454
      }();
      var G__4459 = cljs.reader.read_char.call(null, rdr);
      sb__4454 = G__4458;
      ch__4455 = G__4459;
      continue
    }
    break
  }
};
cljs.reader.skip_line = function skip_line(reader, _) {
  while(true) {
    var ch__4460 = cljs.reader.read_char.call(null, reader);
    if(cljs.core.truth_(function() {
      var or__3548__auto____4461 = cljs.core._EQ_.call(null, ch__4460, "n");
      if(cljs.core.truth_(or__3548__auto____4461)) {
        return or__3548__auto____4461
      }else {
        var or__3548__auto____4462 = cljs.core._EQ_.call(null, ch__4460, "r");
        if(cljs.core.truth_(or__3548__auto____4462)) {
          return or__3548__auto____4462
        }else {
          return ch__4460 === null
        }
      }
    }())) {
      return reader
    }else {
      continue
    }
    break
  }
};
cljs.reader.int_pattern = cljs.core.re_pattern.call(null, "([-+]?)(?:(0)|([1-9][0-9]*)|0[xX]([0-9A-Fa-f]+)|0([0-7]+)|([1-9][0-9]?)[rR]([0-9A-Za-z]+)|0[0-9]+)(N)?");
cljs.reader.ratio_pattern = cljs.core.re_pattern.call(null, "([-+]?[0-9]+)/([0-9]+)");
cljs.reader.float_pattern = cljs.core.re_pattern.call(null, "([-+]?[0-9]+(\\.[0-9]*)?([eE][-+]?[0-9]+)?)(M)?");
cljs.reader.symbol_pattern = cljs.core.re_pattern.call(null, "[:]?([^0-9/].*/)?([^0-9/][^/]*)");
cljs.reader.match_int = function match_int(s) {
  var groups__4463 = cljs.core.re_find.call(null, cljs.reader.int_pattern, s);
  var group3__4464 = cljs.core.nth.call(null, groups__4463, 2);
  if(cljs.core.truth_(cljs.core.not.call(null, function() {
    var or__3548__auto____4465 = void 0 === group3__4464;
    if(cljs.core.truth_(or__3548__auto____4465)) {
      return or__3548__auto____4465
    }else {
      return group3__4464.length < 1
    }
  }()))) {
    return 0
  }else {
    var negate__4467 = cljs.core.truth_(cljs.core._EQ_.call(null, "-", cljs.core.nth.call(null, groups__4463, 1))) ? -1 : 1;
    var vec__4466__4468 = cljs.core.truth_(cljs.core.nth.call(null, groups__4463, 3)) ? cljs.core.Vector.fromArray([cljs.core.nth.call(null, groups__4463, 3), 10]) : cljs.core.truth_(cljs.core.nth.call(null, groups__4463, 4)) ? cljs.core.Vector.fromArray([cljs.core.nth.call(null, groups__4463, 4), 16]) : cljs.core.truth_(cljs.core.nth.call(null, groups__4463, 5)) ? cljs.core.Vector.fromArray([cljs.core.nth.call(null, groups__4463, 5), 8]) : cljs.core.truth_(cljs.core.nth.call(null, groups__4463, 
    7)) ? cljs.core.Vector.fromArray([cljs.core.nth.call(null, groups__4463, 7), parseInt.call(null, cljs.core.nth.call(null, groups__4463, 7))]) : cljs.core.truth_("\ufdd0'default") ? cljs.core.Vector.fromArray([null, null]) : null;
    var n__4469 = cljs.core.nth.call(null, vec__4466__4468, 0, null);
    var radix__4470 = cljs.core.nth.call(null, vec__4466__4468, 1, null);
    if(cljs.core.truth_(n__4469 === null)) {
      return null
    }else {
      return negate__4467 * parseInt.call(null, n__4469, radix__4470)
    }
  }
};
cljs.reader.match_ratio = function match_ratio(s) {
  var groups__4471 = cljs.core.re_find.call(null, cljs.reader.ratio_pattern, s);
  var numinator__4472 = cljs.core.nth.call(null, groups__4471, 1);
  var denominator__4473 = cljs.core.nth.call(null, groups__4471, 2);
  return parseInt.call(null, numinator__4472) / parseInt.call(null, denominator__4473)
};
cljs.reader.match_float = function match_float(s) {
  return parseFloat.call(null, s)
};
cljs.reader.match_number = function match_number(s) {
  if(cljs.core.truth_(cljs.core.re_matches.call(null, cljs.reader.int_pattern, s))) {
    return cljs.reader.match_int.call(null, s)
  }else {
    if(cljs.core.truth_(cljs.core.re_matches.call(null, cljs.reader.ratio_pattern, s))) {
      return cljs.reader.match_ratio.call(null, s)
    }else {
      if(cljs.core.truth_(cljs.core.re_matches.call(null, cljs.reader.float_pattern, s))) {
        return cljs.reader.match_float.call(null, s)
      }else {
        return null
      }
    }
  }
};
cljs.reader.escape_char_map = cljs.core.HashMap.fromArrays(["t", "r", "n", "\\", '"', "b", "f"], ["\t", "\r", "\n", "\\", '"', "\u0008", "\u000c"]);
cljs.reader.read_unicode_char = function read_unicode_char(reader, initch) {
  return cljs.reader.reader_error.call(null, reader, "Unicode characters not supported by reader (yet)")
};
cljs.reader.escape_char = function escape_char(buffer, reader) {
  var ch__4474 = cljs.reader.read_char.call(null, reader);
  var mapresult__4475 = cljs.core.get.call(null, cljs.reader.escape_char_map, ch__4474);
  if(cljs.core.truth_(mapresult__4475)) {
    return mapresult__4475
  }else {
    if(cljs.core.truth_(function() {
      var or__3548__auto____4476 = cljs.core._EQ_.call(null, "u", ch__4474);
      if(cljs.core.truth_(or__3548__auto____4476)) {
        return or__3548__auto____4476
      }else {
        return cljs.reader.numeric_QMARK_.call(null, ch__4474)
      }
    }())) {
      return cljs.reader.read_unicode_char.call(null, reader, ch__4474)
    }else {
      return cljs.reader.reader_error.call(null, reader, "Unsupported escape charater: \\", ch__4474)
    }
  }
};
cljs.reader.read_past = function read_past(pred, rdr) {
  var ch__4477 = cljs.reader.read_char.call(null, rdr);
  while(true) {
    if(cljs.core.truth_(pred.call(null, ch__4477))) {
      var G__4478 = cljs.reader.read_char.call(null, rdr);
      ch__4477 = G__4478;
      continue
    }else {
      return ch__4477
    }
    break
  }
};
cljs.reader.read_delimited_list = function read_delimited_list(delim, rdr, recursive_QMARK_) {
  var a__4479 = cljs.core.Vector.fromArray([]);
  while(true) {
    var ch__4480 = cljs.reader.read_past.call(null, cljs.reader.whitespace_QMARK_, rdr);
    if(cljs.core.truth_(ch__4480)) {
    }else {
      cljs.reader.reader_error.call(null, rdr, "EOF")
    }
    if(cljs.core.truth_(cljs.core._EQ_.call(null, delim, ch__4480))) {
      return a__4479
    }else {
      var temp__3695__auto____4481 = cljs.core.get.call(null, cljs.reader.macros, ch__4480);
      if(cljs.core.truth_(temp__3695__auto____4481)) {
        var macrofn__4482 = temp__3695__auto____4481;
        var mret__4483 = macrofn__4482.call(null, rdr, ch__4480);
        var G__4485 = cljs.core.truth_(cljs.core._EQ_.call(null, mret__4483, rdr)) ? a__4479 : cljs.core.conj.call(null, a__4479, mret__4483);
        a__4479 = G__4485;
        continue
      }else {
        cljs.reader.unread.call(null, rdr, ch__4480);
        var o__4484 = cljs.reader.read.call(null, rdr, true, null, recursive_QMARK_);
        var G__4486 = cljs.core.truth_(cljs.core._EQ_.call(null, o__4484, rdr)) ? a__4479 : cljs.core.conj.call(null, a__4479, o__4484);
        a__4479 = G__4486;
        continue
      }
    }
    break
  }
};
cljs.reader.not_implemented = function not_implemented(rdr, ch) {
  return cljs.reader.reader_error.call(null, rdr, "Reader for ", ch, " not implemented yet")
};
cljs.reader.read_dispatch = function read_dispatch(rdr, _) {
  var ch__4487 = cljs.reader.read_char.call(null, rdr);
  var dm__4488 = cljs.core.get.call(null, cljs.reader.dispatch_macros, ch__4487);
  if(cljs.core.truth_(dm__4488)) {
    return dm__4488.call(null, rdr, _)
  }else {
    return cljs.reader.reader_error.call(null, rdr, "No dispatch macro for ", ch__4487)
  }
};
cljs.reader.read_unmatched_delimiter = function read_unmatched_delimiter(rdr, ch) {
  return cljs.reader.reader_error.call(null, rdr, "Unmached delimiter ", ch)
};
cljs.reader.read_list = function read_list(rdr, _) {
  return cljs.core.apply.call(null, cljs.core.list, cljs.reader.read_delimited_list.call(null, ")", rdr, true))
};
cljs.reader.read_comment = cljs.reader.skip_line;
cljs.reader.read_vector = function read_vector(rdr, _) {
  return cljs.reader.read_delimited_list.call(null, "]", rdr, true)
};
cljs.reader.read_map = function read_map(rdr, _) {
  var l__4489 = cljs.reader.read_delimited_list.call(null, "}", rdr, true);
  if(cljs.core.truth_(cljs.core.odd_QMARK_.call(null, cljs.core.count.call(null, l__4489)))) {
    cljs.reader.reader_error.call(null, rdr, "Map literal must contain an even number of forms")
  }else {
  }
  return cljs.core.apply.call(null, cljs.core.hash_map, l__4489)
};
cljs.reader.read_number = function read_number(reader, initch) {
  var buffer__4490 = new goog.string.StringBuffer(initch);
  var ch__4491 = cljs.reader.read_char.call(null, reader);
  while(true) {
    if(cljs.core.truth_(function() {
      var or__3548__auto____4492 = ch__4491 === null;
      if(cljs.core.truth_(or__3548__auto____4492)) {
        return or__3548__auto____4492
      }else {
        var or__3548__auto____4493 = cljs.reader.whitespace_QMARK_.call(null, ch__4491);
        if(cljs.core.truth_(or__3548__auto____4493)) {
          return or__3548__auto____4493
        }else {
          return cljs.core.contains_QMARK_.call(null, cljs.reader.macros, ch__4491)
        }
      }
    }())) {
      cljs.reader.unread.call(null, reader, ch__4491);
      var s__4494 = buffer__4490.toString();
      var or__3548__auto____4495 = cljs.reader.match_number.call(null, s__4494);
      if(cljs.core.truth_(or__3548__auto____4495)) {
        return or__3548__auto____4495
      }else {
        return cljs.reader.reader_error.call(null, reader, "Invalid number format [", s__4494, "]")
      }
    }else {
      var G__4496 = function() {
        buffer__4490.append(ch__4491);
        return buffer__4490
      }();
      var G__4497 = cljs.reader.read_char.call(null, reader);
      buffer__4490 = G__4496;
      ch__4491 = G__4497;
      continue
    }
    break
  }
};
cljs.reader.read_string = function read_string(reader, _) {
  var buffer__4498 = new goog.string.StringBuffer;
  var ch__4499 = cljs.reader.read_char.call(null, reader);
  while(true) {
    if(cljs.core.truth_(ch__4499 === null)) {
      return cljs.reader.reader_error.call(null, reader, "EOF while reading string")
    }else {
      if(cljs.core.truth_(cljs.core._EQ_.call(null, "\\", ch__4499))) {
        var G__4500 = function() {
          buffer__4498.append(cljs.reader.escape_char.call(null, buffer__4498, reader));
          return buffer__4498
        }();
        var G__4501 = cljs.reader.read_char.call(null, reader);
        buffer__4498 = G__4500;
        ch__4499 = G__4501;
        continue
      }else {
        if(cljs.core.truth_(cljs.core._EQ_.call(null, '"', ch__4499))) {
          return buffer__4498.toString()
        }else {
          if(cljs.core.truth_("\ufdd0'default")) {
            var G__4502 = function() {
              buffer__4498.append(ch__4499);
              return buffer__4498
            }();
            var G__4503 = cljs.reader.read_char.call(null, reader);
            buffer__4498 = G__4502;
            ch__4499 = G__4503;
            continue
          }else {
            return null
          }
        }
      }
    }
    break
  }
};
cljs.reader.special_symbols = cljs.core.ObjMap.fromObject(["nil", "true", "false"], {"nil":null, "true":true, "false":false});
cljs.reader.read_symbol = function read_symbol(reader, initch) {
  var token__4504 = cljs.reader.read_token.call(null, reader, initch);
  if(cljs.core.truth_(goog.string.contains.call(null, token__4504, "/"))) {
    return cljs.core.symbol.call(null, cljs.core.subs.call(null, token__4504, 0, token__4504.indexOf("/")), cljs.core.subs.call(null, token__4504, token__4504.indexOf("/") + 1, token__4504.length))
  }else {
    return cljs.core.get.call(null, cljs.reader.special_symbols, token__4504, cljs.core.symbol.call(null, token__4504))
  }
};
cljs.reader.read_keyword = function read_keyword(reader, initch) {
  var token__4506 = cljs.reader.read_token.call(null, reader, cljs.reader.read_char.call(null, reader));
  var vec__4505__4507 = cljs.core.re_matches.call(null, cljs.reader.symbol_pattern, token__4506);
  var token__4508 = cljs.core.nth.call(null, vec__4505__4507, 0, null);
  var ns__4509 = cljs.core.nth.call(null, vec__4505__4507, 1, null);
  var name__4510 = cljs.core.nth.call(null, vec__4505__4507, 2, null);
  if(cljs.core.truth_(function() {
    var or__3548__auto____4512 = function() {
      var and__3546__auto____4511 = cljs.core.not.call(null, void 0 === ns__4509);
      if(cljs.core.truth_(and__3546__auto____4511)) {
        return ns__4509.substring(ns__4509.length - 2, ns__4509.length) === ":/"
      }else {
        return and__3546__auto____4511
      }
    }();
    if(cljs.core.truth_(or__3548__auto____4512)) {
      return or__3548__auto____4512
    }else {
      var or__3548__auto____4513 = name__4510[name__4510.length - 1] === ":";
      if(cljs.core.truth_(or__3548__auto____4513)) {
        return or__3548__auto____4513
      }else {
        return cljs.core.not.call(null, token__4508.indexOf("::", 1) === -1)
      }
    }
  }())) {
    return cljs.reader.reader_error.call(null, reader, "Invalid token: ", token__4508)
  }else {
    if(cljs.core.truth_(cljs.reader.ns_QMARK_)) {
      return cljs.core.keyword.call(null, ns__4509.substring(0, ns__4509.indexOf("/")), name__4510)
    }else {
      return cljs.core.keyword.call(null, token__4508)
    }
  }
};
cljs.reader.desugar_meta = function desugar_meta(f) {
  if(cljs.core.truth_(cljs.core.symbol_QMARK_.call(null, f))) {
    return cljs.core.ObjMap.fromObject(["\ufdd0'tag"], {"\ufdd0'tag":f})
  }else {
    if(cljs.core.truth_(cljs.core.string_QMARK_.call(null, f))) {
      return cljs.core.ObjMap.fromObject(["\ufdd0'tag"], {"\ufdd0'tag":f})
    }else {
      if(cljs.core.truth_(cljs.core.keyword_QMARK_.call(null, f))) {
        return cljs.core.HashMap.fromArrays([f], [true])
      }else {
        if(cljs.core.truth_("\ufdd0'else")) {
          return f
        }else {
          return null
        }
      }
    }
  }
};
cljs.reader.wrapping_reader = function wrapping_reader(sym) {
  return function(rdr, _) {
    return cljs.core.list.call(null, sym, cljs.reader.read.call(null, rdr, true, null, true))
  }
};
cljs.reader.throwing_reader = function throwing_reader(msg) {
  return function(rdr, _) {
    return cljs.reader.reader_error.call(null, rdr, msg)
  }
};
cljs.reader.read_meta = function read_meta(rdr, _) {
  var m__4514 = cljs.reader.desugar_meta.call(null, cljs.reader.read.call(null, rdr, true, null, true));
  if(cljs.core.truth_(cljs.core.map_QMARK_.call(null, m__4514))) {
  }else {
    cljs.reader.reader_error.call(null, rdr, "Metadata must be Symbol,Keyword,String or Map")
  }
  var o__4515 = cljs.reader.read.call(null, rdr, true, null, true);
  if(cljs.core.truth_(function() {
    var x__352__auto____4516 = o__4515;
    if(cljs.core.truth_(function() {
      var and__3546__auto____4517 = x__352__auto____4516;
      if(cljs.core.truth_(and__3546__auto____4517)) {
        var and__3546__auto____4518 = x__352__auto____4516.cljs$core$IWithMeta$;
        if(cljs.core.truth_(and__3546__auto____4518)) {
          return cljs.core.not.call(null, x__352__auto____4516.hasOwnProperty("cljs$core$IWithMeta$"))
        }else {
          return and__3546__auto____4518
        }
      }else {
        return and__3546__auto____4517
      }
    }())) {
      return true
    }else {
      return cljs.core.type_satisfies_.call(null, cljs.core.IWithMeta, x__352__auto____4516)
    }
  }())) {
    return cljs.core.with_meta.call(null, o__4515, cljs.core.merge.call(null, cljs.core.meta.call(null, o__4515), m__4514))
  }else {
    return cljs.reader.reader_error.call(null, rdr, "Metadata can only be applied to IWithMetas")
  }
};
cljs.reader.read_set = function read_set(rdr, _) {
  return cljs.core.set.call(null, cljs.reader.read_delimited_list.call(null, "}", rdr, true))
};
cljs.reader.read_regex = function read_regex(rdr, ch) {
  return cljs.core.re_pattern.call(null, cljs.reader.read_string.call(null, rdr, ch))
};
cljs.reader.read_discard = function read_discard(rdr, _) {
  cljs.reader.read.call(null, rdr, true, null, true);
  return rdr
};
cljs.reader.macros = cljs.core.HashMap.fromArrays(["@", "`", '"', "#", "%", "'", "(", ")", ":", ";", "[", "{", "\\", "]", "}", "^", "~"], [cljs.reader.wrapping_reader.call(null, "\ufdd1'deref"), cljs.reader.not_implemented, cljs.reader.read_string, cljs.reader.read_dispatch, cljs.reader.not_implemented, cljs.reader.wrapping_reader.call(null, "\ufdd1'quote"), cljs.reader.read_list, cljs.reader.read_unmatched_delimiter, cljs.reader.read_keyword, cljs.reader.not_implemented, cljs.reader.read_vector, 
cljs.reader.read_map, cljs.reader.read_char, cljs.reader.read_unmatched_delimiter, cljs.reader.read_unmatched_delimiter, cljs.reader.read_meta, cljs.reader.not_implemented]);
cljs.reader.dispatch_macros = cljs.core.ObjMap.fromObject(["{", "<", '"', "!", "_"], {"{":cljs.reader.read_set, "<":cljs.reader.throwing_reader.call(null, "Unreadable form"), '"':cljs.reader.read_regex, "!":cljs.reader.read_comment, "_":cljs.reader.read_discard});
cljs.reader.read = function read(reader, eof_is_error, sentinel, is_recursive) {
  while(true) {
    var ch__4519 = cljs.reader.read_char.call(null, reader);
    if(cljs.core.truth_(ch__4519 === null)) {
      if(cljs.core.truth_(eof_is_error)) {
        return cljs.reader.reader_error.call(null, reader, "EOF")
      }else {
        return sentinel
      }
    }else {
      if(cljs.core.truth_(cljs.reader.whitespace_QMARK_.call(null, ch__4519))) {
        var G__4521 = reader;
        var G__4522 = eof_is_error;
        var G__4523 = sentinel;
        var G__4524 = is_recursive;
        reader = G__4521;
        eof_is_error = G__4522;
        sentinel = G__4523;
        is_recursive = G__4524;
        continue
      }else {
        if(cljs.core.truth_(cljs.reader.comment_prefix_QMARK_.call(null, ch__4519))) {
          var G__4525 = cljs.reader.read_comment.call(null, reader, ch__4519);
          var G__4526 = eof_is_error;
          var G__4527 = sentinel;
          var G__4528 = is_recursive;
          reader = G__4525;
          eof_is_error = G__4526;
          sentinel = G__4527;
          is_recursive = G__4528;
          continue
        }else {
          if(cljs.core.truth_("\ufdd0'else")) {
            var res__4520 = cljs.core.truth_(cljs.reader.macros.call(null, ch__4519)) ? cljs.reader.macros.call(null, ch__4519).call(null, reader, ch__4519) : cljs.core.truth_(cljs.reader.number_literal_QMARK_.call(null, reader, ch__4519)) ? cljs.reader.read_number.call(null, reader, ch__4519) : cljs.core.truth_("\ufdd0'else") ? cljs.reader.read_symbol.call(null, reader, ch__4519) : null;
            if(cljs.core.truth_(cljs.core._EQ_.call(null, res__4520, reader))) {
              var G__4529 = reader;
              var G__4530 = eof_is_error;
              var G__4531 = sentinel;
              var G__4532 = is_recursive;
              reader = G__4529;
              eof_is_error = G__4530;
              sentinel = G__4531;
              is_recursive = G__4532;
              continue
            }else {
              return res__4520
            }
          }else {
            return null
          }
        }
      }
    }
    break
  }
};
cljs.reader.read_string = function read_string(s) {
  var r__4533 = cljs.reader.push_back_reader.call(null, s);
  return cljs.reader.read.call(null, r__4533, true, null, false)
};
goog.provide("org.healthsciencessc.rpms2.core");
goog.require("cljs.core");
goog.require("cljs.reader");
org.healthsciencessc.rpms2.core.ajax_sexp_get = function ajax_sexp_get(path, callback) {
  return $.get(path, function(data) {
    return callback.call(null, cljs.reader.read_string.call(null, data))
  }, "text")
};
org.healthsciencessc.rpms2.core.search = function search() {
  return org.healthsciencessc.rpms2.core.ajax_sexp_get.call(null, "/sexp/search/consenters", function(data) {
    return alert.call(null, cljs.core.str.call(null, "I got this data: ", cljs.core.pr_str.call(null, data)))
  })
};
goog.exportSymbol("org.healthsciencessc.rpms2.core.search", org.healthsciencessc.rpms2.core.search);
org.healthsciencessc.rpms2.core.consenter_search_result_clicked = function consenter_search_result_clicked(div) {
  var user__2731 = cljs.reader.read_string.call(null, div.getAttribute("data-user"));
  var details__2732 = $.call(null, "#consenter-details");
  var details_record__2733 = $.call(null, "#consenter-details-section");
  var other_section__2734 = $.call(null, "#other-section");
  details_record__2733.find(cljs.core.str.call(null, "#consenter-visit-number")).text("\ufdd0'visit-number".call(null, user__2731));
  other_section__2734.find(cljs.core.str.call(null, "#patient-id")).val("\ufdd0'medical-record-number".call(null, user__2731));
  other_section__2734.find(cljs.core.str.call(null, "#patient-name")).val(cljs.core.str.call(null, "\ufdd0'firstname".call(null, user__2731), " ", "\ufdd0'lastname".call(null, user__2731)));
  other_section__2734.find(cljs.core.str.call(null, "#patient-encounter-date")).val("\ufdd0'consenter-encounter-date".call(null, user__2731));
  details_record__2733.find(cljs.core.str.call(null, "#consenter-medical-record-number")).text("\ufdd0'medical-record-number".call(null, user__2731));
  details_record__2733.find(cljs.core.str.call(null, "#consenter-encounter-date")).text("\ufdd0'encounter-date".call(null, user__2731));
  details__2732.find(cljs.core.str.call(null, "#consenter-name")).text(cljs.core.str.call(null, "\ufdd0'firstname".call(null, user__2731), " ", "\ufdd0'lastname".call(null, user__2731)));
  details__2732.find(cljs.core.str.call(null, "#consenter-zipcode")).text("\ufdd0'zipcode".call(null, user__2731));
  details__2732.find("#consenter-zipcode").text("\ufdd0'zipcode".call(null, user__2731));
  details__2732.find("#consenter-date-of-birth").text("\ufdd0'date-of-birth".call(null, user__2731));
  details__2732.find("#consenter-last-4-digits-ssn").text("\ufdd0'last-4-digits-ssn".call(null, user__2731));
  details__2732.find("#consenter-referring-doctor").text("\ufdd0'referring-doctor".call(null, user__2731));
  details__2732.find("#consenter-primary-care-physician").text("\ufdd0'primary-care-physician".call(null, user__2731));
  return details__2732.find("#consenter-primary-care-physician-city").text("\ufdd0'primary-care-physician-city".call(null, user__2731))
};
goog.exportSymbol("org.healthsciencessc.rpms2.core.consenter_search_result_clicked", org.healthsciencessc.rpms2.core.consenter_search_result_clicked);
