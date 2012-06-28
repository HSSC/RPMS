(ns org.healthsciencessc.rpms2.consent-services.default-processes.protocol-version-export
  (:use [org.healthsciencessc.rpms2.consent-services.domain-utils :only (forbidden-fn)]
        [org.healthsciencessc.rpms2.consent-services.default-processes.protocol-version :only (auth-designer-for-protocol)]
        [ring.util.response :only (response content-type)])
  (:require [org.healthsciencessc.rpms2.process-engine.core :as process]
            [org.healthsciencessc.rpms2.consent-services.data :as data]
            [org.healthsciencessc.rpms2.consent-domain.types :as types]
            [clojure.data.xml :as xml]
            [clojure.string :as str])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess]))

(def protocol-version-elements
  {:meta-items {:child-types [:labels]
                :attributes [:name]
                :node-name :MetaItem}
   :endorsements {:child-types [:labels]
                  :attributes [:name]
                  :node-name :Endorsement}
   :policies {:child-types [:texts :titles]
              :attributes [:name]
              :node-name :Policy}})

(defn singularize
  [s]
  (cond
   (string? s) (apply str (drop-last (seq s)))
   (keyword? s) (keyword (singularize (name s)))))

(defn camel-case
  [s]
  (cond
   (string? s) (str/join (map str/capitalize (str/split (str/replace s "-" " ") #" ")))
   (keyword? s) (keyword (camel-case (name s)))))

(defn get-children-by-lang
  [record type lang]
  (let [child-types (get-in protocol-version-elements [type :child-types])]
    (filter identity (for [child-type child-types child (child-type record)]
                       (if (= lang (:language child))
                         [(singularize child-type) (:value child)])))))

(defn get-elems-by-lang
  [protocol-version type lang]
  (let [records (type protocol-version)]
    (filter identity
            (map
             (fn [record]
               (let [children (get-children-by-lang record type lang)]
                 (if (not (empty? children))
                   {:children children
                    :attributes (select-keys record (get-in protocol-version-elements [type :attributes]))
                    :node-name (get-in protocol-version-elements [type :node-name])})))
             records))))

(defn generate-elem-node
  [{:keys [node-name children attributes]}]
  (apply xml/element node-name attributes
         (for [[node value] children]
           (xml/element (camel-case node) {} value))))

(defn generate-type-node-by-lang
  [protocol-version type lang]
  (apply xml/element (camel-case type) {}
         (for [element (get-elems-by-lang protocol-version type lang)]
           (generate-elem-node element))))

(defn generate-xml
  [protocol-version]
  (let [element-types (keys protocol-version-elements)
        langs (:languages protocol-version)]
    (xml/emit-str
     (apply xml/element :ProtocolVersion {:id (:id protocol-version)}
            (for [lang langs]
              (apply xml/element (camel-case (keyword (:name lang))) {}
                     (map #(generate-type-node-by-lang protocol-version % lang)
                          element-types)))))))

(def protocol-version-export-process
  {:name "get-protocol-version-export"
   :runnable-fn auth-designer-for-protocol
   :run-fn (fn [params]
             (let [protocol-version-id (get-in params [:query-params :protocol-version])
                   protocol-version (data/find-record types/protocol-version protocol-version-id)]
               (content-type (response (generate-xml protocol-version)) "text/xml")))
   :run-if-false forbidden-fn})

(process/register-process (DefaultProcess/create protocol-version-export-process))