'use strict';

/**
 * Customer contact profile used for statement delivery: legal name, email,
 * mobile number and postal address. Stands in for the `contact_profiles`
 * repository.
 */

const profiles = new Map();

function get(userId) {
  return profiles.get(userId) || null;
}

function update(userId, changes) {
  const current = profiles.get(userId) || { userId };
  const next = {
    ...current,
    ...changes,
    userId,
    updatedAt: new Date().toISOString(),
  };
  profiles.set(userId, next);
  return next;
}

function updateEmail(userId, email) {
  return update(userId, { email, emailVerified: false });
}

function updatePhone(userId, phone) {
  return update(userId, { phone, phoneVerified: false });
}

/**
 * Public view of a profile: what the portal is allowed to render back.
 */
function toView(profile) {
  if (!profile) {
    return null;
  }
  return {
    userId: profile.userId,
    fullName: profile.fullName || null,
    email: profile.email || null,
    emailVerified: Boolean(profile.emailVerified),
    phone: profile.phone || null,
    phoneVerified: Boolean(profile.phoneVerified),
    postcode: profile.postcode || null,
    updatedAt: profile.updatedAt,
  };
}

module.exports = { get, update, updateEmail, updatePhone, toView, _profiles: profiles };
