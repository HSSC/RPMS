(ns org.healthsciencessc.consent.collector.process.unlock-consent
  (:refer-clojure :exclude [root])
  (:require [org.healthsciencessc.consent.client.whoami :as whoami]
            [org.healthsciencessc.consent.collector.respond :as respond]
            [org.healthsciencessc.consent.collector.lock :as lock]
            [org.healthsciencessc.consent.collector.text :as text]
            [org.healthsciencessc.consent.collector.ui.action :as action]
            [org.healthsciencessc.consent.collector.ui.content :as cont]
            [org.healthsciencessc.consent.collector.ui.form :as form]
            [org.healthsciencessc.consent.collector.ui.layout :as layout]
            [pliant.webpoint.request :as endpoint])
  (:use     [pliant.process :only [defprocess as-method]]))


(def fields [{:name :lockcode :type :number :placeholder (text/text :unlock.consent.lockcode.label)}])

(def form-options {:method :post
                   :url "/api/unlock/consent"})

;; Register The Unlock Consent View
(defprocess view-unlock-consent
  "Creates a view to accept the lock code for unlocking the application."
  [ctx]
  (if (whoami/identified?)
    (layout/render-page ctx {:title (text/text :unlock.consent.title) :pageid "UnlockConsent"
                             :header-left "" :header-right ""}
                        (cont/paragraph (text/text :unlock.consent.message))
                        (form/dataform form-options 
                                       (form/render-fields {} fields {})
                                       (action/wrapper
                                         (action/form-submit {:label (text/text :action.unlock.label)}))))
    (respond/forbidden-view ctx)))

(as-method view-unlock-consent endpoint/endpoints "get-view-unlock-consent")


;; Register The Unlock Consent Service
(defprocess api-unlock-consent
  "Attempts to unlock the application and redirect to the review process."
  [ctx]
  (if (whoami/identified?)
    (let [lockcode (get-in ctx [:body-params :lockcode])]
      (if (lock/is-code? lockcode)
        (do 
          (lock/unlock)
          (respond/reset-view "/view/review/consent"))
        (respond/with-error ctx (text/format-text :unlock.consent.message.nomatch {:args [lockcode]}))))
    (respond/forbidden-api ctx)))

(as-method api-unlock-consent endpoint/endpoints "post-api-unlock-consent")

