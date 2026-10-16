'use strict';

const https = require('node:https');
const { config } = require('../config');

const PLATE_PATTERN = /^[A-Z0-9]{2,8}$/;

function normalizePlate(raw) {
  if (typeof raw !== 'string') {
    return null;
  }
  const plate = raw.toUpperCase().replace(/[\s-]/g, '');
  return PLATE_PATTERN.test(plate) ? plate : null;
}

function anprHeaders(imageBuffer, cameraId) {
  return {
    'content-type': 'application/octet-stream',
    'content-length': imageBuffer.length,
    'x-camera-id': cameraId,
    authorization: `Bearer ${config.anpr.apiKey}`,
  };
}

function recognizePlate(imageBuffer, cameraId) {
  const url = new URL('/v1/recognize', config.anpr.url);
  return new Promise((resolve, reject) => {
    const req = https.request(url, { method: 'POST', headers: anprHeaders(imageBuffer, cameraId) }, (res) => {
      const chunks = [];
      res.on('data', (chunk) => chunks.push(chunk));
      res.on('end', () => {
        if (res.statusCode !== 200) {
          reject(new Error(`plate recognition failed with status ${res.statusCode}`));
          return;
        }
        const result = JSON.parse(Buffer.concat(chunks).toString('utf8'));
        resolve({ plate: normalizePlate(result.plate), confidence: result.confidence });
      });
    });
    req.on('error', reject);
    req.end(imageBuffer);
  });
}

module.exports = { normalizePlate, recognizePlate };
