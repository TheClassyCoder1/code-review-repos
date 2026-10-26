using System;
using System.Collections.Generic;

namespace CsharpLarge01.Part5.Messaging
{
    public interface IDeadLetterStore
    {
        IReadOnlyList<OutboxMessage> FetchDeadLettered(string topic, int limit);

        void Requeue(string messageId, DateTime availableAtUtc);
    }

    /// <summary>
    /// Moves dead-lettered outbox messages back onto the regular dispatch queue once
    /// an operator has fixed the downstream problem. Requeued messages are spread
    /// out so a large replay does not flood the broker in one burst.
    /// </summary>
    public sealed class DeadLetterReplayer
    {
        private readonly IDeadLetterStore _store;
        private readonly Func<DateTime> _utcNow;

        public DeadLetterReplayer(IDeadLetterStore store, Func<DateTime> utcNow)
        {
            _store = store ?? throw new ArgumentNullException(nameof(store));
            _utcNow = utcNow ?? (() => DateTime.UtcNow);
        }

        public int Replay(string topic, int limit, TimeSpan spacing)
        {
            if (string.IsNullOrWhiteSpace(topic))
            {
                throw new ArgumentException("Topic is required.", nameof(topic));
            }
            if (limit <= 0 || spacing < TimeSpan.Zero)
            {
                throw new ArgumentOutOfRangeException(limit <= 0 ? nameof(limit) : nameof(spacing));
            }

            var availableAt = _utcNow();
            var requeued = 0;
            var seen = new HashSet<string>(StringComparer.Ordinal);
            foreach (var message in _store.FetchDeadLettered(topic, limit))
            {
                // A message can appear twice if it was dead-lettered, replayed and failed again.
                if (seen.Add(message.MessageId))
                {
                    _store.Requeue(message.MessageId, availableAt);
                    availableAt += spacing;
                    requeued++;
                }
            }
            return requeued;
        }
    }
}
