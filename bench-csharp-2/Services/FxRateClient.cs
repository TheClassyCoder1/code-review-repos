using System.Net.Http.Json;

namespace Hotel.Services;

public class FxRateClient
{
    private readonly HttpClient _http = new()
    {
        BaseAddress = new Uri("https://fx.example/api/"),
        Timeout = TimeSpan.FromSeconds(5),
    };

    public async Task<decimal> GetRateAsync(string from, string to, CancellationToken ct)
    {
        if (string.Equals(from, to, StringComparison.OrdinalIgnoreCase))
        {
            return 1m;
        }
        var quote = await _http.GetFromJsonAsync<FxQuote>($"rates/{from}/{to}", ct);
        return quote?.Rate ?? throw new InvalidOperationException($"No FX rate for {from}/{to}.");
    }

    private record FxQuote(decimal Rate);
}
