(ns org.healthsciencessc.rpms2.consent-collector.dsa-client
  (:require [clojure.string :as s]
            [org.healthsciencessc.rpms2.consent-domain.core :as domain]
            [org.healthsciencessc.rpms2.consent-collector.helpers :as helper]
            [clj-http.client :as http])
  (:import org.apache.http.auth.MalformedChallengeException
           org.apache.http.client.ClientProtocolException)
  (:use [slingshot.slingshot :only (try+)])
  (:use [org.healthsciencessc.rpms2.consent-collector  [config :only (config)]
                                                       [i18n :only [i18n i18n-label-for i18n-placeholder-for]]
                                                       [debug :only (debug!)] ]
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

  [params xorg-id]
  (let [ org-id (helper/current-org-id)
        m (remove-blank-vals (select-keys params consenter-search-fields))]
      (dsa-call :get-consent-consenters  m
                 ;;(assoc m :organization org-id)
                )))

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
  []
  nil
)

(def metadata-mock
   [
     {:name "Guarantor", 
      :organization "BLAH", 
      :default-value "Mr Smith", 
      :description "This person is the guarantor", 
      :data-type "string"} 

     {:mdid "MI002" 
      :name "Referring Doctor", 
      :organization "BLAH 2", 
      :default-value "Dr Refer Ranger", 
      :description "The referring doctor for this patient", 
      :data-type "xsd:string"} 

    {:mdid "MI001" 
     :name "Primary Care Physician", 
     :organization "BLAH 3", 
     :default-value "Dr Primary Person", 
     :description "The primary care physician for this patient", 
     :data-type "xsd:string"} 

    {:uri "urn:admission-date" :name "Admission Date", :organization "BLAH 4", 
     :default-value "today", 
     :description "The date the patient was admitted for this consent", 
     :data-type "xsd:date"}
   ]
)

(defn get-published-protocols-meta-items
  "Returns meta-items for published protocols.
  Sends the list of protocols and version ids and gets back
  the list of meta data items for these forms.

  TODO: call dsa method with specified ids to find the real meta data items"
  [protocol-ids]

  metadata-mock)

(defn get-published-protocols
  "Returns published protocols for the currently logged in user 
  at the currently selected location."

  []
  ;(println "get-published-protocols")
  (let [resp (try+ 
               (dsa-call :protocol-versions-published {:location (session-get :org-location) })
               (catch Exception ex (do 
                 (debug "get-published-protocols exception " ex) 
                 #_(println "get-published-protocols exception " ex)))
              (catch Object ex (do 
                  (debug "get-published-protocols object " ex) 
                  #_(println "get-published-protocols object " ex))))]
      (do
        #_(println "get-published-protocols ==> dsa resp is " resp)
        (debug "get-published-protocols ==> dsa resp is " resp)

  (list {:protocol 
          {:status "published", 
           :name "Lewis Blackman Hospital Patient Safety Act Acknowledgeement", 
           :organization {:name "Some Org", :code "mo", :id "1"}, 
           :languages [ {:name "English", :code "EN", :id "LANG_EN01"} 
                        {:name "Spanish", :code "SP", :id "LANG_EN02"}], 
           :id "P0001"
           :protocol-id "MO01" }, } 

         {:protocol 
            {:status "published", :name "Lewis Blackman Hospital Patient Safety Act Acknowledgeement", 
             :organization {:name "Some Org", :code "mo", :id "1"}, 
             :languages [{:name "English", :code "EN", :id "LANG_EN01"} 
                         {:name "Spanish", :code "SP", :id "LANG_EN02"}], 
                 :id "P0002"}, 
          } 
          {:protocol 
                    {:status "published", :name "Lewis Blackman Hospital Patient Safety Act Acknowledgeement", 
                     :organization {:name "Some Org", :code "mo", :id "1"}, 
                     :languages [{:name "English", :code "EN", :id "LANG_EN01"} 
                                 {:name "Spanish", :code "SP", :id "LANG_EN02"}], 
                     :id "P0003"}, 
           }

        {:protocol 
           {:status "published", :name "Lewis Blackman Hospital Patient Safety Act Acknowledgeement", 
             :organization {:name "Some Org", :code "mo", :id "1"}, 
             :languages [{:name "English", :code "EN", :id "LANG_EN01"} 
                         {:name "Spanish", :code "SP", :id "LANG_EN02"}], 
                     :id "P0004"}, 
} 
))))


(defn- get-languages
  "Temporary method to return languages."
  []

  (list {:id "LANG_EN01" :code "EN" :name "English" }
        {:id "LANG_SP01" :code "SP" :name "Spanish" }
        {:id "LANG_GE01" :code "GP" :name "German" } ) )

(defn get-protocols
  []
  (list 
    {:protocol-id "P0001" 
     :name "Lewis Blackman Hospital Patient Safety Act Acknowledgeement" 
     :select-by-default true 
     :required true 
     :description "Inform patient of right of access to attending physician" } 

    {:protocol-id "P0002"
     :name "Consent for Medical Treatment" 
     :select-by-default false 
     :required false 
     :description "Some consent for medical treatment stuff " } 

    {:protocol-id "P0003"
     :name "Medicare" 
     :select-by-default false 
     :required false 
     :description "Medicare stuff" }  

    {:protocol-id "P0004"
     :name "Tricare" 
     :select-by-default true 
     :required false 
     :description "Tricare stuff" } ))


(defn get-available-protocols-and-languages
  "Returns a map with the available protocols 
  and languages:

  Example:

  {:available-protocols [ {:id <Protocol1> ... } { <Protocol2> .. }  ... ] 
   :languages [ <LANG-1> <LANG-2> ] 
  }

  TODO: compute languages by extract out the language component, getting the unique list"
  []

  {:available-protocols (get-protocols)
   :languages (get-languages) 
  })

(let [ propname "rpms2.dsa.url" 
       dsa-url (config propname) ]
     (if dsa-url (debug "using " propname " value of " dsa-url)
         (warn "WARNING: no value for property " propname " configured")))

