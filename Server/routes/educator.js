const express = require('express');
const router = express.Router();
const deviceTokenManager = require('../DeviceTokenManager');
const SessionsManager = require('../SessionsManager');
const crypto = require('crypto');
const passport = require('passport');
const LocalStrategy = require('passport-local').Strategy;
const ensureLogin = require('connect-ensure-login');
const requestFlash = require('req-flash');
const jwt = require('jsonwebtoken');
const KeyManager = require('../KeyManager');

const AdminUsers = require('../AdminUsers');
const database = require('promise-mysql').createPool({
	host: '138.68.25.144',
	user: 'eclicker',
	password: 'xnUe3jytAtsd6rsh3kiEjTtAiahoMtLRNHDzmas3TvvjJz3ftdpQEHrvtTptbfLa',
	database: 'eclicker'
});

passport.serializeUser(function(user, done) {
	done(null, user);
});

passport.deserializeUser(function(user, done) {
	done(null, user);
});

passport.use(new LocalStrategy(function(username, password, done) {
	if(!AdminUsers.hasOwnProperty(username)) {
		return done(null, false, {message: "A user with the given username could not be found."});
	}

	if(AdminUsers[username] != password) {
		return done(null, false, {message: "The password you entered could not be verified."});
	}

	return done(null, username);
}));

const expressSession = require('express-session')({
	secret: KeyManager.getSessionsKey(),
	resave: false,
	saveUninitialized: false
});

const ensureNotLoggedIn = function(req, res, next) {
	return ensureLogin.ensureNotLoggedIn({ redirectTo: req.baseUrl }).call(this, req, res, next);
};

const ensureLoggedIn = function(req, res, next) {
	return ensureLogin.ensureLoggedIn({ redirectTo: req.baseUrl + '/login' }).call(this, req, res, next);
};

authenticationMiddleware = [expressSession, requestFlash({locals: 'flash'}), passport.initialize(), passport.session()];
authenticationRequired = authenticationMiddleware.concat([ensureLoggedIn]);

router.get('/login', authenticationMiddleware, ensureNotLoggedIn, function(req, res, next) {
	res.render('educator/login');
});

router.post('/login', authenticationMiddleware, function(req, res, next) {
	return passport.authenticate('local', {
		successReturnToOrRedirect: req.baseUrl,
		failureRedirect: req.baseUrl + '/login',
		failureFlash: true
	}).call(this, req, res, next);
});

router.get('/logout', authenticationRequired, function(req, res) {
	req.logout();
	res.redirect(req.baseUrl + '/login');
});

router.get('/', authenticationRequired, function(req, res) {
	res.redirect(req.baseUrl + '/question-sets')
});

router.get('/question-sets', authenticationRequired, function(req, res) {
	let connection;

	database.getConnection().then(function(conn) {
		connection = conn;
		return connection.query("SELECT questionset_id, questionset_name FROM question_set WHERE questionset_owner = ?", [req.user]);
	}).then(function(results) {
		let questionsets = [];
		results.forEach(function(result) {
			questionsets.push({
				id: result.questionset_id,
				name: result.questionset_name
			});
		});
		res.render('educator/question-sets', {baseUrl: req.baseUrl, question_sets: questionsets});
	}).catch(function(error) {
		console.log(error);
	}).finally(function() {
		if (connection && connection.release) connection.release();
	});
});

router.get('/question-set/create', authenticationRequired, function(req, res) {
	res.render('educator/question-set/create', {baseUrl: req.baseUrl});
});

router.post('/question-set/create', authenticationRequired, function(req, res) {
	let connection;

	database.getConnection().then(function(conn) {
		connection = conn;
		return connection.query("INSERT INTO question_set (questionset_name, questionset_owner) VALUES (?, ?)", [req.body.questionSetName, req.user]);
	}).then(function(result) {
		res.redirect(req.baseUrl + "/question-set/" + result.insertId + "/modify");
	}).catch(function(error) {
		console.log(error);
		if(error.code) {
			switch(error.code) {
				case "ER_DUP_ENTRY":
					req.flash("namingError", "An existing question set by this name already exists.");
					break;

				case "ER_DATA_TOO_LONG":
					req.flash("namingError", "Question set name must be 255 characters or less.");
					break;

				default:
					req.flash("namingError", "A system error was encountered. Please try again later.");
					break;
			}
		}
		res.redirect(req.baseUrl + "/question-set/create");
	}).finally(function() {
		if (connection && connection.release) connection.release();
	});
});

router.get('/question-set/:questionSetId/modify', authenticationRequired, function(req, res) {
	var connection;
	database.getConnection().then(function(conn) {
		connection = conn;
		return connection.query("SELECT questionset_name FROM question_set WHERE questionset_id = ?", [req.params["questionSetId"]]);
	}).then(function(result) {
		if(result.length == 0) {
			let error = new Error('Unable to find question set.');
			error.status = 404;
			next(error);

			return;
		}

		res.render('educator/question-set/modify', {baseUrl: req.baseUrl, questionSetId: req.params["questionSetId"], setName: result[0].questionset_name});
	}).catch(function(error) {
		console.log(error);
		res.send({status: "ERROR"});
	}).finally(function() {
		if (connection && connection.release) connection.release();
	});
});

