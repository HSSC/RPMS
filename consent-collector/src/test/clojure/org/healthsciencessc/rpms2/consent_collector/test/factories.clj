(ns org.healthsciencessc.rpms2.consent-collector.test.factories)

(defn- id
  []
  (rand-int 100000000))

(defn user-with-roles-and-locations
  [& role-location-pairs]
  {:username "foobar"
   :title "Mr."
   :organizations {:id (id),
                   :name "Spartanburg"
                   :code "srhs"}
   :role-mappings (for [[role-name location-name] role-location-pairs]
                    {:role {:id (id) :name role-name :code "cc"}
                     :organization {:id (id) :name "Organization-SRHS" :code "srhs"}
                     :location {:id (id) :name location-name :code "reg"}})})

(defn user-with-locations
  [& locations]
  (apply user-with-roles-and-locations
    (for [loc locations] ["Consent Collector" loc])))
