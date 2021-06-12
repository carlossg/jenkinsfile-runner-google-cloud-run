// Copyright 2021 Carlos Sanchez
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

'use strict';

const https = require('https');

const url = process.env.TARGET;

async function delegate(url, message) {
  let path = '/handle';
  message = JSON.stringify(message);
  var options = {
    path: path,
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Content-Length': Buffer.byteLength(message),
    },
  };
  await new Promise((resolve, reject) => {
    let req = https.request(url, options);
    req.on('error', (e) => {
      console.error(`problem with request: ${e.message}`);
      reject(e);
    });
    req.write(message);
    console.log(`Message: ${message}`);
    req.end(() => {
      console.log(`Forwarded request to ${url}${path}`);
      resolve();
    });
  });
};

/**
 * Forwards a POST to Google Cloud Run
 *
 * @param {Object} req Cloud Function request context.
 * @param {Object} res Cloud Function response context.
 */
exports.handle = (req, res) => {
  switch (req.method) {
    case 'POST':
      break;
    default:
      res.status(403).send({ error: 'Only POST allowed' });
      break;
  }

  switch (req.get('content-type')) {
    case 'application/json':
      break;
    default:
      res.status(403).send({ error: 'Only application/json allowed' });
      break;
  }
  // destination host comes in the path
  delegate(url, req.body);
  res.status(200).send(`{ "target": "${url}" }`);
};
