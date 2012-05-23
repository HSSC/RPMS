(ns org.healthsciencessc.rpms2.process-engine.core
  (:require [clojure.java.io :as io]
            [clojure.java.classpath :as cp])
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

(defn clj-file? [name]
  (or (.endsWith name ".clj")
      (.endsWith name ".CLJ")))

(defn clj-files-from-classpath [dir]
  (->> (distinct (mapcat file-seq (cp/classpath-directories)))
       (filter #(.isFile ^java.io.File %))
       (filter #(clj-file? (.getName %)))
       (filter (fn [^java.io.File f]
                 (.endsWith (.getParent f) dir)))))

(defn load-from-classpath [dir]
  (doseq [f (clj-files-from-classpath dir)]
    (println (str "Loading from classpath: " (.getAbsolutePath f)))
    (load-reader (io/reader f))))
                   
(defn clj-in-jars []
  (let [fnames (mapcat cp/filenames-in-jar (cp/classpath-jarfiles))]
        ;;unix-name (.replace dir \\ \/)]
    (filter clj-file? fnames))) 

(defn jar-entry-parent-dir [n]
  (let [slash-idx (.lastIndexOf n (int \/))]
    (cond
      (= slash-idx -1)
      nil
      (= slash-idx 0)
      (throw (Exception. "WAT"))
      (> slash-idx 0)
      (.substring n 0 slash-idx))))

(defn matches-from-jars [dir]
  (filter (fn [n]
            (if-let [parent (jar-entry-parent-dir n)]
              (.endsWith parent dir)))
          (clj-in-jars)))

(defn load-from-jars [dir]
  (let [cl (clojure.lang.RT/baseLoader)]
    (doseq [m (matches-from-jars dir)]
      (println "Loading from jar: " m)
      (load-reader (io/reader (.getResourceAsStream cl m))))))

(defn load-processes [dir]
  (load-from-jars dir)
  (load-from-classpath dir))

(defn bootstrap-addons
  "Provides a way to bootstrap all of the resources matching a specific path into the clojure compiler."
  ([] (bootstrap-addons "/rpms/bootstrap.clj"))
  ([resource]
    (let [cl (clojure.lang.RT/baseLoader)
          resources (enumeration-seq(.getResources cl resource))]
      (doseq [url resources]
        (load-reader (java.io.InputStreamReader. (.openStream url)))))))

(defn dispatch
  "Public function to find and execute the correct process based on name and context"
  [name params]
  (let [process-fn (find-process-fn name params (concat @custom-processes @default-processes))]
    (process-fn params)))
