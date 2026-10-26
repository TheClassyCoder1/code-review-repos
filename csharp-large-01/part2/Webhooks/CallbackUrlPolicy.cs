namespace Lending.Part2.Webhooks;

/// <summary>
/// Decides whether a partner-supplied callback URL may be stored as a webhook target.
/// Only https URLs on the default port whose host is listed under
/// <c>Webhooks:AllowedCallbackHosts</c> are accepted; IP literals and userinfo are refused.
/// </summary>
public sealed class CallbackUrlPolicy(IConfiguration configuration)
{
    private readonly HashSet<string> _allowedHosts = new(
        (configuration.GetSection("Webhooks:AllowedCallbackHosts").Get<string[]>() ?? Array.Empty<string>()).Select(h => h.Trim().TrimEnd('.')),
        StringComparer.OrdinalIgnoreCase);

    public bool TryAccept(string candidate, out Uri? callbackUrl, out string? reason)
    {
        callbackUrl = null;
        reason = null;

        if (!Uri.TryCreate(candidate, UriKind.Absolute, out var uri) || uri.Scheme != Uri.UriSchemeHttps)
        {
            reason = "callback must be an absolute https URL";
            return false;
        }

        if (!string.IsNullOrEmpty(uri.UserInfo) || !uri.IsDefaultPort || uri.HostNameType != UriHostNameType.Dns)
        {
            reason = "callback must be a plain domain name without credentials or a custom port";
            return false;
        }

        if (!_allowedHosts.Contains(uri.IdnHost.TrimEnd('.')))
        {
            reason = "callback host is not on the partner allow-list";
            return false;
        }

        callbackUrl = uri;
        return true;
    }
}
