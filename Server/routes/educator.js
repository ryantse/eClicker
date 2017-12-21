const express = require("express");
const router = express.Router();
const SessionsManager = require("../SessionsManager");
const crypto = require("crypto");
const passport = require("passport");
const LocalStrategy = require("passport-local").Strategy;
const ensureLogin = require("connect-ensure-login");
const requestFlash = require("req-flash");
const jwt = require("jsonwebtoken");
const KeyManager = require("../KeyManager");
const dateformat = require('dateformat');
const csvWriter = require('csv-write-stream');

const AdminUsers = require("../AdminUsers");
const database = require("../DatabaseManager");

passport.serializeUser(function(user, done) {
	done(null, user);
});

passport.deserializeUser(function(user, done) {
	done(null, user);
});

passport.use(new LocalStrategy(function(username, password, done) {
	if(!AdminUsers.hasOwnProperty(username)) {
		return done(null, false, {"message": "A user with the given username could not be found."});
	}

	if(AdminUsers[username] != password) {
		return done(null, false, {"message": "The password you entered could not be verified."});
	}

	return done(null, username);
}));

const expressSession = require("express-session")({
	"secret": KeyManager.getSessionsKey(),
	"resave": false,
	"saveUninitialized": false
});

const ensureNotLoggedIn = function(req, res, next) {
	return ensureLogin.ensureNotLoggedIn({ redirectTo: req["baseUrl"] }).call(this, req, res, next);
};

const ensureLoggedIn = function(req, res, next) {
	return ensureLogin.ensureLoggedIn({ redirectTo: req["baseUrl"] + "/login" }).call(this, req, res, next);
};

authenticationMiddleware = [expressSession, requestFlash({locals: "flash"}), passport.initialize(), passport.session()];
authenticationRequired = authenticationMiddleware.concat([ensureLoggedIn]);

router.get("/login", authenticationMiddleware, ensureNotLoggedIn, function(req, res, next) {
	res.render("educator/login");
});

router.post("/login", authenticationMiddleware, function(req, res, next) {
	return passport.authenticate("local", {
		successReturnToOrRedirect: req["baseUrl"],
		failureRedirect: req["baseUrl"] + "/login",
		failureFlash: true
	}).call(this, req, res, next);
});

router.get("/logout", authenticationRequired, function(req, res) {
	req.logout();
	res.redirect(req["baseUrl"] + "/login");
});

router.get("/", authenticationRequired, function(req, res) {
	res.redirect(req["baseUrl"] + "/question-sets")
});

router.get("/question-sets", authenticationRequired, function(req, res, next) {
	try {
		let databaseResult = database.query("SELECT questionset_id, questionset_name FROM question_set WHERE questionset_owner = ?", [req["user"]]);

		let question_sets = [];
		databaseResult.forEach(function(result) {
			question_sets.push({
				"id": result["questionset_id"],
				"name": result["questionset_name"]
			});
		});

		res.render("educator/question-sets", {"baseUrl": req["baseUrl"], "question_sets": question_sets});
	} catch (error) {
		console.log(error);

		next(error);
	}
});

router.get("/question-set/create", authenticationRequired, function(req, res) {
	res.render("educator/question-set/create", {"baseUrl": req["baseUrl"]});
});

