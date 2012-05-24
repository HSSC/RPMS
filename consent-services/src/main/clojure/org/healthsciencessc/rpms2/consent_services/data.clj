(ns org.healthsciencessc.rpms2.consent-services.data
  (:use [clojure.set :only (difference)])
  (:require [org.healthsciencessc.rpms2.consent-domain.core :as domain]
            [org.healthsciencessc.rpms2.consent-services.auth :as auth]
            [org.healthsciencessc.rpms2.consent-services.config :as config]
            [borneo.core :as neo]
            [clojure.zip :as zip])
  (:import [org.neo4j.graphdb.index IndexManager
            IndexHits
            Index]
           [org.neo4j.graphdb Node Relationship]
           [java.util UUID]))

(declare node->record)

(def schema domain/default-data-defs)

(def default-rel {:has-default :out})

(defn connect!
  [path]
  (neo/start! path))

(defn shutdown!
  []
  (neo/stop!))

(defn delete-all-nodes! []
  (neo/purge!)
  (neo/with-tx
    (let [index-manager (neo/index)]
      (doseq [x (.nodeIndexNames index-manager)]
        (.delete (.forNodes index-manager x)))
      (doseq [x (.relationshipIndexNames index-manager)]
        (.delete (.forRelationships index-manager x))))))

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
  [child-node relation]
  (first (neo/traverse child-node :1 nil {relation :out})))

(defn- get-type
  [node]
  (let [type-node (find-parent node :kind-of)]
    (:name (neo/props type-node))))

(defn- type-of?
  [type node]
  (= type (get-type node)))

(defn children-nodes-by-rel
  [parent-node relation & extra-rels]
  (neo/traverse parent-node :1 nil (apply merge {relation :in} extra-rels)))

(defn children-nodes-by-type
  [parent-node child-type & extra-rels]
  (let [child-rel (domain/get-relationship-from-child (get-type parent-node) child-type schema)]
    (neo/traverse parent-node
                  :1
                  (fn [pos] (let [current-node (:node pos)]
                              (and (not= current-node parent-node) (type-of? child-type current-node))))
                  (apply merge {child-rel :in} extra-rels))))

(defn clean-nils
  [data]
  (into {} (filter (comp not nil? val) data)))

(defn- ^Relationship create-relationship
  [from type to]
  "Relation is a string"
  (neo/create-rel! from type to))

(defn- get-node-by-index [type id]
  (-> (neo-index type :nodes)
      (.get "id" id)
      .getSingle))

(defn neighbors-by-type
  "Gets all adjacent nodes of the given type to the given node"
  [node type extra-rels]
  (let [{:keys [dir rel]} (domain/get-directed-relationship (get-type node) type schema)]
    (if (and dir rel)
      (neo/traverse node
                    :1
                    (fn [pos] (let [current-node (:node pos)]
                                (and (not= current-node node) (type-of? type current-node))))
                    (apply merge {rel dir} extra-rels)))))

(defn walk-types-path
  "Walks from the start node through all nodes of the given types and returns a collection of nodes of the last type in the path"
  [start-node path & extra-rels]
  (loop [nodes (list start-node) type-path path]
    (if (or (empty? type-path) (empty? nodes))
      nodes
      (recur (distinct (filter identity (flatten (map (fn [node] (neighbors-by-type node (first type-path) extra-rels)) nodes))))
             (rest type-path)))))

(defn new-node
  [type data]
  (-> (clean-nils data)
      (domain/validate-persistent-record type schema)
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
                       (clean-nils merged-data) type schema)]
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
    (when parent-node (node->record parent-node related-to :add-rels false))))

(defmethod get-related-obj :has-many
  [record node relation]
  (if-let [relationship (domain/get-relationship-from-child
                         (:record-type record)
                         (:related-to relation)
                         schema)]
    (vec (map #(node->record % (:related-to relation))
              (children-nodes-by-rel node relationship)))))

