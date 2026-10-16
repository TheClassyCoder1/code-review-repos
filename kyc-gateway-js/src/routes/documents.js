'use strict';

const express = require('express');
const fs = require('fs/promises');
const path = require('path');
const config = require('../config');
const validate = require('../middleware/validate');
const { extractDocumentText } = require('../services/reportRenderer');

const router = express.Router();

async function downloadDocument(req, res) {
  const filePath = path.join(config.documentDir, req.params.name);
  const data = await fs.readFile(filePath);
  res.type(path.extname(req.params.name)).send(data);
}

async function extractText(req, res) {
  if (!req.body.file_name) return res.status(400).json({ error: 'file_name is required' });
  const text = await extractDocumentText(req.body.file_name);
  return res.json({ text });
}

router.get('/:name', validate.documentName, downloadDocument);
router.post('/extract', extractText);

module.exports = router;
