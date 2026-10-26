using System.Collections.Concurrent;
using System.Net.Http.Json;

namespace Lending.Part2.Webhooks;

public sealed record WebhookEvent(string EventId, string EventType, string LoanId, DateTimeOffset OccurredAt);

/// <summary>Fans a servicing event out to every partner subscribed to its type.</summary>
public sealed class WebhookDispatcher(HttpClient http, IWebhookSubscriptionStore subscriptions, WebhookDeliveryJournal journal)
{
    private const int SuspendAfterFailures = 20;

    private static readonly ConcurrentDictionary<string, int> ConsecutiveFailures = new(StringComparer.Ordinal);

    public Task DispatchAsync(WebhookEvent webhookEvent, CancellationToken ct) =>
        Task.WhenAll(subscriptions.ListForEvent(webhookEvent.EventType)
            .Where(s => ConsecutiveFailures.GetValueOrDefault(s.PartnerId) < SuspendAfterFailures)
            .Select(s => DeliverAsync(s, webhookEvent, ct)));

    private async Task DeliverAsync(WebhookSubscription subscription, WebhookEvent webhookEvent, CancellationToken ct)
    {
        int status;
        string responseBody;
        try
        {
            using var response = await http.PostAsJsonAsync(subscription.CallbackUrl, webhookEvent, ct);
            status = (int)response.StatusCode;
            responseBody = await response.Content.ReadAsStringAsync(ct);
        }
        catch (Exception ex) when (ex is HttpRequestException || (ex is TaskCanceledException && !ct.IsCancellationRequested))
        {
            status = 0;
            responseBody = ex.Message;
        }

        journal.Record(new WebhookDeliveryAttempt(subscription.Id, webhookEvent.EventId, status, DateTimeOffset.UtcNow, responseBody));

        if (status is >= 200 and < 300)
        {
            ConsecutiveFailures[subscription.PartnerId] = 0;
            return;
        }

        var failures = ConsecutiveFailures.GetValueOrDefault(subscription.PartnerId) + 1;
        ConsecutiveFailures[subscription.PartnerId] = failures;
    }
}
