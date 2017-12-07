const SessionManager = require("./SessionManager");

class SessionsManager {
	constructor() {
		this.sessions = {};
	}

	existsSession(sessionId) {
		return (this.sessions[sessionId] != null);
	}

	removeSession(sessionId) {
		delete this.sessions[sessionId];
	}

	getSession(sessionId) {
		if(this.sessions[sessionId] == null) {
			this.sessions[sessionId] = new SessionManager(sessionId);
		}
		return this.sessions[sessionId];
	}
}

module.exports = new SessionsManager();