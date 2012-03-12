(ns org.healthsciencessc.rpms2.consent-services.data
    (:require [clojurewerkz.neocons.rest :as neorest]
              [clojurewerkz.neocons.rest.nodes :as nodes]
              [clojurewerkz.neocons.rest.relationships :as relationships]
              [clojurewerkz.neocons.rest.paths :as paths]
              [clojurewerkz.neocons.rest.cypher :as cypher]))

(defn connect
  []
  (neorest/connect! "http://localhost:7474/db/data"))

(defn setup-schema
  []
  (let [root-node (nodes/get 0)]
    (map (fn [name]
           (let [obj-node (nodes/create {:name name})]
             (nodes/add-to-index (:id obj-node) "node-index-record-types" "name" name)
             (relationships/create obj-node root-node :root)))
         '("Organization" "Location"))))

(defn find-record-type-node
  [obj]
  (let [type-name (if (instance? String obj)
                    obj
                    (-> obj class .getSimpleName))]
    (first (nodes/find "node-index-record-types" :name type-name))))

(defn find-all-children
  [node relation]
  (nodes/traverse (:id node)
                  :relationships [{:direction "in" :type relation}]
                  :return-filter {:name "all_but_start_node" :language "builtin"}))

(defn clean-nils
  [record]
  (into {} (filter (comp not nil? val) record)))

(defn return-map
  [{:keys [id data]}]
  (assoc data :id id))

(defn find-node
  [id]
  (nodes/get id))

(defn create-type-relationship
  [node record]
  (relationships/create node (find-record-type-node record) :kind_of))

(defn create-node
  [record]
  (let [new-node (nodes/create (clean-nils record))]
    (create-type-relationship new-node record)
    new-node))

(defn update-node
  [record]
  (nodes/update (:id record) (dissoc (clean-nils record) :id)))

(defn find-all-instance-nodes
  [type]
  (let [type-node (find-record-type-node type)]
    (find-all-children type-node :kind_of)))

;; Public API

(defn find-all
  [type]
  (map return-map (find-all-instance-nodes type)))

(defn find-record
  [id]
  (return-map (find-node id)))

(defn create
  [record]
  (return-map (create-node record)))

(defn update
  [record]
  (do
    (update-node record)
    (find-record (:id record))))