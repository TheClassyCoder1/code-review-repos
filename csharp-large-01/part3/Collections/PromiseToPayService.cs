using Lending.Servicing.Part3.Infrastructure;

namespace Lending.Servicing.Part3.Collections;

/// <summary>
/// Entry point for promise-to-pay changes from the agent desktop and the payment feed.
/// Every mutation runs under the per-loan account lock.
/// </summary>
public sealed class PromiseToPayService
{
    private readonly PromiseToPayTracker _tracker;
    private readonly AccountLockProvider _locks;
    private readonly TimeProvider _clock;

    public PromiseToPayService(PromiseToPayTracker tracker, AccountLockProvider locks, TimeProvider clock)
    {
        _tracker = tracker;
        _locks = locks;
        _clock = clock;
    }

    public async Task<PromiseToPay?> RecordAsync(
        string loanId,
        decimal amount,
        DateOnly promisedBy,
        CancellationToken cancellationToken)
    {
        if (amount <= 0m)
        {
            throw new ArgumentOutOfRangeException(nameof(amount), "Promised amount must be positive.");
        }

        if (promisedBy < DateOnly.FromDateTime(_clock.GetUtcNow().UtcDateTime))
        {
            throw new ArgumentOutOfRangeException(nameof(promisedBy), "Promise date is in the past.");
        }

        await using (await _locks.AcquireAsync(loanId, cancellationToken))
        {
            _tracker.Record(loanId, amount, promisedBy);
            return _tracker.Get(loanId);
        }
    }

    public async Task OnPaymentAsync(string loanId, decimal amount, CancellationToken cancellationToken)
    {
        await using (await _locks.AcquireAsync(loanId, cancellationToken))
        {
            _tracker.ApplyPayment(loanId, amount);
        }
    }
}
