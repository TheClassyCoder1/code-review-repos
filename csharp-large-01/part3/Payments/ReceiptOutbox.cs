using Lending.Servicing.Part3.Domain;
using Microsoft.Extensions.Logging;

namespace Lending.Servicing.Part3.Payments;

public interface ILedgerPoster
{
    Task PostAsync(IReadOnlyList<LedgerEntry> entries, CancellationToken cancellationToken);
}

/// <summary>
/// Collects receipts produced by a remittance batch and turns them into balanced ledger
/// entries. The batch job calls <see cref="FlushAsync"/> once the file is fully applied.
/// </summary>
public sealed class ReceiptOutbox
{
    private readonly List<PaymentReceipt> _pending = new();
    private readonly object _sync = new();
    private readonly ILedgerPoster _ledger;
    private readonly ILogger<ReceiptOutbox> _logger;

    public ReceiptOutbox(ILedgerPoster ledger, ILogger<ReceiptOutbox> logger)
    {
        _ledger = ledger;
        _logger = logger;
    }

    public void Add(PaymentReceipt receipt)
    {
        lock (_sync)
        {
            _pending.Add(receipt);
        }
    }

    public async Task<int> FlushAsync(string batchId, CancellationToken cancellationToken)
    {
        List<PaymentReceipt> batch;
        lock (_sync)
        {
            batch = new List<PaymentReceipt>(_pending);
            _pending.Clear();
        }

        if (batch.Count == 0)
        {
            return 0;
        }

        var entries = batch.SelectMany(ToEntries).ToList();

        try
        {
            await _ledger.PostAsync(entries, cancellationToken);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Ledger posting for remittance batch {BatchId} did not complete", batchId);
        }

        _logger.LogInformation("Remittance batch {BatchId}: flushed {Receipts} receipts as {Entries} ledger entries",
            batchId, batch.Count, entries.Count);
        return batch.Count;
    }

    private static IEnumerable<LedgerEntry> ToEntries(PaymentReceipt r)
    {
        yield return new LedgerEntry($"{r.ReceiptId}-CASH", r.LoanId, "1000-CASH-CLEARING", r.Amount, 0m, r.PostedAt);

        var splits = new (string Suffix, string Account, decimal Amount)[]
        {
            ("FEE", "4100-FEE-INCOME", r.AppliedToFees),
            ("INT", "1210-INTEREST-RECEIVABLE", r.AppliedToInterest),
            ("PRN", "1200-LOANS-PRINCIPAL", r.AppliedToPrincipal)
        };

        foreach (var (suffix, account, amount) in splits.Where(s => s.Amount > 0m))
        {
            yield return new LedgerEntry($"{r.ReceiptId}-{suffix}", r.LoanId, account, 0m, amount, r.PostedAt);
        }
    }
}
