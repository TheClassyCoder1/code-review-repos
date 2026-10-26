using System;
using CsharpLarge01.Part5.Domain;

namespace CsharpLarge01.Part5.Collections
{
    public sealed class LateFeePolicy
    {
        public LateFeePolicy(int graceDays, decimal rate, decimal minimumFee, decimal maximumFee)
        {
            if (graceDays < 0)
            {
                throw new ArgumentOutOfRangeException(nameof(graceDays));
            }
            if (rate < 0m || rate > 1m)
            {
                throw new ArgumentOutOfRangeException(nameof(rate));
            }
            if (minimumFee < 0m || maximumFee < minimumFee)
            {
                throw new ArgumentException("Fee bounds must satisfy 0 <= minimum <= maximum.");
            }

            GraceDays = graceDays;
            Rate = rate;
            MinimumFee = minimumFee;
            MaximumFee = maximumFee;
        }

        public int GraceDays { get; }

        public decimal Rate { get; }

        public decimal MinimumFee { get; }

        public decimal MaximumFee { get; }
    }

    /// <summary>
    /// Assesses at most one late fee per installment once its grace period has
    /// elapsed. The fee is a percentage of the overdue principal and interest,
    /// floored and capped by policy, and never larger than the overdue amount.
    /// </summary>
    public sealed class LateFeeCalculator
    {
        private readonly LateFeePolicy _policy;

        public LateFeeCalculator(LateFeePolicy policy)
        {
            _policy = policy ?? throw new ArgumentNullException(nameof(policy));
        }

        public decimal AssessLateFees(ServicedLoan loan, DateTime asOf)
        {
            if (loan == null)
            {
                throw new ArgumentNullException(nameof(loan));
            }

            var today = asOf.Date;
            var totalAssessed = 0m;

            foreach (var installment in loan.UnsettledInstallments)
            {
                if (installment.LateFeeAssessed || today <= installment.DueDate.AddDays(_policy.GraceDays))
                {
                    continue;
                }

                var overdue = installment.OutstandingPrincipal + installment.OutstandingInterest;
                if (overdue <= 0m)
                {
                    continue;
                }

                var fee = Math.Round(overdue * _policy.Rate, 2, MidpointRounding.AwayFromZero);
                fee = Math.Min(Math.Max(fee, _policy.MinimumFee), _policy.MaximumFee);
                fee = Math.Min(fee, overdue);
                if (fee <= 0m)
                {
                    continue;
                }

                installment.AssessLateFee(fee);
                totalAssessed += fee;
            }

            return totalAssessed;
        }
    }
}
