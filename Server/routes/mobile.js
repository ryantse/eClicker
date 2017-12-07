const express = require('express');
const router = express.Router();
const DeviceTokenManager = require('../DeviceTokenManager');
const crypto = require('crypto');
const MobileClient = require('../MobileClient');
const SessionTokenManager = require('../SessionTokenManager');
const SessionsManager = require('../SessionsManager');

router.ws('/session', function(ws, req) {
	const client = new MobileClient(ws);
});

router.post('/session/ask-question', function(req, res, next) {

});

router.post('/session/record-answer', function(req, res, next) {
	const sessionData = SessionTokenManager.retrieveSessionDevice(req.body.token);
	if(sessionData != null) {
		if(SessionsManager.existsSession(sessionData.sessionId)) {
			const session = SessionsManager.getSession(sessionData.sessionId);
			if(session.getQuestionId() != null && session.getQuestionId() == req.body.questionId) {
				// TODO: Record response.
				res.send({
					"status": "OK"
				});
			} else {
				res.send({
					"status": "ERROR",
					"statusExtended": "QUESTION_CLOSED"
				});
			}
		} else {
			res.send({
				"status": "ERROR",
				"statusExtended": "SESSION_EXPIRED"
			});
		}
	} else {
		res.send({
			"status": "ERROR",
			"statusExtended": "AUTHENTICATOR_CODE_INVALID"
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
