;; Provides a singular location by which the names of types and type instance identifiers can be 
;; defined for the base release.
(ns org.healthsciencessc.rpms2.consent-domain.types)

;; TYPES THAT ARE AVAILABLE IN THE BASE APPLICATION

(defmacro def-them-all [& types]
  `(do
     ~@(for [t types]
        `(def ~t ~(name t)))))

(def-them-all 
  organization
  user
  role
  language
  location
  group
  consenter
  role-mapping
  meta-item
  policy
  policy-definition
  endorsement
  endorsement-type
  form
  widget
  widget-property
  encounter
  consent
  consent-meta
  consent-endorsement
  protocol
  protocol-version
  text-i18n)

;; COMMON CODES USED THROUGHOUT THE APPLICATION.  IDENTIFIES UNIQUENESS WITHIN METADATA.

(def code-base-org "!DEFAULTORGANIZATION!")

(def code-role-superadmin "!sadmin!")
(def code-role-admin "!admin!")
(def code-role-collector "!collect!")
(def code-role-designer "!design!")
(def code-role-consentmanager "!manage!")
(def code-role-externalsystem "!csys!")

;; STANDARD PROTOCOL VERSION STATUSES
(def status-draft "Draft")
(def status-submitted "Submitted")
(def status-published "Published")
(def status-retired "Retired")

(defn draft?
  [protocol-version]
  (= status-draft (:status protocol-version)))

(defn submitted?
  [protocol-version]
  (= status-submitted (:status protocol-version)))

(defn published?
  [protocol-version]
  (= status-published (:status protocol-version)))

(defn retired?
  [protocol-version]
  (= status-retired (:status protocol-version)))
