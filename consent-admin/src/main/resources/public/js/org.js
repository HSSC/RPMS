var get_selected_org = function () {	
	var selected_org = $('.organization.ui-selected').data('org-id');
	if (selected_org) {
		return selected_org;
	} else {
		return "";}
};

$('#org-list').selectable({
	stop: function(e, ui) {
			$('.ui-selected:first', this).each(function() {
				$(this).siblings().removeClass('ui-selected');
			});
		},
	filter: '.organization',
	selecting: function(e, ui) {
			$('.ui-selected:first', this).each(function() {
				$(this).siblings().removeClass('ui-selected');
			});
		}
	}

);

$('#org-func').children().button();



$('#add-admin').click(function () {
	var sel_org = get_selected_org();
	if (sel_org) {
        PaneManager.push('user/add', {'organization': sel_org}, {}); }
});
$('#new-org').click(function () {
        PaneManager.push('organization/add', {}, {}); 
});

$('.done-button').click(function () {
        PaneManager.pop({}); 
});

$('#edit-org').click(function () {
	var sel_org = get_selected_org();
	if (sel_org) {
        PaneManager.push('organization/edit', {'organization': sel_org}, {});}
});
