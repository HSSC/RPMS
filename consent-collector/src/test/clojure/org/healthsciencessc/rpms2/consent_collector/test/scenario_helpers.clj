(ns org.healthsciencessc.rpms2.consent-collector.test.scenario-helpers
  (:use org.healthsciencessc.rpms2.consent-collector.test.helpers)
  (:import (com.gargoylesoftware.htmlunit WebClient BrowserVersion))
  (:use [slingshot.slingshot :only [try+ throw+]])
  (:use clojure.test)
  (:require [org.healthsciencessc.rpms2.process-engine.core :as pe]
            [org.healthsciencessc.rpms2.consent-collector.core :as cc]
            [sandbar.stateful-session]
            [net.cgrand.enlive-html :as en]
            [ring.adapter.jetty :as jetty]))

(defmacro is!
  "Like clojure.test/is, but short-circuits the test on failure.
  Works with the def-rpms-test macro."
  ([arg]
     `(let [x# ~arg]
        (is x#)
        (when-not x# (throw+ ::short-circuit))))
  ([arg doc]
     `(let [x# ~arg]
        (is x# ~doc)
        (when-not x# (throw+ ::short-circuit)))))

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

(defn setup-scenarios
  [func]
  (ensure-server-running)
  (try+ (func)
        (catch #{::short-circut} _))) ;; swallow short-circuit errors

(defn fill-out-form
  [form att-val-map]
  (doseq [[att-name val] att-val-map]
    (-> form
        (.getInputByName att-name)
        (.setValueAttribute val)))
  form)

(defn submit-form
  [form]
  (-> form
      (.getElementsByAttribute "input" "type" "submit")
      first
      (.click)))

(defn fill-out-first-form
  [page att-val-map]
  (-> page
      .getForms
      first
      (fill-out-form att-val-map)))

(defn fill-out-and-submit-first-form
  [page att-val-map]
  (-> page
      .getForms
      first
      (fill-out-form att-val-map)
      (submit-form)))

(defn- make-client
  []
  (new WebClient BrowserVersion/FIREFOX_3_6))

(defn run-with-client
  [f url]
  (let [client (make-client)
        page (.getPage client url)]
    ;; (.setJavaScriptEnabled client false) ;; Ignore javascript
    ;; (.setCssEnabled client false) ;; Ignore css
    (try (f page)
         (finally
          (.closeAllWindows client)))))

(defmacro with-client
  [page-name page-url & body]
  `(run-with-client (fn [~page-name] ~@body) ~page-url))

(defn url
  [& args]
  (apply str "http://localhost:24646" args))

(defn should-be-on-page
  [page path]
  (-> page
      .getUrl
      str
      (.endsWith (url path))
      (is! (format "expecting to be on %s (actually on %s)" path (-> page .getUrl str))))
  page)

(defn spit-page
  [page file]
  (.save page (java.io.File. file))
  page)
