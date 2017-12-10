let booleanAnswerData = {
	title: ""
};

function bindActions() {
	const questionTextarea = jQuery("#questionTextarea");

	questionTextarea.on("input", function() {
		booleanAnswerData.title = questionTextarea.val();
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
				question_title: booleanAnswerData.title,
				question_data: JSON.stringify(booleanAnswerData.question_data)
			},
			dataType: "JSON",
			success: function(data) {
				if(data.status == "OK") {
					window.location = data.redirectTo;
				} else {
					submitError.append("<div class=\"alert alert-danger\" role=\"alert\"><b>Question Modify Error</b> " + data.statusExtended + "</div>")
				}
			}
		});
	});

	jQuery("#cancel").on("click", function(event) {
		window.location = jQuery("#dataTarget").val() + "/../../";
	});
}

function loadData() {
	jQuery.ajax({
		method: "GET",
		url: jQuery("#dataTarget").val() + "/data",
		dataType: "JSON",
		success: function(data) {
			booleanAnswerData.title = data.question_title;
			jQuery("#questionTextarea").val(booleanAnswerData.title);
			booleanAnswerData.question_data = data.question_data;
			checkCompletion();
		}
	});
}

function checkCompletion() {
	jQuery("#addQuestion").prop("disabled", (jQuery.trim(booleanAnswerData.title).length == 0));
}

bindActions();
checkCompletion();
loadData();