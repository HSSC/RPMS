(ns org.healthsciencessc.rpms2.consent-collector.debug
  (:require [clojure.tools.logging :as ctl]
            [clojure.string :as s]
            [clojure.pprint :as pp]))

(defn debug
  [& args]
  (ctl/debug (s/join " " args))
  (apply println args))

(defn pprint-str
  [arg]
  (with-out-str (pp/pprint arg)))

(defn debug-var
  [fn-name v]
  (alter-var-root v
                  (fn [old-fn]
                    (fn [& args]
                      (try
                        (debug "CALL:" (pprint-str (list* fn-name args)))
                        (let [ret (apply old-fn args)]
                          (debug (format "RETURN (from %s):"
                                         (name fn-name))
                                 (pprint-str ret))
                          ret)
                        (catch Throwable t
                          (debug (format "ERROR (in %s):"
                                         (name fn-name))
                                 (.getMessage t))
                          (throw t)))))))

(defmacro debug!
  [fn-name]
  `(debug-var (quote ~fn-name) (var ~fn-name)))
