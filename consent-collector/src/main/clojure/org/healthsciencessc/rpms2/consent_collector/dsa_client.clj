(ns org.healthsciencessc.rpms2.consent-collector.dsa-client
  (:require [clojure.string :as s]
            [org.healthsciencessc.rpms2.consent-domain.core :as domain]
            [clj-http.client :as http])
  (:import org.apache.http.auth.MalformedChallengeException
           org.apache.http.client.ClientProtocolException)
  (:use [slingshot.slingshot :only (try+)])
  (:use [org.healthsciencessc.rpms2.consent-collector  [config :only (config)]
                                                       [i18n :only [i18n i18n-label-for i18n-placeholder-for]]
                                                       [debug :only (debug!)] ]
        [clojure.tools.logging :only (debug info error warn)]
        [clojure.pprint :only (pprint)]
        [clojure.data.json :only (read-json json-str)]))

(def ^:dynamic *dsa-auth* nil)

;; we need a list of the required fields based on what is in 
;; consent domain
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


;;consider using values from domain-services
;;MERGE WITH values from domain-services (consenter-id is required but first-name and last-name is not)
(defn- get-consent-domain-required-consenter-fields
  []
  (let [attrs (:attributes (domain/default-data-defs "consenter")) ]
    (filter #(:required (attrs %)) (keys attrs))))

(def my-create-consenter-required-fields [ :first-name
                                        :last-name 
                                        :gender
                                        :dob
                                        :zipcode ])

(defn- generate-create-consenter-required-fields
  "Add the required fields to our list of required fields, to make sure we get everything.
  Note: currently (2012-05-16)  consent-domain is not adding first-name last-name but this app is requiring those"
  []
  (distinct (flatten (merge my-create-consenter-required-fields (get-consent-domain-required-consenter-fields)))))

(def create-consenter-required-fields (generate-create-consenter-required-fields))
(debug "GENERATED create-consenter-required-fields ==> " create-consenter-required-fields)

(defn- no-slashes [s] (second (re-matches #"/*([^/].*[^/])/*" s)))

(defn- build-url 
   "Builds url for DSA for given path."
   [path]
   (let [ dsa-url (config "rpms2.dsa.url") ]
      no-slashes (fn [s] (second (re-matches #"/*([^/].*[^/])/*" s)))
      (if dsa-url 
                  (str (no-slashes dsa-url) "/" (no-slashes path)) 
                  (str "http://obis-rpms-neodb-dev.mdc.musc.edu:8080/" (no-slashes path)))))

;; TODO - find out why the auth isn't working right (we shouldn't
;; be getting this exception)
(defn- request
  "like http/request but doesn't crash on failed auth"
  [req]
  (try+ 
    (do
      (debug (str "request REQ: " req))
      (let [resp (http/request  req )]
        (debug (str "DSA RESPONSE: " resp))
        (debug (str "DSA RESPONSE(pretty): " (pprint resp)))
        resp))
    (catch ClientProtocolException e
      ;; TODO -- check if cause is a MalformedChallengeException
      (do 
        (error (str "ClientProtocol Exception " req " FAILED " (.getMessage e) )) 
        {:status 401}))
    (catch java.net.UnknownHostException ex
        ;; we want to define flash message here
        (do 
           (debug "UNKNOWN HOST " ex)
           {:status 500 :error-message (str "Unknown host: " ex) }))
    (catch slingshot.ExceptionInfo ex
      (do (error "SLINGSHOT EXCEPTION" ex)
        {:status 403  :body (pr-str "INVALID REQUEST " ex " request: "  req)}))

    (catch Exception ex 
      (do 
        (debug "SOME OTHER ERROR: " ex)
        {:status 500 :body (pr-str "INVALID REQUEST " ex " request: "  req)}))
    (catch Object obj 
      (do 
        (error "==http request failed --> " (pprint obj))
        {:status (:status obj) :body (print-str "OBJ INVALID REQUEST - see logs for details" )}))))

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
              (try 
                (assoc resp :json (read-json body))
                (catch Exception ex (do (debug "WARNING: BODY IS NOT JSON " body ) resp) ))
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

  [params org-id]
  (let [consenter-params (remove-blank-vals
                          (select-keys params consenter-search-fields)) ]
      (dsa-call :get-consent-consenters (assoc consenter-params :organization org-id))))

(defn dsa-create-consenter
  "Create a consenter."
  [params]
  (debug "dsa-create-consenter " params )
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

(defn generate-meta-data-items
 []
 (list

{ :id (id) :name "additional-guarantor" :description "Additional guarantor" :data-type "string" :organization "MYORG" }
{ :id (id) :name "referring-doctor" :description "Date admitted" :data-type "string" :organization "MYORG" }
{ :id (id) :name "referring-doctor-city" :description "" :data-type "string" :organization "MYORG" }
{ :id (id) :name "primary-care-physician" :description "" :data-type "string" :organization "MYORG" }
{ :id (id) :name "primary-care-physician-city" :description "" :data-type "string" :organization "MYORG" }
{ :id (id) :name "attending-physician" :description "" :data-type "string" :organization "MYORG" }
{ :id (id) :name "advanced-directives-given" :description "" :data-type "yes-no" :organization "MYORG" }
{ :id (id) :name "admission-date" :description "Date admitted" :data-type "string" :organization "MYORG" }
{ :id (id) :name "form-signer" :description "Signer" :data-type "choice - patient or patient rep" :organization "MYORG" }

  )
)

#_(defn generate-protocol
  [prototype]
  { :name  (:name prototype)
    :description (if (:description prototype) (:description prototype) "description for protocol")
    :protocol-id "generated protocol-id"
    :code "description for protocol"
    :required (if (:required prototype) (:required prototype) false )
    :select-by-default (if 
	(:select-by-default prototype) 
	(:select-by-default prototype) false )
    :organization "description for protocol"
    :location "description for protocol"
  })

(def get-protocol-metadata
  {
   "P0001" (list :meta-a :meta-b  "one" "two") 
   "P0002" (list :meta-b "one" "two") 
   "P0003" (list :meta-a :meta-c "one" "two") 
   "P0004" (list :meta-d "one" "two") 
  })

(defn get-protocols-version
  [protocols]
  ;;for each protocol, collect the meta data fields into a set
  ;;return the set
  {}
)

(defn xx-get-published-protocols
  []
  (list
    {:protocol {:id "P0001" 
                 :name "Lewis Blackman Hospital Patient Safety Act Acknowledgeement" 
                 :status "published"
                 :languages [ 
                              {:id "LANG_EN01" :code "EN" :name "English" }
                              {:id "LANG_EN02" :code "SP" :name "Spanish" } ]
                 :organization { :id 1 :name "Some Org" :code "mo" } } }


    {:protocol {:id "P0002" 
                 :name "Consent for Medical Treatment" 
                 :status "published"
                 :languages [ 
                              {:id "LANG_EN01" :code "EN" :name "English" }
                              {:id "LANG_EN02" :code "SP" :name "Spanish" } ]
                 :organization { :id 1 :name "Some Org" :code "mo" } } }


    {:protocol {:id "P0003" 
                 :name "Medicare" 
                 :status "published"
                 :languages [ 
                              {:id "LANG_EN01" :code "EN" :name "English" }
                              {:id "LANG_EN02" :code "Greek" :name "Greek" } ]
                 :organization { :id 1 :name "Some Org" :code "mo" } } }

    {:protocol {:id "P0004" 
                 :name "Tricare" 
                 :status "published"
                 :languages [ 
                              {:id "LANG_EN01" :code "EN" :name "English" }
                              {:id "LANG_EN02" :code "GE" :name "German" } ]
                 :organization { :id 1 :name "Some Org" :code "mo" } } }
    )
 ) 


(defn get-published-protocols
  []
  (list
    {:protocol {:id "P0001" 
                 :name "Lewis Blackman Hospital Patient Safety Act Acknowledgeement" 
                 :status "published"
                 :languages [ 
                              {:id "LANG_EN01" :code "EN" :name "English" }
                              {:id "LANG_EN02" :code "SP" :name "Spanish" } ]
                 :organization { :id 1 :name "Some Org" :code "mo" } } 
                 :meta-items [ {:name "Meta-1" :description "This is meta-1" 
                                :data-type "xsd:string"
                                :default-value "Meta-1 Default Value"
                                :organization "BLAH" }
                               {:name "Meta-2" :description "This is meta-2" 
                                :data-type "xsd:string"
                                :default-value "Meta-2 Default Value"
                                :organization "BLAH 2" }
                               {:name "Meta-3" :description "This is meta-3" 
                                :data-type "xsd:string"
                                :default-value "Meta-3 Default Value"
                                :organization "BLAH 3" }
                               {:name "Meta-4" :description "This is meta-4" 
                                :data-type "xsd:string"
                                :default-value "Meta-4 Default Value"
                                :organization "BLAH 4" } ]
                 ;; :policies [ "Meta-1" "Meta-2" "Meta-3" "Meta-4" ]
                 ;; :endorsements [ "Meta-1" "Meta-2" "Meta-3" "Meta-4" ]
                 ;; :form [ "Meta-1" "Meta-2" "Meta-3" "Meta-4" ]
                 ;; :protocol [ "Meta-1" "Meta-2" "Meta-3" "Meta-4" ]
     }


    {:protocol {:id "P0002" 
                 :name "Consent for Medical Treatment" 
                 :status "published"
                 :languages [ 
                              {:id "LANG_EN01" :code "EN" :name "English" }
                              {:id "LANG_EN02" :code "SP" :name "Spanish" } ]
                 :organization { :id 1 :name "Some Org" :code "mo" } } }


    {:protocol {:id "P0003" 
                 :name "Medicare" 
                 :status "published"
                 :languages [ 
                              {:id "LANG_EN01" :code "EN" :name "English" }
                              {:id "LANG_EN02" :code "Greek" :name "Greek" } ]
                 :organization { :id 1 :name "Some Org" :code "mo" } } }

    {:protocol {:id "P0004" 
                 :name "Tricare" 
                 :status "published"
                 :languages [ 
                              {:id "LANG_EN01" :code "EN" :name "English" }
                              {:id "LANG_EN02" :code "GE" :name "German" } ]
                 :organization { :id 1 :name "Some Org" :code "mo" } } }
    )
 ) 

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


(let [ propname "rpms2.dsa.url" 
       dsa-url (config propname) ]
     (if dsa-url (debug "using " propname " value of " dsa-url)
         (warn "WARNING: no value for property " propname " configured")))

