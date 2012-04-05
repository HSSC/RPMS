(ns org.healthsciencessc.rpms2.core)

(defn foo-bar
  [name]
  (str name " is awesome!"))

(js* "window.clog=function(arg){console.log(arg)};")
(js/$
 (fn []
   (js/clog (str
             "On load! (" (foo-bar "you")
             ")"))))