const questionsList = $("#questionsList");
const questionListHeader = questionsList.find("li:first");
const deleteModal = $("#deleteModal");
const baseUrl = $("#baseUrl").val();
const deleteQuestionSetModal = $("#deleteQuestionSetModal");

let questionSetData;

function moveObjectAtIndex(array, sourceIndex, destinationIndex) {
	let placeholder = {};
	let objectToMove = array.splice(sourceIndex, 1, placeholder)[0];
	array.splice(destinationIndex, 0, objectToMove);
	array.splice(array.indexOf(placeholder), 1);
}

function renderQuestions() {
	// Clear out existing answer choices.
	questionsList.find(".question").remove();

	if(questionSetData.length > 0) {
		for (let i = questionSetData.length - 1; i >= 0; --i) {
			let question = $("<li class=\"list-group-item align-items-center question\"><i class=\"fa fa-bars mr-2 question-handle\" aria-hidden=\"true\"></i><span id=\"questionText\"></span><a href=\"#\" class=\"question-delete float-right text-muted\"><i class=\"fa fa-trash\" aria-hidden=\"true\"></i></a><a href=\"#\" class=\"question-edit float-right text-muted\"><i class=\"fa fa-pencil mr-2\" aria-hidden=\"true\"></i></a></li>");
			question.find("#questionText").text(questionSetData[i]["title"]);
			question.data("question-id", i);
			question.insertAfter(questionListHeader);
		}
	} else {
		$("<li class=\"list-group-item align-items-center no-sort choice\">To create a new question, press \"Add a Question\" below.</li>").insertAfter(questionListHeader);
	}

	questionsList.sortable({
		"items": ":not(.no-sort)",
		"placeholderClass": "list-group-item",
		"handle": ".question-handle"
	});
}

function bindActions() {
	const questionSetNameModify = $("#questionSetNameModify");
	const questionSetName = $("#questionSetName");

	questionSetNameModify.hide();

	const questionSetNameTooltip = questionSetName.tooltip();
	questionSetNameTooltip.tooltip('show');
	setTimeout(function() {
		questionSetNameTooltip.tooltip('hide');
	}, 1000);

	questionSetName.on("click", function(event) {
		event.preventDefault();
		questionSetNameModify.show();
		$("#questionSetNameDisplay").hide();
	});

	questionsList.on("sortupdate", function(event, ui) {
		let movedItem = $(ui["item"]);
		let questionId = movedItem.data("question-id");

		let questionIdNext = movedItem.next().data("question-id");
		if(questionIdNext === undefined) {
			questionIdNext = questionSetData.length;
		}

		moveObjectAtIndex(questionSetData, questionId, questionIdNext);
		renderQuestions();

		let question_order = [];
		for(let i = 0; i < questionSetData.length; ++i) {
			question_order.push(questionSetData[i].id);
		}

		$.ajax({
			"method": "POST",
			"url": baseUrl + "/setQuestionOrder",
			"data": {
				"question_order": JSON.stringify(question_order)
			},
			"dataType": "JSON",
			"success": function(data) {
				if(data["status"] != "OK") {
					window.location.reload();
				}
			}
		});
	});

	questionsList.on("click", ".question-edit", function() {
		window.location = baseUrl + "/edit/" + questionSetData[$(this).parent("li").data("question-id")].id;
	});

	questionsList.on("click", ".question-delete", function () {
		deleteModal.data("question-id", $(this).parent("li").data("question-id"));
		deleteModal.modal();
	});

	$("#deleteModalConfirm").on("click", function() {
		$.ajax({
			"method": "POST",
			"url": baseUrl + "/deleteQuestion",
			"data": {
				"question_id": questionSetData[deleteModal.data("question-id")].id
			},
			"dataType": "JSON",
			"success": function() {
				window.location.reload();
			}
		});
	});

	$("#deleteQuestionSet").on("click", function() {
		deleteQuestionSetModal.modal();
	});
}

$(document).ready(function() {
	$.ajax({
		"method": "POST",
		"url": baseUrl + "/../getQuestions",
		"dataType": "JSON",
		"success": function(data) {
			questionSetData = data;
			renderQuestions();
		}
	});

	bindActions();
});