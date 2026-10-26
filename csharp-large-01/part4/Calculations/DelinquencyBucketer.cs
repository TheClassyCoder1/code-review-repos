using Lending.Servicing.Part4.Domain;

namespace Lending.Servicing.Part4.Calculations;

public static class DelinquencyBucketer
{
    public const int ChargeOffDays = 180;

    /// <summary>
    /// Buckets a loan by days past due, measured from the oldest instalment that
    /// is still unpaid. Charged-off loans stay charged off regardless of age.
    /// </summary>
    public static DelinquencyBucket Classify(DateOnly? oldestUnpaidDueDate, DateOnly asOf, bool chargedOff)
    {
        if (chargedOff)
        {
            return DelinquencyBucket.ChargedOff;
        }

        if (oldestUnpaidDueDate is null)
        {
            return DelinquencyBucket.Current;
        }

        var daysPastDue = asOf.DayNumber - oldestUnpaidDueDate.Value.DayNumber;
        return daysPastDue switch
        {
            <= 0 => DelinquencyBucket.Current,
            < 30 => DelinquencyBucket.Days1To29,
            < 60 => DelinquencyBucket.Days30To59,
            < 90 => DelinquencyBucket.Days60To89,
            _ => DelinquencyBucket.Days90Plus,
        };
    }

    public static IReadOnlyDictionary<DelinquencyBucket, int> Tally(IEnumerable<DelinquencyBucket> buckets)
    {
        var counts = Enum.GetValues<DelinquencyBucket>().ToDictionary(b => b, _ => 0);
        foreach (var bucket in buckets)
        {
            counts[bucket]++;
        }

        return counts;
    }
}
