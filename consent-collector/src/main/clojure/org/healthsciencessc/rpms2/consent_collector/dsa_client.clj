(ns org.healthsciencessc.rpms2.consent-collector.dsa-client
  (:require [clojure.string :as s]
            [clj-http.client :as http])
  (:import org.apache.http.auth.MalformedChallengeException
           org.apache.http.client.ClientProtocolException)
  (:use [org.healthsciencessc.rpms2.consent-collector  [factories :as factory]
                                                       [config :only (config)]
                                                       [fake-dsa-client :as fake]
          ]
        [clojure.tools.logging :only (debug info error warn)]
        [clojure.data.json :only (read-json json-str)]))

(def ^:dynamic *dsa-auth* nil)

(defn- build-url 
   "Builds url for DSA for the given path."
   [path] 
   (let [ dsa-url (config "rpms2.dsa.url")
      no-slashes (fn [s] (second (re-matches #"/*([^/].*[^/])/*" s)))
      mypath  (if dsa-url 
                  (str (no-slashes dsa-url) "/" (no-slashes path)) 
                  (do 
                      (println  "WARNING: No dsa-url configured" )
                      (warn "WARNING: No dsa-url configured" )
                      (str "http://localhost:8080" "/" "security/authenticate")))]
      (debug "Using dsa-url: " mypath )
      mypath))

(defn- request
  "like http/request but doesn't crash on failed auth"
  [req]
  (try (http/request req)
    (catch ClientProtocolException e
      ;; TODO -- check if the cause is a MalformedChallengeException
      {:status 401})))

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
    (-> (if (= :get method)
          {:query-params arguments}
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
  get-security-authenticate)

(defn authenticate
  "Call security/authenticate userid password"
  [user-id password]
  (binding [*dsa-auth* [user-id password]]
    (fake/fake-authenticate user-id password)
    ;(get-security-authenticate {})
    ))

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
