(ns org.healthsciencessc.consent.manager.ui.login
  (:require [hiccup.form :as form]
            [hiccup.element :as element]))

(defn ui-login-form
  "Generates the login form"
  [ctx]
  (let [error (:error ctx)]
    (list [:div#login-pane  
        (if error 
          [:div#error (:message error)] nil)
        (form/form-to [:post ""] 
          (form/label "username" "Username")
          (form/text-field "username")
          (form/label "password" "Password")
          (form/password-field "password")
          )]
    (element/javascript-tag 
"$('#login-pane').dialog({
	autoOpen: true,
	closeOnEscape: false,
	draggable: false,
	height: 350,
	width: 350,
	modal: true,
	resizable: false,
	title: 'Login Required',
	buttons: {
		'Login': function() {
			$('form').submit();
		}
	},
	open: function(event, ui) {
		$(this).closest('.ui-dialog').find('.ui-dialog-titlebar-close').hide();
	}
});"
))))
