namespace PolicyAdmin.Security;

public sealed class AllowedHostsHandler : DelegatingHandler
{
    private readonly HashSet<string> _allowedHosts;

    public AllowedHostsHandler(IConfiguration configuration)
    {
        var hosts = configuration.GetSection("Carriers:AllowedHosts").Get<string[]>() ?? Array.Empty<string>();
        _allowedHosts = new HashSet<string>(hosts, StringComparer.OrdinalIgnoreCase);
    }

    protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
    {
        var uri = request.RequestUri;
        if (uri is null || uri.Scheme != Uri.UriSchemeHttps || !_allowedHosts.Contains(uri.Host))
        {
            throw new HttpRequestException($"Carrier host is not allowed: {uri?.Host}");
        }
        return base.SendAsync(request, cancellationToken);
    }
}
