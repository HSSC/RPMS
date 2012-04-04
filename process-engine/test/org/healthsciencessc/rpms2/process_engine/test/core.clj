(ns org.healthsciencessc.rpms2.process-engine.test.core
  (:use [org.healthsciencessc.rpms2.process-engine.core]
        [slingshot.slingshot :only (try+)]
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
  (is (= "ran" (dispatch "get-foo-bar" {})))
  (reset-processes!))

(deftest no-runnable-process
  (register-process (DefaultProcess. "get-foo-bar" (fn [args] false) (fn [args] "ran")))
  (try+
    (dispatch "get-foo-bar" {})
    (catch [:type :org.healthsciencessc.rpms2.process-engine.core/no-default-process] {process-name :process-name} 
      (is (= "get-foo-bar" process-name))))
  (reset-processes!))

(deftest custom-processes-found-by-order
  (register-process (CustomProcess. "get-foo-bar" 2 (fn [args] true) (fn [args] "ran 2")))
  (register-process (CustomProcess. "get-foo-bar" 1 (fn [args] true) (fn [args] "ran 1")))
  (is (= "ran 1" (dispatch "get-foo-bar" {})))
  (reset-processes!))

(deftest run-default-if-custom-false
  (register-process (CustomProcess. "get-foo-bar" 2 (fn [args] false) (fn [args] "ran 2")))
  (register-process (CustomProcess. "get-foo-bar" 1 (fn [args] false) (fn [args] "ran 1")))
  (register-process (DefaultProcess. "get-foo-bar" (fn [args] true) (fn [args] "ran default")))
  (is (= "ran default" (dispatch "get-foo-bar" {})))
  (reset-processes!))

(deftest run-fn-if-false
  (register-process (DefaultProcess/create {:name "get-foo-bar" :runnable-fn (fn [args] false) :run-fn (fn [args] "ran true") :run-if-false (fn [args] "ran false")}))
  (is (= "ran false" (dispatch "get-foo-bar" {})))
  (reset-processes!))

(deftest dont-run-false-fn-if-runnable-true
  (register-process (DefaultProcess/create {:name "get-foo-bar" :runnable-fn (fn [args] true) :run-fn (fn [args] "ran true") :run-if-false (fn [args] "ran false")}))
  (is (= "ran true" (dispatch "get-foo-bar" {})))
  (reset-processes!))