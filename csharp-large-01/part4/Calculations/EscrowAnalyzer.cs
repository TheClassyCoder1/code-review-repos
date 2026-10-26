namespace Lending.Servicing.Part4.Calculations;

public sealed record EscrowProjection(
    decimal LowPointBalance,
    int LowPointMonth,
    decimal RequiredCushion,
    decimal Shortage,
    decimal Surplus);

/// <summary>
/// Annual escrow analysis for taxes and insurance paid on the borrower's behalf.
/// The cushion is one sixth of projected annual disbursements.
/// </summary>
public static class EscrowAnalyzer
{
    public const int MonthsInCycle = 12;
    public const decimal CushionFraction = 1m / 6m;
    public const decimal SurplusRefundThreshold = 50m;

    /// <summary>
    /// Projects the escrow balance month by month over the next cycle and
    /// reports the low point together with any shortage or surplus against
    /// the required cushion.
    /// </summary>
    public static EscrowProjection Project(decimal currentBalance, decimal monthlyDeposit, IReadOnlyList<decimal> monthlyDisbursements)
    {
        ArgumentNullException.ThrowIfNull(monthlyDisbursements);

        if (monthlyDisbursements.Count != MonthsInCycle)
        {
            throw new ArgumentException($"Exactly {MonthsInCycle} monthly disbursements are required.", nameof(monthlyDisbursements));
        }

        if (monthlyDeposit < 0m || monthlyDisbursements.Any(d => d < 0m))
        {
            throw new ArgumentOutOfRangeException(nameof(monthlyDeposit), "Deposits and disbursements cannot be negative.");
        }

        var balance = currentBalance;
        var lowPoint = currentBalance;
        var lowPointMonth = 0;
        for (var month = 1; month <= MonthsInCycle; month++)
        {
            balance += monthlyDeposit - monthlyDisbursements[month - 1];
            if (balance < lowPoint)
            {
                lowPoint = balance;
                lowPointMonth = month;
            }
        }

        var cushion = decimal.Round(monthlyDisbursements.Sum() * CushionFraction, 2, MidpointRounding.AwayFromZero);
        var shortage = lowPoint < cushion ? cushion - lowPoint : 0m;
        var surplus = lowPoint > cushion ? lowPoint - cushion : 0m;

        return new EscrowProjection(lowPoint, lowPointMonth, cushion, shortage, surplus);
    }

    /// <summary>
    /// Next cycle's monthly escrow deposit: one twelfth of the projected
    /// disbursements plus any shortage spread evenly over the cycle. Surpluses
    /// at or above the refund threshold are refunded, so they do not reduce
    /// the deposit; smaller surpluses are credited against it.
    /// </summary>
    public static decimal NextMonthlyDeposit(IReadOnlyList<decimal> monthlyDisbursements, EscrowProjection projection)
    {
        ArgumentNullException.ThrowIfNull(monthlyDisbursements);
        ArgumentNullException.ThrowIfNull(projection);

        var baseDeposit = monthlyDisbursements.Sum() / MonthsInCycle;
        var adjustment = 0m;

        if (projection.Shortage > 0m)
        {
            adjustment = projection.Shortage / MonthsInCycle;
        }
        else if (projection.Surplus > 0m && projection.Surplus < SurplusRefundThreshold)
        {
            adjustment = -projection.Surplus / MonthsInCycle;
        }

        var deposit = Math.Max(0m, baseDeposit + adjustment);
        return decimal.Round(deposit, 2, MidpointRounding.AwayFromZero);
    }
}
