(ns org.healthsciencessc.rpms2.consent-admin.services
  (:require [clj-http.client :as client]
            [sandbar.stateful-session :as sess]
            [org.healthsciencessc.rpms2.consent-domain.types :as domain]
            [clojure.pprint :as pp]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.stacktrace :as st]
            [clojure.java.io :as io]
            [hiccup.util :as hutil])
  (:use [org.healthsciencessc.rpms2.consent-admin.config]
        [org.healthsciencessc.rpms2.consent-domain.roles :only (has-role?)]
        [clojure.data.json :only (json-str read-json)]))

;; Request And Response Support

(defn service-error?
  "Any response that isn't HTTP 200 from consent-services will
  assoc the response as metadata."
  [m]
  (not (nil? (meta m))))

(defn- full-url
  "Creates the absolute URL to the services using the configured path to services."
  [url params]
  (.toString (hutil/url (:services.url config) url params)))

(defn- credentials
  "Creats a map of all the header items needed for basic authentication."
  ([] (credentials
        (select-keys (sess/session-get :user)
                     [:username :password])))
  ([user] {:basic-auth [(:username user) (:password user)]}))

(defn- defaults
  "Defines any defaults that will be placed on all requests."
  []
  {:as :clojure ;; this tells clj-http to read-string on the body
   :content-type "application/clojure" ;; tells our services to serve the Right Stuff
   :throw-exceptions false})

(defn- meta-failure-handler
  "Simple handler that attaches error and response information as metadata to a returned map if request is unsuccessful."
  [coll message]
  (fn [resp]
    (if (not= (:status resp) 200)
      (let [m (assoc resp :message message)]
        (with-meta coll m)))))

(def ^:private failure-handler
  (meta-failure-handler {} "Failed to get a valid response from the consent services module."))

