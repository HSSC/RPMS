(ns org.healthsciencessc.consent.collector.lock
  (:require [org.healthsciencessc.consent.collector.respond :as respond]
            [pliant.webpoint.common :as common]
            [org.healthsciencessc.consent.client.session :as sess]))


(defn set-code
  "Sets a code used for locking the application into the session."
  [code]
  (sess/assoc-in-session! [:locker :code] code))

(defn get-code
  "Gets the code used for locking the application from the session."
  []
  (sess/get-in-session :locker :code))

(defn is-code?
  "Checks if the code provided matches the code in session."
  [code]
  (= code (get-code)))

(defn lock
  "Locks the application to only accept"
  [redirect-to & allowed-targets]
  (sess/assoc-in-session! [:locker :locked :allowed] (set (conj allowed-targets redirect-to)))
  (sess/assoc-in-session! [:locker :locked :redirect] redirect-to))

(defn locked?
  []
  (if (sess/get-in-session :locker :locked) true false))

(defn unlock
  []
  (sess/assoc-in-session! [:locker :locked] nil))

(defn lock-handler
  [handler]
  (fn [request]
    (let [lock (sess/get-in-session :locker :locked)]
      (if (or (nil? lock) (contains? (:allowed lock) (common/path request)))
        (handler request)
        (handler (merge request {:path-info (:redirect lock) :uri (str (:context-path request) (:redirect lock))}))))))
