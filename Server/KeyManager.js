const fs = require('fs');
const forge = require('node-forge');
const crypto = require('crypto');

class KeyManager {
	constructor() {
		this.privateKey = null;
		this.publicKey = null;
		this.sessionsKey = null;
		this.loadKeys();
	}

	getSessionsKey() {
		return this.sessionsKey;
	}

	getPrivateKey() {
		return this.privateKey;
	}

	getPublicKey() {
		return this.publicKey;
	}

	loadKeys() {
		// Check if the public/private keys exist.
		if (!(fs.existsSync('./keys') && fs.existsSync('./keys/public.pem') && fs.existsSync('./keys/private.pem') && fs.existsSync('./keys/sessions.key'))) {
			// Create 'keys' directory.
			if(!fs.existsSync('./keys')) {
				console.log("Creating key storage.");
				fs.mkdirSync('./keys/');
			}

			// Generate keys.
			console.log("Generating 2048-bit RSA key-pair. One moment.");
			forge.pki.rsa.generateKeyPair({bits: 2048, workers: 2}, function(error, keypair) {
				if (error != null) {
					console.log(error);
					return;
				}

				// Write public-key to PEM.
				this.publicKey = forge.pki.publicKeyToPem(keypair.publicKey);
				fs.writeFileSync('./keys/public.pem', this.publicKey);

				// Write private-key to PEM.
				this.privateKey = forge.pki.privateKeyToPem(keypair.privateKey);
				fs.writeFileSync('./keys/private.pem', this.privateKey);

				console.log("Successfully generated RSA key-pair.");
			}.bind(this));

			// Generate random session key string.
			this.sessionsKey = crypto.randomBytes(40).toString("hex").slice(0, 20);
			fs.writeFileSync('./keys/sessions.key', this.sessionsKey);
			console.log("Successfully generated sessions key.");
		} else {
			this.sessionsKey = fs.readFileSync('./keys/sessions.key', {encoding: "utf8"});
			this.privateKey = fs.readFileSync('./keys/private.pem', {encoding: "utf8"});
			this.publicKey = fs.readFileSync('./keys/public.pem', {encoding: "utf8"});

			console.log("Loaded RSA key-pair and session key from storage.");
		}
	}
}

module.exports = new KeyManager();