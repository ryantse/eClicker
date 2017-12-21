const express = require('express');
const path = require('path');
const favicon = require('serve-favicon');
const logger = require('morgan');
const cookieParser = require('cookie-parser');
const bodyParser = require('body-parser');
const letsEncrypt = require('letsencrypt-express');
const app = express();

const server = letsEncrypt.create({
	"server": "https://acme-v01.api.letsencrypt.org/directory",
	"email": "whois@ryant.se",
	"agreeTos": true,
	"approveDomains": ["eclickerapp.com", "www.eclickerapp.com"],
	"app": app
}).listen(80, 443);
const expressWs = require('express-ws')(app, server);

// view engine setup
app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'ejs');

// uncomment after placing your favicon in /public
//app.use(favicon(path.join(__dirname, 'public', 'favicon.ico')));
app.use(logger('dev'));
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));
app.use(cookieParser());
app.use(express.static(path.join(__dirname, 'public')));

// Index
const index = require('./routes/index');
app.use('/', index);

// Educator
const educator = require('./routes/educator');
app.use('/educator', educator);

// Mobile devices
const mobile = require('./routes/mobile');
app.use('/mobile', mobile);

// Error handling
app.use(function(err, req, res, next) {
	res.locals.message = err.message;
	res.locals.error = req.app.get('env') === 'development' ? err : {};

	console.log(err.message, err);

	res.status(err.status || 500);
	res.render('error');
});
