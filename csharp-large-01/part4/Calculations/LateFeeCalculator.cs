namespace Lending.Servicing.Part4.Calculations;

public sealed record LateFeePolicy(int GraceDays, decimal FlatFee, decimal PercentOfInstallment, decimal Cap)
{
    public static LateFeePolicy Standard { get; } = new(GraceDays: 10, FlatFee: 15m, PercentOfInstallment: 0.05m, Cap: 100m);
}

public static class LateFeeCalculator
{
    /// <summary>
    /// Late fee for an instalment still unpaid on <paramref name="asOf"/>. No fee
    /// is charged inside the grace period; after that the fee is the greater of
    /// the flat fee and the percentage, never more than the cap or the
    /// instalment itself.
    /// </summary>
    public static decimal ComputeLateFee(decimal installmentAmount, DateOnly dueDate, DateOnly asOf, LateFeePolicy policy)
    {
        ArgumentNullException.ThrowIfNull(policy);

        if (installmentAmount <= 0m)
        {
            return 0m;
        }

        var daysLate = asOf.DayNumber - dueDate.DayNumber;
        if (daysLate <= policy.GraceDays)
        {
            return 0m;
        }

        var percentageFee = installmentAmount * policy.PercentOfInstallment;
        var fee = Math.Max(policy.FlatFee, percentageFee);
        fee = Math.Min(fee, policy.Cap);
        fee = Math.Min(fee, installmentAmount);

        return decimal.Round(fee, 2, MidpointRounding.AwayFromZero);
    }
}
