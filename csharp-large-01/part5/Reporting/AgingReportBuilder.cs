using System;
using System.Collections.Generic;
using System.Linq;
using CsharpLarge01.Part5.Domain;

namespace CsharpLarge01.Part5.Reporting
{
    public sealed class AgingReportRow
    {
        public AgingReportRow(DelinquencyBucket bucket, int loanCount, decimal amountPastDue, decimal shareOfPastDue)
        {
            Bucket = bucket;
            LoanCount = loanCount;
            AmountPastDue = amountPastDue;
            ShareOfPastDue = shareOfPastDue;
        }

        public DelinquencyBucket Bucket { get; }

        public int LoanCount { get; }

        public decimal AmountPastDue { get; }

        /// <summary>Fraction of the portfolio's past-due amount held in this bucket, 0..1.</summary>
        public decimal ShareOfPastDue { get; }
    }

    /// <summary>
    /// Summarises a portfolio's delinquency snapshot into the aging report the
    /// collections team reviews each morning. Every bucket is always present, in
    /// ascending order of severity, so the report layout is stable.
    /// </summary>
    public sealed class AgingReportBuilder
    {
        public IReadOnlyList<AgingReportRow> Build(IEnumerable<DelinquencyStatus> statuses)
        {
            if (statuses == null)
            {
                throw new ArgumentNullException(nameof(statuses));
            }

            var latestPerLoan = new Dictionary<string, DelinquencyStatus>(StringComparer.Ordinal);
            foreach (var status in statuses)
            {
                // A loan may be classified more than once in a batch; the last snapshot wins.
                latestPerLoan[status.LoanId] = status;
            }

            var grouped = latestPerLoan.Values
                .GroupBy(s => s.Bucket)
                .ToDictionary(g => g.Key, g => new { Count = g.Count(), Amount = g.Sum(s => s.AmountPastDue) });

            var totalPastDue = grouped.Where(g => g.Key != DelinquencyBucket.Current).Sum(g => g.Value.Amount);
            var rows = new List<AgingReportRow>();

            foreach (DelinquencyBucket bucket in Enum.GetValues(typeof(DelinquencyBucket)))
            {
                var count = grouped.TryGetValue(bucket, out var entry) ? entry.Count : 0;
                var amount = entry != null && bucket != DelinquencyBucket.Current ? entry.Amount : 0m;
                var share = totalPastDue == 0m ? 0m : Math.Round(amount / totalPastDue, 4, MidpointRounding.ToEven);
                rows.Add(new AgingReportRow(bucket, count, amount, share));
            }

            return rows;
        }
    }
}
