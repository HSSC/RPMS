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

(defn collect-consents [])
(defn collect-endorsements [])


(defn persist
  "This should take finished forms, endorsements, and witness consents and persist them all"
  [])

(defn persist-session!
  "Main entry to persistence, grabs everything from session"
  []
  (session-put! :persist-meta (collect-meta-items))
  (session-put! :persist-consents (collect-consents))
  (session-put! :persist-endorsements (collect-endorsements)))
