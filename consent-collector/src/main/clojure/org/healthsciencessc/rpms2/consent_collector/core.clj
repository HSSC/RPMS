(ns org.healthsciencessc.rpms2.consent-collector.core
  (:require [org.healthsciencessc.rpms2.process-engine [core :as pe]
                                                       [web-service :as ws]]
            [org.healthsciencessc.rpms2.consent-collector [dsa-client :as dsa]
            						  [helpers :as helper]
            						  [login :as login]
            						  [select-location  :as select-location] 
            						  [select-lockcode  :as select-lockcode] 
            						  [select-unlockcode  :as select-unlockcode] 
            						  [select-consenter :as select-consenter] 
            						  [search-consenter :as search-consenter] 
            						  [search-results :as search-results] 
            						  [create-consenter :as create-consenter] 
            						  [select-protocol :as select-protocol] 
            						  [encounter :as encounter] 
            						  [collect-consents :as collect-consents] 
            						  [witness-consents :as witness-consents] 
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


(defn- goto-login-page
  [ctx]
  (login/view ctx))

(defn- active-session? 
  [_]
  (session-get :user))

(defn- no-session? 
  [_]
  (not (active-session?)))

(def processes [{:name "get-login" :runnable-fn (constantly true) :run-fn login/default-get-login }

                {:name "get-view-login" :runnable-fn (fn [context] (not (contains? context :password))), :run-fn login/view }
                {:name "post-view-login" :runnable-fn (constantly true) :run-fn login/perform }

                {:name "post-view-logout" :runnable-fn (constantly true) :run-fn login/logout }

                {:name "get-view-not-authorized" :runnable-fn (constantly true) :run-fn login/default-get-view-not-authorized}

                {:name "get-view-select-lock-code"  :runnable-fn active-session?  :run-fn select-lockcode/view :run-if-false goto-login-page  }
                {:name "post-view-select-lock-code" :runnable-fn active-session?  :run-fn select-lockcode/perform :run-if-false goto-login-page } 

                {:name "get-view-unlock"  :runnable-fn active-session?  :run-fn select-unlockcode/view :run-if-false goto-login-page  }
                {:name "post-view-unlock" :runnable-fn active-session?  :run-fn select-unlockcode/perform :run-if-false goto-login-page } 

                {:name "get-view-select-location"  :runnable-fn active-session?  :run-fn select-location/view :run-if-false goto-login-page }
                {:name "post-view-select-location" :runnable-fn active-session?  :run-fn select-location/perform :run-if-false goto-login-page }

                {:name "get-view-select-consenter" :runnable-fn active-session?  :run-fn select-consenter/view :run-if-false goto-login-page }
                {:name "post-view-select-consenter" :runnable-fn active-session?  :run-fn select-consenter/perform :run-if-false goto-login-page }

                {:name "get-view-select-encounter" :runnable-fn active-session?  :run-fn encounter/view :run-if-false goto-login-page }
                {:name "post-view-select-encounter" :runnable-fn active-session?  :run-fn encounter/perform :run-if-false goto-login-page }

                {:name "get-view-create-encounter" :runnable-fn active-session?  :run-fn encounter/create-view :run-if-false goto-login-page }
                {:name "post-view-create-encounter" :runnable-fn active-session?  :run-fn encounter/create-perform :run-if-false goto-login-page }

                {:name "post-view-search-consenters" :runnable-fn active-session?  :run-fn search-consenter/perform :run-if-false goto-login-page }
                {:name "get-view-search-consenters" :runnable-fn active-session?  :run-fn search-consenter/view :run-if-false goto-login-page }

                {:name "get-view-search-results" :runnable-fn active-session?  :run-fn search-results/view :run-if-false goto-login-page }
                {:name "post-view-search-results" :runnable-fn active-session?  :run-fn search-results/perform  :run-if-false goto-login-page }

                {:name "get-view-create-consenter" :runnable-fn active-session?  :run-fn create-consenter/view :run-if-false goto-login-page }
                {:name "post-view-create-consenter" :runnable-fn active-session?  :run-fn create-consenter/perform :run-if-false goto-login-page }

                {:name "get-view-select-protocols" :runnable-fn active-session?  :run-fn select-protocol/view :run-if-false goto-login-page }
                {:name "post-view-select-protocols" :runnable-fn active-session?  :run-fn select-protocol/perform :run-if-false goto-login-page }



                {:name "get-view-meta-data" :runnable-fn active-session?  :run-fn metadata/view :run-if-false goto-login-page }
                {:name "post-view-meta-data" :runnable-fn active-session?  :run-fn metadata/perform :run-if-false goto-login-page }

                {:name "get-collect-consents" :runnable-fn active-session?  :run-fn collect-consents/view :run-if-false goto-login-page }
                {:name "post-collect-consents" :runnable-fn active-session?  :run-fn collect-consents/perform :run-if-false goto-login-page }

                {:name "get-review-consents" :runnable-fn active-session?  :run-fn collect-consents/view :run-if-false goto-login-page }
                {:name "post-review-consents" :runnable-fn active-session?  :run-fn collect-consents/perform :run-if-false goto-login-page }
                {:name "get-witness-consents" :runnable-fn active-session?  :run-fn witness-consents/view :run-if-false goto-login-page }
                {:name "post-witness-consents" :runnable-fn active-session?  :run-fn witness-consents/perform :run-if-false goto-login-page }

                {:name "get-view-unimplemented" :runnable-fn (constantly true) :run-fn unimplemented/view }
                {:name "post-view-unimplemented" :runnable-fn (constantly true) :run-fn unimplemented/view }

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
        (do ;; (debug "last page (get) " (:uri req))
        (session-put! :last-page (:uri req))))
      (when (= :post (:request-method req))
        (do ;;(debug "last page (post) " (:uri req))
        (session-put! :last-post-page (:uri req))))
      resp)))

(defn add-logging 
  [handler]
  (fn [req]
    (info "core 234: Got request: " (:uri req) " " req)
    (let [resp (handler req)]
      (info "core 234: Got response " (:uri req) " " resp)
      resp)))

;; Enable session handling via sandbar 
;; Make resources/public items in search path
(def app (-> (ws/ws-constructor)
             ;;(add-logging)
             (wrap-dsa-auth)       
             (wrap-context-setter) 
             (wrap-exceptions)
             (wrap-last-page)
             (sandbar.stateful-session/wrap-stateful-session)
             (wrap-resource "public")))
