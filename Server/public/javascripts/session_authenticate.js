let webSocket = new WebSocket(window.location.href.split('#')[0].replace('http', 'ws')+"/connect");
const sessionId = document.getElementById("sessionId").value;
const sessionAuthenticator = document.getElementById("sessionAuthenticator").value;
let qrCode = new QRCode("qrcode", {
	text: "",
	width: Math.min(window.innerWidth, window.innerHeight) - 20,
	height: Math.min(window.innerWidth, window.innerHeight) - 20,
	colorDark: "#000000",
	colorLight: "#FFFFFF",
	correctLevel: QRCode.CorrectLevel.H,
	useSVG: true
});

window.onresize = function() {
	document.getElementById("qrcode").innerHTML = "";

	let squareSize = Math.min(window.innerWidth, window.innerHeight) - 20;
	qrCode = new QRCode("qrcode", {
		text: "",
		width: squareSize,
		height: squareSize,
		colorDark: "#000000",
		colorLight: "#FFFFFF",
		correctLevel: QRCode.CorrectLevel.H,
		useSVG: true
	});
};

webSocket.onopen = function() {
	webSocket.send(JSON.stringify({
		messageType: "CONNECT_SESSION",
		messageData: sessionAuthenticator
	}));
};

webSocket.onmessage = function(message) {
	message = JSON.parse(message.data);
	switch(message.messageType) {
		case "SESSION_AUTHENTICATION":
			qrCode.clear();
			qrCode.makeCode(sessionId + ":" + message.messageData);
			break;
	}
};