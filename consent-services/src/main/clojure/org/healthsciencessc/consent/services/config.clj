(ns org.healthsciencessc.consent.services.config
  (:use [pliant.configure.props :only [slurp-config]]
        [pliant.configure.sniff :only [sniff]])
  (:require [org.healthsciencessc.rpms2.process-engine.util :as util]
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint]))

(def conf
  (slurp-config "consent-services.props" (sniff "RPMSPKEY")))

;; Change the handling of requests to endpoints that are not registered.
(defmethod endpoint/endpoints :default
  [{:keys [path-info request-method] :as request}]
  (let [proc-name (util/uri->process-name (name request-method) path-info)]
    {:status 404 :body (str "Unable to find a process registered as '" proc-name "'.")}))

;; Change the handling of formatting the response.  Will only return clojure, unless specifically 
;; requesting JSON.
(defmethod endpoint/respond :default
  [request body]
  (util/respond-with-clojure body request))