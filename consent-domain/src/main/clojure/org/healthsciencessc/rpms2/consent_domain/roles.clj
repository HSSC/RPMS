(ns org.healthsciencessc.rpms2.consent-domain.roles)

(defn- submap-of?
  [smallmap bigmap]
  (= smallmap (select-keys bigmap (keys smallmap))))

(defn- constraint-filter
  "The filter used to reduce mappings with additional constraints."
  [acc [constraint-key constraint-map]]
  (filter #(submap-of? 
             constraint-map 
             (get % constraint-key)) acc))

(defn get-role-mappings
  "Allows you to retrieve role mappings from a user based on constraints applied to the provided users role-mappings"
  [user & {:as constraints}]
  (reduce constraint-filter (:role-mappings user) constraints))

(defn has-role?
  [user role & {:as constraints}]
  (let [potential-role-mappings 
        (->>
          (:role-mappings user)
          (filter #(submap-of? role (:role %))))
        matching-mappings
        (reduce
          (fn [acc [constraint-key constraint-map]]
            (filter #(submap-of? 
                       constraint-map 
                       (get % constraint-key)) acc))
          potential-role-mappings
          constraints)]
     (if (seq matching-mappings)
       true)))

(defn superadmin?
  [user & constraints]
  (apply has-role? user {:code "sadmin"} constraints))

(defn admin?
  [user & constraints]
  (apply has-role? user {:code "admin"} constraints))

(defn consent-manager?
  [user & constraints]
  (apply has-role? user {:code "manage"} constraints))

(defn consent-collector?
  [user & constraints]
  (apply has-role? user {:code "collect"} constraints))

(defn protocol-designer?
  [user & constraints]
  (apply has-role? user {:code "design"} constraints))

(defn system?
  [user & constraints]
  (apply has-role? user {:code "csys"} constraints))


(defn consent-collector-mappings
  [user & constraints]
  (apply get-role-mappings user :role {:code "collect"} constraints))

(defn protocol-designer-mappings
  [user & constraints]
  (apply get-role-mappings user :role {:code "design"} constraints ))
