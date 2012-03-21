(ns org.healthsciencessc.rpms2.consent-domain.sniff
  (:use 'clojure.core )
  (:require clojure.contrib.properties :as clops)
  (:require clojure.contrib.string :as clings))

(defn resource
  "Gets the input stream of a file on the classpath"
  [url]
   (-> (Thread/currentThread)
               (.getContextClassLoader)
               (.getResourceAsStream url)))

(defn sniff-env
  "Looks for a value in the environment variables."
  [k]
  (System/getenv (clings/as-str k)))

(defn sniff-sys
  "Looks for a value in the system properties."
  [k]
  (System/getProperty (clings/as-str k)))

(defn sniff-jndi
  "Looks for a value in the JNDI Naming context"
  [k]
  (try
    (.lookup (javax.naming.InitialContext.) (clings/as-str k))
    (catch Exception e nil)))

(defn sniff-jndi-prefixed
  "Looks for a value in the JNDI Naming context"
  [k]
  (try
    (.lookup (javax.naming.InitialContext.) (clings/as-str "java:comp/env/" k))
    (catch Exception e nil)))

(defn sniff-content
  "Looks for content in a file on the classpath."
  [k]
  (try 
    (slurp (resource (clings/as-str k)))
    (catch Exception e nil)))

(defn sniff
  "Looks for a value identified by a key in the context of a sniffer."
  [k]
  (first 
    (remove nil?
      ((juxt sniff-env sniff-sys sniff-content sniff-jndi sniff-jndi-prefixed) k))))

