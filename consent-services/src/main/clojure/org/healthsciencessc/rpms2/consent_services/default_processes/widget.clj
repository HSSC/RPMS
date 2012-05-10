(ns org.healthsciencessc.rpms2.consent-services.default-processes.widget
  (:use [org.healthsciencessc.rpms2.consent-services.domain-utils :only (forbidden-fn)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-domain.roles :as role])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

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
    :run-if-false forbidden-fn}])

(process/register-processes (map #(DefaultProcess/create %) widget-processes))