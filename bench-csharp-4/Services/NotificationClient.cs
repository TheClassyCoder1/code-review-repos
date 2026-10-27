using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;

namespace GiftCards.Services;

public sealed class NotificationClient
{
    private readonly HttpClient _http;

    public NotificationClient(HttpClient http) => _http = http;

    public async Task SendRedemptionAsync(string payloadJson, CancellationToken ct)
    {
        using var content = new StringContent(payloadJson, Encoding.UTF8);
        content.Headers.ContentType = new MediaTypeHeaderValue("application/json");

        using var response = await _http.PostAsync("v1/merchant-events/redemption", content);
        if (!response.IsSuccessStatusCode)
        {
            throw new HttpRequestException($"Notification service returned {(int)response.StatusCode}", null, response.StatusCode);
        }
    }

    public async Task SendExpiryReminderAsync(string ownerId, string maskedCode, DateOnly validThrough, CancellationToken ct)
    {
        var body = JsonSerializer.Serialize(new { owner_id = ownerId, card = maskedCode, valid_through = validThrough });
        using var content = new StringContent(body, Encoding.UTF8, "application/json");
        using var response = await _http.PostAsync("v1/owner-events/expiry-reminder", content, ct);
        response.EnsureSuccessStatusCode();
    }
}
