(ns org.healthsciencessc.rpms2.consent-domain.test.roles
  (:use [org.healthsciencessc.rpms2.consent-domain.roles]
        [org.healthsciencessc.rpms2.consent-domain.types]
        [clojure.test]))

(defn- UUID
  []
  (java.util.UUID/randomUUID))

(def test-baseorg {:id (UUID)  :name "Default Organization" :code code-base-org})
(def test-org {:id (UUID)  :name "Test Organization" :code "testorg"})
(def test-loc {:id (UUID)  :name "Test Location" :code "testloc" :organization test-org})
(def test-super-role {:id (UUID)  :name "Designer" :code code-role-superadmin :organization test-baseorg})
(def test-admin-role {:id (UUID)  :name "Designer" :code code-role-admin :organization test-baseorg})
(def test-designer-role {:id (UUID)  :name "Designer" :code code-role-designer :organization test-baseorg})
(def test-collector-role {:id (UUID)  :name "Designer" :code code-role-collector :organization test-baseorg})
(def test-manager-role {:id (UUID)  :name "Designer" :code code-role-consentmanager :organization test-baseorg})
(def test-system-role {:id (UUID)  :name "Designer" :code code-role-externalsystem :organization test-baseorg})

(def test-super-role-mapping {:id (UUID) :role test-super-role :organization test-baseorg})
(def test-admin-role-mapping {:id (UUID) :role test-admin-role :organization test-org})
(def test-designer-role-mapping {:id (UUID) :role test-designer-role :organization test-org :location test-loc})
(def test-collector-role-mapping {:id (UUID) :role test-collector-role :organization test-org :location test-loc})
(def test-manager-role-mapping {:id (UUID) :role test-manager-role :organization test-org :location test-loc})
(def test-system-role-mapping {:id (UUID) :role test-system-role :organization test-org :location test-loc})

(def test-super {:id (UUID) :username "super" :password "password" :organization test-baseorg
                 :role-mappings [test-super-role-mapping]})

(def test-admin {:id (UUID) :username "admin" :password "password" :organization test-org
                 :role-mappings [test-admin-role-mapping]})

(def test-designer {:id (UUID) :username "designer" :password "password" :organization test-org
                 :role-mappings [test-designer-role-mapping]})

(def test-collector {:id (UUID) :username "collector" :password "password" :organization test-org
                 :role-mappings [test-collector-role-mapping]})

(def test-manager {:id (UUID) :username "manager" :password "password" :organization test-org
                 :role-mappings [test-manager-role-mapping]})

(def test-system {:id (UUID) :username "system" :password "password" :organization test-org
                 :role-mappings [test-system-role-mapping]})

(def test-admin-designer {:id (UUID) :username "admindesigner" :password "password" :organization test-org
                 :role-mappings [test-admin-role-mapping test-designer-role-mapping]})

(def test-collector-designer {:id (UUID) :username "collectordesigner" :password "password" :organization test-org
                 :role-mappings [test-collector-role-mapping test-designer-role-mapping]})

(deftest test-has-role
  (is (has-role? test-super {:id (:id test-super-role)})
       "Falsely reported that a user was invalid")
  (is (has-role? test-super {:code code-role-superadmin }) "Falsely reported a superadmin as invalid")
  (is (has-role? test-super {:code code-role-superadmin } :organization {:id (:id test-baseorg)})
    "Falsely reported a user with constraints as invalid")
  (is (not (has-role? test-super {:code "sadfmin"}))
      "User named sadfmin is not valid!")
  (is (not (has-role? test-super {:code code-role-superadmin } :organization {:id 2}
           :location {:code "loc" :id 123}))
      "User with multiple constraint varargs isn't valid")
  (is (has-role? test-designer {:code code-role-designer } :organization {:id (:id test-org)}
           :location {:code (:code test-loc) })
      "User with multiple constraint varargs should be valid"))

(deftest test-roles-no-constraints
  (is (superadmin? test-super))
  (is (admin? test-admin))
  (is (protocol-designer? test-designer))
  (is (consent-collector? test-collector))
  (is (consent-manager? test-manager))
  (is (system? test-system))
  (is (not (superadmin? test-system)))
  (is (not (admin? test-system)))
  (is (not (protocol-designer? test-system)))
  (is (not (consent-collector? test-system)))
  (is (not (consent-manager? test-system)))
  (is (not (system? test-super))))

(deftest test-roles-constraints
  (is (superadmin? test-super :organization {:id (:id test-baseorg)}))
  (is (admin? test-admin :organization {:id (:id test-org)}))
  (is (protocol-designer? test-designer :location {:id (:id test-loc)}))
  (is (consent-collector? test-collector :location {:id (:id test-loc)}))
  (is (consent-manager? test-manager :location {:id (:id test-loc)}))
  (is (system? test-system :location {:id (:id test-loc)}))
  
  ;; Test that should fail - passing in bad constraint.
  (is (not (superadmin? test-super :organization {:id (:code test-baseorg)})))
  (is (not (admin? test-admin :organization {:id (:code test-org)})))
  (is (not (protocol-designer? test-designer :location {:id (:code test-loc)})))
  (is (not (consent-collector? test-collector :location {:id (:code test-loc)})))
  (is (not (consent-manager? test-manager :location {:id (:code test-loc)})))
  (is (not (system? test-system :location {:id (:code test-loc)}))))

(deftest test-find-role-mappings
  (is (< 0 (count (consent-collector-mappings test-collector))))
  (is (< 0 (count (consent-collector-mappings test-collector  :location {:id (:id test-loc)} ))))
  (is (not (< 0 (count (consent-collector-mappings test-collector  :location {:id (:code test-loc)} )))))
  (is (< 0 (count (protocol-designer-mappings test-designer))))
  (is (< 0 (count (protocol-designer-mappings test-designer  :location {:id (:id test-loc)} ))))
  (is (not (< 0 (count (protocol-designer-mappings test-designer  :location {:id (:code test-loc)} ))))))
