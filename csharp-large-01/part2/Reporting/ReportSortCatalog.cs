namespace Lending.Part2.Reporting;

public sealed record SortSpec(string Column, string Direction);

/// <summary>
/// The only place where a report's sort order is turned into SQL. Callers pass the
/// public sort key from the query string; it is looked up in a fixed table of column
/// expressions and the direction is reduced to the literal ASC or DESC. Unknown keys
/// fall back to the default ordering rather than being passed through.
/// </summary>
public sealed class ReportSortCatalog
{
    private static readonly IReadOnlyDictionary<string, string> DelinquencyColumns =
        new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase)
        {
            ["dpd"] = "l.days_past_due",
            ["balance"] = "l.outstanding_balance",
            ["loan"] = "l.loan_id",
            ["branch"] = "l.branch_code",
            ["lastPayment"] = "p.last_payment_on",
        };

    public SortSpec ForDelinquency(string? sortKey, string? direction)
    {
        if (string.IsNullOrWhiteSpace(sortKey) || !DelinquencyColumns.TryGetValue(sortKey, out var column))
        {
            return new SortSpec("l.days_past_due", "DESC");
        }

        return new SortSpec(column, string.Equals(direction, "asc", StringComparison.OrdinalIgnoreCase) ? "ASC" : "DESC");
    }
}
