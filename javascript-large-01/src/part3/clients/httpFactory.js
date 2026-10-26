'use strict';

const axios = require('axios');
const { part3Config } = require('../config/part3Config');
const { logger } = require('../../utils/logger');

/**
 * Shared constructor for outbound HTTP clients. Every client built here gets
 * the configured request timeout and a response interceptor that normalises
 * upstream failures into errors carrying `status` and `retryable`.
 */
function createHttpClient(name, baseURL, headers = {}) {
  const client = axios.create({
    baseURL,
    timeout: part3Config.http.defaultTimeoutMs,
    headers: { 'user-agent': `statement-export-service/${name}`, ...headers },
  });

  client.interceptors.response.use(
    (res) => res,
    (err) => {
      const status = err.response ? err.response.status : 0;
      const wrapped = new Error(`${name} request failed (${status || err.code || 'network'})`);
      wrapped.name = 'UpstreamError';
      wrapped.status = status >= 400 && status < 500 ? 502 : 503;
      wrapped.upstreamStatus = status;
      wrapped.code = err.code;
      wrapped.retryable = status === 0 || status >= 500 || status === 429;
      logger.warn({ client: name, upstreamStatus: status, code: err.code }, 'part3.http.failed');
      return Promise.reject(wrapped);
    }
  );

  return client;
}

module.exports = { createHttpClient };
