(ns org.healthsciencessc.consent.collector.process.select-lockcode
  (:refer-clojure :exclude [root])
  (:require [org.healthsciencessc.consent.collector.respond :as respond]
            [org.healthsciencessc.consent.collector.lock :as lock]
            [org.healthsciencessc.consent.collector.text :as text]
            [org.healthsciencessc.consent.collector.process.authorize :as auth]
            [org.healthsciencessc.consent.collector.ui.action :as action]
            [org.healthsciencessc.consent.collector.ui.form :as form]
            [org.healthsciencessc.consent.collector.ui.layout :as layout]
            
            [org.healthsciencessc.rpms2.process-engine.endpoint :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]))


(def fields [{:name :lockcode :type :number :label (text/text :select.lockcode.label) :autofocus true}])

(def form-options {:method :post
                   :url "/api/select/lockcode"})

;; Register The Select Lockcode View
(defprocess view-select-lockcode
  "Creates a view of to set the lockcode"
  [ctx]
  (if (auth/is-authenticated?)
    (layout/render-page ctx {:title (text/text :select.lockcode.title) :pageid "SelectLockcode"} 
                   (form/dataform form-options 
                                  (form/render-fields {} fields {})
                                  (action/form-submit {:label (text/text :action.select.label)})))
    (respond/forbidden-view ctx)))

(as-method view-select-lockcode endpoint/endpoints "get-view-select-lockcode")


(defprocess valid-lockcode?
  [code]
  (and (= 4 (count code)) (every? #(Character/isDigit %) code)))

;; Register The Location Selection Service Process
(defprocess api-select-lockcode
  "Sets the lock code and redirects to the next view"
  [ctx]
  (if (auth/is-authenticated?)
    (if-let [lockcode (get-in ctx [:body-params :lockcode])]
      (if (valid-lockcode? lockcode)
          (do 
            (lock/set-code lockcode)
            (respond/reset-view "/view/search/consenter"))
          (respond/with-error (text/format-text :select.lockcode.message.notvalid {:args [lockcode]}))))
    (respond/forbidden-view ctx)))

(as-method api-select-lockcode endpoint/endpoints "post-api-select-lockcode")
