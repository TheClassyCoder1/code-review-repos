using System.Net.Http.Json;
using System.Security.Cryptography;
using System.Text;

namespace Hotel.Services;

public record CaptureResult(string TransactionId, string Status);

public class PaymentGatewayClient
{
    private readonly HttpClient _http;
    private readonly ILogger<PaymentGatewayClient> _logger;

    public PaymentGatewayClient(HttpClient http, ILogger<PaymentGatewayClient> logger)
    {
        _http = http;
        _logger = logger;
    }

    public async Task<CaptureResult> CaptureAsync(string paymentToken, decimal amount, string currency, string reference, CancellationToken ct)
    {
        using var request = new HttpRequestMessage(HttpMethod.Post, "captures")
        {
            Content = JsonContent.Create(new { token = paymentToken, amount, currency, reference }),
        };
        request.Headers.Add("Idempotency-Key", BuildIdempotencyKey(reference, amount));
        using var response = await _http.SendAsync(request, ct);
        response.EnsureSuccessStatusCode();
        var result = await response.Content.ReadFromJsonAsync<CaptureResult>(cancellationToken: ct);
        _logger.LogInformation("Captured payment for {Reference}", reference);
        return result ?? throw new InvalidOperationException("Empty capture response.");
    }

    public async Task<CaptureResult> RefundAsync(string transactionId, decimal amount, CancellationToken ct)
    {
        using var response = await _http.PostAsJsonAsync($"captures/{Uri.EscapeDataString(transactionId)}/refunds", new { amount }, ct);
        response.EnsureSuccessStatusCode();
        var result = await response.Content.ReadFromJsonAsync<CaptureResult>(cancellationToken: ct);
        return result ?? throw new InvalidOperationException("Empty refund response.");
    }

    public static string BuildIdempotencyKey(string reference, decimal amount)
    {
        var material = $"{reference}|{amount.ToString("F2", System.Globalization.CultureInfo.InvariantCulture)}";
        var digest = SHA256.HashData(Encoding.UTF8.GetBytes(material));
        return Convert.ToHexString(digest, 0, 16).ToLowerInvariant();
    }
}
