(ns org.healthsciencessc.rpms2.consent-domain.core)


; Represents a top-level tenant node.

(defrecord Organization [^String name
                         ^String location-label
                         ^String protocol-label ])


; Represents a second-level tenant node.

(defrecord Location [^String name
                     ^String protocol-label
                     ^Organization organization ])


; Represents users of the system.

(defrecord User [^String title
                 ^String first-name
                 ^String middle-name
                 ^String last-name
                 ^String suffix
                 ^String user-id
                 ^String password
                 ^Organization organization ])


; Represents groups of users of the system.

(defrecord Group [^String name
                  ^Organization organization ])


; Represents a role a user of the system can play.

(defrecord Role [^String name
                 ^Organization organization ])


; Represents an association between users, groups and the roles they 
; play within specific tenant nodes.

(defrecord RoleMapping [^Role role
                        ^Location location
                        ^Organization organization ])


; Represents data that can be collected for a protocol outside the 
; consenting process.

(defrecord MetaDefinition [^String reference-id
                           ^String label
                           ^String datatype
                           ^Organization organization ])


; Represents a protocol that manages the collection of associated 
; consents.

(defrecord Protocol [^String name
                     ^Location location
                     ^Organization organization
                     ^String protocol-id
                     ^String description ])


; Represents a version of a Protocol

(defrecord ProtocolVersion [^String version-id
                            ^String status
                            ^Protocol protocol ])


