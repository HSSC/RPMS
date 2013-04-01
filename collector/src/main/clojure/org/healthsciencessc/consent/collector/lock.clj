(ns org.healthsciencessc.consent.collector.lock
  (:require [org.healthsciencessc.consent.collector.respond :as respond]
            [pliant.webpoint.common :as common]
            [sandbar.stateful-session :as sandbar]))

;; Request Target Lock Control
(defn locker
  []
  (sandbar/session-get :locker))

(defn update-locker
  [updates]
  (sandbar/session-put! :locker (merge (locker) updates)))

(defn set-code
  [code]
  (update-locker {:code code}))

(defn get-code
  []
  (:code (locker)))

(defn is-code?
  [code]
  (= code (get-code)))

(defn lock
  [redirect-to & allowed-targets]
  (update-locker {:locked {:allowed (set (conj allowed-targets redirect-to)) :redirect redirect-to}}))

(defn locked?
  []
  (if (:locked (locker)) true false))

(defn unlock
  []
  (update-locker {:locked nil}))

(defn lock-handler
  [handler]
  (fn [request]
    (let [lock (:locked (locker))]
      (if (or (nil? lock) (contains? (:allowed lock) (common/path request)))
        (handler request)
        (handler (merge request {:path-info (:redirect lock) :uri (str (:context-path request) (:redirect lock))}))))))
