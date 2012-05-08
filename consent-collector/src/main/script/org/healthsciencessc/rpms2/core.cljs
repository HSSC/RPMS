(ns org.healthsciencessc.rpms2.core
  (:use [cljs.reader :only [read-string]]))

(defn mylog 
   [msg]
   (let [details (js/$ "#consenter-details")  ]
         (.append details (str "<p>HI TAMI" msg "</p>"))))
    
(defn ajax-sexp-get
  [path callback]
  (.get js/$ path (fn [data] (-> data read-string callback)) "text"))


(defn ^:export search
  []
  (.log js/console "25 search() enter search")

  (try 
  (ajax-json "/select/view/consenters" )
  (catch java.lang.Exception ex )
  (.log js/console (str "29 search() EXCEPTION " ex))
  )

  (ajax-sexp-get 
		"/sexp/search/consenters" ;; the url
                 (fn [data]  ;; the callback
                   (js/alert (str "I got this data: " (pr-str data))))))

(defn try-ajax
  []
  (ajax-sexp-get "/collect/some/ajax/data"
                 (fn [data]
                   (js/alert (str "I got this data: " (pr-str data))))))

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
  (.log js/console "b consenter-search-result-clicked")
  (let [user (read-string (.getAttribute div "data-user"))
        details (js/$ "#consenter-details") 
        other-section (js/$ "#other-section")
        {:keys [first-name last-name]} user]

    (.log js/console "c users is " user)
    ;; Set the highlight style on clicked div
    (.removeClass (js/$ ".user-selected") "user-selected")
    (.addClass (js/$ div) "user-selected")

    (.log js/console "d users is " (str (:first-name user)))
    (.log js/console "e " (.find details (str "#consenter-name")) )

    ;; in details section
    (.text (.find details (str "#consenter-name")) (str (:first-name user) " " (:last-name user) ))
    (.text (.find details (str "#consenter-zipcode")) (:zipcode user) )
    (.text (.find details "#consenter-date-of-birth") (:date-of-birth user) )
		
    ;; Set this value in the form that will be submitted if the 
    ;; user selects yes, that this is the correct record
    (.val (.find other-section (str "#patient-name")) (str first-name " " last-name))
    (.log js/console "f patient name set " (str first-name " " last-name))

    (.val (.find other-section (str "#patient-encounter-date")) (:consenter-encounter-date user))
    (.val (.find other-section (str "#patient-id")) (:medical-record-number user))

    ;; Set this value so it can be referred to 
    (.val (.find other-section (str "#current-patient-selection")) user)

))

    ;;(.text (.find details "#consenter-zipcode") (:zipcode user) )
    ;;(.text (.find details "#consenter-date-of-birth") (:date-of-birth user) )
    ;;(.text (.find details "#consenter-last-4-digits-ssn") (:last-4-digits-ssn user)  )
    ;;(.text (.find details "#consenter-referring-doctor") (:referring-doctor user) )
    ;;(.text (.find details "#consenter-primary-care-physician") (:primary-care-physician user) )
    ;;(.text (.find details "#consenter-primary-care-physician-city") (:primary-care-physician-city user)  )

