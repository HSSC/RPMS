(ns org.healthsciencessc.rpms2.process-engine.path
  (:require [clojure.java.io :as io])
  (:use [slingshot.slingshot :only (throw+)]))

(defn rootify
  "Insures that the URL passed only has a slash at the beginning and not at the end."
  [^String url]
  (if (empty? url) 
    ""
    (if (= url "/") 
      url
      (let [lurl (if (.endsWith url "/") (apply str (butlast url)) url)
            rurl (if (.startsWith url "/") lurl (str "/" lurl))]
        rurl))))

(defn clean-paths
  ""
  [seq]
  (remove (fn [x] (= x "/")) (remove empty? seq)))

(defn pop-path
  [x]
  (if (empty? x)
    "/"
    (re-find #"^.*/" x)))

(defmulti root-link
  "Creates a root relative link by prepending the contextPath to a given path."
  (fn [x y] (cond 
              (string? x) :string 
              (map? x) :map 
              (nil? x) :string 
              :else :unknown)))

;; Concats A ContextPath And Url Into A Valid Root Relative String
(defmethod root-link :string
  [context link]
  (let [all [context link]
        paths (clean-paths all)]
    (apply str (map rootify paths))))

;; Creates a valid root relative String from the request context, Concatting
;; the ContextPath, Path-Info, and supplied link where necessary.  If the link
;; starts with a / it will be appended to the ContextPath, else it will be 
;; considered relative from the current location.
(defmethod root-link :map
  [params ^String link]
  (if (.startsWith link "/")
    (root-link (:context params) link)
    (let [all [(:context params) (pop-path (:path-info params)) link]
          paths (clean-paths all)]
     (apply str (map rootify paths)))))
