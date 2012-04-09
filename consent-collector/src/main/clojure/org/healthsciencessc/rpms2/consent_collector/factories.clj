(ns org.healthsciencessc.rpms2.consent-collector.factories
  (:use 
	[clojure.tools.logging :only (debug info error)]))

(def protocol-names 
   	[ 
	"Lewis Blackman Hospital Patient Safety Act Acknowledgeement" 
	"Consent for Medical Treatment" 
	"Medicare" 
	"Tricare" 
	]
)

(def data-mappings {
   :location [ :id :name :code :protocol-label :organization ]
   :meta-item [ :id :name :description :data-type :default-value :organization ]
   :protocol [ :id :name :description :protocol-id :code :required :select-by-default :organization :location ]
})

(def test-data 
	{ 
	:lastname [ "Smith" "Wilson" "Dylan" "Obama" "Jones" "Sanderson" "Rugg" "Gerkin" "Crawford" ] 
	:firstname [ "Tami" "Cindy" "Bob" "Jennifer" "Barkley" "Wendy" "Gary" "Dan" "Jean" ]
        :medical-record-number [ "900000101" "900000102" "900000103" "900000104" ]
        :zipcode [ "29414" "38017" "29810" "12345" "99887" ]
        :referring-doctor [ "Dr Spock"  "Dr Kirk" "Dr Frank" "Dr Johnson" "Dr Jones" ]
        :primary-care-physician [ "Dr Primary 1" "Dr B" "Dr C" "Dr D" "Dr E" "Dr F" ]
        :primary-care-physician-city [ "Boston"  "Charleston" "Springfield" "Seatle" "New York" "Manchester" "Iowa City" ]
	}
)

(def test-location-names [
	"Registration" 
	"Front Desk" 
	"Bus Stop" 
	"Emergency Room" ])

(defn- id
  []
  (rand-int 1000000000))

(defn- locname 
  []
  (nth test-location-names (rand-int (count test-location-names))))

(defn generate-location
  []
  (let [nm (locname) ]
  {:location (id)  :name (locname) :code "reg" } ))


(defn generate-datum 
  "Generate a data value for field.   Uses the prototype to generate the value
  which matches certain criteria (currently not done).
  "
  [field-name prototype]
  (let [data (field-name test-data) 
        data (if-let [given (field-name prototype)]
               (filter #(= given (subs % 0 (count given))) data)
               data)]
    (if (empty? data)
      (field-name prototype)
      (rand-nth data))))


(defn generate-meta-data-items
 []
 (list

{ :id (id) :name "additional-guarantor" :description "Additional guarantor" :data-type "string" :organization "MYORG" }
{ :id (id) :name "referring-doctor" :description "Date admitted" :data-type "string" :organization "MYORG" }
{ :id (id) :name "referring-doctor-city" :description "" :data-type "string" :organization "MYORG" }
{ :id (id) :name "primary-care-physician" :description "" :data-type "string " :organization "MYORG" }
{ :id (id) :name "primary-care-physician-city" :description "" :data-type "string" :organization "MYORG" }
{ :id (id) :name "attending-physician" :description "" :data-type "string" :organization "MYORG" }
{ :id (id) :name "advanced-directives-given" :description "" :data-type "yes-no" :organization "MYORG" }
{ :id (id) :name "admission-date" :description "Date admitted" :data-type "string" :organization "MYORG" }
{ :id (id) :name "form-signer" :description "Signer" :data-type "choice - patient or patient rep" :organization "MYORG" }

  )
)

(defn generate-protocol
  [prototype]
  { :name  (:name prototype)
    :description (if (:description prototype) (:description prototype) "description for protocol")
    :protocol-id "generated protocol-id"
    :code "description for protocol"
    :required (if (:required prototype) (:required prototype) false )
    :select-by-default (if 
	(:select-by-default prototype) 
	(:select-by-default prototype) false )
    :organization "description for protocol"
    :location "description for protocol"
  }
)

(def generate-medical-record-number
  (partial swap! (atom 900000101) inc))

(defn generate-user 
  [prototype]

  {:firstname (generate-datum :firstname prototype) 
    :lastname (generate-datum :lastname prototype) 
    :medical-record-number (generate-medical-record-number)
    :encounter-date "2012-01-15" 
    :visit-number "vn-90001" 
    :date-of-birth "1981-02-03" 
    :last-4-digits-ssn "0000" 
    :zipcode (generate-datum :zipcode prototype)
    :referring-doctor (generate-datum :referring-doctor prototype) 
    :primary-care-physician (generate-datum :primary-care-physician prototype)
    :primary-care-physician-city (generate-datum :primary-care-physician-city prototype)
  }
)

(defn- rarely-zero-rand-int
  "Like rand-int but returns 0 five times less often."
  [n]
  (let [x (inc (* 5 (dec n)))
        y (rand-int x)]
    (if (zero? y)
      0
      (inc (quot (dec y) 5)))))

(defn generate-user-list
  "Generate random list of users that match the specified user
  prototype."

  ([prototype]
   (debug "generate-user-list with random number of users")
   (generate-user-list prototype (rarely-zero-rand-int 10)))

  ([prototype n]
   (for [s (range n)] (generate-user prototype))))

(defn generate-protocol-list
  []
  ;;(def tx (for [pname [protocol-names]] (generate-protocol {:name pname})))
  (list 
	(generate-protocol {:name "Tami-Test" :select-by-default false
		:required true :description "this is a test" } ) 

	(generate-protocol {:name 
		"Lewis Blackman Hospital Patient Safety Act Acknowledgeement" 
		:select-by-default true
		:required true 
		:description "Inform patient of right of access to attending physician" } ) 

	(generate-protocol {:name 
		"Consent for Medical Treatment" 
		:select-by-default false
		:required false 
		:description "Some consent for medical treatment stuff " } ) 


	(generate-protocol {:name 
		"Medicare" 
		:select-by-default false
		:required false 
		:description "Medicare stuff" } ) 

	(generate-protocol {:name 
		"Tricare" 
		:select-by-default true
		:required false 
		:description "Tricare stuff" } ) 
	 )
)

(comment "Sample User"
  [{:firstname "Joe"
    :lastname "Smith"
    :medical-record-number "90001" 
    :encounter-date "2012-01-15" 
    :visit-number "vn-90001" 
    :date-of-birth "1981-02-03" 
    :last-4-digits-ssn "0000" 
    :zipcode "12345" 
    :referring-doctor "Dr Spock" 
    :primary-care-physician "Dr Primary 1" 
    :primary-care-physician-city "Boston" 
  }]
)



;;
;;JSON for a sample location
;;{
;; id: 534
;; name:"Registration",
;; code:"reg",
;; protocol-label:"Consent Forms",
;; organization:{id:110, name="Med Univ", code="musc"}
;;}
  
(def data-no-locations {:locations [ ] })

(def data-one-location {:locations
	[ {:id 534 :name "Registration" :code "reg" :protocol-label "Consent Forms" 
 		:organization {:id 110 :name "Med Univ":code "musc"}  }
	] })

(def data-multiple-locations {:locations [
	{:id 534 :name "Registration" :code "reg" :protocol-label "Consent Forms"
 		:organization {:id 110 :name "Med Univ":code "musc"}  }
	{:id 834 :name "Front Desk" :code "fd" :protocol-label "Agreements" 
 		:organization {:id 111 :name "spartan ":code "srhs"}  }
	{:id 934 :name "Bus Stop" :code "fd" :protocol-label "Insurance Waivers" 
 		:organization {:id 113 :name "greenville":code "ghs"}  }
	] })

