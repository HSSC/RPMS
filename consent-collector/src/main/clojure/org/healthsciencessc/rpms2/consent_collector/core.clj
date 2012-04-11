(ns org.healthsciencessc.rpms2.consent-collector.core
  (:require [org.healthsciencessc.rpms2.process-engine [core :as pe]
                                                       [web-service :as ws]]
            [hiccup.core :as hiccup]
            [hiccup.page-helpers :as hpages]
            [hiccup.form-helpers :as hform]
            [org.healthsciencessc.rpms2.consent-collector [dsa-client :as dsa]
            						  [helpers :as helper]
            						  [login :as login]
            						  [select-location  :as select-location] 
            						  [select-lockcode  :as select-lockcode] 
            						  [select-consenter :as select-consenter] 
            						  [search-consenter :as search-consenter] 
            						  [create-consenter :as create-consenter] 
            						  [search-results :as search-results] 
            						  [select-protocol :as select-protocol] 
            						  [metadata :as metadata]
							]
            [ring.util
             [codec :as codec]
             [response :as response]])
  (:use [sandbar.stateful-session :only [session-put!
                                         session-get
                                         session-delete-key!
                                         flash-put!
                                         flash-get]])
  (:use [ring.util.response :only [redirect]])
  (:use [clojure.string :only (replace-first)])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use clojure.pprint)
  (:import org.healthsciencessc.rpms2.process_engine.core.DefaultProcess))

(defn default-get-view-not-authorized 
  [_]
  (helper/rpms2-page (i18n :not-authorized-message) :pageheader "NOT AUTHORIZED HEADER"))

(def processes [{:name "get-login"
                 :runnable-fn (constantly true)
                 :run-fn (fn [ctx] (login/default-get-login ctx)) }

                {:name "get-view-login"
                 :runnable-fn (fn [context] (not (contains? context :password))),
                 :run-fn (fn [ctx] (login/view ctx))
		}

                {:name "post-view-login"
                 :runnable-fn (constantly true)
                 :run-fn (fn [ctx] (login/perform ctx))
		}

                {:name "get-view-not-authorized"
                 :runnable-fn (constantly true)
                 :run-fn default-get-view-not-authorized}

                {:name "get-view-select-lock-code"
                 :runnable-fn (constantly true)
                 :run-fn (fn [ctx] (select-lockcode/view ctx)) }

                {:name "post-view-select-lock-code"
                 :runnable-fn (constantly true)
                 :run-fn (fn [ctx] (select-lockcode/perform ctx)) }
                                
                {:name "post-view-select-location"
                 :runnable-fn (constantly true)
                 :run-fn (fn [ctx] (select-location/perform ctx))
		}
                                
                {:name "get-view-select-location"
                 :runnable-fn (constantly true)
                 :run-fn (fn [ctx] (select-location/view ctx))
		}

                {:name "get-view-select-consenter"
                 :runnable-fn (constantly true)
                 :run-fn (fn [ctx] (select-consenter/view ctx)) }

                {:name "post-view-search-consenters"
                 :runnable-fn (constantly true)
                 :run-fn (fn [ctx] (search-consenter/post-view ctx)) }

                {:name "get-view-search-consenters"
                 :runnable-fn (constantly true)
                 :run-fn (fn [ctx] (search-consenter/get-view ctx)) }

                {:name "post-view-search-results"
                 :runnable-fn (constantly true)
                 :run-fn (fn [ctx] (search-results/handle-search-selection-response ctx)) }

                {:name "get-view-search-results"
                 :runnable-fn (constantly true)
                 :run-fn (fn [ctx] (search-results/handle-search-selection-response ctx)) }

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

 		])


;;(def newprocesses (helper/add-context-to-processes processes))
;;(println (str "*****\n\n\nNEW PROCESSES\n\n" newprocesses))
;;(pe/register-processes (map #(DefaultProcess/create %) newprocesses))
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
  [app]
  (fn [req]
    (if-let [{:keys [username password]} (session-get :user)]
      (binding [dsa/*dsa-auth* [username password]]
        (app req))
      (app req))))

;; Enable session handling via sandbar 
;; Make resources/public items in search path
(def app (-> (ws/ws-constructor)
             (wrap-dsa-auth)
             (sandbar.stateful-session/wrap-stateful-session)
             (wrap-context-setter)
             (wrap-resource "public")))
