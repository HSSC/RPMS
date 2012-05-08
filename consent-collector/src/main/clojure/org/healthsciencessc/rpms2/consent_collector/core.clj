(ns org.healthsciencessc.rpms2.consent-collector.core
  (:require [org.healthsciencessc.rpms2.process-engine [core :as pe]
                                                       [web-service :as ws]]
            [org.healthsciencessc.rpms2.consent-collector [dsa-client :as dsa]
            						  [helpers :as helper]
            						  [login :as login]
            						  [select-location  :as select-location] 
            						  [select-lockcode  :as select-lockcode] 
            						  [select-consenter :as select-consenter] 
            						  [search-consenter :as search-consenter] 
            						  [search-results :as search-results] 
            						  [create-consenter :as create-consenter] 
            						  [select-protocol :as select-protocol] 
            						  [metadata :as metadata]
            						  [unimplemented :as unimplemented]
							]
            [ring.util
             [codec :as codec]
             [response :as response]])
  (:use [sandbar.stateful-session :only [ session-get session-put! flash-put!]])
  (:use [ring.util.response :only [redirect]])
  (:use [clojure.string :only (replace-first)])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use clojure.pprint)
  (:import org.healthsciencessc.rpms2.process_engine.core.DefaultProcess))



(defn- logged-in
  [ctx]
  (if (session-get :user) true false))

(defn- not-logged-in
  [ctx]
  (if (session-get :user) false true))

(def processes [{:name "get-login"
                 :runnable-fn (constantly true)
                 :run-fn (fn [ctx] (login/default-get-login ctx))}

                {:name "get-view-login"
                 :runnable-fn (fn [context] (not (contains? context :password))),
                 :run-fn (fn [ctx] (login/view ctx))}

                {:name "post-view-login"
                 :runnable-fn (constantly true)
                 :run-fn (fn [ctx] (login/perform ctx))
		}
                {:name "get-view-logout"
                 :runnable-fn (constantly true)
                 :run-fn (fn [ctx] (login/logout ctx)) }

                {:name "get-view-not-authorized"
                 :runnable-fn (constantly true)
                 :run-fn login/default-get-view-not-authorized}

                {:name "get-view-select-lock-code"
                 :runnable-fn (fn [ctx] (if (session-get :user) true false))
                 :run-fn (fn [ctx] (select-lockcode/view ctx)) }

                ;; Go to login screen if not logged in
                {:name "get-view-select-lock-code"
                 :runnable-fn not-logged-in 
                 :run-fn (fn [ctx] (login/view ctx)) }

                {:name "post-view-select-lock-code"
                 :runnable-fn (fn [ctx] (if (session-get :user) true false))
                 :run-fn (fn [ctx] (select-lockcode/perform ctx)) }

                {:name "post-view-select-lock-code"
                 :runnable-fn (constantly true)
                ;; Go to login screen if not logged in
                 :run-fn (fn [ctx] (login/view ctx)) }
                                
                {:name "post-view-select-location"
                 :runnable-fn (constantly true)
                 :run-fn (fn [ctx] (select-location/perform ctx))}
                                
                {:name "get-view-select-location"
                 :runnable-fn (constantly true)
                 :run-fn (fn [ctx] (select-location/view ctx))}

                {:name "get-view-select-consenter"
                 :runnable-fn (constantly true)
                 :run-fn (fn [ctx] (select-consenter/view ctx)) }

                {:name "post-view-select-consenter"
                 :runnable-fn (constantly true)
                 :run-fn (fn [ctx] (select-consenter/perform ctx)) }

                {:name "post-view-search-consenters"
                 :runnable-fn (constantly true)
                 :run-fn (fn [ctx] (search-consenter/perform ctx)) }

                {:name "get-view-search-consenters"
                 :runnable-fn (constantly true)
                 :run-fn (fn [ctx] (search-consenter/view ctx)) }

                {:name "post-view-search-results"
                 :runnable-fn (constantly true)
                 :run-fn (fn [ctx] (search-results/perform ctx)) }

                {:name "get-view-search-results"
                 :runnable-fn (constantly true)
                 :run-fn (fn [ctx] (search-results/view ctx)) }

                {:name "get-view-create-consenter"
                 :runnable-fn (constantly true)
                 :run-fn (fn [ctx] (create-consenter/view ctx)) }

                {:name "post-view-create-consenter"
                 :runnable-fn (constantly true)
                 :run-fn (fn [ctx] (create-consenter/perform ctx)) }

                {:name "get-view-select-protocols"
                 :runnable-fn (constantly true)
                 :run-fn (fn [ctx] (select-protocol/view ctx)) }

                {:name "post-view-select-protocols"
                 :runnable-fn (constantly true)
                 :run-fn (fn [ctx] (select-protocol/perform ctx)) }

                {:name "get-view-meta-data"
                 :runnable-fn (constantly true)
                 :run-fn (fn [ctx] (metadata/view ctx)) }

                {:name "get-view-unimplemented"
                 :runnable-fn (constantly true)
                 :run-fn (fn [ctx] (unimplemented/view ctx)) }

 		])


