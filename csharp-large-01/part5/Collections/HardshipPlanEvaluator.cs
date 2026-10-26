using System;
using CsharpLarge01.Part5.Domain;

namespace CsharpLarge01.Part5.Collections
{
    public enum HardshipDecision
    {
        Approved,
        DeclinedTooDelinquent,
        DeclinedUnaffordable,
        DeclinedRecentPlan
    }

    /// <summary>
    /// First-pass eligibility check for a reduced-payment hardship plan. Agents can
    /// still override a decline; this only decides what the screen pre-selects.
    /// </summary>
    public sealed class HardshipPlanEvaluator
    {
        private const int CoolingOffMonths = 12;
        private const decimal MinimumPaymentShare = 0.25m;

        public HardshipDecision Evaluate(DelinquencyStatus status, decimal statedMonthlySurplus, decimal scheduledPayment,
            DateTime? previousPlanEndedOn, DateTime asOf)
        {
            if (status == null)
            {
                throw new ArgumentNullException(nameof(status));
            }
            if (scheduledPayment <= 0m)
            {
                throw new ArgumentOutOfRangeException(nameof(scheduledPayment));
            }

            if (status.Bucket == DelinquencyBucket.Over90)
            {
                return HardshipDecision.DeclinedTooDelinquent;
            }
            if (previousPlanEndedOn.HasValue && previousPlanEndedOn.Value.Date.AddMonths(CoolingOffMonths) > asOf.Date)
            {
                return HardshipDecision.DeclinedRecentPlan;
            }

            // The reduced payment must still cover a quarter of the scheduled amount.
            var reducedPayment = Math.Round(scheduledPayment * MinimumPaymentShare, 2, MidpointRounding.AwayFromZero);
            return statedMonthlySurplus >= reducedPayment
                ? HardshipDecision.Approved
                : HardshipDecision.DeclinedUnaffordable;
        }
    }
}
