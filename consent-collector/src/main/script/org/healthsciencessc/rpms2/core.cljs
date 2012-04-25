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

(defn try-ajax
  []
  (ajax-sexp-get "/collect/some/ajax/data"
                 (fn [data]
                   (js/alert (str "I got this data: " (pr-str data))))))

(defn ^:export consenter-search-result-clicked
  [div]
  (let [user (read-string (.getAttribute div "data-user"))
        details (js/$ "#consenter-details") 
        ;other-section (js/$ "#consenter-details")
        other-section (js/$ "#other-section")
        {:keys [first-name last-name]} user]

    ;; Set the highlight style on the clicked div
    (.removeClass (js/$ ".user-selected") "user-selected")
    (.addClass (js/$ div) "user-selected")

    ;; if find details returns something then set the field
		
    ;; Set this value in the form that will be submitted if the 
    ;; user selects yes, that this is the correct record
    (.val (.find other-section (str "#patient-id")) (:medical-record-number user))
    (.val (.find other-section (str "#patient-name")) (str first-name " " last-name))
    (.val (.find other-section (str "#patient-encounter-date")) (:consenter-encounter-date user))

    ;; Set text values in details section to the corresponding value in the user record
    (doseq [[id val] (-> user
                         (select-keys [:zipcode :date-of-birth :last-4-digits-ssn
                                       :referring-doctor :primary-care-physician :primary-care-physician-city
				       :visit-number :encounter-date :medical-record-number ])
                         (assoc :name (str first-name " " last-name)))]
      (.text (.find details (str "#consenter-" (name id))) val))
))
