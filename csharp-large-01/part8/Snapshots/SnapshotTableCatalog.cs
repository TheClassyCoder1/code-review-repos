namespace Lending.Part8.Snapshots;

/// <summary>
/// The fixed set of tables operators may snapshot. Request values are only ever used
/// as lookup keys; the table name handed to the dump comes from this map.
/// </summary>
public static class SnapshotTableCatalog
{
    private static readonly IReadOnlyDictionary<string, string> Tables =
        new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase)
        {
            ["loans"] = "servicing.loans",
            ["installments"] = "servicing.installments",
            ["payments"] = "servicing.payments",
            ["delinquency-cases"] = "collections.delinquency_cases",
            ["promises-to-pay"] = "collections.promises_to_pay",
        };

    public static bool TryResolve(string requested, out string tableName)
    {
        if (Tables.TryGetValue(requested, out var known))
        {
            tableName = known;
            return true;
        }

        tableName = string.Empty;
        return false;
    }
}
