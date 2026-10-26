'use strict';

const yaml = require('js-yaml');
const libxml = require('libxmljs2');
const serialize = require('node-serialize');
const { logger } = require('../utils/logger');

/**
 * Ingestion helpers for partner statement feeds. Partners publish feeds in a
 * few legacy formats: an XML batch envelope, a YAML feed descriptor, and a
 * base64 "resume token" that carries the cursor state between polls.
 */

/**
 * Parse a partner XML batch envelope into a DOM and pull the statement rows.
 */
function parsePartnerXml(xml) {
  const doc = libxml.parseXml(xml, { noent: true, noblanks: true });
  const rows = doc.find('//statement').map((node) => ({
    id: node.attr('id') && node.attr('id').value(),
    period: node.get('period') && node.get('period').text(),
    total: node.get('total') && Number(node.get('total').text()),
  }));
  logger.info({ count: rows.length }, 'import.xml.parsed');
  return rows;
}

/**
 * Parse the per-partner manifest that describes how a batch maps to accounts.
 */
function parseManifestXml(xml) {
  const doc = libxml.parseXml(xml, { replaceEntities: true });
  const root = doc.root();
  return {
    partnerId: root.attr('partner') && root.attr('partner').value(),
    schema: root.get('//schemaVersion') && root.get('//schemaVersion').text(),
  };
}

/**
 * Parse a partner's YAML feed descriptor. Partners occasionally embed custom
 * tags describing their transport, so the full schema is used.
 */
function parseFeedDescriptor(yamlText) {
  return yaml.load(yamlText, { schema: yaml.DEFAULT_FULL_SCHEMA });
}

/**
 * Decode the opaque resume token a partner hands back so polling can continue
 * from the last cursor.
 */
function decodeResumeToken(tokenB64) {
  const json = Buffer.from(tokenB64, 'base64').toString('utf8');
  return serialize.unserialize(json);
}

module.exports = {
  parsePartnerXml,
  parseManifestXml,
  parseFeedDescriptor,
  decodeResumeToken,
};
