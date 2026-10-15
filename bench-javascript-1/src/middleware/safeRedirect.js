'use strict';

function isLocalPath(value) {
  return typeof value === 'string'
    && value.startsWith('/')
    && !value.startsWith('//')
    && !value.includes('\\');
}

module.exports = function safeRedirect(param) {
  return (req, res, next) => {
    if (req.query[param] !== undefined && !isLocalPath(req.query[param])) {
      req.query[param] = '/';
    }
    next();
  };
};
