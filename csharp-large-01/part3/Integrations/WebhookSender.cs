using System.Security.Cryptography;
using System.Text;
using Lending.Servicing.Part3.Domain;
using Lending.Servicing.Part3.Infrastructure;
using Microsoft.Extensions.Logging;

namespace Lending.Servicing.Part3.Integrations;

public enum WebhookOutcome { Delivered, Retryable, Rejected }

/// <summary>
/// Single delivery attempt of a signed partner webhook. Never throws for transport or
/// partner-side failures; the outcome tells <see cref="WebhookDeliveryWorker"/> what to do next.
/// </summary>
public sealed class WebhookSender
{
    private readonly HttpClient _http;
    private readonly IReadOnlyDictionary<string, string> _signingKeys;
    private readonly ILogger<WebhookSender> _logger;

    public WebhookSender(
        ServicingHttpClientFactory clients,
        IReadOnlyDictionary<string, string> signingKeys,
        ILogger<WebhookSender> logger)
    {
        _http = clients.CreateWebhookClient();
        _signingKeys = signingKeys;
        _logger = logger;
    }

    public async Task<WebhookOutcome> SendAsync(WebhookDelivery delivery, CancellationToken cancellationToken)
    {
        if (!_signingKeys.TryGetValue(delivery.PartnerCode, out var key))
        {
            _logger.LogError("No signing key for partner {PartnerCode}", delivery.PartnerCode);
            return WebhookOutcome.Rejected;
        }

        using var hmac = new HMACSHA256(Encoding.UTF8.GetBytes(key));
        var signature = Convert.ToHexString(hmac.ComputeHash(Encoding.UTF8.GetBytes(delivery.Payload)));

        using var request = new HttpRequestMessage(HttpMethod.Post, delivery.Target)
        {
            Content = new StringContent(delivery.Payload, Encoding.UTF8, "application/json")
        };
        request.Headers.Add("X-Servicing-Delivery", delivery.DeliveryId);
        request.Headers.Add("X-Servicing-Signature", "sha256=" + signature.ToLowerInvariant());

        try
        {
            using var response = await _http.SendAsync(request, cancellationToken);
            var status = (int)response.StatusCode;
            if (status is >= 200 and < 300)
            {
                return WebhookOutcome.Delivered;
            }

            return status is 408 or 429 or >= 500 ? WebhookOutcome.Retryable : WebhookOutcome.Rejected;
        }
        catch (Exception ex) when ((ex is HttpRequestException or TaskCanceledException)
                                   && !cancellationToken.IsCancellationRequested)
        {
            _logger.LogWarning("Webhook {DeliveryId} to partner {PartnerCode} failed: {Error}",
                delivery.DeliveryId, delivery.PartnerCode, ex.GetType().Name);
            return WebhookOutcome.Retryable;
        }
    }
}
