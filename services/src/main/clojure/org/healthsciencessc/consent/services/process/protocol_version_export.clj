(ns org.healthsciencessc.consent.services.process.protocol-version-export
  (:use     [pliant.process :only [defprocess as-method]])
  (:require [org.healthsciencessc.consent.services.data :as data]
            [org.healthsciencessc.consent.services.respond :as respond]
            [org.healthsciencessc.consent.services.vouch :as vouch]
            [org.healthsciencessc.consent.common.types :as types]
            [pliant.webpoint.request :as endpoint]
            [clojure.data.xml :as xml]
            [clojure.string :as str])
  (:import [clojure.data.xml Element]))

(defn text
  [val]
  (cond 
    (coll? val)
      (into [] (for [v val] (Element. :p  {} [(. v toString)])))
    (nil? val)
      [""]
    :else
      [val]))

(defn emit-endorsements [xs filterfn]
  (Element. :endorsements
                {}
                (for [e xs]
                  (Element. :endorsement
                                {}
                                (for [l (filter filterfn (:labels e))]
                                  (Element. :label
                                                {}
                                                (text (:value l))))))))

(defn emit-metaitems [xs filterfn]
  (Element. :meta-items
                {}
                (for [m xs]
                  (Element. :meta-item
                                {}
                                (for [l (filter filterfn (:labels m))]
                                  (Element. :label
                                                {}
                                                (text (:value l))))))))

(defn emit-policies [policies filterfn]
  (Element. :policies
                {}
                (for [p policies]
                  (Element. :policy
                                (select-keys p [:name]) 
                                (concat (for [t (filter filterfn (:titles p))]
                                          (Element. :title {} (text (:value t))))
                                        (for [t (filter filterfn (:texts p))]
                                          (Element. :text {} (text (:value t))))))))) 

(defn traverse-pv
  [pv filterfn]
  [(emit-policies (:policies pv) filterfn)
   (emit-metaitems (:meta-items pv) filterfn)
   (emit-endorsements (:endorsements pv) filterfn)])

(defn generate-xml
  [protocol-version]
  (xml/emit-str
    (Element. :ProtocolVersion
              (select-keys protocol-version [:id])
              (for [lang (:languages protocol-version)]
                (Element. (keyword (str/lower-case (:name lang)))
                          {} ;; no attrs
                          (traverse-pv protocol-version
                                       (fn [n] (= (:id lang) (:id (:language n))))))))))

(defprocess export-xml
  [ctx]
  (let [protocol-version (vouch/designs-protocol-version ctx (get-in ctx [:query-params :protocol-version]))]
    (if protocol-version
      (respond/with-xml (generate-xml protocol-version))
      (respond/forbidden))))

(as-method export-xml endpoint/endpoints "get-protocol-version-export")

