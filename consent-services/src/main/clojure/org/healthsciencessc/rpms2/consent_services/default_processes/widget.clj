(ns org.healthsciencessc.rpms2.consent-services.default-processes.widget
  (:use [org.healthsciencessc.rpms2.consent-services.domain-utils :only (forbidden-fn)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-domain.roles :as role]
            [org.healthsciencessc.rpms2.consent-domain.runnable :as runnable]
            [org.healthsciencessc.rpms2.consent-domain.types :as types]
            [org.healthsciencessc.rpms2.consent-services.utils :as utils]
            [borneo.core :as neo])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(defn printit
  [title obj]
  (println)
  (println "BEGIN: " title)
  (println)
  (prn obj)
  (println)
  (println "END: " title)
  (println))

(defn post-designer-form
  [ctx]
  (let [body (:body-params ctx)
        form-id (get-in ctx [:query-params :form])
        form (get-in body[:update :form])]
    (neo/with-tx
      (if form (data/update types/form form-id form))
      (doseq [text (get-in body[:create :title])] 
        (let [node (data/create types/text-i18n text)]
          (data/relate-records types/text-i18n (:id node) types/form form-id)))
      (doseq [text (get-in body[:update :title])] 
        (data/update types/text-i18n (:id text) text))
      (doseq [text (get-in body[:delete :title])] 
        (data/delete types/text-i18n (:id text))))
    (dissoc (data/find-record types/form form-id) :contains)))

(defn put-designer-form-widget
  [ctx]
  (let [protocol-version (utils/get-protocol-version-record ctx)
        widget (:body-params ctx)
        widget-id (get-in ctx [:query-params :widget])
        form-id (get-in ctx [:query-params :form])
        widget1 (if widget-id (merge widget {:contained-in {:id widget-id}})
                 (merge widget {:form {:id form-id}}))
        widget2 (assoc widget1 :organization (:organization protocol-version))]
    (data/create-records types/widget widget2)))

(defn post-designer-form-widget
  [ctx]
  (let [body (:body-params ctx)
        widget-id (get-in ctx [:query-params :widget])
        widget (data/find-record types/widget widget-id)
        record (get-in body[:update :widget])]
    (neo/with-tx
      (data/update types/widget widget-id record)
      (doseq [prop (get-in body[:create :property])] 
        (let [prop1 (assoc prop :organization (:organization widget))
              prop2 (assoc prop1 :widget widget)]
              (data/create types/widget-property prop2)))
      (doseq [prop (get-in body[:update :property])] 
        (data/update types/widget-property (:id prop) prop))
      (doseq [prop (get-in body[:delete :property])] 
        (data/delete types/widget-property (:id prop))))
    (dissoc (data/find-record types/widget widget-id) :contains)))

(defn delete-designer-form-widget
  [ctx]
  (let [body (:body-params ctx)
        widget-id (get-in ctx [:query-params :widget])]
    (data/delete types/widget widget-id)))

(defn trueprint
  [ctx]
  (printit "QUERY PARAMS" (:query-params ctx))
  (printit "BODY PARAMS" (:body-params ctx))
  true)

(def widget-processes
  [{:name "get-library-widgets"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])]
                     (role/protocol-designer? user)))
    :run-fn (fn [params]
              (let [user (get-in params [:session :current-user])
                    user-org-id (get-in user [:organization :id])]
                (data/find-children "organization" user-org-id "widget")))
    :run-if-false forbidden-fn}

{:name "get-library-widgets-root"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])]
                     (role/protocol-designer? user)))
    :run-fn (fn [params]
              (let [user (get-in params [:session :current-user])
                    user-org-id (get-in user [:organization :id])
                    widgets (data/find-children "organization" user-org-id "widget")]
                (map (fn [r] (dissoc r :contains)) (filter #(nil? (:contained-in %)) widgets))))
    :run-if-false forbidden-fn}   

   {:name "get-library-widget"
    :runnable-fn (fn [params]
                   (let [widget-id (get-in params [:query-params :widget])
                         user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])]
                     (and (role/protocol-designer? user)
                          (data/belongs-to? "widget" widget-id "organization" user-org-id))))
    :run-fn (fn [params]
              (let [widget-id (get-in params [:query-params :widget])]
                (data/find-record "widget" widget-id)))
    :run-if-false forbidden-fn}

   {:name "put-library-widget"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])
                         item-org-id (get-in params [:body-params :organization :id])]
                     (and (role/protocol-designer? user)
                          (= user-org-id item-org-id))))
    :run-fn (fn [params]
              (let [widget (:body-params params)]
                (data/create "widget" widget)))
    :run-if-false forbidden-fn}

   {:name "put-library-widgets"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])
                         item-org-id (get-in params [:body-params :organization :id])]
                     (and (role/protocol-designer? user)
                          (= user-org-id item-org-id))))
    :run-fn (fn [params]
              (let [widgets (:body-params params)]
                (data/create-records "widget" widgets)))
    :run-if-false forbidden-fn}

   {:name "post-library-widget"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])
                         item-org-id (get-in params [:body-params :organization :id])]
                     (and (role/protocol-designer? user)
                          (= user-org-id item-org-id))))
    :run-fn (fn [params]
              (let [widget-id (get-in params [:query-params :widget])
                    widget (:body-params params)]
                (data/update "widget" widget-id widget)))
    :run-if-false forbidden-fn}

   {:name "delete-library-widget"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])
                         item-org-id (get-in params [:body-params :organization :id])]
                     (and (role/protocol-designer? user)
                          (= user-org-id item-org-id))))
    :run-fn (fn [params]
              (let [widget-id (get-in params [:query-params :widget])]
                (data/delete "widget" widget-id)))
    :run-if-false forbidden-fn}
   
   ;; Services specifically to adhere to the quirks of the designer with the widget/widget-property relations
   {:name "post-designer-form"
    :runnable-fn trueprint ;;(runnable/can-design-protocol-version utils/current-user utils/get-protocol-version-record)
    :run-fn post-designer-form
    :run-if-false forbidden-fn}

   {:name "put-designer-form-widget"
    :runnable-fn trueprint ;;(runnable/can-design-protocol-version utils/current-user utils/get-protocol-version-record)
    :run-fn put-designer-form-widget
    :run-if-false forbidden-fn}

   ;; TODO - Add a owns data method to search throught multiple levels of widgets.
   {:name "post-designer-form-widget"
    :runnable-fn trueprint ;;(runnable/can-design-protocol-version utils/current-user utils/get-protocol-version-record)
    :run-fn post-designer-form-widget
    :run-if-false forbidden-fn}

   {:name "delete-designer-form-widget"
    :runnable-fn trueprint ;;(runnable/can-design-protocol-version utils/current-user utils/get-protocol-version-record)
    :run-fn delete-designer-form-widget
    :run-if-false forbidden-fn}
   ])

(process/register-processes (map #(DefaultProcess/create %) widget-processes))