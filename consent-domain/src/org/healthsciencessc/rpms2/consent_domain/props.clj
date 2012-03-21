(ns org.healthsciencessc.rpms2.consent-domain.props
  (:use 'clojure.core )
  (:use 'org.healthsciencessc.rpms2.consent-domain.codec )
  (:use 'org.healthsciencessc.rpms2.consent-domain.sniff )
  (import [java.util Properties])
  )


(defn slurp-props
  "Loads the key/values found in a properties file found on the classpath into a map"
  [url]
  (into {} (doto (Properties.)
             (.load (resource url)))))
(defn- self
  "Just a bogus function to return what is passed"
  [string]
  string)

(defn slurp-config
  ""
  ([url]
    (slurp-config url nil {}))

  ([url passkey]
    (slurp-config url passkey {}))

  ([url passkey options]
    (let [props (slurp-props url)
          decr (if (nil? passkey) self (decrypter passkey options))]
      (into {} (for [[k v] props] [k (decode v decr)]))
      )))