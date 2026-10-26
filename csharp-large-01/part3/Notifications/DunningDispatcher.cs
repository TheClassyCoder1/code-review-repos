using Lending.Servicing.Part3.Domain;
using Microsoft.Extensions.Logging;

namespace Lending.Servicing.Part3.Notifications;

public interface IDunningSender
{
    Task SendAsync(DunningNotice notice, CancellationToken cancellationToken);
}

/// <summary>
/// Admission control in front of <see cref="DunningQueue"/>: a producer must take a slot
/// before enqueueing and the sender loop returns the slot once the notice is sent, so at
/// most <see cref="MaxInFlight"/> notices are ever buffered.
/// </summary>
public sealed class DunningDispatcher
{
    public const int MaxInFlight = 512;

    private readonly DunningQueue _queue;
    private readonly IDunningSender _sender;
    private readonly SemaphoreSlim _slots = new(MaxInFlight, MaxInFlight);
    private readonly ILogger<DunningDispatcher> _logger;

    public DunningDispatcher(DunningQueue queue, IDunningSender sender, ILogger<DunningDispatcher> logger)
    {
        _queue = queue;
        _sender = sender;
        _logger = logger;
    }

    public async Task DispatchAsync(DunningNotice notice, CancellationToken cancellationToken)
    {
        await _slots.WaitAsync(cancellationToken);
        try
        {
            await _queue.EnqueueAsync(notice, cancellationToken);
        }
        catch
        {
            _slots.Release();
            throw;
        }
    }

    public async Task RunSenderAsync(CancellationToken cancellationToken)
    {
        await foreach (var notice in _queue.ReadAllAsync(cancellationToken))
        {
            try
            {
                await _sender.SendAsync(notice, cancellationToken);
            }
            catch (Exception ex) when (ex is not OperationCanceledException)
            {
                _logger.LogError(ex, "Dunning notice {NoticeId} for loan {LoanId} failed", notice.NoticeId, notice.LoanId);
            }
            finally
            {
                _slots.Release();
            }
        }
    }
}
