(ns org.healthsciencessc.rpms2.consent-admin.ui.navigation
  (require [sandbar.stateful-session :as sess]
           [org.healthsciencessc.rpms2.consent-domain.roles :as roles]))

(defn- use?
  "Checks to see if a structure can be rendered into a navigation group or item."
  [target]
  (if-let [pred (:use? target)]
    (pred)
    true))
  
(defn- admin-or-super?
  "Checks if the current user is an administrator or super administrator."
  []
  (if-let [user (sess/session-get :user)]
    (or (roles/superadmin? user)(roles/admin? user))
    false))

(defn- super?
  "Checks if the current user is a super administrator."
  []
  (roles/superadmin? (sess/session-get :user)))

(defn- designer?
  "Checks if the current user is an administrator or super administrator."
  []
  (roles/protocol-designer? (sess/session-get :user)))

(defn- manager?
  "Checks if the current user is an administrator or super administrator."
  []
  (roles/consent-manager? (sess/session-get :user)))

(defn- default-item-generator
  "Checks if the current user is an administrator or super administrator."
  [item]
  [:li.navitem [:a {:href "#" :onclick (str "PaneManager.stack('" (:url item) "', {}, {})") } (:label item)]])

(defn- protocol-location-item-generator
  "Checks if the current user is an administrator or super administrator."
  [item]
  (for [mapping (roles/protocol-designer-mappings (sess/session-get :user))]
    (let [loc (:location mapping)
          label (:name loc)
          id (:id loc)]
      [:li.navitem 
        [:a {:href "#" 
             :onclick (str "PaneManager.stack('" (:url item) "', {location: '" id "'}, {})") } label]]
    )))

(defn- default-group-generator
  "Generates the output of a group record."
  [group]
  (list [:h4.navlabel [:a {:href "#"} (:group group)]] 
        [:div.navpanel
          [:ul.navlist (for [item (:items group)]
                         (if (use? item)
                           (if (:generator item)
                             ((:generator item) item)
                             (default-item-generator item))))]]))

(def groupings 
  [
    {:group "Organization" :use? admin-or-super?
      :items [{:url "/view/organizations" :label "Organizations" :use? super? }
              {:url "/view/organization" :label "Settings"}
              {:url "/view/locations" :label "Locations"}]}
    {:group "Security" :use? admin-or-super?
      :items [{:url "/view/users" :label "Users"}
              {:url "/view/groups" :label "Groups"}
              {:url "/view/roles" :label "Roles"}]}
    {:group "Protocols" :use? designer?
      :items [{:url "/view/protocol/location" :label "Locations" :generator protocol-location-item-generator}]}
    {:group "Library" :use? designer?
      :items [{:url "/view/policy/definitions" :label "Policy Definitions"}
              {:url "/view/policies" :label "Policies"}
              {:url "/view/metaitems" :label "Meta Items"}
              {:url "/view/widgets" :label "Widgets"}]}
    {:group "Management" :use? manager?
      :items [{:url "/view/consenter/history" :label "Consenter History"}
              {:url "/view/audit" :label "Audit"}]}
  ])

(defn- generate-group
  "Checks to see if a structure can be rendered into a navigation group or item."
  [group]
  (if-let [generator (:generator group)]
    (generator group)
    (default-group-generator group)))

(defn navigator
  "Generates the navigator menu."
  []
  (list [:div#navigator
          (for [group groupings]
            (if (use? group)
              (generate-group group)))]
        [:script 
"$(function() {
  $( \"#navigator\" ).accordion({
    collapsible: true,
    fillSpace: true,
    autoHeight: false
  });
});"]))
