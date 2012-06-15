(ns org.healthsciencessc.rpms2.consent-collector.encounter
  (:require
    [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
    [org.healthsciencessc.rpms2.consent-collector.helpers :as helper]
    [org.healthsciencessc.rpms2.process-engine.core :as process])
  (:use [sandbar.stateful-session :only (session-get session-put! flash-get flash-put!)]
        [org.healthsciencessc.rpms2.consent-collector.debug :only (debug!)]
        [clojure.tools.logging :only (debug info warn error)]
        [org.healthsciencessc.rpms2.consent-collector.i18n :only (i18n)]
        [clojure.pprint])
  (:import java.util.Date
           java.text.DateFormat))

(defn chooser [encounters]
  [:select {:name "encounter"}
    (for [{:keys [id encounter-id date] :as enc} (sort-by :date encounters)]
      [:option {:value id :data-map (pr-str enc)}
        (format "Encounter %s on %s" encounter-id date)])])

(defn view
  [request]
  (session-put! :encounter nil)
  (let [encounters (:encounters (session-get :consenter))]
      (if (not (seq encounters))
        (helper/myredirect "/view/create/encounter")
        (helper/rpms2-page
          (helper/post-form "/view/select/encounter"
                      (list
                        [:div.left (i18n :select-encounter-form-section-label)]
                            (chooser encounters))
                            (helper/submit-btn {:value (i18n "select-encounter-view-select-encounter-button")
                                                :name "select-encounter"})
                            (helper/submit-btn { :value (i18n "select-encounter-view-create-encounter-button")
                                                :name "create-encounter"}))
          :title (i18n :hdr-select-encounter)
          :cancel-btn (helper/cancel-form "/view/select/consenter")))))

(defn set-encounter!
  [enc-id]
  (let [encounter (first (filter #(= enc-id (:id %))
                                 (:encounters (session-get :consenter))))]
    (session-put! :encounter encounter)))

(defn perform
  [request]
  (let [enc-id (-> request :body-params :encounter)
        create-button? (-> request :body-params :create-encounter)]
    (cond
      (not (empty create-button?))
      (helper/myredirect "/view/create/encounter")
      enc-id
      (do
        (set-encounter! enc-id)
        (helper/myredirect "/view/select/protocols"))
      :else
      (helper/myredirect "/view/select/encounter"))))

(defn current-date []
  (.format (DateFormat/getDateInstance DateFormat/SHORT)
           (Date.)))

(defn create-view [request]
  (helper/rpms2-page
    (helper/post-form "/view/create/encounter"
                      (list
                        [:div.left (i18n :create-encounter-form-section-label) ]
                        (for [[nm v] dsa/encounter-field-defs]
                          (list (helper/emit-field 
                                  (dissoc v :required :default-value)
                                  :create-encounter-form
                                  (name nm)))))
                      [:div.centered
                       (helper/submit-button "create-encounter-form"
                                             (i18n "create-encounter-form-submit-button")
                                             "create-encounter")])
    :title (i18n :hdr-create-encounter)
    :cancel-btn (helper/cancel-form "/view/select/consenter")))

(defn create-perform
  [{bparam :body-params}]
  (let [{:keys [date encounter-id] :as enc}
        (select-keys bparam [:date :encounter-id])] 
    (if (and date encounter-id)
      (do
        (session-put! :encounter (dsa/dsa-create-encounter enc))
        (helper/myredirect "/view/select/protocols"))
      (helper/myredirect "/view/select/consenter"))))
