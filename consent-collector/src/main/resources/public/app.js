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
  var or__3548__auto____2761 = p[goog.typeOf.call(null, x)];
  if(cljs.core.truth_(or__3548__auto____2761)) {
    return or__3548__auto____2761
  }else {
    var or__3548__auto____2762 = p["_"];
    if(cljs.core.truth_(or__3548__auto____2762)) {
      return or__3548__auto____2762
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
  var _invoke__2826 = function(this$) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2763 = this$;
      if(cljs.core.truth_(and__3546__auto____2763)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2763
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$)
    }else {
      return function() {
        var or__3548__auto____2764 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2764)) {
          return or__3548__auto____2764
        }else {
          var or__3548__auto____2765 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2765)) {
            return or__3548__auto____2765
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$)
    }
  };
  var _invoke__2827 = function(this$, a) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2766 = this$;
      if(cljs.core.truth_(and__3546__auto____2766)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2766
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a)
    }else {
      return function() {
        var or__3548__auto____2767 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2767)) {
          return or__3548__auto____2767
        }else {
          var or__3548__auto____2768 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2768)) {
            return or__3548__auto____2768
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a)
    }
  };
  var _invoke__2828 = function(this$, a, b) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2769 = this$;
      if(cljs.core.truth_(and__3546__auto____2769)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2769
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b)
    }else {
      return function() {
        var or__3548__auto____2770 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2770)) {
          return or__3548__auto____2770
        }else {
          var or__3548__auto____2771 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2771)) {
            return or__3548__auto____2771
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b)
    }
  };
  var _invoke__2829 = function(this$, a, b, c) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2772 = this$;
      if(cljs.core.truth_(and__3546__auto____2772)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2772
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c)
    }else {
      return function() {
        var or__3548__auto____2773 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2773)) {
          return or__3548__auto____2773
        }else {
          var or__3548__auto____2774 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2774)) {
            return or__3548__auto____2774
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c)
    }
  };
  var _invoke__2830 = function(this$, a, b, c, d) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2775 = this$;
      if(cljs.core.truth_(and__3546__auto____2775)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2775
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d)
    }else {
      return function() {
        var or__3548__auto____2776 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2776)) {
          return or__3548__auto____2776
        }else {
          var or__3548__auto____2777 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2777)) {
            return or__3548__auto____2777
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c, d)
    }
  };
  var _invoke__2831 = function(this$, a, b, c, d, e) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2778 = this$;
      if(cljs.core.truth_(and__3546__auto____2778)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2778
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d, e)
    }else {
      return function() {
        var or__3548__auto____2779 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2779)) {
          return or__3548__auto____2779
        }else {
          var or__3548__auto____2780 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2780)) {
            return or__3548__auto____2780
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c, d, e)
    }
  };
  var _invoke__2832 = function(this$, a, b, c, d, e, f) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2781 = this$;
      if(cljs.core.truth_(and__3546__auto____2781)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2781
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d, e, f)
    }else {
      return function() {
        var or__3548__auto____2782 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2782)) {
          return or__3548__auto____2782
        }else {
          var or__3548__auto____2783 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2783)) {
            return or__3548__auto____2783
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c, d, e, f)
    }
  };
  var _invoke__2833 = function(this$, a, b, c, d, e, f, g) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2784 = this$;
      if(cljs.core.truth_(and__3546__auto____2784)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2784
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d, e, f, g)
    }else {
      return function() {
        var or__3548__auto____2785 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2785)) {
          return or__3548__auto____2785
        }else {
          var or__3548__auto____2786 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2786)) {
            return or__3548__auto____2786
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c, d, e, f, g)
    }
  };
  var _invoke__2834 = function(this$, a, b, c, d, e, f, g, h) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2787 = this$;
      if(cljs.core.truth_(and__3546__auto____2787)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2787
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d, e, f, g, h)
    }else {
      return function() {
        var or__3548__auto____2788 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2788)) {
          return or__3548__auto____2788
        }else {
          var or__3548__auto____2789 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2789)) {
            return or__3548__auto____2789
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c, d, e, f, g, h)
    }
  };
  var _invoke__2835 = function(this$, a, b, c, d, e, f, g, h, i) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2790 = this$;
      if(cljs.core.truth_(and__3546__auto____2790)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2790
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d, e, f, g, h, i)
    }else {
      return function() {
        var or__3548__auto____2791 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2791)) {
          return or__3548__auto____2791
        }else {
          var or__3548__auto____2792 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2792)) {
            return or__3548__auto____2792
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c, d, e, f, g, h, i)
    }
  };
  var _invoke__2836 = function(this$, a, b, c, d, e, f, g, h, i, j) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2793 = this$;
      if(cljs.core.truth_(and__3546__auto____2793)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2793
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d, e, f, g, h, i, j)
    }else {
      return function() {
        var or__3548__auto____2794 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2794)) {
          return or__3548__auto____2794
        }else {
          var or__3548__auto____2795 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2795)) {
            return or__3548__auto____2795
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c, d, e, f, g, h, i, j)
    }
  };
  var _invoke__2837 = function(this$, a, b, c, d, e, f, g, h, i, j, k) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2796 = this$;
      if(cljs.core.truth_(and__3546__auto____2796)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2796
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d, e, f, g, h, i, j, k)
    }else {
      return function() {
        var or__3548__auto____2797 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2797)) {
          return or__3548__auto____2797
        }else {
          var or__3548__auto____2798 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2798)) {
            return or__3548__auto____2798
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c, d, e, f, g, h, i, j, k)
    }
  };
  var _invoke__2838 = function(this$, a, b, c, d, e, f, g, h, i, j, k, l) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2799 = this$;
      if(cljs.core.truth_(and__3546__auto____2799)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2799
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d, e, f, g, h, i, j, k, l)
    }else {
      return function() {
        var or__3548__auto____2800 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2800)) {
          return or__3548__auto____2800
        }else {
          var or__3548__auto____2801 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2801)) {
            return or__3548__auto____2801
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c, d, e, f, g, h, i, j, k, l)
    }
  };
  var _invoke__2839 = function(this$, a, b, c, d, e, f, g, h, i, j, k, l, m) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2802 = this$;
      if(cljs.core.truth_(and__3546__auto____2802)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2802
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d, e, f, g, h, i, j, k, l, m)
    }else {
      return function() {
        var or__3548__auto____2803 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2803)) {
          return or__3548__auto____2803
        }else {
          var or__3548__auto____2804 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2804)) {
            return or__3548__auto____2804
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c, d, e, f, g, h, i, j, k, l, m)
    }
  };
  var _invoke__2840 = function(this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2805 = this$;
      if(cljs.core.truth_(and__3546__auto____2805)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2805
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n)
    }else {
      return function() {
        var or__3548__auto____2806 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2806)) {
          return or__3548__auto____2806
        }else {
          var or__3548__auto____2807 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2807)) {
            return or__3548__auto____2807
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n)
    }
  };
  var _invoke__2841 = function(this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2808 = this$;
      if(cljs.core.truth_(and__3546__auto____2808)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2808
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o)
    }else {
      return function() {
        var or__3548__auto____2809 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2809)) {
          return or__3548__auto____2809
        }else {
          var or__3548__auto____2810 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2810)) {
            return or__3548__auto____2810
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o)
    }
  };
  var _invoke__2842 = function(this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2811 = this$;
      if(cljs.core.truth_(and__3546__auto____2811)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2811
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p)
    }else {
      return function() {
        var or__3548__auto____2812 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2812)) {
          return or__3548__auto____2812
        }else {
          var or__3548__auto____2813 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2813)) {
            return or__3548__auto____2813
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p)
    }
  };
  var _invoke__2843 = function(this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2814 = this$;
      if(cljs.core.truth_(and__3546__auto____2814)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2814
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q)
    }else {
      return function() {
        var or__3548__auto____2815 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2815)) {
          return or__3548__auto____2815
        }else {
          var or__3548__auto____2816 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2816)) {
            return or__3548__auto____2816
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q)
    }
  };
  var _invoke__2844 = function(this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, s) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2817 = this$;
      if(cljs.core.truth_(and__3546__auto____2817)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2817
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, s)
    }else {
      return function() {
        var or__3548__auto____2818 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2818)) {
          return or__3548__auto____2818
        }else {
          var or__3548__auto____2819 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2819)) {
            return or__3548__auto____2819
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, s)
    }
  };
  var _invoke__2845 = function(this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, s, t) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2820 = this$;
      if(cljs.core.truth_(and__3546__auto____2820)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2820
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, s, t)
    }else {
      return function() {
        var or__3548__auto____2821 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2821)) {
          return or__3548__auto____2821
        }else {
          var or__3548__auto____2822 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2822)) {
            return or__3548__auto____2822
          }else {
            throw cljs.core.missing_protocol.call(null, "IFn.-invoke", this$);
          }
        }
      }().call(null, this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, s, t)
    }
  };
  var _invoke__2846 = function(this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, s, t, rest) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2823 = this$;
      if(cljs.core.truth_(and__3546__auto____2823)) {
        return this$.cljs$core$IFn$_invoke
      }else {
        return and__3546__auto____2823
      }
    }())) {
      return this$.cljs$core$IFn$_invoke(this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, s, t, rest)
    }else {
      return function() {
        var or__3548__auto____2824 = cljs.core._invoke[goog.typeOf.call(null, this$)];
        if(cljs.core.truth_(or__3548__auto____2824)) {
          return or__3548__auto____2824
        }else {
          var or__3548__auto____2825 = cljs.core._invoke["_"];
          if(cljs.core.truth_(or__3548__auto____2825)) {
            return or__3548__auto____2825
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
        return _invoke__2826.call(this, this$);
      case 2:
        return _invoke__2827.call(this, this$, a);
      case 3:
        return _invoke__2828.call(this, this$, a, b);
      case 4:
        return _invoke__2829.call(this, this$, a, b, c);
      case 5:
        return _invoke__2830.call(this, this$, a, b, c, d);
      case 6:
        return _invoke__2831.call(this, this$, a, b, c, d, e);
      case 7:
        return _invoke__2832.call(this, this$, a, b, c, d, e, f);
      case 8:
        return _invoke__2833.call(this, this$, a, b, c, d, e, f, g);
      case 9:
        return _invoke__2834.call(this, this$, a, b, c, d, e, f, g, h);
      case 10:
        return _invoke__2835.call(this, this$, a, b, c, d, e, f, g, h, i);
      case 11:
        return _invoke__2836.call(this, this$, a, b, c, d, e, f, g, h, i, j);
      case 12:
        return _invoke__2837.call(this, this$, a, b, c, d, e, f, g, h, i, j, k);
      case 13:
        return _invoke__2838.call(this, this$, a, b, c, d, e, f, g, h, i, j, k, l);
      case 14:
        return _invoke__2839.call(this, this$, a, b, c, d, e, f, g, h, i, j, k, l, m);
      case 15:
        return _invoke__2840.call(this, this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n);
      case 16:
        return _invoke__2841.call(this, this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o);
      case 17:
        return _invoke__2842.call(this, this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p);
      case 18:
        return _invoke__2843.call(this, this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q);
      case 19:
        return _invoke__2844.call(this, this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, s);
      case 20:
        return _invoke__2845.call(this, this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, s, t);
      case 21:
        return _invoke__2846.call(this, this$, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, s, t, rest)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return _invoke
}();
cljs.core.ICounted = {};
cljs.core._count = function _count(coll) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____2848 = coll;
    if(cljs.core.truth_(and__3546__auto____2848)) {
      return coll.cljs$core$ICounted$_count
    }else {
      return and__3546__auto____2848
    }
  }())) {
    return coll.cljs$core$ICounted$_count(coll)
  }else {
    return function() {
      var or__3548__auto____2849 = cljs.core._count[goog.typeOf.call(null, coll)];
      if(cljs.core.truth_(or__3548__auto____2849)) {
        return or__3548__auto____2849
      }else {
        var or__3548__auto____2850 = cljs.core._count["_"];
        if(cljs.core.truth_(or__3548__auto____2850)) {
          return or__3548__auto____2850
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
    var and__3546__auto____2851 = coll;
    if(cljs.core.truth_(and__3546__auto____2851)) {
      return coll.cljs$core$IEmptyableCollection$_empty
    }else {
      return and__3546__auto____2851
    }
  }())) {
    return coll.cljs$core$IEmptyableCollection$_empty(coll)
  }else {
    return function() {
      var or__3548__auto____2852 = cljs.core._empty[goog.typeOf.call(null, coll)];
      if(cljs.core.truth_(or__3548__auto____2852)) {
        return or__3548__auto____2852
      }else {
        var or__3548__auto____2853 = cljs.core._empty["_"];
        if(cljs.core.truth_(or__3548__auto____2853)) {
          return or__3548__auto____2853
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
    var and__3546__auto____2854 = coll;
    if(cljs.core.truth_(and__3546__auto____2854)) {
      return coll.cljs$core$ICollection$_conj
    }else {
      return and__3546__auto____2854
    }
  }())) {
    return coll.cljs$core$ICollection$_conj(coll, o)
  }else {
    return function() {
      var or__3548__auto____2855 = cljs.core._conj[goog.typeOf.call(null, coll)];
      if(cljs.core.truth_(or__3548__auto____2855)) {
        return or__3548__auto____2855
      }else {
        var or__3548__auto____2856 = cljs.core._conj["_"];
        if(cljs.core.truth_(or__3548__auto____2856)) {
          return or__3548__auto____2856
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
  var _nth__2863 = function(coll, n) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2857 = coll;
      if(cljs.core.truth_(and__3546__auto____2857)) {
        return coll.cljs$core$IIndexed$_nth
      }else {
        return and__3546__auto____2857
      }
    }())) {
      return coll.cljs$core$IIndexed$_nth(coll, n)
    }else {
      return function() {
        var or__3548__auto____2858 = cljs.core._nth[goog.typeOf.call(null, coll)];
        if(cljs.core.truth_(or__3548__auto____2858)) {
          return or__3548__auto____2858
        }else {
          var or__3548__auto____2859 = cljs.core._nth["_"];
          if(cljs.core.truth_(or__3548__auto____2859)) {
            return or__3548__auto____2859
          }else {
            throw cljs.core.missing_protocol.call(null, "IIndexed.-nth", coll);
          }
        }
      }().call(null, coll, n)
    }
  };
  var _nth__2864 = function(coll, n, not_found) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2860 = coll;
      if(cljs.core.truth_(and__3546__auto____2860)) {
        return coll.cljs$core$IIndexed$_nth
      }else {
        return and__3546__auto____2860
      }
    }())) {
      return coll.cljs$core$IIndexed$_nth(coll, n, not_found)
    }else {
      return function() {
        var or__3548__auto____2861 = cljs.core._nth[goog.typeOf.call(null, coll)];
        if(cljs.core.truth_(or__3548__auto____2861)) {
          return or__3548__auto____2861
        }else {
          var or__3548__auto____2862 = cljs.core._nth["_"];
          if(cljs.core.truth_(or__3548__auto____2862)) {
            return or__3548__auto____2862
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
        return _nth__2863.call(this, coll, n);
      case 3:
        return _nth__2864.call(this, coll, n, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return _nth
}();
cljs.core.ISeq = {};
cljs.core._first = function _first(coll) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____2866 = coll;
    if(cljs.core.truth_(and__3546__auto____2866)) {
      return coll.cljs$core$ISeq$_first
    }else {
      return and__3546__auto____2866
    }
  }())) {
    return coll.cljs$core$ISeq$_first(coll)
  }else {
    return function() {
      var or__3548__auto____2867 = cljs.core._first[goog.typeOf.call(null, coll)];
      if(cljs.core.truth_(or__3548__auto____2867)) {
        return or__3548__auto____2867
      }else {
        var or__3548__auto____2868 = cljs.core._first["_"];
        if(cljs.core.truth_(or__3548__auto____2868)) {
          return or__3548__auto____2868
        }else {
          throw cljs.core.missing_protocol.call(null, "ISeq.-first", coll);
        }
      }
    }().call(null, coll)
  }
};
cljs.core._rest = function _rest(coll) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____2869 = coll;
    if(cljs.core.truth_(and__3546__auto____2869)) {
      return coll.cljs$core$ISeq$_rest
    }else {
      return and__3546__auto____2869
    }
  }())) {
    return coll.cljs$core$ISeq$_rest(coll)
  }else {
    return function() {
      var or__3548__auto____2870 = cljs.core._rest[goog.typeOf.call(null, coll)];
      if(cljs.core.truth_(or__3548__auto____2870)) {
        return or__3548__auto____2870
      }else {
        var or__3548__auto____2871 = cljs.core._rest["_"];
        if(cljs.core.truth_(or__3548__auto____2871)) {
          return or__3548__auto____2871
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
  var _lookup__2878 = function(o, k) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2872 = o;
      if(cljs.core.truth_(and__3546__auto____2872)) {
        return o.cljs$core$ILookup$_lookup
      }else {
        return and__3546__auto____2872
      }
    }())) {
      return o.cljs$core$ILookup$_lookup(o, k)
    }else {
      return function() {
        var or__3548__auto____2873 = cljs.core._lookup[goog.typeOf.call(null, o)];
        if(cljs.core.truth_(or__3548__auto____2873)) {
          return or__3548__auto____2873
        }else {
          var or__3548__auto____2874 = cljs.core._lookup["_"];
          if(cljs.core.truth_(or__3548__auto____2874)) {
            return or__3548__auto____2874
          }else {
            throw cljs.core.missing_protocol.call(null, "ILookup.-lookup", o);
          }
        }
      }().call(null, o, k)
    }
  };
  var _lookup__2879 = function(o, k, not_found) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2875 = o;
      if(cljs.core.truth_(and__3546__auto____2875)) {
        return o.cljs$core$ILookup$_lookup
      }else {
        return and__3546__auto____2875
      }
    }())) {
      return o.cljs$core$ILookup$_lookup(o, k, not_found)
    }else {
      return function() {
        var or__3548__auto____2876 = cljs.core._lookup[goog.typeOf.call(null, o)];
        if(cljs.core.truth_(or__3548__auto____2876)) {
          return or__3548__auto____2876
        }else {
          var or__3548__auto____2877 = cljs.core._lookup["_"];
          if(cljs.core.truth_(or__3548__auto____2877)) {
            return or__3548__auto____2877
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
        return _lookup__2878.call(this, o, k);
      case 3:
        return _lookup__2879.call(this, o, k, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return _lookup
}();
cljs.core.IAssociative = {};
cljs.core._contains_key_QMARK_ = function _contains_key_QMARK_(coll, k) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____2881 = coll;
    if(cljs.core.truth_(and__3546__auto____2881)) {
      return coll.cljs$core$IAssociative$_contains_key_QMARK_
    }else {
      return and__3546__auto____2881
    }
  }())) {
    return coll.cljs$core$IAssociative$_contains_key_QMARK_(coll, k)
  }else {
    return function() {
      var or__3548__auto____2882 = cljs.core._contains_key_QMARK_[goog.typeOf.call(null, coll)];
      if(cljs.core.truth_(or__3548__auto____2882)) {
        return or__3548__auto____2882
      }else {
        var or__3548__auto____2883 = cljs.core._contains_key_QMARK_["_"];
        if(cljs.core.truth_(or__3548__auto____2883)) {
          return or__3548__auto____2883
        }else {
          throw cljs.core.missing_protocol.call(null, "IAssociative.-contains-key?", coll);
        }
      }
    }().call(null, coll, k)
  }
};
cljs.core._assoc = function _assoc(coll, k, v) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____2884 = coll;
    if(cljs.core.truth_(and__3546__auto____2884)) {
      return coll.cljs$core$IAssociative$_assoc
    }else {
      return and__3546__auto____2884
    }
  }())) {
    return coll.cljs$core$IAssociative$_assoc(coll, k, v)
  }else {
    return function() {
      var or__3548__auto____2885 = cljs.core._assoc[goog.typeOf.call(null, coll)];
      if(cljs.core.truth_(or__3548__auto____2885)) {
        return or__3548__auto____2885
      }else {
        var or__3548__auto____2886 = cljs.core._assoc["_"];
        if(cljs.core.truth_(or__3548__auto____2886)) {
          return or__3548__auto____2886
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
    var and__3546__auto____2887 = coll;
    if(cljs.core.truth_(and__3546__auto____2887)) {
      return coll.cljs$core$IMap$_dissoc
    }else {
      return and__3546__auto____2887
    }
  }())) {
    return coll.cljs$core$IMap$_dissoc(coll, k)
  }else {
    return function() {
      var or__3548__auto____2888 = cljs.core._dissoc[goog.typeOf.call(null, coll)];
      if(cljs.core.truth_(or__3548__auto____2888)) {
        return or__3548__auto____2888
      }else {
        var or__3548__auto____2889 = cljs.core._dissoc["_"];
        if(cljs.core.truth_(or__3548__auto____2889)) {
          return or__3548__auto____2889
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
    var and__3546__auto____2890 = coll;
    if(cljs.core.truth_(and__3546__auto____2890)) {
      return coll.cljs$core$ISet$_disjoin
    }else {
      return and__3546__auto____2890
    }
  }())) {
    return coll.cljs$core$ISet$_disjoin(coll, v)
  }else {
    return function() {
      var or__3548__auto____2891 = cljs.core._disjoin[goog.typeOf.call(null, coll)];
      if(cljs.core.truth_(or__3548__auto____2891)) {
        return or__3548__auto____2891
      }else {
        var or__3548__auto____2892 = cljs.core._disjoin["_"];
        if(cljs.core.truth_(or__3548__auto____2892)) {
          return or__3548__auto____2892
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
    var and__3546__auto____2893 = coll;
    if(cljs.core.truth_(and__3546__auto____2893)) {
      return coll.cljs$core$IStack$_peek
    }else {
      return and__3546__auto____2893
    }
  }())) {
    return coll.cljs$core$IStack$_peek(coll)
  }else {
    return function() {
      var or__3548__auto____2894 = cljs.core._peek[goog.typeOf.call(null, coll)];
      if(cljs.core.truth_(or__3548__auto____2894)) {
        return or__3548__auto____2894
      }else {
        var or__3548__auto____2895 = cljs.core._peek["_"];
        if(cljs.core.truth_(or__3548__auto____2895)) {
          return or__3548__auto____2895
        }else {
          throw cljs.core.missing_protocol.call(null, "IStack.-peek", coll);
        }
      }
    }().call(null, coll)
  }
};
cljs.core._pop = function _pop(coll) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____2896 = coll;
    if(cljs.core.truth_(and__3546__auto____2896)) {
      return coll.cljs$core$IStack$_pop
    }else {
      return and__3546__auto____2896
    }
  }())) {
    return coll.cljs$core$IStack$_pop(coll)
  }else {
    return function() {
      var or__3548__auto____2897 = cljs.core._pop[goog.typeOf.call(null, coll)];
      if(cljs.core.truth_(or__3548__auto____2897)) {
        return or__3548__auto____2897
      }else {
        var or__3548__auto____2898 = cljs.core._pop["_"];
        if(cljs.core.truth_(or__3548__auto____2898)) {
          return or__3548__auto____2898
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
    var and__3546__auto____2899 = coll;
    if(cljs.core.truth_(and__3546__auto____2899)) {
      return coll.cljs$core$IVector$_assoc_n
    }else {
      return and__3546__auto____2899
    }
  }())) {
    return coll.cljs$core$IVector$_assoc_n(coll, n, val)
  }else {
    return function() {
      var or__3548__auto____2900 = cljs.core._assoc_n[goog.typeOf.call(null, coll)];
      if(cljs.core.truth_(or__3548__auto____2900)) {
        return or__3548__auto____2900
      }else {
        var or__3548__auto____2901 = cljs.core._assoc_n["_"];
        if(cljs.core.truth_(or__3548__auto____2901)) {
          return or__3548__auto____2901
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
    var and__3546__auto____2902 = o;
    if(cljs.core.truth_(and__3546__auto____2902)) {
      return o.cljs$core$IDeref$_deref
    }else {
      return and__3546__auto____2902
    }
  }())) {
    return o.cljs$core$IDeref$_deref(o)
  }else {
    return function() {
      var or__3548__auto____2903 = cljs.core._deref[goog.typeOf.call(null, o)];
      if(cljs.core.truth_(or__3548__auto____2903)) {
        return or__3548__auto____2903
      }else {
        var or__3548__auto____2904 = cljs.core._deref["_"];
        if(cljs.core.truth_(or__3548__auto____2904)) {
          return or__3548__auto____2904
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
    var and__3546__auto____2905 = o;
    if(cljs.core.truth_(and__3546__auto____2905)) {
      return o.cljs$core$IDerefWithTimeout$_deref_with_timeout
    }else {
      return and__3546__auto____2905
    }
  }())) {
    return o.cljs$core$IDerefWithTimeout$_deref_with_timeout(o, msec, timeout_val)
  }else {
    return function() {
      var or__3548__auto____2906 = cljs.core._deref_with_timeout[goog.typeOf.call(null, o)];
      if(cljs.core.truth_(or__3548__auto____2906)) {
        return or__3548__auto____2906
      }else {
        var or__3548__auto____2907 = cljs.core._deref_with_timeout["_"];
        if(cljs.core.truth_(or__3548__auto____2907)) {
          return or__3548__auto____2907
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
    var and__3546__auto____2908 = o;
    if(cljs.core.truth_(and__3546__auto____2908)) {
      return o.cljs$core$IMeta$_meta
    }else {
      return and__3546__auto____2908
    }
  }())) {
    return o.cljs$core$IMeta$_meta(o)
  }else {
    return function() {
      var or__3548__auto____2909 = cljs.core._meta[goog.typeOf.call(null, o)];
      if(cljs.core.truth_(or__3548__auto____2909)) {
        return or__3548__auto____2909
      }else {
        var or__3548__auto____2910 = cljs.core._meta["_"];
        if(cljs.core.truth_(or__3548__auto____2910)) {
          return or__3548__auto____2910
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
    var and__3546__auto____2911 = o;
    if(cljs.core.truth_(and__3546__auto____2911)) {
      return o.cljs$core$IWithMeta$_with_meta
    }else {
      return and__3546__auto____2911
    }
  }())) {
    return o.cljs$core$IWithMeta$_with_meta(o, meta)
  }else {
    return function() {
      var or__3548__auto____2912 = cljs.core._with_meta[goog.typeOf.call(null, o)];
      if(cljs.core.truth_(or__3548__auto____2912)) {
        return or__3548__auto____2912
      }else {
        var or__3548__auto____2913 = cljs.core._with_meta["_"];
        if(cljs.core.truth_(or__3548__auto____2913)) {
          return or__3548__auto____2913
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
  var _reduce__2920 = function(coll, f) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2914 = coll;
      if(cljs.core.truth_(and__3546__auto____2914)) {
        return coll.cljs$core$IReduce$_reduce
      }else {
        return and__3546__auto____2914
      }
    }())) {
      return coll.cljs$core$IReduce$_reduce(coll, f)
    }else {
      return function() {
        var or__3548__auto____2915 = cljs.core._reduce[goog.typeOf.call(null, coll)];
        if(cljs.core.truth_(or__3548__auto____2915)) {
          return or__3548__auto____2915
        }else {
          var or__3548__auto____2916 = cljs.core._reduce["_"];
          if(cljs.core.truth_(or__3548__auto____2916)) {
            return or__3548__auto____2916
          }else {
            throw cljs.core.missing_protocol.call(null, "IReduce.-reduce", coll);
          }
        }
      }().call(null, coll, f)
    }
  };
  var _reduce__2921 = function(coll, f, start) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____2917 = coll;
      if(cljs.core.truth_(and__3546__auto____2917)) {
        return coll.cljs$core$IReduce$_reduce
      }else {
        return and__3546__auto____2917
      }
    }())) {
      return coll.cljs$core$IReduce$_reduce(coll, f, start)
    }else {
      return function() {
        var or__3548__auto____2918 = cljs.core._reduce[goog.typeOf.call(null, coll)];
        if(cljs.core.truth_(or__3548__auto____2918)) {
          return or__3548__auto____2918
        }else {
          var or__3548__auto____2919 = cljs.core._reduce["_"];
          if(cljs.core.truth_(or__3548__auto____2919)) {
            return or__3548__auto____2919
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
        return _reduce__2920.call(this, coll, f);
      case 3:
        return _reduce__2921.call(this, coll, f, start)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return _reduce
}();
cljs.core.IEquiv = {};
cljs.core._equiv = function _equiv(o, other) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____2923 = o;
    if(cljs.core.truth_(and__3546__auto____2923)) {
      return o.cljs$core$IEquiv$_equiv
    }else {
      return and__3546__auto____2923
    }
  }())) {
    return o.cljs$core$IEquiv$_equiv(o, other)
  }else {
    return function() {
      var or__3548__auto____2924 = cljs.core._equiv[goog.typeOf.call(null, o)];
      if(cljs.core.truth_(or__3548__auto____2924)) {
        return or__3548__auto____2924
      }else {
        var or__3548__auto____2925 = cljs.core._equiv["_"];
        if(cljs.core.truth_(or__3548__auto____2925)) {
          return or__3548__auto____2925
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
    var and__3546__auto____2926 = o;
    if(cljs.core.truth_(and__3546__auto____2926)) {
      return o.cljs$core$IHash$_hash
    }else {
      return and__3546__auto____2926
    }
  }())) {
    return o.cljs$core$IHash$_hash(o)
  }else {
    return function() {
      var or__3548__auto____2927 = cljs.core._hash[goog.typeOf.call(null, o)];
      if(cljs.core.truth_(or__3548__auto____2927)) {
        return or__3548__auto____2927
      }else {
        var or__3548__auto____2928 = cljs.core._hash["_"];
        if(cljs.core.truth_(or__3548__auto____2928)) {
          return or__3548__auto____2928
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
    var and__3546__auto____2929 = o;
    if(cljs.core.truth_(and__3546__auto____2929)) {
      return o.cljs$core$ISeqable$_seq
    }else {
      return and__3546__auto____2929
    }
  }())) {
    return o.cljs$core$ISeqable$_seq(o)
  }else {
    return function() {
      var or__3548__auto____2930 = cljs.core._seq[goog.typeOf.call(null, o)];
      if(cljs.core.truth_(or__3548__auto____2930)) {
        return or__3548__auto____2930
      }else {
        var or__3548__auto____2931 = cljs.core._seq["_"];
        if(cljs.core.truth_(or__3548__auto____2931)) {
          return or__3548__auto____2931
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
    var and__3546__auto____2932 = o;
    if(cljs.core.truth_(and__3546__auto____2932)) {
      return o.cljs$core$IPrintable$_pr_seq
    }else {
      return and__3546__auto____2932
    }
  }())) {
    return o.cljs$core$IPrintable$_pr_seq(o, opts)
  }else {
    return function() {
      var or__3548__auto____2933 = cljs.core._pr_seq[goog.typeOf.call(null, o)];
      if(cljs.core.truth_(or__3548__auto____2933)) {
        return or__3548__auto____2933
      }else {
        var or__3548__auto____2934 = cljs.core._pr_seq["_"];
        if(cljs.core.truth_(or__3548__auto____2934)) {
          return or__3548__auto____2934
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
    var and__3546__auto____2935 = d;
    if(cljs.core.truth_(and__3546__auto____2935)) {
      return d.cljs$core$IPending$_realized_QMARK_
    }else {
      return and__3546__auto____2935
    }
  }())) {
    return d.cljs$core$IPending$_realized_QMARK_(d)
  }else {
    return function() {
      var or__3548__auto____2936 = cljs.core._realized_QMARK_[goog.typeOf.call(null, d)];
      if(cljs.core.truth_(or__3548__auto____2936)) {
        return or__3548__auto____2936
      }else {
        var or__3548__auto____2937 = cljs.core._realized_QMARK_["_"];
        if(cljs.core.truth_(or__3548__auto____2937)) {
          return or__3548__auto____2937
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
    var and__3546__auto____2938 = this$;
    if(cljs.core.truth_(and__3546__auto____2938)) {
      return this$.cljs$core$IWatchable$_notify_watches
    }else {
      return and__3546__auto____2938
    }
  }())) {
    return this$.cljs$core$IWatchable$_notify_watches(this$, oldval, newval)
  }else {
    return function() {
      var or__3548__auto____2939 = cljs.core._notify_watches[goog.typeOf.call(null, this$)];
      if(cljs.core.truth_(or__3548__auto____2939)) {
        return or__3548__auto____2939
      }else {
        var or__3548__auto____2940 = cljs.core._notify_watches["_"];
        if(cljs.core.truth_(or__3548__auto____2940)) {
          return or__3548__auto____2940
        }else {
          throw cljs.core.missing_protocol.call(null, "IWatchable.-notify-watches", this$);
        }
      }
    }().call(null, this$, oldval, newval)
  }
};
cljs.core._add_watch = function _add_watch(this$, key, f) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____2941 = this$;
    if(cljs.core.truth_(and__3546__auto____2941)) {
      return this$.cljs$core$IWatchable$_add_watch
    }else {
      return and__3546__auto____2941
    }
  }())) {
    return this$.cljs$core$IWatchable$_add_watch(this$, key, f)
  }else {
    return function() {
      var or__3548__auto____2942 = cljs.core._add_watch[goog.typeOf.call(null, this$)];
      if(cljs.core.truth_(or__3548__auto____2942)) {
        return or__3548__auto____2942
      }else {
        var or__3548__auto____2943 = cljs.core._add_watch["_"];
        if(cljs.core.truth_(or__3548__auto____2943)) {
          return or__3548__auto____2943
        }else {
          throw cljs.core.missing_protocol.call(null, "IWatchable.-add-watch", this$);
        }
      }
    }().call(null, this$, key, f)
  }
};
cljs.core._remove_watch = function _remove_watch(this$, key) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____2944 = this$;
    if(cljs.core.truth_(and__3546__auto____2944)) {
      return this$.cljs$core$IWatchable$_remove_watch
    }else {
      return and__3546__auto____2944
    }
  }())) {
    return this$.cljs$core$IWatchable$_remove_watch(this$, key)
  }else {
    return function() {
      var or__3548__auto____2945 = cljs.core._remove_watch[goog.typeOf.call(null, this$)];
      if(cljs.core.truth_(or__3548__auto____2945)) {
        return or__3548__auto____2945
      }else {
        var or__3548__auto____2946 = cljs.core._remove_watch["_"];
        if(cljs.core.truth_(or__3548__auto____2946)) {
          return or__3548__auto____2946
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
  var G__2947 = null;
  var G__2947__2948 = function(o, k) {
    return null
  };
  var G__2947__2949 = function(o, k, not_found) {
    return not_found
  };
  G__2947 = function(o, k, not_found) {
    switch(arguments.length) {
      case 2:
        return G__2947__2948.call(this, o, k);
      case 3:
        return G__2947__2949.call(this, o, k, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__2947
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
  var G__2951 = null;
  var G__2951__2952 = function(_, f) {
    return f.call(null)
  };
  var G__2951__2953 = function(_, f, start) {
    return start
  };
  G__2951 = function(_, f, start) {
    switch(arguments.length) {
      case 2:
        return G__2951__2952.call(this, _, f);
      case 3:
        return G__2951__2953.call(this, _, f, start)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__2951
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
  var G__2955 = null;
  var G__2955__2956 = function(_, n) {
    return null
  };
  var G__2955__2957 = function(_, n, not_found) {
    return not_found
  };
  G__2955 = function(_, n, not_found) {
    switch(arguments.length) {
      case 2:
        return G__2955__2956.call(this, _, n);
      case 3:
        return G__2955__2957.call(this, _, n, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__2955
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
  var ci_reduce__2965 = function(cicoll, f) {
    if(cljs.core.truth_(cljs.core._EQ_.call(null, 0, cljs.core._count.call(null, cicoll)))) {
      return f.call(null)
    }else {
      var val__2959 = cljs.core._nth.call(null, cicoll, 0);
      var n__2960 = 1;
      while(true) {
        if(cljs.core.truth_(n__2960 < cljs.core._count.call(null, cicoll))) {
          var G__2969 = f.call(null, val__2959, cljs.core._nth.call(null, cicoll, n__2960));
          var G__2970 = n__2960 + 1;
          val__2959 = G__2969;
          n__2960 = G__2970;
          continue
        }else {
          return val__2959
        }
        break
      }
    }
  };
  var ci_reduce__2966 = function(cicoll, f, val) {
    var val__2961 = val;
    var n__2962 = 0;
    while(true) {
      if(cljs.core.truth_(n__2962 < cljs.core._count.call(null, cicoll))) {
        var G__2971 = f.call(null, val__2961, cljs.core._nth.call(null, cicoll, n__2962));
        var G__2972 = n__2962 + 1;
        val__2961 = G__2971;
        n__2962 = G__2972;
        continue
      }else {
        return val__2961
      }
      break
    }
  };
  var ci_reduce__2967 = function(cicoll, f, val, idx) {
    var val__2963 = val;
    var n__2964 = idx;
    while(true) {
      if(cljs.core.truth_(n__2964 < cljs.core._count.call(null, cicoll))) {
        var G__2973 = f.call(null, val__2963, cljs.core._nth.call(null, cicoll, n__2964));
        var G__2974 = n__2964 + 1;
        val__2963 = G__2973;
        n__2964 = G__2974;
        continue
      }else {
        return val__2963
      }
      break
    }
  };
  ci_reduce = function(cicoll, f, val, idx) {
    switch(arguments.length) {
      case 2:
        return ci_reduce__2965.call(this, cicoll, f);
      case 3:
        return ci_reduce__2966.call(this, cicoll, f, val);
      case 4:
        return ci_reduce__2967.call(this, cicoll, f, val, idx)
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
  var this__2975 = this;
  return cljs.core.hash_coll.call(null, coll)
};
cljs.core.IndexedSeq.prototype.cljs$core$IReduce$ = true;
cljs.core.IndexedSeq.prototype.cljs$core$IReduce$_reduce = function() {
  var G__2988 = null;
  var G__2988__2989 = function(_, f) {
    var this__2976 = this;
    return cljs.core.ci_reduce.call(null, this__2976.a, f, this__2976.a[this__2976.i], this__2976.i + 1)
  };
  var G__2988__2990 = function(_, f, start) {
    var this__2977 = this;
    return cljs.core.ci_reduce.call(null, this__2977.a, f, start, this__2977.i)
  };
  G__2988 = function(_, f, start) {
    switch(arguments.length) {
      case 2:
        return G__2988__2989.call(this, _, f);
      case 3:
        return G__2988__2990.call(this, _, f, start)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__2988
}();
cljs.core.IndexedSeq.prototype.cljs$core$ICollection$ = true;
cljs.core.IndexedSeq.prototype.cljs$core$ICollection$_conj = function(coll, o) {
  var this__2978 = this;
  return cljs.core.cons.call(null, o, coll)
};
cljs.core.IndexedSeq.prototype.cljs$core$IEquiv$ = true;
cljs.core.IndexedSeq.prototype.cljs$core$IEquiv$_equiv = function(coll, other) {
  var this__2979 = this;
  return cljs.core.equiv_sequential.call(null, coll, other)
};
cljs.core.IndexedSeq.prototype.cljs$core$ISequential$ = true;
cljs.core.IndexedSeq.prototype.cljs$core$IIndexed$ = true;
cljs.core.IndexedSeq.prototype.cljs$core$IIndexed$_nth = function() {
  var G__2992 = null;
  var G__2992__2993 = function(coll, n) {
    var this__2980 = this;
    var i__2981 = n + this__2980.i;
    if(cljs.core.truth_(i__2981 < this__2980.a.length)) {
      return this__2980.a[i__2981]
    }else {
      return null
    }
  };
  var G__2992__2994 = function(coll, n, not_found) {
    var this__2982 = this;
    var i__2983 = n + this__2982.i;
    if(cljs.core.truth_(i__2983 < this__2982.a.length)) {
      return this__2982.a[i__2983]
    }else {
      return not_found
    }
  };
  G__2992 = function(coll, n, not_found) {
    switch(arguments.length) {
      case 2:
        return G__2992__2993.call(this, coll, n);
      case 3:
        return G__2992__2994.call(this, coll, n, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__2992
}();
cljs.core.IndexedSeq.prototype.cljs$core$ICounted$ = true;
cljs.core.IndexedSeq.prototype.cljs$core$ICounted$_count = function(_) {
  var this__2984 = this;
  return this__2984.a.length - this__2984.i
};
cljs.core.IndexedSeq.prototype.cljs$core$ISeq$ = true;
cljs.core.IndexedSeq.prototype.cljs$core$ISeq$_first = function(_) {
  var this__2985 = this;
  return this__2985.a[this__2985.i]
};
cljs.core.IndexedSeq.prototype.cljs$core$ISeq$_rest = function(_) {
  var this__2986 = this;
  if(cljs.core.truth_(this__2986.i + 1 < this__2986.a.length)) {
    return new cljs.core.IndexedSeq(this__2986.a, this__2986.i + 1)
  }else {
    return cljs.core.list.call(null)
  }
};
cljs.core.IndexedSeq.prototype.cljs$core$ISeqable$ = true;
cljs.core.IndexedSeq.prototype.cljs$core$ISeqable$_seq = function(this$) {
  var this__2987 = this;
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
  var G__2996 = null;
  var G__2996__2997 = function(array, f) {
    return cljs.core.ci_reduce.call(null, array, f)
  };
  var G__2996__2998 = function(array, f, start) {
    return cljs.core.ci_reduce.call(null, array, f, start)
  };
  G__2996 = function(array, f, start) {
    switch(arguments.length) {
      case 2:
        return G__2996__2997.call(this, array, f);
      case 3:
        return G__2996__2998.call(this, array, f, start)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__2996
}();
cljs.core.ILookup["array"] = true;
cljs.core._lookup["array"] = function() {
  var G__3000 = null;
  var G__3000__3001 = function(array, k) {
    return array[k]
  };
  var G__3000__3002 = function(array, k, not_found) {
    return cljs.core._nth.call(null, array, k, not_found)
  };
  G__3000 = function(array, k, not_found) {
    switch(arguments.length) {
      case 2:
        return G__3000__3001.call(this, array, k);
      case 3:
        return G__3000__3002.call(this, array, k, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3000
}();
cljs.core.IIndexed["array"] = true;
cljs.core._nth["array"] = function() {
  var G__3004 = null;
  var G__3004__3005 = function(array, n) {
    if(cljs.core.truth_(n < array.length)) {
      return array[n]
    }else {
      return null
    }
  };
  var G__3004__3006 = function(array, n, not_found) {
    if(cljs.core.truth_(n < array.length)) {
      return array[n]
    }else {
      return not_found
    }
  };
  G__3004 = function(array, n, not_found) {
    switch(arguments.length) {
      case 2:
        return G__3004__3005.call(this, array, n);
      case 3:
        return G__3004__3006.call(this, array, n, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3004
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
  var temp__3698__auto____3008 = cljs.core.seq.call(null, coll);
  if(cljs.core.truth_(temp__3698__auto____3008)) {
    var s__3009 = temp__3698__auto____3008;
    return cljs.core._first.call(null, s__3009)
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
      var G__3010 = cljs.core.next.call(null, s);
      s = G__3010;
      continue
    }else {
      return cljs.core.first.call(null, s)
    }
    break
  }
};
cljs.core.ICounted["_"] = true;
cljs.core._count["_"] = function(x) {
  var s__3011 = cljs.core.seq.call(null, x);
  var n__3012 = 0;
  while(true) {
    if(cljs.core.truth_(s__3011)) {
      var G__3013 = cljs.core.next.call(null, s__3011);
      var G__3014 = n__3012 + 1;
      s__3011 = G__3013;
      n__3012 = G__3014;
      continue
    }else {
      return n__3012
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
  var conj__3015 = function(coll, x) {
    return cljs.core._conj.call(null, coll, x)
  };
  var conj__3016 = function() {
    var G__3018__delegate = function(coll, x, xs) {
      while(true) {
        if(cljs.core.truth_(xs)) {
          var G__3019 = conj.call(null, coll, x);
          var G__3020 = cljs.core.first.call(null, xs);
          var G__3021 = cljs.core.next.call(null, xs);
          coll = G__3019;
          x = G__3020;
          xs = G__3021;
          continue
        }else {
          return conj.call(null, coll, x)
        }
        break
      }
    };
    var G__3018 = function(coll, x, var_args) {
      var xs = null;
      if(goog.isDef(var_args)) {
        xs = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3018__delegate.call(this, coll, x, xs)
    };
    G__3018.cljs$lang$maxFixedArity = 2;
    G__3018.cljs$lang$applyTo = function(arglist__3022) {
      var coll = cljs.core.first(arglist__3022);
      var x = cljs.core.first(cljs.core.next(arglist__3022));
      var xs = cljs.core.rest(cljs.core.next(arglist__3022));
      return G__3018__delegate.call(this, coll, x, xs)
    };
    return G__3018
  }();
  conj = function(coll, x, var_args) {
    var xs = var_args;
    switch(arguments.length) {
      case 2:
        return conj__3015.call(this, coll, x);
      default:
        return conj__3016.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  conj.cljs$lang$maxFixedArity = 2;
  conj.cljs$lang$applyTo = conj__3016.cljs$lang$applyTo;
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
  var nth__3023 = function(coll, n) {
    return cljs.core._nth.call(null, coll, Math.floor(n))
  };
  var nth__3024 = function(coll, n, not_found) {
    return cljs.core._nth.call(null, coll, Math.floor(n), not_found)
  };
  nth = function(coll, n, not_found) {
    switch(arguments.length) {
      case 2:
        return nth__3023.call(this, coll, n);
      case 3:
        return nth__3024.call(this, coll, n, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return nth
}();
cljs.core.get = function() {
  var get = null;
  var get__3026 = function(o, k) {
    return cljs.core._lookup.call(null, o, k)
  };
  var get__3027 = function(o, k, not_found) {
    return cljs.core._lookup.call(null, o, k, not_found)
  };
  get = function(o, k, not_found) {
    switch(arguments.length) {
      case 2:
        return get__3026.call(this, o, k);
      case 3:
        return get__3027.call(this, o, k, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return get
}();
cljs.core.assoc = function() {
  var assoc = null;
  var assoc__3030 = function(coll, k, v) {
    return cljs.core._assoc.call(null, coll, k, v)
  };
  var assoc__3031 = function() {
    var G__3033__delegate = function(coll, k, v, kvs) {
      while(true) {
        var ret__3029 = assoc.call(null, coll, k, v);
        if(cljs.core.truth_(kvs)) {
          var G__3034 = ret__3029;
          var G__3035 = cljs.core.first.call(null, kvs);
          var G__3036 = cljs.core.second.call(null, kvs);
          var G__3037 = cljs.core.nnext.call(null, kvs);
          coll = G__3034;
          k = G__3035;
          v = G__3036;
          kvs = G__3037;
          continue
        }else {
          return ret__3029
        }
        break
      }
    };
    var G__3033 = function(coll, k, v, var_args) {
      var kvs = null;
      if(goog.isDef(var_args)) {
        kvs = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
      }
      return G__3033__delegate.call(this, coll, k, v, kvs)
    };
    G__3033.cljs$lang$maxFixedArity = 3;
    G__3033.cljs$lang$applyTo = function(arglist__3038) {
      var coll = cljs.core.first(arglist__3038);
      var k = cljs.core.first(cljs.core.next(arglist__3038));
      var v = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3038)));
      var kvs = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3038)));
      return G__3033__delegate.call(this, coll, k, v, kvs)
    };
    return G__3033
  }();
  assoc = function(coll, k, v, var_args) {
    var kvs = var_args;
    switch(arguments.length) {
      case 3:
        return assoc__3030.call(this, coll, k, v);
      default:
        return assoc__3031.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  assoc.cljs$lang$maxFixedArity = 3;
  assoc.cljs$lang$applyTo = assoc__3031.cljs$lang$applyTo;
  return assoc
}();
cljs.core.dissoc = function() {
  var dissoc = null;
  var dissoc__3040 = function(coll) {
    return coll
  };
  var dissoc__3041 = function(coll, k) {
    return cljs.core._dissoc.call(null, coll, k)
  };
  var dissoc__3042 = function() {
    var G__3044__delegate = function(coll, k, ks) {
      while(true) {
        var ret__3039 = dissoc.call(null, coll, k);
        if(cljs.core.truth_(ks)) {
          var G__3045 = ret__3039;
          var G__3046 = cljs.core.first.call(null, ks);
          var G__3047 = cljs.core.next.call(null, ks);
          coll = G__3045;
          k = G__3046;
          ks = G__3047;
          continue
        }else {
          return ret__3039
        }
        break
      }
    };
    var G__3044 = function(coll, k, var_args) {
      var ks = null;
      if(goog.isDef(var_args)) {
        ks = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3044__delegate.call(this, coll, k, ks)
    };
    G__3044.cljs$lang$maxFixedArity = 2;
    G__3044.cljs$lang$applyTo = function(arglist__3048) {
      var coll = cljs.core.first(arglist__3048);
      var k = cljs.core.first(cljs.core.next(arglist__3048));
      var ks = cljs.core.rest(cljs.core.next(arglist__3048));
      return G__3044__delegate.call(this, coll, k, ks)
    };
    return G__3044
  }();
  dissoc = function(coll, k, var_args) {
    var ks = var_args;
    switch(arguments.length) {
      case 1:
        return dissoc__3040.call(this, coll);
      case 2:
        return dissoc__3041.call(this, coll, k);
      default:
        return dissoc__3042.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  dissoc.cljs$lang$maxFixedArity = 2;
  dissoc.cljs$lang$applyTo = dissoc__3042.cljs$lang$applyTo;
  return dissoc
}();
cljs.core.with_meta = function with_meta(o, meta) {
  return cljs.core._with_meta.call(null, o, meta)
};
cljs.core.meta = function meta(o) {
  if(cljs.core.truth_(function() {
    var x__352__auto____3049 = o;
    if(cljs.core.truth_(function() {
      var and__3546__auto____3050 = x__352__auto____3049;
      if(cljs.core.truth_(and__3546__auto____3050)) {
        var and__3546__auto____3051 = x__352__auto____3049.cljs$core$IMeta$;
        if(cljs.core.truth_(and__3546__auto____3051)) {
          return cljs.core.not.call(null, x__352__auto____3049.hasOwnProperty("cljs$core$IMeta$"))
        }else {
          return and__3546__auto____3051
        }
      }else {
        return and__3546__auto____3050
      }
    }())) {
      return true
    }else {
      return cljs.core.type_satisfies_.call(null, cljs.core.IMeta, x__352__auto____3049)
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
  var disj__3053 = function(coll) {
    return coll
  };
  var disj__3054 = function(coll, k) {
    return cljs.core._disjoin.call(null, coll, k)
  };
  var disj__3055 = function() {
    var G__3057__delegate = function(coll, k, ks) {
      while(true) {
        var ret__3052 = disj.call(null, coll, k);
        if(cljs.core.truth_(ks)) {
          var G__3058 = ret__3052;
          var G__3059 = cljs.core.first.call(null, ks);
          var G__3060 = cljs.core.next.call(null, ks);
          coll = G__3058;
          k = G__3059;
          ks = G__3060;
          continue
        }else {
          return ret__3052
        }
        break
      }
    };
    var G__3057 = function(coll, k, var_args) {
      var ks = null;
      if(goog.isDef(var_args)) {
        ks = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3057__delegate.call(this, coll, k, ks)
    };
    G__3057.cljs$lang$maxFixedArity = 2;
    G__3057.cljs$lang$applyTo = function(arglist__3061) {
      var coll = cljs.core.first(arglist__3061);
      var k = cljs.core.first(cljs.core.next(arglist__3061));
      var ks = cljs.core.rest(cljs.core.next(arglist__3061));
      return G__3057__delegate.call(this, coll, k, ks)
    };
    return G__3057
  }();
  disj = function(coll, k, var_args) {
    var ks = var_args;
    switch(arguments.length) {
      case 1:
        return disj__3053.call(this, coll);
      case 2:
        return disj__3054.call(this, coll, k);
      default:
        return disj__3055.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  disj.cljs$lang$maxFixedArity = 2;
  disj.cljs$lang$applyTo = disj__3055.cljs$lang$applyTo;
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
    var x__352__auto____3062 = x;
    if(cljs.core.truth_(function() {
      var and__3546__auto____3063 = x__352__auto____3062;
      if(cljs.core.truth_(and__3546__auto____3063)) {
        var and__3546__auto____3064 = x__352__auto____3062.cljs$core$ICollection$;
        if(cljs.core.truth_(and__3546__auto____3064)) {
          return cljs.core.not.call(null, x__352__auto____3062.hasOwnProperty("cljs$core$ICollection$"))
        }else {
          return and__3546__auto____3064
        }
      }else {
        return and__3546__auto____3063
      }
    }())) {
      return true
    }else {
      return cljs.core.type_satisfies_.call(null, cljs.core.ICollection, x__352__auto____3062)
    }
  }
};
cljs.core.set_QMARK_ = function set_QMARK_(x) {
  if(cljs.core.truth_(x === null)) {
    return false
  }else {
    var x__352__auto____3065 = x;
    if(cljs.core.truth_(function() {
      var and__3546__auto____3066 = x__352__auto____3065;
      if(cljs.core.truth_(and__3546__auto____3066)) {
        var and__3546__auto____3067 = x__352__auto____3065.cljs$core$ISet$;
        if(cljs.core.truth_(and__3546__auto____3067)) {
          return cljs.core.not.call(null, x__352__auto____3065.hasOwnProperty("cljs$core$ISet$"))
        }else {
          return and__3546__auto____3067
        }
      }else {
        return and__3546__auto____3066
      }
    }())) {
      return true
    }else {
      return cljs.core.type_satisfies_.call(null, cljs.core.ISet, x__352__auto____3065)
    }
  }
};
cljs.core.associative_QMARK_ = function associative_QMARK_(x) {
  var x__352__auto____3068 = x;
  if(cljs.core.truth_(function() {
    var and__3546__auto____3069 = x__352__auto____3068;
    if(cljs.core.truth_(and__3546__auto____3069)) {
      var and__3546__auto____3070 = x__352__auto____3068.cljs$core$IAssociative$;
      if(cljs.core.truth_(and__3546__auto____3070)) {
        return cljs.core.not.call(null, x__352__auto____3068.hasOwnProperty("cljs$core$IAssociative$"))
      }else {
        return and__3546__auto____3070
      }
    }else {
      return and__3546__auto____3069
    }
  }())) {
    return true
  }else {
    return cljs.core.type_satisfies_.call(null, cljs.core.IAssociative, x__352__auto____3068)
  }
};
cljs.core.sequential_QMARK_ = function sequential_QMARK_(x) {
  var x__352__auto____3071 = x;
  if(cljs.core.truth_(function() {
    var and__3546__auto____3072 = x__352__auto____3071;
    if(cljs.core.truth_(and__3546__auto____3072)) {
      var and__3546__auto____3073 = x__352__auto____3071.cljs$core$ISequential$;
      if(cljs.core.truth_(and__3546__auto____3073)) {
        return cljs.core.not.call(null, x__352__auto____3071.hasOwnProperty("cljs$core$ISequential$"))
      }else {
        return and__3546__auto____3073
      }
    }else {
      return and__3546__auto____3072
    }
  }())) {
    return true
  }else {
    return cljs.core.type_satisfies_.call(null, cljs.core.ISequential, x__352__auto____3071)
  }
};
cljs.core.counted_QMARK_ = function counted_QMARK_(x) {
  var x__352__auto____3074 = x;
  if(cljs.core.truth_(function() {
    var and__3546__auto____3075 = x__352__auto____3074;
    if(cljs.core.truth_(and__3546__auto____3075)) {
      var and__3546__auto____3076 = x__352__auto____3074.cljs$core$ICounted$;
      if(cljs.core.truth_(and__3546__auto____3076)) {
        return cljs.core.not.call(null, x__352__auto____3074.hasOwnProperty("cljs$core$ICounted$"))
      }else {
        return and__3546__auto____3076
      }
    }else {
      return and__3546__auto____3075
    }
  }())) {
    return true
  }else {
    return cljs.core.type_satisfies_.call(null, cljs.core.ICounted, x__352__auto____3074)
  }
};
cljs.core.map_QMARK_ = function map_QMARK_(x) {
  if(cljs.core.truth_(x === null)) {
    return false
  }else {
    var x__352__auto____3077 = x;
    if(cljs.core.truth_(function() {
      var and__3546__auto____3078 = x__352__auto____3077;
      if(cljs.core.truth_(and__3546__auto____3078)) {
        var and__3546__auto____3079 = x__352__auto____3077.cljs$core$IMap$;
        if(cljs.core.truth_(and__3546__auto____3079)) {
          return cljs.core.not.call(null, x__352__auto____3077.hasOwnProperty("cljs$core$IMap$"))
        }else {
          return and__3546__auto____3079
        }
      }else {
        return and__3546__auto____3078
      }
    }())) {
      return true
    }else {
      return cljs.core.type_satisfies_.call(null, cljs.core.IMap, x__352__auto____3077)
    }
  }
};
cljs.core.vector_QMARK_ = function vector_QMARK_(x) {
  var x__352__auto____3080 = x;
  if(cljs.core.truth_(function() {
    var and__3546__auto____3081 = x__352__auto____3080;
    if(cljs.core.truth_(and__3546__auto____3081)) {
      var and__3546__auto____3082 = x__352__auto____3080.cljs$core$IVector$;
      if(cljs.core.truth_(and__3546__auto____3082)) {
        return cljs.core.not.call(null, x__352__auto____3080.hasOwnProperty("cljs$core$IVector$"))
      }else {
        return and__3546__auto____3082
      }
    }else {
      return and__3546__auto____3081
    }
  }())) {
    return true
  }else {
    return cljs.core.type_satisfies_.call(null, cljs.core.IVector, x__352__auto____3080)
  }
};
cljs.core.js_obj = function js_obj() {
  return{}
};
cljs.core.js_keys = function js_keys(obj) {
  var keys__3083 = cljs.core.array.call(null);
  goog.object.forEach.call(null, obj, function(val, key, obj) {
    return keys__3083.push(key)
  });
  return keys__3083
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
    var x__352__auto____3084 = s;
    if(cljs.core.truth_(function() {
      var and__3546__auto____3085 = x__352__auto____3084;
      if(cljs.core.truth_(and__3546__auto____3085)) {
        var and__3546__auto____3086 = x__352__auto____3084.cljs$core$ISeq$;
        if(cljs.core.truth_(and__3546__auto____3086)) {
          return cljs.core.not.call(null, x__352__auto____3084.hasOwnProperty("cljs$core$ISeq$"))
        }else {
          return and__3546__auto____3086
        }
      }else {
        return and__3546__auto____3085
      }
    }())) {
      return true
    }else {
      return cljs.core.type_satisfies_.call(null, cljs.core.ISeq, x__352__auto____3084)
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
  var and__3546__auto____3087 = goog.isString.call(null, x);
  if(cljs.core.truth_(and__3546__auto____3087)) {
    return cljs.core.not.call(null, function() {
      var or__3548__auto____3088 = cljs.core._EQ_.call(null, x.charAt(0), "\ufdd0");
      if(cljs.core.truth_(or__3548__auto____3088)) {
        return or__3548__auto____3088
      }else {
        return cljs.core._EQ_.call(null, x.charAt(0), "\ufdd1")
      }
    }())
  }else {
    return and__3546__auto____3087
  }
};
cljs.core.keyword_QMARK_ = function keyword_QMARK_(x) {
  var and__3546__auto____3089 = goog.isString.call(null, x);
  if(cljs.core.truth_(and__3546__auto____3089)) {
    return cljs.core._EQ_.call(null, x.charAt(0), "\ufdd0")
  }else {
    return and__3546__auto____3089
  }
};
cljs.core.symbol_QMARK_ = function symbol_QMARK_(x) {
  var and__3546__auto____3090 = goog.isString.call(null, x);
  if(cljs.core.truth_(and__3546__auto____3090)) {
    return cljs.core._EQ_.call(null, x.charAt(0), "\ufdd1")
  }else {
    return and__3546__auto____3090
  }
};
cljs.core.number_QMARK_ = function number_QMARK_(n) {
  return goog.isNumber.call(null, n)
};
cljs.core.fn_QMARK_ = function fn_QMARK_(f) {
  return goog.isFunction.call(null, f)
};
cljs.core.integer_QMARK_ = function integer_QMARK_(n) {
  var and__3546__auto____3091 = cljs.core.number_QMARK_.call(null, n);
  if(cljs.core.truth_(and__3546__auto____3091)) {
    return n == n.toFixed()
  }else {
    return and__3546__auto____3091
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
    var and__3546__auto____3092 = coll;
    if(cljs.core.truth_(and__3546__auto____3092)) {
      var and__3546__auto____3093 = cljs.core.associative_QMARK_.call(null, coll);
      if(cljs.core.truth_(and__3546__auto____3093)) {
        return cljs.core.contains_QMARK_.call(null, coll, k)
      }else {
        return and__3546__auto____3093
      }
    }else {
      return and__3546__auto____3092
    }
  }())) {
    return cljs.core.Vector.fromArray([k, cljs.core._lookup.call(null, coll, k)])
  }else {
    return null
  }
};
cljs.core.distinct_QMARK_ = function() {
  var distinct_QMARK_ = null;
  var distinct_QMARK___3098 = function(x) {
    return true
  };
  var distinct_QMARK___3099 = function(x, y) {
    return cljs.core.not.call(null, cljs.core._EQ_.call(null, x, y))
  };
  var distinct_QMARK___3100 = function() {
    var G__3102__delegate = function(x, y, more) {
      if(cljs.core.truth_(cljs.core.not.call(null, cljs.core._EQ_.call(null, x, y)))) {
        var s__3094 = cljs.core.set([y, x]);
        var xs__3095 = more;
        while(true) {
          var x__3096 = cljs.core.first.call(null, xs__3095);
          var etc__3097 = cljs.core.next.call(null, xs__3095);
          if(cljs.core.truth_(xs__3095)) {
            if(cljs.core.truth_(cljs.core.contains_QMARK_.call(null, s__3094, x__3096))) {
              return false
            }else {
              var G__3103 = cljs.core.conj.call(null, s__3094, x__3096);
              var G__3104 = etc__3097;
              s__3094 = G__3103;
              xs__3095 = G__3104;
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
    var G__3102 = function(x, y, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3102__delegate.call(this, x, y, more)
    };
    G__3102.cljs$lang$maxFixedArity = 2;
    G__3102.cljs$lang$applyTo = function(arglist__3105) {
      var x = cljs.core.first(arglist__3105);
      var y = cljs.core.first(cljs.core.next(arglist__3105));
      var more = cljs.core.rest(cljs.core.next(arglist__3105));
      return G__3102__delegate.call(this, x, y, more)
    };
    return G__3102
  }();
  distinct_QMARK_ = function(x, y, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 1:
        return distinct_QMARK___3098.call(this, x);
      case 2:
        return distinct_QMARK___3099.call(this, x, y);
      default:
        return distinct_QMARK___3100.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  distinct_QMARK_.cljs$lang$maxFixedArity = 2;
  distinct_QMARK_.cljs$lang$applyTo = distinct_QMARK___3100.cljs$lang$applyTo;
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
      var r__3106 = f.call(null, x, y);
      if(cljs.core.truth_(cljs.core.number_QMARK_.call(null, r__3106))) {
        return r__3106
      }else {
        if(cljs.core.truth_(r__3106)) {
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
  var sort__3108 = function(coll) {
    return sort.call(null, cljs.core.compare, coll)
  };
  var sort__3109 = function(comp, coll) {
    if(cljs.core.truth_(cljs.core.seq.call(null, coll))) {
      var a__3107 = cljs.core.to_array.call(null, coll);
      goog.array.stableSort.call(null, a__3107, cljs.core.fn__GT_comparator.call(null, comp));
      return cljs.core.seq.call(null, a__3107)
    }else {
      return cljs.core.List.EMPTY
    }
  };
  sort = function(comp, coll) {
    switch(arguments.length) {
      case 1:
        return sort__3108.call(this, comp);
      case 2:
        return sort__3109.call(this, comp, coll)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return sort
}();
cljs.core.sort_by = function() {
  var sort_by = null;
  var sort_by__3111 = function(keyfn, coll) {
    return sort_by.call(null, keyfn, cljs.core.compare, coll)
  };
  var sort_by__3112 = function(keyfn, comp, coll) {
    return cljs.core.sort.call(null, function(x, y) {
      return cljs.core.fn__GT_comparator.call(null, comp).call(null, keyfn.call(null, x), keyfn.call(null, y))
    }, coll)
  };
  sort_by = function(keyfn, comp, coll) {
    switch(arguments.length) {
      case 2:
        return sort_by__3111.call(this, keyfn, comp);
      case 3:
        return sort_by__3112.call(this, keyfn, comp, coll)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return sort_by
}();
cljs.core.reduce = function() {
  var reduce = null;
  var reduce__3114 = function(f, coll) {
    return cljs.core._reduce.call(null, coll, f)
  };
  var reduce__3115 = function(f, val, coll) {
    return cljs.core._reduce.call(null, coll, f, val)
  };
  reduce = function(f, val, coll) {
    switch(arguments.length) {
      case 2:
        return reduce__3114.call(this, f, val);
      case 3:
        return reduce__3115.call(this, f, val, coll)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return reduce
}();
cljs.core.seq_reduce = function() {
  var seq_reduce = null;
  var seq_reduce__3121 = function(f, coll) {
    var temp__3695__auto____3117 = cljs.core.seq.call(null, coll);
    if(cljs.core.truth_(temp__3695__auto____3117)) {
      var s__3118 = temp__3695__auto____3117;
      return cljs.core.reduce.call(null, f, cljs.core.first.call(null, s__3118), cljs.core.next.call(null, s__3118))
    }else {
      return f.call(null)
    }
  };
  var seq_reduce__3122 = function(f, val, coll) {
    var val__3119 = val;
    var coll__3120 = cljs.core.seq.call(null, coll);
    while(true) {
      if(cljs.core.truth_(coll__3120)) {
        var G__3124 = f.call(null, val__3119, cljs.core.first.call(null, coll__3120));
        var G__3125 = cljs.core.next.call(null, coll__3120);
        val__3119 = G__3124;
        coll__3120 = G__3125;
        continue
      }else {
        return val__3119
      }
      break
    }
  };
  seq_reduce = function(f, val, coll) {
    switch(arguments.length) {
      case 2:
        return seq_reduce__3121.call(this, f, val);
      case 3:
        return seq_reduce__3122.call(this, f, val, coll)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return seq_reduce
}();
cljs.core.IReduce["_"] = true;
cljs.core._reduce["_"] = function() {
  var G__3126 = null;
  var G__3126__3127 = function(coll, f) {
    return cljs.core.seq_reduce.call(null, f, coll)
  };
  var G__3126__3128 = function(coll, f, start) {
    return cljs.core.seq_reduce.call(null, f, start, coll)
  };
  G__3126 = function(coll, f, start) {
    switch(arguments.length) {
      case 2:
        return G__3126__3127.call(this, coll, f);
      case 3:
        return G__3126__3128.call(this, coll, f, start)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3126
}();
cljs.core._PLUS_ = function() {
  var _PLUS_ = null;
  var _PLUS___3130 = function() {
    return 0
  };
  var _PLUS___3131 = function(x) {
    return x
  };
  var _PLUS___3132 = function(x, y) {
    return x + y
  };
  var _PLUS___3133 = function() {
    var G__3135__delegate = function(x, y, more) {
      return cljs.core.reduce.call(null, _PLUS_, x + y, more)
    };
    var G__3135 = function(x, y, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3135__delegate.call(this, x, y, more)
    };
    G__3135.cljs$lang$maxFixedArity = 2;
    G__3135.cljs$lang$applyTo = function(arglist__3136) {
      var x = cljs.core.first(arglist__3136);
      var y = cljs.core.first(cljs.core.next(arglist__3136));
      var more = cljs.core.rest(cljs.core.next(arglist__3136));
      return G__3135__delegate.call(this, x, y, more)
    };
    return G__3135
  }();
  _PLUS_ = function(x, y, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 0:
        return _PLUS___3130.call(this);
      case 1:
        return _PLUS___3131.call(this, x);
      case 2:
        return _PLUS___3132.call(this, x, y);
      default:
        return _PLUS___3133.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  _PLUS_.cljs$lang$maxFixedArity = 2;
  _PLUS_.cljs$lang$applyTo = _PLUS___3133.cljs$lang$applyTo;
  return _PLUS_
}();
cljs.core._ = function() {
  var _ = null;
  var ___3137 = function(x) {
    return-x
  };
  var ___3138 = function(x, y) {
    return x - y
  };
  var ___3139 = function() {
    var G__3141__delegate = function(x, y, more) {
      return cljs.core.reduce.call(null, _, x - y, more)
    };
    var G__3141 = function(x, y, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3141__delegate.call(this, x, y, more)
    };
    G__3141.cljs$lang$maxFixedArity = 2;
    G__3141.cljs$lang$applyTo = function(arglist__3142) {
      var x = cljs.core.first(arglist__3142);
      var y = cljs.core.first(cljs.core.next(arglist__3142));
      var more = cljs.core.rest(cljs.core.next(arglist__3142));
      return G__3141__delegate.call(this, x, y, more)
    };
    return G__3141
  }();
  _ = function(x, y, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 1:
        return ___3137.call(this, x);
      case 2:
        return ___3138.call(this, x, y);
      default:
        return ___3139.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  _.cljs$lang$maxFixedArity = 2;
  _.cljs$lang$applyTo = ___3139.cljs$lang$applyTo;
  return _
}();
cljs.core._STAR_ = function() {
  var _STAR_ = null;
  var _STAR___3143 = function() {
    return 1
  };
  var _STAR___3144 = function(x) {
    return x
  };
  var _STAR___3145 = function(x, y) {
    return x * y
  };
  var _STAR___3146 = function() {
    var G__3148__delegate = function(x, y, more) {
      return cljs.core.reduce.call(null, _STAR_, x * y, more)
    };
    var G__3148 = function(x, y, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3148__delegate.call(this, x, y, more)
    };
    G__3148.cljs$lang$maxFixedArity = 2;
    G__3148.cljs$lang$applyTo = function(arglist__3149) {
      var x = cljs.core.first(arglist__3149);
      var y = cljs.core.first(cljs.core.next(arglist__3149));
      var more = cljs.core.rest(cljs.core.next(arglist__3149));
      return G__3148__delegate.call(this, x, y, more)
    };
    return G__3148
  }();
  _STAR_ = function(x, y, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 0:
        return _STAR___3143.call(this);
      case 1:
        return _STAR___3144.call(this, x);
      case 2:
        return _STAR___3145.call(this, x, y);
      default:
        return _STAR___3146.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  _STAR_.cljs$lang$maxFixedArity = 2;
  _STAR_.cljs$lang$applyTo = _STAR___3146.cljs$lang$applyTo;
  return _STAR_
}();
cljs.core._SLASH_ = function() {
  var _SLASH_ = null;
  var _SLASH___3150 = function(x) {
    return _SLASH_.call(null, 1, x)
  };
  var _SLASH___3151 = function(x, y) {
    return x / y
  };
  var _SLASH___3152 = function() {
    var G__3154__delegate = function(x, y, more) {
      return cljs.core.reduce.call(null, _SLASH_, _SLASH_.call(null, x, y), more)
    };
    var G__3154 = function(x, y, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3154__delegate.call(this, x, y, more)
    };
    G__3154.cljs$lang$maxFixedArity = 2;
    G__3154.cljs$lang$applyTo = function(arglist__3155) {
      var x = cljs.core.first(arglist__3155);
      var y = cljs.core.first(cljs.core.next(arglist__3155));
      var more = cljs.core.rest(cljs.core.next(arglist__3155));
      return G__3154__delegate.call(this, x, y, more)
    };
    return G__3154
  }();
  _SLASH_ = function(x, y, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 1:
        return _SLASH___3150.call(this, x);
      case 2:
        return _SLASH___3151.call(this, x, y);
      default:
        return _SLASH___3152.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  _SLASH_.cljs$lang$maxFixedArity = 2;
  _SLASH_.cljs$lang$applyTo = _SLASH___3152.cljs$lang$applyTo;
  return _SLASH_
}();
cljs.core._LT_ = function() {
  var _LT_ = null;
  var _LT___3156 = function(x) {
    return true
  };
  var _LT___3157 = function(x, y) {
    return x < y
  };
  var _LT___3158 = function() {
    var G__3160__delegate = function(x, y, more) {
      while(true) {
        if(cljs.core.truth_(x < y)) {
          if(cljs.core.truth_(cljs.core.next.call(null, more))) {
            var G__3161 = y;
            var G__3162 = cljs.core.first.call(null, more);
            var G__3163 = cljs.core.next.call(null, more);
            x = G__3161;
            y = G__3162;
            more = G__3163;
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
    var G__3160 = function(x, y, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3160__delegate.call(this, x, y, more)
    };
    G__3160.cljs$lang$maxFixedArity = 2;
    G__3160.cljs$lang$applyTo = function(arglist__3164) {
      var x = cljs.core.first(arglist__3164);
      var y = cljs.core.first(cljs.core.next(arglist__3164));
      var more = cljs.core.rest(cljs.core.next(arglist__3164));
      return G__3160__delegate.call(this, x, y, more)
    };
    return G__3160
  }();
  _LT_ = function(x, y, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 1:
        return _LT___3156.call(this, x);
      case 2:
        return _LT___3157.call(this, x, y);
      default:
        return _LT___3158.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  _LT_.cljs$lang$maxFixedArity = 2;
  _LT_.cljs$lang$applyTo = _LT___3158.cljs$lang$applyTo;
  return _LT_
}();
cljs.core._LT__EQ_ = function() {
  var _LT__EQ_ = null;
  var _LT__EQ___3165 = function(x) {
    return true
  };
  var _LT__EQ___3166 = function(x, y) {
    return x <= y
  };
  var _LT__EQ___3167 = function() {
    var G__3169__delegate = function(x, y, more) {
      while(true) {
        if(cljs.core.truth_(x <= y)) {
          if(cljs.core.truth_(cljs.core.next.call(null, more))) {
            var G__3170 = y;
            var G__3171 = cljs.core.first.call(null, more);
            var G__3172 = cljs.core.next.call(null, more);
            x = G__3170;
            y = G__3171;
            more = G__3172;
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
    var G__3169 = function(x, y, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3169__delegate.call(this, x, y, more)
    };
    G__3169.cljs$lang$maxFixedArity = 2;
    G__3169.cljs$lang$applyTo = function(arglist__3173) {
      var x = cljs.core.first(arglist__3173);
      var y = cljs.core.first(cljs.core.next(arglist__3173));
      var more = cljs.core.rest(cljs.core.next(arglist__3173));
      return G__3169__delegate.call(this, x, y, more)
    };
    return G__3169
  }();
  _LT__EQ_ = function(x, y, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 1:
        return _LT__EQ___3165.call(this, x);
      case 2:
        return _LT__EQ___3166.call(this, x, y);
      default:
        return _LT__EQ___3167.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  _LT__EQ_.cljs$lang$maxFixedArity = 2;
  _LT__EQ_.cljs$lang$applyTo = _LT__EQ___3167.cljs$lang$applyTo;
  return _LT__EQ_
}();
cljs.core._GT_ = function() {
  var _GT_ = null;
  var _GT___3174 = function(x) {
    return true
  };
  var _GT___3175 = function(x, y) {
    return x > y
  };
  var _GT___3176 = function() {
    var G__3178__delegate = function(x, y, more) {
      while(true) {
        if(cljs.core.truth_(x > y)) {
          if(cljs.core.truth_(cljs.core.next.call(null, more))) {
            var G__3179 = y;
            var G__3180 = cljs.core.first.call(null, more);
            var G__3181 = cljs.core.next.call(null, more);
            x = G__3179;
            y = G__3180;
            more = G__3181;
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
    var G__3178 = function(x, y, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3178__delegate.call(this, x, y, more)
    };
    G__3178.cljs$lang$maxFixedArity = 2;
    G__3178.cljs$lang$applyTo = function(arglist__3182) {
      var x = cljs.core.first(arglist__3182);
      var y = cljs.core.first(cljs.core.next(arglist__3182));
      var more = cljs.core.rest(cljs.core.next(arglist__3182));
      return G__3178__delegate.call(this, x, y, more)
    };
    return G__3178
  }();
  _GT_ = function(x, y, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 1:
        return _GT___3174.call(this, x);
      case 2:
        return _GT___3175.call(this, x, y);
      default:
        return _GT___3176.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  _GT_.cljs$lang$maxFixedArity = 2;
  _GT_.cljs$lang$applyTo = _GT___3176.cljs$lang$applyTo;
  return _GT_
}();
cljs.core._GT__EQ_ = function() {
  var _GT__EQ_ = null;
  var _GT__EQ___3183 = function(x) {
    return true
  };
  var _GT__EQ___3184 = function(x, y) {
    return x >= y
  };
  var _GT__EQ___3185 = function() {
    var G__3187__delegate = function(x, y, more) {
      while(true) {
        if(cljs.core.truth_(x >= y)) {
          if(cljs.core.truth_(cljs.core.next.call(null, more))) {
            var G__3188 = y;
            var G__3189 = cljs.core.first.call(null, more);
            var G__3190 = cljs.core.next.call(null, more);
            x = G__3188;
            y = G__3189;
            more = G__3190;
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
  _GT__EQ_ = function(x, y, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 1:
        return _GT__EQ___3183.call(this, x);
      case 2:
        return _GT__EQ___3184.call(this, x, y);
      default:
        return _GT__EQ___3185.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  _GT__EQ_.cljs$lang$maxFixedArity = 2;
  _GT__EQ_.cljs$lang$applyTo = _GT__EQ___3185.cljs$lang$applyTo;
  return _GT__EQ_
}();
cljs.core.dec = function dec(x) {
  return x - 1
};
cljs.core.max = function() {
  var max = null;
  var max__3192 = function(x) {
    return x
  };
  var max__3193 = function(x, y) {
    return x > y ? x : y
  };
  var max__3194 = function() {
    var G__3196__delegate = function(x, y, more) {
      return cljs.core.reduce.call(null, max, x > y ? x : y, more)
    };
    var G__3196 = function(x, y, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3196__delegate.call(this, x, y, more)
    };
    G__3196.cljs$lang$maxFixedArity = 2;
    G__3196.cljs$lang$applyTo = function(arglist__3197) {
      var x = cljs.core.first(arglist__3197);
      var y = cljs.core.first(cljs.core.next(arglist__3197));
      var more = cljs.core.rest(cljs.core.next(arglist__3197));
      return G__3196__delegate.call(this, x, y, more)
    };
    return G__3196
  }();
  max = function(x, y, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 1:
        return max__3192.call(this, x);
      case 2:
        return max__3193.call(this, x, y);
      default:
        return max__3194.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  max.cljs$lang$maxFixedArity = 2;
  max.cljs$lang$applyTo = max__3194.cljs$lang$applyTo;
  return max
}();
cljs.core.min = function() {
  var min = null;
  var min__3198 = function(x) {
    return x
  };
  var min__3199 = function(x, y) {
    return x < y ? x : y
  };
  var min__3200 = function() {
    var G__3202__delegate = function(x, y, more) {
      return cljs.core.reduce.call(null, min, x < y ? x : y, more)
    };
    var G__3202 = function(x, y, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3202__delegate.call(this, x, y, more)
    };
    G__3202.cljs$lang$maxFixedArity = 2;
    G__3202.cljs$lang$applyTo = function(arglist__3203) {
      var x = cljs.core.first(arglist__3203);
      var y = cljs.core.first(cljs.core.next(arglist__3203));
      var more = cljs.core.rest(cljs.core.next(arglist__3203));
      return G__3202__delegate.call(this, x, y, more)
    };
    return G__3202
  }();
  min = function(x, y, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 1:
        return min__3198.call(this, x);
      case 2:
        return min__3199.call(this, x, y);
      default:
        return min__3200.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  min.cljs$lang$maxFixedArity = 2;
  min.cljs$lang$applyTo = min__3200.cljs$lang$applyTo;
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
  var rem__3204 = n % d;
  return cljs.core.fix.call(null, (n - rem__3204) / d)
};
cljs.core.rem = function rem(n, d) {
  var q__3205 = cljs.core.quot.call(null, n, d);
  return n - d * q__3205
};
cljs.core.rand = function() {
  var rand = null;
  var rand__3206 = function() {
    return Math.random.call(null)
  };
  var rand__3207 = function(n) {
    return n * rand.call(null)
  };
  rand = function(n) {
    switch(arguments.length) {
      case 0:
        return rand__3206.call(this);
      case 1:
        return rand__3207.call(this, n)
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
  var _EQ__EQ___3209 = function(x) {
    return true
  };
  var _EQ__EQ___3210 = function(x, y) {
    return cljs.core._equiv.call(null, x, y)
  };
  var _EQ__EQ___3211 = function() {
    var G__3213__delegate = function(x, y, more) {
      while(true) {
        if(cljs.core.truth_(_EQ__EQ_.call(null, x, y))) {
          if(cljs.core.truth_(cljs.core.next.call(null, more))) {
            var G__3214 = y;
            var G__3215 = cljs.core.first.call(null, more);
            var G__3216 = cljs.core.next.call(null, more);
            x = G__3214;
            y = G__3215;
            more = G__3216;
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
    var G__3213 = function(x, y, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3213__delegate.call(this, x, y, more)
    };
    G__3213.cljs$lang$maxFixedArity = 2;
    G__3213.cljs$lang$applyTo = function(arglist__3217) {
      var x = cljs.core.first(arglist__3217);
      var y = cljs.core.first(cljs.core.next(arglist__3217));
      var more = cljs.core.rest(cljs.core.next(arglist__3217));
      return G__3213__delegate.call(this, x, y, more)
    };
    return G__3213
  }();
  _EQ__EQ_ = function(x, y, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 1:
        return _EQ__EQ___3209.call(this, x);
      case 2:
        return _EQ__EQ___3210.call(this, x, y);
      default:
        return _EQ__EQ___3211.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  _EQ__EQ_.cljs$lang$maxFixedArity = 2;
  _EQ__EQ_.cljs$lang$applyTo = _EQ__EQ___3211.cljs$lang$applyTo;
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
  var n__3218 = n;
  var xs__3219 = cljs.core.seq.call(null, coll);
  while(true) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____3220 = xs__3219;
      if(cljs.core.truth_(and__3546__auto____3220)) {
        return n__3218 > 0
      }else {
        return and__3546__auto____3220
      }
    }())) {
      var G__3221 = n__3218 - 1;
      var G__3222 = cljs.core.next.call(null, xs__3219);
      n__3218 = G__3221;
      xs__3219 = G__3222;
      continue
    }else {
      return xs__3219
    }
    break
  }
};
cljs.core.IIndexed["_"] = true;
cljs.core._nth["_"] = function() {
  var G__3227 = null;
  var G__3227__3228 = function(coll, n) {
    var temp__3695__auto____3223 = cljs.core.nthnext.call(null, coll, n);
    if(cljs.core.truth_(temp__3695__auto____3223)) {
      var xs__3224 = temp__3695__auto____3223;
      return cljs.core.first.call(null, xs__3224)
    }else {
      throw new Error("Index out of bounds");
    }
  };
  var G__3227__3229 = function(coll, n, not_found) {
    var temp__3695__auto____3225 = cljs.core.nthnext.call(null, coll, n);
    if(cljs.core.truth_(temp__3695__auto____3225)) {
      var xs__3226 = temp__3695__auto____3225;
      return cljs.core.first.call(null, xs__3226)
    }else {
      return not_found
    }
  };
  G__3227 = function(coll, n, not_found) {
    switch(arguments.length) {
      case 2:
        return G__3227__3228.call(this, coll, n);
      case 3:
        return G__3227__3229.call(this, coll, n, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3227
}();
cljs.core.str_STAR_ = function() {
  var str_STAR_ = null;
  var str_STAR___3231 = function() {
    return""
  };
  var str_STAR___3232 = function(x) {
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
  var str_STAR___3233 = function() {
    var G__3235__delegate = function(x, ys) {
      return function(sb, more) {
        while(true) {
          if(cljs.core.truth_(more)) {
            var G__3236 = sb.append(str_STAR_.call(null, cljs.core.first.call(null, more)));
            var G__3237 = cljs.core.next.call(null, more);
            sb = G__3236;
            more = G__3237;
            continue
          }else {
            return str_STAR_.call(null, sb)
          }
          break
        }
      }.call(null, new goog.string.StringBuffer(str_STAR_.call(null, x)), ys)
    };
    var G__3235 = function(x, var_args) {
      var ys = null;
      if(goog.isDef(var_args)) {
        ys = cljs.core.array_seq(Array.prototype.slice.call(arguments, 1), 0)
      }
      return G__3235__delegate.call(this, x, ys)
    };
    G__3235.cljs$lang$maxFixedArity = 1;
    G__3235.cljs$lang$applyTo = function(arglist__3238) {
      var x = cljs.core.first(arglist__3238);
      var ys = cljs.core.rest(arglist__3238);
      return G__3235__delegate.call(this, x, ys)
    };
    return G__3235
  }();
  str_STAR_ = function(x, var_args) {
    var ys = var_args;
    switch(arguments.length) {
      case 0:
        return str_STAR___3231.call(this);
      case 1:
        return str_STAR___3232.call(this, x);
      default:
        return str_STAR___3233.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  str_STAR_.cljs$lang$maxFixedArity = 1;
  str_STAR_.cljs$lang$applyTo = str_STAR___3233.cljs$lang$applyTo;
  return str_STAR_
}();
cljs.core.str = function() {
  var str = null;
  var str__3239 = function() {
    return""
  };
  var str__3240 = function(x) {
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
  var str__3241 = function() {
    var G__3243__delegate = function(x, ys) {
      return cljs.core.apply.call(null, cljs.core.str_STAR_, x, ys)
    };
    var G__3243 = function(x, var_args) {
      var ys = null;
      if(goog.isDef(var_args)) {
        ys = cljs.core.array_seq(Array.prototype.slice.call(arguments, 1), 0)
      }
      return G__3243__delegate.call(this, x, ys)
    };
    G__3243.cljs$lang$maxFixedArity = 1;
    G__3243.cljs$lang$applyTo = function(arglist__3244) {
      var x = cljs.core.first(arglist__3244);
      var ys = cljs.core.rest(arglist__3244);
      return G__3243__delegate.call(this, x, ys)
    };
    return G__3243
  }();
  str = function(x, var_args) {
    var ys = var_args;
    switch(arguments.length) {
      case 0:
        return str__3239.call(this);
      case 1:
        return str__3240.call(this, x);
      default:
        return str__3241.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  str.cljs$lang$maxFixedArity = 1;
  str.cljs$lang$applyTo = str__3241.cljs$lang$applyTo;
  return str
}();
cljs.core.subs = function() {
  var subs = null;
  var subs__3245 = function(s, start) {
    return s.substring(start)
  };
  var subs__3246 = function(s, start, end) {
    return s.substring(start, end)
  };
  subs = function(s, start, end) {
    switch(arguments.length) {
      case 2:
        return subs__3245.call(this, s, start);
      case 3:
        return subs__3246.call(this, s, start, end)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return subs
}();
cljs.core.symbol = function() {
  var symbol = null;
  var symbol__3248 = function(name) {
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
  var symbol__3249 = function(ns, name) {
    return symbol.call(null, cljs.core.str_STAR_.call(null, ns, "/", name))
  };
  symbol = function(ns, name) {
    switch(arguments.length) {
      case 1:
        return symbol__3248.call(this, ns);
      case 2:
        return symbol__3249.call(this, ns, name)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return symbol
}();
cljs.core.keyword = function() {
  var keyword = null;
  var keyword__3251 = function(name) {
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
  var keyword__3252 = function(ns, name) {
    return keyword.call(null, cljs.core.str_STAR_.call(null, ns, "/", name))
  };
  keyword = function(ns, name) {
    switch(arguments.length) {
      case 1:
        return keyword__3251.call(this, ns);
      case 2:
        return keyword__3252.call(this, ns, name)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return keyword
}();
cljs.core.equiv_sequential = function equiv_sequential(x, y) {
  return cljs.core.boolean$.call(null, cljs.core.truth_(cljs.core.sequential_QMARK_.call(null, y)) ? function() {
    var xs__3254 = cljs.core.seq.call(null, x);
    var ys__3255 = cljs.core.seq.call(null, y);
    while(true) {
      if(cljs.core.truth_(xs__3254 === null)) {
        return ys__3255 === null
      }else {
        if(cljs.core.truth_(ys__3255 === null)) {
          return false
        }else {
          if(cljs.core.truth_(cljs.core._EQ_.call(null, cljs.core.first.call(null, xs__3254), cljs.core.first.call(null, ys__3255)))) {
            var G__3256 = cljs.core.next.call(null, xs__3254);
            var G__3257 = cljs.core.next.call(null, ys__3255);
            xs__3254 = G__3256;
            ys__3255 = G__3257;
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
  return cljs.core.reduce.call(null, function(p1__3258_SHARP_, p2__3259_SHARP_) {
    return cljs.core.hash_combine.call(null, p1__3258_SHARP_, cljs.core.hash.call(null, p2__3259_SHARP_))
  }, cljs.core.hash.call(null, cljs.core.first.call(null, coll)), cljs.core.next.call(null, coll))
};
cljs.core.extend_object_BANG_ = function extend_object_BANG_(obj, fn_map) {
  var G__3260__3261 = cljs.core.seq.call(null, fn_map);
  if(cljs.core.truth_(G__3260__3261)) {
    var G__3263__3265 = cljs.core.first.call(null, G__3260__3261);
    var vec__3264__3266 = G__3263__3265;
    var key_name__3267 = cljs.core.nth.call(null, vec__3264__3266, 0, null);
    var f__3268 = cljs.core.nth.call(null, vec__3264__3266, 1, null);
    var G__3260__3269 = G__3260__3261;
    var G__3263__3270 = G__3263__3265;
    var G__3260__3271 = G__3260__3269;
    while(true) {
      var vec__3272__3273 = G__3263__3270;
      var key_name__3274 = cljs.core.nth.call(null, vec__3272__3273, 0, null);
      var f__3275 = cljs.core.nth.call(null, vec__3272__3273, 1, null);
      var G__3260__3276 = G__3260__3271;
      var str_name__3277 = cljs.core.name.call(null, key_name__3274);
      obj[str_name__3277] = f__3275;
      var temp__3698__auto____3278 = cljs.core.next.call(null, G__3260__3276);
      if(cljs.core.truth_(temp__3698__auto____3278)) {
        var G__3260__3279 = temp__3698__auto____3278;
        var G__3280 = cljs.core.first.call(null, G__3260__3279);
        var G__3281 = G__3260__3279;
        G__3263__3270 = G__3280;
        G__3260__3271 = G__3281;
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
  var this__3282 = this;
  return cljs.core.hash_coll.call(null, coll)
};
cljs.core.List.prototype.cljs$core$ISequential$ = true;
cljs.core.List.prototype.cljs$core$ICollection$ = true;
cljs.core.List.prototype.cljs$core$ICollection$_conj = function(coll, o) {
  var this__3283 = this;
  return new cljs.core.List(this__3283.meta, o, coll, this__3283.count + 1)
};
cljs.core.List.prototype.cljs$core$ISeqable$ = true;
cljs.core.List.prototype.cljs$core$ISeqable$_seq = function(coll) {
  var this__3284 = this;
  return coll
};
cljs.core.List.prototype.cljs$core$ICounted$ = true;
cljs.core.List.prototype.cljs$core$ICounted$_count = function(coll) {
  var this__3285 = this;
  return this__3285.count
};
cljs.core.List.prototype.cljs$core$IStack$ = true;
cljs.core.List.prototype.cljs$core$IStack$_peek = function(coll) {
  var this__3286 = this;
  return this__3286.first
};
cljs.core.List.prototype.cljs$core$IStack$_pop = function(coll) {
  var this__3287 = this;
  return cljs.core._rest.call(null, coll)
};
cljs.core.List.prototype.cljs$core$ISeq$ = true;
cljs.core.List.prototype.cljs$core$ISeq$_first = function(coll) {
  var this__3288 = this;
  return this__3288.first
};
cljs.core.List.prototype.cljs$core$ISeq$_rest = function(coll) {
  var this__3289 = this;
  return this__3289.rest
};
cljs.core.List.prototype.cljs$core$IEquiv$ = true;
cljs.core.List.prototype.cljs$core$IEquiv$_equiv = function(coll, other) {
  var this__3290 = this;
  return cljs.core.equiv_sequential.call(null, coll, other)
};
cljs.core.List.prototype.cljs$core$IWithMeta$ = true;
cljs.core.List.prototype.cljs$core$IWithMeta$_with_meta = function(coll, meta) {
  var this__3291 = this;
  return new cljs.core.List(meta, this__3291.first, this__3291.rest, this__3291.count)
};
cljs.core.List.prototype.cljs$core$IMeta$ = true;
cljs.core.List.prototype.cljs$core$IMeta$_meta = function(coll) {
  var this__3292 = this;
  return this__3292.meta
};
cljs.core.List.prototype.cljs$core$IEmptyableCollection$ = true;
cljs.core.List.prototype.cljs$core$IEmptyableCollection$_empty = function(coll) {
  var this__3293 = this;
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
  var this__3294 = this;
  return cljs.core.hash_coll.call(null, coll)
};
cljs.core.EmptyList.prototype.cljs$core$ISequential$ = true;
cljs.core.EmptyList.prototype.cljs$core$ICollection$ = true;
cljs.core.EmptyList.prototype.cljs$core$ICollection$_conj = function(coll, o) {
  var this__3295 = this;
  return new cljs.core.List(this__3295.meta, o, null, 1)
};
cljs.core.EmptyList.prototype.cljs$core$ISeqable$ = true;
cljs.core.EmptyList.prototype.cljs$core$ISeqable$_seq = function(coll) {
  var this__3296 = this;
  return null
};
cljs.core.EmptyList.prototype.cljs$core$ICounted$ = true;
cljs.core.EmptyList.prototype.cljs$core$ICounted$_count = function(coll) {
  var this__3297 = this;
  return 0
};
cljs.core.EmptyList.prototype.cljs$core$IStack$ = true;
cljs.core.EmptyList.prototype.cljs$core$IStack$_peek = function(coll) {
  var this__3298 = this;
  return null
};
cljs.core.EmptyList.prototype.cljs$core$IStack$_pop = function(coll) {
  var this__3299 = this;
  return null
};
cljs.core.EmptyList.prototype.cljs$core$ISeq$ = true;
cljs.core.EmptyList.prototype.cljs$core$ISeq$_first = function(coll) {
  var this__3300 = this;
  return null
};
cljs.core.EmptyList.prototype.cljs$core$ISeq$_rest = function(coll) {
  var this__3301 = this;
  return null
};
cljs.core.EmptyList.prototype.cljs$core$IEquiv$ = true;
cljs.core.EmptyList.prototype.cljs$core$IEquiv$_equiv = function(coll, other) {
  var this__3302 = this;
  return cljs.core.equiv_sequential.call(null, coll, other)
};
cljs.core.EmptyList.prototype.cljs$core$IWithMeta$ = true;
cljs.core.EmptyList.prototype.cljs$core$IWithMeta$_with_meta = function(coll, meta) {
  var this__3303 = this;
  return new cljs.core.EmptyList(meta)
};
cljs.core.EmptyList.prototype.cljs$core$IMeta$ = true;
cljs.core.EmptyList.prototype.cljs$core$IMeta$_meta = function(coll) {
  var this__3304 = this;
  return this__3304.meta
};
cljs.core.EmptyList.prototype.cljs$core$IEmptyableCollection$ = true;
cljs.core.EmptyList.prototype.cljs$core$IEmptyableCollection$_empty = function(coll) {
  var this__3305 = this;
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
  list.cljs$lang$applyTo = function(arglist__3306) {
    var items = cljs.core.seq(arglist__3306);
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
  var this__3307 = this;
  return coll
};
cljs.core.Cons.prototype.cljs$core$IHash$ = true;
cljs.core.Cons.prototype.cljs$core$IHash$_hash = function(coll) {
  var this__3308 = this;
  return cljs.core.hash_coll.call(null, coll)
};
cljs.core.Cons.prototype.cljs$core$IEquiv$ = true;
cljs.core.Cons.prototype.cljs$core$IEquiv$_equiv = function(coll, other) {
  var this__3309 = this;
  return cljs.core.equiv_sequential.call(null, coll, other)
};
cljs.core.Cons.prototype.cljs$core$ISequential$ = true;
cljs.core.Cons.prototype.cljs$core$IEmptyableCollection$ = true;
cljs.core.Cons.prototype.cljs$core$IEmptyableCollection$_empty = function(coll) {
  var this__3310 = this;
  return cljs.core.with_meta.call(null, cljs.core.List.EMPTY, this__3310.meta)
};
cljs.core.Cons.prototype.cljs$core$ICollection$ = true;
cljs.core.Cons.prototype.cljs$core$ICollection$_conj = function(coll, o) {
  var this__3311 = this;
  return new cljs.core.Cons(null, o, coll)
};
cljs.core.Cons.prototype.cljs$core$ISeq$ = true;
cljs.core.Cons.prototype.cljs$core$ISeq$_first = function(coll) {
  var this__3312 = this;
  return this__3312.first
};
cljs.core.Cons.prototype.cljs$core$ISeq$_rest = function(coll) {
  var this__3313 = this;
  if(cljs.core.truth_(this__3313.rest === null)) {
    return cljs.core.List.EMPTY
  }else {
    return this__3313.rest
  }
};
cljs.core.Cons.prototype.cljs$core$IMeta$ = true;
cljs.core.Cons.prototype.cljs$core$IMeta$_meta = function(coll) {
  var this__3314 = this;
  return this__3314.meta
};
cljs.core.Cons.prototype.cljs$core$IWithMeta$ = true;
cljs.core.Cons.prototype.cljs$core$IWithMeta$_with_meta = function(coll, meta) {
  var this__3315 = this;
  return new cljs.core.Cons(meta, this__3315.first, this__3315.rest)
};
cljs.core.Cons;
cljs.core.cons = function cons(x, seq) {
  return new cljs.core.Cons(null, x, seq)
};
cljs.core.IReduce["string"] = true;
cljs.core._reduce["string"] = function() {
  var G__3316 = null;
  var G__3316__3317 = function(string, f) {
    return cljs.core.ci_reduce.call(null, string, f)
  };
  var G__3316__3318 = function(string, f, start) {
    return cljs.core.ci_reduce.call(null, string, f, start)
  };
  G__3316 = function(string, f, start) {
    switch(arguments.length) {
      case 2:
        return G__3316__3317.call(this, string, f);
      case 3:
        return G__3316__3318.call(this, string, f, start)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3316
}();
cljs.core.ILookup["string"] = true;
cljs.core._lookup["string"] = function() {
  var G__3320 = null;
  var G__3320__3321 = function(string, k) {
    return cljs.core._nth.call(null, string, k)
  };
  var G__3320__3322 = function(string, k, not_found) {
    return cljs.core._nth.call(null, string, k, not_found)
  };
  G__3320 = function(string, k, not_found) {
    switch(arguments.length) {
      case 2:
        return G__3320__3321.call(this, string, k);
      case 3:
        return G__3320__3322.call(this, string, k, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3320
}();
cljs.core.IIndexed["string"] = true;
cljs.core._nth["string"] = function() {
  var G__3324 = null;
  var G__3324__3325 = function(string, n) {
    if(cljs.core.truth_(n < cljs.core._count.call(null, string))) {
      return string.charAt(n)
    }else {
      return null
    }
  };
  var G__3324__3326 = function(string, n, not_found) {
    if(cljs.core.truth_(n < cljs.core._count.call(null, string))) {
      return string.charAt(n)
    }else {
      return not_found
    }
  };
  G__3324 = function(string, n, not_found) {
    switch(arguments.length) {
      case 2:
        return G__3324__3325.call(this, string, n);
      case 3:
        return G__3324__3326.call(this, string, n, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3324
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
  var G__3334 = null;
  var G__3334__3335 = function(tsym3328, coll) {
    var tsym3328__3330 = this;
    var this$__3331 = tsym3328__3330;
    return cljs.core.get.call(null, coll, this$__3331.toString())
  };
  var G__3334__3336 = function(tsym3329, coll, not_found) {
    var tsym3329__3332 = this;
    var this$__3333 = tsym3329__3332;
    return cljs.core.get.call(null, coll, this$__3333.toString(), not_found)
  };
  G__3334 = function(tsym3329, coll, not_found) {
    switch(arguments.length) {
      case 2:
        return G__3334__3335.call(this, tsym3329, coll);
      case 3:
        return G__3334__3336.call(this, tsym3329, coll, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3334
}();
String["prototype"]["apply"] = function(s, args) {
  if(cljs.core.truth_(cljs.core.count.call(null, args) < 2)) {
    return cljs.core.get.call(null, args[0], s)
  }else {
    return cljs.core.get.call(null, args[0], s, args[1])
  }
};
cljs.core.lazy_seq_value = function lazy_seq_value(lazy_seq) {
  var x__3338 = lazy_seq.x;
  if(cljs.core.truth_(lazy_seq.realized)) {
    return x__3338
  }else {
    lazy_seq.x = x__3338.call(null);
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
  var this__3339 = this;
  return cljs.core.seq.call(null, cljs.core.lazy_seq_value.call(null, coll))
};
cljs.core.LazySeq.prototype.cljs$core$IHash$ = true;
cljs.core.LazySeq.prototype.cljs$core$IHash$_hash = function(coll) {
  var this__3340 = this;
  return cljs.core.hash_coll.call(null, coll)
};
cljs.core.LazySeq.prototype.cljs$core$IEquiv$ = true;
cljs.core.LazySeq.prototype.cljs$core$IEquiv$_equiv = function(coll, other) {
  var this__3341 = this;
  return cljs.core.equiv_sequential.call(null, coll, other)
};
cljs.core.LazySeq.prototype.cljs$core$ISequential$ = true;
cljs.core.LazySeq.prototype.cljs$core$IEmptyableCollection$ = true;
cljs.core.LazySeq.prototype.cljs$core$IEmptyableCollection$_empty = function(coll) {
  var this__3342 = this;
  return cljs.core.with_meta.call(null, cljs.core.List.EMPTY, this__3342.meta)
};
cljs.core.LazySeq.prototype.cljs$core$ICollection$ = true;
cljs.core.LazySeq.prototype.cljs$core$ICollection$_conj = function(coll, o) {
  var this__3343 = this;
  return cljs.core.cons.call(null, o, coll)
};
cljs.core.LazySeq.prototype.cljs$core$ISeq$ = true;
cljs.core.LazySeq.prototype.cljs$core$ISeq$_first = function(coll) {
  var this__3344 = this;
  return cljs.core.first.call(null, cljs.core.lazy_seq_value.call(null, coll))
};
cljs.core.LazySeq.prototype.cljs$core$ISeq$_rest = function(coll) {
  var this__3345 = this;
  return cljs.core.rest.call(null, cljs.core.lazy_seq_value.call(null, coll))
};
cljs.core.LazySeq.prototype.cljs$core$IMeta$ = true;
cljs.core.LazySeq.prototype.cljs$core$IMeta$_meta = function(coll) {
  var this__3346 = this;
  return this__3346.meta
};
cljs.core.LazySeq.prototype.cljs$core$IWithMeta$ = true;
cljs.core.LazySeq.prototype.cljs$core$IWithMeta$_with_meta = function(coll, meta) {
  var this__3347 = this;
  return new cljs.core.LazySeq(meta, this__3347.realized, this__3347.x)
};
cljs.core.LazySeq;
cljs.core.to_array = function to_array(s) {
  var ary__3348 = cljs.core.array.call(null);
  var s__3349 = s;
  while(true) {
    if(cljs.core.truth_(cljs.core.seq.call(null, s__3349))) {
      ary__3348.push(cljs.core.first.call(null, s__3349));
      var G__3350 = cljs.core.next.call(null, s__3349);
      s__3349 = G__3350;
      continue
    }else {
      return ary__3348
    }
    break
  }
};
cljs.core.bounded_count = function bounded_count(s, n) {
  var s__3351 = s;
  var i__3352 = n;
  var sum__3353 = 0;
  while(true) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____3354 = i__3352 > 0;
      if(cljs.core.truth_(and__3546__auto____3354)) {
        return cljs.core.seq.call(null, s__3351)
      }else {
        return and__3546__auto____3354
      }
    }())) {
      var G__3355 = cljs.core.next.call(null, s__3351);
      var G__3356 = i__3352 - 1;
      var G__3357 = sum__3353 + 1;
      s__3351 = G__3355;
      i__3352 = G__3356;
      sum__3353 = G__3357;
      continue
    }else {
      return sum__3353
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
  var concat__3361 = function() {
    return new cljs.core.LazySeq(null, false, function() {
      return null
    })
  };
  var concat__3362 = function(x) {
    return new cljs.core.LazySeq(null, false, function() {
      return x
    })
  };
  var concat__3363 = function(x, y) {
    return new cljs.core.LazySeq(null, false, function() {
      var s__3358 = cljs.core.seq.call(null, x);
      if(cljs.core.truth_(s__3358)) {
        return cljs.core.cons.call(null, cljs.core.first.call(null, s__3358), concat.call(null, cljs.core.rest.call(null, s__3358), y))
      }else {
        return y
      }
    })
  };
  var concat__3364 = function() {
    var G__3366__delegate = function(x, y, zs) {
      var cat__3360 = function cat(xys, zs) {
        return new cljs.core.LazySeq(null, false, function() {
          var xys__3359 = cljs.core.seq.call(null, xys);
          if(cljs.core.truth_(xys__3359)) {
            return cljs.core.cons.call(null, cljs.core.first.call(null, xys__3359), cat.call(null, cljs.core.rest.call(null, xys__3359), zs))
          }else {
            if(cljs.core.truth_(zs)) {
              return cat.call(null, cljs.core.first.call(null, zs), cljs.core.next.call(null, zs))
            }else {
              return null
            }
          }
        })
      };
      return cat__3360.call(null, concat.call(null, x, y), zs)
    };
    var G__3366 = function(x, y, var_args) {
      var zs = null;
      if(goog.isDef(var_args)) {
        zs = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3366__delegate.call(this, x, y, zs)
    };
    G__3366.cljs$lang$maxFixedArity = 2;
    G__3366.cljs$lang$applyTo = function(arglist__3367) {
      var x = cljs.core.first(arglist__3367);
      var y = cljs.core.first(cljs.core.next(arglist__3367));
      var zs = cljs.core.rest(cljs.core.next(arglist__3367));
      return G__3366__delegate.call(this, x, y, zs)
    };
    return G__3366
  }();
  concat = function(x, y, var_args) {
    var zs = var_args;
    switch(arguments.length) {
      case 0:
        return concat__3361.call(this);
      case 1:
        return concat__3362.call(this, x);
      case 2:
        return concat__3363.call(this, x, y);
      default:
        return concat__3364.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  concat.cljs$lang$maxFixedArity = 2;
  concat.cljs$lang$applyTo = concat__3364.cljs$lang$applyTo;
  return concat
}();
cljs.core.list_STAR_ = function() {
  var list_STAR_ = null;
  var list_STAR___3368 = function(args) {
    return cljs.core.seq.call(null, args)
  };
  var list_STAR___3369 = function(a, args) {
    return cljs.core.cons.call(null, a, args)
  };
  var list_STAR___3370 = function(a, b, args) {
    return cljs.core.cons.call(null, a, cljs.core.cons.call(null, b, args))
  };
  var list_STAR___3371 = function(a, b, c, args) {
    return cljs.core.cons.call(null, a, cljs.core.cons.call(null, b, cljs.core.cons.call(null, c, args)))
  };
  var list_STAR___3372 = function() {
    var G__3374__delegate = function(a, b, c, d, more) {
      return cljs.core.cons.call(null, a, cljs.core.cons.call(null, b, cljs.core.cons.call(null, c, cljs.core.cons.call(null, d, cljs.core.spread.call(null, more)))))
    };
    var G__3374 = function(a, b, c, d, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 4), 0)
      }
      return G__3374__delegate.call(this, a, b, c, d, more)
    };
    G__3374.cljs$lang$maxFixedArity = 4;
    G__3374.cljs$lang$applyTo = function(arglist__3375) {
      var a = cljs.core.first(arglist__3375);
      var b = cljs.core.first(cljs.core.next(arglist__3375));
      var c = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3375)));
      var d = cljs.core.first(cljs.core.next(cljs.core.next(cljs.core.next(arglist__3375))));
      var more = cljs.core.rest(cljs.core.next(cljs.core.next(cljs.core.next(arglist__3375))));
      return G__3374__delegate.call(this, a, b, c, d, more)
    };
    return G__3374
  }();
  list_STAR_ = function(a, b, c, d, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 1:
        return list_STAR___3368.call(this, a);
      case 2:
        return list_STAR___3369.call(this, a, b);
      case 3:
        return list_STAR___3370.call(this, a, b, c);
      case 4:
        return list_STAR___3371.call(this, a, b, c, d);
      default:
        return list_STAR___3372.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  list_STAR_.cljs$lang$maxFixedArity = 4;
  list_STAR_.cljs$lang$applyTo = list_STAR___3372.cljs$lang$applyTo;
  return list_STAR_
}();
cljs.core.apply = function() {
  var apply = null;
  var apply__3385 = function(f, args) {
    var fixed_arity__3376 = f.cljs$lang$maxFixedArity;
    if(cljs.core.truth_(f.cljs$lang$applyTo)) {
      if(cljs.core.truth_(cljs.core.bounded_count.call(null, args, fixed_arity__3376 + 1) <= fixed_arity__3376)) {
        return f.apply(f, cljs.core.to_array.call(null, args))
      }else {
        return f.cljs$lang$applyTo(args)
      }
    }else {
      return f.apply(f, cljs.core.to_array.call(null, args))
    }
  };
  var apply__3386 = function(f, x, args) {
    var arglist__3377 = cljs.core.list_STAR_.call(null, x, args);
    var fixed_arity__3378 = f.cljs$lang$maxFixedArity;
    if(cljs.core.truth_(f.cljs$lang$applyTo)) {
      if(cljs.core.truth_(cljs.core.bounded_count.call(null, arglist__3377, fixed_arity__3378) <= fixed_arity__3378)) {
        return f.apply(f, cljs.core.to_array.call(null, arglist__3377))
      }else {
        return f.cljs$lang$applyTo(arglist__3377)
      }
    }else {
      return f.apply(f, cljs.core.to_array.call(null, arglist__3377))
    }
  };
  var apply__3387 = function(f, x, y, args) {
    var arglist__3379 = cljs.core.list_STAR_.call(null, x, y, args);
    var fixed_arity__3380 = f.cljs$lang$maxFixedArity;
    if(cljs.core.truth_(f.cljs$lang$applyTo)) {
      if(cljs.core.truth_(cljs.core.bounded_count.call(null, arglist__3379, fixed_arity__3380) <= fixed_arity__3380)) {
        return f.apply(f, cljs.core.to_array.call(null, arglist__3379))
      }else {
        return f.cljs$lang$applyTo(arglist__3379)
      }
    }else {
      return f.apply(f, cljs.core.to_array.call(null, arglist__3379))
    }
  };
  var apply__3388 = function(f, x, y, z, args) {
    var arglist__3381 = cljs.core.list_STAR_.call(null, x, y, z, args);
    var fixed_arity__3382 = f.cljs$lang$maxFixedArity;
    if(cljs.core.truth_(f.cljs$lang$applyTo)) {
      if(cljs.core.truth_(cljs.core.bounded_count.call(null, arglist__3381, fixed_arity__3382) <= fixed_arity__3382)) {
        return f.apply(f, cljs.core.to_array.call(null, arglist__3381))
      }else {
        return f.cljs$lang$applyTo(arglist__3381)
      }
    }else {
      return f.apply(f, cljs.core.to_array.call(null, arglist__3381))
    }
  };
  var apply__3389 = function() {
    var G__3391__delegate = function(f, a, b, c, d, args) {
      var arglist__3383 = cljs.core.cons.call(null, a, cljs.core.cons.call(null, b, cljs.core.cons.call(null, c, cljs.core.cons.call(null, d, cljs.core.spread.call(null, args)))));
      var fixed_arity__3384 = f.cljs$lang$maxFixedArity;
      if(cljs.core.truth_(f.cljs$lang$applyTo)) {
        if(cljs.core.truth_(cljs.core.bounded_count.call(null, arglist__3383, fixed_arity__3384) <= fixed_arity__3384)) {
          return f.apply(f, cljs.core.to_array.call(null, arglist__3383))
        }else {
          return f.cljs$lang$applyTo(arglist__3383)
        }
      }else {
        return f.apply(f, cljs.core.to_array.call(null, arglist__3383))
      }
    };
    var G__3391 = function(f, a, b, c, d, var_args) {
      var args = null;
      if(goog.isDef(var_args)) {
        args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 5), 0)
      }
      return G__3391__delegate.call(this, f, a, b, c, d, args)
    };
    G__3391.cljs$lang$maxFixedArity = 5;
    G__3391.cljs$lang$applyTo = function(arglist__3392) {
      var f = cljs.core.first(arglist__3392);
      var a = cljs.core.first(cljs.core.next(arglist__3392));
      var b = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3392)));
      var c = cljs.core.first(cljs.core.next(cljs.core.next(cljs.core.next(arglist__3392))));
      var d = cljs.core.first(cljs.core.next(cljs.core.next(cljs.core.next(cljs.core.next(arglist__3392)))));
      var args = cljs.core.rest(cljs.core.next(cljs.core.next(cljs.core.next(cljs.core.next(arglist__3392)))));
      return G__3391__delegate.call(this, f, a, b, c, d, args)
    };
    return G__3391
  }();
  apply = function(f, a, b, c, d, var_args) {
    var args = var_args;
    switch(arguments.length) {
      case 2:
        return apply__3385.call(this, f, a);
      case 3:
        return apply__3386.call(this, f, a, b);
      case 4:
        return apply__3387.call(this, f, a, b, c);
      case 5:
        return apply__3388.call(this, f, a, b, c, d);
      default:
        return apply__3389.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  apply.cljs$lang$maxFixedArity = 5;
  apply.cljs$lang$applyTo = apply__3389.cljs$lang$applyTo;
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
  vary_meta.cljs$lang$applyTo = function(arglist__3393) {
    var obj = cljs.core.first(arglist__3393);
    var f = cljs.core.first(cljs.core.next(arglist__3393));
    var args = cljs.core.rest(cljs.core.next(arglist__3393));
    return vary_meta__delegate.call(this, obj, f, args)
  };
  return vary_meta
}();
cljs.core.not_EQ_ = function() {
  var not_EQ_ = null;
  var not_EQ___3394 = function(x) {
    return false
  };
  var not_EQ___3395 = function(x, y) {
    return cljs.core.not.call(null, cljs.core._EQ_.call(null, x, y))
  };
  var not_EQ___3396 = function() {
    var G__3398__delegate = function(x, y, more) {
      return cljs.core.not.call(null, cljs.core.apply.call(null, cljs.core._EQ_, x, y, more))
    };
    var G__3398 = function(x, y, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3398__delegate.call(this, x, y, more)
    };
    G__3398.cljs$lang$maxFixedArity = 2;
    G__3398.cljs$lang$applyTo = function(arglist__3399) {
      var x = cljs.core.first(arglist__3399);
      var y = cljs.core.first(cljs.core.next(arglist__3399));
      var more = cljs.core.rest(cljs.core.next(arglist__3399));
      return G__3398__delegate.call(this, x, y, more)
    };
    return G__3398
  }();
  not_EQ_ = function(x, y, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 1:
        return not_EQ___3394.call(this, x);
      case 2:
        return not_EQ___3395.call(this, x, y);
      default:
        return not_EQ___3396.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  not_EQ_.cljs$lang$maxFixedArity = 2;
  not_EQ_.cljs$lang$applyTo = not_EQ___3396.cljs$lang$applyTo;
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
        var G__3400 = pred;
        var G__3401 = cljs.core.next.call(null, coll);
        pred = G__3400;
        coll = G__3401;
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
      var or__3548__auto____3402 = pred.call(null, cljs.core.first.call(null, coll));
      if(cljs.core.truth_(or__3548__auto____3402)) {
        return or__3548__auto____3402
      }else {
        var G__3403 = pred;
        var G__3404 = cljs.core.next.call(null, coll);
        pred = G__3403;
        coll = G__3404;
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
    var G__3405 = null;
    var G__3405__3406 = function() {
      return cljs.core.not.call(null, f.call(null))
    };
    var G__3405__3407 = function(x) {
      return cljs.core.not.call(null, f.call(null, x))
    };
    var G__3405__3408 = function(x, y) {
      return cljs.core.not.call(null, f.call(null, x, y))
    };
    var G__3405__3409 = function() {
      var G__3411__delegate = function(x, y, zs) {
        return cljs.core.not.call(null, cljs.core.apply.call(null, f, x, y, zs))
      };
      var G__3411 = function(x, y, var_args) {
        var zs = null;
        if(goog.isDef(var_args)) {
          zs = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
        }
        return G__3411__delegate.call(this, x, y, zs)
      };
      G__3411.cljs$lang$maxFixedArity = 2;
      G__3411.cljs$lang$applyTo = function(arglist__3412) {
        var x = cljs.core.first(arglist__3412);
        var y = cljs.core.first(cljs.core.next(arglist__3412));
        var zs = cljs.core.rest(cljs.core.next(arglist__3412));
        return G__3411__delegate.call(this, x, y, zs)
      };
      return G__3411
    }();
    G__3405 = function(x, y, var_args) {
      var zs = var_args;
      switch(arguments.length) {
        case 0:
          return G__3405__3406.call(this);
        case 1:
          return G__3405__3407.call(this, x);
        case 2:
          return G__3405__3408.call(this, x, y);
        default:
          return G__3405__3409.apply(this, arguments)
      }
      throw"Invalid arity: " + arguments.length;
    };
    G__3405.cljs$lang$maxFixedArity = 2;
    G__3405.cljs$lang$applyTo = G__3405__3409.cljs$lang$applyTo;
    return G__3405
  }()
};
cljs.core.constantly = function constantly(x) {
  return function() {
    var G__3413__delegate = function(args) {
      return x
    };
    var G__3413 = function(var_args) {
      var args = null;
      if(goog.isDef(var_args)) {
        args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 0), 0)
      }
      return G__3413__delegate.call(this, args)
    };
    G__3413.cljs$lang$maxFixedArity = 0;
    G__3413.cljs$lang$applyTo = function(arglist__3414) {
      var args = cljs.core.seq(arglist__3414);
      return G__3413__delegate.call(this, args)
    };
    return G__3413
  }()
};
cljs.core.comp = function() {
  var comp = null;
  var comp__3418 = function() {
    return cljs.core.identity
  };
  var comp__3419 = function(f) {
    return f
  };
  var comp__3420 = function(f, g) {
    return function() {
      var G__3424 = null;
      var G__3424__3425 = function() {
        return f.call(null, g.call(null))
      };
      var G__3424__3426 = function(x) {
        return f.call(null, g.call(null, x))
      };
      var G__3424__3427 = function(x, y) {
        return f.call(null, g.call(null, x, y))
      };
      var G__3424__3428 = function(x, y, z) {
        return f.call(null, g.call(null, x, y, z))
      };
      var G__3424__3429 = function() {
        var G__3431__delegate = function(x, y, z, args) {
          return f.call(null, cljs.core.apply.call(null, g, x, y, z, args))
        };
        var G__3431 = function(x, y, z, var_args) {
          var args = null;
          if(goog.isDef(var_args)) {
            args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
          }
          return G__3431__delegate.call(this, x, y, z, args)
        };
        G__3431.cljs$lang$maxFixedArity = 3;
        G__3431.cljs$lang$applyTo = function(arglist__3432) {
          var x = cljs.core.first(arglist__3432);
          var y = cljs.core.first(cljs.core.next(arglist__3432));
          var z = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3432)));
          var args = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3432)));
          return G__3431__delegate.call(this, x, y, z, args)
        };
        return G__3431
      }();
      G__3424 = function(x, y, z, var_args) {
        var args = var_args;
        switch(arguments.length) {
          case 0:
            return G__3424__3425.call(this);
          case 1:
            return G__3424__3426.call(this, x);
          case 2:
            return G__3424__3427.call(this, x, y);
          case 3:
            return G__3424__3428.call(this, x, y, z);
          default:
            return G__3424__3429.apply(this, arguments)
        }
        throw"Invalid arity: " + arguments.length;
      };
      G__3424.cljs$lang$maxFixedArity = 3;
      G__3424.cljs$lang$applyTo = G__3424__3429.cljs$lang$applyTo;
      return G__3424
    }()
  };
  var comp__3421 = function(f, g, h) {
    return function() {
      var G__3433 = null;
      var G__3433__3434 = function() {
        return f.call(null, g.call(null, h.call(null)))
      };
      var G__3433__3435 = function(x) {
        return f.call(null, g.call(null, h.call(null, x)))
      };
      var G__3433__3436 = function(x, y) {
        return f.call(null, g.call(null, h.call(null, x, y)))
      };
      var G__3433__3437 = function(x, y, z) {
        return f.call(null, g.call(null, h.call(null, x, y, z)))
      };
      var G__3433__3438 = function() {
        var G__3440__delegate = function(x, y, z, args) {
          return f.call(null, g.call(null, cljs.core.apply.call(null, h, x, y, z, args)))
        };
        var G__3440 = function(x, y, z, var_args) {
          var args = null;
          if(goog.isDef(var_args)) {
            args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
          }
          return G__3440__delegate.call(this, x, y, z, args)
        };
        G__3440.cljs$lang$maxFixedArity = 3;
        G__3440.cljs$lang$applyTo = function(arglist__3441) {
          var x = cljs.core.first(arglist__3441);
          var y = cljs.core.first(cljs.core.next(arglist__3441));
          var z = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3441)));
          var args = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3441)));
          return G__3440__delegate.call(this, x, y, z, args)
        };
        return G__3440
      }();
      G__3433 = function(x, y, z, var_args) {
        var args = var_args;
        switch(arguments.length) {
          case 0:
            return G__3433__3434.call(this);
          case 1:
            return G__3433__3435.call(this, x);
          case 2:
            return G__3433__3436.call(this, x, y);
          case 3:
            return G__3433__3437.call(this, x, y, z);
          default:
            return G__3433__3438.apply(this, arguments)
        }
        throw"Invalid arity: " + arguments.length;
      };
      G__3433.cljs$lang$maxFixedArity = 3;
      G__3433.cljs$lang$applyTo = G__3433__3438.cljs$lang$applyTo;
      return G__3433
    }()
  };
  var comp__3422 = function() {
    var G__3442__delegate = function(f1, f2, f3, fs) {
      var fs__3415 = cljs.core.reverse.call(null, cljs.core.list_STAR_.call(null, f1, f2, f3, fs));
      return function() {
        var G__3443__delegate = function(args) {
          var ret__3416 = cljs.core.apply.call(null, cljs.core.first.call(null, fs__3415), args);
          var fs__3417 = cljs.core.next.call(null, fs__3415);
          while(true) {
            if(cljs.core.truth_(fs__3417)) {
              var G__3444 = cljs.core.first.call(null, fs__3417).call(null, ret__3416);
              var G__3445 = cljs.core.next.call(null, fs__3417);
              ret__3416 = G__3444;
              fs__3417 = G__3445;
              continue
            }else {
              return ret__3416
            }
            break
          }
        };
        var G__3443 = function(var_args) {
          var args = null;
          if(goog.isDef(var_args)) {
            args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 0), 0)
          }
          return G__3443__delegate.call(this, args)
        };
        G__3443.cljs$lang$maxFixedArity = 0;
        G__3443.cljs$lang$applyTo = function(arglist__3446) {
          var args = cljs.core.seq(arglist__3446);
          return G__3443__delegate.call(this, args)
        };
        return G__3443
      }()
    };
    var G__3442 = function(f1, f2, f3, var_args) {
      var fs = null;
      if(goog.isDef(var_args)) {
        fs = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
      }
      return G__3442__delegate.call(this, f1, f2, f3, fs)
    };
    G__3442.cljs$lang$maxFixedArity = 3;
    G__3442.cljs$lang$applyTo = function(arglist__3447) {
      var f1 = cljs.core.first(arglist__3447);
      var f2 = cljs.core.first(cljs.core.next(arglist__3447));
      var f3 = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3447)));
      var fs = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3447)));
      return G__3442__delegate.call(this, f1, f2, f3, fs)
    };
    return G__3442
  }();
  comp = function(f1, f2, f3, var_args) {
    var fs = var_args;
    switch(arguments.length) {
      case 0:
        return comp__3418.call(this);
      case 1:
        return comp__3419.call(this, f1);
      case 2:
        return comp__3420.call(this, f1, f2);
      case 3:
        return comp__3421.call(this, f1, f2, f3);
      default:
        return comp__3422.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  comp.cljs$lang$maxFixedArity = 3;
  comp.cljs$lang$applyTo = comp__3422.cljs$lang$applyTo;
  return comp
}();
cljs.core.partial = function() {
  var partial = null;
  var partial__3448 = function(f, arg1) {
    return function() {
      var G__3453__delegate = function(args) {
        return cljs.core.apply.call(null, f, arg1, args)
      };
      var G__3453 = function(var_args) {
        var args = null;
        if(goog.isDef(var_args)) {
          args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 0), 0)
        }
        return G__3453__delegate.call(this, args)
      };
      G__3453.cljs$lang$maxFixedArity = 0;
      G__3453.cljs$lang$applyTo = function(arglist__3454) {
        var args = cljs.core.seq(arglist__3454);
        return G__3453__delegate.call(this, args)
      };
      return G__3453
    }()
  };
  var partial__3449 = function(f, arg1, arg2) {
    return function() {
      var G__3455__delegate = function(args) {
        return cljs.core.apply.call(null, f, arg1, arg2, args)
      };
      var G__3455 = function(var_args) {
        var args = null;
        if(goog.isDef(var_args)) {
          args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 0), 0)
        }
        return G__3455__delegate.call(this, args)
      };
      G__3455.cljs$lang$maxFixedArity = 0;
      G__3455.cljs$lang$applyTo = function(arglist__3456) {
        var args = cljs.core.seq(arglist__3456);
        return G__3455__delegate.call(this, args)
      };
      return G__3455
    }()
  };
  var partial__3450 = function(f, arg1, arg2, arg3) {
    return function() {
      var G__3457__delegate = function(args) {
        return cljs.core.apply.call(null, f, arg1, arg2, arg3, args)
      };
      var G__3457 = function(var_args) {
        var args = null;
        if(goog.isDef(var_args)) {
          args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 0), 0)
        }
        return G__3457__delegate.call(this, args)
      };
      G__3457.cljs$lang$maxFixedArity = 0;
      G__3457.cljs$lang$applyTo = function(arglist__3458) {
        var args = cljs.core.seq(arglist__3458);
        return G__3457__delegate.call(this, args)
      };
      return G__3457
    }()
  };
  var partial__3451 = function() {
    var G__3459__delegate = function(f, arg1, arg2, arg3, more) {
      return function() {
        var G__3460__delegate = function(args) {
          return cljs.core.apply.call(null, f, arg1, arg2, arg3, cljs.core.concat.call(null, more, args))
        };
        var G__3460 = function(var_args) {
          var args = null;
          if(goog.isDef(var_args)) {
            args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 0), 0)
          }
          return G__3460__delegate.call(this, args)
        };
        G__3460.cljs$lang$maxFixedArity = 0;
        G__3460.cljs$lang$applyTo = function(arglist__3461) {
          var args = cljs.core.seq(arglist__3461);
          return G__3460__delegate.call(this, args)
        };
        return G__3460
      }()
    };
    var G__3459 = function(f, arg1, arg2, arg3, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 4), 0)
      }
      return G__3459__delegate.call(this, f, arg1, arg2, arg3, more)
    };
    G__3459.cljs$lang$maxFixedArity = 4;
    G__3459.cljs$lang$applyTo = function(arglist__3462) {
      var f = cljs.core.first(arglist__3462);
      var arg1 = cljs.core.first(cljs.core.next(arglist__3462));
      var arg2 = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3462)));
      var arg3 = cljs.core.first(cljs.core.next(cljs.core.next(cljs.core.next(arglist__3462))));
      var more = cljs.core.rest(cljs.core.next(cljs.core.next(cljs.core.next(arglist__3462))));
      return G__3459__delegate.call(this, f, arg1, arg2, arg3, more)
    };
    return G__3459
  }();
  partial = function(f, arg1, arg2, arg3, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 2:
        return partial__3448.call(this, f, arg1);
      case 3:
        return partial__3449.call(this, f, arg1, arg2);
      case 4:
        return partial__3450.call(this, f, arg1, arg2, arg3);
      default:
        return partial__3451.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  partial.cljs$lang$maxFixedArity = 4;
  partial.cljs$lang$applyTo = partial__3451.cljs$lang$applyTo;
  return partial
}();
cljs.core.fnil = function() {
  var fnil = null;
  var fnil__3463 = function(f, x) {
    return function() {
      var G__3467 = null;
      var G__3467__3468 = function(a) {
        return f.call(null, cljs.core.truth_(a === null) ? x : a)
      };
      var G__3467__3469 = function(a, b) {
        return f.call(null, cljs.core.truth_(a === null) ? x : a, b)
      };
      var G__3467__3470 = function(a, b, c) {
        return f.call(null, cljs.core.truth_(a === null) ? x : a, b, c)
      };
      var G__3467__3471 = function() {
        var G__3473__delegate = function(a, b, c, ds) {
          return cljs.core.apply.call(null, f, cljs.core.truth_(a === null) ? x : a, b, c, ds)
        };
        var G__3473 = function(a, b, c, var_args) {
          var ds = null;
          if(goog.isDef(var_args)) {
            ds = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
          }
          return G__3473__delegate.call(this, a, b, c, ds)
        };
        G__3473.cljs$lang$maxFixedArity = 3;
        G__3473.cljs$lang$applyTo = function(arglist__3474) {
          var a = cljs.core.first(arglist__3474);
          var b = cljs.core.first(cljs.core.next(arglist__3474));
          var c = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3474)));
          var ds = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3474)));
          return G__3473__delegate.call(this, a, b, c, ds)
        };
        return G__3473
      }();
      G__3467 = function(a, b, c, var_args) {
        var ds = var_args;
        switch(arguments.length) {
          case 1:
            return G__3467__3468.call(this, a);
          case 2:
            return G__3467__3469.call(this, a, b);
          case 3:
            return G__3467__3470.call(this, a, b, c);
          default:
            return G__3467__3471.apply(this, arguments)
        }
        throw"Invalid arity: " + arguments.length;
      };
      G__3467.cljs$lang$maxFixedArity = 3;
      G__3467.cljs$lang$applyTo = G__3467__3471.cljs$lang$applyTo;
      return G__3467
    }()
  };
  var fnil__3464 = function(f, x, y) {
    return function() {
      var G__3475 = null;
      var G__3475__3476 = function(a, b) {
        return f.call(null, cljs.core.truth_(a === null) ? x : a, cljs.core.truth_(b === null) ? y : b)
      };
      var G__3475__3477 = function(a, b, c) {
        return f.call(null, cljs.core.truth_(a === null) ? x : a, cljs.core.truth_(b === null) ? y : b, c)
      };
      var G__3475__3478 = function() {
        var G__3480__delegate = function(a, b, c, ds) {
          return cljs.core.apply.call(null, f, cljs.core.truth_(a === null) ? x : a, cljs.core.truth_(b === null) ? y : b, c, ds)
        };
        var G__3480 = function(a, b, c, var_args) {
          var ds = null;
          if(goog.isDef(var_args)) {
            ds = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
          }
          return G__3480__delegate.call(this, a, b, c, ds)
        };
        G__3480.cljs$lang$maxFixedArity = 3;
        G__3480.cljs$lang$applyTo = function(arglist__3481) {
          var a = cljs.core.first(arglist__3481);
          var b = cljs.core.first(cljs.core.next(arglist__3481));
          var c = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3481)));
          var ds = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3481)));
          return G__3480__delegate.call(this, a, b, c, ds)
        };
        return G__3480
      }();
      G__3475 = function(a, b, c, var_args) {
        var ds = var_args;
        switch(arguments.length) {
          case 2:
            return G__3475__3476.call(this, a, b);
          case 3:
            return G__3475__3477.call(this, a, b, c);
          default:
            return G__3475__3478.apply(this, arguments)
        }
        throw"Invalid arity: " + arguments.length;
      };
      G__3475.cljs$lang$maxFixedArity = 3;
      G__3475.cljs$lang$applyTo = G__3475__3478.cljs$lang$applyTo;
      return G__3475
    }()
  };
  var fnil__3465 = function(f, x, y, z) {
    return function() {
      var G__3482 = null;
      var G__3482__3483 = function(a, b) {
        return f.call(null, cljs.core.truth_(a === null) ? x : a, cljs.core.truth_(b === null) ? y : b)
      };
      var G__3482__3484 = function(a, b, c) {
        return f.call(null, cljs.core.truth_(a === null) ? x : a, cljs.core.truth_(b === null) ? y : b, cljs.core.truth_(c === null) ? z : c)
      };
      var G__3482__3485 = function() {
        var G__3487__delegate = function(a, b, c, ds) {
          return cljs.core.apply.call(null, f, cljs.core.truth_(a === null) ? x : a, cljs.core.truth_(b === null) ? y : b, cljs.core.truth_(c === null) ? z : c, ds)
        };
        var G__3487 = function(a, b, c, var_args) {
          var ds = null;
          if(goog.isDef(var_args)) {
            ds = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
          }
          return G__3487__delegate.call(this, a, b, c, ds)
        };
        G__3487.cljs$lang$maxFixedArity = 3;
        G__3487.cljs$lang$applyTo = function(arglist__3488) {
          var a = cljs.core.first(arglist__3488);
          var b = cljs.core.first(cljs.core.next(arglist__3488));
          var c = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3488)));
          var ds = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3488)));
          return G__3487__delegate.call(this, a, b, c, ds)
        };
        return G__3487
      }();
      G__3482 = function(a, b, c, var_args) {
        var ds = var_args;
        switch(arguments.length) {
          case 2:
            return G__3482__3483.call(this, a, b);
          case 3:
            return G__3482__3484.call(this, a, b, c);
          default:
            return G__3482__3485.apply(this, arguments)
        }
        throw"Invalid arity: " + arguments.length;
      };
      G__3482.cljs$lang$maxFixedArity = 3;
      G__3482.cljs$lang$applyTo = G__3482__3485.cljs$lang$applyTo;
      return G__3482
    }()
  };
  fnil = function(f, x, y, z) {
    switch(arguments.length) {
      case 2:
        return fnil__3463.call(this, f, x);
      case 3:
        return fnil__3464.call(this, f, x, y);
      case 4:
        return fnil__3465.call(this, f, x, y, z)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return fnil
}();
cljs.core.map_indexed = function map_indexed(f, coll) {
  var mapi__3491 = function mpi(idx, coll) {
    return new cljs.core.LazySeq(null, false, function() {
      var temp__3698__auto____3489 = cljs.core.seq.call(null, coll);
      if(cljs.core.truth_(temp__3698__auto____3489)) {
        var s__3490 = temp__3698__auto____3489;
        return cljs.core.cons.call(null, f.call(null, idx, cljs.core.first.call(null, s__3490)), mpi.call(null, idx + 1, cljs.core.rest.call(null, s__3490)))
      }else {
        return null
      }
    })
  };
  return mapi__3491.call(null, 0, coll)
};
cljs.core.keep = function keep(f, coll) {
  return new cljs.core.LazySeq(null, false, function() {
    var temp__3698__auto____3492 = cljs.core.seq.call(null, coll);
    if(cljs.core.truth_(temp__3698__auto____3492)) {
      var s__3493 = temp__3698__auto____3492;
      var x__3494 = f.call(null, cljs.core.first.call(null, s__3493));
      if(cljs.core.truth_(x__3494 === null)) {
        return keep.call(null, f, cljs.core.rest.call(null, s__3493))
      }else {
        return cljs.core.cons.call(null, x__3494, keep.call(null, f, cljs.core.rest.call(null, s__3493)))
      }
    }else {
      return null
    }
  })
};
cljs.core.keep_indexed = function keep_indexed(f, coll) {
  var keepi__3504 = function kpi(idx, coll) {
    return new cljs.core.LazySeq(null, false, function() {
      var temp__3698__auto____3501 = cljs.core.seq.call(null, coll);
      if(cljs.core.truth_(temp__3698__auto____3501)) {
        var s__3502 = temp__3698__auto____3501;
        var x__3503 = f.call(null, idx, cljs.core.first.call(null, s__3502));
        if(cljs.core.truth_(x__3503 === null)) {
          return kpi.call(null, idx + 1, cljs.core.rest.call(null, s__3502))
        }else {
          return cljs.core.cons.call(null, x__3503, kpi.call(null, idx + 1, cljs.core.rest.call(null, s__3502)))
        }
      }else {
        return null
      }
    })
  };
  return keepi__3504.call(null, 0, coll)
};
cljs.core.every_pred = function() {
  var every_pred = null;
  var every_pred__3549 = function(p) {
    return function() {
      var ep1 = null;
      var ep1__3554 = function() {
        return true
      };
      var ep1__3555 = function(x) {
        return cljs.core.boolean$.call(null, p.call(null, x))
      };
      var ep1__3556 = function(x, y) {
        return cljs.core.boolean$.call(null, function() {
          var and__3546__auto____3511 = p.call(null, x);
          if(cljs.core.truth_(and__3546__auto____3511)) {
            return p.call(null, y)
          }else {
            return and__3546__auto____3511
          }
        }())
      };
      var ep1__3557 = function(x, y, z) {
        return cljs.core.boolean$.call(null, function() {
          var and__3546__auto____3512 = p.call(null, x);
          if(cljs.core.truth_(and__3546__auto____3512)) {
            var and__3546__auto____3513 = p.call(null, y);
            if(cljs.core.truth_(and__3546__auto____3513)) {
              return p.call(null, z)
            }else {
              return and__3546__auto____3513
            }
          }else {
            return and__3546__auto____3512
          }
        }())
      };
      var ep1__3558 = function() {
        var G__3560__delegate = function(x, y, z, args) {
          return cljs.core.boolean$.call(null, function() {
            var and__3546__auto____3514 = ep1.call(null, x, y, z);
            if(cljs.core.truth_(and__3546__auto____3514)) {
              return cljs.core.every_QMARK_.call(null, p, args)
            }else {
              return and__3546__auto____3514
            }
          }())
        };
        var G__3560 = function(x, y, z, var_args) {
          var args = null;
          if(goog.isDef(var_args)) {
            args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
          }
          return G__3560__delegate.call(this, x, y, z, args)
        };
        G__3560.cljs$lang$maxFixedArity = 3;
        G__3560.cljs$lang$applyTo = function(arglist__3561) {
          var x = cljs.core.first(arglist__3561);
          var y = cljs.core.first(cljs.core.next(arglist__3561));
          var z = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3561)));
          var args = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3561)));
          return G__3560__delegate.call(this, x, y, z, args)
        };
        return G__3560
      }();
      ep1 = function(x, y, z, var_args) {
        var args = var_args;
        switch(arguments.length) {
          case 0:
            return ep1__3554.call(this);
          case 1:
            return ep1__3555.call(this, x);
          case 2:
            return ep1__3556.call(this, x, y);
          case 3:
            return ep1__3557.call(this, x, y, z);
          default:
            return ep1__3558.apply(this, arguments)
        }
        throw"Invalid arity: " + arguments.length;
      };
      ep1.cljs$lang$maxFixedArity = 3;
      ep1.cljs$lang$applyTo = ep1__3558.cljs$lang$applyTo;
      return ep1
    }()
  };
  var every_pred__3550 = function(p1, p2) {
    return function() {
      var ep2 = null;
      var ep2__3562 = function() {
        return true
      };
      var ep2__3563 = function(x) {
        return cljs.core.boolean$.call(null, function() {
          var and__3546__auto____3515 = p1.call(null, x);
          if(cljs.core.truth_(and__3546__auto____3515)) {
            return p2.call(null, x)
          }else {
            return and__3546__auto____3515
          }
        }())
      };
      var ep2__3564 = function(x, y) {
        return cljs.core.boolean$.call(null, function() {
          var and__3546__auto____3516 = p1.call(null, x);
          if(cljs.core.truth_(and__3546__auto____3516)) {
            var and__3546__auto____3517 = p1.call(null, y);
            if(cljs.core.truth_(and__3546__auto____3517)) {
              var and__3546__auto____3518 = p2.call(null, x);
              if(cljs.core.truth_(and__3546__auto____3518)) {
                return p2.call(null, y)
              }else {
                return and__3546__auto____3518
              }
            }else {
              return and__3546__auto____3517
            }
          }else {
            return and__3546__auto____3516
          }
        }())
      };
      var ep2__3565 = function(x, y, z) {
        return cljs.core.boolean$.call(null, function() {
          var and__3546__auto____3519 = p1.call(null, x);
          if(cljs.core.truth_(and__3546__auto____3519)) {
            var and__3546__auto____3520 = p1.call(null, y);
            if(cljs.core.truth_(and__3546__auto____3520)) {
              var and__3546__auto____3521 = p1.call(null, z);
              if(cljs.core.truth_(and__3546__auto____3521)) {
                var and__3546__auto____3522 = p2.call(null, x);
                if(cljs.core.truth_(and__3546__auto____3522)) {
                  var and__3546__auto____3523 = p2.call(null, y);
                  if(cljs.core.truth_(and__3546__auto____3523)) {
                    return p2.call(null, z)
                  }else {
                    return and__3546__auto____3523
                  }
                }else {
                  return and__3546__auto____3522
                }
              }else {
                return and__3546__auto____3521
              }
            }else {
              return and__3546__auto____3520
            }
          }else {
            return and__3546__auto____3519
          }
        }())
      };
      var ep2__3566 = function() {
        var G__3568__delegate = function(x, y, z, args) {
          return cljs.core.boolean$.call(null, function() {
            var and__3546__auto____3524 = ep2.call(null, x, y, z);
            if(cljs.core.truth_(and__3546__auto____3524)) {
              return cljs.core.every_QMARK_.call(null, function(p1__3495_SHARP_) {
                var and__3546__auto____3525 = p1.call(null, p1__3495_SHARP_);
                if(cljs.core.truth_(and__3546__auto____3525)) {
                  return p2.call(null, p1__3495_SHARP_)
                }else {
                  return and__3546__auto____3525
                }
              }, args)
            }else {
              return and__3546__auto____3524
            }
          }())
        };
        var G__3568 = function(x, y, z, var_args) {
          var args = null;
          if(goog.isDef(var_args)) {
            args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
          }
          return G__3568__delegate.call(this, x, y, z, args)
        };
        G__3568.cljs$lang$maxFixedArity = 3;
        G__3568.cljs$lang$applyTo = function(arglist__3569) {
          var x = cljs.core.first(arglist__3569);
          var y = cljs.core.first(cljs.core.next(arglist__3569));
          var z = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3569)));
          var args = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3569)));
          return G__3568__delegate.call(this, x, y, z, args)
        };
        return G__3568
      }();
      ep2 = function(x, y, z, var_args) {
        var args = var_args;
        switch(arguments.length) {
          case 0:
            return ep2__3562.call(this);
          case 1:
            return ep2__3563.call(this, x);
          case 2:
            return ep2__3564.call(this, x, y);
          case 3:
            return ep2__3565.call(this, x, y, z);
          default:
            return ep2__3566.apply(this, arguments)
        }
        throw"Invalid arity: " + arguments.length;
      };
      ep2.cljs$lang$maxFixedArity = 3;
      ep2.cljs$lang$applyTo = ep2__3566.cljs$lang$applyTo;
      return ep2
    }()
  };
  var every_pred__3551 = function(p1, p2, p3) {
    return function() {
      var ep3 = null;
      var ep3__3570 = function() {
        return true
      };
      var ep3__3571 = function(x) {
        return cljs.core.boolean$.call(null, function() {
          var and__3546__auto____3526 = p1.call(null, x);
          if(cljs.core.truth_(and__3546__auto____3526)) {
            var and__3546__auto____3527 = p2.call(null, x);
            if(cljs.core.truth_(and__3546__auto____3527)) {
              return p3.call(null, x)
            }else {
              return and__3546__auto____3527
            }
          }else {
            return and__3546__auto____3526
          }
        }())
      };
      var ep3__3572 = function(x, y) {
        return cljs.core.boolean$.call(null, function() {
          var and__3546__auto____3528 = p1.call(null, x);
          if(cljs.core.truth_(and__3546__auto____3528)) {
            var and__3546__auto____3529 = p2.call(null, x);
            if(cljs.core.truth_(and__3546__auto____3529)) {
              var and__3546__auto____3530 = p3.call(null, x);
              if(cljs.core.truth_(and__3546__auto____3530)) {
                var and__3546__auto____3531 = p1.call(null, y);
                if(cljs.core.truth_(and__3546__auto____3531)) {
                  var and__3546__auto____3532 = p2.call(null, y);
                  if(cljs.core.truth_(and__3546__auto____3532)) {
                    return p3.call(null, y)
                  }else {
                    return and__3546__auto____3532
                  }
                }else {
                  return and__3546__auto____3531
                }
              }else {
                return and__3546__auto____3530
              }
            }else {
              return and__3546__auto____3529
            }
          }else {
            return and__3546__auto____3528
          }
        }())
      };
      var ep3__3573 = function(x, y, z) {
        return cljs.core.boolean$.call(null, function() {
          var and__3546__auto____3533 = p1.call(null, x);
          if(cljs.core.truth_(and__3546__auto____3533)) {
            var and__3546__auto____3534 = p2.call(null, x);
            if(cljs.core.truth_(and__3546__auto____3534)) {
              var and__3546__auto____3535 = p3.call(null, x);
              if(cljs.core.truth_(and__3546__auto____3535)) {
                var and__3546__auto____3536 = p1.call(null, y);
                if(cljs.core.truth_(and__3546__auto____3536)) {
                  var and__3546__auto____3537 = p2.call(null, y);
                  if(cljs.core.truth_(and__3546__auto____3537)) {
                    var and__3546__auto____3538 = p3.call(null, y);
                    if(cljs.core.truth_(and__3546__auto____3538)) {
                      var and__3546__auto____3539 = p1.call(null, z);
                      if(cljs.core.truth_(and__3546__auto____3539)) {
                        var and__3546__auto____3540 = p2.call(null, z);
                        if(cljs.core.truth_(and__3546__auto____3540)) {
                          return p3.call(null, z)
                        }else {
                          return and__3546__auto____3540
                        }
                      }else {
                        return and__3546__auto____3539
                      }
                    }else {
                      return and__3546__auto____3538
                    }
                  }else {
                    return and__3546__auto____3537
                  }
                }else {
                  return and__3546__auto____3536
                }
              }else {
                return and__3546__auto____3535
              }
            }else {
              return and__3546__auto____3534
            }
          }else {
            return and__3546__auto____3533
          }
        }())
      };
      var ep3__3574 = function() {
        var G__3576__delegate = function(x, y, z, args) {
          return cljs.core.boolean$.call(null, function() {
            var and__3546__auto____3541 = ep3.call(null, x, y, z);
            if(cljs.core.truth_(and__3546__auto____3541)) {
              return cljs.core.every_QMARK_.call(null, function(p1__3496_SHARP_) {
                var and__3546__auto____3542 = p1.call(null, p1__3496_SHARP_);
                if(cljs.core.truth_(and__3546__auto____3542)) {
                  var and__3546__auto____3543 = p2.call(null, p1__3496_SHARP_);
                  if(cljs.core.truth_(and__3546__auto____3543)) {
                    return p3.call(null, p1__3496_SHARP_)
                  }else {
                    return and__3546__auto____3543
                  }
                }else {
                  return and__3546__auto____3542
                }
              }, args)
            }else {
              return and__3546__auto____3541
            }
          }())
        };
        var G__3576 = function(x, y, z, var_args) {
          var args = null;
          if(goog.isDef(var_args)) {
            args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
          }
          return G__3576__delegate.call(this, x, y, z, args)
        };
        G__3576.cljs$lang$maxFixedArity = 3;
        G__3576.cljs$lang$applyTo = function(arglist__3577) {
          var x = cljs.core.first(arglist__3577);
          var y = cljs.core.first(cljs.core.next(arglist__3577));
          var z = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3577)));
          var args = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3577)));
          return G__3576__delegate.call(this, x, y, z, args)
        };
        return G__3576
      }();
      ep3 = function(x, y, z, var_args) {
        var args = var_args;
        switch(arguments.length) {
          case 0:
            return ep3__3570.call(this);
          case 1:
            return ep3__3571.call(this, x);
          case 2:
            return ep3__3572.call(this, x, y);
          case 3:
            return ep3__3573.call(this, x, y, z);
          default:
            return ep3__3574.apply(this, arguments)
        }
        throw"Invalid arity: " + arguments.length;
      };
      ep3.cljs$lang$maxFixedArity = 3;
      ep3.cljs$lang$applyTo = ep3__3574.cljs$lang$applyTo;
      return ep3
    }()
  };
  var every_pred__3552 = function() {
    var G__3578__delegate = function(p1, p2, p3, ps) {
      var ps__3544 = cljs.core.list_STAR_.call(null, p1, p2, p3, ps);
      return function() {
        var epn = null;
        var epn__3579 = function() {
          return true
        };
        var epn__3580 = function(x) {
          return cljs.core.every_QMARK_.call(null, function(p1__3497_SHARP_) {
            return p1__3497_SHARP_.call(null, x)
          }, ps__3544)
        };
        var epn__3581 = function(x, y) {
          return cljs.core.every_QMARK_.call(null, function(p1__3498_SHARP_) {
            var and__3546__auto____3545 = p1__3498_SHARP_.call(null, x);
            if(cljs.core.truth_(and__3546__auto____3545)) {
              return p1__3498_SHARP_.call(null, y)
            }else {
              return and__3546__auto____3545
            }
          }, ps__3544)
        };
        var epn__3582 = function(x, y, z) {
          return cljs.core.every_QMARK_.call(null, function(p1__3499_SHARP_) {
            var and__3546__auto____3546 = p1__3499_SHARP_.call(null, x);
            if(cljs.core.truth_(and__3546__auto____3546)) {
              var and__3546__auto____3547 = p1__3499_SHARP_.call(null, y);
              if(cljs.core.truth_(and__3546__auto____3547)) {
                return p1__3499_SHARP_.call(null, z)
              }else {
                return and__3546__auto____3547
              }
            }else {
              return and__3546__auto____3546
            }
          }, ps__3544)
        };
        var epn__3583 = function() {
          var G__3585__delegate = function(x, y, z, args) {
            return cljs.core.boolean$.call(null, function() {
              var and__3546__auto____3548 = epn.call(null, x, y, z);
              if(cljs.core.truth_(and__3546__auto____3548)) {
                return cljs.core.every_QMARK_.call(null, function(p1__3500_SHARP_) {
                  return cljs.core.every_QMARK_.call(null, p1__3500_SHARP_, args)
                }, ps__3544)
              }else {
                return and__3546__auto____3548
              }
            }())
          };
          var G__3585 = function(x, y, z, var_args) {
            var args = null;
            if(goog.isDef(var_args)) {
              args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
            }
            return G__3585__delegate.call(this, x, y, z, args)
          };
          G__3585.cljs$lang$maxFixedArity = 3;
          G__3585.cljs$lang$applyTo = function(arglist__3586) {
            var x = cljs.core.first(arglist__3586);
            var y = cljs.core.first(cljs.core.next(arglist__3586));
            var z = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3586)));
            var args = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3586)));
            return G__3585__delegate.call(this, x, y, z, args)
          };
          return G__3585
        }();
        epn = function(x, y, z, var_args) {
          var args = var_args;
          switch(arguments.length) {
            case 0:
              return epn__3579.call(this);
            case 1:
              return epn__3580.call(this, x);
            case 2:
              return epn__3581.call(this, x, y);
            case 3:
              return epn__3582.call(this, x, y, z);
            default:
              return epn__3583.apply(this, arguments)
          }
          throw"Invalid arity: " + arguments.length;
        };
        epn.cljs$lang$maxFixedArity = 3;
        epn.cljs$lang$applyTo = epn__3583.cljs$lang$applyTo;
        return epn
      }()
    };
    var G__3578 = function(p1, p2, p3, var_args) {
      var ps = null;
      if(goog.isDef(var_args)) {
        ps = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
      }
      return G__3578__delegate.call(this, p1, p2, p3, ps)
    };
    G__3578.cljs$lang$maxFixedArity = 3;
    G__3578.cljs$lang$applyTo = function(arglist__3587) {
      var p1 = cljs.core.first(arglist__3587);
      var p2 = cljs.core.first(cljs.core.next(arglist__3587));
      var p3 = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3587)));
      var ps = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3587)));
      return G__3578__delegate.call(this, p1, p2, p3, ps)
    };
    return G__3578
  }();
  every_pred = function(p1, p2, p3, var_args) {
    var ps = var_args;
    switch(arguments.length) {
      case 1:
        return every_pred__3549.call(this, p1);
      case 2:
        return every_pred__3550.call(this, p1, p2);
      case 3:
        return every_pred__3551.call(this, p1, p2, p3);
      default:
        return every_pred__3552.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  every_pred.cljs$lang$maxFixedArity = 3;
  every_pred.cljs$lang$applyTo = every_pred__3552.cljs$lang$applyTo;
  return every_pred
}();
cljs.core.some_fn = function() {
  var some_fn = null;
  var some_fn__3627 = function(p) {
    return function() {
      var sp1 = null;
      var sp1__3632 = function() {
        return null
      };
      var sp1__3633 = function(x) {
        return p.call(null, x)
      };
      var sp1__3634 = function(x, y) {
        var or__3548__auto____3589 = p.call(null, x);
        if(cljs.core.truth_(or__3548__auto____3589)) {
          return or__3548__auto____3589
        }else {
          return p.call(null, y)
        }
      };
      var sp1__3635 = function(x, y, z) {
        var or__3548__auto____3590 = p.call(null, x);
        if(cljs.core.truth_(or__3548__auto____3590)) {
          return or__3548__auto____3590
        }else {
          var or__3548__auto____3591 = p.call(null, y);
          if(cljs.core.truth_(or__3548__auto____3591)) {
            return or__3548__auto____3591
          }else {
            return p.call(null, z)
          }
        }
      };
      var sp1__3636 = function() {
        var G__3638__delegate = function(x, y, z, args) {
          var or__3548__auto____3592 = sp1.call(null, x, y, z);
          if(cljs.core.truth_(or__3548__auto____3592)) {
            return or__3548__auto____3592
          }else {
            return cljs.core.some.call(null, p, args)
          }
        };
        var G__3638 = function(x, y, z, var_args) {
          var args = null;
          if(goog.isDef(var_args)) {
            args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
          }
          return G__3638__delegate.call(this, x, y, z, args)
        };
        G__3638.cljs$lang$maxFixedArity = 3;
        G__3638.cljs$lang$applyTo = function(arglist__3639) {
          var x = cljs.core.first(arglist__3639);
          var y = cljs.core.first(cljs.core.next(arglist__3639));
          var z = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3639)));
          var args = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3639)));
          return G__3638__delegate.call(this, x, y, z, args)
        };
        return G__3638
      }();
      sp1 = function(x, y, z, var_args) {
        var args = var_args;
        switch(arguments.length) {
          case 0:
            return sp1__3632.call(this);
          case 1:
            return sp1__3633.call(this, x);
          case 2:
            return sp1__3634.call(this, x, y);
          case 3:
            return sp1__3635.call(this, x, y, z);
          default:
            return sp1__3636.apply(this, arguments)
        }
        throw"Invalid arity: " + arguments.length;
      };
      sp1.cljs$lang$maxFixedArity = 3;
      sp1.cljs$lang$applyTo = sp1__3636.cljs$lang$applyTo;
      return sp1
    }()
  };
  var some_fn__3628 = function(p1, p2) {
    return function() {
      var sp2 = null;
      var sp2__3640 = function() {
        return null
      };
      var sp2__3641 = function(x) {
        var or__3548__auto____3593 = p1.call(null, x);
        if(cljs.core.truth_(or__3548__auto____3593)) {
          return or__3548__auto____3593
        }else {
          return p2.call(null, x)
        }
      };
      var sp2__3642 = function(x, y) {
        var or__3548__auto____3594 = p1.call(null, x);
        if(cljs.core.truth_(or__3548__auto____3594)) {
          return or__3548__auto____3594
        }else {
          var or__3548__auto____3595 = p1.call(null, y);
          if(cljs.core.truth_(or__3548__auto____3595)) {
            return or__3548__auto____3595
          }else {
            var or__3548__auto____3596 = p2.call(null, x);
            if(cljs.core.truth_(or__3548__auto____3596)) {
              return or__3548__auto____3596
            }else {
              return p2.call(null, y)
            }
          }
        }
      };
      var sp2__3643 = function(x, y, z) {
        var or__3548__auto____3597 = p1.call(null, x);
        if(cljs.core.truth_(or__3548__auto____3597)) {
          return or__3548__auto____3597
        }else {
          var or__3548__auto____3598 = p1.call(null, y);
          if(cljs.core.truth_(or__3548__auto____3598)) {
            return or__3548__auto____3598
          }else {
            var or__3548__auto____3599 = p1.call(null, z);
            if(cljs.core.truth_(or__3548__auto____3599)) {
              return or__3548__auto____3599
            }else {
              var or__3548__auto____3600 = p2.call(null, x);
              if(cljs.core.truth_(or__3548__auto____3600)) {
                return or__3548__auto____3600
              }else {
                var or__3548__auto____3601 = p2.call(null, y);
                if(cljs.core.truth_(or__3548__auto____3601)) {
                  return or__3548__auto____3601
                }else {
                  return p2.call(null, z)
                }
              }
            }
          }
        }
      };
      var sp2__3644 = function() {
        var G__3646__delegate = function(x, y, z, args) {
          var or__3548__auto____3602 = sp2.call(null, x, y, z);
          if(cljs.core.truth_(or__3548__auto____3602)) {
            return or__3548__auto____3602
          }else {
            return cljs.core.some.call(null, function(p1__3505_SHARP_) {
              var or__3548__auto____3603 = p1.call(null, p1__3505_SHARP_);
              if(cljs.core.truth_(or__3548__auto____3603)) {
                return or__3548__auto____3603
              }else {
                return p2.call(null, p1__3505_SHARP_)
              }
            }, args)
          }
        };
        var G__3646 = function(x, y, z, var_args) {
          var args = null;
          if(goog.isDef(var_args)) {
            args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
          }
          return G__3646__delegate.call(this, x, y, z, args)
        };
        G__3646.cljs$lang$maxFixedArity = 3;
        G__3646.cljs$lang$applyTo = function(arglist__3647) {
          var x = cljs.core.first(arglist__3647);
          var y = cljs.core.first(cljs.core.next(arglist__3647));
          var z = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3647)));
          var args = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3647)));
          return G__3646__delegate.call(this, x, y, z, args)
        };
        return G__3646
      }();
      sp2 = function(x, y, z, var_args) {
        var args = var_args;
        switch(arguments.length) {
          case 0:
            return sp2__3640.call(this);
          case 1:
            return sp2__3641.call(this, x);
          case 2:
            return sp2__3642.call(this, x, y);
          case 3:
            return sp2__3643.call(this, x, y, z);
          default:
            return sp2__3644.apply(this, arguments)
        }
        throw"Invalid arity: " + arguments.length;
      };
      sp2.cljs$lang$maxFixedArity = 3;
      sp2.cljs$lang$applyTo = sp2__3644.cljs$lang$applyTo;
      return sp2
    }()
  };
  var some_fn__3629 = function(p1, p2, p3) {
    return function() {
      var sp3 = null;
      var sp3__3648 = function() {
        return null
      };
      var sp3__3649 = function(x) {
        var or__3548__auto____3604 = p1.call(null, x);
        if(cljs.core.truth_(or__3548__auto____3604)) {
          return or__3548__auto____3604
        }else {
          var or__3548__auto____3605 = p2.call(null, x);
          if(cljs.core.truth_(or__3548__auto____3605)) {
            return or__3548__auto____3605
          }else {
            return p3.call(null, x)
          }
        }
      };
      var sp3__3650 = function(x, y) {
        var or__3548__auto____3606 = p1.call(null, x);
        if(cljs.core.truth_(or__3548__auto____3606)) {
          return or__3548__auto____3606
        }else {
          var or__3548__auto____3607 = p2.call(null, x);
          if(cljs.core.truth_(or__3548__auto____3607)) {
            return or__3548__auto____3607
          }else {
            var or__3548__auto____3608 = p3.call(null, x);
            if(cljs.core.truth_(or__3548__auto____3608)) {
              return or__3548__auto____3608
            }else {
              var or__3548__auto____3609 = p1.call(null, y);
              if(cljs.core.truth_(or__3548__auto____3609)) {
                return or__3548__auto____3609
              }else {
                var or__3548__auto____3610 = p2.call(null, y);
                if(cljs.core.truth_(or__3548__auto____3610)) {
                  return or__3548__auto____3610
                }else {
                  return p3.call(null, y)
                }
              }
            }
          }
        }
      };
      var sp3__3651 = function(x, y, z) {
        var or__3548__auto____3611 = p1.call(null, x);
        if(cljs.core.truth_(or__3548__auto____3611)) {
          return or__3548__auto____3611
        }else {
          var or__3548__auto____3612 = p2.call(null, x);
          if(cljs.core.truth_(or__3548__auto____3612)) {
            return or__3548__auto____3612
          }else {
            var or__3548__auto____3613 = p3.call(null, x);
            if(cljs.core.truth_(or__3548__auto____3613)) {
              return or__3548__auto____3613
            }else {
              var or__3548__auto____3614 = p1.call(null, y);
              if(cljs.core.truth_(or__3548__auto____3614)) {
                return or__3548__auto____3614
              }else {
                var or__3548__auto____3615 = p2.call(null, y);
                if(cljs.core.truth_(or__3548__auto____3615)) {
                  return or__3548__auto____3615
                }else {
                  var or__3548__auto____3616 = p3.call(null, y);
                  if(cljs.core.truth_(or__3548__auto____3616)) {
                    return or__3548__auto____3616
                  }else {
                    var or__3548__auto____3617 = p1.call(null, z);
                    if(cljs.core.truth_(or__3548__auto____3617)) {
                      return or__3548__auto____3617
                    }else {
                      var or__3548__auto____3618 = p2.call(null, z);
                      if(cljs.core.truth_(or__3548__auto____3618)) {
                        return or__3548__auto____3618
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
      var sp3__3652 = function() {
        var G__3654__delegate = function(x, y, z, args) {
          var or__3548__auto____3619 = sp3.call(null, x, y, z);
          if(cljs.core.truth_(or__3548__auto____3619)) {
            return or__3548__auto____3619
          }else {
            return cljs.core.some.call(null, function(p1__3506_SHARP_) {
              var or__3548__auto____3620 = p1.call(null, p1__3506_SHARP_);
              if(cljs.core.truth_(or__3548__auto____3620)) {
                return or__3548__auto____3620
              }else {
                var or__3548__auto____3621 = p2.call(null, p1__3506_SHARP_);
                if(cljs.core.truth_(or__3548__auto____3621)) {
                  return or__3548__auto____3621
                }else {
                  return p3.call(null, p1__3506_SHARP_)
                }
              }
            }, args)
          }
        };
        var G__3654 = function(x, y, z, var_args) {
          var args = null;
          if(goog.isDef(var_args)) {
            args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
          }
          return G__3654__delegate.call(this, x, y, z, args)
        };
        G__3654.cljs$lang$maxFixedArity = 3;
        G__3654.cljs$lang$applyTo = function(arglist__3655) {
          var x = cljs.core.first(arglist__3655);
          var y = cljs.core.first(cljs.core.next(arglist__3655));
          var z = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3655)));
          var args = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3655)));
          return G__3654__delegate.call(this, x, y, z, args)
        };
        return G__3654
      }();
      sp3 = function(x, y, z, var_args) {
        var args = var_args;
        switch(arguments.length) {
          case 0:
            return sp3__3648.call(this);
          case 1:
            return sp3__3649.call(this, x);
          case 2:
            return sp3__3650.call(this, x, y);
          case 3:
            return sp3__3651.call(this, x, y, z);
          default:
            return sp3__3652.apply(this, arguments)
        }
        throw"Invalid arity: " + arguments.length;
      };
      sp3.cljs$lang$maxFixedArity = 3;
      sp3.cljs$lang$applyTo = sp3__3652.cljs$lang$applyTo;
      return sp3
    }()
  };
  var some_fn__3630 = function() {
    var G__3656__delegate = function(p1, p2, p3, ps) {
      var ps__3622 = cljs.core.list_STAR_.call(null, p1, p2, p3, ps);
      return function() {
        var spn = null;
        var spn__3657 = function() {
          return null
        };
        var spn__3658 = function(x) {
          return cljs.core.some.call(null, function(p1__3507_SHARP_) {
            return p1__3507_SHARP_.call(null, x)
          }, ps__3622)
        };
        var spn__3659 = function(x, y) {
          return cljs.core.some.call(null, function(p1__3508_SHARP_) {
            var or__3548__auto____3623 = p1__3508_SHARP_.call(null, x);
            if(cljs.core.truth_(or__3548__auto____3623)) {
              return or__3548__auto____3623
            }else {
              return p1__3508_SHARP_.call(null, y)
            }
          }, ps__3622)
        };
        var spn__3660 = function(x, y, z) {
          return cljs.core.some.call(null, function(p1__3509_SHARP_) {
            var or__3548__auto____3624 = p1__3509_SHARP_.call(null, x);
            if(cljs.core.truth_(or__3548__auto____3624)) {
              return or__3548__auto____3624
            }else {
              var or__3548__auto____3625 = p1__3509_SHARP_.call(null, y);
              if(cljs.core.truth_(or__3548__auto____3625)) {
                return or__3548__auto____3625
              }else {
                return p1__3509_SHARP_.call(null, z)
              }
            }
          }, ps__3622)
        };
        var spn__3661 = function() {
          var G__3663__delegate = function(x, y, z, args) {
            var or__3548__auto____3626 = spn.call(null, x, y, z);
            if(cljs.core.truth_(or__3548__auto____3626)) {
              return or__3548__auto____3626
            }else {
              return cljs.core.some.call(null, function(p1__3510_SHARP_) {
                return cljs.core.some.call(null, p1__3510_SHARP_, args)
              }, ps__3622)
            }
          };
          var G__3663 = function(x, y, z, var_args) {
            var args = null;
            if(goog.isDef(var_args)) {
              args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
            }
            return G__3663__delegate.call(this, x, y, z, args)
          };
          G__3663.cljs$lang$maxFixedArity = 3;
          G__3663.cljs$lang$applyTo = function(arglist__3664) {
            var x = cljs.core.first(arglist__3664);
            var y = cljs.core.first(cljs.core.next(arglist__3664));
            var z = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3664)));
            var args = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3664)));
            return G__3663__delegate.call(this, x, y, z, args)
          };
          return G__3663
        }();
        spn = function(x, y, z, var_args) {
          var args = var_args;
          switch(arguments.length) {
            case 0:
              return spn__3657.call(this);
            case 1:
              return spn__3658.call(this, x);
            case 2:
              return spn__3659.call(this, x, y);
            case 3:
              return spn__3660.call(this, x, y, z);
            default:
              return spn__3661.apply(this, arguments)
          }
          throw"Invalid arity: " + arguments.length;
        };
        spn.cljs$lang$maxFixedArity = 3;
        spn.cljs$lang$applyTo = spn__3661.cljs$lang$applyTo;
        return spn
      }()
    };
    var G__3656 = function(p1, p2, p3, var_args) {
      var ps = null;
      if(goog.isDef(var_args)) {
        ps = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
      }
      return G__3656__delegate.call(this, p1, p2, p3, ps)
    };
    G__3656.cljs$lang$maxFixedArity = 3;
    G__3656.cljs$lang$applyTo = function(arglist__3665) {
      var p1 = cljs.core.first(arglist__3665);
      var p2 = cljs.core.first(cljs.core.next(arglist__3665));
      var p3 = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3665)));
      var ps = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3665)));
      return G__3656__delegate.call(this, p1, p2, p3, ps)
    };
    return G__3656
  }();
  some_fn = function(p1, p2, p3, var_args) {
    var ps = var_args;
    switch(arguments.length) {
      case 1:
        return some_fn__3627.call(this, p1);
      case 2:
        return some_fn__3628.call(this, p1, p2);
      case 3:
        return some_fn__3629.call(this, p1, p2, p3);
      default:
        return some_fn__3630.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  some_fn.cljs$lang$maxFixedArity = 3;
  some_fn.cljs$lang$applyTo = some_fn__3630.cljs$lang$applyTo;
  return some_fn
}();
cljs.core.map = function() {
  var map = null;
  var map__3678 = function(f, coll) {
    return new cljs.core.LazySeq(null, false, function() {
      var temp__3698__auto____3666 = cljs.core.seq.call(null, coll);
      if(cljs.core.truth_(temp__3698__auto____3666)) {
        var s__3667 = temp__3698__auto____3666;
        return cljs.core.cons.call(null, f.call(null, cljs.core.first.call(null, s__3667)), map.call(null, f, cljs.core.rest.call(null, s__3667)))
      }else {
        return null
      }
    })
  };
  var map__3679 = function(f, c1, c2) {
    return new cljs.core.LazySeq(null, false, function() {
      var s1__3668 = cljs.core.seq.call(null, c1);
      var s2__3669 = cljs.core.seq.call(null, c2);
      if(cljs.core.truth_(function() {
        var and__3546__auto____3670 = s1__3668;
        if(cljs.core.truth_(and__3546__auto____3670)) {
          return s2__3669
        }else {
          return and__3546__auto____3670
        }
      }())) {
        return cljs.core.cons.call(null, f.call(null, cljs.core.first.call(null, s1__3668), cljs.core.first.call(null, s2__3669)), map.call(null, f, cljs.core.rest.call(null, s1__3668), cljs.core.rest.call(null, s2__3669)))
      }else {
        return null
      }
    })
  };
  var map__3680 = function(f, c1, c2, c3) {
    return new cljs.core.LazySeq(null, false, function() {
      var s1__3671 = cljs.core.seq.call(null, c1);
      var s2__3672 = cljs.core.seq.call(null, c2);
      var s3__3673 = cljs.core.seq.call(null, c3);
      if(cljs.core.truth_(function() {
        var and__3546__auto____3674 = s1__3671;
        if(cljs.core.truth_(and__3546__auto____3674)) {
          var and__3546__auto____3675 = s2__3672;
          if(cljs.core.truth_(and__3546__auto____3675)) {
            return s3__3673
          }else {
            return and__3546__auto____3675
          }
        }else {
          return and__3546__auto____3674
        }
      }())) {
        return cljs.core.cons.call(null, f.call(null, cljs.core.first.call(null, s1__3671), cljs.core.first.call(null, s2__3672), cljs.core.first.call(null, s3__3673)), map.call(null, f, cljs.core.rest.call(null, s1__3671), cljs.core.rest.call(null, s2__3672), cljs.core.rest.call(null, s3__3673)))
      }else {
        return null
      }
    })
  };
  var map__3681 = function() {
    var G__3683__delegate = function(f, c1, c2, c3, colls) {
      var step__3677 = function step(cs) {
        return new cljs.core.LazySeq(null, false, function() {
          var ss__3676 = map.call(null, cljs.core.seq, cs);
          if(cljs.core.truth_(cljs.core.every_QMARK_.call(null, cljs.core.identity, ss__3676))) {
            return cljs.core.cons.call(null, map.call(null, cljs.core.first, ss__3676), step.call(null, map.call(null, cljs.core.rest, ss__3676)))
          }else {
            return null
          }
        })
      };
      return map.call(null, function(p1__3588_SHARP_) {
        return cljs.core.apply.call(null, f, p1__3588_SHARP_)
      }, step__3677.call(null, cljs.core.conj.call(null, colls, c3, c2, c1)))
    };
    var G__3683 = function(f, c1, c2, c3, var_args) {
      var colls = null;
      if(goog.isDef(var_args)) {
        colls = cljs.core.array_seq(Array.prototype.slice.call(arguments, 4), 0)
      }
      return G__3683__delegate.call(this, f, c1, c2, c3, colls)
    };
    G__3683.cljs$lang$maxFixedArity = 4;
    G__3683.cljs$lang$applyTo = function(arglist__3684) {
      var f = cljs.core.first(arglist__3684);
      var c1 = cljs.core.first(cljs.core.next(arglist__3684));
      var c2 = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3684)));
      var c3 = cljs.core.first(cljs.core.next(cljs.core.next(cljs.core.next(arglist__3684))));
      var colls = cljs.core.rest(cljs.core.next(cljs.core.next(cljs.core.next(arglist__3684))));
      return G__3683__delegate.call(this, f, c1, c2, c3, colls)
    };
    return G__3683
  }();
  map = function(f, c1, c2, c3, var_args) {
    var colls = var_args;
    switch(arguments.length) {
      case 2:
        return map__3678.call(this, f, c1);
      case 3:
        return map__3679.call(this, f, c1, c2);
      case 4:
        return map__3680.call(this, f, c1, c2, c3);
      default:
        return map__3681.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  map.cljs$lang$maxFixedArity = 4;
  map.cljs$lang$applyTo = map__3681.cljs$lang$applyTo;
  return map
}();
cljs.core.take = function take(n, coll) {
  return new cljs.core.LazySeq(null, false, function() {
    if(cljs.core.truth_(n > 0)) {
      var temp__3698__auto____3685 = cljs.core.seq.call(null, coll);
      if(cljs.core.truth_(temp__3698__auto____3685)) {
        var s__3686 = temp__3698__auto____3685;
        return cljs.core.cons.call(null, cljs.core.first.call(null, s__3686), take.call(null, n - 1, cljs.core.rest.call(null, s__3686)))
      }else {
        return null
      }
    }else {
      return null
    }
  })
};
cljs.core.drop = function drop(n, coll) {
  var step__3689 = function(n, coll) {
    while(true) {
      var s__3687 = cljs.core.seq.call(null, coll);
      if(cljs.core.truth_(function() {
        var and__3546__auto____3688 = n > 0;
        if(cljs.core.truth_(and__3546__auto____3688)) {
          return s__3687
        }else {
          return and__3546__auto____3688
        }
      }())) {
        var G__3690 = n - 1;
        var G__3691 = cljs.core.rest.call(null, s__3687);
        n = G__3690;
        coll = G__3691;
        continue
      }else {
        return s__3687
      }
      break
    }
  };
  return new cljs.core.LazySeq(null, false, function() {
    return step__3689.call(null, n, coll)
  })
};
cljs.core.drop_last = function() {
  var drop_last = null;
  var drop_last__3692 = function(s) {
    return drop_last.call(null, 1, s)
  };
  var drop_last__3693 = function(n, s) {
    return cljs.core.map.call(null, function(x, _) {
      return x
    }, s, cljs.core.drop.call(null, n, s))
  };
  drop_last = function(n, s) {
    switch(arguments.length) {
      case 1:
        return drop_last__3692.call(this, n);
      case 2:
        return drop_last__3693.call(this, n, s)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return drop_last
}();
cljs.core.take_last = function take_last(n, coll) {
  var s__3695 = cljs.core.seq.call(null, coll);
  var lead__3696 = cljs.core.seq.call(null, cljs.core.drop.call(null, n, coll));
  while(true) {
    if(cljs.core.truth_(lead__3696)) {
      var G__3697 = cljs.core.next.call(null, s__3695);
      var G__3698 = cljs.core.next.call(null, lead__3696);
      s__3695 = G__3697;
      lead__3696 = G__3698;
      continue
    }else {
      return s__3695
    }
    break
  }
};
cljs.core.drop_while = function drop_while(pred, coll) {
  var step__3701 = function(pred, coll) {
    while(true) {
      var s__3699 = cljs.core.seq.call(null, coll);
      if(cljs.core.truth_(function() {
        var and__3546__auto____3700 = s__3699;
        if(cljs.core.truth_(and__3546__auto____3700)) {
          return pred.call(null, cljs.core.first.call(null, s__3699))
        }else {
          return and__3546__auto____3700
        }
      }())) {
        var G__3702 = pred;
        var G__3703 = cljs.core.rest.call(null, s__3699);
        pred = G__3702;
        coll = G__3703;
        continue
      }else {
        return s__3699
      }
      break
    }
  };
  return new cljs.core.LazySeq(null, false, function() {
    return step__3701.call(null, pred, coll)
  })
};
cljs.core.cycle = function cycle(coll) {
  return new cljs.core.LazySeq(null, false, function() {
    var temp__3698__auto____3704 = cljs.core.seq.call(null, coll);
    if(cljs.core.truth_(temp__3698__auto____3704)) {
      var s__3705 = temp__3698__auto____3704;
      return cljs.core.concat.call(null, s__3705, cycle.call(null, s__3705))
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
  var repeat__3706 = function(x) {
    return new cljs.core.LazySeq(null, false, function() {
      return cljs.core.cons.call(null, x, repeat.call(null, x))
    })
  };
  var repeat__3707 = function(n, x) {
    return cljs.core.take.call(null, n, repeat.call(null, x))
  };
  repeat = function(n, x) {
    switch(arguments.length) {
      case 1:
        return repeat__3706.call(this, n);
      case 2:
        return repeat__3707.call(this, n, x)
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
  var repeatedly__3709 = function(f) {
    return new cljs.core.LazySeq(null, false, function() {
      return cljs.core.cons.call(null, f.call(null), repeatedly.call(null, f))
    })
  };
  var repeatedly__3710 = function(n, f) {
    return cljs.core.take.call(null, n, repeatedly.call(null, f))
  };
  repeatedly = function(n, f) {
    switch(arguments.length) {
      case 1:
        return repeatedly__3709.call(this, n);
      case 2:
        return repeatedly__3710.call(this, n, f)
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
  var interleave__3716 = function(c1, c2) {
    return new cljs.core.LazySeq(null, false, function() {
      var s1__3712 = cljs.core.seq.call(null, c1);
      var s2__3713 = cljs.core.seq.call(null, c2);
      if(cljs.core.truth_(function() {
        var and__3546__auto____3714 = s1__3712;
        if(cljs.core.truth_(and__3546__auto____3714)) {
          return s2__3713
        }else {
          return and__3546__auto____3714
        }
      }())) {
        return cljs.core.cons.call(null, cljs.core.first.call(null, s1__3712), cljs.core.cons.call(null, cljs.core.first.call(null, s2__3713), interleave.call(null, cljs.core.rest.call(null, s1__3712), cljs.core.rest.call(null, s2__3713))))
      }else {
        return null
      }
    })
  };
  var interleave__3717 = function() {
    var G__3719__delegate = function(c1, c2, colls) {
      return new cljs.core.LazySeq(null, false, function() {
        var ss__3715 = cljs.core.map.call(null, cljs.core.seq, cljs.core.conj.call(null, colls, c2, c1));
        if(cljs.core.truth_(cljs.core.every_QMARK_.call(null, cljs.core.identity, ss__3715))) {
          return cljs.core.concat.call(null, cljs.core.map.call(null, cljs.core.first, ss__3715), cljs.core.apply.call(null, interleave, cljs.core.map.call(null, cljs.core.rest, ss__3715)))
        }else {
          return null
        }
      })
    };
    var G__3719 = function(c1, c2, var_args) {
      var colls = null;
      if(goog.isDef(var_args)) {
        colls = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3719__delegate.call(this, c1, c2, colls)
    };
    G__3719.cljs$lang$maxFixedArity = 2;
    G__3719.cljs$lang$applyTo = function(arglist__3720) {
      var c1 = cljs.core.first(arglist__3720);
      var c2 = cljs.core.first(cljs.core.next(arglist__3720));
      var colls = cljs.core.rest(cljs.core.next(arglist__3720));
      return G__3719__delegate.call(this, c1, c2, colls)
    };
    return G__3719
  }();
  interleave = function(c1, c2, var_args) {
    var colls = var_args;
    switch(arguments.length) {
      case 2:
        return interleave__3716.call(this, c1, c2);
      default:
        return interleave__3717.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  interleave.cljs$lang$maxFixedArity = 2;
  interleave.cljs$lang$applyTo = interleave__3717.cljs$lang$applyTo;
  return interleave
}();
cljs.core.interpose = function interpose(sep, coll) {
  return cljs.core.drop.call(null, 1, cljs.core.interleave.call(null, cljs.core.repeat.call(null, sep), coll))
};
cljs.core.flatten1 = function flatten1(colls) {
  var cat__3723 = function cat(coll, colls) {
    return new cljs.core.LazySeq(null, false, function() {
      var temp__3695__auto____3721 = cljs.core.seq.call(null, coll);
      if(cljs.core.truth_(temp__3695__auto____3721)) {
        var coll__3722 = temp__3695__auto____3721;
        return cljs.core.cons.call(null, cljs.core.first.call(null, coll__3722), cat.call(null, cljs.core.rest.call(null, coll__3722), colls))
      }else {
        if(cljs.core.truth_(cljs.core.seq.call(null, colls))) {
          return cat.call(null, cljs.core.first.call(null, colls), cljs.core.rest.call(null, colls))
        }else {
          return null
        }
      }
    })
  };
  return cat__3723.call(null, null, colls)
};
cljs.core.mapcat = function() {
  var mapcat = null;
  var mapcat__3724 = function(f, coll) {
    return cljs.core.flatten1.call(null, cljs.core.map.call(null, f, coll))
  };
  var mapcat__3725 = function() {
    var G__3727__delegate = function(f, coll, colls) {
      return cljs.core.flatten1.call(null, cljs.core.apply.call(null, cljs.core.map, f, coll, colls))
    };
    var G__3727 = function(f, coll, var_args) {
      var colls = null;
      if(goog.isDef(var_args)) {
        colls = cljs.core.array_seq(Array.prototype.slice.call(arguments, 2), 0)
      }
      return G__3727__delegate.call(this, f, coll, colls)
    };
    G__3727.cljs$lang$maxFixedArity = 2;
    G__3727.cljs$lang$applyTo = function(arglist__3728) {
      var f = cljs.core.first(arglist__3728);
      var coll = cljs.core.first(cljs.core.next(arglist__3728));
      var colls = cljs.core.rest(cljs.core.next(arglist__3728));
      return G__3727__delegate.call(this, f, coll, colls)
    };
    return G__3727
  }();
  mapcat = function(f, coll, var_args) {
    var colls = var_args;
    switch(arguments.length) {
      case 2:
        return mapcat__3724.call(this, f, coll);
      default:
        return mapcat__3725.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  mapcat.cljs$lang$maxFixedArity = 2;
  mapcat.cljs$lang$applyTo = mapcat__3725.cljs$lang$applyTo;
  return mapcat
}();
cljs.core.filter = function filter(pred, coll) {
  return new cljs.core.LazySeq(null, false, function() {
    var temp__3698__auto____3729 = cljs.core.seq.call(null, coll);
    if(cljs.core.truth_(temp__3698__auto____3729)) {
      var s__3730 = temp__3698__auto____3729;
      var f__3731 = cljs.core.first.call(null, s__3730);
      var r__3732 = cljs.core.rest.call(null, s__3730);
      if(cljs.core.truth_(pred.call(null, f__3731))) {
        return cljs.core.cons.call(null, f__3731, filter.call(null, pred, r__3732))
      }else {
        return filter.call(null, pred, r__3732)
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
  var walk__3734 = function walk(node) {
    return new cljs.core.LazySeq(null, false, function() {
      return cljs.core.cons.call(null, node, cljs.core.truth_(branch_QMARK_.call(null, node)) ? cljs.core.mapcat.call(null, walk, children.call(null, node)) : null)
    })
  };
  return walk__3734.call(null, root)
};
cljs.core.flatten = function flatten(x) {
  return cljs.core.filter.call(null, function(p1__3733_SHARP_) {
    return cljs.core.not.call(null, cljs.core.sequential_QMARK_.call(null, p1__3733_SHARP_))
  }, cljs.core.rest.call(null, cljs.core.tree_seq.call(null, cljs.core.sequential_QMARK_, cljs.core.seq, x)))
};
cljs.core.into = function into(to, from) {
  return cljs.core.reduce.call(null, cljs.core._conj, to, from)
};
cljs.core.partition = function() {
  var partition = null;
  var partition__3741 = function(n, coll) {
    return partition.call(null, n, n, coll)
  };
  var partition__3742 = function(n, step, coll) {
    return new cljs.core.LazySeq(null, false, function() {
      var temp__3698__auto____3735 = cljs.core.seq.call(null, coll);
      if(cljs.core.truth_(temp__3698__auto____3735)) {
        var s__3736 = temp__3698__auto____3735;
        var p__3737 = cljs.core.take.call(null, n, s__3736);
        if(cljs.core.truth_(cljs.core._EQ_.call(null, n, cljs.core.count.call(null, p__3737)))) {
          return cljs.core.cons.call(null, p__3737, partition.call(null, n, step, cljs.core.drop.call(null, step, s__3736)))
        }else {
          return null
        }
      }else {
        return null
      }
    })
  };
  var partition__3743 = function(n, step, pad, coll) {
    return new cljs.core.LazySeq(null, false, function() {
      var temp__3698__auto____3738 = cljs.core.seq.call(null, coll);
      if(cljs.core.truth_(temp__3698__auto____3738)) {
        var s__3739 = temp__3698__auto____3738;
        var p__3740 = cljs.core.take.call(null, n, s__3739);
        if(cljs.core.truth_(cljs.core._EQ_.call(null, n, cljs.core.count.call(null, p__3740)))) {
          return cljs.core.cons.call(null, p__3740, partition.call(null, n, step, pad, cljs.core.drop.call(null, step, s__3739)))
        }else {
          return cljs.core.list.call(null, cljs.core.take.call(null, n, cljs.core.concat.call(null, p__3740, pad)))
        }
      }else {
        return null
      }
    })
  };
  partition = function(n, step, pad, coll) {
    switch(arguments.length) {
      case 2:
        return partition__3741.call(this, n, step);
      case 3:
        return partition__3742.call(this, n, step, pad);
      case 4:
        return partition__3743.call(this, n, step, pad, coll)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return partition
}();
cljs.core.get_in = function() {
  var get_in = null;
  var get_in__3749 = function(m, ks) {
    return cljs.core.reduce.call(null, cljs.core.get, m, ks)
  };
  var get_in__3750 = function(m, ks, not_found) {
    var sentinel__3745 = cljs.core.lookup_sentinel;
    var m__3746 = m;
    var ks__3747 = cljs.core.seq.call(null, ks);
    while(true) {
      if(cljs.core.truth_(ks__3747)) {
        var m__3748 = cljs.core.get.call(null, m__3746, cljs.core.first.call(null, ks__3747), sentinel__3745);
        if(cljs.core.truth_(sentinel__3745 === m__3748)) {
          return not_found
        }else {
          var G__3752 = sentinel__3745;
          var G__3753 = m__3748;
          var G__3754 = cljs.core.next.call(null, ks__3747);
          sentinel__3745 = G__3752;
          m__3746 = G__3753;
          ks__3747 = G__3754;
          continue
        }
      }else {
        return m__3746
      }
      break
    }
  };
  get_in = function(m, ks, not_found) {
    switch(arguments.length) {
      case 2:
        return get_in__3749.call(this, m, ks);
      case 3:
        return get_in__3750.call(this, m, ks, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return get_in
}();
cljs.core.assoc_in = function assoc_in(m, p__3755, v) {
  var vec__3756__3757 = p__3755;
  var k__3758 = cljs.core.nth.call(null, vec__3756__3757, 0, null);
  var ks__3759 = cljs.core.nthnext.call(null, vec__3756__3757, 1);
  if(cljs.core.truth_(ks__3759)) {
    return cljs.core.assoc.call(null, m, k__3758, assoc_in.call(null, cljs.core.get.call(null, m, k__3758), ks__3759, v))
  }else {
    return cljs.core.assoc.call(null, m, k__3758, v)
  }
};
cljs.core.update_in = function() {
  var update_in__delegate = function(m, p__3760, f, args) {
    var vec__3761__3762 = p__3760;
    var k__3763 = cljs.core.nth.call(null, vec__3761__3762, 0, null);
    var ks__3764 = cljs.core.nthnext.call(null, vec__3761__3762, 1);
    if(cljs.core.truth_(ks__3764)) {
      return cljs.core.assoc.call(null, m, k__3763, cljs.core.apply.call(null, update_in, cljs.core.get.call(null, m, k__3763), ks__3764, f, args))
    }else {
      return cljs.core.assoc.call(null, m, k__3763, cljs.core.apply.call(null, f, cljs.core.get.call(null, m, k__3763), args))
    }
  };
  var update_in = function(m, p__3760, f, var_args) {
    var args = null;
    if(goog.isDef(var_args)) {
      args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
    }
    return update_in__delegate.call(this, m, p__3760, f, args)
  };
  update_in.cljs$lang$maxFixedArity = 3;
  update_in.cljs$lang$applyTo = function(arglist__3765) {
    var m = cljs.core.first(arglist__3765);
    var p__3760 = cljs.core.first(cljs.core.next(arglist__3765));
    var f = cljs.core.first(cljs.core.next(cljs.core.next(arglist__3765)));
    var args = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__3765)));
    return update_in__delegate.call(this, m, p__3760, f, args)
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
  var this__3766 = this;
  return cljs.core.hash_coll.call(null, coll)
};
cljs.core.Vector.prototype.cljs$core$ILookup$ = true;
cljs.core.Vector.prototype.cljs$core$ILookup$_lookup = function() {
  var G__3799 = null;
  var G__3799__3800 = function(coll, k) {
    var this__3767 = this;
    return cljs.core._nth.call(null, coll, k, null)
  };
  var G__3799__3801 = function(coll, k, not_found) {
    var this__3768 = this;
    return cljs.core._nth.call(null, coll, k, not_found)
  };
  G__3799 = function(coll, k, not_found) {
    switch(arguments.length) {
      case 2:
        return G__3799__3800.call(this, coll, k);
      case 3:
        return G__3799__3801.call(this, coll, k, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3799
}();
cljs.core.Vector.prototype.cljs$core$IAssociative$ = true;
cljs.core.Vector.prototype.cljs$core$IAssociative$_assoc = function(coll, k, v) {
  var this__3769 = this;
  var new_array__3770 = cljs.core.aclone.call(null, this__3769.array);
  new_array__3770[k] = v;
  return new cljs.core.Vector(this__3769.meta, new_array__3770)
};
cljs.core.Vector.prototype.cljs$core$IFn$ = true;
cljs.core.Vector.prototype.call = function() {
  var G__3803 = null;
  var G__3803__3804 = function(tsym3771, k) {
    var this__3773 = this;
    var tsym3771__3774 = this;
    var coll__3775 = tsym3771__3774;
    return cljs.core._lookup.call(null, coll__3775, k)
  };
  var G__3803__3805 = function(tsym3772, k, not_found) {
    var this__3776 = this;
    var tsym3772__3777 = this;
    var coll__3778 = tsym3772__3777;
    return cljs.core._lookup.call(null, coll__3778, k, not_found)
  };
  G__3803 = function(tsym3772, k, not_found) {
    switch(arguments.length) {
      case 2:
        return G__3803__3804.call(this, tsym3772, k);
      case 3:
        return G__3803__3805.call(this, tsym3772, k, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3803
}();
cljs.core.Vector.prototype.cljs$core$ISequential$ = true;
cljs.core.Vector.prototype.cljs$core$ICollection$ = true;
cljs.core.Vector.prototype.cljs$core$ICollection$_conj = function(coll, o) {
  var this__3779 = this;
  var new_array__3780 = cljs.core.aclone.call(null, this__3779.array);
  new_array__3780.push(o);
  return new cljs.core.Vector(this__3779.meta, new_array__3780)
};
cljs.core.Vector.prototype.cljs$core$IReduce$ = true;
cljs.core.Vector.prototype.cljs$core$IReduce$_reduce = function() {
  var G__3807 = null;
  var G__3807__3808 = function(v, f) {
    var this__3781 = this;
    return cljs.core.ci_reduce.call(null, this__3781.array, f)
  };
  var G__3807__3809 = function(v, f, start) {
    var this__3782 = this;
    return cljs.core.ci_reduce.call(null, this__3782.array, f, start)
  };
  G__3807 = function(v, f, start) {
    switch(arguments.length) {
      case 2:
        return G__3807__3808.call(this, v, f);
      case 3:
        return G__3807__3809.call(this, v, f, start)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3807
}();
cljs.core.Vector.prototype.cljs$core$ISeqable$ = true;
cljs.core.Vector.prototype.cljs$core$ISeqable$_seq = function(coll) {
  var this__3783 = this;
  if(cljs.core.truth_(this__3783.array.length > 0)) {
    var vector_seq__3784 = function vector_seq(i) {
      return new cljs.core.LazySeq(null, false, function() {
        if(cljs.core.truth_(i < this__3783.array.length)) {
          return cljs.core.cons.call(null, this__3783.array[i], vector_seq.call(null, i + 1))
        }else {
          return null
        }
      })
    };
    return vector_seq__3784.call(null, 0)
  }else {
    return null
  }
};
cljs.core.Vector.prototype.cljs$core$ICounted$ = true;
cljs.core.Vector.prototype.cljs$core$ICounted$_count = function(coll) {
  var this__3785 = this;
  return this__3785.array.length
};
cljs.core.Vector.prototype.cljs$core$IStack$ = true;
cljs.core.Vector.prototype.cljs$core$IStack$_peek = function(coll) {
  var this__3786 = this;
  var count__3787 = this__3786.array.length;
  if(cljs.core.truth_(count__3787 > 0)) {
    return this__3786.array[count__3787 - 1]
  }else {
    return null
  }
};
cljs.core.Vector.prototype.cljs$core$IStack$_pop = function(coll) {
  var this__3788 = this;
  if(cljs.core.truth_(this__3788.array.length > 0)) {
    var new_array__3789 = cljs.core.aclone.call(null, this__3788.array);
    new_array__3789.pop();
    return new cljs.core.Vector(this__3788.meta, new_array__3789)
  }else {
    throw new Error("Can't pop empty vector");
  }
};
cljs.core.Vector.prototype.cljs$core$IVector$ = true;
cljs.core.Vector.prototype.cljs$core$IVector$_assoc_n = function(coll, n, val) {
  var this__3790 = this;
  return cljs.core._assoc.call(null, coll, n, val)
};
cljs.core.Vector.prototype.cljs$core$IEquiv$ = true;
cljs.core.Vector.prototype.cljs$core$IEquiv$_equiv = function(coll, other) {
  var this__3791 = this;
  return cljs.core.equiv_sequential.call(null, coll, other)
};
cljs.core.Vector.prototype.cljs$core$IWithMeta$ = true;
cljs.core.Vector.prototype.cljs$core$IWithMeta$_with_meta = function(coll, meta) {
  var this__3792 = this;
  return new cljs.core.Vector(meta, this__3792.array)
};
cljs.core.Vector.prototype.cljs$core$IMeta$ = true;
cljs.core.Vector.prototype.cljs$core$IMeta$_meta = function(coll) {
  var this__3793 = this;
  return this__3793.meta
};
cljs.core.Vector.prototype.cljs$core$IIndexed$ = true;
cljs.core.Vector.prototype.cljs$core$IIndexed$_nth = function() {
  var G__3811 = null;
  var G__3811__3812 = function(coll, n) {
    var this__3794 = this;
    if(cljs.core.truth_(function() {
      var and__3546__auto____3795 = 0 <= n;
      if(cljs.core.truth_(and__3546__auto____3795)) {
        return n < this__3794.array.length
      }else {
        return and__3546__auto____3795
      }
    }())) {
      return this__3794.array[n]
    }else {
      return null
    }
  };
  var G__3811__3813 = function(coll, n, not_found) {
    var this__3796 = this;
    if(cljs.core.truth_(function() {
      var and__3546__auto____3797 = 0 <= n;
      if(cljs.core.truth_(and__3546__auto____3797)) {
        return n < this__3796.array.length
      }else {
        return and__3546__auto____3797
      }
    }())) {
      return this__3796.array[n]
    }else {
      return not_found
    }
  };
  G__3811 = function(coll, n, not_found) {
    switch(arguments.length) {
      case 2:
        return G__3811__3812.call(this, coll, n);
      case 3:
        return G__3811__3813.call(this, coll, n, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3811
}();
cljs.core.Vector.prototype.cljs$core$IEmptyableCollection$ = true;
cljs.core.Vector.prototype.cljs$core$IEmptyableCollection$_empty = function(coll) {
  var this__3798 = this;
  return cljs.core.with_meta.call(null, cljs.core.Vector.EMPTY, this__3798.meta)
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
  vector.cljs$lang$applyTo = function(arglist__3815) {
    var args = cljs.core.seq(arglist__3815);
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
  var this__3816 = this;
  return cljs.core.hash_coll.call(null, coll)
};
cljs.core.Subvec.prototype.cljs$core$ILookup$ = true;
cljs.core.Subvec.prototype.cljs$core$ILookup$_lookup = function() {
  var G__3844 = null;
  var G__3844__3845 = function(coll, k) {
    var this__3817 = this;
    return cljs.core._nth.call(null, coll, k, null)
  };
  var G__3844__3846 = function(coll, k, not_found) {
    var this__3818 = this;
    return cljs.core._nth.call(null, coll, k, not_found)
  };
  G__3844 = function(coll, k, not_found) {
    switch(arguments.length) {
      case 2:
        return G__3844__3845.call(this, coll, k);
      case 3:
        return G__3844__3846.call(this, coll, k, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3844
}();
cljs.core.Subvec.prototype.cljs$core$IAssociative$ = true;
cljs.core.Subvec.prototype.cljs$core$IAssociative$_assoc = function(coll, key, val) {
  var this__3819 = this;
  var v_pos__3820 = this__3819.start + key;
  return new cljs.core.Subvec(this__3819.meta, cljs.core._assoc.call(null, this__3819.v, v_pos__3820, val), this__3819.start, this__3819.end > v_pos__3820 + 1 ? this__3819.end : v_pos__3820 + 1)
};
cljs.core.Subvec.prototype.cljs$core$IFn$ = true;
cljs.core.Subvec.prototype.call = function() {
  var G__3848 = null;
  var G__3848__3849 = function(tsym3821, k) {
    var this__3823 = this;
    var tsym3821__3824 = this;
    var coll__3825 = tsym3821__3824;
    return cljs.core._lookup.call(null, coll__3825, k)
  };
  var G__3848__3850 = function(tsym3822, k, not_found) {
    var this__3826 = this;
    var tsym3822__3827 = this;
    var coll__3828 = tsym3822__3827;
    return cljs.core._lookup.call(null, coll__3828, k, not_found)
  };
  G__3848 = function(tsym3822, k, not_found) {
    switch(arguments.length) {
      case 2:
        return G__3848__3849.call(this, tsym3822, k);
      case 3:
        return G__3848__3850.call(this, tsym3822, k, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3848
}();
cljs.core.Subvec.prototype.cljs$core$ISequential$ = true;
cljs.core.Subvec.prototype.cljs$core$ICollection$ = true;
cljs.core.Subvec.prototype.cljs$core$ICollection$_conj = function(coll, o) {
  var this__3829 = this;
  return new cljs.core.Subvec(this__3829.meta, cljs.core._assoc_n.call(null, this__3829.v, this__3829.end, o), this__3829.start, this__3829.end + 1)
};
cljs.core.Subvec.prototype.cljs$core$IReduce$ = true;
cljs.core.Subvec.prototype.cljs$core$IReduce$_reduce = function() {
  var G__3852 = null;
  var G__3852__3853 = function(coll, f) {
    var this__3830 = this;
    return cljs.core.ci_reduce.call(null, coll, f)
  };
  var G__3852__3854 = function(coll, f, start) {
    var this__3831 = this;
    return cljs.core.ci_reduce.call(null, coll, f, start)
  };
  G__3852 = function(coll, f, start) {
    switch(arguments.length) {
      case 2:
        return G__3852__3853.call(this, coll, f);
      case 3:
        return G__3852__3854.call(this, coll, f, start)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3852
}();
cljs.core.Subvec.prototype.cljs$core$ISeqable$ = true;
cljs.core.Subvec.prototype.cljs$core$ISeqable$_seq = function(coll) {
  var this__3832 = this;
  var subvec_seq__3833 = function subvec_seq(i) {
    if(cljs.core.truth_(cljs.core._EQ_.call(null, i, this__3832.end))) {
      return null
    }else {
      return cljs.core.cons.call(null, cljs.core._nth.call(null, this__3832.v, i), new cljs.core.LazySeq(null, false, function() {
        return subvec_seq.call(null, i + 1)
      }))
    }
  };
  return subvec_seq__3833.call(null, this__3832.start)
};
cljs.core.Subvec.prototype.cljs$core$ICounted$ = true;
cljs.core.Subvec.prototype.cljs$core$ICounted$_count = function(coll) {
  var this__3834 = this;
  return this__3834.end - this__3834.start
};
cljs.core.Subvec.prototype.cljs$core$IStack$ = true;
cljs.core.Subvec.prototype.cljs$core$IStack$_peek = function(coll) {
  var this__3835 = this;
  return cljs.core._nth.call(null, this__3835.v, this__3835.end - 1)
};
cljs.core.Subvec.prototype.cljs$core$IStack$_pop = function(coll) {
  var this__3836 = this;
  if(cljs.core.truth_(cljs.core._EQ_.call(null, this__3836.start, this__3836.end))) {
    throw new Error("Can't pop empty vector");
  }else {
    return new cljs.core.Subvec(this__3836.meta, this__3836.v, this__3836.start, this__3836.end - 1)
  }
};
cljs.core.Subvec.prototype.cljs$core$IVector$ = true;
cljs.core.Subvec.prototype.cljs$core$IVector$_assoc_n = function(coll, n, val) {
  var this__3837 = this;
  return cljs.core._assoc.call(null, coll, n, val)
};
cljs.core.Subvec.prototype.cljs$core$IEquiv$ = true;
cljs.core.Subvec.prototype.cljs$core$IEquiv$_equiv = function(coll, other) {
  var this__3838 = this;
  return cljs.core.equiv_sequential.call(null, coll, other)
};
cljs.core.Subvec.prototype.cljs$core$IWithMeta$ = true;
cljs.core.Subvec.prototype.cljs$core$IWithMeta$_with_meta = function(coll, meta) {
  var this__3839 = this;
  return new cljs.core.Subvec(meta, this__3839.v, this__3839.start, this__3839.end)
};
cljs.core.Subvec.prototype.cljs$core$IMeta$ = true;
cljs.core.Subvec.prototype.cljs$core$IMeta$_meta = function(coll) {
  var this__3840 = this;
  return this__3840.meta
};
cljs.core.Subvec.prototype.cljs$core$IIndexed$ = true;
cljs.core.Subvec.prototype.cljs$core$IIndexed$_nth = function() {
  var G__3856 = null;
  var G__3856__3857 = function(coll, n) {
    var this__3841 = this;
    return cljs.core._nth.call(null, this__3841.v, this__3841.start + n)
  };
  var G__3856__3858 = function(coll, n, not_found) {
    var this__3842 = this;
    return cljs.core._nth.call(null, this__3842.v, this__3842.start + n, not_found)
  };
  G__3856 = function(coll, n, not_found) {
    switch(arguments.length) {
      case 2:
        return G__3856__3857.call(this, coll, n);
      case 3:
        return G__3856__3858.call(this, coll, n, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3856
}();
cljs.core.Subvec.prototype.cljs$core$IEmptyableCollection$ = true;
cljs.core.Subvec.prototype.cljs$core$IEmptyableCollection$_empty = function(coll) {
  var this__3843 = this;
  return cljs.core.with_meta.call(null, cljs.core.Vector.EMPTY, this__3843.meta)
};
cljs.core.Subvec;
cljs.core.subvec = function() {
  var subvec = null;
  var subvec__3860 = function(v, start) {
    return subvec.call(null, v, start, cljs.core.count.call(null, v))
  };
  var subvec__3861 = function(v, start, end) {
    return new cljs.core.Subvec(null, v, start, end)
  };
  subvec = function(v, start, end) {
    switch(arguments.length) {
      case 2:
        return subvec__3860.call(this, v, start);
      case 3:
        return subvec__3861.call(this, v, start, end)
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
  var this__3863 = this;
  return coll
};
cljs.core.PersistentQueueSeq.prototype.cljs$core$IHash$ = true;
cljs.core.PersistentQueueSeq.prototype.cljs$core$IHash$_hash = function(coll) {
  var this__3864 = this;
  return cljs.core.hash_coll.call(null, coll)
};
cljs.core.PersistentQueueSeq.prototype.cljs$core$IEquiv$ = true;
cljs.core.PersistentQueueSeq.prototype.cljs$core$IEquiv$_equiv = function(coll, other) {
  var this__3865 = this;
  return cljs.core.equiv_sequential.call(null, coll, other)
};
cljs.core.PersistentQueueSeq.prototype.cljs$core$ISequential$ = true;
cljs.core.PersistentQueueSeq.prototype.cljs$core$IEmptyableCollection$ = true;
cljs.core.PersistentQueueSeq.prototype.cljs$core$IEmptyableCollection$_empty = function(coll) {
  var this__3866 = this;
  return cljs.core.with_meta.call(null, cljs.core.List.EMPTY, this__3866.meta)
};
cljs.core.PersistentQueueSeq.prototype.cljs$core$ICollection$ = true;
cljs.core.PersistentQueueSeq.prototype.cljs$core$ICollection$_conj = function(coll, o) {
  var this__3867 = this;
  return cljs.core.cons.call(null, o, coll)
};
cljs.core.PersistentQueueSeq.prototype.cljs$core$ISeq$ = true;
cljs.core.PersistentQueueSeq.prototype.cljs$core$ISeq$_first = function(coll) {
  var this__3868 = this;
  return cljs.core._first.call(null, this__3868.front)
};
cljs.core.PersistentQueueSeq.prototype.cljs$core$ISeq$_rest = function(coll) {
  var this__3869 = this;
  var temp__3695__auto____3870 = cljs.core.next.call(null, this__3869.front);
  if(cljs.core.truth_(temp__3695__auto____3870)) {
    var f1__3871 = temp__3695__auto____3870;
    return new cljs.core.PersistentQueueSeq(this__3869.meta, f1__3871, this__3869.rear)
  }else {
    if(cljs.core.truth_(this__3869.rear === null)) {
      return cljs.core._empty.call(null, coll)
    }else {
      return new cljs.core.PersistentQueueSeq(this__3869.meta, this__3869.rear, null)
    }
  }
};
cljs.core.PersistentQueueSeq.prototype.cljs$core$IMeta$ = true;
cljs.core.PersistentQueueSeq.prototype.cljs$core$IMeta$_meta = function(coll) {
  var this__3872 = this;
  return this__3872.meta
};
cljs.core.PersistentQueueSeq.prototype.cljs$core$IWithMeta$ = true;
cljs.core.PersistentQueueSeq.prototype.cljs$core$IWithMeta$_with_meta = function(coll, meta) {
  var this__3873 = this;
  return new cljs.core.PersistentQueueSeq(meta, this__3873.front, this__3873.rear)
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
  var this__3874 = this;
  return cljs.core.hash_coll.call(null, coll)
};
cljs.core.PersistentQueue.prototype.cljs$core$ISequential$ = true;
cljs.core.PersistentQueue.prototype.cljs$core$ICollection$ = true;
cljs.core.PersistentQueue.prototype.cljs$core$ICollection$_conj = function(coll, o) {
  var this__3875 = this;
  if(cljs.core.truth_(this__3875.front)) {
    return new cljs.core.PersistentQueue(this__3875.meta, this__3875.count + 1, this__3875.front, cljs.core.conj.call(null, function() {
      var or__3548__auto____3876 = this__3875.rear;
      if(cljs.core.truth_(or__3548__auto____3876)) {
        return or__3548__auto____3876
      }else {
        return cljs.core.Vector.fromArray([])
      }
    }(), o))
  }else {
    return new cljs.core.PersistentQueue(this__3875.meta, this__3875.count + 1, cljs.core.conj.call(null, this__3875.front, o), cljs.core.Vector.fromArray([]))
  }
};
cljs.core.PersistentQueue.prototype.cljs$core$ISeqable$ = true;
cljs.core.PersistentQueue.prototype.cljs$core$ISeqable$_seq = function(coll) {
  var this__3877 = this;
  var rear__3878 = cljs.core.seq.call(null, this__3877.rear);
  if(cljs.core.truth_(function() {
    var or__3548__auto____3879 = this__3877.front;
    if(cljs.core.truth_(or__3548__auto____3879)) {
      return or__3548__auto____3879
    }else {
      return rear__3878
    }
  }())) {
    return new cljs.core.PersistentQueueSeq(null, this__3877.front, cljs.core.seq.call(null, rear__3878))
  }else {
    return cljs.core.List.EMPTY
  }
};
cljs.core.PersistentQueue.prototype.cljs$core$ICounted$ = true;
cljs.core.PersistentQueue.prototype.cljs$core$ICounted$_count = function(coll) {
  var this__3880 = this;
  return this__3880.count
};
cljs.core.PersistentQueue.prototype.cljs$core$IStack$ = true;
cljs.core.PersistentQueue.prototype.cljs$core$IStack$_peek = function(coll) {
  var this__3881 = this;
  return cljs.core._first.call(null, this__3881.front)
};
cljs.core.PersistentQueue.prototype.cljs$core$IStack$_pop = function(coll) {
  var this__3882 = this;
  if(cljs.core.truth_(this__3882.front)) {
    var temp__3695__auto____3883 = cljs.core.next.call(null, this__3882.front);
    if(cljs.core.truth_(temp__3695__auto____3883)) {
      var f1__3884 = temp__3695__auto____3883;
      return new cljs.core.PersistentQueue(this__3882.meta, this__3882.count - 1, f1__3884, this__3882.rear)
    }else {
      return new cljs.core.PersistentQueue(this__3882.meta, this__3882.count - 1, cljs.core.seq.call(null, this__3882.rear), cljs.core.Vector.fromArray([]))
    }
  }else {
    return coll
  }
};
cljs.core.PersistentQueue.prototype.cljs$core$ISeq$ = true;
cljs.core.PersistentQueue.prototype.cljs$core$ISeq$_first = function(coll) {
  var this__3885 = this;
  return cljs.core.first.call(null, this__3885.front)
};
cljs.core.PersistentQueue.prototype.cljs$core$ISeq$_rest = function(coll) {
  var this__3886 = this;
  return cljs.core.rest.call(null, cljs.core.seq.call(null, coll))
};
cljs.core.PersistentQueue.prototype.cljs$core$IEquiv$ = true;
cljs.core.PersistentQueue.prototype.cljs$core$IEquiv$_equiv = function(coll, other) {
  var this__3887 = this;
  return cljs.core.equiv_sequential.call(null, coll, other)
};
cljs.core.PersistentQueue.prototype.cljs$core$IWithMeta$ = true;
cljs.core.PersistentQueue.prototype.cljs$core$IWithMeta$_with_meta = function(coll, meta) {
  var this__3888 = this;
  return new cljs.core.PersistentQueue(meta, this__3888.count, this__3888.front, this__3888.rear)
};
cljs.core.PersistentQueue.prototype.cljs$core$IMeta$ = true;
cljs.core.PersistentQueue.prototype.cljs$core$IMeta$_meta = function(coll) {
  var this__3889 = this;
  return this__3889.meta
};
cljs.core.PersistentQueue.prototype.cljs$core$IEmptyableCollection$ = true;
cljs.core.PersistentQueue.prototype.cljs$core$IEmptyableCollection$_empty = function(coll) {
  var this__3890 = this;
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
  var this__3891 = this;
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
  var len__3892 = array.length;
  var i__3893 = 0;
  while(true) {
    if(cljs.core.truth_(i__3893 < len__3892)) {
      if(cljs.core.truth_(cljs.core._EQ_.call(null, k, array[i__3893]))) {
        return i__3893
      }else {
        var G__3894 = i__3893 + incr;
        i__3893 = G__3894;
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
  var obj_map_contains_key_QMARK___3896 = function(k, strobj) {
    return obj_map_contains_key_QMARK_.call(null, k, strobj, true, false)
  };
  var obj_map_contains_key_QMARK___3897 = function(k, strobj, true_val, false_val) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____3895 = goog.isString.call(null, k);
      if(cljs.core.truth_(and__3546__auto____3895)) {
        return strobj.hasOwnProperty(k)
      }else {
        return and__3546__auto____3895
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
        return obj_map_contains_key_QMARK___3896.call(this, k, strobj);
      case 4:
        return obj_map_contains_key_QMARK___3897.call(this, k, strobj, true_val, false_val)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return obj_map_contains_key_QMARK_
}();
cljs.core.obj_map_compare_keys = function obj_map_compare_keys(a, b) {
  var a__3900 = cljs.core.hash.call(null, a);
  var b__3901 = cljs.core.hash.call(null, b);
  if(cljs.core.truth_(a__3900 < b__3901)) {
    return-1
  }else {
    if(cljs.core.truth_(a__3900 > b__3901)) {
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
  var this__3902 = this;
  return cljs.core.hash_coll.call(null, coll)
};
cljs.core.ObjMap.prototype.cljs$core$ILookup$ = true;
cljs.core.ObjMap.prototype.cljs$core$ILookup$_lookup = function() {
  var G__3929 = null;
  var G__3929__3930 = function(coll, k) {
    var this__3903 = this;
    return cljs.core._lookup.call(null, coll, k, null)
  };
  var G__3929__3931 = function(coll, k, not_found) {
    var this__3904 = this;
    return cljs.core.obj_map_contains_key_QMARK_.call(null, k, this__3904.strobj, this__3904.strobj[k], not_found)
  };
  G__3929 = function(coll, k, not_found) {
    switch(arguments.length) {
      case 2:
        return G__3929__3930.call(this, coll, k);
      case 3:
        return G__3929__3931.call(this, coll, k, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3929
}();
cljs.core.ObjMap.prototype.cljs$core$IAssociative$ = true;
cljs.core.ObjMap.prototype.cljs$core$IAssociative$_assoc = function(coll, k, v) {
  var this__3905 = this;
  if(cljs.core.truth_(goog.isString.call(null, k))) {
    var new_strobj__3906 = goog.object.clone.call(null, this__3905.strobj);
    var overwrite_QMARK___3907 = new_strobj__3906.hasOwnProperty(k);
    new_strobj__3906[k] = v;
    if(cljs.core.truth_(overwrite_QMARK___3907)) {
      return new cljs.core.ObjMap(this__3905.meta, this__3905.keys, new_strobj__3906)
    }else {
      var new_keys__3908 = cljs.core.aclone.call(null, this__3905.keys);
      new_keys__3908.push(k);
      return new cljs.core.ObjMap(this__3905.meta, new_keys__3908, new_strobj__3906)
    }
  }else {
    return cljs.core.with_meta.call(null, cljs.core.into.call(null, cljs.core.hash_map.call(null, k, v), cljs.core.seq.call(null, coll)), this__3905.meta)
  }
};
cljs.core.ObjMap.prototype.cljs$core$IAssociative$_contains_key_QMARK_ = function(coll, k) {
  var this__3909 = this;
  return cljs.core.obj_map_contains_key_QMARK_.call(null, k, this__3909.strobj)
};
cljs.core.ObjMap.prototype.cljs$core$IFn$ = true;
cljs.core.ObjMap.prototype.call = function() {
  var G__3933 = null;
  var G__3933__3934 = function(tsym3910, k) {
    var this__3912 = this;
    var tsym3910__3913 = this;
    var coll__3914 = tsym3910__3913;
    return cljs.core._lookup.call(null, coll__3914, k)
  };
  var G__3933__3935 = function(tsym3911, k, not_found) {
    var this__3915 = this;
    var tsym3911__3916 = this;
    var coll__3917 = tsym3911__3916;
    return cljs.core._lookup.call(null, coll__3917, k, not_found)
  };
  G__3933 = function(tsym3911, k, not_found) {
    switch(arguments.length) {
      case 2:
        return G__3933__3934.call(this, tsym3911, k);
      case 3:
        return G__3933__3935.call(this, tsym3911, k, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3933
}();
cljs.core.ObjMap.prototype.cljs$core$ICollection$ = true;
cljs.core.ObjMap.prototype.cljs$core$ICollection$_conj = function(coll, entry) {
  var this__3918 = this;
  if(cljs.core.truth_(cljs.core.vector_QMARK_.call(null, entry))) {
    return cljs.core._assoc.call(null, coll, cljs.core._nth.call(null, entry, 0), cljs.core._nth.call(null, entry, 1))
  }else {
    return cljs.core.reduce.call(null, cljs.core._conj, coll, entry)
  }
};
cljs.core.ObjMap.prototype.cljs$core$ISeqable$ = true;
cljs.core.ObjMap.prototype.cljs$core$ISeqable$_seq = function(coll) {
  var this__3919 = this;
  if(cljs.core.truth_(this__3919.keys.length > 0)) {
    return cljs.core.map.call(null, function(p1__3899_SHARP_) {
      return cljs.core.vector.call(null, p1__3899_SHARP_, this__3919.strobj[p1__3899_SHARP_])
    }, this__3919.keys.sort(cljs.core.obj_map_compare_keys))
  }else {
    return null
  }
};
cljs.core.ObjMap.prototype.cljs$core$ICounted$ = true;
cljs.core.ObjMap.prototype.cljs$core$ICounted$_count = function(coll) {
  var this__3920 = this;
  return this__3920.keys.length
};
cljs.core.ObjMap.prototype.cljs$core$IEquiv$ = true;
cljs.core.ObjMap.prototype.cljs$core$IEquiv$_equiv = function(coll, other) {
  var this__3921 = this;
  return cljs.core.equiv_map.call(null, coll, other)
};
cljs.core.ObjMap.prototype.cljs$core$IWithMeta$ = true;
cljs.core.ObjMap.prototype.cljs$core$IWithMeta$_with_meta = function(coll, meta) {
  var this__3922 = this;
  return new cljs.core.ObjMap(meta, this__3922.keys, this__3922.strobj)
};
cljs.core.ObjMap.prototype.cljs$core$IMeta$ = true;
cljs.core.ObjMap.prototype.cljs$core$IMeta$_meta = function(coll) {
  var this__3923 = this;
  return this__3923.meta
};
cljs.core.ObjMap.prototype.cljs$core$IEmptyableCollection$ = true;
cljs.core.ObjMap.prototype.cljs$core$IEmptyableCollection$_empty = function(coll) {
  var this__3924 = this;
  return cljs.core.with_meta.call(null, cljs.core.ObjMap.EMPTY, this__3924.meta)
};
cljs.core.ObjMap.prototype.cljs$core$IMap$ = true;
cljs.core.ObjMap.prototype.cljs$core$IMap$_dissoc = function(coll, k) {
  var this__3925 = this;
  if(cljs.core.truth_(function() {
    var and__3546__auto____3926 = goog.isString.call(null, k);
    if(cljs.core.truth_(and__3546__auto____3926)) {
      return this__3925.strobj.hasOwnProperty(k)
    }else {
      return and__3546__auto____3926
    }
  }())) {
    var new_keys__3927 = cljs.core.aclone.call(null, this__3925.keys);
    var new_strobj__3928 = goog.object.clone.call(null, this__3925.strobj);
    new_keys__3927.splice(cljs.core.scan_array.call(null, 1, k, new_keys__3927), 1);
    cljs.core.js_delete.call(null, new_strobj__3928, k);
    return new cljs.core.ObjMap(this__3925.meta, new_keys__3927, new_strobj__3928)
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
  var this__3938 = this;
  return cljs.core.hash_coll.call(null, coll)
};
cljs.core.HashMap.prototype.cljs$core$ILookup$ = true;
cljs.core.HashMap.prototype.cljs$core$ILookup$_lookup = function() {
  var G__3976 = null;
  var G__3976__3977 = function(coll, k) {
    var this__3939 = this;
    return cljs.core._lookup.call(null, coll, k, null)
  };
  var G__3976__3978 = function(coll, k, not_found) {
    var this__3940 = this;
    var bucket__3941 = this__3940.hashobj[cljs.core.hash.call(null, k)];
    var i__3942 = cljs.core.truth_(bucket__3941) ? cljs.core.scan_array.call(null, 2, k, bucket__3941) : null;
    if(cljs.core.truth_(i__3942)) {
      return bucket__3941[i__3942 + 1]
    }else {
      return not_found
    }
  };
  G__3976 = function(coll, k, not_found) {
    switch(arguments.length) {
      case 2:
        return G__3976__3977.call(this, coll, k);
      case 3:
        return G__3976__3978.call(this, coll, k, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3976
}();
cljs.core.HashMap.prototype.cljs$core$IAssociative$ = true;
cljs.core.HashMap.prototype.cljs$core$IAssociative$_assoc = function(coll, k, v) {
  var this__3943 = this;
  var h__3944 = cljs.core.hash.call(null, k);
  var bucket__3945 = this__3943.hashobj[h__3944];
  if(cljs.core.truth_(bucket__3945)) {
    var new_bucket__3946 = cljs.core.aclone.call(null, bucket__3945);
    var new_hashobj__3947 = goog.object.clone.call(null, this__3943.hashobj);
    new_hashobj__3947[h__3944] = new_bucket__3946;
    var temp__3695__auto____3948 = cljs.core.scan_array.call(null, 2, k, new_bucket__3946);
    if(cljs.core.truth_(temp__3695__auto____3948)) {
      var i__3949 = temp__3695__auto____3948;
      new_bucket__3946[i__3949 + 1] = v;
      return new cljs.core.HashMap(this__3943.meta, this__3943.count, new_hashobj__3947)
    }else {
      new_bucket__3946.push(k, v);
      return new cljs.core.HashMap(this__3943.meta, this__3943.count + 1, new_hashobj__3947)
    }
  }else {
    var new_hashobj__3950 = goog.object.clone.call(null, this__3943.hashobj);
    new_hashobj__3950[h__3944] = cljs.core.array.call(null, k, v);
    return new cljs.core.HashMap(this__3943.meta, this__3943.count + 1, new_hashobj__3950)
  }
};
cljs.core.HashMap.prototype.cljs$core$IAssociative$_contains_key_QMARK_ = function(coll, k) {
  var this__3951 = this;
  var bucket__3952 = this__3951.hashobj[cljs.core.hash.call(null, k)];
  var i__3953 = cljs.core.truth_(bucket__3952) ? cljs.core.scan_array.call(null, 2, k, bucket__3952) : null;
  if(cljs.core.truth_(i__3953)) {
    return true
  }else {
    return false
  }
};
cljs.core.HashMap.prototype.cljs$core$IFn$ = true;
cljs.core.HashMap.prototype.call = function() {
  var G__3980 = null;
  var G__3980__3981 = function(tsym3954, k) {
    var this__3956 = this;
    var tsym3954__3957 = this;
    var coll__3958 = tsym3954__3957;
    return cljs.core._lookup.call(null, coll__3958, k)
  };
  var G__3980__3982 = function(tsym3955, k, not_found) {
    var this__3959 = this;
    var tsym3955__3960 = this;
    var coll__3961 = tsym3955__3960;
    return cljs.core._lookup.call(null, coll__3961, k, not_found)
  };
  G__3980 = function(tsym3955, k, not_found) {
    switch(arguments.length) {
      case 2:
        return G__3980__3981.call(this, tsym3955, k);
      case 3:
        return G__3980__3982.call(this, tsym3955, k, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__3980
}();
cljs.core.HashMap.prototype.cljs$core$ICollection$ = true;
cljs.core.HashMap.prototype.cljs$core$ICollection$_conj = function(coll, entry) {
  var this__3962 = this;
  if(cljs.core.truth_(cljs.core.vector_QMARK_.call(null, entry))) {
    return cljs.core._assoc.call(null, coll, cljs.core._nth.call(null, entry, 0), cljs.core._nth.call(null, entry, 1))
  }else {
    return cljs.core.reduce.call(null, cljs.core._conj, coll, entry)
  }
};
cljs.core.HashMap.prototype.cljs$core$ISeqable$ = true;
cljs.core.HashMap.prototype.cljs$core$ISeqable$_seq = function(coll) {
  var this__3963 = this;
  if(cljs.core.truth_(this__3963.count > 0)) {
    var hashes__3964 = cljs.core.js_keys.call(null, this__3963.hashobj).sort();
    return cljs.core.mapcat.call(null, function(p1__3937_SHARP_) {
      return cljs.core.map.call(null, cljs.core.vec, cljs.core.partition.call(null, 2, this__3963.hashobj[p1__3937_SHARP_]))
    }, hashes__3964)
  }else {
    return null
  }
};
cljs.core.HashMap.prototype.cljs$core$ICounted$ = true;
cljs.core.HashMap.prototype.cljs$core$ICounted$_count = function(coll) {
  var this__3965 = this;
  return this__3965.count
};
cljs.core.HashMap.prototype.cljs$core$IEquiv$ = true;
cljs.core.HashMap.prototype.cljs$core$IEquiv$_equiv = function(coll, other) {
  var this__3966 = this;
  return cljs.core.equiv_map.call(null, coll, other)
};
cljs.core.HashMap.prototype.cljs$core$IWithMeta$ = true;
cljs.core.HashMap.prototype.cljs$core$IWithMeta$_with_meta = function(coll, meta) {
  var this__3967 = this;
  return new cljs.core.HashMap(meta, this__3967.count, this__3967.hashobj)
};
cljs.core.HashMap.prototype.cljs$core$IMeta$ = true;
cljs.core.HashMap.prototype.cljs$core$IMeta$_meta = function(coll) {
  var this__3968 = this;
  return this__3968.meta
};
cljs.core.HashMap.prototype.cljs$core$IEmptyableCollection$ = true;
cljs.core.HashMap.prototype.cljs$core$IEmptyableCollection$_empty = function(coll) {
  var this__3969 = this;
  return cljs.core.with_meta.call(null, cljs.core.HashMap.EMPTY, this__3969.meta)
};
cljs.core.HashMap.prototype.cljs$core$IMap$ = true;
cljs.core.HashMap.prototype.cljs$core$IMap$_dissoc = function(coll, k) {
  var this__3970 = this;
  var h__3971 = cljs.core.hash.call(null, k);
  var bucket__3972 = this__3970.hashobj[h__3971];
  var i__3973 = cljs.core.truth_(bucket__3972) ? cljs.core.scan_array.call(null, 2, k, bucket__3972) : null;
  if(cljs.core.truth_(cljs.core.not.call(null, i__3973))) {
    return coll
  }else {
    var new_hashobj__3974 = goog.object.clone.call(null, this__3970.hashobj);
    if(cljs.core.truth_(3 > bucket__3972.length)) {
      cljs.core.js_delete.call(null, new_hashobj__3974, h__3971)
    }else {
      var new_bucket__3975 = cljs.core.aclone.call(null, bucket__3972);
      new_bucket__3975.splice(i__3973, 2);
      new_hashobj__3974[h__3971] = new_bucket__3975
    }
    return new cljs.core.HashMap(this__3970.meta, this__3970.count - 1, new_hashobj__3974)
  }
};
cljs.core.HashMap;
cljs.core.HashMap.EMPTY = new cljs.core.HashMap(null, 0, cljs.core.js_obj.call(null));
cljs.core.HashMap.fromArrays = function(ks, vs) {
  var len__3984 = ks.length;
  var i__3985 = 0;
  var out__3986 = cljs.core.HashMap.EMPTY;
  while(true) {
    if(cljs.core.truth_(i__3985 < len__3984)) {
      var G__3987 = i__3985 + 1;
      var G__3988 = cljs.core.assoc.call(null, out__3986, ks[i__3985], vs[i__3985]);
      i__3985 = G__3987;
      out__3986 = G__3988;
      continue
    }else {
      return out__3986
    }
    break
  }
};
cljs.core.hash_map = function() {
  var hash_map__delegate = function(keyvals) {
    var in$__3989 = cljs.core.seq.call(null, keyvals);
    var out__3990 = cljs.core.HashMap.EMPTY;
    while(true) {
      if(cljs.core.truth_(in$__3989)) {
        var G__3991 = cljs.core.nnext.call(null, in$__3989);
        var G__3992 = cljs.core.assoc.call(null, out__3990, cljs.core.first.call(null, in$__3989), cljs.core.second.call(null, in$__3989));
        in$__3989 = G__3991;
        out__3990 = G__3992;
        continue
      }else {
        return out__3990
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
  hash_map.cljs$lang$applyTo = function(arglist__3993) {
    var keyvals = cljs.core.seq(arglist__3993);
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
      return cljs.core.reduce.call(null, function(p1__3994_SHARP_, p2__3995_SHARP_) {
        return cljs.core.conj.call(null, function() {
          var or__3548__auto____3996 = p1__3994_SHARP_;
          if(cljs.core.truth_(or__3548__auto____3996)) {
            return or__3548__auto____3996
          }else {
            return cljs.core.ObjMap.fromObject([], {})
          }
        }(), p2__3995_SHARP_)
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
  merge.cljs$lang$applyTo = function(arglist__3997) {
    var maps = cljs.core.seq(arglist__3997);
    return merge__delegate.call(this, maps)
  };
  return merge
}();
cljs.core.merge_with = function() {
  var merge_with__delegate = function(f, maps) {
    if(cljs.core.truth_(cljs.core.some.call(null, cljs.core.identity, maps))) {
      var merge_entry__4000 = function(m, e) {
        var k__3998 = cljs.core.first.call(null, e);
        var v__3999 = cljs.core.second.call(null, e);
        if(cljs.core.truth_(cljs.core.contains_QMARK_.call(null, m, k__3998))) {
          return cljs.core.assoc.call(null, m, k__3998, f.call(null, cljs.core.get.call(null, m, k__3998), v__3999))
        }else {
          return cljs.core.assoc.call(null, m, k__3998, v__3999)
        }
      };
      var merge2__4002 = function(m1, m2) {
        return cljs.core.reduce.call(null, merge_entry__4000, function() {
          var or__3548__auto____4001 = m1;
          if(cljs.core.truth_(or__3548__auto____4001)) {
            return or__3548__auto____4001
          }else {
            return cljs.core.ObjMap.fromObject([], {})
          }
        }(), cljs.core.seq.call(null, m2))
      };
      return cljs.core.reduce.call(null, merge2__4002, maps)
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
  merge_with.cljs$lang$applyTo = function(arglist__4003) {
    var f = cljs.core.first(arglist__4003);
    var maps = cljs.core.rest(arglist__4003);
    return merge_with__delegate.call(this, f, maps)
  };
  return merge_with
}();
cljs.core.select_keys = function select_keys(map, keyseq) {
  var ret__4005 = cljs.core.ObjMap.fromObject([], {});
  var keys__4006 = cljs.core.seq.call(null, keyseq);
  while(true) {
    if(cljs.core.truth_(keys__4006)) {
      var key__4007 = cljs.core.first.call(null, keys__4006);
      var entry__4008 = cljs.core.get.call(null, map, key__4007, "\ufdd0'user/not-found");
      var G__4009 = cljs.core.truth_(cljs.core.not_EQ_.call(null, entry__4008, "\ufdd0'user/not-found")) ? cljs.core.assoc.call(null, ret__4005, key__4007, entry__4008) : ret__4005;
      var G__4010 = cljs.core.next.call(null, keys__4006);
      ret__4005 = G__4009;
      keys__4006 = G__4010;
      continue
    }else {
      return ret__4005
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
  var this__4011 = this;
  return cljs.core.hash_coll.call(null, coll)
};
cljs.core.Set.prototype.cljs$core$ILookup$ = true;
cljs.core.Set.prototype.cljs$core$ILookup$_lookup = function() {
  var G__4032 = null;
  var G__4032__4033 = function(coll, v) {
    var this__4012 = this;
    return cljs.core._lookup.call(null, coll, v, null)
  };
  var G__4032__4034 = function(coll, v, not_found) {
    var this__4013 = this;
    if(cljs.core.truth_(cljs.core._contains_key_QMARK_.call(null, this__4013.hash_map, v))) {
      return v
    }else {
      return not_found
    }
  };
  G__4032 = function(coll, v, not_found) {
    switch(arguments.length) {
      case 2:
        return G__4032__4033.call(this, coll, v);
      case 3:
        return G__4032__4034.call(this, coll, v, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__4032
}();
cljs.core.Set.prototype.cljs$core$IFn$ = true;
cljs.core.Set.prototype.call = function() {
  var G__4036 = null;
  var G__4036__4037 = function(tsym4014, k) {
    var this__4016 = this;
    var tsym4014__4017 = this;
    var coll__4018 = tsym4014__4017;
    return cljs.core._lookup.call(null, coll__4018, k)
  };
  var G__4036__4038 = function(tsym4015, k, not_found) {
    var this__4019 = this;
    var tsym4015__4020 = this;
    var coll__4021 = tsym4015__4020;
    return cljs.core._lookup.call(null, coll__4021, k, not_found)
  };
  G__4036 = function(tsym4015, k, not_found) {
    switch(arguments.length) {
      case 2:
        return G__4036__4037.call(this, tsym4015, k);
      case 3:
        return G__4036__4038.call(this, tsym4015, k, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__4036
}();
cljs.core.Set.prototype.cljs$core$ICollection$ = true;
cljs.core.Set.prototype.cljs$core$ICollection$_conj = function(coll, o) {
  var this__4022 = this;
  return new cljs.core.Set(this__4022.meta, cljs.core.assoc.call(null, this__4022.hash_map, o, null))
};
cljs.core.Set.prototype.cljs$core$ISeqable$ = true;
cljs.core.Set.prototype.cljs$core$ISeqable$_seq = function(coll) {
  var this__4023 = this;
  return cljs.core.keys.call(null, this__4023.hash_map)
};
cljs.core.Set.prototype.cljs$core$ISet$ = true;
cljs.core.Set.prototype.cljs$core$ISet$_disjoin = function(coll, v) {
  var this__4024 = this;
  return new cljs.core.Set(this__4024.meta, cljs.core.dissoc.call(null, this__4024.hash_map, v))
};
cljs.core.Set.prototype.cljs$core$ICounted$ = true;
cljs.core.Set.prototype.cljs$core$ICounted$_count = function(coll) {
  var this__4025 = this;
  return cljs.core.count.call(null, cljs.core.seq.call(null, coll))
};
cljs.core.Set.prototype.cljs$core$IEquiv$ = true;
cljs.core.Set.prototype.cljs$core$IEquiv$_equiv = function(coll, other) {
  var this__4026 = this;
  var and__3546__auto____4027 = cljs.core.set_QMARK_.call(null, other);
  if(cljs.core.truth_(and__3546__auto____4027)) {
    var and__3546__auto____4028 = cljs.core._EQ_.call(null, cljs.core.count.call(null, coll), cljs.core.count.call(null, other));
    if(cljs.core.truth_(and__3546__auto____4028)) {
      return cljs.core.every_QMARK_.call(null, function(p1__4004_SHARP_) {
        return cljs.core.contains_QMARK_.call(null, coll, p1__4004_SHARP_)
      }, other)
    }else {
      return and__3546__auto____4028
    }
  }else {
    return and__3546__auto____4027
  }
};
cljs.core.Set.prototype.cljs$core$IWithMeta$ = true;
cljs.core.Set.prototype.cljs$core$IWithMeta$_with_meta = function(coll, meta) {
  var this__4029 = this;
  return new cljs.core.Set(meta, this__4029.hash_map)
};
cljs.core.Set.prototype.cljs$core$IMeta$ = true;
cljs.core.Set.prototype.cljs$core$IMeta$_meta = function(coll) {
  var this__4030 = this;
  return this__4030.meta
};
cljs.core.Set.prototype.cljs$core$IEmptyableCollection$ = true;
cljs.core.Set.prototype.cljs$core$IEmptyableCollection$_empty = function(coll) {
  var this__4031 = this;
  return cljs.core.with_meta.call(null, cljs.core.Set.EMPTY, this__4031.meta)
};
cljs.core.Set;
cljs.core.Set.EMPTY = new cljs.core.Set(null, cljs.core.hash_map.call(null));
cljs.core.set = function set(coll) {
  var in$__4041 = cljs.core.seq.call(null, coll);
  var out__4042 = cljs.core.Set.EMPTY;
  while(true) {
    if(cljs.core.truth_(cljs.core.not.call(null, cljs.core.empty_QMARK_.call(null, in$__4041)))) {
      var G__4043 = cljs.core.rest.call(null, in$__4041);
      var G__4044 = cljs.core.conj.call(null, out__4042, cljs.core.first.call(null, in$__4041));
      in$__4041 = G__4043;
      out__4042 = G__4044;
      continue
    }else {
      return out__4042
    }
    break
  }
};
cljs.core.replace = function replace(smap, coll) {
  if(cljs.core.truth_(cljs.core.vector_QMARK_.call(null, coll))) {
    var n__4045 = cljs.core.count.call(null, coll);
    return cljs.core.reduce.call(null, function(v, i) {
      var temp__3695__auto____4046 = cljs.core.find.call(null, smap, cljs.core.nth.call(null, v, i));
      if(cljs.core.truth_(temp__3695__auto____4046)) {
        var e__4047 = temp__3695__auto____4046;
        return cljs.core.assoc.call(null, v, i, cljs.core.second.call(null, e__4047))
      }else {
        return v
      }
    }, coll, cljs.core.take.call(null, n__4045, cljs.core.iterate.call(null, cljs.core.inc, 0)))
  }else {
    return cljs.core.map.call(null, function(p1__4040_SHARP_) {
      var temp__3695__auto____4048 = cljs.core.find.call(null, smap, p1__4040_SHARP_);
      if(cljs.core.truth_(temp__3695__auto____4048)) {
        var e__4049 = temp__3695__auto____4048;
        return cljs.core.second.call(null, e__4049)
      }else {
        return p1__4040_SHARP_
      }
    }, coll)
  }
};
cljs.core.distinct = function distinct(coll) {
  var step__4057 = function step(xs, seen) {
    return new cljs.core.LazySeq(null, false, function() {
      return function(p__4050, seen) {
        while(true) {
          var vec__4051__4052 = p__4050;
          var f__4053 = cljs.core.nth.call(null, vec__4051__4052, 0, null);
          var xs__4054 = vec__4051__4052;
          var temp__3698__auto____4055 = cljs.core.seq.call(null, xs__4054);
          if(cljs.core.truth_(temp__3698__auto____4055)) {
            var s__4056 = temp__3698__auto____4055;
            if(cljs.core.truth_(cljs.core.contains_QMARK_.call(null, seen, f__4053))) {
              var G__4058 = cljs.core.rest.call(null, s__4056);
              var G__4059 = seen;
              p__4050 = G__4058;
              seen = G__4059;
              continue
            }else {
              return cljs.core.cons.call(null, f__4053, step.call(null, cljs.core.rest.call(null, s__4056), cljs.core.conj.call(null, seen, f__4053)))
            }
          }else {
            return null
          }
          break
        }
      }.call(null, xs, seen)
    })
  };
  return step__4057.call(null, coll, cljs.core.set([]))
};
cljs.core.butlast = function butlast(s) {
  var ret__4060 = cljs.core.Vector.fromArray([]);
  var s__4061 = s;
  while(true) {
    if(cljs.core.truth_(cljs.core.next.call(null, s__4061))) {
      var G__4062 = cljs.core.conj.call(null, ret__4060, cljs.core.first.call(null, s__4061));
      var G__4063 = cljs.core.next.call(null, s__4061);
      ret__4060 = G__4062;
      s__4061 = G__4063;
      continue
    }else {
      return cljs.core.seq.call(null, ret__4060)
    }
    break
  }
};
cljs.core.name = function name(x) {
  if(cljs.core.truth_(cljs.core.string_QMARK_.call(null, x))) {
    return x
  }else {
    if(cljs.core.truth_(function() {
      var or__3548__auto____4064 = cljs.core.keyword_QMARK_.call(null, x);
      if(cljs.core.truth_(or__3548__auto____4064)) {
        return or__3548__auto____4064
      }else {
        return cljs.core.symbol_QMARK_.call(null, x)
      }
    }())) {
      var i__4065 = x.lastIndexOf("/");
      if(cljs.core.truth_(i__4065 < 0)) {
        return cljs.core.subs.call(null, x, 2)
      }else {
        return cljs.core.subs.call(null, x, i__4065 + 1)
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
    var or__3548__auto____4066 = cljs.core.keyword_QMARK_.call(null, x);
    if(cljs.core.truth_(or__3548__auto____4066)) {
      return or__3548__auto____4066
    }else {
      return cljs.core.symbol_QMARK_.call(null, x)
    }
  }())) {
    var i__4067 = x.lastIndexOf("/");
    if(cljs.core.truth_(i__4067 > -1)) {
      return cljs.core.subs.call(null, x, 2, i__4067)
    }else {
      return null
    }
  }else {
    throw new Error(cljs.core.str.call(null, "Doesn't support namespace: ", x));
  }
};
cljs.core.zipmap = function zipmap(keys, vals) {
  var map__4070 = cljs.core.ObjMap.fromObject([], {});
  var ks__4071 = cljs.core.seq.call(null, keys);
  var vs__4072 = cljs.core.seq.call(null, vals);
  while(true) {
    if(cljs.core.truth_(function() {
      var and__3546__auto____4073 = ks__4071;
      if(cljs.core.truth_(and__3546__auto____4073)) {
        return vs__4072
      }else {
        return and__3546__auto____4073
      }
    }())) {
      var G__4074 = cljs.core.assoc.call(null, map__4070, cljs.core.first.call(null, ks__4071), cljs.core.first.call(null, vs__4072));
      var G__4075 = cljs.core.next.call(null, ks__4071);
      var G__4076 = cljs.core.next.call(null, vs__4072);
      map__4070 = G__4074;
      ks__4071 = G__4075;
      vs__4072 = G__4076;
      continue
    }else {
      return map__4070
    }
    break
  }
};
cljs.core.max_key = function() {
  var max_key = null;
  var max_key__4079 = function(k, x) {
    return x
  };
  var max_key__4080 = function(k, x, y) {
    if(cljs.core.truth_(k.call(null, x) > k.call(null, y))) {
      return x
    }else {
      return y
    }
  };
  var max_key__4081 = function() {
    var G__4083__delegate = function(k, x, y, more) {
      return cljs.core.reduce.call(null, function(p1__4068_SHARP_, p2__4069_SHARP_) {
        return max_key.call(null, k, p1__4068_SHARP_, p2__4069_SHARP_)
      }, max_key.call(null, k, x, y), more)
    };
    var G__4083 = function(k, x, y, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
      }
      return G__4083__delegate.call(this, k, x, y, more)
    };
    G__4083.cljs$lang$maxFixedArity = 3;
    G__4083.cljs$lang$applyTo = function(arglist__4084) {
      var k = cljs.core.first(arglist__4084);
      var x = cljs.core.first(cljs.core.next(arglist__4084));
      var y = cljs.core.first(cljs.core.next(cljs.core.next(arglist__4084)));
      var more = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__4084)));
      return G__4083__delegate.call(this, k, x, y, more)
    };
    return G__4083
  }();
  max_key = function(k, x, y, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 2:
        return max_key__4079.call(this, k, x);
      case 3:
        return max_key__4080.call(this, k, x, y);
      default:
        return max_key__4081.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  max_key.cljs$lang$maxFixedArity = 3;
  max_key.cljs$lang$applyTo = max_key__4081.cljs$lang$applyTo;
  return max_key
}();
cljs.core.min_key = function() {
  var min_key = null;
  var min_key__4085 = function(k, x) {
    return x
  };
  var min_key__4086 = function(k, x, y) {
    if(cljs.core.truth_(k.call(null, x) < k.call(null, y))) {
      return x
    }else {
      return y
    }
  };
  var min_key__4087 = function() {
    var G__4089__delegate = function(k, x, y, more) {
      return cljs.core.reduce.call(null, function(p1__4077_SHARP_, p2__4078_SHARP_) {
        return min_key.call(null, k, p1__4077_SHARP_, p2__4078_SHARP_)
      }, min_key.call(null, k, x, y), more)
    };
    var G__4089 = function(k, x, y, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
      }
      return G__4089__delegate.call(this, k, x, y, more)
    };
    G__4089.cljs$lang$maxFixedArity = 3;
    G__4089.cljs$lang$applyTo = function(arglist__4090) {
      var k = cljs.core.first(arglist__4090);
      var x = cljs.core.first(cljs.core.next(arglist__4090));
      var y = cljs.core.first(cljs.core.next(cljs.core.next(arglist__4090)));
      var more = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__4090)));
      return G__4089__delegate.call(this, k, x, y, more)
    };
    return G__4089
  }();
  min_key = function(k, x, y, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 2:
        return min_key__4085.call(this, k, x);
      case 3:
        return min_key__4086.call(this, k, x, y);
      default:
        return min_key__4087.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  min_key.cljs$lang$maxFixedArity = 3;
  min_key.cljs$lang$applyTo = min_key__4087.cljs$lang$applyTo;
  return min_key
}();
cljs.core.partition_all = function() {
  var partition_all = null;
  var partition_all__4093 = function(n, coll) {
    return partition_all.call(null, n, n, coll)
  };
  var partition_all__4094 = function(n, step, coll) {
    return new cljs.core.LazySeq(null, false, function() {
      var temp__3698__auto____4091 = cljs.core.seq.call(null, coll);
      if(cljs.core.truth_(temp__3698__auto____4091)) {
        var s__4092 = temp__3698__auto____4091;
        return cljs.core.cons.call(null, cljs.core.take.call(null, n, s__4092), partition_all.call(null, n, step, cljs.core.drop.call(null, step, s__4092)))
      }else {
        return null
      }
    })
  };
  partition_all = function(n, step, coll) {
    switch(arguments.length) {
      case 2:
        return partition_all__4093.call(this, n, step);
      case 3:
        return partition_all__4094.call(this, n, step, coll)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return partition_all
}();
cljs.core.take_while = function take_while(pred, coll) {
  return new cljs.core.LazySeq(null, false, function() {
    var temp__3698__auto____4096 = cljs.core.seq.call(null, coll);
    if(cljs.core.truth_(temp__3698__auto____4096)) {
      var s__4097 = temp__3698__auto____4096;
      if(cljs.core.truth_(pred.call(null, cljs.core.first.call(null, s__4097)))) {
        return cljs.core.cons.call(null, cljs.core.first.call(null, s__4097), take_while.call(null, pred, cljs.core.rest.call(null, s__4097)))
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
  var this__4098 = this;
  return cljs.core.hash_coll.call(null, rng)
};
cljs.core.Range.prototype.cljs$core$ISequential$ = true;
cljs.core.Range.prototype.cljs$core$ICollection$ = true;
cljs.core.Range.prototype.cljs$core$ICollection$_conj = function(rng, o) {
  var this__4099 = this;
  return cljs.core.cons.call(null, o, rng)
};
cljs.core.Range.prototype.cljs$core$IReduce$ = true;
cljs.core.Range.prototype.cljs$core$IReduce$_reduce = function() {
  var G__4115 = null;
  var G__4115__4116 = function(rng, f) {
    var this__4100 = this;
    return cljs.core.ci_reduce.call(null, rng, f)
  };
  var G__4115__4117 = function(rng, f, s) {
    var this__4101 = this;
    return cljs.core.ci_reduce.call(null, rng, f, s)
  };
  G__4115 = function(rng, f, s) {
    switch(arguments.length) {
      case 2:
        return G__4115__4116.call(this, rng, f);
      case 3:
        return G__4115__4117.call(this, rng, f, s)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__4115
}();
cljs.core.Range.prototype.cljs$core$ISeqable$ = true;
cljs.core.Range.prototype.cljs$core$ISeqable$_seq = function(rng) {
  var this__4102 = this;
  var comp__4103 = cljs.core.truth_(this__4102.step > 0) ? cljs.core._LT_ : cljs.core._GT_;
  if(cljs.core.truth_(comp__4103.call(null, this__4102.start, this__4102.end))) {
    return rng
  }else {
    return null
  }
};
cljs.core.Range.prototype.cljs$core$ICounted$ = true;
cljs.core.Range.prototype.cljs$core$ICounted$_count = function(rng) {
  var this__4104 = this;
  if(cljs.core.truth_(cljs.core.not.call(null, cljs.core._seq.call(null, rng)))) {
    return 0
  }else {
    return Math["ceil"].call(null, (this__4104.end - this__4104.start) / this__4104.step)
  }
};
cljs.core.Range.prototype.cljs$core$ISeq$ = true;
cljs.core.Range.prototype.cljs$core$ISeq$_first = function(rng) {
  var this__4105 = this;
  return this__4105.start
};
cljs.core.Range.prototype.cljs$core$ISeq$_rest = function(rng) {
  var this__4106 = this;
  if(cljs.core.truth_(cljs.core._seq.call(null, rng))) {
    return new cljs.core.Range(this__4106.meta, this__4106.start + this__4106.step, this__4106.end, this__4106.step)
  }else {
    return cljs.core.list.call(null)
  }
};
cljs.core.Range.prototype.cljs$core$IEquiv$ = true;
cljs.core.Range.prototype.cljs$core$IEquiv$_equiv = function(rng, other) {
  var this__4107 = this;
  return cljs.core.equiv_sequential.call(null, rng, other)
};
cljs.core.Range.prototype.cljs$core$IWithMeta$ = true;
cljs.core.Range.prototype.cljs$core$IWithMeta$_with_meta = function(rng, meta) {
  var this__4108 = this;
  return new cljs.core.Range(meta, this__4108.start, this__4108.end, this__4108.step)
};
cljs.core.Range.prototype.cljs$core$IMeta$ = true;
cljs.core.Range.prototype.cljs$core$IMeta$_meta = function(rng) {
  var this__4109 = this;
  return this__4109.meta
};
cljs.core.Range.prototype.cljs$core$IIndexed$ = true;
cljs.core.Range.prototype.cljs$core$IIndexed$_nth = function() {
  var G__4119 = null;
  var G__4119__4120 = function(rng, n) {
    var this__4110 = this;
    if(cljs.core.truth_(n < cljs.core._count.call(null, rng))) {
      return this__4110.start + n * this__4110.step
    }else {
      if(cljs.core.truth_(function() {
        var and__3546__auto____4111 = this__4110.start > this__4110.end;
        if(cljs.core.truth_(and__3546__auto____4111)) {
          return cljs.core._EQ_.call(null, this__4110.step, 0)
        }else {
          return and__3546__auto____4111
        }
      }())) {
        return this__4110.start
      }else {
        throw new Error("Index out of bounds");
      }
    }
  };
  var G__4119__4121 = function(rng, n, not_found) {
    var this__4112 = this;
    if(cljs.core.truth_(n < cljs.core._count.call(null, rng))) {
      return this__4112.start + n * this__4112.step
    }else {
      if(cljs.core.truth_(function() {
        var and__3546__auto____4113 = this__4112.start > this__4112.end;
        if(cljs.core.truth_(and__3546__auto____4113)) {
          return cljs.core._EQ_.call(null, this__4112.step, 0)
        }else {
          return and__3546__auto____4113
        }
      }())) {
        return this__4112.start
      }else {
        return not_found
      }
    }
  };
  G__4119 = function(rng, n, not_found) {
    switch(arguments.length) {
      case 2:
        return G__4119__4120.call(this, rng, n);
      case 3:
        return G__4119__4121.call(this, rng, n, not_found)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return G__4119
}();
cljs.core.Range.prototype.cljs$core$IEmptyableCollection$ = true;
cljs.core.Range.prototype.cljs$core$IEmptyableCollection$_empty = function(rng) {
  var this__4114 = this;
  return cljs.core.with_meta.call(null, cljs.core.List.EMPTY, this__4114.meta)
};
cljs.core.Range;
cljs.core.range = function() {
  var range = null;
  var range__4123 = function() {
    return range.call(null, 0, Number["MAX_VALUE"], 1)
  };
  var range__4124 = function(end) {
    return range.call(null, 0, end, 1)
  };
  var range__4125 = function(start, end) {
    return range.call(null, start, end, 1)
  };
  var range__4126 = function(start, end, step) {
    return new cljs.core.Range(null, start, end, step)
  };
  range = function(start, end, step) {
    switch(arguments.length) {
      case 0:
        return range__4123.call(this);
      case 1:
        return range__4124.call(this, start);
      case 2:
        return range__4125.call(this, start, end);
      case 3:
        return range__4126.call(this, start, end, step)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return range
}();
cljs.core.take_nth = function take_nth(n, coll) {
  return new cljs.core.LazySeq(null, false, function() {
    var temp__3698__auto____4128 = cljs.core.seq.call(null, coll);
    if(cljs.core.truth_(temp__3698__auto____4128)) {
      var s__4129 = temp__3698__auto____4128;
      return cljs.core.cons.call(null, cljs.core.first.call(null, s__4129), take_nth.call(null, n, cljs.core.drop.call(null, n, s__4129)))
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
    var temp__3698__auto____4131 = cljs.core.seq.call(null, coll);
    if(cljs.core.truth_(temp__3698__auto____4131)) {
      var s__4132 = temp__3698__auto____4131;
      var fst__4133 = cljs.core.first.call(null, s__4132);
      var fv__4134 = f.call(null, fst__4133);
      var run__4135 = cljs.core.cons.call(null, fst__4133, cljs.core.take_while.call(null, function(p1__4130_SHARP_) {
        return cljs.core._EQ_.call(null, fv__4134, f.call(null, p1__4130_SHARP_))
      }, cljs.core.next.call(null, s__4132)));
      return cljs.core.cons.call(null, run__4135, partition_by.call(null, f, cljs.core.seq.call(null, cljs.core.drop.call(null, cljs.core.count.call(null, run__4135), s__4132))))
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
  var reductions__4150 = function(f, coll) {
    return new cljs.core.LazySeq(null, false, function() {
      var temp__3695__auto____4146 = cljs.core.seq.call(null, coll);
      if(cljs.core.truth_(temp__3695__auto____4146)) {
        var s__4147 = temp__3695__auto____4146;
        return reductions.call(null, f, cljs.core.first.call(null, s__4147), cljs.core.rest.call(null, s__4147))
      }else {
        return cljs.core.list.call(null, f.call(null))
      }
    })
  };
  var reductions__4151 = function(f, init, coll) {
    return cljs.core.cons.call(null, init, new cljs.core.LazySeq(null, false, function() {
      var temp__3698__auto____4148 = cljs.core.seq.call(null, coll);
      if(cljs.core.truth_(temp__3698__auto____4148)) {
        var s__4149 = temp__3698__auto____4148;
        return reductions.call(null, f, f.call(null, init, cljs.core.first.call(null, s__4149)), cljs.core.rest.call(null, s__4149))
      }else {
        return null
      }
    }))
  };
  reductions = function(f, init, coll) {
    switch(arguments.length) {
      case 2:
        return reductions__4150.call(this, f, init);
      case 3:
        return reductions__4151.call(this, f, init, coll)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return reductions
}();
cljs.core.juxt = function() {
  var juxt = null;
  var juxt__4154 = function(f) {
    return function() {
      var G__4159 = null;
      var G__4159__4160 = function() {
        return cljs.core.vector.call(null, f.call(null))
      };
      var G__4159__4161 = function(x) {
        return cljs.core.vector.call(null, f.call(null, x))
      };
      var G__4159__4162 = function(x, y) {
        return cljs.core.vector.call(null, f.call(null, x, y))
      };
      var G__4159__4163 = function(x, y, z) {
        return cljs.core.vector.call(null, f.call(null, x, y, z))
      };
      var G__4159__4164 = function() {
        var G__4166__delegate = function(x, y, z, args) {
          return cljs.core.vector.call(null, cljs.core.apply.call(null, f, x, y, z, args))
        };
        var G__4166 = function(x, y, z, var_args) {
          var args = null;
          if(goog.isDef(var_args)) {
            args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
          }
          return G__4166__delegate.call(this, x, y, z, args)
        };
        G__4166.cljs$lang$maxFixedArity = 3;
        G__4166.cljs$lang$applyTo = function(arglist__4167) {
          var x = cljs.core.first(arglist__4167);
          var y = cljs.core.first(cljs.core.next(arglist__4167));
          var z = cljs.core.first(cljs.core.next(cljs.core.next(arglist__4167)));
          var args = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__4167)));
          return G__4166__delegate.call(this, x, y, z, args)
        };
        return G__4166
      }();
      G__4159 = function(x, y, z, var_args) {
        var args = var_args;
        switch(arguments.length) {
          case 0:
            return G__4159__4160.call(this);
          case 1:
            return G__4159__4161.call(this, x);
          case 2:
            return G__4159__4162.call(this, x, y);
          case 3:
            return G__4159__4163.call(this, x, y, z);
          default:
            return G__4159__4164.apply(this, arguments)
        }
        throw"Invalid arity: " + arguments.length;
      };
      G__4159.cljs$lang$maxFixedArity = 3;
      G__4159.cljs$lang$applyTo = G__4159__4164.cljs$lang$applyTo;
      return G__4159
    }()
  };
  var juxt__4155 = function(f, g) {
    return function() {
      var G__4168 = null;
      var G__4168__4169 = function() {
        return cljs.core.vector.call(null, f.call(null), g.call(null))
      };
      var G__4168__4170 = function(x) {
        return cljs.core.vector.call(null, f.call(null, x), g.call(null, x))
      };
      var G__4168__4171 = function(x, y) {
        return cljs.core.vector.call(null, f.call(null, x, y), g.call(null, x, y))
      };
      var G__4168__4172 = function(x, y, z) {
        return cljs.core.vector.call(null, f.call(null, x, y, z), g.call(null, x, y, z))
      };
      var G__4168__4173 = function() {
        var G__4175__delegate = function(x, y, z, args) {
          return cljs.core.vector.call(null, cljs.core.apply.call(null, f, x, y, z, args), cljs.core.apply.call(null, g, x, y, z, args))
        };
        var G__4175 = function(x, y, z, var_args) {
          var args = null;
          if(goog.isDef(var_args)) {
            args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
          }
          return G__4175__delegate.call(this, x, y, z, args)
        };
        G__4175.cljs$lang$maxFixedArity = 3;
        G__4175.cljs$lang$applyTo = function(arglist__4176) {
          var x = cljs.core.first(arglist__4176);
          var y = cljs.core.first(cljs.core.next(arglist__4176));
          var z = cljs.core.first(cljs.core.next(cljs.core.next(arglist__4176)));
          var args = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__4176)));
          return G__4175__delegate.call(this, x, y, z, args)
        };
        return G__4175
      }();
      G__4168 = function(x, y, z, var_args) {
        var args = var_args;
        switch(arguments.length) {
          case 0:
            return G__4168__4169.call(this);
          case 1:
            return G__4168__4170.call(this, x);
          case 2:
            return G__4168__4171.call(this, x, y);
          case 3:
            return G__4168__4172.call(this, x, y, z);
          default:
            return G__4168__4173.apply(this, arguments)
        }
        throw"Invalid arity: " + arguments.length;
      };
      G__4168.cljs$lang$maxFixedArity = 3;
      G__4168.cljs$lang$applyTo = G__4168__4173.cljs$lang$applyTo;
      return G__4168
    }()
  };
  var juxt__4156 = function(f, g, h) {
    return function() {
      var G__4177 = null;
      var G__4177__4178 = function() {
        return cljs.core.vector.call(null, f.call(null), g.call(null), h.call(null))
      };
      var G__4177__4179 = function(x) {
        return cljs.core.vector.call(null, f.call(null, x), g.call(null, x), h.call(null, x))
      };
      var G__4177__4180 = function(x, y) {
        return cljs.core.vector.call(null, f.call(null, x, y), g.call(null, x, y), h.call(null, x, y))
      };
      var G__4177__4181 = function(x, y, z) {
        return cljs.core.vector.call(null, f.call(null, x, y, z), g.call(null, x, y, z), h.call(null, x, y, z))
      };
      var G__4177__4182 = function() {
        var G__4184__delegate = function(x, y, z, args) {
          return cljs.core.vector.call(null, cljs.core.apply.call(null, f, x, y, z, args), cljs.core.apply.call(null, g, x, y, z, args), cljs.core.apply.call(null, h, x, y, z, args))
        };
        var G__4184 = function(x, y, z, var_args) {
          var args = null;
          if(goog.isDef(var_args)) {
            args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
          }
          return G__4184__delegate.call(this, x, y, z, args)
        };
        G__4184.cljs$lang$maxFixedArity = 3;
        G__4184.cljs$lang$applyTo = function(arglist__4185) {
          var x = cljs.core.first(arglist__4185);
          var y = cljs.core.first(cljs.core.next(arglist__4185));
          var z = cljs.core.first(cljs.core.next(cljs.core.next(arglist__4185)));
          var args = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__4185)));
          return G__4184__delegate.call(this, x, y, z, args)
        };
        return G__4184
      }();
      G__4177 = function(x, y, z, var_args) {
        var args = var_args;
        switch(arguments.length) {
          case 0:
            return G__4177__4178.call(this);
          case 1:
            return G__4177__4179.call(this, x);
          case 2:
            return G__4177__4180.call(this, x, y);
          case 3:
            return G__4177__4181.call(this, x, y, z);
          default:
            return G__4177__4182.apply(this, arguments)
        }
        throw"Invalid arity: " + arguments.length;
      };
      G__4177.cljs$lang$maxFixedArity = 3;
      G__4177.cljs$lang$applyTo = G__4177__4182.cljs$lang$applyTo;
      return G__4177
    }()
  };
  var juxt__4157 = function() {
    var G__4186__delegate = function(f, g, h, fs) {
      var fs__4153 = cljs.core.list_STAR_.call(null, f, g, h, fs);
      return function() {
        var G__4187 = null;
        var G__4187__4188 = function() {
          return cljs.core.reduce.call(null, function(p1__4136_SHARP_, p2__4137_SHARP_) {
            return cljs.core.conj.call(null, p1__4136_SHARP_, p2__4137_SHARP_.call(null))
          }, cljs.core.Vector.fromArray([]), fs__4153)
        };
        var G__4187__4189 = function(x) {
          return cljs.core.reduce.call(null, function(p1__4138_SHARP_, p2__4139_SHARP_) {
            return cljs.core.conj.call(null, p1__4138_SHARP_, p2__4139_SHARP_.call(null, x))
          }, cljs.core.Vector.fromArray([]), fs__4153)
        };
        var G__4187__4190 = function(x, y) {
          return cljs.core.reduce.call(null, function(p1__4140_SHARP_, p2__4141_SHARP_) {
            return cljs.core.conj.call(null, p1__4140_SHARP_, p2__4141_SHARP_.call(null, x, y))
          }, cljs.core.Vector.fromArray([]), fs__4153)
        };
        var G__4187__4191 = function(x, y, z) {
          return cljs.core.reduce.call(null, function(p1__4142_SHARP_, p2__4143_SHARP_) {
            return cljs.core.conj.call(null, p1__4142_SHARP_, p2__4143_SHARP_.call(null, x, y, z))
          }, cljs.core.Vector.fromArray([]), fs__4153)
        };
        var G__4187__4192 = function() {
          var G__4194__delegate = function(x, y, z, args) {
            return cljs.core.reduce.call(null, function(p1__4144_SHARP_, p2__4145_SHARP_) {
              return cljs.core.conj.call(null, p1__4144_SHARP_, cljs.core.apply.call(null, p2__4145_SHARP_, x, y, z, args))
            }, cljs.core.Vector.fromArray([]), fs__4153)
          };
          var G__4194 = function(x, y, z, var_args) {
            var args = null;
            if(goog.isDef(var_args)) {
              args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
            }
            return G__4194__delegate.call(this, x, y, z, args)
          };
          G__4194.cljs$lang$maxFixedArity = 3;
          G__4194.cljs$lang$applyTo = function(arglist__4195) {
            var x = cljs.core.first(arglist__4195);
            var y = cljs.core.first(cljs.core.next(arglist__4195));
            var z = cljs.core.first(cljs.core.next(cljs.core.next(arglist__4195)));
            var args = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__4195)));
            return G__4194__delegate.call(this, x, y, z, args)
          };
          return G__4194
        }();
        G__4187 = function(x, y, z, var_args) {
          var args = var_args;
          switch(arguments.length) {
            case 0:
              return G__4187__4188.call(this);
            case 1:
              return G__4187__4189.call(this, x);
            case 2:
              return G__4187__4190.call(this, x, y);
            case 3:
              return G__4187__4191.call(this, x, y, z);
            default:
              return G__4187__4192.apply(this, arguments)
          }
          throw"Invalid arity: " + arguments.length;
        };
        G__4187.cljs$lang$maxFixedArity = 3;
        G__4187.cljs$lang$applyTo = G__4187__4192.cljs$lang$applyTo;
        return G__4187
      }()
    };
    var G__4186 = function(f, g, h, var_args) {
      var fs = null;
      if(goog.isDef(var_args)) {
        fs = cljs.core.array_seq(Array.prototype.slice.call(arguments, 3), 0)
      }
      return G__4186__delegate.call(this, f, g, h, fs)
    };
    G__4186.cljs$lang$maxFixedArity = 3;
    G__4186.cljs$lang$applyTo = function(arglist__4196) {
      var f = cljs.core.first(arglist__4196);
      var g = cljs.core.first(cljs.core.next(arglist__4196));
      var h = cljs.core.first(cljs.core.next(cljs.core.next(arglist__4196)));
      var fs = cljs.core.rest(cljs.core.next(cljs.core.next(arglist__4196)));
      return G__4186__delegate.call(this, f, g, h, fs)
    };
    return G__4186
  }();
  juxt = function(f, g, h, var_args) {
    var fs = var_args;
    switch(arguments.length) {
      case 1:
        return juxt__4154.call(this, f);
      case 2:
        return juxt__4155.call(this, f, g);
      case 3:
        return juxt__4156.call(this, f, g, h);
      default:
        return juxt__4157.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  juxt.cljs$lang$maxFixedArity = 3;
  juxt.cljs$lang$applyTo = juxt__4157.cljs$lang$applyTo;
  return juxt
}();
cljs.core.dorun = function() {
  var dorun = null;
  var dorun__4198 = function(coll) {
    while(true) {
      if(cljs.core.truth_(cljs.core.seq.call(null, coll))) {
        var G__4201 = cljs.core.next.call(null, coll);
        coll = G__4201;
        continue
      }else {
        return null
      }
      break
    }
  };
  var dorun__4199 = function(n, coll) {
    while(true) {
      if(cljs.core.truth_(function() {
        var and__3546__auto____4197 = cljs.core.seq.call(null, coll);
        if(cljs.core.truth_(and__3546__auto____4197)) {
          return n > 0
        }else {
          return and__3546__auto____4197
        }
      }())) {
        var G__4202 = n - 1;
        var G__4203 = cljs.core.next.call(null, coll);
        n = G__4202;
        coll = G__4203;
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
        return dorun__4198.call(this, n);
      case 2:
        return dorun__4199.call(this, n, coll)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return dorun
}();
cljs.core.doall = function() {
  var doall = null;
  var doall__4204 = function(coll) {
    cljs.core.dorun.call(null, coll);
    return coll
  };
  var doall__4205 = function(n, coll) {
    cljs.core.dorun.call(null, n, coll);
    return coll
  };
  doall = function(n, coll) {
    switch(arguments.length) {
      case 1:
        return doall__4204.call(this, n);
      case 2:
        return doall__4205.call(this, n, coll)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return doall
}();
cljs.core.re_matches = function re_matches(re, s) {
  var matches__4207 = re.exec(s);
  if(cljs.core.truth_(cljs.core._EQ_.call(null, cljs.core.first.call(null, matches__4207), s))) {
    if(cljs.core.truth_(cljs.core._EQ_.call(null, cljs.core.count.call(null, matches__4207), 1))) {
      return cljs.core.first.call(null, matches__4207)
    }else {
      return cljs.core.vec.call(null, matches__4207)
    }
  }else {
    return null
  }
};
cljs.core.re_find = function re_find(re, s) {
  var matches__4208 = re.exec(s);
  if(cljs.core.truth_(matches__4208 === null)) {
    return null
  }else {
    if(cljs.core.truth_(cljs.core._EQ_.call(null, cljs.core.count.call(null, matches__4208), 1))) {
      return cljs.core.first.call(null, matches__4208)
    }else {
      return cljs.core.vec.call(null, matches__4208)
    }
  }
};
cljs.core.re_seq = function re_seq(re, s) {
  var match_data__4209 = cljs.core.re_find.call(null, re, s);
  var match_idx__4210 = s.search(re);
  var match_str__4211 = cljs.core.truth_(cljs.core.coll_QMARK_.call(null, match_data__4209)) ? cljs.core.first.call(null, match_data__4209) : match_data__4209;
  var post_match__4212 = cljs.core.subs.call(null, s, match_idx__4210 + cljs.core.count.call(null, match_str__4211));
  if(cljs.core.truth_(match_data__4209)) {
    return new cljs.core.LazySeq(null, false, function() {
      return cljs.core.cons.call(null, match_data__4209, re_seq.call(null, re, post_match__4212))
    })
  }else {
    return null
  }
};
cljs.core.re_pattern = function re_pattern(s) {
  var vec__4214__4215 = cljs.core.re_find.call(null, /^(?:\(\?([idmsux]*)\))?(.*)/, s);
  var ___4216 = cljs.core.nth.call(null, vec__4214__4215, 0, null);
  var flags__4217 = cljs.core.nth.call(null, vec__4214__4215, 1, null);
  var pattern__4218 = cljs.core.nth.call(null, vec__4214__4215, 2, null);
  return new RegExp(pattern__4218, flags__4217)
};
cljs.core.pr_sequential = function pr_sequential(print_one, begin, sep, end, opts, coll) {
  return cljs.core.concat.call(null, cljs.core.Vector.fromArray([begin]), cljs.core.flatten1.call(null, cljs.core.interpose.call(null, cljs.core.Vector.fromArray([sep]), cljs.core.map.call(null, function(p1__4213_SHARP_) {
    return print_one.call(null, p1__4213_SHARP_, opts)
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
          var and__3546__auto____4219 = cljs.core.get.call(null, opts, "\ufdd0'meta");
          if(cljs.core.truth_(and__3546__auto____4219)) {
            var and__3546__auto____4223 = function() {
              var x__352__auto____4220 = obj;
              if(cljs.core.truth_(function() {
                var and__3546__auto____4221 = x__352__auto____4220;
                if(cljs.core.truth_(and__3546__auto____4221)) {
                  var and__3546__auto____4222 = x__352__auto____4220.cljs$core$IMeta$;
                  if(cljs.core.truth_(and__3546__auto____4222)) {
                    return cljs.core.not.call(null, x__352__auto____4220.hasOwnProperty("cljs$core$IMeta$"))
                  }else {
                    return and__3546__auto____4222
                  }
                }else {
                  return and__3546__auto____4221
                }
              }())) {
                return true
              }else {
                return cljs.core.type_satisfies_.call(null, cljs.core.IMeta, x__352__auto____4220)
              }
            }();
            if(cljs.core.truth_(and__3546__auto____4223)) {
              return cljs.core.meta.call(null, obj)
            }else {
              return and__3546__auto____4223
            }
          }else {
            return and__3546__auto____4219
          }
        }()) ? cljs.core.concat.call(null, cljs.core.Vector.fromArray(["^"]), pr_seq.call(null, cljs.core.meta.call(null, obj), opts), cljs.core.Vector.fromArray([" "])) : null, cljs.core.truth_(function() {
          var x__352__auto____4224 = obj;
          if(cljs.core.truth_(function() {
            var and__3546__auto____4225 = x__352__auto____4224;
            if(cljs.core.truth_(and__3546__auto____4225)) {
              var and__3546__auto____4226 = x__352__auto____4224.cljs$core$IPrintable$;
              if(cljs.core.truth_(and__3546__auto____4226)) {
                return cljs.core.not.call(null, x__352__auto____4224.hasOwnProperty("cljs$core$IPrintable$"))
              }else {
                return and__3546__auto____4226
              }
            }else {
              return and__3546__auto____4225
            }
          }())) {
            return true
          }else {
            return cljs.core.type_satisfies_.call(null, cljs.core.IPrintable, x__352__auto____4224)
          }
        }()) ? cljs.core._pr_seq.call(null, obj, opts) : cljs.core.list.call(null, "#<", cljs.core.str.call(null, obj), ">"))
      }else {
        return null
      }
    }
  }
};
cljs.core.pr_str_with_opts = function pr_str_with_opts(objs, opts) {
  var first_obj__4227 = cljs.core.first.call(null, objs);
  var sb__4228 = new goog.string.StringBuffer;
  var G__4229__4230 = cljs.core.seq.call(null, objs);
  if(cljs.core.truth_(G__4229__4230)) {
    var obj__4231 = cljs.core.first.call(null, G__4229__4230);
    var G__4229__4232 = G__4229__4230;
    while(true) {
      if(cljs.core.truth_(obj__4231 === first_obj__4227)) {
      }else {
        sb__4228.append(" ")
      }
      var G__4233__4234 = cljs.core.seq.call(null, cljs.core.pr_seq.call(null, obj__4231, opts));
      if(cljs.core.truth_(G__4233__4234)) {
        var string__4235 = cljs.core.first.call(null, G__4233__4234);
        var G__4233__4236 = G__4233__4234;
        while(true) {
          sb__4228.append(string__4235);
          var temp__3698__auto____4237 = cljs.core.next.call(null, G__4233__4236);
          if(cljs.core.truth_(temp__3698__auto____4237)) {
            var G__4233__4238 = temp__3698__auto____4237;
            var G__4241 = cljs.core.first.call(null, G__4233__4238);
            var G__4242 = G__4233__4238;
            string__4235 = G__4241;
            G__4233__4236 = G__4242;
            continue
          }else {
          }
          break
        }
      }else {
      }
      var temp__3698__auto____4239 = cljs.core.next.call(null, G__4229__4232);
      if(cljs.core.truth_(temp__3698__auto____4239)) {
        var G__4229__4240 = temp__3698__auto____4239;
        var G__4243 = cljs.core.first.call(null, G__4229__4240);
        var G__4244 = G__4229__4240;
        obj__4231 = G__4243;
        G__4229__4232 = G__4244;
        continue
      }else {
      }
      break
    }
  }else {
  }
  return cljs.core.str.call(null, sb__4228)
};
cljs.core.pr_with_opts = function pr_with_opts(objs, opts) {
  var first_obj__4245 = cljs.core.first.call(null, objs);
  var G__4246__4247 = cljs.core.seq.call(null, objs);
  if(cljs.core.truth_(G__4246__4247)) {
    var obj__4248 = cljs.core.first.call(null, G__4246__4247);
    var G__4246__4249 = G__4246__4247;
    while(true) {
      if(cljs.core.truth_(obj__4248 === first_obj__4245)) {
      }else {
        cljs.core.string_print.call(null, " ")
      }
      var G__4250__4251 = cljs.core.seq.call(null, cljs.core.pr_seq.call(null, obj__4248, opts));
      if(cljs.core.truth_(G__4250__4251)) {
        var string__4252 = cljs.core.first.call(null, G__4250__4251);
        var G__4250__4253 = G__4250__4251;
        while(true) {
          cljs.core.string_print.call(null, string__4252);
          var temp__3698__auto____4254 = cljs.core.next.call(null, G__4250__4253);
          if(cljs.core.truth_(temp__3698__auto____4254)) {
            var G__4250__4255 = temp__3698__auto____4254;
            var G__4258 = cljs.core.first.call(null, G__4250__4255);
            var G__4259 = G__4250__4255;
            string__4252 = G__4258;
            G__4250__4253 = G__4259;
            continue
          }else {
          }
          break
        }
      }else {
      }
      var temp__3698__auto____4256 = cljs.core.next.call(null, G__4246__4249);
      if(cljs.core.truth_(temp__3698__auto____4256)) {
        var G__4246__4257 = temp__3698__auto____4256;
        var G__4260 = cljs.core.first.call(null, G__4246__4257);
        var G__4261 = G__4246__4257;
        obj__4248 = G__4260;
        G__4246__4249 = G__4261;
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
  pr_str.cljs$lang$applyTo = function(arglist__4262) {
    var objs = cljs.core.seq(arglist__4262);
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
  pr.cljs$lang$applyTo = function(arglist__4263) {
    var objs = cljs.core.seq(arglist__4263);
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
  cljs_core_print.cljs$lang$applyTo = function(arglist__4264) {
    var objs = cljs.core.seq(arglist__4264);
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
  println.cljs$lang$applyTo = function(arglist__4265) {
    var objs = cljs.core.seq(arglist__4265);
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
  prn.cljs$lang$applyTo = function(arglist__4266) {
    var objs = cljs.core.seq(arglist__4266);
    return prn__delegate.call(this, objs)
  };
  return prn
}();
cljs.core.HashMap.prototype.cljs$core$IPrintable$ = true;
cljs.core.HashMap.prototype.cljs$core$IPrintable$_pr_seq = function(coll, opts) {
  var pr_pair__4267 = function(keyval) {
    return cljs.core.pr_sequential.call(null, cljs.core.pr_seq, "", " ", "", opts, keyval)
  };
  return cljs.core.pr_sequential.call(null, pr_pair__4267, "{", ", ", "}", opts, coll)
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
      var temp__3698__auto____4268 = cljs.core.namespace.call(null, obj);
      if(cljs.core.truth_(temp__3698__auto____4268)) {
        var nspc__4269 = temp__3698__auto____4268;
        return cljs.core.str.call(null, nspc__4269, "/")
      }else {
        return null
      }
    }(), cljs.core.name.call(null, obj)))
  }else {
    if(cljs.core.truth_(cljs.core.symbol_QMARK_.call(null, obj))) {
      return cljs.core.list.call(null, cljs.core.str.call(null, function() {
        var temp__3698__auto____4270 = cljs.core.namespace.call(null, obj);
        if(cljs.core.truth_(temp__3698__auto____4270)) {
          var nspc__4271 = temp__3698__auto____4270;
          return cljs.core.str.call(null, nspc__4271, "/")
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
  var pr_pair__4272 = function(keyval) {
    return cljs.core.pr_sequential.call(null, cljs.core.pr_seq, "", " ", "", opts, keyval)
  };
  return cljs.core.pr_sequential.call(null, pr_pair__4272, "{", ", ", "}", opts, coll)
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
  var this__4273 = this;
  return goog.getUid.call(null, this$)
};
cljs.core.Atom.prototype.cljs$core$IWatchable$ = true;
cljs.core.Atom.prototype.cljs$core$IWatchable$_notify_watches = function(this$, oldval, newval) {
  var this__4274 = this;
  var G__4275__4276 = cljs.core.seq.call(null, this__4274.watches);
  if(cljs.core.truth_(G__4275__4276)) {
    var G__4278__4280 = cljs.core.first.call(null, G__4275__4276);
    var vec__4279__4281 = G__4278__4280;
    var key__4282 = cljs.core.nth.call(null, vec__4279__4281, 0, null);
    var f__4283 = cljs.core.nth.call(null, vec__4279__4281, 1, null);
    var G__4275__4284 = G__4275__4276;
    var G__4278__4285 = G__4278__4280;
    var G__4275__4286 = G__4275__4284;
    while(true) {
      var vec__4287__4288 = G__4278__4285;
      var key__4289 = cljs.core.nth.call(null, vec__4287__4288, 0, null);
      var f__4290 = cljs.core.nth.call(null, vec__4287__4288, 1, null);
      var G__4275__4291 = G__4275__4286;
      f__4290.call(null, key__4289, this$, oldval, newval);
      var temp__3698__auto____4292 = cljs.core.next.call(null, G__4275__4291);
      if(cljs.core.truth_(temp__3698__auto____4292)) {
        var G__4275__4293 = temp__3698__auto____4292;
        var G__4300 = cljs.core.first.call(null, G__4275__4293);
        var G__4301 = G__4275__4293;
        G__4278__4285 = G__4300;
        G__4275__4286 = G__4301;
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
  var this__4294 = this;
  return this$.watches = cljs.core.assoc.call(null, this__4294.watches, key, f)
};
cljs.core.Atom.prototype.cljs$core$IWatchable$_remove_watch = function(this$, key) {
  var this__4295 = this;
  return this$.watches = cljs.core.dissoc.call(null, this__4295.watches, key)
};
cljs.core.Atom.prototype.cljs$core$IPrintable$ = true;
cljs.core.Atom.prototype.cljs$core$IPrintable$_pr_seq = function(a, opts) {
  var this__4296 = this;
  return cljs.core.concat.call(null, cljs.core.Vector.fromArray(["#<Atom: "]), cljs.core._pr_seq.call(null, this__4296.state, opts), ">")
};
cljs.core.Atom.prototype.cljs$core$IMeta$ = true;
cljs.core.Atom.prototype.cljs$core$IMeta$_meta = function(_) {
  var this__4297 = this;
  return this__4297.meta
};
cljs.core.Atom.prototype.cljs$core$IDeref$ = true;
cljs.core.Atom.prototype.cljs$core$IDeref$_deref = function(_) {
  var this__4298 = this;
  return this__4298.state
};
cljs.core.Atom.prototype.cljs$core$IEquiv$ = true;
cljs.core.Atom.prototype.cljs$core$IEquiv$_equiv = function(o, other) {
  var this__4299 = this;
  return o === other
};
cljs.core.Atom;
cljs.core.atom = function() {
  var atom = null;
  var atom__4308 = function(x) {
    return new cljs.core.Atom(x, null, null, null)
  };
  var atom__4309 = function() {
    var G__4311__delegate = function(x, p__4302) {
      var map__4303__4304 = p__4302;
      var map__4303__4305 = cljs.core.truth_(cljs.core.seq_QMARK_.call(null, map__4303__4304)) ? cljs.core.apply.call(null, cljs.core.hash_map, map__4303__4304) : map__4303__4304;
      var validator__4306 = cljs.core.get.call(null, map__4303__4305, "\ufdd0'validator");
      var meta__4307 = cljs.core.get.call(null, map__4303__4305, "\ufdd0'meta");
      return new cljs.core.Atom(x, meta__4307, validator__4306, null)
    };
    var G__4311 = function(x, var_args) {
      var p__4302 = null;
      if(goog.isDef(var_args)) {
        p__4302 = cljs.core.array_seq(Array.prototype.slice.call(arguments, 1), 0)
      }
      return G__4311__delegate.call(this, x, p__4302)
    };
    G__4311.cljs$lang$maxFixedArity = 1;
    G__4311.cljs$lang$applyTo = function(arglist__4312) {
      var x = cljs.core.first(arglist__4312);
      var p__4302 = cljs.core.rest(arglist__4312);
      return G__4311__delegate.call(this, x, p__4302)
    };
    return G__4311
  }();
  atom = function(x, var_args) {
    var p__4302 = var_args;
    switch(arguments.length) {
      case 1:
        return atom__4308.call(this, x);
      default:
        return atom__4309.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  atom.cljs$lang$maxFixedArity = 1;
  atom.cljs$lang$applyTo = atom__4309.cljs$lang$applyTo;
  return atom
}();
cljs.core.reset_BANG_ = function reset_BANG_(a, new_value) {
  var temp__3698__auto____4313 = a.validator;
  if(cljs.core.truth_(temp__3698__auto____4313)) {
    var validate__4314 = temp__3698__auto____4313;
    if(cljs.core.truth_(validate__4314.call(null, new_value))) {
    }else {
      throw new Error(cljs.core.str.call(null, "Assert failed: ", "Validator rejected reference state", "\n", cljs.core.pr_str.call(null, cljs.core.with_meta(cljs.core.list("\ufdd1'validate", "\ufdd1'new-value"), cljs.core.hash_map("\ufdd0'line", 3073)))));
    }
  }else {
  }
  var old_value__4315 = a.state;
  a.state = new_value;
  cljs.core._notify_watches.call(null, a, old_value__4315, new_value);
  return new_value
};
cljs.core.swap_BANG_ = function() {
  var swap_BANG_ = null;
  var swap_BANG___4316 = function(a, f) {
    return cljs.core.reset_BANG_.call(null, a, f.call(null, a.state))
  };
  var swap_BANG___4317 = function(a, f, x) {
    return cljs.core.reset_BANG_.call(null, a, f.call(null, a.state, x))
  };
  var swap_BANG___4318 = function(a, f, x, y) {
    return cljs.core.reset_BANG_.call(null, a, f.call(null, a.state, x, y))
  };
  var swap_BANG___4319 = function(a, f, x, y, z) {
    return cljs.core.reset_BANG_.call(null, a, f.call(null, a.state, x, y, z))
  };
  var swap_BANG___4320 = function() {
    var G__4322__delegate = function(a, f, x, y, z, more) {
      return cljs.core.reset_BANG_.call(null, a, cljs.core.apply.call(null, f, a.state, x, y, z, more))
    };
    var G__4322 = function(a, f, x, y, z, var_args) {
      var more = null;
      if(goog.isDef(var_args)) {
        more = cljs.core.array_seq(Array.prototype.slice.call(arguments, 5), 0)
      }
      return G__4322__delegate.call(this, a, f, x, y, z, more)
    };
    G__4322.cljs$lang$maxFixedArity = 5;
    G__4322.cljs$lang$applyTo = function(arglist__4323) {
      var a = cljs.core.first(arglist__4323);
      var f = cljs.core.first(cljs.core.next(arglist__4323));
      var x = cljs.core.first(cljs.core.next(cljs.core.next(arglist__4323)));
      var y = cljs.core.first(cljs.core.next(cljs.core.next(cljs.core.next(arglist__4323))));
      var z = cljs.core.first(cljs.core.next(cljs.core.next(cljs.core.next(cljs.core.next(arglist__4323)))));
      var more = cljs.core.rest(cljs.core.next(cljs.core.next(cljs.core.next(cljs.core.next(arglist__4323)))));
      return G__4322__delegate.call(this, a, f, x, y, z, more)
    };
    return G__4322
  }();
  swap_BANG_ = function(a, f, x, y, z, var_args) {
    var more = var_args;
    switch(arguments.length) {
      case 2:
        return swap_BANG___4316.call(this, a, f);
      case 3:
        return swap_BANG___4317.call(this, a, f, x);
      case 4:
        return swap_BANG___4318.call(this, a, f, x, y);
      case 5:
        return swap_BANG___4319.call(this, a, f, x, y, z);
      default:
        return swap_BANG___4320.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  swap_BANG_.cljs$lang$maxFixedArity = 5;
  swap_BANG_.cljs$lang$applyTo = swap_BANG___4320.cljs$lang$applyTo;
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
  alter_meta_BANG_.cljs$lang$applyTo = function(arglist__4324) {
    var iref = cljs.core.first(arglist__4324);
    var f = cljs.core.first(cljs.core.next(arglist__4324));
    var args = cljs.core.rest(cljs.core.next(arglist__4324));
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
  var gensym__4325 = function() {
    return gensym.call(null, "G__")
  };
  var gensym__4326 = function(prefix_string) {
    if(cljs.core.truth_(cljs.core.gensym_counter === null)) {
      cljs.core.gensym_counter = cljs.core.atom.call(null, 0)
    }else {
    }
    return cljs.core.symbol.call(null, cljs.core.str.call(null, prefix_string, cljs.core.swap_BANG_.call(null, cljs.core.gensym_counter, cljs.core.inc)))
  };
  gensym = function(prefix_string) {
    switch(arguments.length) {
      case 0:
        return gensym__4325.call(this);
      case 1:
        return gensym__4326.call(this, prefix_string)
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
  var this__4328 = this;
  return cljs.core.not.call(null, cljs.core.deref.call(null, this__4328.state) === null)
};
cljs.core.Delay.prototype.cljs$core$IDeref$ = true;
cljs.core.Delay.prototype.cljs$core$IDeref$_deref = function(_) {
  var this__4329 = this;
  if(cljs.core.truth_(cljs.core.deref.call(null, this__4329.state))) {
  }else {
    cljs.core.swap_BANG_.call(null, this__4329.state, this__4329.f)
  }
  return cljs.core.deref.call(null, this__4329.state)
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
  delay.cljs$lang$applyTo = function(arglist__4330) {
    var body = cljs.core.seq(arglist__4330);
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
    var map__4331__4332 = options;
    var map__4331__4333 = cljs.core.truth_(cljs.core.seq_QMARK_.call(null, map__4331__4332)) ? cljs.core.apply.call(null, cljs.core.hash_map, map__4331__4332) : map__4331__4332;
    var keywordize_keys__4334 = cljs.core.get.call(null, map__4331__4333, "\ufdd0'keywordize-keys");
    var keyfn__4335 = cljs.core.truth_(keywordize_keys__4334) ? cljs.core.keyword : cljs.core.str;
    var f__4341 = function thisfn(x) {
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
                var iter__416__auto____4340 = function iter__4336(s__4337) {
                  return new cljs.core.LazySeq(null, false, function() {
                    var s__4337__4338 = s__4337;
                    while(true) {
                      if(cljs.core.truth_(cljs.core.seq.call(null, s__4337__4338))) {
                        var k__4339 = cljs.core.first.call(null, s__4337__4338);
                        return cljs.core.cons.call(null, cljs.core.Vector.fromArray([keyfn__4335.call(null, k__4339), thisfn.call(null, x[k__4339])]), iter__4336.call(null, cljs.core.rest.call(null, s__4337__4338)))
                      }else {
                        return null
                      }
                      break
                    }
                  })
                };
                return iter__416__auto____4340.call(null, cljs.core.js_keys.call(null, x))
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
    return f__4341.call(null, x)
  };
  var js__GT_clj = function(x, var_args) {
    var options = null;
    if(goog.isDef(var_args)) {
      options = cljs.core.array_seq(Array.prototype.slice.call(arguments, 1), 0)
    }
    return js__GT_clj__delegate.call(this, x, options)
  };
  js__GT_clj.cljs$lang$maxFixedArity = 1;
  js__GT_clj.cljs$lang$applyTo = function(arglist__4342) {
    var x = cljs.core.first(arglist__4342);
    var options = cljs.core.rest(arglist__4342);
    return js__GT_clj__delegate.call(this, x, options)
  };
  return js__GT_clj
}();
cljs.core.memoize = function memoize(f) {
  var mem__4343 = cljs.core.atom.call(null, cljs.core.ObjMap.fromObject([], {}));
  return function() {
    var G__4347__delegate = function(args) {
      var temp__3695__auto____4344 = cljs.core.get.call(null, cljs.core.deref.call(null, mem__4343), args);
      if(cljs.core.truth_(temp__3695__auto____4344)) {
        var v__4345 = temp__3695__auto____4344;
        return v__4345
      }else {
        var ret__4346 = cljs.core.apply.call(null, f, args);
        cljs.core.swap_BANG_.call(null, mem__4343, cljs.core.assoc, args, ret__4346);
        return ret__4346
      }
    };
    var G__4347 = function(var_args) {
      var args = null;
      if(goog.isDef(var_args)) {
        args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 0), 0)
      }
      return G__4347__delegate.call(this, args)
    };
    G__4347.cljs$lang$maxFixedArity = 0;
    G__4347.cljs$lang$applyTo = function(arglist__4348) {
      var args = cljs.core.seq(arglist__4348);
      return G__4347__delegate.call(this, args)
    };
    return G__4347
  }()
};
cljs.core.trampoline = function() {
  var trampoline = null;
  var trampoline__4350 = function(f) {
    while(true) {
      var ret__4349 = f.call(null);
      if(cljs.core.truth_(cljs.core.fn_QMARK_.call(null, ret__4349))) {
        var G__4353 = ret__4349;
        f = G__4353;
        continue
      }else {
        return ret__4349
      }
      break
    }
  };
  var trampoline__4351 = function() {
    var G__4354__delegate = function(f, args) {
      return trampoline.call(null, function() {
        return cljs.core.apply.call(null, f, args)
      })
    };
    var G__4354 = function(f, var_args) {
      var args = null;
      if(goog.isDef(var_args)) {
        args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 1), 0)
      }
      return G__4354__delegate.call(this, f, args)
    };
    G__4354.cljs$lang$maxFixedArity = 1;
    G__4354.cljs$lang$applyTo = function(arglist__4355) {
      var f = cljs.core.first(arglist__4355);
      var args = cljs.core.rest(arglist__4355);
      return G__4354__delegate.call(this, f, args)
    };
    return G__4354
  }();
  trampoline = function(f, var_args) {
    var args = var_args;
    switch(arguments.length) {
      case 1:
        return trampoline__4350.call(this, f);
      default:
        return trampoline__4351.apply(this, arguments)
    }
    throw"Invalid arity: " + arguments.length;
  };
  trampoline.cljs$lang$maxFixedArity = 1;
  trampoline.cljs$lang$applyTo = trampoline__4351.cljs$lang$applyTo;
  return trampoline
}();
cljs.core.rand = function() {
  var rand = null;
  var rand__4356 = function() {
    return rand.call(null, 1)
  };
  var rand__4357 = function(n) {
    return Math.random() * n
  };
  rand = function(n) {
    switch(arguments.length) {
      case 0:
        return rand__4356.call(this);
      case 1:
        return rand__4357.call(this, n)
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
    var k__4359 = f.call(null, x);
    return cljs.core.assoc.call(null, ret, k__4359, cljs.core.conj.call(null, cljs.core.get.call(null, ret, k__4359, cljs.core.Vector.fromArray([])), x))
  }, cljs.core.ObjMap.fromObject([], {}), coll)
};
cljs.core.make_hierarchy = function make_hierarchy() {
  return cljs.core.ObjMap.fromObject(["\ufdd0'parents", "\ufdd0'descendants", "\ufdd0'ancestors"], {"\ufdd0'parents":cljs.core.ObjMap.fromObject([], {}), "\ufdd0'descendants":cljs.core.ObjMap.fromObject([], {}), "\ufdd0'ancestors":cljs.core.ObjMap.fromObject([], {})})
};
cljs.core.global_hierarchy = cljs.core.atom.call(null, cljs.core.make_hierarchy.call(null));
cljs.core.isa_QMARK_ = function() {
  var isa_QMARK_ = null;
  var isa_QMARK___4368 = function(child, parent) {
    return isa_QMARK_.call(null, cljs.core.deref.call(null, cljs.core.global_hierarchy), child, parent)
  };
  var isa_QMARK___4369 = function(h, child, parent) {
    var or__3548__auto____4360 = cljs.core._EQ_.call(null, child, parent);
    if(cljs.core.truth_(or__3548__auto____4360)) {
      return or__3548__auto____4360
    }else {
      var or__3548__auto____4361 = cljs.core.contains_QMARK_.call(null, "\ufdd0'ancestors".call(null, h).call(null, child), parent);
      if(cljs.core.truth_(or__3548__auto____4361)) {
        return or__3548__auto____4361
      }else {
        var and__3546__auto____4362 = cljs.core.vector_QMARK_.call(null, parent);
        if(cljs.core.truth_(and__3546__auto____4362)) {
          var and__3546__auto____4363 = cljs.core.vector_QMARK_.call(null, child);
          if(cljs.core.truth_(and__3546__auto____4363)) {
            var and__3546__auto____4364 = cljs.core._EQ_.call(null, cljs.core.count.call(null, parent), cljs.core.count.call(null, child));
            if(cljs.core.truth_(and__3546__auto____4364)) {
              var ret__4365 = true;
              var i__4366 = 0;
              while(true) {
                if(cljs.core.truth_(function() {
                  var or__3548__auto____4367 = cljs.core.not.call(null, ret__4365);
                  if(cljs.core.truth_(or__3548__auto____4367)) {
                    return or__3548__auto____4367
                  }else {
                    return cljs.core._EQ_.call(null, i__4366, cljs.core.count.call(null, parent))
                  }
                }())) {
                  return ret__4365
                }else {
                  var G__4371 = isa_QMARK_.call(null, h, child.call(null, i__4366), parent.call(null, i__4366));
                  var G__4372 = i__4366 + 1;
                  ret__4365 = G__4371;
                  i__4366 = G__4372;
                  continue
                }
                break
              }
            }else {
              return and__3546__auto____4364
            }
          }else {
            return and__3546__auto____4363
          }
        }else {
          return and__3546__auto____4362
        }
      }
    }
  };
  isa_QMARK_ = function(h, child, parent) {
    switch(arguments.length) {
      case 2:
        return isa_QMARK___4368.call(this, h, child);
      case 3:
        return isa_QMARK___4369.call(this, h, child, parent)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return isa_QMARK_
}();
cljs.core.parents = function() {
  var parents = null;
  var parents__4373 = function(tag) {
    return parents.call(null, cljs.core.deref.call(null, cljs.core.global_hierarchy), tag)
  };
  var parents__4374 = function(h, tag) {
    return cljs.core.not_empty.call(null, cljs.core.get.call(null, "\ufdd0'parents".call(null, h), tag))
  };
  parents = function(h, tag) {
    switch(arguments.length) {
      case 1:
        return parents__4373.call(this, h);
      case 2:
        return parents__4374.call(this, h, tag)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return parents
}();
cljs.core.ancestors = function() {
  var ancestors = null;
  var ancestors__4376 = function(tag) {
    return ancestors.call(null, cljs.core.deref.call(null, cljs.core.global_hierarchy), tag)
  };
  var ancestors__4377 = function(h, tag) {
    return cljs.core.not_empty.call(null, cljs.core.get.call(null, "\ufdd0'ancestors".call(null, h), tag))
  };
  ancestors = function(h, tag) {
    switch(arguments.length) {
      case 1:
        return ancestors__4376.call(this, h);
      case 2:
        return ancestors__4377.call(this, h, tag)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return ancestors
}();
cljs.core.descendants = function() {
  var descendants = null;
  var descendants__4379 = function(tag) {
    return descendants.call(null, cljs.core.deref.call(null, cljs.core.global_hierarchy), tag)
  };
  var descendants__4380 = function(h, tag) {
    return cljs.core.not_empty.call(null, cljs.core.get.call(null, "\ufdd0'descendants".call(null, h), tag))
  };
  descendants = function(h, tag) {
    switch(arguments.length) {
      case 1:
        return descendants__4379.call(this, h);
      case 2:
        return descendants__4380.call(this, h, tag)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return descendants
}();
cljs.core.derive = function() {
  var derive = null;
  var derive__4390 = function(tag, parent) {
    if(cljs.core.truth_(cljs.core.namespace.call(null, parent))) {
    }else {
      throw new Error(cljs.core.str.call(null, "Assert failed: ", cljs.core.pr_str.call(null, cljs.core.with_meta(cljs.core.list("\ufdd1'namespace", "\ufdd1'parent"), cljs.core.hash_map("\ufdd0'line", 3365)))));
    }
    cljs.core.swap_BANG_.call(null, cljs.core.global_hierarchy, derive, tag, parent);
    return null
  };
  var derive__4391 = function(h, tag, parent) {
    if(cljs.core.truth_(cljs.core.not_EQ_.call(null, tag, parent))) {
    }else {
      throw new Error(cljs.core.str.call(null, "Assert failed: ", cljs.core.pr_str.call(null, cljs.core.with_meta(cljs.core.list("\ufdd1'not=", "\ufdd1'tag", "\ufdd1'parent"), cljs.core.hash_map("\ufdd0'line", 3369)))));
    }
    var tp__4385 = "\ufdd0'parents".call(null, h);
    var td__4386 = "\ufdd0'descendants".call(null, h);
    var ta__4387 = "\ufdd0'ancestors".call(null, h);
    var tf__4388 = function(m, source, sources, target, targets) {
      return cljs.core.reduce.call(null, function(ret, k) {
        return cljs.core.assoc.call(null, ret, k, cljs.core.reduce.call(null, cljs.core.conj, cljs.core.get.call(null, targets, k, cljs.core.set([])), cljs.core.cons.call(null, target, targets.call(null, target))))
      }, m, cljs.core.cons.call(null, source, sources.call(null, source)))
    };
    var or__3548__auto____4389 = cljs.core.truth_(cljs.core.contains_QMARK_.call(null, tp__4385.call(null, tag), parent)) ? null : function() {
      if(cljs.core.truth_(cljs.core.contains_QMARK_.call(null, ta__4387.call(null, tag), parent))) {
        throw new Error(cljs.core.str.call(null, tag, "already has", parent, "as ancestor"));
      }else {
      }
      if(cljs.core.truth_(cljs.core.contains_QMARK_.call(null, ta__4387.call(null, parent), tag))) {
        throw new Error(cljs.core.str.call(null, "Cyclic derivation:", parent, "has", tag, "as ancestor"));
      }else {
      }
      return cljs.core.ObjMap.fromObject(["\ufdd0'parents", "\ufdd0'ancestors", "\ufdd0'descendants"], {"\ufdd0'parents":cljs.core.assoc.call(null, "\ufdd0'parents".call(null, h), tag, cljs.core.conj.call(null, cljs.core.get.call(null, tp__4385, tag, cljs.core.set([])), parent)), "\ufdd0'ancestors":tf__4388.call(null, "\ufdd0'ancestors".call(null, h), tag, td__4386, parent, ta__4387), "\ufdd0'descendants":tf__4388.call(null, "\ufdd0'descendants".call(null, h), parent, ta__4387, tag, td__4386)})
    }();
    if(cljs.core.truth_(or__3548__auto____4389)) {
      return or__3548__auto____4389
    }else {
      return h
    }
  };
  derive = function(h, tag, parent) {
    switch(arguments.length) {
      case 2:
        return derive__4390.call(this, h, tag);
      case 3:
        return derive__4391.call(this, h, tag, parent)
    }
    throw"Invalid arity: " + arguments.length;
  };
  return derive
}();
cljs.core.underive = function() {
  var underive = null;
  var underive__4397 = function(tag, parent) {
    cljs.core.swap_BANG_.call(null, cljs.core.global_hierarchy, underive, tag, parent);
    return null
  };
  var underive__4398 = function(h, tag, parent) {
    var parentMap__4393 = "\ufdd0'parents".call(null, h);
    var childsParents__4394 = cljs.core.truth_(parentMap__4393.call(null, tag)) ? cljs.core.disj.call(null, parentMap__4393.call(null, tag), parent) : cljs.core.set([]);
    var newParents__4395 = cljs.core.truth_(cljs.core.not_empty.call(null, childsParents__4394)) ? cljs.core.assoc.call(null, parentMap__4393, tag, childsParents__4394) : cljs.core.dissoc.call(null, parentMap__4393, tag);
    var deriv_seq__4396 = cljs.core.flatten.call(null, cljs.core.map.call(null, function(p1__4382_SHARP_) {
      return cljs.core.cons.call(null, cljs.core.first.call(null, p1__4382_SHARP_), cljs.core.interpose.call(null, cljs.core.first.call(null, p1__4382_SHARP_), cljs.core.second.call(null, p1__4382_SHARP_)))
    }, cljs.core.seq.call(null, newParents__4395)));
    if(cljs.core.truth_(cljs.core.contains_QMARK_.call(null, parentMap__4393.call(null, tag), parent))) {
      return cljs.core.reduce.call(null, function(p1__4383_SHARP_, p2__4384_SHARP_) {
        return cljs.core.apply.call(null, cljs.core.derive, p1__4383_SHARP_, p2__4384_SHARP_)
      }, cljs.core.make_hierarchy.call(null), cljs.core.partition.call(null, 2, deriv_seq__4396))
    }else {
      return h
    }
  };
  underive = function(h, tag, parent) {
    switch(arguments.length) {
      case 2:
        return underive__4397.call(this, h, tag);
      case 3:
        return underive__4398.call(this, h, tag, parent)
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
  var xprefs__4400 = cljs.core.deref.call(null, prefer_table).call(null, x);
  var or__3548__auto____4402 = cljs.core.truth_(function() {
    var and__3546__auto____4401 = xprefs__4400;
    if(cljs.core.truth_(and__3546__auto____4401)) {
      return xprefs__4400.call(null, y)
    }else {
      return and__3546__auto____4401
    }
  }()) ? true : null;
  if(cljs.core.truth_(or__3548__auto____4402)) {
    return or__3548__auto____4402
  }else {
    var or__3548__auto____4404 = function() {
      var ps__4403 = cljs.core.parents.call(null, y);
      while(true) {
        if(cljs.core.truth_(cljs.core.count.call(null, ps__4403) > 0)) {
          if(cljs.core.truth_(prefers_STAR_.call(null, x, cljs.core.first.call(null, ps__4403), prefer_table))) {
          }else {
          }
          var G__4407 = cljs.core.rest.call(null, ps__4403);
          ps__4403 = G__4407;
          continue
        }else {
          return null
        }
        break
      }
    }();
    if(cljs.core.truth_(or__3548__auto____4404)) {
      return or__3548__auto____4404
    }else {
      var or__3548__auto____4406 = function() {
        var ps__4405 = cljs.core.parents.call(null, x);
        while(true) {
          if(cljs.core.truth_(cljs.core.count.call(null, ps__4405) > 0)) {
            if(cljs.core.truth_(prefers_STAR_.call(null, cljs.core.first.call(null, ps__4405), y, prefer_table))) {
            }else {
            }
            var G__4408 = cljs.core.rest.call(null, ps__4405);
            ps__4405 = G__4408;
            continue
          }else {
            return null
          }
          break
        }
      }();
      if(cljs.core.truth_(or__3548__auto____4406)) {
        return or__3548__auto____4406
      }else {
        return false
      }
    }
  }
};
cljs.core.dominates = function dominates(x, y, prefer_table) {
  var or__3548__auto____4409 = cljs.core.prefers_STAR_.call(null, x, y, prefer_table);
  if(cljs.core.truth_(or__3548__auto____4409)) {
    return or__3548__auto____4409
  }else {
    return cljs.core.isa_QMARK_.call(null, x, y)
  }
};
cljs.core.find_and_cache_best_method = function find_and_cache_best_method(name, dispatch_val, hierarchy, method_table, prefer_table, method_cache, cached_hierarchy) {
  var best_entry__4418 = cljs.core.reduce.call(null, function(be, p__4410) {
    var vec__4411__4412 = p__4410;
    var k__4413 = cljs.core.nth.call(null, vec__4411__4412, 0, null);
    var ___4414 = cljs.core.nth.call(null, vec__4411__4412, 1, null);
    var e__4415 = vec__4411__4412;
    if(cljs.core.truth_(cljs.core.isa_QMARK_.call(null, dispatch_val, k__4413))) {
      var be2__4417 = cljs.core.truth_(function() {
        var or__3548__auto____4416 = be === null;
        if(cljs.core.truth_(or__3548__auto____4416)) {
          return or__3548__auto____4416
        }else {
          return cljs.core.dominates.call(null, k__4413, cljs.core.first.call(null, be), prefer_table)
        }
      }()) ? e__4415 : be;
      if(cljs.core.truth_(cljs.core.dominates.call(null, cljs.core.first.call(null, be2__4417), k__4413, prefer_table))) {
      }else {
        throw new Error(cljs.core.str.call(null, "Multiple methods in multimethod '", name, "' match dispatch value: ", dispatch_val, " -> ", k__4413, " and ", cljs.core.first.call(null, be2__4417), ", and neither is preferred"));
      }
      return be2__4417
    }else {
      return be
    }
  }, null, cljs.core.deref.call(null, method_table));
  if(cljs.core.truth_(best_entry__4418)) {
    if(cljs.core.truth_(cljs.core._EQ_.call(null, cljs.core.deref.call(null, cached_hierarchy), cljs.core.deref.call(null, hierarchy)))) {
      cljs.core.swap_BANG_.call(null, method_cache, cljs.core.assoc, dispatch_val, cljs.core.second.call(null, best_entry__4418));
      return cljs.core.second.call(null, best_entry__4418)
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
    var and__3546__auto____4419 = mf;
    if(cljs.core.truth_(and__3546__auto____4419)) {
      return mf.cljs$core$IMultiFn$_reset
    }else {
      return and__3546__auto____4419
    }
  }())) {
    return mf.cljs$core$IMultiFn$_reset(mf)
  }else {
    return function() {
      var or__3548__auto____4420 = cljs.core._reset[goog.typeOf.call(null, mf)];
      if(cljs.core.truth_(or__3548__auto____4420)) {
        return or__3548__auto____4420
      }else {
        var or__3548__auto____4421 = cljs.core._reset["_"];
        if(cljs.core.truth_(or__3548__auto____4421)) {
          return or__3548__auto____4421
        }else {
          throw cljs.core.missing_protocol.call(null, "IMultiFn.-reset", mf);
        }
      }
    }().call(null, mf)
  }
};
cljs.core._add_method = function _add_method(mf, dispatch_val, method) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____4422 = mf;
    if(cljs.core.truth_(and__3546__auto____4422)) {
      return mf.cljs$core$IMultiFn$_add_method
    }else {
      return and__3546__auto____4422
    }
  }())) {
    return mf.cljs$core$IMultiFn$_add_method(mf, dispatch_val, method)
  }else {
    return function() {
      var or__3548__auto____4423 = cljs.core._add_method[goog.typeOf.call(null, mf)];
      if(cljs.core.truth_(or__3548__auto____4423)) {
        return or__3548__auto____4423
      }else {
        var or__3548__auto____4424 = cljs.core._add_method["_"];
        if(cljs.core.truth_(or__3548__auto____4424)) {
          return or__3548__auto____4424
        }else {
          throw cljs.core.missing_protocol.call(null, "IMultiFn.-add-method", mf);
        }
      }
    }().call(null, mf, dispatch_val, method)
  }
};
cljs.core._remove_method = function _remove_method(mf, dispatch_val) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____4425 = mf;
    if(cljs.core.truth_(and__3546__auto____4425)) {
      return mf.cljs$core$IMultiFn$_remove_method
    }else {
      return and__3546__auto____4425
    }
  }())) {
    return mf.cljs$core$IMultiFn$_remove_method(mf, dispatch_val)
  }else {
    return function() {
      var or__3548__auto____4426 = cljs.core._remove_method[goog.typeOf.call(null, mf)];
      if(cljs.core.truth_(or__3548__auto____4426)) {
        return or__3548__auto____4426
      }else {
        var or__3548__auto____4427 = cljs.core._remove_method["_"];
        if(cljs.core.truth_(or__3548__auto____4427)) {
          return or__3548__auto____4427
        }else {
          throw cljs.core.missing_protocol.call(null, "IMultiFn.-remove-method", mf);
        }
      }
    }().call(null, mf, dispatch_val)
  }
};
cljs.core._prefer_method = function _prefer_method(mf, dispatch_val, dispatch_val_y) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____4428 = mf;
    if(cljs.core.truth_(and__3546__auto____4428)) {
      return mf.cljs$core$IMultiFn$_prefer_method
    }else {
      return and__3546__auto____4428
    }
  }())) {
    return mf.cljs$core$IMultiFn$_prefer_method(mf, dispatch_val, dispatch_val_y)
  }else {
    return function() {
      var or__3548__auto____4429 = cljs.core._prefer_method[goog.typeOf.call(null, mf)];
      if(cljs.core.truth_(or__3548__auto____4429)) {
        return or__3548__auto____4429
      }else {
        var or__3548__auto____4430 = cljs.core._prefer_method["_"];
        if(cljs.core.truth_(or__3548__auto____4430)) {
          return or__3548__auto____4430
        }else {
          throw cljs.core.missing_protocol.call(null, "IMultiFn.-prefer-method", mf);
        }
      }
    }().call(null, mf, dispatch_val, dispatch_val_y)
  }
};
cljs.core._get_method = function _get_method(mf, dispatch_val) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____4431 = mf;
    if(cljs.core.truth_(and__3546__auto____4431)) {
      return mf.cljs$core$IMultiFn$_get_method
    }else {
      return and__3546__auto____4431
    }
  }())) {
    return mf.cljs$core$IMultiFn$_get_method(mf, dispatch_val)
  }else {
    return function() {
      var or__3548__auto____4432 = cljs.core._get_method[goog.typeOf.call(null, mf)];
      if(cljs.core.truth_(or__3548__auto____4432)) {
        return or__3548__auto____4432
      }else {
        var or__3548__auto____4433 = cljs.core._get_method["_"];
        if(cljs.core.truth_(or__3548__auto____4433)) {
          return or__3548__auto____4433
        }else {
          throw cljs.core.missing_protocol.call(null, "IMultiFn.-get-method", mf);
        }
      }
    }().call(null, mf, dispatch_val)
  }
};
cljs.core._methods = function _methods(mf) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____4434 = mf;
    if(cljs.core.truth_(and__3546__auto____4434)) {
      return mf.cljs$core$IMultiFn$_methods
    }else {
      return and__3546__auto____4434
    }
  }())) {
    return mf.cljs$core$IMultiFn$_methods(mf)
  }else {
    return function() {
      var or__3548__auto____4435 = cljs.core._methods[goog.typeOf.call(null, mf)];
      if(cljs.core.truth_(or__3548__auto____4435)) {
        return or__3548__auto____4435
      }else {
        var or__3548__auto____4436 = cljs.core._methods["_"];
        if(cljs.core.truth_(or__3548__auto____4436)) {
          return or__3548__auto____4436
        }else {
          throw cljs.core.missing_protocol.call(null, "IMultiFn.-methods", mf);
        }
      }
    }().call(null, mf)
  }
};
cljs.core._prefers = function _prefers(mf) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____4437 = mf;
    if(cljs.core.truth_(and__3546__auto____4437)) {
      return mf.cljs$core$IMultiFn$_prefers
    }else {
      return and__3546__auto____4437
    }
  }())) {
    return mf.cljs$core$IMultiFn$_prefers(mf)
  }else {
    return function() {
      var or__3548__auto____4438 = cljs.core._prefers[goog.typeOf.call(null, mf)];
      if(cljs.core.truth_(or__3548__auto____4438)) {
        return or__3548__auto____4438
      }else {
        var or__3548__auto____4439 = cljs.core._prefers["_"];
        if(cljs.core.truth_(or__3548__auto____4439)) {
          return or__3548__auto____4439
        }else {
          throw cljs.core.missing_protocol.call(null, "IMultiFn.-prefers", mf);
        }
      }
    }().call(null, mf)
  }
};
cljs.core._dispatch = function _dispatch(mf, args) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____4440 = mf;
    if(cljs.core.truth_(and__3546__auto____4440)) {
      return mf.cljs$core$IMultiFn$_dispatch
    }else {
      return and__3546__auto____4440
    }
  }())) {
    return mf.cljs$core$IMultiFn$_dispatch(mf, args)
  }else {
    return function() {
      var or__3548__auto____4441 = cljs.core._dispatch[goog.typeOf.call(null, mf)];
      if(cljs.core.truth_(or__3548__auto____4441)) {
        return or__3548__auto____4441
      }else {
        var or__3548__auto____4442 = cljs.core._dispatch["_"];
        if(cljs.core.truth_(or__3548__auto____4442)) {
          return or__3548__auto____4442
        }else {
          throw cljs.core.missing_protocol.call(null, "IMultiFn.-dispatch", mf);
        }
      }
    }().call(null, mf, args)
  }
};
cljs.core.do_dispatch = function do_dispatch(mf, dispatch_fn, args) {
  var dispatch_val__4443 = cljs.core.apply.call(null, dispatch_fn, args);
  var target_fn__4444 = cljs.core._get_method.call(null, mf, dispatch_val__4443);
  if(cljs.core.truth_(target_fn__4444)) {
  }else {
    throw new Error(cljs.core.str.call(null, "No method in multimethod '", cljs.core.name, "' for dispatch value: ", dispatch_val__4443));
  }
  return cljs.core.apply.call(null, target_fn__4444, args)
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
  var this__4445 = this;
  return goog.getUid.call(null, this$)
};
cljs.core.MultiFn.prototype.cljs$core$IMultiFn$ = true;
cljs.core.MultiFn.prototype.cljs$core$IMultiFn$_reset = function(mf) {
  var this__4446 = this;
  cljs.core.swap_BANG_.call(null, this__4446.method_table, function(mf) {
    return cljs.core.ObjMap.fromObject([], {})
  });
  cljs.core.swap_BANG_.call(null, this__4446.method_cache, function(mf) {
    return cljs.core.ObjMap.fromObject([], {})
  });
  cljs.core.swap_BANG_.call(null, this__4446.prefer_table, function(mf) {
    return cljs.core.ObjMap.fromObject([], {})
  });
  cljs.core.swap_BANG_.call(null, this__4446.cached_hierarchy, function(mf) {
    return null
  });
  return mf
};
cljs.core.MultiFn.prototype.cljs$core$IMultiFn$_add_method = function(mf, dispatch_val, method) {
  var this__4447 = this;
  cljs.core.swap_BANG_.call(null, this__4447.method_table, cljs.core.assoc, dispatch_val, method);
  cljs.core.reset_cache.call(null, this__4447.method_cache, this__4447.method_table, this__4447.cached_hierarchy, this__4447.hierarchy);
  return mf
};
cljs.core.MultiFn.prototype.cljs$core$IMultiFn$_remove_method = function(mf, dispatch_val) {
  var this__4448 = this;
  cljs.core.swap_BANG_.call(null, this__4448.method_table, cljs.core.dissoc, dispatch_val);
  cljs.core.reset_cache.call(null, this__4448.method_cache, this__4448.method_table, this__4448.cached_hierarchy, this__4448.hierarchy);
  return mf
};
cljs.core.MultiFn.prototype.cljs$core$IMultiFn$_get_method = function(mf, dispatch_val) {
  var this__4449 = this;
  if(cljs.core.truth_(cljs.core._EQ_.call(null, cljs.core.deref.call(null, this__4449.cached_hierarchy), cljs.core.deref.call(null, this__4449.hierarchy)))) {
  }else {
    cljs.core.reset_cache.call(null, this__4449.method_cache, this__4449.method_table, this__4449.cached_hierarchy, this__4449.hierarchy)
  }
  var temp__3695__auto____4450 = cljs.core.deref.call(null, this__4449.method_cache).call(null, dispatch_val);
  if(cljs.core.truth_(temp__3695__auto____4450)) {
    var target_fn__4451 = temp__3695__auto____4450;
    return target_fn__4451
  }else {
    var temp__3695__auto____4452 = cljs.core.find_and_cache_best_method.call(null, this__4449.name, dispatch_val, this__4449.hierarchy, this__4449.method_table, this__4449.prefer_table, this__4449.method_cache, this__4449.cached_hierarchy);
    if(cljs.core.truth_(temp__3695__auto____4452)) {
      var target_fn__4453 = temp__3695__auto____4452;
      return target_fn__4453
    }else {
      return cljs.core.deref.call(null, this__4449.method_table).call(null, this__4449.default_dispatch_val)
    }
  }
};
cljs.core.MultiFn.prototype.cljs$core$IMultiFn$_prefer_method = function(mf, dispatch_val_x, dispatch_val_y) {
  var this__4454 = this;
  if(cljs.core.truth_(cljs.core.prefers_STAR_.call(null, dispatch_val_x, dispatch_val_y, this__4454.prefer_table))) {
    throw new Error(cljs.core.str.call(null, "Preference conflict in multimethod '", this__4454.name, "': ", dispatch_val_y, " is already preferred to ", dispatch_val_x));
  }else {
  }
  cljs.core.swap_BANG_.call(null, this__4454.prefer_table, function(old) {
    return cljs.core.assoc.call(null, old, dispatch_val_x, cljs.core.conj.call(null, cljs.core.get.call(null, old, dispatch_val_x, cljs.core.set([])), dispatch_val_y))
  });
  return cljs.core.reset_cache.call(null, this__4454.method_cache, this__4454.method_table, this__4454.cached_hierarchy, this__4454.hierarchy)
};
cljs.core.MultiFn.prototype.cljs$core$IMultiFn$_methods = function(mf) {
  var this__4455 = this;
  return cljs.core.deref.call(null, this__4455.method_table)
};
cljs.core.MultiFn.prototype.cljs$core$IMultiFn$_prefers = function(mf) {
  var this__4456 = this;
  return cljs.core.deref.call(null, this__4456.prefer_table)
};
cljs.core.MultiFn.prototype.cljs$core$IMultiFn$_dispatch = function(mf, args) {
  var this__4457 = this;
  return cljs.core.do_dispatch.call(null, mf, this__4457.dispatch_fn, args)
};
cljs.core.MultiFn;
cljs.core.MultiFn.prototype.call = function() {
  var G__4458__delegate = function(_, args) {
    return cljs.core._dispatch.call(null, this, args)
  };
  var G__4458 = function(_, var_args) {
    var args = null;
    if(goog.isDef(var_args)) {
      args = cljs.core.array_seq(Array.prototype.slice.call(arguments, 1), 0)
    }
    return G__4458__delegate.call(this, _, args)
  };
  G__4458.cljs$lang$maxFixedArity = 1;
  G__4458.cljs$lang$applyTo = function(arglist__4459) {
    var _ = cljs.core.first(arglist__4459);
    var args = cljs.core.rest(arglist__4459);
    return G__4458__delegate.call(this, _, args)
  };
  return G__4458
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
    var and__3546__auto____4461 = reader;
    if(cljs.core.truth_(and__3546__auto____4461)) {
      return reader.cljs$reader$PushbackReader$read_char
    }else {
      return and__3546__auto____4461
    }
  }())) {
    return reader.cljs$reader$PushbackReader$read_char(reader)
  }else {
    return function() {
      var or__3548__auto____4462 = cljs.reader.read_char[goog.typeOf.call(null, reader)];
      if(cljs.core.truth_(or__3548__auto____4462)) {
        return or__3548__auto____4462
      }else {
        var or__3548__auto____4463 = cljs.reader.read_char["_"];
        if(cljs.core.truth_(or__3548__auto____4463)) {
          return or__3548__auto____4463
        }else {
          throw cljs.core.missing_protocol.call(null, "PushbackReader.read-char", reader);
        }
      }
    }().call(null, reader)
  }
};
cljs.reader.unread = function unread(reader, ch) {
  if(cljs.core.truth_(function() {
    var and__3546__auto____4464 = reader;
    if(cljs.core.truth_(and__3546__auto____4464)) {
      return reader.cljs$reader$PushbackReader$unread
    }else {
      return and__3546__auto____4464
    }
  }())) {
    return reader.cljs$reader$PushbackReader$unread(reader, ch)
  }else {
    return function() {
      var or__3548__auto____4465 = cljs.reader.unread[goog.typeOf.call(null, reader)];
      if(cljs.core.truth_(or__3548__auto____4465)) {
        return or__3548__auto____4465
      }else {
        var or__3548__auto____4466 = cljs.reader.unread["_"];
        if(cljs.core.truth_(or__3548__auto____4466)) {
          return or__3548__auto____4466
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
  var this__4467 = this;
  if(cljs.core.truth_(cljs.core.empty_QMARK_.call(null, cljs.core.deref.call(null, this__4467.buffer_atom)))) {
    var idx__4468 = cljs.core.deref.call(null, this__4467.index_atom);
    cljs.core.swap_BANG_.call(null, this__4467.index_atom, cljs.core.inc);
    return cljs.core.nth.call(null, this__4467.s, idx__4468)
  }else {
    var buf__4469 = cljs.core.deref.call(null, this__4467.buffer_atom);
    cljs.core.swap_BANG_.call(null, this__4467.buffer_atom, cljs.core.rest);
    return cljs.core.first.call(null, buf__4469)
  }
};
cljs.reader.StringPushbackReader.prototype.cljs$reader$PushbackReader$unread = function(reader, ch) {
  var this__4470 = this;
  return cljs.core.swap_BANG_.call(null, this__4470.buffer_atom, function(p1__4460_SHARP_) {
    return cljs.core.cons.call(null, ch, p1__4460_SHARP_)
  })
};
cljs.reader.StringPushbackReader;
cljs.reader.push_back_reader = function push_back_reader(s) {
  return new cljs.reader.StringPushbackReader(s, cljs.core.atom.call(null, 0), cljs.core.atom.call(null, null))
};
cljs.reader.whitespace_QMARK_ = function whitespace_QMARK_(ch) {
  var or__3548__auto____4471 = goog.string.isBreakingWhitespace.call(null, ch);
  if(cljs.core.truth_(or__3548__auto____4471)) {
    return or__3548__auto____4471
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
  var or__3548__auto____4472 = cljs.reader.numeric_QMARK_.call(null, initch);
  if(cljs.core.truth_(or__3548__auto____4472)) {
    return or__3548__auto____4472
  }else {
    var and__3546__auto____4474 = function() {
      var or__3548__auto____4473 = cljs.core._EQ_.call(null, "+", initch);
      if(cljs.core.truth_(or__3548__auto____4473)) {
        return or__3548__auto____4473
      }else {
        return cljs.core._EQ_.call(null, "-", initch)
      }
    }();
    if(cljs.core.truth_(and__3546__auto____4474)) {
      return cljs.reader.numeric_QMARK_.call(null, function() {
        var next_ch__4475 = cljs.reader.read_char.call(null, reader);
        cljs.reader.unread.call(null, reader, next_ch__4475);
        return next_ch__4475
      }())
    }else {
      return and__3546__auto____4474
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
  reader_error.cljs$lang$applyTo = function(arglist__4476) {
    var rdr = cljs.core.first(arglist__4476);
    var msg = cljs.core.rest(arglist__4476);
    return reader_error__delegate.call(this, rdr, msg)
  };
  return reader_error
}();
cljs.reader.macro_terminating_QMARK_ = function macro_terminating_QMARK_(ch) {
  var and__3546__auto____4477 = cljs.core.not_EQ_.call(null, ch, "#");
  if(cljs.core.truth_(and__3546__auto____4477)) {
    var and__3546__auto____4478 = cljs.core.not_EQ_.call(null, ch, "'");
    if(cljs.core.truth_(and__3546__auto____4478)) {
      var and__3546__auto____4479 = cljs.core.not_EQ_.call(null, ch, ":");
      if(cljs.core.truth_(and__3546__auto____4479)) {
        return cljs.core.contains_QMARK_.call(null, cljs.reader.macros, ch)
      }else {
        return and__3546__auto____4479
      }
    }else {
      return and__3546__auto____4478
    }
  }else {
    return and__3546__auto____4477
  }
};
cljs.reader.read_token = function read_token(rdr, initch) {
  var sb__4480 = new goog.string.StringBuffer(initch);
  var ch__4481 = cljs.reader.read_char.call(null, rdr);
  while(true) {
    if(cljs.core.truth_(function() {
      var or__3548__auto____4482 = ch__4481 === null;
      if(cljs.core.truth_(or__3548__auto____4482)) {
        return or__3548__auto____4482
      }else {
        var or__3548__auto____4483 = cljs.reader.whitespace_QMARK_.call(null, ch__4481);
        if(cljs.core.truth_(or__3548__auto____4483)) {
          return or__3548__auto____4483
        }else {
          return cljs.reader.macro_terminating_QMARK_.call(null, ch__4481)
        }
      }
    }())) {
      cljs.reader.unread.call(null, rdr, ch__4481);
      return sb__4480.toString()
    }else {
      var G__4484 = function() {
        sb__4480.append(ch__4481);
        return sb__4480
      }();
      var G__4485 = cljs.reader.read_char.call(null, rdr);
      sb__4480 = G__4484;
      ch__4481 = G__4485;
      continue
    }
    break
  }
};
cljs.reader.skip_line = function skip_line(reader, _) {
  while(true) {
    var ch__4486 = cljs.reader.read_char.call(null, reader);
    if(cljs.core.truth_(function() {
      var or__3548__auto____4487 = cljs.core._EQ_.call(null, ch__4486, "n");
      if(cljs.core.truth_(or__3548__auto____4487)) {
        return or__3548__auto____4487
      }else {
        var or__3548__auto____4488 = cljs.core._EQ_.call(null, ch__4486, "r");
        if(cljs.core.truth_(or__3548__auto____4488)) {
          return or__3548__auto____4488
        }else {
          return ch__4486 === null
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
  var groups__4489 = cljs.core.re_find.call(null, cljs.reader.int_pattern, s);
  var group3__4490 = cljs.core.nth.call(null, groups__4489, 2);
  if(cljs.core.truth_(cljs.core.not.call(null, function() {
    var or__3548__auto____4491 = void 0 === group3__4490;
    if(cljs.core.truth_(or__3548__auto____4491)) {
      return or__3548__auto____4491
    }else {
      return group3__4490.length < 1
    }
  }()))) {
    return 0
  }else {
    var negate__4493 = cljs.core.truth_(cljs.core._EQ_.call(null, "-", cljs.core.nth.call(null, groups__4489, 1))) ? -1 : 1;
    var vec__4492__4494 = cljs.core.truth_(cljs.core.nth.call(null, groups__4489, 3)) ? cljs.core.Vector.fromArray([cljs.core.nth.call(null, groups__4489, 3), 10]) : cljs.core.truth_(cljs.core.nth.call(null, groups__4489, 4)) ? cljs.core.Vector.fromArray([cljs.core.nth.call(null, groups__4489, 4), 16]) : cljs.core.truth_(cljs.core.nth.call(null, groups__4489, 5)) ? cljs.core.Vector.fromArray([cljs.core.nth.call(null, groups__4489, 5), 8]) : cljs.core.truth_(cljs.core.nth.call(null, groups__4489, 
    7)) ? cljs.core.Vector.fromArray([cljs.core.nth.call(null, groups__4489, 7), parseInt.call(null, cljs.core.nth.call(null, groups__4489, 7))]) : cljs.core.truth_("\ufdd0'default") ? cljs.core.Vector.fromArray([null, null]) : null;
    var n__4495 = cljs.core.nth.call(null, vec__4492__4494, 0, null);
    var radix__4496 = cljs.core.nth.call(null, vec__4492__4494, 1, null);
    if(cljs.core.truth_(n__4495 === null)) {
      return null
    }else {
      return negate__4493 * parseInt.call(null, n__4495, radix__4496)
    }
  }
};
cljs.reader.match_ratio = function match_ratio(s) {
  var groups__4497 = cljs.core.re_find.call(null, cljs.reader.ratio_pattern, s);
  var numinator__4498 = cljs.core.nth.call(null, groups__4497, 1);
  var denominator__4499 = cljs.core.nth.call(null, groups__4497, 2);
  return parseInt.call(null, numinator__4498) / parseInt.call(null, denominator__4499)
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
  var ch__4500 = cljs.reader.read_char.call(null, reader);
  var mapresult__4501 = cljs.core.get.call(null, cljs.reader.escape_char_map, ch__4500);
  if(cljs.core.truth_(mapresult__4501)) {
    return mapresult__4501
  }else {
    if(cljs.core.truth_(function() {
      var or__3548__auto____4502 = cljs.core._EQ_.call(null, "u", ch__4500);
      if(cljs.core.truth_(or__3548__auto____4502)) {
        return or__3548__auto____4502
      }else {
        return cljs.reader.numeric_QMARK_.call(null, ch__4500)
      }
    }())) {
      return cljs.reader.read_unicode_char.call(null, reader, ch__4500)
    }else {
      return cljs.reader.reader_error.call(null, reader, "Unsupported escape charater: \\", ch__4500)
    }
  }
};
cljs.reader.read_past = function read_past(pred, rdr) {
  var ch__4503 = cljs.reader.read_char.call(null, rdr);
  while(true) {
    if(cljs.core.truth_(pred.call(null, ch__4503))) {
      var G__4504 = cljs.reader.read_char.call(null, rdr);
      ch__4503 = G__4504;
      continue
    }else {
      return ch__4503
    }
    break
  }
};
cljs.reader.read_delimited_list = function read_delimited_list(delim, rdr, recursive_QMARK_) {
  var a__4505 = cljs.core.Vector.fromArray([]);
  while(true) {
    var ch__4506 = cljs.reader.read_past.call(null, cljs.reader.whitespace_QMARK_, rdr);
    if(cljs.core.truth_(ch__4506)) {
    }else {
      cljs.reader.reader_error.call(null, rdr, "EOF")
    }
    if(cljs.core.truth_(cljs.core._EQ_.call(null, delim, ch__4506))) {
      return a__4505
    }else {
      var temp__3695__auto____4507 = cljs.core.get.call(null, cljs.reader.macros, ch__4506);
      if(cljs.core.truth_(temp__3695__auto____4507)) {
        var macrofn__4508 = temp__3695__auto____4507;
        var mret__4509 = macrofn__4508.call(null, rdr, ch__4506);
        var G__4511 = cljs.core.truth_(cljs.core._EQ_.call(null, mret__4509, rdr)) ? a__4505 : cljs.core.conj.call(null, a__4505, mret__4509);
        a__4505 = G__4511;
        continue
      }else {
        cljs.reader.unread.call(null, rdr, ch__4506);
        var o__4510 = cljs.reader.read.call(null, rdr, true, null, recursive_QMARK_);
        var G__4512 = cljs.core.truth_(cljs.core._EQ_.call(null, o__4510, rdr)) ? a__4505 : cljs.core.conj.call(null, a__4505, o__4510);
        a__4505 = G__4512;
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
  var ch__4513 = cljs.reader.read_char.call(null, rdr);
  var dm__4514 = cljs.core.get.call(null, cljs.reader.dispatch_macros, ch__4513);
  if(cljs.core.truth_(dm__4514)) {
    return dm__4514.call(null, rdr, _)
  }else {
    return cljs.reader.reader_error.call(null, rdr, "No dispatch macro for ", ch__4513)
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
  var l__4515 = cljs.reader.read_delimited_list.call(null, "}", rdr, true);
  if(cljs.core.truth_(cljs.core.odd_QMARK_.call(null, cljs.core.count.call(null, l__4515)))) {
    cljs.reader.reader_error.call(null, rdr, "Map literal must contain an even number of forms")
  }else {
  }
  return cljs.core.apply.call(null, cljs.core.hash_map, l__4515)
};
cljs.reader.read_number = function read_number(reader, initch) {
  var buffer__4516 = new goog.string.StringBuffer(initch);
  var ch__4517 = cljs.reader.read_char.call(null, reader);
  while(true) {
    if(cljs.core.truth_(function() {
      var or__3548__auto____4518 = ch__4517 === null;
      if(cljs.core.truth_(or__3548__auto____4518)) {
        return or__3548__auto____4518
      }else {
        var or__3548__auto____4519 = cljs.reader.whitespace_QMARK_.call(null, ch__4517);
        if(cljs.core.truth_(or__3548__auto____4519)) {
          return or__3548__auto____4519
        }else {
          return cljs.core.contains_QMARK_.call(null, cljs.reader.macros, ch__4517)
        }
      }
    }())) {
      cljs.reader.unread.call(null, reader, ch__4517);
      var s__4520 = buffer__4516.toString();
      var or__3548__auto____4521 = cljs.reader.match_number.call(null, s__4520);
      if(cljs.core.truth_(or__3548__auto____4521)) {
        return or__3548__auto____4521
      }else {
        return cljs.reader.reader_error.call(null, reader, "Invalid number format [", s__4520, "]")
      }
    }else {
      var G__4522 = function() {
        buffer__4516.append(ch__4517);
        return buffer__4516
      }();
      var G__4523 = cljs.reader.read_char.call(null, reader);
      buffer__4516 = G__4522;
      ch__4517 = G__4523;
      continue
    }
    break
  }
};
cljs.reader.read_string = function read_string(reader, _) {
  var buffer__4524 = new goog.string.StringBuffer;
  var ch__4525 = cljs.reader.read_char.call(null, reader);
  while(true) {
    if(cljs.core.truth_(ch__4525 === null)) {
      return cljs.reader.reader_error.call(null, reader, "EOF while reading string")
    }else {
      if(cljs.core.truth_(cljs.core._EQ_.call(null, "\\", ch__4525))) {
        var G__4526 = function() {
          buffer__4524.append(cljs.reader.escape_char.call(null, buffer__4524, reader));
          return buffer__4524
        }();
        var G__4527 = cljs.reader.read_char.call(null, reader);
        buffer__4524 = G__4526;
        ch__4525 = G__4527;
        continue
      }else {
        if(cljs.core.truth_(cljs.core._EQ_.call(null, '"', ch__4525))) {
          return buffer__4524.toString()
        }else {
          if(cljs.core.truth_("\ufdd0'default")) {
            var G__4528 = function() {
              buffer__4524.append(ch__4525);
              return buffer__4524
            }();
            var G__4529 = cljs.reader.read_char.call(null, reader);
            buffer__4524 = G__4528;
            ch__4525 = G__4529;
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
  var token__4530 = cljs.reader.read_token.call(null, reader, initch);
  if(cljs.core.truth_(goog.string.contains.call(null, token__4530, "/"))) {
    return cljs.core.symbol.call(null, cljs.core.subs.call(null, token__4530, 0, token__4530.indexOf("/")), cljs.core.subs.call(null, token__4530, token__4530.indexOf("/") + 1, token__4530.length))
  }else {
    return cljs.core.get.call(null, cljs.reader.special_symbols, token__4530, cljs.core.symbol.call(null, token__4530))
  }
};
cljs.reader.read_keyword = function read_keyword(reader, initch) {
  var token__4532 = cljs.reader.read_token.call(null, reader, cljs.reader.read_char.call(null, reader));
  var vec__4531__4533 = cljs.core.re_matches.call(null, cljs.reader.symbol_pattern, token__4532);
  var token__4534 = cljs.core.nth.call(null, vec__4531__4533, 0, null);
  var ns__4535 = cljs.core.nth.call(null, vec__4531__4533, 1, null);
  var name__4536 = cljs.core.nth.call(null, vec__4531__4533, 2, null);
  if(cljs.core.truth_(function() {
    var or__3548__auto____4538 = function() {
      var and__3546__auto____4537 = cljs.core.not.call(null, void 0 === ns__4535);
      if(cljs.core.truth_(and__3546__auto____4537)) {
        return ns__4535.substring(ns__4535.length - 2, ns__4535.length) === ":/"
      }else {
        return and__3546__auto____4537
      }
    }();
    if(cljs.core.truth_(or__3548__auto____4538)) {
      return or__3548__auto____4538
    }else {
      var or__3548__auto____4539 = name__4536[name__4536.length - 1] === ":";
      if(cljs.core.truth_(or__3548__auto____4539)) {
        return or__3548__auto____4539
      }else {
        return cljs.core.not.call(null, token__4534.indexOf("::", 1) === -1)
      }
    }
  }())) {
    return cljs.reader.reader_error.call(null, reader, "Invalid token: ", token__4534)
  }else {
    if(cljs.core.truth_(cljs.reader.ns_QMARK_)) {
      return cljs.core.keyword.call(null, ns__4535.substring(0, ns__4535.indexOf("/")), name__4536)
    }else {
      return cljs.core.keyword.call(null, token__4534)
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
  var m__4540 = cljs.reader.desugar_meta.call(null, cljs.reader.read.call(null, rdr, true, null, true));
  if(cljs.core.truth_(cljs.core.map_QMARK_.call(null, m__4540))) {
  }else {
    cljs.reader.reader_error.call(null, rdr, "Metadata must be Symbol,Keyword,String or Map")
  }
  var o__4541 = cljs.reader.read.call(null, rdr, true, null, true);
  if(cljs.core.truth_(function() {
    var x__352__auto____4542 = o__4541;
    if(cljs.core.truth_(function() {
      var and__3546__auto____4543 = x__352__auto____4542;
      if(cljs.core.truth_(and__3546__auto____4543)) {
        var and__3546__auto____4544 = x__352__auto____4542.cljs$core$IWithMeta$;
        if(cljs.core.truth_(and__3546__auto____4544)) {
          return cljs.core.not.call(null, x__352__auto____4542.hasOwnProperty("cljs$core$IWithMeta$"))
        }else {
          return and__3546__auto____4544
        }
      }else {
        return and__3546__auto____4543
      }
    }())) {
      return true
    }else {
      return cljs.core.type_satisfies_.call(null, cljs.core.IWithMeta, x__352__auto____4542)
    }
  }())) {
    return cljs.core.with_meta.call(null, o__4541, cljs.core.merge.call(null, cljs.core.meta.call(null, o__4541), m__4540))
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
    var ch__4545 = cljs.reader.read_char.call(null, reader);
    if(cljs.core.truth_(ch__4545 === null)) {
      if(cljs.core.truth_(eof_is_error)) {
        return cljs.reader.reader_error.call(null, reader, "EOF")
      }else {
        return sentinel
      }
    }else {
      if(cljs.core.truth_(cljs.reader.whitespace_QMARK_.call(null, ch__4545))) {
        var G__4547 = reader;
        var G__4548 = eof_is_error;
        var G__4549 = sentinel;
        var G__4550 = is_recursive;
        reader = G__4547;
        eof_is_error = G__4548;
        sentinel = G__4549;
        is_recursive = G__4550;
        continue
      }else {
        if(cljs.core.truth_(cljs.reader.comment_prefix_QMARK_.call(null, ch__4545))) {
          var G__4551 = cljs.reader.read_comment.call(null, reader, ch__4545);
          var G__4552 = eof_is_error;
          var G__4553 = sentinel;
          var G__4554 = is_recursive;
          reader = G__4551;
          eof_is_error = G__4552;
          sentinel = G__4553;
          is_recursive = G__4554;
          continue
        }else {
          if(cljs.core.truth_("\ufdd0'else")) {
            var res__4546 = cljs.core.truth_(cljs.reader.macros.call(null, ch__4545)) ? cljs.reader.macros.call(null, ch__4545).call(null, reader, ch__4545) : cljs.core.truth_(cljs.reader.number_literal_QMARK_.call(null, reader, ch__4545)) ? cljs.reader.read_number.call(null, reader, ch__4545) : cljs.core.truth_("\ufdd0'else") ? cljs.reader.read_symbol.call(null, reader, ch__4545) : null;
            if(cljs.core.truth_(cljs.core._EQ_.call(null, res__4546, reader))) {
              var G__4555 = reader;
              var G__4556 = eof_is_error;
              var G__4557 = sentinel;
              var G__4558 = is_recursive;
              reader = G__4555;
              eof_is_error = G__4556;
              sentinel = G__4557;
              is_recursive = G__4558;
              continue
            }else {
              return res__4546
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
  var r__4559 = cljs.reader.push_back_reader.call(null, s);
  return cljs.reader.read.call(null, r__4559, true, null, false)
};
goog.provide("org.healthsciencessc.rpms2.core");
goog.require("cljs.core");
goog.require("cljs.reader");
org.healthsciencessc.rpms2.core.ajax_sexp_get = function ajax_sexp_get(path, callback) {
  return $.get(path, function(data) {
    return callback.call(null, cljs.reader.read_string.call(null, data))
  }, "text")
};
org.healthsciencessc.rpms2.core.consenter_search_result_clicked = function consenter_search_result_clicked(div) {
  console.log("consenter-search-result-clicked");
  var user__2732 = cljs.reader.read_string.call(null, div.getAttribute("data-user"));
  var details__2733 = $.call(null, "#consenter-details");
  var other_section__2734 = $.call(null, "#other-section");
  var map__2731__2735 = user__2732;
  var map__2731__2736 = cljs.core.truth_(cljs.core.seq_QMARK_.call(null, map__2731__2735)) ? cljs.core.apply.call(null, cljs.core.hash_map, map__2731__2735) : map__2731__2735;
  var last_name__2737 = cljs.core.get.call(null, map__2731__2736, "\ufdd0'last-name");
  var first_name__2738 = cljs.core.get.call(null, map__2731__2736, "\ufdd0'first-name");
  $.call(null, ".user-selected").removeClass("user-selected");
  $.call(null, div).addClass("user-selected");
  details__2733.find(cljs.core.str.call(null, "#consenter-name")).text(cljs.core.str.call(null, "\ufdd0'first-name".call(null, user__2732), " ", "\ufdd0'last-name".call(null, user__2732)));
  details__2733.find(cljs.core.str.call(null, "#consenter-zipcode")).text("\ufdd0'zipcode".call(null, user__2732));
  details__2733.find("#consenter-date-of-birth").text("\ufdd0'date-of-birth".call(null, user__2732));
  details__2733.find("#consenter-dob").text("\ufdd0'dob".call(null, user__2732));
  details__2733.find("#consenter-consenter-id").text("\ufdd0'consenter-id".call(null, user__2732));
  other_section__2734.find(cljs.core.str.call(null, "#patient-name")).val(cljs.core.str.call(null, first_name__2738, " ", last_name__2737));
  console.log("patient name set ", cljs.core.str.call(null, first_name__2738, " ", last_name__2737));
  other_section__2734.find(cljs.core.str.call(null, "#patient-encounter-date")).val("\ufdd0'consenter-encounter-date".call(null, user__2732));
  other_section__2734.find(cljs.core.str.call(null, "#patient-id")).val("\ufdd0'medical-record-number".call(null, user__2732));
  return other_section__2734.find(cljs.core.str.call(null, "#current-patient-selection")).val(user__2732)
};
goog.exportSymbol("org.healthsciencessc.rpms2.core.consenter_search_result_clicked", org.healthsciencessc.rpms2.core.consenter_search_result_clicked);
org.healthsciencessc.rpms2.core.data_change_clicked = function data_change_clicked(div) {
  var data_change_class__2739 = cljs.reader.read_string.call(null, div.getAttribute("data-change-class"));
  var mdid__2740 = cljs.reader.read_string.call(null, div.getAttribute("mdid"));
  var details__2741 = $.call(null, cljs.core.str.call(null, "#", mdid__2740));
  var data_change_section__2742 = $.call(null, "div.control.data-change");
  var h__2743 = $.call(null, cljs.core.str.call(null, "#hidden-", mdid__2740));
  $.call(null, details__2741).addClass("changed");
  return data_change_section__2742.find(cljs.core.str.call(null, "#hidden-", mdid__2740)).val("CHANGED")
};
goog.exportSymbol("org.healthsciencessc.rpms2.core.data_change_clicked", org.healthsciencessc.rpms2.core.data_change_clicked);
