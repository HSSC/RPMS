(ns org.healthsciencessc.rpms2.consent-services.process)

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

(defn append-process-coll
  [process-list new-process]
  (reset! process-list
          (conj @process-list new-process)))

(defmulti register-process class)

(defmethod register-process CustomProcess
  [new-processes]
  (append-process-coll custom-processes new-processes))

(defmethod register-process DefaultProcess
  [new-processes]
  (append-process-coll default-processes new-processes))

(defn register-processes
  [processes]
  (map register-process processes))

(defn find-process
  [type name params]
  (filter #(and (= name (:name %)) (runnable? % params)) type))

(defn find-custom-process
  [name params]
  (first (sort-by :order (find-process @custom-processes name params))))

(defn find-default-process
  [name params]
  (first (find-process @default-processes name params)))

(defn run-default-process
  [name params]
  (let [dp (find-default-process name params)]
    (if dp
      (run dp params))))