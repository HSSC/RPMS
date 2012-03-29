(ns org.healthsciencessc.rpms2.consent-services.data
  (:use [clojure.set :only (difference)])
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
  (when id
    (nodes/get id)))

(defn create-relationship
  [from to relation]
  (relationships/create from to relation))

(defn create-type-relationship
  [node type]
  (create-relationship node (find-record-type-node type) :kind-of))

(defn create-parent-relationship
  [child-node parent-id relationship]
  (if-let [parent-node (find-node parent-id)]
    (create-relationship child-node parent-node relationship)))

(defn create-parent-relationships
  [node type data]
  (let [parent-relations (domain/get-parent-relations type domain/default-data-defs)]
    (doall (map (fn [parent-relation]
                  (let [parent-id (get-in data [(keyword (:related-to parent-relation)) :id])
                        relation (:relationship parent-relation)]
                    (create-parent-relationship node parent-id relation)))
                parent-relations))))

(defn new-node
  [type data]
  (-> (clean-nils data)
      (domain/validate-persistant-record type domain/default-data-defs)
      nodes/create))

(defn create-node
  [type data]
  (let [new-node (new-node type data)]
    (do
      (create-type-relationship new-node type)
      (create-parent-relationships new-node type data))
    new-node))

(defn update-node
  [type id data]
  (let [old-data (:data (find-node id))
        merged-data (merge old-data data)
        update-data (domain/validate-persistant-record (clean-nils merged-data) type domain/default-data-defs)]
                                        ;;(nodes/update id update-data)
    ))

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
  (let [parent-node (find-parent id relationship)]
    (when parent-node
      (node->record parent-node related-to))))

(defmethod get-related-obj :has-many
  [record relation]
  (let [relationship (domain/get-relationship-from-child (:type record) (:related-to relation) domain/default-data-defs)]
    (map #(node->record % (:related-to relation)) (find-all-children (:id record) relationship))))

(defn find-node-by-type-id
  [type id]
  (first (filter #(= id (:id %)) (find-all-instance-nodes type))))

(defn add-relations
  [record relations]
  (into record
        (for [relation relations]
          (let [related-obj (get-related-obj record relation)]
            (when related-obj
              [(domain/relation-name->key relation) related-obj])))))

(defn node->record
  [{:keys [id data]} type]
  (let [relations (domain/record-relations type domain/default-data-defs)]
    (-> (assoc data :id id :type type)
        (add-relations relations)
        (domain/validate-record type domain/default-data-defs))))

(defn delete-node
  [id]
  (if-let [node (find-node id)]
    (do
      (relationships/purge-all node)
      (nodes/delete id))))

(defn setup-schema
  [data-defs]
  (let [type-nodes (set (map #(get-in % [:data :name]) (find-all-children 0 :root)))
        data-def-types (set (keys data-defs))]
    (do
      (doall (map #(create-record-type-node %) (difference data-def-types type-nodes)))
      (doall (map #(delete-node (:id (find-record-type-node %)))
                  (difference type-nodes data-def-types)))))
  "Done")

(defn setup-default-schema
  []
  (setup-schema domain/default-data-defs))


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
  [type id data]
  (do
    (update-node type id data)
    (find-record type id)))

(defn create-test-nodes
  []
  (let [org (create "organization" {:name "MUSC"})
        user (create "user" {:username "foo" :password "$2a$10$1Qy9gB.cDZ5PWS.fWYGUcuPJ1X2V3xGYIbV/50TM6EFy7Yj2L4cjm"
                             :salt "$2a$10$1Qy9gB.cDZ5PWS.fWYGUcu" :organization {:id (:id org)}})
        location (create "location" {:name "Registration Desk" :organization {:id (:id org)}})
        admin-role (create "role" {:name "Administrator" :organization {:id (:id org)}})
        clerk-role (create "role" {:name "Clerk" :organization {:id (:id org)}})]
    (do
      (create "role-mapping" {:organization {:id (:id org)} :role {:id (:id admin-role)} :user {:id (:id user)} :location {:id (:id location)}})
      (create "role-mapping" {:organization {:id (:id org)} :role {:id (:id clerk-role)} :user {:id (:id user)} :location {:id (:id location)}}))))


(defn reset-test-db!
  []
  (do
    (delete-all-nodes!)
    (setup-default-schema)
    (create-test-nodes)))