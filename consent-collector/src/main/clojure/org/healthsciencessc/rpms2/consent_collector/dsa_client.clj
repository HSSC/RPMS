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

;; where to catch exceptions
;;  java.net.UnknownHostException

(defn get-protocols-version
  [protocols]
  ;;for each protocol, collect the meta data fields into a set
  ;;return the set
  {}
)

(defn get-languages
  []
  (list 
    {:id "LANG_EN01" :code "EN" :name "English" }
    {:id "LANG_EN02" :code "SP" :name "Spanish" } 
    {:id "LANG_EN03" :code "GP" :name "German" } ))

(defn old-get-published-protocols
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
  (list {:protocol 
          {:status "published", :name "Lewis Blackman Hospital Patient Safety Act Acknowledgeement", 
           :organization {:name "Some Org", :code "mo", :id "1"}, 
           :languages [
                       {:name "English", :code "EN", :id "LANG_EN01"} 
                       {:name "Spanish", :code "SP", :id "LANG_EN02"}], 
           :id "P0001"}, 

:meta-items [
{:uri "urn:gurantor" :name "Guarantor", :organization "BLAH", :default-value "Mr Smith", :description "This person is the guarantor", :data-type "string"} 

{:mdid "MI002" :uri "urn:referring-doctor" :name "Referring Doctor", :organization "BLAH 2", :default-value "Dr Refer Ranger", :description "The referring doctor for this patient", :data-type "xsd:string"} 

{:mdid "MI001" :uri "urn:primary-care-physician" :name "Primary Care Physician", :organization "BLAH 3", :default-value "Dr Primary Person", :description "The primary care physician for this patient", :data-type "xsd:string"} 

{:uri "urn:admission-date" :name "Admission Date", :organization "BLAH 4", :default-value "today", :description "The date the patient was admitted for this consent", :data-type "xsd:date"}
             ]

} 

               {:protocol 
                    {:status "published", :name "Lewis Blackman Hospital Patient Safety Act Acknowledgeement", 
                     :organization {:name "Some Org", :code "mo", :id "1"}, 
                     :languages [{:name "English", :code "EN", :id "LANG_EN01"} 
                                 {:name "Spanish", :code "SP", :id "LANG_EN02"}], 
                     :id "P0002"}, 
                    :meta-items [
{:uri "urn:gurantor" :name "Guarantor", :organization "BLAH", :default-value "Mr Smith", :description "This person is the guarantor", :data-type "string"} 
{:uri "urn:admission-date" :name "Admission Date", :organization "BLAH 4", :default-value "today", :description "The date the patient was admitted for this consent", :data-type "xsd:date"}

                ]} 


               {:protocol 
                    {:status "published", :name "Lewis Blackman Hospital Patient Safety Act Acknowledgeement", 
                     :organization {:name "Some Org", :code "mo", :id "1"}, 
                     :languages [{:name "English", :code "EN", :id "LANG_EN01"} 
                                 {:name "Spanish", :code "SP", :id "LANG_EN02"}], 
                     :id "P0003"}, 
                    :meta-items [
{:uri "urn:gurantor" :name "Guarantor", :organization "BLAH", :default-value "Mr Smith", :description "This person is the guarantor", :data-type "string"} 
{:uri "MD-1" :name "Meta-1", :organization "BLAH", :default-value "Meta-1 Default Value", :description "This is meta-1", :data-type "xsd:string"} 
{:uri "MD3-1" :name "Meta-2", :organization "BLAH 2", :default-value "Meta-2 Default Value", :description "This is meta-2", :data-type "xsd:string"} 
{:uri "MD3-2" :name "Meta-3", :organization "BLAH 3", :default-value "Meta-3 Default Value", :description "This is meta-3", :data-type "xsd:string"} ]}

{:protocol 
           {:status "published", :name "Lewis Blackman Hospital Patient Safety Act Acknowledgeement", 
                     :organization {:name "Some Org", :code "mo", :id "1"}, 
                     :languages [{:name "English", :code "EN", :id "LANG_EN01"} 
                                 {:name "Spanish", :code "SP", :id "LANG_EN02"}], 
                     :id "P0004"}, 
                    :meta-items [

{:uri "urn:gurantor" :name "Guarantor", :organization "BLAH", :default-value "Mr Smith", :description "This person is the guarantor", :data-type "string"} 

                                 ]} 

))

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


