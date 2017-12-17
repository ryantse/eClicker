const express = require('express');
const router = express.Router();
const DeviceTokenManager = require('../DeviceTokenManager');
const crypto = require('crypto');
const MobileClient = require('../MobileClient');
const SessionTokenManager = require('../SessionTokenManager');
const SessionsManager = require('../SessionsManager');
const database = require('../DatabaseManager');

router.ws('/session', function(ws, req) {
	const client = new MobileClient(ws);
});

router.post('/session/ask-question', function(req, res, next) {
	const sessionData = SessionTokenManager.retrieveSessionDevice(req["body"]["token"]);
	if(sessionData == null) {
		res.send({
			"status": "ERROR",
			"statusExtended": "AUTHENTICATOR_CODE_INVALID"
		});
		return;
	}


	if(!SessionsManager.existsSession(sessionData["sessionId"])) {
		res.send({
			"status": "ERROR",
			"statusExtended": "SESSION_EXPIRED"
		});
		return;
	}

	const session = SessionsManager.getSession(sessionData["sessionId"]);
	if(session.sendEducatorQuestion(sessionData["deviceId"], req["body"]["question"])) {
		res.send({
			"status": "OK"
		});
		return;
	}
	
	res.send({
		"status": "ERROR",
		"statusExtended": "EDUCATOR_UNAVAILABLE"
	});
});

router.post('/session/record-answer', function(req, res, next) {
	const sessionData = SessionTokenManager.retrieveSessionDevice(req["body"]["token"]);
	if(sessionData == null) {
		res.send({
			"status": "ERROR",
			"statusExtended": "AUTHENTICATOR_CODE_INVALID"
		});
		return;
	}


	if(!SessionsManager.existsSession(sessionData["sessionId"])) {
		res.send({
			"status": "ERROR",
			"statusExtended": "SESSION_EXPIRED"
		});
		return;
	}

	const session = SessionsManager.getSession(sessionData["sessionId"]);
	if(session.getQuestionId() == null || session.getQuestionId() !== parseInt(req["body"]["questionId"])) {
		res.send({
			"status": "ERROR",
			"statusExtended": "QUESTION_CLOSED"
		});
		return;
	}

	try {
		let answerData = JSON.parse(req["body"]["answerData"]);
		if(answerData["answer"] === undefined) {
			throw new Error();
		}
	} catch (error) {
		res.send({
			"status": "ERROR",
			"statusExtended": "INVALID_ANSWER"
		});
	}

	try {
		let databaseResult = database.query("INSERT INTO response (response_session, response_device, response_question, response_data) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE response_data = ?", [sessionData["sessionId"], sessionData["deviceId"], session.getQuestionId(), req["body"]["answerData"], req["body"]["answerData"]]);
		session.updateEducatorQuestionResponses(req["body"]["answerData"]);
		res.send({
			"status": "OK"
		});
	} catch (error) {
		console.log(error);
		res.send({
			"status": "ERROR",
			"statusExtended": "INTERNAL_ERROR"
		});
	}
});

router.get('/register-device', function(req, res, next) {
	// Generate a unique device ID.
	let deviceId = crypto.randomBytes(4).toString("hex").slice(0, 8).toUpperCase();

	res.send({
		"deviceId": deviceId,
		"deviceToken": DeviceTokenManager.generateDeviceToken(deviceId)
	});
});

module.exports = router;
