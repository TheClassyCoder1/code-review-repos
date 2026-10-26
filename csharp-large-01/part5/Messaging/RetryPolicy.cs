using System;

namespace CsharpLarge01.Part5.Messaging
{
    /// <summary>
    /// Exponential backoff with equal jitter for outbox redelivery and partner calls.
    /// </summary>
    public sealed class RetryPolicy
    {
        // 2^30 times any sensible base delay is far beyond the cap; clamping the
        // exponent keeps the multiplication finite for very large attempt counts.
        private const int MaxExponent = 30;

        private readonly TimeSpan _baseDelay;
        private readonly TimeSpan _maxDelay;
        private readonly int _maxAttempts;
        private readonly Random _random;

        public RetryPolicy(TimeSpan baseDelay, TimeSpan maxDelay, int maxAttempts, Random random = null)
        {
            if (baseDelay <= TimeSpan.Zero)
            {
                throw new ArgumentOutOfRangeException(nameof(baseDelay));
            }
            if (maxDelay < baseDelay)
            {
                throw new ArgumentOutOfRangeException(nameof(maxDelay), "Max delay must be at least the base delay.");
            }
            if (maxAttempts < 1)
            {
                throw new ArgumentOutOfRangeException(nameof(maxAttempts));
            }

            _baseDelay = baseDelay;
            _maxDelay = maxDelay;
            _maxAttempts = maxAttempts;
            _random = random ?? new Random();
        }

        public int MaxAttempts => _maxAttempts;

        public bool ShouldRetry(int attemptsSoFar)
        {
            return attemptsSoFar < _maxAttempts;
        }

        public TimeSpan ComputeDelay(int attempt)
        {
            if (attempt < 1)
            {
                throw new ArgumentOutOfRangeException(nameof(attempt), "Attempts are numbered from 1.");
            }

            var exponent = Math.Min(attempt - 1, MaxExponent);
            var ceilingMs = Math.Min(
                _baseDelay.TotalMilliseconds * Math.Pow(2, exponent),
                _maxDelay.TotalMilliseconds);

            double sample;
            // System.Random is not thread-safe; the outbox and HTTP clients share one policy.
            lock (_random)
            {
                sample = _random.NextDouble();
            }

            // Equal jitter: half the ceiling is guaranteed, the other half is random,
            // which spreads retries out without ever collapsing to an immediate retry.
            var half = ceilingMs / 2d;
            return TimeSpan.FromMilliseconds(half + sample * half);
        }
    }
}