(def metadata-map
    { 
        :MI001 { :mdid "MI001" :label "Primary Care Physician", :value "Dr. Bob Smith" },
        :MI002 { :mdid "MI002" :label "Refering Physician", :value "Dr. Bob Jones" }
    }
)

(def policy-map
  { :P00001 { :title "Your Permission Is Needed",
         :text ["I, Bob R. Smith, consent to and authorize medical treatment and diagnostic procedures which may be ordered by my doctors and performed by MUSC Medical Center (\"The Hospital\"). I consent to have blood drawn and to be tested for infectious diseases, including but not limited to: syphilis, AIDS, hepatitis, and testing for drugs if my doctor orders these tests."],
          :media  "http://obis-vac-stg.mdc.musc.edu/videos/bodpod.mp4" 
           } ,
    :P00003  {
       :title "Your Agreement to Pay for Treatment",
       :text [
"I assign and transfer to The Hospital and / or my doctors all rights, and interest in benefits I may have under any insurance policy I may have, including but not limited to hospitalization, medical, third party liability insurance coverage, workers compensation benefits, or benefits paid by Medicare or Medicaid. This assignment is intended to include any interest in benefits that I may have relating to this date of service as well as any prior dates of service. I direct that any insurance company or other party make payment of such benefits to The Hospital or my doctor. I authorize The Hospital and / or my doctor to collect benefits from any responsible third party through whatever means may be deemed necessary, and to endorse benefit checks made payable directly to me.", 

              "I understand that by signing below, I promise to pay all Hospital and doctor charges at the standard rates and terms of The Hospital or doctor including all charges not covered by my insurance or any other party. I promise to pay the patient's account at the rates stated in The Hospital's price list (known as the \"Charge Master\") and / or the doctor's fee schedule in effect on the date the charge is processed for the services provided. I understand that there will be a separate charge for the doctor and other professional services, but understand that The Hospital may bill for some professional fees.",
"I understand that The Hospital files insurance as a courtesy to me, and agree that I am responsible for payment of my bill, including any charges that are denied by my insurance or any other responsible party. I understand that care that is experimental as determined by my insurance company may not be covered and that I will be responsible for those charges. I agree that if this account is not paid, it may be turned over to a collection agency or attorney, and I must pay the amount due plus all costs of collection, including reasonable attorney's fees.", 

              "I understand that if I am unable to pay my bills, I may speak with a Financial Counselor to determine whether I qualify for assistance or for a discount. I may call (843) 792-2311 for information or questions about my hospital bill and (843) 792-6200 for information about my doctor bill."]
    },
   :P00004 {
:title "Retention / Disposal and Use of Blood, Body Fluids, or Tissue",
:text [ 
     "I understand and agree that any blood, body fluids or tissues normally removed from my body by MUSC in the course of any diagnostic procedures, surgery, or medical treatment that would otherwise be disposed of may be retained and used for research, including research on the genetic material (DNA) or other information contained in those tissues or specimens. I acknowledge that such research by MUSC may result in new inventions that may have commercial value and I understand that there are no plans to compensate me should this occur, regardless of the value of any such invention. I understand that any research using these leftover specimens or tissues will be done in a way that will not identify me.", 
"I also understand that if I do not want research to be done using my leftover blood, body fluids or tissue, I need to check the box shown below. If I have questions, I should call (843) 792-8300."]
	},

  :P00005 {
	:title "Permission to Contact for Research Studies",
	:text [
"I agree to be contacted about future research studies at MUSC for which I may be eligible. I understand that if I do not want to be contacted about future research studies, I need to check the box shown below."]
	},
  :P00006 {
	:title "Admission Photographs",
	:text [
"I agree that my photograph may be taken for purposes of identifying me, or providing treatment to me. This photograph may become part of my medical record and may be disclosed if copies of my medical record are disclosed."]
	},
   :P00007 {
	:title "Responsibility for Personal Items",
	:text [
"I understand that The Hospital is not responsible for valuable items which I bring with me. I understand it is my responsibility to send any valuable items (such as medications, money, jewelry, electronics, etc.) home for safe keeping. Any items left at the hospital in excess of 30 days will be disposed of."]
    },
  :P00008 {
	:title "I received a copy of the MUSC Notice of Privacy Practices.",
	:text ["I received a copy of the MUSC Notice of Privacy Practices."]
   },
   :P00009 {
	:title "Consent and Certifications",
	:text [
"I certify that I have read or have had read to me this consent and agree to its terms. I also certify that I am the patient, or am duly authorized by the patient, or am duly appointed to sign this agreement. I accept and understand its terms."]
    } 
   }  	
)

