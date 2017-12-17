const SessionAuthenticator = require('./SessionAuthenticator');

class SessionManager {
	constructor(sessionId) {
		this._sessionId = sessionId;
		this._authenticator = new SessionAuthenticator(this._authenticatorCallback.bind(this));
		this._authenticatorSession = null;
		this._educatorSession = null;
		this._sessionClients = [];
		this._question = null;
	}

	endSession() {
		const SessionsManager = require('./SessionsManager');

		SessionsManager.removeSession(this._sessionId);

		this.setAuthenticatorSession(null);
		this.setEducatorSession(null);

		this._sendClients({
			"messageType": "SESSION_TERMINATE",
			"messageData": {}
		});
	}

	setEducatorSession(socket) {
		this._educatorSession = socket;
	}

	setAuthenticatorSession(socket) {
		this._authenticatorSession = socket;

		if(this._authenticatorSession) {
			this._authenticator.start();
		} else {
			this._authenticator.stop();
		}
	}

	checkAuthenticatorCode(codeA, codeB) {
		return this._authenticator.authenticate(codeA, codeB);
	}

	addSessionClient(client) {
		if(this._sessionClients.indexOf(client) >= 0) {
			return;
		}

		this._sessionClients.push(client);
	}

	removeSessionClient(client) {
		let clientIndex = this._sessionClients.indexOf(client);

		if(clientIndex >= 0) {
			this._sessionClients.splice(clientIndex, 1);
		}
	}

	setQuestion(question) {
		if(this._question != null && question != null && this._question.questionId === question.questionId) {
			return;
		}

		this._question = question;

		if(this._question != null) {
			this._sendClients({
				"messageType": "QUESTION_BEGIN",
				"messageData": this._question
			});
		} else {
			this._sendClients({
				"messageType": "QUESTION_END",
				"messageData": {}
			});
		}
	}

	getQuestionId() {
		if(this._question === null) {
			return null;
		}

		return this._question["questionId"];
	}

	sendCurrentQuestion(client) {
		if(this._question != null) {
			client.websocket.send(JSON.stringify({
				"messageType": "QUESTION_BEGIN",
				"messageData": this._question
			}));
		}
	}

	sendEducatorQuestion(device, question) {
		if(this._educatorSession) {
			this._educatorSession.send(JSON.stringify({
				"messageType": "ASK_QUESTION",
				"messageData": {
					"message": question,
					"device": device
				}
			}));
			return true;
		}

		return false;
	}

	updateEducatorQuestionResponses(answer) {
		if(this._educatorSession) {
			this._educatorSession.send(JSON.stringify({
				"messageType": "RESPONSE_UPDATE",
				"messageData": {
					"lastAnswer": answer
				}
			}));
			return true;
		}
	}

	_authenticatorCallback(sessionCode) {
		if(!this._authenticatorSession) {
			this._authenticatorSession.stop();
		}

		try {
			this._authenticatorSession.send(JSON.stringify({
				"messageType": "SESSION_AUTHENTICATION",
				"messageData": sessionCode
			}));
		} catch (error) {}
	}

	_sendClients(message) {
		const formattedMessage = JSON.stringify(message);

		this._sessionClients.forEach(function (sessionClient) {
			sessionClient.websocket.send(formattedMessage);
		});
	}
}

module.exports = SessionManager;