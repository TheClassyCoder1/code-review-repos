using Lending.Servicing.Part4.Domain;

namespace Lending.Servicing.Part4.RateLocks;

public enum RateLockOutcome
{
    Locked,
    AlreadyLocked,
    Extended,
    NotFound,
    Expired,
}

/// <summary>
/// In-memory book of active rate locks, keyed by loan id. Plain collections are
/// used deliberately; see <see cref="RateLockCoordinator"/> for how access is ordered.
/// </summary>
public sealed class RateLockRegistry
{
    private readonly Dictionary<string, RateLock> _locks = new(StringComparer.Ordinal);

    public int Count => _locks.Count;

    public RateLockOutcome TryLock(string loanId, decimal rate, DateTimeOffset now, TimeSpan duration)
    {
        if (_locks.TryGetValue(loanId, out var existing) && existing.ExpiresAt > now)
        {
            return RateLockOutcome.AlreadyLocked;
        }

        _locks[loanId] = new RateLock(loanId, rate, now, now + duration);
        return RateLockOutcome.Locked;
    }

    public RateLockOutcome Extend(string loanId, DateTimeOffset now, TimeSpan extension)
    {
        if (!_locks.TryGetValue(loanId, out var existing))
        {
            return RateLockOutcome.NotFound;
        }

        if (existing.ExpiresAt <= now)
        {
            _locks.Remove(loanId);
            return RateLockOutcome.Expired;
        }

        _locks[loanId] = existing with { ExpiresAt = existing.ExpiresAt + extension };
        return RateLockOutcome.Extended;
    }

    public RateLock? Find(string loanId, DateTimeOffset now) =>
        _locks.TryGetValue(loanId, out var existing) && existing.ExpiresAt > now ? existing : null;

    public int PurgeExpired(DateTimeOffset now)
    {
        var expired = _locks.Where(kv => kv.Value.ExpiresAt <= now).Select(kv => kv.Key).ToList();
        foreach (var loanId in expired)
        {
            _locks.Remove(loanId);
        }

        return expired.Count;
    }
}
