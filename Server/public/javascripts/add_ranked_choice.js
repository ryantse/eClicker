const multipleChoiceAnswers = $("#multipleChoiceAnswers");
const multipleChoiceAnswersHeader = multipleChoiceAnswers.find("li:first");
const addModal = $("#addModal");
const addModalText = $("#addModalText");
const addModalAdd = $("#addModalAdd");
const questionTextarea = $("#questionTextarea");
const deleteModal = $("#deleteModal");
const editModal = $("#editModal");
const editModalSave = $("#editModalSave");
const editModalText = $("#editModalText");
const addQuestion = $("#addQuestion");
const submitTarget = $("#submitTarget").val();

let questionData = {
	"title": "",
	"question_data": {
		"choices": []
	}
};

function renderChoices() {
	multipleChoiceAnswers.find(".choice").remove();

	if(questionData["question_data"]["choices"].length > 0) {
		for(let i = (questionData["question_data"]["choices"].length - 1); i >= 0; --i) {
			let choice = $("<li class=\"list-group-item align-items-center choice\"><span id=\"choiceText\"></span><a href=\"#\" class=\"choice-delete float-right text-muted\"><i class=\"fa fa-trash\" aria-hidden=\"true\"></i></a><a href=\"#\" class=\"choice-edit float-right text-muted\"><i class=\"fa fa-pencil mr-2\" aria-hidden=\"true\"></i></a></li>");
			choice.find("#choiceText").text(questionData["question_data"]["choices"][i]);
			choice.data("choice-id", i);
			choice.insertAfter(multipleChoiceAnswersHeader);
		}
	} else {
		$("<li class=\"list-group-item align-items-center no-sort choice\">To create a new ranked choice option, press \"Add an Option\" below.</li>").insertAfter(multipleChoiceAnswersHeader);
	}
}

function bindActions() {
	// Delete Modal.
	multipleChoiceAnswers.on("click", ".choice-delete", function() {
		deleteModal.data("choice-id", $(this).parent("li").data("choice-id"));
		deleteModal.modal();
	});

	$("#deleteModalConfirm").on("click", function() {
		questionData["question_data"]["choices"].splice(deleteModal.data("choice-id"), 1);
		deleteModal.modal("hide");

		renderChoices();
		checkCompletion();
	});

	// Edit Modal.
	multipleChoiceAnswers.on("click", ".choice-edit", function() {
		const choiceId = $(this).parent("li").data("choice-id");
		editModal.data("choice-id", choiceId);
		editModalText.val(questionData["question_data"]["choices"][choiceId]);
		editModal.modal();
	});

	editModalText.on("input", function() {
		editModalSave.prop("disabled", !($(this).val().length > 0));
	});

	editModalText.keypress(function (event) {
		if(event.which === 13) {
			$("#editModalSave:enabled").click();
			return false;
		}
	});

	editModalSave.on("click", function() {
		questionData["question_data"]["choices"][editModal.data("choice-id")] = editModalText.val();
		editModal.modal("hide");

		renderChoices();
	});

	// Add Modal.
	$(".choiceAdd").on("click", function() {
		addModalText.val("");
		addModalText.trigger("input");

		addModal.modal();
	});

	addModalText.on("input", function() {
		addModalAdd.prop("disabled", !($(this).val().length > 0));
	});

	addModalText.keypress(function (event) {
		if(event.which === 13) {
			$("#addModalAdd:enabled").click();
			return false;
		}
	});

	addModalAdd.on("click", function() {
		questionData["question_data"]["choices"].push(addModalText.val());
		addModal.modal("hide");

		renderChoices();
		checkCompletion();
	});

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
				"question_type": "RankedChoice",
				"question_title": questionData["title"],
				"question_data": JSON.stringify(questionData["question_data"])
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
	addQuestion.prop("disabled", ($.trim(questionData["title"]).length == 0 || questionData["question_data"]["choices"].length < 2));
}

$(document).ready(function() {
	renderChoices();
	bindActions();
	checkCompletion();
});