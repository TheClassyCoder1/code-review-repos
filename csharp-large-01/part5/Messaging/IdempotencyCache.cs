using System;
using System.Collections.Concurrent;
using System.Collections.Generic;

namespace CsharpLarge01.Part5.Messaging
{
    /// <summary>
    /// Remembers recently processed idempotency keys (payment webhooks, partner
    /// callbacks) so a redelivered request is acknowledged without being applied
    /// twice. Entries expire after a fixed time-to-live.
    /// </summary>
    public sealed class IdempotencyCache
    {
        private const int SweepEvery = 1024;

        private readonly ConcurrentDictionary<string, DateTime> _entries =
            new ConcurrentDictionary<string, DateTime>(StringComparer.Ordinal);
        private readonly TimeSpan _ttl;
        private readonly Func<DateTime> _utcNow;
        private int _operationsSinceSweep;

        public IdempotencyCache(TimeSpan ttl, Func<DateTime> utcNow = null)
        {
            if (ttl <= TimeSpan.Zero)
            {
                throw new ArgumentOutOfRangeException(nameof(ttl));
            }
            _ttl = ttl;
            _utcNow = utcNow ?? (() => DateTime.UtcNow);
        }

        public int Count => _entries.Count;

        /// <summary>
        /// Returns true if the caller is the first to claim <paramref name="idempotencyKey"/>
        /// within the TTL and should process the request; false for a duplicate.
        /// </summary>
        public bool TryBegin(string idempotencyKey)
        {
            if (string.IsNullOrEmpty(idempotencyKey))
            {
                throw new ArgumentException("Idempotency key is required.", nameof(idempotencyKey));
            }

            var now = _utcNow();
            var expiresAt = now + _ttl;
            SweepIfDue(now);

            while (true)
            {
                if (_entries.TryAdd(idempotencyKey, expiresAt))
                {
                    return true;
                }
                if (!_entries.TryGetValue(idempotencyKey, out var existingExpiry))
                {
                    // Removed by a concurrent sweep between the two calls; try again.
                    continue;
                }
                if (existingExpiry > now)
                {
                    return false;
                }
                // Expired entry: only one caller can swap it for a fresh claim.
                if (_entries.TryUpdate(idempotencyKey, expiresAt, existingExpiry))
                {
                    return true;
                }
            }
        }

        private void SweepIfDue(DateTime now)
        {
            if (System.Threading.Interlocked.Increment(ref _operationsSinceSweep) % SweepEvery != 0)
            {
                return;
            }

            foreach (var entry in _entries)
            {
                if (entry.Value <= now)
                {
                    ((ICollection<KeyValuePair<string, DateTime>>)_entries).Remove(entry);
                }
            }
        }
    }
}
