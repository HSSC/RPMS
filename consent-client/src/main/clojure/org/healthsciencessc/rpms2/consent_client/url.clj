(ns org.healthsciencessc.rpms2.consent-client.url
  "Functions for creating URLs and URL-based strings."
  (:use [clojure.string :only [join]]
        [pliant.configure.props :only [slurp-config]]
        [pliant.configure.sniff :only [sniff]])
  (:import java.net.URLEncoder))


;; Configuration
(def config (slurp-config "consent-client.props" (sniff "RPMSPKEY")))

;; Encoding for 
(def encoding "UTF-8")

(defprotocol Stringify
  (^String stringify [x] "Convert a value into a string."))

(extend-protocol Stringify
  java.lang.String
  (stringify [s] s)
  clojure.lang.Keyword
  (stringify [k] (name k))
  clojure.lang.Ratio
  (stringify [r] (str (float r)))
  java.lang.Object
  (stringify [x] (str x))
  nil
  (stringify [_] ""))

(defn encode
  [v]
  (URLEncoder/encode (stringify v) encoding))

(defn params->string
  [params]
  (join "&" (flatten 
              (for [[k v] params] 
                (if (coll? v) 
                  (for [x v] (str (encode k) "=" (encode x))) 
                  (str (encode k) "=" (encode v)))))))

(defn url
  "Creates the absolute URL to the services using the configured path to services."
  ([url] (url nil))
  ([url params]
    (if (or (nil? params) (empty? params))
      (str (:services.url config) url)
      (str (:services.url config) url "?" (params->string params)))))
