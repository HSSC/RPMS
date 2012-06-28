(ns org.healthsciencessc.rpms2.consent-collector.metadata
  "Processes for the meta data view and actions. Generates a form for user to enter meta data
  items.  
  
  The items are on the form are determined by the dsa call
     (dsa/get-published-protocols-meta-items protocol-ids) which will return 
     map of metadata items, which is a map where the key is the meta-data id
     and the value is meta-data info for that key.

     This is stored in the session variable :all-meta-data 

  When the form is submitted, the values are updated in the session variable :all-meta-data 
     which will now contain a  map of metadata items, 
     each of which is a map with [:name :id :data-type :value]
  "
  (:require
   [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
   [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [clojure.pprint :only (pprint)])
  (:use [org.healthsciencessc.rpms2.consent-collector.debug :only [debug! pprint-str]])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only (i18n)]))

(def ^:private field-type {"date" "date"
                           "string" "text"
                           "boolean" "checkbox"})

(defn- get-label [mi]
  (:name mi))
  ;; FIXME Either call the service that returns the right language,
  ;; or dig into the :labels on the map

(defn- emit-mi-field
  [mi]
  [:div.inputdata {:data-role "fieldcontain" } 
      [:label {:for (:id mi) :class "labelclass" } (get-label mi)  ]
      [:input {:type (get field-type (:data-type mi) "text")
               :name (:id mi)
               :id (:id mi)
               :class "inputclass"}]])

(defn- grab-metaitems
  [protocol-ids]
  (let [selected (set protocol-ids) 
        proto-vers (session-get :protocol-versions)]
    (->> proto-vers
      (filter #(contains? selected (:id %)))
      (map :meta-items)
      (apply concat)
      distinct)))
 
(defn view 
  "Returns Consenter Information ( meta data ) form"
  [ctx]
  (let [ids (session-get :selected-protocol-version-ids) 
        meta-data (into {} (for [{id :id :as mi} (grab-metaitems ids)]
                             [(keyword id) mi]))]
    ;meta-data (dsa/get-published-protocols-meta-items protocol-ids)]  probably call the service when it works?
    (session-put! :all-meta-data meta-data)
    (if (empty? meta-data)
      (helper/myredirect "/collect/consents")
      (helper/rpms2-page 
        (helper/post-form "/view/meta-data"
                          (list [:div.left "Enter the following information:" ]
                                (for [[_ mi] meta-data]
                                   (emit-mi-field mi)))
                          (helper/submit-btn { :value (i18n :meta-data-form-submit-button) } ))
        :title (i18n :hdr-metadata)
        :cancel-btn (helper/cancel-form "/view/select/consenter")))))

(defn perform
  "Save meta data and prepare to enter the data.
  Takes the values specified and adds them to 
  session :all-meta-data "
  [{parms :body-params :as ctx}]
  (let [meta-data (session-get :all-meta-data)
        answer-map (into {} (for [[mi-id answer] parms]
                              [mi-id (assoc (get meta-data mi-id)
                                            :value
                                            (str answer))]))]
    (session-put! :all-meta-data answer-map)
    (helper/myredirect "/collect/consents")))
