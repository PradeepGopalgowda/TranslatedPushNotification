/*eslint-env node*/

//------------------------------------------------------------------------------
// node.js starter application for Bluemix
//------------------------------------------------------------------------------

// This application uses express as its web server
// for more info, see: http://expressjs.com
var express = require('express');
var LanguageTranslatorV2 = require('watson-developer-cloud/language-translator/v2');
var transCred = {};

// cfenv provides access to your Cloud Foundry environment
// for more info, see: https://www.npmjs.com/package/cfenv
var cfenv = require('cfenv');

// create a new express server
var app = express();

// serve the files out of ./public as our main files
app.use(express.static(__dirname + '/public'));

// get the app environment from Cloud Foundry
var appEnv = cfenv.getAppEnv();
if(appEnv.services['language_translator'] != null) {
	transCred = appEnv.services['language_translator'][0]['credentials'];
}
else {
	console.log("Bind the language translator service to the app");
}
console.log("----transCred is "+transCred.username);

var language_translator = new LanguageTranslatorV2({
  username: transCred.username,
  password: transCred.password,
  url: transCred.url
});

console.log("language_translator is "+language_translator);

app.get('/translate', function (req, res) {
	var originalContent =  req.query.content;
	var targetLanguage =  req.query.targetLang;
	language_translator.translate({
		text: originalContent,
		source: 'en',
		target: targetLanguage
	}, function(err, translation) {
		if (err)
		res.send('Original text is -'+originalContent+" & translation failed "+err);
		else {
			console.log("translation is "+translation.translations[0].translation);
			res.send(translation.translations[0].translation);
		}
	});
});

// start server on the specified port and binding host
app.listen(appEnv.port, '0.0.0.0', function() {
  // print a message when the server starts listening
  console.log("server starting on " + appEnv.url);
});
