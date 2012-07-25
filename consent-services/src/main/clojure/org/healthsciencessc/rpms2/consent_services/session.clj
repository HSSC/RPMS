(ns org.healthsciencessc.rpms2.consent-services.session)

(def ^:dynamic *current-user*)


;; Functions for getting current user and user org
(defn current-user
  "Function used to get the current user from the request context.  Used for providing a user fetcher to runnable functions."
  ([] *current-user*)
  ([ctx] (get-in ctx [:session :current-user])))

(defn current-org
  ([] (:organization (current-user)))
  ([ctx] (get-in ctx [:session :current-user :organization])))

(defn current-org-id
  ([] (:id (current-org)))
  ([ctx] (get-in ctx [:session :current-user :organization :id])))
