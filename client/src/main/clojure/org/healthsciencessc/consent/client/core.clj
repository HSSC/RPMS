(ns org.healthsciencessc.consent.client.core
  (:require [clj-http.client :as client]
            [org.healthsciencessc.consent.client.whoami :as whoami]
            [clojure.tools.logging :as logging]
            [org.healthsciencessc.consent.domain.credentials :as credentials])
  (:use [clojure.string :only [blank?]]
        [org.healthsciencessc.consent.domain.roles :only [has-role?]]
        [org.healthsciencessc.consent.client.url :only [url]]))

;; Request And Response Support
(defn service-error?
  "Any response that isn't HTTP 200 from consent-services will
  assoc the response as metadata."
  [m]
  (not (nil? (meta m))))

(defn- credentials
  "Creats a map of all the header items needed for basic authentication."
  ([] (credentials (whoami/get-identity)))
  ([id] {:basic-auth [(credentials/wrap-username id) (credentials/wrap-password id)]}))

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
        (logging/error "Consent Service Call:" resp)
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
      (logging/error e)
      nil)))

(defn- GET
  "Makes a get request to the server"
  [path params & handlers]
  (DO client/get
      (url path params)
      (merge (credentials) (defaults))
      handlers))

(defn- POST
  "Makes a post request to the server"
  [path params form body & handlers]
  (DO client/post
      (url path params)
      (merge {:body body :form-params form} (credentials) (defaults))
      handlers))

(defn- PUT
  "Makes a put request to the server"
  [path params form body & handlers]
  (DO client/put
      (url path params)
      (merge {:body body :form-params form} (credentials) (defaults))
      handlers))

(defn- DELETE
  "Makes a delete request to the server"
  [path params form body & handlers]
  (DO client/delete
      (url path params)
      (merge {:body body :form-params form} (credentials) (defaults))
      handlers))

;; Define Public Specific Integration Functions.

;; AUTHENTICATION
(defn authenticate
  "Calls the authentication process within the consent services."
  ([username password] (authenticate username password "local"))
  ([username password realm]
    (DO client/get
        (url "/security/authenticate" {})
        (merge (credentials {:username username :password password :realm realm}) (defaults))
        [(fn [r] 
           (if (= 200 (:status r))
             (let [user (:body r)
                   id (:identity user)]
               (whoami/put-identity! (assoc id :password password))
               (dissoc user :identity))
             :invalid))])))


;; ORGANIZATIONS
(defn delete-organization
  [organization-id]
  (DELETE "/security/organization" {:organization organization-id} nil nil))

(defn get-organizations
  [_]
  (GET "/security/organizations" {}))

(defn add-organization
  [data]
  (PUT "/security/organization"
       nil
       nil
       (with-out-str (prn data))))

(defn update-organization
  [organization-id data]
  (POST "/security/organization"
        {:organization organization-id}
        nil
        (with-out-str (prn data))))

(defn get-organization
  [organization-id]
  (GET "/security/organization" {:organization organization-id}))

(defn assign-language-to-organization
  [language-id organization-id]
  (PUT "/security/organization/language" 
       {:language language-id :organization organization-id} nil nil))


;; LOCATIONS
(defn get-locations
  []
  (GET "/security/locations" {}))

(defn get-location
  [location-id]
  (GET "/security/location" {:location location-id}))

(defn add-location
  [org-id data]
  (PUT "/security/location"
        {:organization org-id}
        nil
        (with-out-str (prn data))))

(defn update-location
  [location-id data]
  (POST "/security/location"
        {:location location-id}
        nil
        (with-out-str (prn data))))

(defn delete-location
  [location-id]
  (DELETE "/security/location" {:location location-id} nil nil))

;; USERS
(defn get-users
  [org-id]
  (GET "/security/users" {:organization org-id}))

(defn get-user
  [user-id]
  (GET "/security/user" {:user user-id}))

(defn add-user
  [org-id data]
  (PUT "/security/user"
        {:organization org-id}
        nil
        (with-out-str (prn data))))

(defn delete-user
  [user-id]
  (DELETE "/security/user" {:user user-id} nil nil))

(defn update-user
  [user-id data]
  (let [password (:password data)
        user (if (or (nil? password) (blank? password))
               (dissoc data :password))]
    (POST "/security/user"
          {:user user-id}
          nil
          (with-out-str (prn user)))))

;; ROLES
(defn get-roles
  []
  (GET "/security/roles" {}))

(defn get-role
  [role-id]
  (GET "/security/role" {:role role-id}))

