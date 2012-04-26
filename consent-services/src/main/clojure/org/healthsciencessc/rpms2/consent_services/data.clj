(ns org.healthsciencessc.rpms2.consent-services.data
  (:use [clojure.set :only (difference)])
  (:require [org.healthsciencessc.rpms2.consent-domain.core :as domain]
            [org.healthsciencessc.rpms2.consent-services.auth :as auth]
            [org.healthsciencessc.rpms2.consent-services.config :as config]
            [borneo.core :as neo])
  (:import [org.neo4j.graphdb.index IndexManager
            IndexHits
            Index]
           [org.neo4j.graphdb Node Relationship]
           [java.util UUID]))

(declare node->record)

(defn connect!
  [path]
  (neo/start! path))

(defn shutdown!
  []
  (neo/stop!))

(defn delete-all-nodes! []
  (neo/purge!))

(defn ^Index neo-index [name type]
  "Gets or creates an index, which may be for :nodes or :relationships"
  (let [^IndexManager index-mgr (neo/index)]
    (cond
     (= :nodes type)
     (.forNodes index-mgr (str "node-" name "-idx"))
     (= :relationships type)
     (.forRelationships index-mgr (str "node-" name "-idx"))
     :else
     (throw (IllegalArgumentException.)))))

(defn index-node [idx k v node]
  (.add idx node k v))

(defn create-type [name] ;; was create-record-type-node
  (neo/with-tx
    (let [type-node (neo/create-node! {:name name})]
      (neo/create-rel! type-node :root (neo/root))
      (index-node (neo-index "roottypes" :nodes)
                  "name"
                  name
                  type-node))))

(defn ^Node find-record-type-node
  [type-name]
  "Returns a node."
  (-> (neo-index "roottypes" :nodes)
      (.get "name" type-name)
      .getSingle))

