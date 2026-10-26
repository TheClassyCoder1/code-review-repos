using Lending.Servicing.Part3.Domain;

namespace Lending.Servicing.Part3.Payments;

public sealed record AccrualSlice(DateOnly From, DateOnly To, decimal Interest);

/// <summary>
/// Pure interest arithmetic for the nightly accrual job. Interest is computed in slices of
/// <see cref="AccrualOptions.PeriodDays"/> and rounded per slice, which is how statements show it.
/// </summary>
public sealed class InterestAccrualCalculator
{
    private readonly AccrualOptions _options;

    public InterestAccrualCalculator(AccrualOptions options)
    {
        _options = options;
    }

    public IReadOnlyList<AccrualSlice> Accrue(LoanAccount account, DateOnly asOf)
    {
        var slices = new List<AccrualSlice>();
        var cursor = account.LastAccrualDate ?? asOf;
        if (account.PrincipalBalance <= 0m || account.AnnualRate <= 0m)
        {
            return slices;
        }

        while (cursor < asOf)
        {
            var next = cursor.AddDays(_options.PeriodDays);
            if (next > asOf)
            {
                next = asOf;
            }

            var yearFraction = (next.DayNumber - cursor.DayNumber) / 365m;
            var interest = Math.Round(account.PrincipalBalance * account.AnnualRate * yearFraction, 2, MidpointRounding.ToEven);
            slices.Add(new AccrualSlice(cursor, next, interest));
            cursor = next;
        }

        return slices;
    }
}
