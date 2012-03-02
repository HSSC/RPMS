(ns org.healthsciencessc.rpms2.consent-services.custom-processes)

(def custom-processes
  [{:name "get-security-users"
    :order 1
    :runnable-fn (fn [& args] false)
    :run-fn (fn [& args]
              (println "Before 1")
              ;;           (process/run-default "get-security-users" args)
              (println "After 1"))}
   {:name "get-security-users"
    :order 2
    :runnable-fn (fn [& args] true)
    :run-fn (fn [& args]
              (println "Before 2")
              ;;           (println (process/run-default "get-security-users" args))
              (println "After 2"))}])