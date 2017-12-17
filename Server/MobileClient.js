const SessionsManager = require('./SessionsManager');
const SessionTokenManager = require('./SessionTokenManager');
const DeviceTokenManager = require('./DeviceTokenManager');

class MobileClient {
	constructor(websocket) {
		this.websocket = websocket;
		this.deviceId = null;
		this.session = null;
		this._bindHandlers();
	}

	_bindHandlers() {
		this.websocket.on("message", this._onMessage.bind(this));
		this.websocket.on("close", this._onClose.bind(this));
	}

	_onClose() {
		console.log("Connection closed.");
		if(this.session != null) {
			this.session.removeSessionClient(this);
		}
	}

	_onMessage(message) {
		console.log(message);

		try {
			message = JSON.parse(message);
		} catch (error) {
			return;
		}

		switch(message.messageType) {
			case "DEVICE_JOIN":
				this._onMessageDeviceJoin(message.messageData);
				break;

			case "SESSION_JOIN":
				this._onMessageSessionJoin(message.messageData);
				break;

			case "SEND_QUESTION":
				break;

			case "QUESTION_WAITING":
				if(this.session != null) {
					this.session.sendCurrentQuestion(this);
				}
				break;
		}
	}

	_send(message) {
		console.log(message);
		this.websocket.send(JSON.stringify(message));
	}

	_onMessageDeviceJoin(messageData) {
		let deviceId = DeviceTokenManager.retrieveDeviceId(messageData.deviceToken);

		if(deviceId == null) {
			this._send({
				messageType: "DEVICE_JOIN",
				messageData: {
					status: "ERROR",
					statusExtended: {
						errorReason: "DEVICE_TOKEN_INVALID"
					}
				}
			});
			return;
		}

		this.deviceId = deviceId;
		this._send({
			messageType: "DEVICE_JOIN",
			messageData: {
				status: "OK"
			}
		});
	}

	_onMessageSessionJoin(messageData) {
		const sessionJoinFailure = function(failureReason) {
			this._send({
				messageType: "SESSION_JOIN",
				messageData: {
					status: "ERROR",
					statusExtended: {
						errorReason: failureReason
					}
				}
			});
		}.bind(this);

		if(this.deviceId == null) {
			sessionJoinFailure("DEVICE_NOT_IDENTIFIED");
			return;
		}

		if(this.session != null) {
			this.session.removeSessionClient(this);
		}

		switch(messageData.authenticationType) {
			case "sessionKey":
				let sessionId = messageData.sessionId;
				if(!SessionsManager.existsSession(sessionId)) {
					sessionJoinFailure("SESSION_EXPIRED");
					return;
				}


				if(messageData.sessionKeys == null || messageData.sessionKeys.length != 2) {
					sessionJoinFailure("AUTHENTICATOR_CODE_INVALID");
					return;
				}

				let session = SessionsManager.getSession(sessionId);
				let sessionKeyA = messageData.sessionKeys[0];
				let sessionKeyB = messageData.sessionKeys[1];

				if (!session.checkAuthenticatorCode(sessionKeyA, sessionKeyB)) {
					sessionJoinFailure("AUTHENTICATOR_CODE_EXPIRED");
					return;
				}

				this.session = session;
				this.session.addSessionClient(this);
				this._send({
					messageType: "SESSION_JOIN",
					messageData: {
						status: "OK",
						statusExtended: {
							"authenticationType": "sessionKey",
							"sessionId": sessionId,
							"sessionToken": SessionTokenManager.generateSessionToken(sessionId, this.deviceId)
						}
					}
				});
				break;

			case "sessionToken":
				let sessionData = SessionTokenManager.retrieveSessionDevice(messageData.sessionToken);
				if(sessionData == null || sessionData.deviceId != this.deviceId) {
					sessionJoinFailure("AUTHENTICATOR_CODE_INVALID");
					return;
				}

				if(!SessionsManager.existsSession(sessionData.sessionId)) {
					sessionJoinFailure("SESSION_EXPIRED");
					return;
				}

				this.session = SessionsManager.getSession(sessionData.sessionId);
				this.session.addSessionClient(this);
				this._send({
					messageType: "SESSION_JOIN",
					messageData: {
						status: "OK",
						statusExtended: {
							"authenticationType": "sessionToken",
							"sessionId": sessionData.sessionId,
							"sessionToken": messageData.sessionToken
						}
					}
				});
				break;
		}
	}
}

module.exports = MobileClient;