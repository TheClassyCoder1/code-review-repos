namespace Lending.Servicing.Part3.Infrastructure;

/// <summary>
/// Bounded exponential backoff. <see cref="NextDelay"/> returns <c>null</c> once the
/// attempt budget is spent, which callers treat as "give up and rethrow".
/// </summary>
public sealed class RetryPolicy
{
    public const int MaxAllowedAttempts = 10;

    private readonly TimeSpan _baseDelay;
    private readonly TimeSpan _maxDelay;

    public RetryPolicy(int maxAttempts, TimeSpan baseDelay, TimeSpan maxDelay)
    {
        // Misconfiguration must never turn a retry loop into an unbounded one.
        MaxAttempts = Math.Clamp(maxAttempts, 1, MaxAllowedAttempts);
        _baseDelay = baseDelay <= TimeSpan.Zero ? TimeSpan.FromMilliseconds(100) : baseDelay;
        _maxDelay = maxDelay < _baseDelay ? _baseDelay : maxDelay;
    }

    public int MaxAttempts { get; }

    public static RetryPolicy Default { get; } =
        new(maxAttempts: 5, baseDelay: TimeSpan.FromMilliseconds(200), maxDelay: TimeSpan.FromSeconds(5));

    /// <param name="failedAttempts">Number of attempts that have already failed (1-based).</param>
    public TimeSpan? NextDelay(int failedAttempts)
    {
        if (failedAttempts >= MaxAttempts)
        {
            return null;
        }

        var factor = Math.Pow(2, Math.Max(0, failedAttempts - 1));
        var millis = Math.Min(_baseDelay.TotalMilliseconds * factor, _maxDelay.TotalMilliseconds);
        return TimeSpan.FromMilliseconds(millis);
    }
}
