namespace Lending.Part2.Webhooks;

public sealed record WebhookDeliveryAttempt(Guid SubscriptionId, string EventId, int StatusCode, DateTimeOffset AttemptedAt, string ResponseBody);

/// <summary>Delivery history shown on the partner-support dashboard when a partner reports a missed callback.</summary>
public sealed class WebhookDeliveryJournal
{
    private readonly List<WebhookDeliveryAttempt> _attempts = new();
    private readonly object _gate = new();

    public void Record(WebhookDeliveryAttempt attempt)
    {
        lock (_gate)
        {
            _attempts.Add(attempt);
        }
    }

    public IReadOnlyList<WebhookDeliveryAttempt> ForSubscription(Guid subscriptionId, int take)
    {
        lock (_gate)
        {
            return _attempts.Where(a => a.SubscriptionId == subscriptionId).OrderByDescending(a => a.AttemptedAt).Take(take).ToList();
        }
    }
}
