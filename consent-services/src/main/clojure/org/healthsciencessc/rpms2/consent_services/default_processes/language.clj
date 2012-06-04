(ns org.healthsciencessc.rpms2.consent-services.default-processes.language
  (:use [org.healthsciencessc.rpms2.consent-services.domain-utils :only (forbidden-fn)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-domain.roles :as role])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))


(def language-processes
  [{:name "get-library-languages"
    :runnable-fn (fn [params]
                   (let [user (get-in params [:session :current-user])]
                     (or (role/consent-collector? user) (role/protocol-designer? user))))
    :run-fn (fn [params]
              (let [user (get-in params [:session :current-user])
                    user-org-id (get-in user [:organization :id])]
                (data/find-children "organization" user-org-id "language")))
    :run-if-false forbidden-fn}])

(process/register-processes (map #(DefaultProcess/create %) language-processes))