router.post("/question-set/create", authenticationRequired, function(req, res) {
	try {
		let databaseResult = database.query("INSERT INTO question_set (questionset_name, questionset_owner) VALUES (?, ?)", [req["body"]["questionSetName"], req["user"]]);
		res.redirect(req["baseUrl"] + "/question-set/" + databaseResult["insertId"] + "/modify");
	} catch (error) {
		console.log(error);
		if(error["code"]) {
			switch(error["code"]) {
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
		res.redirect(req["baseUrl"] + "/question-set/create");
	}
});

router.get("/question-set/:questionSetId", function(req, res) {
	res.redirect(req["baseUrl"] + "/question-set/" + req["params"]["questionSetId"] + "/modify");
});

router.post("/question-set/:questionSetId/delete", authenticationRequired, function(req, res, next) {
	try {
		let databaseResult = database.query("SELECT questionset_owner FROM question_set WHERE questionset_id = ?", [req["params"]["questionSetId"]]);
		if(databaseResult.length === 0 || databaseResult[0]["questionset_owner"] !== req["user"]) {
			let error = new Error("Unable to find question set.");
			error["status"] = 404;
			next(error);

			return;
		}

		database.query("DELETE FROM question_set WHERE questionset_id = ?", [req["params"]["questionSetId"]]);
		res.redirect(req["baseUrl"] + "/question-sets/");
	} catch (error) {
		console.log(error);

		next(error);
	}
});

router.get("/question-set/:questionSetId/modify", authenticationRequired, function(req, res, next) {
	try {
		let databaseResult = database.query("SELECT questionset_name, questionset_owner FROM question_set WHERE questionset_id = ?", [req["params"]["questionSetId"]]);
		if(databaseResult.length === 0 || databaseResult[0]["questionset_owner"] !== req["user"]) {
			let error = new Error("Unable to find question set.");
			error["status"] = 404;
			next(error);

			return;
		}

		let questionSetName = databaseResult[0]["questionset_name"];

		databaseResult = database.query("SELECT COUNT(*) as session_count FROM session WHERE session_questionset = ?", [req["params"]["questionSetId"]]);
		let sessionCount = databaseResult[0]["session_count"];

		res.render("educator/question-set/modify", {"baseUrl": req["baseUrl"], "questionSetId": req["params"]["questionSetId"], "setName": questionSetName, "sessionCount": sessionCount});
	} catch (error) {
		console.log(error);

		next(error);
	}
});

router.post("/question-set/:questionSetId/getQuestions", authenticationRequired, function(req, res, next) {
	try {
		// Check that the question set is owned by the logged in user.
		let databaseResult = database.query("SELECT questionset_owner, questionset_order FROM question_set WHERE questionset_id = ?", [req["params"]["questionSetId"]]);
		if(databaseResult.length === 0 || databaseResult[0]["questionset_owner"] !== req["user"]) {
			let error = new Error("Unable to find question set.");
			error["status"] = 404;
			next(error);

			return;
		}

		let questionSetOrder = [];
		try {
			questionSetOrder = JSON.parse(databaseResult[0]["questionset_order"]);
			if(questionSetOrder == null) {
				questionSetOrder = [];
			}
		} catch (error) {}

		let results = {};
		let unprocessedKeys = [];
		databaseResult = database.query("SELECT question_id, question_type, question_title, question_data FROM question WHERE question_set = ?", [req["params"]["questionSetId"]]);
		databaseResult.forEach(function(result) {
			unprocessedKeys.push(result["question_id"]);
			let question_data;

			try {
				question_data = JSON.parse(result["question_data"])
			} catch (error) {
				question_data = {};
			}

			results[result["question_id"]] = {
				"title": result["question_title"],
				"type": result["question_type"],
				"data": question_data
			};
		});

		let resultData = [];
		for(let i = 0; i < questionSetOrder.length; ++i) {
			if(results[questionSetOrder[i]] !== undefined) {
				resultData.push(Object.assign({
					"id": questionSetOrder[i]
				}, results[questionSetOrder[i]]));
				unprocessedKeys.splice(unprocessedKeys.indexOf(questionSetOrder[i]), 1);
			}
		}

		for(let i = 0; i < unprocessedKeys.length; ++i) {
			resultData.push(Object.assign({
				"id": unprocessedKeys[i]
			}, results[unprocessedKeys[i]]));
		}

		res.send(resultData);
	} catch (error) {
		console.log(error);

		next(error);
	}
});

router.post("/question-set/:questionSetId/modify/setQuestionOrder", authenticationRequired, function(req, res) {
	try {
		// Check that the question set is owned by the logged in user.
		let databaseResult = database.query("SELECT questionset_owner FROM question_set WHERE questionset_id = ?", [req["params"]["questionSetId"]]);
		if(databaseResult.length === 0 || databaseResult[0]["questionset_owner"] !== req["user"]) {
			let error = new Error("Unable to find question set.");
			error["status"] = 404;
			next(error);

			return;
		}

		database.query("UPDATE question_set SET questionset_order = ? WHERE questionset_id = ?", [req["body"]["question_order"], req["params"]["questionSetId"]]);
		res.send({"status": "OK"});
		return;
	} catch (error) {
		console.log(error);
	}

	req.flash("questionError", "An internal error occurred. Please try again.");
	res.send({"status": "ERROR"});
});

router.post("/question-set/:questionSetId/modify/deleteQuestion", authenticationRequired, function(req, res) {
	try {
		// Check that the question set is owned by the logged in user.
		let databaseResult = database.query("SELECT questionset_owner FROM question_set WHERE questionset_id = ?", [req["params"]["questionSetId"]]);
		if(databaseResult.length === 0 || databaseResult[0]["questionset_owner"] !== req["user"]) {
			let error = new Error("Unable to find question set.");
			error["status"] = 404;
			next(error);

			return;
		}

		database.query("DELETE FROM question WHERE question_id = ? AND question_set = ?", [req["body"]["question_id"], req["params"]["questionSetId"]]);
		res.send({"status": "OK"});
		return;
	} catch (error) {
		console.log(error);
	}

	req.flash("questionError", "An internal error occurred. Please try again.");
	res.send({"status": "ERROR"});
});

router.get("/question-set/:questionSetId/modify/edit/:questionId", authenticationRequired, function(req, res, next) {
	try {
		let databaseResult = database.query("SELECT questionset_owner FROM question_set WHERE questionset_id = ?", [req["params"]["questionSetId"]]);
		if(databaseResult.length === 0 || databaseResult[0]["questionset_owner"] !== req["user"]) {
			let error = new Error("Unable to find question set.");
			error["status"] = 404;
			next(error);

			return;
		}

		databaseResult = database.query("SELECT question_type FROM question WHERE question_id = ? AND question_set = ?", [req["params"]["questionId"], req["params"]["questionSetId"]]);
		if(databaseResult.length === 0) {
			let error = new Error("Unable to find question.");
			error["status"] = 404;
			next(error);

			return;
		}

		switch(databaseResult[0]["question_type"]) {
			case "MultipleChoice":
				res.render("educator/question-set/edit-multiple-choice", {"baseUrl": req["baseUrl"], "questionSetId": req["params"]["questionSetId"], "questionId": req["params"]["questionId"]});
				break;
			case "RankedChoice":
				res.render("educator/question-set/edit-ranked-choice", {"baseUrl": req["baseUrl"], "questionSetId": req["params"]["questionSetId"], "questionId": req["params"]["questionId"]});
				break;
			case "ShortAnswer":
				res.render("educator/question-set/edit-short-answer", {"baseUrl": req["baseUrl"], "questionSetId": req["params"]["questionSetId"], "questionId": req["params"]["questionId"]});
				break;
			case "BooleanAnswer":
				res.render("educator/question-set/edit-boolean-answer", {"baseUrl": req["baseUrl"], "questionSetId": req["params"]["questionSetId"], "questionId": req["params"]["questionId"]});
				break;
		}
	} catch (error) {
		console.log(error);

		next(error);
	}
});

router.get("/question-set/:questionSetId/modify/edit/:questionId/data", authenticationRequired, function(req, res, next) {
	try {
		// Check that the question set is owned by the logged in user.
		let databaseResult = database.query("SELECT questionset_owner FROM question_set WHERE questionset_id = ?", [req["params"]["questionSetId"]]);
		if(databaseResult.length === 0 || databaseResult[0]["questionset_owner"] !== req["user"]) {
			let error = new Error("Unable to find question set.");
			error["status"] = 404;
			next(error);

			return;
		}

		databaseResult = database.query("SELECT question_title, question_data FROM question WHERE question_id = ? AND question_set = ?", [req["params"]["questionId"], req["params"]["questionSetId"]]);
		if(databaseResult.length === 0) {
			let error = new Error("Unable to find question.");
			error["status"] = 404;
			next(error);

			return;
		}

		res.send({
			"question_title": databaseResult[0]["question_title"],
			"question_data": JSON.parse(databaseResult[0]["question_data"])
		});
	} catch (error) {
		console.log(error);

		next(error);
	}
});

router.post("/question-set/:questionSetId/modify/edit/:questionId/modify", authenticationRequired, function(req, res, next) {
	try {
		// Check that the question set is owned by the logged in user.
		let databaseResult = database.query("SELECT questionset_owner FROM question_set WHERE questionset_id = ?", [req["params"]["questionSetId"]]);
		if(databaseResult.length === 0 || databaseResult[0]["questionset_owner"] !== req["user"]) {
			let error = new Error("Unable to find question set.");
			error["status"] = 404;
			next(error);

			return;
		}

		databaseResult = database.query("UPDATE question SET question_title = ?, question_data = ? WHERE question_id = ? AND question_set = ?", [req["body"]["question_title"], req["body"]["question_data"], req["params"]["questionId"], req["params"]["questionSetId"]]);

		res.send({
			"status": "OK",
			"redirectTo": req["baseUrl"] + "/question-set/" + req["params"]["questionSetId"] + "/modify"
		});
	} catch (error) {
		console.log(error);

		next(error);
	}
});

router.get("/question-set/:questionSetId/modify/add", authenticationRequired, function(req, res, next) {
	try {
		let databaseResult = database.query("SELECT questionset_name, questionset_owner FROM question_set WHERE questionset_id = ?", [req["params"]["questionSetId"]]);
		if(databaseResult.length === 0 || databaseResult[0]["questionset_owner"] !== req["user"]) {
			let error = new Error("Unable to find question set.");
			error["status"] = 404;
			next(error);

			return;
		}

		res.render("educator/question-set/add", {"baseUrl": req["baseUrl"], "questionSetId": req["params"]["questionSetId"], "setName": databaseResult[0]["questionset_name"]});
	} catch (error) {
		console.log(error);

		next(error);
	}
});

router.get("/question-set/:questionSetId/modify/add/multiple-choice", authenticationRequired, function(req, res, next) {
	try {
		let databaseResult = database.query("SELECT questionset_name, questionset_owner FROM question_set WHERE questionset_id = ?", [req["params"]["questionSetId"]]);
		if(databaseResult.length === 0 || databaseResult[0]["questionset_owner"] !== req["user"]) {
			let error = new Error("Unable to find question set.");
			error["status"] = 404;
			next(error);

			return;
		}

		res.render("educator/question-set/add-multiple-choice", {"baseUrl": req["baseUrl"], "questionSetId": req["params"]["questionSetId"], "setName": databaseResult[0]["questionset_name"]});
	} catch (error) {
		console.log(error);

		next(error);
	}
});

router.get("/question-set/:questionSetId/modify/add/short-answer", authenticationRequired, function(req, res, next) {
	try {
		let databaseResult = database.query("SELECT questionset_name, questionset_owner FROM question_set WHERE questionset_id = ?", [req["params"]["questionSetId"]]);
		if(databaseResult.length === 0 || databaseResult[0]["questionset_owner"] !== req["user"]) {
			let error = new Error("Unable to find question set.");
			error["status"] = 404;
			next(error);

			return;
		}

		res.render("educator/question-set/add-short-answer", {"baseUrl": req["baseUrl"], "questionSetId": req["params"]["questionSetId"], "setName": databaseResult[0]["questionset_name"]});
	} catch (error) {
		console.log(error);

		next(error);
	}
});

router.get("/question-set/:questionSetId/modify/add/ranked-choice", authenticationRequired, function(req, res, next) {
	try {
		let databaseResult = database.query("SELECT questionset_name, questionset_owner FROM question_set WHERE questionset_id = ?", [req["params"]["questionSetId"]]);
		if(databaseResult.length === 0 || databaseResult[0]["questionset_owner"] !== req["user"]) {
			let error = new Error("Unable to find question set.");
			error["status"] = 404;
			next(error);

			return;
		}

		res.render("educator/question-set/add-ranked-choice", {"baseUrl": req["baseUrl"], "questionSetId": req["params"]["questionSetId"], "setName": databaseResult[0]["questionset_name"]});
	} catch (error) {
		console.log(error);

		next(error);
	}
});

router.get("/question-set/:questionSetId/modify/add/boolean-answer", authenticationRequired, function(req, res, next) {
	try {
		let databaseResult = database.query("SELECT questionset_name, questionset_owner FROM question_set WHERE questionset_id = ?", [req["params"]["questionSetId"]]);
		if(databaseResult.length === 0 || databaseResult[0]["questionset_owner"] !== req["user"]) {
			let error = new Error("Unable to find question set.");
			error["status"] = 404;
			next(error);

			return;
		}

		res.render("educator/question-set/add-boolean-answer", {"baseUrl": req["baseUrl"], "questionSetId": req["params"]["questionSetId"], "setName": databaseResult[0]["questionset_name"]});
	} catch (error) {
		console.log(error);

		next(error);
	}
});

router.post("/question-set/:questionSetId/modify/add/question", authenticationRequired, function(req, res, next) {
	let result = {
		"status": "ERROR"
	};

	/*
	 * Check the quesiton type that is being created. They must be of the
	 * types listed below.
	 */
	const permittedQuestionTypes = ["MultipleChoice", "BooleanAnswer", "RankedChoice", "ShortAnswer"];
	if(permittedQuestionTypes.indexOf(req["body"].question_type) < 0) {
		result["statusExtended"] = "An internal error occurred. Please try again.";
		res.send(result);
		return;
	}

	// Check that the question title is not empty.
	if(req["body"]["question_title"] === undefined || req["body"]["question_title"].trim().length === 0) {
		result["statusExtended"] = "Please specify a title for this question.";
		res.send(result);
		return;
	}

	try {
		// Make sure that the question set is owned by the logged in user.
		let databaseResult = database.query("SELECT questionset_owner FROM question_set WHERE questionset_id = ?", [req["params"]["questionSetId"]]);
		if(databaseResult.length === 0 || databaseResult[0]["questionset_owner"] !== req["user"])  {
			result["statusExtended"] = "An internal error occurred. Please try again.";
			res.send(result);
			return;
		}

		// Insert the question into the database.
		database.query("INSERT INTO question (question_set, question_type, question_title, question_data) VALUES (?, ?, ?, ?)", [req["params"]["questionSetId"], req["body"]["question_type"], req["body"]["question_title"], req["body"]["question_data"]]);
		result["status"] = "OK";
		result["redirectTo"] = req["baseUrl"] + "/question-set/" + req["params"]["questionSetId"] + "/modify";
		res.send(result);
	} catch (error) {
		console.log(error);

		next(error);
	}
});

router.post("/question-set/:questionSetId/modify/setName", authenticationRequired, function(req, res) {
	try {
		// Check that the question set is owned by the logged in user.
		let databaseResult = database.query("SELECT questionset_owner FROM question_set WHERE questionset_id = ?", [req["params"]["questionSetId"]]);
		if(databaseResult.length === 0 || databaseResult[0]["questionset_owner"] !== req["user"]) {
			let error = new Error("Unable to find question set.");
			error["status"] = 404;
			next(error);

			return;
		}

		// Update the question set name.
		database.query("UPDATE question_set SET questionset_name = ? WHERE questionset_id = ?", [req["body"]["name"], req["params"]["questionSetId"]]);
	} catch (error) {
		console.log(error);

		if(error["code"]) {
			switch(error["code"]) {
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

	res.redirect(req["baseUrl"] + "/question-set/" + req["params"]["questionSetId"] + "/modify");
});

router.get("/question-set/:questionSetId/sessions", authenticationRequired, function(req, res, next) {
	try {
		let databaseResult = database.query("SELECT questionset_name, questionset_owner FROM question_set WHERE questionset_id = ?", [req["params"]["questionSetId"]]);
		if(databaseResult.length === 0 || databaseResult[0]["questionset_owner"] !== req["user"]) {
			let error = new Error("Unable to find question set.");
			error["status"] = 404;
			next(error);

			return;
		}

		let questionSetName = databaseResult[0]["questionset_name"];

		databaseResult = database.query("SELECT session_id, session_starttime, session_endtime FROM session WHERE session_questionset = ? ORDER BY session_endtime DESC, session_starttime DESC", [req["params"]["questionSetId"]]);
		let sessions = [];
		if(databaseResult.length > 0) {
			databaseResult.forEach(function(session) {
				sessions.push({
					"id": session["session_id"],
					"starttime": dateformat(session["session_starttime"], "mm/dd/yyyy h:MM:ss TT"),
					"endtime": (session["session_endtime"] != null) ? dateformat(session["session_endtime"], "mm/dd/yyyy h:MM:ss TT") : null
				});
			});
		}

		res.render("educator/question-set/sessions", {"baseUrl": req["baseUrl"], "questionSetId": req["params"]["questionSetId"], "setName": questionSetName, "sessions": sessions});
	} catch (error) {
		console.log(error);

		next(error);
	}
});

router.get("/question-set/:questionSetId/create-session", authenticationRequired, function(req, res, next) {
	const sessionId = crypto.randomBytes(4).toString("hex").slice(0, 7).toUpperCase();

	try {
		let databaseResult = database.query("SELECT questionset_owner FROM question_set WHERE questionset_id = ?", [req["params"]["questionSetId"]]);
		if(databaseResult.length === 0 || databaseResult[0]["questionset_owner"] !== req["user"]) {
			let error = new Error("Unable to find question set.");
			error["status"] = 404;
			next(error);

			return;
		}

		database.query("INSERT INTO session (session_id, session_owner, session_questionset) VALUES (?, ?, ?)", [sessionId, req["user"], req["params"]["questionSetId"]]);
		res.redirect(req["baseUrl"] + "/session/" + sessionId + "/control");
	} catch (error) {
		console.log(error);
		let systemError = new Error("A system error was encountered. Please try again later.");
		systemError["status"] = 500;
		next(systemError);
	}
});

router.get("/session/:sessionId/aggregate-response/:questionId", authenticationRequired, function(req, res, next) {
	try {
		let databaseResult = database.query("SELECT session_owner, session_questionset FROM session WHERE session_id = ?", [req["params"]["sessionId"]]);
		if (databaseResult.length === 0 || databaseResult[0]["session_owner"] !== req["user"]) {
			let error = new Error("Unable to find session.");
			error["status"] = 404;
			next(error);

			return;
		}

		let questionSet = databaseResult[0]["session_questionset"];

		databaseResult = database.query("SELECT question_type, question_data FROM question WHERE question_id = ? AND question_set = ?", [req["params"]["questionId"], questionSet]);
		if(databaseResult.length === 0) {
			let error = new Error("Unable to find question.");
			error["status"] = 404;
			next(error);

			return;
		}

		let questionType = databaseResult[0].question_type;
		let questionData = databaseResult[0].question_data;

		try {
			switch(questionType) {
				case "MultipleChoice":
				case "BooleanAnswer": {
					databaseResult = databaseResult = database.query("SELECT response_data, COUNT(*) as response_count FROM response WHERE response_session = ? AND response_question = ? GROUP BY response_data", [req["params"]["sessionId"], req["params"]["questionId"]]);
					let resultData = {};

					if (questionType === "BooleanAnswer") {
						resultData["True"] = 0;
						resultData["False"] = 0;
					} else {
						questionData = JSON.parse(questionData);

						for (let i = 0; i < questionData["choices"].length; ++i) {
							resultData[questionData["choices"][i]] = 0;
						}
					}

					if (databaseResult.length > 0) {
						databaseResult.forEach(function (result) {
							let answer = JSON.parse(result["response_data"])["answer"];
							resultData[answer] = result["response_count"];
						});
					}

					res.send(resultData);
				}
				break;

				case "RankedChoice": {
					let resultData = {};
					databaseResult = databaseResult = database.query("SELECT response_data, COUNT(*) as response_count FROM response WHERE response_session = ? AND response_question = ? GROUP BY response_data", [req["params"]["sessionId"], req["params"]["questionId"]]);
					if (databaseResult.length > 0) {
						databaseResult.forEach(function (result) {
							let answer = JSON.parse(result["response_data"])["answer"];
							resultData[answer.join(", ")] = result["response_count"];
						});
					}

					res.send(resultData);
				}
				break;

				case "ShortAnswer": {
					res.send({});
				}
				break;
			}
		} catch (error) {
			console.log(error);

			let errorReturn = new Error("An internal error occurred.");
			errorReturn["status"] = 500;
			next(errorReturn);

			return;
		}
	} catch (error) {
		console.log(error);

		next(error);
	}
});

router.get("/session/:sessionId", authenticationRequired, function(req, res, next) {
	try {
		let databaseResult = database.query("SELECT session_owner, session_questionset, session_starttime, session_endtime FROM session WHERE session_id = ?", [req["params"]["sessionId"]]);
		if (databaseResult.length === 0 || databaseResult[0]["session_owner"] !== req["user"]) {
			let error = new Error("Unable to find session.");
			error["status"] = 404;
			next(error);

			return;
		}

		let questionSetId = databaseResult[0]["session_questionset"];
		let sessionStartTime = dateformat(databaseResult[0]["session_starttime"], "mm/dd/yyyy h:MM:ss TT");
		let sessionEndTime = (databaseResult[0]["session_endtime"] != null) ? dateformat(databaseResult[0]["session_endtime"], "mm/dd/yyyy h:MM:ss TT") : null;

		databaseResult = database.query("SELECT questionset_name FROM question_set WHERE questionset_id = ?", [questionSetId]);
		let questionSetName = databaseResult[0]["questionset_name"];

		databaseResult = database.query("SELECT COUNT(DISTINCT(response_device)) as total_students FROM response WHERE response_session = ?", [req["params"]["sessionId"]]);
		let totalStudents = databaseResult[0]["total_students"];

		res.render("educator/session/home", {
			"baseUrl": req["baseUrl"],
			"sessionId": req["params"]["sessionId"],
			"questionSetId": questionSetId,
			"questionSetName": questionSetName,
			"startTime": sessionStartTime,
			"endTime": sessionEndTime,
			"totalStudents": totalStudents
		});
	} catch (error) {
		console.log(error);

		next(error);
	}
});

router.get("/session/:sessionId/export", authenticationRequired, function(req, res, next) {
	try {
		let databaseResult = database.query("SELECT session_owner, session_questionset FROM session WHERE session_id = ?", [req["params"]["sessionId"]]);
		if (databaseResult.length === 0 || databaseResult[0]["session_owner"] !== req["user"]) {
			let error = new Error("Unable to find session.");
			error["status"] = 404;
			next(error);

			return;
		}

		databaseResult = database.query("SELECT question_id, question_title FROM question WHERE question_set = ?", [databaseResult[0]["session_questionset"]]);
		let questions = {};
		for(let i = 0; i < databaseResult.length; ++i) {
			questions[databaseResult[i]["question_id"]] = databaseResult[i]["question_title"];
		}

		let recordedQuestions = [];
		let recordedAnswersByDevice = {};
		databaseResult = database.query("SELECT response_device, response_question, response_data FROM response WHERE response_session = ?", [req["params"]["sessionId"]]);
		databaseResult.forEach(function(result) {
			if(!recordedAnswersByDevice[result["response_device"]]) {
				recordedAnswersByDevice[result["response_device"]] = {};
			}
			try {
				recordedAnswersByDevice[result["response_device"]][result["response_question"]] = JSON.parse(result["response_data"])["answer"];
			} catch (error) {}

			if(recordedQuestions.indexOf(result["response_question"]) < 0) {
				recordedQuestions.push(result["response_question"]);
			}
		});

		let csvHeaders = ["Question"];
		for(const device in recordedAnswersByDevice) {
			csvHeaders.push(device);
		}

		res.setHeader('Content-disposition', 'attachment; filename=session-' + req["params"]["sessionId"] + '.csv');
		res.setHeader('Content-type', 'text/csv');

		let csv = csvWriter({"headers": csvHeaders});
		csv.pipe(res);
		for(let i = 0; i < recordedQuestions.length; ++i) {
			let csvRow = [questions[recordedQuestions[i]]];
			for(let j = 1; j < csvHeaders.length; ++j) {
				if(recordedAnswersByDevice[csvHeaders[j]][recordedQuestions[i]]) {
					csvRow.push(recordedAnswersByDevice[csvHeaders[j]][recordedQuestions[i]]);
				} else {
					csvRow.push("");
				}
			}
			csv.write(csvRow);
		}
		csv.end();
	} catch (error) {
		console.log(error);

		next(error);
	}
});

router.post("/session/:sessionId/delete", authenticationRequired, function(req, res, next) {
	try {
		let databaseResult = database.query("SELECT session_owner, session_questionset FROM session WHERE session_id = ?", [req["params"]["sessionId"]]);
		if (databaseResult.length === 0 || databaseResult[0]["session_owner"] !== req["user"]) {
			let error = new Error("Unable to find session.");
			error["status"] = 404;
			next(error);

			return;
		}

		database.query("DELETE FROM session WHERE session_id = ?", [req["params"]["sessionId"]]);
		res.redirect(req["baseUrl"] + "/question-set/" + databaseResult[0]["session_questionset"] + "/sessions");
	} catch (error) {
		console.log(error);

		next(error);
	}
});

router.get("/session/:sessionId/control", authenticationRequired, function(req, res, next) {
	try {
		let databaseResult = database.query("SELECT session_owner, session_questionset, session_endtime FROM session WHERE session_id = ?", [req["params"]["sessionId"]]);
		if (databaseResult.length === 0 || databaseResult[0]["session_owner"] !== req["user"]) {
			let error = new Error("Unable to find session.");
			error["status"] = 404;
			next(error);

			return;
		}

		if(databaseResult[0]["session_endtime"] !== null) {
			database.query("UPDATE session SET session_endtime = NULL WHERE session_id = ?", [req["params"]["sessionId"]]);
		}

		res.render("educator/session/control", {
			"baseUrl": req["baseUrl"],
			"sessionId": req["params"]["sessionId"],
			"sessionAuthenticator": jwt.sign({ "sessionId": req["params"]["sessionId"] }, KeyManager.getPrivateKey(), { algorithm: "RS256", expiresIn: 5 }),
			"questionSet": databaseResult[0]["session_questionset"]
		});
	} catch(error) {
		console.log(error);
		let returnError = new Error("Failed to start session.");
		returnError["status"] = 500;
		next(returnError);
	}
});

router.post("/session/:sessionId/end", authenticationRequired, function(req, res, next) {
	try {
		let databaseResult = database.query("SELECT session_owner FROM session WHERE session_id = ?", [req["params"]["sessionId"]]);
		if (databaseResult.length === 0 || databaseResult[0]["session_owner"] !== req["user"]) {
			let error = new Error("Unable to find session.");
			error["status"] = 404;
			next(error);

			return;
		}

		let sessionId = req["params"]["sessionId"];

		if(SessionsManager.existsSession(sessionId)) {
			SessionsManager.getSession(sessionId).endSession();
		}

		database.query("UPDATE session SET session_endtime = NOW() WHERE session_id = ?", [sessionId]);

		res.redirect(req["baseUrl"] + "/session/" + sessionId);
	} catch(error) {
		console.log(error);
		let returnError = new Error("Failed to start session.");
		returnError["status"] = 500;
		next(returnError);
	}
});

router.ws("/session/:sessionId/control/connect", function(ws, req) {
	let sessionId = req["params"]["sessionId"];
	let session = null;
	let questionSet = null;

	try {
		let databaseResult = database.query("SELECT session_questionset FROM session WHERE session_id = ?", [req["params"]["sessionId"]]);
		if (databaseResult.length === 0) {
			let error = new Error("Unable to find session.");
			error["status"] = 404;
			next(error);

			return;
		}

		questionSet = databaseResult[0]["session_questionset"];
	} catch(error) {
		console.log(error);

		let returnError = new Error("Failed to start session.");
		returnError["status"] = 500;
		next(returnError);

		return;
	}

	ws.on("message", function(message) {
		message = JSON.parse(message);

		/*
		 * The first message from the client should always been an authenticating message.
		 * If the message is not to authenticate and the session hasn"t been established,
		 * we are going to kill the connection.
		 */
		if(session == null && message["messageType"] !== "CONNECT_SESSION") {
			ws.close();
			return;
		}

		switch(message.messageType) {
			case "CONNECT_SESSION":
				try {
					let sessionData = jwt.verify(message["messageData"], KeyManager.getPublicKey(), {algorithms: ["RS256"]});

					// Though this is a valid key, if it isn"t for the current session, let"s kill it.
					if (sessionData["sessionId"] !== sessionId) {
						ws.close();
						return;
					}

					session = SessionsManager.getSession(sessionId);
					session.setEducatorSession(ws);

					ws.send(JSON.stringify({
						"messageType": "CONNECT_SESSION",
						"messageData": {
							"status": "OK",
							"statusExtended": {
								"currentQuestion": session.getQuestionId()
							}
						}
					}));
				} catch (error) {
					// Failed to authenticate with session.
					ws.close();
				}
				break;

			case "SEND_QUESTION":
				if(message["messageData"]["question_id"] === -1) {
					session.setQuestion(null);
					return;
				}

				let databaseResult = database.query("SELECT question_set, question_type, question_title, question_data FROM question WHERE question_id = ?", [message["messageData"]["question_id"]]);
				if(databaseResult.length === 0 || databaseResult[0]["question_set"] !== questionSet) {
					return;
				}

				let question_data = {};
				try {
					question_data = JSON.parse(databaseResult[0]["question_data"]);
					if(question_data == null) {
						question_data = {};
					}
				} catch (error) {
					question_data = {};
				}

				session.setQuestion({
					"questionId": message["messageData"]["question_id"],
					"questionType": databaseResult[0]["question_type"],
					"questionTitle": databaseResult[0]["question_title"],
					"questionData": question_data
				});
				break;
		}
	});

	ws.on("close", function() {
		if(session != null) {
			session.setEducatorSession(null);
		}
	});
});

router.get("/session/:sessionId/authenticate", authenticationRequired, function(req, res, next) {
	try {
		let databaseResult = database.query("SELECT session_owner FROM session WHERE session_id = ? AND session_endtime IS NULL", [req["params"]["sessionId"]]);
		if (databaseResult.length === 0 || databaseResult[0]["session_owner"] !== req["user"]) {
			let error = new Error("Unable to find session.");
			error["status"] = 404;
			next(error);

			return;
		}

		res.render("educator/session/authenticate", {
			"sessionId": req["params"]["sessionId"],
			"sessionAuthenticator": jwt.sign({ "sessionId": req["params"]["sessionId"] }, KeyManager.getPrivateKey(), { algorithm: "RS256", expiresIn: 5 })
		});
	} catch (error) {
		console.log(error);

		next(error);
	}
});

router.ws("/session/:sessionId/authenticate/connect", function(ws, req) {
	let sessionId = req["params"]["sessionId"];
	let session = null;

	ws.on("message", function(message) {
		message = JSON.parse(message);

		/*
		 * The first message from the client should always been an authenticating message.
		 * If the message is not to authenticate and the session hasn"t been established,
		 * we are going to kill the connection.
		 */
		if(session == null && message["messageType"] !== "CONNECT_SESSION") {
			ws.close();
			return;
		}

		switch(message["messageType"]) {
			case "CONNECT_SESSION":
				try {
					let sessionData = jwt.verify(message["messageData"], KeyManager.getPublicKey(), {algorithms: ["RS256"]});

					// Though this is a valid key, if it isn"t for the current session, let"s kill it.
					if (sessionData["sessionId"] !== sessionId) {
						ws.close();
						return;
					}

					session = SessionsManager.getSession(sessionId);
					session.setAuthenticatorSession(ws);
				} catch (error) {
					console.log(error);
					// Failed to authenticate with session.
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

router.use(function(err, req, res, next) {
	res["locals"]["message"] = err["message"];
	res["locals"]["error"] = req["app"].get("env") === "development" ? err : {};

	res.status(err["status"] || 500);
	res.render("educator/error", {"baseUrl": req["baseUrl"]});
});

module.exports = router;
