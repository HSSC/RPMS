(ns org.healthsciencessc.rpms2.core
  (:use [cljs.reader :only [read-string]]))

(defn ajax-sexp-get
  [path callback]
  (.get js/$ path (fn [data] (-> data read-string callback)) "text"))

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

    ;(.log js/console "d users is " (str (:first-name user)))
    ;(.log js/console "e " (.find details (str "#consenter-name")) )

    ;; in details section
    (.text (.find details (str "#consenter-name")) (str (:first-name user) " " (:last-name user) ))
    (.text (.find details (str "#consenter-zipcode")) (:zipcode user) )
    (.text (.find details "#consenter-date-of-birth") (:date-of-birth user) )
    (.text (.find details "#consenter-dob") (:dob user) )
    (.text (.find details "#consenter-consenter-id") (:consenter-id user) )
		
    (.val (.find other-section (str "#patient-name")) (str first-name " " last-name))
    (.log js/console "patient name set " (str first-name " " last-name))

    ;; Set this value in the form that will be submitted if the 
    ;; user selects yes, that this is the correct record
    (.log js/console "consenter id set: " (:consenter-id user))
    (.val (js/$ "#consenter-id") (:consenter-id user))

    ;(.val (.find other-section (str "#patient-encounter-date")) (:consenter-encounter-date user))
    ;(.val (.find other-section (str "#patient-id")) (:medical-record-number user))

    ;; Set this value so it can be referred to 
    ;;;;;;(.val (.find other-section (str "#current-patient-selection")) user)
))

;;=============================================
(defn ^:export data-change-clicked
  "A data change button has been pressed. The name of the id
   is passed in.  Add the 'changed' style to items with that id.
   Also changes the value of the hidden input field named hidden-[id]
   to CHANGED."

  [div]
  ;(.log js/console "data-change-clicked")
  (let [mdid (read-string (.getAttribute div "mdid"))
        details (js/$ (str "#" mdid) ) 
        data-change-section (js/$ "div.control.data-change")
        h (js/$ (str "#hidden-" mdid) ) ]

        ;(.log js/console "mdid is " mdid) 
        ;(.log js/console "h is " h) 

        ;; Set highlight style on clicked div
        (.addClass (js/$ details) "changed" )
        (.val (.find data-change-section (str "#hidden-" mdid)) "CHANGED")
        ;(.val h "CHANGED")
  ))
