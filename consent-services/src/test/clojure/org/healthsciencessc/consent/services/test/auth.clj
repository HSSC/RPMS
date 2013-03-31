(ns org.healthsciencessc.consent.services.test.auth
  (:use [org.healthsciencessc.consent.services.auth]
        [clojure.test])
  (:require [clojure.data.codec.base64 :as b64]))

(def test-authenticate
  (fn [username password]
    (when (and (= username "foo") (= password "foobar"))
      {:username "foo" :password "foobar" :organization {:name "MUSC"}})))

(defn encode-creds
  [username password]
  (str "Basic " (String. (b64/encode (.getBytes (str username ":" password))))))

(deftest decodes-base64-string
  (is (= "foobar" (decode-cred "Zm9vYmFy"))))

(deftest auth-success
  (is (= :pass
         ((wrap-authentication (fn [_] :pass)
                               test-authenticate)
          {:headers {"authorization" (encode-creds "foo" "foobar")}}))))

(deftest add-authenticated-user-to-session
  (is (= "foo" (get-in ((wrap-authentication (fn [req] req)
                                             test-authenticate)
                        {:headers {"authorization" (encode-creds "foo" "foobar")}})
                       [:session :current-user :username]))))

(deftest unauthorized-user
  (is (= 401 (:status ((wrap-authentication (fn [req] req)
                                            test-authenticate)
                       {:headers {"authorization" (encode-creds "bar" "baz")}})))))