(defn get-policy
  [p]
  (get policy-map (keyword p)))

(defn get-metadata
  [p]
  (debug "METADATA " p " IS " (get metadata-map (keyword p)))
  (get metadata-map (keyword p)))




(def lewis-blackman-form
{
:policies {:P00001 {:title "Your Acknowledgement Is Needed",:text [ "I understand that MUSC is an academic medical center providing healthcare, teaching, and research. I also understand that I may be cared for by physicians and other healthcare providers in training. My care will be supervised by an attending physician and staff. The information related to the health care I receive may be used for training or for scientific study purposes. I understand that if information contained in my health care record is used for such purposes, precautions will be taken to carefully preserve my anonymity. I also understand that pictures or other recordings may be made for purposes of my treatment or for educational purposes.", "In addition, I understand that if I have questions about my medical care I can speak to my attending physician, nurse, resident, or medical student. In addition, I can contact my attending physician or the House Services Coordinator through the operator at 792-8080 (2-8080 from the bedside phone).","I acknowledge that I have received a copy of the brochure entitled \"Medical University of South Carolina, An Academic Medical Center of Excellence\" explaining the role of residents and students in my care."]}}



    :meta-items {
        :MI001 {
            :label "Admission Date",
            :value "05-24-2012" 
        },

	:MI002 {
            :label "Gaurantor",
            :value "Medical Services Foundation" 
        },
    },    

:endorsements {:E00001 {:label "Signature of Consenter", :endorsement "the base64/png encoded signature value."}}

:form {
:header { :title "Lewis Blackman Hospital Patient Safety Act Acknowledgement" },
:contains [{ 
:type "page",
:name "page1",
:title "Acknowledgement", 
:contains [{ :type "section",
:name "section1",
:contains [
{:type "text",
:name "info",
:title "Verify Information",
:text ["Verify the information below and use corresponding change button to correct as needed."]
},	
{ :type "data-change", :name "", :meta-items ["MI001", "MI002"] } ] },
{ :type "section",
:name "section2",
:contains [{ :type "policy-text",
:name "Acknowledge",
:policy "P00001",
:render-title false,  
:render-text true    },
{ :type "signature",  
:name "Sign Here", 
:endorsement "E00001", 
:clear-label "Clear" }]
}],}],
:footer { :title ["Patient Name: Bob R. Smith"] }, 
:collect-start "page1", 
:summary-start "summary1"
}}
  )


