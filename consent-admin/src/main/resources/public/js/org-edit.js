

var save_edit_org = function () {
var org_opts = {
  url: '/api/organization/edit',
  target: '#form-status',
  type: 'POST'
};

  $('#edit-org-form').ajaxSubmit(org_opts);
 setTimeout(function() {PaneManager.pop({})}, 1500) ;
};

$('#save-button').button();
$('#save-button').click(save_edit_org);


