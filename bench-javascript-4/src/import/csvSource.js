'use strict';

const fs = require('node:fs');
const { Transform } = require('node:stream');

const READ_CHUNK_BYTES = 64 * 1024;

function openCsvSource(filePath) {
  return fs.createReadStream(filePath, {
    encoding: 'utf8',
    highWaterMark: READ_CHUNK_BYTES,
  });
}

function splitLines() {
  let carry = '';
  return new Transform({
    readableObjectMode: true,
    transform(chunk, _encoding, callback) {
      const parts = (carry + chunk).split(/\r?\n/);
      carry = parts.pop();
      for (const line of parts) {
        if (line.trim() !== '') this.push(line);
      }
      callback();
    },
    flush(callback) {
      if (carry.trim() !== '') this.push(carry);
      callback();
    },
  });
}

module.exports = { openCsvSource, splitLines };