(defn add-role
  [org-id data]
  (PUT "/security/role"
        {:organization org-id}
        nil
        (with-out-str (prn data))))

(defn update-role
  [role-id data]
  (POST "/security/role"
        {:role role-id}
        nil
        (with-out-str (prn data))))

(defn delete-role
  [role-id]
  (DELETE "/security/role" {:role role-id} nil nil))

;; GROUPS
(defn get-groups
  [_]
  (GET "/security/groups" {}))

(defn get-group
  [group-id]
  (GET "/security/group" {:group group-id}))

(defn delete-group
  [group-id]
  (DELETE "/security/group" {:group group-id} nil nil))

(defn add-group
  [org-id data]
  (PUT "/security/group"
        {:organization  org-id}
        nil
        (with-out-str (prn data))))

(defn update-group
  [group-id data]
  (POST "/security/group"
        {:group group-id}
        nil
        (with-out-str (prn data))))

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

(defn delete-role-mapping
  [role-mapping-id]
  (DELETE "/security/role-mapping" 
          {:role-mapping role-mapping-id}
          nil nil))

(defn get-group-members
  [group-id]
  (GET "/security/group/members" {:group group-id}))

(defn get-group-nonmembers
  [group-id]
  (GET "/security/group/nonmembers" {:group group-id}))

(defn add-group-member
  [group-id user-id]
  (PUT "/security/group/member"
    {:group group-id
     :user user-id}
    nil
    nil))

(defn remove-group-member
  [group-id user-id]
  (DELETE "/security/group/member"
    {:group group-id
     :user user-id}
    nil
    nil))

(defn add-admin
  [org-id data]
  (PUT "/security/user/admin"
        {:organization org-id}
        nil
        (with-out-str (prn data))))

;; LANGUAGES
(defn get-languages
  [org-id]
  (GET "/library/languages" {:organization org-id}))

(defn get-language
  [language-id]
  (GET "/library/language" {:language language-id}))

(defn add-language
  [org-id data]
  (PUT "/library/language"
        {:organization org-id}
        nil
        (with-out-str (prn data))))

