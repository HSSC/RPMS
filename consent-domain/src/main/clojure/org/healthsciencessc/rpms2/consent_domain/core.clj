(ns org.healthsciencessc.rpms2.consent-domain.core
  (:use [slingshot.slingshot :only (throw+)]))

(def base
  {:id {:persisted false}
   :active {:persisted true :omit true}})

(def person
  {:first-name {:persisted true}
   :middle-name {:persisted true}
   :last-name {:persisted true}
   :title {:persisted true}
   :suffix {:persisted true}})

(def default-data-defs
  {"organization" {:attributes
                   (merge base
                          {:name {:required true :persisted true}
                           :code {:persisted true}
                           :location-label {:persisted true}
                           :protocol-label {:persisted true}})}

   "user" {:attributes (merge base
                              person
                              {:username {:persisted true :required true}
                               :password {:omit true :persisted true :required true :validation (fn [password] (< 5 (count password)))}})
           :relations [{:type :belongs-to :related-to "organization" :relationship :owned-by}
                       {:type :belongs-to :related-to "group" :relationship :in-group}
                       {:type :has-many :related-to "role-mapping"}
                       {:type :has-many-through :related-to "role-mapping" :relation-path ["group"]}]}

   "role" {:attributes (merge base
                              {:name {:persisted true :required true}
                               :code {:persisted true}})
           :relations [{:type :belongs-to :related-to "organization" :relationship :owned-by}]}

   "language" {:attributes (merge base
                              {:name {:persisted true :required true}
                               :code {:persisted true}})
               :relations [{:type :belongs-to :related-to "organization" :relationship :owned-by}]}

   "location" {:attributes (merge base
                                  {:name {:persisted true}
                                   :code {:persisted true}
                                   :protocol-label {:persisted true}})
               :relations [{:type :belongs-to :related-to "organization" :relationship :owned-by}]}

   "group" {:attributes (merge base
                               {:name {:persisted true}})
            :relations [{:type :belongs-to :related-to "organization" :relationship :owned-by}]}
   
   "consenter" {:attributes (merge base
                                   person
                                  {:consenter-id {:persisted true :required true}
                                   :gender {:persisted true :required true}
                                   :dob {:persisted true :required true}
                                   :zipcode {:persisted true :required true}})
               :relations [{:type :belongs-to :related-to "organization" :relationship :owned-by}
                           {:type :belongs-to :related-to "location" :relationship :in-location}]}

   "role-mapping" {:attributes base
                   :relations [{:type :belongs-to :related-to "user" :relationship :has-user :omit true}
                               {:type :belongs-to :related-to "group" :relationship :has-group :omit true}
                               {:type :belongs-to :related-to "role" :relationship :has-role}
                               {:type :belongs-to :related-to "organization" :relationship :has-organization}
                               {:type :belongs-to :related-to "location" :relationship :has-location}]}
   })

(defn get-relations
  [type data-defs]
  (get-in data-defs [type :relations]))

(defn get-parent-relations
  [type data-defs]
  (filter #(= :belongs-to (:type %)) (get-relations type data-defs)))

(defn get-parent-relation
  [parent-type child-type data-defs]
  (first (filter #(= parent-type (:related-to %)) (get-parent-relations child-type data-defs))))

(defn record-relations
  [type data-defs]
  (remove :omit (get-relations type data-defs)))

(defn attr-search
  [term]
  (fn [[attr desc]]
    (if (term desc)
      attr)))

(defn required-attrs
  [data-def]
  (keep (attr-search :required)
        (:attributes data-def)))

(defn persisted-attrs
  [data-def]
  (keep (attr-search :persisted)
        (:attributes data-def)))

(defmulti relation-name->key
  (fn [relation]
    ((:type relation)
     {:belongs-to :parent
      :has-many :children
      :has-many-through :children})))

(defmethod relation-name->key :parent
  [relation]
  (keyword (:related-to relation)))

(defmethod relation-name->key :children
  [relation]
  (keyword (str (:related-to relation) "s")))

(defn all-valid-keys
  [{:keys [attributes relations]}]
  (concat (map first (remove #(-> % second :omit) attributes))
          (map relation-name->key relations)))

(defn get-relationship-from-child
  [parent-type child-type data-defs]
  (:relationship (first (filter #(= parent-type (:related-to %))
                                (:relations (data-defs child-type))))))

(defn get-directed-relation
  [start-type end-type data-defs]
  (let [parent-dir (get-parent-relation start-type end-type data-defs)
        child-dir (get-parent-relation end-type start-type data-defs)]
    (cond
     parent-dir {:dir :in :rel (:relationship parent-dir)}
     child-dir {:dir :out :rel (:relationship child-dir)})))

(defn validate
  [record]
  record)

(defn validate-record
  [record type data-defs]
  (->> (data-defs type)
       all-valid-keys
       (select-keys record)
       validate))

(defn validate-persistent-record
  [record type data-defs]
  (select-keys record (persisted-attrs (data-defs type))))
