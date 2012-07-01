(ns org.healthsciencessc.rpms2.consent-collector.dsa-client
  (:require [clojure.string :as s]
            [org.healthsciencessc.rpms2.consent-domain.core :as domain]
            [org.healthsciencessc.rpms2.consent-collector.mock :as mock]
            [clj-http.client :as http])
  (:import org.apache.http.auth.MalformedChallengeException
           org.apache.http.client.ClientProtocolException)
  (:use [slingshot.slingshot :only (try+)])
  (:use [org.healthsciencessc.rpms2.consent-collector  [config :only (config)]
                                                       [i18n :only [i18n i18n-label-for i18n-placeholder-for]]
                                                       [debug :only (debug! pprint-str)] ]
        [clojure.tools.logging :only (debug info error warn)]
        [clojure.pprint :only (pprint)]
        [sandbar.stateful-session :only (session-get session-put!)]
        [clojure.data.json :only (read-json json-str)]))

(def ^:dynamic *dsa-auth* nil)

;; need required fields from consent domain
(def consenter-search-fields  [:first-name
                               :last-name
                               :consenter-id
                               :dob
                               :zipcode])

(defn- generate-default-consenter-id
  []
  (str "CONSENTER-" (rand-int 1000)))

(def create-consenter-fields [ :first-name
                               :middle-name
                               :last-name
                               :title
                               :suffix
                               :consenter-id
                               :gender
                               :dob
                               :zipcode ])


