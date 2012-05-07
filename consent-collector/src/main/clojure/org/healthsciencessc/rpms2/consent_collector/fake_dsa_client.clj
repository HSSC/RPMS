(ns org.healthsciencessc.rpms2.consent-collector.fake-dsa-client
  (:require [clojure.string :as s]
            ;[clj-http.fake :as http]
            )
  (:use [org.healthsciencessc.rpms2.consent-collector.factories :as factory]
        [clojure.tools.logging :only (debug info error)]
        [org.healthsciencessc.rpms2.consent-collector.config :only (config)]
        [clojure.data.json :only (read-json json-str)]))


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


(def data-user-multisite { :username "foo"  :title "Mr." 

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



(defn fake-authenticate
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

(defn fake-search-consenters
  [params]
  (try (let [m (factory/generate-user-list params) 
	v (into [] m )]
	(debug "search-consenters - ==> " (count v) " " v )
	v)
  (catch Exception ex (error "search-consenters failed: " params " " ex))))


(defn fake-get-protocols
   []
   (factory/generate-protocol-list))


(defn- return-json
  [data]
  {:status 200 :body (json-str data)})

;; best we can do without a macro
;; (make-fake-fn (fn [{:keys [userid password]}] (authenticate userid password)))
;; (fake-fn [userid password] (authenticate userid password))
;;(make-fake-fn (fn [{:keys [userid password]}] (authenticate userid password)))
                        ;;
(defn- make-fake-fn
  "Example
   (make-fake-fn (fn [{:keys [userid password]}] (fake-authenticate userid password)))
   "
   [func]
   (fn [req] (return-json (func (:query-params req)))))

(defmacro ^:private fake-fn 
   "Example: 
    This in conjunction with make-fake-fun allows us to use this:
       (fake-fn [userid password] (fake-authenticate userid password))
    instead of this:
    (fn [{{:keys [userid password]} :query-params}]
        (return-json (authenticate userid password )))  
   "
   [keys & body]
   `(make-fake-fn (fn [{:keys ~keys }] ~@body ))
)

;; (def m {:userid "foo" :password "bar"})
;; (fn [m] (let [userid (:userid m) password (:password m)]))
;; (fn [{userid :userid password :password}])
;; (fn [{:keys [userid password]}])

(def my-fake-routes
  {
    #".*security/authenticate" (fake-fn [userid password] (fake-authenticate userid password))
    #".*consent/protocols" (constantly {:status 200 :body (json-str (fake-get-protocols)) } )  
    #".*consent/search" (fake-fn [] (fake-search-consenters "")) 
  } )  
    ;; #".*consent/search" (constantly {:status 200 :body (json-str (fake-search-consenters)) } )  
   
 

;; if fake-dsa is enabled, then set clj-http.fake routes to 
;; permanently map to the mocked routes instead of 
;; actually invoking dsa (clj-http.fake)
#_(when (= (config "fake-dsa")  "true" )
  (debug "Using fake-dsa")
  (alter-var-root #'http/*fake-routes* (constantly my-fake-routes)))