(defn sample-form 
  []
  
  {:form 
   ;;{:policies policy-map }
   {:header 
    {:title "Consent For Medical Treatment"}, 
    :contains [
               {:type "page", :name "page1", :title "Consent To Treat Me", 
                :contains [
                           {:type "section", :name "section1", 
                            :contains [{:type "policy-text", 
                                        :name "MedicalTreatmentText", 
                                        :policy "P00001", 
                                        :render-title true, 
                                        :render-text true, 
                                        :render-media true} 
                                       {:type "policy-choice-buttons", 
                                        :name "MedicalTreatmentChoiceButtons", 
                                        :policy "P00001", 
                                        :true-label "I Agree", 
                                        :false-label "I do not agree"}]} 
                           {:type "section", 
                            :name "section2", 
                            :contains [
                                       {:type "text", 
                                        :name "ReferringDoctorText", 
                                        :title "Release of Information to My Referring Doctor", 
                                        :text ["If either of these are not correct, please press the corresponding change button."]}]} 
                           {:type "section", 
                            :name "section3", 
                            :contains [{:type "data-change", 
                                        :name "flagThemDoctors", 
                                        :meta-items ["MI002" "MI001"]}]}], 
                :next "page2"} 
               {:type "page", :name "page2", 
                                :title "Consent To Pay", 
                                :contains [{:type "section", 
                                            :name "section1", 
                                            :contains [
                                                       {:type "policy-text", 
                                                        :name "PaymentText", 
                                                        :policy "P00003"} 
                                                       {:type "policy-choice-buttons", 
                                                        :name "PaymentChoiceButtons", 
                                                        :policy "P00003", 
                                                        :true-label "I Agree", :false-label "I do not agree"}]}], 
                                :previous "page1", 
                                :next "page3"} 
               {:type "page", 
                :name "page3", 
                :title "Tissue Retention Disposal", 
                :contains [{:type "section", :name "section1", 
                            :contains [{:type "policy-text", 
                                        :name "TissueText", 
                                        :policy "P00004"} 
                                       {:type "policy-checkbox", 
                                        :name "TissueCheckbox", 
                                        :policy "P00004", 
                                        :label "I DO NOT agree to have my tissue or blood used for future research studies.", 
                                        :checked-value false, 
                                        :unchecked-value true}]}], 
                :previous "page2", :next "page4"} 
               {:type "page", 
                :name "page4", 
                :title "Permission to Contact", 
                :contains [
                           {:type "section", 
                            :name "section1", 
                            :contains [
                                       {:type "policy-text", 
                                        :name "PermissionText", 
                                        :policy "P00005"} 
                                       {:type "policy-checkbox", 
                                        :name "PermissionCheckbox", 
                                        :policy "P00005", 
                                        :label "I DO NOT agree to be contacted for future research studies.", 
                                        :checked-value false, 
                                        :unchecked-value true}]}], 
                :previous "page3", 
                :next "page5"} 
               {:type "page", 
                :name "page5", 
                :title "Photographs", 
                :contains [{:type "section", :name "section1", :contains [{:type "policy-text", :name "PhotographText", :policy "P00006"} {:type "policy-choice-buttons", :name "PhotographChoiceButtons", :policy "P00006", :true-label "I Agree", :false-label "I do not agree"}]}], :previous "page4", :next "page6"} {:type "page", :name "page6", :title "PersonalItems", :contains [{:type "section", :name "section1", :contains [{:type "policy-text", :name "PersonalItemsText", :policy "P00007"} {:type "policy-button", :name "PersonalItemsButton", :policy "P00007", :label "I Understand", :action-value true}]}], :previous "page5", :next "page7"} 
               {:type "page", :name "page7", :title "Copy of Privacy Practices", :contains [{:type "section", :name "section1", :contains [{:type "policy-text", :name "PrivacyPracticesText", :policy "P00008"} {:type "policy-choice-buttons", :name "PrivacyPracticesChoiceButtons", :policy "P00008", :true-label "I Agree", :false-label "I do not agree"}]}], :previous "page6", :next "page8"} {:type "page", :name "page8", :title "Consent Certifications", :contains [{:type "section", :name "section1", :contains [{:type "policy-text", :name "ConsentText", :policy "P00009", :render-title false} {:type "policy-choice-buttons", :name "ConsentChoiceButtons", :policy "P00009", :true-label "I Agree", :false-label "I do not agree"}]} {:type "section", :name "section2", :contains [{:type "signature", :name "consenter", :endorsement "E00001", :clear-label "Clear"}]}], :previous "page7", :next "page9"}], 
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          :footer



                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          {:title 
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           ["Patient Name: Bob R. Smith"]}, 
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          :collect-start "page1", 
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    :summary-start "summary1"}} 

               )


