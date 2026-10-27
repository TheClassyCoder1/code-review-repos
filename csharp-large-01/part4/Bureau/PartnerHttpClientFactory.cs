using Lending.Servicing.Part4.Infrastructure;

namespace Lending.Servicing.Part4.Bureau;

public sealed record PartnerBureauOptions(
    Uri BaseAddress,
    string ApiKey,
    TimeSpan RequestTimeout,
    TimeSpan ConnectTimeout)
{
    public static readonly TimeSpan MaxRequestTimeout = TimeSpan.FromSeconds(15);

    public static PartnerBureauOptions Default(Uri baseAddress, string apiKey) =>
        new(baseAddress, apiKey, TimeSpan.FromSeconds(8), TimeSpan.FromSeconds(3));
}

/// <summary>
/// Builds the single shared transport for partner bureau traffic. Every request
/// sent through the resulting <see cref="HttpClient"/> is bounded by
/// <see cref="HttpClient.Timeout"/> and the handler's connect timeout.
/// </summary>
public sealed class PartnerHttpClientFactory : IDisposable
{
    private readonly HttpClient _client;

    public PartnerHttpClientFactory(PartnerBureauOptions options)
    {
        ArgumentNullException.ThrowIfNull(options);

        if (options.RequestTimeout <= TimeSpan.Zero || options.RequestTimeout > PartnerBureauOptions.MaxRequestTimeout)
        {
            throw new ArgumentOutOfRangeException(nameof(options), "Bureau request timeout must be between zero and 15 seconds.");
        }

        if (options.ConnectTimeout <= TimeSpan.Zero || options.ConnectTimeout > options.RequestTimeout)
        {
            throw new ArgumentOutOfRangeException(nameof(options), "Bureau connect timeout must be positive and no longer than the request timeout.");
        }

        var handler = new SocketsHttpHandler
        {
            ConnectTimeout = options.ConnectTimeout,
            PooledConnectionLifetime = TimeSpan.FromMinutes(5),
            MaxConnectionsPerServer = 16,
        };

        _client = new HttpClient(handler, disposeHandler: true)
        {
            BaseAddress = options.BaseAddress,
            Timeout = options.RequestTimeout,
        };
        _client.DefaultRequestHeaders.Add("X-Partner-Key", options.ApiKey);
    }

    public PartnerBureauClient CreateBureauClient(IClock clock, IServicingLog log) =>
        new(_client, clock, log);

    public void Dispose() => _client.Dispose();
}
