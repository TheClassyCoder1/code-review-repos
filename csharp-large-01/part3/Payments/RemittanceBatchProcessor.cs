using Lending.Servicing.Part3.Domain;
using Microsoft.Extensions.Logging;

namespace Lending.Servicing.Part3.Payments;

public sealed record RemittanceBatchSummary(
    string BatchId, int LinesReceived, int LinesApplied, decimal AmountReceived, decimal AmountApplied);

/// <summary>
/// Applies a parsed remittance file. Lines are independent payments, so they are applied
/// in parallel to keep the morning bank file inside its processing window.
/// </summary>
public sealed class RemittanceBatchProcessor
{
    private const int MaxParallelism = 16;

    private readonly PaymentAllocationService _allocation;
    private readonly ReceiptOutbox _outbox;
    private readonly ILogger<RemittanceBatchProcessor> _logger;

    public RemittanceBatchProcessor(
        PaymentAllocationService allocation,
        ReceiptOutbox outbox,
        ILogger<RemittanceBatchProcessor> logger)
    {
        _allocation = allocation;
        _outbox = outbox;
        _logger = logger;
    }

    public async Task<RemittanceBatchSummary> ProcessAsync(
        string batchId,
        IReadOnlyList<RemittanceLine> lines,
        CancellationToken cancellationToken)
    {
        var applied = 0;
        var amountApplied = 0m;

        var options = new ParallelOptions
        {
            MaxDegreeOfParallelism = MaxParallelism,
            CancellationToken = cancellationToken
        };

        await Parallel.ForEachAsync(lines, options, async (line, ct) =>
        {
            var receipt = await _allocation.AllocateAsync(line, ct);
            if (receipt is null)
            {
                return;
            }

            Interlocked.Increment(ref applied);
            amountApplied += receipt.Amount;
            _outbox.Add(receipt);
        });

        var summary = new RemittanceBatchSummary(
            batchId,
            lines.Count,
            applied,
            lines.Sum(l => l.Amount),
            amountApplied);

        _logger.LogInformation(
            "Remittance batch {BatchId}: {Applied}/{Received} lines applied, {AmountApplied} of {AmountReceived}",
            batchId, summary.LinesApplied, summary.LinesReceived, summary.AmountApplied, summary.AmountReceived);

        return summary;
    }
}
