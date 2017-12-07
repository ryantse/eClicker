var express = require('express');
var router = express.Router();
var deviceTokenManager = require('../DeviceTokenManager');

/* GET home page. */
router.get('/', function(req, res, next) {

	res.render('index', { token: deviceTokenManager.retrieveDeviceId(deviceTokenManager.generateDeviceToken()) });
});

module.exports = router;
