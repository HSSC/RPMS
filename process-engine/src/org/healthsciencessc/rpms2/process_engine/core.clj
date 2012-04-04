(ns org.healthsciencessc.rpms2.process-engine.core
  (:require [clojure.java.io :as io])
  (:use [slingshot.slingshot :only (throw+)]))

(defrecord CustomProcess
    [name order runnable-fn run-fn])

(defrecord DefaultProcess
    [name runnable-fn run-fn])

(def default-processes
  (atom []))

(def custom-processes
  (atom []))

(defmulti register-process
  "Adds the process to the correct collection based on record type"
  class)

(defmethod register-process CustomProcess
  [new-process]
  (reset! custom-processes (sort-by :order (conj @custom-processes new-process))))

(defmethod register-process DefaultProcess
  [new-process]
  (swap! default-processes conj new-process))

(defn register-processes
  "Adds a coll of processes to the correct type collection"
  [processes]
  (doall (map register-process processes)))

(defn- find-process-fns
  "Searches given process list for match on name and runnable with supplied context and returns the function to run"
  [process-name params process-list]
  (->> process-list
       (filter #(= process-name (:name %)))
       (map (fn [{:keys [runnable-fn run-fn run-if-false]}]
              (if (runnable-fn params)
                run-fn
                run-if-false)))
       (filter identity)))

(defn find-process-fn
  [name params process-list]
  (if-let [process-fn (first (find-process-fns name params process-list))]
    process-fn
    (throw+ {:type ::no-default-process :process-name name})))

(defn run-default
  "Runs the correct function of the default process with the given name"
  [name params]
  ((find-process-fn name params @default-processes) params))

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

(defn dispatch
  "Public function to find and execute the correct process based on name and context"
  [name params]
  (let [process-fn (find-process-fn name params (concat @custom-processes @default-processes))]
    (process-fn params)))
