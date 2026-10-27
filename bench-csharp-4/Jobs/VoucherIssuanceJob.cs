using GiftCards.Services;

namespace GiftCards.Jobs;

public sealed class VoucherIssuanceJob : BackgroundService
{
    private readonly VoucherQueue _queue;
    private readonly IServiceScopeFactory _scopes;
    private readonly ILogger<VoucherIssuanceJob> _logger;

    public VoucherIssuanceJob(VoucherQueue queue, IServiceScopeFactory scopes, ILogger<VoucherIssuanceJob> logger)
    {
        _queue = queue;
        _scopes = scopes;
        _logger = logger;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        await foreach (var request in _queue.ReadAllAsync(stoppingToken))
        {
            using var scope = _scopes.CreateScope();
            var cards = scope.ServiceProvider.GetRequiredService<GiftCardService>();
            try
            {
                await cards.IssueAsync(request.ProgrammeId, request.RequestedBy, request.FaceValue, request.Currency, request.BatchId, stoppingToken);
            }
            catch (Exception ex) when (ex is not OperationCanceledException)
            {
                _logger.LogError(ex, "Issuing card for batch {BatchId} failed", request.BatchId);
            }
        }
    }
}
