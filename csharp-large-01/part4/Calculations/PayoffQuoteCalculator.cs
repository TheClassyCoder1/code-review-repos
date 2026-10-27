namespace Lending.Servicing.Part4.Calculations;

public sealed record PayoffQuote(
    DateOnly PayoffDate,
    decimal PrincipalBalance,
    decimal InterestThroughPayoff,
    decimal FeesDue,
    decimal PerDiem,
    decimal Total);

public static class PayoffQuoteCalculator
{
    public const int MaxQuoteHorizonDays = 30;

    /// <summary>
    /// Quotes the amount needed to close the loan on <paramref name="payoffDate"/>,
    /// accruing simple interest on an Actual/365 basis from the last accrual date.
    /// </summary>
    public static PayoffQuote Quote(
        decimal principalBalance,
        decimal accruedInterest,
        decimal annualRate,
        DateOnly lastAccrualDate,
        DateOnly payoffDate,
        decimal feesDue)
    {
        if (principalBalance < 0m || accruedInterest < 0m || feesDue < 0m || annualRate < 0m)
        {
            throw new ArgumentOutOfRangeException(nameof(principalBalance), "Balances and rate cannot be negative.");
        }

        var days = payoffDate.DayNumber - lastAccrualDate.DayNumber;
        if (days < 0 || days > MaxQuoteHorizonDays)
        {
            throw new ArgumentOutOfRangeException(nameof(payoffDate), $"Payoff date must be within {MaxQuoteHorizonDays} days of the last accrual.");
        }

        var perDiem = decimal.Round(principalBalance * annualRate / 365m, 4, MidpointRounding.AwayFromZero);
        var interest = decimal.Round(accruedInterest + perDiem * days, 2, MidpointRounding.AwayFromZero);
        var total = principalBalance + interest + feesDue;

        return new PayoffQuote(payoffDate, principalBalance, interest, feesDue, perDiem, total);
    }
}
