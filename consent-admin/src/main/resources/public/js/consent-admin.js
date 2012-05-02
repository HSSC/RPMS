var RPMS = {};
RPMS.setContext = function(x){RPMS.context = x};
RPMS.launchInPane = function(x){
	if(RPMS.context != null && x.indexOf(RPMS.context) != 0){
		x = RPMS.context + x;
	}
	alert("Launching In Pane: " + x);
}

RPMS.logout = function(){
	url = RPMS.context + "/logout";
	// Do local state check.  Ask to continue if there is state.
	alert("Change me to a jquery dialog.  I'm in the consent-admin.js file.")
	
	// If All Good.  Logout
	document.location = url;
};