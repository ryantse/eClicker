let shortAnswerData = {
	title: ""
};

function bindActions() {
	const questionTextarea = jQuery("#questionTextarea");

	questionTextarea.on("input", function() {
		shortAnswerData.title = questionTextarea.val();
		checkCompletion();
	});

	jQuery("#saveQuestion").on("click", function(event) {
		event.preventDefault();
		const submitError = jQuery("#submitError");

		submitError.empty();

		jQuery.ajax({
			method: "POST",
			url: jQuery("#dataTarget").val() + "/modify",
			data: {
				question_title: shortAnswerData.title,
				question_data: JSON.stringify({})
			},
			dataType: "JSON",
			success: function(data) {
				if(data.status == "OK") {
					window.location = data.redirectTo;
				} else {
					submitError.append("<div class=\"alert alert-danger\" role=\"alert\"><b>Question Create Error</b> " + data.statusExtended + "</div>")
				}
			}
		});
	});
}

function checkCompletion() {
	jQuery("#saveQuestion").prop("disabled", (jQuery.trim(shortAnswerData.title).length == 0));
}

function loadData() {
	jQuery.ajax({
		method: "GET",
		url: jQuery("#dataTarget").val() + "/data",
		dataType: "JSON",
		success: function(data) {
			shortAnswerData.title = data.question_title;
			jQuery("#questionTextarea").val(shortAnswerData.title);
			checkCompletion();
		}
	});
}

bindActions();
checkCompletion();
loadData();