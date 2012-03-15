(ns org.healthsciencessc.rpms2.consent-services.data
  (:require [org.healthsciencessc.rpms2.consent-domain.core :as domain]
            [org.healthsciencessc.rpms2.consent-services.config :as config]
            [clojurewerkz.neocons.rest :as neorest]
            [clojurewerkz.neocons.rest.nodes :as nodes]
            [clojurewerkz.neocons.rest.relationships :as relationships]
            [clojurewerkz.neocons.rest.paths :as paths]
            [clojurewerkz.neocons.rest.cypher :as cypher]))

(declare node->record)

(defn connect!
  []
  (neorest/connect! (config/conf "neo4j-url")))

(defn delete-all-nodes!
  []
  "For development only!"
  (neorest/DELETE "http://localhost:7474/cleandb/foobar"))

(defn create-record-type-node
  [name]
  (let [root-node (nodes/get 0)
        obj-node (nodes/create {:name name})]
    (nodes/add-to-index (:id obj-node) "node-index-record-types" "name" name)
    (relationships/create obj-node root-node :root)))

(defn setup-schema
  []
  (map create-record-type-node
       '("organization" "location" "user")))

(defn find-record-type-node
  [type]
  (first (nodes/find "node-index-record-types" :name type)))

(defn find-all-children
  [node-id relation]
  (nodes/traverse node-id
                  :relationships [{:direction "in" :type relation}]
                  :return-filter {:name "all_but_start_node" :language "builtin"}))

(defn find-parent
  [node-id relation]
  (first (nodes/traverse node-id
                         :relationships [{:direction "out" :type relation}]
                         :return-filter {:name "all_but_start_node" :language "builtin"})))

(defn clean-nils
  [data]
  (into {} (filter (comp not nil? val) data)))

(defn find-node
  [id]
  (nodes/get id))

(defn create-relationship
  [from to relation]
  (relationships/create from to relation))

(defn create-type-relationship
  [node type]
  (create-relationship node (find-record-type-node type) :kind-of))

(defn create-parent-relationship
  [child-node parent-id relationship]
  (let [parent-node (find-node parent-id)]
    (create-relationship child-node parent-node relationship)))

(defn create-parent-relationships
  [node type data]
  (let [parent-relations (domain/get-parent-relations type)]
    (doall (map (fn [parent-relation]
                  (let [parent-id (get-in data [(keyword (:related-to parent-relation)) :id])
                        relation (:relationship parent-relation)]
                    (create-parent-relationship node parent-id relation)))
                parent-relations))))

(defn create-node
  [type data]
  (let [new-node (-> (clean-nils data) (domain/validate-persistant-record type) nodes/create)]
    (do
      (create-type-relationship new-node type)
      (create-parent-relationships new-node type data))
    new-node))

(defn update-node
  [type record]
  (let [node-id (:id record)
        update-data (domain/validate-persistant-record (clean-nils record) type)]
    (nodes/update node-id update-data)))

(defn find-all-instance-nodes
  [type]
  (if-let [type-node-id (:id (find-record-type-node type))]
    (find-all-children type-node-id :kind-of)))

(defn create-new-node-relationship
  [new-node [dir record rel]]
  (if-let [other-node (find-node (:id record))]
    (if (= :to dir)
      (create-relationship new-node other-node rel)
      (create-relationship other-node new-node rel))))

(defn create-new-node-relationships
  [new-node relationships]
  (doall (map #(create-new-node-relationship new-node %)
              relationships)))

(defmulti get-related-obj
  (fn [record relation] (:type relation)))

(defmethod get-related-obj :belongs-to
  [{id :id} {:keys [relationship related-to]}]
  (-> (find-parent id relationship)
      (node->record related-to)))

(defmethod get-related-obj :has-many
  [record relation])

(defn find-node-by-type-id
  [type id]
  (first (filter #(= id (:id %)) (find-all-instance-nodes type))))

(defn add-relations
  [record relations]
  (into record
        (for [relation relations]
          [(domain/relation-name->key relation) (get-related-obj record relation)])))

(defn node->record
  [{:keys [id data]} type]
  (let [relations (get-in domain/data-defs [type :relations])]
    (-> (assoc data :id id :type type)
        (add-relations relations)
        domain/validate-record)))


;; Public API

(defn find-all
  [type]
  (map #(node->record % type) (find-all-instance-nodes type)))

(defn find-record
  [type id]
  (-> (find-node-by-type-id type id)
      (node->record type)))

(defn find-records-by-attrs
  [type attr-map]
  (filter (fn [record]
            (= attr-map (select-keys record (keys attr-map))))
          (find-all type)))

(defn create
  [type data & relationships]
  (let [new-node (create-node type data)]
    (do
      (create-new-node-relationships new-node relationships)
      (node->record new-node type))))

(defn update
  [type {id :id :as record}]
  (do
    (update-node type record)
    (find-record type id)))