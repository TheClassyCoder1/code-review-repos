namespace Lending.Part2.Account;

/// <summary>Post-logout redirect URIs registered per portal client in configuration.</summary>
public sealed class PortalClientRegistry
{
    private readonly Dictionary<string, List<Uri>> _postLogoutUris = new(StringComparer.Ordinal);

    public PortalClientRegistry(IConfiguration configuration)
    {
        foreach (var client in configuration.GetSection("PortalClients").GetChildren())
        {
            var uris = client.GetSection("PostLogoutRedirectUris").Get<string[]>() ?? Array.Empty<string>();
            _postLogoutUris[client.Key] = uris.Select(u => new Uri(u, UriKind.Absolute)).ToList();
        }
    }

    public bool IsRegisteredPostLogoutUri(string clientId, string candidate) =>
        _postLogoutUris.TryGetValue(clientId, out var registered)
        && Uri.TryCreate(candidate, UriKind.Absolute, out var uri)
        && registered.Any(r => Uri.Compare(r, uri, UriComponents.AbsoluteUri, UriFormat.UriEscaped, StringComparison.Ordinal) == 0);

    public Uri? DefaultPostLogoutUri(string clientId) =>
        _postLogoutUris.TryGetValue(clientId, out var registered) ? registered.FirstOrDefault() : null;
}
