(ns org.healthsciencessc.rpms2.consent-collector.common)

(defn formal-name
  [person]
  (str (:first-name person) " " (:last-name person)))