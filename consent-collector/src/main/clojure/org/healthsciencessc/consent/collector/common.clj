(ns org.healthsciencessc.consent.collector.common)

(defn formal-name
  [person]
  (str (:first-name person) " " (:last-name person)))