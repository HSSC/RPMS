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


(defmacro is!
  "Like clojure.test/is, but short-circuits the test on failure."
  ([arg]
     `(let [x# ~arg]
        (is x#)
        (when-not x# (throw+ ::short-circuit))))
  ([arg doc]
     `(let [x# ~arg]
        (is x# ~doc)
        (when-not x# (throw+ ::short-circuit)))))

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

(defn mock-session
  [session-map func]
  (binding [sandbar.stateful-session/sandbar-session (atom session-map)]
    (func)))

(defmacro with-session
  [m & body]
  `(mock-session ~m (fn [] ~@body)))

(def ^:private
  test-server
  (atom nil))

(defn- wrap-path-info
  [app]
  (fn [req]
    (-> req (assoc :path-info (:uri req)) app)))

(defn start-test-server
  []
  (future (jetty/run-jetty
           (wrap-path-info cc/app)
           {:port 24646}))
  true)

(defn ensure-server-running
  ""
  []
  (swap! test-server
         (fn [v]
           (or v
               (start-test-server)))))

(defn run-rpms-test
  [test-fn]
  (let [old-defaults @pe/default-processes,
        old-customs @pe/custom-processes]
    (ensure-server-running)
    (try+
      (with-session {}
        (binding [sandbar.stateful-session/sandbar-flash (atom {:incoming {} :outgoing {}})]
          (test-fn)))
      (catch #{::short-circuit} _) ;; swallow short-circuit errors
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
