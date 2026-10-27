namespace Lending.Part8.Calculations;

public static class MonthlyPaymentCalculator
{
    /// <summary>
    /// Level monthly payment for a fully amortizing loan, rounded half-up to the cent.
    /// </summary>
    public static decimal Calculate(decimal principal, decimal annualRatePercent, int termMonths)
    {
        if (principal <= 0)
        {
            throw new ArgumentOutOfRangeException(nameof(principal), "Principal must be positive.");
        }
        if (annualRatePercent < 0)
        {
            throw new ArgumentOutOfRangeException(nameof(annualRatePercent), "Rate cannot be negative.");
        }
        if (termMonths <= 0)
        {
            throw new ArgumentOutOfRangeException(nameof(termMonths), "Term must be at least one month.");
        }

        if (annualRatePercent == 0)
        {
            return Math.Round(principal / termMonths, 2, MidpointRounding.AwayFromZero);
        }

        var monthlyRate = (double)(annualRatePercent / 100m / 12m);
        var growth = Math.Pow(1 + monthlyRate, termMonths);
        var payment = (double)principal * monthlyRate * growth / (growth - 1);
        return Math.Round((decimal)payment, 2, MidpointRounding.AwayFromZero);
    }
}
