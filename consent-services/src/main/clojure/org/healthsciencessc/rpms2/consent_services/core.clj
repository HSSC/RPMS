(ns org.healthsciencessc.rpms2.consent-services.core
  (:use compojure.core
        ring.middleware.reload-modified
        ring.util.serve
        [slingshot.slingshot :only (try+)])
  (:require [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-services.config :as config]
            [org.healthsciencessc.rpms2.consent-services.auth :as auth]
            [org.healthsciencessc.rpms2.consent-services.seed :as seed]
            [org.healthsciencessc.rpms2.consent-services.default-processes.init]
            [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.process-engine.web-service :as process-ws]))

(defn ws-init
  []
  (data/connect! (config/conf "neo4j-db-path"))
  (seed/seed)
  (process/bootstrap-addons))

(defn ws-destroy
  []
  (data/shutdown!))

(defroutes static-dev-routes
  (GET "/reset-processes"
       []
       (do
         (reset! process/default-processes [])
         (reset! process/custom-processes [])
         (process/bootstrap-addons))
       "Done")
  (GET "/reseed-db"
          []
          (do
            (data/delete-all-nodes!)
            (seed/setup-default-schema!)
            (seed/seed-base-org!)
            (seed/seed-example-org!))
          "Done"))

(defn add-path-info
  [handler]
  (fn [req]
    (handler (assoc req :path-info (:uri req)))))

(defn wrap-data-errors
  [handler]
  (fn [req]
    (try+
     (handler req)
     (catch [:type :org.healthsciencessc.rpms2.consent-services.data/invalid-record] {errors :errors}
       (process-ws/format-response-body {:status 422 :headers {} :body {:errors errors}} req))
     (catch [:type :org.healthsciencessc.rpms2.consent-services.data/record-not-found] {:keys [record-type id]}
         (process-ws/format-response-body {:status 404 :headers {} :body {:errors {:type record-type :id id}}} req)))))

(defroutes app
  (wrap-data-errors
   (process-ws/ws-constructor (fn [handler]
                                (auth/wrap-authentication handler auth/authenticate)))))

(defroutes dev-routes
  static-dev-routes
  (wrap-data-errors
   (process-ws/ws-constructor (fn [handler]
                                (-> handler
                                    (auth/wrap-authentication auth/authenticate)
                                    add-path-info)))))

(def dev-app
  (-> dev-routes
      (wrap-reload-modified ["src"])))

(defn start-dev-server
  []
  (ws-init)
  (serve-headless dev-app 8080))

(defn stop-dev-server
  []
  (stop-server)
  (ws-destroy))
