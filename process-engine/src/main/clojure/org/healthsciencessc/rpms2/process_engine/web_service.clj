(ns org.healthsciencessc.rpms2.process-engine.web-service
  (:use compojure.core
        ring.middleware.session
        [slingshot.slingshot :only (try+)])
  (:require [compojure.handler :as handler]
            [clojure.tools.logging :as logging]
            [org.healthsciencessc.rpms2.process-engine.util :as util]
            [org.healthsciencessc.rpms2.process-engine.core :as process]))

(defn process-not-found-body
  [req process-name]
  (str "<h1>Process Not Found</h1>"
       (str "<h2>Process Name: " process-name "</h2>")
       "<h2>Request:</h2>"
       (str "<p>" req "</p>")))

(defroutes service-routes
  (ANY "*" {:keys [uri context path-info request-method query-params form-params session] :as req}
       (logging/info "REQUEST: " req)
       (let [process-name (util/uri->process-name (name request-method) path-info)
             body-params (util/resolve-body req)
             params {:query-params (util/keyify-params query-params) :body-params body-params :session session :context context :path-info path-info}]
         (logging/info "PROCESS NAME: " process-name)
         (try+
          (util/format-response-body (process/dispatch process-name params) req)
          (catch [:type :org.healthsciencessc.rpms2.process-engine.core/no-default-process] _
            (logging/error (str "Could not find process " process-name))
            {:status 404 :body (process-not-found-body req process-name)})))))

(defn ws-constructor
  [& middlewares]
  (-> (reduce #(%2 %1) service-routes middlewares)
      wrap-session
      handler/api))
