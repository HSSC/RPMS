(ns org.healthsciencessc.rpms2.process-engine.web-service
  (:use compojure.core
        ring.middleware.session
        [ring.util.response :only (response content-type response?)]
        [clojure.data.json :only (read-json pprint-json)]
        [clojure.string :only (blank? join split)]
        [slingshot.slingshot :only (try+)]
        [clojure.tools.logging :only (info error)])
  (:require [compojure.handler :as handler]
            [org.healthsciencessc.rpms2.process-engine.core :as process]))

(defn uri->process-name
  [method uri]
  (let [split-uri (filter #(not (blank? %)) (split uri #"\/"))]
    (join "-" (conj split-uri method))))

(defn get-json-params
  [body]
  (let [body-str (slurp body)]
    (if (not (blank? body-str))
      (read-json body-str))))

(defn read-clojure
  [body]
  (let [body-str (slurp body)]
    (if (not (blank? body-str))
      (binding [*read-eval* false]
        (read-string body-str)))))

(defn keyify-params
  [params]
  (if (map? params)
    (into {}
          (for [[k v] params]
            [(keyword k) v]))))

;; The resolve-body function has used cond to allow for more resolutions to be added.
;; For example, to add a resolution for handling multipart requests with files.
(defn resolve-body
  "If the body is available to be resolved, it will be read. There are instances when
   the stream to the body has been closed and should not be read, such as when an HTML
   form submits data."
  [request]
  (let [content-type (or (:content-type request) "NA")
        data (:form-params request)
        body (:body request)]
    (cond
     (.startsWith content-type "application/json") (get-json-params body)
     (.startsWith content-type "application/clojure") (read-clojure body)
     (.startsWith content-type "application/x-www-form-urlencoded") (keyify-params data)
     (< 0 (count data)) (keyify-params data)
     :else {})))

(defn json-requested?
  [content-type]
  (.startsWith content-type "application/json"))

(defn clojure-requested?
  [content-type]
  (or (.startsWith content-type "text/clojure")
      (.startsWith content-type "application/clojure")))

(defn format-response-body
  [body request]
  (let [requested-content-type (or (:content-type request) "NA")]
    (cond
     (json-requested? requested-content-type)
     (if (response? body)
       (content-type (update-in body [:body] (fn [b] (with-out-str (pprint-json b)))) requested-content-type)
       (content-type (response (with-out-str (pprint-json body))) requested-content-type))
     (or (map? body) (clojure-requested? requested-content-type))
     (if (response? body)
       (content-type (update-in body [:body] (fn [b] (with-out-str (prn b)))) "application/clojure")
       (content-type (response (with-out-str (prn body))) "application/clojure"))
     :else body)))

(defn process-not-found-body
  [req process-name]
  (str "<h1>Process Not Found</h1>"
       (str "<h2>Process Name: " process-name "</h2>")
       "<h2>Request:</h2>"
       (str "<p>" req "</p>")))

(defroutes service-routes
  (ANY "*" {:keys [uri context path-info request-method query-params form-params session] :as req}
       (info "REQUEST: " req)
       (let [process-name (uri->process-name (name request-method) path-info)
             body-params (resolve-body req)
             params {:query-params (keyify-params query-params) :body-params body-params :session session :context context :path-info path-info}]
         (info "PROCESS NAME: " process-name)
         (try+
          (format-response-body (process/dispatch process-name params) req)
          (catch [:type :org.healthsciencessc.rpms2.process-engine.core/no-default-process] _
            (error (str "Could not find process " process-name))
            {:status 404 :body (process-not-found-body req process-name)})))))

(defn ws-constructor
  [& middlewares]
  (-> (reduce #(%2 %1) service-routes middlewares)
      wrap-session
      handler/api))
