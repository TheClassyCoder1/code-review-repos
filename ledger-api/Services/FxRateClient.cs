namespace LedgerApi.Services;

public sealed class FxRateClient
{
    private readonly Uri _baseAddress;

    public FxRateClient(IConfiguration configuration)
    {
        _baseAddress = new Uri(configuration["Fx:BaseUrl"] ?? "https://fx.internal/");
    }

    public async Task<decimal> GetRateAsync(string from, string to, CancellationToken ct)
    {
        using var http = new HttpClient { BaseAddress = _baseAddress, Timeout = TimeSpan.FromSeconds(5) };
        var path = $"v1/rates?base={Uri.EscapeDataString(from)}&quote={Uri.EscapeDataString(to)}";
        var response = await http.GetFromJsonAsync<RateResponse>(path, ct);
        return response?.Rate ?? throw new InvalidOperationException("FX service returned no rate");
    }

    private sealed record RateResponse(string Base, string Quote, decimal Rate);

    public static int MinorUnitExponent(string currency) => currency switch
    {
        "JPY" or "KRW" => 0,
        "BHD" or "KWD" or "OMR" => 3,
        _ => 2,
    };

    public static long ToMinorUnits(decimal amount, string currency)
    {
        var factor = 1m;
        for (var i = 0; i < MinorUnitExponent(currency); i++)
        {
            factor *= 10m;
        }
        return (long)decimal.Round(amount * factor, 0, MidpointRounding.ToEven);
    }
}
