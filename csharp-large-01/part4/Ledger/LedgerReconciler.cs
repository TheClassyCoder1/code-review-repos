using Lending.Servicing.Part4.Domain;

namespace Lending.Servicing.Part4.Ledger;

/// <summary>
/// Compares the ledger for a loan with the statement the customer received.
/// Statement lines carry the ledger entry id in their description prefix,
/// e.g. "LE-000123 Monthly payment".
/// </summary>
public static class LedgerReconciler
{
    public static ReconciliationResult Reconcile(
        string loanId,
        IReadOnlyCollection<LedgerEntry> ledger,
        Statement statement)
    {
        ArgumentNullException.ThrowIfNull(ledger);
        ArgumentNullException.ThrowIfNull(statement);

        var inPeriod = ledger
            .Where(e => e.LoanId == loanId && e.PostedOn >= statement.PeriodStart && e.PostedOn <= statement.PeriodEnd)
            .ToList();

        var ledgerIds = new HashSet<string>(inPeriod.Select(e => e.EntryId), StringComparer.Ordinal);
        var statementIds = new HashSet<string>(StringComparer.Ordinal);
        foreach (var line in statement.Lines)
        {
            var space = line.Description.IndexOf(' ');
            statementIds.Add(space < 0 ? line.Description : line.Description[..space]);
        }

        var missing = ledgerIds.Where(id => !statementIds.Contains(id)).OrderBy(id => id, StringComparer.Ordinal).ToList();
        var unknown = statementIds.Where(id => !ledgerIds.Contains(id)).OrderBy(id => id, StringComparer.Ordinal).ToList();

        var ledgerBalance = statement.OpeningBalance + inPeriod.Sum(e => e.Amount);
        var statementBalance = statement.OpeningBalance + statement.Lines.Sum(l => l.Amount);

        return new ReconciliationResult(loanId, ledgerBalance, statementBalance, missing, unknown);
    }
}
