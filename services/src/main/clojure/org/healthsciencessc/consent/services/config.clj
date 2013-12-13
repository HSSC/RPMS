(ns org.healthsciencessc.consent.services.config
  (:use [pliant.configure.props :only [slurp-config]]
        [pliant.configure.sniff :only [sniff]])
  (:require [pliant.webpoint.common :as common]
            [pliant.webpoint.request :as request]
            [pliant.webpoint.response :as response]))

(def conf
  (slurp-config "consent-services.props" (sniff "RPMSPKEY")))

;; Change the handling of requests to endpoints that are not registered.
#_(defmethod request/endpoints :default
   [{:keys [path-info uri request-method] :as request}]
   (let [proc-name (common/uri->process-name (name request-method) (or path-info uri))]
     {:status 404 :body (str "Unable to find a process registered as '" proc-name "'.")}))

;; Change the handling of formatting the response.  Will only return clojure, unless specifically 
;; requesting JSON.
#_(defmethod response/respond :default
   [request body]
   (response/respond-with-clojure body request))