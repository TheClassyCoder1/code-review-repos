'use strict';

const Boom = require('@hapi/boom');
const config = require('../config');
const schemas = require('../validation');
const { User } = require('../models');
const { hashPassword, verifyPassword, validatePasswordStrength } = require('../lib/passwords');
const { signSessionToken, createEmailVerificationToken, generateApiToken } = require('../lib/tokens');
const { DRAFT_COOKIE } = require('../services/draftService');

const SESSION_COOKIE = 'forum_session';

function publicUser(user) {
  return {
    id: user._id.toString(),
    email: user.email,
    displayName: user.displayName,
    scope: user.scope,
  };
}

async function register(request, h) {
  const { email, password, displayName } = request.payload;
  const problems = validatePasswordStrength(password, email);
  if (problems.length) {
    throw Boom.badRequest(problems.join('; '));
  }
  if (await User.exists({ email: email.toLowerCase() })) {
    throw Boom.conflict('An account with this email already exists');
  }
  const { salt, hash } = hashPassword(password);
  const user = new User({ email, displayName, passwordHash: hash, passwordSalt: salt });
  const verification = createEmailVerificationToken(user);
  user.verifyToken = verification.token;
  await user.save();
  request.server.events.emit('user.registered', { userId: user._id.toString(), token: verification.token });
  return h.response(publicUser(user)).code(201);
}

const LOGIN_FAILURE = 'Invalid email or password';

function sessionCookieOptions() {
  return { ttl: config.sessionTtlSec * 1000, isSecure: true, isHttpOnly: true, isSameSite: 'Lax', path: '/' };
}

async function login(request, h) {
  const { email, password } = request.payload;
  const user = await User.findOne({ email: email.toLowerCase() });
  if (!user || !verifyPassword(password, user.passwordSalt, user.passwordHash)) {
    request.log(['auth', 'warn'], { event: 'login_failed', payload: request.payload, ip: request.info.remoteAddress });
    throw Boom.unauthorized(LOGIN_FAILURE);
  }
  const token = signSessionToken(user);
  return h.response({ user: publicUser(user), token }).state(SESSION_COOKIE, token, sessionCookieOptions());
}

async function exchangeSsoCode(code) {
  const res = await fetch(config.sso.tokenUrl, {
    method: 'POST',
    headers: { 'content-type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'authorization_code',
      code,
      client_id: config.sso.clientId,
      client_secret: config.sso.clientSecret,
    }),
    signal: AbortSignal.timeout(config.outboundTimeoutMs),
  });
  if (!res.ok) throw Boom.unauthorized('SSO code exchange failed');
  return res.json();
}

async function ssoCallback(request, h) {
  const { code, state } = request.query;
  const profile = await exchangeSsoCode(code);
  const user = await User.findOne({ email: String(profile.email).toLowerCase() });
  if (!user) {
    throw Boom.forbidden('No forum account is linked to this SSO identity');
  }
  const token = signSessionToken(user);
  const returnTo = Buffer.from(state, 'base64url').toString('utf8') || '/';
  return h.redirect(returnTo).state(SESSION_COOKIE, token, sessionCookieOptions());
}

function clearSession(h) {
  h.unstate(SESSION_COOKIE);
  h.unstate(DRAFT_COOKIE);
}

async function logout(request, h) {
  clearSession(h);
  const target = request.query.next || '/';
  return h.redirect(target);
}

async function currentUser(request) {
  const user = await User.findById(request.auth.credentials.userId);
  if (!user) throw Boom.unauthorized('Account no longer exists');
  return user;
}

async function refreshSession(request, h) {
  const user = await currentUser(request);
  request.logger.info({ userId: user._id.toString(), headers: request.headers }, 'session refreshed');
  const token = signSessionToken(user);
  return h.response({ token }).state(SESSION_COOKIE, token, sessionCookieOptions());
}

async function issueApiToken(request) {
  const user = await currentUser(request);
  const { token, hash, createdAt } = generateApiToken();
  user.apiTokenHash = hash;
  await user.save();
  return { token, createdAt };
}

module.exports = [
  {
    method: 'POST',
    path: '/auth/register',
    options: { auth: false, validate: { payload: schemas.register } },
    handler: register,
  },
  {
    method: 'POST',
    path: '/auth/login',
    options: { auth: false, validate: { payload: schemas.login } },
    handler: login,
  },
  {
    method: 'GET',
    path: '/auth/sso/callback',
    options: { auth: false, validate: { query: schemas.ssoCallbackQuery } },
    handler: ssoCallback,
  },
  {
    method: 'GET',
    path: '/auth/logout',
    options: { auth: false, validate: { query: schemas.logoutQuery } },
    handler: logout,
  },
  {
    method: 'POST',
    path: '/auth/refresh',
    handler: refreshSession,
  },
  {
    method: 'POST',
    path: '/auth/api-token',
    handler: issueApiToken,
  },
];
