(ns org.healthsciencessc.rpms2.consent-services.seed-protocol
  (:require [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-domain.tenancy :as tenancy]
            [org.healthsciencessc.rpms2.consent-domain.types :as types]))

(defn- get-languages
  "Creates the languages that are available from the base organization if they do not exist."
  []
  (let [langs (tenancy/only-base-org (data/find-all types/language))]
    (filter #(or (= "en" (:code %)) (= "es" (:code %))) langs)))

(defn- get-endorsements 
  "Creates the languages that are available from the base organization if they do not exist."
  [org langs]
  (let [types (tenancy/only-base-org (data/find-all types/endorsement-type))]
    (map (fn [t]
           (let [labels (for [lang langs] {:value (str (:name t) " in " (:name lang)) :language lang})
                 endorsement {:name (:name t) :endorsement-type t :organization org :labels labels}]
           (data/create types/endorsement endorsement))) types)))

(defn- get-meta-items 
  []
  (tenancy/only-base-org (data/find-all types/meta-item)))

(defn- get-policies 
  [org langs]
  (let [defs (tenancy/only-base-org (data/find-all types/policy-definition))]
    (map (fn [d]
           (let [titles (for [lang langs] {:value (str (:name d) " in " (:name lang)) :language lang})
                 texts (for [lang langs] {:value (str (:description d) " in " (:name lang)) :language lang})
                 policy {:name (:name d) :policy-definition d :organization org :titles titles :texts texts}]
           (data/create types/policy policy))) defs)))

(defn- get-locations
  [org]
  (let [locations (data/find-children types/organization (:id org) types/location)
        less (map (fn [loc]
                    (let [code (str (:code loc) "-sample-protocol")
                          prots (data/find-children types/location (:id loc) types/protocol)]
                      (if (some #(= code (:code %)) prots)
                        nil 
                        loc))) locations)]
    (filter identity less)))

(def collects (atom 0))
(def reviews (atom 0))

(defn next-name
  [operation]
  (if (= "collect" operation)
    (str "collect" (swap! collects inc))
    (str "review" (swap! reviews inc))))

(defn gen-sections-review
  [org page operation policy endorsement langs]
  (let [pid (:id policy)
        eid (:id endorsement)
        labels (for [lang langs] {:key "label" :language lang :value (str "Edit " (:code lang)) })
        cprops [{:key "returnpage" :value (:name page) }
                {:key "operation" :value operation }]
        pprops [{:key "policy" :value pid }]
        eprops [{:key "endorsement" :value eid }]
        pwid {:name (next-name operation) :type "review-policy" :organization org :properties (concat pprops cprops labels) }
        ewid {:name (next-name operation) :type "review-endorsement" :organization org :properties (concat eprops cprops labels)}
        swid {:name (next-name operation) :type "section" :organization org :contains [pwid ewid]}]
    [swid]))

(defn gen-sections-collect
  [org page operation policy endorsement langs]
  (let [pid (:id policy)
        eid (:id endorsement)
        clear (for [lang langs] {:key "clear-label" :language lang :value (str "Clear " (:code lang)) })
        agree (for [lang langs] {:key "true-label" :language lang :value (str "Agree " (:code lang)) })
        noagree (for [lang langs] {:key "false-label" :language lang :value (str "No Agree " (:code lang)) })
        cprops [{:key "operation" :value operation }]
        pprops [{:key "policy" :value pid }
                {:key "render-title" :value true }
                {:key "render-text" :value true }
                {:key "render-media" :value false }]
        bprops [{:key "policy" :value [pid] }]
        eprops [{:key "endorsement" :value eid }]
        
        pwid {:name (next-name operation) :type "policy-text" :organization org :properties (concat pprops cprops)}
        bwid {:name (next-name operation) :type "policy-choice-buttons" :organization org :properties (concat bprops cprops agree noagree)}
        ewid {:name (next-name operation) :type "signature" :organization org :properties (concat eprops cprops clear)}
        swid1 {:name (next-name operation) :type "section" :organization org :contains [pwid bwid]}
        swid2 {:name (next-name operation) :type "section" :organization org :contains [ewid]}]
    [swid1 swid2]))

(defn gen-page
  [org form operation policy endorsement langs sectionfn i total]
  (let [props [{:key "operation" :value operation }]
        pprops (if (> i 0) [{:key "previous" :value (str operation (dec i))}] [])
        nprops (if (< i (dec total)) [{:key "next" :value (str operation (inc i))}] [])
        name (str operation i)
        page {:name name :type "page" :organization org}
        sections (sectionfn org page operation policy endorsement langs)
        page (merge page {:contains sections :properties (concat props pprops nprops)})
        record (data/create-records types/widget page)]
    (data/relate-records types/widget (:id record) types/form (:id form))))

(defn make-form
  [org form policies endorsements meta-items langs]
  (let [total (count endorsements)
        po-pos (take total policies)]
    (doseq [[policy endorsement i] (map vector po-pos endorsements (range))]
      (gen-page org form "collect" policy endorsement langs gen-sections-collect i total))
    (doseq [[policy endorsement i] (map vector po-pos endorsements (range))]
      (gen-page org form "review" policy endorsement langs gen-sections-review i total))))

(defn add-protocols
  "Creates an organization that will contain all of the example and best practice data."
  [org]
  (let [locations (get-locations org)
        langs (get-languages)
        meta-items (get-meta-items)
        endorsements (get-endorsements org langs)
        policies (get-policies org langs)]
    (doseq [location locations]
      (let [code (str (:code location) "-sample-protocol")
            protocol (data/create types/protocol {:name (str "Protocol For " (:name location)) :code code :location location :organization org})
            titles (for [lang langs] {:value (str "Protocol For " (:name location) " in " (:name lang)) :language lang})
            form {:name (:name protocol) :collect-start "collect0" :review-start "review0" :titles titles}
            version (data/create types/protocol-version {:version "1" :status types/status-draft :protocol protocol 
                                                         :organization org :form form})
            form (:form version)]
        (doseq [lang langs] 
          (data/relate-records types/protocol-version (:id version) types/language (:id lang)))
        (doseq [meta-item meta-items] 
          (data/relate-records types/protocol-version (:id version) types/meta-item (:id meta-item)))
        (doseq [endorsment endorsements] 
          (data/relate-records types/protocol-version (:id version) types/endorsement (:id endorsment)))
        (doseq [policy policies] 
          (data/relate-records types/protocol-version (:id version) types/policy (:id policy)))
        (make-form org form policies endorsements meta-items langs)
        ))))
 