(defn- get-create-required
  "Add consent domain required fields to our list of required fields, 
  to make sure we get everything.  Note: currently (2012-05-16)  
  consent-domain is not adding first-name last-name 
  but this app is requiring those"
  []
  (let [attrs (:attributes (domain/default-data-defs "consenter")) 
        domain-required (filter #(:required (attrs %)) (keys attrs)) ] 
       (distinct (flatten 
                   (merge [:first-name :last-name :gender :dob :zipcode]
                          domain-required)))))

(defonce create-consenter-required-fields (get-create-required))
;(debug "GENERATED create-consenter-required-fields ==> " create-consenter-required-fields)

(defn- no-slashes [s] (second (re-matches #"/*([^/].*[^/])/*" s)))

(defn- build-url 
   "Builds url for DSA for given path."
   [path]
   (let [ dsa-url (config "rpms2.dsa.url") ]
      no-slashes (fn [s] (second (re-matches #"/*([^/].*[^/])/*" s)))
      (if dsa-url 
                  (str (no-slashes dsa-url) "/" (no-slashes path)) 
                  (str "http://obis-rpms-neodb-dev.mdc.musc.edu:8080/" (no-slashes path)))))

(defn- request 
  [r]
  (try+
      (let [resp (http/request r)]
        (debug "calling: " r)
        resp)
      (catch Exception e (debug "calling: " r " exception " e))
      (catch Object e (debug "calling: " r " object " e))
      (finally "after request " r )))

(defn dsa-call
  [process-name arguments]
  (let [[_ method path-dashes] 
        (re-matches #"(get|post|put|delete)-(.+)" (name process-name))
        method (keyword method),
        path (s/replace path-dashes "-" "/")
        maybe-parse-json
        (fn [{:keys [content-type status body headers] :as resp}]
          (if (= nil body) 
              (assoc resp :json {})
              (assoc resp :json body) )
          ) ]
    
    (-> (if (= :get method)
          (if (empty? arguments)
            {}
            {:query-params arguments})
          ;;{:body (json-str arguments) })
          {:body (pr-str arguments) })
        (assoc :request-method method
               :basic-auth *dsa-auth* 
               :as :clojure
               :content-type "application/clojure"
               :throw-exceptions false
               :url (build-url path) )
        request  
        maybe-parse-json
      )))


(defn authenticate
  "Call security/authenticate userid password"
  [user-id password]
  (binding [*dsa-auth* [user-id password]]
    (dsa-call :get-security-authenticate {})))

(defn- remove-blank-vals
  "Given a map, removes all key/val pairs for which the value
  is blank."
  [m]
  (into {}
        (for [[k v] m :when (not (s/blank? v))]
          [k v])))

(defn- has-all-required-fields 
  "Given a map, ensures all required fields are there. 
  Returns a list of missing fields."
  [m required-fields]

  (if (= (count (select-keys m required-fields)) (count required-fields))
    nil
    (pr-str "All of these are required: " required-fields)))

(defn dsa-search-consenters
  "Search consenters."

  [params]
  (let [org-id (get-in (session-get :user) [:organization :id])
        m (remove-blank-vals (select-keys params consenter-search-fields))]
      (dsa-call :get-consent-consenters  m)))

(defn dsa-create-encounter 
  [e]
  (let [location (session-get :org-location)
        consenter (session-get :consenter)
        encounter (merge e {:location location}
                         {:consenter consenter})
        resp (dsa-call :put-consent-encounter encounter)]
    (debug "dsa-create-encounter" e location consenter)
    (debug "dsa-create-encounter-resp" resp)
    (condp = (:status resp)
      200 (:body resp)
      nil)))

(defn dsa-create-consenter
  "Create a consenter."
  [params]
  (let [p (remove-blank-vals (select-keys params create-consenter-fields)) 
        invalid (has-all-required-fields p create-consenter-required-fields)]
      (if invalid 
          (do (debug "INVALID - CANNOT CREATE " p  " invalid msg " invalid)
              {:status 409 :body 
               (i18n :create-consenter-form-validation-failed) })
          (do (debug "dsa-create-consenter P = " p  " count " (count p))
              (dsa-call :put-consent-consenter p) ))))
      

(defn- id
  []
  (rand-int 1000000000))

(def consenter-field-defs { 
  :first-name          { :required true :i18n-name "first-name" }
  :middle-name         {}
  :last-name           { :required true :i18n-name "last-name"}
  :title               {}
  :suffix              {}
  :consenter-id        { :required true :default-value generate-default-consenter-id }
  :gender              { :required true :type "gender" :i18n-name "gender" }
  :dob                 { :required true :type "date" :i18n-name "date-of-birth"}
  :zipcode             { :required true :type "number" :i18n-name "zipcode" } 
})

(def encounter-field-defs [[:encounter-id {:i18n-name "encounter-id" :required true}]
                           [:date {:type "date" :i18-name "encounter-date" :required true}]])

(defn get-published-protocols-form 
  "Returns the form for a single protocol."
  [pv-id]
  (let [lang (session-get :selected-language)
        resp (dsa-call :get-protocol-versions-published-form {:language lang
                                                              :protocol-version pv-id})]
    (-> resp :body first)))     ;; this dsa call returns a list, but we only call it with one param, so return first

(defn- fix-metadata
  "Ensure that meta data is a map with the key being the id."
  [col]
  (let [r (apply merge {} (for [a col] (hash-map (keyword (:id a)) a))) ]
    (println "fix-metadata " r)
    r))

(defn get-published-protocols-meta-items
  "Returns meta-items for published protocols."
  [protocol-ids]
  (:body (dsa-call :get-protocol-versions-published-meta 
               {:protocol-version protocol-ids})))

(defn get-published-protocols
  "Returns published protocols for currently logged in user 
  at the currently selected location."
  []
  (let [resp (dsa-call :get-protocol-versions-published {:location (:id (session-get :org-location)) })]
    (:body resp)))


(defn get-nth-form-raw
  "Return the nth form."
  [n]
  (if (< n (count (session-get :selected-protocol-version-ids)))
    (let [pv-id (nth (session-get :selected-protocol-version-ids) n)]
      (get-published-protocols-form pv-id))))

(defn get-nth-form
  [n]
  (if (< n (count (session-get :selected-protocol-version-ids)))
    (let [b (get-nth-form-raw n) ]
      (if (config "mock-data") 
          (if (= n 1) mock/lewis-blackman-form mock/sample-form)
          b))))

(let [ propname "rpms2.dsa.url" 
       dsa-url (config propname) ]
     (if dsa-url (debug "using " propname " value of " dsa-url)
         (warn "WARNING: no value for property " propname " configured")))

;;(debug! get-published-protocols-meta-items)
;;(debug! get-published-protocols-form)

