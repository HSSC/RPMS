(ns org.healthsciencessc.rpms2.core
  (:use [cljs.reader :only [read-string]]))

(defn ajax-sexp-get
  [path callback]
  (.get js/$ path (fn [data] (-> data read-string callback)) "text"))

(defn ^:export search
  []
  (ajax-sexp-get "/sexp/search/consenters"
                 (fn [data]
                   (js/alert (str "I got this data: " (pr-str data))))))

(defn ^:export consenter-search-result-clicked
  [div]
  (let [user (read-string (.getAttribute div "data-user"))
        details (js/$ "#consenter-details") 
        details-record (js/$ "#consenter-details-section") 
        other-section (js/$ "#other-section") ]

    ;; if find details returns something then set the field
    (.text (.find details-record (str "#consenter-visit-number")) (:visit-number user))
		
	;; Set this value in the form that will be submitted if the 
	;; user selects yes, that this is the correct record
    (.val (.find other-section (str "#patient-id")) (:medical-record-number user))
    (.val (.find other-section (str "#patient-name")) (str (:firstname user) " " (:lastname user) ))
    (.val (.find other-section (str "#patient-encounter-date")) (:consenter-encounter-date user))

    (.text (.find details-record (str "#consenter-medical-record-number")) (:medical-record-number user))
    (.text (.find details-record (str "#consenter-encounter-date")) (:encounter-date user))

    ;; in details section
    (.text (.find details (str "#consenter-name")) (str (:firstname user) " " (:lastname user) ))
    (.text (.find details (str "#consenter-zipcode")) (:zipcode user) )
    (.text (.find details "#consenter-zipcode") (:zipcode user) )
    (.text (.find details "#consenter-date-of-birth") (:date-of-birth user) )
    (.text (.find details "#consenter-last-4-digits-ssn") (:last-4-digits-ssn user)  )
    (.text (.find details "#consenter-referring-doctor") (:referring-doctor user) )
    (.text (.find details "#consenter-primary-care-physician") (:primary-care-physician user) )
    (.text (.find details "#consenter-primary-care-physician-city") (:primary-care-physician-city user)  )

))
