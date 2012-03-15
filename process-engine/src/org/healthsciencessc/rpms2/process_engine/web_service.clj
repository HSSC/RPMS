(ns org.healthsciencessc.rpms2.process-engine.web-service
  (:use compojure.core
        ring.adapter.jetty
        ring.middleware.session
        [clojure.data.json :only (read-json)]
        [clojure.string :only (blank? join split)]
        [slingshot.slingshot :only (try+)]
        [clojure.tools.logging :only (info error)])
  (:require [compojure.handler :as handler]
            [org.healthsciencessc.rpms2.process-engine.core :as process]))

(defn uri->process-name
  [method uri]
  (let [split-uri (filter #(not (blank? %)) (split uri #"\/"))]
    (join "-" (conj split-uri method))))

(defn get-json-params
  [body]
  (let [body-str (slurp body)]
    (if (not (blank? body-str))
      (read-json body-str))))

(defn keyify-params
  [params]
  (if (map? params)
    (into {}
          (for [[k v] params]
            [(keyword k) v]))))

(defn process-not-found-body
  [req process-name]
  (str "<h1>Process Not Found</h1>"
       (str "<h2>Process Name: " process-name "</h2>")
       "<h2>Request:</h2>"
       (str "<p>" req "</p>")))

(defroutes service-routes
  (ANY "*" {:keys [uri request-method query-params form-params session body] :as req}
       (let [process-name (uri->process-name (name request-method) uri)
             body-params (merge (get-json-params body) (keyify-params form-params))
             params {:query-params (keyify-params query-params) :body-params body-params :session session}]
         (info "REQUEST: " req)
         (info "PROCESS NAME: " process-name)
         (try+
          (process/dispatch process-name params)
          (catch [:type :org.healthsciencessc.rpms2.process-engine.core/no-default-process] _
            (error (str "Could not find process " process-name))
            {:status 404 :body (process-not-found-body req process-name)})))))

(defn ws-constructor
  [& middlewares]
  (-> (reduce #(%2 %1) service-routes middlewares)
      wrap-session
      handler/api))
