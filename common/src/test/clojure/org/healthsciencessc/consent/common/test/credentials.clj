(ns org.healthsciencessc.consent.common.test.credentials
  (:use [org.healthsciencessc.consent.common.credentials]
        [clojure.test]))


(defn wrap
  ([m] (str (wrap-username m) ":" (wrap-password m)))
  ([username password] (str (wrap-username username) ":" (wrap-password password)))
  ([username password realm] (str (wrap-username username realm) ":" (wrap-password password))))

(deftest test-credentials-map 
  (let [cred-map {:username "bob" :realm "remote" :password "yeah"}
        basic (wrap cred-map)
        decoded (unwrap-credentials basic)]
    (is (= (:username cred-map)(:username decoded)))
    (is (= (:password cred-map)(:password decoded)))
    (is (= (:realm cred-map)(:realm decoded)))))


(deftest test-credentials-local 
  (let [username "bob"
        password "yeah"
        basic (wrap username password)
        decoded (unwrap-credentials basic)]
    (is (= username (:username decoded)))
    (is (= password (:password decoded)))
    (is (= "local" (:realm decoded)))))


(deftest test-credentials-realm 
  (let [username "bob"
        password "yeah"
        realm "shibboleth"
        basic (wrap username password realm)
        decoded (unwrap-credentials basic)]
    (is (= username (:username decoded)))
    (is (= password (:password decoded)))
    (is (= realm (:realm decoded)))))


(deftest test-credentials-url 
  (let [username "https://u@p:openid.org/this/is/idAbd dfji kdihjid"
        password "yeah"
        realm "shibboleth"
        basic (wrap username password realm)
        decoded (unwrap-credentials basic)]
    (is (= username (:username decoded)))
    (is (= password (:password decoded)))
    (is (= realm (:realm decoded)))))


(deftest test-credentials-special 
  (let [username "~!@#$%^&*()_+{}[][|;';\",.<>?/"
        password "yeah"
        realm "shibboleth"
        basic (wrap username password realm)
        decoded (unwrap-credentials basic)]
    (is (= username (:username decoded)))
    (is (= password (:password decoded)))
    (is (= realm (:realm decoded)))))

