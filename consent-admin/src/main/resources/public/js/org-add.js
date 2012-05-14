


var save_org = function () {
	var org_opts = {
	  url: '/api/organization/add',
	  target: '#add-form-status',
	  type: 'POST'
	};
  $('#add-org-form').ajaxSubmit(org_opts);
  $('#add-form-status').show();
  setTimeout(function() {PaneManager.pop({})}, 1500)
};

$('#save-button').button();
$('#save-button').click(save_org);


