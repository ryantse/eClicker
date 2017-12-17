const questionTextarea = $("#questionTextarea");
const dataTarget = $("#dataTarget").val();

let questionData = {
	"title": ""
};

function bindActions() {
	questionTextarea.on("input", function() {
		questionData["title"] = questionTextarea.val();
		checkCompletion();
	});

	$("#cancel").on("click", function() {
		window.location = dataTarget + "/../../";
	});

	$("#saveQuestion").on("click", function(event) {
		event.preventDefault();
		const submitError = $("#submitError");

		submitError.empty();

		$.ajax({
			"method": "POST",
			"url": dataTarget + "/modify",
			"data": {
				"question_title": questionData["title"],
				"question_data": "{}"
			},
			"dataType": "JSON",
			"success": function(data) {
				if(data["status"] == "OK") {
					window.location = data["redirectTo"];
				} else {
					const submitErrorContent = $("<div class=\"alert alert-danger\" role=\"alert\"><b>Question Modify Error</b> <span id=\"alertText\"></span></div>");
					submitErrorContent.find("#alertText").text(data["statusExtended"]);
					submitError.append(submitErrorContent);
				}
			}
		});
	});
}

function checkCompletion() {
	$("#saveQuestion").prop("disabled", ($.trim(questionData["title"]).length == 0));
}

function loadData() {
	$.ajax({
		"method": "GET",
		"url": dataTarget+ "/data",
		"dataType": "JSON",
		"success": function(data) {
			questionData["title"] = data["question_title"];
			questionTextarea.val(questionData["title"]);

			checkCompletion();
		}
	});
}

$(document).ready(function() {
	bindActions();
	loadData();
});
