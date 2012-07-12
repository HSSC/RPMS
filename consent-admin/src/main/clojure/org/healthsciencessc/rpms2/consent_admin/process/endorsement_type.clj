;; Provides the configuration of the protocol managemant UIs.
(ns org.healthsciencessc.rpms2.consent-admin.process.endorsement-type
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

(def fields [{:name :name :label "Name"}
             {:name :code :label "Code"}
             {:name :uri :label "URI"}])

(def type-name types/endorsement-type)
(def type-label "Endorsement Type")
(def type-path "endorsement/type")
(def type-kw (keyword type-name))

(defn view-endorsement-types
  [ctx]
  (let [org-id (common/lookup-organization ctx)
        nodes (services/get-endorsement-types org-id)
        endorsement-id (lookup/get-endorsement-in-query ctx)
        endorsement-type-id (lookup/get-endorsement-type-in-query ctx)]
    (if (meta nodes)
      (rutil/not-found (:message (meta nodes)))
      (layout/render ctx (str type-label "s")
                     (container/scrollbox 
                       (list/selectlist {:action :.detail-action}
                                              (for [n nodes]
                                                {:label (:name n) :data n})))
                     (actions/actions 
                       (if endorsement-id
                         (actions/ajax-action 
                           {:method :post :url (str "/api/" type-path "/assign") :label "Assign Type"
                            :params {:organization org-id :assign-type :selected#id 
                                     :endorsement endorsement-id type-kw endorsement-type-id}
                            :action-on-success ".back-action"
                            :verify {:action "selected" :message "Please select an Endorsement Type."}})
                         (list
                           (actions/details-action 
                             {:url (str "/view/" type-path) :params {:organization org-id type-kw :selected#id}
                              :verify (actions/gen-verify-a-selected "Endorsement Type")})
                           (actions/new-action 
                             {:url (str "/view/" type-path "/new") :params {:organization org-id}})))
                       (actions/back-action))))))

(defn view-endorsement-type
 [ctx]
  (if-let [node-id (lookup/get-endorsement-type-in-query ctx)]
    (let [n (services/get-endorsement-type node-id)
          editable (= (get-in n [:organization :id]) (security/current-org-id))]
      (if (meta n)
        (rutil/not-found (:message (meta n)))
        (layout/render ctx (str type-label ": " (:name n))
                       (container/scrollbox (form/dataform (form/render-fields {:editable editable} fields n)))
                       (actions/actions
                         (if editable
                           (list 
                             (actions/save-action 
                               {:url (str "/api/" type-path) :params {type-kw node-id}})
                             (actions/delete-action 
                               {:url (str "/api/" type-path) :params {type-kw node-id}})))
                         (actions/back-action)))))
    ;; Handle Error
    (layout/render-error ctx {:message "An endorsement type is required."})))

(defn view-endorsement-type-new
  "Generates a view that allows you to create a new protocol."
  [ctx]
  (let [org-id (common/lookup-organization ctx)]
    (layout/render ctx (str "Create " type-label)
                   (container/scrollbox (form/dataform (form/render-fields {} fields )))
                   (actions/actions 
                     (actions/create-action 
                       {:url (str "/api/" type-path) :params {:organization org-id}})
                     (actions/back-action)))))

(defn assign-endorsement-type
  [ctx]
  (let [endorsement-id (lookup/get-endorsement-in-query ctx)
        new-type-id (get-in ctx [:query-params :assign-type])
        endorsement-type-id (lookup/get-endorsement-type-in-query ctx)
        resp (services/assign-endorsement-type endorsement-id endorsement-type-id new-type-id)]
    (if (services/service-error? resp)
          (ajax/save-failed (meta resp))
          (ajax/success resp))))

(def process-defns
  [{:name (str "get-view-" type-name "s")
    :runnable-fn (runnable/gen-designer-org-check security/current-user common/lookup-organization)
    :run-fn view-endorsement-types
    :run-if-false ajax/forbidden}
   
   {:name (str "get-view-" type-name)
    :runnable-fn (runnable/gen-designer-org-check security/current-user common/lookup-organization)
    :run-fn view-endorsement-type
    :run-if-false ajax/forbidden}
   
   {:name (str "get-view-" type-name "-new")
    :runnable-fn (runnable/gen-designer-org-check security/current-user common/lookup-organization)
    :run-fn view-endorsement-type-new
    :run-if-false ajax/forbidden}
   
   {:name (str "put-api-" type-name)
    :runnable-fn (runnable/gen-designer-org-check security/current-user common/lookup-organization) ;; Service Will Catch Auth
    :run-fn (common/get-api-type-add 
              services/add-endorsement-type)
    :run-if-false ajax/forbidden}
   
   {:name (str "post-api-" type-name)
    :runnable-fn (runnable/gen-designer-org-check security/current-user common/lookup-organization) ;; Service Will Catch Auth
    :run-fn (common/gen-api-type-update 
              services/edit-endorsement-type 
              lookup/get-endorsement-type-in-query (str "A valid " type-label " is required."))
    :run-if-false ajax/forbidden}
   
   {:name (str "post-api-" type-name "-assign")
    :runnable-fn (runnable/gen-designer-org-check security/current-user common/lookup-organization) ;; Service Will Catch Auth
    :run-fn assign-endorsement-type
    :run-if-false ajax/forbidden}
   
   {:name (str "delete-api-" type-name)
    :runnable-fn (runnable/gen-designer-org-check security/current-user common/lookup-organization) ;; Service Will Catch Auth
    :run-fn (common/gen-api-type-delete 
              services/delete-endorsement-type 
              lookup/get-endorsement-type-in-query (str "A valid " type-label " is required."))
    :run-if-false ajax/forbidden}])

(process/register-processes (map #(DefaultProcess/create %) process-defns))
