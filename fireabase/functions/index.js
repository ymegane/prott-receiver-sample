const URL = require('url');
const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp(functions.config().firebase);

exports.preview = functions.https.onRequest((request, response) => {
  console.log('request:', request);

  var shareUrl;

  if (request.method === "POST") {
    console.log('body:', request.body);
    shareUrl = request.body.s;
  } else if (request.method === "GET") {
    shareUrl = request.query.s;
  } else {
    response.status(404).end();
    return;
  }

  console.log('share url:', shareUrl);

  if (!shareUrl || URL.parse(shareUrl).host !== "prottapp.com") {
    response.status(400).end();
    return;
  }

  const payload = {
    data : {
      shareUrl : `${shareUrl}`
    }
  };

  const topic = "preview"
  admin.messaging().sendToTopic(topic, payload).then(function(response) {
      console.log("Successfully sent message:", response);
    }).catch(function(error) {
      console.log("Error sending message:", error);
    });

  response.send(200).end();
});

const express = require('express');
const cookieParser = require('cookie-parser')();
const cors = require('cors')({origin: true});
const app = express();

// Express middleware that validates Firebase ID Tokens passed in the Authorization HTTP header.
// The Firebase ID token needs to be passed as a Bearer token in the Authorization HTTP header like this:
// `Authorization: Bearer <Firebase ID Token>`.
// when decoded successfully, the ID Token content will be added as `req.user`.
const validateFirebaseIdToken = (req, res, next) => {
  console.log('Check if request is authorized with Firebase ID token');

  if ((!req.headers.authorization || !req.headers.authorization.startsWith('Bearer ')) &&
      !req.cookies.__session) {
    console.error('No Firebase ID token was passed as a Bearer token in the Authorization header.',
        'Make sure you authorize your request by providing the following HTTP header:',
        'Authorization: Bearer <Firebase ID Token>',
        'or by passing a "__session" cookie.');
    res.status(403).send('Unauthorized');
    return;
  }

  let idToken;
  if (req.headers.authorization && req.headers.authorization.startsWith('Bearer ')) {
    console.log('Found "Authorization" header');
    // Read the ID Token from the Authorization header.
    idToken = req.headers.authorization.split('Bearer ')[1];
  } else {
    console.log('Found "__session" cookie');
    // Read the ID Token from cookie.
    idToken = req.cookies.__session;
  }
  admin.auth().verifyIdToken(idToken).then(decodedIdToken => {
    console.log('ID Token correctly decoded', decodedIdToken);
    req.user = decodedIdToken;
    next();
  }).catch(error => {
    console.error('Error while verifying Firebase ID token:', error);
    res.status(403).send('Unauthorized');
  });
};

app.use(cors);
app.use(cookieParser);
app.use(validateFirebaseIdToken);
app.get('/authpreview', (req, res) => {
  console.log('uid:', `${req.user.uid}`);

  admin.database().ref(`/users/${req.user.uid}`).once('value').then(snap => {
    const tokens = [];
    const snapshot = snap.val();

    console.log('snapshot.devices:', snapshot.devices);
    if (snapshot.devices) {
        Object.keys(snapshot.devices).forEach(deviceId => {
          console.log('deviceId:', deviceId);
          tokens.push(snapshot.devices[deviceId].token);
        });

        const shareUrl = req.query.s;
        console.log('share url:', shareUrl);

        if (!shareUrl || URL.parse(shareUrl).host !== "prottapp.com") {
          res.status(400).send('Illegal params');
          return;
        }

        const payload = {
          data : {
            shareUrl : `${shareUrl}`
          }
        };

        admin.messaging().sendToDevice(tokens, payload).then(function(response) {
          console.log("Successfully sent message:", response);
          res.status(200).send("Successfully sent message");
        }).catch(function(error) {
          console.log("Error sending message:", error);
          res.status(200).send("Error sending message");
        });
        return;
      }
      res.status(200).send("First, you need to register the devices from app");
    });
});
exports.app = functions.https.onRequest(app);
