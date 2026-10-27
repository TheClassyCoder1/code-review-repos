using GiftCards.Data;
using GiftCards.Services;

namespace GiftCards.Jobs;

public sealed class NotificationRetryJob : BackgroundService
{
    private static readonly TimeSpan Interval = TimeSpan.FromSeconds(30);

    private readonly IServiceScopeFactory _scopes;
    private readonly RedemptionRepository _redemptions;
    private readonly ILogger<NotificationRetryJob> _logger;

    public NotificationRetryJob(IServiceScopeFactory scopes, RedemptionRepository redemptions, ILogger<NotificationRetryJob> logger)
    {
        _scopes = scopes;
        _redemptions = redemptions;
        _logger = logger;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        using var timer = new PeriodicTimer(Interval);
        while (await timer.WaitForNextTickAsync(stoppingToken))
        {
            var pending = await _redemptions.GetPendingNotificationsAsync(100, stoppingToken);
            if (pending.Count == 0)
            {
                continue;
            }

            using var scope = _scopes.CreateScope();
            var service = scope.ServiceProvider.GetRequiredService<RedemptionService>();
            foreach (var (id, payload) in pending)
            {
                await service.NotifyMerchantAsync(id, payload, stoppingToken);
            }
            _logger.LogInformation("Retried {Count} merchant notifications", pending.Count);
        }
    }
}
