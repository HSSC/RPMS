(ns org.healthsciencessc.rpms2.consent-services.default-processes.protocol-version-export
  (:use [org.healthsciencessc.rpms2.consent-services.domain-utils :only (forbidden-fn)]
        [org.healthsciencessc.rpms2.consent-services.default-processes.protocol-version :only (auth-designer-for-protocol)]
        [ring.util.response :only (response content-type)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-domain.types :as types]
            [clojure.data.xml :as xml]
            [clojure.string :as str])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]
           [clojure.data.xml Element]))

(defn emit-endorsements [xs filterfn]
  (Element. :endorsements
                {}
                (for [e xs]
                  (Element. :endorsement
                                {}
                                (for [l (filter filterfn (:labels e))]
                                  (Element. :label
                                                {}
                                                [(:value l)]))))))

(defn emit-metaitems [xs filterfn]
  (Element. :meta-items
                {}
                (for [m xs]
                  (Element. :meta-item
                                {}
                                (for [l (filter filterfn (:labels m))]
                                  (Element. :label
                                                {}
                                                [(:value l)]))))))

(defn emit-policies [policies filterfn]
  (Element. :policies
                {}
                (for [p policies]
                  (Element. :policy
                                (select-keys p [:name]) 
                                (concat (for [t (filter filterfn (:titles p))]
                                          (Element. :title {} [(apply str (:value t))]))
                                        (for [t (filter filterfn (:texts p))]
                                          (Element. :text {} (for [para (:value t)]
                                                               (Element. :p {} [para]))))))))) 

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

(def protocol-version-export-process
  {:name "get-protocol-version-export"
   :runnable-fn auth-designer-for-protocol
   :run-fn (fn [params]
             (let [protocol-version-id (get-in params [:query-params :protocol-version])
                   protocol-version (data/find-record types/protocol-version protocol-version-id)]
               (content-type (response (generate-xml protocol-version)) "text/xml")))
   :run-if-false forbidden-fn})

(process/register-process (DefaultProcess/create protocol-version-export-process))
