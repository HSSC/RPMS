(ns ^{:doc "Collect consents - collects information from forms." }
  org.healthsciencessc.rpms2.consent-collector.witness-consents
  (:require
   [org.healthsciencessc.rpms2.consent-collector.dsa-client :as dsa]
   [org.healthsciencessc.rpms2.consent-collector.mock :as mock]
   [org.healthsciencessc.rpms2.consent-collector.formutil :as formutil]
   [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [sandbar.stateful-session :only [session-get session-put! flash-get flash-put! ]])
  (:use [clojure.tools.logging :only (debug info warn error)])
  (:use [clojure.pprint :only (pprint)])
  (:use [clojure.data.json :only (json-str)])
  (:use [org.healthsciencessc.rpms2.consent-collector.debug :only [debug! pprint-str]])
  (:use [org.healthsciencessc.rpms2.consent-collector.config :only [config]])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))

(defn- my-signature
  [widget] 

  [:div.control.signature 
   (:name widget)
  [:div.sigpad-control
      [:div.sigPad  ; sigPad must be on div which directly contains sigNav
      [:ul.sigNav 
         #_[:li.clearButton [:a {:href "#clear"} "Clear" ] ] ; use an onclick event here
         [:li (helper/submit-btn { :value (:clear-label widget)
                                   :name (str "signature-btn-" (:name widget)) }) ] 
      ] 
      [:div {:class "sig sigWrapper" }
        [:div.typed ] 
          [:canvas {:class "pad" :width "198" :height "55" }  ]
          [:input {:type "hidden" 
                   :name (:name widget) 
                   :class "output" 
                   } ]
      ]]]
   ])

(defn view 
  "Returns witness consent form"
  [ctx]

  (helper/rpms2-page 
     (helper/post-form "/view/unimplemented" 
        (list [:h1 "Witness Consent" ]
              (my-signature {:name "NAME" :clear-label "Clear-The-Button" } ))
        (helper/submit-btn { :value "Submit" } ))
   :title "Witness Consent Form" ))

(defn perform
  "Collect consents."

  [{parms :body-params :as ctx}]

  (helper/myredirect "/view/unimplemented"))
