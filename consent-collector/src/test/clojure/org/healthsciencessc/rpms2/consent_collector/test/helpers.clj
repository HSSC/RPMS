(ns org.healthsciencessc.rpms2.consent-collector.test.helpers
  (:use clojure.test)
  (:require [org.healthsciencessc.rpms2.process-engine.core :as pe]
            [sandbar.stateful-session]))

(defn mock-session
  [session-map func]
  (binding [sandbar.stateful-session/sandbar-session (atom session-map)]
    (func)))

(defmacro with-session
  [m & body]
  `(mock-session ~m (fn [] ~@body)))

(defn run-rpms-test
  [test-fn]
  (let [old-defaults @pe/default-processes,
        old-customs @pe/custom-processes]
    (try
      (with-session {}
        (binding [sandbar.stateful-session/sandbar-flash (atom {:incoming {} :outgoing {}})]
          (test-fn)))
      (finally
       (reset! pe/default-processes old-defaults)
       (reset! pe/custom-processes old-customs)))))

(defmacro def-rpms-test
  "Sets up stuff, and also allows adding a docstring at the
  top of the test."
  [test-name maybe-desc & body]
  (let [desc (if (string? maybe-desc) maybe-desc),
        body (if desc body (cons maybe-desc body)),
        body-form `(run-rpms-test (fn [] ~@body))]
    (if desc
      `(deftest ~test-name (testing ~desc ~body-form))
      `(deftest ~test-name ~body-form))))

(defn make-spy
  [returns]
  (let [arg-store (atom [])]
    (with-meta
      (fn [& args]
        (swap! arg-store conj args)
        returns)
      {:args arg-store})))

(defmacro with-spy
  [var-name & body]
  `(with-redefs [~var-name (make-spy nil)]
     ~@body))

(defmacro with-spy-and-return
  [var-name return & body]
  `(with-redefs [~var-name (make-spy ~return)]
     ~@body))

(defn last-call-args
  [f]
  (-> f meta :args deref last))