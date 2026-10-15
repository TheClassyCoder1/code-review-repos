using System.Threading.Channels;
using LedgerApi.Models;

namespace LedgerApi.Services;

public sealed class AuditPublisher : BackgroundService
{
    private readonly Channel<AuditEvent> _channel = Channel.CreateBounded<AuditEvent>(
        new BoundedChannelOptions(10_000) { FullMode = BoundedChannelFullMode.DropOldest });

    private readonly ILogger<AuditPublisher> _logger;

    public AuditPublisher(ILogger<AuditPublisher> logger)
    {
        _logger = logger;
    }

    public Task EnqueueAsync(AuditEvent evt)
    {
        try
        {
            if (!_channel.Writer.TryWrite(evt))
            {
                _logger.LogWarning("Audit channel rejected {Action} for account {AccountId}", evt.Action, evt.AccountId);
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to enqueue audit event {Action}", evt.Action);
        }
        return Task.CompletedTask;
    }

    public int Pending => _channel.Reader.Count;

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        await foreach (var evt in _channel.Reader.ReadAllAsync(stoppingToken))
        {
            _logger.LogInformation(
                "audit action={Action} account={AccountId} actor={ActorId} at={At:o}",
                evt.Action, evt.AccountId, evt.ActorId, evt.At);
        }
    }
}
