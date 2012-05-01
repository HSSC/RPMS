(ns org.healthsciencessc.rpms2.consent-collector.dsa-client
  (:require [clojure.string :as s]
            [clj-http.client :as http])
  (:import org.apache.http.auth.MalformedChallengeException
           org.apache.http.client.ClientProtocolException)
  (:use [org.healthsciencessc.rpms2.consent-collector  [factories :as factory]
                                                       [config :only (config)]
                                                       [debug :only (debug!)]
                                                       [fake-dsa-client :as fake]]
        [clojure.tools.logging :only (debug info error warn)]
        [clojure.data.json :only (read-json json-str)]))

(def ^:dynamic *dsa-auth* nil)

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
  (try (http/request req)
    (catch ClientProtocolException e
      ;; TODO -- check if cause is a MalformedChallengeException
      {:status 401})
    (catch java.net.UnknownHostException ex
        ;; we want to define flash message here
        (do (println "UNKNOWN HOST: " req " exception " ex) 
            (debug "UNKNOWN HOST " ex)
           {:status 500 :error-message (str "Unknown host: " ex) })
        ;;(throw ex)
      )))

;; where to catch exceptions
;;  java.net.UnknownHostException
(defn dsa-call
  [process-name arguments]
  (let [[_ method path-dashes] (re-matches #"(get|post|put|delete)-(.+)" (name process-name))
        method (keyword method),
        path (s/replace path-dashes "-" "/")
        maybe-parse-json
        (fn [{:keys [status body] :as resp}]
          (if (= 200 status)
            (assoc resp :json (read-json body))
            resp)) ]
    
    ;; try catch here?
    (-> (if (= :get method)
          (if (empty? arguments)
            {}
            {:query-params arguments})
          {:body (json-str arguments)})
        (assoc :request-method method
               :basic-auth *dsa-auth*
               :url (build-url path) )
        request
        maybe-parse-json)))

(defn authenticate
  "Call security/authenticate userid password"
  [user-id password]
  (binding [*dsa-auth* [user-id password]]
    (dsa-call :get-security-authenticate {})
    ;(fake/fake-authenticate user-id password)
    ))

(defn get-protocols
  []
  (factory/generate-protocol-list))
