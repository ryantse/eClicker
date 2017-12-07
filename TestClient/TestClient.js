const WebSocket = require('ws');
const request = require('sync-request');

// Get client data.
//let clientDevice = JSON.parse(request('GET', 'http://localhost:3000/mobile/register-device').getBody('utf8'));
let clientDevice = {
	deviceId: '6B9D3A00',
	deviceToken: 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJkZXZpY2UiOiI2QjlEM0EwMCIsImlhdCI6MTUxMjU0NzY4OX0.MzPrhQvnnA_goEtJTf29PPoaC0ML1O50ryhq_2n1SapB7k6R6JwWaDKQfwV65HP3bXV-kpskrDG146aIeuwgZDc3AAutHIFaM5SfCPw7yOmb3M_lA3cumkv8hkQH-Xi9YK7Qg1uX83Wo85xVVe3qNuO-rwcWeFsO-Aq1nS90eRVQZ7btJqVps3Lv7Pp3gqQryi1_GSBA7DwOo17mXOI7dQ8WW9mSiIwPltmoqmfY7urskF_pL9v9BTynfH2hWOisgJMo5qqPOKgYJjK1KwvKx2olslRHmWAeJBAg-uro9tb_mliFIwCj0-c7TmVB4jJXSjMWgnhhQA0fZ3vDqH_9tQ'
};
console.log(clientDevice);

const ws = new WebSocket('ws://localhost:3000/mobile/session');

ws.on("open", function() {
	ws.send(JSON.stringify({
		messageType: "DEVICE_JOIN",
		messageData: clientDevice.deviceToken
	}));
});

ws.on("message", function(data) {
	message = JSON.parse(data);
	console.log(message);

	switch(message.messageType) {
		case "DEVICE_JOIN":
			/*ws.send(JSON.stringify({
				messageType: "SESSION_JOIN",
				messageData: {
					authenticationType: "sessionKey",
					sessionId: "0B026725",
					sessionKeys: ["190", "c08"]
				}
			}));*/
			ws.send(JSON.stringify({
				messageType: "SESSION_JOIN",
				messageData: {
					authenticationType: "sessionToken",
					sessionToken: "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJkZXZpY2VJZCI6IjZCOUQzQTAwIiwic2Vzc2lvbklkIjoiMEIwMjY3MjUiLCJpYXQiOjE1MTI1NDc2ODl9.f-RhAn5SXnLUr8dnOq18w3ckuNl1gPON_GtPy6HuOKieDVcNi2jjcr0M61Gac8ZxRlep-dNEqNoAmfim6mH2OC2RlBv2bW-REUkNo8cKPlm6oSfIWyyY-0ZpZ2ez3MBoY1GvMijHy38O9vcEBKCvim3MjY6V9ksWluSPG2i7ZT45rLeQOCYDRl8nkGWKO6lzeO-D-rmebJwIANJNqB1xBg0JMpnA-bLVKKlA2rfuoPlMpcR_xY2UuN5Y_CyFeHvKIJrP2L3n1AzdswsAwQbkQnDCllpACBjcmvQNN0EkGeTlu8aPrjEWal6UprzGOKcu3VVcwqKyrAKaA2RJ5hSilA"
				}
			}));
			break;

		case "SESSION_JOIN":

			break;
	}
});