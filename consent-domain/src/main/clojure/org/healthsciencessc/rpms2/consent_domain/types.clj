;; Provides a singular location by which the names of types and type instance identifiers can be 
;; defined for the base release.
(ns org.healthsciencessc.rpms2.consent-domain.types)

;; TYPES THAT ARE AVAILABLE IN THE BASE APPLICATION

(def organization "organization")
(def user "user")
(def role "role")
(def language "language")
(def location "location")
(def group "group")
(def consenter "consenter")
(def role-mapping "role-mapping")
(def meta-item "meta-item")
(def policy "policy")
(def policy-definition "policy-defintion")
(def endorsement "endorsement")
(def form "form")
(def widget "widget")
(def encounter "encounter")
(def consent "consent")
(def consent "consent-meta")
(def consent "consent-endorsement")

;; COMMON CODES USED THROUGHOUT THE APPLICATION.  IDENTIFIES UNIQUENESS WITHIN METADATA.

(def code-base-org "!DEFAULTORGANIZATION!")

(def code-role-superadmin "!sadmin!")
(def code-role-admin "!admin!")
(def code-role-collector "!collect!")
(def code-role-designer "!design!")
(def code-role-consentmanager "!manage!")
(def code-role-externalsystem "!csys!")
