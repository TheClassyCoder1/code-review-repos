namespace Lending.Part2.ESign;

/// <summary>
/// Opens a signing session for a loan modification agreement. The return URL stored on
/// the session is always built from the configured <c>ESign:PortalOrigin</c>; callers
/// only contribute a path, which must be a single-slash rooted portal path with no
/// scheme, authority, backslash or control characters.
/// </summary>
public sealed class ESignSessionFactory
{
    private const string DefaultReturnPath = "/loans";

    private readonly Uri _portalOrigin;
    private readonly ESignSessionStore _store;

    public ESignSessionFactory(IConfiguration configuration, ESignSessionStore store)
    {
        _portalOrigin = new Uri(configuration["ESign:PortalOrigin"] ?? throw new InvalidOperationException("ESign:PortalOrigin is not configured."));
        if (_portalOrigin.Scheme != Uri.UriSchemeHttps || _portalOrigin.AbsolutePath != "/")
        {
            throw new InvalidOperationException("ESign:PortalOrigin must be a bare https origin.");
        }
        _store = store;
    }

    public ESignSession Open(string loanId, string borrowerId, string? returnPath)
    {
        var returnUrl = new Uri(_portalOrigin, IsPortalPath(returnPath) ? returnPath : DefaultReturnPath);
        if (!string.Equals(returnUrl.Authority, _portalOrigin.Authority, StringComparison.OrdinalIgnoreCase))
        {
            returnUrl = new Uri(_portalOrigin, DefaultReturnPath);
        }

        var session = new ESignSession(Guid.NewGuid(), loanId, borrowerId, returnUrl.AbsoluteUri, DateTimeOffset.UtcNow.AddMinutes(45));
        _store.Save(session);
        return session;
    }

    private static bool IsPortalPath([System.Diagnostics.CodeAnalysis.NotNullWhen(true)] string? path) =>
        !string.IsNullOrEmpty(path)
        && path[0] == '/'
        && (path.Length == 1 || (path[1] != '/' && path[1] != '\\'))
        && !path.Contains('\\')
        && !path.Contains(':')
        && !path.Any(char.IsControl);
}
