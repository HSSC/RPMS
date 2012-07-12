;; Provides the configuration of the protocol managemant UIs.
(ns org.healthsciencessc.rpms2.consent-admin.process.meta-item
  (:require [org.healthsciencessc.rpms2.consent-admin.ajax :as ajax]
            [org.healthsciencessc.rpms2.consent-admin.security :as security]
            [org.healthsciencessc.rpms2.consent-admin.services :as services]
            [org.healthsciencessc.rpms2.consent-admin.process.common :as common]
            
            [org.healthsciencessc.rpms2.consent-admin.ui.actions :as actions]
            [org.healthsciencessc.rpms2.consent-admin.ui.container :as container]
            [org.healthsciencessc.rpms2.consent-admin.ui.form :as form]
            [org.healthsciencessc.rpms2.consent-admin.ui.layout :as layout]
            [org.healthsciencessc.rpms2.consent-admin.ui.list :as list]
            
            [org.healthsciencessc.rpms2.consent-domain.lookup :as lookup]
            [org.healthsciencessc.rpms2.consent-domain.runnable :as runnable]
            [org.healthsciencessc.rpms2.consent-domain.types :as types]
            
            [org.healthsciencessc.rpms2.process-engine.core :as process]
            [ring.util.response :as rutil])
  (:use [clojure.tools.logging :only (info error)])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(def fields [{:name :name :label "Name" :required true}
             {:name :description :label "Description"}
             {:name :data-type :label "Data Type" :type :singleselect :items [{:label "String" :data "string"}
                                                                               {:label "Date" :data "date"}
                                                                               {:label "Number" :data "number"}
                                                                               {:label "Boolean" :data "boolean"}]}
             ;;{:name :choice-values :label "Choice Values"}
             {:name :default-value :label "Default Value"}
             {:name :labels :label "Labels" :type :i18ntext}])

(def type-name types/meta-item)
(def type-label "Meta Item")
(def type-path "meta-item")
(def type-kw (keyword type-name))

(defn view-meta-items
  [ctx]
  (let [org-id (common/lookup-organization ctx)
        nodes (services/get-meta-items)
        protocol-version-id (lookup/get-protocol-version-in-query ctx)
        prot-props (if protocol-version-id {:protocol-version protocol-version-id} {})
        params (merge {:organization org-id} prot-props)]
    (if (meta nodes)
      (rutil/not-found (:message (meta nodes)))
      (layout/render ctx (str type-label "s")
                     (container/scrollbox 
                       (list/selectlist {:action :.detail-action}
                                              (for [n nodes]
                                                {:label (:name n) :data (select-keys n [:id])})))
                     (actions/actions 
                       (if protocol-version-id
                         (actions/assign-action 
                           {:url (str "/api/" type-path "/assign") 
                            :params (merge params {type-kw :selected#id})
                            :verify (actions/gen-verify-a-selected "Meta Item")}))
                       (actions/details-action 
                         {:url (str "/view/" type-path) 
                          :params (merge params {type-kw :selected#id})
                          :verify (actions/gen-verify-a-selected "Meta Item")})
                       (actions/new-action 
                         {:url (str "/view/" type-path "/new") :params {:organization org-id}})
                       (actions/back-action))))))

(defn view-meta-item
 [ctx]
  (if-let [node-id (lookup/get-meta-item-in-query ctx)]
    (let [n (services/get-meta-item node-id)
          editable (common/owned-by-user-org n)
          langs (services/get-languages)]
      (if (meta n)
        (rutil/not-found (:message (meta n)))
        (layout/render ctx (str type-label ": " (:name n))
                       (container/scrollbox 
                         (form/dataform 
                           (form/render-fields {:editable editable
                                                :fields {:labels {:languages langs 
                                                                  :default-language (get-in n [:organization :language])
                                                                  :url "/api/text/i18n"
                                                                  :params {:parent-id node-id
                                                                           :parent-type type-name
                                                                           :property :labels}}}} fields n)))
                       (actions/actions
                         (if editable
                           (list
                             (actions/save-action 
                               {:url (str "/api/" type-path) :params {type-kw node-id}})
                             (actions/delete-action 
                               {:url (str "/api/" type-path) :params {type-kw node-id}})))
                         (actions/back-action)))))
    ;; Handle Error
    (layout/render-error ctx {:message "An meta-item type is required."})))

(defn view-meta-item-new
  "Generates a view that allows you to create a new protocol."
  [ctx]
  (let [org-id (common/lookup-organization ctx)
        org (services/get-organization org-id)
        langs (services/get-languages)]
    (layout/render ctx (str "Create " type-label)
                   (container/scrollbox 
                     (form/dataform 
                       (form/render-fields {:fields 
                                            {:labels {:languages langs
                                                      :default-language (:language org)}}} fields {})))
                   (actions/actions 
                     (actions/create-action 
                       {:url (str "/api/" type-path) :params {:organization org-id}})
                     (actions/back-action)))))


(defn- api-assign-meta-item
  [ctx]
  (let [meta-item-id (lookup/get-meta-item-in-query ctx)
        protocol-version-id (lookup/get-protocol-version-in-query ctx)
        resp (services/assign-meta-item-to-protocol-version meta-item-id protocol-version-id)]
      (if (services/service-error? resp)
        (ajax/save-failed (meta resp))
        (ajax/success resp))))

(def process-defns
  [{:name (str "get-view-" type-name "s")
    :runnable-fn (runnable/gen-designer-org-check security/current-user common/lookup-organization)
    :run-fn view-meta-items
    :run-if-false ajax/forbidden}
   
   {:name (str "get-view-protocol-version-" type-name "-add")
    :runnable-fn (runnable/gen-designer-org-check security/current-user common/lookup-organization)
    :run-fn view-meta-items
    :run-if-false ajax/forbidden}
   
   {:name (str "get-view-" type-name)
    :runnable-fn (runnable/gen-designer-org-check security/current-user common/lookup-organization)
    :run-fn view-meta-item
    :run-if-false ajax/forbidden}
   
   {:name (str "get-view-" type-name "-new")
    :runnable-fn (runnable/gen-designer-org-check security/current-user common/lookup-organization)
    :run-fn view-meta-item-new
    :run-if-false ajax/forbidden}
   
   {:name (str "put-api-" type-name)
    :runnable-fn (runnable/gen-designer-org-check security/current-user common/lookup-organization) ;; Service Will Catch Auth
    :run-fn (common/get-api-type-add 
              services/add-meta-item)
    :run-if-false ajax/forbidden}
   
   {:name (str "post-api-" type-name)
    :runnable-fn (runnable/gen-designer-org-check security/current-user common/lookup-organization) ;; Service Will Catch Auth
    :run-fn (common/gen-api-type-update 
              services/edit-meta-item 
              lookup/get-meta-item-in-query (str "A valid " type-label " is required."))
    :run-if-false ajax/forbidden}
   
   {:name (str "delete-api-" type-name)
    :runnable-fn (runnable/gen-designer-org-check security/current-user common/lookup-organization) ;; Service Will Catch Auth
    :run-fn (common/gen-api-type-delete 
              services/delete-meta-item 
              lookup/get-meta-item-in-query (str "A valid " type-label " is required."))
    :run-if-false ajax/forbidden}
   
   {:name (str "post-api-" type-name "-assign")
    :runnable-fn (runnable/gen-designer-org-check security/current-user common/lookup-organization) ;; Service Will Catch Auth
    :run-fn api-assign-meta-item
    :run-if-false ajax/forbidden}])

(process/register-processes (map #(DefaultProcess/create %) process-defns))
