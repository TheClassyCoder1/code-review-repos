'use strict';

function createHttpClient({ baseUrl, timeoutMs, headers = {} }) {
  if (!timeoutMs) throw new Error('timeoutMs is required');

  async function request(method, path, body) {
    const res = await fetch(new URL(path, baseUrl), {
      method,
      headers: { 'content-type': 'application/json', ...headers },
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: AbortSignal.timeout(timeoutMs),
    });
    if (!res.ok) {
      const err = new Error(`upstream ${method} ${path} returned ${res.status}`);
      err.status = 502;
      throw err;
    }
    return res.status === 204 ? null : res.json();
  }

  return {
    get: (path) => request('GET', path),
    post: (path, body) => request('POST', path, body),
  };
}

module.exports = { createHttpClient };
