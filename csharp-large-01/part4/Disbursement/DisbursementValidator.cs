using Lending.Servicing.Part4.Domain;

namespace Lending.Servicing.Part4.Disbursement;

public static class DisbursementValidator
{
    public const decimal MaxSingleDisbursement = 2_500_000m;
    public const int MaxDaysAhead = 90;

    private static readonly HashSet<string> SupportedCurrencies = new(StringComparer.Ordinal) { "EUR", "GBP", "USD" };

    /// <summary>Returns every problem with the request; an empty list means it is valid.</summary>
    public static IReadOnlyList<string> Validate(DisbursementRequest request, DateOnly today)
    {
        ArgumentNullException.ThrowIfNull(request);
        var errors = new List<string>();

        if (string.IsNullOrWhiteSpace(request.RequestId))
        {
            errors.Add("request id is required");
        }

        if (string.IsNullOrWhiteSpace(request.LoanId))
        {
            errors.Add("loan id is required");
        }

        if (request.Amount <= 0m || request.Amount > MaxSingleDisbursement || decimal.Round(request.Amount, 2) != request.Amount)
        {
            errors.Add($"amount must be positive, at most {MaxSingleDisbursement}, with at most two decimal places");
        }

        if (request.CurrencyCode is null || !SupportedCurrencies.Contains(request.CurrencyCode))
        {
            errors.Add("currency is not supported for disbursement");
        }

        if (!IbanValidator.IsValid(request.DestinationIban))
        {
            errors.Add("destination account is not a valid IBAN");
        }

        if (request.RequestedDate < today || request.RequestedDate > today.AddDays(MaxDaysAhead))
        {
            errors.Add($"requested date must be between today and {MaxDaysAhead} days ahead");
        }

        return errors;
    }
}
