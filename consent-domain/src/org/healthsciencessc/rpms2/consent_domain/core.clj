(ns org.healthsciencessc.rpms2.consent-domain.core
  (:use [slingshot.slingshot :only (throw+)]))

(def base
  {:id {:persisted false}})

(def person
  {:first-name {:persisted true}
   :middle-name {:persisted true}
   :last-name {:persisted true}
   :title {:persisted true}
   :suffix {:persisted true}})

(def data-defs
  {"organization" {:attributes
                  (merge base
                         {:name {:required true :persisted true}
                          :code {:persisted true}
                          :location-label {:persisted true}
                          :protocol-label {:persisted true}})}
   
   "user" {:attributes (merge base
                              person
                              {:username {:persisted true :required true}
                               :password {:persisted true :required true :validation (fn [password] (< 5 (count password)))}
                               :salt {:persisted true :required true}})
           :relations [{:type :belongs-to :related-to "organization" :relationship :owned-by}
                       {:type :has-many :related-to "role-mapping"}]}
   
   "role" {:attributes (merge base
                              {:name {:persisted true :required true}
                               :code {:persisted true}})
           :relations [{:type :belongs-to :related-to "organization" :relationship :owned-by}]}

   "location" {:attributes (merge base
                                  {:name {:persisted true}
                                   :code {:persisted true}
                                   :protocol-label {:persisted true}})
               :relations [{:type :belongs-to :related-to "organization" :relationship :owned-by}]}

   "role-mapping" {:attributes base
                   :relations [{:type :belongs-to :related-to "user" :relationship :has-user :omit true}
                               {:type :belongs-to :related-to "role" :relationship :has-role}
                               {:type :belongs-to :related-to "organization" :relationship :has-organization}
                               {:type :belongs-to :related-to "location" :relationship :has-location}]}
   })

(defn get-relations
  [type]
  (filter #(not (:omit %)) (get-in data-defs [type :relations])))

(defn get-parent-relations
  [type]
  (filter #(= :belongs-to (:type %)) (get-relations type)))

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

(defmulti relation-name->key :type)

(defmethod relation-name->key :belongs-to
  [relation]
  (keyword (:related-to relation)))

(defmethod relation-name->key :has-many
  [relation]
  (keyword (str (:related-to relation) "s")))

(defn all-valid-keys
  [{:keys [attributes relations]}]
  (into (keys attributes)
        (map relation-name->key relations)))

(defn validate
  [record]
  record)

(defn validate-record
  [{type :type :as record}]
  (->> (data-defs type)
      all-valid-keys
      (select-keys record)
      validate))

(defn validate-persistant-record
  [record type]
  (select-keys record (persisted-attrs (data-defs type))))
