const addQuestion = $("#addQuestion");
const questionTextarea = $("#questionTextarea");
const submitTarget = $("#submitTarget").val();

let questionData = {
	"title": ""
};

function bindActions() {
	questionTextarea.on("input", function() {
		questionData["title"] = questionTextarea.val();
		checkCompletion();
	});

	addQuestion.on("click", function(event) {
		event.preventDefault();
		const submitError = $("#submitError");

		submitError.empty();

		$.ajax({
			"method": "POST",
			"url": submitTarget,
			"data": {
				"question_type": "ShortAnswer",
				"question_title": questionData["title"],
				"question_data": "{}"
			},
			"dataType": "JSON",
			"success": function(data) {
				if(data["status"] == "OK") {
					window.location = data["redirectTo"];
				} else {
					const submitErrorContent = $("<div class=\"alert alert-danger\" role=\"alert\"><b>Question Create Error</b> <span id=\"alertText\"></span></div>");
					submitErrorContent.find("#alertText").text(data["statusExtended"]);
					submitError.append(submitErrorContent);
				}
			}
		});
	});
}

function checkCompletion() {
	addQuestion.prop("disabled", ($.trim(questionData["title"]).length == 0));
}

$(document).ready(function() {
	bindActions();

	checkCompletion();
});