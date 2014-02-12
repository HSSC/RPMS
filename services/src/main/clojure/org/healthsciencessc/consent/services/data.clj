(ns org.healthsciencessc.consent.services.data
  (:use [clojure.set :only (difference)]
        [clojure.string :only (blank? capitalize)]
        [org.healthsciencessc.consent.services.session :only (*current-user*)])
  (:require [org.healthsciencessc.consent.common.core :as domain]
            [org.healthsciencessc.consent.common.types :as types]
            [borneo.core :as neo]
            [clojure.zip :as zip])
  (:import [org.neo4j.graphdb.index IndexManager
            IndexHits
            Index]
           [org.neo4j.graphdb Node Relationship]
           [java.util UUID]))

(declare node->record)
(declare create-node-with-default-relationships)

(def schema domain/default-data-defs)

(def default-rel :has-default)
(def directed-default-rel
  {default-rel :out})

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

(defn rels-between
  [node1 node2]
  (filter #(= node2 (neo/other-node % node1)) (neo/rels node1)))

(defn rel-between
  "Returns the Relationship of type rel between node1 and node2"
  [node1 node2 rel]
  (first (filter #(= rel (neo/rel-type %)) (rels-between node1 node2))))

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
  (if-let [child-rel (domain/get-relationship-from-child (get-type parent-node) child-type schema)]
    (neo/traverse parent-node
                  :1
                  (fn [pos] (let [current-node (:node pos)]
                              (and (not= current-node parent-node) (type-of? child-type current-node))))
                  (apply merge {child-rel :in} extra-rels))))

(defn find-nodes-by-props
  [type props]
  (let [type-node (find-record-type-node type)]
    (neo/traverse type-node :1 props {:kind-of :in})))

(defn clean-nils
  [data]
  (into {} (filter (comp not nil? val) data)))

(defn- ^Relationship create-relationship
  [from type to]
  "Relation is a string"
  (neo/create-rel! from type to))

(defn get-node-by-index [type id]
  (if id
    (-> (neo-index type :nodes)
        (.get "id" id)
        .getSingle)))

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

(defn find-or-create-node
  [type id props]
  (let [node (get-node-by-index type id)]
    (if node
      node
      (create-node-with-default-relationships type props nil))))

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
  [record node {:keys [relationship related-to omit-rels]}]
  (if-let [parent-node (find-parent node relationship)]
    (when parent-node (node->record parent-node related-to :omit-rels omit-rels))))

(defmethod get-related-obj :has-many
  [record node relation]
  (let [{:keys [related-to omit-rels]} relation]
    (vec (filter identity (map #(node->record % related-to :omit-rels omit-rels)
                               (children-nodes-by-type node related-to))))))

(defmethod get-related-obj :has-many-through
  [record node relation]
  (let [{:keys [related-to relation-path omit-rels]} relation
        path (conj relation-path related-to)]
    (vec (filter identity (map #(node->record % related-to :omit-rels omit-rels) (walk-types-path node path))))))

(defmethod get-related-obj :many-to-many
  [record node relation]
  (let [{:keys [relationship related-to omit-rels]} relation]
    (vec (filter identity (map #(node->record % related-to :omit-rels omit-rels) (neo/traverse node :1 :all-but-start relationship))))))

(defn add-related-records
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
  [node type & {:keys [omit-rels] :or {omit-rels false}}]
  (let [props (neo/props node)
        relations (domain/record-relations type schema)]
    (if (:active props)
      (domain/validate-record
       (let [record (assoc props :record-type type)]
         (if omit-rels
           record
           (add-related-records record node relations)))
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

(defn parent-relationship
  [parent {:keys [related-to relationship can-create-parent]}]
  (let [parent-id (:id parent)
        parent-node (if can-create-parent
                      (find-or-create-node related-to parent-id parent)
                      (get-node-by-index related-to parent-id))]
    {:from :self
     :to parent-node
     :rel-type relationship}))

(defn create-domain-relations
  [type props]
  (flatten
   (for [relation (domain/get-parent-relations type schema)
         :let [rel-key (domain/get-relation-name relation)
               parent-data (rel-key props)]
         :when parent-data]
     (if (map? parent-data)
       (parent-relationship parent-data relation)
       (map #(parent-relationship % relation) parent-data)))))

(defn create-default-vaule-realtions
  [type props]
  (let [default-org (first (filter #(= types/code-base-org (:code (neo/props %))) (find-all-instance-nodes "organization")))
        default-org-id (if default-org (:id (neo/props default-org)))]
    (cond
     (and (= "organization" type) default-org)
     (let [default-val-nodes (reduce (fn [nodes type] (concat nodes (children-nodes-by-type default-org type)))
                                     []
                                     domain/default-value-types)]
       (for [node default-val-nodes]
         {:from :self
          :to node
          :rel-type default-rel}))
     (and (= default-org-id (get-in props [:organization :id])) (some (partial = type) domain/default-value-types))
     (let [non-default-orgs (filter #(not= default-org-id (:id (neo/props %))) (find-all-instance-nodes "organization"))]
       (for [org non-default-orgs]
         {:from org
          :to :self
          :rel-type default-rel})))))

(defn create-node-with-default-relationships
  "Creates a node along with domain relationships.  Needs to be run in a neo4j transaction."
  [node-type node-properties extra-relationships]
  (let [rels (concat [{:from :self
                       :to (find-record-type-node node-type)
                       :rel-type :kind-of}]
                     (create-domain-relations node-type node-properties)
                     (create-default-vaule-realtions node-type node-properties)
                     (map validate-relation extra-relationships))
        node (create-node node-type node-properties)]
    (create-edges node rels)
    node))

(defn get-raw-nodes
  [type]
  (map neo/props (find-all-instance-nodes type)))

(defn add-type-to-children
  "Adds a :record-type key to the children of record with their type"
  [type record]
  (let [child-relations (domain/get-child-relations type schema)
        rels (for [relation child-relations]
               [(domain/get-relation-name relation) (:related-to relation)])
        children-by-rel (select-keys record (map first rels))]
    (flatten (for [rel rels]
               (map #(assoc % :record-type (second rel)) ((first rel) children-by-rel))))))

;; Data audits

(defn create-audit
  [node audit-type]
  (let [current-time (System/currentTimeMillis)
        audit-rel (audit-type domain/audit-relationships)
        current-user-node (get-node-by-index "user" (:id *current-user*))]
    (if current-user-node
      (neo/with-tx
        (neo/set-prop!
         (create-relationship node audit-rel current-user-node)
         :timestamp current-time))))
  node)

;; Data Validations

(defn validate-required-props
  [type record]
  (let [required-props (domain/get-attrs type schema :required)]
    (->> required-props
         (filter (fn [prop] (blank? (prop record))))
         (map (fn [prop] (str (capitalize (name prop)) " is required."))))))

(defn validate-unique-fields
  [type record]
  (let [unique-props (domain/get-attrs type schema :unique)
        current-node (get-node-by-index type (:id record))]
    (->> unique-props
         (filter (fn [prop] (not (empty? (remove (partial = current-node) (find-nodes-by-props type (select-keys record (list prop))))))))
         (map (fn [prop] (str (capitalize (name prop)) " must be unique."))))))

(defn custom-validations
  [type record]
  (let [validations (for [prop (domain/get-attrs type schema :validation)]
                      [prop (get-in schema [type :attributes prop :validation])])]
    (->> validations
         (filter (fn [[prop valid-fn]] (not (nil? (valid-fn (prop record))))))
         (map (fn [[prop valid-fn]] (valid-fn (prop record)))))))

(defn validate-required-rels
  [type record]
  (let [reqired-rels (domain/required-rels type schema)]
    (->> reqired-rels
         (filter (fn [rel] (nil? (get-node-by-index (:related-to rel) (get-in record [(domain/get-relation-name rel) :id])))))
         (map (fn [rel] (str (capitalize (name (domain/get-relation-name rel))) " is required."))))))

(defn validate-properties
  [type record]
  (concat (validate-required-props type record)
          (validate-unique-fields type record)
          (custom-validations type record)))

(defn validate-record
  [type record]
  (concat (validate-properties type record)
          (validate-required-rels type record)))

(defn with-validation
  [type record execfn & validfns]
  (let [errors (if validfns
                 (reduce conj (map #(% type record) validfns))
                 (validate-record type record))]
    (if (empty? errors)
      (execfn)
      (throw (ex-info (str "Validation failed on type '" type "'.") {:type ::invalid-record :errors errors :data record})))))

;; Public API
(defn find-all
  [type]
  (filter identity (map #(node->record % type) (find-all-instance-nodes type))))

(defn find-record
  [type id]
  (if-let [node (get-node-by-index type id)]
    (node->record node type)
    (throw (ex-info (str "Failed to find type '" type "'.") {:type ::record-not-found :record-type type :id id}))))

(defn find-records-by-attrs
  [type attr-map]
  (let [nodes (find-nodes-by-props type attr-map)]
    (map #(node->record % type) nodes)))

(defn find-related-records
  "From the start record, finds all the records at the end of the relation path"
  ([start-type start-id relation-path]
     (find-related-records start-type start-id relation-path true))
  ([start-type start-id relation-path include-defaults]
     (if-let [start-node (get-node-by-index start-type start-id)]
       (let [nodes (walk-types-path start-node relation-path (if include-defaults directed-default-rel))]
         (filter identity (map #(node->record % (last relation-path)) nodes)))
       (throw (ex-info (str "Failed to find records related to type '" start-type "'.") {:type ::record-not-found :record-type start-type :id start-id})))))

(defn find-children
  ([parent-type parent-id child-type]
     (find-children parent-type parent-id child-type true))
  ([parent-type parent-id child-type include-defaults]
     (if-let [parent-node (get-node-by-index parent-type parent-id)]
       (filter identity (map #(node->record % child-type)
                             (children-nodes-by-type parent-node child-type (if include-defaults directed-default-rel))))
       (throw (ex-info (str "Failed to find type '" child-type "' children of type '" parent-type "'.") {:type ::record-not-found :record-type parent-type :id parent-id})))))

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
   (create-audit
    (with-validation type properties
      #(neo/with-tx
         (create-node-with-default-relationships type properties extra-relationships)))
    :create)
   type))

(defn create-records
  [type props]
  (let [record-tree (zip/zipper (fn [node] (not (empty? (add-type-to-children (:record-type node) node))))
                                (fn [node] (add-type-to-children (:record-type node) node))
                                (fn [node children] node)
                                (assoc props :record-type type))]
    (neo/with-tx
      (loop [loc record-tree]
        (if (zip/end? loc)
          (let [root-id (get-in (zip/root loc) [:neo-data :id])]
            (find-record type root-id))
          (let [parent-loc (zip/up loc)
                parent-node (if parent-loc (zip/node parent-loc))
                parent-type (:record-type parent-node)
                child-node (zip/node loc)
                child-type (:record-type child-node)
                relation (domain/get-parent-relation parent-type child-type schema)
                rel-name (if relation (domain/get-relation-name relation))
                neo-data (if rel-name (assoc child-node rel-name (:neo-data parent-node)) child-node)
                record (if (:id neo-data)
                         (find-record child-type (:id neo-data))
                         (node->record
                          (create-audit (with-validation child-type neo-data #(create-node-with-default-relationships child-type neo-data nil))
                                        :create)
                          child-type))]
            (recur
             (zip/next (zip/replace loc (assoc child-node :neo-data record))))))))))

(defn update
  [type id data]
  (if-let [update-node (get-node-by-index type id)]
    (let [merged-data (merge (neo/props update-node) data)
          update-data (domain/validate-persistent-record
                       (clean-nils merged-data) type schema)]
      (neo/with-tx
        (with-validation type (assoc update-data :id id)
          #(neo/set-props! update-node update-data)
          validate-properties)
        (create-audit update-node :update))))
  (find-record type id))

(defn delete
  [type id]
  (let [node (get-node-by-index type id)
        props (assoc (neo/props node) :active false)
        child-types (filter (fn [child-type]
                              (some (fn [relation]
                                      (and (:deletable-by-parent relation) (= type (:related-to relation))))
                                    (domain/get-parent-relations child-type schema)))
                            (keys schema))
        child-nodes (flatten (map #(children-nodes-by-type node %) child-types))]
    (neo/with-tx
      (neo/set-props! node props)
      (doseq [child-node child-nodes] (delete (get-type child-node) (:id (neo/props child-node))))
      (create-audit node :destroy))
    true))

(defn relate-records
  [type1 id1 type2 id2 & {rel-name :rel-name}]
  (let [node1 (get-node-by-index type1 id1)
        node2 (get-node-by-index type2 id2)
        {:keys [dir rel]} (domain/get-directed-relationship type1 type2 schema :rel-name rel-name)]
    (cond
     (= :out dir) (create-relationship node1 rel node2)
     (= :in dir) (create-relationship node2 rel node1))
    (find-record type1 id1)))

(defn filter-rels
  [rels rel-name]
  (filter #(= (name rel-name) (.. % getType name)) rels))

(defn unrelate-records
  [type1 id1 type2 id2 & {rel-name :rel-name}]
  (let [node1 (get-node-by-index type1 id1)
        node2 (get-node-by-index type2 id2)
        rels (rels-between node1 node2)
        rels-count (count rels)
        rels (if rel-name 
               (let [{:keys [dir rel]} (domain/get-directed-relationship type1 type2 schema :rel-name rel-name)]
                 (filter-rels rels rel))
               rels)]
    (neo/with-tx
      (doseq [rel rels]
        (neo/delete! rel)))
    (find-record type1 id1)))

;; Yes, a terrible name.
(defn re-relate-records
  [type id old-type old-id new-id]
  (let [node (get-node-by-index type id)
        old-node (get-node-by-index old-type old-id)
        new-node (get-node-by-index old-type new-id)
        rel (first (rels-between node old-node))
        rel-type (neo/rel-type rel)
        start-node (neo/start-node rel)]
    (neo/with-tx
      (if rel-type
        (cond
         (= node start-node) (create-relationship node rel-type new-node)
         (= old-node start-node) (create-relationship new-node rel-type node)))
      (neo/delete! (rel-between node old-node rel-type)))
    (find-record type id)))