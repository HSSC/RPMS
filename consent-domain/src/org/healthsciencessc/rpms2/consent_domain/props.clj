(ns org.healthsciencessc.rpms2.consent-domain.props
  (:use [org.healthsciencessc.rpms2.consent-domain.codec :only [decrypter decode]])
  (:use [org.healthsciencessc.rpms2.consent-domain.sniff :only [resource]])
  )


(defn slurp-props
  "Loads the key/values found in a properties file found on the classpath into a map"
  [url]
  (try
    (into {} (doto (java.util.Properties.)(.load (resource url))))
    (catch Exception e {})))

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