(defn- handle-response
  "Handle the response from all requests.
   xs is a list of handlers to casacde through."
  [resp xs]
  (let [handlers (concat xs [failure-handler #(:body %)])]
    (first (drop-while (complement identity)
                (map #(% resp)
                     handlers)))))

;; The HTTP Request Functions

(defn- DO
  "Performs the actual http request, applying any handlers that are needed."
  [method url settings handlers]
  (try
    (handle-response (method url settings) handlers)
    (catch Exception e
      ;; Handle Some What The Fudge Situations
      (st/print-stack-trace e)
      nil)))

(defn- GET
  "Makes a get request to the server"
  [url params & handlers]
  (DO client/get
      (full-url url params)
      (merge (credentials) (defaults))
      handlers))

(defn- POST
  "Makes a post request to the server"
  [url params form body & handlers]
  (DO client/post
      (full-url url params)
      (merge {:body body :form-params form} (credentials) (defaults))
      handlers))

(defn- PUT
  "Makes a put request to the server"
  [url params form body & handlers]
  (DO client/put
      (full-url url params)
      (merge {:body body :form-params form} (credentials) (defaults))
      handlers))

(defn- DELETE
  "Makes a delete request to the server"
  [url params form body & handlers]
  (DO client/delete
      (full-url url params)
      (merge {:body body :form-params form} (credentials) (defaults))
      handlers))

;; Define Public Specific Integration Functions.

(defn authenticate
  "Calls the authentication process within the consent services."
  [username password]
  (DO client/get
      (full-url "/security/authenticate" {})
      (merge (credentials {:username username :password password}) (defaults))
      [(fn [r] (if (= 200 (:status r))
                 (assoc (:body r)
                        :password password)
                 :invalid))]))

;; Domain utilities

(defn merge-with-curr-org
  [m]
  (assoc m :organization 
         (:organization (sess/session-get :user))))

;; LOCATIONS
(defn get-locations
  []
  (GET "/security/locations" {}))

(defn get-location
  [id]
  (GET "/security/location" {:location id}))

(defn add-location
  [l]
  (PUT "/security/location"
        nil
        nil
        (with-out-str (prn (merge-with-curr-org l)))))

(defn edit-location
  [id l]
  (POST "/security/location"
        {:location id}
        nil
        (with-out-str (prn l))))

(defn delete-location
  [id]
  (DELETE "/security/location" {:location id} nil nil))

;; USERS
(defn get-users
  []
  (GET "/security/users" {}))

(defn get-user
  [id]
  (GET "/security/user" {:user id}))

(defn add-user
  [u]
  (PUT "/security/user"
        nil
        nil
        (with-out-str (prn u))))

(defn delete-user
  [id]
  (DELETE "/security/user" {:user id} nil nil))

(defn edit-user
  [id u]
  (let [password (:password u)
        user (if (or (nil? password) (str/blank? password))
               (dissoc u :password))]
    (POST "/security/user"
          {:user id}
          nil
          (with-out-str (prn user)))))

;; ORGANIZATIONS
(defn delete-organization
  [id]
  (DELETE "/security/organization" {:organization id} nil nil))

(defn get-organizations
  [_]
  (GET "/security/organizations" {}))

(defn add-organization
  [o]
  (PUT "/security/organization"
       nil
       nil
       (with-out-str (prn o))))

(defn edit-organization
  [id o]
  (POST "/security/organization"
        {:organization id}
        nil
        (with-out-str (prn o))))

(defn get-organization
  [id]
  (GET "/security/organization" {:organization id}))

;; ROLES
(defn get-roles
  []
  (GET "/security/roles" {}))

(defn get-role
  [id]
  (GET "/security/role" {:role id}))

(defn add-role
  [r]
  (PUT "/security/role"
        nil
        nil
        (with-out-str (prn (merge-with-curr-org r)))))

(defn edit-role
  [id r]
  (POST "/security/role"
        {:role id}
        nil
        (with-out-str (prn r))))

(defn delete-role
  [id]
  (DELETE "/security/role" {:role id} nil nil))

;; GROUPS
(defn get-groups
  [_]
  (GET "/security/groups" {}))

(defn get-group
  [id]
  (GET "/security/group" {:group id}))

(defn get-group-members
  [gid]
  (let [group-members (GET "/security/users" {:group gid})
        all-users (get-users)]
   {:in group-members :out (apply set/difference 
                                  (map set [all-users group-members]))}))

(defn delete-group
  [id]
  (DELETE "/security/group" {:group id} nil nil))

(defn add-group
  [g]
  (PUT "/security/group"
        nil
        nil
        (with-out-str (prn (merge-with-curr-org g)))))

(defn edit-group
  [id g]
  (POST "/security/group"
        {:group id}
        nil
        (with-out-str (prn g))))

(defn filter-direct-roles
  [m filter-key]
  (update-in m [:role-mappings]
             #(vec (filter filter-key %))))

(defn fetch-assignee
  [type id]
  (case type
    :user (get-user id)
    :group (get-group id)))

(defn get-assigned-roles 
  [id type]
  {:pre [(or (= type :user)
             (= type :group))]}
  (let [roles (:role-mappings (fetch-assignee type id))]
    (group-by #(some #{:user :group} (keys %)) roles)))

(defn- add-rolemapping-helper [assignee-id
                 assignee-type
                 role-id
                 loc-id]
  (let [location (if loc-id
                   {:location loc-id})
        qry-params (merge 
                     {assignee-type assignee-id
                      :role role-id}
                     location)]
    (case assignee-type
      :user
      (PUT "/security/userrole"
           qry-params nil nil)
      :group
      (PUT "/security/grouprole"
           qry-params nil nil))))

(defn add-rolemapping
  "location key is optional, assignee-type is a :user or :group"
  [{:keys [assignee-id
           assignee-type
           role-id
           loc-id]}]
  {:pre [(and role-id assignee-id
              (or (= assignee-type :user)
                  (= assignee-type :group)))]}
  (let [assignee (-> (fetch-assignee assignee-type assignee-id)
                   (filter-direct-roles assignee-type))
        redundant-role? (if loc-id   ;; this checks that the user/group doesn't already have that role
                          #(has-role? % {:id role-id} :location {:id loc-id})
                          #(has-role? % {:id role-id}))]
    (if-not (redundant-role? assignee)
      (add-rolemapping-helper assignee-id assignee-type role-id loc-id))))

(defn remove-rolemapping
  [role-mapping-id]
  (DELETE "/security/role-mapping" 
          {:role-mapping role-mapping-id}
          nil nil))

(defn add-group-member
  [g u]
  (PUT "/security/usergroup"
    {:group g
     :user u}
    nil
    nil))

(defn remove-group-member
  [g u]
  (DELETE "/security/usergroup"
    {:group g
     :user u}
    nil
    nil))

(defn add-admin
  [u]
  (let [admin-id (-> (filter #(= (:code %)
                             domain/code-role-admin)
                             (get-roles))
                   first
                   :id)
        usr-resp (add-user u)]
    (if (:id usr-resp)
      (let [role-params {:user (:id usr-resp)
                         :role admin-id}]
            (PUT "/security/userrole"
               role-params
               nil
               nil))
      usr-resp)))  ;; pass this back directly

;; LANGUAGES
(defn get-languages
  []
  (GET "/library/languages" {}))

(defn get-language
  [language-id]
  (GET "/library/language" {:language language-id}))

(defn add-language
  [r]
  (PUT "/library/language"
        nil
        nil
        (with-out-str (prn (merge-with-curr-org r)))))

(defn edit-language
  [language-id data]
  (POST "/library/language"
        {:language language-id}
        nil
        (with-out-str (prn data))))

(defn delete-language
  [language-id]
  (DELETE "/library/language" {:language language-id} nil nil))

;; PROTOCOLS
(defn get-protocols
  "Gets all of the available protocols within a location."
  [location-id]
  (GET "/protocols" {:location location-id}))

(defn get-protocol
  "Gets a single protocol by it's ID."
  [protocol-id]
  (GET "/protocol" {:protocol protocol-id}))

(defn add-protocol
  "Adds a new protocol to a location."
  [data]
  (PUT "/protocol"
        nil
        nil
        (with-out-str (prn data))))

(defn delete-protocol
  "Deletes a new protocol from a location."
  [protocol-id]
  (DELETE "/protocol"
        {:protocol protocol-id}
        nil
        nil))

(defn update-protocol
  "Updates a protocol with data changes."
  [protocol-id data]
  (POST "/protocol"
        {:protocol protocol-id}
        nil
        (with-out-str (prn data))))


;; PROTOCOLS
(defn get-protocol-versions
  "Gets all of the available protocol versions for a protocol."
  [protocol-id]
  (GET "/protocol/versions" {:protocol protocol-id}))

(defn get-protocol-version
  "Gets a single protocol version by it's ID."
  [protocol-version-id]
  (GET "/protocol/version" {:protocol-version protocol-version-id}))

(defn add-protocol-version
  "Adds a new protoco version to a protocol."
  [data]
  (PUT "/protocol/version"
        nil
        nil
        (with-out-str (prn data))))

(defn delete-protocol-version
  "Deletes a new protocol version from a protocol."
  [protocol-version-id]
  (DELETE "/protocol/version"
        {:protocol-version protocol-version-id}
        nil
        nil))

(defn update-protocol-version
  "Updates a protocol version with data changes."
  [protocol-version-id data]
  (POST "/protocol/version"
        {:protocol-version protocol-version-id}
        nil
        (with-out-str (prn data))))

(defn publish-protocol-version
  "Updates a protocol version with data changes."
  [protocol-version-id]
  (POST "/protocol/publish"
        {:protocol-version protocol-version-id}
        nil
        nil))

(defn retire-protocol-version
  "Updates a protocol version with data changes."
  [protocol-version-id]
  (POST "/protocol/retire"
        {:protocol-version protocol-version-id}
        nil
        nil))

(defn draft-protocol-version
  "Updates a protocol version with data changes."
  [protocol-version-id]
  (POST "/protocol/draft"
        {:protocol-version protocol-version-id}
        nil
        nil))


(defn assign-language-to-protocol-version
  [language-id protocol-version-id]
  (PUT "/protocol/version/language" 
       {:language language-id :protocol-version protocol-version-id} nil nil))

(defn assign-endorsement-to-protocol-version
  [endorsement-id protocol-version-id]
  (PUT "/protocol/version/endorsement" 
       {:endorsement endorsement-id :protocol-version protocol-version-id} nil nil))

(defn assign-meta-item-to-protocol-version
  [meta-item-id protocol-version-id]
  (PUT "/protocol/version/meta-item" 
       {:meta-item meta-item-id :protocol-version protocol-version-id} nil nil))

(defn assign-policy-to-protocol-version
  [policy-id protocol-version-id]
  (PUT "/protocol/version/policy" 
       {:policy policy-id :protocol-version protocol-version-id} nil nil))

(defn remove-language-from-protocol-version
  [language-id protocol-version-id]
  (DELETE "/protocol/version/language" 
          {:language language-id :protocol-version protocol-version-id} nil nil))

(defn remove-endorsement-from-protocol-version
  [endorsement-id protocol-version-id]
  (DELETE "/protocol/version/endorsement" 
          {:endorsement endorsement-id :protocol-version protocol-version-id} nil nil))

(defn remove-meta-item-from-protocol-version
  [meta-item-id protocol-version-id]
  (DELETE "/protocol/version/meta-item" 
          {:meta-item meta-item-id :protocol-version protocol-version-id} nil nil))

(defn remove-policy-from-protocol-version
  [policy-id protocol-version-id]
  (DELETE "/protocol/version/policy" 
          {:policy policy-id :protocol-version protocol-version-id} nil nil))

;; Endorsement Types
(defn get-endorsement-types
  [_]
  (GET "/library/endorsement/types" {}))

(defn get-endorsement-type
  [id]
  (GET "/library/endorsement/type" {:endorsement-type id}))

(defn add-endorsement-type
  [o]
  (PUT "/library/endorsement/type"
       nil
       nil
       (with-out-str (prn o))))

(defn edit-endorsement-type
  [id o]
  (POST "/library/endorsement/type"
        {:endorsement-type id}
        nil
        (with-out-str (prn o))))

(defn delete-endorsement-type
  [id]
  (DELETE "/library/endorsement/type" {:endorsement-type id} nil nil))

;; Endorsements
(defn get-endorsements
  [_]
  (GET "/library/endorsements" {}))

(defn get-endorsement
  [id]
  (GET "/library/endorsement" {:endorsement id}))

(defn add-endorsement
  [o]
  (PUT "/library/endorsement"
       nil
       nil
       (with-out-str (prn o))))

(defn edit-endorsement
  [id o]
  (POST "/library/endorsement"
        {:endorsement id}
        nil
        (with-out-str (prn o))))

(defn delete-endorsement
  [id]
  (DELETE "/library/endorsement" {:endorsement id} nil nil))

(defn assign-endorsement-type
  [id, old-id, new-id]
  (POST "/library/endorsement/type/assign" {:endorsement id :endorsement-type old-id :assign-type new-id} nil nil))

;; Meta Items
(defn get-meta-items
  []
  (GET "/library/meta-items" {}))

(defn get-meta-item
  [meta-item-id]
  (GET "/library/meta-item" {:meta-item meta-item-id}))

(defn add-meta-item
  [data]
  (PUT "/library/meta-item"
       nil
       nil
       (with-out-str (prn data))))

(defn edit-meta-item
  [meta-item-id data]
  (POST "/library/meta-item"
        {:meta-item meta-item-id}
        nil
        (with-out-str (prn data))))

(defn delete-meta-item
  [meta-item-id]
  (DELETE "/library/meta-item" {:meta-item meta-item-id} nil nil))


;; Policy Definitions
(defn get-policy-definitions
  [_]
  (GET "/library/policy-definitions" {}))

(defn get-policy-definition
  [id]
  (GET "/library/policy-definition" {:policy-definition id}))

(defn add-policy-definition
  [o]
  (PUT "/library/policy-definition"
       nil
       nil
       (with-out-str (prn o))))

(defn edit-policy-definition
  [id o]
  (POST "/library/policy-definition"
        {:policy-definition id}
        nil
        (with-out-str (prn o))))

(defn delete-policy-definition
  [id]
  (DELETE "/library/policy-definition" {:policy-definition id} nil nil))

;; Policy
(defn get-policys
  []
  (GET "/library/policies" {}))

(defn get-policy
  [policy-id]
  (GET "/library/policy" {:policy policy-id}))

(defn add-policy
  [data]
  (PUT "/library/policy"
       nil
       nil
       (with-out-str (prn data))))

(defn edit-policy
  [policy-id data]
  (POST "/library/policy"
        {:policy policy-id}
        nil
        (with-out-str (prn data))))

(defn delete-policy
  [policy-id]
  (DELETE "/library/policy" {:policy policy-id} nil nil))

;; Forms
(defn get-form
  [form-id]
  (GET "/library/form" {:form form-id}))


;; text-i18n
(defn add-text-i18n
  [parent-type parent-id property data]
  (PUT "/library/text-i18n"
       {:parent-type parent-type :parent-id parent-id :property property}
       nil
       (with-out-str (prn data))))

(defn update-text-i18n
  [parent-type parent-id property text-i18n-id data]
  (POST "/library/text-i18n"
        {:parent-type parent-type :parent-id parent-id :property property :text-i18n text-i18n-id}
        nil
        (with-out-str (prn data))))

(defn delete-text-i18n
  [parent-type parent-id property text-i18n-id]
  (DELETE "/library/text-i18n" 
          {:parent-type parent-type :parent-id parent-id :property property :text-i18n text-i18n-id} nil nil))

;; Designer Services
(defn update-designer-form
  [protocol-version-id form-id data]
  (POST "/designer/form"
        {:protocol-version protocol-version-id :form form-id}
        nil
        (with-out-str (prn data))))

(defn create-designer-form-widget
  [protocol-version-id form-id widget-id data]
  (let [form-props (if form-id {:form form-id} {})
        widget-props (if widget-id {:widget widget-id} {})]
    (PUT "/designer/form/widget"
         (merge {:protocol-version protocol-version-id} form-props widget-props)
         nil
         (with-out-str (prn data)))))

(defn update-designer-form-widget
  [protocol-version-id widget-id data]
  (POST "/designer/form/widget"
        {:protocol-version protocol-version-id :widget widget-id}
        nil
        (with-out-str (prn data))))

(defn delete-designer-form-widget
  [protocol-version-id widget-id]
  (DELETE "/designer/form/widget"
          {:protocol-version protocol-version-id :widget widget-id} nil nil))