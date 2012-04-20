(ns org.healthsciencessc.rpms2.consent-collector.test.helpers
  (:use clojure.test)
  (:use [slingshot.slingshot :only [try+ throw+]])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as pe]
            [org.healthsciencessc.rpms2.consent-collector.core :as cc]
            [sandbar.stateful-session]
            [net.cgrand.enlive-html :as en]
            [ring.adapter.jetty :as jetty]))

#_(alter-var-root #'report
                (fn [report]
                  (fn [{:keys [type expected] :as arg}]
                    (when (= :pass type)
                      (println "PASSED:"
                               (if (and (sequential? expected)
                                        (= 'testing (first expected)))
                                 (second expected)
                                 (pr-str expected))))
                    (report arg))))

(defn mock-session
  [session-map func]
  (binding [sandbar.stateful-session/sandbar-session (atom session-map)]
    (func)))

(defmacro with-session
  [m & body]
  `(mock-session ~m (fn [] ~@body)))

(defn setup-session-and-flash
  [func]
  (with-session {}
    (binding [sandbar.stateful-session/sandbar-flash (atom {:incoming {} :outgoing {}})]
      (func))))

(defn page-has?
  [html-string selector]
  (-> html-string
      (en/html-snippet)
      (en/select selector)
      (empty?)
      (not)))

(defn page-has-text?
  [html-string text]
  (page-has? html-string [(en/text-pred (partial re-find (re-pattern text)))]))
