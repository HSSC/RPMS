(ns org.healthsciencessc.rpms2.consent-collector.dsa-client
  (:require [clojure.string :as s]
            [clj-http.client :as http])
  (:import org.apache.http.auth.MalformedChallengeException
           org.apache.http.client.ClientProtocolException)
  (:use [slingshot.slingshot :only (try+)])
  (:use [org.healthsciencessc.rpms2.consent-collector  [factories :as factory]
                                                       [config :only (config)]
                                                       [debug :only (debug!)] ]
        [clojure.tools.logging :only (debug info error warn)]
        [clojure.pprint :only (pprint)]
        [clojure.data.json :only (read-json json-str)]))

(def ^:dynamic *dsa-auth* nil)

(def consenter-fields  [:first-name
                        :last-name
                        :consenter-id
                        :date-of-birth
                        :zipcode])


(def create-consenter-fields [ :first-name
                               :middle-name
                               :last-name
                               :local-identifier
                               :local-identifier-type
                               :gender
                               :race
                               :religion
                               :address
                               :phone
                               :email
                               :date-of-birth ])



(defn- build-url 
   "Builds url for DSA for given path."
   [path]
   (let [ dsa-url (config "rpms2.dsa.url")
      no-slashes (fn [s] (second (re-matches #"/*([^/].*[^/])/*" s)))
      mypath  (if dsa-url 
                  (str (no-slashes dsa-url) "/" (no-slashes path)) 
                  (do 
                      (println  "WARNING: No dsa-url configured " path )
                      (warn "WARNING: No dsa-url configured "  path)
                      (str "http://obis-rpms-neodb-dev.mdc.musc.edu:8080/services" "/" path )))] 
      (debug "Using dsa-url: " mypath )
      (println "Using dsa-url: " mypath )
      mypath))

;; TODO - find out why the auth isn't working right (we shouldn't
;; be getting this exception)
(defn- request
  "like http/request but doesn't crash on failed auth"
  [req]
  (try+ 
    (do
      (debug (str "request REQ: " req))
      (let [resp (http/request  req )]
        (debug (str "request RESPONSE: " resp))
        resp))
    (catch ClientProtocolException e
      ;; TODO -- check if cause is a MalformedChallengeException
      (do 
        (println (str "FAILED: " req) )
        (println (str "ClientProtocol Exception " (.getMessage e) )
                   ) {:status 401}))
    (catch java.net.UnknownHostException ex
        ;; we want to define flash message here
        (do (println "UNKNOWN HOST: " req " exception " ex) 
            (debug "UNKNOWN HOST " ex)
           {:status 500 :error-message (str "Unknown host: " ex) }))
    (catch slingshot.ExceptionInfo ex
      (do (println "SLINGSHOT EXCEPTION" ex)
        {:status 403  :body (pr-str "INVALID REQUEST " ex " request: "  req)}))

    (catch Exception ex 
      (do 
        (debug "SOME OTHER ERROR: " ex)
        {:status 999  :body (pr-str "INVALID REQUEST " ex " request: "  req)}))
    (catch Object obj 
      (do 
        (error "http/request failed: object error " obj)
        ;;(println "An OBJECT error " obj)
        ;;(println "======== STATUS " (:status obj))
        (println "==http request failed --> " (pprint obj))
        {:status (:status obj) :body (print-str "OBJ INVALID REQUEST - see logs for details" )}))))
    
(debug! request)

;; where to catch exceptions
;;  java.net.UnknownHostException
(defn dsa-call
  [process-name arguments]
  (let [[_ method path-dashes] 
        (re-matches #"(get|post|put|delete)-(.+)" (name process-name))
        method (keyword method),
        path (s/replace path-dashes "-" "/")
        maybe-parse-json
        (fn [{:keys [content-type status body headers] :as resp}]
          (if (= 403 status) 
            (do (println "FORBIDDEN") {:status 403} )
            (if (= 200 status)
              (assoc resp :json (read-json body))
              resp))) ]
    
    ;; try catch here?
    (-> (if (= :get method)
          (if (empty? arguments)
            {}
            {:query-params arguments})
          {:body (json-str arguments) })
        (assoc :request-method method
               :basic-auth *dsa-auth* 
               ; :content-type "text/clojure"
               :content-type "application/json"
               :url (build-url path) )
        request
        maybe-parse-json
      )))

(defn authenticate
  "Call security/authenticate userid password"
  [user-id password]
  (binding [*dsa-auth* [user-id password]]
    (dsa-call :get-security-authenticate {})
    ;(fake/fake-authenticate user-id password)
    ))


(defn- remove-blank-vals
  "Given a map, removes all key/val pairs for which the value
  is blank."
  [m]
  (into {}
        (for [[k v] m :when (not (s/blank? v))]
          [k v])))

(defn dsa-search-consenters
  "Search consenters."

  [params org-id]
  (debug "dsa-search-consenters PARAMS = " params " ORG " org-id)
  (let [consenter-params (remove-blank-vals
                          (select-keys params consenter-fields)) ]
      (dsa-call :get-consent-consenters (assoc consenter-params :organization org-id))))

(defn dsa-create-consenter
  "Create a consenter."
  [params]
  (debug "dsa-create-consenter PARAMS = " params)
  (let [p (remove-blank-vals (select-keys params create-consenter-fields)) ]
      (debug "dsa-create-consenter P = " p )
      (debug "dsa-create-consenter JSON = " (json-str p) )
      (dsa-call :put-consent-consenter p)))


#_(defn dsa-search-consenters
    [params]
    {:status 200
     :json
     (vec (factory/generate-user-list params))})

;(debug! dsa-search-consenters)

(defn get-protocols
  []
  (factory/generate-protocol-list))
