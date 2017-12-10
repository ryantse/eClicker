const SessionAuthenticator = require('./SessionAuthenticator');
const SessionsManager = require('./SessionsManager');

class SessionManager {
	constructor(sessionId) {
		this.sessionId = sessionId;
		this.authenticator = new SessionAuthenticator(this.authenticatorCallback.bind(this));
		this.authenticatorSession = null;
		this.educatorSession = null;
		this.sessionClients = [];
		this.questionId = null;
	}

	close() {
		SessionsManager.removeSession(this.sessionId);
	}

	setEducatorSession(socket) {
		this.educatorSession = socket;
	}

	setAuthenticatorSession(callbackSocket) {
		this.authenticatorSession = callbackSocket;
		if(this.authenticatorSession) {
			this.authenticator.start();
		} else {
			this.authenticator.stop();
		}
	}

	authenticatorCallback(sessionCode) {
		if(this.authenticatorSession) {
			console.log(sessionCode);
			try {
				this.authenticatorSession.send(JSON.stringify({
					messageType: "SESSION_AUTHENTICATION",
					messageData: sessionCode
				}));
			} catch (error) {}
		}
	}

	checkAuthenticatorCode(codeA, codeB) {
		return this.authenticator.authenticate(codeA, codeB);
	}

	addSessionClient(client) {
		if(this.sessionClients.indexOf(client) < 0) {
			this.sessionClients.push(client);
		}
	}

	removeSessionClient(client) {
		let clientIndex = this.sessionClients.indexOf(client);
		this.sessionClients.splice(clientIndex, 1);
	}

	sendClients(message) {
		this.sessionClients.forEach(function (sessionClient) {
			sessionClient.websocket.send(message);
		});
	}

	setQuestionId(questionId) {
		this.questionId = questionId;
	}

	getQuestionId() {
		return this.questionId;
	}
}

module.exports = SessionManager;