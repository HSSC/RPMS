(ns org.healthsciencessc.rpms2.consent-collector.dsa-client
  (:require [clojure.string :as s]
            [clj-http.client :as http])
  (:import org.apache.http.auth.MalformedChallengeException
           org.apache.http.client.ClientProtocolException)
  (:use [org.healthsciencessc.rpms2.consent-collector  [factories :as factory]
                                                       [config :only (config)]
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
(defn dsa-process-call
  [process-name arguments]
  (let [[_ method path-dashes] (re-matches #"(get|post|put|delete)-(.+)" process-name)
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

(defmacro def-dsa-processes
  [& pnames]
  `(do ~@(for [pname pnames]
           `(defn ~pname [args#]
                (dsa-process-call ~(str pname) args#)))))

(def-dsa-processes
  get-authorized-locations

  ;; requires an admin
  get-security-locations

  ;; requires either you are a super admin or are an admin
  ;; for the related organization ?location=some-location-id
  get-security-location

  ;; create new location; probably gotta be an admin for the org
  ;; or something
  put-security-location

  ;; update location
  post-security-location

  ;; curl -i -X GET -H "Content-type: application/json" http://localhost:3000/organizations
  get-security-organizations

  ;; curl -i -X GET -H "Content-type: application/json" http://localhost:3000/organization?organization=<ID>
  get-security-organization

  ;; curl -i -X PUT -H "Content-type: application/json" -d "{\"name\" : \"MUSC FOOBAR\"}" http://localhost:3000/organizat
  put-security-organization

   ;; curl -i -X POST -H "Content-type: application/json" -d "{\"name\" : \"MUSC BAZ\"}" http://localhost:3000/organization?organization=<ID>
  post-security-organization

  delete-security-organization

  get-security-roles

  get-security-role

  put-security-role

  post-security-roles

  get-security-authenticate

  get-security-users

  get-security-user

  put-security-user

  post-security-user)

(defn authenticate
  "Call security/authenticate userid password"
  [user-id password]
  (binding [*dsa-auth* [user-id password]]
    (get-security-authenticate {})))

(defn lock 
  "Sends the lock code to the server to lock."
  [lockcode]
  {:status 200})

(defn unlock 
  "Sends the lock code to the server to unlock."
  [lockcode]
  {:status 200})

(defn search-consenters
  [params]
  (try (let [m (factory/generate-user-list params) 
	v (into [] m )]
	(debug "search-consenters - ==> " (count v) " " v )
	v)
  (catch Exception ex (error "search-consenters failed: " params " " ex))))

(defn get-protocols
  []
  (factory/generate-protocol-list))
