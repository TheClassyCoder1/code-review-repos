using System;
using CsharpLarge01.Part5.Domain;

namespace CsharpLarge01.Part5.Collections
{
    /// <summary>
    /// Places a loan into a days-past-due bucket. Small residual balances below the
    /// tolerance (rounding pennies) do not make an account delinquent.
    /// </summary>
    public sealed class DelinquencyClassifier
    {
        private readonly decimal _pastDueTolerance;

        public DelinquencyClassifier(decimal pastDueTolerance)
        {
            if (pastDueTolerance < 0m)
            {
                throw new ArgumentOutOfRangeException(nameof(pastDueTolerance));
            }
            _pastDueTolerance = pastDueTolerance;
        }

        public DelinquencyStatus Classify(ServicedLoan loan, DateTime asOf)
        {
            if (loan == null)
            {
                throw new ArgumentNullException(nameof(loan));
            }

            var today = asOf.Date;
            DateTime? oldestMissedDueDate = null;
            var amountPastDue = 0m;

            foreach (var installment in loan.UnsettledInstallments)
            {
                // An installment due today is not yet late.
                if (installment.DueDate >= today)
                {
                    continue;
                }

                amountPastDue += installment.TotalOutstanding;
                if (oldestMissedDueDate == null || installment.DueDate < oldestMissedDueDate.Value)
                {
                    oldestMissedDueDate = installment.DueDate;
                }
            }

            if (oldestMissedDueDate == null || amountPastDue <= _pastDueTolerance)
            {
                return new DelinquencyStatus(loan.LoanId, 0, amountPastDue, DelinquencyBucket.Current);
            }

            var daysPastDue = (today - oldestMissedDueDate.Value).Days;
            return new DelinquencyStatus(loan.LoanId, daysPastDue, amountPastDue, ToBucket(daysPastDue));
        }

        public static DelinquencyBucket ToBucket(int daysPastDue)
        {
            if (daysPastDue <= 0)
            {
                return DelinquencyBucket.Current;
            }
            if (daysPastDue <= 30)
            {
                return DelinquencyBucket.Days1To30;
            }
            if (daysPastDue <= 60)
            {
                return DelinquencyBucket.Days31To60;
            }
            return daysPastDue <= 90 ? DelinquencyBucket.Days61To90 : DelinquencyBucket.Over90;
        }
    }
}
