(ns org.healthsciencessc.rpms2.consent-collector.test.scenario-helpers
  (:use org.healthsciencessc.rpms2.consent-collector.test.helpers)
  (:import (com.gargoylesoftware.htmlunit WebClient BrowserVersion)))

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
      (is!))
  page)
