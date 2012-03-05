(ns org.healthsciencessc.rpms2.consent-services.test.process
  (:use [org.healthsciencessc.rpms2.consent-services.process]
        [clojure.test])
  (:import [org.healthsciencessc.rpms2.consent_services.process DefaultProcess CustomProcess]))

(defn reset-process-coll
  [coll]
  (reset! coll []))

(deftest initialize-empty-process-colls
  (is (= true (empty? @default-processes)))
  (is (= true (empty? @custom-processes))))

(deftest register-a-default-process
  (let [dp (DefaultProcess. "get-foo-bar" (fn [] true) (fn [] "record"))]
    (register-process dp)
    (is (= 1 (count @default-processes)))
    (is (= 0 (count @custom-processes))))
  (reset-process-coll default-processes))

(deftest register-a-custom-process
  (let [cp (CustomProcess. "get-baz-qux" 1 (fn [] true) (fn [] "record"))]
    (register-process cp)
    (is (= 1 (count @custom-processes)))
    (is (= 0 (count @default-processes))))
  (reset-process-coll custom-processes))
