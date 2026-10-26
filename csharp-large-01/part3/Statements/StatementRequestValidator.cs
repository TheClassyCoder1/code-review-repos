using Lending.Servicing.Part3.Domain;

namespace Lending.Servicing.Part3.Statements;

/// <summary>
/// Admission rules for on-demand statements. Long ranges are served from the archived
/// annual statements, never rebuilt from the ledger.
/// </summary>
public static class StatementRequestValidator
{
    /// <summary>A quarter plus slack: the longest period the ledger rebuild will accept.</summary>
    public const int MaxPeriodDays = 93;

    public static void Validate(StatementRequest request, DateOnly today)
    {
        if (string.IsNullOrWhiteSpace(request.LoanId))
        {
            throw new ArgumentException("A loan id is required.", nameof(request));
        }

        if (request.PeriodEnd < request.PeriodStart || request.PeriodEnd > today)
        {
            throw new ArgumentException("Statement period must be a past, non-empty range.", nameof(request));
        }

        if (request.PeriodEnd.DayNumber - request.PeriodStart.DayNumber + 1 > MaxPeriodDays)
        {
            throw new ArgumentException(
                $"Statement periods are limited to {MaxPeriodDays} days; use the annual archive.", nameof(request));
        }
    }
}
