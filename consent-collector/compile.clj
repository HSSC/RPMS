(require '[cljs.closure :as closure])
(import '(java.io File))


;; Ensures the the project path is the base path of all project file references.
;; The user.dir property is screwy when building nested modules.
(defn base-path
  ""
  []
  (let [file (File. (System/getProperty "user.dir"))
        fullPath (.getPath file)
        base (.getName file)]
    (if (= "consent-collector" (.getName file))
      (.getPath file)
      (str (.getPath file) (File/separator) "consent-collector"))))

;; Gets full path of a relative project path.
(defn project-file
  "Makes the path relative to the project."
  [path]
  (str (base-path) (File/separator) path))

(def source (project-file "src/main/script/org/healthsciencessc/rpms2"))

(def options {:output-to (project-file "src/main/resources/public/app.js")
              ;;:output-dir (project-file "src/main/resources/public") 
              :pretty-print true
              ;; :optimizations :advanced
              ;; :optimizations :simple
              :optimizations :whitespace
              })

(closure/build source options)