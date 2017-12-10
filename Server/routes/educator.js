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
const MySQL = require('sync-mysql');
const database = new MySQL({
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

router.get('/question-sets', authenticationRequired, function(req, res, next) {
	try {
		let databaseResult = database.query("SELECT questionset_id, questionset_name FROM question_set WHERE questionset_owner = ?", [req.user]);

		let question_sets = [];
		databaseResult.forEach(function(result) {
			question_sets.push({
				id: result.questionset_id,
				name: result.questionset_name
			});
		});

		res.render('educator/question-sets', {"baseUrl": req.baseUrl, "question_sets": question_sets});
	} catch (error) {
		console.log(error);
		next(error);
	}
});

router.get('/question-set/create', authenticationRequired, function(req, res) {
	res.render('educator/question-set/create', {"baseUrl": req.baseUrl});
});

router.post('/question-set/create', authenticationRequired, function(req, res) {
	try {
		let databaseResult = database.query("INSERT INTO question_set (questionset_name, questionset_owner) VALUES (?, ?)", [req.body.questionSetName, req.user]);
		res.redirect(req.baseUrl + "/question-set/" + databaseResult.insertId + "/modify");
	} catch (error) {
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
	}
});

router.get('/question-set/:questionSetId/modify', authenticationRequired, function(req, res, next) {
	try {
		let databaseResult = database.query("SELECT questionset_name, questionset_owner FROM question_set WHERE questionset_id = ?", [req.params["questionSetId"]]);
		if(databaseResult.length == 0 || databaseResult[0].questionset_owner != req.user) {
			let error = new Error('Unable to find question set.');
			error.status = 404;
			next(error);

			return;
		}

		res.render('educator/question-set/modify', {"baseUrl": req.baseUrl, "questionSetId": req.params["questionSetId"], "setName": databaseResult[0].questionset_name});
	} catch (error) {
		console.log(error);
		next(error);
	}
});

router.post('/question-set/:questionSetId/modify/getQuestions', authenticationRequired, function(req, res, next) {
	try {
		let databaseResult = database.query("SELECT questionset_name, questionset_owner, questionset_order FROM question_set WHERE questionset_id = ?", [req.params["questionSetId"]]);
		if(databaseResult.length == 0 || databaseResult[0].questionset_owner != req.user) {
			let error = new Error('Unable to find question set.');
			error.status = 404;
			next(error);

			return;
		}

		let questionSetOrder = [];
		try {
			questionSetOrder = JSON.parse(databaseResult[0].questionset_order);
			if(questionSetOrder == null) {
				questionSetOrder = [];
			}
		} catch (error) {}

		let results = {};
		let unprocessedKeys = [];
		databaseResult = database.query("SELECT question_id, question_title FROM question WHERE question_set = ?", [req.params["questionSetId"]]);
		databaseResult.forEach(function(result) {
			unprocessedKeys.push(result.question_id);
			results[result.question_id] = result.question_title;
		});

		let resultData = [];
		for(let i = 0; i < questionSetOrder.length; ++i) {
			if(results[questionSetOrder[i]] != undefined) {
				resultData.push({
					id: questionSetOrder[i],
					title: results[questionSetOrder[i]]
				});
				unprocessedKeys.splice(unprocessedKeys.indexOf(questionSetOrder[i]), 1);
			}
		}

		for(let i = 0; i < unprocessedKeys.length; ++i) {
			resultData.push({
				id: unprocessedKeys[i],
				title: results[unprocessedKeys[i]]
			});
		}

		res.send(resultData);
	} catch (error) {
		console.log(error);
		next(error);
	}
});

router.post('/question-set/:questionSetId/modify/setQuestionOrder', authenticationRequired, function(req, res) {
	try {
		let databaseResult = database.query("SELECT questionset_owner FROM question_set WHERE questionset_id = ?", [req.params["questionSetId"]]);
		if(databaseResult.length == 0 || databaseResult[0].questionset_owner != req.user) {
			let error = new Error('Unable to find question set.');
			error.status = 404;
			next(error);

			return;
		}

		database.query("UPDATE question_set SET questionset_order = ? WHERE questionset_id = ?", [req.body.question_order, req.params["questionSetId"]]);
		res.send({"status": "OK"});
		return;
	} catch (error) {
		console.log(error);
	}

	req.flash("questionError", "An internal error occurred. Please try again.");
	res.send({"status": "ERROR"});
});

router.post('/question-set/:questionSetId/modify/deleteQuestion', authenticationRequired, function(req, res) {
	try {
		let databaseResult = database.query("SELECT questionset_owner FROM question_set WHERE questionset_id = ?", [req.params["questionSetId"]]);
		if(databaseResult.length == 0 || databaseResult[0].questionset_owner != req.user) {
			let error = new Error('Unable to find question set.');
			error.status = 404;
			next(error);

			return;
		}

		database.query("DELETE FROM question WHERE question_id = ? AND question_set = ?", [req.body.question_id, req.params["questionSetId"]]);
		res.send({"status": "OK"});
		return;
	} catch (error) {
		console.log(error);
	}

	req.flash("questionError", "An internal error occurred. Please try again.");
	res.send({"status": "ERROR"});
});

router.get('/question-set/:questionSetId/modify/edit/:questionId', authenticationRequired, function(req, res, next) {
	try {
		let databaseResult = database.query("SELECT questionset_owner FROM question_set WHERE questionset_id = ?", [req.params["questionSetId"]]);
		if(databaseResult.length == 0 || databaseResult[0].questionset_owner != req.user) {
			let error = new Error('Unable to find question set.');
			error.status = 404;
			next(error);

			return;
		}

		databaseResult = database.query("SELECT question_type FROM question WHERE question_id = ? AND question_set = ?", [req.params["questionId"], req.params["questionSetId"]]);
		if(databaseResult.length == 0) {
			let error = new Error('Unable to find question.');
			error.status = 404;
			next(error);

			return;
		}

		switch(databaseResult[0].question_type) {
			case "MultipleChoice":
				res.render('educator/question-set/edit-multiple-choice', {"baseUrl": req.baseUrl, "questionSetId": req.params["questionSetId"], "questionId": req.params["questionId"]});
				break;
			case "RankedChoice":
				res.render('educator/question-set/edit-ranked-choice', {"baseUrl": req.baseUrl, "questionSetId": req.params["questionSetId"], "questionId": req.params["questionId"]});
				break;
			case "ShortAnswer":
				res.render('educator/question-set/edit-short-answer', {"baseUrl": req.baseUrl, "questionSetId": req.params["questionSetId"], "questionId": req.params["questionId"]});
				break;
			case "BooleanAnswer":
				res.render('educator/question-set/edit-boolean-answer', {"baseUrl": req.baseUrl, "questionSetId": req.params["questionSetId"], "questionId": req.params["questionId"]});
				break;
		}
	} catch (error) {
		console.log(error);
		next(error);
	}
});

router.get('/question-set/:questionSetId/modify/edit/:questionId/data', authenticationRequired, function(req, res, next) {
	try {
		let databaseResult = database.query("SELECT questionset_owner FROM question_set WHERE questionset_id = ?", [req.params["questionSetId"]]);
		if(databaseResult.length == 0 || databaseResult[0].questionset_owner != req.user) {
			let error = new Error('Unable to find question set.');
			error.status = 404;
			next(error);

			return;
		}

		databaseResult = database.query("SELECT question_title, question_data FROM question WHERE question_id = ? AND question_set = ?", [req.params["questionId"], req.params["questionSetId"]]);
		if(databaseResult.length == 0) {
			let error = new Error('Unable to find question.');
			error.status = 404;
			next(error);

			return;
		}

		res.send({
			question_title: databaseResult[0].question_title,
			question_data: JSON.parse(databaseResult[0].question_data)
		});
	} catch (error) {
		console.log(error);
		next(error);
	}
});

router.post('/question-set/:questionSetId/modify/edit/:questionId/modify', authenticationRequired, function(req, res, next) {
	try {
		let databaseResult = database.query("SELECT questionset_owner FROM question_set WHERE questionset_id = ?", [req.params["questionSetId"]]);
		if(databaseResult.length == 0 || databaseResult[0].questionset_owner != req.user) {
			let error = new Error('Unable to find question set.');
			error.status = 404;
			next(error);

			return;
		}

		databaseResult = database.query("UPDATE question SET question_title = ?, question_data = ? WHERE question_id = ? AND question_set = ?", [req.body.question_title, req.body.question_data, req.params["questionId"], req.params["questionSetId"]]);

		res.send({
			status: "OK",
			redirectTo: req.baseUrl + '/question-set/' + req.params["questionSetId"] + "/modify"
		});
	} catch (error) {
		console.log(error);
		next(error);
	}
});

router.get('/question-set/:questionSetId/modify/add', authenticationRequired, function(req, res, next) {
	try {
		let databaseResult = database.query("SELECT questionset_name, questionset_owner FROM question_set WHERE questionset_id = ?", [req.params["questionSetId"]]);
		if(databaseResult.length == 0 || databaseResult[0].questionset_owner != req.user) {
			let error = new Error('Unable to find question set.');
			error.status = 404;
			next(error);

			return;
		}

		res.render('educator/question-set/add', {"baseUrl": req.baseUrl, "questionSetId": req.params["questionSetId"], "setName": databaseResult[0].questionset_name});
	} catch (error) {
		console.log(error);
		next(error);
	}
});

router.get('/question-set/:questionSetId/modify/add/multiple-choice', authenticationRequired, function(req, res, next) {
	try {
		let databaseResult = database.query("SELECT questionset_name, questionset_owner FROM question_set WHERE questionset_id = ?", [req.params["questionSetId"]]);
		if(databaseResult.length == 0 || databaseResult[0].questionset_owner != req.user) {
			let error = new Error('Unable to find question set.');
			error.status = 404;
			next(error);

			return;
		}

		res.render('educator/question-set/add-multiple-choice', {"baseUrl": req.baseUrl, "questionSetId": req.params["questionSetId"], "setName": databaseResult[0].questionset_name});
	} catch (error) {
		console.log(error);
		next(error);
	}
});

router.get('/question-set/:questionSetId/modify/add/short-answer', authenticationRequired, function(req, res, next) {
	try {
		let databaseResult = database.query("SELECT questionset_name, questionset_owner FROM question_set WHERE questionset_id = ?", [req.params["questionSetId"]]);
		if(databaseResult.length == 0 || databaseResult[0].questionset_owner != req.user) {
			let error = new Error('Unable to find question set.');
			error.status = 404;
			next(error);

			return;
		}

		res.render('educator/question-set/add-short-answer', {"baseUrl": req.baseUrl, "questionSetId": req.params["questionSetId"], "setName": databaseResult[0].questionset_name});
	} catch (error) {
		console.log(error);
		next(error);
	}
});

router.get('/question-set/:questionSetId/modify/add/ranked-choice', authenticationRequired, function(req, res, next) {
	try {
		let databaseResult = database.query("SELECT questionset_name, questionset_owner FROM question_set WHERE questionset_id = ?", [req.params["questionSetId"]]);
		if(databaseResult.length == 0 || databaseResult[0].questionset_owner != req.user) {
			let error = new Error('Unable to find question set.');
			error.status = 404;
			next(error);

			return;
		}

		res.render('educator/question-set/add-ranked-choice', {"baseUrl": req.baseUrl, "questionSetId": req.params["questionSetId"], "setName": databaseResult[0].questionset_name});
	} catch (error) {
		console.log(error);
		next(error);
	}
});

router.get('/question-set/:questionSetId/modify/add/boolean-answer', authenticationRequired, function(req, res, next) {
	try {
		let databaseResult = database.query("SELECT questionset_name, questionset_owner FROM question_set WHERE questionset_id = ?", [req.params["questionSetId"]]);
		if(databaseResult.length == 0 || databaseResult[0].questionset_owner != req.user) {
			let error = new Error('Unable to find question set.');
			error.status = 404;
			next(error);

			return;
		}

		res.render('educator/question-set/add-boolean-answer', {"baseUrl": req.baseUrl, "questionSetId": req.params["questionSetId"], "setName": databaseResult[0].questionset_name});
	} catch (error) {
		console.log(error);
		next(error);
	}
});

router.post('/question-set/:questionSetId/modify/add/question', authenticationRequired, function(req, res, next) {
	let result = {
		"status": "ERROR"
	};

	// Check question type.
	const permittedQuestionTypes = ["MultipleChoice", "BooleanAnswer", "RankedChoice", "ShortAnswer"];
	if(permittedQuestionTypes.indexOf(req.body.question_type) < 0) {
		result["statusExtended"] = "An internal error occurred. Please try again.";
		res.send(result);
		return;
	}

	if(req.body["question_title"] === undefined || req.body.question_title.trim().length == 0) {
		result["statusExtended"] = "Please specify a title for this question.";
		res.send(result);
		return;
	}

	try {
		let databaseResult = database.query("SELECT questionset_name, questionset_owner FROM question_set WHERE questionset_id = ?", [req.params["questionSetId"]]);
		if(databaseResult.length == 0 || databaseResult[0].questionset_owner != req.user)  {
			result["statusExtended"] = "An internal error occurred. Please try again.";
			res.send(result);
			return;
		}

		database.query("INSERT INTO question (question_set, question_type, question_title, question_data) VALUES (?, ?, ?, ?)", [req.params["questionSetId"], req.body.question_type, req.body.question_title, req.body.question_data]);
		result["status"] = "OK";
		result["redirectTo"] = req.baseUrl + '/question-set/' + req.params["questionSetId"] + "/modify";
		res.send(result);
	} catch (error) {
		console.log(error);
		next(error);
	}
});

router.post('/question-set/:questionSetId/modify/setName', authenticationRequired, function(req, res) {
	try {
		let databaseResult = database.query("SELECT questionset_name, questionset_owner, questionset_order FROM question_set WHERE questionset_id = ?", [req.params["questionSetId"]]);
		if(databaseResult.length == 0 || databaseResult[0].questionset_owner != req.user) {
			let error = new Error('Unable to find question set.');
			error.status = 404;
			next(error);

			return;
		}

		database.query("UPDATE question_set SET questionset_name = ? WHERE questionset_id = ?", [req.body.name, req.params["questionSetId"]]);
	} catch (error) {
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
	}

	res.redirect(req.baseUrl + '/question-set/' + req.params["questionSetId"] + "/modify");
});

router.get('/question-set/:questionSetId/create-session', authenticationRequired, function(req, res) {
	const sessionId = crypto.randomBytes(4).toString("hex").slice(0, 7);

	try {
		database.query("INSERT INTO session (session_id, session_owner, session_questionset) VALUES (?, ?, ?)", [sessionId, req.user, req.params["questionSetId"]]);
		res.redirect(req.baseUrl + "/session/" + sessionId);
	} catch (error) {
		console.log(error);
		req.flash("sessionError", "A system error was encountered. Please try again later");
	}
});

router.get('/session/:sessionId', authenticationRequired, function(req, res, next) {
	try {
		let databaseResult = database.query("SELECT session_owner, session_questionset, session_endtime FROM session WHERE session_id = ?", [req.params["sessionId"]]);
		if (databaseResult.length == 0 || databaseResult[0].session_owner != req.user) {
			let error = new Error('Unable to find session.');
			error.status = 404;
			next(error);

			return;
		}

		res.render('educator/session/home', {
			"sessionId": req.params["sessionId"],
			"sessionAuthenticator": jwt.sign({ sessionId: req.params["sessionId"] }, KeyManager.getPrivateKey(), { algorithm: "RS256", expiresIn: 5 })
		});
	} catch(error) {
		console.log(error);
		let returnError = new Error('Failed to start session.');
		returnError.status = 500;
		next(returnError);
	}
});

router.ws('/session/:sessionId/connect', function(ws, req) {
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
					session.setEducatorSession(ws);
				} catch (error) {
					ws.close();
				}
				break;

			case "SEND_QUESTION":
				
				break;
		}
	});

	ws.on("close", function() {
		if(session != null) {
			session.setEducatorSession(null);
		}
	});
});

router.get('/session/:sessionId/authenticate', authenticationRequired, function(req, res, next) {
	try {
		let databaseResult = database.query("SELECT session_owner FROM session WHERE session_id = ? AND session_endtime IS NULL", [req.params["sessionId"]]);
		if (databaseResult.length == 0 || databaseResult[0].session_owner != req.user) {
			let error = new Error('Unable to find session.');
			error.status = 404;
			next(error);

			return;
		}

		res.render('educator/session/authenticate', {
			"sessionId": req.params["sessionId"],
			"sessionAuthenticator": jwt.sign({ sessionId: req.params["sessionId"] }, KeyManager.getPrivateKey(), { algorithm: "RS256", expiresIn: 5 })
		});
	} catch (error) {
		console.log(error);
		next(error);
	}
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
