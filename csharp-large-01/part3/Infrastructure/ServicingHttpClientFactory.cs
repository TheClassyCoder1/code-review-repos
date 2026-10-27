namespace Lending.Servicing.Part3.Infrastructure;

public sealed class ServicingHttpOptions
{
    public Uri RateCardBaseAddress { get; set; } = new("http://pricing.internal:8080/");
    public Uri AgencyBaseAddress { get; set; } = new("http://collections-gw.internal:8443/");
    public TimeSpan RateCardTimeout { get; set; } = TimeSpan.FromSeconds(10);
    public TimeSpan AgencyRequestTimeout { get; set; } = TimeSpan.FromSeconds(15);
    public TimeSpan ConnectTimeout { get; set; } = TimeSpan.FromSeconds(3);
    public TimeSpan WebhookTimeout { get; set; } = TimeSpan.FromSeconds(8);
}

/// <summary>
/// Single place where the servicing module builds its outbound HTTP clients, so every
/// client gets a connect timeout and an overall request deadline.
/// </summary>
public sealed class ServicingHttpClientFactory
{
    private readonly ServicingHttpOptions _options;

    public ServicingHttpClientFactory(ServicingHttpOptions options)
    {
        _options = options;
    }

    public HttpClient CreateRateCardClient()
    {
        var handler = new SocketsHttpHandler
        {
            ConnectTimeout = _options.ConnectTimeout,
            PooledConnectionLifetime = TimeSpan.FromMinutes(5)
        };

        return new HttpClient(handler)
        {
            BaseAddress = _options.RateCardBaseAddress,
            Timeout = _options.RateCardTimeout
        };
    }

    public HttpClient CreateAgencyClient()
    {
        var sockets = new SocketsHttpHandler
        {
            ConnectTimeout = _options.ConnectTimeout,
            PooledConnectionLifetime = TimeSpan.FromMinutes(5)
        };

        // Agency exports can be large, so the deadline lives in the handler, not on the client.
        var pipeline = new PerRequestTimeoutHandler(_options.AgencyRequestTimeout)
        {
            InnerHandler = sockets
        };

        return new HttpClient(pipeline)
        {
            BaseAddress = _options.AgencyBaseAddress,
            Timeout = Timeout.InfiniteTimeSpan
        };
    }

    public HttpClient CreateWebhookClient()
    {
        var handler = new SocketsHttpHandler
        {
            ConnectTimeout = _options.ConnectTimeout,
            PooledConnectionLifetime = TimeSpan.FromMinutes(2),
            AllowAutoRedirect = false
        };

        return new HttpClient(handler)
        {
            Timeout = _options.WebhookTimeout
        };
    }
}
