namespace Lending.Part8.Receipts;

public sealed record OutboxEntry(long Id, PaymentReceipt Receipt, int Attempts);

public interface IReceiptOutbox
{
    Task<IReadOnlyList<OutboxEntry>> NextBatchAsync(int size, CancellationToken ct);

    Task MarkPublishedAsync(long id, CancellationToken ct);

    Task RecordFailedAttemptAsync(long id, CancellationToken ct);

    Task MoveToDeadLetterAsync(long id, CancellationToken ct);
}

/// <summary>
/// Drains the receipt outbox. A failed publish leaves the entry in place for the next
/// pass; entries that keep failing are logged and parked for manual replay.
/// </summary>
public sealed class ReceiptOutboxWorker(
    IReceiptOutbox outbox,
    ReceiptPublisher publisher,
    ILogger<ReceiptOutboxWorker> logger) : BackgroundService
{
    private const int MaxAttempts = 8;
    private static readonly TimeSpan PollInterval = TimeSpan.FromSeconds(15);

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        using var timer = new PeriodicTimer(PollInterval);
        do
        {
            foreach (var entry in await outbox.NextBatchAsync(50, stoppingToken))
            {
                if (await publisher.TryPublishAsync(entry.Receipt, stoppingToken))
                {
                    await outbox.MarkPublishedAsync(entry.Id, stoppingToken);
                    continue;
                }

                await outbox.RecordFailedAttemptAsync(entry.Id, stoppingToken);
                if (entry.Attempts + 1 >= MaxAttempts)
                {
                    logger.LogError(
                        "Receipt {ReceiptId} failed to publish after {Attempts} attempts; moved to dead letter",
                        entry.Receipt.ReceiptId, entry.Attempts + 1);
                    await outbox.MoveToDeadLetterAsync(entry.Id, stoppingToken);
                }
                else
                {
                    logger.LogWarning(
                        "Receipt {ReceiptId} publish attempt {Attempt} failed; will retry",
                        entry.Receipt.ReceiptId, entry.Attempts + 1);
                }
            }
        }
        while (await timer.WaitForNextTickAsync(stoppingToken));
    }
}
