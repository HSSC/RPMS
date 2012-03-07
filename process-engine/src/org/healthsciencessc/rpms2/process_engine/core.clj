(ns org.healthsciencessc.rpms2.process-engine.core
  (:require [clojure.java.io :as io])
  (:use [slingshot.slingshot :only (throw+)]))

(defprotocol IConsentProcess
  (runnable? [this arg-map])
  (run [this arg-map]))

(defrecord CustomProcess
    [name order runnable-fn run-fn]
  IConsentProcess
  (runnable? [consent-process arg-map]
    ((:runnable-fn consent-process) arg-map))
  (run [consent-process arg-map]
    ((:run-fn consent-process) arg-map)))

(defrecord DefaultProcess
    [name runnable-fn run-fn]
  IConsentProcess
  (runnable? [consent-process arg-map]
    ((:runnable-fn consent-process) arg-map))
  (run [consent-process arg-map]
    ((:run-fn consent-process) arg-map)))

(def default-processes
  (atom []))

(def custom-processes
  (atom []))

(defn- append-process-coll
  "Adds the new process to the given process collection"
  [process-list new-process]
  (swap! process-list conj new-process))

(defmulti register-process
  "Adds the process to the correct collection based on record type"
  class)

(defmethod register-process CustomProcess
  [new-processes]
  (append-process-coll custom-processes new-processes))

(defmethod register-process DefaultProcess
  [new-processes]
  (append-process-coll default-processes new-processes))

(defn register-processes
  "Adds a coll of processes to the correct type collection"
  [processes]
  (doall (map register-process processes)))

(defn- find-processes
  "Searches given process type for match on name and runnable with supplied context"
  [type name params]
  (filter #(and (= name (:name %)) (runnable? % params)) type))

(defn find-custom-process
  "Returns the first registered custom process with the given name"
  [name params]
  (first (sort-by :order (find-processes @custom-processes name params))))

(defn find-default-process
  "Returns the first registered default process with the given name"
  [name params]
  (or (first (find-processes @default-processes name params))
      (throw+ {:type ::no-default-process :process-name name})))

(defn run-default
  "Runs the default process with the given name"
  [name params]
  (let [dp (find-default-process name params)]
    (if dp
      (run dp params))))

(defn- glob-dir
  "Returns a list of canonical file names of clj files in dir"
  [dir]
  (map (fn [file-io] (.getCanonicalPath file-io))
       (filter (fn [file-io] (.endsWith (.getName file-io) ".clj"))
               (file-seq (io/file dir)))))

(defn- load-files
  "Loads all the clj files in dir"
  [dir]
  (doall (map load-file (glob-dir dir))))

(defn load-processes
  "Loads all the proccess definitions in path. Path is assumed to be a relative path from project root unless prefixed with a slash"
  [path]
  (if (= \/ (first path))
    (load-files path)
    (load-files (str (.getCanonicalPath (io/file ".")) "/" path))))

(defn- search-processes
  "First looks for a custom process then a default process that is runnable"
  [name params]
  (let [custom-process (find-custom-process name params)]
    (or custom-process (find-default-process name params))))

(defn dispatch
  "Public function to find and execute the correct process based on name and context"
  [name params]
  (let [process (search-processes name params)]
    (run process params)))
