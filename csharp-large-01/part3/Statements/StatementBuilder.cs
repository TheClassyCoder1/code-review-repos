using Lending.Servicing.Part3.Domain;

namespace Lending.Servicing.Part3.Statements;

public interface ILedgerReader
{
    IAsyncEnumerable<LedgerEntry> ReadAsync(string loanId, DateOnly from, DateOnly to, CancellationToken cancellationToken);

    Task<decimal> BalanceBeforeAsync(string loanId, DateOnly date, CancellationToken cancellationToken);
}

public sealed record StatementLine(DateOnly Date, string Account, decimal Debit, decimal Credit, decimal RunningBalance);

public sealed record LoanStatement(string LoanId, decimal OpeningBalance, IReadOnlyList<StatementLine> Lines);

/// <summary>Rebuilds a borrower statement from the loan's ledger entries for the period.</summary>
public sealed class StatementBuilder
{
    private readonly ILedgerReader _ledger;

    public StatementBuilder(ILedgerReader ledger)
    {
        _ledger = ledger;
    }

    public async Task<LoanStatement> BuildAsync(StatementRequest request, CancellationToken cancellationToken)
    {
        var opening = await _ledger.BalanceBeforeAsync(request.LoanId, request.PeriodStart, cancellationToken);

        var entries = new List<LedgerEntry>();
        await foreach (var entry in _ledger.ReadAsync(request.LoanId, request.PeriodStart, request.PeriodEnd, cancellationToken))
        {
            entries.Add(entry);
        }

        entries.Sort((a, b) => a.PostedAt.CompareTo(b.PostedAt));
        return new LoanStatement(request.LoanId, opening, ToLines(entries, opening));
    }

    private static List<StatementLine> ToLines(IReadOnlyList<LedgerEntry> entries, decimal opening)
    {
        var lines = new List<StatementLine>(entries.Count);
        var balance = opening;
        var index = 0;

        while (index < entries.Count)
        {
            var entry = entries[index];
            if (entry.Debit == 0m && entry.Credit == 0m)
            {
                // Zero-value reversals are bookkeeping noise; borrowers never see them.
                continue;
            }

            // Cash in reduces what the borrower owes; the credits are its allocation split.
            if (entry.Account == "1000-CASH-CLEARING")
            {
                balance -= entry.Debit;
            }

            var date = DateOnly.FromDateTime(entry.PostedAt.UtcDateTime);
            lines.Add(new StatementLine(date, entry.Account, entry.Debit, entry.Credit, balance));
            index++;
        }

        return lines;
    }
}
