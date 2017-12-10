let rankedChoiceData = {
	title: "",
	question_data: {
		choices: []
	}
};

function moveObjectAtIndex(array, sourceIndex, destinationIndex) {
	let placeholder = {};
	let objectToMove = array.splice(sourceIndex, 1, placeholder)[0];
	array.splice(destinationIndex, 0, objectToMove);
	array.splice(array.indexOf(placeholder), 1);
}

function renderChoices() {
	let multipleChoiceAnswers = jQuery("#multipleChoiceAnswers");
	let multipleChoiceAnswersHeader = multipleChoiceAnswers.find("li:first");

	// Clear out existing answer choices.
	multipleChoiceAnswers.find(".choice").remove();

	if(rankedChoiceData.question_data.choices.length > 0) {
		for (let i = rankedChoiceData.question_data.choices.length - 1; i >= 0; --i) {
			let choiceText = rankedChoiceData.question_data.choices[i];
			let choice = jQuery("<li class=\"list-group-item align-items-center choice\">" + choiceText + "<a href=\"#\" class=\"choice-delete float-right text-muted\"><i class=\"fa fa-trash\" aria-hidden=\"true\"></i></a><a href=\"#\" class=\"choice-edit float-right text-muted\"><i class=\"fa fa-pencil mr-2\" aria-hidden=\"true\"></i></a></li>");
			choice.data("choice-id", i);
			choice.insertAfter(multipleChoiceAnswersHeader);
		}
	} else {
		jQuery("<li class=\"list-group-item align-items-center no-sort choice\">To create a new ranked choice option, press \"Add an Option\" below.</li>").insertAfter(multipleChoiceAnswersHeader);
	}

	jQuery(".choice-delete").on("click", function () {
		const choiceId = jQuery(this).parent("li").data("choice-id");
		const deleteModal = jQuery("#deleteModal");

		deleteModal.modal().modal("show");
		jQuery("#deleteModalConfirm").on("click", function() {
			rankedChoiceData.question_data.choices.splice(choiceId, 1);
			renderChoices();
			deleteModal.modal("hide");
			jQuery(this).unbind("click");
			checkCompletion();
		});
	});

	jQuery(".choice-edit").on("click", function() {
		const choiceId = jQuery(this).parent("li").data("choice-id");
		const editModal = jQuery("#editModal");
		const editModalText = jQuery("#editModalText");

		// Set edit modal text.
		editModalText.val(rankedChoiceData.question_data.choices[choiceId]);

		// Initialize and show modal.
		editModal.modal().modal("show");

		// Bind edit modal save button.
		jQuery("#editModalSave").on("click", function() {
			rankedChoiceData.question_data.choices[choiceId] = editModalText.val();
			renderChoices();
			editModal.modal("hide");
			jQuery(this).unbind("click");
		});
	});
}

function bindActions() {
	const addModal = jQuery("#addModal");
	const addModalText = jQuery("#addModalText");
	const questionTextarea = jQuery("#questionTextarea");

	jQuery("#choiceAdd").on("click", function() {
		addModalText.val("");
		addModalText.trigger("input");
		addModal.modal().modal("show");
	});

	addModalText.on("input", function() {
		jQuery("#addModalAdd").prop("disabled", !(jQuery(this).val().length > 0));
	});

	jQuery("#addModalAdd").on("click", function() {
		rankedChoiceData.question_data.choices.push(addModalText.val());
		renderChoices();
		addModal.modal("hide");
		checkCompletion();
	});

	questionTextarea.on("input", function() {
		rankedChoiceData.title = questionTextarea.val();
		checkCompletion();
	});

	jQuery("#multipleChoiceAnswers").on("sortupdate", function(event, ui) {
		let movedItem = jQuery(ui.item);
		let choiceId = movedItem.data("choice-id");

		let choiceIdNext = movedItem.next().data("choice-id");
		if(choiceIdNext === undefined) {
			choiceIdNext = rankedChoiceData.question_data.choices.length;
		}

		moveObjectAtIndex(rankedChoiceData.question_data.choices, choiceId, choiceIdNext);
		renderChoices();
	});

	jQuery("#addQuestion").on("click", function(event) {
		event.preventDefault();
		const submitError = jQuery("#submitError");

		submitError.empty();


		jQuery.ajax({
			method: "POST",
			url: jQuery("#submitTarget").val(),
			data: {
				question_type: "RankedChoice",
				question_title: rankedChoiceData.title,
				question_data: JSON.stringify(rankedChoiceData.question_data)
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
	jQuery("#addQuestion").prop("disabled", (jQuery.trim(rankedChoiceData.title).length == 0 || rankedChoiceData.question_data.choices.length < 2));
}

renderChoices();
bindActions();
checkCompletion();