(ns org.healthsciencessc.rpms2.consent-admin.services
  (:require [clj-http.client :as client]
            [sandbar.stateful-session :as sess]
            [org.healthsciencessc.rpms2.consent-domain.types :as domain]
            [clojure.pprint :as pp]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.stacktrace :as st]
            [hiccup.util :as hutil])
  (:use [org.healthsciencessc.rpms2.consent-admin.config]
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
  [_]
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
  [_]
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
        all-users (get-users nil)]
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

(defn add-role-to-user
  [u r])

(defn add-role-to-group
  [g r])

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
  (GET "/protocol/version" {:version protocol-version-id}))

(defn add-protocol-version
  "Adds a new protoco version to a protocol."
  [data]
  (PUT "/protocol/version"
        nil
        nil
        (with-out-str (prn data))))

(defn delete-protocol-version
  "Deletes a new protocol version from a protocol."
  [protocol-id]
  (DELETE "/protocol/version"
        {:version protocol-id}
        nil
        nil))

(defn update-protocol-version
  "Updates a protocol version with data changes."
  [protocol-id data]
  (POST "/protocol/version"
        {:version protocol-id}
        nil
        (with-out-str (prn data))))

(defn publish-protocol-version
  "Updates a protocol version with data changes."
  [protocol-version-id]
  (POST "/protocol/publish"
        {:version protocol-version-id}
        nil
        nil))

(defn retire-protocol-version
  "Updates a protocol version with data changes."
  [protocol-version-id]
  (POST "/protocol/retire"
        {:version protocol-version-id}
        nil
        nil))





