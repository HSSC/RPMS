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
  [record]
  (first (nodes/find "node-index-record-types" :name (-> record class .getSimpleName))))

(defn find-all-children
  [node relation]
  (nodes/traverse (:id node)
                  :relationships [{:direction "in" :type relation}]
                  :return-filter {:name "all_but_start_node" :language "builtin"}))

(defn find-node
  [id]
  (nodes/get id))

(defn create-type-relationship
  [node record]
  (relationships/create node (find-record-type-node record) :kind_of))

(defn create-node
  [record]
  (let [new-node (nodes/create record)]
    (create-type-relationship new-node record)
    new-node))

(defn update-node
  [record]
  (nodes/update (:id record) (dissoc :id record)))

(defn find-all-instance-nodes
  [type]
  (let [type-node (find-record-type-node type)]
    (find-all-children type-node :kind_of)))

;; Public API

(defn find-all
  [type]
  (map :data (find-all-instance-nodes type)))

(defn find-record
  [id]
  (:data (find-node id)))

(defn create
  [record]
  (:data (create-node record)))

(defn update
  [record]
  (:data (update-node record)))