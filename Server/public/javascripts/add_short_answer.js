let shortAnswerData = {
	title: ""
};

function bindActions() {
	const questionTextarea = jQuery("#questionTextarea");

	questionTextarea.on("input", function() {
		shortAnswerData.title = questionTextarea.val();
		checkCompletion();
	});

	jQuery("#addQuestion").on("click", function(event) {
		event.preventDefault();
		const submitError = jQuery("#submitError");

		submitError.empty();

		jQuery.ajax({
			method: "POST",
			url: jQuery("#submitTarget").val(),
			data: {
				question_type: "ShortAnswer",
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
	jQuery("#addQuestion").prop("disabled", (jQuery.trim(shortAnswerData.title).length == 0));
}

bindActions();
checkCompletion();