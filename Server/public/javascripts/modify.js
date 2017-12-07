const questionSetNameModify = jQuery("#questionSetNameModify");
const questionSetName = jQuery("#questionSetName");

questionSetNameModify.hide();

const questionSetNameTooltip = questionSetName.tooltip();
questionSetNameTooltip.tooltip('show');
setTimeout(function() {
	questionSetNameTooltip.tooltip('hide');
}, 1000);

questionSetName.on("click", function(event) {
	event.preventDefault();
	questionSetNameModify.show();
	jQuery("#questionSetNameDisplay").hide();
});

jQuery(".list-group").sortable({
	items: ':not(.no-sort)',
	placeholderClass: 'list-group-item',
	handle: 'i'
});