const keyManager = require('./KeyManager');
const jwt = require('jsonwebtoken');

class SessionTokenManager {
	constructor() {
	}

	generateSessionToken(sessionId, deviceId) {
		// Create a signed JWT for the given device token and session.
		return jwt.sign({
			"deviceId": deviceId,
			"sessionId": sessionId
		}, keyManager.getPrivateKey(), {
			algorithm: "RS256"
		});
	}

	retrieveSessionDevice(signedToken) {
		try {
			let verifiedToken = jwt.verify(signedToken, keyManager.getPublicKey(), { algorithms: ['RS256'] });
			return verifiedToken;
		} catch (error) {
			console.log(error);
		}

		return null;
	}
}

module.exports = new SessionTokenManager();