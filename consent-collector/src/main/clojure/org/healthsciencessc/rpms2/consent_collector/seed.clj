(ns org.healthsciencessc.rpms2.consent-collector.seed
  (:require [clojure.string :as s]
            [clj-http.client :as http])
  (:import org.apache.http.auth.MalformedChallengeException
           org.apache.http.client.ClientProtocolException)
  (:use [org.healthsciencessc.rpms2.consent-collector  [factories :as factory]
                                                       [config :only (config)]
                                                       [debug :only (debug!)]
                                                       [dsa-client :as dsa]
                                                       [fake-dsa-client :as fake]]
        [clojure.tools.logging :only (debug info error warn)]
        [clojure.data.json :only (read-json json-str)]))


(defn add-test-data
  "Add test users."
  [ctx]
  ;;create organizations
  (println "Adding test data....")
  ;(dsa-call :authentiate {:name "MUSC" :code "musc-org"} )
  ;
  (try
  (let [
        o (dsa-call :put-security-organization {:name "MUSC" :code "musc-org"} )
        generated-id (get-in o [:json :id])
        b (dsa-call :post-security-organization {:id generated-id :name "MUSC" :code "musc-org"} )
        ]
    (println (str "\n\nCreated org: " o \n" ID = " (get-in o [:json :id]) #_(:id o)))
    (println (str "B IS " b))
    )
  
  ;(dsa-call :put-security-organization {:name "SRHS" :code "srhs-org"} )
  ;(dsa-call :put-security-organization {:name "GHS" :code "ghs-org"} )
  ;(dsa-call :put-security-organization {:name "PH" :code "ph-org"} )
  ;;(dsa-call :get-security-authenticate {})
    (catch Exception e (println (str "seed 30 EXCEPTION " e))))
  (pr-str "add-test-data returns")
) 

(debug! add-test-data)
(debug! dsa-call)

