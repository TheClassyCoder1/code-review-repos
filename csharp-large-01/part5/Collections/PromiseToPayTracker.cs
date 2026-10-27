using System;
using System.Collections.Generic;
using System.Linq;
using CsharpLarge01.Part5.Domain;

namespace CsharpLarge01.Part5.Collections
{
    public enum PromiseOutcome
    {
        Pending,
        Kept,
        PartiallyKept,
        Broken
    }

    public sealed class PromiseToPay
    {
        public PromiseToPay(string promiseId, string loanId, decimal amount, DateTime createdOn, DateTime promisedDate)
        {
            if (amount <= 0m)
            {
                throw new ArgumentOutOfRangeException(nameof(amount));
            }
            if (promisedDate.Date < createdOn.Date)
            {
                throw new ArgumentException("A promise cannot be dated before it was taken.", nameof(promisedDate));
            }

            PromiseId = promiseId ?? throw new ArgumentNullException(nameof(promiseId));
            LoanId = loanId ?? throw new ArgumentNullException(nameof(loanId));
            Amount = amount;
            CreatedOn = createdOn;
            PromisedDate = promisedDate;
        }

        public string PromiseId { get; }

        public string LoanId { get; }

        public decimal Amount { get; }

        public DateTime CreatedOn { get; }

        public DateTime PromisedDate { get; }
    }

    /// <summary>
    /// Decides whether a promise-to-pay recorded by a collections agent was honoured.
    /// Payments count if they land between the day the promise was taken and the
    /// promised date plus a small clearing tolerance.
    /// </summary>
    public sealed class PromiseToPayTracker
    {
        private readonly int _clearingToleranceDays;

        public PromiseToPayTracker(int clearingToleranceDays)
        {
            if (clearingToleranceDays < 0)
            {
                throw new ArgumentOutOfRangeException(nameof(clearingToleranceDays));
            }
            _clearingToleranceDays = clearingToleranceDays;
        }

        public PromiseOutcome Evaluate(PromiseToPay promise, IEnumerable<PaymentReceipt> payments, DateTime asOf)
        {
            if (promise == null)
            {
                throw new ArgumentNullException(nameof(promise));
            }

            var windowStart = promise.CreatedOn.Date;
            var windowEnd = promise.PromisedDate.Date.AddDays(_clearingToleranceDays);

            var paidInWindow = (payments ?? Enumerable.Empty<PaymentReceipt>())
                .Where(p => string.Equals(p.LoanId, promise.LoanId, StringComparison.Ordinal))
                .Where(p => p.ReceivedOn.Date >= windowStart && p.ReceivedOn.Date <= windowEnd)
                .Sum(p => p.Amount);

            if (paidInWindow >= promise.Amount)
            {
                return PromiseOutcome.Kept;
            }
            if (asOf.Date <= windowEnd)
            {
                return PromiseOutcome.Pending;
            }
            return paidInWindow > 0m ? PromiseOutcome.PartiallyKept : PromiseOutcome.Broken;
        }
    }
}
