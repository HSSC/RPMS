(ns org.healthsciencessc.rpms2.process-engine.endpoint
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [ring.middleware.session :as session]
            [ring.util.response :as ring]
            [clojure.tools.logging :as logging]
            [org.healthsciencessc.rpms2.process-engine.util :as util]
            [org.healthsciencessc.rpms2.process-engine.core :as process]))


(defmulti endpoints 
  (fn [{:keys [request-method] :as request}] 
    (util/uri->process-name (name request-method) (util/path request))))

(defmethod endpoints :default
  [{:keys [request-method] :as request}]
  (let [contype (or (:content-type request) "")
        proc-name (util/uri->process-name (name request-method) (util/path request))
        message (str "Unable to find a process registered as '" proc-name "'.")]
    (cond
      (or (util/json-requested? contype) (util/clojure-requested? contype))
        {:status 404 :body message}
      :else
        {:status 404 :body (str "<html><head><title>Process Not Found</title></head><body><h1>Process Not Found</h1><h3>" 
                                message "</h3></body></html>") })))


(defmulti respond
  (fn [request body] 
    (let [contype (or (:content-type request) "NA")]
      (cond 
        (and (ring/response? body) (not (empty? (:headers body))))
          :response
        (util/json-requested? contype) 
          :json
        (util/clojure-requested? contype) 
          :clojure
        :else 
          :default))))


(defmethod respond :response
  [request body]
  body)

(defmethod respond :json
  [request body]
  (util/respond-with-json body request))

(defmethod respond :clojure
  [request body]
  (util/respond-with-clojure body request))

(defmethod respond :default
  [request body]
  body)

(defmulti on-exception 
  (fn [exception request] (class exception)))

(defmethod on-exception :default
  [exception request]
  (logging/error exception (select-keys request [:request-method :uri :context :path-info :query-params :body-params]))
  (respond {:status 401 
            :body {:message "The process failed to execute successfully due to an unexpected exception.  
                      The exception has been logged. Contact your administrator and inform them of the incident."}}
           request))

(defn loggit
  [request]
  (logging/info (str "METHOD: " (:request-method request)))
  (logging/info (str "URI: " (:uri request)))
  (logging/info (str "CONTEXT: " (:context request)))
  (logging/info (str "PATH: " (:path-info request)))
  (logging/info (str "QUERY: " (:query-params request)))
  (if (= "/security/login" (:path-info request))
    (logging/info (str "BODY: " (dissoc (:body-params request) :password)))
    (logging/info (str "BODY: " (:body-params request))))
  (logging/info "--------------------" ))

(defroutes service-routes
  (ANY "*" request
       (let [clean-request (util/clean-request request)]
         (loggit clean-request)
         (try 
           (respond request (endpoints clean-request))
           (catch Exception e
             (on-exception e clean-request))))))

(defn ws-constructor
  [& middlewares]
  (-> (reduce #(%2 %1) service-routes middlewares)
      session/wrap-session
      handler/api))