router.get('/question-set/:questionSetId/modify/add', authenticationRequired, function(req, res) {
	var connection;
	database.getConnection().then(function(conn) {
		connection = conn;
		return connection.query("SELECT questionset_name FROM question_set WHERE questionset_id = ?", [req.params["questionSetId"]]);
	}).then(function(result) {
		if(result.length == 0) {
			let error = new Error('Unable to find question set.');
			error.status = 404;
			next(error);

			return;
		}

		res.render('educator/question-set/add', {baseUrl: req.baseUrl, questionSetId: req.params["questionSetId"], setName: result[0].questionset_name});
	}).catch(function(error) {
		console.log(error);
		res.send({status: "ERROR"});
	}).finally(function() {
		if (connection && connection.release) connection.release();
	});
});

router.post('/question-set/:questionSetId/modify/setName', authenticationRequired, function(req, res) {
	var connection;
	database.getConnection().then(function(conn) {
		connection = conn;
		return connection.query("UPDATE question_set SET questionset_name = ? WHERE questionset_id = ?", [req.body.name, req.params["questionSetId"]]);
	}).then(function(result) {
		if(result.affectedRows == 0) {
			let error = new Error('Unable to find question set.');
			error.status = 404;
			next(error);

			return;
		}

		res.redirect(req.baseUrl + '/question-set/' + req.params["questionSetId"] + "/modify");
	}).catch(function(error) {
		console.log(error);
		if(error.code) {
			switch(error.code) {
				case "ER_DUP_ENTRY":
					req.flash("namingError", "An existing question set by this name already exists.");
					break;

				case "ER_DATA_TOO_LONG":
					req.flash("namingError", "Question set name must be 255 characters or less.");
					break;

				default:
					req.flash("namingError", "A system error was encountered. Please try again later.");
					break;
			}
		}
		res.redirect(req.baseUrl + '/question-set/' + req.params["questionSetId"] + "/modify");
	}).finally(function() {
		if (connection && connection.release) connection.release();
	});
});

router.get('/question-set/:questionSetId/create-session', authenticationRequired, function(req, res) {
	const sessionId = crypto.randomBytes(4).toString("hex").slice(0, 7);

	var connection;
	database.getConnection().then(function(conn) {
		connection = conn;
		return connection.query("INSERT INTO session (session_id, session_owner, session_questionset) VALUES (?, ?, ?)", [sessionId, req.user, req.params["questionSetId"]]);
	}).then(function(result) {
		res.redirect(req.baseUrl + "/session/" + sessionId);
	}).catch(function(error) {
		console.log(error);
		req.flash("sessionError", "A system error was encountered. Please try again later");
	}).finally(function() {
		if (connection && connection.release) connection.release();
	});
});

router.get('/session/:sessionId', authenticationRequired, function(req, res, next) {
	let connection;

	database.getConnection().then(function(conn) {
		connection = conn;
		return connection.query("SELECT session_owner, session_questionset, session_endtime FROM session WHERE session_id = ?", [req.params["sessionId"]]);
	}).then(function(result) {
		if (result.length == 0 || result[0].session_owner != req.user) {
			let error = new Error('Unable to find session.');
			error.status = 404;
			next(error);

			return;
		}

		return connection.query("UPDATE session SET session_endtime = NULL WHERE session_id = ?", [req.params["sessionId"]]);
	}).then(function(result) {
		res.render('educator/session/home', {
			"sessionId": req.params["sessionId"],
			"sessionAuthenticator": jwt.sign({ sessionId: req.params["sessionId"] }, KeyManager.getPrivateKey(), { algorithm: "RS256", expiresIn: 5 })
		});
	}).catch(function(error) {
		console.log(error);
		let returnError = new Error('Failed to start session.');
		returnError.status = 500;
		next(returnError);
	}).finally(function() {
		if (connection && connection.release) connection.release();
	});
});

router.get('/session/:sessionId/authenticate', authenticationRequired, function(req, res, next) {
	let connection;

	database.getConnection().then(function(conn) {
		connection = conn;
		return connection.query("SELECT session_owner FROM session WHERE session_id = ? AND session_endtime IS NULL", [req.params["sessionId"]]);
	}).then(function(result) {
		if (result.length == 0 || result[0].session_owner != req.user) {
			let error = new Error('Unable to find session.');
			error.status = 404;
			next(error);

			return;
		}

		res.render('educator/session/authenticate', {
			"sessionId": req.params["sessionId"],
			"sessionAuthenticator": jwt.sign({ sessionId: req.params["sessionId"] }, KeyManager.getPrivateKey(), { algorithm: "RS256", expiresIn: 5 })
		});
	}).catch(function(error) {
		console.log(error);
	}).finally(function() {
		if (connection && connection.release) connection.release();
	});
});

router.ws('/session/:sessionId/authenticate/connect', function(ws, req) {
	let sessionId = req.params["sessionId"];
	let sessionVerified = false;
	let session = null;

	ws.on("message", function(message) {
		message = JSON.parse(message);
		switch(message.messageType) {
			case "CONNECT_SESSION":
				try {
					let sessionData = jwt.verify(message.messageData, KeyManager.getPublicKey(), {algorithms: ['RS256']});
					if (sessionData.sessionId != sessionId) {
						ws.close();
						return;
					}

					sessionVerified = true;
					session = SessionsManager.getSession(sessionId);
					session.setAuthenticatorSession(ws);
				} catch (error) {
					ws.close();
				}
				break;
		}
	});

	ws.on("close", function() {
		if(session != null) {
			session.setAuthenticatorSession(null);
		}
	});
});

module.exports = router;
