using Lending.Servicing.Part3.Domain;
using Lending.Servicing.Part3.Infrastructure;
using Microsoft.Extensions.Logging;

namespace Lending.Servicing.Part3.Integrations;

public interface IWebhookDeadLetterStore
{
    Task StoreAsync(WebhookDelivery delivery, string reason, CancellationToken cancellationToken);
}

/// <summary>
/// Delivers partner webhooks. Retryable outcomes back off according to
/// <see cref="RetryPolicy"/>; once the policy gives up, or the partner rejects the call,
/// the delivery is dead-lettered for the integrations team to replay.
/// </summary>
public sealed class WebhookDeliveryWorker
{
    private readonly WebhookSender _sender;
    private readonly RetryPolicy _retry;
    private readonly IWebhookDeadLetterStore _deadLetters;
    private readonly ILogger<WebhookDeliveryWorker> _logger;

    public WebhookDeliveryWorker(
        WebhookSender sender,
        RetryPolicy retry,
        IWebhookDeadLetterStore deadLetters,
        ILogger<WebhookDeliveryWorker> logger)
    {
        _sender = sender;
        _retry = retry;
        _deadLetters = deadLetters;
        _logger = logger;
    }

    public async Task<WebhookOutcome> DeliverAsync(WebhookDelivery delivery, CancellationToken cancellationToken)
    {
        var failedAttempts = 0;

        while (true)
        {
            var outcome = await _sender.SendAsync(delivery, cancellationToken);
            if (outcome != WebhookOutcome.Retryable)
            {
                if (outcome == WebhookOutcome.Rejected)
                {
                    await _deadLetters.StoreAsync(delivery, "rejected by partner", cancellationToken);
                }

                return outcome;
            }

            failedAttempts++;
            var delay = _retry.NextDelay(failedAttempts);
            if (delay is null)
            {
                _logger.LogWarning("Webhook {DeliveryId} exhausted {Attempts} attempts; dead-lettering",
                    delivery.DeliveryId, failedAttempts);
                await _deadLetters.StoreAsync(delivery, $"retries exhausted after {failedAttempts} attempts", cancellationToken);
                return outcome;
            }

            await Task.Delay(delay.Value, cancellationToken);
        }
    }
}
