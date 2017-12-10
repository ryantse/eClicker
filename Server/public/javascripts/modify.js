let questionSetData;

function moveObjectAtIndex(array, sourceIndex, destinationIndex) {
	let placeholder = {};
	let objectToMove = array.splice(sourceIndex, 1, placeholder)[0];
	array.splice(destinationIndex, 0, objectToMove);
	array.splice(array.indexOf(placeholder), 1);
}

function renderQuestions() {
	let questionsList = jQuery("#questionsList");
	let questionListHeader = questionsList.find("li:first");

	// Clear out existing answer choices.
	questionsList.find(".question").remove();

	if(questionSetData.length > 0) {
		for (let i = questionSetData.length - 1; i >= 0; --i) {
			let questionText = questionSetData[i].title;
			let question = jQuery("<li class=\"list-group-item align-items-center question\"><i class=\"fa fa-bars mr-2 question-handle\" aria-hidden=\"true\"></i>" + questionText + "<a href=\"#\" class=\"question-delete float-right text-muted\"><i class=\"fa fa-trash\" aria-hidden=\"true\"></i></a><a href=\"#\" class=\"question-edit float-right text-muted\"><i class=\"fa fa-pencil mr-2\" aria-hidden=\"true\"></i></a></li>");
			question.data("question-id", i);
			question.insertAfter(questionListHeader);
		}
	} else {
		jQuery("<li class=\"list-group-item align-items-center no-sort choice\">To create a new question, press \"Add a Question\" below.</li>").insertAfter(questionListHeader);
	}

	jQuery(".question-edit").on("click", function() {
		const questionId = jQuery(this).parent("li").data("question-id");
		window.location = jQuery("#baseUrl").val() + "/edit/" + questionSetData[questionId].id;
	});

	jQuery(".question-delete").on("click", function () {
		const questionId = jQuery(this).parent("li").data("question-id");
		const deleteModal = jQuery("#deleteModal");

		deleteModal.modal().modal("show");
		jQuery("#deleteModalConfirm").on("click", function() {
			jQuery.ajax({
				method: "POST",
				url: jQuery("#baseUrl").val() + "/deleteQuestion",
				data: {
					"question_id": questionSetData[questionId].id
				},
				dataType: "JSON",
				success: function(data) {
					window.location.reload();
				}
			});
		});
	});

	questionsList.sortable({
		items: ':not(.no-sort)',
		placeholderClass: 'list-group-item',
		handle: '.question-handle'
	});
}

function bindActions() {
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

	jQuery("#questionsList").on("sortupdate", function(event, ui) {
		let movedItem = jQuery(ui.item);
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

		jQuery.ajax({
			method: "POST",
			url: jQuery("#baseUrl").val() + "/setQuestionOrder",
			data: {
				"question_order": JSON.stringify(question_order)
			},
			dataType: "JSON",
			success: function(data) {
				if(data.status != "OK") {
					window.location.reload();
				}
			}
		});
	});
}

jQuery.ajax({
	method: "POST",
	url: jQuery("#baseUrl").val() + "/getQuestions",
	dataType: "JSON",
	success: function(data) {
		questionSetData = data;
		renderQuestions();
	}
});

bindActions();