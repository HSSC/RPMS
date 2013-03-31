(function( window, undefined ) {
	var consent = window.Consent;
	if(consent == null){
		consent = window.Consent = {};
	}
	
	var action = consent.Action;
	if(action == null){
		action = consent.Action = {};
	}

	action.postData = function(event){
		var target = $(event.target);
		var url = Utils.DataSet.get(target, "url");
		var method = Utils.DataSet.get(target, "method") || "post";
		var data = Utils.DataSet.getObject(target, "parameters");
		var includeForm = Utils.DataSet.getBoolean(target, "include-form");
		if(includeForm){
			var targetForm = target.parents("form").first();
			var formData = Consent.Form.dataModel(targetForm, false);
			if(formData != null){
				data = data == null ? formData : $.extend(data, formData);
			}
		}
		if(method == "get"){
			url = Utils.Url.render(url, data);
			data = null;
		}
		else{
			url = Utils.Url.render(url);
		}
		Consent.Request.api(method, url, data);
	};

	action.nextView = function(event){
		var target = $(event.target);
		var url = Utils.DataSet.get(target, "url");
		var method = Utils.DataSet.get(target, "method") || "get";
		var data = Utils.DataSet.getObject(target, "parameters");
		var includeForm = Utils.DataSet.getBoolean(target, "include-form");
		if(includeForm){
			var targetForm = target.parents("form").first();
			var formData = Consent.Form.dataModel(targetForm, false);
			if(formData != null){
				data = data == null ? formData : $.extend(data, formData);
			}
		}
		Consent.Request.changePage(url, method, data);
	};
	action.selectDataListViewItem = function(event){
		var target = $(event.currentTarget);
		var data = Utils.DataSet.getObject(target, "item");
		var container = target.parents(".datalistview").first();
		var form = container.find("form");
		Consent.Form.applyData(form, data, {resetAll: true});
		target.parents("div.datalist").find("a").removeClass("datalistitem-selected");
		target.addClass("datalistitem-selected");
	};
	action.cancel = function(event){
		var target = $(event.currentTarget);
		var title = Utils.DataSet.get(target, "title");
		var message = Utils.DataSet.get(target, "message");
		var actions = [{label:"No"},{label: "Yes", callback: function(){
			Consent.Request.api("post", Utils.Url.render("/api/cancel/consent"), null);
		}}];
		Consent.Dialog.confirm(title, message, {actions: actions});
	};
	action.logout = function(event){
		Consent.Collect.reset();
		Consent.Request.api("GET", "/logout");
	};
	
})( window );

$(document).on("pageinit", function(event){
	// Register Actions
	$(event.target).find(".action-form-submit").on("click", function(event){Consent.Form.submit(event);});
	$(event.target).find(".action-form-submit-state").on("click", function(event){Consent.Form.submitState(event);});
	$(event.target).find(".action-post-data").on("click", Consent.Action.postData);
	$(event.target).find(".action-next-view").on("click", Consent.Action.nextView);
	$(event.target).find(".action-datalistviewitem").on("click", Consent.Action.selectDataListViewItem);
	$(event.target).find(".action-cancel").on("click", Consent.Action.cancel);
	$(event.target).find(".action-logout").on("click", Consent.Action.logout);
});
