(ns org.healthsciencessc.rpms2.process-engine.web-service
  (:use compojure.core
        ring.adapter.jetty
        [clojure.data.json :only (read-json)]
        [clojure.string :only (blank? join split)]
        [slingshot.slingshot :only (try+)])
  (:require [compojure.handler :as handler]
            [org.healthsciencessc.rpms2.process-engine.core :as process]))

(defn uri->process-name
  [method uri]
  (let [split-uri (filter #(not (blank? %)) (split uri #"\/"))]
    (join "-" (conj split-uri method))))

(defn get-body-params
  [body]
  (let [body-str (slurp body)]
    (if (not (blank? body-str))
      (read-json body-str))))

(defn keyify-query-params
  [query-params]
  (if (map? query-params)
    (into {} 
          (for [[k v] query-params]
            [(keyword k) v]))))

(defroutes service-routes
  (ANY "*" {:keys [uri request-method query-params body]}
       (let [process-name (uri->process-name (name request-method) uri)
             params {:query-params (keyify-query-params query-params) :body-params (get-body-params body)}]
         (try+
          (process/dispatch process-name params)
          (catch [:type :org.healthsciencessc.rpms2.process-engine.core/no-default-process] _
            {:status 404 :body "Process not found"})))))

(def processes
  (-> service-routes
      handler/api))
