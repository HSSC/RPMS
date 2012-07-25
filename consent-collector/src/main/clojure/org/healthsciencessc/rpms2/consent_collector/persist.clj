(ns
  ^{:doc "Persisting of collected forms from session to the DB"}
  org.healthsciencessc.rpms2.consent-collector.persist
  (:use org.healthsciencessc.rpms2.consent-collector.formutil
        [sandbar.stateful-session :only (session-get session-put!)]
        [org.healthsciencessc.rpms2.consent-collector.dsa-client :only (dsa-call)]))

(comment things to persist
         :witness-png
         :meta-items
         :endorsements
         :consents)

(defn protocol-version-owners
  "Gets the owning protocol versions of the passed in meta-item.
   Returns a vec of protocol version {:id}"
  [protocol-versions {id :id}]
  (let [metas->pvs (apply merge-with concat
                          (for [{pv-id :id metas :meta-items}
                                protocol-versions
                                {metaid :id}
                                metas]
                            {metaid [pv-id]}))]
    (vec (get metas->pvs id))))

(defn current-encounter
  []
  {:encounter (select-keys (session-get :encounter) [:id])})

(defn current-organization
  []
  {:organization (select-keys (:organization
                                (session-get :org-location)) [:id])})

(defn meta-items-with-pv
  [metas]
  (let [pvs (session-get :protocol-versions)]
    (for [m metas
          pvid (protocol-version-owners pvs m)]
      {:meta-item (select-keys m [:id])
       :value (:value m)
       :protocol-version {:id pvid}})))

(defn collect-meta-items
  []
  (let [metas (meta-items-with-pv (vals (session-get :all-meta-data)))]
    (map merge
         (repeat (current-encounter))
         (repeat (current-organization))
         metas)))

(defn assemble-consents
  []
  (flatten 
    (for [[pv-id answers] (session-get :finished-forms)]  ;; note pv-id is kw 
      (let [pvform (get-form-by-id (name pv-id))]
        (for [[widget-guid widget-value] answers]  ;; widget-guid is also a kw
          (let [wprops (widget-properties (widget-by-guid pvform (name widget-guid)))]
            (merge 
              (select-keys wprops [:endorsement :policy])
              {:value widget-value
               :protocol-version {:id (name pv-id)}})))))))

(defn flatten-consents 
  "Widgets refer to lists of consents, so this flattens them out"
  [xs]
  (for [{policies :policy :as consent} xs
        pol-id policies]
    (-> (assoc consent :policy {:id pol-id}
                   :consented (:value consent))
      (dissoc :value))))

(defn get-policies-endorsements
  [assembled-answers]
  (let [raw-consents (map merge
                          (repeat (current-encounter))
                          (repeat (current-organization))
                          assembled-answers)
        {:keys [policy endorsement]} (group-by #(some #{:policy :endorsement} (keys %)) raw-consents)]
    {:consents (flatten-consents policy)
     :consent-endorsements (map #(assoc % :endorsement
                                        {:id (get % :endorsement)})
                                endorsement)}))

(defn persist
  "This should take finished forms, endorsements, and witness consents and persist them all"
  [])

(defn add-witnesses [persisted-data]
  (if-let [witness-sig (session-get :witness-png)]
    (let [endorsements (map merge 
                            (repeat {:value witness-sig})
                            (repeat (current-encounter))
                            (repeat (current-organization))
                            (witnesses-needed))]
      (update-in persisted-data [:consent-endorsements]
                 #(concat % endorsements)))
    persisted-data))

(defn persist-session!
  "Main entry to persistence, grabs everything from session"
  []
  (let [pdata (-> (get-policies-endorsements (assemble-consents))
                (assoc :consent-meta-items (collect-meta-items))
                (add-witnesses))]
    (session-put! :persist-data pdata)
    (dsa-call :put-consent-collect pdata :encounter (:id (session-get :encounter)))))

