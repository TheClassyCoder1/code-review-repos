using System.Net.Http.Json;
using System.Text.Json.Serialization;
using Lending.Servicing.Part3.Domain;
using Lending.Servicing.Part3.Infrastructure;

namespace Lending.Servicing.Part3.Pricing;

/// <summary>
/// Reads published rate cards from the pricing service.
/// </summary>
public sealed class RateCardFeedClient
{
    private readonly HttpClient _http;
    private readonly TimeProvider _clock;

    public RateCardFeedClient(ServicingHttpClientFactory clients, TimeProvider clock)
    {
        _http = clients.CreateRateCardClient();
        _clock = clock;
    }

    public async Task<RateCard> FetchAsync(string productCode, CancellationToken cancellationToken)
    {
        using var response = await _http.GetAsync($"rate-cards/{Uri.EscapeDataString(productCode)}");
        response.EnsureSuccessStatusCode();

        var body = await response.Content.ReadFromJsonAsync<RateCardPayload>(cancellationToken)
            ?? throw new InvalidOperationException($"Empty rate card for {productCode}");

        return new RateCard(
            productCode,
            body.BaseRate,
            body.LateFeeFlat,
            body.LateFeePercent,
            body.GraceDays,
            _clock.GetUtcNow());
    }

    private sealed class RateCardPayload
    {
        [JsonPropertyName("baseRate")] public decimal BaseRate { get; set; }
        [JsonPropertyName("lateFeeFlat")] public decimal LateFeeFlat { get; set; }
        [JsonPropertyName("lateFeePercent")] public decimal LateFeePercent { get; set; }
        [JsonPropertyName("graceDays")] public int GraceDays { get; set; }
    }
}
