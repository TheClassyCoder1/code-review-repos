namespace Lending.Part8.Calculations;

public sealed record PrepaymentTerms(int ProtectedMonths, decimal ChargePercent, decimal ChargeCap, decimal FreeAnnualAllowancePercent);

public static class PrepaymentChargeCalculator
{
    /// <summary>
    /// Charge on an early repayment: a percentage of the amount above the yearly free
    /// allowance, waived after the protected period and never above the cap.
    /// </summary>
    public static decimal Calculate(
        PrepaymentTerms terms, decimal outstandingPrincipal, decimal prepaymentAmount,
        decimal prepaidThisYear, DateOnly originatedOn, DateOnly prepaidOn)
    {
        ArgumentNullException.ThrowIfNull(terms);
        if (prepaymentAmount <= 0 || outstandingPrincipal <= 0)
        {
            return 0m;
        }

        var monthsElapsed = (prepaidOn.Year - originatedOn.Year) * 12 + prepaidOn.Month - originatedOn.Month;
        if (prepaidOn.Day < originatedOn.Day)
        {
            monthsElapsed--;
        }
        if (monthsElapsed >= terms.ProtectedMonths)
        {
            return 0m;
        }

        var allowance = outstandingPrincipal * terms.FreeAnnualAllowancePercent / 100m;
        var remainingAllowance = Math.Max(0m, allowance - prepaidThisYear);
        var chargeable = Math.Max(0m, Math.Min(prepaymentAmount, outstandingPrincipal) - remainingAllowance);

        var charge = chargeable * terms.ChargePercent / 100m;
        return Math.Round(Math.Min(charge, terms.ChargeCap), 2, MidpointRounding.AwayFromZero);
    }
}
