namespace Lending.Part2.Webhooks;

public sealed class WebhookRegistrationException(string message) : Exception(message);

public sealed class WebhookSubscriptionService(
    IWebhookSubscriptionStore store,
    CallbackUrlPolicy policy,
    ILogger<WebhookSubscriptionService> logger)
{
    private static readonly string[] SupportedEvents = { "payment.posted", "payment.returned", "loan.delinquent", "loan.paid_off" };

    public WebhookSubscription Subscribe(string partnerId, string callbackUrl, IReadOnlyCollection<string> eventTypes)
    {
        if (eventTypes.Count == 0 || eventTypes.Any(e => !SupportedEvents.Contains(e, StringComparer.OrdinalIgnoreCase)))
        {
            throw new WebhookRegistrationException("event types must be a non-empty subset of " + string.Join(", ", SupportedEvents));
        }

        if (!policy.TryAccept(callbackUrl, out var accepted, out var reason))
        {
            throw new WebhookRegistrationException(reason!);
        }

        var subscription = new WebhookSubscription(Guid.NewGuid(), partnerId, accepted!, eventTypes.Select(e => e.ToLowerInvariant()).Distinct().ToList());
        store.Add(subscription);
        logger.LogInformation("Partner {PartnerId} created webhook subscription {SubscriptionId}", partnerId, subscription.Id);
        return subscription;
    }
}
