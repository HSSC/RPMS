(ns org.healthsciencessc.rpms2.consent-admin.ui.burp)

(defn burp
  "Burp provides a method for use with hiccup that deconstructs/flattens out any subelements that are provided in a 
   sequence."
  [target]
  (if (and (seq? target) (seq? (first target)))
    (flatten target)
    target))