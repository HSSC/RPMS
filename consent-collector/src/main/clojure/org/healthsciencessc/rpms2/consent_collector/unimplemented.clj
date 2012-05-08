(ns org.healthsciencessc.rpms2.consent-collector.unimplemented
  (:require [org.healthsciencessc.rpms2.consent-collector.helpers :as helper])
  (:use [clojure.tools.logging :only (debug info warn error)])
  (:use [clojure.pprint])
  (:use [org.healthsciencessc.rpms2.consent-collector.i18n :only [i18n]]))

(defn view 
   "Displays unimplemented message"
  [ctx]

  (helper/rpms2-page "Unimplemented" :title "Feature is not available."))