(pe/register-processes (map #(DefaultProcess/create %) processes))
(debug "Processes have been registered")

(defn- debug-pprint
  [& args] (debug (with-out-str (apply pprint args))))

(defn debug-ring
  [f]
  (fn [arg]
    (debug "RING BEFORE:")
    (debug-pprint (select-keys arg [:uri :body :status :request-method]))
    (try
      (let [ret (f arg)]
        (debug "RING AFTER:")
        (debug-pprint ret)
        ret)
      (catch Throwable e
        (debug "RING ERROR: " (.getMessage e))
        (.printStackTrace e)
        (throw e)))))

;; TODO: move wrap-resource and wrap-context-setter into process-engine

;; COPIED FROM RING
(defn wrap-resource
  "Middleware that first checks to see whether the request map matches a static
  resource. If it does, the resource is returned in a response map, otherwise
  the request map is passed onto the handler. The root-path argument will be
  added to the beginning of the resource path."
  [handler root-path]
  (fn [request]
    (if-not (= :get (:request-method request))
      (handler request)                         ;; changing :uri to :path-info here
      (let [path (.substring (codec/url-decode (:path-info request)) 1)]
        (or (response/resource-response path {:root root-path})
                        (handler request))))))

(defn- get-context
  [{:keys [uri path-info]}]
  (subs uri 0 (- (count uri) (count path-info))))

(defn wrap-context-setter
  [app]
  (fn [req]
    (binding [helper/*context* (get-context req)]
      (app req))))

(defn wrap-dsa-auth
  "Include basic authentication in the request if username password
  have been previously stored in the session."
  [app]
  (fn [req]
    (if-let [{:keys [username password]} (session-get :user)]
      (binding [dsa/*dsa-auth* [username password]]
        (app req))
      (app req))))

(defn wrap-exceptions
  [app]
  (fn [req]
    (try (app req)
      (catch Throwable t
        (.printStackTrace t)
        (error "core 203 EXCEPTION CAUGHT " t " req " req)
        {:status 500, :body (.getMessage t)}))))

(defn process-not-found-response
  [req]
  (flash-put! :header (str "PROCESS NOT FOUND: " (:uri req)))
  (helper/myredirect (or (session-get :last-page)
                         "login")))

(defn wrap-better-process-not-found-response
  [app]
  (fn [req]
    (let [{:keys [status body] :as resp} (app req)]
      (if (= 404 status)
        (process-not-found-response req)
        resp))))

(defn wrap-last-page
  "Save a copy of the last page we visited, in case
  there is an error and we need to return to the original page."
  [app]
  (fn [req]
    (let [resp (app req)]
      (when (= :get (:request-method req))
        (session-put! :last-page (:uri req)))
      resp)))



(defn add-logging 
  [handler]
  (fn [req]
    (info "core 234: Got request: " (:uri req) " " req)
    (handler req)))

(defn add-path-info
  [handler]
  (fn [req] 
    #_(handler (assoc req :xpath-info (:uri req)))
    (handler req)
    ))

(defn middleware [handler] (-> handler
                             (add-logging)
                             ;(add-path-info)
                             ))


;; Enable session handling via sandbar 
;; Make resources/public items in search path
(def app (-> (ws/ws-constructor middleware )
            ;; add middleware function
             (wrap-dsa-auth)       ;; add basic authentication to request
             (wrap-context-setter) ;; bind helper/*context*
             #_(wrap-exceptions)
             #_(wrap-better-process-not-found-response)
             #_(wrap-last-page)
             (sandbar.stateful-session/wrap-stateful-session)
             (wrap-resource "public")))
