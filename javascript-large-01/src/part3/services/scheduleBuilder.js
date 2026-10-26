'use strict';

const { round2 } = require('../utils/keyedLock');

/**
 * Amortisation schedules for the "what if I pay X per month" calculator in the
 * portal. Inputs are validated by the schedules route before they get here.
 */

function addMonths(isoDate, months) {
  const d = new Date(`${isoDate}T00:00:00Z`);
  const day = d.getUTCDate();
  d.setUTCDate(1);
  d.setUTCMonth(d.getUTCMonth() + months);
  const lastDay = new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth() + 1, 0)).getUTCDate();
  d.setUTCDate(Math.min(day, lastDay));
  return d.toISOString().slice(0, 10);
}

/**
 * Build a month-by-month schedule paying `payment` until the balance is
 * cleared. The final instalment is reduced to whatever is left.
 */
function buildSchedule({ principal, monthlyRate, payment, firstDueDate }) {
  const rows = [];
  let balance = round2(principal);
  let period = 0;
  let totalInterest = 0;

  while (balance > 0.005) {
    period += 1;
    const interest = round2(balance * monthlyRate);
    const principalPart = Math.min(balance, round2(payment - interest));
    balance = round2(balance - principalPart);
    totalInterest = round2(totalInterest + interest);
    rows.push({
      period,
      dueDate: addMonths(firstDueDate, period - 1),
      payment: round2(principalPart + interest),
      interest,
      principal: principalPart,
      balance,
    });
  }

  return { periods: rows.length, totalInterest, rows };
}

/**
 * Smallest monthly payment that still makes progress on the balance.
 */
function minimumViablePayment(principal, monthlyRate) {
  return round2(principal * monthlyRate) + 1;
}

module.exports = { buildSchedule, minimumViablePayment, addMonths };
