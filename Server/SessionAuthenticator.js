const crypto = require('crypto');

class SessionAuthenticator {
	constructor(callback) {
		// Codes should change 2 times a second.
		this.changeFrequency = 1000 / 2;

		// Codes should only be valid for 3 seconds.
		// this.maxCodes = Math.ceil(3 * (1000 / this.changeFrequency));
		console.log("WARNING!! Debugging, codes are valid for 500 seconds.");
		this.maxCodes = Math.ceil(500 * (1000 / this.changeFrequency));

		// Allowed delay period (distance between the two codes captured).
		this.maxDelay = 2;

		// Valid codes storage array.
		this.validCodes = [];

		// Store the callback function.
		this.callback = callback;

		this.refreshInterval = null;
	}

	authenticate(sessionKeyA, sessionKeyB) {
		const sessionIndexA = this.validCodes.lastIndexOf(sessionKeyA);
		const sessionIndexB = this.validCodes.lastIndexOf(sessionKeyB);
		const keyDistance = Math.abs(sessionIndexA - sessionIndexB);

		return (sessionIndexA != -1 && sessionIndexB != -1 && keyDistance > 0 && keyDistance <= this.maxDelay);
	}

	start() {
		if (this.refreshInterval == null) {
			this.refreshInterval = setInterval(this.rotateCode.bind(this), this.changeFrequency);
		}
	}

	stop() {
		if (this.refreshInterval != null) {
			clearInterval(this.refreshInterval);
			this.validCodes = [];
			this.refreshInterval = null;
		}
	}

	rotateCode() {
		// Generate new authentication code.
		let authenticatorCode = crypto.randomBytes(2).toString("hex").slice(0, 3);

		this.validCodes.push(authenticatorCode);
		while (this.validCodes.length > this.maxCodes) this.validCodes.shift();
		console.log("validCodes:", this.validCodes.length);

		this.callback(authenticatorCode);
	}
}

module.exports = SessionAuthenticator;