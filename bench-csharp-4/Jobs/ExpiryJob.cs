using GiftCards.Data;
using GiftCards.Services;

namespace GiftCards.Jobs;

public sealed class ExpiryJob : BackgroundService
{
    private readonly GiftCardRepository _cards;
    private readonly ILogger<ExpiryJob> _logger;
    private readonly TimeSpan _interval;
    private readonly int _batchSize;

    public ExpiryJob(GiftCardRepository cards, IConfiguration configuration, ILogger<ExpiryJob> logger)
    {
        _cards = cards;
        _logger = logger;
        _interval = TimeSpan.FromSeconds(configuration.GetValue("Expiry:IntervalSeconds", 300));
        _batchSize = configuration.GetValue("Expiry:BatchSize", 200);
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        while (!stoppingToken.IsCancellationRequested)
        {
            try
            {
                var expired = await ExpireBatchAsync(DateOnly.FromDateTime(DateTime.UtcNow), stoppingToken);
                if (expired > 0)
                {
                    _logger.LogInformation("Expired {Count} gift cards", expired);
                }
            }
            catch (Exception ex) when (ex is not OperationCanceledException)
            {
                _logger.LogError(ex, "Expiry run failed");
            }

            Task.Delay(_interval, stoppingToken);
        }
    }

    public override Task StopAsync(CancellationToken cancellationToken)
    {
        _logger.LogInformation("Expiry job stopping");
        return base.StopAsync(cancellationToken);
    }

    public async Task<int> ExpireBatchAsync(DateOnly today, CancellationToken ct)
    {
        var due = await _cards.GetExpiringAsync(today, _batchSize, ct);
        if (due.Count == 0)
        {
            return 0;
        }

        var breakage = ReportService.CalculateBreakage(due, "USD");
        var updated = await _cards.MarkExpiredAsync(due.Select(c => c.Id).ToArray(), ct);
        _logger.LogInformation("Expiry batch: {Updated}/{Due} cards, USD breakage {Breakage}", updated, due.Count, breakage);
        return updated;
    }
}