(defmethod get-related-obj :has-many-through
  [record node relation]
  (let [{:keys [related-to relation-path]} relation
        path (conj relation-path related-to)]
    (vec (map #(node->record % related-to) (walk-types-path node path)))))

(defmethod get-related-obj :many-to-many
  [record node relation]
  (let [{related-to :related-to} relation]
    (vec (map #(node->record % related-to) (walk-types-path node (vector related-to))))))

(defn add-relations
  [record node relations]
  (reduce
   (fn [record-map relation]
     (let [related-obj (get-related-obj record node relation)
           related-obj-key (domain/get-relation-name relation)]
       (if related-obj
         (if-let [current-val (related-obj-key record-map)]
           (assoc record-map related-obj-key (into current-val related-obj))
           (assoc record-map related-obj-key related-obj))
         record-map)))
   record
   relations))

(defn node->record
  [node type & {:keys [add-rels] :or {add-rels true}}]
  (let [props (neo/props node)
        relations (domain/record-relations type schema)]
    (if (:active props)
      (domain/validate-record
       (let [record (assoc props :record-type type)]
         (if add-rels
           (add-relations record node relations)
           record))
       type
       schema))))

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
  (for [{:keys [related-to relationship] :as relation}
        (domain/get-parent-relations type schema)
        :let [rel-key (domain/get-relation-name relation)]
        :when (get props rel-key)]
    {:from :self
     :to (get-node-by-index related-to (get-in props [rel-key :id]))
     :rel-type relationship}))

(defn get-raw-nodes
  [type]
  (map neo/props (find-all-instance-nodes type)))

(defn create-node-with-default-relationships
  [node-type node-properties extra-relationships]
  (let [rels (concat [{:from :self
                       :to (find-record-type-node node-type)
                       :rel-type :kind-of}]
                     (validate-domain-relations node-type node-properties)
                     (map validate-relation extra-relationships))
        node (create-node node-type node-properties)]
    (create-edges node rels)
    node))

;; Public API
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
  ([start-type start-id relation-path]
     (find-related-records start-type start-id relation-path true))
  ([start-type start-id relation-path include-defaults]
     (let [start-node (get-node-by-index start-type start-id)
           nodes (walk-types-path start-node relation-path (if include-defaults default-rel))]
       (map #(node->record % (last relation-path)) nodes))))

(defn find-children
  ([parent-type parent-id child-type]
     (find-children parent-type parent-id child-type true))
  ([parent-type parent-id child-type include-defaults]
     (let [parent-node (get-node-by-index parent-type parent-id)]
       (map #(node->record % child-type)
            (children-nodes-by-type parent-node child-type (if include-defaults default-rel))))))

(defn belongs-to?
  ([child-type child-id parent-type parent-id]
     (belongs-to? child-type child-id parent-type parent-id true))
  ([child-type child-id parent-type parent-id include-defaults]
     (let [children (find-children parent-type parent-id child-type include-defaults)]
       (some (partial = child-id) (map :id children)))))

(defn create
  [type properties & extra-relationships]
  "extra-relationships is a sequence of maps with either :from or :to, plus a relationtype"
  (node->record
   (neo/with-tx
     (create-node-with-default-relationships type properties extra-relationships))
   type))

(defn create-records
  [type props]
  (let [parent-rel (domain/get-parent-relationship type type schema)
        child-rel (domain/get-child-relationship type type schema)
        record-tree (zip/zipper (fn [node] (not (empty? (child-rel node))))
                                (fn [node] (child-rel node))
                                (fn [node children] (assoc node child-rel children))
                                props)]
    (neo/with-tx
      (loop [loc record-tree]
        (if (zip/end? loc)
          (let [root-id (get-in (zip/root loc) [:neo-data :id])]
            (find-record type root-id))
          (let [parent-loc (zip/up loc)
                parent-data (if parent-loc (:neo-data (zip/node parent-loc)))
                child-node (zip/node loc)
                neo-data (if parent-data
                           (assoc child-node parent-rel parent-data)
                           child-node)
                record (node->record (create-node-with-default-relationships type neo-data nil) type)]
            (recur
             (zip/next (zip/replace loc (assoc child-node :neo-data record))))))))))

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
        rel (domain/get-parent-relationship parent-type child-type schema)]
    (do
      (create-relationship child-node rel parent-node)
      (find-record child-type child-id))))

(defn unrelate-records
  [child-type child-id parent-type parent-id]
  (let [child-node (get-node-by-index child-type child-id)
        parent-node (get-node-by-index parent-type parent-id)
        rel (domain/get-parent-relationship parent-type child-type schema)]
    (do
      (neo/delete! (rel-between child-node parent-node rel))
      (find-record child-type child-id))))