(defn sample-form-2
  []
  
  {:form 
   {:header 
    {:title "2 Consent For Medical Treatment"}, 
    :contains [
               {:type "page", :name "page1", :title "Consent To Treat Me", 
                :contains [
                           {:type "section", :name "section1", 
                            :contains [{:type "policy-text", 
                                        :name "MedicalTreatmentText", 
                                        :policy "P00001", 
                                        :render-title true, 
                                        :render-text true, 
                                        :render-media true} 
                                       {:type "policy-choice-buttons", 
                                        :name "MedicalTreatmentChoiceButtons", 
                                        :policy "P00001", 
                                        :true-label "I Agree", 
                                        :false-label "I do not agree"}]} 
                           {:type "section", 
                            :name "section2", 
                            :contains [
                                       {:type "text", 
                                        :name "ReferringDoctorText", 
                                        :title "Release of Information to My Referring Doctor", 
                                        :text ["If either of these are not correct, please press the corresponding change button."]}]} 
                           {:type "section", 
                            :name "section3", 
                            :contains [{:type "data-change", 
                                        :name "flagThemDoctors", 
                                        :meta-items ["MI002" "MI001"]}]}], 
                :next "page2"} 
               {:type "page", :name "page2", 
                                :title "Consent To Pay", 
                                :contains [{:type "section", 
                                            :name "section1", 
                                            :contains [
                                                       {:type "policy-text", 
                                                        :name "PaymentText", 
                                                        :policy "P00003"} 
                                                       {:type "policy-choice-buttons", 
                                                        :name "PaymentChoiceButtons", 
                                                        :policy "P00003", 
                                                        :true-label "I Agree", :false-label "I do not agree"}]}], 
                                :previous "page1", 
                                :next "page3"} 
               {:type "page", 
                :name "page3", 
                :title "Tissue Retention Disposal", 
                :contains [{:type "section", :name "section1", 
                            :contains [{:type "policy-text", 
                                        :name "TissueText", 
                                        :policy "P00004"} 
                                       {:type "policy-checkbox", 
                                        :name "TissueCheckbox", 
                                        :policy "P00004", 
                                        :label "I DO NOT agree to have my tissue or blood used for future research studies.", 
                                        :checked-value false, 
                                        :unchecked-value true}]}], 
                :previous "page2", :next "page4"} 
               {:type "page", 
                :name "page4", 
                :title "Permission to Contact", 
                :contains [
                           {:type "section", 
                            :name "section1", 
                            :contains [
                                       {:type "policy-text", 
                                        :name "PermissionText", 
                                        :policy "P00005"} 
                                       {:type "policy-checkbox", 
                                        :name "PermissionCheckbox", 
                                        :policy "P00005", 
                                        :label "I DO NOT agree to be contacted for future research studies.", 
                                        :checked-value false, 
                                        :unchecked-value true}]}], 
                :previous "page3", 
                :next "page5"} 
               {:type "page", 
                :name "page5", 
                :title "Photographs", 
                :contains [{:type "section", :name "section1", :contains [{:type "policy-text", :name "PhotographText", :policy "P00006"} {:type "policy-choice-buttons", :name "PhotographChoiceButtons", :policy "P00006", :true-label "I Agree", :false-label "I do not agree"}]}], :previous "page4", :next "page6"} {:type "page", :name "page6", :title "PersonalItems", :contains [{:type "section", :name "section1", :contains [{:type "policy-text", :name "PersonalItemsText", :policy "P00007"} {:type "policy-button", :name "PersonalItemsButton", :policy "P00007", :label "I Understand", :action-value true}]}], :previous "page5", :next "page7"} 
               {:type "page", :name "page7", :title "Copy of Privacy Practices", :contains [{:type "section", :name "section1", :contains [{:type "policy-text", :name "PrivacyPracticesText", :policy "P00008"} {:type "policy-choice-buttons", :name "PrivacyPracticesChoiceButtons", :policy "P00008", :true-label "I Agree", :false-label "I do not agree"}]}], :previous "page6", :next "page8"} {:type "page", :name "page8", :title "Consent Certifications", :contains [{:type "section", :name "section1", :contains [{:type "policy-text", :name "ConsentText", :policy "P00009", :render-title false} {:type "policy-choice-buttons", :name "ConsentChoiceButtons", :policy "P00009", :true-label "I Agree", :false-label "I do not agree"}]} {:type "section", :name "section2", :contains [{:type "signature", :name "consenter", :endorsement "E00001", :clear-label "Clear"}]}], :previous "page7", :next "page9"}], 
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          :footer



                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          {:title 
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           ["Patient Name: Bob R. Smith"]}, 
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          :collect-start "page1", 
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    :summary-start "summary1"}} 

               )

(let [ propname "rpms2.dsa.url" 
       dsa-url (config propname) ]
     (if dsa-url (debug "using " propname " value of " dsa-url)
         (warn "WARNING: no value for property " propname " configured")))

;;(debug! get-published-protocols)
(debug! dsa-search-consenters)
