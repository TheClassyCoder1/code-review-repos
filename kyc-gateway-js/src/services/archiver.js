'use strict';

const { exec } = require('child_process');
const fs = require('fs/promises');
const path = require('path');
const { promisify } = require('util');
const config = require('../config');

const execAsync = promisify(exec);

async function archiveApplicant(applicantId, snapshot) {
  const workDir = path.join(config.archiveDir, applicantId);
  await fs.mkdir(workDir, { recursive: true });
  await fs.writeFile(path.join(workDir, 'applicant.json'), JSON.stringify(snapshot));
  await execAsync(`tar -czf ${workDir}.tgz -C ${config.archiveDir} ${applicantId}`);
  await fs.rm(workDir, { recursive: true, force: true });
  return `${workDir}.tgz`;
}

module.exports = { archiveApplicant };
