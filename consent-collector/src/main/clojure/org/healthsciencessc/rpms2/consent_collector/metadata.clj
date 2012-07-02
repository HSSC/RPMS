(ns org.healthsciencessc.rpms2.consent-collector.metadata
  "Processes for meta data. Meta data is 
   stored in the session variable :all-meta-data 
   which is a map of metadata items"

  (:require
   [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
   [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [sandbar.stateful-session :only [session-get session-put! ]])
  (:use [clojure.tools.logging :only (debug info error)])
  (:use [org.healthsciencessc.rpms2.consent-collector.debug :only [debug! pprint-str]])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only (i18n)]))

(def ^:private field-type {"date" "date"
                           "string" "text"
                           "boolean" "checkbox"})

(defn- get-label [mi]
  (:name mi))

(defn- emit-mi-field
  "If field type is checkbox, need to style differently"
  [mi]
  (if (= (:data-type mi) "boolean")
    (helper/checkbox-group {:name (:id mi)
                            :label (get-label mi)
                            :value (:value mi)
                            } )
     [:div.inputdata {:data-role "fieldcontain" } 
      [:label {:for (:id mi) :class "labelclass" } (get-label mi)  ]
      [:input {:type (get field-type (:data-type mi) "text")
               :name (:id mi)
               :id (:id mi)
               :class "inputclass"}]]))

(defn view 
  "Returns Consenter Information ( meta data ) form.
  Identity meta-data items used in the selected forms.

  :protocol-versions contains only the protocol versions
  that need to be filled out 

  Save transformed meta-data into :all-meta-data"
  [ctx]
  (let [selected-id (->> (session-get :protocol-versions)
                         (map :id)
                         (distinct))
        raw-meta (dsa/get-published-protocols-meta-items 
                   (vec selected-id) 
                   (session-get :selected-language))
        _ (debug "raw-meta lang: " (session-get :selected-language) " " 
                 (pprint-str raw-meta))
        meta-data (into {} (for [{id :id :as mi} raw-meta]
                            [(keyword id) mi])) ] 
        
    (session-put! :all-meta-data meta-data)
    (debug "INITIAL META DATA IS " (session-get :all-meta-data))
    (debug "INITIAL META DATA IS " (pprint-str (session-get :all-meta-data)))
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
    ;(println "meta data " (pprint-str answer-map))
    ;(println "meta data " (pprint-str (keys answer-map)))
    (helper/myredirect "/collect/consents")))

