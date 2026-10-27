using System.Collections.Concurrent;

namespace Lending.Part2.Webhooks;

public sealed record WebhookSubscription(Guid Id, string PartnerId, Uri CallbackUrl, IReadOnlyCollection<string> EventTypes);

public interface IWebhookSubscriptionStore
{
    void Add(WebhookSubscription subscription);

    IReadOnlyList<WebhookSubscription> ListForEvent(string eventType);
}

public sealed class InMemoryWebhookSubscriptionStore : IWebhookSubscriptionStore
{
    private readonly ConcurrentDictionary<Guid, WebhookSubscription> _subscriptions = new();

    public void Add(WebhookSubscription subscription) => _subscriptions[subscription.Id] = subscription;

    public IReadOnlyList<WebhookSubscription> ListForEvent(string eventType) =>
        _subscriptions.Values.Where(s => s.EventTypes.Contains(eventType, StringComparer.OrdinalIgnoreCase)).ToList();
}