(defn rel-between
  "Returns the Relationship of type rel between node1 and node2"
  [node1 node2 rel]
  (first (filter #(= node2 (neo/other-node % node1)) (neo/rels node1 rel))))

(defn find-parent
  [node relation]
  (if-let [rels (neo/rels node relation :out)]
    (.getEndNode (first rels))))

(defn- get-type
  [node]
  (let [type-node (find-parent node :kind-of)]
    (:name (neo/props type-node))))

(defn- type-of?
  [type node]
  (= type (get-type node)))

(defn children-nodes-by-rel
  [parent-node relation]
  (doall (map #(.getStartNode %) (neo/rels parent-node relation :in))))

(defn children-nodes-by-type
  [parent-node child-type]
  (let [child-rel (domain/get-relationship-from-child (get-type parent-node) child-type domain/default-data-defs)]
    (neo/traverse parent-node (fn [pos] (type-of? child-type (:node pos))) {child-rel :in})))

(defn clean-nils
  [data]
  (into {} (filter (comp not nil? val) data)))

(defn- find-node
  [neo4j-id]
  (throw (Exception. "Not a good idea.  Node ID's are unstable according to bitly/HA1qG6"))
  (if neo4j-id
    (neo/get-id neo4j-id)))

(defn- ^Relationship create-relationship
  [from type to]
  "Relation is a string"
  (neo/create-rel! from type to))

(defn- get-node-by-index [type id]
  (-> (neo-index type :nodes)
      (.get "id" id)
      .getSingle))

(defn sibling-nodes-by-type
  [{:keys [start-type start-id parent-type parent-id sibling-type]}]
  (let [parent-rel (domain/get-relationship-from-child parent-type start-type domain/default-data-defs)
        parent-node (if parent-id
                      (get-node-by-index parent-type parent-id)
                      (find-parent (get-node-by-index start-type start-id) parent-rel))]
    (children-nodes-by-type parent-node sibling-type)))

(defn neighbors-by-type
  "Gets all adjacent nodes of the given type to the given node"
  [node type]
  (let [{:keys [dir rel]} (domain/get-directed-relation (get-type node) type domain/default-data-defs)]
    (if (and dir rel)
      (neo/traverse node (fn [pos] (type-of? type (:node pos))) {rel dir}))))

(defn walk-types-path
  "Walks from the start node through all nodes of the given types and returns a collection of nodes of the last type in the path"
  [start-node path]
  (loop [nodes (list start-node) type-path path]
    (if (or (empty? type-path) (empty? nodes))
      nodes
      (recur (distinct (filter identity (flatten (map (fn [node] (neighbors-by-type node (first type-path))) nodes))))
             (rest type-path)))))

(defn new-node
  [type data]
  (-> (clean-nils data)
      (domain/validate-persistent-record type domain/default-data-defs)
      (assoc :active true
             :id (str (UUID/randomUUID)))))

(defn create-node
  [type props]
  (neo/with-tx
    (let [node-props (new-node type props)
          node (neo/create-node! node-props)]
      (index-node (neo-index type :nodes) "id" (:id node-props) node)
      node)))

(defn update-node-props
  [type id data]
  "Returns the node"
  (neo/with-tx
    (let [upd-node (get-node-by-index type id)
          merged-data (merge (neo/props upd-node) data)
          update-data (domain/validate-persistent-record
                       (clean-nils merged-data) type domain/default-data-defs)]
      (neo/set-props! upd-node update-data)
      upd-node)))

(defn find-all-instance-nodes
  [type]
  (if-let [type-node (find-record-type-node type)]
    (children-nodes-by-rel type-node :kind-of)))

(defn- create-edges [node relation-list]
  (let [real-edges (for [edge relation-list]
                     (into {} (for [[k v] edge]
                                (if (= v :self)
                                  [k node]
                                  [k v]))))]
    (neo/with-tx
      (doseq [{:keys [from to rel-type]} real-edges]
        (create-relationship from rel-type to)))))

(defmulti get-related-obj
  (fn [record node relation] (:type relation)))

(defmethod get-related-obj :belongs-to
  [record node {:keys [relationship related-to]}]
  (if-let [parent-node (find-parent node relationship)]
    (when parent-node (node->record parent-node related-to))))

(defmethod get-related-obj :has-many
  [record node relation]
  (if-let [relationship (domain/get-relationship-from-child
                         (:type record)
                         (:related-to relation)
                         domain/default-data-defs)]
    (vec (map #(node->record % (:related-to relation))
              (children-nodes-by-rel node relationship)))))

(defmethod get-related-obj :has-many-through
  [record node relation]
  (let [{:keys [related-to relation-path]} relation
        path (conj relation-path related-to)]
    (vec (map #(node->record % related-to) (walk-types-path node path)))))

(defn add-relations
  [record node relations]
  (reduce
   (fn [record-map relation]
     (let [related-obj (get-related-obj record node relation)
           related-obj-key (domain/relation-name->key relation)]
       (if related-obj
         (if-let [current-val (related-obj-key record-map)]
           (assoc record-map related-obj-key (into current-val related-obj))
           (assoc record-map related-obj-key related-obj))
         record-map)))
   record
   relations))

(defn node->record
  [node type]
  (let [props (neo/props node)
        relations (domain/record-relations type domain/default-data-defs)]
    (if (:active props)
      (-> props
          (assoc :type type)
          (add-relations node relations)
          (domain/validate-record type domain/default-data-defs)))))

(defn setup-schema
  [data-defs]
  (let [type-nodes (set (map :name (map neo/props (children-nodes-by-rel (neo/root) :root))))
        data-def-types (set (keys data-defs))]
    (doseq [node (difference data-def-types type-nodes)]
      (create-type node))))
    ;;(doseq [rel (difference type-nodes data-def-types)]
      ;;(neo/delete-node! (find-record-type-node rel)))))

(defn validate-relation
  [{:keys [from to rel-type] :as relation}]
  (let [{:keys [id type] :as node} 
          (cond (map? from) from
                (map? to) to
                :else
                (throw (IllegalArgumentException. "Bad relation")))
         ]
    (if (and rel-type type id)
      (let [other-node (get-node-by-index type id)]
        (cond
         from (assoc relation
                :from other-node
                :to :self)
         to (assoc relation
              :from :self
              :to other-node)))
      (throw (IllegalArgumentException. "Bad relation")))))

(defn validate-domain-relations
  [type props]
  (for [{:keys [related-to relationship]}
        (domain/get-parent-relations type domain/default-data-defs)
        :when (get props (keyword related-to))]
    {:from :self
     :to (get-node-by-index related-to (get-in props [(keyword related-to) :id]))
     :rel-type relationship}))

(defn get-raw-nodes
  [type]
  (map neo/props (find-all-instance-nodes type)))

;; Public API
(defn belongs-to?
  [child-type child-id parent-type parent-id]
  (let [parent-node (get-node-by-index parent-type parent-id)
        child-node (get-node-by-index child-type child-id)
        rel (domain/get-relationship-from-child parent-type child-type domain/default-data-defs)]
    (= parent-node (find-parent child-node rel))))

(defn find-all
  [type]
  (filter identity (map #(node->record % type) (find-all-instance-nodes type))))

(defn find-record
  [type id]
  (if-let [node (get-node-by-index type id)]
    (node->record node type)))

(defn find-records-by-attrs
  [type attr-map]
  (filter (fn [record]
            (= attr-map (select-keys record (keys attr-map))))
          (find-all type)))

(defn find-related-records
  "From the start record, finds all the records at the end of the relation path"
  [start-type start-id & relation-path]
  (let [start-node (get-node-by-index start-type start-id)
        nodes (walk-types-path start-node relation-path)]
    (map #(node->record % (last relation-path)) nodes)))

(defn find-children
  [parent-type parent-id child-type]
  (let [parent-node (get-node-by-index parent-type parent-id)]
    (map #(node->record % child-type) (children-nodes-by-type parent-node child-type))))

(defn find-siblings
  "Takes :start-type :start-id :parent-type :parent-id :sibling-type as keys"
  [{:keys [sibling-type] :as sibling-relations}]
  (map #(node->record % sibling-type) (sibling-nodes-by-type sibling-relations)))

(defn siblings?
  [{:keys [sibling-id] :as sibling-relations}]
  (let [sibling-nodes (sibling-nodes-by-type sibling-relations)
        sibling-ids (map #(:id (neo/props %)) sibling-nodes)]
    (not (nil? (some #{sibling-id} sibling-ids)))))

(defn create
  [type properties & extra-relationships]   ;; data includes default relationships.  relationships is extra relationships
  "extra-relationships is a sequence of maps with either :from or :to, plus a relationtype"
  (let [rels (concat [{:from :self
                       :to (find-record-type-node type)
                       :rel-type :kind-of}]
                     (validate-domain-relations type properties)
                     (map validate-relation extra-relationships))]
    (neo/with-tx
      (let [node (create-node type properties)]
        (create-edges node rels)
        (node->record node type)))))

(defn update
  [type id data]
  "This only updates properties, not relations"
  (update-node-props type id data)
  (find-record type id))

(defn delete
  [type id]
  (let [node (get-node-by-index type id)
        props (assoc (neo/props node) :active false)]
    (neo/with-tx
      (neo/set-props! node props))
    true))

(defn relate-records
  [child-type child-id parent-type parent-id]
  (let [child-node (get-node-by-index child-type child-id)
        parent-node (get-node-by-index parent-type parent-id)
        rel (:relationship (domain/get-parent-relation parent-type child-type domain/default-data-defs))]
    (do
      (create-relationship child-node rel parent-node)
      (find-record child-type child-id))))

(defn unrelate-records
  [child-type child-id parent-type parent-id]
  (let [child-node (get-node-by-index child-type child-id)
        parent-node (get-node-by-index parent-type parent-id)
        rel (:relationship (domain/get-parent-relation parent-type child-type domain/default-data-defs))]
    (do
      (neo/delete! (rel-between child-node parent-node rel))
      (find-record child-type child-id))))
