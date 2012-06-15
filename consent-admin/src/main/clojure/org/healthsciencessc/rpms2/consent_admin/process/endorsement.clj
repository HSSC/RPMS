;; Provides the configuration of the protocol managemant UIs.
(ns org.healthsciencessc.rpms2.consent-admin.process.endorsement
  (:require [org.healthsciencessc.rpms2.consent-admin.ajax :as ajax]
            [org.healthsciencessc.rpms2.consent-admin.security :as security]
            [org.healthsciencessc.rpms2.consent-admin.services :as services]
            [org.healthsciencessc.rpms2.consent-admin.process.common :as common]
            
            [org.healthsciencessc.rpms2.consent-admin.ui.actions :as actions]
            [org.healthsciencessc.rpms2.consent-admin.ui.container :as container]
            [org.healthsciencessc.rpms2.consent-admin.ui.form :as form]
            [org.healthsciencessc.rpms2.consent-admin.ui.layout :as layout]
            [org.healthsciencessc.rpms2.consent-admin.ui.selectlist :as selectlist]
            
            [org.healthsciencessc.rpms2.consent-domain.lookup :as lookup]
            [org.healthsciencessc.rpms2.consent-domain.runnable :as runnable]
            [org.healthsciencessc.rpms2.consent-domain.types :as types]
            
            [org.healthsciencessc.rpms2.process-engine.core :as process]
            [ring.util.response :as rutil])
  (:use [clojure.tools.logging :only (info error)])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(def fields [{:name :name :label "Name" :required true}
             {:name :code :label "Code"}
             {:name :endorsement-type :label "Type" :type :singleselect :required true :blank true :parser :id}])

(def type-name types/endorsement)
(def type-label "Endorsement")
(def type-path "endorsement")
(def type-kw (keyword type-name))

(defn- gen-endorsement-type-items
  [org-id]
  (let [types (services/get-endorsement-types org-id)
        items (map 
                (fn [t] {:label (:name t) 
                   :data (:id t) 
                   :item (select-keys t [:id])}) 
                types)]
    items))

(defn view-endorsements
  [ctx]
  (let [org-id (common/lookup-organization ctx)
        nodes (services/get-endorsements org-id)]
    (if (meta nodes)
      (rutil/not-found (:message (meta nodes)))
      (layout/render ctx (str type-label "s")
                     (container/scrollbox 
                       (selectlist/selectlist {:action :.detail-action}
                                              (for [n nodes]
                                                {:label (:name n) :data n})))
                     (actions/actions 
                       (actions/details-action 
                         {:url (str "/view/" type-path) :params {:organization org-id type-kw :selected#id}})
                       (actions/new-action 
                         {:url (str "/view/" type-path "/new") :params {:organization org-id}})
                       (actions/back-action))))))

(defn view-endorsement
 [ctx]
  (if-let [node-id (lookup/get-endorsement-in-query ctx)]
    (let [n (services/get-endorsement node-id)
          org-id (get-in n [:organization :id])
          endorsement-type (get-in n [:endorsement-type :id])]
      (if (meta n)
        (rutil/not-found (:message (meta n)))
        (layout/render ctx (str type-label ": " (:name n))
                       (container/scrollbox 
                         (form/dataform 
                           (form/render-fields 
                             {:fields {:endorsement-type {:readonly true
                                                          :items (gen-endorsement-type-items org-id)}}} fields n)))
                       (actions/actions
                         (actions/details-action 
                           {:url (str "/view/" type-path "/types") 
                            :params {:organization org-id :endorsement node-id :endorsement-type endorsement-type}
                            :label "Change Type"})
                         (actions/save-action 
                           {:url (str "/api/" type-path) :params {type-kw node-id}})
                         (actions/delete-action 
                           {:url (str "/api/" type-path) :params {type-kw node-id}})
                         (actions/back-action)))))
    ;; Handle Error
    (layout/render-error ctx {:message "An endorsement type is required."})))

(defn view-endorsement-new
  "Generates a view that allows you to create a new protocol."
  [ctx]
  (let [org-id (common/lookup-organization ctx)]
    (layout/render ctx (str "Create " type-label)
                   (container/scrollbox 
                     (form/dataform 
                       (form/render-fields 
                         {:fields {:endorsement-type {:items (gen-endorsement-type-items org-id)}}} fields {})))
                   (actions/actions 
                     (actions/create-action 
                       {:url (str "/api/" type-path) :params {:organization org-id}})
                     (actions/back-action)))))
    
(def process-defns
  [{:name (str "get-view-" type-name "s")
    :runnable-fn (runnable/gen-designer-org-check security/current-user common/lookup-organization)
    :run-fn view-endorsements
    :run-if-false ajax/forbidden}
   
   {:name (str "get-view-" type-name)
    :runnable-fn (runnable/gen-designer-org-check security/current-user common/lookup-organization)
    :run-fn view-endorsement
    :run-if-false ajax/forbidden}
   
   {:name (str "get-view-" type-name "-new")
    :runnable-fn (runnable/gen-designer-org-check security/current-user common/lookup-organization)
    :run-fn view-endorsement-new
    :run-if-false ajax/forbidden}
   
   {:name (str "put-api-" type-name)
    :runnable-fn (runnable/gen-designer-org-check security/current-user common/lookup-organization) ;; Service Will Catch Auth
    :run-fn (common/get-api-type-add 
              services/add-endorsement)
    :run-if-false ajax/forbidden}
   
   {:name (str "post-api-" type-name)
    :runnable-fn (runnable/gen-designer-org-check security/current-user common/lookup-organization) ;; Service Will Catch Auth
    :run-fn (common/gen-api-type-update 
              services/edit-endorsement 
              lookup/get-endorsement-in-query (str "A valid " type-label " is required."))
    :run-if-false ajax/forbidden}
   
   {:name (str "delete-api-" type-name)
    :runnable-fn (runnable/gen-designer-org-check security/current-user common/lookup-organization) ;; Service Will Catch Auth
    :run-fn (common/gen-api-type-delete 
              services/delete-endorsement 
              lookup/get-endorsement-in-query (str "A valid " type-label " is required."))
    :run-if-false ajax/forbidden}])

(process/register-processes (map #(DefaultProcess/create %) process-defns))
