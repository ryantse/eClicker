function bindActions() {
	$("#deleteSession").on("click", function() {
		$("#deleteModal").modal();
	});
}

$(document).ready(function() {
	bindActions();
});