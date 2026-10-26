using Lending.Servicing.Part4.Domain;
using Lending.Servicing.Part4.Infrastructure;

namespace Lending.Servicing.Part4.RateLocks;

public sealed record RateLockResponse(RateLockOutcome Outcome, RateLock? Lock, decimal ExtensionFee);

/// <summary>
/// The only entry point to <see cref="RateLockRegistry"/>. Every operation runs
/// under a single async gate, so the registry never sees concurrent callers.
/// </summary>
public sealed class RateLockCoordinator : IDisposable
{
    private static readonly TimeSpan DefaultLockDuration = TimeSpan.FromDays(30);

    private readonly RateLockRegistry _registry;
    private readonly IClock _clock;
    private readonly IServicingLog _log;
    private readonly SemaphoreSlim _gate = new(1, 1);

    public RateLockCoordinator(RateLockRegistry registry, IClock clock, IServicingLog log)
    {
        _registry = registry;
        _clock = clock;
        _log = log;
    }

    public Task<RateLockResponse> LockAsync(string loanId, decimal rate, CancellationToken cancellationToken) =>
        RunExclusiveAsync(() =>
        {
            var now = _clock.UtcNow;
            var outcome = _registry.TryLock(loanId, rate, now, DefaultLockDuration);
            _log.Info("Rate lock request for loan {0}: {1}", loanId, outcome);
            return new RateLockResponse(outcome, _registry.Find(loanId, now), 0m);
        }, cancellationToken);

    public Task<RateLockResponse> ExtendAsync(string loanId, decimal loanAmount, int extensionDays, CancellationToken cancellationToken) =>
        RunExclusiveAsync(() =>
        {
            var now = _clock.UtcNow;
            var current = _registry.Find(loanId, now);
            var fee = current is null ? 0m : RateLockPricing.ExtensionFee(loanAmount, current, extensionDays);
            var outcome = _registry.Extend(loanId, now, TimeSpan.FromDays(extensionDays));
            _log.Info("Rate lock extension for loan {0}: {1}", loanId, outcome);
            return new RateLockResponse(outcome, _registry.Find(loanId, now), outcome == RateLockOutcome.Extended ? fee : 0m);
        }, cancellationToken);

    public Task<int> PurgeExpiredAsync(CancellationToken cancellationToken) =>
        RunExclusiveAsync(() => _registry.PurgeExpired(_clock.UtcNow), cancellationToken);

    private async Task<T> RunExclusiveAsync<T>(Func<T> action, CancellationToken cancellationToken)
    {
        await _gate.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            return action();
        }
        finally
        {
            _gate.Release();
        }
    }

    public void Dispose() => _gate.Dispose();
}