(defn update-language
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
  [location-id data]
  (PUT "/protocol"
        {:location location-id}
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
  [protocol-id data]
  (PUT "/protocol/version"
        {:protocol protocol-id}
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

(defn clone-protocol-version
  "Clones a published protocol version into a new draft."
  [protocol-version-id]
  (POST "/protocol/version/clone" {:protocol-version protocol-version-id} nil nil))

(defn publish-protocol-version
  "Updates a protocol version with data changes."
  [protocol-version-id]
  (POST "/protocol/version/publish"
        {:protocol-version protocol-version-id}
        nil
        nil))

(defn retire-protocol-version
  "Updates a protocol version with data changes."
  [protocol-version-id]
  (POST "/protocol/version/retire"
        {:protocol-version protocol-version-id}
        nil
        nil))

(defn draft-protocol-version
  "Updates a protocol version with data changes."
  [protocol-version-id]
  (POST "/protocol/version/draft"
        {:protocol-version protocol-version-id}
        nil
        nil))

(defn submit-protocol-version
  "Updates a protocol version with data changes."
  [protocol-version-id]
  (POST "/protocol/version/submit"
        {:protocol-version protocol-version-id}
        nil
        nil))

(defn export-protocol-version
  "Gets the protocol version XML rendition."
  [protocol-version-id]
  (DO client/get
      (url "/protocol/version/export" {:protocol-version protocol-version-id})
      (merge (credentials) {:content-type "application/xml" :throw-exceptions false})
      []))

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

(defn get-published-protocol-versions
  [location-id]
  (GET "/protocol/versions/published" {:location location-id}))

(defn get-published-protocol-versions-meta
  [protocol-version-ids]
  (GET "/protocol/versions/published/meta" {:protocol-version protocol-version-ids}))

(defn get-published-protocol-versions-form
  [protocol-version-ids language-id]
  (GET "/protocol/versions/published/form" {:protocol-version protocol-version-ids :language language-id}))

;; Endorsement Types
(defn get-endorsement-types
  [org-id]
  (GET "/library/endorsement/types" {:organization org-id}))

(defn get-endorsement-type
  [endorsement-type-id]
  (GET "/library/endorsement/type" {:endorsement-type endorsement-type-id}))

(defn add-endorsement-type
  [org-id data]
  (PUT "/library/endorsement/type"
        {:organization org-id}
       nil
       (with-out-str (prn data))))

(defn update-endorsement-type
  [endorsement-type-id data]
  (POST "/library/endorsement/type"
        {:endorsement-type endorsement-type-id}
        nil
        (with-out-str (prn data))))

(defn delete-endorsement-type
  [endorsement-type-id]
  (DELETE "/library/endorsement/type" {:endorsement-type endorsement-type-id} nil nil))

;; Endorsements
(defn get-endorsements
  [org-id]
  (GET "/library/endorsements" {:organization org-id}))

(defn get-endorsement
  [endorsement-id]
  (GET "/library/endorsement" {:endorsement endorsement-id}))

(defn add-endorsement
  [org-id data]
  (PUT "/library/endorsement"
       {:organization org-id}
       nil
       (with-out-str (prn data))))

(defn update-endorsement
  [endorsement-id data]
  (POST "/library/endorsement"
        {:endorsement endorsement-id}
        nil
        (with-out-str (prn data))))

(defn delete-endorsement
  [endorsement-id]
  (DELETE "/library/endorsement" {:endorsement endorsement-id} nil nil))

(defn assign-endorsement-type
  [id, old-id, new-id]
  (POST "/library/endorsement/endorsement/type" {:endorsement id :endorsement-type old-id :assign-type new-id} nil nil))

;; Meta Items
(defn get-meta-items
  [org-id]
  (GET "/library/meta-items" {:organization org-id}))

(defn get-meta-item
  [meta-item-id]
  (GET "/library/meta-item" {:meta-item meta-item-id}))

(defn add-meta-item
  [org-id data]
  (PUT "/library/meta-item"
       {:organization org-id}
       nil
       (with-out-str (prn data))))

(defn update-meta-item
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
  [org-id]
  (GET "/library/policy-definitions" {:organization org-id}))

(defn get-policy-definition
  [policy-definition-id]
  (GET "/library/policy-definition" {:policy-definition policy-definition-id}))

(defn add-policy-definition
  [org-id data]
  (PUT "/library/policy-definition"
       {:organization org-id}
       nil
       (with-out-str (prn data))))

(defn update-policy-definition
  [policy-definition-id data]
  (POST "/library/policy-definition"
        {:policy-definition policy-definition-id}
        nil
        (with-out-str (prn data))))

(defn delete-policy-definition
  [policy-definition-id]
  (DELETE "/library/policy-definition" {:policy-definition policy-definition-id} nil nil))

;; Policy
(defn get-policies
  [org-id]
  (GET "/library/policies" {:organization org-id}))

(defn get-policy
  [policy-id]
  (GET "/library/policy" {:policy policy-id}))

(defn add-policy
  [org-id data]
  (PUT "/library/policy"
       {:organization org-id}
       nil
       (with-out-str (prn data))))

(defn update-policy
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

;; Consent History Services
(defn find-consenters
  [params]
  (GET "/consent/consenters" params))

(defn find-consenters-by-location
  [location-id criteria]
  (GET "/consent/consenters" (assoc criteria :location location-id)))

(defn get-consenter
  [consenter-id]
  (GET "/consent/consenter" {:consenter consenter-id}))

(defn add-consenter
  [location-id data]
  (PUT "/consent/consenter"
       {:location location-id}
       nil
       (with-out-str (prn data))))

;; Consents
(defn get-consents
  [consenter-id]
  (GET "/consents" {:consenter consenter-id}))

(defn add-consents
  [encounter-id data]
  (PUT "/consents"
       {:encounter encounter-id}
       nil
       (with-out-str (prn data))))

;; Encounter
(defn get-encounters
  [org-id]
  (GET "/consent/encounters" {:organization org-id}))

(defn get-encounters-by-location
  [location-id]
  (GET "/consent/encounters" {:location location-id}))

(defn get-encounter
  [encounter-id]
  (GET "/consent/encounter" {:encounter encounter-id}))


(defn add-encounter
  [location-id consenter-id data]
  (PUT "/consent/encounter"
       {:location location-id :consenter consenter-id}
       nil
       (with-out-str (prn data))))

(defn update-encounter
  [encounter-id data]
  (POST "/consent/encounter"
        {:encounter encounter-id}
        nil
        (with-out-str (prn data))))

(defn delete-encounter
  [encounter-id]
  (DELETE "/consent/encounter" 
          {:encounter encounter-id} nil nil))

(defn get-encounter-consents
  [encounter-id]
  (GET "/consent/encounter/consents" {:encounter encounter-id}))

(defn get-encounter-consent-endorsements
  [encounter-id]
  (GET "/consent/encounter/consent/endorsements" {:encounter encounter-id}))

(defn get-encounter-consent-meta-items
  [encounter-id]
  (GET "/consent/encounter/consent/meta-items" {:encounter encounter-id}))

