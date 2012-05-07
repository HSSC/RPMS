(ns org.healthsciencessc.rpms2.core
  (:require [goog.net.XhrIo :as xhio ])
  (:use [cljs.reader :only [read-string]]))

(defn mylog 
   [msg]
   (let [details (js/$ "#consenter-details")  ]
         (.append details (str "<p>HI TAMI" msg "</p>"))))
    
(defn ajax-sexp-get
  [path callback]
  (.get js/$ path (fn [data] (-> data read-string callback)) "text"))


(def xhr xhr-connection)

(defn callback [reply]
   (.log js/console "**** IN CALLBACK")
   (.log js/console (str "**** IN CALLBACK REPLAY IS " reply) )
   (let [v 
	(js->clj (-getResponseJson (.-target reply)))] ;v is a Clojure data structure
      (.log js/console "**** 16 IN CALLBACK")
      (js/alert (str "Hi this is the callback " v))
      (.log js/console "**** 18 IN CALLBACK")
	v))

(defn ^:export search
  []
  (.log js/console "25 search() enter search")

  (try 
  (ajax-json "/select/view/consenters" )
  (catch java.lang.Exception ex )
  (.log js/console (str "29 search() EXCEPTION " ex))
  )

  (.log js/console "27 search() after json call search")
  (.log js/console "28 calling search is anybody out there?")
  (ajax-sexp-get 
		"/sexp/search/consenters" ;; the url
                 (fn [data]  ;; the callback
                   (js/alert (str "I got this data: " (pr-str data))))))

(defn try-ajax
  []
  (ajax-sexp-get "/collect/some/ajax/data"
                 (fn [data]
                   (js/alert (str "I got this data: " (pr-str data))))))


(defn ajax-json 
  [url]
  (.log js/console (str "  55 AAA ajax-json " url))
  (.send goog.net.XhrIo url callback)
  #_(.send xhr url callback)
  (.log js/console (str "  xhr request has been sent to " url))
  )

;; ===========================


;;=============================================
(defn ^:export consenter-search-clicked
  [div]
  (let [details (js/$ "#consenter-details")  ]
      (.append (js/$ div) "<p>HI TAMI</p>")) 
   ;;(.send goog.net.XhrIo url callback)
)

;;=============================================
(defn ^:export consenter-confirmed-clicked
  "Called when the user confirms that the current patient
  is the correct patient. " 
  []
  (let [url "http://localhost:8081/collect/view/select/consenters" ]
     (ajax-json  url)
  (js/alert (str "This patient is confirmed! ")))
  )

;;=============================================
(defn ^:export consenter-search-result-clicked
  [div]
  (.log js/console "consenter-search-result-clicked")
  (let [user (read-string (.getAttribute div "data-user"))
        details (js/$ "#consenter-details") 
        other-section (js/$ "#other-section")
        {:keys [first-name last-name]} user]

    ;; Set the highlight style on clicked div
    (.removeClass (js/$ ".user-selected") "user-selected")
    (.addClass (js/$ div) "user-selected")
		
    ;; Set this value so it can be referred to 
    (.val (.find other-section (str "#current-patient-selection")) user)

    ;; Set this value in the form that will be submitted if the 
    ;; user selects yes, that this is the correct record
    (.val (.find other-section (str "#patient-id")) (:medical-record-number user))
    (.val (.find other-section (str "#patient-name")) (str first-name " " last-name))
    (.val (.find other-section (str "#patient-encounter-date")) (:consenter-encounter-date user))

    ;; Set text values in details section to the corresponding value in user record
    (def display-fields [:zipcode :date-of-birth :consenter-id ])
    (doseq [[id val] (-> user
                         (assoc :name (str first-name " " last-name)
                         (select-keys display-fields #_[:zipcode  
                                       :date-of-birth 
                                       :last-4-digits-ssn
                                       :referring-doctor 
				       :primary-care-physician 
                                       :primary-care-physician-city
				       :visit-number 
                                       :encounter-date 
                                       :consenter-id 
                                       :medical-record-number ])))]
      (.text (.find details (str "#consenter-" (name id))) val))
))
