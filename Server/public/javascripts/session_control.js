const questionSet = $("#questionSet").val();
const sessionId = $("#sessionId").val();
const sessionAuthenticator = $("#sessionAuthenticator").val();
const questionsList = $("#questionsList");
const questionListHeader = questionsList.find("li:first");
const endQuestion = $("#endQuestion");
const currentQuestion = $("#currentQuestion");
const incomingQuestions = $("#incomingQuestions");
const clearIncoming = $("#clearIncoming");
const aggregateChartCanvas = $("#currentQuestionAggregate");

let aggregateChart = null;
let aggregateChartData = {
	labels: [],
	datasets: []
};
let questionSetData;
let currentQuestionId = null;
let authenticatorWindow = null;
let webSocket;

function renderQuestions() {
	for (let i = questionSetData.length - 1; i >= 0; --i) {
		let question = $("<li class=\"list-group-item align-items-center question\"><span id=\"questionText\"></span></li>");
		question.find("#questionText").text(questionSetData[i]["title"]);
		question.data("question-id", i);
		question.insertAfter(questionListHeader);
	}
}

function renderCurrentQuestion() {
	if(currentQuestionId !== null) {
		currentQuestion.html("<b>Question Title:</b> <span id=\"questionText\"></span><br /><br />");
		currentQuestion.find("#questionText").text(questionSetData[currentQuestionId]["title"]);
		switch(questionSetData[currentQuestionId]["type"]) {
			case "MultipleChoice": {
					currentQuestion.append("<b>Question Type:</b> Multiple Choice<br /><b>Available Choices:</b>");
					let choicesList = $("<ul>");
					for (let i = 0; i < questionSetData[currentQuestionId]["data"]["choices"].length; ++i) {
						choicesList.append($("<li>").text(questionSetData[currentQuestionId]["data"]["choices"][i]));
					}
					currentQuestion.append(choicesList);
					currentQuestion.append("<b>Scramble Choices: </b>" + (questionSetData[currentQuestionId]["data"]["scramble"] ? "Yes" : "No") + "<br />");
					renderChart();
					aggregateChartCanvas.show();

				}
				break;

			case "RankedChoice": {
					currentQuestion.append("<b>Question Type:</b> Ranked Choice<br /><b>Available Choices:</b>");
					let choicesList = $("<ul>");
					for (let i = 0; i < questionSetData[currentQuestionId]["data"]["choices"].length; ++i) {
						choicesList.append($("<li>").text(questionSetData[currentQuestionId]["data"]["choices"][i]));
					}
					currentQuestion.append(choicesList);
					renderChart();
					aggregateChartCanvas.show();
				}
				break;

			case "BooleanAnswer":
				currentQuestion.append("<b>Question Type:</b> Boolean Answer");
				renderChart();
				aggregateChartCanvas.show();
				break;

			case "ShortAnswer":
				currentQuestion.append("<b>Question Type:</b> Short Answer");
				break;
		}
	} else {
		currentQuestion.html("<b>No question currently selected.</b>");
	}
}

function renderChart() {
	if(currentQuestionId == null) {
		return;
	}

	if(aggregateChart == null) {
		aggregateChart = new Chart(aggregateChartCanvas, {
			type: 'horizontalBar',
			data: aggregateChartData,
			options: {}
		});
	}

	$.ajax({
		"method": "GET",
		"url": window.location.href.split('#')[0] + "/../aggregate-response/" + questionSetData[currentQuestionId]["id"],
		"dataType": "JSON",
		"success": function(data) {
			console.log(data);

			let dataSet = [];

			aggregateChartData.labels.length = 0;

			for(let answer in data) {
				if(data.hasOwnProperty(answer)) {
					aggregateChartData.labels.push(answer);
					dataSet.push(data[answer]);
					console.log(answer);
				}
			}

			aggregateChartData.datasets.length = 0;
			aggregateChartData.datasets.push({"label": "Student Responses", "data": dataSet});
			aggregateChart.update();
		}
	});
}

function bindActions() {
	questionsList.on("click", ".question", function() {
		const questionId = $(this).data("question-id");
		setQuestion(questionId);
		renderCurrentQuestion();
	});

	endQuestion.on("click", function() {
		setQuestion(null);
		renderCurrentQuestion();
	});

	clearIncoming.on("click", function() {
		incomingQuestions.html("");
	});

	$("#connectionLostModalReconnect").on("click", function() {
		window.location.reload();
	});

	$("#showAuthenticator").on("click", function() {
		const authenticatorWindowDimension = Math.floor(Math.min(screen.width, screen.height) * 0.75);
		authenticatorWindow = window.open(window.location.href.split('#')[0] + "/../authenticate", "SessionAuthenticate_" + sessionId, "menubar=0,toolbar=0,location=0,status=0,fullscreen=0,width=" + authenticatorWindowDimension + ",height=" + authenticatorWindowDimension);
	});

	$("#endSession").on("click", function() {
		// TODO: Make a request to close the session.
	});
}

function setQuestion(newQuestionId) {
	let sendQuestionId = (newQuestionId === null) ? -1 : questionSetData[newQuestionId]["id"];

	webSocket.send(JSON.stringify({
		"messageType": "SEND_QUESTION",
		"messageData": {
			"question_id": sendQuestionId
		}
	}));

	endQuestion.prop("disabled", newQuestionId === null);
	aggregateChartCanvas.hide();
	currentQuestionId = newQuestionId;
}

$(document).ready(function() {
	webSocket = new WebSocket(window.location.href.split('#')[0].replace('http', 'ws') + "/connect");

	webSocket.onopen = function() {
		webSocket.send(JSON.stringify({
			"messageType": "CONNECT_SESSION",
			"messageData": sessionAuthenticator
		}));
	};

	webSocket.onmessage = function(message) {
		message = JSON.parse(message["data"]);

		switch(message["messageType"]) {
			case "CONNECT_SESSION":
				if(message["messageData"]["status"] === "OK") {
					let questionId = message["messageData"]["statusExtended"]["currentQuestion"];
					for(let i = 0; i < questionSetData.length; ++i) {
						if(questionSetData[i]["id"] === questionId) {
							currentQuestionId = i;
							break;
						}
					}
					endQuestion.prop("disabled", currentQuestionId === null);
					renderCurrentQuestion();
				}
				break;

			case "ASK_QUESTION":
				let incomingQuestion = $("<div class=\"alert alert-primary alert-dismissible fade show\" role=\"alert\"><b>Question:</b><br /><span id=\"questionText\"></span><hr><b>Device ID:</b> <span id=\"deviceId\"></span><button type=\"button\" class=\"close\" data-dismiss=\"alert\" aria-label=\"Close\"><span aria-hidden=\"true\">&times;</span></button></div>");
				incomingQuestion.find("#questionText").text(message["messageData"]["message"]);
				incomingQuestion.find("#deviceId").text(message["messageData"]["device"]);
				incomingQuestions.prepend(incomingQuestion);
				break;

			case "RESPONSE_UPDATE":
				renderChart();
				break;
		}
	};

	webSocket.onclose = function() {
		$("#connectionLostModal").modal();
	};

	$.ajax({
		"method": "POST",
		"url": "../../question-set/" + questionSet + "/getQuestions",
		"dataType": "JSON",
		"success": function(data) {
			questionSetData = data;
			renderQuestions();
		}
	});

	bindActions();
});