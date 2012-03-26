(ns org.healthsciencessc.rpms2.consent-collector.dsa-client
  (:require [clojure.string :as s]
            [clj-http.client :as http])
  (:use [clojure.tools.logging :only (debug info error)]
        [clojure.data.json :only (read-json json-str)]))

(defn dsa-process-call
  [process-name arguments]
  (let [[_ method path-dashes] (re-matches #"(get|post|put|delete)-(.+)" process-name)
        path (s/replace path-dashes "-" "/")
        maybe-parse-json
        (fn [{:keys [status body] :as resp}]
          (if (= 200 status)
            (assoc resp :json (read-json body))
            resp))]
    (-> {:method method
         :path path
         :body (json-str arguments)}
        http/request
        maybe-parse-json)))

(defmacro def-dsa-processes
  [& pnames]
  `(do ~@(for [pname pnames]
           `(defn ~pname [args#]
             (dsa-process-call ~(str pname) args#)))))

(def-dsa-processes
  get-authorized-locations
  post-security-authenticate)

(defn run-dsa-process
  [name context])

(defn consenters-for-org-loc
  [org loc]
  ( {["MUSC" "musc-loc1" ]   [ "Musc-Consenter-1" "Musc-Consenter-2" ] 
     ["MUSC" "musc-loc2" ]   [ "Musc-Consenter-Org2-1"  "Musc-Consenter-Org2-2" ]
     ["MUSC" "musc-loc3" ]   [ "Musc-Consenter-Org3-1" ]
     ["MUSC" "musc-loc4" ]   [ "Musc-Consenter-Org4-1" "Musc-Consenter-Org5-1" ] 
    } [org loc] )
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



(def data-user-foo { :username "foo"  :title "Mr." 
	:organization {:id 111 :name "spartan ":code "srhs"}  
	:role-mappings [ 
			{ 
			:role { :id 4 :name "Consent Collector" :code "cc" } 
			:organization { :id 2 :name "Med Univ" :code "musc" }
			:location { :id 2 :name "Registration" :code "reg" }
			}


			{ 
			:role { :id 4 :name "Consent Collector" :code "cc" } 
			:organization { :id 2 :name "Med Univ" :code "musc" }
			:location { :id 2 :name "Lunchroom" :code "reg" }
			}

		]
	} )


(def data-user-onesite { :username "onesite"  :title "Mr." 
	:organization {:id 111 :name "Spartanburg" :code "srhs"}  
	:role-mappings [ 
			{ 
			:role { :id 4 :name "Consent Collector" :code "cc" } 
			:organization { :id 111 :name "Spartanburg" :code "srhs" }
			:location { :id 2 :name "One Stop Shop" :code "reg" }
			}
		]
	} )



(def data-user-nosite { :username "nosites"  :title "Ms." 
	:organization {:id 112 :name "Medical University of South Carolina" :code "musc"}  
	:role-mappings [ 
		]
	} )


(defn authenticate
  "Built-in test accounts."
  [user-id password]
  (let [retval ( { 
	["foo" "bar"] {:json data-user-foo :status 200} 
	["foobar" "hunter2"] {:status 200} 
	["admin" "admin"] {:status 200} ;; user authorized for all sites
	["multisite" "bar"] {:json data-user-foo :status 200} ;; user authorized for multiple sites
	["onesite" "bar"] {:json data-user-onesite :status 200} ;; user authorized for one site
	["nosites" "bar"] {:json data-user-nosite :status 200} ;; user authorized for no sites
	} [user-id password]) ] 
	(if retval 
         (info "(mock) authenticated as [" user-id "] returning [" retval "]")
         (info "(mock) authenticate - NOT AUTHENTICATED: [" user-id "]")) 
        retval))
