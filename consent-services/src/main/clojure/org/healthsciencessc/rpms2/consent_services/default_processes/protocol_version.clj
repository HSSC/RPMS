(ns org.healthsciencessc.rpms2.consent-services.default-processes.protocol-version
  (:use [org.healthsciencessc.rpms2.consent-services.domain-utils :only (forbidden-fn)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-domain.roles :as role])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(def protocol-version-processes
  [{:name "get-protocol-versions"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org (get-in user [:organization :id])
                         protocol (get-in params [:query-params :protocol])]
                     (and (role/protocol-designer? user) (data/belongs-to? "protocol" protocol "organization" user-org))))
    :run-fn (fn [params]
              (let [protocol (get-in params [:query-params :protocol])]
                (data/find-children "protocol" protocol "protocol-version")))
    :run-if-false forbidden-fn}

   {:name "get-protocol-version"
    :runnable-fn (fn [params]
                   (let [protocol-version-id (get-in params [:query-params :version])
                         user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])]
                     (data/belongs-to? "protocol-version" protocol-version-id "organization" user-org-id)))
    :run-fn (fn [params]
              (let [protocol-version-id (get-in params [:query-params :version])]
                (data/find-record "protocol-version" protocol-version-id)))
    :run-if-false forbidden-fn}

   {:name "put-protocol-version"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])
                         item-org-id (get-in params [:body-params :organization :id])]
                     (and (role/protocol-designer? user)
                          (= user-org-id item-org-id))))
    :run-fn (fn [params]
              (let [protocol-version (:body-params params)]
                (data/create "protocol-version" (merge protocol-version :status "Draft"))))
    :run-if-false forbidden-fn}

   {:name "post-protocol-version"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])
                         item-org-id (get-in params [:body-params :organization :id])
                         protocol-version (data/find-record "protocol-version" (get-in params [:query-params :version]))]
                     (and (role/protocol-designer? user)
                          (= user-org-id item-org-id)
                          (= "Draft" (:status protocol-version)))))
    :run-fn (fn [params]
              (let [protocol-version-id (get-in params [:query-params :version])
                    protocol-version (:body-params params)]
                (data/update "protocol-version" protocol-version-id protocol-version)))
    :run-if-false forbidden-fn}

   {:name "delete-protocol-version"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])
                         item-org-id (get-in params [:body-params :organization :id])]
                     (and (role/protocol-designer? user)
                          (= user-org-id item-org-id))))
    :run-fn (fn [params]
              (let [protocol-version-id (get-in params [:query-params :version])]
                (data/delete "protocol-version" protocol-version-id)))
    :run-if-false forbidden-fn}

   {:name "post-protocol-publish"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])
                         protocol-version-id (get-in params [:query-params :version])]
                     (and (role/protocol-designer? user)
                          (data/belongs-to? "protocol-version" protocol-version-id "organization" user-org-id))))
    :run-fn (fn [params]
              (let [protocol-version-id (get-in params [:query-params :version])
                    protocol-version (data/find-record "protocol-version" protocol-version-id)
                    protocol-id (get-in protocol-version [:protocol :id])
                    versions (data/find-children "protocol" protocol-id "protocol-version")
                    published-versions (filter #(= "Published" (:status %)) versions)]
                (do
                  (map (fn [version]
                         (data/update "protocol-version" (:id version) (assoc version :status "Retired")))
                       published-versions)
                  (data/update "protocol-version" protocol-version-id (assoc protocol-version :status "Published")))))
    :run-if-false forbidden-fn}

   {:name "post-protocol-publish"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org-id (get-in user [:organization :id])
                         protocol-version-id (get-in params [:query-params :version])]
                     (and (role/protocol-designer? user)
                          (data/belongs-to? "protocol-version" protocol-version-id "organization" user-org-id))))
    :run-fn (fn [params]
              (let [protocol-version-id (get-in params [:query-params :version])
                    protocol-version (data/find-record "protocol-version" protocol-version-id)]
                (data/update "protocol-version" protocol-version-id (assoc protocol-version :status "Retired"))))
    :run-if-false forbidden-fn}

   {:name "get-protocol-versions-published"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])
                         user-org (get-in user [:organization :id])
                         loc (get-in params [:query-params :loc])]
                     (and (role/protocol-designer? user) (data/belongs-to? "location" loc "organization" user-org))))
    :run-fn (fn [params]
              (let [loc (get-in params [:query-params :location])]
                (filter (partial = "Published") (data/find-children "location" loc "protocol"))))
    :run-if-false forbidden-fn}
   
   {:name "get-protocol-version-published-meta"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])]
                     (role/protocol-designer? user)))
    :run-fn (fn [params]
              (let [ids (get-in params [:query-params :version])]
                (distinct (if (coll? ids)
                            (map #(data/find-related-records "protocol-version" % (list "meta-item")) ids)
                            (data/find-related-records "protocol-version" ids (list "meta-item"))))))
    :run-if-false forbidden-fn}])

(process/register-processes (map #(DefaultProcess/create %) protocol-version-processes))