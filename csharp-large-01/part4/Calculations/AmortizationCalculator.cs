using Lending.Servicing.Part4.Domain;

namespace Lending.Servicing.Part4.Calculations;

/// <summary>Level-payment amortisation for fixed-rate instalment loans.</summary>
public static class AmortizationCalculator
{
    public const decimal MaxAnnualRate = 1m;
    public const int MaxTermMonths = 480;

    public static decimal ComputeMonthlyPayment(decimal principal, decimal annualRate, int termMonths)
    {
        if (principal <= 0m)
        {
            throw new ArgumentOutOfRangeException(nameof(principal), "Principal must be positive.");
        }

        if (termMonths <= 0 || termMonths > MaxTermMonths)
        {
            throw new ArgumentOutOfRangeException(nameof(termMonths), $"Term must be between 1 and {MaxTermMonths} months.");
        }

        if (annualRate < 0m || annualRate > MaxAnnualRate)
        {
            throw new ArgumentOutOfRangeException(nameof(annualRate), "Annual rate must be between 0 and 1.");
        }

        if (annualRate == 0m)
        {
            return decimal.Round(principal / termMonths, 2, MidpointRounding.AwayFromZero);
        }

        var monthlyRate = annualRate / 12m;
        var growth = 1m;
        for (var i = 0; i < termMonths; i++)
        {
            growth *= 1m + monthlyRate;
        }

        var payment = principal * monthlyRate * growth / (growth - 1m);
        return decimal.Round(payment, 2, MidpointRounding.AwayFromZero);
    }

    public static IReadOnlyList<ScheduleRow> BuildSchedule(LoanAccount loan)
    {
        ArgumentNullException.ThrowIfNull(loan);

        var payment = ComputeMonthlyPayment(loan.Principal, loan.AnnualRate, loan.TermMonths);
        var monthlyRate = loan.AnnualRate / 12m;
        var balance = loan.Principal;
        var rows = new List<ScheduleRow>(loan.TermMonths);

        for (var n = 1; n <= loan.TermMonths && balance > 0m; n++)
        {
            var interest = decimal.Round(balance * monthlyRate, 2, MidpointRounding.AwayFromZero);
            var principalPart = payment - interest;

            // The last instalment absorbs rounding drift so the loan closes at exactly zero.
            if (n == loan.TermMonths || principalPart > balance)
            {
                principalPart = balance;
            }

            balance -= principalPart;
            rows.Add(new ScheduleRow(
                Number: n,
                DueDate: loan.FirstPaymentDate.AddMonths(n - 1),
                Payment: principalPart + interest,
                Interest: interest,
                Principal: principalPart,
                RemainingBalance: balance));
        }

        return rows;
    }
}
