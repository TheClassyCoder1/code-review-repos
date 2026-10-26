using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;

namespace CsharpLarge01.Part5.Messaging
{
    public sealed class OutboxMessage
    {
        public OutboxMessage(string messageId, string topic, string key, string payload, int attempts)
        {
            MessageId = messageId ?? throw new ArgumentNullException(nameof(messageId));
            Topic = topic ?? throw new ArgumentNullException(nameof(topic));
            Key = key;
            Payload = payload ?? throw new ArgumentNullException(nameof(payload));
            Attempts = attempts;
        }

        public string MessageId { get; }

        public string Topic { get; }

        public string Key { get; }

        public string Payload { get; }

        public int Attempts { get; }
    }

    public interface IOutboxStore
    {
        IReadOnlyList<OutboxMessage> FetchDue(DateTime nowUtc, int limit);

        void MarkPublished(string messageId, DateTime publishedAtUtc);

        void ScheduleRetry(string messageId, int attempts, DateTime nextAttemptUtc, string failureReason);

        void MarkDeadLettered(string messageId, string failureReason);
    }

    public interface IMessagePublisher
    {
        Task PublishAsync(string topic, string key, string payload, CancellationToken cancellationToken);
    }

    public sealed class DispatchSummary
    {
        public DispatchSummary(int published, int retried, int deadLettered)
        {
            Published = published;
            Retried = retried;
            DeadLettered = deadLettered;
        }

        public int Published { get; }

        public int Retried { get; }

        public int DeadLettered { get; }
    }

    /// <summary>
    /// Relays collections events (promise broken, fee assessed, payoff quoted) from
    /// the transactional outbox to the broker. Payloads are forwarded byte-for-byte;
    /// the dispatcher never reshapes them.
    /// </summary>
    public sealed class OutboxDispatcher
    {
        private readonly IOutboxStore _store;
        private readonly IMessagePublisher _publisher;
        private readonly RetryPolicy _retryPolicy;
        private readonly Func<DateTime> _utcNow;
        private readonly int _batchSize;

        public OutboxDispatcher(IOutboxStore store, IMessagePublisher publisher, RetryPolicy retryPolicy,
            Func<DateTime> utcNow, int batchSize)
        {
            if (batchSize <= 0)
            {
                throw new ArgumentOutOfRangeException(nameof(batchSize));
            }

            _store = store ?? throw new ArgumentNullException(nameof(store));
            _publisher = publisher ?? throw new ArgumentNullException(nameof(publisher));
            _retryPolicy = retryPolicy ?? throw new ArgumentNullException(nameof(retryPolicy));
            _utcNow = utcNow ?? (() => DateTime.UtcNow);
            _batchSize = batchSize;
        }

        public async Task<DispatchSummary> DispatchPendingAsync(CancellationToken cancellationToken)
        {
            var due = _store.FetchDue(_utcNow(), _batchSize);
            int published = 0, retried = 0, deadLettered = 0;

            foreach (var message in due)
            {
                cancellationToken.ThrowIfCancellationRequested();
                try
                {
                    await _publisher.PublishAsync(message.Topic, message.Key, message.Payload, cancellationToken)
                        .ConfigureAwait(false);
                    _store.MarkPublished(message.MessageId, _utcNow());
                    published++;
                }
                catch (OperationCanceledException) when (cancellationToken.IsCancellationRequested)
                {
                    throw;
                }
                catch (Exception ex)
                {
                    // Record only the exception type: broker errors can echo the payload back.
                    var reason = ex.GetType().Name;
                    var attempts = message.Attempts + 1;
                    if (_retryPolicy.ShouldRetry(attempts))
                    {
                        _store.ScheduleRetry(message.MessageId, attempts, _utcNow() + _retryPolicy.ComputeDelay(attempts), reason);
                        retried++;
                    }
                    else
                    {
                        _store.MarkDeadLettered(message.MessageId, reason);
                        deadLettered++;
                    }
                }
            }

            return new DispatchSummary(published, retried, deadLettered);
        }
    }
}
