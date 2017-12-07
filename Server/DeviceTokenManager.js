
const keyManager = require('./KeyManager');
const jwt = require('jsonwebtoken');

class DeviceTokenManager {
	constructor() {
		this.tokenLength = 8;
	}

	generateDeviceToken(deviceId) {
		return jwt.sign({ device: deviceId }, keyManager.getPrivateKey(), { algorithm: "RS256" });
	}

	retrieveDeviceId(signedToken) {
		try {
			return jwt.verify(signedToken, keyManager.getPublicKey(), { algorithms: ["RS256"] }).device;
		} catch (error) {
			console.log(error);
		}
		return null;
	}
}

module.exports = new DeviceTokenManager();