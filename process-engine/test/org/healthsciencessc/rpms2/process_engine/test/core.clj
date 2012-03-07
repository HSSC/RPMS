(ns org.healthsciencessc.rpms2.process-engine.test.core
  (:use [org.healthsciencessc.rpms2.process-engine.core]
        [clojure.test])
  (:import [org.healthsciencessc.rpms2.process_engine.core DefaultProcess CustomProcess]))

(defn reset-process-coll!
  [coll]
  (reset! coll []))

(defn reset-processes!
  []
  (reset-process-coll! default-processes)
  (reset-process-coll! custom-processes))

(deftest initialize-empty-process-colls
  (is (true? (empty? @default-processes)))
  (is (true? (empty? @custom-processes))))

(deftest register-a-default-process
  (let [dp (DefaultProcess. "get-foo-bar" (fn [] true) (fn [] "record"))]
    (register-process dp)
    (is (= 1 (count @default-processes)))
    (is (= 0 (count @custom-processes))))
  (reset-processes!))

(deftest register-a-custom-process
  (let [cp (CustomProcess. "get-baz-qux" 1 (fn [] true) (fn [] "record"))]
    (register-process cp)
    (is (= 1 (count @custom-processes)))
    (is (= 0 (count @default-processes))))
  (reset-processes!))

(deftest register-multiple-processes
  (let [process-defs [{:name "get-foo-bar" :runnable-fn (fn [] true) :run-fn (fn [] true)}
                      {:name "get-baz-qux" :runnable-fn (fn [] true) :run-fn (fn [] true)}]
        processes (map #(map->DefaultProcess %) process-defs)]
    (register-processes processes)
    (is (= 2 (count @default-processes))))
  (reset-processes!))

(deftest register-process-appends-coll
  (register-process (DefaultProcess. "get-foo-bar" (fn [] true) (fn [] "ran")))
  (is (= 1 (count @default-processes)))
  (register-process (DefaultProcess. "get-baz-qux" (fn [] true) (fn [] "ran")))
  (is (= 2 (count @default-processes)))
  (reset-processes!))

(deftest find-runnable-process
  (register-process (DefaultProcess. "get-foo-bar" (fn [args] true) (fn [args] "ran")))
  (is (= "get-foo-bar" (:name (find-default-process "get-foo-bar" {}))))
  (reset-processes!))


;; (deftest no-runnable-process
;;   (register-process (DefaultProcess. "get-foo-bar" (fn [args] false) (fn [args] "ran")))
;;   (is (nil? (find-default-process "get-foo-bar" {})))
;;   (reset-processes!))

(deftest custom-processes-found-by-order
  (register-process (CustomProcess. "get-foo-bar" 2 (fn [args] true) (fn [args] "ran")))
  (register-process (CustomProcess. "get-foo-bar" 1 (fn [args] true) (fn [args] "ran")))
  (is (= 1 (:order (find-custom-process "get-foo-bar" {}))))
  (reset-processes!))
